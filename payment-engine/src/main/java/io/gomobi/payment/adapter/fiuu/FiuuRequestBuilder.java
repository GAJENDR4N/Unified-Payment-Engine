package io.gomobi.payment.adapter.fiuu;

import io.gomobi.payment.adapter.fiuu.dto.FiuuPaymentRequestDto;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.model.PaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Translates the generic {@link PaymentRequest} into FIUU's expected
 * gate-in.php form fields, including the per-brand channel code and the
 * vsign signature (pre_skey then skey, per FIUU's documented formula).
 */
@Component
public class FiuuRequestBuilder {

    private static final Map<PaymentBrand, String> BRAND_TO_CHANNEL = Map.of(
            PaymentBrand.DUITNOW, "DuitNowQR",
            PaymentBrand.QRIS, "QRIS",
            PaymentBrand.TNG, "TNG"
    );

    private final FiuuClient fiuuClient;
    private final String merchantId;
    private final String secretKey;

    public FiuuRequestBuilder(FiuuClient fiuuClient,
                              @Value("${payment.provider.fiuu.merchant-id}") String merchantId,
                              @Value("${payment.provider.fiuu.secret-key}") String secretKey) {
        this.fiuuClient = fiuuClient;
        this.merchantId = merchantId;
        this.secretKey = secretKey;
    }

    public FiuuPaymentRequestDto build(PaymentRequest request) {
        String channel = BRAND_TO_CHANNEL.get(request.getBrand());
        String formattedAmount = formatAmount(request.getAmount());

        // FIUU vsign formula: md5( md5(amount+merchantID+orderid+secretKey) )
        String preSkey = FiuuClient.computeSignature(
                formattedAmount, merchantId, request.getMerchantRefNo(), secretKey);
        String vsign = FiuuClient.computeSignature(preSkey);

        return FiuuPaymentRequestDto.builder()
                .merchantId(merchantId)
                .amount(formattedAmount)
                .orderId(request.getMerchantRefNo())
                .currency(request.getCurrency())
                .channel(channel)
                .billName(request.getCustomerDetails().getName())
                .billEmail(request.getCustomerDetails().getEmail())
                .billMobile(request.getCustomerDetails().getPhone())
                .returnUrl(request.getReturnUrl())
                .notifyUrl(request.getNotifyUrl())
                .vsign(vsign)
                .build();
    }

    /** FIUU requires 2 decimal places, no thousands separators. */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}