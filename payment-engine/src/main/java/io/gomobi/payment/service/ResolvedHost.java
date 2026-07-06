package io.gomobi.payment.service;

import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.ProviderConfiguration;

/**
 * Result of resolving which host (provider) and which technical gateway
 * adapter should process a request for a given payment method channel.
 */
public record ResolvedHost(
        PaymentMethod paymentMethod,
        ProviderConfiguration providerConfiguration,
        PaymentProvider gateway
) {
}
