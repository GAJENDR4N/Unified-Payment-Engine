package io.gomobi.payment.service;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.core.enums.MerchantStatus;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.QrMode;
import io.gomobi.payment.core.enums.QrType;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.CustomerDetails;
import io.gomobi.payment.core.model.PaymentRequest;
import io.gomobi.payment.core.model.PaymentResponse;
import io.gomobi.payment.dto.CreatePaymentRequest;
import io.gomobi.payment.dto.CreatePaymentResponse;
import io.gomobi.payment.dto.CustomerInfo;
import io.gomobi.payment.entity.Merchant;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.QrTransactionDetails;
import io.gomobi.payment.entity.SubMerchant;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.registry.PaymentAdapterRegistry;
import io.gomobi.payment.repository.MerchantRepository;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import io.gomobi.payment.repository.QrTransactionDetailsRepository;
import io.gomobi.payment.repository.SubMerchantRepository;
import io.gomobi.payment.util.DbPersistLog;
import io.gomobi.payment.util.JsonUtils;
import io.gomobi.payment.util.PaymentMethodBrandMapper;
import io.gomobi.payment.util.TransactionIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Single entry point for creating a payment.
 * <p>
 * Flow: resolve merchant/sub-merchant/host -> persist PAYMENT_TRANSACTION
 * and QR_TRANSACTION_DETAILS with status INITIATED -> call the resolved
 * provider adapter -> persist the outcome. The transaction row exists in
 * the database *before* the provider is ever called, so a crash or timeout
 * mid-call still leaves a durable, queryable record rather than losing the
 * attempt entirely.
 * <p>
 * There is no separate audit table here by design - the gateway layer
 * upstream owns cross-system audit/recon; this service's job is to create
 * the payment, persist it, and hand back a response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final MerchantRepository merchantRepository;
    private final SubMerchantRepository subMerchantRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final QrTransactionDetailsRepository qrTransactionDetailsRepository;
    private final HostResolutionService hostResolutionService;
    private final PaymentAdapterRegistry adapterRegistry;
    private final TransactionIdGenerator transactionIdGenerator;

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        long flowStart = System.currentTimeMillis();
        Merchant merchant = resolveMerchant(request);
        SubMerchant subMerchant = resolveSubMerchant(request.getSubMerchantMid());

        Optional<PaymentTransaction> existing = checkIdempotency(merchant, request);
        if (existing.isPresent()) {
            return toResponse(existing.get(), request, null);
        }

        ResolvedHost resolvedHost = hostResolutionService.resolve(request.getPaymentMethod().getChannelCode());
        PaymentBrand brand = PaymentMethodBrandMapper.toBrand(resolvedHost.paymentMethod().getPaymentMethodCode());

        String transactionId = transactionIdGenerator.generate();

        PaymentTransaction transaction = persistInitialTransaction(request, merchant, subMerchant, resolvedHost, transactionId);
        persistQrDetails(request, transaction);

        PaymentResponse providerResponse;
        try {
            providerResponse = callProvider(request, resolvedHost, brand, transaction);
        } catch (PaymentEngineException e) {
            markTransactionFailed(transaction, e);
            log.error("event=PAYMENT_CREATION_FAILED transactionId={} referenceId={} provider={} stage=PROVIDER_REQUEST errorCode={} error={}",
                    transaction.getTransactionId(), request.getReferenceId(), resolvedHost.gateway(),
                    e.getErrorCode().getCode(), e.getMessage());
            throw e;
        }

        try {
            transaction = updateTransactionOutcome(transaction, providerResponse);
        } catch (PaymentEngineException e) {
            log.error("event=PAYMENT_CREATION_FAILED transactionId={} referenceId={} provider={} providerPaymentId={} stage=PAYMENT_UPDATED_DB errorCode={} error={}",
                    transaction.getTransactionId(), request.getReferenceId(), resolvedHost.gateway(),
                    providerResponse.getPspRefNo(), e.getErrorCode().getCode(), e.getMessage());
            throw e;
        }

        log.info("event=PAYMENT_CREATED transactionId={} referenceId={} provider={} paymentStatus={} providerStatus={} providerPaymentId={} result=SUCCESS durationMs={}",
                transaction.getTransactionId(), request.getReferenceId(), resolvedHost.gateway(),
                transaction.getTransactionStatus(), providerResponse.getProviderStatus(), transaction.getPspRefNo(),
                System.currentTimeMillis() - flowStart);

        return toResponse(transaction, request, providerResponse);
    }

    private Merchant resolveMerchant(CreatePaymentRequest request) {
        Merchant merchant = merchantRepository.findByMasterMid(request.getMasterMid())
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.MERCHANT_NOT_FOUND,
                        "No merchant found for master_mid: " + request.getMasterMid()));
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new PaymentEngineException(ErrorCode.MERCHANT_INACTIVE,
                    "Merchant " + request.getMasterMid() + " is not active");
        }
        if (!Objects.equals(merchant.getGlobalAccountId(), request.getGlobalAccountId())) {
            throw new PaymentEngineException(ErrorCode.MERCHANT_NOT_FOUND,
                    "global_account_id does not match master_mid " + request.getMasterMid());
        }
        return merchant;
    }

    private SubMerchant resolveSubMerchant(String subMerchantMid) {
        if (subMerchantMid == null || subMerchantMid.isBlank()) {
            return null;
        }
        return subMerchantRepository.findBySubMerchantMid(subMerchantMid)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.SUB_MERCHANT_NOT_FOUND,
                        "No sub-merchant found for sub_merchant_mid: " + subMerchantMid));
    }

    /**
     * Idempotency + duplicate reference guard. If a transaction already
     * exists for this merchant + idempotency key, the caller is retrying
     * the exact same request - return the original result rather than
     * creating a second transaction or calling the provider again.
     */
    private Optional<PaymentTransaction> checkIdempotency(Merchant merchant, CreatePaymentRequest request) {
        Optional<PaymentTransaction> byIdempotencyKey = paymentTransactionRepository
                .findByMerchantFkAndGatewayIdempotencyKey(merchant.getMerchantId(), request.getIdempotencyKey());

        if (byIdempotencyKey.isPresent()) {
            PaymentTransaction existing = byIdempotencyKey.get();
            if (!Objects.equals(existing.getMerchantRefNo(), request.getReferenceId())) {
                throw new PaymentEngineException(ErrorCode.DUPLICATE_IDEMPOTENCY_KEY,
                        "idempotency key " + request.getIdempotencyKey()
                                + " was already used with a different reference_id");
            }
            return byIdempotencyKey;
        }

        Optional<PaymentTransaction> byReferenceId = paymentTransactionRepository
                .findByMerchantFkAndMerchantRefNo(merchant.getMerchantId(), request.getReferenceId());
        if (byReferenceId.isPresent()) {
            throw new PaymentEngineException(ErrorCode.DUPLICATE_REFERENCE_ID,
                    "reference_id " + request.getReferenceId() + " already exists for this merchant");
        }

        return Optional.empty();
    }

    private PaymentTransaction persistInitialTransaction(CreatePaymentRequest request, Merchant merchant,
                                                           SubMerchant subMerchant, ResolvedHost resolvedHost,
                                                           String transactionId) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(transactionId)
                .merchantRefNo(request.getReferenceId())
                .gatewayIdempotencyKey(request.getIdempotencyKey())
                .transactionStatus(TransactionStatus.INITIATED)
                .providerConfigurationFk(resolvedHost.providerConfiguration().getProviderConfigurationId())
                .merchantFk(merchant.getMerchantId())
                .subMerchantFk(subMerchant != null ? subMerchant.getSubMerchantId() : null)
                .paymentMethodFk(resolvedHost.paymentMethod().getPaymentMethodId())
                .transactionAmount(request.getAmount())
                .currencyCode(request.getCurrency())
                .build();

        long start = System.currentTimeMillis();
        try {
            transaction = paymentTransactionRepository.save(transaction);
            // CREATED_TIMESTAMP/UPDATED_TIMESTAMP are DB-generated defaults (insertable=false) -
            // re-read so the response can report the actual DB-assigned value rather than null.
            transaction = paymentTransactionRepository.findById(transaction.getPaymentTransactionId()).orElse(transaction);
            DbPersistLog.log(log, "PAYMENT_TRANSACTION", transactionId, System.currentTimeMillis() - start);
            return transaction;
        } catch (Exception e) {
            DbPersistLog.logError(log, "PAYMENT_TRANSACTION", transactionId, System.currentTimeMillis() - start, e);
            throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist payment transaction", e);
        }
    }

    private void persistQrDetails(CreatePaymentRequest request, PaymentTransaction transaction) {
        CustomerInfo customer = request.getCustomer();

        QrTransactionDetails details = QrTransactionDetails.builder()
                .paymentTransactionFk(transaction.getPaymentTransactionId())
                .qrType(QrType.DYNAMIC)
                .qrMode(QrMode.MERCHANT_PRESENTED)
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .metadata(JsonUtils.toJson(request.getMetadata()))
                .build();

        long start = System.currentTimeMillis();
        try {
            qrTransactionDetailsRepository.save(details);
            DbPersistLog.log(log, "QR_TRANSACTION_DETAILS", transaction.getTransactionId(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            DbPersistLog.logError(log, "QR_TRANSACTION_DETAILS", transaction.getTransactionId(),
                    System.currentTimeMillis() - start, e);
            throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist QR transaction details", e);
        }
    }

    private PaymentResponse callProvider(CreatePaymentRequest request, ResolvedHost resolvedHost,
                                          PaymentBrand brand, PaymentTransaction transaction) {
        PaymentRequest providerRequest = PaymentRequest.builder()
                .merchantId(transaction.getMerchantFk())
                .subMerchantId(transaction.getSubMerchantFk())
                .category(brand.getCategory())
                .brand(brand)
                .provider(resolvedHost.gateway())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .merchantRefNo(request.getReferenceId())
                .description(request.getDescription())
                .customerDetails(toCustomerDetails(request.getCustomer()))
                .additionalData(toProviderAdditionalData(request))
                .build();

        PaymentProviderAdapter adapter = adapterRegistry.getAdapter(resolvedHost.gateway());
        return adapter.initiatePayment(providerRequest);
    }

    private CustomerDetails toCustomerDetails(CustomerInfo customer) {
        if (customer == null) {
            return null;
        }
        return CustomerDetails.builder()
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .build();
    }

    private Map<String, Object> toProviderAdditionalData(CreatePaymentRequest request) {
        Map<String, Object> additionalData = new LinkedHashMap<>();
        additionalData.put("description", request.getDescription());
        additionalData.put("metadata", request.getMetadata());
        return additionalData;
    }

    private PaymentTransaction updateTransactionOutcome(PaymentTransaction transaction, PaymentResponse providerResponse) {
        transaction.setTransactionStatus(providerResponse.getStatus() != null ? providerResponse.getStatus() : TransactionStatus.PENDING);
        transaction.setPspRefNo(providerResponse.getPspRefNo());

        long start = System.currentTimeMillis();
        try {
            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            DbPersistLog.log(log, "PAYMENT_TRANSACTION_UPDATE", transaction.getTransactionId(), System.currentTimeMillis() - start);
            return saved;
        } catch (Exception e) {
            DbPersistLog.logError(log, "PAYMENT_TRANSACTION_UPDATE", transaction.getTransactionId(),
                    System.currentTimeMillis() - start, e);
            throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist payment transaction outcome", e);
        }
    }

    private void markTransactionFailed(PaymentTransaction transaction, PaymentEngineException cause) {
        transaction.setTransactionStatus(TransactionStatus.FAILED);
        long start = System.currentTimeMillis();
        try {
            paymentTransactionRepository.save(transaction);
            DbPersistLog.log(log, "PAYMENT_TRANSACTION_UPDATE_FAILED", transaction.getTransactionId(),
                    System.currentTimeMillis() - start);
            log.warn("event=PAYMENT_MARKED_FAILED transactionId={} paymentStatus={} result=SUCCESS failureCode={} reason={}",
                    transaction.getTransactionId(), transaction.getTransactionStatus(),
                    cause.getErrorCode().getCode(), cause.getMessage());
        } catch (Exception markError) {
            DbPersistLog.logError(log, "PAYMENT_TRANSACTION_UPDATE_FAILED", transaction.getTransactionId(),
                    System.currentTimeMillis() - start, markError);
            log.error("event=PAYMENT_MARKED_FAILED transactionId={} paymentStatus={} result=FAILED originalFailureCode={} markFailure={}",
                    transaction.getTransactionId(), transaction.getTransactionStatus(),
                    cause.getErrorCode().getCode(), markError.getMessage(), markError);
        }
    }

    private CreatePaymentResponse toResponse(PaymentTransaction transaction, CreatePaymentRequest request,
                                              PaymentResponse providerResponse) {
        return CreatePaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .referenceId(transaction.getMerchantRefNo())
                .globalAccountId(request.getGlobalAccountId())
                .status(transaction.getTransactionStatus())
                .amount(transaction.getTransactionAmount())
                .currency(transaction.getCurrencyCode())
                .paymentMethod(request.getPaymentMethod())
                .qrString(providerResponse != null ? providerResponse.getQrString() : null)
                .paymentUrl(providerResponse != null ? providerResponse.getPaymentUrl() : null)
                .createdAt(transaction.getCreatedTimestamp())
                .build();
    }
}
