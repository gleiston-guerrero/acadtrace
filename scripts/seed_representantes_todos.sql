-- ============================================================================
-- seed_representantes_todos.sql
-- Crea un representante para cada estudiante que aun no tenga uno asignado
-- (aplica a los 677 estudiantes originales). Idempotente: solo procesa
-- estudiantes con id_representante IS NULL.
-- Cedula del representante: 140XXXXXXX (Manabi) derivada del id_estudiante
-- para evitar colisiones con los seed anteriores (135XXXXXXX).
-- ============================================================================
BEGIN;
SET client_min_messages = WARNING;

CREATE TEMP TABLE _reps_todos ON COMMIT DROP AS
SELECT
  e.id_estudiante,
  e.apellidos AS est_apellidos,
  '140' || LPAD((1000000 + e.id_estudiante)::text, 7, '0') AS cedula_rep,
  CASE WHEN (e.id_estudiante % 2) = 0 THEN 'Madre' ELSE 'Padre' END AS parentesco,
  CASE WHEN (e.id_estudiante % 2) = 0
       THEN (ARRAY['Maria','Ana','Rosa','Carmen','Lucia','Patricia','Elena','Sofia','Gabriela','Monica','Veronica','Cecilia','Alexandra','Diana','Silvia'])[1 + (e.id_estudiante % 15)]
       ELSE (ARRAY['Juan','Luis','Carlos','Jorge','Miguel','Pedro','Diego','Fernando','Ricardo','Andres','Manuel','Roberto','Javier','Cristian','Marco'])[1 + (e.id_estudiante % 15)]
  END AS rep_nombres,
  SPLIT_PART(TRIM(e.apellidos), ' ', 1) AS rep_apellidos,
  '09' || LPAD(((e.id_estudiante * 179 + 41) % 100000000)::text, 8, '0') AS telefono,
  'Direccion registrada #' || e.id_estudiante || ', Manabi' AS direccion
FROM sga_secretaria.estudiantes e
WHERE e.id_representante IS NULL;

-- Insertar en sga_secretaria.representantes y enlazar con el estudiante
WITH ins AS (
  INSERT INTO sga_secretaria.representantes
    (cedula, nombres, apellidos, parentesco, telefono_principal, correo, direccion)
  SELECT r.cedula_rep, r.rep_nombres, r.rep_apellidos, r.parentesco, r.telefono,
         LOWER(r.rep_nombres || '.' || REPLACE(r.rep_apellidos,' ','') || r.id_estudiante || '@correo.demo.ec'),
         r.direccion
  FROM _reps_todos r
  RETURNING id_representante, cedula
)
UPDATE sga_secretaria.estudiantes s
   SET id_representante = i.id_representante
  FROM ins i, _reps_todos r
 WHERE r.cedula_rep = i.cedula
   AND s.id_estudiante = r.id_estudiante;

-- Espejo en sga_principal.representantes con mismo id
INSERT INTO sga_principal.representantes
  (id_representante, cedula, nombres, apellidos, parentesco, telefono_principal, correo, direccion)
SELECT sr.id_representante, sr.cedula, sr.nombres, sr.apellidos, sr.parentesco,
       sr.telefono_principal, sr.correo, sr.direccion
FROM sga_secretaria.representantes sr
WHERE sr.cedula LIKE '140%'
ON CONFLICT (id_representante) DO NOTHING;

SELECT setval('sga_principal.representantes_id_representante_seq',
              (SELECT MAX(id_representante) FROM sga_principal.representantes));

-- Enlazar en sga_principal.estudiantes tambien
UPDATE sga_principal.estudiantes p
   SET id_representante = s.id_representante
  FROM sga_secretaria.estudiantes s
 WHERE p.id_estudiante = s.id_estudiante
   AND p.id_representante IS NULL
   AND s.id_representante IS NOT NULL;

COMMIT;

-- Verificacion
SELECT 'estudiantes_totales'      AS metrica, COUNT(*) FROM sga_secretaria.estudiantes
UNION ALL SELECT 'con_representante',       COUNT(*) FROM sga_secretaria.estudiantes WHERE id_representante IS NOT NULL
UNION ALL SELECT 'sin_representante',       COUNT(*) FROM sga_secretaria.estudiantes WHERE id_representante IS NULL
UNION ALL SELECT 'representantes_sec',      COUNT(*) FROM sga_secretaria.representantes
UNION ALL SELECT 'representantes_ppal',     COUNT(*) FROM sga_principal.representantes;
