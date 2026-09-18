"""Deriva el conjunto A y consulta los agregados historicos B y E.

Solo --generate-latex escribe un archivo; --emit-latex-block imprime macros.

--check-latex valida las afirmaciones numéricas explícitas de los párrafos
oficiales y filas nominales del manuscrito actual; no es un parser general
de LaTeX ni certifica resultados históricos o causas de fallos.
Salidas: 0 correcto, 1 discrepancias, 2 entrada inválida.
"""
import argparse
import csv
import json
import re
import sys
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREFIX = ROOT / "microservicio-soporte/locust_esc1"
HISTORICAL_STATS = {
    "B": ROOT / "experimentos/resultados/locust_esc1_stats.csv",
    "E": ROOT / "experimentos/resultados/locust_esc3_stats.csv",
}
MANUSCRIPT = ROOT / "Informe-E4_BCEL/TA-PFC-E4_BCEL.tex"
GENERATED = ROOT / "Informe-E4_BCEL/cifras_carga_generadas.tex"
FIELDS = {
    "peticiones": "Request Count", "fallos": "Failure Count",
    "rps": "Requests/s", "media_ms": "Average Response Time",
    "p50_ms": "50%", "p95_ms": "95%", "p99_ms": "99%",
}
MACROS = dict(zip((*FIELDS, "usuarios_maximos"), (
    "CargaPeticiones", "CargaFallos", "CargaRPS", "CargaMediaMs",
    "CargaPcinquentaMs", "CargaPnoventaCincoMs", "CargaPnoventaNueveMs",
    "CargaUsuariosMaximos")))


def read_csv(suffix, required):
    path = Path(str(PREFIX) + suffix)
    with path.open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        missing = set(required) - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"{path.name}: faltan columnas {sorted(missing)}")
        return list(reader)


def read_aggregate(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        missing = {"Name", *FIELDS.values()} - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"{path.name}: faltan columnas {sorted(missing)}")
        aggregate = [row for row in reader if row["Name"].strip() == "Aggregated"]
    if len(aggregate) != 1:
        raise ValueError(f"{path.name}: se requiere exactamente una fila Aggregated")
    return statistics(aggregate[0])[0]


def number(value):
    result = Decimal(value)
    if not result.is_finite() or result < 0:
        raise ValueError(f"Número no válido: {value!r}")
    return result


def count(value):
    result = number(value)
    if result != result.to_integral_value():
        raise ValueError(f"Conteo no entero: {value!r}")
    return result


def statistics(row):
    result = {key: number(row[column]) for key, column in FIELDS.items()}
    for key in ("peticiones", "fallos"):
        result[key] = count(result[key])
    if result["fallos"] > result["peticiones"]:
        raise ValueError("Failure Count supera Request Count")
    optional = {"Median Response Time": "mediana_ms",
                "Min Response Time": "minimo_ms", "Max Response Time": "maximo_ms"}
    optional.update({column: "percentil_" + column[:-1] + "_ms"
                     for column in row if re.fullmatch(r"[0-9]+(?:\.[0-9]+)?%", column)
                     and column not in FIELDS.values()})
    extra = {key: number(row[column]) for column, key in optional.items() if column in row}
    return result, extra


def derive():
    rows = read_csv("_stats.csv", ["Type", "Name", *FIELDS.values()])
    aggregate = [r for r in rows if r["Name"].strip() == "Aggregated"]
    if len(aggregate) != 1:
        raise ValueError("Se requiere exactamente una fila Aggregated en stats")
    metrics, extra = statistics(aggregate[0])
    endpoints = []
    for row in rows:
        if row["Name"].strip() == "Aggregated":
            continue
        if not row["Name"].strip() or not row["Type"].strip():
            raise ValueError("Endpoint sin Name o Type")
        values, optional = statistics(row)
        endpoints.append({"metodo": row["Type"], "endpoint": row["Name"],
                          "metrics": values, "additional": optional})
    history = read_csv("_stats_history.csv", ["Name", "User Count", "Timestamp"])
    samples = [r for r in history if r["Name"].strip() == "Aggregated"]
    if not samples:
        raise ValueError("History no contiene muestras Aggregated")
    metrics["usuarios_maximos"] = max(count(r["User Count"]) for r in samples)
    timestamps = [int(count(r["Timestamp"])) for r in samples]
    first, last = min(timestamps), max(timestamps)
    window = {"inicio_utc": datetime.fromtimestamp(first, timezone.utc).isoformat(),
              "fin_utc": datetime.fromtimestamp(last, timezone.utc).isoformat(),
              "duracion_observada_s": last - first, "muestras": len(samples)}
    failures = read_csv("_failures.csv", ["Occurrences"])
    exceptions = read_csv("_exceptions.csv", ["Count"])
    auxiliary = {"ocurrencias_fallos": sum((count(r["Occurrences"]) for r in failures), Decimal(0)),
                 "excepciones": sum((count(r["Count"]) for r in exceptions), Decimal(0))}
    return metrics, auxiliary, extra, endpoints, window


def derive_historical():
    return {name: read_aggregate(path) for name, path in HISTORICAL_STATS.items()}


def render(metrics):
    return "% Generado desde el conjunto A por scripts/recalcular_metricas_carga.py\n" + "".join(
        f"\\newcommand{{\\{MACROS[key]}}}{{{format(value, 'f')}}}\n"
        for key, value in metrics.items())


