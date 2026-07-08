package io.gomobi.payment.util;

import java.math.BigDecimal;

/**
 * Converts between minor units (integer API/storage contract) and decimal
 * major units expected by provider integrations.
 */
public final class AmountMinorUnits {

    private static final int DEFAULT_SCALE = 2;

    private AmountMinorUnits() {
    }

    public static BigDecimal toMajor(Long amountMinor) {
        if (amountMinor == null) {
            return null;
        }
        return BigDecimal.valueOf(amountMinor).movePointLeft(DEFAULT_SCALE);
    }
}

