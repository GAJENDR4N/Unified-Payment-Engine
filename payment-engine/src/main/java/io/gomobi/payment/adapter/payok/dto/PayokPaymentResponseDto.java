package io.gomobi.payment.adapter.payok.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayokPaymentResponseDto {
    private String transactionId;
    private String orderId;
    private String status;
    private String qrString;
    private String errorCode;
    private String errorDesc;
    private String rawBody;
}
