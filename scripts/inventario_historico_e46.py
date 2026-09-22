"""Read-only historical inventory. Output contains metadata only, never values."""
import argparse
import json
from collections import Counter
from pathlib import Path
import re

from auditar_referencias_e46 import git, production_literals


def classify_existing():
    """Context classification only; never suppress findings or change the gate."""
    out = Path('docs/seguridad')
    target = out / 'e46_historial_metadata.json'
    data = json.loads(target.read_text(encoding='utf-8'))
    cache = {}
    for f in data['findings']:
        key = (f['commit'], f['path'])
        if key not in cache:
            cache[key] = git('show', f['commit'] + ':' + f['path']).decode(errors='replace')
        content = cache[key]
        lines = content.splitlines()
        line = lines[f['line'] - 1]
        kind = 'requiere revisión'
        evidence = 'Contexto insuficiente'
        category = 'Sin determinar'
        rule = f['type']
        if rule == 'e46-sql-password-hash':
            category, kind = 'Hashes de autenticación', 'credencial potencialmente real'
            evidence = 'Material bcrypt en dump/baseline operativo; no acredita cuentas distintas ni vigencia'
        elif rule == 'e46-operational-python-password':
            category, kind = 'Cuenta API administrativa', 'credencial potencialmente real'
            evidence = 'Contraseña literal en petición de autenticación del script operativo'
        elif rule in ('e46-env-literal-secret', 'e46-properties-literal-secret'):
            category = ('SMTP' if 'mail.password' in line else 'JWT' if 'jwt' in line.lower()
                        else 'PostgreSQL' if 'DB_PASSWORD' in line or 'db.password' in line else 'Sin determinar')
            kind = 'credencial potencialmente real'
            evidence = 'Literal de configuración de servicio, sin prueba de procedencia sintética'
        elif f['path'] == '.github/workflows/ci-cd.yml':
            jobs = re.findall(r'^  ([\w-]+):', '\n'.join(lines[:f['line']]), re.M)
            if jobs and jobs[-1] == 'test-secretaria-backend' and 'AES_SECRET_KEY' in line:
                category, kind = 'Cifrado AES', 'fixture/test'
                evidence = 'Variable del job test-secretaria-backend, no secreto de despliegue'
        elif f['path'].endswith('/SecurityTest.java') and '@Test' in content and 'private static final String SECRET' in content:
            category, kind = 'JWT', 'fixture/test'
            evidence = 'Constante de clase SecurityTest utilizada en pruebas unitarias'
        elif f['path'] == 'tests/browser/test_sga_navegador.py' and 'page.set_content' in content and 'token_expirado' in content:
            category, kind = 'JWT', 'fixture/test'
            evidence = 'Token de escenario de navegador con HTML preparado mediante page.set_content'
        f.update(risk_classification=kind, category=category, classification_evidence=evidence,
                 assessment=kind + '; ' + evidence)
    counts = Counter(f['risk_classification'] for f in data['findings'])
    data['risk_summary'] = {'occurrences': len(data['findings']),
                           'unique_commits': len({f['commit'] for f in data['findings']}),
                           'unique_paths': len({f['path'] for f in data['findings']}),
                           'categories': dict(Counter(f['category'] for f in data['findings'])),
                           'classification_counts': dict(counts),
                           'note': 'Ocurrencias no equivalen a secretos distintos; potencial no significa confirmado. No se modifica el detector ni el baseline.'}
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    md = out / 'e46_inventario_historico.md'
    tail = md.read_text(encoding='utf-8').partition('\n## Verificación adicional')[2]
    rows = ['# E46 — Inventario histórico sanitizado', '',
            '102 ocurrencias; 11 del baseline y 91 adicionales. No son 102 credenciales reales distintas.', '',
            'Clasificación contextual: ' + '; '.join(f'{k}: {v}' for k, v in counts.items()) + '.', '',
            f"Commits únicos: {data['risk_summary']['unique_commits']}; rutas únicas: {data['risk_summary']['unique_paths']}; categorías: {len(data['risk_summary']['categories'])}.", '',
            'Las 91 adicionales son material potencialmente sensible (65 hashes y 26 literales), sin acreditar vigencia. Los 11 originales pertenecen a contextos de prueba. No se añadieron excepciones: la compuerta conserva las 102 ocurrencias.', '',
            '| Categoría | Ruta histórica | Commit | Clasificación | Evidencia contextual |', '|---|---|---|---|---|']
    rows += [f"| {f['category']} | `{f['path']}` | `{f['commit']}` | {f['risk_classification']} | {f['classification_evidence']} |" for f in data['findings']]
    md.write_text('\n'.join(rows) + '\n\n## Verificación adicional' + tail, encoding='utf-8')
    print(json.dumps(data['risk_summary'], ensure_ascii=True))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    parser.add_argument('--classify-existing', action='store_true')
    args = parser.parse_args()
    if args.classify_existing:
        classify_existing()
        return
    if not args.report:
        parser.error('--report is required unless --classify-existing is used')
    report = json.loads(args.report.read_text(encoding='utf-8-sig'))
    literals = production_literals()
    refs = git('for-each-ref', '--format=%(refname)').decode().splitlines()
    cache = {}

    def contains(commit):
        if commit not in cache:
            cache[commit] = git('for-each-ref', '--contains', commit,
                               '--format=%(refname)').decode().splitlines()
        return cache[commit]

    findings = report['findings']
    for item in findings:
        item['refs'] = contains(item['commit'])
        item['assessment'] = ('Probable fixture; no acredita credencial real'
                              if '/test/' in item['path'] or item['path'].startswith('tests/')
                              else 'Potencialmente real; validar con responsable')
    messages = []
    log_fields = git('log', '--all', '--format=%H%x00%aI%x00%B%x00').split(b'\0')
    for index in range(0, len(log_fields)-1, 3):
        sha, date, body = log_fields[index:index+3]
        sha = sha.strip()
        if any(value in body for value in literals):
            # Do not publish arbitrary subject content, which may itself hold credentials.
            messages.append({'commit': sha.decode(), 'date': date.decode(),
                             'subject': '[Asunto omitido: contiene referencia de servidor]',
                             'refs': contains(sha.decode())})
    paths = ['microservicio-soporte/.env', 'sga-principal/sql/supabase_dump_completo.sql',
             'scripts/populate_all_docentes_full.py']
    presence = []
    for path in paths:
        commits = [f['commit'] for f in findings if f['path'] == path]
        commits += git('log', '--all', '--full-history', '--format=%H', '--', path).decode().splitlines()
        for sha in dict.fromkeys(commits):
            try:
                data = git('show', sha + ':' + path)
            except Exception:
                continue
            categories = []
            patterns = {
                'PostgreSQL': rb'(?i)(?:DB_PASSWORD|POSTGRES_PASSWORD|spring\.datasource\.password)',
                'JWT': rb'(?i)JWT_SECRET', 'gRPC': rb'GRPC_INTERNAL_TOKEN',
                'SMTP': rb'(?i)(?:MAIL_PASSWORD|SMTP_PASSWORD|spring\.mail\.password)',
                'API': rb'(?i)(?:API_KEY|GEMINI_API_KEY)',
                'Firebase': rb'(?i)(?:private_key|FIREBASE)',
                'Cifrado': rb'(?:AES_SECRET_KEY|ENCRYPTION_KEY)',
                'Hashes de autenticacion': rb'\$2[aby]\$\d\d\$',
                'Cuenta API operativa': rb'''["']password["']\s*:\s*["'][^"']+["']''',
            }
            categories = [name for name, pattern in patterns.items() if re.search(pattern, data)]
            if not categories:
                continue
            presence.append({'path': path, 'commit': sha, 'refs': contains(sha),
                             'categories': categories, 'status': 'PENDIENTE; presencia no prueba vigencia'})
            break
    seeds = {f['commit'] for f in findings} | {m['commit'] for m in messages}
    ip_pattern = '|'.join(re.escape(value.decode()) for value in literals)
    seeds.update(git('log', '--all', '--full-history', '-m', '--format=%H',
                     '-G' + ip_pattern).decode().splitlines())
    for path in paths[:2]:
        seeds.update(git('log', '--all', '--full-history', '--format=%H', '--', path).decode().splitlines())
    affected = set()
    # Descendants change IDs even when their own patch has no secret.
    graph = git('rev-list', '--all', '--reverse', '--topo-order', '--parents').decode().splitlines()
    for row in graph:
        sha, *parents = row.split()
        if sha in seeds or any(parent in affected for parent in parents):
            affected.add(sha)
    result = {'scope': 'refs locales disponibles; sin fetch ni comprobacion del servidor',
              'findings': findings, 'messages': messages, 'historical_presence': presence,
              'refs': refs, 'commits_total': len(graph),
              'commits_affected_minimum': len(affected),
              'affected_refs': sorted({ref for seed in seeds for ref in contains(seed)}),
              'count_note': 'Minimo por hallazgos y mensajes; el conteo exacto requiere simulacion aislada autorizada y mapa completo de reemplazos.'}
    out = Path('docs/seguridad')
    (out / 'e46_historial_metadata.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    lines = ['# E46 — Inventario histórico sanitizado', '',
             'Alcance: referencias locales disponibles. No acredita vigencia ni rotación. Cada ocurrencia y sus refs están en `e46_historial_metadata.json`.', '',
             f"Hallazgos: {len(findings)}; conocidos: {report['known']}; adicionales: {report['added']}. Los 11 originales se conservan sin ampliar el baseline.", '',
             '| Sistema / categoría | Ruta histórica | Commit | Estado | Acción requerida | Evidencia disponible |',
             '|---|---|---|---|---|---|']
    for f in findings:
        lines.append(f"| {f['type']} | `{f['path']}` | `{f['commit']}` | {f['classification']}; {f['assessment']} | Revisar, rotar si aplica y retirar historia tras autorización | Gitleaks; refs en metadata |")
    lines += ['', '## Verificación adicional de archivos históricos', '',
              'Los nombres de variables indican categorías a investigar; su presencia no certifica que contengan un secreto literal.', '']
    for p in presence:
        lines.append(f"- `{p['path']}` en `{p['commit']}`: {', '.join(p['categories']) or 'revisión requerida'}; alcanzable desde {', '.join(p['refs'])}.")
    lines += ['', '## Mensajes de commit', '', '| SHA | Fecha | Asunto sanitizado |', '|---|---|---|']
    for m in messages:
        lines.append(f"| `{m['commit']}` | {m['date']} | {m['subject']} |")
    (out / 'e46_inventario_historico.md').write_text('\n'.join(lines)+'\n', encoding='utf-8')
    classify_existing()
    print(json.dumps({'findings': len(findings), 'types': dict(Counter(f['type'] for f in findings)),
                      'messages': len(messages), 'commits_total': len(graph),
                      'commits_affected_minimum': len(affected)}))


if __name__ == '__main__':
    main()
