# Cobertura de app-movil-docente

Reporte JaCoCo del módulo Android en Kotlin/Jetpack Compose.

## Compuerta

Definida en `app-movil-docente/app/build.gradle.kts`:

- Tarea: `jacocoCoverageVerification` (encadenada a `check`).
- Elemento: `BUNDLE`.
- Umbral: `INSTRUCTION >= 0.10` (**10 %**) a nivel de módulo completo
  (`BUNDLE`).
- Medición oficial: **10,21 % de instrucciones cubiertas
  (7611 de 74556)**.
- Fuente: `docs/cobertura/movil/jacoco.xml`, generado por el CI #876,
  run `35693153935`, sobre el commit `6c1f67ab28d569643b4c7ec4f740d7221bd60b0f`.
- La tarea `jacocoCoverageVerification` forma parte del comando del job
  de CI y aplica la misma compuerta mínima del **10 % INSTRUCTION**
  configurada en `app/build.gradle.kts`.

## Regenerar el reporte

Desde `app-movil-docente`:

```bash
./gradlew clean testDebugUnitTest jacocoTestReport jacocoCoverageVerification
```

El HTML se publica en `docs/cobertura/movil/` y el XML en
`docs/cobertura/movil/jacoco.xml`.
