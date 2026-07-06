package io.gomobi.payment.repository;

import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.ProviderSupportedMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderSupportedMethodRepository extends JpaRepository<ProviderSupportedMethod, Long> {

    List<ProviderSupportedMethod> findByPaymentMethodFkAndStatus(Long paymentMethodFk, Status status);
}
