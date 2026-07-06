package io.gomobi.payment.repository;

import io.gomobi.payment.core.enums.Status;
import io.gomobi.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByChannelCodeAndStatus(String channelCode, Status status);
}
