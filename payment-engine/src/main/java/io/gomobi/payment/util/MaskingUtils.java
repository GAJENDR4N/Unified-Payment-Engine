package io.gomobi.payment.util;

/**
 * Centralized masking so sensitive values (phone numbers, tokens, card
 * numbers, signatures/secrets) never land in plaintext in logs.
 * Every logging call site touching these fields should route through here
 * rather than re-implementing masking ad hoc.
 */
public final class MaskingUtils {

    private MaskingUtils() {
    }

    /** 60111234567 -> 601111***4567 (keep first 4, last 4, mask the middle). */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.trim();
        if (digits.length() <= 8) {
            return "*".repeat(digits.length());
        }
        String prefix = digits.substring(0, 4);
        String suffix = digits.substring(digits.length() - 4);
        return prefix + "*".repeat(digits.length() - 8) + suffix;
    }

    /** Card PAN -> keep first 6 (BIN) + last 4, mask the rest. */
    public static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) {
            return "****";
        }
        String digitsOnly = cardNumber.replaceAll("\\s", "");
        String prefix = digitsOnly.substring(0, 6);
        String suffix = digitsOnly.substring(digitsOnly.length() - 4);
        return prefix + "*".repeat(Math.max(0, digitsOnly.length() - 10)) + suffix;
    }

    /** Tokens/signatures/secrets/API keys/JWTs -> show only a short prefix, mask the rest. */
    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        if (token.length() <= 6) {
            return "*".repeat(token.length());
        }
        return token.substring(0, 6) + "*".repeat(Math.max(4, token.length() - 6));
    }

    /** Emails -> j***@domain.com */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.isEmpty()) {
            return email;
        }
        return local.charAt(0) + "*".repeat(Math.max(1, local.length() - 1)) + domain;
    }
}
