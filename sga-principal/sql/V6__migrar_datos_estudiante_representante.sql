-- V6: Migra los datos REALES de Estudiante y Representante desde
-- sga_principal (la copia vieja, congelada) hacia sga_secretaria (el
-- esquema nuevo creado por V5, donde el codigo actual lee y escribe).
--
-- Alcance deliberadamente limitado: SOLO estudiantes y representantes.
-- Matricula se queda intacta en sga_principal.matriculas -- NO se toca ni
-- se copia -- porque sga_docente (asistencias, calificaciones, promedios,
-- seguimiento_academico, resumen_asistencia) tiene llaves foraneas directas
-- hacia sga_principal.matriculas y mover esa tabla rompería ese
-- microservicio. Esa decision se revisa en otra sesion, coordinada con
-- quien mantiene sga_docente.
--
-- Que hace este script, en orden:
--   1. Copia representantes (preservando id_representante).
--   2. Copia estudiantes (preservando id_estudiante; su FK a
--      representante ya apunta a sga_secretaria.representantes, ver V5).
--   3. Reajusta las secuencias de sga_secretaria para que el proximo
--      INSERT sin id explicito no choque con los ids recien copiados.
--   4. Verifica (con un bloque DO que aborta si algo no cuadra) que todas
--      las matriculas/fichas/historial existentes sigan encontrando su
--      estudiante correspondiente en la copia nueva antes de continuar.
--   5. Actualiza las FK de sga_principal.matriculas, historial_promocion y
--      fichas_estudiante para que apunten a sga_secretaria.estudiantes en
--      vez de sga_principal.estudiantes (mismo patron que las migraciones
--      003/004 ya aplicadas en el entorno de pruebas local).
--
-- sga_principal.estudiantes y sga_principal.representantes NO se borran
-- ni se modifican -- quedan como respaldo de solo lectura. Si mas
-- adelante se decide eliminarlas, es una decision aparte y deliberada.
--
-- Requiere haber corrido V5 antes (crea sga_secretaria.estudiantes/
-- representantes, ambas vacias).
--
-- Como correrla: psql "$DATABASE_URL" -f sql/V6__migrar_datos_estudiante_representante.sql
-- Se recomienda correrla completa de una sola vez (usa una transaccion
-- implicita por bloque DO): si el bloque de verificacion falla, aborta
-- antes de tocar ninguna FK.

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Representantes
-- ---------------------------------------------------------------------------
INSERT INTO sga_secretaria.representantes
  (id_representante, cedula, nombres, apellidos, parentesco, telefono_principal,
   telefono_alt, correo, direccion, fecha_creacion, fecha_actualizacion)
SELECT
  id_representante, cedula, nombres, apellidos, parentesco, telefono_principal,
  telefono_alt, correo, direccion, fecha_creacion, fecha_actualizacion
FROM sga_principal.representantes
ON CONFLICT (id_representante) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Estudiantes
-- ---------------------------------------------------------------------------
INSERT INTO sga_secretaria.estudiantes
  (id_estudiante, cedula, codigo_estudiante, nombres, apellidos, fecha_nacimiento,
   genero, direccion, telefono, telefono_alt, correo, discapacidad, tipo_discapacidad,
   porcentaje_disc, id_representante, origen_listado, estado, foto_url, creado_por,
   fecha_creacion, fecha_actualizacion, carnet_conadis, nacionalidad, etnia,
   lugar_nacimiento, vive_con, numeros_hermanos, beneficio_social)
SELECT
  id_estudiante, cedula, codigo_estudiante, nombres, apellidos, fecha_nacimiento,
  genero, direccion, telefono, telefono_alt, correo, discapacidad, tipo_discapacidad,
  porcentaje_disc, id_representante, origen_listado, estado, foto_url, creado_por,
  fecha_creacion, fecha_actualizacion, carnet_conadis, nacionalidad, etnia,
  lugar_nacimiento, vive_con, numeros_hermanos, beneficio_social
FROM sga_principal.estudiantes
ON CONFLICT (id_estudiante) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Reajustar secuencias (para que el proximo estudiante/representante
--    nuevo, creado por el propio codigo, siga desde el numero correcto)
-- ---------------------------------------------------------------------------
SELECT setval('sga_secretaria.estudiantes_id_estudiante_seq',
              GREATEST((SELECT COALESCE(MAX(id_estudiante), 1) FROM sga_secretaria.estudiantes),
                        (SELECT last_value FROM sga_principal.estudiantes_id_estudiante_seq)));

