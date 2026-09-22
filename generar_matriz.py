"""E22: preview por defecto. Solo --write-csv/--write-latex escriben archivos.
Las categorias solicitadas por el proyecto no certifican ISO/IEC 25010 completo.
La evidencia historica se distingue del conjunto nominal A. Solo biblioteca estandar.
"""
import argparse
import csv
import io
import json
import sys
import xml.etree.ElementTree as ET
from decimal import Decimal, InvalidOperation
from pathlib import Path

ROOT = Path(__file__).resolve().parent
STATS = Path('microservicio-soporte/locust_esc1_stats.csv')
HISTORY = Path('microservicio-soporte/locust_esc1_stats_history.csv')
FALSE_POSITIVES = Path('experimentos/resultados/falsos_positivos.csv')
HISTORICAL_WINDOWS = Path('experimentos/resultados/iso25010.csv')
CSV_OUTPUT = ROOT / 'docs/experimentos/resultados/matriz_iso25010.csv'
TEX_OUTPUT = ROOT / 'Informe-E4_BCEL/matriz_iso25010_generada.tex'
FIELDS = {'peticiones': 'Request Count', 'fallos': 'Failure Count',
          'rps': 'Requests/s', 'media_ms': 'Average Response Time',
          'p50_ms': '50%', 'p95_ms': '95%', 'p99_ms': '99%'}
COLUMNS = ('caracteristica', 'indicador', 'valor', 'fuente', 'alcance', 'criterio', 'veredicto')
# Criterio documental, nunca resultado esperado.
P99_LIMIT_MS = Decimal('500')
COVERAGE_LIMIT_PCT = Decimal('70')  # Criterio de esta matriz, no medicion.
COVERAGE_REPORTS = (
    ('Principal', Path('docs/cobertura/sga-principal/jacoco.xml'),
     'INSTRUCTION', Decimal('30')),
    ('Secretaría', Path('docs/cobertura/secretaria/jacoco.xml'),
     'LINE', Decimal('70')),
    ('Soporte', Path('docs/cobertura/soporte/jacoco.xml'),
     'LINE', Decimal('70')),
    ('Aplicación móvil', Path('docs/cobertura/movil/jacoco.xml'),
     'INSTRUCTION', Decimal('10')),
)


def wilson_interval(successes, total, z: float = 1.96):
    """Calcula el intervalo de confianza de Wilson (1927) para una proporcion binomial."""
    total_f = float(total)
    successes_f = float(successes)
    if total_f <= 0:
        return 0.0, 0.0, 0.0
    p = successes_f / total_f
    denom = 1 + (z ** 2) / total_f
    center = (p + (z ** 2) / (2 * total_f)) / denom
    margin = (z / denom) * ((p * (1 - p) / total_f + (z ** 2) / (4 * (total_f ** 2))) ** 0.5)
    lower = max(0.0, center - margin)
    upper = min(1.0, center + margin)
    return p, lower, upper


def format_ci_percent(lower: float, upper: float) -> str:
    low_pct = lower * 100
    high_pct = upper * 100
    if lower < 1.0 and f"{low_pct:.2f}" == "100.00":
        # El redondeo a 2 decimales lo confunde con 100%; se muestra con mas
        # precision en vez de sustituirlo por un valor fijo no derivado.
        low_str = f"{low_pct:.4f}%"
    else:
        low_str = f"{low_pct:.2f}%"
    high_str = f"{high_pct:.2f}%"
    return f"[{low_str}, {high_str}]"


def derive_coverage():
    results = []
    for module, relative, metric, minimum in COVERAGE_REPORTS:
        try:
            report = ET.parse(ROOT / relative).getroot()
        except ET.ParseError as exc:
            raise ValueError(f'{relative}: XML JaCoCo inválido') from exc

        counters = [
            c for c in report.findall('counter')
            if c.get('type') == metric
        ]

        if report.tag != 'report' or len(counters) != 1:
            raise ValueError(
                f'{relative}: se requiere un contador global '
                f'BUNDLE/{metric}'
            )

        covered = number(
            counters[0].get('covered'),
            f'{relative}: cubiertas',
            True,
        )
        missed = number(
            counters[0].get('missed'),
            f'{relative}: no cubiertas',
            True,
        )

        total = covered + missed

        if total == 0:
            raise ValueError(
                f'{relative}: contador {metric} sin elementos'
            )

        percent = covered * 100 / total

        results.append(
            (module, relative, metric, minimum, covered, total, percent)
        )

    return results


