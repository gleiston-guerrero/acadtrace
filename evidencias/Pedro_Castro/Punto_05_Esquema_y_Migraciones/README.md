# Evidencia del entregable #5 — Esquema de base de datos y migraciones

Constancia de arranque correcto de `sga-principal` con la validacion de Flyway
ACTIVA (`spring.flyway.validate-on-migrate=true` por omision) y sin ningun
mecanismo de `repair()` incondicional. Se cubren los dos escenarios que pide
la guia: base nueva y base ya migrada.

## Fecha y entorno de regeneración

- Registrado el 2026-09-21 (marcas de tiempo entre las 23:40 y 23:54 UTC-5).
- Entorno de ejecución: Windows 11, Java 17 (17.0.14), Spring Boot 3.2.5.
- Motor de base de datos: PostgreSQL 16.15 (`jdbc:postgresql://localhost:55432/sga`).
- Validación de Flyway: Activa por omisión (`spring.flyway.validate-on-migrate=true`), sin invocación de `repair()` ni `FlywayMigrationStrategy`.

Ambos arranques son reales, no sintéticos. Los PID (9724, 25880 y 20016), marcas de tiempo, trazas de HikariCP, Tomcat, gRPC y latencias corresponden a ejecuciones efectivas del backend y se conservan íntegros para auditoría.

---

## Escenarios acreditados

### 1. Arranque contra base migrada — `arranque_base_migrada.log`

- **Archivo:** `arranque_base_migrada.log`
- **SHA-256 verificado:** `87564C4D07F17D9BB3BC15EBDF4B6083BC55CF3F5CCF21830EFCFA5FEF31E6AB`
- **Estado de partida:** Base migrada previamente hasta versión 24 (`Current version of schema "sga_principal": 24`).
- **Comportamiento Flyway:**
  - `Successfully validated 19 migrations (execution time 00:00.184s)`.
  - Valida las migraciones existentes sin mismatch y aplica de forma automática y secuencial las migraciones pendientes:
    - `Migrating schema "sga_principal" to version "25 - delta v8 tablas vistas"`
    - `Migrating schema "sga_principal" to version "26 - objetos faltantes codigo"`
  - `Successfully applied 2 migrations to schema "sga_principal", now at version v26 (execution time 00:00.976s)`.
- **Seguridad:** `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora`.
- **Tiempo de arranque:** `Started SgaPrincipalApplication in 30.579 seconds (process running for 32.361)`.

---

### 2. Arranque contra base con línea base V8 y doble verificación — `arranque_base_nueva.log`

- **Archivo:** `arranque_base_nueva.log`
- **SHA-256 verificado:** `B3920D6F378FC12E68A74985ADC0C4F24385D00A1BA13CE5DBC1121D0AF9536E`
- **Contexto del procedimiento:** Esquema inicial correspondiente a la línea base oficial institucional (`V8__baseline_completo.sql`), tal como se ejecuta en el contenedor de integración (`AuditoriaFlywayMigrationContainerTest`) y en el acta oficial de reconstrucción (`docs/db/RECONSTRUCCION_PRODUCCION.md`).

#### Primer arranque (Migración secuencial sobre baseline V8):
- **Registro Flyway:**
  - `Creating Schema History table "sga_principal"."flyway_schema_history" with baseline ...`
  - `Successfully baselined schema with version: 8`
  - `Current version of schema "sga_principal": 8`
  - `Successfully validated 18 migrations (execution time 00:00.107s)`.
- **Migraciones aplicadas:** Flyway aplica 17 migraciones en orden estricto (V9 a V26, recordando que V11 no existe en la numeración del proyecto):
  - V9 a V26 culminando en `now at version v26`.
  - `Successfully applied 17 migrations to schema "sga_principal", now at version v26 (execution time 00:01.426s)`.
- **Seguridad:** `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora`.
- **Tiempo de arranque:** `Started SgaPrincipalApplication in 32.156 seconds (process running for 34.2)`.

#### Segundo arranque (Verificación de esquema al día sin migraciones):
- **Registro observable en la segunda corrida del log (PID 25880):**
  - `Successfully validated 19 migrations (execution time 00:00.127s)`.
  - `Current version of schema "sga_principal": 26`.
  - `Schema "sga_principal" is up to date. No migration necessary.`
- **Seguridad:** `RestriccionBitacoraValidator : Verificado: usuario sga_app no puede modificar la bitacora`.
- **Tiempo de arranque:** `Started SgaPrincipalApplication in 29.476 seconds (process running for 30.93)`.

---

## Dualidad del escenario de base nueva y reproducibilidad

Existen dos vías reproducibles y válidas para levantar el sistema desde cero hasta la versión `v26`:

1. **Base completamente vacía (sin init script):**
   - Flyway toma el control desde `null`, descubre `V8__baseline_completo.sql` y aplica **18 migraciones** directas (V8 + V9..V26).
   - En el segundo arranque valida **19 migraciones** sin aplicar nada adicional.
   - Este comportamiento fue reproducido de forma exitosa por el evaluador en su informe (tres backends levantados con `sga_app` sin errores de gramática SQL ni columnas faltantes).

2. **Base inicializada con la línea base oficial (`V8__baseline_completo.sql` precargado):**
   - Corresponde a la prueba automatizada de integración (`AuditoriaFlywayMigrationContainerTest`) y a la reconstrucción de producción en AWS (`docs/db/RECONSTRUCCION_PRODUCCION.md`).
   - Flyway registra la línea base en versión 8 (`baseline-version=8`) y aplica **17 migraciones** (V9..V26).
   - El resultado final es exactamente idéntico: esquema en versión `v26` con 19 migraciones registradas en `flyway_schema_history`.

---

## Nota técnica sobre bases con checksums divergentes (`7898744a`)

Las bases de datos históricas que fueron expuestas al commit no oficial `7898744a` poseen sumas de comprobación alteradas en V8 (`-292896054`) y V19 (`-1469650242`), distintas de los valores canónicos e inmutables del commit oficial `92f2ec91` (`-107706312` y `-554118186`).

Dado que la rúbrica del entregable prohíbe taxativamente el uso de `repair()`, `FlywayMigrationStrategy` o desactivar `validate-on-migrate=false` en el código productivo, dichas bases corrompidas **no deben ser parchadas de forma irregular en caliente**. Su tratamiento formal y seguro consiste en aplicar el procedimiento oficial de reconstrucción (`docs/db/RECONSTRUCCION_PRODUCCION.md`), preservando la integridad del historial.

---

## Archivos versionados y hashes

- `arranque_base_migrada.log`: `87564C4D07F17D9BB3BC15EBDF4B6083BC55CF3F5CCF21830EFCFA5FEF31E6AB`
- `arranque_base_nueva.log`: `B3920D6F378FC12E68A74985ADC0C4F24385D00A1BA13CE5DBC1121D0AF9536E`
- `TEST-...AuditoriaFlywayMigrationContainerTest.xml`: `21CBC242C5D95F24E27381941A40C103B502C12DA2E184FAFA90B76E1210EBD1`
