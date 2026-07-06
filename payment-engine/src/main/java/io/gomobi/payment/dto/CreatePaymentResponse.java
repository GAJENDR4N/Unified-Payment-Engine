package io.gomobi.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gomobi.payment.core.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResponse {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("global_account_id")
    private String globalAccountId;

    private TransactionStatus status;

    private BigDecimal amount;

    private String currency;

    @JsonProperty("payment_method")
    private PaymentMethodInfo paymentMethod;

    @JsonProperty("qr_string")
    private String qrString;

    @JsonProperty("payment_url")
    private String paymentUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
