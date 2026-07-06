package io.gomobi.payment.exception;

import lombok.Getter;

/**
 * Single exception type for every expected/handled failure in this
 * service - validation, routing, provider, or system errors alike. Carries
 * an {@link ErrorCode}, which already knows its HTTP status and default
 * message, so callers branch on a stable enum constant instead of matching
 * exception subtypes or message strings.
 *
 * An optional {@code detail} overrides/extends the error code's default
 * message with context specific to this occurrence (e.g. which field
 * failed, which merchant/channel was involved) without inventing a new
 * error code for every variation.
 */
@Getter
public class PaymentEngineException extends RuntimeException {

    private final ErrorCode errorCode;

    public PaymentEngineException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public PaymentEngineException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public PaymentEngineException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }
}
