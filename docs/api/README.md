# Contrato OpenAPI de AcadTrace

## Estrategia

`openapi.yaml` es un catálogo agregado OpenAPI 3.0.3. Las operaciones están separadas por tags (`principal`, `secretaria`, `docente`, `soporte`, `ia`) y declaran `servers` por operación para no mezclar los puertos de los microservicios.

La fuente de verdad de cada servicio sigue siendo su implementación:

- `sga-principal`: controladores Spring bajo `sga-principal/src/main/java/.../presentation/controller` y configuración de seguridad.
- `microservicio-secretaria`: controladores Spring bajo `microservicio-secretaria/backend/src/main/java/.../presentation/controller` y sus DTOs.
- `microservicio-docente`: `microservicio-docente/micro_docente/urls.py`, `microservicio-docente/docentes/urls.py`, ViewSets y serializers.
- `microservicio-soporte`: controladores y DTOs bajo `microservicio-soporte/backend/src/main/java/.../presentation` y `.../dto`.
- `microservicio-ia`: `microservicio-ia/app/main.py`; FastAPI genera el contrato runtime.

El bloque IA del catálogo documenta las rutas verificadas, pero el contrato autoritativo de IA es `GET http://localhost:8084/openapi.json`. La misma aplicación expone Swagger UI en `/docs` y ReDoc en `/redoc`.

## Validación reproducible

Requiere Python con PyYAML disponible:

```powershell
py -m pip install pyyaml
py docs/api/validate_openapi.py
```

El script valida todos los YAML/JSON OpenAPI versionados bajo `docs/api`, comprueba referencias locales, rechaza `/api/secretaria` y verifica las rutas críticas de los cinco servicios.

Para consultar el contrato generado por IA cuando el servicio esté levantado:

```powershell
Invoke-WebRequest http://localhost:8084/openapi.json
Start-Process http://localhost:8084/docs
Start-Process http://localhost:8084/redoc
```

No se mantienen contratos Swagger duplicados para IA: `microservicio-ia/app/main.py` es la fuente runtime.
