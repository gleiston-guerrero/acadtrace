CREATE TABLE sga_docente.anuncios (
    id_anuncio SERIAL PRIMARY KEY,
    id_asignacion INT NOT NULL,
    titulo VARCHAR(150),
    contenido TEXT,
    autor_id INT,
    fecha TIMESTAMPTZ DEFAULT NOW(),
    fijado BOOLEAN DEFAULT FALSE
);

CREATE INDEX ix_anuncios_asignacion
ON sga_docente.anuncios(id_asignacion);
