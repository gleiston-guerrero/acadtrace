"""Compara las operaciones de Secretaria con su OpenAPI HTTP real.

Requiere PyYAML. No inicia servicios ni normaliza rutas o trailing slashes.
Salida: 0 coincidencia, 1 diferencias, 2 verificacion no realizable.
"""

import json
from pathlib import Path
import re
import sys
from urllib.error import URLError
from urllib.request import Request, urlopen


CONTRACT = Path(__file__).resolve().parents[1] / "docs/api/openapi.yaml"
RUNTIME_URL = "http://localhost:5176/v3/api-docs"
TIMEOUT_SECONDS = 10
METHODS = {"get", "post", "put", "patch", "delete", "head", "options", "trace"}


class VerificationError(Exception):
    """Documento o seleccion que no permite una comparacion fiable."""


def in_namespace(path):
    return path == "/api/secretario" or path.startswith("/api/secretario/")


def unique_json(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise VerificationError("JSON con claves duplicadas.")
        result[key] = value
    return result


def load_contract():
    import yaml

    class UniqueLoader(yaml.SafeLoader):
        pass

    def mapping(loader, node, deep=False):
        loader.flatten_mapping(node)
        pairs = [(loader.construct_object(k, deep=deep),
                  loader.construct_object(v, deep=deep)) for k, v in node.value]
        result = {}
        for key, value in pairs:
            if key in result:
                raise VerificationError("YAML con claves duplicadas.")
            result[key] = value
        return result

    UniqueLoader.add_constructor(
        yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, mapping
    )
    try:
        return yaml.load(CONTRACT.read_text(encoding="utf-8"), Loader=UniqueLoader)
    except yaml.YAMLError as exc:
        raise VerificationError("YAML invalido.") from exc


def fetch_runtime():
    request = Request(RUNTIME_URL, headers={"Accept": "application/json"})
    try:
        with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            if response.geturl() != RUNTIME_URL:
                raise VerificationError("El endpoint runtime redirigio a otra URL.")
            if not 200 <= response.status < 300:
                raise VerificationError("HTTP runtime no exitoso.")
            raw = response.read()
    except (URLError, TimeoutError, OSError) as exc:
        raise VerificationError(
            "No se pudo obtener el contrato HTTP de Secretaria "
            "(conexion, timeout o estado HTTP no exitoso)."
        ) from exc
    try:
        return json.loads(raw, object_pairs_hook=unique_json,
                          parse_constant=reject_constant)
    except (ValueError, UnicodeError) as exc:
        raise VerificationError("La respuesta runtime no es JSON valido.") from exc


def reject_constant(value):
    raise ValueError("Constante JSON no permitida")


def resolve(document, value):
    seen = set()
    while isinstance(value, dict) and "$ref" in value:
        reference = value["$ref"]
        if not isinstance(reference, str) or not reference.startswith("#/"):
            raise VerificationError("Referencia externa no soportada para operaciones.")
        if reference in seen:
            raise VerificationError("Referencia circular en una operacion.")
        seen.add(reference)
        try:
            value = document
            for part in reference[2:].split("/"):
                value = value[part.replace("~1", "/").replace("~0", "~")]
        except (KeyError, TypeError) as exc:
            raise VerificationError("Referencia OpenAPI no resuelta.") from exc
    return value


def operations(document):
    # Validacion estructural necesaria para esta comparacion; no sustituye
    # un validador completo de schemas OpenAPI.
    if not isinstance(document, dict):
        raise VerificationError("OpenAPI debe ser un objeto.")
    version = document.get("openapi")
    if not isinstance(version, str) or not re.fullmatch(r"3\.(0|1)\.\d+", version):
        raise VerificationError("Version OpenAPI ausente o no soportada (3.0/3.1).")
    info = document.get("info")
    if not isinstance(info, dict) or not all(
        isinstance(info.get(k), str) and info[k] for k in ("title", "version")
    ):
        raise VerificationError("OpenAPI sin info.title/info.version validos.")
    paths = document.get("paths")
    if not isinstance(paths, dict):
        raise VerificationError("OpenAPI sin objeto paths valido.")
    result = {}
    for path, raw_item in paths.items():
        if isinstance(path, str) and path.startswith("x-"):
            continue
        if not isinstance(path, str) or not path.startswith("/"):
            raise VerificationError("Path OpenAPI invalido.")
        item = resolve(document, raw_item)
        if not isinstance(item, dict):
            raise VerificationError("Path Item invalido.")
        for method, operation in item.items():
            if method not in METHODS:
                continue
            if not isinstance(operation, dict):
                raise VerificationError("Operacion OpenAPI invalida.")
            responses = operation.get("responses")
            if not isinstance(responses, dict) or not responses:
                raise VerificationError("Operacion sin responses validas.")
            for status, response in responses.items():
                if isinstance(status, str) and status.startswith("x-"):
                    continue
                if not isinstance(status, str) or not re.fullmatch(
                    r"default|[1-5](?:\d{2}|XX)", status
                ):
                    raise VerificationError("Codigo de respuesta OpenAPI invalido.")
                response = resolve(document, response)
                if not isinstance(response, dict) or not isinstance(
                    response.get("description"), str
                ):
                    raise VerificationError("Respuesta OpenAPI sin descripcion.")
            tags = operation.get("tags", [])
            if not isinstance(tags, list) or not all(isinstance(t, str) for t in tags):
                raise VerificationError("Tags invalidos.")
            servers = operation.get("servers", item.get("servers", document.get("servers", [])))
            if not isinstance(servers, list) or not all(
                isinstance(s, dict) and isinstance(s.get("url"), str) for s in servers
            ):
                raise VerificationError("Servers invalidos.")
            result[(method.upper(), path)] = (tags, {s["url"] for s in servers})
    return result


def select_versioned(all_operations):
    selected = set()
    primary_servers = set()
    for pair, (tags, servers) in all_operations.items():
        if in_namespace(pair[1]):
            if tags != ["secretaria"]:
                raise VerificationError("Metadata contradictoria bajo /api/secretario.")
            primary_servers.update(servers)
        if "secretaria" in tags:
            selected.add(pair)
    if not selected or not primary_servers:
        raise VerificationError("Seleccion de Secretaria vacia o sin servidores identificables.")
    shared_paths = set()
    for pair in selected:
        tags, servers = all_operations[pair]
        if not in_namespace(pair[1]):
            # Rutas compartidas: tag de Secretaria mas otro servicio y servidor
            # compartido con las operaciones de su namespace, no solo un puerto.
            if len(set(tags)) < 2 or not servers.intersection(primary_servers):
                raise VerificationError("Metadata contradictoria en ruta compartida.")
            shared_paths.add(pair[1])
    return selected, shared_paths


def show_difference(label, operations_set):
    print(label)
    if not operations_set:
        print("  Ninguna")
    for method, path in sorted(operations_set):
        print(f"  {method} {path}")


def main():
    try:
        versioned, shared = select_versioned(operations(load_contract()))
        print(f"Contrato versionado Secretaria: {len(versioned)} operaciones")
        runtime_all = operations(fetch_runtime())
        runtime = {pair for pair in runtime_all
                   if in_namespace(pair[1]) or pair[1] in shared}
        if not runtime:
            raise VerificationError("Seleccion runtime de Secretaria vacia.")
        print(f"Contrato runtime Secretaria: {len(runtime)} operaciones")
        show_difference("Runtime fuera del alcance de Secretaria:", set(runtime_all) - runtime)
        show_difference("Solo en versionado:", versioned - runtime)
        show_difference("Solo en runtime:", runtime - versioned)
        if versioned != runtime:
            return 1
        print("Coinciden las operaciones HTTP de Secretaria.")
        return 0
    except Exception as exc:
        # No volcar documentos, respuestas HTTP ni posibles datos sensibles.
        message = str(exc) if isinstance(exc, VerificationError) else (
            "Error de infraestructura/verificacion: " + type(exc).__name__
            + ". Compruebe el archivo y la disponibilidad de PyYAML."
        )
        print("ERROR: " + message, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
