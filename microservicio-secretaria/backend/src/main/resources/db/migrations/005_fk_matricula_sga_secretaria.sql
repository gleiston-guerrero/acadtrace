-- Migracion 005: apuntar las FK de historial_promocion/documentos_matricula
-- a sga_secretaria.matriculas en vez de sga_principal.matriculas.
--
-- Por que hace falta: al migrar Matricula a gRPC (ver migracion 004 y
-- MatriculaService), se confirmo que la entidad JPA de sga-principal ya
-- escribe en sga_secretaria.matriculas (no en sga_principal.matriculas, que
-- quedo como copia congelada). historial_promocion y documentos_matricula
-- seguian referenciando esa copia vieja -> el mismo problema que la
-- migracion 004 corrigio para estudiantes, ahora para matriculas: un
-- id_matricula nuevo puede coincidir por casualidad con un id viejo de
-- sga_principal.matriculas y quedar enlazado a la matricula equivocada.
--
-- Verificado antes de aplicarla: 0 filas de historial_promocion con
-- id_matricula huerfano respecto a sga_secretaria.matriculas, y
-- documentos_matricula estaba vacia (0 filas) en el entorno donde se
-- escribio esta migracion. Ajustar segun el entorno real antes de correrla
-- en otra base.
--
-- Como correrla: psql "$DATABASE_URL" -f backend/src/main/resources/db/migrations/005_fk_matricula_sga_secretaria.sql

ALTER TABLE sga_principal.historial_promocion
  DROP CONSTRAINT historial_id_matricula_fkey,
  ADD CONSTRAINT historial_id_matricula_fkey
    FOREIGN KEY (id_matricula) REFERENCES sga_secretaria.matriculas(id_matricula);

ALTER TABLE sga_principal.documentos_matricula
  DROP CONSTRAINT documentos_matricula_id_matricula_fkey,
  ADD CONSTRAINT documentos_matricula_id_matricula_fkey
    FOREIGN KEY (id_matricula) REFERENCES sga_secretaria.matriculas(id_matricula) ON DELETE CASCADE;
