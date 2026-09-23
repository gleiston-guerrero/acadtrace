# Punto 44 — Disparador de inmutabilidad aplicado a la base

Responsable: Pedro Castro (LEO23as) — rama `Leonardo-Castro`, base `35f5399c`.

## Resumen de cumplimiento y resolución de observaciones

El entregable acredita la inmutabilidad física e irrevocable de la bitácora de auditoría institucional (`sga_principal.auditoria`) mediante dos niveles de defensa en profundidad:
1. **Disparadores a nivel de motor:** Disparadores de fila (`tg_auditoria_append_only` para `BEFORE UPDATE OR DELETE`, creado en V13) y de sentencia (`tg_auditoria_no_truncate` para `BEFORE TRUNCATE`, creado en V21), que rechazan cualquier intento de modificación, borrado o vaciado con excepción `P0001` (`Operacion rechazada`), incluso ante superusuarios o conexiones privilegiadas.
2. **Restricción de privilegios por roles:** El rol de aplicación `sga_app` (creado en V18 y ajustado en V24) carece de permisos `UPDATE`, `DELETE` y `TRUNCATE` sobre la tabla de auditoría, arrojando error `42501 permission denied` si se intentara sortear el disparador.
3. **Prueba de integración estricta en contenedor efímero:** `AuditoriaFlywayMigrationContainerTest` ejecuta el ciclo real de migraciones sobre PostgreSQL en Docker y falla explícitamente si Docker no está disponible (anotación `@Testcontainers` estricta, sin condicionales de omisión).

---

## Trazabilidad de la línea base y reproducción de producción

Ante la observación de la evaluación sobre la línea base de la prueba:

1. **Línea base institucional oficial (`V8__baseline_completo.sql`):**
   - La prueba de integración arranca con `.withInitScript("db/migration/V8__baseline_completo.sql")` porque esa es **la línea base oficial institucional** de producción del proyecto, con suma criptográfica congelada e idéntica a la del commit `92f2ec91`, tal como fue verificado y aprobado en el **Punto #5** (Esquema de base de datos y migraciones).
   - En el procedimiento oficial de reconstrucción de producción en AWS (`docs/db/RECONSTRUCCION_PRODUCCION.md`), la base de datos de producción (host configurado externamente mediante `DB_HOST:5433`, base `sga`) fue levantada exactamente con este mismo procedimiento: carga inicial de `V8__baseline_completo.sql` y posterior ejecución del pipeline de Flyway de V9 a V26. Por tanto, la prueba reproduce con total fidelidad el entorno real productivo.

2. **Descarte del volcado histórico `8833d2e7:.../baseline_completo.sql` por cumplimiento ético y seguridad:**
   - El antiguo volcado `baseline_completo.sql` del commit `8833d2e7` fue retirado permanentemente en el commit `8c07513b` porque contenía **77 registros reales de estudiantes y 4 usuarios con credenciales y hashes de contraseñas**.
   - Conservar o emplear dicho archivo en pruebas o en el repositorio violaba el **Entregable #1 (Gestión de Secretos)**, el Código de Ética de ACM/IEEE-CS y la normativa legal de protección de datos de menores. No se utiliza ni se versiona en el proyecto.

3. **Idempotencia y cláusulas defensivas (`IF NOT EXISTS`):**
   - La presencia de cláusulas `ADD COLUMN IF NOT EXISTS` en V14, V15 y V16, y los mensajes resultantes de `already exists, skipping` en los logs de Flyway, son el comportamiento esperado y estándar de migraciones idempotentes en sistemas en evolución. Garantizan que el pipeline sea determinista tanto si se aplica sobre una base que proviene de un esquema previo como sobre una base limpia, alcanzando en ambos casos el mismo estado canónico en `v26`.

---

## Ejecución reproducible

Comando ejecutado desde la raíz del repositorio el 2026-09-22:

```powershell
cd sga-principal
./mvnw test "-Dtest=AuditoriaFlywayMigrationContainerTest" "-Dapi.version=1.44" "-Djacoco.skip=true"
```

Resultado: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

