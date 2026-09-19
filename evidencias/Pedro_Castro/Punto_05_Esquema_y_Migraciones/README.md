# Evidencia del entregable #5 — Esquema de base de datos y migraciones

Este directorio contiene la constancia de arranque correcto de
`sga-principal` con la validación de Flyway ACTIVA
(`validateOnMigrate=true` por defecto). Tras las correcciones realizadas,
ya no es necesario invocar `repair()` para reescribir historiales y se ha
recuperado el historial inmutable de migraciones.

Los logs fueron regenerados contra la punta del proyecto el 2026-09-18T19:25:00Z:
- Commit base aproximado: 4e8effff (con las correcciones del entregable aplicadas).

1. **Base ya migrada**: Arranque sobre una base que ya contenía las migraciones
   hasta V22. Al estar V19 corregida a su estado original (commit 186593cb),
   Flyway la valida correctamente. Luego, aplica la nueva migración V23.
2. **Base NUEVA**: Arranque desde vacío, aplicando la línea base
   completa `V8__baseline_completo.sql` y luego las migraciones V9 a V23 en orden.

## Archivos versionados

- `arranque_base_migrada.log`: log del arranque en el escenario 1, capturando
  la salida real de `sga-principal`.
- `arranque_base_nueva.log`: log del arranque en el escenario 2, capturando
  la salida real de `sga-principal` desde línea base.
- `Captura de pantalla 2026-09-16 110617.png` y
  `Captura de pantalla 2026-09-16 110659.png`: capturas previas de contexto.

## Trazabilidad

- Línea base oficial: `sga-principal/src/main/resources/db/migration/V8__baseline_completo.sql`.
- Nueva migración: `sga-principal/src/main/resources/db/migration/V23__grants_sga_secretaria_y_auditoria_diferidos.sql`.
- Configuración de Flyway (`spring.flyway.locations=classpath:db/migration`) en `application.properties`.
- Documentación completa: `sga-principal/src/main/resources/db/migration/README.md`.
