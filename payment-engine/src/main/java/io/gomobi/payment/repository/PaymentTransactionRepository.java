package io.gomobi.payment.repository;

import io.gomobi.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, BigInteger> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findTopByPspRefNoOrderByIdDesc(String pspRefNo);

//    Optional<PaymentTransaction> findByMerchant_IdAndGatewayIdempotencyKey(BigInteger merchantId, String gatewayIdempotencyKey);

    Optional<PaymentTransaction> findByMerchant_IdAndMerchantRefNo(BigInteger merchantId, String merchantRefNo);

    Optional<PaymentTransaction> findTopByMerchantRefNoOrderByIdDesc(String merchantRefNo);
}
