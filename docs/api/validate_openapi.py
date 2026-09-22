"""Valida los contratos OpenAPI documentales de AcadTrace."""

from pathlib import Path

import yaml

ROOT = Path(__file__).parent
CONTRACT = ROOT / "openapi.yaml"
REQUIRED_PATHS = {
    "principal": {"/api/estudiantes", "/api/matriculas", "/api/auditoria", "/actuator/health"},
    "secretaria": {"/api/secretario/estudiantes", "/api/secretario/matriculas", "/api/secretario/calendario/eventos"},
    "docente": {"/api/docente/actividades/", "/api/docente/asistencias/masivo", "/metrics"},
    "soporte": {
        "/health",
        "/api/soporte/election/status",
        "/api/soporte/logs",
        "/api/soporte/tecnicos-list",
        "/api/soporte/tecnicos",
        "/api/soporte/usuarios",
        "/api/soporte/tickets",
        "/api/soporte/tickets/mis-tickets",
        "/api/soporte/tickets/estadisticas",
        "/api/soporte/tickets/reportes",
        "/api/soporte/tickets/{id}",
        "/api/soporte/tickets/{id}/escalar",
        "/api/soporte/tickets/{id}/historial",
        "/api/soporte/tickets/{id}/comentarios",
    },
    "ia": {"/health", "/api/ia/diagnostico-estudiante", "/api/ia/asistente", "/openapi.json", "/docs", "/redoc"},
}


def resolve_local_refs(document: dict) -> int:
    count = 0

    def visit(value):
        nonlocal count
        if isinstance(value, dict):
            if "$ref" in value:
                ref = value["$ref"]
                if not ref.startswith("#/"):
                    raise ValueError(f"Referencia no local o no soportada: {ref}")
                node = document
                for part in ref[2:].split("/"):
                    node = node[part]
                count += 1
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(document)
    return count


def main() -> None:
    documents = sorted(ROOT.glob("*.yaml")) + sorted(ROOT.glob("*.yml")) + sorted(ROOT.glob("*.json"))
    if not documents:
        raise SystemExit("No hay contratos OpenAPI en docs/api")

    for path in documents:
        document = yaml.safe_load(path.read_text(encoding="utf-8"))
        if not isinstance(document, dict) or not str(document.get("openapi", "")).startswith("3."):
            raise SystemExit(f"No es OpenAPI 3.x: {path}")
        refs = resolve_local_refs(document)
        paths = set(document.get("paths", {}))
        if "/api/secretaria" in "\n".join(paths):
            raise SystemExit(f"Prefijo obsoleto /api/secretaria encontrado en {path}")
        print(f"OK {path}: {len(paths)} paths, {refs} refs locales")

        if path.name == CONTRACT.name:
            for service, required in REQUIRED_PATHS.items():
                missing = sorted(required - paths)
                if missing:
                    raise SystemExit(f"Faltan rutas de {service}: {', '.join(missing)}")
            print("OK rutas críticas de principal, secretaria, docente, soporte e ia")


if __name__ == "__main__":
    main()
