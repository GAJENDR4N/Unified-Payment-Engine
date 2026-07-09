package io.gomobi.payment.service;

import io.gomobi.payment.adapter.pricing.PricingEngineClient;
import io.gomobi.payment.adapter.pricing.dto.PricingCalculationRequestDto;
import io.gomobi.payment.dto.PricingNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PricingEngineNotificationServiceTest {

    @Mock
    private PricingEngineClient pricingEngineClient;

    private PricingEngineNotificationService pricingEngineNotificationService;

    @BeforeEach
    void setUp() {
        pricingEngineNotificationService = new PricingEngineNotificationService(pricingEngineClient, true, "VN");
    }

    @Test
    void notifyPaymentSuccess_sendsPricingRequestWithMappedPayload() {
        PricingNotificationRequest request = PricingNotificationRequest.builder()
                .transactionId("TXN202607080001")
                .paymentCode("VIETQR")
                .regionCode("VN")
                .transactionAmountMinor(BigInteger.valueOf(150075))
                .currencyCode("VND")
                .build();

        pricingEngineNotificationService.notifyPaymentSuccess(request);

        ArgumentCaptor<PricingCalculationRequestDto> payloadCaptor = ArgumentCaptor.forClass(PricingCalculationRequestDto.class);
        verify(pricingEngineClient).submitPricingCalculation(payloadCaptor.capture());

        PricingCalculationRequestDto payload = payloadCaptor.getValue();
        assertEquals("TXN202607080001", payload.getTransactionId());
        assertEquals("VN", payload.getRegion());
        assertEquals("VIETQR", payload.getPaymentCode());
        assertEquals(new BigDecimal("1500.75"), payload.getTxnAmount());
        assertEquals("VND", payload.getCurrencyCode());
    }

    @Test
    void notifyPaymentSuccess_skipsWhenPaymentCodeIsMissing() {
        PricingNotificationRequest request = PricingNotificationRequest.builder()
                .transactionId("TXN_PENDING")
                .transactionAmountMinor(BigInteger.valueOf(150075))
                .currencyCode("VND")
                .build();

        pricingEngineNotificationService.notifyPaymentSuccess(request);

        verify(pricingEngineClient, never()).submitPricingCalculation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void notifyPaymentSuccess_usesDefaultRegionWhenMissing() {
        PricingNotificationRequest request = PricingNotificationRequest.builder()
                .transactionId("TXN_NO_REGION")
                .paymentCode("VIETQR")
                .transactionAmountMinor(BigInteger.valueOf(150075))
                .currencyCode("VND")
                .build();

        pricingEngineNotificationService.notifyPaymentSuccess(request);

        ArgumentCaptor<PricingCalculationRequestDto> payloadCaptor = ArgumentCaptor.forClass(PricingCalculationRequestDto.class);
        verify(pricingEngineClient).submitPricingCalculation(payloadCaptor.capture());
        assertEquals("VN", payloadCaptor.getValue().getRegion());
    }
}

