#!/usr/bin/env python3
"""E17: comprueba bytes Git (HEAD por defecto), sin filtros de checkout."""
import argparse
from pathlib import Path
import subprocess
import sys

EXTENSIONS = {
    ".csv", ".tsv", ".sql", ".md", ".yml", ".yaml", ".py", ".java",
    ".js", ".jsx", ".json", ".xml", ".properties", ".sh", ".tex",
}


def git(*args, data=None):
    return subprocess.check_output(["git", *args], input=data)


def inspect(source):
    root = Path(git("rev-parse", "--show-toplevel").decode().strip())
    records = git("ls-tree", "-r", "-z", "HEAD").split(b"\0")
    entries = []
    for record in filter(None, records):
        metadata, name = record.split(b"\t", 1)
        mode, kind, oid = metadata.split()
        if kind == b"blob" and mode != b"120000" and Path(name.decode()).suffix.lower() in EXTENSIONS:
            entries.append((name, oid))
    # Consultar atributos efectivos; las reglas versionadas deben exigir LF.
    attributes = git("check-attr", "-z", "--stdin", "eol", "text",
                     data=b"\0".join(name for name, _ in entries) + b"\0").split(b"\0")
    attrs = {}
    for i in range(0, len(attributes) - 1, 3):
        name, key, value = attributes[i:i + 3]
        attrs.setdefault(name, {})[key] = value
    entries = [(name, oid) for name, oid in entries
               if attrs[name].get(b"eol") == b"lf" and attrs[name].get(b"text") != b"unset"]
    blobs = b""
    if source == "HEAD":
        blobs = git("cat-file", "--batch", data=b"\n".join(oid for _, oid in entries) + b"\n")
    cursor = 0
    bad = []
    for name, _ in entries:
        path = name.decode()
        if source == "HEAD":
            end = blobs.index(b"\n", cursor)
            size = int(blobs[cursor:end].split()[2])
            data = blobs[end + 1:end + 1 + size]
            cursor = end + 2 + size
        else:
            data = (root / path).read_bytes()
        if b"\r\n" in data:
            bad.append(path)
            print(f"ERROR CRLF: {path}")
    print(f"{source}: revisados={len(entries)}; sin CRLF={len(entries)-len(bad)}; CRLF={len(bad)}")
    return 1 if bad else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", choices=("HEAD", "worktree"), default="HEAD")
    args = parser.parse_args()
    try:
        return inspect(args.source)
    except (OSError, ValueError, subprocess.CalledProcessError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
