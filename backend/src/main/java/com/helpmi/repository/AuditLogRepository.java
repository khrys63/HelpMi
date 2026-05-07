package com.helpmi.repository;

import com.helpmi.domain.AuditLog;
import com.helpmi.domain.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
}
