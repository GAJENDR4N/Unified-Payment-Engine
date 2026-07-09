package io.gomobi.payment.adapter.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingCalculationRequestDto {

    private String transactionId;
    private String region;
    private String paymentCode;
    private BigDecimal txnAmount;
    private String currencyCode;
}

