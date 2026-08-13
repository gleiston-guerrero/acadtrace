CREATE TABLE sga_docente.materiales (
    id_material SERIAL PRIMARY KEY,
    id_asignacion INT NOT NULL,
    tipo VARCHAR(20),
    titulo VARCHAR(150),
    descripcion TEXT,
    url TEXT NOT NULL,
    tamano_bytes BIGINT,
    fecha TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX ix_materiales_asignacion
ON sga_docente.materiales(id_asignacion);
