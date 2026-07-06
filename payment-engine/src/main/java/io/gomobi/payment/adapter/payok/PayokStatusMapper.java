package io.gomobi.payment.adapter.payok;

import io.gomobi.payment.adapter.payok.dto.PayokPaymentResponseDto;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.PaymentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PayokStatusMapper {

    public PaymentResponse toPaymentResponse(PayokPaymentResponseDto dto, PaymentBrand brand, BigDecimal amount, String currency) {
        return PaymentResponse.builder()
                .transactionId(dto.getTransactionId())
                .merchantRefNo(dto.getOrderId())
                .brand(brand)
                .provider(PaymentProvider.PAYOK)
                .status(mapStatus(dto.getStatus()))
                .amount(amount)
                .currency(currency)
                .qrString(dto.getQrString())
                .rawProviderResponse(dto.getRawBody())
                .build();
    }

    /** TODO: replace with PAYOK's actual documented status codes once confirmed. */
    public TransactionStatus mapStatus(String payokStatus) {
        if (payokStatus == null) {
            return TransactionStatus.INITIATED;
        }
        return switch (payokStatus.toUpperCase()) {
            case "SUCCESS", "PAID" -> TransactionStatus.SUCCESS;
            case "FAILED" -> TransactionStatus.FAILED;
            case "PENDING" -> TransactionStatus.PENDING;
            default -> TransactionStatus.INITIATED;
        };
    }
}
