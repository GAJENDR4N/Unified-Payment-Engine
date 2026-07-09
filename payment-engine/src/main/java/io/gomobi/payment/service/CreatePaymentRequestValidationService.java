package io.gomobi.payment.service;

import io.gomobi.payment.dto.CreatePaymentRequest;
import io.gomobi.payment.dto.CustomerInfo;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.util.ValidationUtils;
import org.springframework.stereotype.Service;

/**
 * Request-level validation and normalization that depends on payment method / region context.
 * Bean Validation handles structural checks (@NotBlank, @NotNull, etc.). This service applies
 * cross-field rules that need the resolved region/payment configuration, such as Vietnam phone
 * normalization for Vietnam QR flows.
 */
@Service
public class CreatePaymentRequestValidationService {

    public void validateAndNormalize(CreatePaymentRequest request, ResolvedHost resolvedHost) {
        if (request == null) {
            throw new PaymentEngineException(ErrorCode.VALIDATION_ERROR, "request must not be null");
        }
        if (!ValidationUtils.isPositiveAmount(request.getAmount())) {
            throw new PaymentEngineException(ErrorCode.VALIDATION_ERROR, "amount must be a positive whole number");
        }

        if (resolvedHost == null || resolvedHost.providerConfiguration() == null || resolvedHost.paymentMethod() == null) {
            return;
        }

        if (ValidationUtils.isVietnamRegion(
                resolvedHost.providerConfiguration().getRegionCode(),
                resolvedHost.paymentMethod().getChannelCode())) {
            normalizeVietnamPhone(request.getCustomer());
        }
    }

    private void normalizeVietnamPhone(CustomerInfo customer) {
        if (customer == null || ValidationUtils.isBlank(customer.getPhone())) {
            throw new PaymentEngineException(ErrorCode.VALIDATION_ERROR,
                    "customer.phone is required for Vietnam payments");
        }

        String normalizedPhone = ValidationUtils.normalizeVietnamMobileNumber(customer.getPhone());
        if (normalizedPhone == null) {
            throw new PaymentEngineException(ErrorCode.VALIDATION_ERROR,
                    "customer.phone must be a Vietnam mobile number and start with 0 or +84");
        }

        customer.setPhone(normalizedPhone);
    }
}

