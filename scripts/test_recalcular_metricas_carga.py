"""Pruebas de la compuerta #48 en copias temporales, nunca en CSV oficiales."""
import csv
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class OfficialGateTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.manifest_path = self.root / 'docs/locust/manifest_carga.json'
        self.manifest = json.loads((ROOT / 'docs/locust/manifest_carga.json').read_text(encoding='utf-8'))
        files = ['scripts/recalcular_metricas_carga.py', 'docs/locust/manifest_carga.json',
                 'experimentos/resultados/locust_esc1_stats.csv',
                 'experimentos/resultados/locust_esc3_stats.csv',
                 'microservicio-soporte/locust_esc1_failures.csv',
                 'microservicio-soporte/locust_esc1_exceptions.csv']
        for entry in self.manifest['runs'].values():
            files.extend(entry['sha256'])
        for name in files:
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)

    def run_gate(self):
        return subprocess.run([sys.executable, str(self.root / 'scripts/recalcular_metricas_carga.py'),
                               '--json'], capture_output=True)

    def test_originals_pass(self):
        result = self.run_gate()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout)['stress']['metrics']['peticiones'], '98684')

    def test_each_file_and_hash_change_fails(self):
        for entry in self.manifest['runs'].values():
            for name in entry['sha256']:
                with self.subTest(file=name):
                    path = self.root / name
                    original = path.read_bytes()
                    path.write_bytes(original + b'\n')
                    result = self.run_gate()
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn(b'SHA-256', result.stderr)
                    path.write_bytes(original)
                    digest = entry['sha256'][name]
                    entry['sha256'][name] = '0' * 64
                    self.manifest_path.write_text(json.dumps(self.manifest), encoding='utf-8')
                    self.assertNotEqual(self.run_gate().returncode, 0)
                    entry['sha256'][name] = digest
                    self.manifest_path.write_text(json.dumps(self.manifest), encoding='utf-8')

    def test_metrics_fail_even_if_hash_is_updated(self):
        for run, entry in self.manifest['runs'].items():
            name = next(p for p in entry['sha256'] if p.endswith('_stats.csv'))
            path = self.root / name
            original = path.read_bytes()
            digest = entry['sha256'][name]
            for field in ('Request Count', 'Failure Count', '95%', '99%'):
                with self.subTest(run=run, field=field):
                    with path.open(encoding='utf-8-sig', newline='') as stream:
                        reader = csv.DictReader(stream)
                        headers, rows = reader.fieldnames, list(reader)
                    aggregate = next(row for row in rows if row['Name'] == 'Aggregated')
                    aggregate[field] = str(int(aggregate[field]) + 1)
                    with path.open('w', encoding='utf-8', newline='') as stream:
                        writer = csv.DictWriter(stream, fieldnames=headers)
                        writer.writeheader()
                        writer.writerows(rows)
                    entry['sha256'][name] = hashlib.sha256(path.read_bytes()).hexdigest()
                    self.manifest_path.write_text(json.dumps(self.manifest), encoding='utf-8')
                    result = self.run_gate()
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn(b'referencia declarada', result.stderr)
                    path.write_bytes(original)
                    entry['sha256'][name] = digest
                    self.manifest_path.write_text(json.dumps(self.manifest), encoding='utf-8')


if __name__ == '__main__':
    unittest.main()
