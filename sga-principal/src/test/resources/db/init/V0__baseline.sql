-- =============================================================================
-- V0__baseline.sql  (solo para tests con Testcontainers)
--
-- Crea los schemas y todos los tipos enum nativos de PostgreSQL que las
-- entidades JPA necesitan a traves de @ColumnTransformer.
-- Las tablas las genera Hibernate con ddl-auto=create (configurado en el test).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Schemas
CREATE SCHEMA IF NOT EXISTS sga_principal;
CREATE SCHEMA IF NOT EXISTS sga_docente;

-- 2. Tipos enum nativos de sga_principal

DO $$ BEGIN
    CREATE TYPE sga_principal.accion_auditoria_t AS ENUM (
        'CREAR', 'EDITAR', 'ELIMINAR',
        'LOGIN', 'LOGOUT',
        'CAMBIO_PASSWORD', 'BLOQUEO', 'DESBLOQUEO',
        'LOGIN_EXITOSO', 'LOGIN_FALLIDO',
        'ROL_ASIGNADO', 'LLAMADA_GRPC', 'CONSULTAR',
        'MODIFICAR', 'INSERT'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.estado_matricula_t AS ENUM (
        'ACTIVA', 'RETIRADA', 'TRASLADADA', 'PROMOVIDA', 'REPROBADA'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.genero_t AS ENUM (
        'MASCULINO', 'FEMENINO', 'OTRO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.dia_semana_t AS ENUM (
        'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.nivel_educativo_t AS ENUM (
        'INICIAL_1', 'INICIAL_2', 'PREPARATORIA',
        'BASICA_ELEMENTAL', 'BASICA_MEDIA', 'BASICA_SUPERIOR'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.origen_listado_t AS ENUM (
        'NUEVO', 'TRANSFERIDO_INTERNO', 'TRANSFERIDO_EXTERNO',
        'REPITENTE', 'REINGRESO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.resultado_promocion_t AS ENUM (
        'PROMOVIDO', 'REPROBADO', 'RETIRADO', 'TRASLADADO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.tipo_asignacion_t AS ENUM (
        'TITULAR', 'ESPECIALIZADO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.tipo_documento_t AS ENUM (
        'PARTIDA_NACIMIENTO', 'CEDULA_IDENTIDAD', 'FOTO',
        'INFORME_PREVIO', 'CERTIFICADO_MEDICO',
        'CARNET_DISCAPACIDAD', 'COMPROBANTE_DOMICILIO', 'OTRO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.tipo_escala_t AS ENUM (
        'CUANTITATIVA', 'CUALITATIVA', 'MIXTA'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3. Tipos enum nativos de sga_docente (requerido por PeriodoEvaluacion.java)

DO $$ BEGIN
    CREATE TYPE sga_docente.tipo_periodo_t AS ENUM (
        'PRIMER_TRIMESTRE', 'SEGUNDO_TRIMESTRE', 'TERCER_TRIMESTRE'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
