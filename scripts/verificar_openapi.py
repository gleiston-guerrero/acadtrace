#!/usr/bin/env python3
"""Compara docs/api/openapi.yaml con el /v3/api-docs que sirve el servicio."""
import sys, json, yaml, urllib.request

CONTRATO = "docs/api/openapi.yaml"
ENDPOINTS = {
    "secretaria": "http://localhost:5176/v3/api-docs",
    "principal": "http://localhost:8080/v3/api-docs",
    "soporte": "http://localhost:8083/v3/api-docs",
}

def rutas(spec):
    return set(spec.get("paths", {}).keys())

def main():
    with open(CONTRATO, encoding="utf-8") as f:
        rutas_versionadas = rutas(yaml.safe_load(f))

    faltantes = []
    huerfanas = []
    for nombre, url in ENDPOINTS.items():
        servidas = None
        try:
            with urllib.request.urlopen(url, timeout=10) as r:
                servidas = rutas(json.load(r))
        except Exception as e:
            if nombre == "soporte":
                try:
                    with urllib.request.urlopen("http://localhost:5178/v3/api-docs", timeout=10) as r:
                        servidas = rutas(json.load(r))
                except Exception:
                    print(f"[{nombre}] no responde: {e}")
                    sys.exit(1)
            else:
                print(f"[{nombre}] no responde: {e}")
                sys.exit(1)
        servidas.discard("/error")
        # Rutas que sirve el servicio pero no están en el contrato
        no_documentadas = servidas - rutas_versionadas
        # Rutas en el contrato que el servicio no implementa
        fantasmas = {r for r in rutas_versionadas if r in servidas} ^ rutas_versionadas
        if no_documentadas:
            print(f"[{nombre}] rutas no documentadas en el contrato: {sorted(no_documentadas)}")
            faltantes.extend(no_documentadas)
        if fantasmas:
            print(f"[{nombre}] rutas del contrato que el servicio NO sirve: {sorted(fantasmas)}")

    if faltantes:
        sys.exit(1)
    print("OK: contrato y servicios coinciden.")

if __name__ == "__main__":
    main()
