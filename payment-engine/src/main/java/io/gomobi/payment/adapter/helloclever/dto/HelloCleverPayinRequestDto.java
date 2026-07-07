package io.gomobi.payment.adapter.helloclever.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Outbound request payload for Hello Clever pay-in creation API. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelloCleverPayinRequestDto {

    private String currency;
    private Boolean gst;
    private Integer amount;
    private String name;
    private String email;

    @JsonProperty("external_id")
    private String externalId;

    private String description;

    @JsonProperty("payin_method_name")
    private String payinMethodName;

    @JsonProperty("payin_method_params")
    private PayinMethodParams payinMethodParams;

    @JsonProperty("webhook_notification")
    private WebhookNotification webhookNotification;

    @JsonProperty("expired_at")
    private String expiredAt;

    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayinMethodParams {

        private Boolean transliterate;

        @JsonProperty("ip_address")
        private String ipAddress;

        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookNotification {

        @JsonProperty("endpoint_url")
        private String endpointUrl;

        @JsonProperty("authorization_header")
        private String authorizationHeader;
    }
}

