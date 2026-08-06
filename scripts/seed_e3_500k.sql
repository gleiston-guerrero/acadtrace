-- ============================================================================
-- seed_e3_500k.sql
-- Poblado masivo para cumplir el nucleo de Entrega 3 (dataset >= 500.000).
-- Inserta:
--   1) Trimestres 2 y 3 en sga_docente.periodos_evaluacion (faltaban)
--   2) 50 estudiantes nuevos por paralelo A de cada uno de los 12 grados (600)
--   3) Matriculas de los 600 nuevos en el paralelo A del ano lectivo 1
--   4) 918 actividades = 34 asignaciones x 3 periodos x 9 tipos_aporte
--   5) Calificaciones de cada actividad para cada matricula de su paralelo
--   6) Asistencias diarias L-V para cada matricula x asignacion x dia
--
-- Todos los estudiantes seed usan codigo_estudiante 'EST-SEED-XXXXXX'
-- y cedulas 1250000001..1250000600 (provincia 12 = Los Rios).
-- Para revertir: DELETE ... WHERE codigo_estudiante LIKE 'EST-SEED-%' (cascada
-- manual segun FK).
-- ============================================================================
BEGIN;

SET client_min_messages = WARNING;

-- ---------------------------------------------------------------------------
-- 1) PERIODOS DE EVALUACION FALTANTES EN sga_docente
-- ---------------------------------------------------------------------------
INSERT INTO sga_docente.periodos_evaluacion (id_ano_lectivo, tipo, nombre, fecha_inicio, fecha_fin, activo)
SELECT 1, 'SEGUNDO_TRIMESTRE', 'Segundo Trimestre 2026-2027', DATE '2026-08-11', DATE '2026-11-19', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sga_docente.periodos_evaluacion WHERE id_ano_lectivo=1 AND tipo='SEGUNDO_TRIMESTRE');

INSERT INTO sga_docente.periodos_evaluacion (id_ano_lectivo, tipo, nombre, fecha_inicio, fecha_fin, activo)
SELECT 1, 'TERCER_TRIMESTRE', 'Tercer Trimestre 2026-2027', DATE '2026-11-20', DATE '2027-02-28', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sga_docente.periodos_evaluacion WHERE id_ano_lectivo=1 AND tipo='TERCER_TRIMESTRE');

-- ---------------------------------------------------------------------------
-- 2) ESTUDIANTES NUEVOS (600)  +  3) MATRICULAS (600)
-- ---------------------------------------------------------------------------
-- Tabla temporal con los datos generados
CREATE TEMP TABLE _seed_nuevos ON COMMIT DROP AS
WITH nombres(n) AS (VALUES
 ('Mateo'),('Sofia'),('Sebastian'),('Emilia'),('Nicolas'),('Valentina'),('Santiago'),('Camila'),
 ('Alejandro'),('Isabella'),('Diego'),('Martina'),('Daniel'),('Renata'),('Emiliano'),('Antonella'),
 ('Joaquin'),('Luciana'),('Benjamin'),('Victoria'),('Julian'),('Emma'),('Adrian'),('Mia'),
 ('Ivan'),('Regina'),('Cristian'),('Paula'),('Andres'),('Fernanda')
),
apellidos(a) AS (VALUES
 ('Vera'),('Zambrano'),('Cedeno'),('Mendoza'),('Alcivar'),('Loor'),('Macias'),('Pincay'),
 ('Bravo'),('Palma'),('Chavez'),('Moreira'),('Vasquez'),('Solorzano'),('Delgado'),('Ponce'),
 ('Intriago'),('Cevallos'),('Andrade'),('Molina'),('Rodriguez'),('Garcia'),('Lopez'),('Sanchez'),
 ('Gomez'),('Torres'),('Ramirez'),('Diaz'),('Reyes'),('Ortiz')
),
grados_a AS (
  SELECT p.id_paralelo, p.id_grado, ROW_NUMBER() OVER (ORDER BY g.orden) AS ord_grado
  FROM sga_principal.paralelos p
  JOIN sga_principal.grados g USING (id_grado)
  WHERE p.letra = 'A'
)
SELECT
  1250000000 + (g.ord_grado - 1) * 50 + s AS cedula_num,
  LPAD((1250000000 + (g.ord_grado - 1) * 50 + s)::text, 10, '0') AS cedula,
  'EST-SEED-' || LPAD(((g.ord_grado - 1) * 50 + s)::text, 6, '0') AS codigo_estudiante,
  (SELECT n FROM nombres OFFSET (((g.ord_grado * 7 + s) % 30)) LIMIT 1) AS nombres,
  (SELECT a FROM apellidos OFFSET ((((g.ord_grado - 1) * 3 + s) % 30)) LIMIT 1)
    || ' ' ||
  (SELECT a FROM apellidos OFFSET (((s * 11 + g.ord_grado) % 30)) LIMIT 1) AS apellidos,
  (DATE '2010-01-01' + ((g.ord_grado - 1) * 365 + (s * 7)) * INTERVAL '1 day')::date AS fecha_nacimiento,
  CASE WHEN s % 2 = 0 THEN 'MASCULINO' ELSE 'FEMENINO' END AS genero,
  g.id_grado,
  g.id_paralelo
