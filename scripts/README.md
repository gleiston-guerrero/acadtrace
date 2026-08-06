# Scripts de poblado E3

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

```bash
PGPASSWORD=***REMOVED*** psql -h 192.0.2.1 -p 5433 -U postgres -d sga \
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

Contiene los dumps `pg_dump` previos a cada corrida del seed.
