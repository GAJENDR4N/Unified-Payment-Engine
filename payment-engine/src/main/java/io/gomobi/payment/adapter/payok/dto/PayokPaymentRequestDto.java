package io.gomobi.payment.adapter.payok.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound request shape for PAYOK's payment initiation endpoint.
 * TODO: field names/shape are provisional - confirm against PAYOK's actual
 * VietQR API contract before enabling live traffic (see FiuuPaymentRequestDto
 * for the equivalent, confirmed FIUU shape).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayokPaymentRequestDto {
    private String merchantId;
    private String orderId;
    private String amount;
    private String currency;
    private String bankCode;      // receiving bank's paymentMethodCode for VietQR direct-transfer model
    private String description;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String notifyUrl;
    private String signature;
}
