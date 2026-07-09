package io.gomobi.payment.repository;

import io.gomobi.payment.entity.QrCallbackAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface QrCallbackAuditRepository extends JpaRepository<QrCallbackAudit, BigInteger> {
}

