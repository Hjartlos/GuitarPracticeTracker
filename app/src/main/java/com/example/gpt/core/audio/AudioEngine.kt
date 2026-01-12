package com.example.gpt.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

class AudioEngine {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
        private const val OVERLAP = 2048
        private const val PITCH_PROBABILITY_THRESHOLD = 0.85f
    }

    private val _tunerResult = MutableStateFlow(TunerResult())
    val tunerResult: StateFlow<TunerResult> = _tunerResult.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

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
                Log.e("AudioEngine", "AudioRecord failed to initialize. Trying MIC fallback.")
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioBufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioEngine", "AudioRecord MIC fallback failed.")
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error starting mic: ${e.message}")
            return
        }

        val format = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val audioStream = AndroidAudioInputStream(audioRecord!!, format)
        dispatcher = AudioDispatcher(audioStream, BUFFER_SIZE, OVERLAP)

        val pitchHandler = PitchDetectionHandler { result, audioEvent ->
            val buffer = audioEvent.floatBuffer
            val rawRms = calculateRMS(buffer)

            _amplitude.value = (sqrt(rawRms) * 300).coerceAtMost(100f)

            if (ToneGenerator.isPlaying.value) {
                return@PitchDetectionHandler
            }

            val tunerThreshold = 0.005f

            if (rawRms > tunerThreshold && result.probability > PITCH_PROBABILITY_THRESHOLD && result.pitch > 20f) {
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
                if (rawRms < tunerThreshold) {
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

    private fun calculateRMS(floatBuffer: FloatArray): Float {
        var sum = 0.0
        for (sample in floatBuffer) {
            sum += sample * sample
        }
        return sqrt(sum / floatBuffer.size).toFloat()
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