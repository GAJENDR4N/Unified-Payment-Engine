package io.gomobi.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gomobi.payment.adapter.helloclever.HelloCleverStatusMapper;
import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.dto.PricingNotificationRequest;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.ProviderConfiguration;
import io.gomobi.payment.entity.QrCallbackAudit;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import io.gomobi.payment.repository.QrCallbackAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelloCleverWebhookServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private ReferenceDataCacheService referenceDataCacheService;

    @Mock
    private QrCallbackAuditRepository qrCallbackAuditRepository;

    @Mock
    private HelloCleverStatusMapper helloCleverStatusMapper;

    @Mock
    private PricingEngineNotificationService pricingEngineNotificationService;

    private HelloCleverWebhookService helloCleverWebhookService;

    @BeforeEach
    void setUp() {
        CallbackAuditService callbackAuditService = new CallbackAuditService(qrCallbackAuditRepository);
        helloCleverWebhookService = new HelloCleverWebhookService(
                new ObjectMapper(),
                paymentTransactionRepository,
                referenceDataCacheService,
                helloCleverStatusMapper,
                pricingEngineNotificationService,
                callbackAuditService
        );
    }

    @Test
    void processWebhook_updatesTransactionAndPersistsAudit() {
        String body = "{\"uuid\":\"pi_123\",\"external_id\":\"ORDER-1\",\"status\":\"received\"}";

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .id(BigInteger.valueOf(10))
                .paymentMethodCode("VIETQR")
                .build();

        ProviderConfiguration providerConfiguration = ProviderConfiguration.builder()
                .id(BigInteger.valueOf(20))
                .regionCode("VN")
                .build();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(BigInteger.valueOf(88))
                .transactionId("txn_abc")
                .transactionStatus(TransactionStatus.PENDING)
                .paymentMethod(paymentMethod)
                .providerConfiguration(providerConfiguration)
                .currencyCode("VND")
                .transactionAmount(java.math.BigInteger.valueOf(150075))
                .pspRefNo("pi_123")
                .build();

        when(paymentTransactionRepository.findTopByPspRefNoOrderByIdDesc("pi_123"))
                .thenReturn(Optional.of(transaction));
        when(referenceDataCacheService.findPaymentMethodById(BigInteger.valueOf(10))).thenReturn(Optional.of(paymentMethod));
        when(referenceDataCacheService.findProviderConfigurationById(BigInteger.valueOf(20))).thenReturn(Optional.of(providerConfiguration));
        when(helloCleverStatusMapper.mapStatus("received"))
                .thenReturn(TransactionStatus.SUCCESS);
        when(qrCallbackAuditRepository.save(any(QrCallbackAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        helloCleverWebhookService.processWebhook(body, Map.of("x-signature", "abc"));

        ArgumentCaptor<PaymentTransaction> txnCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txnCaptor.capture());
        assertEquals(TransactionStatus.SUCCESS, txnCaptor.getValue().getTransactionStatus());
        assertNotNull(txnCaptor.getValue().getPaidTimestamp());

        ArgumentCaptor<QrCallbackAudit> auditCaptor = ArgumentCaptor.forClass(QrCallbackAudit.class);
        verify(qrCallbackAuditRepository).save(auditCaptor.capture());
        assertEquals(ProcessResult.SUCCESS, auditCaptor.getValue().getProcessResult());
        assertEquals(BigInteger.valueOf(88), auditCaptor.getValue().getPaymentTransaction().getId());
        assertEquals(CallbackType.PAYMENT_NOTIFICATION, auditCaptor.getValue().getCallbackType());

        ArgumentCaptor<PricingNotificationRequest> requestCaptor = ArgumentCaptor.forClass(PricingNotificationRequest.class);
        verify(pricingEngineNotificationService).notifyPaymentSuccess(requestCaptor.capture());
        assertEquals("txn_abc", requestCaptor.getValue().getTransactionId());
        assertEquals("VIETQR", requestCaptor.getValue().getPaymentCode());
        assertEquals("VN", requestCaptor.getValue().getRegionCode());
    }

    @Test
    void processWebhook_throwsWhenTransactionNotFound() {
        String body = "{\"uuid\":\"pi_404\",\"external_id\":\"ORDER-404\",\"status\":\"failed\"}";

        when(paymentTransactionRepository.findTopByPspRefNoOrderByIdDesc("pi_404"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findTopByMerchantRefNoOrderByIdDesc("ORDER-404"))
                .thenReturn(Optional.empty());

        assertThrows(PaymentEngineException.class, () -> helloCleverWebhookService.processWebhook(body, Map.of()));

        verify(paymentTransactionRepository, never()).save(any());
        verify(qrCallbackAuditRepository, never()).save(any());
        verify(pricingEngineNotificationService, never()).notifyPaymentSuccess(any());
    }

    @Test
    void processWebhook_throwsValidationErrorForInvalidPayload() {
        String invalidJson = "not-a-json";

        assertThrows(PaymentEngineException.class,
                () -> helloCleverWebhookService.processWebhook(invalidJson, Map.of("x-key", "1")));

        verify(qrCallbackAuditRepository, never()).save(any());
        verify(pricingEngineNotificationService, never()).notifyPaymentSuccess(any());
    }
}
