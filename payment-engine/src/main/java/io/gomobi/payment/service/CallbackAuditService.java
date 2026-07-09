package io.gomobi.payment.service;

import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.QrCallbackAudit;
import io.gomobi.payment.repository.QrCallbackAuditRepository;
import io.gomobi.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CallbackAuditService {

    private final QrCallbackAuditRepository qrCallbackAuditRepository;

    public void saveAudit(PaymentTransaction paymentTransaction,
                          CallbackType callbackType,
                          Map<String, String> requestHeaders,
                          String requestBody,
                          boolean processedFlag,
                          ProcessResult processResult,
                          int httpStatus,
                          int retryCount) {
        QrCallbackAudit callbackAudit = QrCallbackAudit.builder()
                .paymentTransaction(paymentTransaction)
                .callbackType(callbackType)
                .requestHeaders(JsonUtils.toJson(requestHeaders))
                .requestBody(requestBody)
                .processedFlag(processedFlag)
                .processResult(processResult)
                .httpStatus(httpStatus)
                .retryCount(retryCount)
                .build();
        qrCallbackAuditRepository.save(callbackAudit);
    }
}