def read_rows(relative, required):
    with (ROOT / relative).open(encoding='utf-8-sig', newline='') as stream:
        reader = csv.DictReader(stream)
        headers = reader.fieldnames or []
        if len(headers) != len(set(headers)):
            raise ValueError(f'{relative}: columnas duplicadas')
        missing = set(required) - set(headers)
        if missing:
            raise ValueError(f'{relative}: faltan columnas {sorted(missing)}')
        rows = list(reader)
        if any(None in row or any(v is None for v in row.values()) for row in rows):
            raise ValueError(f'{relative}: fila incompleta o columnas sobrantes')
        return rows


def number(value, label, integer=False):
    try:
        result = Decimal(value)
    except (InvalidOperation, TypeError, ValueError) as exc:
        raise ValueError(f'{label}: numero invalido') from exc
    if not result.is_finite() or result < 0:
        raise ValueError(f'{label}: debe ser finito y no negativo')
    if integer and result != result.to_integral_value():
        raise ValueError(f'{label}: debe ser entero')
    return result


def derive_metrics():
    rows = read_rows(STATS, ['Name', *FIELDS.values()])
    aggregate = [r for r in rows if r['Name'].strip() == 'Aggregated']
    if len(aggregate) != 1:
        raise ValueError(f'{STATS}: se requiere exactamente una fila Aggregated')
    metrics = {key: number(aggregate[0][column], column, key in ('peticiones', 'fallos'))
               for key, column in FIELDS.items()}
    if metrics['peticiones'] == 0 or metrics['fallos'] > metrics['peticiones']:
        raise ValueError('Se requieren peticiones > 0 y fallos <= peticiones')
    samples = [r for r in read_rows(HISTORY, ['Name', 'User Count'])
               if r['Name'].strip() == 'Aggregated']
    if not samples:
        raise ValueError(f'{HISTORY}: faltan muestras Aggregated')
    metrics['usuarios_maximos'] = max(number(r['User Count'], 'User Count', True) for r in samples)
    return metrics


def derive_false_positives():
    rows = [row for row in read_rows(FALSE_POSITIVES,
                                     ['corrida', 'mecanismo', 'cadena_integra',
                                      'estado_tabla_integro', 'falso_positivo'])
            if row['mecanismo'].strip() == 'M2']
    if not rows:
        raise ValueError(f'{FALSE_POSITIVES}: faltan observaciones M2')
    ids = [number(row['corrida'], 'corrida', True) for row in rows]
    if len(ids) != len(set(ids)):
        raise ValueError(f'{FALSE_POSITIVES}: corridas M2 duplicadas')
    positives = []
    for row in rows:
        if (number(row['cadena_integra'], 'cadena_integra', True) != 1 or
                number(row['estado_tabla_integro'], 'estado_tabla_integro', True) != 1):
            raise ValueError(f'{FALSE_POSITIVES}: M2 incluye una cadena no integra')
        value = number(row['falso_positivo'], 'falso_positivo', True)
        if value not in (0, 1):
            raise ValueError(f'{FALSE_POSITIVES}: falso_positivo debe ser 0 o 1')
        positives.append(value)
    return sum(positives), len(rows)


