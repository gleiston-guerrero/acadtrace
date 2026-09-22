"""Validate complementary Locust runs and summarize between-run variability.

Reads raw evidence without changing it. Bootstrap percentile CI is for the
between-run mean (seed 12345, 10000 resamples), not for individual requests.
"""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import random
import re
import statistics

ROOT = Path(__file__).resolve().parents[1]
DEFAULT = ROOT / 'microservicio-soporte/resultados_repeticiones'
SEED = 12345
RESAMPLES = 10000
SELECTED = {'nominal': ('nominal_02', 'nominal_03', 'nominal_06'),
            'estres': ('estres_01', 'estres_05', 'estres_06')}
FIELDS = {
    'peticiones': 'Request Count', 'fallos': 'Failure Count',
    'rps': 'Requests/s', 'media_ms': 'Average Response Time',
    'p50_ms': '50%', 'p95_ms': '95%', 'p99_ms': '99%',
    'maximo_ms': 'Max Response Time',
}
HTTP_ERROR = re.compile(
    r'(?:HTTP(?:Error)?\s*[:=]?\s*|Error\s+|status(?:_code)?\s*[:=]?\s*|'
    r'code\s*[:=]?\s*|c[oó]digo[^\r\n]{0,30}?|ClientError\s*\(\s*[\"\x27]?)(401|500|503)\b', re.I)


def read_text(path):
    data = path.read_bytes()
    encoding = 'utf-16' if data.startswith((b'\xff\xfe', b'\xfe\xff')) else 'utf-8-sig'
    return data.decode(encoding)


def read_csv(path):
    with path.open(encoding='utf-8-sig', newline='') as stream:
        return list(csv.DictReader(stream))