NUM = r"(?<![\d.,])([0-9]+(?:[.,][0-9]+)*)"
PATTERNS = {
    "peticiones": [NUM + r"\s*(?:peticiones|reqs\b)"],
    "fallos": [NUM + r"\s*fallos\b"],
    "rps": [NUM + r"\s*(?:req/s|RPS)"],
    "media_ms": [r"(?:promedio|media)\s*" + NUM],
    "p50_ms": [r"P_?50\s*=\s*" + NUM],
    "p95_ms": [r"P_?95\s*=\s*" + NUM],
    "p99_ms": [r"P_?99\s*=\s*" + NUM],
    "usuarios_maximos": [NUM + r"\s*usuarios\s*(?:virtuales|máximos|concurrentes)"],
}


def matches(token, expected, integer):
    # Decimales punto/coma; agrupación de miles solo con grupos de tres.
    candidates = []
    if re.fullmatch(r"\d{1,3}(?:[.,]\d{3})+", token):
        candidates.append((Decimal(re.sub(r"[.,]", "", token)), 0))
    if token.count(".") + token.count(",") <= 1:
        normalized = token.replace(",", ".")
        places = len(normalized.split(".")[1]) if "." in normalized else 0
        candidates.append((Decimal(normalized), places))
    elif "." in token and "," in token:
        separator = "." if token.rfind(".") > token.rfind(",") else ","
        whole, fraction = token.rsplit(separator, 1)
        grouping = "," if separator == "." else "."
        if re.fullmatch(r"\d{1,3}(?:" + re.escape(grouping) + r"\d{3})+", whole):
            candidates.append((Decimal(whole.replace(grouping, "") + "." + fraction), len(fraction)))
    return any(value == (expected if integer else expected.quantize(
        Decimal(1).scaleb(-places), rounding=ROUND_HALF_UP)) for value, places in candidates)


def check_latex(metrics, text):
    errors, seen = [], set()
    in_official_section = False
    for line_no, raw in enumerate(text.splitlines(), 1):
        if r"\subsection" in raw:
            in_official_section = "Conjunto oficial de carga" in raw
        # Revisar métricas oficiales; excluir discusiones históricas y endpoints.
        selected = ("OFICIAL NOMINAL" in raw or "Nominal oficial:" in raw
                    or "Nominal Esc-1:" in raw
                    or (in_official_section and "El perfil configura" in raw))
        if not selected:
            continue
        line = raw.split("Nominal Esc-1:", 1)[-1].split("Estrés Esc-2:", 1)[0]
        if "Nominal oficial:" in line:
            # Solo la celda de resultados agregados, no recomendaciones por endpoint.
            line = line.split("Nominal oficial:", 1)[1].split("&", 1)[0]
        line = line.replace(r"\,", " ").replace(r"\;", " ")
        line = re.sub(r"\\[A-Za-z]+", "", line)
        line = re.sub(r"[{}$]", "", line)
        for key, patterns in PATTERNS.items():
            for pattern in patterns:
                for match in re.finditer(pattern, line, re.IGNORECASE):
                    seen.add(key)
                    token = match.group(1)
                    if not matches(token, metrics[key], key in ("peticiones", "fallos", "usuarios_maximos")):
                        errors.append(f"Línea {line_no}: {key}={token}; CSV={metrics[key]}")
    errors.extend(f"No se encontró una afirmación oficial verificable para {key}" for key in metrics if key not in seen)
    return errors


def check_generated_latex(metrics):
    actual = GENERATED.read_text(encoding="utf-8")
    if actual != render(metrics):
        return [f"{GENERATED.relative_to(ROOT)} no coincide con las macros derivadas del CSV A"]
    return []


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--generate-latex", action="store_true")
    parser.add_argument("--check-latex", action="store_true")
    parser.add_argument("--emit-latex-block", action="store_true")
    args = parser.parse_args()
    try:
        metrics, auxiliary, extra, endpoints, window = derive()
        if args.emit_latex_block:
            print(render(metrics), end="")
            return 0
        historical = derive_historical()
        if args.generate_latex:
            GENERATED.write_text(render(metrics), encoding="utf-8")
        if args.json:
            print(json.dumps({"metrics": metrics, "auxiliary": auxiliary,
                              "additional": extra, "endpoints": endpoints,
                              "history": window, "historical": historical},
                             default=str, ensure_ascii=False, indent=2))
        else:
            print("Conjunto A: microservicio-soporte/locust_esc1_stats.csv")
            for key, value in {**metrics, **auxiliary, **extra}.items():
                print(f"{key}: {value}")
            print("Historial (muestras Aggregated; duracion entre muestras):")
            for key, value in window.items():
                print(f"  {key}: {value}")
            for endpoint in endpoints:
                print(f"Endpoint: {endpoint['metodo']} {endpoint['endpoint']}")
                for key, value in {**endpoint['metrics'], **endpoint['additional']}.items():
                    print(f"  {key}: {value}")
            for name, values in historical.items():
                print(f"Conjunto {name}: {HISTORICAL_STATS[name].relative_to(ROOT)}")
                for key, value in values.items():
                    print(f"  {key}: {value}")
        if args.check_latex:
            errors = check_latex(metrics, MANUSCRIPT.read_text(encoding="utf-8"))
            errors.extend(check_generated_latex(metrics))
            for error in errors:
                print("ERROR: " + error, file=sys.stderr)
            if errors:
                return 1
            print("OK: métricas oficiales explícitas coinciden con los CSV")
        return 0
    except (OSError, ValueError, InvalidOperation, KeyError, TypeError, AttributeError, OverflowError, csv.Error) as exc:
        print(f"ERROR de entrada: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
