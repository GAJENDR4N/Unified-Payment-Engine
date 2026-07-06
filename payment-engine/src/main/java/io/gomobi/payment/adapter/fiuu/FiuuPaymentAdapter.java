package io.gomobi.payment.adapter.fiuu;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.adapter.fiuu.dto.FiuuPaymentRequestDto;
import io.gomobi.payment.adapter.fiuu.dto.FiuuPaymentResponseDto;
import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.core.model.PaymentRequest;
import io.gomobi.payment.core.model.PaymentResponse;
import io.gomobi.payment.core.model.StatusQueryRequest;
import io.gomobi.payment.core.model.StatusQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Adapter for the FIUU / Razer Merchant Services gateway.
 * Supports DuitNow QR, QRIS and TNG in the current GoMobi setup - extend
 * SUPPORTED_BRANDS if FIUU is certified for additional brands.
 *
 * Scope: payment creation and status enquiry only. Inbound FIUU IPN
 * callbacks are received and verified by the gateway layer upstream of
 * this service.
 */
@Slf4j
@Component
public class FiuuPaymentAdapter implements PaymentProviderAdapter {

    private static final Set<PaymentBrand> SUPPORTED_BRANDS =
            Set.of(PaymentBrand.DUITNOW, PaymentBrand.QRIS, PaymentBrand.TNG);

    private final FiuuRequestBuilder requestBuilder;
    private final FiuuClient client;
    private final FiuuStatusMapper statusMapper;

    public FiuuPaymentAdapter(FiuuRequestBuilder requestBuilder, FiuuClient client, FiuuStatusMapper statusMapper) {
        this.requestBuilder = requestBuilder;
        this.client = client;
        this.statusMapper = statusMapper;
    }

    @Override
    public PaymentProvider getProviderCode() {
        return PaymentProvider.FIUU;
    }

    @Override
    public Set<PaymentBrand> getSupportedBrands() {
        return SUPPORTED_BRANDS;
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        validateBrandSupported(request.getBrand());

        FiuuPaymentRequestDto providerRequest = requestBuilder.build(request);
        FiuuPaymentResponseDto rawResponse = client.submitPayment(providerRequest);

        return statusMapper.toPaymentResponse(rawResponse, request.getBrand(), request.getAmount(), request.getCurrency());
    }

    @Override
    public StatusQueryResponse queryStatus(StatusQueryRequest request) throws NoSuchAlgorithmException {
        validateBrandSupported(request.getBrand());

        String rawBody = client.queryStatus(request.getMerchantRefNo());

        // NOTE: FIUU's query response is delimited text, not JSON - parse per their documented
        // field order here. Left as a stub since the exact response format should be verified
        // against the current FIUU API docs / sandbox before wiring this up for real traffic.
        return StatusQueryResponse.builder()
                .transactionId(request.getTransactionId())
                .rawProviderResponse(rawBody)
                .status(TransactionStatus.PENDING)
                .build();
    }

    private void validateBrandSupported(PaymentBrand brand) {
        if (!supports(brand)) {
            throw new PaymentEngineException(ErrorCode.UNSUPPORTED_BRAND,
                    "Provider " + PaymentProvider.FIUU + " does not support brand " + brand);
        }
    }
}
