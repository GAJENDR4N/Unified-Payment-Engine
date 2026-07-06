package io.gomobi.payment.adapter.fiuu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Outbound request shape for FIUU's payment initiation endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiuuPaymentRequestDto {
    private String merchantId;
    private String amount;
    private String orderId;
    private String currency;
    private String billName;
    private String billEmail;
    private String billMobile;
    private String channel;     // maps from PaymentBrand -> FIUU's channel code
    private String returnUrl;
    private String notifyUrl;
    private String vsign;       // computed signature
}