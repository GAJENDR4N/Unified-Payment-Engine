package io.gomobi.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gomobi.payment.adapter.helloclever.HelloCleverStatusMapper;
import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.QrCallbackAudit;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import io.gomobi.payment.repository.QrCallbackAuditRepository;
import io.gomobi.payment.util.JsonUtils;
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
    private final QrCallbackAuditRepository qrCallbackAuditRepository;
    private final HelloCleverStatusMapper helloCleverStatusMapper;

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
            boolean updated = applyTransactionUpdate(transaction, mappedStatus, pspRefNo);

            QrCallbackAudit callbackAudit = QrCallbackAudit.builder()
                    .paymentTransactionFk(transaction.getPaymentTransactionId())
                    .callbackType(CallbackType.PAYMENT_NOTIFICATION)
                    .requestHeaders(JsonUtils.toJson(headers))
                    .requestBody(requestBody)
                    .processedFlag(true)
                    .processResult(updated ? ProcessResult.SUCCESS : ProcessResult.IGNORED)
                    .httpStatus(200)
                    .retryCount(0)
                    .build();
            qrCallbackAuditRepository.save(callbackAudit);

            log.info("event=HELLO_CLEVER_WEBHOOK_PROCESSED transactionId={} providerStatus={} mappedStatus={} result={}",
                    transaction.getTransactionId(), providerStatus, mappedStatus, callbackAudit.getProcessResult());
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
                    .findTopByPspRefNoOrderByPaymentTransactionIdDesc(pspRefNo);
            if (byPspRef.isPresent()) {
                return byPspRef;
            }
        }

        if (merchantRefNo != null && !merchantRefNo.isBlank()) {
            return paymentTransactionRepository.findTopByMerchantRefNoOrderByPaymentTransactionIdDesc(merchantRefNo);
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

    private String textValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }
}

