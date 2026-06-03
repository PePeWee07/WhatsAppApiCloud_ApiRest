-- ============================================================================
--  Semilla de permisos de tools (tabla tool_permissions + tool_permission_roles)
--  Inserta las 10 tools actuales con sus roles por defecto: DOCENTE, ADMINISTRATIVO, ENCARGATURA.
--
--  Requisitos:
--    - Las tablas ya deben existir (Hibernate las crea con ddl-auto al arrancar el core).
--    - Motor: PostgreSQL.
--
--  Es IDEMPOTENTE: se puede correr varias veces sin duplicar ni pisar ediciones manuales.
--  Tras correrlo, evicta el cache "toolPermissionsCache" (o reinicia el core) para que
--  los cambios se reflejen al instante; si no, se refrescan solos en <= 30 min (TTL).
-- ============================================================================

-- 1) Insertar las tools (no hace nada si el tool_name ya existe)
INSERT INTO tool_permissions (tool_name, enabled)
VALUES
    ('send_support_email',             true),
    ('invite_user_feedback',           true),
    ('get_user_tickets',               true),
    ('submit_support_case',            true),
    ('get_ticket_info',                true),
    ('agg_attachment_existing_ticket', true),
    ('accept_ticket',                  true),
    ('open_attachment_session',        true),
    ('create_ticket_note',             true),
    ('request_human_handoff',          true)
ON CONFLICT (tool_name) DO NOTHING;

-- 2) Asignar los roles por defecto a cada tool (no duplica si ya existen)
INSERT INTO tool_permission_roles (tool_permission_id, role)
SELECT tp.id, r.role
FROM tool_permissions tp
CROSS JOIN (VALUES ('DOCENTE'), ('ADMINISTRATIVO'), ('ENCARGATURA')) AS r(role)
WHERE tp.tool_name IN (
    'send_support_email',
    'invite_user_feedback',
    'get_user_tickets',
    'submit_support_case',
    'get_ticket_info',
    'agg_attachment_existing_ticket',
    'accept_ticket',
    'open_attachment_session',
    'create_ticket_note',
    'request_human_handoff'
)
AND NOT EXISTS (
    SELECT 1
    FROM tool_permission_roles tpr
    WHERE tpr.tool_permission_id = tp.id
      AND tpr.role = r.role
);

-- 3) Verificación (opcional): ver lo que quedó
-- SELECT tp.tool_name, tp.enabled, tpr.role
-- FROM tool_permissions tp
-- LEFT JOIN tool_permission_roles tpr ON tpr.tool_permission_id = tp.id
-- ORDER BY tp.tool_name, tpr.role;
