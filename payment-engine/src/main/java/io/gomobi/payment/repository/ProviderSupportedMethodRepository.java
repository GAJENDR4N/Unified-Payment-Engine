package io.gomobi.payment.repository;

import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.ProviderSupportedMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.List;

public interface ProviderSupportedMethodRepository extends JpaRepository<ProviderSupportedMethod, BigInteger> {

    List<ProviderSupportedMethod> findByPaymentMethod_IdAndStatusOrderByIdAsc(Long paymentMethodId, Status status);

    List<ProviderSupportedMethod> findAllByStatusOrderByIdAsc(Status status);

}
