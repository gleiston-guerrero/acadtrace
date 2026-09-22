"""Una corrida Locust; inhibidor ligado al hilo y liberacion obligatoria."""
import argparse
import csv
import ctypes
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from datetime import datetime, timezone


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('name')
    parser.add_argument('users', type=int)
    parser.add_argument('spawn', type=int)
    parser.add_argument('seconds', type=int)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    folder = root / 'microservicio-soporte/resultados_repeticiones' / args.name
    if folder.exists():
        raise RuntimeError('La carpeta ya existe; no se sobrescribe ni repite')
    if not os.environ.get('JWT_SECRET') or os.environ['JWT_SECRET'] == 'dummy_secret':
        raise RuntimeError('JWT ausente o invalido')
    api = ctypes.WinDLL('kernel32', use_last_error=True)
    api.SetThreadExecutionState.argtypes = [ctypes.c_uint32]
    api.SetThreadExecutionState.restype = ctypes.c_uint32
    continuous = 0x80000000
    folder.mkdir()
    profile = folder / 'perfil.txt'
    start = datetime.now(timezone.utc)
    profile.write_text(
        f'CORRIDA={args.name}\nUSUARIOS={args.users}\nSPAWN_RATE={args.spawn}\n'
        f'DURACION_CONFIGURADA_SEGUNDOS={args.seconds}\nHOST=http://localhost:8085\n'
        f'INICIO_UTC={start.isoformat()}\nJWT_ORIGEN=contenedor; misma sesion PowerShell\n',
        encoding='utf-8')
    child = None
    exit_code = None
    acquired = False
    released = False
    try:
        # SYSTEM_REQUIRED | DISPLAY_REQUIRED: evita el inicio de espera por inactividad.
        if not api.SetThreadExecutionState(continuous | 1 | 2):
            raise ctypes.WinError(ctypes.get_last_error())
        acquired = True
        print(f'{args.name}: INHIBIDOR_ACTIVO; sistema y pantalla; PID={os.getpid()}', flush=True)
        with (folder / 'console.log').open('wb') as console:
            child = subprocess.Popen([
                sys.executable, '-m', 'locust', '-f', str(root / 'microservicio-soporte/locustfile.py'),
                '--headless', '-u', str(args.users), '-r', str(args.spawn),
                '--run-time', f'{args.seconds}s', '--host', 'http://localhost:8085',
                '--csv', str(folder / 'stats'), '--csv-full-history',
            ], stdout=console, stderr=subprocess.STDOUT)
            print(f'{args.name}: LOCUST_PID={child.pid}', flush=True)
            while True:
                try:
                    exit_code = child.wait(timeout=30)
                    break
                except subprocess.TimeoutExpired:
                    print(f'{args.name}: en ejecucion; transcurridos={(datetime.now(timezone.utc)-start).total_seconds():.0f}s', flush=True)
    finally:
        try:
            if child is not None:
                if child.poll() is None:
                    child.terminate()
                    try:
                        child.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        child.kill()
                        child.wait()
                exit_code = child.returncode
        finally:
            if acquired:
                released = bool(api.SetThreadExecutionState(continuous))
            end = datetime.now(timezone.utc)
            with profile.open('a', encoding='utf-8') as out:
                out.write(f'FIN_UTC={end.isoformat()}\nDURACION_REAL_SEGUNDOS={(end-start).total_seconds()}\nEXIT_CODE={exit_code}\nINHIBIDOR_ACTIVADO={acquired}\nINHIBIDOR_LIBERADO={released}\n')
            (folder / 'EXIT_CODE.txt').write_text(f'EXIT_CODE={exit_code}\n', encoding='utf-8')
            print(f'{args.name}: INHIBIDOR_LIBERADO={released}; EXIT_CODE={exit_code}', flush=True)
    # Normalizar solamente nombres de archivos nuevos, nunca contenido CSV.
    names = {'stats_stats.csv':'stats.csv', 'stats_stats_history.csv':'stats_history.csv',
             'stats_failures.csv':'failures.csv', 'stats_exceptions.csv':'exceptions.csv'}
    for old, new in names.items():
        (folder / old).rename(folder / new)
    def rows(name):
        with (folder / name).open(encoding='utf-8-sig', newline='') as f:
            return list(csv.DictReader(f))
    history = [r for r in rows('stats_history.csv') if r['Name'] == 'Aggregated']
    total = next(r for r in rows('stats.csv') if r['Name'] == 'Aggregated')
    gaps = [{'antes':a['Timestamp'], 'despues':b['Timestamp'],
             'segundos':float(b['Timestamp'])-float(a['Timestamp']),
             'usuarios_antes':int(a['User Count']), 'usuarios_despues':int(b['User Count'])}
            for a,b in zip(history,history[1:])]
    console = (folder / 'console.log').read_text(encoding='utf-8', errors='replace')
    duration = float(history[-1]['Timestamp'])-float(history[0]['Timestamp'])
    failures = rows('failures.csv')
    exceptions = rows('exceptions.csv')
    http_errors = re.findall(r'(?i)(?:error|http|status|code|c[oó]digo)[^\r\n]{0,60}\b(?:401|500|503)\b', console + json.dumps(failures))
    result = dict(corrida=args.name, exit_code=exit_code, failure_count=int(total['Failure Count']),
                  max_usuarios=max(int(r['User Count']) for r in history),
                  duracion_observada=duration, max_intervalo=max(g['segundos'] for g in gaps),
                  discontinuidades_mayores_5s=[g for g in gaps if g['segundos']>5],
                  errores_http_401_500_503=len(http_errors), failures_rows=len(failures),
                  exceptions_rows=len(exceptions), console_bytes=(folder/'console.log').stat().st_size,
                  inhibidor_liberado=released, tolerancia_duracion_segundos=5)
    result['valida'] = (exit_code == 0 and result['failure_count'] == 0 and
        result['max_usuarios'] == args.users and abs(duration-args.seconds)<=5 and
        not result['discontinuidades_mayores_5s'] and not http_errors and not failures and
        not exceptions and result['console_bytes']>0 and released)
    (folder/'validacion.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    with profile.open('a', encoding='utf-8') as f:
        f.write(f"CLASIFICACION={'valida' if result['valida'] else 'invalida'}\n")
    print(json.dumps(result), flush=True)
    return 0 if result['valida'] else 2


if __name__ == '__main__':
    sys.exit(main())
