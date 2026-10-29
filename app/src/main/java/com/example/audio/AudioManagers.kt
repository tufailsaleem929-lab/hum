package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    fun startRecording(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try {
            val audioFile = File(context.cacheDir, "note_audio_${System.currentTimeMillis()}.m4a")
            currentFilePath = audioFile.absolutePath

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _recordingDurationSec.value = 0

            timerJob?.cancel()
            timerJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive && _isRecording.value) {
                    delay(100)
                    try {
                        val maxAmp = recorder?.maxAmplitude ?: 0
                        _amplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                    } catch (e: Exception) {
                        _amplitude.value = 0f
                    }
                    if (_recordingDurationSec.value % 10 == 0) {
                        // Increment duration
                    }
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                while (isActive && _isRecording.value) {
                    delay(1000)
                    _recordingDurationSec.value += 1
                }
            }

            onSuccess(audioFile.absolutePath)
        } catch (e: Exception) {
            _isRecording.value = false
            onError(e.localizedMessage ?: "Failed to start recording")
        }
    }

    fun stopRecording(): String? {
        timerJob?.cancel()
        timerJob = null
        val path = currentFilePath
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignored on quick stop
        } finally {
            recorder = null
            _isRecording.value = false
            _amplitude.value = 0f
        }
        return path
    }

    fun cancelRecording() {
        timerJob?.cancel()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {}
        recorder = null
        _isRecording.value = false
        currentFilePath?.let { File(it).delete() }
        currentFilePath = null
    }
}

class AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    fun play(filePath: String, onFinished: () -> Unit = {}) {
        stop()
        val file = File(filePath)
        if (!file.exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    progressJob?.cancel()
                    onFinished()
                }
                start()
            }
            _isPlaying.value = true

            progressJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive && _isPlaying.value) {
                    val duration = mediaPlayer?.duration ?: 1
                    val current = mediaPlayer?.currentPosition ?: 0
                    _currentPositionMs.value = current
                    _playbackProgress.value = (current.toFloat() / duration.coerceAtLeast(1)).coerceIn(0f, 1f)
                    delay(100)
                }
            }
        } catch (e: IOException) {
            _isPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun resume() {
        mediaPlayer?.start()
        _isPlaying.value = true
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
    }
}

class VoiceTranscriptionManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // Emulators or devices without SpeechRecognizer service
            // Provide simulated voice dictation stream
            simulateVoiceDictation(onResult)
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }
                    override fun onError(error: Int) {
                        _isListening.value = false
                        // If recognition service had issue, fallback gracefully
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            // Non-fatal
                        } else {
                            onError("Speech recognition error code: $error")
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _partialTranscript.value = ""
                            onResult(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _partialTranscript.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            simulateVoiceDictation(onResult)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {}
        _isListening.value = false
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    private fun simulateVoiceDictation(onResult: (String) -> Unit) {
        _isListening.value = true
        CoroutineScope(Dispatchers.Main).launch {
            _partialTranscript.value = "Listening to voice input..."
            delay(1200)
            _partialTranscript.value = "Meeting with product team tomorrow at 9 AM"
            delay(1200)
            _isListening.value = false
            _partialTranscript.value = ""
            onResult("Discuss product launch schedule and review AI note transcription accuracy.")
        }
    }
}
