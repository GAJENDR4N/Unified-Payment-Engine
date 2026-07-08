package io.gomobi.payment.service;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.core.enums.MerchantCategory;
import io.gomobi.payment.core.enums.MerchantStatus;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.CustomerDetails;
import io.gomobi.payment.core.model.PaymentRequest;
import io.gomobi.payment.core.model.PaymentResponse;
import io.gomobi.payment.dto.CreatePaymentRequest;
import io.gomobi.payment.dto.CreatePaymentResponse;
import io.gomobi.payment.dto.CustomerInfo;
import io.gomobi.payment.dto.PaymentMethodInfo;
import io.gomobi.payment.entity.Merchant;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.QrTransactionDetails;
import io.gomobi.payment.entity.SubMerchant;
import io.gomobi.payment.exception.DuplicateReferenceException;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.registry.PaymentAdapterRegistry;
import io.gomobi.payment.repository.MerchantRepository;
import io.gomobi.payment.repository.PaymentMethodRepository;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import io.gomobi.payment.repository.QrTransactionDetailsRepository;
import io.gomobi.payment.repository.SubMerchantRepository;
import io.gomobi.payment.util.DbPersistLog;
import io.gomobi.payment.util.AmountMinorUnits;
import io.gomobi.payment.util.PaymentMethodBrandMapper;
import io.gomobi.payment.util.TransactionIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates payment creation end to end:
 * resolve merchant/sub-merchant -> idempotency check -> resolve host/gateway
 * -> persist INITIATED state (atomically, via {@link PaymentInitialStatePersister})
 * -> call the provider -> persist the outcome.
 * <p>
 * The provider HTTP call deliberately happens outside of any DB transaction
 * (see {@link PaymentInitialStatePersister}'s javadoc) - a slow/failed
 * external call should never hold a DB connection open.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final MerchantRepository merchantRepository;
    private final SubMerchantRepository subMerchantRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final QrTransactionDetailsRepository qrTransactionDetailsRepository;
    private final HostResolutionService hostResolutionService;
    private final PaymentAdapterRegistry adapterRegistry;
    private final TransactionIdGenerator transactionIdGenerator;
    private final PaymentInitialStatePersister paymentInitialStatePersister;

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        long flowStart = System.currentTimeMillis();

        Merchant merchant = resolveMerchant(request);
        SubMerchant subMerchant = resolveSubMerchant(merchant, request.getSubMerchantMid());

        ResolvedHost resolvedHost = hostResolutionService.resolve(request.getPaymentMethod().getChannelCode());
        PaymentBrand brand = PaymentMethodBrandMapper.toBrand(resolvedHost.paymentMethod().getPaymentMethodCode());
        String transactionId = transactionIdGenerator.generate();

        // Persist PAYMENT_TRANSACTION and QR_TRANSACTION_DETAILS before provider call
        // via PaymentInitialStatePersister (separate transactional bean).
        PaymentTransaction transaction;
        try {
            transaction = paymentInitialStatePersister.persist(request, merchant, subMerchant, resolvedHost, transactionId);
        } catch (PaymentEngineException e) {
            if (isDuplicateMerchantReferenceViolation(e)) {
                throw buildDuplicateReferenceException(merchant.getMerchantId(), request.getReferenceId(), e);
            }
            throw e;
        }

        PaymentResponse providerResponse = callProvider(request, resolvedHost, brand, transaction);
        transaction = applyOutcome(transaction, providerResponse);

        log.info("event=PAYMENT_CREATED transactionId={} referenceId={} provider={} paymentStatus={} "
                        + "providerStatus={} providerPaymentId={} result=SUCCESS durationMs={}",
                transaction.getTransactionId(), request.getReferenceId(), resolvedHost.gateway(),
                transaction.getTransactionStatus(), providerResponse.getProviderStatus(), transaction.getPspRefNo(),
                System.currentTimeMillis() - flowStart);

        return toResponse(transaction, request, providerResponse);
    }

    // ---- resolution -----------------------------------------------------

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

    private SubMerchant resolveSubMerchant(Merchant merchant, String subMerchantMid) {

        //validate sub-mid based on the merchant.getMerchantCategory()
        if (merchant.getMerchantCategory().equals(MerchantCategory.PGE) && (subMerchantMid == null || subMerchantMid.isBlank())) {
            throw new PaymentEngineException(ErrorCode.SUB_MERCHANT_REQUIRED,
                    "sub_merchant_mid is required for merchant category PGE");

        }

        if (subMerchantMid == null || subMerchantMid.isBlank()) {
            return null;
        }

        SubMerchant subMerchant = subMerchantRepository.findBySubMerchantMid(subMerchantMid)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.SUB_MERCHANT_NOT_FOUND,
                        "No sub-merchant found for sub_merchant_mid: " + subMerchantMid));
        if (!Objects.equals(subMerchant.getMerchantFk(), merchant.getMerchantId())) {
            throw new PaymentEngineException(ErrorCode.SUB_MERCHANT_MISMATCH,
                    "sub_merchant_mid " + subMerchantMid + " does not belong to master_mid " + merchant.getMasterMid());
        }
        return subMerchant;
    }

    // ---- provider call ----------------------------------------------------

    private PaymentResponse callProvider(CreatePaymentRequest request, ResolvedHost resolvedHost,
                                         PaymentBrand brand, PaymentTransaction transaction) {
        PaymentRequest providerRequest = PaymentRequest.builder()
                .merchantId(transaction.getMerchantFk())
                .subMerchantId(transaction.getSubMerchantFk())
                .category(brand.getCategory())
                .brand(brand)
                .provider(resolvedHost.gateway())
                .amount(AmountMinorUnits.toMajor(request.getAmount()))
                .currency(request.getCurrency())
                .merchantRefNo(request.getReferenceId())
                .description(request.getDescription())
                .customerDetails(toCustomerDetails(request.getCustomer()))
                .additionalData(request.getMetadata() != null ? Map.copyOf(request.getMetadata()) : Map.of())
                .build();

        PaymentProviderAdapter adapter = adapterRegistry.getAdapter(resolvedHost.gateway());
        try {
            return adapter.initiatePayment(providerRequest);
        } catch (PaymentEngineException e) {
            markTransactionFailed(transaction, e);
            log.error("event=PAYMENT_CREATION_FAILED transactionId={} referenceId={} provider={} stage=PROVIDER_REQUEST "
                            + "errorCode={} error={}",
                    transaction.getTransactionId(), request.getReferenceId(), resolvedHost.gateway(),
                    e.getErrorCode().getCode(), e.getMessage());
            throw e;
        }
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

    // ---- outcome persistence ------------------------------------------

    /**
     * Persists the transaction status/PSP reference from the provider response,
     * and - when present - the QR payload, so it can be handed back on a status
     * enquiry or idempotent replay without calling the provider again.
     */
    private PaymentTransaction applyOutcome(PaymentTransaction transaction, PaymentResponse providerResponse) {
        transaction.setTransactionStatus(providerResponse.getStatus() != null ? providerResponse.getStatus() : TransactionStatus.PENDING);
        transaction.setPspRefNo(providerResponse.getPspRefNo());

        long start = System.currentTimeMillis();
        try {
            PaymentTransaction saved = paymentTransactionRepository.save(transaction);
            persistQrStringIfPresent(transaction.getPaymentTransactionId(), providerResponse.getQrString());
            DbPersistLog.log(log, "PAYMENT_TRANSACTION_UPDATE", transaction.getTransactionId(), System.currentTimeMillis() - start);
            return saved;
        } catch (Exception e) {
            DbPersistLog.logError(log, "PAYMENT_TRANSACTION_UPDATE", transaction.getTransactionId(),
                    System.currentTimeMillis() - start, e);
            log.error("event=PAYMENT_CREATION_FAILED transactionId={} referenceId={} providerPaymentId={} "
                            + "stage=PAYMENT_UPDATED_DB errorCode={} error={}",
                    transaction.getTransactionId(), transaction.getMerchantRefNo(), providerResponse.getPspRefNo(),
                    ErrorCode.DB_PERSIST_ERROR.getCode(), e.getMessage());
            throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist payment transaction outcome", e);
        }
    }

    private void persistQrStringIfPresent(Long paymentTransactionId, String qrString) {
        if (qrString == null || qrString.isBlank()) {
            return;
        }
        qrTransactionDetailsRepository.findByPaymentTransactionFk(paymentTransactionId).ifPresent(details -> {
            details.setQrImageUrl(qrString);
            qrTransactionDetailsRepository.save(details);
        });
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
            log.error("event=PAYMENT_MARKED_FAILED transactionId={} paymentStatus={} result=FAILED "
                            + "originalFailureCode={} markFailure={}",
                    transaction.getTransactionId(), transaction.getTransactionStatus(),
                    cause.getErrorCode().getCode(), markError.getMessage(), markError);
        }
    }

    private CreatePaymentResponse toResponse(PaymentTransaction transaction, CreatePaymentRequest request,
                                             PaymentResponse providerResponse) {
        return CreatePaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .referenceId(transaction.getMerchantRefNo())
                .status(transaction.getTransactionStatus())
                .amount(transaction.getTransactionAmount().longValueExact())
                .currency(transaction.getCurrencyCode())
                .paymentMethod(request.getPaymentMethod())
                .customer(request.getCustomer())
                .qrString(providerResponse != null ? providerResponse.getQrString() : null)
                .paymentUrl(providerResponse != null ? providerResponse.getPaymentUrl() : null)
                .createdAt(transaction.getCreatedTimestamp())
                .build();
    }

    private boolean isDuplicateMerchantReferenceViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException) {
                String detail = buildErrorText(current);
                return detail.contains("UK_MERCHANT_REF")
                        || (detail.contains("DUPLICATE ENTRY") && detail.contains("MERCHANT_REF"));
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildErrorText(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        if (throwable.getMessage() != null) {
            builder.append(throwable.getMessage());
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null) {
            builder.append(' ').append(cause.getMessage());
        }
        return builder.toString().toUpperCase();
    }

    private DuplicateReferenceException buildDuplicateReferenceException(Long merchantId,
                                                                         String referenceId,
                                                                         Throwable cause) {
        PaymentTransaction existingTransaction = paymentTransactionRepository
                .findByMerchantFkAndMerchantRefNo(merchantId, referenceId)
                .orElseThrow(() -> new PaymentEngineException(
                        ErrorCode.DUPLICATE_REFERENCE_ID,
                        "Reference " + referenceId + " already exists, but existing transaction details could not be loaded",
                        cause));

        PaymentMethodInfo paymentMethod = paymentMethodRepository.findById(existingTransaction.getPaymentMethodFk())
                .map(this::toPaymentMethodInfo)
                .orElse(null);

        CustomerInfo customer = qrTransactionDetailsRepository
                .findByPaymentTransactionFk(existingTransaction.getPaymentTransactionId())
                .map(this::toCustomerInfo)
                .orElse(null);

        CreatePaymentResponse existingPayment = CreatePaymentResponse.builder()
                .transactionId(existingTransaction.getTransactionId())
                .referenceId(existingTransaction.getMerchantRefNo())
                .status(existingTransaction.getTransactionStatus())
                .amount(existingTransaction.getTransactionAmount().longValueExact())
                .currency(existingTransaction.getCurrencyCode())
                .paymentMethod(paymentMethod)
                .customer(customer)
                .createdAt(existingTransaction.getCreatedTimestamp())
                .build();

        String detail = "For reference_id " + referenceId
                + " you have already initiated another transaction";
        return new DuplicateReferenceException(detail, existingPayment, cause);
    }

    private PaymentMethodInfo toPaymentMethodInfo(PaymentMethod paymentMethod) {
        return PaymentMethodInfo.builder()
                .type(paymentMethod.getPaymentMethodType())
                .channelCode(paymentMethod.getChannelCode())
                .build();
    }

    private CustomerInfo toCustomerInfo(QrTransactionDetails details) {
        return CustomerInfo.builder()
                .id(details.getCustomerId())
                .name(details.getCustomerName())
                .email(details.getCustomerEmail())
                .phone(details.getCustomerPhone())
                .build();
    }
}