package io.gomobi.payment.service;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.StatusQueryRequest;
import io.gomobi.payment.core.model.StatusQueryResponse;
import io.gomobi.payment.dto.PricingNotificationRequest;
import io.gomobi.payment.dto.StatusEnquiryResponse;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.ProviderConfiguration;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.registry.PaymentAdapterRegistry;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import io.gomobi.payment.util.DbPersistLog;
import io.gomobi.payment.util.PaymentMethodBrandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * Status enquiry: looks up the persisted PAYMENT_TRANSACTION, asks the
 * original provider for its live status, and persists any change before
 * returning the latest known state to the caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReferenceDataCacheService referenceDataCacheService;
    private final PaymentAdapterRegistry adapterRegistry;
    private final HostResolutionService hostResolutionService;
    private final PricingEngineNotificationService pricingEngineNotificationService;

    public StatusEnquiryResponse getStatus(String transactionId) throws NoSuchAlgorithmException {
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "No transaction found for transaction_id: " + transactionId));

        BigInteger paymentMethodId = transaction.getPaymentMethod().getId();
        PaymentMethod paymentMethod = referenceDataCacheService.findPaymentMethodById(paymentMethodId)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.PAYMENT_METHOD_NOT_CONFIGURED,
                        "Payment method (id=" + paymentMethodId + ") no longer exists"));

        BigInteger providerConfigId = transaction.getProviderConfiguration().getId();
        ProviderConfiguration providerConfiguration = referenceDataCacheService
                .findProviderConfigurationById(providerConfigId)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.NO_ACTIVE_PROVIDER_FOR_METHOD,
                        "Provider (id=" + providerConfigId + ") no longer exists"));

        PaymentProvider gateway = hostResolutionService.resolveGateway(providerConfiguration.getProviderCode());
        PaymentBrand brand = PaymentMethodBrandMapper.toBrand(paymentMethod.getPaymentMethodCode());

        StatusQueryRequest statusQueryRequest = StatusQueryRequest.builder()
                .transactionId(transaction.getTransactionId())
                .merchantRefNo(transaction.getMerchantRefNo())
                .brand(brand)
                .provider(gateway)
                .merchantId(transaction.getMerchant().getId())
                .build();

        PaymentProviderAdapter adapter = adapterRegistry.getAdapter(gateway);
        StatusQueryResponse liveStatus = adapter.queryStatus(statusQueryRequest);

        if (liveStatus.getStatus() != null && liveStatus.getStatus() != transaction.getTransactionStatus()) {
            TransactionStatus previousStatus = transaction.getTransactionStatus();
            transaction.setTransactionStatus(liveStatus.getStatus());
            if (liveStatus.getPaidTimestamp() != null) {
                transaction.setPaidTimestamp(liveStatus.getPaidTimestamp());
            }
            if (liveStatus.getPspRefNo() != null) {
                transaction.setPspRefNo(liveStatus.getPspRefNo());
            }

            long start = System.currentTimeMillis();
            try {
                transaction = paymentTransactionRepository.save(transaction);
                DbPersistLog.log(log, "PAYMENT_TRANSACTION_STATUS_UPDATE", transactionId, System.currentTimeMillis() - start);
                if (previousStatus != TransactionStatus.SUCCESS && transaction.getTransactionStatus() == TransactionStatus.SUCCESS) {
                    pricingEngineNotificationService.notifyPaymentSuccess(PricingNotificationRequest.fromTransaction(
                            transaction,
                            paymentMethod.getPaymentMethodCode(),
                            providerConfiguration.getRegionCode()
                    ));
                }
            } catch (Exception e) {
                DbPersistLog.logError(log, "PAYMENT_TRANSACTION_STATUS_UPDATE", transactionId,
                        System.currentTimeMillis() - start, e);
                throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist updated transaction status", e);
            }
        }

        return StatusEnquiryResponse.builder()
                .transactionId(transaction.getTransactionId())
                .referenceId(transaction.getMerchantRefNo())
                .status(transaction.getTransactionStatus())
                .amount(transaction.getTransactionAmount())
                .currency(transaction.getCurrencyCode())
                .paidAt(transaction.getPaidTimestamp())
                .updatedAt(transaction.getUpdatedTimestamp())
                .build();
    }
}
