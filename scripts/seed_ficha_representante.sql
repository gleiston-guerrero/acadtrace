-- ============================================================================
-- seed_ficha_representante.sql
-- Extiende la tabla representantes (ambos esquemas) con los campos de una
-- ficha real: fecha_nacimiento, genero, estado_civil, nacionalidad,
-- ocupacion, lugar_trabajo, telefono_trabajo, cargo, nivel_instruccion,
-- ingreso_mensual, convive_con_estudiante, contacto_emergencia_nombre,
-- contacto_emergencia_telefono, observaciones. Rellena con datos sinteticos
-- para los 1.277 representantes existentes. Idempotente.
-- ============================================================================
BEGIN;
SET client_min_messages = WARNING;

-- ---------- SECRETARIA ----------
ALTER TABLE sga_secretaria.representantes
  ADD COLUMN IF NOT EXISTS fecha_nacimiento             date,
  ADD COLUMN IF NOT EXISTS genero                       varchar(20),
  ADD COLUMN IF NOT EXISTS estado_civil                 varchar(30),
  ADD COLUMN IF NOT EXISTS nacionalidad                 varchar(50),
  ADD COLUMN IF NOT EXISTS ocupacion                    varchar(100),
  ADD COLUMN IF NOT EXISTS lugar_trabajo                varchar(150),
  ADD COLUMN IF NOT EXISTS telefono_trabajo             varchar(20),
  ADD COLUMN IF NOT EXISTS cargo                        varchar(100),
  ADD COLUMN IF NOT EXISTS nivel_instruccion            varchar(50),
  ADD COLUMN IF NOT EXISTS ingreso_mensual              numeric(10,2),
  ADD COLUMN IF NOT EXISTS convive_con_estudiante       boolean,
  ADD COLUMN IF NOT EXISTS contacto_emergencia_nombre   varchar(150),
  ADD COLUMN IF NOT EXISTS contacto_emergencia_telefono varchar(20),
  ADD COLUMN IF NOT EXISTS observaciones                text;

-- ---------- PRINCIPAL (mirror) ----------
ALTER TABLE sga_principal.representantes
  ADD COLUMN IF NOT EXISTS fecha_nacimiento             date,
  ADD COLUMN IF NOT EXISTS genero                       varchar(20),
  ADD COLUMN IF NOT EXISTS estado_civil                 varchar(30),
  ADD COLUMN IF NOT EXISTS nacionalidad                 varchar(50),
  ADD COLUMN IF NOT EXISTS ocupacion                    varchar(100),
  ADD COLUMN IF NOT EXISTS lugar_trabajo                varchar(150),
  ADD COLUMN IF NOT EXISTS telefono_trabajo             varchar(20),
  ADD COLUMN IF NOT EXISTS cargo                        varchar(100),
  ADD COLUMN IF NOT EXISTS nivel_instruccion            varchar(50),
  ADD COLUMN IF NOT EXISTS ingreso_mensual              numeric(10,2),
  ADD COLUMN IF NOT EXISTS convive_con_estudiante       boolean,
  ADD COLUMN IF NOT EXISTS contacto_emergencia_nombre   varchar(150),
  ADD COLUMN IF NOT EXISTS contacto_emergencia_telefono varchar(20),
  ADD COLUMN IF NOT EXISTS observaciones                text;

