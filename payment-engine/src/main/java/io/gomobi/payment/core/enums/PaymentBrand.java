package io.gomobi.payment.core.enums;

/**
 * Specific payment brand/method, e.g. what the customer sees at checkout.
 * Each brand belongs to exactly one {@link PaymentCategory}.
 * A brand can be served by more than one {@link PaymentProvider} (adapter).
 */
public enum PaymentBrand {

    // QR
    QRIS(PaymentCategory.QR),
    DUITNOW(PaymentCategory.QR),
    VIETQR(PaymentCategory.QR),

    // E-Wallet
    BOOST(PaymentCategory.E_WALLET),
    GRAB(PaymentCategory.E_WALLET),
    TNG(PaymentCategory.E_WALLET),
    SHOPEE(PaymentCategory.E_WALLET),
    DANA(PaymentCategory.E_WALLET),
    LINKAJA(PaymentCategory.E_WALLET),

    // Online Banking
    VIRTUAL_ACCOUNT(PaymentCategory.ONLINE_BANKING),
    FPX(PaymentCategory.ONLINE_BANKING);

    private final PaymentCategory category;

    PaymentBrand(PaymentCategory category) {
        this.category = category;
    }

    public PaymentCategory getCategory() {
        return category;
    }
}