"""Deriva el conjunto A y consulta los agregados historicos B y E.

Solo --generate-latex escribe un archivo; --emit-latex-block imprime macros.

--check-latex valida las publicaciones oficiales delimitadas en Markdown,
las macros y los párrafos oficiales del manuscrito; no es un parser general
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
    "peticiones": [NUM + r"\s*(?:peticiones|reqs\b|requests\b)"],
    "fallos": [NUM + r"\s*(?:fallos|failures)\b"],
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
                    or "official local nominal Support run" in raw
                    or any("\\" + macro in raw for macro in MACROS.values())
                    or (in_official_section and "El perfil configura" in raw))
        if not selected or 'pruebas-carga/image.png' in raw:
            continue
        line = raw.split("Nominal Esc-1:", 1)[-1].split("Estrés Esc-2:", 1)[0]
        if "Nominal oficial:" in line:
            # Solo la celda de resultados agregados, no recomendaciones por endpoint.
            line = line.split("Nominal oficial:", 1)[1].split("&", 1)[0]
        # Expandir solo las macros de carga desde A, nunca desde literales alternativos.
        for key, macro in MACROS.items():
            line = re.sub(r"\\" + macro + r"\b(?:\{\})?",
                          format(metrics[key], 'f'), line)
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


# Secciones oficiales explícitas; los inventarios B-F, protocolos históricos,
# capturas descritas y resultados experimentales no son cifras oficiales de A.
PUBLICATIONS = {
    'README.md': ('## Resultados oficiales de carga', '**Corrida oficial de estrés:'),
    'docs/locust/README.md': ('## 1.', '## 2.'),
    'docs/locust/entorno_medicion.md': ('> CSV oficial:', '> **Estrés oficial:'),
    'experimentos/protocolo-e4.md': ('## 5. Resultados de carga E5', '**Corrida oficial de estrés:'),
    'docs/experimentos/protocolo-e4.md': ('## 5. Resultados de carga E5', '**Corrida oficial de estrés:'),
    'experimentos/resultados/corridas-e5.md': ('## Artefactos y métricas oficiales de A', '## Evidencia visual'),
}
TABLE_FIELDS = {
    'Peticiones': ('peticiones',), 'Fallos': ('fallos',),
    'Peticiones / fallos': ('peticiones', 'fallos'),
    'RPS': ('rps',), 'Promedio': ('media_ms',),
    'P50': ('p50_ms',), 'P95': ('p95_ms',), 'P99': ('p99_ms',),
    'P50 / P95 / P99': ('p50_ms', 'p95_ms', 'p99_ms'),
    'Máximo': ('maximo_ms',),
    'Request Count': ('peticiones',), 'Failure Count': ('fallos',),
    'Requests/s': ('rps',), 'Average Response Time': ('media_ms',),
    'Median Response Time / 50%': ('p50_ms',), '95%': ('p95_ms',),
    '99%': ('p99_ms',), 'Max Response Time': ('maximo_ms',),
    'Peticiones Totales': ('peticiones',), 'Fallos Totales': ('fallos',),
    'Throughput Promedio': ('rps',), 'Latencia Promedio': ('media_ms',),
    'Percentil 50 (P50 / Mediana)': ('p50_ms',),
    'Percentil 95 (P95)': ('p95_ms',), 'Percentil 99 (P99)': ('p99_ms',),
    'Latencia Máxima': ('maximo_ms',),
}


def check_publication(path, text, metrics):
    start, end = PUBLICATIONS[path]
    if start not in text or end not in text.split(start, 1)[1]:
        return [f'{path}: falta delimitador de la publicación oficial']
    block = text.split(start, 1)[1].split(end, 1)[0]
    errors, seen = [], set()

    def check(key, token):
        seen.add(key)
        if not matches(token, metrics[key], key in ('peticiones', 'fallos', 'usuarios_maximos')):
            errors.append(f'{path}: {key}={token}; CSV A={metrics[key]}')

    for raw in block.splitlines():
        line = raw.replace('**', '').strip().lstrip('> ').lstrip('- ')
        if line.startswith('|'):
            cells = [cell.strip() for cell in line.strip('|').split('|')]
            label, value = cells[:2] if len(cells) >= 2 else ('', '')
        else:
            label, _, value = line.partition(':')
        if label in TABLE_FIELDS:
            keys = TABLE_FIELDS[label]
            tokens = re.findall(NUM, value)
            if len(tokens) < len(keys):
                errors.append(f'{path}: valor ausente para {label}')
            for key, token in zip(keys, tokens):
                check(key, token)
        for key, patterns in PATTERNS.items():
            for pattern in patterns:
                for match in re.finditer(pattern, line, re.IGNORECASE):
                    check(key, match.group(1))
        # Resumen oficial de entorno_medicion; los bloques historicos quedan fuera.
        for pattern, key in [(r'P50\s+' + NUM, 'p50_ms'),
                             (r'P95\s+' + NUM, 'p95_ms'),
                             (r'P99\s+' + NUM, 'p99_ms'),
                             (r'máximo\s+' + NUM, 'maximo_ms')]:
            for match in re.finditer(pattern, line, re.IGNORECASE):
                check(key, match.group(1))
    required = set(FIELDS) | {'maximo_ms'}
    if path != 'experimentos/resultados/corridas-e5.md':
        required.add('usuarios_maximos')
    errors.extend(f'{path}: falta métrica oficial verificable {key}' for key in sorted(required - seen))
    return errors


STRESS_DIR = ROOT / "microservicio-soporte/resultados_estres/20260920_164722"


def derive_stress():
    stats_path = STRESS_DIR / "locust_estres_200_stats.csv"
    history_path = STRESS_DIR / "locust_estres_200_stats_history.csv"
    if not stats_path.exists():
        return None
    metrics = read_aggregate(stats_path)
    if history_path.exists():
        with history_path.open(encoding="utf-8-sig", newline="") as stream:
            reader = csv.DictReader(stream)
            samples = [r for r in reader if r.get("Name", "").strip() == "Aggregated"]
            if samples:
                metrics["usuarios_maximos"] = max(count(r["User Count"]) for r in samples)
    return metrics


def check_stress_consistency(stress_metrics, text, path_name):
    if not stress_metrics:
        return []
    errors = []
    # Buscar el bloque específico de la corrida oficial de estrés vigente
    official_marker = None
    for marker in ["Corrida oficial de estrés:", "## Corrida oficial de estrés vigente", "La corrida oficial de estrés en"]:
        if marker in text:
            official_marker = marker
            break
    if not official_marker:
        return []
    
    # Extraer el párrafo o sección correspondiente a la corrida oficial de estrés
    block = text.split(official_marker, 1)[1][:1000]
    
    # Peticiones: debe contener 98.684 o 98684 o 98{,}684
    m_pet = re.search(r"(\d+(?:[.,{}]*\d+)?)\s*peticiones", block)
    if m_pet:
        clean_num = m_pet.group(1).replace(".", "").replace(",", "").replace("{", "").replace("}", "")
        if clean_num.isdigit() and int(clean_num) != int(stress_metrics["peticiones"]):
            errors.append(f"{path_name}: peticiones oficiales de estrés {clean_num} != {stress_metrics['peticiones']}")
    
    # Fallos: debe ser 0 fallos
    m_fal = re.search(r"(\d+)\s*fallos", block)
    if m_fal:
        if int(m_fal.group(1)) != int(stress_metrics["fallos"]):
            errors.append(f"{path_name}: fallos oficiales de estrés {m_fal.group(1)} != {stress_metrics['fallos']}")
            
    # Percentiles P95 y P99
    m_p95 = re.search(r"P_?95\s*=\s*(\d+)", block)
    if m_p95 and int(m_p95.group(1)) != int(stress_metrics["p95_ms"]):
        errors.append(f"{path_name}: P95 oficial de estrés {m_p95.group(1)} != {stress_metrics['p95_ms']}")
        
    m_p99 = re.search(r"P_?99\s*=\s*(\d+)", block)
    if m_p99 and int(m_p99.group(1)) != int(stress_metrics["p99_ms"]):
        errors.append(f"{path_name}: P99 oficial de estrés {m_p99.group(1)} != {stress_metrics['p99_ms']}")
        
    return errors


def check_declared_hashes():
    import hashlib
    errors = []
    hash_docs = ['docs/locust/README.md', 'docs/locust/entorno_medicion.md']
    for rel_doc in hash_docs:
        doc_path = ROOT / rel_doc
        if not doc_path.exists():
            continue
        text = doc_path.read_text(encoding='utf-8')
        for line_no, line in enumerate(text.splitlines(), 1):
            if '|' in line:
                cells = [c.strip() for c in line.split('|')]
                for i in range(len(cells) - 1):
                    cell_file = cells[i].replace('`', '').replace('*', '').strip()
                    cell_hash = cells[i+1].replace('`', '').replace('*', '').strip()
                    if cell_file.startswith('microservicio-soporte/') and re.fullmatch(r'[A-Fa-f0-9]{64}', cell_hash):
                        target = ROOT / cell_file
                        if not target.exists():
                            errors.append(f"{rel_doc}:{line_no}: archivo {cell_file} no existe")
                        else:
                            real_hash = hashlib.sha256(target.read_bytes()).hexdigest().upper()
                            if real_hash != cell_hash.upper():
                                errors.append(f"{rel_doc}:{line_no}: hash de {cell_file} mismatch (declarado={cell_hash}, real={real_hash})")
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
        stress_metrics = derive_stress()
        if args.emit_latex_block:
            print(render(metrics), end="")
            return 0
        historical = derive_historical()
        if args.generate_latex:
            GENERATED.write_text(render(metrics), encoding="utf-8")
        if args.json:
            print(json.dumps({"metrics": metrics, "auxiliary": auxiliary,
                              "additional": extra, "endpoints": endpoints,
                              "history": window, "historical": historical,
                              "stress": stress_metrics},
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
            if stress_metrics:
                print("Conjunto Oficial de Estrés: microservicio-soporte/resultados_estres/20260920_164722")
                for key, value in stress_metrics.items():
                    print(f"  {key}: {value}")
        if args.check_latex:
            manuscript_text = MANUSCRIPT.read_text(encoding="utf-8")
            errors = check_latex(metrics, manuscript_text)
            errors.extend(check_generated_latex(metrics))
            errors.extend(check_declared_hashes())
            errors.extend(check_stress_consistency(stress_metrics, manuscript_text, "Informe-E4_BCEL/TA-PFC-E4_BCEL.tex"))
            for path in PUBLICATIONS:
                pub_text = (ROOT / path).read_text(encoding='utf-8')
                errors.extend(check_publication(path, pub_text, {**metrics, **extra}))
                errors.extend(check_stress_consistency(stress_metrics, pub_text, path))
            for error in errors:
                print("ERROR: " + error, file=sys.stderr)
            if errors:
                return 1
            print("OK: métricas oficiales explícitas coinciden con los CSV y hashes verificados")
        return 0
    except (OSError, ValueError, InvalidOperation, KeyError, TypeError, AttributeError, OverflowError, csv.Error) as exc:
        print(f"ERROR de entrada: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
