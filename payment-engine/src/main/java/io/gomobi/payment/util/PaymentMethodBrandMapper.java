package io.gomobi.payment.util;

import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;

import java.util.Map;

/**
 * Maps PAYMENT_METHOD.PAYMENT_METHOD_CODE (schema/master-data naming, e.g.
 * "DUITNOW_QR") onto the internal {@link PaymentBrand} enum used by adapter
 * dispatch (e.g. DUITNOW). Kept as an explicit table rather than assuming
 * the two always line up, since they already diverge for DuitNow QR.
 */
public final class PaymentMethodBrandMapper {

    private static final Map<String, PaymentBrand> CODE_TO_BRAND = Map.of(
            "QRIS", PaymentBrand.QRIS,
            "DUITNOW_QR", PaymentBrand.DUITNOW,
            "VIETQR", PaymentBrand.VIETQR
    );

    private PaymentMethodBrandMapper() {
    }

    public static PaymentBrand toBrand(String paymentMethodCode) {
        PaymentBrand brand = CODE_TO_BRAND.get(paymentMethodCode);
        if (brand == null) {
            throw new PaymentEngineException(ErrorCode.PAYMENT_METHOD_NOT_CONFIGURED,
                    "No PaymentBrand mapping configured for payment method code: " + paymentMethodCode);
        }
        return brand;
    }
}
