package io.gomobi.payment.repository;

import io.gomobi.payment.entity.ProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderConfigurationRepository extends JpaRepository<ProviderConfiguration, Long> {

    Optional<ProviderConfiguration> findByProviderCode(String providerCode);
}
