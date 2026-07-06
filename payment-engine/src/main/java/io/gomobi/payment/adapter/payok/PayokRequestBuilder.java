package io.gomobi.payment.adapter.payok;

import io.gomobi.payment.adapter.payok.dto.PayokPaymentRequestDto;
import io.gomobi.payment.core.model.PaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/** Translates the generic {@link PaymentRequest} into PAYOK's expected request fields. */
@Component
public class PayokRequestBuilder {

    @Value("${payment.provider.payok.merchant-id}")
    private String merchantId;

    @Value("${payment.provider.payok.secret-key}")
    private String secretKey;

    public PayokPaymentRequestDto build(PaymentRequest request) {
        String formattedAmount = request.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();

        // TODO: replace with PAYOK's documented signature formula once confirmed.
        String signature = PayokClient.computeSignature(merchantId, request.getMerchantRefNo(), formattedAmount, secretKey);

        return PayokPaymentRequestDto.builder()
                .merchantId(merchantId)
                .orderId(request.getMerchantRefNo())
                .amount(formattedAmount)
                .currency(request.getCurrency())
                .description("Order " + request.getMerchantRefNo())
                .customerName(request.getCustomerDetails() != null ? request.getCustomerDetails().getName() : null)
                .customerEmail(request.getCustomerDetails() != null ? request.getCustomerDetails().getEmail() : null)
                .customerPhone(request.getCustomerDetails() != null ? request.getCustomerDetails().getPhone() : null)
                .notifyUrl(request.getNotifyUrl())
                .signature(signature)
                .build();
    }
}
