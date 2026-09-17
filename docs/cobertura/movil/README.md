# Cobertura de app-movil-docente

Reporte JaCoCo del módulo Android en Kotlin/Jetpack Compose.

## Compuerta

Definida en `app-movil-docente/app/build.gradle.kts`:

- Tarea: `jacocoCoverageVerification` (encadenada a `check`).
- Elemento: `BUNDLE`.
- Umbral: `INSTRUCTION >= 0.10` (10 %), coherente con la cobertura real
  actual del módulo. Se documenta como deuda técnica; el objetivo declarado
  es elevarlo al 40 % en próximas iteraciones cuando se instrumenten las
  pantallas Compose con tests de ViewModel.
- Exclusiones estándar: `R.class`, `R$*.class`, `BuildConfig.*`, `Manifest*.*`
  y clases `*_Impl*` generadas por Room.

## Regenerar el reporte

Desde `app-movil-docente`:

```bash
./gradlew clean testDebugUnitTest jacocoTestReport jacocoCoverageVerification
```

El HTML se publica en `docs/cobertura/movil/` y el XML en
`docs/cobertura/movil/jacoco.xml`.
