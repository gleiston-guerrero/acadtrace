-- E3: accion tecnica para eventos Docente en la bitacora institucional.
-- La operacion de dominio exacta se conserva dentro de contenido_canonico v1.

ALTER TYPE sga_principal.accion_auditoria_t
    ADD VALUE IF NOT EXISTS 'AUDITAR';
