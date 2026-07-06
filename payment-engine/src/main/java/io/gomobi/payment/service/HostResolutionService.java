package io.gomobi.payment.service;

import io.gomobi.payment.config.HostGatewayMappingProperties;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.ProviderConfiguration;
import io.gomobi.payment.entity.ProviderSupportedMethod;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.repository.PaymentMethodRepository;
import io.gomobi.payment.repository.ProviderConfigurationRepository;
import io.gomobi.payment.repository.ProviderSupportedMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves the "requested host" for an inbound payment: given the
 * payment_method.channel_code on the request, finds the configured
 * payment method, the active provider(s) supporting it
 * (PROVIDER_SUPPORTED_METHOD), and the technical gateway adapter that
 * processes that host.
 *
 * The Create Payment contract does not let the caller name a host
 * explicitly - routing is fully config-driven off channel_code. Today
 * every channel_code in seed data resolves to exactly one active
 * provider, so resolution is unambiguous; if/when a channel is ever
 * supported by more than one active provider, {@link ErrorCode#AMBIGUOUS_PROVIDER_ROUTING}
 * is raised rather than guessing - at that point this service is the
 * right place to add an explicit routing/preference rule.
 */
@Service
@RequiredArgsConstructor
public class HostResolutionService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final ProviderSupportedMethodRepository providerSupportedMethodRepository;
    private final ProviderConfigurationRepository providerConfigurationRepository;
    private final HostGatewayMappingProperties hostGatewayMappingProperties;

    public ResolvedHost resolve(String channelCode) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByChannelCodeAndStatus(channelCode, Status.ACTIVE)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.PAYMENT_METHOD_NOT_CONFIGURED,
                        "No active payment method configured for channel_code: " + channelCode));

        List<ProviderSupportedMethod> supportedMethods = providerSupportedMethodRepository
                .findByPaymentMethodFkAndStatus(paymentMethod.getPaymentMethodId(), Status.ACTIVE);

        if (supportedMethods.isEmpty()) {
            throw new PaymentEngineException(ErrorCode.NO_ACTIVE_PROVIDER_FOR_METHOD,
                    "No active provider is configured for channel_code: " + channelCode);
        }
        if (supportedMethods.size() > 1) {
            throw new PaymentEngineException(ErrorCode.AMBIGUOUS_PROVIDER_ROUTING,
                    "Multiple active providers support channel_code: " + channelCode
                            + " - add an explicit routing rule before enabling this channel");
        }

        Long providerConfigurationFk = supportedMethods.get(0).getProviderConfigurationFk();
        ProviderConfiguration providerConfiguration = providerConfigurationRepository.findById(providerConfigurationFk)
                .orElseThrow(() -> new PaymentEngineException(ErrorCode.NO_ACTIVE_PROVIDER_FOR_METHOD,
                        "Configured provider (id=" + providerConfigurationFk + ") no longer exists"));

        PaymentProvider gateway = resolveGateway(providerConfiguration.getProviderCode());

        return new ResolvedHost(paymentMethod, providerConfiguration, gateway);
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
