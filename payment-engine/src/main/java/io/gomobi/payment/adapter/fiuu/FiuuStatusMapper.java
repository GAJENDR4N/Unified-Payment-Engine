package io.gomobi.payment.adapter.fiuu;

import io.gomobi.payment.adapter.fiuu.dto.FiuuPaymentResponseDto;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.PaymentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FiuuStatusMapper {

    public PaymentResponse toPaymentResponse(FiuuPaymentResponseDto dto, PaymentBrand brand, BigDecimal amount, String currency) {
        return PaymentResponse.builder()
                .transactionId(dto.getTranID())
                .merchantRefNo(dto.getOrderId())
                .brand(brand)
                .provider(PaymentProvider.FIUU)
                .status(mapStatus(dto.getStatus()))
                .amount(amount)
                .currency(currency)
                .rawProviderResponse(dto.getRawBody())
                .build();
    }

    /** FIUU status codes: 00 = success, 11 = failed, 22 = pending, else treat as initiated/unknown. */
    public TransactionStatus mapStatus(String fiuuStatus) {
        if (fiuuStatus == null) {
            return TransactionStatus.INITIATED;
        }
        return switch (fiuuStatus) {
            case "00" -> TransactionStatus.SUCCESS;
            case "11" -> TransactionStatus.FAILED;
            case "22" -> TransactionStatus.PENDING;
            default -> TransactionStatus.INITIATED;
        };
    }
}