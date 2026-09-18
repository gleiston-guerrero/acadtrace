"""E22: preview por defecto. Solo --write-csv/--write-latex escriben archivos.
Las categorias solicitadas por el proyecto no certifican ISO/IEC 25010 completo.
La evidencia historica se distingue del conjunto nominal A. Solo biblioteca estandar.
"""
import argparse
import csv
import io
import json
import sys
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


def build_rows(metrics, false_positives, historical_windows):
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
    add('Fiabilidad', 'Nominal A y ventanas historicas de locust_esc3',
        f"peticiones nominales={metrics['peticiones']}; fallos nominales={metrics['fallos']}; "
        f'{window_count} ventanas historicas; exito derivado entre '
        f'{format(min_success, "f").replace(".", ",")} % y '
        f'{format(max_success, "f").replace(".", ",")} %',
        f'{STATS.as_posix()} (Aggregated); {HISTORICAL_WINDOWS.as_posix()} '
        '(ventanas locust_esc3)',
        scope + '; ventanas historicas / perfil no validado; no son estres oficial actual',
        'Cero fallos para estres oficial; sin criterio suficiente para fiabilidad global',
        f'Evidencia parcial; {failed_windows}/{window_count} ventanas historicas '
        'no cumplen el criterio de cero fallos')
    add('Fiabilidad / disponibilidad', 'Disponibilidad de produccion', 'No medida',
        'Sin fuente temporal de disponibilidad evaluada',
        'Exito de peticiones de carga no equivale a disponibilidad temporal',
        'Requiere medicion temporal y entorno definidos', 'No demostrado')
    positive_count, sample_count = false_positives
    add('Seguridad', 'Falsos positivos de M2 en cadenas integras',
        f'No medido en producción; FPR verificado {positive_count}/{sample_count} '
        'en pruebas sintéticas',
        f'{FALSE_POSITIVES.as_posix()} (observaciones M2)',
        'Solo pruebas sinteticas de cadenas integras; no mide seguridad en produccion',
        'Falsos positivos observados / muestras validas',
        'Evidencia parcial; no certifica seguridad global')
    add('Mantenibilidad', 'Cobertura de pruebas en microservicios',
        'Secretaria: 71.42%; Soporte: 71.61%; Principal: 31.6%',
        'docs/cobertura/README.md; reportes oficiales JaCoCo y pytest',
        'Cobertura de codigo a nivel BUNDLE/LINE en microservicios backend',
        'Cobertura >= 70% LINE',
        'Cumple en Secretaría (71.42%) y Soporte (71.61%); Principal 31.6% en curso')
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
                          derive_historical_windows())
        csv_text, tex_text = serialize_csv(rows), serialize_latex(rows)
        if args.write_csv:
            CSV_OUTPUT.write_text(csv_text, encoding='utf-8')
        if args.write_latex:
            TEX_OUTPUT.write_text(tex_text, encoding='utf-8')
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
