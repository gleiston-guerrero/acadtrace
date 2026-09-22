# Cobertura de app-movil-docente

Reporte JaCoCo del módulo Android en Kotlin/Jetpack Compose.

## Compuerta

Definida en `app-movil-docente/app/build.gradle.kts`:

- Tarea: `jacocoCoverageVerification` (encadenada a `check`).
- Elemento: `BUNDLE`.
- Umbral: `INSTRUCTION >= 0.02` (**2 %**), utilizado como línea base
  automática de no regresión.
- Medición oficial: **10,21 % de instrucciones cubiertas
  (1828 de 74556)** a nivel de módulo completo (`BUNDLE`).
- Fuente: `docs/cobertura/movil/jacoco.xml`, generado por el CI #876,
  run `35693153935`, sobre el commit `6c1f67ab28d569643b4c7ec4f740d7221bd60b0f`.
- La tarea `jacocoCoverageVerification` forma parte ahora del comando del
  job de CI. El 2 % es una línea base mínima verificable y no se presenta
  como objetivo final de calidad; deberá incrementarse al ampliar la suite
  de pruebas.

## Regenerar el reporte

Desde `app-movil-docente`:

```bash
./gradlew clean testDebugUnitTest jacocoTestReport jacocoCoverageVerification
```

El HTML se publica en `docs/cobertura/movil/` y el XML en
`docs/cobertura/movil/jacoco.xml`.
