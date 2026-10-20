package com.example.keyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

interface VoiceInputListener {
    fun onStateChanged(state: VoiceInputState, message: String)
    fun onRmsChanged(rmsdB: Float)
    fun onSpeechResult(text: String)
    fun onError(errorMessage: String)
}

object VoiceInputManager {

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentState: VoiceInputState = VoiceInputState.IDLE
    private var currentListener: VoiceInputListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isListening(): Boolean = currentState == VoiceInputState.LISTENING

    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isRecognitionAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        context: Context,
        languageCode: String, // "en" or "bn"
        listener: VoiceInputListener
    ) {
        mainHandler.post {
            currentListener = listener

            if (!hasMicrophonePermission(context)) {
                setState(VoiceInputState.ERROR, "Microphone permission needed. Enable it in App Settings.")
                listener.onError("Microphone permission needed")
                return@post
            }

            if (!isRecognitionAvailable(context)) {
                setState(VoiceInputState.ERROR, "Speech recognition service not found on device.")
                listener.onError("Speech service not found")
                return@post
            }

            try {
                destroyRecognizer()

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        setState(VoiceInputState.LISTENING, "Listening... Speak now")
                    }

                    override fun onBeginningOfSpeech() {
                        setState(VoiceInputState.LISTENING, "Hearing voice...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        currentListener?.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        setState(VoiceInputState.PROCESSING, "Processing speech...")
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard. Try speaking louder."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error for online speech."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Could not recognize speech ($error)."
                        }
                        setState(VoiceInputState.ERROR, message)
                        currentListener?.onError(message)
                        destroyRecognizer()
                    }

                    override fun onResults(results: Bundle?) {
                        setState(VoiceInputState.IDLE, "")
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            currentListener?.onSpeechResult(text)
                        } else {
                            currentListener?.onError("No words recognized")
                        }
                        destroyRecognizer()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrEmpty()) {
                            setState(VoiceInputState.LISTENING, partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    val targetLocale = if (languageCode == "bn") "bn-BD" else "en-US"
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "bn-BD"))
                }

                setState(VoiceInputState.LISTENING, "Starting speech recognition...")
                recognizer.startListening(intent)

            } catch (e: Exception) {
                setState(VoiceInputState.ERROR, "Could not start microphone: ${e.message}")
                currentListener?.onError(e.message ?: "Voice recognition error")
                destroyRecognizer()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                setState(VoiceInputState.PROCESSING, "Processing...")
            } catch (_: Exception) {
                destroyRecognizer()
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            destroyRecognizer()
            setState(VoiceInputState.IDLE, "")
        }
    }

    private fun setState(state: VoiceInputState, message: String) {
        currentState = state
        currentListener?.onStateChanged(state, message)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        currentState = VoiceInputState.IDLE
    }
}
