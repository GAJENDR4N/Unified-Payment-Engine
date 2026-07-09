package io.gomobi.payment.service;

import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.ProviderConfiguration;
import io.gomobi.payment.entity.ProviderSupportedMethod;
import io.gomobi.payment.repository.PaymentMethodRepository;
import io.gomobi.payment.repository.ProviderConfigurationRepository;
import io.gomobi.payment.repository.ProviderSupportedMethodRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory cache for read-mostly reference data (payment method / provider config).
 *
 * The cache is loaded at startup and can be refreshed via an admin endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceDataCacheService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final ProviderConfigurationRepository providerConfigurationRepository;
    private final ProviderSupportedMethodRepository providerSupportedMethodRepository;

    private volatile CacheSnapshot snapshot = CacheSnapshot.empty();

    @PostConstruct
    void initialize() {
        refreshCache();
    }

    public CacheMetrics refreshCache() {
        List<PaymentMethod> allPaymentMethods = paymentMethodRepository.findAll();
        List<ProviderConfiguration> allProviderConfigurations = providerConfigurationRepository.findAll();
        List<PaymentMethod> activePaymentMethods = paymentMethodRepository.findAllByStatus(Status.ACTIVE);
        List<ProviderConfiguration> activeProviderConfigurations = providerConfigurationRepository.findAllByStatus(Status.ACTIVE);
        List<ProviderSupportedMethod> activeMappings = providerSupportedMethodRepository
                .findAllByStatusOrderByIdAsc(Status.ACTIVE);

        Map<BigInteger, PaymentMethod> paymentMethodsById = allPaymentMethods.stream()
                .collect(Collectors.toMap(PaymentMethod::getId, paymentMethod -> paymentMethod));
        Map<BigInteger, ProviderConfiguration> providerConfigurationsById = allProviderConfigurations.stream()
                .collect(Collectors.toMap(ProviderConfiguration::getId, provider -> provider));

        Map<String, PaymentMethod> activePaymentMethodByChannelCode = activePaymentMethods.stream()
                .collect(Collectors.toMap(PaymentMethod::getChannelCode, paymentMethod -> paymentMethod));
        Map<BigInteger, ProviderConfiguration> activeProviderById = activeProviderConfigurations.stream()
                .collect(Collectors.toMap(ProviderConfiguration::getId, provider -> provider));

        Map<String, CachedHostReference> activeHostByChannelCode = new LinkedHashMap<>();
        for (ProviderSupportedMethod mapping : activeMappings) {
            PaymentMethod paymentMethod = paymentMethodsById.get(mapping.getPaymentMethod().getId());
            ProviderConfiguration providerConfiguration = activeProviderById.get(mapping.getProviderConfiguration().getId());
            if (paymentMethod == null
                    || paymentMethod.getStatus() != Status.ACTIVE
                    || providerConfiguration == null) {
                continue;
            }
            // Keep deterministic precedence: oldest active mapping wins.
            activeHostByChannelCode.putIfAbsent(
                    paymentMethod.getChannelCode(),
                    new CachedHostReference(paymentMethod, providerConfiguration)
            );
        }

        snapshot = new CacheSnapshot(
                Map.copyOf(paymentMethodsById),
                Map.copyOf(providerConfigurationsById),
                Map.copyOf(activePaymentMethodByChannelCode),
                Map.copyOf(activeHostByChannelCode)
        );

        CacheMetrics metrics = metrics();
        log.info("event=REFERENCE_DATA_CACHE_REFRESHED paymentMethods={} providerConfigurations={} activeChannels={}",
                metrics.paymentMethodCount(), metrics.providerConfigurationCount(), metrics.activeChannelCount());
        return metrics;
    }

    public Optional<PaymentMethod> findPaymentMethodById(BigInteger paymentMethodId) {
        if (paymentMethodId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.paymentMethodsById().get(paymentMethodId));
    }

    public Optional<ProviderConfiguration> findProviderConfigurationById(BigInteger providerConfigurationId) {
        if (providerConfigurationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.providerConfigurationsById().get(providerConfigurationId));
    }

    public Optional<CachedHostReference> findActiveHostByChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.activeHostByChannelCode().get(channelCode));
    }

    public Optional<PaymentMethod> findActivePaymentMethodByChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.activePaymentMethodByChannelCode().get(channelCode));
    }

    public CacheMetrics metrics() {
        CacheSnapshot current = snapshot;
        return new CacheMetrics(
                current.paymentMethodsById().size(),
                current.providerConfigurationsById().size(),
                current.activeHostByChannelCode().size()
        );
    }

    public record CachedHostReference(PaymentMethod paymentMethod, ProviderConfiguration providerConfiguration) {
    }

    public record CacheMetrics(int paymentMethodCount, int providerConfigurationCount, int activeChannelCount) {
    }

    private record CacheSnapshot(Map<BigInteger, PaymentMethod> paymentMethodsById,
                                 Map<BigInteger, ProviderConfiguration> providerConfigurationsById,
                                 Map<String, PaymentMethod> activePaymentMethodByChannelCode,
                                 Map<String, CachedHostReference> activeHostByChannelCode) {

        private static CacheSnapshot empty() {
            return new CacheSnapshot(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}

