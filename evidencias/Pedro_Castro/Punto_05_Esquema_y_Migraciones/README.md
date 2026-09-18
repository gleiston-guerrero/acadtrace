# Evidencia del entregable #5 — Esquema de base de datos y migraciones

Este directorio contiene la constancia de arranque correcto de
`sga-principal` con la validación de Flyway ACTIVA
(`validateOnMigrate=true` por defecto), en los dos escenarios que
exige el criterio del ingeniero:

1. **Base ya migrada** con la versión anterior de V18 (13/09) y arranque
   con la versión posterior (con marcador `${sga_app_password}`).
2. **Base NUEVA** desde vacío, aplicando la línea base
   `baseline_flyway_v8.sql` como versión 8 y luego V9 a V22.

## Archivos versionados

- `arranque_base_migrada.log`: log del arranque en el escenario 1,
  capturado con PostgreSQL 17 y Flyway 9.22.3 locales. Contiene la
  ejecución de `flyway.repair()` (recalcula el checksum de V18)
  seguida de `flyway.migrate()` (aplica V19 a V22 con validación
  activa).
- `arranque_base_nueva.log`: log del arranque en el escenario 2.
  `flyway.repair()` es no-op (no hay historial que reparar) y
  `flyway.migrate()` aplica desde la línea base V8 hasta V22 con
  validación activa.
- `Captura de pantalla 2026-09-16 110617.png` y
  `Captura de pantalla 2026-09-16 110659.png`: capturas de contexto
  del entorno de reproducción (compose y panel Dozzle con los 18
  contenedores en marcha).

## Cómo se reproduce en un clon limpio

En una máquina con PostgreSQL 17 y JDK 17 disponibles:

1. **Base NUEVA (escenario 2)**:
   - `createdb -h localhost -p 5432 sga`
   - `cd sga-principal && ./mvnw spring-boot:run` con las variables
     `DB_HOST=localhost DB_PORT=5432 DB_NAME=sga DB_USER=postgres
     DB_PASSWORD=<pass>` apuntando a esa base vacía.
   - Flyway registra la versión 8 como línea base usando
     `baseline_flyway_v8.sql` y aplica V9 a V22. El log queda en
     `arranque_base_nueva.log`.

2. **Base ya migrada (escenario 1)**:
   - Detener el servicio, revertir V18 a la versión del 13/09
     (contraseña literal).
   - Volver a lanzar sga-principal. Flyway detecta el checksum
     mismatch, `FlywayConfig.repairAntesDeMigrar` ejecuta
     `flyway.repair()` (que reescribe el checksum en el historial),
     y `flyway.migrate()` aplica V19 a V22 con validación activa.
     Log en `arranque_base_migrada.log`.

## Trazabilidad

- Código de reparación:
  `sga-principal/src/main/java/ec/edu/uteq/sga/infrastructure/config/FlywayConfig.java`.
- Línea base versionada:
  `sga-principal/src/main/resources/db/baseline/baseline_flyway_v8.sql`.
- Marcas en el historial: V20 y V22 en
  `sga-principal/src/main/resources/db/migration/`.
- Configuración de Flyway:
  `sga-principal/src/main/resources/application.properties`
  (`spring.flyway.baseline-on-migrate=true`,
  `spring.flyway.baseline-version=8`).
- Documentación completa:
  `sga-principal/src/main/resources/db/migration/README.md`,
  sección "Reparación de V18 y arranque desde base nueva".

## Verificación de que la validación está activa

Ninguno de los siguientes archivos contiene una desactivación de
la validación de Flyway:

- `docker-compose.yml`: sin `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false`.
- `sga-principal/src/main/resources/application.properties`: sin
  `spring.flyway.validate-on-migrate=false`.
- Ningún workflow en `.github/workflows/`.

La validación es la por defecto de Flyway (activa) y los dos logs
versionados demuestran arranque correcto con ella habilitada.