def validate_run(directory):
    result = {'corrida': directory.name, 'valida': False, 'errores': []}
    errors = result['errores']
    match = re.fullmatch(r'(nominal|estres)_(\d{2})', directory.name)
    if not match:
        errors.append('Nombre excluido: corrida interrumpida/incompleta o ajena al conjunto.')
        return result
    kind, repetition = match.groups()
    result['tipo'] = kind
    users, duration, rate = (50, 300, 5) if kind == 'nominal' else (200, 600, 1)
    prefix = directory / ('locust_' + directory.name)
    def artifact(suffix):
        modern = directory / suffix.lstrip('_')
        return modern if modern.exists() else Path(str(prefix) + suffix)
    try:
        profile = dict(line.split('=', 1) for line in read_text(directory / 'perfil.txt').splitlines()
                       if '=' in line)
        for key, expected in {
            'USUARIOS': str(users), 'SPAWN_RATE': str(rate),
            'HOST': 'http://localhost:8085',
        }.items():
            if profile.get(key) != expected:
                errors.append(f'Perfil: {key} ausente o incompatible.')
        if profile.get('DURACION_CONFIGURADA', '') != f'{duration // 60}m' and profile.get('DURACION_CONFIGURADA_SEGUNDOS') != str(duration):
            errors.append('Duracion configurada incompatible.')
        if not (profile.get('FECHA_UTC') or profile.get('INICIO_UTC')):
            errors.append('Falta fecha declarada.')
        if profile.get('CLASIFICACION', '').startswith('invalida'):
            errors.append('Corrida previamente clasificada invalida.')
        if 'INHIBIDOR_ACTIVADO' in profile and profile.get('INHIBIDOR_LIBERADO') != 'True':
            errors.append('Liberacion del inhibidor no acreditada.')
        console = read_text(directory / 'console.log')
        if not console.strip():
            errors.append('Console.log vacio.')
        console_exits = re.findall(r'Shutting down \(exit code (\d+)\)', console)
        exit_code = profile.get('EXIT_CODE')
        if exit_code is None and console_exits:
            exit_code = console_exits[-1]
            result['fuente_exit_code'] = 'console.log (cierre real de Locust)'
        else:
            result['fuente_exit_code'] = 'perfil.txt' if exit_code is not None else 'no disponible'
        result['exit_code'] = int(exit_code) if exit_code is not None else None
        if exit_code != '0' or any(code != '0' for code in console_exits):
            errors.append('EXIT_CODE=0 no acreditado o cierre no exitoso.')
        if (directory / 'EXIT_CODE.txt').exists() and read_text(directory / 'EXIT_CODE.txt').strip() != 'EXIT_CODE=0':
            errors.append('EXIT_CODE.txt incompatible con exito.')
        rows = read_csv(artifact('_stats.csv'))
        aggregates = [row for row in rows if row.get('Name') == 'Aggregated']
        if len(aggregates) != 1:
            raise ValueError('Se requiere exactamente una fila Aggregated.')
        aggregate = aggregates[0]
        metrics = {key: float(aggregate[column]) for key, column in FIELDS.items()}
        if any(not math.isfinite(value) or value < 0 for value in metrics.values()):
            raise ValueError('Metricas no finitas o negativas.')
        if any(not metrics[key].is_integer() for key in ('peticiones', 'fallos')):
            raise ValueError('Conteos no enteros.')
        if metrics['peticiones'] <= 0 or metrics['fallos'] != 0:
            errors.append('Sin peticiones o Failure Count distinto de cero.')
        if sum(int(row['Request Count']) for row in rows if row.get('Name') != 'Aggregated') != metrics['peticiones']:
            errors.append('Peticiones de endpoints no coinciden con Aggregated.')
        if any(int(row['Failure Count']) != 0 for row in rows):
            errors.append('Fallos en endpoints.')
        history = [row for row in read_csv(artifact('_stats_history.csv'))
                   if row.get('Name') == 'Aggregated']
        times = [float(row['Timestamp']) for row in history]
        if not times or any(not math.isfinite(t) for t in times) or times != sorted(times):
            raise ValueError('Historial vacio, no finito o desordenado.')
        metrics['duracion_observada_s'] = max(times) - min(times)
        metrics['usuarios_maximos'] = max(int(row['User Count']) for row in history)
        result['muestras_historial'] = len(history)
        if not duration - 5 <= metrics['duracion_observada_s'] <= duration + 5:
            errors.append(f'Duracion fuera de {duration - 5}..{duration + 5} segundos.')
        gaps = [b-a for a,b in zip(times,times[1:])]
        result['max_intervalo_s'] = max(gaps, default=0)
        result['discontinuidades_mayores_5s'] = sum(gap > 5 for gap in gaps)
        if any(gap > 5 or gap <= 0 for gap in gaps):
            errors.append('Discontinuidad temporal >5 s o timestamps repetidos.')
        if metrics['usuarios_maximos'] != users:
            errors.append(f'Maximo de usuarios distinto de {users}.')
        if kind == 'nominal' and not 290 <= len(history) <= 305:
            errors.append('Muestras nominales fuera de tolerancia 290..305 (aproximadamente 295..300).')
        for suffix, count in (('_failures.csv', 'Occurrences'), ('_exceptions.csv', 'Count')):
            auxiliary = read_csv(artifact(suffix))
            if any(int(row[count]) != 0 for row in auxiliary):
                errors.append(f'{suffix}: fallos o excepciones registrados.')
            if HTTP_ERROR.search(read_text(artifact(suffix))):
                errors.append(f'{suffix}: HTTP 401/500/503 registrado.')
        if HTTP_ERROR.search(console):
            errors.append('HTTP 401/500/503 registrado en console.log.')
        result['metricas'] = metrics
        result['criterio_latencia'] = {
            'descripcion': 'P99 < 500 ms' if kind == 'nominal' else 'P95 < 500 ms',
            'cumple': metrics['p99_ms' if kind == 'nominal' else 'p95_ms'] < 500,
        }
        result['valida'] = not errors
    except (OSError, ValueError, KeyError, UnicodeError, csv.Error):
        errors.append('Artefactos faltantes, ilegibles o con formato invalido; no se publican contenidos crudos.')
    return result


def quantile(values, fraction):
    position = (len(values) - 1) * fraction
    lower = int(position)
    upper = min(lower + 1, len(values) - 1)
    return values[lower] + (values[upper] - values[lower]) * (position - lower)


def summarize(values):
    n = len(values)
    if not n:
        return None
    interval = None
    if n >= 2:
        rng = random.Random(SEED)
        means = sorted(statistics.mean(rng.choices(values, k=n)) for _ in range(RESAMPLES))
        interval = [quantile(means, .025), quantile(means, .975)]
    return {'media': statistics.mean(values), 'mediana': statistics.median(values),
            'desviacion_estandar_muestral': statistics.stdev(values) if n >= 2 else None,
            'minimo': min(values), 'maximo': max(values), 'ic95_bootstrap_media': interval}


