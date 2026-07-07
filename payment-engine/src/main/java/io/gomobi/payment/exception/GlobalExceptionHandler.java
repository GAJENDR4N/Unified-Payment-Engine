package io.gomobi.payment.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Single place converting every exception into the common {@link ErrorResponse} shape. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentEngineException.class)
    public ResponseEntity<ErrorResponse> handlePaymentEngineException(PaymentEngineException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.error("event=PAYMENT_CREATION_FAILED result=FAILED errorCode={} message={}",
                errorCode.getCode(), ex.getMessage());
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", errorCode.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("[{}] {}", errorCode.getCode(), ex.getMessage());
        }
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getField() + ": "
                        + ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        log.warn("event=REQUEST_VALIDATED result=FAILED errorCode={} message={}",
                ErrorCode.VALIDATION_ERROR.getCode(), message);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("event=REQUEST_VALIDATED result=FAILED errorCode={} message={}",
                ErrorCode.VALIDATION_ERROR.getCode(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getDefaultMessage()));
    }
}
