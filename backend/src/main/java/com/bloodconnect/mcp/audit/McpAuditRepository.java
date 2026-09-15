package com.bloodconnect.mcp.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface McpAuditRepository extends JpaRepository<McpAuditEvent, Long> {
}
