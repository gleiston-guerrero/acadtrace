// Only publish sanitized metadata; never forward scanner output or raw reports.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createHash } = require('node:crypto');
const { spawnSync, execFileSync } = require('node:child_process');

function historyIdentity(item) {
  const file = item.File.replaceAll('\\', '/');
  const metadata = [item.Commit, file, item.RuleID, item.StartLine,
    item.EndLine, item.StartColumn, item.EndColumn];
  if (!/^[a-f0-9]{40}$/.test(item.Commit) || metadata.some(value => value === undefined)) {
    throw new Error('Invalid finding metadata');
  }
  return { id: createHash('sha256').update(JSON.stringify(metadata)).digest('hex'),
    path: file, type: item.RuleID };
}

function compareHistory(findings, baseline) {
  if (baseline.version !== 1 || baseline.scanner !== '8.18.0' ||
      !Array.isArray(baseline.findings) || baseline.findings.length !== 11) {
    throw new Error('Invalid historical debt baseline');
  }
  const remaining = new Map();
  const key = item => JSON.stringify([item.id, item.path, item.type]);
  for (const item of baseline.findings) {
    if (!/^[a-f0-9]{64}$/.test(item.id) || typeof item.path !== 'string' ||
        typeof item.type !== 'string') throw new Error('Invalid baseline entry');
    remaining.set(key(item), (remaining.get(key(item)) || 0) + 1);
  }
  const classified = findings.map(item => {
    const count = remaining.get(key(item)) || 0;
    if (count) remaining.set(key(item), count - 1);
    return { ...item, classification: count ? 'known-pending' : 'new' };
  });
  const added = classified.filter(item => item.classification === 'new').length;
  return { findings: classified, known: findings.length - added, added,
    absent: [...remaining.values()].reduce((sum, count) => sum + count, 0),
    exitCode: added ? 2 : 0 };
}

const mode = process.argv[2];
if (!['tree', 'history', 'self-test', 'self-test-history'].includes(mode)) {
  console.error('Usage: node scripts/scan-secrets.cjs tree|history|self-test|self-test-history');
  process.exit(1);
}
const root = process.cwd();
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'acadtrace-secrets-'));
let status = 1;
try {
  const baselinePath = path.join(root, 'docs/seguridad/gitleaks-history-baseline.json');
  if (mode === 'self-test-history') {
    const baseline = JSON.parse(fs.readFileSync(baselinePath, 'utf8'));
    const clean = compareHistory(baseline.findings, baseline);
    const original = { Commit: '1'.repeat(40), File: 'same-path.txt', RuleID: 'generic-api-key',
      StartLine: 1, EndLine: 1, StartColumn: 1, EndColumn: 40 };
    const changed = { ...original, Commit: '2'.repeat(40) };
    const probe = { ...baseline.findings[0], id: historyIdentity(changed).id };
    const extra = compareHistory([...baseline.findings, probe], baseline);
    const duplicate = compareHistory([...baseline.findings, baseline.findings[0]], baseline);
    const replaced = compareHistory([probe, ...baseline.findings.slice(1)], baseline);
    const absent = compareHistory([], baseline);
    if (clean.exitCode !== 0 || clean.known !== 11 || extra.exitCode !== 2 ||
        extra.added !== 1 || duplicate.exitCode !== 2 || replaced.exitCode !== 2 ||
        absent.absent !== 11 || historyIdentity(original).id === historyIdentity(changed).id) {
      throw new Error('Historical regression test failed');
    }
    console.log('Historical baseline test: PASS; new/replaced/extra occurrence rejected (exit 2).');
    status = 0;
  } else {
    let source = root;
    if (mode !== 'history') {
      source = path.join(temporary, 'tree');
      fs.mkdirSync(source);
      const files = execFileSync('git', ['ls-files', '-z'], { cwd: root })
        .toString('utf8').split('\0').filter(Boolean);
      for (const file of files) {
        const original = path.join(root, file);
        if (!fs.existsSync(original)) continue; // Locally deleted tracked file.
        if (!fs.lstatSync(original).isFile()) throw new Error('Unsupported tracked entry');
        const destination = path.join(source, file);
        fs.mkdirSync(path.dirname(destination), { recursive: true });
        fs.copyFileSync(original, destination);
      }
      // Include the new helper and metadata baseline before they enter the index.
      fs.copyFileSync(__filename, path.join(source, 'scripts', 'scan-secrets.cjs'));
      fs.copyFileSync(baselinePath, path.join(source, 'docs/seguridad/gitleaks-history-baseline.json'));
      if (mode === 'self-test') {
        // Synthetic value, created only in the temporary snapshot.
        fs.writeFileSync(path.join(source, 'secret-gate-probe.txt'),
          'github_token = "' + 'ghp_' + 'Ab9Cd8Ef7Gh6Ij5Kl4Mn3Op2Qr1St0Uv9Wx8' + '"\n');
      }
    } else if (execFileSync('git', ['rev-parse', '--is-shallow-repository'])
      .toString().trim() !== 'false') {
      throw new Error('Full history required');
    }
    const report = path.join(temporary, 'report.json');
    const args = ['detect', '--source', source, '--config', path.join(root, '.gitleaks.toml'),
      '--redact', '--exit-code', '2', '--report-format', 'json', '--report-path', report];
    args.push(mode === 'history' ? '--log-opts=--all --full-history -m' : '--no-git');
    const result = spawnSync(process.env.GITLEAKS_BIN || 'gitleaks', args,
      { cwd: root, stdio: 'ignore' });
    if (result.error || ![0, 2].includes(result.status) || !fs.existsSync(report)) {
      throw new Error('Scanner failed');
    }
    const findings = JSON.parse(fs.readFileSync(report, 'utf8')) || [];
    if (!Array.isArray(findings) || (findings.length > 0) !== (result.status === 2)) {
      throw new Error('Inconsistent scan result');
    }
    const comparison = mode === 'history' ? compareHistory(findings.map(historyIdentity),
      JSON.parse(fs.readFileSync(baselinePath, 'utf8'))) : null;
    const safe = findings.map(item => ({
      path: path.isAbsolute(item.File) ? path.relative(source, item.File) : item.File,
      type: item.RuleID,
    }));
    if (process.env.SECRET_SCAN_REPORT_DIR && mode !== 'self-test') {
      fs.mkdirSync(process.env.SECRET_SCAN_REPORT_DIR, { recursive: true });
      fs.writeFileSync(path.join(process.env.SECRET_SCAN_REPORT_DIR, `${mode}.json`),
        JSON.stringify(comparison || safe, null, 2) + '\n');
    }
    if (mode === 'self-test') {
      const detected = safe.some(item => item.path.replaceAll('\\', '/') === 'secret-gate-probe.txt');
      status = result.status === 2 && detected ? 0 : 1;
      console.log(`Controlled mutation: ${status === 0 ? 'PASS (gate exit 2)' : 'FAIL'}`);
    } else if (comparison) {
      status = comparison.exitCode;
      console.log(`history: ${comparison.known} known pending; ${comparison.added} new; ` +
        `${comparison.absent} baseline occurrences absent (not proof of resolution); ` +
        `scanner exit ${result.status}; gate exit ${status}.`);
      if (comparison.known) {
        console.log('WARNING: historical debt remains; history not clean, rotations not verified.');
      }
    } else {
      status = result.status;
      console.log(`${mode}: ${safe.length} findings; gate exit ${status}.`);
    }
  }
} catch {
  console.error('Secret scan could not complete; gate closed. Raw diagnostics suppressed.');
} finally {
  fs.rmSync(temporary, { recursive: true, force: true });
}
process.exitCode = status;