def generate(base, check=False):
    selected = {name for names in SELECTED.values() for name in names}
    runs = [validate_run(base / name) for name in sorted(selected)]
    rejected = [run for run in runs if not run['valida']]
    if rejected:
        raise ValueError('Se requieren 3+3 validas: ' + '; '.join(
            r['corrida'] + ': ' + ', '.join(r['errores']) for r in rejected))
    summary = {'metodo': {'semilla': SEED, 'remuestras': RESAMPLES,
                         'intervalo': 'Percentil bootstrap bilateral 95% para la media entre corridas; interpolacion lineal.',
                         'limites': 'n=3 sigue siendo pequeno; precision limitada. P50/P95/P99 de cada CSV describen peticiones, no variabilidad entre ejecuciones.'},
               'seleccion': SELECTED, 'corridas': runs, 'escenarios': {}}
    for kind in ('nominal', 'estres'):
        valid = [r for r in runs if r.get('tipo') == kind and r['valida']]
        summary['escenarios'][kind] = {'n_validas': len(valid), 'metricas': {
            key: summarize([r['metricas'][key] for r in valid])
            for key in (*FIELDS, 'duracion_observada_s', 'usuarios_maximos')}}
    text = ['# Repeticiones complementarias de carga #48', '',
            'Los CSV oficiales anteriores permanecen intactos. Solo se agregan corridas validas.',
            'n=3 sigue siendo una muestra pequena; los IC entre corridas tienen precision limitada.',
            'P50/P95/P99 describen peticiones dentro de cada corrida, no repeticiones independientes.',
            'IC: percentil bootstrap 95% de la media, semilla 12345, 10000 remuestras.',
            'Con menos de dos corridas, desviacion muestral e IC no son estimables.', '',
            '| Corrida | Valida | Peticiones | Fallos | RPS | Media ms | P50 | P95 | P99 | Max ms | Segundos | Usuarios |',
            '|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|']
    for run in runs:
        m = run.get('metricas', {})
        values = [f'{m[k]:.6f}'.rstrip('0').rstrip('.') if k in m else 'N/D'
                  for k in (*FIELDS, 'duracion_observada_s', 'usuarios_maximos')]
        text.append('| ' + ' | '.join([run['corrida'], 'si' if run['valida'] else 'no', *values]) + ' |')
    for run in runs:
        if run['errores']:
            text.extend(['', run['corrida'] + ': ' + '; '.join(run['errores']), ''])
    for kind, group in summary['escenarios'].items():
        text.extend(['', f'## {kind}: n={group["n_validas"]}', '',
                     '| Metrica | Media | Mediana | DE muestral | Min | Max | IC95 de la media |',
                     '|---|---:|---:|---:|---:|---:|---|'])
        for key, data in group['metricas'].items():
            if data is None:
                continue
            fmt = lambda v: 'no estimable' if v is None else f'{v:.6f}'
            ci = data['ic95_bootstrap_media']
            text.append('| ' + ' | '.join([key, *[fmt(data[k]) for k in
                ('media', 'mediana', 'desviacion_estandar_muestral', 'minimo', 'maximo')],
                'no estimable' if ci is None else f'[{ci[0]:.6f}, {ci[1]:.6f}]']) + ' |')
    documents = {'resumen_repeticiones.json': json.dumps(summary, ensure_ascii=False, indent=2) + '\n',
                 'resumen_repeticiones.md': '\n'.join(text) + '\n'}
    for name, content in documents.items():
        if check:
            if (base / name).read_bytes() != content.encode('utf-8'):
                raise ValueError(f'{name}: resumen publicado difiere del recalculo')
        else:
            (base / name).write_bytes(content.encode('utf-8'))
    hashes = []
    for run in runs:
        if run['valida']:
            # Orden determinista reproducible entre sistemas operativos (Windows/Linux)
            for file in sorted((base / run['corrida']).rglob('*'),
                               key=lambda p: p.relative_to(base).as_posix().casefold()):
                if file.is_file():
                    hashes.append(f'{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.relative_to(base).as_posix()}')
    hashes.extend(f'{hashlib.sha256(content.encode("utf-8")).hexdigest()}  {name}'
                  for name, content in sorted(documents.items()))
    manifest = ('\n'.join(hashes) + '\n').encode('utf-8')
    if check:
        if (base / 'SHA256SUMS.txt').read_bytes() != manifest:
            raise ValueError('SHA-256: contenido o cobertura del manifiesto difiere')
    else:
        (base / 'SHA256SUMS.txt').write_bytes(manifest)
    return summary


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path, default=DEFAULT)
    parser.add_argument('--check', action='store_true', help='Verificar sin reescribir evidencia ni resumen')
    args = parser.parse_args()
    output = generate(args.base.resolve(), check=args.check)
    for scenario, group in output['escenarios'].items():
        print(f'{scenario}: {group["n_validas"]} corridas validas')
    for run in output['corridas']:
        if not run['valida']:
            print(f'{run["corrida"]}: EXCLUIDA: ' + '; '.join(run['errores']))
