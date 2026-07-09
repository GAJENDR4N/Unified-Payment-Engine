package io.gomobi.payment.repository;

import io.gomobi.payment.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, BigInteger> {

    Optional<Merchant> findByMasterMid(String masterMid);
}
