-- ============================================================================
-- seed_completar_datos.sql
-- Rellena TODOS los NULL / vacios en estudiantes y representantes de ambos
-- esquemas (principal y secretaria). Idempotente: solo actualiza cuando la
-- columna esta NULL o en cadena vacia.
-- ============================================================================
BEGIN;
SET client_min_messages = WARNING;

-- ---------------------------------------------------------------------------
-- ESTUDIANTES: rellenar campos vacios en sga_secretaria
-- ---------------------------------------------------------------------------
UPDATE sga_secretaria.estudiantes SET
  telefono          = COALESCE(NULLIF(telefono,''),          '09' || LPAD(((id_estudiante*211+7) % 100000000)::text, 8, '0')),
  telefono_alt      = COALESCE(NULLIF(telefono_alt,''),      '05' || LPAD(((id_estudiante*97+3) % 10000000)::text, 7, '0')),
  correo            = COALESCE(NULLIF(correo,''),            LOWER(REPLACE(nombres,' ','') || '.' || REPLACE(SPLIT_PART(apellidos,' ',1),' ','') || id_estudiante || '@correo-demo.ec')),
  direccion         = COALESCE(NULLIF(direccion,''),         'Av. Principal ' || id_estudiante || ', Sector ' || (id_estudiante % 15 + 1)),
  fecha_nacimiento  = COALESCE(fecha_nacimiento,             (DATE '2010-01-01' + ((id_estudiante % 3650)) * INTERVAL '1 day')::date),
  genero            = COALESCE(NULLIF(genero,''),            CASE WHEN (id_estudiante % 2)=0 THEN 'MASCULINO' ELSE 'FEMENINO' END),
  nacionalidad      = COALESCE(NULLIF(nacionalidad,''),      'Ecuatoriana'),
  etnia             = COALESCE(NULLIF(etnia,''),             (ARRAY['Mestiza','Indigena','Afroecuatoriana','Blanca','Montubia'])[1 + (id_estudiante % 5)]),
  lugar_nacimiento  = COALESCE(NULLIF(lugar_nacimiento,''),  (ARRAY['Portoviejo','Manta','Quevedo','Guayaquil','Quito','Chone','Jipijapa','Babahoyo'])[1 + (id_estudiante % 8)]),
  vive_con          = COALESCE(NULLIF(vive_con,''),          (ARRAY['Ambos padres','Madre','Padre','Abuelos','Tios'])[1 + (id_estudiante % 5)]),
  numeros_hermanos  = COALESCE(numeros_hermanos,             (id_estudiante % 5)::smallint),
  beneficio_social  = COALESCE(beneficio_social,             (id_estudiante % 4 = 0)),
  origen_listado    = COALESCE(NULLIF(origen_listado,''),    'MANUAL'),
  fecha_actualizacion = NOW();

-- Espejo en sga_principal.estudiantes (mismos ids)
UPDATE sga_principal.estudiantes p SET
  telefono          = s.telefono,
  telefono_alt      = s.telefono_alt,
  correo            = s.correo,
  direccion         = s.direccion,
  fecha_nacimiento  = COALESCE(p.fecha_nacimiento, s.fecha_nacimiento),
  genero            = COALESCE(NULLIF(p.genero,''), s.genero),
  nacionalidad      = COALESCE(NULLIF(p.nacionalidad,''), s.nacionalidad),
  etnia             = COALESCE(NULLIF(p.etnia,''), s.etnia),
  lugar_nacimiento  = COALESCE(NULLIF(p.lugar_nacimiento,''), s.lugar_nacimiento),
  vive_con          = COALESCE(NULLIF(p.vive_con,''), s.vive_con),
  numeros_hermanos  = COALESCE(p.numeros_hermanos, s.numeros_hermanos),
  beneficio_social  = COALESCE(p.beneficio_social, s.beneficio_social),
  origen_listado    = COALESCE(NULLIF(p.origen_listado,''), s.origen_listado),
  fecha_actualizacion = NOW()
FROM sga_secretaria.estudiantes s
WHERE p.id_estudiante = s.id_estudiante;

-- ---------------------------------------------------------------------------
-- REPRESENTANTES: rellenar campos vacios
-- ---------------------------------------------------------------------------
UPDATE sga_secretaria.representantes SET
  telefono_alt      = COALESCE(NULLIF(telefono_alt,''),  '05' || LPAD(((id_representante*137+13) % 10000000)::text, 7, '0')),
  correo            = COALESCE(NULLIF(correo,''),        LOWER(REPLACE(nombres,' ','') || '.' || REPLACE(SPLIT_PART(apellidos,' ',1),' ','') || id_representante || '@correo-demo.ec')),
  direccion         = COALESCE(NULLIF(direccion,''),     'Av. Familiar #' || id_representante || ', barrio ' || (id_representante % 20 + 1)),
  fecha_actualizacion = NOW();

UPDATE sga_principal.representantes p SET
  telefono_alt = s.telefono_alt,
  correo       = s.correo,
  direccion    = s.direccion,
  fecha_actualizacion = NOW()
FROM sga_secretaria.representantes s
WHERE p.id_representante = s.id_representante;

COMMIT;

-- Verificacion
SELECT 'estudiantes sin telefono'    AS metrica, COUNT(*) FROM sga_secretaria.estudiantes WHERE telefono IS NULL OR telefono=''
UNION ALL SELECT 'estudiantes sin correo',            COUNT(*) FROM sga_secretaria.estudiantes WHERE correo IS NULL OR correo=''
UNION ALL SELECT 'estudiantes sin direccion',         COUNT(*) FROM sga_secretaria.estudiantes WHERE direccion IS NULL OR direccion=''
UNION ALL SELECT 'estudiantes sin fecha_nacimiento',  COUNT(*) FROM sga_secretaria.estudiantes WHERE fecha_nacimiento IS NULL
UNION ALL SELECT 'estudiantes sin representante',     COUNT(*) FROM sga_secretaria.estudiantes WHERE id_representante IS NULL
UNION ALL SELECT 'representantes sin correo',         COUNT(*) FROM sga_secretaria.representantes WHERE correo IS NULL OR correo=''
UNION ALL SELECT 'representantes sin direccion',      COUNT(*) FROM sga_secretaria.representantes WHERE direccion IS NULL OR direccion='';
