package io.gomobi.payment.dto;

import io.gomobi.payment.entity.PaymentTransaction;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

@Value
@Builder
public class PricingNotificationRequest {

    String transactionId;
    String paymentCode;
    String regionCode;
    BigInteger transactionAmountMinor;
    String currencyCode;

    public static PricingNotificationRequest fromTransaction(PaymentTransaction transaction,
                                                             String paymentCode,
                                                             String regionCode) {
        return PricingNotificationRequest.builder()
                .transactionId(transaction.getTransactionId())
                .paymentCode(paymentCode)
                .regionCode(regionCode)
                .transactionAmountMinor(transaction.getTransactionAmount())
                .currencyCode(transaction.getCurrencyCode())
                .build();
    }
}

