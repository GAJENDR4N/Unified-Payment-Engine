package io.gomobi.payment.exception;

import org.springframework.http.HttpStatus;

/**
 * Central catalogue of business error codes. Each code carries the HTTP
 * status to return and a default human-readable message, so the exception
 * handler doesn't need a switch statement to translate one into the other.
 *
 * Codes are grouped into numeric ranges by concern, and new codes should be
 * appended at the end of their group's range rather than renumbering
 * existing ones (existing codes may already be relied on by client-side
 * error handling / alerting rules):
 *
 *   1000-1999  Request validation errors               -> 400
 *   2000-2999  Merchant / routing / configuration       -> 400/404/409
 *   3000-3999  Transaction lookup / state errors        -> 404/409
 *   5000-5999  External provider / gateway errors       -> 502/504
 *   8000-8999  Auth errors                              -> 401/403
 *   9000-9999  System / unexpected errors               -> 500
 */
public enum ErrorCode {

    // ---- 1xxx: request validation -------------------------------------
    VALIDATION_ERROR("PAY-1000", HttpStatus.BAD_REQUEST, "Request validation failed"),
    DUPLICATE_IDEMPOTENCY_KEY("PAY-1001", HttpStatus.CONFLICT,
            "A payment with this idempotency key was already submitted with different request data"),
    DUPLICATE_REFERENCE_ID("PAY-1002", HttpStatus.CONFLICT,
            "A payment with this reference_id already exists for this merchant"),

    // ---- 2xxx: merchant / routing / configuration ----------------------
    MERCHANT_NOT_FOUND("PAY-2000", HttpStatus.NOT_FOUND, "Merchant not found for the given master_mid"),
    MERCHANT_INACTIVE("PAY-2001", HttpStatus.CONFLICT, "Merchant is not active"),
    SUB_MERCHANT_NOT_FOUND("PAY-2002", HttpStatus.NOT_FOUND, "Sub-merchant not found for the given sub_merchant_mid"),
    PAYMENT_METHOD_NOT_CONFIGURED("PAY-2003", HttpStatus.BAD_REQUEST,
            "No payment method is configured for the given channel_code"),
    NO_ACTIVE_PROVIDER_FOR_METHOD("PAY-2004", HttpStatus.CONFLICT,
            "No active provider is configured to process this payment method"),
    AMBIGUOUS_PROVIDER_ROUTING("PAY-2005", HttpStatus.CONFLICT,
            "More than one active provider supports this payment method; explicit routing is required"),
    UNSUPPORTED_BRAND("PAY-2006", HttpStatus.BAD_REQUEST, "Provider does not support the requested payment brand"),
    PROVIDER_NOT_FOUND("PAY-2007", HttpStatus.INTERNAL_SERVER_ERROR, "No adapter registered for the resolved provider"),

    // ---- 3xxx: transaction lookup / state -------------------------------
    TRANSACTION_NOT_FOUND("PAY-3000", HttpStatus.NOT_FOUND, "Transaction not found"),

    // ---- 5xxx: external provider / gateway ------------------------------
    EXTERNAL_PROVIDER_ERROR("PAY-5000", HttpStatus.BAD_GATEWAY, "The upstream payment provider returned an error"),
    EXTERNAL_PROVIDER_TIMEOUT("PAY-5001", HttpStatus.GATEWAY_TIMEOUT, "The upstream payment provider did not respond in time"),
    EXTERNAL_PROVIDER_UNAVAILABLE("PAY-5002", HttpStatus.SERVICE_UNAVAILABLE, "The upstream payment provider is currently unavailable"),

    // ---- 8xxx: auth ------------------------------------------------------
    AUTH_FAILED("PAY-8000", HttpStatus.UNAUTHORIZED, "Missing or invalid API key"),

    // ---- 9xxx: system ------------------------------------------------------
    DB_PERSIST_ERROR("PAY-9000", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist transaction"),
    SYSTEM_ERROR("PAY-9999", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
