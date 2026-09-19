# Evidencia del entregable #5 — Esquema de base de datos y migraciones

Constancia de arranque correcto de `sga-principal` con la validacion de Flyway
ACTIVA (`spring.flyway.validate-on-migrate=true` por omision) y sin ningun
mecanismo de `repair()` incondicional. Se cubren los dos escenarios que pide
la guia: base nueva y base ya migrada.

## Fecha de regeneracion

- Regenerados el 2026-09-18 (America/Guayaquil, UTC-5).
- Punta del arbol de trabajo al momento: HEAD = `42744dd4`
  (`fix(e45): cerrar restriccion de privilegios sobre la bitacora (#167)`).
- Version de PostgreSQL usada: 15.19 (imagen `postgres:15-alpine`) en el
  contenedor `pg-test-sga` publicado en el host en `localhost:5433`.
- Version del jar arrancado: `sga-principal-0.0.1-SNAPSHOT.jar` compilado
  desde ese HEAD.

Ambos arranques son reales, no sinteticos. Los PID, timestamps y latencias
de las lineas de log corresponden a ejecuciones efectivas del jar; se dejan
intactos para que el evaluador pueda reproducir.

## Escenarios

### 1. Arranque contra base ya migrada — `arranque_base_migrada.log`

Base `sga` en el contenedor `pg-test-sga`, con `flyway_schema_history` en
v24 (V8..V24 aplicadas previamente).

Comando de arranque (una sola linea):

```
DB_HOST=localhost DB_PORT=5433 DB_NAME=sga \
DB_USER=sga_app DB_PASSWORD=test-e5-pass \
FLYWAY_USER=postgres FLYWAY_PASSWORD=postgres \
SGA_APP_PASSWORD=test-e5-pass \
JWT_SECRET=test-jwt AES_SECRET_KEY=test-aes \
GRPC_INTERNAL_TOKEN=test MAIL_PASSWORD=test \
SERVER_PORT=8099 \
java -Dgrpc.server.port=9099 \
     -jar sga-principal/target/sga-principal-0.0.1-SNAPSHOT.jar
```

Resultado observable en el log:

- `Successfully validated 17 migrations` (validacion activa, sin repair).
- `Current version of schema "sga_principal": 24`.
- `Schema "sga_principal" is up to date. No migration necessary.`
- `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede
  modificar la bitacora`.
- `Started SgaPrincipalApplication in 45.093 seconds`.

### 2. Arranque contra base nueva — `arranque_base_nueva.log`

Base `sga_nueva` creada vacia en el mismo contenedor. Flyway aplica V8
(baseline oficial) y todas las migraciones posteriores hasta V24 sin
intervencion manual y sin `db/init/*` ni `db/baseline/*` fuera del registro.

Preparacion:

```
docker exec pg-test-sga psql -U postgres -c "CREATE DATABASE sga_nueva;"
```

Comando de arranque:

```
DB_HOST=localhost DB_PORT=5433 DB_NAME=sga_nueva \
DB_USER=sga_app DB_PASSWORD=test-e5-pass \
FLYWAY_USER=postgres FLYWAY_PASSWORD=postgres \
SGA_APP_PASSWORD=test-e5-pass \
JWT_SECRET=test-jwt AES_SECRET_KEY=test-aes \
GRPC_INTERNAL_TOKEN=test MAIL_PASSWORD=test \
SERVER_PORT=8098 \
java -Dgrpc.server.port=9098 \
     -jar sga-principal/target/sga-principal-0.0.1-SNAPSHOT.jar
```

Resultado observable en el log:

- `Migrating schema "sga_principal" to version "8 - baseline completo"`.
- Migracion continua ordenada hasta
  `Migrating schema "sga_principal" to version "24 - acotar revocacion bitacora"`.
- `Successfully applied 16 migrations to schema "sga_principal", now at
  version v24 (execution time 00:11.737s)`.
- `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede
  modificar la bitacora`.
- `Started SgaPrincipalApplication in 83.061 seconds`.

## Archivos versionados

- `arranque_base_migrada.log`: salida real del arranque en el escenario 1.
- `arranque_base_nueva.log`: salida real del arranque en el escenario 2.
- `Captura de pantalla 2026-09-16 110617.png` y
  `Captura de pantalla 2026-09-16 110659.png`: capturas previas de contexto.

## Trazabilidad

- Linea base oficial:
  `sga-principal/src/main/resources/db/migration/V8__baseline_completo.sql`.
- Correccion de V19 sin editar historial:
  `sga-principal/src/main/resources/db/migration/V23__grants_sga_secretaria_y_auditoria_diferidos.sql`.
- Acotamiento de la revocacion de privilegios sobre la bitacora:
  `sga-principal/src/main/resources/db/migration/V24__acotar_revocacion_bitacora.sql`.
- Configuracion de Flyway (`spring.flyway.locations=classpath:db/migration`)
  en `sga-principal/src/main/resources/application.properties`.
- Documentacion completa del flujo:
  `sga-principal/src/main/resources/db/migration/README.md`.
- No existe ningun `FlywayMigrationStrategy` que invoque `repair()`; se
  puede verificar con
  `grep -rn "flyway.repair\|FlywayMigrationStrategy" sga-principal/src/main`.
