"""Compara las operaciones de Secretaria con su OpenAPI HTTP real.

Requiere PyYAML. No inicia servicios ni normaliza rutas o trailing slashes.
Salida: 0 coincidencia, 1 diferencias, 2 verificacion no realizable.
"""

import argparse
import json
import os
from pathlib import Path
import re
import sys
from urllib.error import URLError
from urllib.request import Request, urlopen


CONTRACT = Path(__file__).resolve().parents[1] / "docs/api/openapi.yaml"
TIMEOUT_SECONDS = 10
METHODS = {"get", "post", "put", "patch", "delete", "head", "options", "trace"}

SERVICES_CONFIG = {
    "secretaria": {
        "tag": "secretaria",
        "runtime_url": os.environ.get("OPENAPI_SECRETARIA_URL", "http://localhost:5176/v3/api-docs"),
        "namespace": "/api/secretario",
        "name": "Secretaria",
        # Rutas que el servicio sirve de verdad pero que springdoc nunca
        # documenta en /v3/api-docs (Actuator y health-checks simples).
        # Se verifican con una peticion HTTP directa, no por comparacion
        # de esquema.
        "out_of_scope": set(),
    },
    "soporte": {
        "tag": "soporte",
        "runtime_url": os.environ.get("OPENAPI_SOPORTE_URL", "http://localhost:8083/v3/api-docs"),
        "namespace": "/api/soporte",
        "name": "Soporte",
        "out_of_scope": {("GET", "/health")},
    },
    "principal": {
        "tag": "principal",
        "runtime_url": os.environ.get("OPENAPI_PRINCIPAL_URL", "http://localhost:8080/v3/api-docs"),
        "namespace": "/api",
        "name": "Principal",
        "out_of_scope": {("GET", "/actuator/health")},
    },
}


class VerificationError(Exception):
    """Documento o seleccion que no permite una comparacion fiable."""


def in_namespace(path, namespace="/api/secretario"):
    return path == namespace or path.startswith(namespace + "/")


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


def fetch_runtime(runtime_url, service_name="Secretaria"):
    request = Request(runtime_url, headers={"Accept": "application/json"})
    try:
        with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            if response.geturl() != runtime_url:
                raise VerificationError(f"El endpoint runtime de {service_name} redirigio a otra URL.")
            if not 200 <= response.status < 300:
                raise VerificationError(f"HTTP runtime no exitoso ({response.status}) para {service_name}.")
            raw = response.read()
    except (URLError, TimeoutError, OSError) as exc:
        raise VerificationError(
            f"No se pudo obtener el contrato HTTP de {service_name} en {runtime_url} "
            "(conexion, timeout o estado HTTP no exitoso)."
        ) from exc
    try:
        return json.loads(raw, object_pairs_hook=unique_json,
                          parse_constant=reject_constant)
    except (ValueError, UnicodeError) as exc:
        raise VerificationError(f"La respuesta runtime de {service_name} no es JSON valido.") from exc


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


def select_versioned(all_operations, tag="secretaria", namespace="/api/secretario", name="Secretaria"):
    selected = set()
    primary_servers = set()
    for pair, (tags, servers) in all_operations.items():
        if in_namespace(pair[1], namespace):
            if tag == "secretaria" and tags != ["secretaria"]:
                raise VerificationError("Metadata contradictoria bajo /api/secretario.")
            primary_servers.update(servers)
        if tag in tags:
            selected.add(pair)
    if not selected:
        raise VerificationError(f"Seleccion de {name} vacia.")
    shared_paths = set()
    for pair in selected:
        tags, servers = all_operations[pair]
        if not in_namespace(pair[1], namespace):
            if tag == "secretaria" and (len(set(tags)) < 2 or not servers.intersection(primary_servers)):
                raise VerificationError("Metadata contradictoria en ruta compartida.")
            shared_paths.add(pair[1])
    return selected, shared_paths


