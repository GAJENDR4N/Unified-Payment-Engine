package io.gomobi.payment.dto;

import io.gomobi.payment.core.enums.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodInfo {

    @NotNull
    private PaymentMethodType type;

    @NotBlank
    private String channelCode;
}
