package io.gomobi.payment.adapter.helloclever;

import io.gomobi.payment.adapter.helloclever.dto.HelloCleverPayinRequestDto;
import io.gomobi.payment.core.model.PaymentRequest;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Translates the generic {@link PaymentRequest} into Hello Clever pay-in request fields. */
@Component
public class HelloCleverRequestBuilder {

    public HelloCleverPayinRequestDto build(PaymentRequest request) {
        Map<String, Object> additionalData = request.getAdditionalData();

        return HelloCleverPayinRequestDto.builder()
                .currency(request.getCurrency())
                .gst(false)
                .amount(request.getAmount().setScale(0, RoundingMode.HALF_UP).intValueExact())
                .name(request.getCustomerDetails().getName())
                .email(request.getCustomerDetails().getEmail())
                .externalId(request.getMerchantRefNo())
                .description(request.getDescription())
                .payinMethodName(getProviderPayinMethodName(request))
                .payinMethodParams(HelloCleverPayinRequestDto.PayinMethodParams.builder()
                        .ipAddress("134.168.161.19")
                        .phone(request.getCustomerDetails().getPhone())
                        .build())
                .webhookNotification(HelloCleverPayinRequestDto.WebhookNotification.builder()
                        .endpointUrl("https://webhook.site/50df29b0-25cf-4af8-8d44-5ba539da7f15")
                        .authorizationHeader("*****")
                        .build())
                .expiredAt(getExpiredAt())
                .build();
    }

    private String getProviderPayinMethodName(PaymentRequest request) {
        switch (request.getBrand()) {
            case VIETQR:
                return "vn_vietqr_vnd";
            default:
                throw new IllegalArgumentException("Unsupported brand for Hello Clever: " + request.getBrand());
        }
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static String getExpiredAt() {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusHours(1)
                .format(FORMATTER);
    }
}
