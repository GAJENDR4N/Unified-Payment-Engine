package io.gomobi.payment.core.model;

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
public class StatusQueryResponse {
    private String transactionId;
    private String pspRefNo;
    private TransactionStatus status;
    private BigDecimal amount;
    private LocalDateTime paidTimestamp;
    private String rawProviderResponse;
}