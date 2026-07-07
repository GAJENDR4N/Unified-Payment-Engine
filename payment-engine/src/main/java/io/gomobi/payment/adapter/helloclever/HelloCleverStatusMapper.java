package io.gomobi.payment.adapter.helloclever;

import io.gomobi.payment.adapter.helloclever.dto.HelloCleverPayinResponseDto;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.PaymentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HelloCleverStatusMapper {

    public PaymentResponse toPaymentResponse(HelloCleverPayinResponseDto dto,
                                             PaymentBrand brand,
                                             BigDecimal amount,
                                             String currency) {
        return PaymentResponse.builder()
                .transactionId(dto.getUuid())
                .merchantRefNo(dto.getExternalId())
                .pspRefNo(dto.getUuid())
                .brand(brand)
                .provider(PaymentProvider.HELLO_CLEVER)
                .status(mapStatus(dto.getStatus()))
                .providerStatus(dto.getStatus())
                .amount(amount)
                .currency(currency)
                .qrString(dto.getPayCode() != null ? dto.getPayCode().getQrString() : null)
                .rawProviderResponse(dto.getRawBody())
                .build();
    }

    public TransactionStatus mapStatus(String providerStatus) {
        if (providerStatus == null) {
            return TransactionStatus.INITIATED;
        }

        return switch (providerStatus.trim().toUpperCase()) {
            case "SUCCESS", "PAID", "COMPLETED" -> TransactionStatus.SUCCESS;
            case "FAILED", "CANCELLED", "EXPIRED" -> TransactionStatus.FAILED;
            case "PENDING", "INITIATED", "CREATED" -> TransactionStatus.PENDING;
            default -> TransactionStatus.INITIATED;
        };
    }
}

