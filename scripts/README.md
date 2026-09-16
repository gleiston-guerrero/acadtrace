# Scripts de poblado E3

## Validación de finales de línea (E17)

`python scripts/verificar_finales_linea.py` examina los blobs de `HEAD` mediante
`git cat-file`, sin depender de los finales del checkout de Windows. Revisa
CSV, TSV, SQL, MD, YML, YAML, PY, JAVA, JS, JSX, JSON, XML, PROPERTIES, SH y TEX
cuando los atributos efectivos exigen LF y no declaran el archivo binario.
Imprime cada ruta con CRLF y termina con 1 si encuentra alguna o falla la lectura;
devuelve 0 si todas cumplen. CI lo ejecuta en el job `lint` antes de E7.

`--source worktree` comprueba las copias locales de esas mismas rutas para
revisar una normalización todavía sin commit. No sustituye la comprobación de
HEAD: mientras los cambios no se confirmen, los blobs antiguos siguen presentes.
El script no modifica archivos, índice ni certificados.

## Recálculo y Derivación Determinista de Métricas de Carga (E5 / Item 48)

`python scripts/recalcular_metricas_carga.py` lee exclusivamente el conjunto oficial declarado en `microservicio-soporte/locust_esc1_stats.csv` y sus archivos complementarios (`stats_history.csv`, `failures.csv`, `exceptions.csv`).

- Deriva deterministamente el total de peticiones (12.994), fallos (0), rendimiento (43.537580 req/s), latencia promedio (109.113664 ms), percentiles (P50=6 ms, P95=440 ms, P99=850 ms, Max=2037.104700 ms) y desglose por endpoint.
- Clasifica formalmente los 5 juegos de datos existentes en el repositorio (1 oficial, 4 preliminares/históricos/retirados).
- Audita automáticamente que `Informe-E4_BCEL/TA-PFC-E4_BCEL.tex` se encuentre 100% sincronizado y libre de cifras retractadas o inventadas (como 12.265 o 12.735).
- Soporta exportación en formato JSON (`--json`) o verificación estricta (`--check-latex`).


## `seed_e3_500k.sql`

Poblado masivo del dataset para cumplir el requisito de Entrega 3
(dataset >= 500.000 registros).

### Que inserta

| Tabla | Filas insertadas |
|---|---|
| `sga_docente.periodos_evaluacion` | 2 (trimestres 2 y 3 faltantes) |
| `sga_secretaria.estudiantes` | 600 (50 por paralelo A de cada grado) |
| `sga_principal.estudiantes` | 600 (mismos ids, patron existente) |
| `sga_principal.matriculas` | 600 |
| `sga_docente.actividades` | 918 (34 asignaciones x 3 periodos x 9 tipos_aporte) |
| `sga_docente.calificaciones` | ~70.500 |
| `sga_docente.asistencias` | ~563.700 |

**Total agregado: ~636.000 filas**

### Como correrlo

La variable `PGPASSWORD` debe suministrarse externamente en el entorno del proceso
antes de ejecutar `psql`. No guardar la contrasena en el repositorio.

```bash
psql -h 3.23.195.43 -p 5433 -U postgres -d sga \
  -v ON_ERROR_STOP=1 -f scripts/seed_e3_500k.sql
```

Los estudiantes seed usan `codigo_estudiante` con prefijo `EST-SEED-` y
cedulas en rango `1250000001..1250000600` (provincia 12 - Los Rios).

### Como revertir

```sql
BEGIN;
DELETE FROM sga_docente.calificaciones c
 USING sga_principal.matriculas m, sga_secretaria.estudiantes s
 WHERE c.id_matricula = m.id_matricula
   AND m.id_estudiante = s.id_estudiante
   AND s.codigo_estudiante LIKE 'EST-SEED-%';

DELETE FROM sga_docente.asistencias a
 USING sga_principal.matriculas m, sga_secretaria.estudiantes s
 WHERE a.id_matricula = m.id_matricula
   AND m.id_estudiante = s.id_estudiante
   AND s.codigo_estudiante LIKE 'EST-SEED-%';

DELETE FROM sga_principal.matriculas m
 USING sga_secretaria.estudiantes s
 WHERE m.id_estudiante = s.id_estudiante
   AND s.codigo_estudiante LIKE 'EST-SEED-%';

DELETE FROM sga_principal.estudiantes  WHERE codigo_estudiante LIKE 'EST-SEED-%';
DELETE FROM sga_secretaria.estudiantes WHERE codigo_estudiante LIKE 'EST-SEED-%';
COMMIT;
```

## `backups/`

Los respaldos pre-seed pueden contener datos sensibles: no deben versionarse y deben almacenarse fuera del repositorio. Para reproducibilidad, utilizar el esquema y las migraciones junto con datos sint?ticos apropiados.
