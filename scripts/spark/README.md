# Spark E3

## Driver JDBC PostgreSQL

The file `jars/postgresql-42.7.5.jar` is required by the Spark jobs. Download it
from the repository root when needed:

```bash
curl -Lo scripts/spark/jars/postgresql-42.7.5.jar https://jdbc.postgresql.org/download/postgresql-42.7.5.jar
```

The global `*.jar` rule ignores this file; `.gitignore` is intentionally unchanged.

## Trimestral weighting

The current schema has no foreign key from `sga_docente.actividades` to
`sga_principal.tipos_aporte`. The E3 seed creates each activity name as
`tipos_aporte.nombre + ' - ' + periodos_evaluacion.nombre`, so the Spark job uses
that prefix and `id_ano_lectivo` as a descriptive join.

`tipos_aporte` has no `porcentaje` column in the current schema. The job uses
`actividades.ponderacion`, the available structural weight, and gives priority to
`porcentaje` only if that column is added later. A manually created activity
without the seed prefix does not join to `tipos_aporte` and keeps its own weight.