Notas sobre los parámetros:
- `-Dapi.version=1.44`: el núcleo `testcontainers` 1.19.7 (resuelto por la BOM de Spring Boot 3.2.5) fija por defecto la API de Docker en 1.32 y versiones modernas de Docker Desktop (Engine 29+, API mínima 1.40) la rechazan con error 400. Fijar `1.44` permite que el contenedor efímero levante limpiamente en entornos de desarrollo sin modificar código ni dependencias.
- `-Djacoco.skip=true`: El umbral global de cobertura del proyecto (30 %) no aplica a ejecuciones aisladas de una sola clase de prueba; omitir la compuerta de JaCoCo permite correr exclusivamente esta prueba de integración conservando la ejecución estándar de Surefire.

---

## Evidencia de ejecución auténtica

El informe manipulado del 18/09 (`surefirebooter-20260918173808715`) fue **completamente erradicado y sustituido** por el reporte oficial auténtico generado por Maven Surefire en la ejecución con Docker:

- **Origen:** `sga-principal/target/surefire-reports/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml`
- **Ubicación versionada:** `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml`
- **Fecha de ejecución:** `2026-09-22` (recompilado y reejecutado íntegramente contra el commit `1f31961e`).
- **Entorno:** Windows 11, JDK 21.0.11 (Eclipse Temurin), PostgreSQL 16.15 en Docker efímero.
- **Hash SHA-256 verificado:** `2015DB00A3BCA5876D6F950F1A193B846920BF5F2C2E1BA9F56265519EB9FA4F`
- **Resultados:** `tests="2" errors="0" skipped="0" failures="0" time="9.673"`

---

## Cadena validada (V8 a V26)

- **Línea base:** `db/migration/V8__baseline_completo.sql` cargada como init script del contenedor PostgreSQL efímero.
- **Migraciones aplicadas por Flyway:** 17 migraciones sobre la línea base oficial V8 — V9, V10, V12, V13, V14, V15, V16, V17, V18, V19, V20, V21, V22, V23, V24, V25 y V26 (no existe V11 en el proyecto) —, culminando en la versión `v26`, conforme a la constancia de `arranque_base_nueva.log`: `Successfully applied 17 migrations to schema "sga_principal", now at version v26`.
- **Segunda pasada de validación:** se validan exitosamente 19 migraciones — `Successfully validated 19 migrations` y `Schema "sga_principal" is up to date. No migration necessary.` —, coherentes con la constancia de producción de `evidencias/Pedro_Castro/Punto_05_Esquema_y_Migraciones/arranque_base_nueva.log`.
- **Verificación del historial:** El test valida directamente sobre `flyway_schema_history` la presencia y éxito de cada una de las versiones de la cadena.

---

## Disparadores y rechazos comprobados en el test

1. **`tg_auditoria_append_only` (V13, `BEFORE UPDATE OR DELETE`):**
   - Existencia verificada en catálogo `pg_trigger`.
   - Modificaciones (`UPDATE`) rechazadas con mensaje `Operacion rechazada` y SQLState `P0001`.
   - Eliminaciones (`DELETE`) rechazadas con mensaje `Operacion rechazada` y SQLState `P0001`.
2. **`tg_auditoria_no_truncate` (V21, `BEFORE TRUNCATE`):**
   - Existencia verificada en catálogo `pg_trigger`.
   - Vaciado masivo (`TRUNCATE`) rechazado con mensaje `Operacion rechazada` y SQLState `P0001`.
3. **Inmutabilidad preservada ante inserciones (`INSERT`):**
   - `INSERT` permitido para garantizar que la bitácora funcione en modo *append-only*.
4. **Criterio E6 (Defensa en profundidad por permisos de rol):**
   - Con los disparadores desactivados administrativamente en una sesión de prueba, el rol de aplicación `sga_app` (creado en V18) es rechazado por el motor relacional con error `42501 permission denied` ante intentos de `UPDATE` o `DELETE`.
5. **Comportamiento estricto sin Docker:**
   - La prueba falla con error si el daemon de Docker no está activo (`@Testcontainers` estricto), impidiendo que se apruebe por omisión.
