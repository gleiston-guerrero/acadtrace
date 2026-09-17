--
-- PostgreSQL database dump
--

\restrict a7wBy5EIUHF0IT6Fm3OJqxJdgAmI6zYbqeVLc9abUGM1t83Q7yMqwOFTsfGaXJZ

-- Dumped from database version 17.6
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: sga_docente; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA sga_docente;


--
-- Name: SCHEMA sga_docente; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA sga_docente IS 'M├│dulo docente: calificaciones, asistencias, seguimiento';


--
-- Name: sga_principal; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA sga_principal;


--
-- Name: SCHEMA sga_principal; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA sga_principal IS 'M├│dulo principal: usuarios, matr├¡culas, grados, asignaciones, auditor├¡a';


--
-- Name: sga_soporte; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA sga_soporte;


--
-- Name: categoria_seguimiento_t; Type: TYPE; Schema: sga_docente; Owner: -
--

CREATE TYPE sga_docente.categoria_seguimiento_t AS ENUM (
    'ACADEMICO',
    'CONDUCTUAL',
    'DECE',
    'MEDICO',
    'FAMILIAR',
    'OTRO'
);


--
-- Name: estado_asistencia_t; Type: TYPE; Schema: sga_docente; Owner: -
--

CREATE TYPE sga_docente.estado_asistencia_t AS ENUM (
    'PRESENTE',
    'AUSENTE',
    'JUSTIFICADO',
    'ATRASO'
);


--
-- Name: nota_cualitativa_t; Type: TYPE; Schema: sga_docente; Owner: -
--

CREATE TYPE sga_docente.nota_cualitativa_t AS ENUM (
    'A_MAS',
    'A_MENOS',
    'B_MAS',
    'B_MENOS',
    'C_MAS',
    'C_MENOS',
    'D'
);


--
-- Name: tipo_actividad_t; Type: TYPE; Schema: sga_docente; Owner: -
--

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


--
-- Name: tipo_periodo_t; Type: TYPE; Schema: sga_docente; Owner: -
--

CREATE TYPE sga_docente.tipo_periodo_t AS ENUM (
    'PRIMER_TRIMESTRE',
    'SEGUNDO_TRIMESTRE',
    'TERCER_TRIMESTRE'
);


--
-- Name: accion_auditoria_t; Type: TYPE; Schema: sga_principal; Owner: -
--

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


--
-- Name: dia_semana_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.dia_semana_t AS ENUM (
    'LUNES',
    'MARTES',
    'MIERCOLES',
    'JUEVES',
    'VIERNES'
);


--
-- Name: estado_matricula_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.estado_matricula_t AS ENUM (
    'ACTIVA',
    'RETIRADA',
    'TRASLADADA',
    'PROMOVIDA',
    'REPROBADA'
);


--
-- Name: genero_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.genero_t AS ENUM (
    'MASCULINO',
    'FEMENINO',
    'OTRO'
);


--
-- Name: nivel_educativo_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.nivel_educativo_t AS ENUM (
    'INICIAL_1',
    'INICIAL_2',
    'PREPARATORIA',
    'BASICA_ELEMENTAL',
    'BASICA_MEDIA',
    'BASICA_SUPERIOR'
);


--
-- Name: origen_listado_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.origen_listado_t AS ENUM (
    'NUEVO',
    'TRANSFERIDO_INTERNO',
    'TRANSFERIDO_EXTERNO',
    'REPITENTE',
    'REINGRESO'
);


--
-- Name: resultado_promocion_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.resultado_promocion_t AS ENUM (
    'PROMOVIDO',
    'REPROBADO',
    'RETIRADO',
    'TRASLADADO'
);


--
-- Name: tipo_asignacion_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.tipo_asignacion_t AS ENUM (
    'TITULAR',
    'ESPECIALIZADO'
);


--
-- Name: tipo_documento_t; Type: TYPE; Schema: sga_principal; Owner: -
--

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


--
-- Name: tipo_escala_t; Type: TYPE; Schema: sga_principal; Owner: -
--

CREATE TYPE sga_principal.tipo_escala_t AS ENUM (
    'CUANTITATIVA',
    'CUALITATIVA',
    'MIXTA'
);


--
-- Name: rls_auto_enable(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.rls_auto_enable() RETURNS event_trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'pg_catalog'
    AS $$
DECLARE
  cmd record;
BEGIN
  FOR cmd IN
    SELECT *
    FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE', 'CREATE TABLE AS', 'SELECT INTO')
      AND object_type IN ('table','partitioned table')
  LOOP
     IF cmd.schema_name IS NOT NULL AND cmd.schema_name IN ('public') AND cmd.schema_name NOT IN ('pg_catalog','information_schema') AND cmd.schema_name NOT LIKE 'pg_toast%' AND cmd.schema_name NOT LIKE 'pg_temp%' THEN
      BEGIN
        EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity);
        RAISE LOG 'rls_auto_enable: enabled RLS on %', cmd.object_identity;
      EXCEPTION
        WHEN OTHERS THEN
          RAISE LOG 'rls_auto_enable: failed to enable RLS on %', cmd.object_identity;
      END;
     ELSE
        RAISE LOG 'rls_auto_enable: skip % (either system schema or not in enforced list: %.)', cmd.object_identity, cmd.schema_name;
     END IF;
  END LOOP;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth_permission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_permission (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    content_type_id integer NOT NULL,
    codename character varying(100) NOT NULL
);


