package io.gomobi.payment.adapter.payok;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.adapter.payok.dto.PayokPaymentRequestDto;
import io.gomobi.payment.adapter.payok.dto.PayokPaymentResponseDto;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.core.model.PaymentRequest;
import io.gomobi.payment.core.model.PaymentResponse;
import io.gomobi.payment.core.model.StatusQueryRequest;
import io.gomobi.payment.core.model.StatusQueryResponse;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Adapter for the PAYOK VietQR gateway.
 * STATUS: typed skeleton - see {@link PayokClient} javadoc. Supports
 * VIETQR only; extend SUPPORTED_BRANDS once PAYOK is certified for others.
 */
@Component
public class PayokPaymentAdapter implements PaymentProviderAdapter {

    private static final Set<PaymentBrand> SUPPORTED_BRANDS = Set.of(PaymentBrand.VIETQR);

    private final PayokRequestBuilder requestBuilder;
    private final PayokClient client;
    private final PayokStatusMapper statusMapper;

    public PayokPaymentAdapter(PayokRequestBuilder requestBuilder, PayokClient client, PayokStatusMapper statusMapper) {
        this.requestBuilder = requestBuilder;
        this.client = client;
        this.statusMapper = statusMapper;
    }

    @Override
    public PaymentProvider getProviderCode() {
        return PaymentProvider.PAYOK;
    }

    @Override
    public Set<PaymentBrand> getSupportedBrands() {
        return SUPPORTED_BRANDS;
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        validateBrandSupported(request.getBrand());

        PayokPaymentRequestDto providerRequest = requestBuilder.build(request);
        PayokPaymentResponseDto rawResponse = client.submitPayment(providerRequest);

        return statusMapper.toPaymentResponse(rawResponse, request.getBrand(), request.getAmount(), request.getCurrency());
    }

    @Override
    public StatusQueryResponse queryStatus(StatusQueryRequest request) {
        validateBrandSupported(request.getBrand());

        String rawBody = client.queryStatus(request.getMerchantRefNo());

        return StatusQueryResponse.builder()
                .transactionId(request.getTransactionId())
                .rawProviderResponse(rawBody)
                .status(TransactionStatus.PENDING)
                .build();
    }

    private void validateBrandSupported(PaymentBrand brand) {
        if (!supports(brand)) {
            throw new PaymentEngineException(ErrorCode.UNSUPPORTED_BRAND,
                    "Provider " + PaymentProvider.PAYOK + " does not support brand " + brand);
        }
    }
}
