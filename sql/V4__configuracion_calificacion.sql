-- V4: Configuración del esquema de calificación.
--
-- Define, por año lectivo, CÓMO se califica: los 3 trimestres, la ponderación
-- formativa/sumativa (70/30), los tipos de aporte y la escala cualitativa.
-- El microservicio docente consume esta configuración (no la duplica) y con
-- ella calcula y guarda las notas en su propio esquema sga_docente.

-- ---------------------------------------------------------------------------
-- 1. Periodos de evaluación (los 3 trimestres del año lectivo)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_principal.periodos_evaluacion (
    id_periodo     SERIAL PRIMARY KEY,
    id_ano_lectivo INTEGER      NOT NULL REFERENCES sga_principal.anos_lectivos (id_ano_lectivo),
    tipo           VARCHAR(20)  NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    fecha_inicio   DATE         NOT NULL,
    fecha_fin      DATE         NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_periodo_ano_tipo UNIQUE (id_ano_lectivo, tipo)
);

-- ---------------------------------------------------------------------------
-- 2. Ponderación formativa / sumativa (70% - 30%)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_principal.esquema_calificacion (
    id_esquema     SERIAL PRIMARY KEY,
    id_ano_lectivo INTEGER      NOT NULL UNIQUE REFERENCES sga_principal.anos_lectivos (id_ano_lectivo),
    peso_formativa NUMERIC(5,2) NOT NULL DEFAULT 70.00,
    peso_sumativa  NUMERIC(5,2) NOT NULL DEFAULT 30.00,
    CONSTRAINT ck_esquema_pesos_100 CHECK (peso_formativa + peso_sumativa = 100)
);

-- ---------------------------------------------------------------------------
-- 3. Tipos de aporte (actividades que componen cada evaluación)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_principal.tipos_aporte (
    id_tipo_aporte  SERIAL PRIMARY KEY,
    id_ano_lectivo  INTEGER     NOT NULL REFERENCES sga_principal.anos_lectivos (id_ano_lectivo),
    nombre          VARCHAR(60) NOT NULL,
    tipo_evaluacion VARCHAR(12) NOT NULL DEFAULT 'FORMATIVA'
                    CHECK (tipo_evaluacion IN ('FORMATIVA', 'SUMATIVA')),
    orden           INTEGER     NOT NULL DEFAULT 0,
    activo          BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_aporte_ano_nombre UNIQUE (id_ano_lectivo, nombre)
);

-- ---------------------------------------------------------------------------
-- SEMILLA para el año lectivo actual
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_ano   INTEGER;
    v_ini   DATE;
    v_fin   DATE;
BEGIN
    SELECT id_ano_lectivo, fecha_inicio, fecha_fin
      INTO v_ano, v_ini, v_fin
      FROM sga_principal.anos_lectivos
     WHERE es_actual = TRUE
     LIMIT 1;

    IF v_ano IS NULL THEN
        RAISE NOTICE 'No hay año lectivo actual; se omite la semilla.';
        RETURN;
    END IF;

    -- 3 trimestres repartidos en el año lectivo
    INSERT INTO sga_principal.periodos_evaluacion (id_ano_lectivo, tipo, nombre, fecha_inicio, fecha_fin)
    VALUES
        (v_ano, 'PRIMER_TRIMESTRE',  'Primer Trimestre',  v_ini,                                   v_ini + ((v_fin - v_ini) / 3)),
        (v_ano, 'SEGUNDO_TRIMESTRE', 'Segundo Trimestre', v_ini + ((v_fin - v_ini) / 3) + 1,        v_ini + (2 * (v_fin - v_ini) / 3)),
        (v_ano, 'TERCER_TRIMESTRE',  'Tercer Trimestre',  v_ini + (2 * (v_fin - v_ini) / 3) + 1,    v_fin)
    ON CONFLICT (id_ano_lectivo, tipo) DO NOTHING;

    -- Ponderación 70/30
    INSERT INTO sga_principal.esquema_calificacion (id_ano_lectivo, peso_formativa, peso_sumativa)
    VALUES (v_ano, 70.00, 30.00)
    ON CONFLICT (id_ano_lectivo) DO NOTHING;

    -- Tipos de aporte: formativa (los 7 del registro docente) y sumativa
    INSERT INTO sga_principal.tipos_aporte (id_ano_lectivo, nombre, tipo_evaluacion, orden) VALUES
        (v_ano, 'Lección Oral',               'FORMATIVA', 1),
        (v_ano, 'Lección Escrita',            'FORMATIVA', 2),
        (v_ano, 'Tareas',                     'FORMATIVA', 3),
        (v_ano, 'Talleres',                   'FORMATIVA', 4),
        (v_ano, 'Cuaderno',                   'FORMATIVA', 5),
        (v_ano, 'Trabajo Individual',         'FORMATIVA', 6),
        (v_ano, 'Exposición',                 'FORMATIVA', 7),
        (v_ano, 'Proyecto Interdisciplinario','SUMATIVA',  1),
        (v_ano, 'Examen del Trimestre',       'SUMATIVA',  2)
    ON CONFLICT (id_ano_lectivo, nombre) DO NOTHING;

    -- Escala cualitativa (MINEDUC ampliada con +/-), para cada nivel educativo
    IF NOT EXISTS (SELECT 1 FROM sga_principal.escala_calificaciones WHERE id_ano_lectivo = v_ano) THEN
        INSERT INTO sga_principal.escala_calificaciones
            (id_ano_lectivo, id_nivel, nota_minima, nota_maxima, equivalente_cualitativo, descripcion)
        SELECT v_ano, n.id_nivel, e.minimo, e.maximo, e.letra, e.detalle
          FROM sga_principal.niveles_educativos n
         CROSS JOIN (VALUES
                (9.50, 10.00, 'A+', 'Domina los aprendizajes requeridos'),
                (8.50,  9.49, 'A-', 'Domina los aprendizajes requeridos'),
                (7.50,  8.49, 'B+', 'Alcanza los aprendizajes requeridos'),
                (6.50,  7.49, 'B-', 'Alcanza los aprendizajes requeridos'),
                (5.50,  6.49, 'C+', 'Está próximo a alcanzar los aprendizajes'),
                (4.50,  5.49, 'C-', 'Está próximo a alcanzar los aprendizajes'),
                (3.50,  4.49, 'D+', 'No alcanza los aprendizajes requeridos'),
                (2.50,  3.49, 'D-', 'No alcanza los aprendizajes requeridos'),
                (1.50,  2.49, 'E+', 'No alcanza los aprendizajes requeridos'),
                (0.00,  1.49, 'E-', 'No alcanza los aprendizajes requeridos')
            ) AS e(minimo, maximo, letra, detalle);
    END IF;
END $$;
