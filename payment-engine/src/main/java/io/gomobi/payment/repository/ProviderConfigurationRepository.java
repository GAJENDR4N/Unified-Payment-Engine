package io.gomobi.payment.repository;

import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.ProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface ProviderConfigurationRepository extends JpaRepository<ProviderConfiguration, BigInteger> {

    Optional<ProviderConfiguration> findByProviderCode(String providerCode);

    List<ProviderConfiguration> findAllByStatus(Status status);
}
