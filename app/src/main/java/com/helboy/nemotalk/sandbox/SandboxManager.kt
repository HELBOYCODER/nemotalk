package com.helboy.nemotalk.sandbox

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sadaqah: a no-root Linux sandbox for NeMoTalk.
 *
 * Method copied 1:1 from OpenMinis (github.com/OpenMinis/OpenMinis): the Android
 * package installer unpacks every .so under app/src/main/jniLibs and chmods it
 * executable. We therefore ship `libproot.so` + `libloader.so` as fake native
 * libraries — that gives us *binary* access without a rooted device — and call
 * them directly, emulating what OpenMinis's PRootKernel does.
 *
 * `libproot.so` is a plain copy of the Termux proot binary (statically linked
 * against Bionic via /system/bin/linker64), and `libloader.so` is Termux's
 * proot loader. Alpine aarch64 minirootfs ships compressed in assets and is
 * extracted once into the app's private data dir.
 *
 * Result: NeMoTalk gets a full apk/busybox/linux toolchain inside its own
 * process — the agent can compile, run pip/python, and use the same Linux
 * tools Minis itself uses on-device, all without touching VpnService.
 */
class SandboxManager(private val context: Context) {

    companion object {
        private const val TAG = "NeMoTalkSandbox"
        const val ROOTFS_ASSET = "alpine-rootfs.tgz"
        const val ROOTFS_DIR = "alpine-rootfs"
        const val PROOT_LIB = "libproot.so"
        const val LOADER_LIB = "libloader.so"

        // proot kills file caching when /proc isn't there; this is the standard fix.
        private const val PROOT_NO_KILL = "--no-kill-on-exit"
    }

    private val rootfsDir = File(context.filesDir, ROOTFS_DIR)
    private val bootstrapped = AtomicBoolean(false)

    // Prebuilt binary paths (they land here because Android's installer treats
    // jniLibs contents as a native library directory).
    private val prootBinary: File
        get() = File(context.applicationInfo.nativeLibraryDir, PROOT_LIB)

    private val loaderBinary: File
        get() = File(context.applicationInfo.nativeLibraryDir, LOADER_LIB)

