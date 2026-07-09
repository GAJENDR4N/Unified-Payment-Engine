package io.gomobi.payment.service;

import io.gomobi.payment.adapter.pricing.PricingEngineClient;
import io.gomobi.payment.adapter.pricing.dto.PricingCalculationRequestDto;
import io.gomobi.payment.dto.PricingNotificationRequest;
import io.gomobi.payment.util.AmountMinorUnits;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PricingEngineNotificationService {

    private final PricingEngineClient pricingEngineClient;
    private final boolean pricingEngineEnabled;
    private final String defaultRegion;

    public PricingEngineNotificationService(PricingEngineClient pricingEngineClient,
                                            @Value("${payment.pricing-engine.enabled:true}") boolean pricingEngineEnabled,
                                            @Value("${payment.pricing-engine.default-region:VN}") String defaultRegion) {
        this.pricingEngineClient = pricingEngineClient;
        this.pricingEngineEnabled = pricingEngineEnabled;
        this.defaultRegion = defaultRegion;
    }

    @Async
    public void notifyPaymentSuccess(PricingNotificationRequest request) {
        if (!pricingEngineEnabled) {
            return;
        }

        if (request == null) {
            return;
        }

        if (request.getPaymentCode() == null || request.getPaymentCode().isBlank()) {
            log.warn("event=PRICING_ENGINE_NOTIFY_SKIPPED transactionId={} reason=PAYMENT_CODE_MISSING",
                    request.getTransactionId());
            return;
        }

        if (request.getTransactionAmountMinor() == null) {
            log.warn("event=PRICING_ENGINE_NOTIFY_SKIPPED transactionId={} reason=TRANSACTION_AMOUNT_MISSING",
                    request.getTransactionId());
            return;
        }

        PricingCalculationRequestDto payload = PricingCalculationRequestDto.builder()
                .transactionId(request.getTransactionId())
                .region(resolveRegion(request.getRegionCode()))
                .paymentCode(request.getPaymentCode())
                .txnAmount(AmountMinorUnits.toMajor(request.getTransactionAmountMinor().longValueExact()))
                .currencyCode(request.getCurrencyCode())
                .build();

        try {
            pricingEngineClient.submitPricingCalculation(payload);
            log.info("event=PRICING_ENGINE_NOTIFY_SUCCESS transactionId={} paymentCode={} region={} amount={} currency={}",
                    payload.getTransactionId(), payload.getPaymentCode(), payload.getRegion(),
                    payload.getTxnAmount(), payload.getCurrencyCode());
        } catch (Exception error) {
            log.error("event=PRICING_ENGINE_NOTIFY_FAILED transactionId={} reason={}",
                    request.getTransactionId(), error.getMessage(), error);
        }
    }

    private String resolveRegion(String regionFromProviderConfig) {
        if (regionFromProviderConfig == null || regionFromProviderConfig.isBlank()) {
            return defaultRegion;
        }
        return regionFromProviderConfig;
    }
}

