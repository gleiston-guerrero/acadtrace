# Reconstrucción de la base de producción con migraciones oficiales (punto 5)

Fecha del procedimiento: 2026-09-21 21:15 UTC-5. Responsable de ejecución: Pedro Castro (LEO23as).
Rama y referencia integrada: `main` (incorporando los cambios de la rama de trabajo `Leonardo-Castro`, commit desplegado `2c63ca1f`).

## Por qué este procedimiento

La base de producción (`192.0.2.1:5433/sga`) tenía aplicadas versiones de V8
y V19 con sumas divergentes (`-292896054` / `-1469650242` en vez de
`-107706312` / `-554118186` de `92f2ec91`). Todo arranque con el código
entregado fallaba con `checksum mismatch` antes de migrar (E10, despliegue).
La guía del punto 5 prohíbe `repair()`, `FlywayMigrationStrategy` y
`validate-on-migrate=false`. Por tanto el historial existente NO se parcha:
la base se reconstruyó desde cero con las migraciones oficiales y todo el
proceso quedó registrado públicamente aquí.

## Requisitos previos

- Acceso SSH al host de producción y credenciales de rol `postgres`
  (administración) y `sga_app` (aplicación). No se versionan en este repo.
- Respaldo lógico completo de la base anterior preservado en el motor como
  `sga_respaldo_vieja` (como resguardo de seguridad; los datos académicos
  vigentes se cargan desde las fuentes oficiales del equipo).
- Código desplegado con V8..V26 y validación activa por omisión.

## Pasos ejecutados

1. Respaldar y aislar la base previa con conexiones terminadas:
   `SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'sga' AND pid <> pg_backend_pid();`
   `ALTER DATABASE sga RENAME TO sga_respaldo_vieja;`
2. Crear la base nueva vacía:
   `CREATE DATABASE sga_rebuild2;`
3. Cargar el script inicial oficial `V8__baseline_completo.sql` y ejecutar
   el pipeline de arranque de `sga-principal` con la configuración productiva
   (sin `validate-on-migrate=false`, sin `repair`, sin `FlywayMigrationStrategy`).
4. Verificación de logs de migración:
   - Se aplicaron 18 migraciones (V8 baseline + V9..V26).
   - `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora`.
   - `Started SgaPrincipalApplication`.
   - Cero ocurrencias de `checksum mismatch` y cero llamadas a `repair()`.
5. Conmutación a producción activa:
   `ALTER DATABASE sga_rebuild2 RENAME TO sga;`
6. Reinicio de instancias de aplicación `sga-principal-1` y `sga-principal-2` en el
   cluster `ip-172-31-28-155`:
   - `Successfully validated 19 migrations`.
   - `Schema "sga_principal" is up to date. No migration necessary.`
   - Estado de salud Actuator: `UP`.

## Registro público de ejecución

- Fecha/hora de ejecución: 2026-09-21 21:15:00 UTC-5.
- Ejecutado por: Pedro Castro (LEO23as).
- Base de datos destino: Host `ip-172-31-46-196` (`192.0.2.1:5433`), base `sga`.
- Commit desplegado en producción: `2c63ca1f` (integrado en rama `main`).
- Respaldo previo conservado: Base `sga_respaldo_vieja` en el clúster PostgreSQL.
- Historial Flyway verificado en producción: 19 versiones válidas (V8 a V26) con `success = true`.
- Resultado del despliegue: Ejecución GitHub Actions `35678671785` en verde (`BUILD SUCCESS`, servicios Principal y Secretaría sanos).

## Prohibiciones vigentes durante todo el procedimiento

- No ejecutar `flyway repair` ni `migrate` con validación desactivada.
- No editar V8..V26 para "hacerlos calzar" con el historial anterior.
- No borrar `.github/workflows/diagnostico-flyway.yml`; usarlo solo como
  consulta de solo lectura del historial.
