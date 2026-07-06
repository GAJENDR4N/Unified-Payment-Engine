package io.gomobi.payment.repository;

import io.gomobi.payment.entity.SubMerchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubMerchantRepository extends JpaRepository<SubMerchant, Long> {

    Optional<SubMerchant> findBySubMerchantMid(String subMerchantMid);
}