def derive_historical_windows():
    rows = [row for row in read_rows(HISTORICAL_WINDOWS,
                                     ['escenario', 'ventana_derivada',
                                      'fallos_generales_derivados_pct',
                                      'exito_peticiones_derivado_pct', 'origen', 'alcance'])
            if row['escenario'].strip() == 'Histórico locust_esc3 (perfil no validado)']
    if not rows:
        raise ValueError(f'{HISTORICAL_WINDOWS}: faltan ventanas historicas locust_esc3')
    ids = [number(row['ventana_derivada'], 'ventana_derivada', True) for row in rows]
    if len(ids) != len(set(ids)):
        raise ValueError(f'{HISTORICAL_WINDOWS}: ventanas locust_esc3 duplicadas')
    success = []
    failures = []
    for row in rows:
        if (row['origen'].strip() != 'experimentos/resultados/locust_esc3_stats_history.csv'
                or 'históricas' not in row['alcance']
                or 'no nueva carga ni agregado oficial' not in row['alcance']):
            raise ValueError(f'{HISTORICAL_WINDOWS}: alcance historico no acreditado')
        passed = number(row['exito_peticiones_derivado_pct'], 'exito_peticiones_derivado_pct')
        failed = number(row['fallos_generales_derivados_pct'], 'fallos_generales_derivados_pct')
        if passed > 100 or failed > 100 or passed + failed != 100:
            raise ValueError(f'{HISTORICAL_WINDOWS}: porcentajes de ventana inconsistentes')
        success.append(passed)
        failures.append(failed)
    return len(rows), min(success), max(success), sum(value > 0 for value in failures)


STRESS_STATS = Path('microservicio-soporte/resultados_estres/20260920_164722/locust_estres_200_stats.csv')


def derive_stress_metrics():
    stress_file = ROOT / STRESS_STATS
    if not stress_file.exists():
        return None
    rows = read_rows(STRESS_STATS, ['Name', 'Request Count', 'Failure Count'])
    aggregate = [r for r in rows if r['Name'].strip() == 'Aggregated']
    if len(aggregate) != 1:
        return None
    return {
        'peticiones': number(aggregate[0]['Request Count'], 'Request Count', True),
        'fallos': number(aggregate[0]['Failure Count'], 'Failure Count', True)
    }


