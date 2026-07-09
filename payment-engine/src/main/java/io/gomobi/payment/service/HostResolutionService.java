package io.gomobi.payment.service;

import io.gomobi.payment.config.HostGatewayMappingProperties;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves the "requested host" for an inbound payment: given the
 * payment_method.channel_code on the request, finds the configured
 * payment method, the active provider(s) supporting it
 * (PROVIDER_SUPPORTED_METHOD), and the technical gateway adapter that
 * processes that host.
 * The Creation Payment contract does not let the caller name a host
 * explicitly - routing is fully config-driven off channel_code.
 * <p>
 * Current temporary behavior for channels mapped to multiple active providers:
 * pick the first row by PROVIDER_SUPPORTED_METHOD_ID ascending (oldest inserted mapping).
 * A dedicated routing strategy can replace this deterministic fallback later.
 */
@Service
@RequiredArgsConstructor
public class HostResolutionService {

    private final ReferenceDataCacheService referenceDataCacheService;
    private final HostGatewayMappingProperties hostGatewayMappingProperties;

    public ResolvedHost resolve(String channelCode) {
        referenceDataCacheService.findActivePaymentMethodByChannelCode(channelCode)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.PAYMENT_METHOD_NOT_CONFIGURED,
                        "No active payment method configured for channel_code: " + channelCode));

        ReferenceDataCacheService.CachedHostReference hostReference = referenceDataCacheService
                .findActiveHostByChannelCode(channelCode)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.NO_ACTIVE_PROVIDER_FOR_METHOD,
                        "No active provider is configured for channel_code: " + channelCode));

        PaymentProvider gateway = resolveGateway(hostReference.providerConfiguration().getProviderCode());
        return new ResolvedHost(hostReference.paymentMethod(), hostReference.providerConfiguration(), gateway);
    }

    /** Exposed for callers (e.g. status enquiry) that already know the host provider code. */
    public PaymentProvider resolveGateway(String hostProviderCode) {
        String gatewayName = hostGatewayMappingProperties.getHostGatewayMapping().get(hostProviderCode);
        if (gatewayName == null || gatewayName.isBlank()) {
            throw new PaymentEngineException(ErrorCode.NO_ACTIVE_PROVIDER_FOR_METHOD,
                    "No gateway adapter mapping configured for host provider: " + hostProviderCode);
        }
        try {
            return PaymentProvider.valueOf(gatewayName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentEngineException(ErrorCode.PROVIDER_NOT_FOUND,
                    "Configured gateway '" + gatewayName + "' for host " + hostProviderCode + " is not a known adapter", e);
        }
    }
}
