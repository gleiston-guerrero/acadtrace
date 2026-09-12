ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS hash_anterior VARCHAR(64),
    ADD COLUMN IF NOT EXISTS hash_actual VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reloj_lamport BIGINT;

CREATE INDEX IF NOT EXISTS idx_auditoria_hash_actual
    ON sga_principal.auditoria (hash_actual);
