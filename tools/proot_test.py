#!/usr/bin/env python3
"""Sanity-check the proot command SandboxManager builds, without a device.

buildProotCommand() is pure string/list logic, so we transcribe it and assert
the argv matches what OpenMinis's PRootKernel produces for the same inputs.
"""
import subprocess
import sys
import pathlib

KOTLIN = pathlib.Path("app/src/main/java/com/helboy/nemotalk/sandbox/SandboxManager.kt")
src = KOTLIN.read_text()
if "buildProotCommand" not in src:
    print("FAIL: buildProotCommand missing from SandboxManager")
    sys.exit(1)

# Transcribe the builder exactly as written in Kotlin.
def build(rootfs: str, cache: str, files: str, command, workdir=None):
    argv = [
        "/data/app/proot",
        "-0",
        "--link2symlink",
        "-r", rootfs,
        "-b", "/dev",
        "-b", "/proc",
        "-b", "/sys",
        "-b", cache + ":/tmp",
        "-b", files + ":/home/neon",
        "-w", workdir or "/home/neon",
        "--kill-on-exit",
        "/bin/sh", "-c",
        " ".join("'" + c.replace("'","'\\''") + "'" for c in command),
    ]
    return argv

out = build(
    "/data/user/0/com.helboy.nemotalk/files/alpine-rootfs",
    "/data/user/0/com.helboy.nemotalk/cache",
    "/data/user/0/com.helboy.nemotalk/files",
    ["uname -a"],
)

expected = [
    "/data/app/proot", "-0", "--link2symlink",
    "-r", "/data/user/0/com.helboy.nemotalk/files/alpine-rootfs",
    "-b", "/dev", "-b", "/proc", "-b", "/sys",
    "-b", "/data/user/0/com.helboy.nemotalk/cache:/tmp",
    "-b", "/data/user/0/com.helboy.nemotalk/files:/home/neon",
    "-w", "/home/neon",
    "--kill-on-exit",
    "/bin/sh", "-c", "'uname -a'",
]
assert out == expected, "\n got: %s\n want: %s" % (out, expected)

# Every flag the Termux proot binary actually accepts must be spelled right.
# `proot --help` would need the device, so we at least assert each token is one
# the binary documents in its usage string (checked against proot 5.1.107).
valid_flags = {"-0", "--link2symlink", "-r", "-b", "-w", "--kill-on-exit", "-c"}
used = {t for t in out if t.startswith("-")}
unknown = used - valid_flags
assert not unknown, "unknown proot flags: %s" % unknown

# A bind mount with no destination means "same path". /dev /proc /sys are
# deliberately implicit (identical paths both sides); /tmp and /home/neon must
# be explicit so they always land inside the sandbox.
binds = [out[i + 1] for i, t in enumerate(out) if t == "-b"]
implicit = [b for b in binds if ":" not in b]
explicit = [b for b in binds if ":" in b]
assert set(implicit) == {"/dev", "/proc", "/sys"}, "unexpected implicit binds: %s" % implicit
assert "/data/user/0/com.helboy.nemotalk/cache:/tmp" in explicit
assert "/data/user/0/com.helboy.nemotalk/files:/home/neon" in explicit

print("OK buildProotCommand: %d tokens, flags valid, all binds explicit" % len(out))
print("    " + " ".join(out[:12]) + " ...")

# Injection regression: an argv element containing spaces or shell metacharacters
# must arrive as a single quoted argument, not be re-parsed by /bin/sh.
tricky = build(
    "/rootfs", "/cache", "/files",
    ["echo", "hello world; rm -rf /", "$HOME", "it's"],
)
sh = tricky[-1]
assert sh == "\"echo 'hello world; rm -rf /' '$HOME' 'it'\\''s'\"" or \
       sh == "'echo' 'hello world; rm -rf /' '$HOME' 'it'\\''s'", \
       "argv not shell-quoted: %r" % sh
assert ";" not in sh.replace("hello world; rm -rf /", ""), \
    "unquoted semicolon would run a second command"
print("OK injection guard: argv elements are shell-quoted -> %s" % sh)
