package org.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tms.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