-- ---------- RELLENAR SECRETARIA ----------
UPDATE sga_secretaria.representantes SET
  fecha_nacimiento = COALESCE(fecha_nacimiento,
                              (DATE '1975-01-01' + ((id_representante * 173) % 9000) * INTERVAL '1 day')::date),
  genero = COALESCE(NULLIF(genero,''),
                    CASE WHEN parentesco IN ('Madre','Abuela','Tia','Hermana') THEN 'FEMENINO'
                         WHEN parentesco IN ('Padre','Abuelo','Tio','Hermano') THEN 'MASCULINO'
                         ELSE (ARRAY['FEMENINO','MASCULINO'])[1 + (id_representante % 2)] END),
  estado_civil = COALESCE(NULLIF(estado_civil,''),
                          (ARRAY['Casado/a','Union libre','Soltero/a','Divorciado/a','Viudo/a'])[1 + (id_representante % 5)]),
  nacionalidad = COALESCE(NULLIF(nacionalidad,''), 'Ecuatoriana'),
  ocupacion = COALESCE(NULLIF(ocupacion,''),
                       (ARRAY['Agricultor','Comerciante','Docente','Empleado publico','Empleado privado',
                              'Ama de casa','Chofer','Albanil','Estilista','Enfermera',
                              'Contador','Mecanico','Vendedora','Pescador','Costurera'])[1 + (id_representante % 15)]),
  lugar_trabajo = COALESCE(NULLIF(lugar_trabajo,''),
                           (ARRAY['Mercado municipal','Ministerio de Salud','Escuela Fiscal',
                                  'Empresa Provincial','Cooperativa 24 de Mayo','Hospital IESS',
                                  'Comercial Local','Trabajo independiente','Fabrica Textil',
                                  'Municipio del Canton'])[1 + (id_representante % 10)]),
  telefono_trabajo = COALESCE(NULLIF(telefono_trabajo,''),
                              '05' || LPAD(((id_representante * 251) % 10000000)::text, 7, '0')),
  cargo = COALESCE(NULLIF(cargo,''),
                   (ARRAY['Operativo','Supervisor/a','Auxiliar','Jefe de area','Asistente',
                          'Independiente','Tecnico/a','Coordinador/a'])[1 + (id_representante % 8)]),
  nivel_instruccion = COALESCE(NULLIF(nivel_instruccion,''),
                                (ARRAY['Primaria','Secundaria','Bachillerato','Tecnico','Superior','Postgrado'])[1 + (id_representante % 6)]),
  ingreso_mensual = COALESCE(ingreso_mensual,
                             ROUND((450 + (id_representante % 25) * 55)::numeric, 2)),
  convive_con_estudiante = COALESCE(convive_con_estudiante,
                                    parentesco IN ('Madre','Padre','Abuela','Abuelo')),
  contacto_emergencia_nombre = COALESCE(NULLIF(contacto_emergencia_nombre,''),
                                        (ARRAY['Rosa Vera','Luis Zambrano','Carmen Mendoza','Ana Loor',
                                               'Pedro Cedeno','Maria Alcivar','Jorge Bravo','Sofia Delgado'])[1 + (id_representante % 8)]),
  contacto_emergencia_telefono = COALESCE(NULLIF(contacto_emergencia_telefono,''),
                                          '09' || LPAD(((id_representante * 313) % 100000000)::text, 8, '0')),
  observaciones = COALESCE(NULLIF(observaciones,''),
                           CASE WHEN id_representante % 4 = 0 THEN 'Autorizado a retirar al estudiante en cualquier horario.'
                                WHEN id_representante % 4 = 1 THEN 'Contactar preferentemente por la tarde.'
                                WHEN id_representante % 4 = 2 THEN 'Familia con seguimiento del DECE.'
                                ELSE 'Sin observaciones adicionales.' END),
  fecha_actualizacion = NOW();

-- ---------- ESPEJAR EN PRINCIPAL ----------
UPDATE sga_principal.representantes p SET
  fecha_nacimiento             = s.fecha_nacimiento,
  genero                       = s.genero,
  estado_civil                 = s.estado_civil,
  nacionalidad                 = s.nacionalidad,
  ocupacion                    = s.ocupacion,
  lugar_trabajo                = s.lugar_trabajo,
  telefono_trabajo             = s.telefono_trabajo,
  cargo                        = s.cargo,
  nivel_instruccion            = s.nivel_instruccion,
  ingreso_mensual              = s.ingreso_mensual,
  convive_con_estudiante       = s.convive_con_estudiante,
  contacto_emergencia_nombre   = s.contacto_emergencia_nombre,
  contacto_emergencia_telefono = s.contacto_emergencia_telefono,
  observaciones                = s.observaciones,
  fecha_actualizacion          = NOW()
FROM sga_secretaria.representantes s
WHERE p.id_representante = s.id_representante;

COMMIT;

-- Verificacion
SELECT 'representantes sin ocupacion'  AS metrica, COUNT(*) FROM sga_secretaria.representantes WHERE ocupacion IS NULL OR ocupacion=''
UNION ALL SELECT 'representantes sin nivel_instruccion', COUNT(*) FROM sga_secretaria.representantes WHERE nivel_instruccion IS NULL
UNION ALL SELECT 'representantes sin contacto_emergencia', COUNT(*) FROM sga_secretaria.representantes WHERE contacto_emergencia_telefono IS NULL
UNION ALL SELECT 'principal - reps con ocupacion', COUNT(*) FROM sga_principal.representantes WHERE ocupacion IS NOT NULL;
