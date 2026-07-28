-- Migracion 004: apuntar las FK de matriculas/historial_promocion/fichas_estudiante
-- a sga_secretaria.estudiantes en vez de sga_principal.estudiantes.
--
-- Por que hace falta: cuando Estudiante se movio a su propio esquema
-- (sga_secretaria), sga_principal.estudiantes quedo congelada (una copia
-- vieja) mientras sga_secretaria.estudiantes es la que sigue creciendo con
-- estudiantes nuevos. Estas 3 tablas nunca se actualizaron: sus FK todavia
-- apuntan a la copia congelada. Como las dos tablas usan secuencias de id
-- independientes, un id_estudiante nuevo puede coincidir por casualidad con
-- un id viejo de sga_principal.estudiantes -> el INSERT pasa la validacion
-- de FK pero queda enlazado al estudiante EQUIVOCADO (mismo numero, persona
-- distinta). Ver MatriculaService/HistorialService/FichaEstudianteService.
--
-- IMPORTANTE antes de correrla: verificar que no haya filas con id_estudiante
-- que no exista en sga_secretaria.estudiantes (la constraint fallaria al
-- crearse). En el entorno donde se escribio esta migracion habia 1 fila asi
-- en matriculas (id_matricula que referenciaba un estudiante que nunca se
-- copio a sga_secretaria) y 2 filas de prueba mal enlazadas por la colision
-- de ids descrita arriba; se borraron antes de aplicar esto. Ajustar segun
-- el entorno real antes de correr en otra base.
--
-- Como correrla: psql "$DATABASE_URL" -f backend/src/main/resources/db/migrations/004_fk_estudiante_sga_secretaria.sql

ALTER TABLE sga_principal.matriculas
  DROP CONSTRAINT matriculas_id_estudiante_fkey,
  ADD CONSTRAINT matriculas_id_estudiante_fkey
    FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes(id_estudiante);

ALTER TABLE sga_principal.historial_promocion
  DROP CONSTRAINT historial_id_estudiante_fkey,
  ADD CONSTRAINT historial_id_estudiante_fkey
    FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes(id_estudiante);

ALTER TABLE sga_principal.fichas_estudiante
  DROP CONSTRAINT fichas_estudiante_id_estudiante_fkey,
  ADD CONSTRAINT fichas_estudiante_id_estudiante_fkey
    FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes(id_estudiante) ON DELETE CASCADE;
