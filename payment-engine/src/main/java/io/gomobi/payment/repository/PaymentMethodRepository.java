package io.gomobi.payment.repository;

import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, BigInteger> {

    Optional<PaymentMethod> findByChannelCodeAndStatus(String channelCode, Status status);

    List<PaymentMethod> findAllByStatus(Status status);
}
