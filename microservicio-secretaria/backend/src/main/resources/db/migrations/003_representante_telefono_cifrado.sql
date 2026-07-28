-- Migracion 003: ampliar sga_secretaria.representantes.telefono_principal a TEXT.
--
-- Por que hace falta: RepresentanteService cifra telefono_principal con
-- CryptoService (AES-256-GCM), igual que direccion/telefono en estudiantes
-- (ver migracion 001). La columna se quedo en varchar(20) porque la tabla
-- sga_secretaria.representantes se creo despues, al mover el esquema de
-- Estudiante/Representante fuera de sga_principal, y nunca se le aplico el
-- mismo ajuste. Un telefono cifrado (nonce + tag + base64) ocupa bastante
-- mas de 20 caracteres, por lo que cualquier insert falla con
-- "value too long for type character varying(20)".
--
-- Como correrla: psql "$DATABASE_URL" -f backend/src/main/resources/db/migrations/003_representante_telefono_cifrado.sql

ALTER TABLE sga_secretaria.representantes
  ALTER COLUMN telefono_principal TYPE TEXT;