def build_rows(metrics, false_positives, historical_windows, stress_metrics=None):
    """Una sola matriz para todas las serializaciones."""
    rows = []

    def add(characteristic, indicator, value, source, scope, criterion, verdict):
        rows.append(dict(zip(COLUMNS, (characteristic, indicator, value, source,
                                       scope, criterion, verdict))))

    scope = ('Conjunto A, agregado nominal local de Soporte; no demuestra '
             'estres exitoso ni disponibilidad de produccion')
    measured = '; '.join(f'{key}={format(value, "f")}' for key, value in metrics.items())
    verdict = (f"No cumple: P99={format(metrics['p99_ms'], 'f')} ms supera umbral "
               f"{P99_LIMIT_MS} ms" if metrics['p99_ms'] >= P99_LIMIT_MS
               else 'Cumple el umbral en escenario nominal local')
    add('Eficiencia de desempe\u00f1o', 'Carga y latencias del agregado nominal', measured,
        f'{STATS.as_posix()} (Aggregated); {HISTORY.as_posix()} (maximo User Count)',
        scope + '; el veredicto evalua p99 conforme a PI-1 (umbral 500 ms)',
        f'p99 < {P99_LIMIT_MS} ms', verdict)
    window_count, min_success, max_success, failed_windows = historical_windows
    if stress_metrics:
        p_nom, low_nom, high_nom = wilson_interval(metrics['peticiones'] - metrics['fallos'], metrics['peticiones'])
        p_str, low_str, high_str = wilson_interval(stress_metrics['peticiones'] - stress_metrics['fallos'], stress_metrics['peticiones'])
        fiab_value = (f"Nominal A: peticiones={metrics['peticiones']}, fallos={metrics['fallos']} (éxito {p_nom * 100:.2f}%, IC 95% {format_ci_percent(low_nom, high_nom)}); "
                      f"Estrés oficial (200 usuarios): peticiones={stress_metrics['peticiones']}, fallos={stress_metrics['fallos']} (éxito {p_str * 100:.2f}%, IC 95% {format_ci_percent(low_str, high_str)}); "
                      f"Antecedente histórico: {window_count} ventanas locust_esc3 (éxito entre {format(min_success, 'f').replace('.', ',')} % y {format(max_success, 'f').replace('.', ',')} %)")
        fiab_source = f"{STATS.as_posix()} (Aggregated); {STRESS_STATS.as_posix()} (Aggregated); {HISTORICAL_WINDOWS.as_posix()} (ventanas locust_esc3)"
        fiab_scope = "Conjunto nominal A (50 usuarios) y corrida oficial de estrés (200 usuarios) en entorno local/contenedorizado; ventanas históricas de referencia"
        fiab_crit = "Cero fallos (tasa de error 0.00%, éxito >= 99.90%) en nominal y estrés oficial"
        if metrics['fallos'] == 0 and stress_metrics['fallos'] == 0:
            fiab_verd = "Cumple criterio de cero fallos en corrida nominal y corrida oficial de estrés de 200 usuarios; antecedentes históricos conservados"
        else:
            total_f = metrics['fallos'] + stress_metrics['fallos']
            fiab_verd = f"No cumple: se registraron {total_f} fallos en pruebas de carga y estrés"
    else:
        fiab_value = (f"peticiones nominales={metrics['peticiones']}; fallos nominales={metrics['fallos']}; "
                      f'{window_count} ventanas historicas; exito derivado entre '
                      f'{format(min_success, "f").replace(".", ",")} % y '
                      f'{format(max_success, "f").replace(".", ",")} %')
        fiab_source = f'{STATS.as_posix()} (Aggregated); {HISTORICAL_WINDOWS.as_posix()} (ventanas locust_esc3)'
        fiab_scope = scope + '; ventanas historicas / perfil no validado; no son estres oficial actual'
        fiab_crit = 'Cero fallos para estres oficial; sin criterio suficiente para fiabilidad global'
        fiab_verd = f'Evidencia parcial; {failed_windows}/{window_count} ventanas historicas no cumplen el criterio de cero fallos'
    add('Fiabilidad', 'Carga nominal A, estrés oficial 200 usuarios y ventanas históricas',
        fiab_value, fiab_source, fiab_scope, fiab_crit, fiab_verd)
    add('Fiabilidad / disponibilidad', 'Disponibilidad de produccion', 'No medida',
        'Sin fuente temporal de disponibilidad evaluada',
        'Exito de peticiones de carga no equivale a disponibilidad temporal',
        'Requiere medicion temporal y entorno definidos', 'No demostrado')
    positive_count, sample_count = false_positives
    fp_rate, low_fp, high_fp = wilson_interval(positive_count, sample_count)
    if positive_count == 0:
        seg_verd = f"Cumple en pruebas sintéticas: 0 falsos positivos (FPR = 0.00%, IC 95% {format_ci_percent(low_fp, high_fp)}); no certifica seguridad global"
    else:
        seg_verd = f"No cumple: {positive_count} falsos positivos detectados en pruebas sintéticas (FPR = {fp_rate * 100:.2f}%, IC 95% {format_ci_percent(low_fp, high_fp)})"
    add('Seguridad', 'Falsos positivos de M2 en cadenas integras',
        f'No medido en producción; FPR verificado {positive_count}/{sample_count} '
        f'({fp_rate * 100:.2f}%, IC 95% {format_ci_percent(low_fp, high_fp)}) en pruebas sintéticas',
        f'{FALSE_POSITIVES.as_posix()} (observaciones M2)',
        'Solo pruebas sinteticas de cadenas integras; no mide seguridad en produccion',
        'FPR = 0.00% en muestra de control (umbral tolerable < 5.0% a nivel de confianza 95%)',
        seg_verd)
    coverage = derive_coverage()
    add(
        'Mantenibilidad',
        'Cobertura oficial de pruebas por módulo',
        '; '.join(
            f'{module}: {metric} {covered}/{total} = {percent:.2f}%'
            for module, _, metric, _, covered, total, percent in coverage
        ),
        '; '.join(
            f'{path.as_posix()} (contador global {metric})'
            for _, path, metric, _, _, _, _ in coverage
        )
        + '; evidencia común: CI #876, run 35693153935, commit 6c1f67ab28d569643b4c7ec4f740d7221bd60b0f',
        'Un único contador oficial por módulo, obtenido del reporte '
        'JaCoCo versionado generado por integración continua',
        '; '.join(
            f'{module}: mínimo {minimum}% {metric}'
            for module, _, metric, minimum, _, _, _ in coverage
        ),
        '; '.join(
            f'{module}: '
            f'{"Cumple" if percent >= minimum else "No cumple"} '
            f'la compuerta de {minimum}% {metric}'
            for module, _, metric, minimum, _, _, percent in coverage
        ),
    )
    unevaluated = (
        ('Adecuaci\u00f3n funcional', 'Satisfaccion de requisitos funcionales',
         'No se evalua evidencia funcional en este generador'),
        ('Usabilidad', 'Indicadores de uso', 'Sin estudio de usuarios evaluado en este alcance'),
        ('Portabilidad', 'Ejecucion en entornos definidos', 'Sin medicion de portabilidad evaluada en este alcance'),
        ('Compatibilidad', 'Interoperabilidad y coexistencia', 'Sin evidencia de compatibilidad evaluada en este alcance'),
    )
    for characteristic, indicator, explanation in unevaluated:
        add(characteristic, indicator, 'Sin medicion evaluada', 'Sin fuente evaluada en este alcance',
            explanation, 'No establecido en esta evaluacion', 'No evaluado')
    return rows


