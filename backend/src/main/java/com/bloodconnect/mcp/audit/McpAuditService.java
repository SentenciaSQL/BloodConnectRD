package com.bloodconnect.mcp.audit;

import com.bloodconnect.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Audita invocaciones MCP sin registrar parámetros, JWT ni datos personales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpAuditService {

    private final McpAuditRepository mcpAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String toolName) {
        persist(toolName, McpAuditOutcome.SUCCESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordError(String toolName, String errorCode) {
        persist(toolName, McpAuditOutcome.ERROR, sanitizeErrorCode(errorCode));
    }

    private void persist(String toolName, McpAuditOutcome outcome, String errorCode) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long principalId = null;
            String principalRole = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
                principalId = principal.getId();
                principalRole = principal.getRole() == null ? null : principal.getRole().name();
            } else if (authentication != null && authentication.isAuthenticated()) {
                principalRole = "CLIENT";
            }

            mcpAuditRepository.save(McpAuditEvent.builder()
                    .toolName(toolName)
                    .invokedAt(Instant.now())
                    .outcome(outcome)
                    .principalId(principalId)
                    .principalRole(principalRole)
                    .errorCode(errorCode)
                    .build());
        } catch (RuntimeException exception) {
            log.warn("No se pudo persistir la auditoría MCP de la herramienta {}", toolName);
        }
    }

    private String sanitizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "UNKNOWN";
        }
        String trimmed = errorCode.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }
}