--
-- Name: auth_permission_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_permission ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_user (
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


--
-- Name: auth_user_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_user_groups (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    group_id integer NOT NULL
);


--
-- Name: auth_user_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_user_groups ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_user_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_user ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_user_user_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_user_user_permissions (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    permission_id integer NOT NULL
);


--
-- Name: auth_user_user_permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auth_user_user_permissions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.auth_user_user_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_admin_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.django_admin_log (
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


--
-- Name: django_admin_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.django_admin_log ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.django_admin_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_content_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.django_content_type (
    id integer NOT NULL,
    app_label character varying(100) NOT NULL,
    model character varying(100) NOT NULL
);


--
-- Name: django_content_type_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.django_content_type ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.django_content_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: actividades; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.actividades (
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


--
-- Name: actividades_id_actividad_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.actividades_id_actividad_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: actividades_id_actividad_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.actividades_id_actividad_seq OWNED BY sga_docente.actividades.id_actividad;


--
-- Name: asistencias; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.asistencias (
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


--
-- Name: asistencias_id_asistencia_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.asistencias_id_asistencia_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: asistencias_id_asistencia_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.asistencias_id_asistencia_seq OWNED BY sga_docente.asistencias.id_asistencia;


--
-- Name: auth_group; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.auth_group (
    id integer NOT NULL,
    name character varying(150) NOT NULL
);


--
-- Name: auth_group_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.auth_group ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.auth_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_group_permissions; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.auth_group_permissions (
    id bigint NOT NULL,
    group_id integer NOT NULL,
    permission_id integer NOT NULL
);


--
-- Name: auth_group_permissions_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.auth_group_permissions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.auth_group_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_permission; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.auth_permission (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    content_type_id integer NOT NULL,
    codename character varying(100) NOT NULL
);


--
-- Name: auth_permission_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.auth_permission ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.auth_permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_user; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.auth_user (
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


--
-- Name: auth_user_groups; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.auth_user_groups (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    group_id integer NOT NULL
);


--
-- Name: auth_user_groups_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.auth_user_groups ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.auth_user_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_user_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.auth_user ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.auth_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auth_user_user_permissions; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.auth_user_user_permissions (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    permission_id integer NOT NULL
);


--
-- Name: auth_user_user_permissions_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.auth_user_user_permissions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.auth_user_user_permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: calificaciones; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.calificaciones (
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


--
-- Name: calificaciones_id_calificacion_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.calificaciones_id_calificacion_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: calificaciones_id_calificacion_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.calificaciones_id_calificacion_seq OWNED BY sga_docente.calificaciones.id_calificacion;


--
-- Name: django_admin_log; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.django_admin_log (
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


--
-- Name: django_admin_log_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.django_admin_log ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.django_admin_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_content_type; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.django_content_type (
    id integer NOT NULL,
    app_label character varying(100) NOT NULL,
    model character varying(100) NOT NULL
);


--
-- Name: django_content_type_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.django_content_type ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.django_content_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: django_migrations; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.django_migrations (
    id bigint NOT NULL,
    app character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    applied timestamp with time zone NOT NULL
);


--
-- Name: django_migrations_id_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

ALTER TABLE sga_docente.django_migrations ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME sga_docente.django_migrations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: periodos_evaluacion; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.periodos_evaluacion (
    id_periodo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    tipo sga_docente.tipo_periodo_t NOT NULL,
    nombre character varying(40) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: periodos_evaluacion_id_periodo_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.periodos_evaluacion_id_periodo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: periodos_evaluacion_id_periodo_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.periodos_evaluacion_id_periodo_seq OWNED BY sga_docente.periodos_evaluacion.id_periodo;


--
-- Name: promedios_anuales; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.promedios_anuales (
    id_promedio_anual integer NOT NULL,
    id_matricula integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    promedio_anual numeric(4,2),
    nota_cualitativa sga_docente.nota_cualitativa_t,
    registrado_por integer NOT NULL,
    calculado_en timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: promedios_anuales_detalle; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.promedios_anuales_detalle (
    id_detalle integer NOT NULL,
    id_promedio_anual integer NOT NULL,
    id_promedio_trim integer NOT NULL
);


--
-- Name: promedios_anuales_detalle_id_detalle_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.promedios_anuales_detalle_id_detalle_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promedios_anuales_detalle_id_detalle_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.promedios_anuales_detalle_id_detalle_seq OWNED BY sga_docente.promedios_anuales_detalle.id_detalle;


--
-- Name: promedios_anuales_id_promedio_anual_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.promedios_anuales_id_promedio_anual_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promedios_anuales_id_promedio_anual_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.promedios_anuales_id_promedio_anual_seq OWNED BY sga_docente.promedios_anuales.id_promedio_anual;


--
-- Name: promedios_trimestrales; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.promedios_trimestrales (
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


--
-- Name: promedios_trimestrales_id_promedio_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.promedios_trimestrales_id_promedio_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: promedios_trimestrales_id_promedio_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.promedios_trimestrales_id_promedio_seq OWNED BY sga_docente.promedios_trimestrales.id_promedio;


--
-- Name: resumen_asistencia; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.resumen_asistencia (
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


--
-- Name: resumen_asistencia_id_resumen_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.resumen_asistencia_id_resumen_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: resumen_asistencia_id_resumen_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.resumen_asistencia_id_resumen_seq OWNED BY sga_docente.resumen_asistencia.id_resumen;


--
-- Name: seguimiento_academico; Type: TABLE; Schema: sga_docente; Owner: -
--

CREATE TABLE sga_docente.seguimiento_academico (
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


--
-- Name: TABLE seguimiento_academico; Type: COMMENT; Schema: sga_docente; Owner: -
--

COMMENT ON TABLE sga_docente.seguimiento_academico IS '[C11] Notas de seguimiento por DECE, tutores o directivos; categorizado por tipo';


--
-- Name: seguimiento_academico_id_seguimiento_seq; Type: SEQUENCE; Schema: sga_docente; Owner: -
--

CREATE SEQUENCE sga_docente.seguimiento_academico_id_seguimiento_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seguimiento_academico_id_seguimiento_seq; Type: SEQUENCE OWNED BY; Schema: sga_docente; Owner: -
--

ALTER SEQUENCE sga_docente.seguimiento_academico_id_seguimiento_seq OWNED BY sga_docente.seguimiento_academico.id_seguimiento;


--
-- Name: anos_lectivos; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.anos_lectivos (
    id_ano_lectivo integer NOT NULL,
    nombre character varying(20) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    es_actual boolean DEFAULT false NOT NULL,
    creado_por integer,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: anos_lectivos_id_ano_lectivo_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.anos_lectivos_id_ano_lectivo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: anos_lectivos_id_ano_lectivo_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.anos_lectivos_id_ano_lectivo_seq OWNED BY sga_principal.anos_lectivos.id_ano_lectivo;


--
-- Name: asignaciones; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.asignaciones (
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


--
-- Name: COLUMN asignaciones.id_paralelo; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.asignaciones.id_paralelo IS '[C2] Permite asignar distintos docentes para la misma asignatura en paralelos diferentes';


--
-- Name: asignaciones_id_asignacion_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.asignaciones_id_asignacion_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: asignaciones_id_asignacion_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.asignaciones_id_asignacion_seq OWNED BY sga_principal.asignaciones.id_asignacion;


--
-- Name: asignaturas; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.asignaturas (
    id_asignatura integer NOT NULL,
    nombre character varying(100) NOT NULL,
    codigo character varying(20),
    descripcion text,
    horas_semana smallint,
    activa boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: asignaturas_id_asignatura_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.asignaturas_id_asignatura_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: asignaturas_id_asignatura_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.asignaturas_id_asignatura_seq OWNED BY sga_principal.asignaturas.id_asignatura;


--
-- Name: asignaturas_por_nivel; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.asignaturas_por_nivel (
    id_asignatura integer NOT NULL,
    id_nivel integer NOT NULL,
    tipo_escala sga_principal.tipo_escala_t NOT NULL
);


--
-- Name: auditoria; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.auditoria (
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


--
-- Name: TABLE auditoria; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON TABLE sga_principal.auditoria IS '[FIX-3] Tabla unificada de auditor├¡a para ambos m├│dulos. Usar schema_origen = ''PRINCIPAL'' o ''DOCENTE'' al insertar desde cada m├│dulo.';


--
-- Name: COLUMN auditoria.schema_origen; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.auditoria.schema_origen IS '[FIX-3] M├│dulo que origin├│ el evento. PRINCIPAL = sga_principal, DOCENTE = sga_docente.';


--
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.auditoria_id_auditoria_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.auditoria_id_auditoria_seq OWNED BY sga_principal.auditoria.id_auditoria;


--
-- Name: documentos_matricula; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.documentos_matricula (
    id_documento integer NOT NULL,
    id_matricula integer NOT NULL,
    tipo_documento sga_principal.tipo_documento_t NOT NULL,
    nombre_archivo character varying(200) NOT NULL,
    ruta_archivo character varying(500) NOT NULL,
    fecha_subida timestamp with time zone DEFAULT now() NOT NULL,
    subido_por integer
);


--
-- Name: COLUMN documentos_matricula.tipo_documento; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.documentos_matricula.tipo_documento IS '[C12] Cat├ílogo cerrado de documentos requeridos en el proceso de matr├¡cula';


--
-- Name: documentos_matricula_id_documento_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.documentos_matricula_id_documento_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documentos_matricula_id_documento_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.documentos_matricula_id_documento_seq OWNED BY sga_principal.documentos_matricula.id_documento;


--
-- Name: escala_calificaciones; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.escala_calificaciones (
    id_escala integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    id_nivel integer NOT NULL,
    nota_minima numeric(4,2) NOT NULL,
    nota_maxima numeric(4,2) NOT NULL,
    equivalente_cualitativo character varying(5),
    descripcion character varying(100),
    CONSTRAINT escala_check CHECK ((nota_minima < nota_maxima))
);


--
-- Name: COLUMN escala_calificaciones.id_nivel; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.escala_calificaciones.id_nivel IS '[C6] Diferencia la escala por nivel (Inicial=cualitativa, B├ísica Media+=cuantitativa)';


--
-- Name: escala_calificaciones_id_escala_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.escala_calificaciones_id_escala_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: escala_calificaciones_id_escala_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.escala_calificaciones_id_escala_seq OWNED BY sga_principal.escala_calificaciones.id_escala;


--
-- Name: estudiantes; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.estudiantes (
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


--
-- Name: COLUMN estudiantes.origen_listado; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.estudiantes.origen_listado IS '[C10] Tipo de ingreso del estudiante: NUEVO, TRANSFERIDO, REPITENTE, REINGRESO';


--
-- Name: estudiantes_id_estudiante_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.estudiantes_id_estudiante_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estudiantes_id_estudiante_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.estudiantes_id_estudiante_seq OWNED BY sga_principal.estudiantes.id_estudiante;


--
-- Name: fichas_estudiante; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.fichas_estudiante (
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


--
-- Name: fichas_estudiante_id_ficha_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.fichas_estudiante_id_ficha_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fichas_estudiante_id_ficha_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.fichas_estudiante_id_ficha_seq OWNED BY sga_principal.fichas_estudiante.id_ficha;


--
-- Name: grados; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.grados (
    id_grado integer NOT NULL,
    id_nivel integer NOT NULL,
    nombre character varying(60) NOT NULL,
    orden smallint NOT NULL,
    capacidad_max smallint,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: grados_id_grado_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.grados_id_grado_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: grados_id_grado_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.grados_id_grado_seq OWNED BY sga_principal.grados.id_grado;


--
-- Name: historial_promocion; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.historial_promocion (
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


--
-- Name: TABLE historial_promocion; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON TABLE sga_principal.historial_promocion IS '[C13] Registro permanente del resultado de cada estudiante al cierre de a├▒o lectivo';


--
-- Name: historial_promocion_id_historial_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.historial_promocion_id_historial_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: historial_promocion_id_historial_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.historial_promocion_id_historial_seq OWNED BY sga_principal.historial_promocion.id_historial;


--
-- Name: horarios; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.horarios (
    id_horario integer NOT NULL,
    id_asignacion integer NOT NULL,
    id_periodo_diario integer NOT NULL,
    dia_semana sga_principal.dia_semana_t NOT NULL
);


--
-- Name: horarios_id_horario_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.horarios_id_horario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: horarios_id_horario_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.horarios_id_horario_seq OWNED BY sga_principal.horarios.id_horario;


--
-- Name: matriculas; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.matriculas (
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


--
-- Name: COLUMN matriculas.id_paralelo; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.matriculas.id_paralelo IS '[C1] Identifica el aula/paralelo espec├¡fico del estudiante en el a├▒o lectivo';


--
-- Name: matriculas_id_matricula_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.matriculas_id_matricula_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: matriculas_id_matricula_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.matriculas_id_matricula_seq OWNED BY sga_principal.matriculas.id_matricula;


--
-- Name: niveles_educativos; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.niveles_educativos (
    id_nivel integer NOT NULL,
    nombre character varying(60) NOT NULL,
    tipo_escala sga_principal.tipo_escala_t NOT NULL,
    grado_inicio smallint NOT NULL,
    grado_fin smallint NOT NULL
);


--
-- Name: niveles_educativos_id_nivel_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.niveles_educativos_id_nivel_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: niveles_educativos_id_nivel_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.niveles_educativos_id_nivel_seq OWNED BY sga_principal.niveles_educativos.id_nivel;


--
-- Name: paralelos; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.paralelos (
    id_paralelo integer NOT NULL,
    id_grado integer NOT NULL,
    letra character(1) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: paralelos_ano_lectivo; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.paralelos_ano_lectivo (
    id_paralelo_al integer NOT NULL,
    id_paralelo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    capacidad_max smallint DEFAULT 35 NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE paralelos_ano_lectivo; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON TABLE sga_principal.paralelos_ano_lectivo IS '[C5] Activa paralelos por a├▒o lectivo y registra capacidad real de cada aula';


--
-- Name: paralelos_ano_lectivo_id_paralelo_al_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: paralelos_ano_lectivo_id_paralelo_al_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq OWNED BY sga_principal.paralelos_ano_lectivo.id_paralelo_al;


--
-- Name: paralelos_id_paralelo_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.paralelos_id_paralelo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: paralelos_id_paralelo_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.paralelos_id_paralelo_seq OWNED BY sga_principal.paralelos.id_paralelo;


--
-- Name: periodos_diarios; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.periodos_diarios (
    id_periodo_diario integer NOT NULL,
    numero smallint NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    aplica_nivel sga_principal.nivel_educativo_t
);


--
-- Name: periodos_diarios_id_periodo_diario_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.periodos_diarios_id_periodo_diario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: periodos_diarios_id_periodo_diario_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.periodos_diarios_id_periodo_diario_seq OWNED BY sga_principal.periodos_diarios.id_periodo_diario;


--
-- Name: personas; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.personas (
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


--
-- Name: COLUMN personas.fecha_ingreso_inst; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.personas.fecha_ingreso_inst IS '[C9] Fecha en que el docente/personal ingres├│ a la instituci├│n';


--
-- Name: COLUMN personas.cargo; Type: COMMENT; Schema: sga_principal; Owner: -
--

COMMENT ON COLUMN sga_principal.personas.cargo IS '[C9] Cargo institucional del personal (Docente, Rector, DECE, Administrativo, etc.)';


--
-- Name: personas_id_persona_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.personas_id_persona_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: personas_id_persona_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.personas_id_persona_seq OWNED BY sga_principal.personas.id_persona;


--
-- Name: representantes; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.representantes (
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


--
-- Name: representantes_id_representante_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.representantes_id_representante_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: representantes_id_representante_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.representantes_id_representante_seq OWNED BY sga_principal.representantes.id_representante;


--
-- Name: roles; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.roles (
    id_rol integer NOT NULL,
    nombre character varying(30) NOT NULL,
    descripcion text,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: roles_id_rol_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.roles_id_rol_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_rol_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.roles_id_rol_seq OWNED BY sga_principal.roles.id_rol;


--
-- Name: usuario_roles; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.usuario_roles (
    id_usuario integer NOT NULL,
    id_rol integer NOT NULL,
    asignado_por integer,
    asignado_el timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: usuarios; Type: TABLE; Schema: sga_principal; Owner: -
--

CREATE TABLE sga_principal.usuarios (
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


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE; Schema: sga_principal; Owner: -
--

CREATE SEQUENCE sga_principal.usuarios_id_usuario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE OWNED BY; Schema: sga_principal; Owner: -
--

ALTER SEQUENCE sga_principal.usuarios_id_usuario_seq OWNED BY sga_principal.usuarios.id_usuario;


--
-- Name: comentarios; Type: TABLE; Schema: sga_soporte; Owner: -
--

CREATE TABLE sga_soporte.comentarios (
    id_comentario bigint NOT NULL,
    autor character varying(100) NOT NULL,
    contenido text NOT NULL,
    fecha_creacion timestamp(6) without time zone NOT NULL,
    nota_interna boolean NOT NULL,
    id_ticket bigint NOT NULL
);


--
-- Name: comentarios_id_comentario_seq; Type: SEQUENCE; Schema: sga_soporte; Owner: -
--

CREATE SEQUENCE sga_soporte.comentarios_id_comentario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comentarios_id_comentario_seq; Type: SEQUENCE OWNED BY; Schema: sga_soporte; Owner: -
--

ALTER SEQUENCE sga_soporte.comentarios_id_comentario_seq OWNED BY sga_soporte.comentarios.id_comentario;


--
-- Name: tickets; Type: TABLE; Schema: sga_soporte; Owner: -
--

CREATE TABLE sga_soporte.tickets (
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


--
-- Name: tickets_id_ticket_seq; Type: SEQUENCE; Schema: sga_soporte; Owner: -
--

CREATE SEQUENCE sga_soporte.tickets_id_ticket_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tickets_id_ticket_seq; Type: SEQUENCE OWNED BY; Schema: sga_soporte; Owner: -
--

ALTER SEQUENCE sga_soporte.tickets_id_ticket_seq OWNED BY sga_soporte.tickets.id_ticket;


--
-- Name: actividades id_actividad; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.actividades ALTER COLUMN id_actividad SET DEFAULT nextval('sga_docente.actividades_id_actividad_seq'::regclass);


--
-- Name: asistencias id_asistencia; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias ALTER COLUMN id_asistencia SET DEFAULT nextval('sga_docente.asistencias_id_asistencia_seq'::regclass);


--
-- Name: calificaciones id_calificacion; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.calificaciones ALTER COLUMN id_calificacion SET DEFAULT nextval('sga_docente.calificaciones_id_calificacion_seq'::regclass);


--
-- Name: periodos_evaluacion id_periodo; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.periodos_evaluacion ALTER COLUMN id_periodo SET DEFAULT nextval('sga_docente.periodos_evaluacion_id_periodo_seq'::regclass);


--
-- Name: promedios_anuales id_promedio_anual; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales ALTER COLUMN id_promedio_anual SET DEFAULT nextval('sga_docente.promedios_anuales_id_promedio_anual_seq'::regclass);


--
-- Name: promedios_anuales_detalle id_detalle; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales_detalle ALTER COLUMN id_detalle SET DEFAULT nextval('sga_docente.promedios_anuales_detalle_id_detalle_seq'::regclass);


--
-- Name: promedios_trimestrales id_promedio; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_trimestrales ALTER COLUMN id_promedio SET DEFAULT nextval('sga_docente.promedios_trimestrales_id_promedio_seq'::regclass);


--
-- Name: resumen_asistencia id_resumen; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.resumen_asistencia ALTER COLUMN id_resumen SET DEFAULT nextval('sga_docente.resumen_asistencia_id_resumen_seq'::regclass);


--
-- Name: seguimiento_academico id_seguimiento; Type: DEFAULT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.seguimiento_academico ALTER COLUMN id_seguimiento SET DEFAULT nextval('sga_docente.seguimiento_academico_id_seguimiento_seq'::regclass);


--
-- Name: anos_lectivos id_ano_lectivo; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.anos_lectivos ALTER COLUMN id_ano_lectivo SET DEFAULT nextval('sga_principal.anos_lectivos_id_ano_lectivo_seq'::regclass);


--
-- Name: asignaciones id_asignacion; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones ALTER COLUMN id_asignacion SET DEFAULT nextval('sga_principal.asignaciones_id_asignacion_seq'::regclass);


--
-- Name: asignaturas id_asignatura; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaturas ALTER COLUMN id_asignatura SET DEFAULT nextval('sga_principal.asignaturas_id_asignatura_seq'::regclass);


--
-- Name: auditoria id_auditoria; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.auditoria ALTER COLUMN id_auditoria SET DEFAULT nextval('sga_principal.auditoria_id_auditoria_seq'::regclass);


--
-- Name: documentos_matricula id_documento; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.documentos_matricula ALTER COLUMN id_documento SET DEFAULT nextval('sga_principal.documentos_matricula_id_documento_seq'::regclass);


--
-- Name: escala_calificaciones id_escala; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.escala_calificaciones ALTER COLUMN id_escala SET DEFAULT nextval('sga_principal.escala_calificaciones_id_escala_seq'::regclass);


--
-- Name: estudiantes id_estudiante; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.estudiantes ALTER COLUMN id_estudiante SET DEFAULT nextval('sga_principal.estudiantes_id_estudiante_seq'::regclass);


--
-- Name: fichas_estudiante id_ficha; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.fichas_estudiante ALTER COLUMN id_ficha SET DEFAULT nextval('sga_principal.fichas_estudiante_id_ficha_seq'::regclass);


--
-- Name: grados id_grado; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.grados ALTER COLUMN id_grado SET DEFAULT nextval('sga_principal.grados_id_grado_seq'::regclass);


--
-- Name: historial_promocion id_historial; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion ALTER COLUMN id_historial SET DEFAULT nextval('sga_principal.historial_promocion_id_historial_seq'::regclass);


--
-- Name: horarios id_horario; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.horarios ALTER COLUMN id_horario SET DEFAULT nextval('sga_principal.horarios_id_horario_seq'::regclass);


--
-- Name: matriculas id_matricula; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas ALTER COLUMN id_matricula SET DEFAULT nextval('sga_principal.matriculas_id_matricula_seq'::regclass);


--
-- Name: niveles_educativos id_nivel; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.niveles_educativos ALTER COLUMN id_nivel SET DEFAULT nextval('sga_principal.niveles_educativos_id_nivel_seq'::regclass);


--
-- Name: paralelos id_paralelo; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos ALTER COLUMN id_paralelo SET DEFAULT nextval('sga_principal.paralelos_id_paralelo_seq'::regclass);


--
-- Name: paralelos_ano_lectivo id_paralelo_al; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos_ano_lectivo ALTER COLUMN id_paralelo_al SET DEFAULT nextval('sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq'::regclass);


--
-- Name: periodos_diarios id_periodo_diario; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.periodos_diarios ALTER COLUMN id_periodo_diario SET DEFAULT nextval('sga_principal.periodos_diarios_id_periodo_diario_seq'::regclass);


--
-- Name: personas id_persona; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.personas ALTER COLUMN id_persona SET DEFAULT nextval('sga_principal.personas_id_persona_seq'::regclass);


--
-- Name: representantes id_representante; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.representantes ALTER COLUMN id_representante SET DEFAULT nextval('sga_principal.representantes_id_representante_seq'::regclass);


--
-- Name: roles id_rol; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.roles ALTER COLUMN id_rol SET DEFAULT nextval('sga_principal.roles_id_rol_seq'::regclass);


--
-- Name: usuarios id_usuario; Type: DEFAULT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuarios ALTER COLUMN id_usuario SET DEFAULT nextval('sga_principal.usuarios_id_usuario_seq'::regclass);


--
-- Name: comentarios id_comentario; Type: DEFAULT; Schema: sga_soporte; Owner: -
--

ALTER TABLE ONLY sga_soporte.comentarios ALTER COLUMN id_comentario SET DEFAULT nextval('sga_soporte.comentarios_id_comentario_seq'::regclass);


--
-- Name: tickets id_ticket; Type: DEFAULT; Schema: sga_soporte; Owner: -
--

ALTER TABLE ONLY sga_soporte.tickets ALTER COLUMN id_ticket SET DEFAULT nextval('sga_soporte.tickets_id_ticket_seq'::regclass);


--
-- Data for Name: auth_permission; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_permission (id, name, content_type_id, codename) FROM stdin;
1	Can add log entry	1	add_logentry
2	Can change log entry	1	change_logentry
3	Can delete log entry	1	delete_logentry
4	Can view log entry	1	view_logentry
5	Can add permission	3	add_permission
6	Can change permission	3	change_permission
7	Can delete permission	3	delete_permission
8	Can view permission	3	view_permission
9	Can add group	2	add_group
10	Can change group	2	change_group
11	Can delete group	2	delete_group
12	Can view group	2	view_group
13	Can add user	4	add_user
14	Can change user	4	change_user
15	Can delete user	4	delete_user
16	Can view user	4	view_user
17	Can add content type	5	add_contenttype
18	Can change content type	5	change_contenttype
19	Can delete content type	5	delete_contenttype
20	Can view content type	5	view_contenttype
21	Can add session	6	add_session
22	Can change session	6	change_session
23	Can delete session	6	delete_session
24	Can view session	6	view_session
25	Can add actividad	7	add_actividad
26	Can change actividad	7	change_actividad
27	Can delete actividad	7	delete_actividad
28	Can view actividad	7	view_actividad
29	Can add periodo evaluacion	10	add_periodoevaluacion
30	Can change periodo evaluacion	10	change_periodoevaluacion
31	Can delete periodo evaluacion	10	delete_periodoevaluacion
32	Can view periodo evaluacion	10	view_periodoevaluacion
33	Can add promedio anual	11	add_promedioanual
34	Can change promedio anual	11	change_promedioanual
35	Can delete promedio anual	11	delete_promedioanual
36	Can view promedio anual	11	view_promedioanual
37	Can add calificacion	9	add_calificacion
38	Can change calificacion	9	change_calificacion
39	Can delete calificacion	9	delete_calificacion
40	Can view calificacion	9	view_calificacion
41	Can add asistencia	8	add_asistencia
42	Can change asistencia	8	change_asistencia
43	Can delete asistencia	8	delete_asistencia
44	Can view asistencia	8	view_asistencia
45	Can add promedio trimestral	13	add_promediotrimestral
46	Can change promedio trimestral	13	change_promediotrimestral
47	Can delete promedio trimestral	13	delete_promediotrimestral
48	Can view promedio trimestral	13	view_promediotrimestral
49	Can add promedio anual detalle	12	add_promedioanualdetalle
50	Can change promedio anual detalle	12	change_promedioanualdetalle
51	Can delete promedio anual detalle	12	delete_promedioanualdetalle
52	Can view promedio anual detalle	12	view_promedioanualdetalle
53	Can add resumen asistencia	14	add_resumenasistencia
54	Can change resumen asistencia	14	change_resumenasistencia
55	Can delete resumen asistencia	14	delete_resumenasistencia
56	Can view resumen asistencia	14	view_resumenasistencia
57	Can add seguimiento academico	15	add_seguimientoacademico
58	Can change seguimiento academico	15	change_seguimientoacademico
59	Can delete seguimiento academico	15	delete_seguimientoacademico
60	Can view seguimiento academico	15	view_seguimientoacademico
\.


--
-- Data for Name: auth_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) FROM stdin;
\.


--
-- Data for Name: auth_user_groups; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_user_groups (id, user_id, group_id) FROM stdin;
\.


--
-- Data for Name: auth_user_user_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_user_user_permissions (id, user_id, permission_id) FROM stdin;
\.


--
-- Data for Name: django_admin_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_admin_log (id, action_time, object_id, object_repr, action_flag, change_message, content_type_id, user_id) FROM stdin;
\.


--
-- Data for Name: django_content_type; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.django_content_type (id, app_label, model) FROM stdin;
1	admin	logentry
2	auth	group
3	auth	permission
4	auth	user
5	contenttypes	contenttype
6	sessions	session
7	sga_docente_app	actividad
8	sga_docente_app	asistencia
9	sga_docente_app	calificacion
10	sga_docente_app	periodoevaluacion
11	sga_docente_app	promedioanual
12	sga_docente_app	promedioanualdetalle
13	sga_docente_app	promediotrimestral
14	sga_docente_app	resumenasistencia
15	sga_docente_app	seguimientoacademico
\.


--
-- Data for Name: actividades; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.actividades (id_actividad, id_asignacion, id_periodo, tipo, nombre, descripcion, fecha_entrega, ponderacion, nota_maxima, es_sumativa, fecha_creacion) FROM stdin;
\.


--
-- Data for Name: asistencias; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.asistencias (id_asistencia, id_matricula, id_asignacion, id_periodo, fecha, estado, justificacion, registrado_por, fecha_registro, fecha_actualizacion) FROM stdin;
\.


--
-- Data for Name: auth_group; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.auth_group (id, name) FROM stdin;
\.


--
-- Data for Name: auth_group_permissions; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.auth_group_permissions (id, group_id, permission_id) FROM stdin;
\.


--
-- Data for Name: auth_permission; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.auth_permission (id, name, content_type_id, codename) FROM stdin;
\.


--
-- Data for Name: auth_user; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.auth_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined) FROM stdin;
\.


--
-- Data for Name: auth_user_groups; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.auth_user_groups (id, user_id, group_id) FROM stdin;
\.


--
-- Data for Name: auth_user_user_permissions; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.auth_user_user_permissions (id, user_id, permission_id) FROM stdin;
\.


--
-- Data for Name: calificaciones; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.calificaciones (id_calificacion, id_actividad, id_matricula, nota, nota_cualitativa, observacion, registrado_por, fecha_registro, fecha_actualizacion) FROM stdin;
\.


--
-- Data for Name: django_admin_log; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.django_admin_log (id, action_time, object_id, object_repr, action_flag, change_message, content_type_id, user_id) FROM stdin;
\.


--
-- Data for Name: django_content_type; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.django_content_type (id, app_label, model) FROM stdin;
\.


--
-- Data for Name: django_migrations; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.django_migrations (id, app, name, applied) FROM stdin;
1	contenttypes	0001_initial	2026-07-06 13:49:56.442364+00
2	auth	0001_initial	2026-07-06 13:49:59.621649+00
3	admin	0001_initial	2026-07-06 13:50:00.471964+00
4	admin	0002_logentry_remove_auto_add	2026-07-06 13:50:00.581781+00
5	admin	0003_logentry_add_action_flag_choices	2026-07-06 13:50:00.890777+00
6	contenttypes	0002_remove_content_type_name	2026-07-06 13:50:01.536201+00
7	auth	0002_alter_permission_name_max_length	2026-07-06 13:50:01.954674+00
8	auth	0003_alter_user_email_max_length	2026-07-06 13:50:03.414373+00
9	auth	0004_alter_user_username_opts	2026-07-06 13:50:04.1026+00
10	auth	0005_alter_user_last_login_null	2026-07-06 13:50:06.279197+00
11	auth	0006_require_contenttypes_0002	2026-07-06 13:50:07.064657+00
12	auth	0007_alter_validators_add_error_messages	2026-07-06 13:50:08.012022+00
13	auth	0008_alter_user_username_max_length	2026-07-06 13:50:08.538137+00
14	auth	0009_alter_user_last_name_max_length	2026-07-06 13:50:08.956624+00
15	auth	0010_alter_group_name_max_length	2026-07-06 13:50:09.369371+00
16	auth	0011_update_proxy_permissions	2026-07-06 13:50:09.578289+00
17	auth	0012_alter_user_first_name_max_length	2026-07-06 13:50:10.217764+00
\.


--
-- Data for Name: periodos_evaluacion; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.periodos_evaluacion (id_periodo, id_ano_lectivo, tipo, nombre, fecha_inicio, fecha_fin, activo) FROM stdin;
1	1	PRIMER_TRIMESTRE	Primer Trimestre 2026-2027	2026-05-01	2026-08-31	t
\.


--
-- Data for Name: promedios_anuales; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.promedios_anuales (id_promedio_anual, id_matricula, id_asignacion, id_ano_lectivo, promedio_anual, nota_cualitativa, registrado_por, calculado_en) FROM stdin;
\.


--
-- Data for Name: promedios_anuales_detalle; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.promedios_anuales_detalle (id_detalle, id_promedio_anual, id_promedio_trim) FROM stdin;
\.


--
-- Data for Name: promedios_trimestrales; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.promedios_trimestrales (id_promedio, id_matricula, id_asignacion, id_periodo, promedio_formativo, nota_sumativa, promedio_trimestral, nota_cualitativa, calculado_en) FROM stdin;
\.


--
-- Data for Name: resumen_asistencia; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.resumen_asistencia (id_resumen, id_matricula, id_asignacion, id_periodo, total_presentes, total_ausentes, total_justificados, total_atrasos, calculado_en) FROM stdin;
\.


--
-- Data for Name: seguimiento_academico; Type: TABLE DATA; Schema: sga_docente; Owner: -
--

COPY sga_docente.seguimiento_academico (id_seguimiento, id_matricula, id_periodo, categoria, descripcion, acciones_tomadas, requiere_followup, fecha_evento, registrado_por, fecha_registro) FROM stdin;
\.


--
-- Data for Name: anos_lectivos; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.anos_lectivos (id_ano_lectivo, nombre, fecha_inicio, fecha_fin, es_actual, creado_por, fecha_creacion) FROM stdin;
1	2026 - 2027	2026-05-01	2027-02-28	t	1	2026-06-02 03:15:32.929711+00
\.


--
-- Data for Name: asignaciones; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.asignaciones (id_asignacion, id_docente, id_asignatura, id_grado, id_paralelo, id_ano_lectivo, es_tutor, tipo, activo, asignado_por, fecha_asignacion) FROM stdin;
\.


--
-- Data for Name: asignaturas; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.asignaturas (id_asignatura, nombre, codigo, descripcion, horas_semana, activa, fecha_creacion) FROM stdin;
1	Lengua y Literatura	LEN	\N	\N	t	2026-05-29 04:18:24.904419+00
2	Matem├ítica	MAT	\N	\N	t	2026-05-29 04:18:24.904419+00
3	Ciencias Naturales	CN	\N	\N	t	2026-05-29 04:18:24.904419+00
4	Ciencias Sociales	CS	\N	\N	t	2026-05-29 04:18:24.904419+00
5	Educaci├│n Cultural y Art├¡stica	ECA	\N	\N	t	2026-05-29 04:18:24.904419+00
6	Educaci├│n F├¡sica	EF	\N	\N	t	2026-05-29 04:18:24.904419+00
7	Ingl├®s	ING	\N	\N	t	2026-05-29 04:18:24.904419+00
8	Animaci├│n a la Lectura	AL	\N	\N	t	2026-05-29 04:18:24.904419+00
9	Proyectos Escolares	PE	\N	\N	t	2026-05-29 04:18:24.904419+00
10	Desarrollo Personal y Social	DPS	\N	\N	t	2026-05-29 04:18:24.904419+00
11	Descubrimiento del Medio Natural y Cultural	DMNC	\N	\N	t	2026-05-29 04:18:24.904419+00
12	Matem├íticas	MAT-01	\N	\N	t	2026-06-02 04:03:09.002662+00
\.


--
-- Data for Name: asignaturas_por_nivel; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.asignaturas_por_nivel (id_asignatura, id_nivel, tipo_escala) FROM stdin;
\.


--
-- Data for Name: auditoria; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.auditoria (id_auditoria, schema_origen, id_usuario, username, accion, tabla_afectada, registro_id, descripcion, ip_address, user_agent, hmac, fecha) FROM stdin;
\.


--
-- Data for Name: documentos_matricula; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.documentos_matricula (id_documento, id_matricula, tipo_documento, nombre_archivo, ruta_archivo, fecha_subida, subido_por) FROM stdin;
\.


--
-- Data for Name: escala_calificaciones; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.escala_calificaciones (id_escala, id_ano_lectivo, id_nivel, nota_minima, nota_maxima, equivalente_cualitativo, descripcion) FROM stdin;
\.


--
-- Data for Name: estudiantes; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.estudiantes (id_estudiante, cedula, codigo_estudiante, nombres, apellidos, fecha_nacimiento, genero, direccion, telefono, telefono_alt, correo, discapacidad, tipo_discapacidad, porcentaje_disc, id_representante, origen_listado, estado, foto_url, creado_por, fecha_creacion, fecha_actualizacion, carnet_conadis, nacionalidad, etnia, lugar_nacimiento, vive_con, numeros_hermanos, beneficio_social) FROM stdin;
1	\N	\N	Ana Mar├¡a	Gonz├ílez Reyes	\N	\N	\N	\N	\N	\N	f	\N	\N	\N	\N	ACTIVO	\N	1	2026-06-02 04:03:28.015797+00	2026-06-02 04:03:28.015797+00	\N	\N	\N	\N	\N	\N	f
7	1353464074	\N	JOYMI MILAGROS	AMAGUA PAREDES	\N	\N	\N	\N	\N	ampajomi14810424@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:07.513604+00	2026-07-08 19:28:07.513604+00	\N	Ecuatoriana	\N	\N	\N	\N	f
8	0964892095	\N	YEIMY ISABELLA	CATAGUA PARRAGA	\N	\N	\N	\N	\N	capayeis14833522@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:08.244245+00	2026-07-08 19:28:08.244245+00	\N	Ecuatoriana	\N	\N	\N	\N	f
9	0964533913	\N	JANDER MOISES	CEDE├æO RODRIGUEZ	\N	\N	\N	\N	\N	cerojamo14811752@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:08.953773+00	2026-07-08 19:28:08.953773+00	\N	Ecuatoriana	\N	\N	\N	\N	f
10	1252540669	\N	EMMANUEL ALEJANDRO	CHAVARRIA PINCAY	\N	\N	\N	\N	\N	chpiemal14732931@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:09.835521+00	2026-07-08 19:28:09.835521+00	\N	Ecuatoriana	\N	\N	\N	\N	f
11	0965078967	\N	ALICIA VICTORIA	CHAVEZ MACIAS	\N	\N	\N	\N	\N	chmaalvi14885883@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:10.783193+00	2026-07-08 19:28:10.783193+00	\N	Ecuatoriana	\N	\N	\N	\N	f
12	0964616148	\N	ANDER EMIR	CHILAN SOLORZANO	\N	\N	\N	\N	\N	chsoanem14840300@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:11.41712+00	2026-07-08 19:28:11.41712+00	\N	Ecuatoriana	\N	\N	\N	\N	f
13	1252528946	\N	CARLOS THOMAS	DELGADO ZAMBRANO	\N	\N	\N	\N	\N	dezacath14813120@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:11.993575+00	2026-07-08 19:28:11.993575+00	\N	Ecuatoriana	\N	\N	\N	\N	f
14	0964914279	\N	MARIANA NOHEMY	GARCIA MERO	\N	\N	\N	\N	\N	gavenagu14763212@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:12.462324+00	2026-07-08 19:28:12.462324+00	\N	Ecuatoriana	\N	\N	\N	\N	f
15	1252500267	\N	NARCISA GUADALUPE	GARCIA VELEZ	\N	\N	\N	\N	\N	hocemaar14763310@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:12.927485+00	2026-07-08 19:28:12.927485+00	\N	Ecuatoriana	\N	\N	\N	\N	f
16	0965034499	\N	MARIETZY ARISBETH	HOLGUIN CEDE├æO	\N	\N	\N	\N	\N	ingasnal15702639@estudiantes2.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:14.394462+00	2026-07-08 19:28:14.394462+00	\N	Ecuatoriana	\N	\N	\N	\N	f
17	0751919648	\N	SNAIDER ALEXANDER	INTRIAGO GARCIA	\N	\N	\N	\N	\N	intualya14811668@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:15.946439+00	2026-07-08 19:28:15.946439+00	\N	Ecuatoriana	\N	\N	\N	\N	f
18	0964875538	\N	ALEXA YAMILET	INTRIAGO TUAREZ	\N	\N	\N	\N	\N	lamojosu14732934@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:17.654937+00	2026-07-08 19:28:17.654937+00	\N	Ecuatoriana	\N	\N	\N	\N	f
19	0964544274	\N	JOSEPH SURIEL	LAAZ MONTECE	\N	\N	\N	\N	\N	lojaanma14810677@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:19.07914+00	2026-07-08 19:28:19.07914+00	\N	Ecuatoriana	\N	\N	\N	\N	f
20	0965018583	\N	ANGELICA MARILUZ	LOOR JAIME	\N	\N	\N	\N	\N	lomaarju14856919@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:19.540823+00	2026-07-08 19:28:19.540823+00	\N	Ecuatoriana	\N	\N	\N	\N	f
21	0964505879	\N	ARELYS JULIETH	LOPEZ MARCILLO	\N	\N	\N	\N	\N	lujaedga14811454@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:20.011247+00	2026-07-08 19:28:20.011247+00	\N	Ecuatoriana	\N	\N	\N	\N	f
22	0964510648	\N	EDUAR GABRIEL	LUCAS JAMA	\N	\N	\N	\N	\N	mevalist14728799@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:20.478828+00	2026-07-08 19:28:20.478828+00	\N	Ecuatoriana	\N	\N	\N	\N	f
23	0964893234	\N	LISBETH STEFANIA	MENDOZA VASQUEZ	\N	\N	\N	\N	\N	moalxaem16166351@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:20.94457+00	2026-07-08 19:28:20.94457+00	\N	Ecuatoriana	\N	\N	\N	\N	f
24	0964527907	\N	XAVIER EMIR	MORAN ALVARADO	\N	\N	\N	\N	\N	moansnja14812461@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:21.412638+00	2026-07-08 19:28:21.412638+00	\N	Ecuatoriana	\N	\N	\N	\N	f
25	0964455778	\N	SNAIDER JAVIER	MOREIRA ANDRADE	\N	\N	\N	\N	\N	momoalyu14714961@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:22.044164+00	2026-07-08 19:28:22.044164+00	\N	Ecuatoriana	\N	\N	\N	\N	f
26	0964911242	\N	ALEXA YURHEY	MORETA MORALES	\N	\N	\N	\N	\N	muavgrya16174283@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:23.154628+00	2026-07-08 19:28:23.154628+00	\N	Ecuatoriana	\N	\N	\N	\N	f
27	0964825657	\N	GRAZMELY YARDLEY	MU├æOZ AVILA	\N	\N	\N	\N	\N	oraraxga14847166@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:24.013468+00	2026-07-08 19:28:24.013468+00	\N	Ecuatoriana	\N	\N	\N	\N	f
28	0964525224	\N	AXEL GAEL	ORMAZA ARTEAGA	\N	\N	\N	\N	\N	pavalial14763222@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:25.021877+00	2026-07-08 19:28:25.021877+00	\N	Ecuatoriana	\N	\N	\N	\N	f
29	0964491062	\N	LIZ ALEXA	PARRAGA VALENZUELA	\N	\N	\N	\N	\N	pelakada14813329@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:25.834893+00	2026-07-08 19:28:25.834893+00	\N	Ecuatoriana	\N	\N	\N	\N	f
30	0964934731	\N	KATIHUSKA DANAE	PE├æAFIEL LAJE	\N	\N	\N	\N	\N	piloeiad14713526@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:26.652894+00	2026-07-08 19:28:26.652894+00	\N	Ecuatoriana	\N	\N	\N	\N	f
31	0964586903	\N	EINER ADRIEL	PINARGOTE LOZANO	\N	\N	\N	\N	\N	recamada16111208@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:28.137071+00	2026-07-08 19:28:28.137071+00	\N	Ecuatoriana	\N	\N	\N	\N	f
32	1252516958	\N	MARCOS DAVID	REYES CARDENAS	\N	\N	\N	\N	\N	ricasaju14730615@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:29.695642+00	2026-07-08 19:28:29.695642+00	\N	Ecuatoriana	\N	\N	\N	\N	f
33	0964540686	\N	SAMARA JULIETTE	RISCO CARRE├æO	\N	\N	\N	\N	\N	romadeez14812311@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:31.416787+00	2026-07-08 19:28:31.416787+00	\N	Ecuatoriana	\N	\N	\N	\N	f
34	1353484775	\N	DERECK EZEQUIELL	ROSADO MACIAS	\N	\N	\N	\N	\N	sabrkiai16162971@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:33.042731+00	2026-07-08 19:28:33.042731+00	\N	Ecuatoriana	\N	\N	\N	\N	f
35	1353564055	\N	KIMBERLY AILYN	SALDA├æA BRAVO	\N	\N	\N	\N	\N	sachwajo14812066@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:33.515518+00	2026-07-08 19:28:33.515518+00	\N	Ecuatoriana	\N	\N	\N	\N	f
36	0964451033	\N	WALTER JOHAN	SALTOS CHILAN	\N	\N	\N	\N	\N	sagamami14811172@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:34.313467+00	2026-07-08 19:28:34.313467+00	\N	Ecuatoriana	\N	\N	\N	\N	f
37	0965195092	\N	MARIA MILAGROS	SANCHEZ GARCIA	\N	\N	\N	\N	\N	sapithma16165957@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:35.235752+00	2026-07-08 19:28:35.235752+00	\N	Ecuatoriana	\N	\N	\N	\N	f
38	0964984371	\N	THIAGO MATEO	SANCHEZ PINARGOTE	\N	\N	\N	\N	\N	varedajo16165467@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:35.912966+00	2026-07-08 19:28:35.912966+00	\N	Ecuatoriana	\N	\N	\N	\N	f
39	0964622138	\N	DARIXON JOSUE	VALENCIA REYES	\N	\N	\N	\N	\N	zacemaza14814255@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:36.608714+00	2026-07-08 19:28:36.608714+00	\N	Ecuatoriana	\N	\N	\N	\N	f
40	0964958789	\N	MATEO ZABDIEL	ZAMBRANO CEDE├æO	\N	\N	\N	\N	\N	zapadoad14801398@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:37.476156+00	2026-07-08 19:28:37.476156+00	\N	Ecuatoriana	\N	\N	\N	\N	f
41	0964961890	\N	DORIAN ADRIAN	ZAMBRANO PARRAGA	\N	\N	\N	\N	\N	zadeaxez14813578@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:38.999427+00	2026-07-08 19:28:38.999427+00	\N	Ecuatoriana	\N	\N	\N	\N	f
42	0965130974	\N	AXEL EZEQUIEL	ZAMORA DELGADO	\N	\N	\N	\N	\N		f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:40.548938+00	2026-07-08 19:28:40.548938+00	\N	Ecuatoriana	\N	\N	\N	\N	f
6	1353490590	\N	DAVID SANTIAGO	ALCIVAR INTRIAGO	2002-05-08	M	av quevedo - las tecas	0983621086	\N	alindasa14810863@estudiantes3.edu.ec	f		\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:28:06.986931+00	2026-07-08 19:31:40.314389+00		Ecuatoriana	Mestizo/a	El empalme	Solo Madre	0	t
43	0963708177	\N	ANGELA MARIA	CARRASCO CATAGUA	\N	\N	\N	\N	\N	cacaanma14051224@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:35.485223+00	2026-07-08 19:44:35.485223+00	\N	Ecuatoriana	\N	\N	\N	\N	f
44	0964049381	\N	LIAM JOSE	CARRE├æO CRUZATTY	\N	\N	\N	\N	\N	cacrlijo14046724@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:35.899203+00	2026-07-08 19:44:35.899203+00	\N	Ecuatoriana	\N	\N	\N	\N	f
45	1353286261	\N	JOSHUA DAYAN	CATAGUA MOREIRA	\N	\N	\N	\N	\N	camojoda13950290@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:36.784729+00	2026-07-08 19:44:36.784729+00	\N	Ecuatoriana	\N	\N	\N	\N	f
46	0964200190	\N	ULBIO JUNIOR	CONFORME GANCHOZO	\N	\N	\N	\N	\N	cogaulju14043634@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:37.890604+00	2026-07-08 19:44:37.890604+00	\N	Ecuatoriana	\N	\N	\N	\N	f
47	0963913470	\N	ERICKA ZHARICK	CRUZATTI SANCHEZ	\N	\N	\N	\N	\N	crsaerzh16176913@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:38.729942+00	2026-07-08 19:44:38.729942+00	\N	Ecuatoriana	\N	\N	\N	\N	f
48	0963848882	\N	JESSICA SAMHARA	DELVALLE VERA	\N	\N	\N	\N	\N	devejesa14044754@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:39.528351+00	2026-07-08 19:44:39.528351+00	\N	Ecuatoriana	\N	\N	\N	\N	f
49	1353360009	\N	MAYEXY ESPERANZA	ESPINALES MENDOZA	\N	\N	\N	\N	\N	esmemaes13930738@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:40.47035+00	2026-07-08 19:44:40.47035+00	\N	Ecuatoriana	\N	\N	\N	\N	f
50	0964145767	\N	ANGELO NARCISO	ESPINALES RODRIGUEZ	\N	\N	\N	\N	\N	esroanna14108938@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:41.391459+00	2026-07-08 19:44:41.391459+00	\N	Ecuatoriana	\N	\N	\N	\N	f
51	0963913520	\N	EDUARDO JOEL	GARCIA MERO	\N	\N	\N	\N	\N	gameedjo15474303@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:42.73723+00	2026-07-08 19:44:42.73723+00	\N	Ecuatoriana	\N	\N	\N	\N	f
52	0963729678	\N	JULITZA SCARLETH	GARCIA RODRIGUEZ	\N	\N	\N	\N	\N	garojusc15475604@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:44.259781+00	2026-07-08 19:44:44.259781+00	\N	Ecuatoriana	\N	\N	\N	\N	f
53	1252409808	\N	JULIETH KATHERINE	GARCIA SALTOS	\N	\N	\N	\N	\N	gasajuka14038171@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:46.05632+00	2026-07-08 19:44:46.05632+00	\N	Ecuatoriana	\N	\N	\N	\N	f
54	0963695507	\N	ANGELICA MARIED	HOLGUIN CEDE├æO	\N	\N	\N	\N	\N	hoceanma13950326@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:47.710663+00	2026-07-08 19:44:47.710663+00	\N	Ecuatoriana	\N	\N	\N	\N	f
55	0964236608	\N	ANGEL SEBASTIAN	JAMA MOREIRA	\N	\N	\N	\N	\N	jamoanse14442688@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:48.082146+00	2026-07-08 19:44:48.082146+00	\N	Ecuatoriana	\N	\N	\N	\N	f
56	0963852033	\N	ANDRY JOAN	LUCAS SANTANA	\N	\N	\N	\N	\N	lusaanjo14070020@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:48.459643+00	2026-07-08 19:44:48.459643+00	\N	Ecuatoriana	\N	\N	\N	\N	f
57	0963964804	\N	CRISTOPHER JAVIER	MENDOZA MARCILLO	\N	\N	\N	\N	\N	memacrja14138959@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:48.8345+00	2026-07-08 19:44:48.8345+00	\N	Ecuatoriana	\N	\N	\N	\N	f
58	1353434994	\N	MARYS ALEJANDRA	MORALES RODRIGUEZ	\N	\N	\N	\N	\N	moromaal14728114@estudiantes2.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:49.207058+00	2026-07-08 19:44:49.207058+00	\N	Ecuatoriana	\N	\N	\N	\N	f
59	1353396870	\N	JOSUA AGUSTIN	MOREIRA CATAGUA	\N	\N	\N	\N	\N	mocajoag14442361@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:49.571433+00	2026-07-08 19:44:49.571433+00	\N	Ecuatoriana	\N	\N	\N	\N	f
60	0964034888	\N	MARIA VALENTINA	MOSQUERA RODRIGUEZ	\N	\N	\N	\N	\N	moromava14814633@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:49.934797+00	2026-07-08 19:44:49.934797+00	\N	Ecuatoriana	\N	\N	\N	\N	f
61	0963977046	\N	ELIANYS MIRELYS	MU├æOZ VASQUEZ	\N	\N	\N	\N	\N	muvaelmi13929890@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:50.786455+00	2026-07-08 19:44:50.786455+00	\N	Ecuatoriana	\N	\N	\N	\N	f
62	0964060842	\N	ROMINA ISABELLA	ORMAZA MANTUANO	\N	\N	\N	\N	\N	ormarois14068337@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:51.837992+00	2026-07-08 19:44:51.837992+00	\N	Ecuatoriana	\N	\N	\N	\N	f
63	1252316474	\N	KARLEY ELIZABETH	PALACIOS OLMEDO	\N	\N	\N	\N	\N	paolkael14056304@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:52.444701+00	2026-07-08 19:44:52.444701+00	\N	Ecuatoriana	\N	\N	\N	\N	f
64	0964263909	\N	GUADALUPE RAQUEL	QUIROZ SANCHEZ	\N	\N	\N	\N	\N	qusagura14885033@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:53.738655+00	2026-07-08 19:44:53.738655+00	\N	Ecuatoriana	\N	\N	\N	\N	f
65	1252197254	\N	EILEEN ANGELINA	REYES CARDENAS	\N	\N	\N	\N	\N	recaeian15477210@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:55.669711+00	2026-07-08 19:44:55.669711+00	\N	Ecuatoriana	\N	\N	\N	\N	f
66	1353431313	\N	JEREMY SEBASTIAN	RIVAS INTRIAGO	\N	\N	\N	\N	\N	riinjese15698068@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:57.337411+00	2026-07-08 19:44:57.337411+00	\N	Ecuatoriana	\N	\N	\N	\N	f
67	0964253751	\N	ANTHONY RUBEN	RODRIGUEZ LUCAS	\N	\N	\N	\N	\N	roluanru14882055@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:58.746736+00	2026-07-08 19:44:58.746736+00	\N	Ecuatoriana	\N	\N	\N	\N	f
68	0963898002	\N	ALICE VICTORIA	RODRIGUEZ POSLIGUA	\N	\N	\N	\N	\N	ropoalvi15427591@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:59.112173+00	2026-07-08 19:44:59.112173+00	\N	Ecuatoriana	\N	\N	\N	\N	f
69	1353283334	\N	FERNANDO JHOEL	RODRIGUEZ RODRIGUEZ	\N	\N	\N	\N	\N	rorofejh15046634@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:59.476239+00	2026-07-08 19:44:59.476239+00	\N	Ecuatoriana	\N	\N	\N	\N	f
70	0963485479	\N	ERICK GAEL	ROSADO HOLGUIN	\N	\N	\N	\N	\N	rohoerga14913296@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:44:59.843181+00	2026-07-08 19:44:59.843181+00	\N	Ecuatoriana	\N	\N	\N	\N	f
71	0963955851	\N	MELANY AYLIN	SACON SUAREZ	\N	\N	\N	\N	\N	sasumeay13950256@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:00.275065+00	2026-07-08 19:45:00.275065+00	\N	Ecuatoriana	\N	\N	\N	\N	f
72	1353424631	\N	AMAIA HAILY	SOLORZANO DELGADO	\N	\N	\N	\N	\N	sodeamha14442501@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:01.279852+00	2026-07-08 19:45:01.279852+00	\N	Ecuatoriana	\N	\N	\N	\N	f
73	0963939582	\N	LIAN ALEJANDRO	SOLORZANO GARCIA	\N	\N	\N	\N	\N	sogalial14037402@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:02.078064+00	2026-07-08 19:45:02.078064+00	\N	Ecuatoriana	\N	\N	\N	\N	f
74	0964207435	\N	LUCAS JHULIAN	TAPIA MENDOZA	\N	\N	\N	\N	\N	tamelujh14073932@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:02.760296+00	2026-07-08 19:45:02.760296+00	\N	Ecuatoriana	\N	\N	\N	\N	f
75	0964447999	\N	ARIANA MAILEN	TORRES HOLGUIN	\N	\N	\N	\N	\N	tohoarma14445623@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:03.82214+00	2026-07-08 19:45:03.82214+00	\N	Ecuatoriana	\N	\N	\N	\N	f
76	0964214597	\N	JEFFERSON ALEXANDER	VASQUEZ RISCO	\N	\N	\N	\N	\N	varijeal13930895@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:04.732534+00	2026-07-08 19:45:04.732534+00	\N	Ecuatoriana	\N	\N	\N	\N	f
77	0964139695	\N	DYLAN JESUS	VELASQUEZ CASTRO	\N	\N	\N	\N	\N	vecadyje14884961@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:05.562063+00	2026-07-08 19:45:05.562063+00	\N	Ecuatoriana	\N	\N	\N	\N	f
78	0964436323	\N	OHANA MONSERRATE	VELEZ MENDOZA	\N	\N	\N	\N	\N	venamavi14442452@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:06.35289+00	2026-07-08 19:45:06.35289+00	\N	Ecuatoriana	\N	\N	\N	\N	f
79	0964251946	\N	MARIA VICTORIA	VELEZ NAVARRETE	\N	\N	\N	\N	\N	vevemaja13938981@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:07.73397+00	2026-07-08 19:45:07.73397+00	\N	Ecuatoriana	\N	\N	\N	\N	f
80	0963988563	\N	MATHIUS JACOP	VERA VERA	\N	\N	\N	\N	\N	zamalima13964790@estudiantes3.edu.ec	f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:09.267843+00	2026-07-08 19:45:09.267843+00	\N	Ecuatoriana	\N	\N	\N	\N	f
81	0964212567	\N	LIAH MAYTE	ZAMORA MACIAS	\N	\N	\N	\N	\N		f	\N	\N	\N	CAS	ACTIVA	\N	\N	2026-07-08 19:45:11.066766+00	2026-07-08 19:45:11.066766+00	\N	Ecuatoriana	\N	\N	\N	\N	f
\.


--
-- Data for Name: fichas_estudiante; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.fichas_estudiante (id_ficha, id_estudiante, tipo_sangre, alergias, medicacion_permanente, enfermedad_catastrofica, detalle_enfermedad, contacto_emergencia, telefono_emergencia, direccion_referencia, fecha_actualizacion) FROM stdin;
\.


--
-- Data for Name: grados; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.grados (id_grado, id_nivel, nombre, orden, capacidad_max, activo) FROM stdin;
1	1	Inicial 1	1	\N	t
2	1	Inicial 2	2	\N	t
3	2	Preparatoria (1er a├▒o EGB)	3	\N	t
4	3	Segundo a├▒o EGB	4	\N	t
5	3	Tercer a├▒o EGB	5	\N	t
6	3	Cuarto a├▒o EGB	6	\N	t
7	4	Quinto a├▒o EGB	7	\N	t
8	4	Sexto a├▒o EGB	8	\N	t
9	4	S├®ptimo a├▒o EGB	9	\N	t
10	5	Octavo a├▒o EGB	10	\N	t
11	5	Noveno a├▒o EGB	11	\N	t
12	5	D├®cimo a├▒o EGB	12	\N	t
\.


--
-- Data for Name: historial_promocion; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.historial_promocion (id_historial, id_matricula, id_estudiante, id_grado_origen, id_ano_lectivo, resultado, promedio_anual, observaciones, registrado_por, fecha_registro) FROM stdin;
\.


--
-- Data for Name: horarios; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.horarios (id_horario, id_asignacion, id_periodo_diario, dia_semana) FROM stdin;
\.


--
-- Data for Name: matriculas; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.matriculas (id_matricula, id_estudiante, id_grado, id_paralelo, id_ano_lectivo, numero_orden, fecha_registro, estado, observaciones, registrado_por, fecha_creacion) FROM stdin;
5	6	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:07.269328+00
6	7	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:07.747337+00
7	8	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:08.651567+00
8	9	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:09.470775+00
9	10	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:10.250461+00
10	11	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:11.032842+00
11	12	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:11.767466+00
12	13	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:12.229499+00
13	14	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:12.694348+00
14	15	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:13.510576+00
15	16	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:15.089629+00
16	17	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:16.85246+00
17	18	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:18.538751+00
18	19	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:19.306008+00
19	20	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:19.77454+00
20	21	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:20.243818+00
21	22	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:20.711423+00
22	23	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:21.179556+00
23	24	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:21.645648+00
24	25	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:22.56306+00
25	26	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:23.565704+00
26	27	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:24.485848+00
27	28	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:25.511763+00
28	29	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:26.229686+00
29	30	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:27.263026+00
30	31	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:28.824385+00
31	32	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:30.618548+00
32	33	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:32.807567+00
33	34	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:33.280156+00
34	35	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:33.74878+00
35	36	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:34.726758+00
36	37	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:35.531299+00
37	38	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:36.366225+00
38	39	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:37.003942+00
39	40	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:38.116406+00
40	41	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:39.68452+00
41	42	3	7	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:28:41.421589+00
42	43	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:35.712025+00
43	44	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:36.360321+00
44	45	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:37.377336+00
45	46	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:38.299672+00
46	47	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:39.139607+00
47	48	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:39.958565+00
48	49	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:40.973258+00
49	50	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:41.793756+00
50	51	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:43.623931+00
51	52	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:45.17246+00
52	53	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:46.855631+00
53	54	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:47.894032+00
54	55	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:48.271366+00
55	56	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:48.649644+00
56	57	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:49.021315+00
57	58	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:49.388993+00
58	59	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:49.752861+00
59	60	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:50.396176+00
60	61	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:51.304638+00
61	62	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:52.071731+00
62	63	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:52.766834+00
63	64	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:54.81638+00
64	65	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:56.546528+00
65	66	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:58.225843+00
66	67	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:58.928495+00
67	68	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:59.294176+00
68	69	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:44:59.658984+00
69	70	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:00.024979+00
70	71	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:00.829453+00
71	72	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:01.747388+00
72	73	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:02.375527+00
73	74	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:03.28479+00
74	75	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:04.312047+00
75	76	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:05.11711+00
76	77	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:06.064355+00
77	78	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:06.790336+00
78	79	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:08.626797+00
79	80	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:10.181271+00
80	81	4	9	1	\N	2026-07-08	ACTIVA	\N	\N	2026-07-08 19:45:11.870991+00
\.


--
-- Data for Name: niveles_educativos; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.niveles_educativos (id_nivel, nombre, tipo_escala, grado_inicio, grado_fin) FROM stdin;
1	Inicial	CUALITATIVA	1	2
2	Preparatoria	CUALITATIVA	3	3
3	B├ísica Elemental	CUALITATIVA	4	6
4	B├ísica Media	CUANTITATIVA	7	9
5	B├ísica Superior	CUANTITATIVA	10	12
\.


--
-- Data for Name: paralelos; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.paralelos (id_paralelo, id_grado, letra, activo) FROM stdin;
3	1	A	t
4	1	B	t
5	2	A	t
6	2	B	t
7	3	A	t
8	3	B	t
9	4	A	t
10	4	B	t
11	5	A	t
12	5	B	t
13	6	A	t
14	6	B	t
15	7	A	t
16	7	B	t
17	8	A	t
18	8	B	t
19	9	A	t
20	9	B	t
21	10	A	t
22	10	B	t
23	11	A	t
24	11	B	t
25	12	A	t
26	12	B	t
27	7	C	t
28	8	C	t
29	9	C	t
30	10	C	t
31	11	C	t
32	12	C	t
\.


--
-- Data for Name: paralelos_ano_lectivo; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.paralelos_ano_lectivo (id_paralelo_al, id_paralelo, id_ano_lectivo, capacidad_max, activo) FROM stdin;
\.


--
-- Data for Name: periodos_diarios; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.periodos_diarios (id_periodo_diario, numero, hora_inicio, hora_fin, aplica_nivel) FROM stdin;
1	1	07:30:00	08:15:00	\N
2	2	08:15:00	09:00:00	\N
3	3	09:00:00	09:45:00	\N
4	4	09:45:00	10:30:00	BASICA_ELEMENTAL
5	4	09:45:00	10:30:00	BASICA_MEDIA
6	4	09:45:00	10:30:00	BASICA_SUPERIOR
7	5	10:30:00	11:15:00	INICIAL_1
8	5	10:30:00	11:15:00	INICIAL_2
9	5	10:30:00	11:15:00	PREPARATORIA
10	5	11:00:00	11:45:00	BASICA_ELEMENTAL
11	5	11:00:00	11:45:00	BASICA_MEDIA
12	5	11:00:00	11:45:00	BASICA_SUPERIOR
13	6	11:45:00	12:30:00	\N
\.


--
-- Data for Name: personas; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.personas (id_persona, id_usuario, cedula, nombres, apellidos, fecha_nacimiento, genero, telefono, telefono_alt, direccion, correo_personal, titulo_academico, especializacion, fecha_ingreso_inst, cargo, foto_url, fecha_creacion, fecha_actualizacion) FROM stdin;
1	1	\N	Pedro	Castro	\N	\N	\N	\N	\N	\N	\N	\N	\N	Docente	\N	2026-06-02 04:03:15.065205+00	2026-06-02 04:03:15.065205+00
\.


--
-- Data for Name: representantes; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.representantes (id_representante, cedula, nombres, apellidos, parentesco, telefono_principal, telefono_alt, correo, direccion, fecha_creacion, fecha_actualizacion) FROM stdin;
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.roles (id_rol, nombre, descripcion, activo, fecha_creacion) FROM stdin;
1	DIRECTOR	Gestiona configuraci├│n acad├®mica institucional	t	2026-05-29 04:18:24.904419+00
2	SECRETARIA	Registro de estudiantes y matr├¡culas	t	2026-05-29 04:18:24.904419+00
3	DOCENTE	Registra calificaciones y asistencia	t	2026-05-29 04:18:24.904419+00
4	SOPORTE_TECNICO	Administraci├│n t├®cnica del sistema	t	2026-05-29 04:18:24.904419+00
\.


--
-- Data for Name: usuario_roles; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.usuario_roles (id_usuario, id_rol, asignado_por, asignado_el) FROM stdin;
3	1	1	2026-05-31 04:14:00.188786+00
4	4	\N	2026-06-09 02:10:46.711574+00
9	2	\N	2026-06-30 16:37:25.823286+00
1	2	\N	2026-06-30 16:40:40.414861+00
\.


--
-- Data for Name: usuarios; Type: TABLE DATA; Schema: sga_principal; Owner: -
--

COPY sga_principal.usuarios (id_usuario, uuid, username, correo, password_hash, primer_ingreso, intentos_fallidos, bloqueado_hasta, estado, ultimo_acceso, creado_por, fecha_creacion, fecha_actualizacion) FROM stdin;
3	867bf00c-853f-4f6f-a3cf-758754766dfb	pcastrol2	pcastrol2@sga.com	$2a$10$550UnewwgLmNjlYOz5CCVu3W6Nn0gmWtws6IE0vDqQEpWqHUt0Q26	f	0	\N	t	\N	\N	2026-05-31 04:13:44.251959+00	2026-05-31 04:13:44.251959+00
1	45c64850-5d3f-4bc1-9747-c39b86dc51fb	plcastrol	plcastrol@sga.com	$2a$10$s8i4c2tW5Dq7dGeh7Uv.Xu4uAc/qISf9RQ7fvg4oBcvGI81WfhLvC	f	0	\N	t	\N	\N	2026-05-31 04:04:14.206932+00	2026-05-31 04:04:14.206932+00
9	459e1bcc-65b4-4458-a598-bb1ff661da5a	eluna	ernestolunamora2004@gmail.com	$2a$10$i3MydX62cEZu0kmD7iy91.5R3ZEFZpGSjFixpTN9GhoJfMtveY1tS	f	0	\N	t	\N	\N	2026-06-27 22:50:21.839068+00	2026-06-27 22:50:21.839068+00
4	0db83cd2-55aa-477e-8c7b-6780e81ada84	jemanuel	jemanuelp@uteq.edu.ec	$2b$10$IVKyybmg00ggBJ7hKZ0mWOm2dfxbJqC7MS.RolVuhqbr8WHXvMqxm	t	0	\N	t	\N	\N	2026-06-09 02:10:23.360528+00	2026-06-09 02:10:23.360528+00
\.


--
-- Data for Name: comentarios; Type: TABLE DATA; Schema: sga_soporte; Owner: -
--

COPY sga_soporte.comentarios (id_comentario, autor, contenido, fecha_creacion, nota_interna, id_ticket) FROM stdin;
\.


--
-- Data for Name: tickets; Type: TABLE DATA; Schema: sga_soporte; Owner: -
--

COPY sga_soporte.tickets (id_ticket, asignado_a, categoria, creado_por, descripcion, estado, fecha_creacion, fecha_resolucion, numero_ticket, prioridad, solucion_aplicada, titulo) FROM stdin;
\.


--
-- Name: auth_permission_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_permission_id_seq', 60, true);


--
-- Name: auth_user_groups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_user_groups_id_seq', 1, false);


--
-- Name: auth_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_user_id_seq', 1, false);


--
-- Name: auth_user_user_permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_user_user_permissions_id_seq', 1, false);


--
-- Name: django_admin_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.django_admin_log_id_seq', 1, false);


--
-- Name: django_content_type_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.django_content_type_id_seq', 15, true);


--
-- Name: actividades_id_actividad_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.actividades_id_actividad_seq', 4, true);


--
-- Name: asistencias_id_asistencia_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.asistencias_id_asistencia_seq', 1, false);


--
-- Name: auth_group_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.auth_group_id_seq', 1, false);


--
-- Name: auth_group_permissions_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.auth_group_permissions_id_seq', 1, false);


--
-- Name: auth_permission_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.auth_permission_id_seq', 1, false);


--
-- Name: auth_user_groups_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.auth_user_groups_id_seq', 1, false);


--
-- Name: auth_user_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.auth_user_id_seq', 1, false);


--
-- Name: auth_user_user_permissions_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.auth_user_user_permissions_id_seq', 1, false);


--
-- Name: calificaciones_id_calificacion_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.calificaciones_id_calificacion_seq', 5, true);


--
-- Name: django_admin_log_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.django_admin_log_id_seq', 1, false);


--
-- Name: django_content_type_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.django_content_type_id_seq', 1, false);


--
-- Name: django_migrations_id_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.django_migrations_id_seq', 17, true);


--
-- Name: periodos_evaluacion_id_periodo_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.periodos_evaluacion_id_periodo_seq', 2, true);


--
-- Name: promedios_anuales_detalle_id_detalle_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.promedios_anuales_detalle_id_detalle_seq', 1, false);


--
-- Name: promedios_anuales_id_promedio_anual_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.promedios_anuales_id_promedio_anual_seq', 1, false);


--
-- Name: promedios_trimestrales_id_promedio_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.promedios_trimestrales_id_promedio_seq', 1, false);


--
-- Name: resumen_asistencia_id_resumen_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.resumen_asistencia_id_resumen_seq', 1, false);


--
-- Name: seguimiento_academico_id_seguimiento_seq; Type: SEQUENCE SET; Schema: sga_docente; Owner: -
--

SELECT pg_catalog.setval('sga_docente.seguimiento_academico_id_seguimiento_seq', 1, false);


--
-- Name: anos_lectivos_id_ano_lectivo_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.anos_lectivos_id_ano_lectivo_seq', 1, true);


--
-- Name: asignaciones_id_asignacion_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.asignaciones_id_asignacion_seq', 1, true);


--
-- Name: asignaturas_id_asignatura_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.asignaturas_id_asignatura_seq', 12, true);


--
-- Name: auditoria_id_auditoria_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.auditoria_id_auditoria_seq', 1, false);


--
-- Name: documentos_matricula_id_documento_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.documentos_matricula_id_documento_seq', 1, false);


--
-- Name: escala_calificaciones_id_escala_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.escala_calificaciones_id_escala_seq', 1, false);


--
-- Name: estudiantes_id_estudiante_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.estudiantes_id_estudiante_seq', 81, true);


--
-- Name: fichas_estudiante_id_ficha_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.fichas_estudiante_id_ficha_seq', 1, false);


--
-- Name: grados_id_grado_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.grados_id_grado_seq', 14, true);


--
-- Name: historial_promocion_id_historial_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.historial_promocion_id_historial_seq', 1, false);


--
-- Name: horarios_id_horario_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.horarios_id_horario_seq', 1, false);


--
-- Name: matriculas_id_matricula_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.matriculas_id_matricula_seq', 80, true);


--
-- Name: niveles_educativos_id_nivel_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.niveles_educativos_id_nivel_seq', 7, true);


--
-- Name: paralelos_ano_lectivo_id_paralelo_al_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.paralelos_ano_lectivo_id_paralelo_al_seq', 1, false);


--
-- Name: paralelos_id_paralelo_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.paralelos_id_paralelo_seq', 32, true);


--
-- Name: periodos_diarios_id_periodo_diario_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.periodos_diarios_id_periodo_diario_seq', 13, true);


--
-- Name: personas_id_persona_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.personas_id_persona_seq', 1, true);


--
-- Name: representantes_id_representante_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.representantes_id_representante_seq', 1, false);


--
-- Name: roles_id_rol_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.roles_id_rol_seq', 6, true);


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE SET; Schema: sga_principal; Owner: -
--

SELECT pg_catalog.setval('sga_principal.usuarios_id_usuario_seq', 9, true);


--
-- Name: comentarios_id_comentario_seq; Type: SEQUENCE SET; Schema: sga_soporte; Owner: -
--

SELECT pg_catalog.setval('sga_soporte.comentarios_id_comentario_seq', 1, false);


--
-- Name: tickets_id_ticket_seq; Type: SEQUENCE SET; Schema: sga_soporte; Owner: -
--

SELECT pg_catalog.setval('sga_soporte.tickets_id_ticket_seq', 1, false);


--
-- Name: auth_permission auth_permission_content_type_id_codename_01ab375a_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_permission
    ADD CONSTRAINT auth_permission_content_type_id_codename_01ab375a_uniq UNIQUE (content_type_id, codename);


--
-- Name: auth_permission auth_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_permission
    ADD CONSTRAINT auth_permission_pkey PRIMARY KEY (id);


--
-- Name: auth_user_groups auth_user_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_groups
    ADD CONSTRAINT auth_user_groups_pkey PRIMARY KEY (id);


--
-- Name: auth_user_groups auth_user_groups_user_id_group_id_94350c0c_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_groups
    ADD CONSTRAINT auth_user_groups_user_id_group_id_94350c0c_uniq UNIQUE (user_id, group_id);


--
-- Name: auth_user auth_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user
    ADD CONSTRAINT auth_user_pkey PRIMARY KEY (id);


--
-- Name: auth_user_user_permissions auth_user_user_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permissions_pkey PRIMARY KEY (id);


--
-- Name: auth_user_user_permissions auth_user_user_permissions_user_id_permission_id_14a6b632_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permissions_user_id_permission_id_14a6b632_uniq UNIQUE (user_id, permission_id);


--
-- Name: auth_user auth_user_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user
    ADD CONSTRAINT auth_user_username_key UNIQUE (username);


--
-- Name: django_admin_log django_admin_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_admin_log
    ADD CONSTRAINT django_admin_log_pkey PRIMARY KEY (id);


--
-- Name: django_content_type django_content_type_app_label_model_76bd3d3b_uniq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_content_type
    ADD CONSTRAINT django_content_type_app_label_model_76bd3d3b_uniq UNIQUE (app_label, model);


--
-- Name: django_content_type django_content_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_content_type
    ADD CONSTRAINT django_content_type_pkey PRIMARY KEY (id);


--
-- Name: actividades actividades_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.actividades
    ADD CONSTRAINT actividades_pkey PRIMARY KEY (id_actividad);


--
-- Name: asistencias asistencias_id_matricula_id_asignacion_fecha_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias
    ADD CONSTRAINT asistencias_id_matricula_id_asignacion_fecha_key UNIQUE (id_matricula, id_asignacion, fecha);


--
-- Name: asistencias asistencias_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias
    ADD CONSTRAINT asistencias_pkey PRIMARY KEY (id_asistencia);


--
-- Name: auth_group auth_group_name_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_group
    ADD CONSTRAINT auth_group_name_key UNIQUE (name);


--
-- Name: auth_group_permissions auth_group_permissions_group_id_permission_id_0cd325b0_uniq; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_group_permissions
    ADD CONSTRAINT auth_group_permissions_group_id_permission_id_0cd325b0_uniq UNIQUE (group_id, permission_id);


--
-- Name: auth_group_permissions auth_group_permissions_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_group_permissions
    ADD CONSTRAINT auth_group_permissions_pkey PRIMARY KEY (id);


--
-- Name: auth_group auth_group_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_group
    ADD CONSTRAINT auth_group_pkey PRIMARY KEY (id);


--
-- Name: auth_permission auth_permission_content_type_id_codename_01ab375a_uniq; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_permission
    ADD CONSTRAINT auth_permission_content_type_id_codename_01ab375a_uniq UNIQUE (content_type_id, codename);


--
-- Name: auth_permission auth_permission_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_permission
    ADD CONSTRAINT auth_permission_pkey PRIMARY KEY (id);


--
-- Name: auth_user_groups auth_user_groups_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_groups
    ADD CONSTRAINT auth_user_groups_pkey PRIMARY KEY (id);


--
-- Name: auth_user_groups auth_user_groups_user_id_group_id_94350c0c_uniq; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_groups
    ADD CONSTRAINT auth_user_groups_user_id_group_id_94350c0c_uniq UNIQUE (user_id, group_id);


--
-- Name: auth_user auth_user_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user
    ADD CONSTRAINT auth_user_pkey PRIMARY KEY (id);


--
-- Name: auth_user_user_permissions auth_user_user_permissions_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permissions_pkey PRIMARY KEY (id);


--
-- Name: auth_user_user_permissions auth_user_user_permissions_user_id_permission_id_14a6b632_uniq; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permissions_user_id_permission_id_14a6b632_uniq UNIQUE (user_id, permission_id);


--
-- Name: auth_user auth_user_username_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user
    ADD CONSTRAINT auth_user_username_key UNIQUE (username);


--
-- Name: calificaciones calificaciones_id_actividad_id_matricula_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.calificaciones
    ADD CONSTRAINT calificaciones_id_actividad_id_matricula_key UNIQUE (id_actividad, id_matricula);


--
-- Name: calificaciones calificaciones_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.calificaciones
    ADD CONSTRAINT calificaciones_pkey PRIMARY KEY (id_calificacion);


--
-- Name: django_admin_log django_admin_log_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.django_admin_log
    ADD CONSTRAINT django_admin_log_pkey PRIMARY KEY (id);


--
-- Name: django_content_type django_content_type_app_label_model_76bd3d3b_uniq; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.django_content_type
    ADD CONSTRAINT django_content_type_app_label_model_76bd3d3b_uniq UNIQUE (app_label, model);


--
-- Name: django_content_type django_content_type_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.django_content_type
    ADD CONSTRAINT django_content_type_pkey PRIMARY KEY (id);


--
-- Name: django_migrations django_migrations_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.django_migrations
    ADD CONSTRAINT django_migrations_pkey PRIMARY KEY (id);


--
-- Name: periodos_evaluacion periodos_evaluacion_id_ano_lectivo_tipo_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.periodos_evaluacion
    ADD CONSTRAINT periodos_evaluacion_id_ano_lectivo_tipo_key UNIQUE (id_ano_lectivo, tipo);


--
-- Name: periodos_evaluacion periodos_evaluacion_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.periodos_evaluacion
    ADD CONSTRAINT periodos_evaluacion_pkey PRIMARY KEY (id_periodo);


--
-- Name: promedios_anuales_detalle promedios_anuales_detalle_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales_detalle
    ADD CONSTRAINT promedios_anuales_detalle_pkey PRIMARY KEY (id_detalle);


--
-- Name: promedios_anuales_detalle promedios_anuales_detalle_unique; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales_detalle
    ADD CONSTRAINT promedios_anuales_detalle_unique UNIQUE (id_promedio_anual, id_promedio_trim);


--
-- Name: promedios_anuales promedios_anuales_id_matricula_id_asignacion_id_ano_lectivo_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales
    ADD CONSTRAINT promedios_anuales_id_matricula_id_asignacion_id_ano_lectivo_key UNIQUE (id_matricula, id_asignacion, id_ano_lectivo);


--
-- Name: promedios_anuales promedios_anuales_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales
    ADD CONSTRAINT promedios_anuales_pkey PRIMARY KEY (id_promedio_anual);


--
-- Name: promedios_trimestrales promedios_trimestrales_id_matricula_id_asignacion_id_period_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_trimestrales
    ADD CONSTRAINT promedios_trimestrales_id_matricula_id_asignacion_id_period_key UNIQUE (id_matricula, id_asignacion, id_periodo);


--
-- Name: promedios_trimestrales promedios_trimestrales_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_trimestrales
    ADD CONSTRAINT promedios_trimestrales_pkey PRIMARY KEY (id_promedio);


--
-- Name: resumen_asistencia resumen_asistencia_id_matricula_id_asignacion_id_periodo_key; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.resumen_asistencia
    ADD CONSTRAINT resumen_asistencia_id_matricula_id_asignacion_id_periodo_key UNIQUE (id_matricula, id_asignacion, id_periodo);


--
-- Name: resumen_asistencia resumen_asistencia_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.resumen_asistencia
    ADD CONSTRAINT resumen_asistencia_pkey PRIMARY KEY (id_resumen);


--
-- Name: seguimiento_academico seguimiento_academico_pkey; Type: CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.seguimiento_academico
    ADD CONSTRAINT seguimiento_academico_pkey PRIMARY KEY (id_seguimiento);


--
-- Name: anos_lectivos anos_lectivos_nombre_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.anos_lectivos
    ADD CONSTRAINT anos_lectivos_nombre_key UNIQUE (nombre);


--
-- Name: anos_lectivos anos_lectivos_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.anos_lectivos
    ADD CONSTRAINT anos_lectivos_pkey PRIMARY KEY (id_ano_lectivo);


--
-- Name: asignaciones asignaciones_asignatura_paralelo_ano_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_asignatura_paralelo_ano_key UNIQUE (id_asignatura, id_paralelo, id_ano_lectivo);


--
-- Name: asignaciones asignaciones_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_pkey PRIMARY KEY (id_asignacion);


--
-- Name: asignaturas asignaturas_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaturas
    ADD CONSTRAINT asignaturas_pkey PRIMARY KEY (id_asignatura);


--
-- Name: asignaturas_por_nivel asignaturas_por_nivel_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaturas_por_nivel
    ADD CONSTRAINT asignaturas_por_nivel_pkey PRIMARY KEY (id_asignatura, id_nivel);


--
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id_auditoria);


--
-- Name: documentos_matricula documentos_matricula_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.documentos_matricula
    ADD CONSTRAINT documentos_matricula_pkey PRIMARY KEY (id_documento);


--
-- Name: escala_calificaciones escala_ano_nivel_rango_unique; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.escala_calificaciones
    ADD CONSTRAINT escala_ano_nivel_rango_unique UNIQUE (id_ano_lectivo, id_nivel, nota_minima);


--
-- Name: escala_calificaciones escala_calificaciones_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.escala_calificaciones
    ADD CONSTRAINT escala_calificaciones_pkey PRIMARY KEY (id_escala);


--
-- Name: estudiantes estudiantes_cedula_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.estudiantes
    ADD CONSTRAINT estudiantes_cedula_key UNIQUE (cedula);


--
-- Name: estudiantes estudiantes_codigo_estudiante_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.estudiantes
    ADD CONSTRAINT estudiantes_codigo_estudiante_key UNIQUE (codigo_estudiante);


--
-- Name: estudiantes estudiantes_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.estudiantes
    ADD CONSTRAINT estudiantes_pkey PRIMARY KEY (id_estudiante);


--
-- Name: fichas_estudiante fichas_estudiante_id_estudiante_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.fichas_estudiante
    ADD CONSTRAINT fichas_estudiante_id_estudiante_key UNIQUE (id_estudiante);


--
-- Name: fichas_estudiante fichas_estudiante_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.fichas_estudiante
    ADD CONSTRAINT fichas_estudiante_pkey PRIMARY KEY (id_ficha);


--
-- Name: grados grados_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.grados
    ADD CONSTRAINT grados_pkey PRIMARY KEY (id_grado);


--
-- Name: historial_promocion historial_matricula_unique; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_matricula_unique UNIQUE (id_matricula);


--
-- Name: historial_promocion historial_promocion_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_promocion_pkey PRIMARY KEY (id_historial);


--
-- Name: horarios horarios_id_asignacion_id_periodo_diario_dia_semana_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.horarios
    ADD CONSTRAINT horarios_id_asignacion_id_periodo_diario_dia_semana_key UNIQUE (id_asignacion, id_periodo_diario, dia_semana);


--
-- Name: horarios horarios_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.horarios
    ADD CONSTRAINT horarios_pkey PRIMARY KEY (id_horario);


--
-- Name: matriculas matriculas_id_estudiante_id_ano_lectivo_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_id_estudiante_id_ano_lectivo_key UNIQUE (id_estudiante, id_ano_lectivo);


--
-- Name: matriculas matriculas_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_pkey PRIMARY KEY (id_matricula);


--
-- Name: niveles_educativos niveles_educativos_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.niveles_educativos
    ADD CONSTRAINT niveles_educativos_pkey PRIMARY KEY (id_nivel);


--
-- Name: paralelos_ano_lectivo paralelos_ano_lectivo_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos_ano_lectivo
    ADD CONSTRAINT paralelos_ano_lectivo_pkey PRIMARY KEY (id_paralelo_al);


--
-- Name: paralelos_ano_lectivo paralelos_ano_lectivo_unique; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos_ano_lectivo
    ADD CONSTRAINT paralelos_ano_lectivo_unique UNIQUE (id_paralelo, id_ano_lectivo);


--
-- Name: paralelos paralelos_id_grado_letra_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos
    ADD CONSTRAINT paralelos_id_grado_letra_key UNIQUE (id_grado, letra);


--
-- Name: paralelos paralelos_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos
    ADD CONSTRAINT paralelos_pkey PRIMARY KEY (id_paralelo);


--
-- Name: periodos_diarios periodos_diarios_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.periodos_diarios
    ADD CONSTRAINT periodos_diarios_pkey PRIMARY KEY (id_periodo_diario);


--
-- Name: personas personas_cedula_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.personas
    ADD CONSTRAINT personas_cedula_key UNIQUE (cedula);


--
-- Name: personas personas_id_usuario_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.personas
    ADD CONSTRAINT personas_id_usuario_key UNIQUE (id_usuario);


--
-- Name: personas personas_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.personas
    ADD CONSTRAINT personas_pkey PRIMARY KEY (id_persona);


--
-- Name: representantes representantes_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.representantes
    ADD CONSTRAINT representantes_pkey PRIMARY KEY (id_representante);


--
-- Name: roles roles_nombre_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.roles
    ADD CONSTRAINT roles_nombre_key UNIQUE (nombre);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id_rol);


--
-- Name: usuario_roles usuario_roles_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuario_roles
    ADD CONSTRAINT usuario_roles_pkey PRIMARY KEY (id_usuario, id_rol);


--
-- Name: usuarios usuarios_correo_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuarios
    ADD CONSTRAINT usuarios_correo_key UNIQUE (correo);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);


--
-- Name: usuarios usuarios_username_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (username);


--
-- Name: usuarios usuarios_uuid_key; Type: CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuarios
    ADD CONSTRAINT usuarios_uuid_key UNIQUE (uuid);


--
-- Name: comentarios comentarios_pkey; Type: CONSTRAINT; Schema: sga_soporte; Owner: -
--

ALTER TABLE ONLY sga_soporte.comentarios
    ADD CONSTRAINT comentarios_pkey PRIMARY KEY (id_comentario);


--
-- Name: tickets tickets_pkey; Type: CONSTRAINT; Schema: sga_soporte; Owner: -
--

ALTER TABLE ONLY sga_soporte.tickets
    ADD CONSTRAINT tickets_pkey PRIMARY KEY (id_ticket);


--
-- Name: tickets uk_k0msfrh058q2goawy9u1sig33; Type: CONSTRAINT; Schema: sga_soporte; Owner: -
--

ALTER TABLE ONLY sga_soporte.tickets
    ADD CONSTRAINT uk_k0msfrh058q2goawy9u1sig33 UNIQUE (numero_ticket);


--
-- Name: auth_permission_content_type_id_2f476e4b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_permission_content_type_id_2f476e4b ON public.auth_permission USING btree (content_type_id);


--
-- Name: auth_user_groups_group_id_97559544; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_user_groups_group_id_97559544 ON public.auth_user_groups USING btree (group_id);


--
-- Name: auth_user_groups_user_id_6a12ed8b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_user_groups_user_id_6a12ed8b ON public.auth_user_groups USING btree (user_id);


--
-- Name: auth_user_user_permissions_permission_id_1fbb5f2c; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_user_user_permissions_permission_id_1fbb5f2c ON public.auth_user_user_permissions USING btree (permission_id);


--
-- Name: auth_user_user_permissions_user_id_a95ead1b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_user_user_permissions_user_id_a95ead1b ON public.auth_user_user_permissions USING btree (user_id);


--
-- Name: auth_user_username_6821ab7c_like; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_user_username_6821ab7c_like ON public.auth_user USING btree (username varchar_pattern_ops);


--
-- Name: django_admin_log_content_type_id_c4bce8eb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX django_admin_log_content_type_id_c4bce8eb ON public.django_admin_log USING btree (content_type_id);


--
-- Name: django_admin_log_user_id_c564eba6; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX django_admin_log_user_id_c564eba6 ON public.django_admin_log USING btree (user_id);


--
-- Name: auth_group_name_a6ea08ec_like; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_group_name_a6ea08ec_like ON sga_docente.auth_group USING btree (name varchar_pattern_ops);


--
-- Name: auth_group_permissions_group_id_b120cbf9; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_group_permissions_group_id_b120cbf9 ON sga_docente.auth_group_permissions USING btree (group_id);


--
-- Name: auth_group_permissions_permission_id_84c5c92e; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_group_permissions_permission_id_84c5c92e ON sga_docente.auth_group_permissions USING btree (permission_id);


--
-- Name: auth_permission_content_type_id_2f476e4b; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_permission_content_type_id_2f476e4b ON sga_docente.auth_permission USING btree (content_type_id);


--
-- Name: auth_user_groups_group_id_97559544; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_user_groups_group_id_97559544 ON sga_docente.auth_user_groups USING btree (group_id);


--
-- Name: auth_user_groups_user_id_6a12ed8b; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_user_groups_user_id_6a12ed8b ON sga_docente.auth_user_groups USING btree (user_id);


--
-- Name: auth_user_user_permissions_permission_id_1fbb5f2c; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_user_user_permissions_permission_id_1fbb5f2c ON sga_docente.auth_user_user_permissions USING btree (permission_id);


--
-- Name: auth_user_user_permissions_user_id_a95ead1b; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_user_user_permissions_user_id_a95ead1b ON sga_docente.auth_user_user_permissions USING btree (user_id);


--
-- Name: auth_user_username_6821ab7c_like; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX auth_user_username_6821ab7c_like ON sga_docente.auth_user USING btree (username varchar_pattern_ops);


--
-- Name: django_admin_log_content_type_id_c4bce8eb; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX django_admin_log_content_type_id_c4bce8eb ON sga_docente.django_admin_log USING btree (content_type_id);


--
-- Name: django_admin_log_user_id_c564eba6; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX django_admin_log_user_id_c564eba6 ON sga_docente.django_admin_log USING btree (user_id);


--
-- Name: idx_actividades_asignacion; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_actividades_asignacion ON sga_docente.actividades USING btree (id_asignacion, id_periodo);


--
-- Name: idx_asistencias_asignacion; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_asistencias_asignacion ON sga_docente.asistencias USING btree (id_asignacion, id_periodo);


--
-- Name: idx_asistencias_matricula; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_asistencias_matricula ON sga_docente.asistencias USING btree (id_matricula, fecha);


--
-- Name: idx_calificaciones_actividad; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_calificaciones_actividad ON sga_docente.calificaciones USING btree (id_actividad);


--
-- Name: idx_calificaciones_matricula; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_calificaciones_matricula ON sga_docente.calificaciones USING btree (id_matricula);


--
-- Name: idx_promedios_anuales_detalle; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_promedios_anuales_detalle ON sga_docente.promedios_anuales_detalle USING btree (id_promedio_anual);


--
-- Name: idx_promedios_trim_matricula; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_promedios_trim_matricula ON sga_docente.promedios_trimestrales USING btree (id_matricula, id_asignacion);


--
-- Name: idx_seguimiento_categoria; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_seguimiento_categoria ON sga_docente.seguimiento_academico USING btree (categoria, fecha_evento);


--
-- Name: idx_seguimiento_matricula; Type: INDEX; Schema: sga_docente; Owner: -
--

CREATE INDEX idx_seguimiento_matricula ON sga_docente.seguimiento_academico USING btree (id_matricula, id_periodo);


--
-- Name: idx_asignaciones_docente; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_asignaciones_docente ON sga_principal.asignaciones USING btree (id_docente, id_ano_lectivo);


--
-- Name: idx_asignaciones_grado; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_asignaciones_grado ON sga_principal.asignaciones USING btree (id_grado, id_ano_lectivo);


--
-- Name: idx_asignaciones_paralelo; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_asignaciones_paralelo ON sga_principal.asignaciones USING btree (id_paralelo, id_ano_lectivo);


--
-- Name: idx_auditoria_schema_origen; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_auditoria_schema_origen ON sga_principal.auditoria USING btree (schema_origen, fecha);


--
-- Name: idx_estudiantes_apellidos; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_estudiantes_apellidos ON sga_principal.estudiantes USING btree (apellidos, nombres);


--
-- Name: idx_estudiantes_cedula; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_estudiantes_cedula ON sga_principal.estudiantes USING btree (cedula);


--
-- Name: idx_historial_estudiante; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_historial_estudiante ON sga_principal.historial_promocion USING btree (id_estudiante, id_ano_lectivo);


--
-- Name: idx_horarios_asignacion; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_horarios_asignacion ON sga_principal.horarios USING btree (id_asignacion);


--
-- Name: idx_matriculas_ano_lectivo; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_matriculas_ano_lectivo ON sga_principal.matriculas USING btree (id_ano_lectivo);


--
-- Name: idx_matriculas_estudiante; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_matriculas_estudiante ON sga_principal.matriculas USING btree (id_estudiante);


--
-- Name: idx_matriculas_grado; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_matriculas_grado ON sga_principal.matriculas USING btree (id_grado);


--
-- Name: idx_matriculas_paralelo; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_matriculas_paralelo ON sga_principal.matriculas USING btree (id_paralelo, id_ano_lectivo);


--
-- Name: idx_paralelos_al_ano; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_paralelos_al_ano ON sga_principal.paralelos_ano_lectivo USING btree (id_ano_lectivo);


--
-- Name: idx_personas_cedula; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_personas_cedula ON sga_principal.personas USING btree (cedula);


--
-- Name: idx_usuario_roles_usuario; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_usuario_roles_usuario ON sga_principal.usuario_roles USING btree (id_usuario);


--
-- Name: idx_usuarios_username; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE INDEX idx_usuarios_username ON sga_principal.usuarios USING btree (username);


--
-- Name: uq_ano_lectivo_actual; Type: INDEX; Schema: sga_principal; Owner: -
--

CREATE UNIQUE INDEX uq_ano_lectivo_actual ON sga_principal.anos_lectivos USING btree (es_actual) WHERE (es_actual = true);


--
-- Name: auth_permission auth_permission_content_type_id_2f476e4b_fk_django_co; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_permission
    ADD CONSTRAINT auth_permission_content_type_id_2f476e4b_fk_django_co FOREIGN KEY (content_type_id) REFERENCES public.django_content_type(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_groups auth_user_groups_user_id_6a12ed8b_fk_auth_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_groups
    ADD CONSTRAINT auth_user_groups_user_id_6a12ed8b_fk_auth_user_id FOREIGN KEY (user_id) REFERENCES public.auth_user(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_user_permissions auth_user_user_permi_permission_id_1fbb5f2c_fk_auth_perm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permi_permission_id_1fbb5f2c_fk_auth_perm FOREIGN KEY (permission_id) REFERENCES public.auth_permission(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_user_permissions auth_user_user_permissions_user_id_a95ead1b_fk_auth_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permissions_user_id_a95ead1b_fk_auth_user_id FOREIGN KEY (user_id) REFERENCES public.auth_user(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: django_admin_log django_admin_log_content_type_id_c4bce8eb_fk_django_co; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_admin_log
    ADD CONSTRAINT django_admin_log_content_type_id_c4bce8eb_fk_django_co FOREIGN KEY (content_type_id) REFERENCES public.django_content_type(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: django_admin_log django_admin_log_user_id_c564eba6_fk_auth_user_id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.django_admin_log
    ADD CONSTRAINT django_admin_log_user_id_c564eba6_fk_auth_user_id FOREIGN KEY (user_id) REFERENCES public.auth_user(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: actividades actividades_id_asignacion_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.actividades
    ADD CONSTRAINT actividades_id_asignacion_fkey FOREIGN KEY (id_asignacion) REFERENCES sga_principal.asignaciones(id_asignacion);


--
-- Name: actividades actividades_id_periodo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.actividades
    ADD CONSTRAINT actividades_id_periodo_fkey FOREIGN KEY (id_periodo) REFERENCES sga_docente.periodos_evaluacion(id_periodo);


--
-- Name: asistencias asistencias_id_asignacion_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias
    ADD CONSTRAINT asistencias_id_asignacion_fkey FOREIGN KEY (id_asignacion) REFERENCES sga_principal.asignaciones(id_asignacion);


--
-- Name: asistencias asistencias_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias
    ADD CONSTRAINT asistencias_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: asistencias asistencias_id_periodo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias
    ADD CONSTRAINT asistencias_id_periodo_fkey FOREIGN KEY (id_periodo) REFERENCES sga_docente.periodos_evaluacion(id_periodo);


--
-- Name: asistencias asistencias_registrado_por_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.asistencias
    ADD CONSTRAINT asistencias_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: auth_group_permissions auth_group_permissio_permission_id_84c5c92e_fk_auth_perm; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_group_permissions
    ADD CONSTRAINT auth_group_permissio_permission_id_84c5c92e_fk_auth_perm FOREIGN KEY (permission_id) REFERENCES sga_docente.auth_permission(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_group_permissions auth_group_permissions_group_id_b120cbf9_fk_auth_group_id; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_group_permissions
    ADD CONSTRAINT auth_group_permissions_group_id_b120cbf9_fk_auth_group_id FOREIGN KEY (group_id) REFERENCES sga_docente.auth_group(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_permission auth_permission_content_type_id_2f476e4b_fk_django_co; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_permission
    ADD CONSTRAINT auth_permission_content_type_id_2f476e4b_fk_django_co FOREIGN KEY (content_type_id) REFERENCES sga_docente.django_content_type(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_groups auth_user_groups_group_id_97559544_fk_auth_group_id; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_groups
    ADD CONSTRAINT auth_user_groups_group_id_97559544_fk_auth_group_id FOREIGN KEY (group_id) REFERENCES sga_docente.auth_group(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_groups auth_user_groups_user_id_6a12ed8b_fk_auth_user_id; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_groups
    ADD CONSTRAINT auth_user_groups_user_id_6a12ed8b_fk_auth_user_id FOREIGN KEY (user_id) REFERENCES sga_docente.auth_user(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_user_permissions auth_user_user_permi_permission_id_1fbb5f2c_fk_auth_perm; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permi_permission_id_1fbb5f2c_fk_auth_perm FOREIGN KEY (permission_id) REFERENCES sga_docente.auth_permission(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: auth_user_user_permissions auth_user_user_permissions_user_id_a95ead1b_fk_auth_user_id; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.auth_user_user_permissions
    ADD CONSTRAINT auth_user_user_permissions_user_id_a95ead1b_fk_auth_user_id FOREIGN KEY (user_id) REFERENCES sga_docente.auth_user(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: calificaciones calificaciones_id_actividad_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.calificaciones
    ADD CONSTRAINT calificaciones_id_actividad_fkey FOREIGN KEY (id_actividad) REFERENCES sga_docente.actividades(id_actividad);


--
-- Name: calificaciones calificaciones_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.calificaciones
    ADD CONSTRAINT calificaciones_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: calificaciones calificaciones_registrado_por_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.calificaciones
    ADD CONSTRAINT calificaciones_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: django_admin_log django_admin_log_content_type_id_c4bce8eb_fk_django_co; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.django_admin_log
    ADD CONSTRAINT django_admin_log_content_type_id_c4bce8eb_fk_django_co FOREIGN KEY (content_type_id) REFERENCES sga_docente.django_content_type(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: django_admin_log django_admin_log_user_id_c564eba6_fk_auth_user_id; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.django_admin_log
    ADD CONSTRAINT django_admin_log_user_id_c564eba6_fk_auth_user_id FOREIGN KEY (user_id) REFERENCES sga_docente.auth_user(id) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: promedios_anuales_detalle pad_id_promedio_anual_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales_detalle
    ADD CONSTRAINT pad_id_promedio_anual_fkey FOREIGN KEY (id_promedio_anual) REFERENCES sga_docente.promedios_anuales(id_promedio_anual) ON DELETE CASCADE;


--
-- Name: promedios_anuales_detalle pad_id_promedio_trim_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales_detalle
    ADD CONSTRAINT pad_id_promedio_trim_fkey FOREIGN KEY (id_promedio_trim) REFERENCES sga_docente.promedios_trimestrales(id_promedio);


--
-- Name: periodos_evaluacion periodos_evaluacion_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.periodos_evaluacion
    ADD CONSTRAINT periodos_evaluacion_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: promedios_anuales promedios_anuales_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales
    ADD CONSTRAINT promedios_anuales_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: promedios_anuales promedios_anuales_id_asignacion_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales
    ADD CONSTRAINT promedios_anuales_id_asignacion_fkey FOREIGN KEY (id_asignacion) REFERENCES sga_principal.asignaciones(id_asignacion);


--
-- Name: promedios_anuales promedios_anuales_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales
    ADD CONSTRAINT promedios_anuales_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: promedios_anuales promedios_anuales_registrado_por_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_anuales
    ADD CONSTRAINT promedios_anuales_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: promedios_trimestrales promedios_trimestrales_id_asignacion_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_trimestrales
    ADD CONSTRAINT promedios_trimestrales_id_asignacion_fkey FOREIGN KEY (id_asignacion) REFERENCES sga_principal.asignaciones(id_asignacion);


--
-- Name: promedios_trimestrales promedios_trimestrales_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_trimestrales
    ADD CONSTRAINT promedios_trimestrales_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: promedios_trimestrales promedios_trimestrales_id_periodo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.promedios_trimestrales
    ADD CONSTRAINT promedios_trimestrales_id_periodo_fkey FOREIGN KEY (id_periodo) REFERENCES sga_docente.periodos_evaluacion(id_periodo);


--
-- Name: resumen_asistencia resumen_asistencia_id_asignacion_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.resumen_asistencia
    ADD CONSTRAINT resumen_asistencia_id_asignacion_fkey FOREIGN KEY (id_asignacion) REFERENCES sga_principal.asignaciones(id_asignacion);


--
-- Name: resumen_asistencia resumen_asistencia_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.resumen_asistencia
    ADD CONSTRAINT resumen_asistencia_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: resumen_asistencia resumen_asistencia_id_periodo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.resumen_asistencia
    ADD CONSTRAINT resumen_asistencia_id_periodo_fkey FOREIGN KEY (id_periodo) REFERENCES sga_docente.periodos_evaluacion(id_periodo);


--
-- Name: seguimiento_academico seguimiento_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.seguimiento_academico
    ADD CONSTRAINT seguimiento_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: seguimiento_academico seguimiento_id_periodo_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.seguimiento_academico
    ADD CONSTRAINT seguimiento_id_periodo_fkey FOREIGN KEY (id_periodo) REFERENCES sga_docente.periodos_evaluacion(id_periodo);


--
-- Name: seguimiento_academico seguimiento_registrado_por_fkey; Type: FK CONSTRAINT; Schema: sga_docente; Owner: -
--

ALTER TABLE ONLY sga_docente.seguimiento_academico
    ADD CONSTRAINT seguimiento_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: anos_lectivos anos_lectivos_creado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.anos_lectivos
    ADD CONSTRAINT anos_lectivos_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: asignaciones asignaciones_asignado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_asignado_por_fkey FOREIGN KEY (asignado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: asignaciones asignaciones_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: asignaciones asignaciones_id_asignatura_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_id_asignatura_fkey FOREIGN KEY (id_asignatura) REFERENCES sga_principal.asignaturas(id_asignatura);


--
-- Name: asignaciones asignaciones_id_docente_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_id_docente_fkey FOREIGN KEY (id_docente) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: asignaciones asignaciones_id_grado_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_id_grado_fkey FOREIGN KEY (id_grado) REFERENCES sga_principal.grados(id_grado);


--
-- Name: asignaciones asignaciones_id_paralelo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaciones
    ADD CONSTRAINT asignaciones_id_paralelo_fkey FOREIGN KEY (id_paralelo) REFERENCES sga_principal.paralelos(id_paralelo);


--
-- Name: asignaturas_por_nivel asignaturas_por_nivel_id_asignatura_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaturas_por_nivel
    ADD CONSTRAINT asignaturas_por_nivel_id_asignatura_fkey FOREIGN KEY (id_asignatura) REFERENCES sga_principal.asignaturas(id_asignatura);


--
-- Name: asignaturas_por_nivel asignaturas_por_nivel_id_nivel_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.asignaturas_por_nivel
    ADD CONSTRAINT asignaturas_por_nivel_id_nivel_fkey FOREIGN KEY (id_nivel) REFERENCES sga_principal.niveles_educativos(id_nivel);


--
-- Name: auditoria auditoria_id_usuario_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.auditoria
    ADD CONSTRAINT auditoria_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: documentos_matricula documentos_matricula_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.documentos_matricula
    ADD CONSTRAINT documentos_matricula_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula) ON DELETE CASCADE;


--
-- Name: documentos_matricula documentos_matricula_subido_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.documentos_matricula
    ADD CONSTRAINT documentos_matricula_subido_por_fkey FOREIGN KEY (subido_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: escala_calificaciones escala_calificaciones_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.escala_calificaciones
    ADD CONSTRAINT escala_calificaciones_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: escala_calificaciones escala_calificaciones_id_nivel_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.escala_calificaciones
    ADD CONSTRAINT escala_calificaciones_id_nivel_fkey FOREIGN KEY (id_nivel) REFERENCES sga_principal.niveles_educativos(id_nivel);


--
-- Name: estudiantes estudiantes_creado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.estudiantes
    ADD CONSTRAINT estudiantes_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: fichas_estudiante fichas_estudiante_id_estudiante_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.fichas_estudiante
    ADD CONSTRAINT fichas_estudiante_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES sga_principal.estudiantes(id_estudiante) ON DELETE CASCADE;


--
-- Name: estudiantes fk_estudiante_representante; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.estudiantes
    ADD CONSTRAINT fk_estudiante_representante FOREIGN KEY (id_representante) REFERENCES sga_principal.representantes(id_representante);


--
-- Name: matriculas fk_matricula_paralelo; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT fk_matricula_paralelo FOREIGN KEY (id_paralelo) REFERENCES sga_principal.paralelos(id_paralelo);


--
-- Name: grados grados_id_nivel_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.grados
    ADD CONSTRAINT grados_id_nivel_fkey FOREIGN KEY (id_nivel) REFERENCES sga_principal.niveles_educativos(id_nivel);


--
-- Name: historial_promocion historial_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: historial_promocion historial_id_estudiante_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES sga_principal.estudiantes(id_estudiante);


--
-- Name: historial_promocion historial_id_grado_origen_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_id_grado_origen_fkey FOREIGN KEY (id_grado_origen) REFERENCES sga_principal.grados(id_grado);


--
-- Name: historial_promocion historial_id_matricula_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_principal.matriculas(id_matricula);


--
-- Name: historial_promocion historial_registrado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.historial_promocion
    ADD CONSTRAINT historial_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: horarios horarios_id_asignacion_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.horarios
    ADD CONSTRAINT horarios_id_asignacion_fkey FOREIGN KEY (id_asignacion) REFERENCES sga_principal.asignaciones(id_asignacion);


--
-- Name: horarios horarios_id_periodo_diario_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.horarios
    ADD CONSTRAINT horarios_id_periodo_diario_fkey FOREIGN KEY (id_periodo_diario) REFERENCES sga_principal.periodos_diarios(id_periodo_diario);


--
-- Name: matriculas matriculas_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: matriculas matriculas_id_estudiante_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES sga_principal.estudiantes(id_estudiante);


--
-- Name: matriculas matriculas_id_grado_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_id_grado_fkey FOREIGN KEY (id_grado) REFERENCES sga_principal.grados(id_grado);


--
-- Name: matriculas matriculas_id_paralelo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_id_paralelo_fkey FOREIGN KEY (id_paralelo) REFERENCES sga_principal.paralelos(id_paralelo);


--
-- Name: matriculas matriculas_registrado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.matriculas
    ADD CONSTRAINT matriculas_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: paralelos_ano_lectivo paralelos_al_id_ano_lectivo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos_ano_lectivo
    ADD CONSTRAINT paralelos_al_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos(id_ano_lectivo);


--
-- Name: paralelos_ano_lectivo paralelos_al_id_paralelo_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos_ano_lectivo
    ADD CONSTRAINT paralelos_al_id_paralelo_fkey FOREIGN KEY (id_paralelo) REFERENCES sga_principal.paralelos(id_paralelo);


--
-- Name: paralelos paralelos_id_grado_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.paralelos
    ADD CONSTRAINT paralelos_id_grado_fkey FOREIGN KEY (id_grado) REFERENCES sga_principal.grados(id_grado);


--
-- Name: personas personas_id_usuario_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.personas
    ADD CONSTRAINT personas_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: usuario_roles usuario_roles_asignado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuario_roles
    ADD CONSTRAINT usuario_roles_asignado_por_fkey FOREIGN KEY (asignado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: usuario_roles usuario_roles_id_rol_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuario_roles
    ADD CONSTRAINT usuario_roles_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES sga_principal.roles(id_rol);


--
-- Name: usuario_roles usuario_roles_id_usuario_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuario_roles
    ADD CONSTRAINT usuario_roles_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: usuarios usuarios_creado_por_fkey; Type: FK CONSTRAINT; Schema: sga_principal; Owner: -
--

ALTER TABLE ONLY sga_principal.usuarios
    ADD CONSTRAINT usuarios_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES sga_principal.usuarios(id_usuario);


--
-- Name: comentarios fk3ir8o2d73nfe7aji8nqairgeo; Type: FK CONSTRAINT; Schema: sga_soporte; Owner: -
--

ALTER TABLE ONLY sga_soporte.comentarios
    ADD CONSTRAINT fk3ir8o2d73nfe7aji8nqairgeo FOREIGN KEY (id_ticket) REFERENCES sga_soporte.tickets(id_ticket);


--
-- Name: auth_permission; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.auth_permission ENABLE ROW LEVEL SECURITY;

--
-- Name: auth_user; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.auth_user ENABLE ROW LEVEL SECURITY;

--
-- Name: auth_user_groups; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.auth_user_groups ENABLE ROW LEVEL SECURITY;

--
-- Name: auth_user_user_permissions; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.auth_user_user_permissions ENABLE ROW LEVEL SECURITY;

--
-- Name: django_admin_log; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.django_admin_log ENABLE ROW LEVEL SECURITY;

--
-- Name: django_content_type; Type: ROW SECURITY; Schema: public; Owner: -
--

ALTER TABLE public.django_content_type ENABLE ROW LEVEL SECURITY;

--
-- PostgreSQL database dump complete
--

\unrestrict a7wBy5EIUHF0IT6Fm3OJqxJdgAmI6zYbqeVLc9abUGM1t83Q7yMqwOFTsfGaXJZ

