package io.gomobi.payment.util;

import java.math.BigInteger;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isPositiveAmount(BigInteger amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isVietnamRegion(String regionCode, String channelCode) {
        return (regionCode != null && regionCode.equalsIgnoreCase("VN"))
                || (channelCode != null && channelCode.toUpperCase().startsWith("VN_"));
    }

    public static String normalizeVietnamMobileNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String sanitized = phoneNumber.trim().replaceAll("[\\s\\-()]+", "");
        if (sanitized.matches("^0[35789]\\d{8}$")) {
            return "+84" + sanitized.substring(1);
        }
        if (sanitized.matches("^\\+84[35789]\\d{8}$")) {
            return sanitized;
        }
        if (sanitized.matches("^84[35789]\\d{8}$")) {
            return "+84" + sanitized.substring(2);
        }
        return null;
    }
}