FROM grados_a g
CROSS JOIN generate_series(1, 50) s;

-- 2.a) Inserta primero en sga_secretaria (owner de la FK de matriculas)
WITH ins_sec AS (
  INSERT INTO sga_secretaria.estudiantes
    (cedula, codigo_estudiante, nombres, apellidos, fecha_nacimiento, genero, estado)
  SELECT cedula, codigo_estudiante, nombres, apellidos, fecha_nacimiento, genero, 'ACTIVO'
  FROM _seed_nuevos
  RETURNING id_estudiante, cedula
)
-- 2.b) Refleja los mismos id en sga_principal.estudiantes (patron existente)
INSERT INTO sga_principal.estudiantes
  (id_estudiante, cedula, codigo_estudiante, nombres, apellidos, fecha_nacimiento, genero, estado, discapacidad)
SELECT i.id_estudiante, n.cedula, n.codigo_estudiante, n.nombres, n.apellidos,
       n.fecha_nacimiento, n.genero, 'ACTIVO', FALSE
FROM ins_sec i
JOIN _seed_nuevos n USING (cedula);

-- Sincroniza la secuencia principal (los inserts explicitos no la avanzaron)
SELECT setval('sga_principal.estudiantes_id_estudiante_seq',
              (SELECT MAX(id_estudiante) FROM sga_principal.estudiantes));

-- 3) Matriculas para los estudiantes seed en su paralelo A / ano lectivo 1
INSERT INTO sga_principal.matriculas
  (id_estudiante, id_grado, id_paralelo, id_ano_lectivo, fecha_registro, estado)
SELECT s.id_estudiante, n.id_grado, n.id_paralelo, 1, DATE '2026-05-01', 'ACTIVA'
FROM sga_secretaria.estudiantes s
JOIN _seed_nuevos n USING (cedula);

-- ---------------------------------------------------------------------------
-- 4) ACTIVIDADES: 34 asignaciones x 3 periodos x 9 tipos_aporte = 918
-- ---------------------------------------------------------------------------
INSERT INTO sga_docente.actividades
  (id_asignacion, id_periodo, tipo, nombre, ponderacion, nota_maxima, es_sumativa, fecha_creacion)
SELECT
  a.id_asignacion,
  pd.id_periodo,
  CASE ta.tipo_evaluacion WHEN 'SUMATIVA' THEN 'EXAMEN_TRIMESTRAL' ELSE 'TAREA' END::sga_docente.tipo_actividad_t,
  ta.nombre || ' - ' || pd.nombre,
  CASE ta.tipo_evaluacion WHEN 'SUMATIVA' THEN 30.0 ELSE 10.0 END,
  10.0,
  (ta.tipo_evaluacion = 'SUMATIVA'),
  NOW()
