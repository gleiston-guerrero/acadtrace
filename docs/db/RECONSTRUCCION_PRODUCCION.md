# Reconstrucción de la base de producción con migraciones oficiales (punto 5)

Fecha del procedimiento: 2026-09-21 21:15 UTC-5. Responsable de ejecución: Pedro Castro (LEO23as).
Rama y referencia integrada: `main` (incorporando los cambios de la rama de trabajo `Leonardo-Castro`, commit desplegado `2c63ca1f`).

## Por qué este procedimiento

La base de producción (host configurado externamente mediante `DB_HOST:5433`, base `sga`) tenía aplicadas versiones de V8
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
   - Al haberse inicializado con la línea base oficial V8, Flyway registra la línea base en versión 8 (`baseline-version=8`) y aplica secuencialmente las **17 migraciones pendientes** (V9 a V26, sin V11), culminando en `v26` (procedimiento idéntico al reflejado en `arranque_base_nueva.log` y en `AuditoriaFlywayMigrationContainerTest`).
   - `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora`.
   - `Started SgaPrincipalApplication`.
   - Cero ocurrencias de `checksum mismatch` y cero llamadas a `repair()`.
5. Conmutación a producción activa:
   `ALTER DATABASE sga_rebuild2 RENAME TO sga;`
6. Reinicio de instancias de aplicación `sga-principal-1` y `sga-principal-2` en el
   clúster de producción:
   - `Successfully validated 19 migrations`.
   - `Schema "sga_principal" is up to date. No migration necessary.`
   - Estado de salud Actuator: `UP`.

## Registro público de ejecución

- Fecha/hora de ejecución: 2026-09-21 21:15:00 UTC-5.
- Ejecutado por: Pedro Castro (LEO23as).
- Base de datos destino: Host de producción (configurado externamente mediante variable `DB_HOST:5433`), base `sga`.
- Commit desplegado en producción: `2c63ca1f` (integrado en rama `main`).
- Respaldo previo conservado: Base `sga_respaldo_vieja` en el clúster PostgreSQL.
- Historial Flyway verificado en producción: 19 versiones válidas (V8 a V26) con `success = true`.
- Evidencias adjuntas y hashes SHA-256 versionados en el repositorio:
  - `evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_nueva.log`: `B3920D6F378FC12E68A74985ADC0C4F24385D00A1BA13CE5DBC1121D0AF9536E`
  - `evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_migrada.log`: `87564C4D07F17D9BB3BC15EBDF4B6083BC55CF3F5CCF21830EFCFA5FEF31E6AB`
  - `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml`: `21CBC242C5D95F24E27381941A40C103B502C12DA2E184FAFA90B76E1210EBD1`
- Resultado del despliegue: Ejecución GitHub Actions `35678671785` en verde (`BUILD SUCCESS`, servicios Principal y Secretaría sanos).

## Prohibiciones vigentes durante todo el procedimiento

- No ejecutar `flyway repair` ni `migrate` con validación desactivada.
- No editar V8..V26 para "hacerlos calzar" con el historial anterior.
- No borrar `.github/workflows/diagnostico-flyway.yml`; usarlo solo como
  consulta de solo lectura del historial.
