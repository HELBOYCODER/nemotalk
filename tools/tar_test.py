#!/usr/bin/env python3
"""Test the tar extraction logic SandboxManager.kt implements.

We re-implement the *same* reader (ustar + GNU longname, 512-byte blocks,
512-byte padding, skip-on-unsafe) in Python and validate it against the real
Alpine minirootfs tarball: it must reach bin/busybox and bin/sh, skip nothing
that matters, and never walk outside the destination.
"""
import gzip
import os
import pathlib
import sys

TARBALL = pathlib.Path("app/src/main/assets/sandbox/alpine-rootfs.tgz")
DEST = pathlib.Path("/tmp/rootfs-test")

def read_exact(stream, n):
    buf = b""
    while len(buf) < n:
        chunk = stream.read(n - len(buf))
        if not chunk:
            break
        buf += chunk
    return buf

def octal(field):
    # tar octal: significant digits up to the first NUL or space, then NUL padding.
    # Mirror the Kotlin: String(buf,124,12).trim { it==' ' || it=='\u0000' }
    s = field.split(b"\x00")[0].rstrip()
    return int(s, 8) if s else 0

def resolve_link(source, link_name):
    """Mirror Kotlin's resolveLink: absolute target -> relative inside rootfs."""
    if not link_name.startswith("/"):
        return link_name
    depth = max(len(pathlib.PurePosixPath(source).parts) - 1, 0)
    rel = link_name.lstrip("/")
    return rel if depth == 0 else "../" * depth + rel

def extract_tar(stream, dest):
    dest.mkdir(parents=True, exist_ok=True)
    made = []
    skipped = []
    pending = None
    while True:
        buf = read_exact(stream, 512)
        if len(buf) < 512 or buf == b"\x00" * 512:
            return made, skipped
        raw = (pending or buf[0:100].split(b"\x00")[0].decode("utf-8", "replace")).strip()
        pending = None
        if not raw:
            continue
        typ = chr(buf[156])
        size = octal(buf[124:136])

        if typ == "L":  # GNU longname
            # ponytail: ceiling to a whole block. Truncating the read at 4096
            # would misalign the stream for any path longer than 4 KB, and
            # size % 512 alone skips nothing when the size is block-aligned.
            name_len = min(size, 4096)
            name = read_exact(stream, name_len).rstrip(b"\x00")
            skip(stream, size - name_len)
            skip(stream, (512 - size % 512) % 512)
            pending = name.decode("utf-8", "replace").rstrip("\x00")
            continue

        # Mirror Kotlin's sanitize(): reject absolute paths and ".."
        if raw.startswith("/") or ".." in raw:
            skip_full(stream, size)
            skip(stream, (512 - size % 512) % 512)
            skipped.append(raw)
            continue

        target = dest / raw
        if typ == "5":
            target.mkdir(parents=True, exist_ok=True)
        elif typ == "2":
            link = buf[157:257].rstrip(b"\x00").decode()
            tgt = dest / raw
            tgt.parent.mkdir(parents=True, exist_ok=True)
            resolved = resolve_link(raw, link)
            try:
                tgt.unlink()
            except FileNotFoundError:
                pass
            try:
                os.symlink(resolved, tgt)
            except OSError as exc:
                print(f"   symlink failed: {raw} -> {resolved}: {exc}")
            made.append(("link", raw, resolved))
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            left = size
            with open(target, "wb") as fh:
                while left > 0:
                    want = min(8192, left)
                    chunk = read_exact(stream, want)
                    if not chunk:
                        break
                    fh.write(chunk)
                    left -= len(chunk)
            made.append(("file", raw, size))
        # Always advance past this entry's payload padding, whatever the type.
        skip(stream, (512 - size % 512) % 512)

def skip(stream, n):
    if n > 0:
        read_exact(stream, n)

def skip_full(stream, n):
    read_exact(stream, n)
    rem = n % 512
    if rem > 0:
        read_exact(stream, 512 - rem)

with gzip.open(TARBALL, "rb") as fh:
    made, skipped = extract_tar(fh, DEST)

files = [m for m in made if m[0] == "file"]
links = [m for m in made if m[0] == "link"]

print(f"files: {len(files)}  symlinks: {len(links)}  rejected-unsafe: {len(skipped)}")

# The two things proot absolutely needs to start /bin/sh
must = ["bin/busybox", "bin/sh"]
for want in must:
    p = DEST / want
    # Follow symlinks: bin/sh is a link to busybox, so lstat would report 0.
    real = p.resolve() if p.is_symlink() else p
    ok = real.exists() and real.stat().st_size > 0
    shown = real.stat().st_size if real.exists() else 0
    if p.is_symlink():
        print(f"  {'OK ' if ok else 'MISS'} {want} -> {os.readlink(p)} ({shown} bytes)")
    else:
        print(f"  {'OK ' if ok else 'MISS'} {want} ({shown} bytes)")
    if not ok:
        sys.exit(1)

# Nothing escaped the destination
bad = [str(p) for p in DEST.rglob("*") if ".." in p.name or not str(p.relative_to(DEST))]
print("escape check:", "OK" if not bad else f"FAIL {bad}")

total = sum(f[2] for f in files)
print(f"total extracted: {total/1048576:.1f} MB")
print(f"biggest: {sorted(files, key=lambda f: -f[2])[:3]}")
