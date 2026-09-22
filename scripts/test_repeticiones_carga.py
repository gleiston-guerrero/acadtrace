"""Mutaciones de evidencia en copias temporales; nunca modifica originales."""
import csv
import json
from pathlib import Path
import shutil
import tempfile
import unittest
import resumir_repeticiones_carga as gate


class RepeticionesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        for names in gate.SELECTED.values():
            for name in names:
                shutil.copytree(gate.DEFAULT / name, self.base / name)
        for name in ('resumen_repeticiones.json', 'resumen_repeticiones.md', 'SHA256SUMS.txt'):
            shutil.copyfile(gate.DEFAULT / name, self.base / name)

    def test_originals_and_read_only_check(self):
        before = {p.relative_to(self.base): p.read_bytes() for p in self.base.rglob('*') if p.is_file()}
        result = gate.generate(self.base, check=True)
        self.assertEqual([result['escenarios'][k]['n_validas'] for k in ('nominal','estres')], [3,3])
        self.assertEqual(result['metodo']['semilla'], 12345)
        self.assertEqual(result['metodo']['remuestras'], 10000)
        self.assertEqual(before, {p.relative_to(self.base): p.read_bytes() for p in self.base.rglob('*') if p.is_file()})

    def test_raw_byte_mutation_rejected(self):
        p = self.base / 'nominal_06/stats.csv'
        p.write_bytes(p.read_bytes() + b'\n')
        with self.assertRaisesRegex(ValueError, 'SHA-256'):
            gate.generate(self.base, check=True)

    def test_manifest_cannot_omit_file(self):
        p = self.base / 'SHA256SUMS.txt'
        p.write_bytes(b'\n'.join(p.read_bytes().splitlines()[1:]) + b'\n')
        with self.assertRaisesRegex(ValueError, 'SHA-256'):
            gate.generate(self.base, check=True)

    def test_changed_percentile_rejected_even_with_updated_hash(self):
        p = self.base / 'nominal_06/stats.csv'
        with p.open(newline='', encoding='utf-8-sig') as f:
            reader = csv.DictReader(f)
            headers, rows = reader.fieldnames, list(reader)
        aggregate = next(r for r in rows if r['Name']=='Aggregated')
        aggregate['99%'] = str(float(aggregate['99%']) + 1)
        old_hash = gate.hashlib.sha256(p.read_bytes()).hexdigest()
        with p.open('w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=headers)
            writer.writeheader(); writer.writerows(rows)
        new_hash = gate.hashlib.sha256(p.read_bytes()).hexdigest()
        manifest = self.base / 'SHA256SUMS.txt'
        manifest.write_bytes(manifest.read_bytes().replace(old_hash.encode(), new_hash.encode()))
        with self.assertRaisesRegex(ValueError, 'resumen publicado'):
            gate.generate(self.base, check=True)

    def test_summary_cannot_change_seed_or_metrics(self):
        p = self.base / 'resumen_repeticiones.json'
        original = p.read_bytes()
        for field in ('seed', 'metric'):
            with self.subTest(field=field):
                data = json.loads(original)
                if field == 'seed':
                    data['metodo']['semilla'] = 1
                else:
                    data['escenarios']['nominal']['metricas']['rps']['media'] += 1
                p.write_text(json.dumps(data), encoding='utf-8')
                with self.assertRaisesRegex(ValueError, 'resumen publicado'):
                    gate.generate(self.base, check=True)
                p.write_bytes(original)

    def test_missing_run_cannot_generate_partial_summary(self):
        p = self.base / 'estres_06/console.log'
        p.unlink()
        before = (self.base / 'resumen_repeticiones.json').read_bytes()
        with self.assertRaisesRegex(ValueError, r'3\+3'):
            gate.generate(self.base)
        self.assertEqual(before, (self.base / 'resumen_repeticiones.json').read_bytes())

    def test_invalid_runs_excluded_without_reading_metrics(self):
        excluded = self.base / 'estres_04_configuracion_invalida'
        excluded.mkdir()
        (excluded/'stats.csv').write_bytes(b'not a CSV')
        result = gate.generate(self.base, check=True)
        self.assertNotIn(excluded.name, [r['corrida'] for r in result['corridas']])

    def test_semantic_mutations_fail_before_hashes(self):
        directory = self.base / 'nominal_06'
        for filename, column, value, which in (
            ('stats.csv','Failure Count','1','aggregate'),
            ('stats.csv','Request Count','1','aggregate'),
            ('stats_history.csv','User Count','51','last'),
            ('stats_history.csv','Timestamp',None,'last'),
            ('failures.csv','Occurrences','1','new'),
            ('exceptions.csv','Count','1','new'),
        ):
            with self.subTest(file=filename, column=column):
                p = directory / filename
                original = p.read_bytes()
                with p.open(newline='', encoding='utf-8-sig') as f:
                    reader = csv.DictReader(f)
                    headers, rows = reader.fieldnames, list(reader)
                if which == 'new':
                    row = {key:'' for key in headers}
                    rows.append(row)
                else:
                    candidates = [r for r in rows if r['Name']=='Aggregated']
                    row = candidates[-1] if which == 'last' else candidates[0]
                row[column] = value if value is not None else str(int(row[column])+20)
                with p.open('w', newline='', encoding='utf-8') as f:
                    writer = csv.DictWriter(f, fieldnames=headers)
                    writer.writeheader(); writer.writerows(rows)
                self.assertFalse(gate.validate_run(directory)['valida'])
                p.write_bytes(original)

    def test_console_exit_and_release_mutations(self):
        directory = self.base / 'estres_06'
        for filename, content in (
            ('console.log', b''), ('console.log', b'Error 401\n'),
            ('console.log', b'HTTP: 500\n'), ('console.log', b'HTTP: 503\n'),
            ('EXIT_CODE.txt', b'EXIT_CODE=1\n'),
            ('perfil.txt', (directory/'perfil.txt').read_bytes().replace(b'INHIBIDOR_LIBERADO=True',b'INHIBIDOR_LIBERADO=False')),
        ):
            with self.subTest(file=filename, content=content[:20]):
                p = directory / filename
                original = p.read_bytes(); p.write_bytes(content)
                self.assertFalse(gate.validate_run(directory)['valida'])
                p.write_bytes(original)


if __name__ == '__main__':
    unittest.main()
