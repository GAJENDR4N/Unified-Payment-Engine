package io.gomobi.payment.repository;

import io.gomobi.payment.entity.QrTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;

public interface QrTransactionDetailsRepository extends JpaRepository<QrTransactionDetails, BigInteger> {

    Optional<QrTransactionDetails> findByPaymentTransaction_Id(BigInteger paymentTransactionId);
}
