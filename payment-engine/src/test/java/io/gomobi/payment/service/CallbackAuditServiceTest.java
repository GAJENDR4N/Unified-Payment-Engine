package io.gomobi.payment.service;

import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.QrCallbackAudit;
import io.gomobi.payment.repository.QrCallbackAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackAuditServiceTest {

    @Mock
    private QrCallbackAuditRepository qrCallbackAuditRepository;

    @InjectMocks
    private CallbackAuditService callbackAuditService;

    @Test
    void save_buildsAndPersistsCallbackAudit() {
        when(qrCallbackAuditRepository.save(any(QrCallbackAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .id(BigInteger.valueOf(88))
                .build();

        callbackAuditService.saveAudit(
                paymentTransaction,
                CallbackType.PAYMENT_NOTIFICATION,
                Map.of("x-signature", "abc"),
                "{\"status\":\"received\"}",
                true,
                ProcessResult.SUCCESS,
                200,
                0
        );

        ArgumentCaptor<QrCallbackAudit> auditCaptor = ArgumentCaptor.forClass(QrCallbackAudit.class);
        verify(qrCallbackAuditRepository).save(auditCaptor.capture());

        QrCallbackAudit audit = auditCaptor.getValue();
        assertEquals(BigInteger.valueOf(88), audit.getPaymentTransaction().getId());
        assertEquals(CallbackType.PAYMENT_NOTIFICATION, audit.getCallbackType());
        assertEquals("{\"x-signature\":\"abc\"}", audit.getRequestHeaders());
        assertEquals("{\"status\":\"received\"}", audit.getRequestBody());
        assertEquals(Boolean.TRUE, audit.getProcessedFlag());
        assertEquals(ProcessResult.SUCCESS, audit.getProcessResult());
        assertEquals(200, audit.getHttpStatus());
        assertEquals(0, audit.getRetryCount());
    }
}