SELECT setval('sga_secretaria.representantes_id_representante_seq',
              GREATEST((SELECT COALESCE(MAX(id_representante), 1) FROM sga_secretaria.representantes),
                        (SELECT last_value FROM sga_principal.representantes_id_representante_seq)));

-- ---------------------------------------------------------------------------
-- 4. Verificacion: aborta toda la transaccion si algo no cuadra antes de
--    tocar ninguna FK.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  huerfanas_matricula   integer;
  huerfanas_historial   integer;
  huerfanas_ficha       integer;
  total_estudiantes_ppal integer;
  total_estudiantes_sec  integer;
BEGIN
  SELECT count(*) INTO total_estudiantes_ppal FROM sga_principal.estudiantes;
  SELECT count(*) INTO total_estudiantes_sec FROM sga_secretaria.estudiantes;
  IF total_estudiantes_sec < total_estudiantes_ppal THEN
    RAISE EXCEPTION 'Copia incompleta: sga_principal.estudiantes tiene % filas, sga_secretaria.estudiantes solo tiene %',
      total_estudiantes_ppal, total_estudiantes_sec;
  END IF;

  SELECT count(*) INTO huerfanas_matricula
  FROM sga_principal.matriculas m
  WHERE NOT EXISTS (SELECT 1 FROM sga_secretaria.estudiantes e WHERE e.id_estudiante = m.id_estudiante);
  IF huerfanas_matricula > 0 THEN
    RAISE EXCEPTION 'Hay % matriculas cuyo id_estudiante no existe en sga_secretaria.estudiantes -- abortando', huerfanas_matricula;
  END IF;

  SELECT count(*) INTO huerfanas_historial
  FROM sga_principal.historial_promocion h
  WHERE NOT EXISTS (SELECT 1 FROM sga_secretaria.estudiantes e WHERE e.id_estudiante = h.id_estudiante);
  IF huerfanas_historial > 0 THEN
    RAISE EXCEPTION 'Hay % filas de historial_promocion cuyo id_estudiante no existe en sga_secretaria.estudiantes -- abortando', huerfanas_historial;
  END IF;

  SELECT count(*) INTO huerfanas_ficha
  FROM sga_principal.fichas_estudiante f
  WHERE NOT EXISTS (SELECT 1 FROM sga_secretaria.estudiantes e WHERE e.id_estudiante = f.id_estudiante);
  IF huerfanas_ficha > 0 THEN
    RAISE EXCEPTION 'Hay % fichas_estudiante cuyo id_estudiante no existe en sga_secretaria.estudiantes -- abortando', huerfanas_ficha;
  END IF;

  RAISE NOTICE 'Verificacion OK: % estudiantes copiados, 0 filas huerfanas en matriculas/historial/fichas.', total_estudiantes_sec;
END $$;

-- ---------------------------------------------------------------------------
-- 5. Apuntar las FK de las tablas que se quedan en sga_principal hacia el
--    estudiante real (sga_secretaria.estudiantes), igual que las
--    migraciones 003/004 ya aplicadas en el entorno de pruebas local.
-- ---------------------------------------------------------------------------
ALTER TABLE sga_principal.matriculas
  DROP CONSTRAINT matriculas_id_estudiante_fkey,
  ADD CONSTRAINT matriculas_id_estudiante_fkey
    FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes(id_estudiante);

ALTER TABLE sga_principal.historial_promocion
  DROP CONSTRAINT historial_id_estudiante_fkey,
  ADD CONSTRAINT historial_id_estudiante_fkey
    FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes(id_estudiante);

ALTER TABLE sga_principal.fichas_estudiante
  DROP CONSTRAINT fichas_estudiante_id_estudiante_fkey,
  ADD CONSTRAINT fichas_estudiante_id_estudiante_fkey
    FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes(id_estudiante) ON DELETE CASCADE;

COMMIT;

-- Nota post-migracion: sga_principal.estudiantes y sga_principal.representantes
-- quedan intactas como respaldo de solo lectura. El codigo actual (JPA de
-- sga-principal, gRPC de sga-secretaria) ya no las usa para nada -- toda
-- lectura/escritura de Estudiante/Representante va contra sga_secretaria.
