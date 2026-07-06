package io.gomobi.payment.adapter.fiuu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiuuPaymentResponseDto {
    private String tranID;
    private String orderId;
    private String status;
    private String paymentUrl;
    private String qrString;
    private String errorCode;
    private String errorDesc;
    private String rawBody;
}