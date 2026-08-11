-- ============================================================================
-- seed_soporte.sql
-- Puebla sga_soporte.tickets (150) y sga_soporte.comentarios (400) con datos
-- demo realistas, usando usuarios REALES de sga_principal.usuarios (no se
-- hardcodean usernames: se toman dinamicamente al momento de correr el script).
--
-- Reglas de negocio respetadas (ver 001_init_soporte.sql):
--   categoria : HARDWARE | SOFTWARE | RED | CUENTA | OTRO
--   prioridad : BAJO | MEDIO | ALTO | CRITICO
--   estado    : ABIERTO | EN_PROCESO | RESUELTO | CERRADO
--
-- Idempotente: los tickets seed llevan numero_ticket 'TK-SEED-%'; si ya
-- existen, el script los borra primero (cascada a comentarios) y los
-- vuelve a generar, asi se puede correr varias veces sin duplicar.
-- ============================================================================
BEGIN;
SET client_min_messages = WARNING;

DELETE FROM sga_soporte.tickets WHERE numero_ticket LIKE 'TK-SEED-%';

-- 1) Candidatos: cualquier usuario activo para "creado_por"
--    y usuarios con rol SOPORTE_TECNICO para "asignado_a" (si no hay
--    ninguno con ese rol, cae a cualquier usuario activo).
CREATE TEMP TABLE _usuarios_all ON COMMIT DROP AS
SELECT username FROM sga_principal.usuarios WHERE estado = true;

CREATE TEMP TABLE _tecnicos ON COMMIT DROP AS
SELECT DISTINCT u.username
FROM sga_principal.usuarios u
JOIN sga_principal.usuario_roles ur ON ur.id_usuario = u.id_usuario
JOIN sga_principal.roles r ON r.id_rol = ur.id_rol
WHERE r.nombre = 'SOPORTE_TECNICO' AND u.estado = true;

INSERT INTO _tecnicos
SELECT username FROM _usuarios_all
WHERE NOT EXISTS (SELECT 1 FROM _tecnicos);

-- 2) Generar 150 tickets demo
CREATE TEMP TABLE _tickets_seed ON COMMIT DROP AS
WITH n AS (
    SELECT gs AS i FROM generate_series(1, 150) gs
),
arrs AS (
    SELECT
      (SELECT array_agg(username) FROM _usuarios_all) AS reporteros,
      (SELECT array_agg(username) FROM _tecnicos)      AS tecnicos
),
crudo AS (
    -- OJO: cada columna calcula su random() UNA sola vez aqui; mas abajo solo
    -- se reutiliza el valor (si se llamara random() de nuevo en cada WHEN de
    -- un CASE, cada rama compararia un numero distinto y la distribucion
    -- saldria torcida).
    SELECT
        n.i,
        (ARRAY['HARDWARE','SOFTWARE','RED','CUENTA','OTRO'])[1 + floor(random() * 5)::int] AS categoria,
        random() AS r_prioridad,
        random() AS r_estado,
        (arrs.reporteros)[1 + floor(random() * array_length(arrs.reporteros, 1))::int] AS creado_por,
        (arrs.tecnicos)[1 + floor(random() * array_length(arrs.tecnicos, 1))::int]      AS asignado_a,
        NOW() - (floor(random() * 90) || ' days')::interval
              - (floor(random() * 24) || ' hours')::interval AS fecha_creacion
    FROM n CROSS JOIN arrs
),
base AS (
    SELECT
        i, categoria, creado_por, asignado_a, fecha_creacion,
        -- distribucion de prioridad: 25% BAJO, 40% MEDIO, 25% ALTO, 10% CRITICO
        CASE
            WHEN r_prioridad < 0.25 THEN 'BAJO'
            WHEN r_prioridad < 0.65 THEN 'MEDIO'
            WHEN r_prioridad < 0.90 THEN 'ALTO'
            ELSE 'CRITICO'
        END AS prioridad,
        -- distribucion de estado: 30% ABIERTO, 40% EN_PROCESO, 15% RESUELTO, 15% CERRADO
        CASE
            WHEN r_estado < 0.30 THEN 'ABIERTO'
            WHEN r_estado < 0.70 THEN 'EN_PROCESO'
            WHEN r_estado < 0.85 THEN 'RESUELTO'
            ELSE 'CERRADO'
        END AS estado
    FROM crudo
)
SELECT
    i,
    'TK-SEED-' || LPAD(i::text, 4, '0')                          AS numero_ticket,
    categoria,
    prioridad,
    estado,
    creado_por,
    -- solo hay tecnico asignado si el ticket ya salio de ABIERTO
    CASE WHEN estado <> 'ABIERTO' THEN asignado_a ELSE NULL END  AS asignado_a,
    fecha_creacion,
    CASE
        WHEN estado IN ('RESUELTO', 'CERRADO')
            THEN fecha_creacion + (floor(random() * 72) || ' hours')::interval
        ELSE NULL
    END AS fecha_resolucion,
    CASE categoria
        WHEN 'HARDWARE' THEN (ARRAY[
            'Computador no enciende', 'Impresora no responde', 'Monitor con pantalla partida',
            'Mouse/teclado no detectado', 'Proyector de aula no enciende'])[1 + floor(random()*5)::int]
        WHEN 'SOFTWARE' THEN (ARRAY[
            'Error al iniciar sesion en el sistema', 'Aplicacion se cierra sola', 'No carga el modulo de calificaciones',
            'Version desactualizada genera fallos', 'Error al generar reporte PDF'])[1 + floor(random()*5)::int]
        WHEN 'RED' THEN (ARRAY[
            'Sin acceso a internet en laboratorio', 'Wifi intermitente en bloque C', 'VPN no conecta',
            'Lentitud extrema en la red', 'No hay acceso a servidores internos'])[1 + floor(random()*5)::int]
        WHEN 'CUENTA' THEN (ARRAY[
            'Olvido de contrasena', 'Cuenta bloqueada por intentos fallidos', 'Solicitud de cambio de correo institucional',
            'No puede acceder con su usuario', 'Solicitud de creacion de cuenta'])[1 + floor(random()*5)::int]
        ELSE (ARRAY[
            'Consulta general sobre el sistema', 'Solicitud de capacitacion', 'Reporte de comportamiento extrano',
            'Sugerencia de mejora', 'Otro tipo de incidencia'])[1 + floor(random()*5)::int]
    END AS titulo
