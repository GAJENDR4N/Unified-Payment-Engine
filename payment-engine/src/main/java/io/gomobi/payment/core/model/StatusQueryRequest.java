package io.gomobi.payment.core.model;

import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusQueryRequest {
    private String transactionId;
    private String merchantRefNo;
    private PaymentBrand brand;
    private PaymentProvider provider;
    private Long merchantId;
}