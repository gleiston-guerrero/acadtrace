"""Genera/verifica resumen y SHA256SUMS tras validar las seis corridas.

--verify no escribe archivos. La cobertura incluye toda la evidencia de las
seis corridas seleccionadas y los dos resumenes derivados; nunca las invalidas.
"""
import argparse
from pathlib import Path
import sys
from resumir_repeticiones_carga import DEFAULT, generate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify', action='store_true')
    parser.add_argument('--base', type=Path, default=DEFAULT)
    args = parser.parse_args()
    try:
        generate(args.base.resolve(), check=args.verify)
    except (OSError, ValueError, KeyError) as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        return 1
    print('OK: 3+3, resumen y SHA256SUMS ' + ('verificados' if args.verify else 'generados'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