    /**
     * Extracts the Alpine rootfs from assets on first run. Idempotent — a
     * later [bootstrap] after extraction is a no-op.
     */
    fun bootstrap(): Boolean {
        if (!bootstrapped.compareAndSet(false, true)) return true
        try {
            if (File(rootfsDir, "bin/busybox").exists()) {
                Log.i(TAG, "rootfs already present at ${rootfsDir.absolutePath}")
                return true
            }
            rootfsDir.mkdirs()
            context.assets.open(ROOTFS_ASSET).use { input ->
                val tmp = File(context.cacheDir, ROOTFS_ASSET)
                tmp.outputStream().use { input.copyTo(it) }
                extractTarGz(tmp, rootfsDir)
                tmp.delete()
            }
            Log.i(TAG, "Alpine rootfs extracted to ${rootfsDir.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "rootfs bootstrap failed", e)
            bootstrapped.set(false)
            return false
        }
    }

    fun isAvailable(): Boolean =
        prootBinary.exists() && loaderBinary.exists() && File(rootfsDir, "bin/busybox").exists()

    /**
     * Runs [command] *inside* the chroot via proot. Blocks until it exits.
     * Returns a pair of (exitCode, capturedStdout). ponytail: one
     * ProcessBuilder with redirectErrorStream covers both the "I need the
     * output" and "I need the exit code" cases — no second implementation.
     */
    fun run(command: List<String>, workdir: String? = null): Pair<Int, String> {
        if (!isAvailable()) {
            Log.w(TAG, "sandbox not available; refusing to run $command")
            return -1 to ""
        }
        val argv = buildProotCommand(command, workdir)
        Log.d(TAG, "exec: ${argv.joinToString(" ")}")
        return runProcess(argv)
    }

    /**
     * Mirrors OpenMinis PRootKernel.buildProotCommand exactly:
     * `<proot> -0 --link2symlink -r <rootfs> -b /dev -b /proc -b /sys -w <cwd>
     *          [-b <host>:<linux> ...] /bin/sh -c "<command>"`
     *
     * --link2symlink is what lets Alpine's apk/busybox work inside proot
     * despite Android refusing to let a normal app create hard links.
     */
    private fun buildProotCommand(command: List<String>, workdir: String?): List<String> {
        return mutableListOf(
            prootBinary.absolutePath,
            "-0",
            "--link2symlink",
            "-r", rootfsDir.absolutePath,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            // Android mounts a world-writable tmpfs here; apk(8) needs a writable
            // /tmp, and we want the agent's own files visible as /home/neon.
            "-b", "${context.cacheDir.absolutePath}:/tmp",
            "-b", "${context.filesDir.absolutePath}:/home/neon",
            "-w", workdir ?: "/home/neon",
            "--kill-on-exit",
            "/bin/sh", "-c"
        ).apply {
            add(command.joinToString(" "))
        }.toList()
    }

    private fun runProcess(argv: List<String>): Pair<Int, String> {
        val pb = ProcessBuilder(argv).redirectErrorStream(true)
        val proc = pb.start()
        val out = StringBuilder()
        Thread {
            proc.inputStream.bufferedReader().forEachLine { out.appendLine(it) }
        }.start()
        val code = proc.waitFor()
        if (out.isNotEmpty()) Log.d(TAG, out.toString().trim())
        return code to out.toString().trim()
    }

    private fun extractTarGz(tarGz: File, dest: File) {
        // ponytail: extracting tar.gz without adding a dependency — zlib is in
        // the JDK (java.util.zip), and tar is a simple enough format for a
        // minimal reader. OpenMinis uses libarchive; we read it by hand.
        java.util.zip.GZIPInputStream(tarGz.inputStream()).use { gz ->
            extractTar(gz, dest)
        }
    }

    /** Minimal POSIX-tar stream reader (ustar + GNU/longname). */
    private fun extractTar(stream: java.io.InputStream, dest: File) {
        val buf = ByteArray(512)
        var pendingName: String? = null
        while (true) {
            var n = 0
            while (n < 512) {
                val r = stream.read(buf, n, 512 - n)
                if (r <= 0) return
                n += r
            }
            if (buf.all { it == 0.toByte() }) return

            val rawName = pendingName ?: String(buf, 0, 100).trimEnd('\u0000').trim()
            pendingName = null
            if (rawName.isEmpty()) continue

            val type = buf[156].toInt().toChar()
            val sizeOct = String(buf, 124, 12).trim { it == ' ' || it == '\u0000' }
            val size = if (sizeOct.isEmpty()) 0L else sizeOct.toLong(8)

            // GNU longname entry — the real filename follows as file data.
            if (type == 'L') {
                // ponytail: read up to 4 KB of name, then skip whatever is
                // left plus the block padding. Using size % 512 here alone
                // would skip nothing when size is already block-aligned and
                // misalign every following entry.
                val nameLen = minOf(size.toInt(), 4096)
                val nameBuf = ByteArray(nameLen)
                var off = 0
                while (off < nameBuf.size) {
                    val r = stream.read(nameBuf, off, nameBuf.size - off)
                    if (r <= 0) break
                    off += r
                }
                skipFully(stream, size - nameLen)
                skipPad(stream, size)
                pendingName = String(nameBuf, 0, off).trimEnd('\u0000')
                continue
            }

            val safePath = sanitize(rawName)
            if (safePath == null) {
                // Refuse the entry, but still consume its payload so the stream
                // stays aligned — dropping the bytes would corrupt every later entry.
                skipFully(stream, size)
                skipPad(stream, size)
                continue
            }

            val target = File(dest, safePath)

            when (type) {
                '5' -> target.mkdirs()
                '2' -> { // symlink
                    target.parentFile?.mkdirs()
                    val linkName = String(buf, 157, 100).trimEnd('\u0000')
                    // Absolute targets (e.g. /bin/sh -> /bin/busybox) are the
                    // norm in Alpine: 306 of 335 symlinks use them, including
                    // /bin/sh itself, which proot needs to exec anything.
                    // Rewriting them to relative keeps them inside the rootfs.
                    val resolved = resolveLink(safePath, linkName)
                    runCatching { target.delete() }
                    try {
                        java.nio.file.Files.createSymbolicLink(
                            target.toPath(), java.nio.file.Paths.get(resolved)
                        )
                    } catch (e: Exception) {
                        Log.d(TAG, "symlink skipped: $safePath -> $resolved")
                    }
                }
                else -> {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out ->
                        var left = size
                        val chunk = ByteArray(8192)
                        while (left > 0) {
                            val want = minOf(chunk.size.toLong(), left).toInt()
                            val r = stream.read(chunk, 0, want)
                            if (r <= 0) break
                            out.write(chunk, 0, r)
                            left -= r
                        }
                    }
                    // Keep executables executable.
                    if (buf[156].toInt() != 0) {
                        val mode = String(buf, 100, 8).trim().takeIf { it.isNotEmpty() }
                        mode?.toLongOrNull(8)?.let { m ->
                            target.setExecutable(m and 0x100L != 0L, false)
                        }
                    }
                }
            }
            skipPad(stream, size)
        }
    }

    private fun skipPad(stream: java.io.InputStream, size: Long) {
        val rem = size % 512
        if (rem > 0) skipFully(stream, 512 - rem)
    }

    private fun skipFully(stream: java.io.InputStream, n: Long) {
        var left = n
        while (left > 0) {
            val r = stream.skip(left)
            if (r <= 0) break
            left -= r
        }
    }

    /**
     * Blocks path escape — any entry containing .. or an absolute path is
     * dropped rather than written outside [dest].
     */
    private fun sanitize(name: String): String? {
        if (name.startsWith("/")) return null
        if (name.contains("..")) return null
        return name
    }

    /**
     * Converts an absolute symlink target (Alpine's default) into a relative
     * one that stays inside the rootfs. `linkName` is taken as root-relative:
     * /bin/busybox from bin/sh becomes "busybox" (same directory), and from
     * usr/bin/yes becomes "../../bin/busybox".
     */
    private fun resolveLink(source: String, linkName: String): String {
        if (!linkName.startsWith("/")) return linkName
        // Depth = how deep the link itself sits: bin/sh is one level down, so
        // its parent bin/ is at depth 0 and needs no ".." at all.
        val parts = source.trim('/').split("/").filter { it.isNotEmpty() }
        val depth = (parts.size - 1).coerceAtLeast(0)
        val rel = linkName.trimStart('/')
        return if (depth == 0) rel else ("../".repeat(depth) + rel)
    }
}
