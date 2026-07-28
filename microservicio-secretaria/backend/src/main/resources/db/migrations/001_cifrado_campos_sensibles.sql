-- Migracion 001: ampliar columnas que a partir de ahora guardan texto cifrado
-- (AES-256-GCM, ver ec.uteq.sga.secretaria.security.CryptoService).
--
-- Por que hace falta: nonce (12 bytes) + tag (16 bytes) + base64 (~33% mas)
-- hacen que el valor guardado sea mas largo que el texto plano original.
-- Ejemplo: una direccion de 200 caracteres -> ~304 caracteres cifrados.
-- Se usa TEXT (sin limite) para no tener que volver a ajustar el ancho cada
-- vez que cambie el dato mas largo esperado.
--
-- Como correrla: psql "$DATABASE_URL" -f backend/src/main/resources/db/migrations/001_cifrado_campos_sensibles.sql
-- (o pegar el contenido en el SQL editor de Supabase). Requiere permisos de
-- owner sobre sga_principal.estudiantes.
--
-- IMPORTANTE (coordinar con sga-principal antes de correr en producción):
-- los datos existentes quedan en texto plano hasta que se editen desde
-- Secretaria (el codigo tolera leer texto plano sin fallar, ver
-- EstudianteService.descifrarFila). Si sga-principal tambien escribe estas
-- columnas directamente, sus filas seguiran en texto plano indefinidamente
-- hasta que la migracion a gRPC centralice las escrituras en un solo lugar.

ALTER TABLE sga_principal.estudiantes
  ALTER COLUMN direccion TYPE TEXT,
  ALTER COLUMN telefono TYPE TEXT,
  ALTER COLUMN tipo_discapacidad TYPE TEXT;
