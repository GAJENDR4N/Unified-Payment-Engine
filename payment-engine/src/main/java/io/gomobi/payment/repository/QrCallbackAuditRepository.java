package io.gomobi.payment.repository;

import io.gomobi.payment.entity.QrCallbackAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCallbackAuditRepository extends JpaRepository<QrCallbackAudit, Long> {
}