FROM base;

-- 3) Insertar tickets con descripcion generada a partir del titulo
INSERT INTO sga_soporte.tickets
    (numero_ticket, titulo, descripcion, categoria, prioridad, estado,
     creado_por, asignado_a, solucion_aplicada, fecha_creacion, fecha_resolucion)
SELECT
    numero_ticket,
    titulo,
    titulo || '. Reportado por el usuario, requiere atencion del area de soporte tecnico. '
           || 'Detalle adicional generado para datos de demostracion (ticket #' || i || ').',
    categoria,
    prioridad,
    estado,
    creado_por,
    asignado_a,
    CASE WHEN estado IN ('RESUELTO', 'CERRADO')
         THEN 'Incidencia revisada y solucionada por el tecnico asignado. Se verifico con el usuario el correcto funcionamiento.'
         ELSE NULL
    END,
    fecha_creacion,
    fecha_resolucion
FROM _tickets_seed;

-- 4) Generar ~400 comentarios distribuidos entre los 150 tickets
--    (autor alterna entre el creador y el asignado del ticket)
WITH tk AS (
    SELECT id_ticket, creado_por, asignado_a, fecha_creacion, estado
    FROM sga_soporte.tickets
    WHERE numero_ticket LIKE 'TK-SEED-%'
),
c AS (
    SELECT
        gs AS n,
        tk.id_ticket,
        tk.fecha_creacion,
        CASE WHEN gs % 2 = 0 AND tk.asignado_a IS NOT NULL
             THEN tk.asignado_a
             ELSE tk.creado_por
        END AS autor,
        (gs % 2 = 0 AND tk.asignado_a IS NOT NULL) AS es_tecnico
    FROM tk
    -- ~2-3 comentarios por ticket para llegar a ~400 en 150 tickets
    CROSS JOIN generate_series(1, 3) gs
    WHERE random() < 0.9
)
INSERT INTO sga_soporte.comentarios (id_ticket, autor, contenido, nota_interna, fecha_creacion)
SELECT
    id_ticket,
    autor,
    CASE WHEN es_tecnico THEN
        (ARRAY[
            'Revisando el caso, en breve confirmamos.',
            'Se identifico la causa, aplicando solucion.',
            'Se solicita mas informacion para continuar el diagnostico.',
            'Incidencia escalada al area correspondiente.',
            'Solucion aplicada, quedamos atentos a confirmacion del usuario.'
        ])[1 + floor(random()*5)::int]
    ELSE
        (ARRAY[
            'Buenas, sigo con el mismo problema.',
            'Gracias por la atencion, quedo pendiente.',
            'Adjunto mas detalles del inconveniente.',
            'Confirmo que el problema ya se soluciono, gracias.',
            'Podrian darle prioridad, es urgente para mis actividades.'
        ])[1 + floor(random()*5)::int]
    END,
    es_tecnico AND random() < 0.15,  -- notas internas: solo ocasionales y solo cuando el autor es el tecnico
    fecha_creacion + (n || ' hours')::interval + (floor(random()*48) || ' hours')::interval
FROM c;

COMMIT;

-- ============================================================================
-- Verificacion
-- ============================================================================
SELECT estado, COUNT(*) FROM sga_soporte.tickets WHERE numero_ticket LIKE 'TK-SEED-%' GROUP BY estado ORDER BY estado;
SELECT categoria, COUNT(*) FROM sga_soporte.tickets WHERE numero_ticket LIKE 'TK-SEED-%' GROUP BY categoria ORDER BY categoria;
SELECT prioridad, COUNT(*) FROM sga_soporte.tickets WHERE numero_ticket LIKE 'TK-SEED-%' GROUP BY prioridad ORDER BY prioridad;
SELECT COUNT(*) AS total_tickets FROM sga_soporte.tickets WHERE numero_ticket LIKE 'TK-SEED-%';
SELECT COUNT(*) AS total_comentarios FROM sga_soporte.comentarios c
  JOIN sga_soporte.tickets t ON t.id_ticket = c.id_ticket WHERE t.numero_ticket LIKE 'TK-SEED-%';
