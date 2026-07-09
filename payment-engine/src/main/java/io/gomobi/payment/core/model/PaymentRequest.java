package io.gomobi.payment.core.model;

import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentCategory;
import io.gomobi.payment.core.enums.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * Inbound request from the calling gateway. The gateway has already decided
 * which brand and which provider should handle this transaction - this
 * service's job is purely to dispatch to the matching adapter and process it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull
    private BigInteger merchantId;

    private BigInteger subMerchantId;

    @NotNull
    private PaymentCategory category;

    @NotNull
    private PaymentBrand brand;

    @NotNull
    private PaymentProvider provider;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String merchantRefNo;

    private CustomerDetails customerDetails;

    private String returnUrl;

    private String notifyUrl;

    @NotBlank
    private String description;

    /** Provider/brand-specific extra fields that don't fit the common shape. */
    private Map<String, Object> additionalData;
}