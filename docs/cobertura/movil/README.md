# Cobertura de app-movil-docente

Reporte JaCoCo del módulo Android en Kotlin/Jetpack Compose.

## Compuerta

Definida en `app-movil-docente/app/build.gradle.kts`:

- Tarea: `jacocoCoverageVerification` (encadenada a `check`).
- Elemento: `BUNDLE`.
- Umbral: `INSTRUCTION >= 0.10` (10 %).
- Medición actual reproducible: `1828/74556` instrucciones cubiertas,
  equivalente a `2,45 %` de cobertura `INSTRUCTION` del `BUNDLE`.
- La medición actual no supera la compuerta del 10 %. Se mantiene el umbral
  como deuda técnica y no se reduce para adaptar la regla al resultado medido.
- Exclusiones estándar: `R.class`, `R$*.class`, `BuildConfig.*`, `Manifest*.*`
  y clases `*_Impl*` generadas por Room.

## Regenerar el reporte

Desde `app-movil-docente`:

```bash
./gradlew clean testDebugUnitTest jacocoTestReport jacocoCoverageVerification
```

El HTML se publica en `docs/cobertura/movil/` y el XML en
`docs/cobertura/movil/jacoco.xml`.
