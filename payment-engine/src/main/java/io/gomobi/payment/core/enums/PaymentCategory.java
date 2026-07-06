package io.gomobi.payment.core.enums;

/**
 * Top-level grouping of payment methods. Used mainly for reporting/filtering;
 * routing itself works off {@link PaymentBrand} + {@link PaymentProvider}.
 */
public enum PaymentCategory {
    QR,
    E_WALLET,
    ONLINE_BANKING
}