def serialize_csv(rows):
    output = io.StringIO(newline='')
    writer = csv.DictWriter(output, fieldnames=COLUMNS, lineterminator='\n')
    writer.writeheader()
    writer.writerows(rows)
    return output.getvalue()


def latex_escape(text):
    escapes = {'\\': r'\textbackslash{}', '&': r'\&', '%': r'\%', '$': r'\$',
               '#': r'\#', '_': r'\_', '{': r'\{', '}': r'\}', '~': r'\textasciitilde{}',
               '^': r'\textasciicircum{}', '<': r'\textless{}', '>': r'\textgreater{}'}
    return ''.join(escapes.get(char, char) for char in str(text))


def serialize_latex(rows):
    # Bloques de dos columnas: conservan las siete propiedades sin comprimir
    # una matriz ancha en una pagina. Requiere tabularx en el documento padre.
    lines = ['% Generado por generar_matriz.py; requiere tabularx y UTF-8.']
    for row in rows:
        lines.extend([r'\begin{tabularx}{\linewidth}{|p{0.22\linewidth}|X|}', r'\hline'])
        for column in COLUMNS:
            lines.append(latex_escape(column) + ' & ' + latex_escape(row[column]) + r' \\ \hline')
        lines.extend([r'\end{tabularx}', r'\par\medskip'])
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--preview', action='store_true', help='Solo consola; incompatible con escritura')
    parser.add_argument('--format', choices=('json', 'csv', 'latex'), default='json')
    parser.add_argument('--write-csv', action='store_true', help=f'Escribir {CSV_OUTPUT.relative_to(ROOT)}')
    parser.add_argument('--write-latex', action='store_true', help=f'Escribir {TEX_OUTPUT.relative_to(ROOT)}')
    args = parser.parse_args()
    if args.preview and (args.write_csv or args.write_latex):
        parser.error('--preview no permite escribir archivos')
    try:
        rows = build_rows(derive_metrics(), derive_false_positives(),
                          derive_historical_windows(), derive_stress_metrics())
        csv_text, tex_text = serialize_csv(rows), serialize_latex(rows)
        if args.write_csv:
            CSV_OUTPUT.write_text(csv_text, encoding='utf-8', newline='\n')
        if args.write_latex:
            TEX_OUTPUT.write_text(tex_text, encoding='utf-8', newline='\n')
        if args.format == 'json':
            print(json.dumps(rows, ensure_ascii=False, indent=2))
        else:
            print(csv_text if args.format == 'csv' else tex_text, end='')
        return 0
    except (OSError, ValueError, csv.Error) as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
