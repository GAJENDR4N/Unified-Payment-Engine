package io.gomobi.payment.adapter.helloclever.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Inbound response payload from Hello Clever pay-in creation API. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelloCleverPayinResponseDto {

    private String uuid;
    private String name;
    private String email;

    @JsonProperty("external_id")
    private String externalId;

    private String status;

    @JsonProperty("pay_code")
    private PayCode payCode;

    private String currency;
    private String amount;
    private String total;

    @JsonProperty("paid_amount")
    private String paidAmount;

    @JsonProperty("is_refundable")
    private Boolean refundable;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("expired_at")
    private String expiredAt;

    @JsonProperty("webhook_notification")
    private WebhookNotification webhookNotification;

    @JsonProperty("status_text")
    private String statusText;

    private String description;
    private Boolean gst;

    @JsonProperty("gst_amount")
    private Integer gstAmount;

    @JsonProperty("pay_by")
    private String payBy;

    @JsonProperty("sender_details")
    private Object senderDetails;

    private String stage;
    private Map<String, Object> metadata;

    private String rawBody;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayCode {

        @JsonProperty("qr_string")
        private String qrString;

        @JsonProperty("bank_information")
        private BankInformation bankInformation;

        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInformation {

        @JsonProperty("bank_name")
        private String bankName;

        @JsonProperty("account_number")
        private String accountNumber;

        @JsonProperty("account_name")
        private String accountName;

        @JsonProperty("branch_name")
        private String branchName;

        private String description;

        @JsonProperty("swift_code")
        private String swiftCode;
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