def show_difference(label, operations_set):
    print(label)
    if not operations_set:
        print("  Ninguna")
    for method, path in sorted(operations_set):
        print(f"  {method} {path}")


def verify_out_of_scope(method, path, runtime_url):
    base = re.sub(r"/v3/api-docs$", "", runtime_url)
    url = base + path
    request = Request(url, method=method)
    try:
        with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return 200 <= response.status < 300
    except (URLError, TimeoutError, OSError):
        return False


def verificar_un_servicio(contract_doc, service_key, custom_url=None):
    cfg = SERVICES_CONFIG[service_key]
    name = cfg["name"]
    tag = cfg["tag"]
    namespace = cfg["namespace"]
    runtime_url = custom_url or cfg["runtime_url"]
    out_of_scope = cfg.get("out_of_scope", set())

    versioned, shared = select_versioned(contract_doc, tag=tag, namespace=namespace, name=name)
    versioned_in_scope = versioned - out_of_scope
    print(f"Contrato versionado {name}: {len(versioned)} operaciones "
          f"({len(out_of_scope & versioned)} fuera del alcance de /v3/api-docs)")
    runtime_all = operations(fetch_runtime(runtime_url, service_name=name))
    runtime = {pair for pair in runtime_all
               if in_namespace(pair[1], namespace) or pair[1] in shared or pair in versioned_in_scope}
    if not runtime:
        raise VerificationError(f"Seleccion runtime de {name} vacia.")
    print(f"Contrato runtime {name}: {len(runtime)} operaciones")
    show_difference(f"Runtime fuera del alcance de {name}:", set(runtime_all) - runtime)
    show_difference("Solo en versionado:", versioned_in_scope - runtime)
    show_difference("Solo en runtime:", runtime - versioned_in_scope)
    ok = versioned_in_scope == runtime

    for method, path in sorted(out_of_scope & versioned):
        healthy = verify_out_of_scope(method, path, runtime_url)
        estado = "OK" if healthy else "FALLA"
        print(f"Verificacion directa (fuera de /v3/api-docs) {method} {path}: {estado}")
        ok = ok and healthy

    if not ok:
        return 1
    print(f"Coinciden las operaciones HTTP de {name}.")
    return 0


def main():
    parser = argparse.ArgumentParser(description="Verificador de contratos OpenAPI runtime vs versionado.")
    parser.add_argument(
        "--service",
        choices=["secretaria", "soporte", "principal", "all"],
        default="secretaria",
        help="Microservicio a verificar (por defecto: secretaria).",
    )
    parser.add_argument(
        "--url",
        default=None,
        help="URL alternativa del endpoint runtime /v3/api-docs.",
    )
    args = parser.parse_args()

    try:
        contract_ops = operations(load_contract())
        if args.service == "all":
            exit_codes = []
            for s_key in ["secretaria", "soporte", "principal"]:
                print(f"\n=== Verificando {SERVICES_CONFIG[s_key]['name']} ===")
                try:
                    rc = verificar_un_servicio(contract_ops, s_key, custom_url=args.url)
                    exit_codes.append(rc)
                except Exception as exc:
                    # Un servicio inalcanzable es una verificacion fallida,
                    # no una advertencia: antes se perdia silenciosamente y
                    # --service all podia devolver 0 con un servicio caido.
                    print(f"ERROR: {SERVICES_CONFIG[s_key]['name']} no verificable: {exc}", file=sys.stderr)
                    exit_codes.append(2)
            if any(c == 2 for c in exit_codes):
                return 2
            return 1 if any(c == 1 for c in exit_codes) else 0
        else:
            return verificar_un_servicio(contract_ops, args.service, custom_url=args.url)
    except Exception as exc:
        message = str(exc) if isinstance(exc, VerificationError) else (
            "Error de infraestructura/verificacion: " + type(exc).__name__
            + ". Compruebe el archivo y la disponibilidad de PyYAML."
        )
        print("ERROR: " + message, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
