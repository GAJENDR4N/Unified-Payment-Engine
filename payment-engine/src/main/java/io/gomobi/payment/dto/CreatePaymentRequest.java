package io.gomobi.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Inbound Create Payment API request. Field names/casing follow the agreed
 * API contract (snake_case over the wire, mapped to camelCase in Java).
 * NOTE: the contract as supplied spells the idempotency field
 * "idempotent_key" - this is accepted via {@code @JsonAlias} alongside the
 * more conventional "idempotency_key" so a future spelling fix on either
 * side doesn't break the other. Flagging this for confirmation - once
 * <p>
 * confirmed, the alias can be dropped.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

//    @NotBlank
//    private String idempotencyKey;

    @NotBlank
    private String referenceId;

    @NotBlank
    private String globalAccountId;

    @NotBlank
    private String masterMid;

    private String subMerchantMid;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String description;

    @NotNull
    @Valid
    private PaymentMethodInfo paymentMethod;

    @NotNull
    @Valid
    private CustomerInfo customer;

    private Map<String, Object> metadata;
}
