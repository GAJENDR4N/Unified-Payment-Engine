package io.gomobi.payment.repository;

import io.gomobi.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findTopByPspRefNoOrderByPaymentTransactionIdDesc(String pspRefNo);

//    Optional<PaymentTransaction> findByMerchantFkAndGatewayIdempotencyKey(Long merchantFk, String gatewayIdempotencyKey);

    Optional<PaymentTransaction> findByMerchantFkAndMerchantRefNo(Long merchantFk, String merchantRefNo);

    Optional<PaymentTransaction> findTopByMerchantRefNoOrderByPaymentTransactionIdDesc(String merchantRefNo);
}
