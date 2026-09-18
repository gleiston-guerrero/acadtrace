-- Datos sinteticos y reproducibles para E10 Playwright.
-- Este archivo se ejecuta exclusivamente sobre la PostgreSQL efimera
-- creada por el job "E10 - Playwright Frontend Docente".

BEGIN;

-- Usuario docente sintetico.
INSERT INTO sga_principal.usuarios (
    id_usuario,
    username,
    correo,
    password_hash,
    primer_ingreso,
    estado
) VALUES (
    900001,
    'docente.e10@acadtrace.test',
    'docente.e10@acadtrace.test',
    :'e2e_docente_hash',
    false,
    true
);

INSERT INTO sga_principal.personas (
    id_persona,
    id_usuario,
    cedula,
    nombres,
    apellidos,
    correo_personal,
    cargo
) VALUES (
    900001,
    900001,
    '9999000001',
    'Docente',
    'Sintetico E10',
    'docente.e10@acadtrace.test',
    'DOCENTE'
);

INSERT INTO sga_principal.roles (
    id_rol,
    nombre,
    descripcion,
    activo
) VALUES (
    900001,
    'ROLE_DOCENTE',
    'Rol sintetico exclusivo de E10',
    true
);

INSERT INTO sga_principal.usuario_roles (
    id_usuario,
    id_rol
) VALUES (
    900001,
    900001
);

-- Ano lectivo sintetico.
INSERT INTO sga_principal.anos_lectivos (
    id_ano_lectivo,
    nombre,
    fecha_inicio,
    fecha_fin,
    es_actual
) VALUES (
    900001,
    'E10-2026',
    DATE '2026-01-01',
    DATE '2026-12-31',
    true
);

-- Estructura academica sintetica.
INSERT INTO sga_principal.niveles_educativos (
    id_nivel,
    nombre,
    tipo_escala,
    grado_inicio,
    grado_fin
) VALUES (
    900001,
    'Nivel Sintetico E10',
    'CUANTITATIVA',
    1,
    10
);

INSERT INTO sga_principal.grados (
    id_grado,
    id_nivel,
    nombre,
    orden,
    capacidad_max,
    activo
) VALUES (
    900001,
    900001,
    'Grado E10',
    1,
    30,
    true
);

INSERT INTO sga_principal.paralelos (
    id_paralelo,
    id_grado,
    letra,
    activo
) VALUES (
    900001,
    900001,
    'A',
    true
);

INSERT INTO sga_principal.paralelos_ano_lectivo (
    id_paralelo_al,
    id_paralelo,
    id_ano_lectivo,
    capacidad_max,
    activo
) VALUES (
    900001,
    900001,
    900001,
    30,
    true
);

INSERT INTO sga_principal.asignaturas (
    id_asignatura,
    nombre,
    codigo,
    descripcion,
    horas_semana,
    activa
) VALUES (
    900001,
    'Matematica E10',
    'E10-MAT',
    'Asignatura sintetica exclusiva de Playwright E10',
    5,
    true
);

INSERT INTO sga_principal.asignaciones (
    id_asignacion,
    id_docente,
    id_asignatura,
    id_grado,
    id_paralelo,
    id_ano_lectivo,
    es_tutor,
    activo
) VALUES (
    900001,
    900001,
    900001,
    900001,
    900001,
    900001,
    true,
    true
);

-- Estudiante sintetico.
INSERT INTO sga_principal.estudiantes (
    id_estudiante,
    cedula,
    codigo_estudiante,
    nombres,
    apellidos,
    estado
) VALUES (
    900001,
    '9999000002',
    'E10-EST-001',
    'Estudiante',
    'Sintetico E10',
    'ACTIVO'
);

INSERT INTO sga_principal.matriculas (
    id_matricula,
    id_estudiante,
    id_grado,
    id_paralelo,
    id_ano_lectivo,
    numero_orden,
    estado
) VALUES (
    900001,
    900001,
    900001,
    900001,
    900001,
    1,
    'ACTIVA'
);

COMMIT;