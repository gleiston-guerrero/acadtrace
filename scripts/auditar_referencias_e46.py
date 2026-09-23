"""Sanitized audit of tracked files and APK entries. Never prints endpoint values.

The reference commits are pre-remediation evidence, not credentials. After an
authorized rewrite, replace this evidence source with an external protected policy.
"""
import argparse
import hashlib
import ipaddress
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import urlsplit
import zipfile

IP = re.compile(rb'(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])')
HTTP = re.compile(rb'http://[^\s<>"\x27\\)]+')
def git(*args):
    return subprocess.check_output(['git', *args], stderr=subprocess.DEVNULL)


def public(value):
    try:
        return ipaddress.ip_address(value.decode('ascii')).is_global
    except (ValueError, UnicodeError):
        return False


def production_literals(policy_path=None):
    # After the authorized history rewrite, production references must not be
    # recovered from obsolete commits. When exact historical references are
    # required, load them from a protected file outside the repository.
    if policy_path is None:
        return []

    path = Path(policy_path)
    if not path.is_file():
        raise SystemExit('Production-reference policy file does not exist.')

    values = set()
    for raw in path.read_text(encoding='utf-8').splitlines():
        value = raw.strip()
        if not value or value.startswith('#'):
            continue
        try:
            encoded = value.encode('ascii')
        except UnicodeEncodeError:
            raise SystemExit('Production-reference policy contains a non-ASCII value.')
        if not IP.fullmatch(encoded) or not public(encoded):
            raise SystemExit('Production-reference policy contains an invalid public IPv4 literal.')
        values.add(encoded)

    if len(values) != 2:
        raise SystemExit('Expected exactly two production reference literals in protected policy.')

    return sorted(values)


def audit_tree(literals):
    rows = []
    for name in git('ls-files', '-z').decode('utf-8').split('\0'):
        if not name or not Path(name).is_file():
            continue
        data = Path(name).read_bytes()
        prod = sum(data.count(value) + data.count(value.decode().encode('utf-16le')) for value in literals)
        candidates = list(IP.finditer(data))
        public_hits = [m for m in candidates if public(m.group())]
        versions = 0
        unknown = 0
        for match in public_hits:
            if match.group() in literals:
                continue
            prefix = data[max(0, match.start()-25):match.start()]
            suffix = data[match.end():match.end()+20]
            is_version = (prefix.endswith(b'jdk-') and suffix.startswith(b'-hotspot')) or (
                prefix.endswith((b'annotations\\', b'annotations/', b'annotations-')) and
                suffix.startswith((b'\\annotations-', b'/annotations-', b'.jar')))
            if is_version:
                versions += 1  # Explicit library/JDK version context; not a test-IP exception.
            else:
                unknown += 1
        nonlocal_http = 0
        for match in HTTP.finditer(data):
            try:
                host = urlsplit(match.group().decode('ascii')).hostname
                if not host or host == 'localhost' or host.endswith(('.localhost', '.invalid', '.test', '.example')):
                    continue
                try:
                    if not ipaddress.ip_address(host).is_global:
                        continue
                except ValueError:
                    pass
                nonlocal_http += 1
            except (ValueError, UnicodeError):
                continue
        if prod or public_hits or nonlocal_http:
            rows.append({'path': name, 'production_server_occurrences': prod,
                         'public_ipv4_candidates': len(public_hits),
                         'library_version_occurrences': versions,
                         'unclassified_public_ipv4': unknown,
                         'nonlocal_http_occurrences': nonlocal_http,
                         'action': 'REVIEW' if prod or unknown else 'PRESERVE; no production literal; HTTP references require context review'})
    return {'total_public_ipv4_candidates': sum(r['public_ipv4_candidates'] for r in rows),
            'classified_library_version_occurrences': sum(r['library_version_occurrences'] for r in rows),
            'total_public_ipv4_addresses': sum(r['public_ipv4_candidates'] - r['library_version_occurrences'] for r in rows),
            'production_server_literal_occurrences': sum(r['production_server_occurrences'] for r in rows),
            'public_ipv4_unclassified': sum(r['unclassified_public_ipv4'] for r in rows),
            'rows': rows}


def audit_apk(path, literals):
    entries = []
    with zipfile.ZipFile(path) as apk:
        for name in apk.namelist():
            data = apk.read(name)
            count = sum(data.count(value) + data.count(value.decode().encode('utf-16le')) for value in literals)
            if count:
                entries.append({'entry': name, 'occurrences': count})
    return {'path': str(path), 'sha256': hashlib.sha256(Path(path).read_bytes()).hexdigest(),
            'production_server_literal_occurrences': sum(r['occurrences'] for r in entries),
            'entries': entries,
            'scope': 'all ZIP entries, including every DEX and resource; ASCII/UTF-8 and UTF-16LE literals'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--production-literals-file', type=Path)
    args = parser.parse_args()

    policy_path = args.production_literals_file
    if policy_path is None:
        external = os.environ.get('E46_PRODUCTION_LITERALS_FILE')
        if external:
            policy_path = Path(external)

    literals = production_literals(policy_path)

    if args.apk and not literals:
        raise SystemExit(
            'APK audit requires --production-literals-file or '
            'E46_PRODUCTION_LITERALS_FILE pointing to protected external policy.'
        )

    result = audit_apk(args.apk, literals) if args.apk else audit_tree(literals)
    result['production_policy_loaded'] = bool(literals)
    result['production_literal_count'] = len(literals)
    text = json.dumps(result, indent=2, ensure_ascii=True) + '\n'
    if args.output:
        args.output.write_text(text, encoding='utf-8')
    print(json.dumps({key: value for key, value in result.items() if key not in ('rows', 'entries')}))
    return 2 if result['production_server_literal_occurrences'] or result.get('public_ipv4_unclassified') else 0


if __name__ == '__main__':
    raise SystemExit(main())