FROM sga_principal.asignaciones a
CROSS JOIN sga_docente.periodos_evaluacion pd
CROSS JOIN sga_principal.tipos_aporte ta
WHERE a.id_ano_lectivo = 1
  AND pd.id_ano_lectivo = 1
  AND ta.id_ano_lectivo = 1;

-- ---------------------------------------------------------------------------
-- 5) CALIFICACIONES: por cada actividad, 1 nota por matricula del paralelo
-- ---------------------------------------------------------------------------
INSERT INTO sga_docente.calificaciones
  (id_actividad, id_matricula, nota, registrado_por, fecha_registro, fecha_actualizacion)
SELECT
  act.id_actividad,
  m.id_matricula,
  ROUND((5 + random() * 5)::numeric, 2),   -- notas 5.00..10.00
  1,
  NOW(), NOW()
FROM sga_docente.actividades act
JOIN sga_principal.asignaciones a ON a.id_asignacion = act.id_asignacion
JOIN sga_principal.matriculas m
  ON m.id_paralelo = a.id_paralelo
 AND m.id_ano_lectivo = a.id_ano_lectivo
ON CONFLICT (id_actividad, id_matricula) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6) ASISTENCIAS: por cada matricula x asignacion x dia habil del periodo
-- ---------------------------------------------------------------------------
INSERT INTO sga_docente.asistencias
  (id_matricula, id_asignacion, id_periodo, fecha, estado, registrado_por,
   fecha_registro, fecha_actualizacion)
SELECT
  m.id_matricula,
  a.id_asignacion,
  pd.id_periodo,
  d::date,
  CASE
    WHEN (random() * 100)::int < 92 THEN 'PRESENTE'
    WHEN (random() * 100)::int < 97 THEN 'AUSENTE'
    WHEN (random() * 100)::int < 99 THEN 'ATRASO'
    ELSE 'JUSTIFICADO'
  END::sga_docente.estado_asistencia_t,
  1,
  NOW(), NOW()
FROM sga_principal.asignaciones a
JOIN sga_principal.matriculas m
  ON m.id_paralelo = a.id_paralelo
 AND m.id_ano_lectivo = a.id_ano_lectivo
JOIN sga_docente.periodos_evaluacion pd
  ON pd.id_ano_lectivo = a.id_ano_lectivo
JOIN LATERAL generate_series(pd.fecha_inicio, pd.fecha_fin, INTERVAL '1 day') d
  ON EXTRACT(dow FROM d) BETWEEN 1 AND 5
WHERE a.id_ano_lectivo = 1
ON CONFLICT (id_matricula, id_asignacion, fecha) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Actualiza estadisticas
-- ---------------------------------------------------------------------------
COMMIT;

VACUUM ANALYZE sga_docente.asistencias;
VACUUM ANALYZE sga_docente.calificaciones;
VACUUM ANALYZE sga_docente.actividades;
VACUUM ANALYZE sga_principal.matriculas;
VACUUM ANALYZE sga_principal.estudiantes;
VACUUM ANALYZE sga_secretaria.estudiantes;

-- ---------------------------------------------------------------------------
-- Verificacion final
-- ---------------------------------------------------------------------------
SELECT 'estudiantes_principal'  AS tabla, COUNT(*) FROM sga_principal.estudiantes
UNION ALL SELECT 'estudiantes_secretaria', COUNT(*) FROM sga_secretaria.estudiantes
UNION ALL SELECT 'matriculas',             COUNT(*) FROM sga_principal.matriculas
UNION ALL SELECT 'actividades',            COUNT(*) FROM sga_docente.actividades
UNION ALL SELECT 'calificaciones',         COUNT(*) FROM sga_docente.calificaciones
UNION ALL SELECT 'asistencias',            COUNT(*) FROM sga_docente.asistencias
UNION ALL SELECT 'TOTAL_BD_APROX',
  (SELECT SUM(n_live_tup)::bigint FROM pg_stat_user_tables
    WHERE schemaname IN ('sga_principal','sga_docente','sga_secretaria'));
