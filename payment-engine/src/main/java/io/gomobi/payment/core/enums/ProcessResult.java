package io.gomobi.payment.core.enums;

/**
 * Processing result values stored in QR_CALLBACK_AUDIT.PROCESS_RESULT.
 */
public enum ProcessResult {
    SUCCESS,
    FAILED,
    DUPLICATE,
    IGNORED,
    INVALID_SIGNATURE,
    VALIDATION_FAILED
}

