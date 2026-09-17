-- =============================================================================
-- baseline_flyway_v8.sql
--
-- Linea base real V8 de produccion, derivada de
--   sga-principal/src/main/resources/db/init/baseline_completo.sql
-- Contiene:
--   - 22 tablas en sga_principal (auditoria con hmac, user_agent, id_usuario
--     y restriccion auditoria_schema_origen_check)
--   - 18 tablas en sga_docente
--   -  2 tablas en sga_soporte
--
-- Se usa como script de inicializacion en los contenedores efimeros de las
-- pruebas de integracion. Sobre este esquema, Flyway aplica V9..V21 tal
-- como en produccion.
-- =============================================================================


-- El esquema public existe por defecto en PostgreSQL; se conserva la
-- linea como no-op para dejar constancia del volcado original.
CREATE SCHEMA IF NOT EXISTS public;

CREATE SCHEMA IF NOT EXISTS sga_docente;

CREATE SCHEMA IF NOT EXISTS sga_principal;

CREATE SCHEMA IF NOT EXISTS sga_soporte;

DO $$
BEGIN
    CREATE TYPE sga_docente.categoria_seguimiento_t AS ENUM (
    'ACADEMICO',
    'CONDUCTUAL',
    'DECE',
    'MEDICO',
    'FAMILIAR',
    'OTRO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_docente.estado_asistencia_t AS ENUM (
    'PRESENTE',
    'AUSENTE',
    'JUSTIFICADO',
    'ATRASO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_docente.nota_cualitativa_t AS ENUM (
    'A_MAS',
    'A_MENOS',
    'B_MAS',
    'B_MENOS',
    'C_MAS',
    'C_MENOS',
    'D'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_docente.tipo_actividad_t AS ENUM (
    'LECCION_ORAL',
    'LECCION_ESCRITA',
    'TAREA',
    'TALLER',
    'CUADERNO',
    'TRABAJO_INDIVIDUAL',
    'EXPOSICION',
    'PROYECTO_INTERDISCIPLINARIO',
    'EXAMEN_TRIMESTRAL'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_docente.tipo_periodo_t AS ENUM (
    'PRIMER_TRIMESTRE',
    'SEGUNDO_TRIMESTRE',
    'TERCER_TRIMESTRE'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.accion_auditoria_t AS ENUM (
    'CREAR',
    'EDITAR',
    'ELIMINAR',
    'LOGIN',
    'LOGOUT',
    'CAMBIO_PASSWORD',
    'BLOQUEO',
    'DESBLOQUEO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.dia_semana_t AS ENUM (
    'LUNES',
    'MARTES',
    'MIERCOLES',
    'JUEVES',
    'VIERNES'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.estado_matricula_t AS ENUM (
    'ACTIVA',
    'RETIRADA',
    'TRASLADADA',
    'PROMOVIDA',
    'REPROBADA'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.genero_t AS ENUM (
    'MASCULINO',
    'FEMENINO',
    'OTRO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.nivel_educativo_t AS ENUM (
    'INICIAL_1',
    'INICIAL_2',
    'PREPARATORIA',
    'BASICA_ELEMENTAL',
    'BASICA_MEDIA',
    'BASICA_SUPERIOR'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.origen_listado_t AS ENUM (
    'NUEVO',
    'TRANSFERIDO_INTERNO',
    'TRANSFERIDO_EXTERNO',
    'REPITENTE',
    'REINGRESO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.resultado_promocion_t AS ENUM (
    'PROMOVIDO',
    'REPROBADO',
    'RETIRADO',
    'TRASLADADO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.tipo_asignacion_t AS ENUM (
    'TITULAR',
    'ESPECIALIZADO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.tipo_documento_t AS ENUM (
    'PARTIDA_NACIMIENTO',
    'CEDULA_IDENTIDAD',
    'FOTO',
    'INFORME_PREVIO',
    'CERTIFICADO_MEDICO',
    'CARNET_DISCAPACIDAD',
    'COMPROBANTE_DOMICILIO',
    'OTRO'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE sga_principal.tipo_escala_t AS ENUM (
    'CUANTITATIVA',
    'CUALITATIVA',
    'MIXTA'
);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE SEQUENCE IF NOT EXISTS sga_docente.actividades_id_actividad_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.asistencias_id_asistencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.calificaciones_id_calificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.periodos_evaluacion_id_periodo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.promedios_anuales_detalle_id_detalle_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.promedios_anuales_id_promedio_anual_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.promedios_trimestrales_id_promedio_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.resumen_asistencia_id_resumen_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_docente.seguimiento_academico_id_seguimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.anos_lectivos_id_ano_lectivo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.asignaciones_id_asignacion_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.asignaturas_id_asignatura_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.auditoria_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.documentos_matricula_id_documento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.escala_calificaciones_id_escala_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.estudiantes_id_estudiante_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.fichas_estudiante_id_ficha_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.grados_id_grado_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.historial_promocion_id_historial_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.horarios_id_horario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.matriculas_id_matricula_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.niveles_educativos_id_nivel_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.paralelos_id_paralelo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.periodos_diarios_id_periodo_diario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.personas_id_persona_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.representantes_id_representante_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.roles_id_rol_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_principal.usuarios_id_usuario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_soporte.comentarios_id_comentario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS sga_soporte.tickets_id_ticket_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS public.auth_permission (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    content_type_id integer NOT NULL,
    codename character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.auth_user (
    id integer NOT NULL,
    password character varying(128) NOT NULL,
    last_login timestamp with time zone,
    is_superuser boolean NOT NULL,
    username character varying(150) NOT NULL,
    first_name character varying(150) NOT NULL,
    last_name character varying(150) NOT NULL,
    email character varying(254) NOT NULL,
    is_staff boolean NOT NULL,
    is_active boolean NOT NULL,
    date_joined timestamp with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS public.auth_user_groups (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    group_id integer NOT NULL
);

CREATE TABLE IF NOT EXISTS public.auth_user_user_permissions (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    permission_id integer NOT NULL
);

CREATE TABLE IF NOT EXISTS public.django_admin_log (
    id integer NOT NULL,
    action_time timestamp with time zone NOT NULL,
    object_id text,
    object_repr character varying(200) NOT NULL,
    action_flag smallint NOT NULL,
    change_message text NOT NULL,
    content_type_id integer,
    user_id integer NOT NULL,
    CONSTRAINT django_admin_log_action_flag_check CHECK ((action_flag >= 0))
);

CREATE TABLE IF NOT EXISTS public.django_content_type (
    id integer NOT NULL,
    app_label character varying(100) NOT NULL,
    model character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.actividades (
    id_actividad integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_periodo integer NOT NULL,
    tipo sga_docente.tipo_actividad_t NOT NULL,
    nombre character varying(200),
    descripcion text,
    fecha_entrega date,
    ponderacion numeric(5,2) DEFAULT 1.0 NOT NULL,
    nota_maxima numeric(4,2) DEFAULT 10.0 NOT NULL,
    es_sumativa boolean DEFAULT false NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT nota_maxima_check CHECK (((nota_maxima > (0)::numeric) AND (nota_maxima <= (10)::numeric))),
    CONSTRAINT ponderacion_positiva CHECK (((ponderacion > (0)::numeric) AND (ponderacion <= (100)::numeric)))
);

CREATE TABLE IF NOT EXISTS sga_docente.asistencias (
    id_asistencia bigint NOT NULL,
    id_matricula integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_periodo integer NOT NULL,
    fecha date NOT NULL,
    estado sga_docente.estado_asistencia_t NOT NULL,
    justificacion text,
    registrado_por integer NOT NULL,
    fecha_registro timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.auth_group (
    id integer NOT NULL,
    name character varying(150) NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.auth_group_permissions (
    id bigint NOT NULL,
    group_id integer NOT NULL,
    permission_id integer NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.auth_permission (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    content_type_id integer NOT NULL,
    codename character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.auth_user (
    id integer NOT NULL,
    password character varying(128) NOT NULL,
    last_login timestamp with time zone,
    is_superuser boolean NOT NULL,
    username character varying(150) NOT NULL,
    first_name character varying(150) NOT NULL,
    last_name character varying(150) NOT NULL,
    email character varying(254) NOT NULL,
    is_staff boolean NOT NULL,
    is_active boolean NOT NULL,
    date_joined timestamp with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.auth_user_groups (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    group_id integer NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.auth_user_user_permissions (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    permission_id integer NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.calificaciones (
    id_calificacion bigint NOT NULL,
    id_actividad integer NOT NULL,
    id_matricula integer NOT NULL,
    nota numeric(4,2),
    nota_cualitativa sga_docente.nota_cualitativa_t,
    observacion text,
    registrado_por integer NOT NULL,
    fecha_registro timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT nota_rango CHECK (((nota >= (0)::numeric) AND (nota <= (10)::numeric)))
);

CREATE TABLE IF NOT EXISTS sga_docente.django_admin_log (
    id integer NOT NULL,
    action_time timestamp with time zone NOT NULL,
    object_id text,
    object_repr character varying(200) NOT NULL,
    action_flag smallint NOT NULL,
    change_message text NOT NULL,
    content_type_id integer,
    user_id integer NOT NULL,
    CONSTRAINT django_admin_log_action_flag_check CHECK ((action_flag >= 0))
);

CREATE TABLE IF NOT EXISTS sga_docente.django_content_type (
    id integer NOT NULL,
    app_label character varying(100) NOT NULL,
    model character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.django_migrations (
    id bigint NOT NULL,
    app character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    applied timestamp with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.periodos_evaluacion (
    id_periodo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    tipo sga_docente.tipo_periodo_t NOT NULL,
    nombre character varying(40) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.promedios_anuales (
    id_promedio_anual integer NOT NULL,
    id_matricula integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    promedio_anual numeric(4,2),
    nota_cualitativa sga_docente.nota_cualitativa_t,
    registrado_por integer NOT NULL,
    calculado_en timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.promedios_anuales_detalle (
    id_detalle integer NOT NULL,
    id_promedio_anual integer NOT NULL,
    id_promedio_trim integer NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.promedios_trimestrales (
    id_promedio integer NOT NULL,
    id_matricula integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_periodo integer NOT NULL,
    promedio_formativo numeric(4,2),
    nota_sumativa numeric(4,2),
    promedio_trimestral numeric(4,2),
    nota_cualitativa sga_docente.nota_cualitativa_t,
    calculado_en timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.resumen_asistencia (
    id_resumen integer NOT NULL,
    id_matricula integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_periodo integer NOT NULL,
    total_presentes smallint DEFAULT 0 NOT NULL,
    total_ausentes smallint DEFAULT 0 NOT NULL,
    total_justificados smallint DEFAULT 0 NOT NULL,
    total_atrasos smallint DEFAULT 0 NOT NULL,
    calculado_en timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_docente.seguimiento_academico (
    id_seguimiento bigint NOT NULL,
    id_matricula integer NOT NULL,
    id_periodo integer NOT NULL,
    categoria sga_docente.categoria_seguimiento_t DEFAULT 'ACADEMICO'::sga_docente.categoria_seguimiento_t NOT NULL,
    descripcion text NOT NULL,
    acciones_tomadas text,
    requiere_followup boolean DEFAULT false NOT NULL,
    fecha_evento date DEFAULT CURRENT_DATE NOT NULL,
    registrado_por integer NOT NULL,
    fecha_registro timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.anos_lectivos (
    id_ano_lectivo integer NOT NULL,
    nombre character varying(20) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    es_actual boolean DEFAULT false NOT NULL,
    creado_por integer,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.asignaciones (
    id_asignacion integer NOT NULL,
    id_docente integer NOT NULL,
    id_asignatura integer NOT NULL,
    id_grado integer NOT NULL,
    id_paralelo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    es_tutor boolean DEFAULT false NOT NULL,
    tipo sga_principal.tipo_asignacion_t DEFAULT 'ESPECIALIZADO'::sga_principal.tipo_asignacion_t NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    asignado_por integer,
    fecha_asignacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.asignaturas (
    id_asignatura integer NOT NULL,
    nombre character varying(100) NOT NULL,
    codigo character varying(20),
    descripcion text,
    horas_semana smallint,
    activa boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.asignaturas_por_nivel (
    id_asignatura integer NOT NULL,
    id_nivel integer NOT NULL,
    tipo_escala sga_principal.tipo_escala_t NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.auditoria (
    id_auditoria bigint NOT NULL,
    schema_origen character varying(20) DEFAULT 'PRINCIPAL'::character varying NOT NULL,
    id_usuario integer,
    username character varying(60),
    accion sga_principal.accion_auditoria_t NOT NULL,
    tabla_afectada character varying(60),
    registro_id bigint,
    descripcion text,
    ip_address character varying(45),
    user_agent text,
    hmac character varying(64),
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT auditoria_schema_origen_check CHECK (((schema_origen)::text = ANY (ARRAY[('PRINCIPAL'::character varying)::text, ('DOCENTE'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS sga_principal.documentos_matricula (
    id_documento integer NOT NULL,
    id_matricula integer NOT NULL,
    tipo_documento sga_principal.tipo_documento_t NOT NULL,
    nombre_archivo character varying(200) NOT NULL,
    ruta_archivo character varying(500) NOT NULL,
    fecha_subida timestamp with time zone DEFAULT now() NOT NULL,
    subido_por integer
);

CREATE TABLE IF NOT EXISTS sga_principal.escala_calificaciones (
    id_escala integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    id_nivel integer NOT NULL,
    nota_minima numeric(4,2) NOT NULL,
    nota_maxima numeric(4,2) NOT NULL,
    equivalente_cualitativo character varying(5),
    descripcion character varying(100),
    CONSTRAINT escala_check CHECK ((nota_minima < nota_maxima))
);

CREATE TABLE IF NOT EXISTS sga_principal.estudiantes (
    id_estudiante integer NOT NULL,
    cedula character varying(10),
    codigo_estudiante character varying(20),
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    fecha_nacimiento date,
    genero character varying(10),
    direccion text,
    telefono character varying(20),
    telefono_alt character varying(20),
    correo character varying(150),
    discapacidad boolean DEFAULT false NOT NULL,
    tipo_discapacidad character varying(100),
    porcentaje_disc smallint,
    id_representante integer,
    origen_listado character varying(50),
    estado character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    foto_url character varying(255),
    creado_por integer,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL,
    carnet_conadis character varying(30),
    nacionalidad character varying(50),
    etnia character varying(50),
    lugar_nacimiento character varying(150),
    vive_con character varying(50),
    numeros_hermanos smallint,
    beneficio_social boolean DEFAULT false,
    CONSTRAINT porcentaje_disc_check CHECK (((porcentaje_disc >= 0) AND (porcentaje_disc <= 100)))
);

CREATE TABLE IF NOT EXISTS sga_principal.fichas_estudiante (
    id_ficha integer NOT NULL,
    id_estudiante integer NOT NULL,
    tipo_sangre character varying(5),
    alergias text,
    medicacion_permanente text,
    enfermedad_catastrofica boolean DEFAULT false NOT NULL,
    detalle_enfermedad text,
    contacto_emergencia character varying(100),
    telefono_emergencia character varying(20),
    direccion_referencia text,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.grados (
    id_grado integer NOT NULL,
    id_nivel integer NOT NULL,
    nombre character varying(60) NOT NULL,
    orden smallint NOT NULL,
    capacidad_max smallint,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.historial_promocion (
    id_historial integer NOT NULL,
    id_matricula integer NOT NULL,
    id_estudiante integer NOT NULL,
    id_grado_origen integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    resultado sga_principal.resultado_promocion_t NOT NULL,
    promedio_anual numeric(4,2),
    observaciones text,
    registrado_por integer,
    fecha_registro timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.horarios (
    id_horario integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_periodo_diario integer NOT NULL,
    dia_semana sga_principal.dia_semana_t NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.matriculas (
    id_matricula integer NOT NULL,
    id_estudiante integer NOT NULL,
    id_grado integer NOT NULL,
    id_paralelo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    numero_orden smallint,
    fecha_registro date DEFAULT CURRENT_DATE NOT NULL,
    estado sga_principal.estado_matricula_t DEFAULT 'ACTIVA'::sga_principal.estado_matricula_t NOT NULL,
    observaciones text,
    registrado_por integer,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.niveles_educativos (
    id_nivel integer NOT NULL,
    nombre character varying(60) NOT NULL,
    tipo_escala sga_principal.tipo_escala_t NOT NULL,
    grado_inicio smallint NOT NULL,
    grado_fin smallint NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.paralelos (
    id_paralelo integer NOT NULL,
    id_grado integer NOT NULL,
    letra character(1) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.paralelos_ano_lectivo (
    id_paralelo_al integer NOT NULL,
    id_paralelo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    capacidad_max smallint DEFAULT 35 NOT NULL,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.periodos_diarios (
    id_periodo_diario integer NOT NULL,
    numero smallint NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    aplica_nivel sga_principal.nivel_educativo_t
);

CREATE TABLE IF NOT EXISTS sga_principal.personas (
    id_persona integer NOT NULL,
    id_usuario integer NOT NULL,
    cedula character varying(10),
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    fecha_nacimiento date,
    genero sga_principal.genero_t,
    telefono character varying(20),
    telefono_alt character varying(20),
    direccion text,
    correo_personal character varying(150),
    titulo_academico character varying(120),
    especializacion character varying(120),
    fecha_ingreso_inst date,
    cargo character varying(80),
    foto_url character varying(255),
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.representantes (
    id_representante integer NOT NULL,
    cedula character varying(10),
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    parentesco character varying(50) NOT NULL,
    telefono_principal character varying(20),
    telefono_alt character varying(20),
    correo character varying(150),
    direccion text,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.roles (
    id_rol integer NOT NULL,
    nombre character varying(30) NOT NULL,
    descripcion text,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.usuario_roles (
    id_usuario integer NOT NULL,
    id_rol integer NOT NULL,
    asignado_por integer,
    asignado_el timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.usuarios (
    id_usuario integer NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    username character varying(60) NOT NULL,
    correo character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    primer_ingreso boolean DEFAULT true NOT NULL,
    intentos_fallidos smallint DEFAULT 0 NOT NULL,
    bloqueado_hasta timestamp with time zone,
    estado boolean DEFAULT true NOT NULL,
    ultimo_acceso timestamp with time zone,
    creado_por integer,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_soporte.comentarios (
    id_comentario bigint NOT NULL,
    autor character varying(100) NOT NULL,
    contenido text NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    nota_interna boolean NOT NULL,
    id_ticket bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_soporte.tickets (
    id_ticket bigint NOT NULL,
    asignado_a character varying(100),
    categoria character varying(20) NOT NULL,
    creado_por character varying(100) NOT NULL,
    descripcion text NOT NULL,
    estado character varying(20) NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    fecha_resolucion timestamp(6) without time zone,
    numero_ticket character varying(20) NOT NULL,
    prioridad character varying(20) NOT NULL,
    solucion_aplicada text,
    titulo character varying(150) NOT NULL,
    CONSTRAINT tickets_categoria_check CHECK (((categoria)::text = ANY ((ARRAY['HARDWARE'::character varying, 'SOFTWARE'::character varying, 'RED'::character varying, 'CUENTA'::character varying, 'OTRO'::character varying])::text[]))),
    CONSTRAINT tickets_estado_check CHECK (((estado)::text = ANY ((ARRAY['ABIERTO'::character varying, 'EN_PROCESO'::character varying, 'RESUELTO'::character varying, 'CERRADO'::character varying])::text[]))),
    CONSTRAINT tickets_prioridad_check CHECK (((prioridad)::text = ANY ((ARRAY['BAJO'::character varying, 'MEDIO'::character varying, 'ALTO'::character varying, 'CRITICO'::character varying])::text[])))
);

ALTER SEQUENCE sga_docente.actividades_id_actividad_seq OWNED BY sga_docente.actividades.id_actividad;

ALTER SEQUENCE sga_docente.asistencias_id_asistencia_seq OWNED BY sga_docente.asistencias.id_asistencia;

ALTER SEQUENCE sga_docente.calificaciones_id_calificacion_seq OWNED BY sga_docente.calificaciones.id_calificacion;

ALTER SEQUENCE sga_docente.periodos_evaluacion_id_periodo_seq OWNED BY sga_docente.periodos_evaluacion.id_periodo;

ALTER SEQUENCE sga_docente.promedios_anuales_detalle_id_detalle_seq OWNED BY sga_docente.promedios_anuales_detalle.id_detalle;

ALTER SEQUENCE sga_docente.promedios_anuales_id_promedio_anual_seq OWNED BY sga_docente.promedios_anuales.id_promedio_anual;

ALTER SEQUENCE sga_docente.promedios_trimestrales_id_promedio_seq OWNED BY sga_docente.promedios_trimestrales.id_promedio;

ALTER SEQUENCE sga_docente.resumen_asistencia_id_resumen_seq OWNED BY sga_docente.resumen_asistencia.id_resumen;

ALTER SEQUENCE sga_docente.seguimiento_academico_id_seguimiento_seq OWNED BY sga_docente.seguimiento_academico.id_seguimiento;

ALTER SEQUENCE sga_principal.anos_lectivos_id_ano_lectivo_seq OWNED BY sga_principal.anos_lectivos.id_ano_lectivo;

ALTER SEQUENCE sga_principal.asignaciones_id_asignacion_seq OWNED BY sga_principal.asignaciones.id_asignacion;

ALTER SEQUENCE sga_principal.asignaturas_id_asignatura_seq OWNED BY sga_principal.asignaturas.id_asignatura;

ALTER SEQUENCE sga_principal.auditoria_id_auditoria_seq OWNED BY sga_principal.auditoria.id_auditoria;

ALTER SEQUENCE sga_principal.documentos_matricula_id_documento_seq OWNED BY sga_principal.documentos_matricula.id_documento;

ALTER SEQUENCE sga_principal.escala_calificaciones_id_escala_seq OWNED BY sga_principal.escala_calificaciones.id_escala;

ALTER SEQUENCE sga_principal.estudiantes_id_estudiante_seq OWNED BY sga_principal.estudiantes.id_estudiante;

ALTER SEQUENCE sga_principal.fichas_estudiante_id_ficha_seq OWNED BY sga_principal.fichas_estudiante.id_ficha;

ALTER SEQUENCE sga_principal.grados_id_grado_seq OWNED BY sga_principal.grados.id_grado;

ALTER SEQUENCE sga_principal.historial_promocion_id_historial_seq OWNED BY sga_principal.historial_promocion.id_historial;

ALTER SEQUENCE sga_principal.horarios_id_horario_seq OWNED BY sga_principal.horarios.id_horario;

ALTER SEQUENCE sga_principal.matriculas_id_matricula_seq OWNED BY sga_principal.matriculas.id_matricula;

ALTER SEQUENCE sga_principal.niveles_educativos_id_nivel_seq OWNED BY sga_principal.niveles_educativos.id_nivel;

ALTER SEQUENCE sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq OWNED BY sga_principal.paralelos_ano_lectivo.id_paralelo_al;

ALTER SEQUENCE sga_principal.paralelos_id_paralelo_seq OWNED BY sga_principal.paralelos.id_paralelo;

ALTER SEQUENCE sga_principal.periodos_diarios_id_periodo_diario_seq OWNED BY sga_principal.periodos_diarios.id_periodo_diario;

ALTER SEQUENCE sga_principal.personas_id_persona_seq OWNED BY sga_principal.personas.id_persona;

ALTER SEQUENCE sga_principal.representantes_id_representante_seq OWNED BY sga_principal.representantes.id_representante;

ALTER SEQUENCE sga_principal.roles_id_rol_seq OWNED BY sga_principal.roles.id_rol;

ALTER SEQUENCE sga_principal.usuarios_id_usuario_seq OWNED BY sga_principal.usuarios.id_usuario;

ALTER SEQUENCE sga_soporte.comentarios_id_comentario_seq OWNED BY sga_soporte.comentarios.id_comentario;

ALTER SEQUENCE sga_soporte.tickets_id_ticket_seq OWNED BY sga_soporte.tickets.id_ticket;

ALTER TABLE ONLY sga_docente.actividades ALTER COLUMN id_actividad SET DEFAULT nextval('sga_docente.actividades_id_actividad_seq'::regclass);

ALTER TABLE ONLY sga_docente.asistencias ALTER COLUMN id_asistencia SET DEFAULT nextval('sga_docente.asistencias_id_asistencia_seq'::regclass);

ALTER TABLE ONLY sga_docente.calificaciones ALTER COLUMN id_calificacion SET DEFAULT nextval('sga_docente.calificaciones_id_calificacion_seq'::regclass);

ALTER TABLE ONLY sga_docente.periodos_evaluacion ALTER COLUMN id_periodo SET DEFAULT nextval('sga_docente.periodos_evaluacion_id_periodo_seq'::regclass);

ALTER TABLE ONLY sga_docente.promedios_anuales ALTER COLUMN id_promedio_anual SET DEFAULT nextval('sga_docente.promedios_anuales_id_promedio_anual_seq'::regclass);

ALTER TABLE ONLY sga_docente.promedios_anuales_detalle ALTER COLUMN id_detalle SET DEFAULT nextval('sga_docente.promedios_anuales_detalle_id_detalle_seq'::regclass);

ALTER TABLE ONLY sga_docente.promedios_trimestrales ALTER COLUMN id_promedio SET DEFAULT nextval('sga_docente.promedios_trimestrales_id_promedio_seq'::regclass);

ALTER TABLE ONLY sga_docente.resumen_asistencia ALTER COLUMN id_resumen SET DEFAULT nextval('sga_docente.resumen_asistencia_id_resumen_seq'::regclass);

ALTER TABLE ONLY sga_docente.seguimiento_academico ALTER COLUMN id_seguimiento SET DEFAULT nextval('sga_docente.seguimiento_academico_id_seguimiento_seq'::regclass);

ALTER TABLE ONLY sga_principal.anos_lectivos ALTER COLUMN id_ano_lectivo SET DEFAULT nextval('sga_principal.anos_lectivos_id_ano_lectivo_seq'::regclass);

ALTER TABLE ONLY sga_principal.asignaciones ALTER COLUMN id_asignacion SET DEFAULT nextval('sga_principal.asignaciones_id_asignacion_seq'::regclass);

ALTER TABLE ONLY sga_principal.asignaturas ALTER COLUMN id_asignatura SET DEFAULT nextval('sga_principal.asignaturas_id_asignatura_seq'::regclass);

ALTER TABLE ONLY sga_principal.auditoria ALTER COLUMN id_auditoria SET DEFAULT nextval('sga_principal.auditoria_id_auditoria_seq'::regclass);

ALTER TABLE ONLY sga_principal.documentos_matricula ALTER COLUMN id_documento SET DEFAULT nextval('sga_principal.documentos_matricula_id_documento_seq'::regclass);

ALTER TABLE ONLY sga_principal.escala_calificaciones ALTER COLUMN id_escala SET DEFAULT nextval('sga_principal.escala_calificaciones_id_escala_seq'::regclass);

ALTER TABLE ONLY sga_principal.estudiantes ALTER COLUMN id_estudiante SET DEFAULT nextval('sga_principal.estudiantes_id_estudiante_seq'::regclass);

ALTER TABLE ONLY sga_principal.fichas_estudiante ALTER COLUMN id_ficha SET DEFAULT nextval('sga_principal.fichas_estudiante_id_ficha_seq'::regclass);

ALTER TABLE ONLY sga_principal.grados ALTER COLUMN id_grado SET DEFAULT nextval('sga_principal.grados_id_grado_seq'::regclass);

ALTER TABLE ONLY sga_principal.historial_promocion ALTER COLUMN id_historial SET DEFAULT nextval('sga_principal.historial_promocion_id_historial_seq'::regclass);

ALTER TABLE ONLY sga_principal.horarios ALTER COLUMN id_horario SET DEFAULT nextval('sga_principal.horarios_id_horario_seq'::regclass);

ALTER TABLE ONLY sga_principal.matriculas ALTER COLUMN id_matricula SET DEFAULT nextval('sga_principal.matriculas_id_matricula_seq'::regclass);

ALTER TABLE ONLY sga_principal.niveles_educativos ALTER COLUMN id_nivel SET DEFAULT nextval('sga_principal.niveles_educativos_id_nivel_seq'::regclass);

ALTER TABLE ONLY sga_principal.paralelos ALTER COLUMN id_paralelo SET DEFAULT nextval('sga_principal.paralelos_id_paralelo_seq'::regclass);

ALTER TABLE ONLY sga_principal.paralelos_ano_lectivo ALTER COLUMN id_paralelo_al SET DEFAULT nextval('sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq'::regclass);

ALTER TABLE ONLY sga_principal.periodos_diarios ALTER COLUMN id_periodo_diario SET DEFAULT nextval('sga_principal.periodos_diarios_id_periodo_diario_seq'::regclass);

ALTER TABLE ONLY sga_principal.personas ALTER COLUMN id_persona SET DEFAULT nextval('sga_principal.personas_id_persona_seq'::regclass);

ALTER TABLE ONLY sga_principal.representantes ALTER COLUMN id_representante SET DEFAULT nextval('sga_principal.representantes_id_representante_seq'::regclass);

ALTER TABLE ONLY sga_principal.roles ALTER COLUMN id_rol SET DEFAULT nextval('sga_principal.roles_id_rol_seq'::regclass);

ALTER TABLE ONLY sga_principal.usuarios ALTER COLUMN id_usuario SET DEFAULT nextval('sga_principal.usuarios_id_usuario_seq'::regclass);

ALTER TABLE ONLY sga_soporte.comentarios ALTER COLUMN id_comentario SET DEFAULT nextval('sga_soporte.comentarios_id_comentario_seq'::regclass);

ALTER TABLE ONLY sga_soporte.tickets ALTER COLUMN id_ticket SET DEFAULT nextval('sga_soporte.tickets_id_ticket_seq'::regclass);

-- =============================================================================
-- Restricciones PRIMARY KEY de las 42 tablas.
-- En el volcado original de pg_dump vienen como ALTER TABLE tras los CREATE.
-- Se aplican con guardas idempotentes para tolerar re-ejecucion del script.
-- =============================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'actividades_pkey') THEN
        ALTER TABLE sga_docente.actividades ADD CONSTRAINT actividades_pkey PRIMARY KEY (id_actividad);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'asistencias_pkey') THEN
        ALTER TABLE sga_docente.asistencias ADD CONSTRAINT asistencias_pkey PRIMARY KEY (id_asistencia);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_group_permissions_pkey') THEN
        ALTER TABLE sga_docente.auth_group_permissions ADD CONSTRAINT auth_group_permissions_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_group_pkey') THEN
        ALTER TABLE sga_docente.auth_group ADD CONSTRAINT auth_group_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_permission_pkey') THEN
        ALTER TABLE sga_docente.auth_permission ADD CONSTRAINT auth_permission_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_user_groups_pkey') THEN
        ALTER TABLE sga_docente.auth_user_groups ADD CONSTRAINT auth_user_groups_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_user_pkey') THEN
        ALTER TABLE sga_docente.auth_user ADD CONSTRAINT auth_user_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_user_user_permissions_pkey') THEN
        ALTER TABLE sga_docente.auth_user_user_permissions ADD CONSTRAINT auth_user_user_permissions_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'calificaciones_pkey') THEN
        ALTER TABLE sga_docente.calificaciones ADD CONSTRAINT calificaciones_pkey PRIMARY KEY (id_calificacion);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'django_admin_log_pkey') THEN
        ALTER TABLE sga_docente.django_admin_log ADD CONSTRAINT django_admin_log_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'django_content_type_pkey') THEN
        ALTER TABLE sga_docente.django_content_type ADD CONSTRAINT django_content_type_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'django_migrations_pkey') THEN
        ALTER TABLE sga_docente.django_migrations ADD CONSTRAINT django_migrations_pkey PRIMARY KEY (id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'periodos_evaluacion_pkey') THEN
        ALTER TABLE sga_docente.periodos_evaluacion ADD CONSTRAINT periodos_evaluacion_pkey PRIMARY KEY (id_periodo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'promedios_anuales_detalle_pkey') THEN
        ALTER TABLE sga_docente.promedios_anuales_detalle ADD CONSTRAINT promedios_anuales_detalle_pkey PRIMARY KEY (id_detalle);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'promedios_anuales_pkey') THEN
        ALTER TABLE sga_docente.promedios_anuales ADD CONSTRAINT promedios_anuales_pkey PRIMARY KEY (id_promedio_anual);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'promedios_trimestrales_pkey') THEN
        ALTER TABLE sga_docente.promedios_trimestrales ADD CONSTRAINT promedios_trimestrales_pkey PRIMARY KEY (id_promedio);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'resumen_asistencia_pkey') THEN
        ALTER TABLE sga_docente.resumen_asistencia ADD CONSTRAINT resumen_asistencia_pkey PRIMARY KEY (id_resumen);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'seguimiento_academico_pkey') THEN
        ALTER TABLE sga_docente.seguimiento_academico ADD CONSTRAINT seguimiento_academico_pkey PRIMARY KEY (id_seguimiento);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'anos_lectivos_pkey') THEN
        ALTER TABLE sga_principal.anos_lectivos ADD CONSTRAINT anos_lectivos_pkey PRIMARY KEY (id_ano_lectivo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'asignaciones_pkey') THEN
        ALTER TABLE sga_principal.asignaciones ADD CONSTRAINT asignaciones_pkey PRIMARY KEY (id_asignacion);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'asignaturas_pkey') THEN
        ALTER TABLE sga_principal.asignaturas ADD CONSTRAINT asignaturas_pkey PRIMARY KEY (id_asignatura);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'asignaturas_por_nivel_pkey') THEN
        ALTER TABLE sga_principal.asignaturas_por_nivel ADD CONSTRAINT asignaturas_por_nivel_pkey PRIMARY KEY (id_asignatura, id_nivel);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auditoria_pkey') THEN
        ALTER TABLE sga_principal.auditoria ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id_auditoria);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'documentos_matricula_pkey') THEN
        ALTER TABLE sga_principal.documentos_matricula ADD CONSTRAINT documentos_matricula_pkey PRIMARY KEY (id_documento);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'escala_calificaciones_pkey') THEN
        ALTER TABLE sga_principal.escala_calificaciones ADD CONSTRAINT escala_calificaciones_pkey PRIMARY KEY (id_escala);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'estudiantes_pkey') THEN
        ALTER TABLE sga_principal.estudiantes ADD CONSTRAINT estudiantes_pkey PRIMARY KEY (id_estudiante);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fichas_estudiante_pkey') THEN
        ALTER TABLE sga_principal.fichas_estudiante ADD CONSTRAINT fichas_estudiante_pkey PRIMARY KEY (id_ficha);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'grados_pkey') THEN
        ALTER TABLE sga_principal.grados ADD CONSTRAINT grados_pkey PRIMARY KEY (id_grado);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'historial_promocion_pkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT historial_promocion_pkey PRIMARY KEY (id_historial);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'horarios_pkey') THEN
        ALTER TABLE sga_principal.horarios ADD CONSTRAINT horarios_pkey PRIMARY KEY (id_horario);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'matriculas_pkey') THEN
        ALTER TABLE sga_principal.matriculas ADD CONSTRAINT matriculas_pkey PRIMARY KEY (id_matricula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'niveles_educativos_pkey') THEN
        ALTER TABLE sga_principal.niveles_educativos ADD CONSTRAINT niveles_educativos_pkey PRIMARY KEY (id_nivel);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'paralelos_ano_lectivo_pkey') THEN
        ALTER TABLE sga_principal.paralelos_ano_lectivo ADD CONSTRAINT paralelos_ano_lectivo_pkey PRIMARY KEY (id_paralelo_al);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'paralelos_pkey') THEN
        ALTER TABLE sga_principal.paralelos ADD CONSTRAINT paralelos_pkey PRIMARY KEY (id_paralelo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'periodos_diarios_pkey') THEN
        ALTER TABLE sga_principal.periodos_diarios ADD CONSTRAINT periodos_diarios_pkey PRIMARY KEY (id_periodo_diario);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'personas_pkey') THEN
        ALTER TABLE sga_principal.personas ADD CONSTRAINT personas_pkey PRIMARY KEY (id_persona);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'representantes_pkey') THEN
        ALTER TABLE sga_principal.representantes ADD CONSTRAINT representantes_pkey PRIMARY KEY (id_representante);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'roles_pkey') THEN
        ALTER TABLE sga_principal.roles ADD CONSTRAINT roles_pkey PRIMARY KEY (id_rol);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usuario_roles_pkey') THEN
        ALTER TABLE sga_principal.usuario_roles ADD CONSTRAINT usuario_roles_pkey PRIMARY KEY (id_usuario, id_rol);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usuarios_pkey') THEN
        ALTER TABLE sga_principal.usuarios ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'comentarios_pkey') THEN
        ALTER TABLE sga_soporte.comentarios ADD CONSTRAINT comentarios_pkey PRIMARY KEY (id_comentario);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'tickets_pkey') THEN
        ALTER TABLE sga_soporte.tickets ADD CONSTRAINT tickets_pkey PRIMARY KEY (id_ticket);
    END IF;
END $$;


-- =============================================================================
-- Restricciones UNIQUE.
-- =============================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'asistencias_id_matricula_id_asignacion_fecha_key') THEN
        ALTER TABLE sga_docente.asistencias ADD CONSTRAINT asistencias_id_matricula_id_asignacion_fecha_key UNIQUE (id_matricula, id_asignacion, fecha);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_group_name_key') THEN
        ALTER TABLE sga_docente.auth_group ADD CONSTRAINT auth_group_name_key UNIQUE (name);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_group_permissions_group_id_permission_id_0cd325b0_uniq') THEN
        ALTER TABLE sga_docente.auth_group_permissions ADD CONSTRAINT auth_group_permissions_group_id_permission_id_0cd325b0_uniq UNIQUE (group_id, permission_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_permission_content_type_id_codename_01ab375a_uniq') THEN
        ALTER TABLE sga_docente.auth_permission ADD CONSTRAINT auth_permission_content_type_id_codename_01ab375a_uniq UNIQUE (content_type_id, codename);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_user_groups_user_id_group_id_94350c0c_uniq') THEN
        ALTER TABLE sga_docente.auth_user_groups ADD CONSTRAINT auth_user_groups_user_id_group_id_94350c0c_uniq UNIQUE (user_id, group_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_user_user_permissions_user_id_permission_id_14a6b632_uniq') THEN
        ALTER TABLE sga_docente.auth_user_user_permissions ADD CONSTRAINT auth_user_user_permissions_user_id_permission_id_14a6b632_uniq UNIQUE (user_id, permission_id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'auth_user_username_key') THEN
        ALTER TABLE sga_docente.auth_user ADD CONSTRAINT auth_user_username_key UNIQUE (username);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'calificaciones_id_actividad_id_matricula_key') THEN
        ALTER TABLE sga_docente.calificaciones ADD CONSTRAINT calificaciones_id_actividad_id_matricula_key UNIQUE (id_actividad, id_matricula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'django_content_type_app_label_model_76bd3d3b_uniq') THEN
        ALTER TABLE sga_docente.django_content_type ADD CONSTRAINT django_content_type_app_label_model_76bd3d3b_uniq UNIQUE (app_label, model);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'periodos_evaluacion_id_ano_lectivo_tipo_key') THEN
        ALTER TABLE sga_docente.periodos_evaluacion ADD CONSTRAINT periodos_evaluacion_id_ano_lectivo_tipo_key UNIQUE (id_ano_lectivo, tipo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'promedios_anuales_detalle_unique') THEN
        ALTER TABLE sga_docente.promedios_anuales_detalle ADD CONSTRAINT promedios_anuales_detalle_unique UNIQUE (id_promedio_anual, id_promedio_trim);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'promedios_anuales_id_matricula_id_asignacion_id_ano_lectivo_key') THEN
        ALTER TABLE sga_docente.promedios_anuales ADD CONSTRAINT promedios_anuales_id_matricula_id_asignacion_id_ano_lectivo_key UNIQUE (id_matricula, id_asignacion, id_ano_lectivo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'promedios_trimestrales_id_matricula_id_asignacion_id_period_key') THEN
        ALTER TABLE sga_docente.promedios_trimestrales ADD CONSTRAINT promedios_trimestrales_id_matricula_id_asignacion_id_period_key UNIQUE (id_matricula, id_asignacion, id_periodo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'resumen_asistencia_id_matricula_id_asignacion_id_periodo_key') THEN
        ALTER TABLE sga_docente.resumen_asistencia ADD CONSTRAINT resumen_asistencia_id_matricula_id_asignacion_id_periodo_key UNIQUE (id_matricula, id_asignacion, id_periodo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'anos_lectivos_nombre_key') THEN
        ALTER TABLE sga_principal.anos_lectivos ADD CONSTRAINT anos_lectivos_nombre_key UNIQUE (nombre);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'asignaciones_asignatura_paralelo_ano_key') THEN
        ALTER TABLE sga_principal.asignaciones ADD CONSTRAINT asignaciones_asignatura_paralelo_ano_key UNIQUE (id_asignatura, id_paralelo, id_ano_lectivo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'escala_ano_nivel_rango_unique') THEN
        ALTER TABLE sga_principal.escala_calificaciones ADD CONSTRAINT escala_ano_nivel_rango_unique UNIQUE (id_ano_lectivo, id_nivel, nota_minima);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'estudiantes_cedula_key') THEN
        ALTER TABLE sga_principal.estudiantes ADD CONSTRAINT estudiantes_cedula_key UNIQUE (cedula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'estudiantes_codigo_estudiante_key') THEN
        ALTER TABLE sga_principal.estudiantes ADD CONSTRAINT estudiantes_codigo_estudiante_key UNIQUE (codigo_estudiante);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fichas_estudiante_id_estudiante_key') THEN
        ALTER TABLE sga_principal.fichas_estudiante ADD CONSTRAINT fichas_estudiante_id_estudiante_key UNIQUE (id_estudiante);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'historial_matricula_unique') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT historial_matricula_unique UNIQUE (id_matricula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'horarios_id_asignacion_id_periodo_diario_dia_semana_key') THEN
        ALTER TABLE sga_principal.horarios ADD CONSTRAINT horarios_id_asignacion_id_periodo_diario_dia_semana_key UNIQUE (id_asignacion, id_periodo_diario, dia_semana);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'matriculas_id_estudiante_id_ano_lectivo_key') THEN
        ALTER TABLE sga_principal.matriculas ADD CONSTRAINT matriculas_id_estudiante_id_ano_lectivo_key UNIQUE (id_estudiante, id_ano_lectivo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'paralelos_ano_lectivo_unique') THEN
        ALTER TABLE sga_principal.paralelos_ano_lectivo ADD CONSTRAINT paralelos_ano_lectivo_unique UNIQUE (id_paralelo, id_ano_lectivo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'paralelos_id_grado_letra_key') THEN
        ALTER TABLE sga_principal.paralelos ADD CONSTRAINT paralelos_id_grado_letra_key UNIQUE (id_grado, letra);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'personas_cedula_key') THEN
        ALTER TABLE sga_principal.personas ADD CONSTRAINT personas_cedula_key UNIQUE (cedula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'personas_id_usuario_key') THEN
        ALTER TABLE sga_principal.personas ADD CONSTRAINT personas_id_usuario_key UNIQUE (id_usuario);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'roles_nombre_key') THEN
        ALTER TABLE sga_principal.roles ADD CONSTRAINT roles_nombre_key UNIQUE (nombre);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usuarios_correo_key') THEN
        ALTER TABLE sga_principal.usuarios ADD CONSTRAINT usuarios_correo_key UNIQUE (correo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usuarios_username_key') THEN
        ALTER TABLE sga_principal.usuarios ADD CONSTRAINT usuarios_username_key UNIQUE (username);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usuarios_uuid_key') THEN
        ALTER TABLE sga_principal.usuarios ADD CONSTRAINT usuarios_uuid_key UNIQUE (uuid);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_k0msfrh058q2goawy9u1sig33') THEN
        ALTER TABLE sga_soporte.tickets ADD CONSTRAINT uk_k0msfrh058q2goawy9u1sig33 UNIQUE (numero_ticket);
    END IF;
END $$;


CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================================
-- Columnas de auditoria añadidas al baseline para alinearlo con la entidad
-- JPA de sga-principal. En el volcado original estas columnas viven en la
-- misma tabla; se añaden aquí como ALTER TABLE ADD COLUMN IF NOT EXISTS
-- para mantener idempotencia.
-- =============================================================================

ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS resultado           varchar(10) NOT NULL DEFAULT 'EXITO';
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS trace_id            uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS hash_actual         varchar(64);
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS hash_anterior       varchar(64);
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS contenido_canonico  text;
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS reloj_lamport       bigint;
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS vector_reloj        text;
ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS version_canonica    varchar(20);
