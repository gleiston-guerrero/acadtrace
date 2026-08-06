-- ============================================================================
-- seed_e3_500k_extras.sql
-- Complemento del seed principal:
--   - Marca origen_listado='CAS' a los 600 estudiantes seed
--   - Crea un representante por estudiante seed (mama/papa alternado)
--   - Enlaza estudiantes.id_representante en ambos esquemas
-- Idempotente: usa NOT EXISTS / cedulas dedicadas 135000XXXX.
-- ============================================================================
BEGIN;
SET client_min_messages = WARNING;

-- 1) Marcar origen = CAS a los 600 estudiantes seed en ambos esquemas
UPDATE sga_secretaria.estudiantes
   SET origen_listado = 'CAS'
 WHERE codigo_estudiante LIKE 'EST-SEED-%'
   AND (origen_listado IS NULL OR origen_listado <> 'CAS');

UPDATE sga_principal.estudiantes
   SET origen_listado = 'CAS'
 WHERE codigo_estudiante LIKE 'EST-SEED-%'
   AND (origen_listado IS NULL OR origen_listado <> 'CAS');

-- 2) Crear un representante por estudiante seed (mama/papa alternado)
-- Cedula representante = 135 + los ultimos 7 digitos de la cedula del estudiante
CREATE TEMP TABLE _seed_reps ON COMMIT DROP AS
SELECT
  e.id_estudiante,
  e.nombres  AS est_nombres,
  e.apellidos AS est_apellidos,
  '135' || SUBSTRING(e.cedula, 4, 7)                       AS cedula_rep,
  CASE WHEN (e.id_estudiante % 2) = 0 THEN 'Madre' ELSE 'Padre' END AS parentesco,
  CASE WHEN (e.id_estudiante % 2) = 0
       THEN (ARRAY['Maria','Ana','Rosa','Carmen','Lucia','Patricia','Elena','Sofia','Gabriela','Monica'])[1 + (e.id_estudiante % 10)]
       ELSE (ARRAY['Juan','Luis','Carlos','Jorge','Miguel','Pedro','Diego','Fernando','Ricardo','Andres'])[1 + (e.id_estudiante % 10)]
  END AS rep_nombres,
  SPLIT_PART(e.apellidos, ' ', 1) AS rep_apellidos,
  '09' || LPAD(((e.id_estudiante * 137) % 100000000)::text, 8, '0') AS telefono
FROM sga_secretaria.estudiantes e
WHERE e.codigo_estudiante LIKE 'EST-SEED-%'
  AND e.id_representante IS NULL;

-- 2.a) Insertar en sga_secretaria.representantes y capturar id
WITH ins AS (
  INSERT INTO sga_secretaria.representantes
    (cedula, nombres, apellidos, parentesco, telefono_principal, correo, direccion)
  SELECT r.cedula_rep, r.rep_nombres, r.rep_apellidos, r.parentesco,
         r.telefono,
         LOWER(r.rep_nombres || '.' || r.rep_apellidos || r.id_estudiante || '@correo-demo.ec'),
         'Direccion demo #' || r.id_estudiante
  FROM _seed_reps r
  RETURNING id_representante, cedula
)
UPDATE sga_secretaria.estudiantes s
   SET id_representante = i.id_representante
  FROM ins i, _seed_reps r
 WHERE r.cedula_rep = i.cedula
   AND s.id_estudiante = r.id_estudiante;

-- 2.b) Espejo en sga_principal.representantes con MISMO id
INSERT INTO sga_principal.representantes
  (id_representante, cedula, nombres, apellidos, parentesco, telefono_principal, correo, direccion)
SELECT sr.id_representante, sr.cedula, sr.nombres, sr.apellidos, sr.parentesco,
       sr.telefono_principal, sr.correo, sr.direccion
FROM sga_secretaria.representantes sr
JOIN sga_secretaria.estudiantes se ON se.id_representante = sr.id_representante
WHERE se.codigo_estudiante LIKE 'EST-SEED-%'
ON CONFLICT (id_representante) DO NOTHING;

SELECT setval('sga_principal.representantes_id_representante_seq',
              (SELECT MAX(id_representante) FROM sga_principal.representantes));

-- 2.c) Enlazar en sga_principal.estudiantes tambien
UPDATE sga_principal.estudiantes p
   SET id_representante = s.id_representante
  FROM sga_secretaria.estudiantes s
 WHERE p.id_estudiante = s.id_estudiante
   AND s.codigo_estudiante LIKE 'EST-SEED-%'
   AND p.id_representante IS NULL;

COMMIT;

-- Verificacion
SELECT origen_listado, COUNT(*) FROM sga_secretaria.estudiantes GROUP BY 1 ORDER BY 1 NULLS LAST;
SELECT 'representantes_secretaria', COUNT(*) FROM sga_secretaria.representantes
UNION ALL SELECT 'representantes_principal', COUNT(*) FROM sga_principal.representantes
UNION ALL SELECT 'estudiantes_seed_con_rep',
  COUNT(*) FROM sga_principal.estudiantes WHERE codigo_estudiante LIKE 'EST-SEED-%' AND id_representante IS NOT NULL;
