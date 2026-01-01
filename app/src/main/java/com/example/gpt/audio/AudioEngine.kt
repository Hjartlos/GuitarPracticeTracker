package com.example.gpt.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.writer.WriterProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile

class AudioEngine {

    private val _tunerResult = MutableStateFlow(TunerResult())
    val tunerResult: StateFlow<TunerResult> = _tunerResult.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var dispatcher: AudioDispatcher? = null
    private var isRunning = false
    private var audioRecord: AudioRecord? = null
    private var currentRecordingFile: File? = null

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

        val sampleRate = 44100
        val bufferSize = 2048
        val overlap = 0

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val audioBufferSize = maxOf(bufferSize * 2, minBufferSize)

        try {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, audioBufferSize)

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioEngine", "AudioRecord failed to initialize")
                return
            }

            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            Log.e("AudioEngine", "Brak uprawnień do mikrofonu: ${e.message}")
            return
        } catch (e: Exception) {
            Log.e("AudioEngine", "Inny błąd mikrofonu: ${e.message}")
            return
        }

        val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val audioStream = AndroidAudioInputStream(audioRecord!!, format)
        dispatcher = AudioDispatcher(audioStream, bufferSize, overlap)

        val pitchHandler = PitchDetectionHandler { result, _ ->
            if (result.probability > 0.90f) {
                _tunerResult.value = AudioUtils.processPitch(result.pitch)
            }
        }
        dispatcher?.addAudioProcessor(PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, sampleRate.toFloat(), bufferSize, pitchHandler))

        dispatcher?.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                _amplitude.value = audioEvent.rms.toFloat() * 100
                return true
            }
            override fun processingFinished() {}
        })

        if (outputFile != null) {
            try {
                val randomAccessFile = RandomAccessFile(outputFile, "rw")
                val writerProcessor = WriterProcessor(format, randomAccessFile)
                dispatcher?.addAudioProcessor(writerProcessor)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Thread(dispatcher, "Audio Thread").start()
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        dispatcher?.stop()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { e.printStackTrace() }
        audioRecord = null
        isRunning = false
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
}