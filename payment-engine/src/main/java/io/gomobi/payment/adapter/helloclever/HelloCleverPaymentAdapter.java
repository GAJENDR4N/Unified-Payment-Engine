package io.gomobi.payment.adapter.helloclever;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.adapter.helloclever.dto.HelloCleverPayinRequestDto;
import io.gomobi.payment.adapter.helloclever.dto.HelloCleverPayinResponseDto;
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

/** Adapter for Hello Clever pay-in creation. */
@Component
public class HelloCleverPaymentAdapter implements PaymentProviderAdapter {

    private static final Set<PaymentBrand> SUPPORTED_BRANDS = Set.of(PaymentBrand.VIETQR);

    private final HelloCleverRequestBuilder requestBuilder;
    private final HelloCleverClient client;
    private final HelloCleverStatusMapper statusMapper;

    public HelloCleverPaymentAdapter(HelloCleverRequestBuilder requestBuilder,
                                     HelloCleverClient client,
                                     HelloCleverStatusMapper statusMapper) {
        this.requestBuilder = requestBuilder;
        this.client = client;
        this.statusMapper = statusMapper;
    }

    @Override
    public PaymentProvider getProviderCode() {
        return PaymentProvider.HELLO_CLEVER;
    }

    @Override
    public Set<PaymentBrand> getSupportedBrands() {
        return SUPPORTED_BRANDS;
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        validateBrandSupported(request.getBrand());

        HelloCleverPayinRequestDto providerRequest = requestBuilder.build(request);
        HelloCleverPayinResponseDto rawResponse = client.submitPayment(providerRequest);

        return statusMapper.toPaymentResponse(rawResponse, request.getBrand(), request.getAmount(), request.getCurrency());
    }

    @Override
    public StatusQueryResponse queryStatus(StatusQueryRequest request) {
        validateBrandSupported(request.getBrand());

        return StatusQueryResponse.builder()
                .transactionId(request.getTransactionId())
                .rawProviderResponse("Hello Clever status query is not yet implemented")
                .status(TransactionStatus.PENDING)
                .build();
    }

    private void validateBrandSupported(PaymentBrand brand) {
        if (!supports(brand)) {
            throw new PaymentEngineException(ErrorCode.UNSUPPORTED_BRAND,
                    "Provider " + PaymentProvider.HELLO_CLEVER + " does not support brand " + brand);
        }
    }
}

