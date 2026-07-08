package io.gomobi.payment.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gomobi.payment.dto.CreatePaymentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

/** Error body for duplicate reference conflicts, including existing transaction data. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateReferenceErrorResponse {

    private String code;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String traceId;

    @JsonProperty("existing_payment")
    private CreatePaymentResponse existingPayment;

    public static DuplicateReferenceErrorResponse of(ErrorCode errorCode,
                                                     String message,
                                                     CreatePaymentResponse existingPayment) {
        return DuplicateReferenceErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getDefaultMessage())
                .timestamp(LocalDateTime.now())
                .traceId(MDC.get(io.gomobi.payment.logging.CorrelationIdFilter.TRACE_ID_MDC_KEY))
                .existingPayment(existingPayment)
                .build();
    }
}

