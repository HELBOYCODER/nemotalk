package com.helboy.nemotalk.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EmbeddedPersianTtsEngine(private val context: Context) {

    private val tag = "NeMoTalkTTS"
    private var offlineTts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPlayJob: Job? = null

    init {
        scope.launch {
            initEngine()
        }
    }

    private suspend fun initEngine() = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Initializing Embedded Neural Persian TTS Engine (Piper VITS Amir INT8)...")

            // 1. Ensure espeak-ng-data directory is copied to local filesystem (required by C++ espeak-ng)
            val modelSubdir = "vits-piper-fa_IR-amir-medium-int8"
            val targetDataDir = File(context.filesDir, "$modelSubdir/espeak-ng-data")

            if (!targetDataDir.exists() || targetDataDir.list().isNullOrEmpty()) {
                Log.d(tag, "Extracting espeak-ng-data to internal storage...")
                copyAssetFolder("$modelSubdir/espeak-ng-data", targetDataDir)
            }

            // 2. Configure Sherpa-ONNX with the bundled INT8 Persian model
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelSubdir/fa_IR-amir-medium.onnx",
                        tokens = "$modelSubdir/tokens.txt",
                        dataDir = targetDataDir.absolutePath,
                        noiseScale = 0.667f,
                        lengthScale = 1.0f
                    ),
                    numThreads = 2,
                    provider = "cpu"
                ),
                maxNumSentences = 2,
                silenceScale = 0.2f
            )

            offlineTts = OfflineTts(assetManager = context.assets, config = config)
            _isReady.value = true
            Log.d(tag, "Embedded Persian TTS successfully initialized and ready!")
        } catch (e: Throwable) {
            Log.e(tag, "Embedded Persian TTS init failed: ${e.message}", e)
            _isReady.value = false
        }
    }

    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        for (fileName in files) {
            val subAsset = if (assetPath.isEmpty()) fileName else "$assetPath/$fileName"
            val subFiles = assetManager.list(subAsset)

            val destFile = File(targetDir, fileName)
            if (subFiles.isNullOrEmpty()) {
                // File: copy bytes
                if (!destFile.exists() || destFile.length() == 0L) {
                    try {
                        assetManager.open(subAsset).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(tag, "Failed copying $subAsset: ${e.message}")
                    }
                }
            } else {
                // Subdirectory: recursive copy
                copyAssetFolder(subAsset, destFile)
            }
        }
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        onDone: () -> Unit = {}
    ) {
        val tts = offlineTts
        if (tts == null || !_isReady.value) {
            Log.w(tag, "Engine not ready yet, skipping speak")
            onDone()
            return
        }

        stop()

        currentPlayJob = scope.launch(Dispatchers.Default) {
            try {
                _isSpeaking.value = true
                val cleanText = cleanMarkdownForSpeech(text)

                if (cleanText.isBlank()) {
                    _isSpeaking.value = false
                    onDone()
                    return@launch
                }

                // Speed calculation: lengthScale in VITS (lower lengthScale = faster speech)
                val speed = speechRate.coerceIn(0.7f, 1.5f)

                // Generate neural audio wave (Amir voice, speaker id 0)
                Log.d(tag, "Synthesizing Persian audio with Amir neural voice: \"$cleanText\"")
                val audio = tts.generate(text = cleanText, sid = 0, speed = speed)

                if (audio.samples.isEmpty()) {
                    Log.w(tag, "Audio generation produced empty samples")
                    _isSpeaking.value = false
                    onDone()
                    return@launch
                }

                playPcmFloatSamples(audio.samples, audio.sampleRate, onDone)
            } catch (e: Throwable) {
                Log.e(tag, "Error during Persian speech synthesis: ${e.message}", e)
                _isSpeaking.value = false
                onDone()
            }
        }
    }

    private suspend fun playPcmFloatSamples(
        samples: FloatArray,
        sampleRate: Int,
        onDone: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(sampleRate)
                .build()

            val bufferSize = samples.size * 4

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            audioTrack?.play()

            val durationMs = (samples.size.toDouble() / sampleRate * 1000).toLong()
            delay(durationMs + 100)

            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
            audioTrack = null

            _isSpeaking.value = false
            withContext(Dispatchers.Main) {
                onDone()
            }
        } catch (e: Exception) {
            Log.e(tag, "AudioTrack playback error: ${e.message}")
            _isSpeaking.value = false
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    fun stop() {
        currentPlayJob?.cancel()
        currentPlayJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _isSpeaking.value = false
    }

    private fun cleanMarkdownForSpeech(text: String): String {
        return text
            // Strip code blocks and inline code
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("`[^`]*`"), "")
            // Strip markdown links [label](url)
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            // Strip markdown header/bold/italic tags
            .replace(Regex("[*#_~`>]"), "")
            // Strip emoji characters
            .replace(Regex("[\\p{So}\\p{Cn}]"), " ")
            // Standardize Persian pause commas
            .replace("،", " ، ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
