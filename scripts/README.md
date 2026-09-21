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

El dataset canónico es A (`microservicio-soporte/locust_esc1_stats.csv` y sus tres CSV complementarios). El inventario documental comprende seis conjuntos distintos: A oficial nominal; B nominal anterior; C nominal preliminar; D estrés fallido; E ejecución fallida con HTTP 401; F carga corta de 59 segundos. Las rutas y resultados se conservan en [el registro E5](../experimentos/resultados/corridas-e5.md). El script deriva A y el estrés oficial 20260920_164722, verifica sus hashes y métricas contra docs/locust/manifest_carga.json y consulta B y E; no clasifica automáticamente todo el inventario. Los auxiliares nominales no acreditan procedencia de la corrida actual.

```bash
python scripts/recalcular_metricas_carga.py --json
python scripts/recalcular_metricas_carga.py --check-latex
python scripts/recalcular_metricas_carga.py --emit-latex-block
```

`--json` informa métricas derivadas de A, auxiliares, endpoints e históricos B y E; no comprueba documentos. `--emit-latex-block` imprime las macros de A sin escribir. `--check-latex` compara las macros almacenadas y las afirmaciones oficiales seleccionadas del manuscrito (incluido el abstract), además de las secciones oficiales de `README.md`, `docs/locust/README.md`, `docs/locust/entorno_medicion.md`, ambos `protocolo-e4.md` y la tabla de métricas de A en `corridas-e5.md`. Termina con error ante discrepancias o métricas requeridas ausentes. No es un parser general: no valida todo el manuscrito, imágenes, fechas ni resultados históricos. Las cifras se derivan de A, admitiendo el redondeo publicado; cero fallos no demuestra disponibilidad de producción.

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

## Integridad y límites de evidencia (#48)

El manifiesto `docs/locust/manifest_carga.json` fija los SHA-256 de los dos
CSV nominales vigentes y los cuatro CSV del estrés `20260920_164722`, además
de sus métricas declaradas. `scripts/recalcular_metricas_carga.py` verifica
ambos conjuntos antes de emitir resultados o generar macros; `--check-latex`
verifica también los hashes nominales publicados y las afirmaciones nominales
seleccionadas. No valida automáticamente toda la prosa de estrés del informe.
Los hashes se calculan sobre bytes exactos, incluidos finales de línea;
una normalización del archivo provoca fallo y requiere revisión explícita.

Hay **n=1 corrida nominal y n=1 corrida de estrés**. No existen repeticiones
independientes oficiales para estimar variabilidad entre ejecuciones: el
intervalo de confianza entre corridas **no es estimable**. Los percentiles
son descriptivos de peticiones dentro de cada corrida, no repeticiones.

La carpeta oficial de estrés conserva cuatro CSV y dos TXT. `perfil.txt` y
`resumen_validacion.txt` son documentación derivada; la captura relee los CSV.
No se conserva consola original de Locust ni otra evidencia primaria adicional
que acredite independientemente la ejecución. El commit ejecutado, host,
spawn rate, duración configurada y EXIT_CODE son metadatos declarados en esos
TXT, no comprobados por el hash ni por la captura. El nominal tampoco conserva
la evidencia original de configuración. La compuerta acredita integridad y
coherencia de los artefactos declarados, no una validación retrospectiva.
No se usa la corrida incompleta `20260920_163041/` ni los candidatos.
No se ejecutaron nuevas cargas ni se fabricaron repeticiones o logs.

Pruebas reproducibles de mutación, sin tocar los originales:

```bash
python scripts/test_recalcular_metricas_carga.py
```

Comprueban cambios de bytes y SHA-256 en los seis archivos del manifiesto,
y cambios de peticiones, fallos, P95 y P99 de ambos escenarios incluso
actualizando el hash de la copia temporal. No generan corridas Locust.
