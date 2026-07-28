ALTER TABLE sga_principal.matriculas
ADD COLUMN IF NOT EXISTS id_paralelo BIGINT,
ADD CONSTRAINT fk_matricula_paralelo FOREIGN KEY (id_paralelo) REFERENCES sga_principal.paralelos(id_paralelo);
