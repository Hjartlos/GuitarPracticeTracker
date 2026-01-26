package com.example.gpt.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.*

class AudioEngine {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
        private const val OVERLAP = 2048
        private const val PITCH_PROBABILITY_THRESHOLD = 0.70f
    }

    private val _tunerResult = MutableStateFlow(TunerResult())
    val tunerResult: StateFlow<TunerResult> = _tunerResult.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _tapEvent = MutableSharedFlow<Long>(extraBufferCapacity = 64)
    val tapEvent: SharedFlow<Long> = _tapEvent.asSharedFlow()

    private val pitchHistory = mutableListOf<Float>()
    private val centsHistory = mutableListOf<Int>()

    private var dispatcher: AudioDispatcher? = null
    private var audioThread: Thread? = null

    @Volatile
    private var isRunning = false
    private var audioRecord: AudioRecord? = null
    private var currentRecordingFile: File? = null

    @Volatile
    var currentThreshold: Float = 0.02f

    @Volatile
    var isMonitoringEnabled: Boolean = false
    private var monitoringProcessor: MonitoringProcessor? = null

    private var tapDetectionProcessor: TapDetectionProcessor? = null

    private val scope = CoroutineScope(Dispatchers.Default)

    @SuppressLint("MissingPermission")
    fun start(outputFile: File? = null) {
        if (isRunning) {
            if (currentRecordingFile == null && outputFile != null) {
                stop()
            } else {
                return
            }
        }

        currentRecordingFile = outputFile

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val audioBufferSize = maxOf(BUFFER_SIZE * 2, minBufferSize)

        try {
            val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MediaRecorder.AudioSource.UNPROCESSED
            } else {
                MediaRecorder.AudioSource.MIC
            }

            audioRecord = AudioRecord(
                audioSource,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                try {
                    audioRecord?.release()
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        audioBufferSize
                    )
                } catch (e: Exception) {
                    Log.e("AudioEngine", "MIC fallback failed: ${e.message}")
                }
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            return
        }

        val format = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val audioStream = AndroidAudioInputStream(audioRecord!!, format)
        dispatcher = AudioDispatcher(audioStream, BUFFER_SIZE, OVERLAP)

        dispatcher?.addAudioProcessor(HighPassFilter(cutoffFreq = 30f))
        dispatcher?.addAudioProcessor(NoiseGateProcessor(currentThreshold))

        tapDetectionProcessor = TapDetectionProcessor()
        dispatcher?.addAudioProcessor(tapDetectionProcessor)

        monitoringProcessor = MonitoringProcessor()
        dispatcher?.addAudioProcessor(monitoringProcessor)

        val pitchHandler = PitchDetectionHandler { result, audioEvent ->
            val buffer = audioEvent.floatBuffer
            val rawRms = calculateRMS(buffer)

            _amplitude.value = (sqrt(rawRms) * 400).coerceAtMost(100f)

            monitoringProcessor?.updateRms(rawRms)

            if (ToneGenerator.isPlaying.value) {
                return@PitchDetectionHandler
            }

            val effectiveTunerThreshold = (currentThreshold / 4f).coerceAtLeast(0.0005f)

            if (rawRms > effectiveTunerThreshold && result.probability > PITCH_PROBABILITY_THRESHOLD && result.pitch > 20f) {
                pitchHistory.add(result.pitch)
                if (pitchHistory.size > 3) pitchHistory.removeAt(0)

                val smoothedPitch = if (pitchHistory.size >= 2) {
                    pitchHistory.sorted()[pitchHistory.size / 2]
                } else {
                    result.pitch
                }

                val tunerResult = AudioUtils.processPitch(smoothedPitch)

                centsHistory.add(tunerResult.cents)
                if (centsHistory.size > 3) centsHistory.removeAt(0)

                val smoothedCents = if (centsHistory.size >= 2) {
                    centsHistory.sorted()[centsHistory.size / 2]
                } else {
                    tunerResult.cents
                }

                _tunerResult.value = tunerResult.copy(cents = smoothedCents)
            } else {
                if (rawRms < effectiveTunerThreshold) {
                    pitchHistory.clear()
                    centsHistory.clear()
                    _tunerResult.value = _tunerResult.value.copy(isLocked = false)
                }
            }
        }
        dispatcher?.addAudioProcessor(PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, SAMPLE_RATE.toFloat(), BUFFER_SIZE, pitchHandler))

        if (outputFile != null) {
            try {
                if (outputFile.exists()) outputFile.delete()
                outputFile.createNewFile()

                val randomAccessFile = RandomAccessFile(outputFile, "rw")
                val nonOverlapWriter = NonOverlapWriterProcessor(format, randomAccessFile, BUFFER_SIZE - OVERLAP)
                dispatcher?.addAudioProcessor(nonOverlapWriter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        audioThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            dispatcher?.run()
        }, "Audio Thread")
        audioThread?.start()
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return

        monitoringProcessor?.release()
        monitoringProcessor = null
        tapDetectionProcessor = null
        isMonitoringEnabled = false

        dispatcher?.stop()

        try {
            audioThread?.join(1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { e.printStackTrace() }

        audioRecord = null
        audioThread = null
        isRunning = false

        _tunerResult.value = TunerResult()
        _amplitude.value = 0f
    }

    fun setMinTimeBetweenTaps(ms: Long) {
        tapDetectionProcessor?.updateMinTime(ms)
    }

    fun toggleMonitoring(enabled: Boolean) {
        isMonitoringEnabled = enabled
    }

    private fun calculateRMS(floatBuffer: FloatArray): Float {
        var sum = 0.0
        for (sample in floatBuffer) {
            sum += sample * sample
        }
        return sqrt(sum / floatBuffer.size).toFloat()
    }

    private inner class TapDetectionProcessor : AudioProcessor {
        private var lastTapTime = 0L
        private var minTimeBetweenTaps = 50L

        fun updateMinTime(ms: Long) {
            minTimeBetweenTaps = ms
        }

        override fun process(audioEvent: AudioEvent): Boolean {
            val buffer = audioEvent.floatBuffer
            val rms = calculateRMS(buffer)
            val tapThreshold = currentThreshold * 1.2f

            if (rms > tapThreshold) {
                val now = System.currentTimeMillis()
                if (now - lastTapTime > minTimeBetweenTaps) {
                    lastTapTime = now
                    scope.launch {
                        _tapEvent.emit(now)
                    }
                }
            }
            return true
        }

        override fun processingFinished() {}
    }

    private inner class HighPassFilter(
        private val cutoffFreq: Float = 80f
    ) : AudioProcessor {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        private val omega = 2f * PI.toFloat() * cutoffFreq / SAMPLE_RATE
        private val sn = sin(omega)
        private val cs = cos(omega)
        private val alpha = sn / (2f * 0.707f)

        private val b0 = (1f + cs) / 2f
        private val b1 = -(1f + cs)
        private val b2 = (1f + cs) / 2f
        private val a0 = 1f + alpha
        private val a1 = -2f * cs
        private val a2 = 1f - alpha

        override fun process(audioEvent: AudioEvent): Boolean {
            val buffer = audioEvent.floatBuffer

            for (i in buffer.indices) {
                val x0 = buffer[i]
                val y0 = (b0/a0)*x0 + (b1/a0)*x1 + (b2/a0)*x2 - (a1/a0)*y1 - (a2/a0)*y2

                x2 = x1
                x1 = x0
                y2 = y1
                y1 = y0

                buffer[i] = y0
            }
            return true
        }

        override fun processingFinished() {}
    }

    private inner class NoiseGateProcessor(
        private var threshold: Float,
        private val attackTime: Float = 0.001f,
        private val releaseTime: Float = 0.05f
    ) : AudioProcessor {
        private var envelopeGain = 0f

        fun updateThreshold(newThreshold: Float) {
            threshold = newThreshold
        }

        override fun process(audioEvent: AudioEvent): Boolean {
            val buffer = audioEvent.floatBuffer
            val attackCoeff = 1f - exp(-1f / (SAMPLE_RATE * attackTime))
            val releaseCoeff = 1f - exp(-1f / (SAMPLE_RATE * releaseTime))

            for (i in buffer.indices) {
                val sample = abs(buffer[i])

                envelopeGain = if (sample > envelopeGain) {
                    envelopeGain + attackCoeff * (sample - envelopeGain)
                } else {
                    envelopeGain + releaseCoeff * (sample - envelopeGain)
                }

                buffer[i] = if (envelopeGain > threshold) {
                    buffer[i]
                } else {
                    buffer[i] * (envelopeGain / threshold).coerceIn(0f, 1f)
                }
            }
            return true
        }

        override fun processingFinished() {}
    }

    private inner class MonitoringProcessor : AudioProcessor {
        private var audioTrack: AudioTrack? = null
        private var currentRms: Float = 0f

        init {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(minBufferSize)
                    .build()

                audioTrack?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateRms(rms: Float) {
            currentRms = rms
        }

        override fun process(audioEvent: AudioEvent): Boolean {
            if (!isMonitoringEnabled) return true

            if (currentRms < currentThreshold) {
                return true
            }

            val floatBuffer = audioEvent.floatBuffer
            val shortBuffer = ShortArray(floatBuffer.size)

            for (i in floatBuffer.indices) {
                val sample = (floatBuffer[i] * Short.MAX_VALUE).toInt()
                shortBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            try {
                audioTrack?.write(shortBuffer, 0, shortBuffer.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return true
        }

        override fun processingFinished() {
            release()
        }

        fun release() {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) { }
            audioTrack = null
        }
    }

    private class AndroidAudioInputStream(
        private val record: AudioRecord,
        private val format: TarsosDSPAudioFormat
    ) : TarsosDSPAudioInputStream {
        override fun skip(n: Long): Long = 0
        override fun read(b: ByteArray, off: Int, len: Int): Int = record.read(b, off, len)
        override fun close() {}
        override fun getFormat(): TarsosDSPAudioFormat = format
        override fun getFrameLength(): Long = -1
    }

    private class NonOverlapWriterProcessor(
        private val format: TarsosDSPAudioFormat,
        private val file: RandomAccessFile,
        private val hopSize: Int
    ) : AudioProcessor {

        private var isFirstBuffer = true
        private var totalSamplesWritten = 0L

        init {
            file.write(ByteArray(44))
        }

        override fun process(audioEvent: AudioEvent): Boolean {
            val floatBuffer = audioEvent.floatBuffer
            val bytesPerSample = format.sampleSizeInBits / 8

            val samplesToWrite = if (isFirstBuffer) {
                isFirstBuffer = false
                floatBuffer.size
            } else {
                hopSize.coerceAtMost(floatBuffer.size)
            }

            val byteBuffer = ByteArray(samplesToWrite * bytesPerSample)
            for (i in 0 until samplesToWrite) {
                val sampleIndex = floatBuffer.size - samplesToWrite + i
                if (sampleIndex >= 0 && sampleIndex < floatBuffer.size) {
                    val sample = (floatBuffer[sampleIndex] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    byteBuffer[i * 2] = (sample and 0xFF).toByte()
                    byteBuffer[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                }
            }

            try {
                file.write(byteBuffer)
                totalSamplesWritten += samplesToWrite
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }

        override fun processingFinished() {
            try {
                val totalDataBytes = totalSamplesWritten * 2
                file.seek(0)
                writeWavHeader(file, totalDataBytes.toInt(), format.sampleRate.toInt(), 1, 16)
                file.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun writeWavHeader(raf: RandomAccessFile, dataSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int) {
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = channels * bitsPerSample / 8

            raf.writeBytes("RIFF")
            raf.writeInt(Integer.reverseBytes(36 + dataSize))
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeInt(Integer.reverseBytes(16))
            raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
            raf.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
            raf.writeInt(Integer.reverseBytes(sampleRate))
            raf.writeInt(Integer.reverseBytes(byteRate))
            raf.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
            raf.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
            raf.writeBytes("data")
            raf.writeInt(Integer.reverseBytes(dataSize))
        }
    }
}