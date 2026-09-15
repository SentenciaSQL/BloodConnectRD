-- Auditoría de invocaciones MCP (sin parámetros ni datos personales).
CREATE TABLE mcp_audit_events (
    id              BIGSERIAL PRIMARY KEY,
    tool_name       VARCHAR(80)  NOT NULL,
    invoked_at      TIMESTAMPTZ  NOT NULL,
    outcome         VARCHAR(20)  NOT NULL,
    principal_id    BIGINT,
    principal_role  VARCHAR(20),
    error_code      VARCHAR(80)
);

CREATE INDEX idx_mcp_audit_events_invoked_at ON mcp_audit_events (invoked_at DESC);
CREATE INDEX idx_mcp_audit_events_tool_name ON mcp_audit_events (tool_name);
