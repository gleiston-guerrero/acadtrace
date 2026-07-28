-- Migracion 002: columna para el reloj logico de Lamport en historial_promocion
-- (ver ec.uteq.sga.secretaria.service.LamportClock).
--
-- lamport_ts NO es un timestamp real: es un contador entero que ordena
-- causalmente los eventos de registro de promocion sin depender del reloj de
-- pared de cada servicio (que puede desincronizarse entre Secretaria,
-- sga-principal y Supabase). fecha_registro sigue siendo la columna a usar
-- para "cuando pasó" en horario real; lamport_ts es solo para "que pasó antes".
--
-- Como correrla: psql "$DATABASE_URL" -f backend/src/main/resources/db/migrations/002_lamport_clock.sql
--
-- Nota: HistorialService.seedLamportClock() ya tolera que esta columna no
-- exista (arranca el reloj en 0 y loguea un warning), asi que el servicio no
-- se cae si esta migracion todavia no corrió. Lo que SI falla hasta correrla
-- es registrar una promocion nueva (INSERT referencia la columna).

ALTER TABLE sga_principal.historial_promocion
  ADD COLUMN IF NOT EXISTS lamport_ts BIGINT;

CREATE INDEX IF NOT EXISTS idx_historial_promocion_lamport_ts
  ON sga_principal.historial_promocion (lamport_ts);
