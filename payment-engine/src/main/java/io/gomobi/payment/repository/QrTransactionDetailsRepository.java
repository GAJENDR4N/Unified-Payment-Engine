package io.gomobi.payment.repository;

import io.gomobi.payment.entity.QrTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrTransactionDetailsRepository extends JpaRepository<QrTransactionDetails, Long> {

    Optional<QrTransactionDetails> findByPaymentTransactionFk(Long paymentTransactionFk);
}
