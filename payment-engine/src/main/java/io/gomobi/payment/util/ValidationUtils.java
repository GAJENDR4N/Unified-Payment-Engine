package io.gomobi.payment.util;

import java.math.BigDecimal;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
