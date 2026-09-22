# Punto 44 — Disparador de inmutabilidad aplicado a la base

Responsable: Pedro Castro (LEO23as) — rama `Leonardo-Castro`, base `35f5399c`.

## Ejecución reproducible

Comando ejecutado desde la raíz del repositorio el 2026-09-21:

```powershell
cd sga-principal
./mvnw test "-Dtest=AuditoriaFlywayMigrationContainerTest" "-Dapi.version=1.44" "-Djacoco.skip=true"
```

Resultado: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

Notas sobre los parámetros:

- `-Dapi.version=1.44`: Testcontainers 1.19.7 fija por defecto la API de Docker en
  1.32 y Docker Desktop 4.90 (Engine 29, API mínima 1.40) la rechaza con 400.
  Fijar 1.44 permite que el contenedor efímero levante sin tocar código ni el pom.
- `-Djacoco.skip=true`: el gate de cobertura del proyecto (30 %) no aplica al correr
  una sola clase; se omite solo la verificación de JaCoCo, la ejecución de Surefire
  es la estándar.

## Evidencia

Reporte generado por Maven Surefire, copiado byte por byte sin edición manual:

- Origen: `sga-principal/target/surefire-reports/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml`
- Versionado en: `docs/evidencia/pruebas/sga-principal/TEST-ec.edu.uteq.sga.integration.AuditoriaFlywayMigrationContainerTest.xml`
- SHA-256: `4C88EE974C7BAE1D630B40D58ABC67DC684B4720F4DA7C8D0B3B33692E69D042`

El XML contiene el timestamp de la corrida (`2026-09-21T21-52-54`) y el log completo
de Flyway; cualquier verificador puede recalcular el hash y comparar contra el
archivo versionado.

## Cadena validada (V8 a V26)

Baseline: `db/migration/V8__baseline_completo.sql` como init script del contenedor.
Flyway aplica sobre el esquema `sga_principal`: V9, V10, V12, V13, V14, V15, V16,
V17, V18, V19, V20, V21, V22, V23, V24, V25 y V26 (no existe V11 en el proyecto),
dejando el esquema en `v26`. El historial oficial `flyway_schema_history` se
verifica versión por versión dentro del test.

## Disparadores y rechazos comprobados

- `tg_auditoria_append_only` (V13, `BEFORE UPDATE OR DELETE`): existencia
  verificada por definición; UPDATE y DELETE rechazados con `Operacion rechazada`
  y SQLState `P0001`.
- `tg_auditoria_no_truncate` (V21, `BEFORE TRUNCATE`): existencia verificada por
  definición; TRUNCATE rechazado con `Operacion rechazada` y SQLState `P0001`.
- Criterio E6: con el disparador desactivado, el rol `sga_app` (creado en V18)
  es rechazado por permisos a nivel de motor (`42501 permission denied`) en
  UPDATE y DELETE.
- Sin Docker la prueba falla explícitamente (`@Testcontainers` sin
  `disabledWithoutDocker` ni condiciones de omisión).
