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
  // Validate baseline shape
  if (baseline.version !== 1 || baseline.scanner !== '8.18.0' ||
      !Array.isArray(baseline.findings) || baseline.findings.length !== 11) {
    throw new Error('Invalid historical debt baseline');
  }

  // Index baseline entries by id and by path+type
  const byId = new Map();
  const byKey = new Map(); // key = `${path}|${type}`
  const key = item => `${item.path}|${item.type}`;

  for (const item of baseline.findings) {
    if (!/^[a-f0-9]{64}$/.test(item.id) || typeof item.path !== 'string' ||
        typeof item.type !== 'string') {
      throw new Error('Invalid baseline entry');
    }
    byId.set(item.id, (byId.get(item.id) || 0) + 1);
    const k = key(item);
    byKey.set(k, (byKey.get(k) || 0) + 1);
  }

  const classified = findings.map(item => {
    const idMatch = byId.get(item.id) || 0;
    const k = key(item);
    const keyMatch = byKey.get(k) || 0;

    let classification = 'new';
    if (idMatch) {
      classification = 'known';
      byId.set(item.id, idMatch - 1);
      byKey.set(k, keyMatch - 1);
    } else if (keyMatch) {
      classification = 'replaced';
      byKey.set(k, keyMatch - 1);
    } else {
      classification = 'extra';
    }
    return { ...item, classification };
  });

  const counts = {
    known: classified.filter(i => i.classification === 'known').length,
    new: classified.filter(i => i.classification === 'new').length,
    replaced: classified.filter(i => i.classification === 'replaced').length,
    extra: classified.filter(i => i.classification === 'extra').length,
  };

  const scannerExitCode = findings.length ? 2 : 0;
  const gateExitCode = (counts.new || counts.replaced || counts.extra) ? 2 : 0;

  return {
    findings: classified,
    ...counts,
    scannerExitCode,
    gateExitCode,
  };
}

const mode = process.argv[2];
if (!['tree', 'history', 'self-test', 'self-test-history'].includes(mode)) {
  console.error('Usage: node scripts/scan-secrets.cjs tree|history|self-test|self-test-history');
  process.exit(1);
}
const root = process.cwd();
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'acadtrace-secrets-'));
// reviewFiles removed – scanner now relies solely on git ls-files for snapshot

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

    // Sanitized diagnostic output for each probe
    const logProbe = (name, res) => {
      console.log(`${name}: known=${res.known} new=${res.new} replaced=${res.replaced} extra=${res.extra} gateExitCode=${res.gateExitCode}`);
    };
    logProbe('clean', clean);
    logProbe('extra probe', extra);
    logProbe('duplicate probe', duplicate);
    logProbe('replaced probe', replaced);
    logProbe('absent probe', absent);

    if (
      clean.gateExitCode !== 0 ||
      clean.known !== 11 ||
      extra.gateExitCode !== 2 ||
      duplicate.gateExitCode !== 2 ||
      replaced.gateExitCode !== 2 ||
      historyIdentity(original).id === historyIdentity(changed).id
    ) {
      throw new Error('Historical regression test failed');
    }
    // Exercise the actual history detector in an isolated disposable repository.
    // No objects, refs, index entries or commits are written to the user's repository.
    const fixture = path.join(temporary, 'history-probe');
    fs.mkdirSync(fixture);
    execFileSync('git', ['init', '--quiet', fixture], { stdio: 'ignore' });
    const value = 'github_token = "' + 'ghp_' + 'Ab9Cd8Ef7Gh6Ij5Kl4Mn3Op2Qr1St0Uv9Wx8' + '"\n';
    const stream = `blob\nmark :1\ndata ${Buffer.byteLength(value)}\n${value}\n` +
      'commit refs/heads/probe\ncommitter Synthetic Test <probe@example.invalid> 1 +0000\n' +
      'data 6\nprobe\n\nM 100644 :1 probe.txt\n\ndone\n';
    execFileSync('git', ['-C', fixture, 'fast-import', '--quiet'], { input: stream, stdio: ['pipe', 'ignore', 'ignore'] });
    const fixtureReport = path.join(temporary, 'history-probe.json');
    const probeResult = spawnSync(process.env.GITLEAKS_BIN || 'gitleaks', [
      'detect', '--source', fixture, '--config', path.join(root, '.gitleaks.toml'),
      '--redact', '--exit-code', '2', '--report-format', 'json', '--report-path', fixtureReport,
      '--log-opts=--all --full-history -m',
    ], { stdio: 'ignore' });
    if (probeResult.status !== 2 || !fs.existsSync(fixtureReport) ||
        !JSON.parse(fs.readFileSync(fixtureReport, 'utf8')).some(item => item.File === 'probe.txt')) {
      throw new Error('Historical detector mutation failed');
    }
    console.log('Historical mutation: PASS; actual detector exit 2; known/new/replaced/extra occurrence rejected.');
    status = 0;
  } else {
    let source = root;
    if (mode !== 'history') {
      source = path.join(temporary, 'tree');
      fs.mkdirSync(source);
      const files = [...new Set(execFileSync('git', ['ls-files', '-z'], { cwd: root }).toString('utf8').split('\0').filter(Boolean))];
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
    if (comparison) {
      comparison.findings.forEach((item, index) => {
        item.commit = findings[index].Commit;
        item.line = findings[index].StartLine;
      });
    }
    if (process.env.SECRET_SCAN_REPORT_DIR && mode !== 'self-test') {
      fs.mkdirSync(process.env.SECRET_SCAN_REPORT_DIR, { recursive: true });
      fs.writeFileSync(path.join(process.env.SECRET_SCAN_REPORT_DIR, `${mode}.json`),
        JSON.stringify(comparison || safe, null, 2) + '\n');
    }
    if (mode === 'self-test') {
      const detected = safe.some(item => item.path.replaceAll('\\\\', '/') === 'secret-gate-probe.txt');
      // scanner must have reported findings (status 2) and our synthetic file must be detected
      status = result.status === 2 && detected ? 0 : 1;
      console.log(`Controlled mutation: ${status === 0 ? 'PASS (gate exit 2)' : 'FAIL'}`);
    } else if (mode === 'history') {
      // Use gateExitCode from comparison for CI decision
      status = comparison ? comparison.gateExitCode : 1;
      console.log(`history: ${comparison.known} known pending; ${comparison.new} new; ${comparison.replaced} replaced; ${comparison.extra} extra; scanner exit ${result.status}; gate exit ${status}.`);
      if (comparison.known) {
        console.log('NOTE: historical debt remains; history not clean, rotations not verified.');
      }
    } else {
      // tree mode – gate mirrors scanner exit code
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
