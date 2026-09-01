-- Reutiliza cuentas existentes cuando el correo del representante coincide;
-- los casos sin coincidencia quedan explícitamente pendientes de vinculación administrativa.
UPDATE sga_principal.representantes r
SET id_usuario = u.id_usuario
FROM sga_principal.usuarios u
WHERE r.id_usuario IS NULL
  AND r.correo IS NOT NULL
  AND LOWER(r.correo) = LOWER(u.correo);

INSERT INTO sga_principal.usuario_roles (id_usuario, id_rol)
SELECT r.id_usuario, rol.id_rol
FROM sga_principal.representantes r
JOIN sga_principal.roles rol ON rol.nombre = 'ROLE_REPRESENTANTE'
WHERE r.id_usuario IS NOT NULL
ON CONFLICT DO NOTHING;
