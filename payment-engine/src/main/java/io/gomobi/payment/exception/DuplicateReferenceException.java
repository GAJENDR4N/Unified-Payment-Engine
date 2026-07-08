package io.gomobi.payment.exception;

import io.gomobi.payment.dto.CreatePaymentResponse;
import lombok.Getter;

/** Carries existing transaction details for duplicate merchant reference conflicts. */
@Getter
public class DuplicateReferenceException extends PaymentEngineException {

    private final CreatePaymentResponse existingPayment;

    public DuplicateReferenceException(String detail,
                                       CreatePaymentResponse existingPayment,
                                       Throwable cause) {
        super(ErrorCode.DUPLICATE_REFERENCE_ID, detail, cause);
        this.existingPayment = existingPayment;
    }
}

