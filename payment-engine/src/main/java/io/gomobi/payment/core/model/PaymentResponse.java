package io.gomobi.payment.core.model;

import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String transactionId;
    private String merchantRefNo;
    private String pspRefNo;
    private PaymentBrand brand;
    private PaymentProvider provider;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;

    /** Where to redirect / render for the customer to complete payment (QR image URL, redirect URL, VA number, etc). */
    private String paymentUrl;
    private String qrString;

    private String rawProviderResponse;
}