package io.gomobi.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gomobi.payment.adapter.helloclever.HelloCleverStatusMapper;
import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.dto.PricingNotificationRequest;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.ProviderConfiguration;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Persists Hello Clever webhook payloads and applies status transitions to
 * PAYMENT_TRANSACTION when a matching transaction can be identified.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelloCleverWebhookService {

    private final ObjectMapper objectMapper;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReferenceDataCacheService referenceDataCacheService;
    private final HelloCleverStatusMapper helloCleverStatusMapper;
    private final PricingEngineNotificationService pricingEngineNotificationService;
    private final CallbackAuditService callbackAuditService;

    @Transactional
    public void processWebhook(String requestBody, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            String pspRefNo = textValue(root, "uuid");
            String merchantRefNo = textValue(root, "external_id");
            String providerStatus = textValue(root, "status");

            Optional<PaymentTransaction> transactionOptional = findTransaction(pspRefNo, merchantRefNo);
            if (transactionOptional.isEmpty()) {
                log.warn("event=HELLO_CLEVER_WEBHOOK_FAILED providerStatus={} pspRefNo={} merchantRefNo={} reason=TRANSACTION_NOT_FOUND",
                        providerStatus, pspRefNo, merchantRefNo);
                throw new PaymentEngineException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "No payment transaction found for Hello Clever webhook");
            }

            PaymentTransaction transaction = transactionOptional.get();

            TransactionStatus mappedStatus = helloCleverStatusMapper.mapStatus(providerStatus);
            boolean transitionedToSuccess = transaction.getTransactionStatus() != TransactionStatus.SUCCESS
                    && mappedStatus == TransactionStatus.SUCCESS;
            boolean updated = applyTransactionUpdate(transaction, mappedStatus, pspRefNo);

            if (updated && transitionedToSuccess) {
                buildPricingNotificationRequest(transaction)
                        .ifPresent(pricingEngineNotificationService::notifyPaymentSuccess);
            }

            ProcessResult processResult = updated ? ProcessResult.SUCCESS : ProcessResult.IGNORED;
            callbackAuditService.saveAudit(
                    transaction,
                    CallbackType.PAYMENT_NOTIFICATION,
                    headers,
                    requestBody,
                    true,
                    processResult,
                    200,
                    0
            );

            log.info("event=HELLO_CLEVER_WEBHOOK_PROCESSED transactionId={} providerStatus={} mappedStatus={} result={}",
                    transaction.getTransactionId(), providerStatus, mappedStatus, processResult);
        } catch (PaymentEngineException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentEngineException(ErrorCode.VALIDATION_ERROR,
                    "Invalid Hello Clever webhook payload", ex);
        }
    }

    private Optional<PaymentTransaction> findTransaction(String pspRefNo, String merchantRefNo) {
        if (pspRefNo != null && !pspRefNo.isBlank()) {
            Optional<PaymentTransaction> byPspRef = paymentTransactionRepository
                    .findTopByPspRefNoOrderByIdDesc(pspRefNo);
            if (byPspRef.isPresent()) {
                return byPspRef;
            }
        }

        if (merchantRefNo != null && !merchantRefNo.isBlank()) {
            return paymentTransactionRepository.findTopByMerchantRefNoOrderByIdDesc(merchantRefNo);
        }

        return Optional.empty();
    }

    private boolean applyTransactionUpdate(PaymentTransaction transaction,
                                           TransactionStatus mappedStatus,
                                           String pspRefNo) {
        boolean updated = false;

        if (mappedStatus != null && mappedStatus != transaction.getTransactionStatus()) {
            transaction.setTransactionStatus(mappedStatus);
            updated = true;
            if (mappedStatus == TransactionStatus.SUCCESS && transaction.getPaidTimestamp() == null) {
                transaction.setPaidTimestamp(LocalDateTime.now());
            }
        }

        if (pspRefNo != null && !pspRefNo.isBlank() && !pspRefNo.equals(transaction.getPspRefNo())) {
            transaction.setPspRefNo(pspRefNo);
            updated = true;
        }

        if (updated) {
            paymentTransactionRepository.save(transaction);
        }

        return updated;
    }

    private Optional<PricingNotificationRequest> buildPricingNotificationRequest(PaymentTransaction transaction) {
        Optional<PaymentMethod> paymentMethodOptional = referenceDataCacheService.findPaymentMethodById(transaction.getPaymentMethod().getId());
        if (paymentMethodOptional.isEmpty()) {
            log.warn("event=PRICING_ENGINE_NOTIFY_SKIPPED transactionId={} reason=PAYMENT_METHOD_NOT_FOUND paymentMethodId={}",
                    transaction.getTransactionId(), transaction.getPaymentMethod().getId());
            return Optional.empty();
        }

        Optional<ProviderConfiguration> providerConfigurationOptional = referenceDataCacheService
                .findProviderConfigurationById(transaction.getProviderConfiguration().getId());
        if (providerConfigurationOptional.isEmpty()) {
            log.warn("event=PRICING_ENGINE_NOTIFY_SKIPPED transactionId={} reason=PROVIDER_CONFIGURATION_NOT_FOUND providerConfigurationId={}",
                    transaction.getTransactionId(), transaction.getProviderConfiguration().getId());
            return Optional.empty();
        }

        PaymentMethod paymentMethod = paymentMethodOptional.get();
        ProviderConfiguration providerConfiguration = providerConfigurationOptional.get();
        return Optional.of(PricingNotificationRequest.fromTransaction(
                transaction,
                paymentMethod.getPaymentMethodCode(),
                providerConfiguration.getRegionCode()
        ));
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }
}
