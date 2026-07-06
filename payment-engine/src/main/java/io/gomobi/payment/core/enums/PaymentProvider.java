package io.gomobi.payment.core.enums;

/**
 * Technical gateway/switch adapter that actually processes the transaction
 * (i.e. which {@code PaymentProviderAdapter} implementation to invoke).
 * This is distinct from the "host" provider recorded on the transaction
 * (PROVIDER_CONFIGURATION, e.g. MAYBANK, TNG, GOPAY, PAYOK) - several hosts
 * can be processed through the same technical gateway. See
 * {@code HostResolutionService} for how a host provider code is mapped to
 * one of these gateways.
 */
public enum PaymentProvider {
    FIUU,
    PAYOK,
    CURLEC
}
