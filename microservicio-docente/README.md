# MICRO-DOCENTE

Microservicio Django REST para el modulo de docentes del SGA de la Escuela de Educacion Basica Provincias Unidas.

## Stack

- Python 3.13
- Django 6.0.6
- Django REST Framework
- PostgreSQL Supabase
- Schema: `sga_docente`
- Puerto: `8081`

## Instalacion

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py runserver 0.0.0.0:8081
```

La configuracion de Supabase se toma desde variables de entorno. El archivo `.env.example` contiene los valores base entregados para desarrollo.

## Endpoints

Base URL local: `http://localhost:8081/api/docente/`

| Recurso | Endpoint |
| --- | --- |
| Periodos de evaluacion | `/periodos-evaluacion/` |
| Actividades | `/actividades/` |
| Calificaciones | `/calificaciones/` |
| Asistencias | `/asistencias/` |
| Resumen de asistencia | `/resumen-asistencia/` |
| Promedios trimestrales | `/promedios-trimestrales/` |
| Promedios anuales | `/promedios-anuales/` |
| Detalle de promedios anuales | `/promedios-anuales-detalle/` |
| Seguimiento academico | `/seguimiento-academico/` |

Todos los recursos tienen CRUD REST: `GET`, `POST`, `GET /{id}/`, `PUT`, `PATCH`, `DELETE`.

## Auditoría académica

La variable `AUDIT` selecciona una única estrategia para mutaciones REST y gRPC:

| Valor | Comportamiento |
| --- | --- |
| `m0` | Sin escrituras, hashes ni relojes de auditoría. Es el valor predeterminado. |
| `m1` | Bitácora plana PostgreSQL con evento y payload canónico. |
| `m2` | Cadena SHA-256 enlazada y reloj lógico de Lamport. |
| `m3` | Todo m2, más reloj vectorial y detección de versiones concurrentes. |

El payload se serializa como JSON UTF-8 con claves ordenadas y separadores
compactos. Nunca incluye JWT, `Authorization`, passwords, tokens internos o
secretos. En m3 una edición concurrente se marca `CONFLICTO`: ambas versiones
quedan preservadas en auditoría para resolución manual y no se elige un ganador
silencioso.

La cadena usa un registro de estado bloqueado mediante la transacción y
`select_for_update()`. El material exacto del hash es:

```text
SHA256(hash_anterior + JSON_CANONICO(evento, timestamp, payload, Lamport y vector))
```

### Verificación y experimentos

```powershell
python -m pytest
python -m pytest --cov=. --cov-report=term-missing --cov-report=html:..\docs\cobertura\docente
python experimentos/verificador_cadena.py
python experimentos/generador_sintetico.py --salida dataset-docente.json
```

El inyector es destructivo y se bloquea salvo que se use una base experimental,
`DJANGO_DEBUG=True` y autorización explícita:

```powershell
$env:ALLOW_AUDIT_EXPERIMENTS="1"
python experimentos/inyector_manipulaciones.py --tipo T1
```

Los tipos disponibles son T1, T2, T3, T4 y T5. El verificador solo diagnostica;
devuelve código 0 para una cadena válida y 2 para una cadena inválida, sin reparar.

## Endpoints de negocio

### Promedio formativo

```http
GET /api/docente/calificaciones/promedio-formativo/?id_matricula=1&id_asignacion=10&id_periodo=1&nivel=EGB
```

Calcula promedio simple de calificaciones asociadas a actividades no sumativas.

### Promedio trimestral

```http
POST /api/docente/promedios-trimestrales/calcular/
Content-Type: application/json

{
  "id_matricula": 1,
  "id_asignacion": 10,
  "id_periodo": 1,
  "nivel": "EGB"
}
```

Formula: `promedio_trimestral = promedio_formativo * 0.70 + nota_sumativa * 0.30`.

### Promedio anual

```http
POST /api/docente/promedios-anuales/calcular/
Content-Type: application/json

{
  "id_matricula": 1,
  "id_asignacion": 10,
  "id_ano_lectivo": 2026,
  "nivel": "EGB",
  "registrado_por": 1
}
```

Promedia los promedios trimestrales calculados para el estudiante, asignacion y ano lectivo.

### Resumen de asistencia

```http
POST /api/docente/resumen-asistencia/calcular/
Content-Type: application/json

{
  "id_matricula": 1,
  "id_asignacion": 10,
  "id_periodo": 1
}
```

Cuenta presentes, ausentes, justificados y atrasos.

## Conversion cuantitativa a cualitativa

Para `EGB`:

| Rango | Codigo |
| --- | --- |
| 9.00 - 10.00 | `DAR` |
| 7.00 - 8.99 | `AAR` |
| 4.01 - 6.99 | `PAR` |
| 0.00 - 4.00 | `NAR` |
