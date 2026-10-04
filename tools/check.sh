#!/usr/bin/env python3
"""Structural sanity check for the Kotlin tree before pushing to CI.

1) Brace/paren/bracket balance (string/comment aware).
2) Duplicate imports.
3) Imports that are never referenced (best-effort, conservative).
"""
import pathlib
import re
import sys

SRC = pathlib.Path("app/src/main/java")
failures = []


def scan_file(path):
    text = path.read_text()
    depth = {"(": 0, "{": 0, "[": 0}
    pairs = {")": "(", "}": "{", "]": "["}
    in_str = None
    in_lc = in_bc = False
    i = 0
    line = 1
    while i < len(text):
        c = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if c == "\n":
            line += 1
            in_lc = False
            i += 1
            continue
        if in_lc:
            i += 1
            continue
        if in_bc:
            if c == "*" and nxt == "/":
                in_bc = False
                i += 2
                continue
            i += 1
            continue
        if in_str:
            if c == "\\":
                i += 2
                continue
            if c == in_str:
                in_str = None
            i += 1
            continue
        if c == "/" and nxt == "/":
            in_lc = True
            i += 2
            continue
        if c == "/" and nxt == "*":
            in_bc = True
            i += 2
            continue
        if c in ('"', "'"):
            in_str = c
            i += 1
            continue
        if c in depth:
            depth[c] += 1
        elif c in pairs:
            depth[pairs[c]] -= 1
            if depth[pairs[c]] < 0:
                failures.append(f"{path}: unbalanced '{c}' at line {line}")
                return
        i += 1
    for k, v in depth.items():
        if v != 0:
            failures.append(f"{path}: unbalanced '{k}' delta {v:+d}")

    # duplicate / unused imports
    seen = {}
    for m in re.finditer(r"^import\s+([\w.]+)\s*$", text, re.M):
        fqn = m.group(1)
        seen[fqn] = seen.get(fqn, 0) + 1
    for fqn, n in seen.items():
        if n > 1:
            failures.append(f"{path}: duplicate import {fqn} ({n}x)")
        simple = fqn.split(".")[-1]
        body = "\n".join(
            l for l in text.splitlines() if not l.strip().startswith("import ")
        )
        # getValue/setValue back the `by remember { ... }` delegate syntax and
        # are required even though the simple name alone never appears.
        if simple in ("getValue", "setValue"):
            if re.search(r"\bby\s+\w+", body):
                continue
        if not re.search(rf"\b{re.escape(simple)}\b", body):
            failures.append(f"{path}: unused import {fqn}")


for path in sorted(SRC.rglob("*.kt")):
    scan_file(path)

if failures:
    print("FAIL (%d):" % len(failures))
    for f in failures:
        print("  x", f)
    sys.exit(1)
print("OK - %d files clean" % len(list(SRC.rglob('*.kt'))))
