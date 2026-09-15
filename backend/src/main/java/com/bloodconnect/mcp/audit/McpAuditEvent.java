package com.bloodconnect.mcp.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mcp_audit_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_name", nullable = false, length = 80)
    private String toolName;

    @Column(name = "invoked_at", nullable = false)
    private Instant invokedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private McpAuditOutcome outcome;

    @Column(name = "principal_id")
    private Long principalId;

    @Column(name = "principal_role", length = 20)
    private String principalRole;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @PrePersist
    void onCreate() {
        if (invokedAt == null) {
            invokedAt = Instant.now();
        }
    }
}
