package com.ritsu.ai_assistant

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

/**
 * Manages the main Speech-to-Text (STT) functionality using the Vosk library.
 * This class is responsible for listening to the device's microphone after the
 * hotword is detected, and returning the recognized text. It provides callbacks
 * for results and listening status changes.
 */
class SttManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (Exception) -> Unit,
    private val onListeningStarted: () -> Unit,
    private val onListeningStopped: () -> Unit
) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    init {
        initVosk()
    }

    private fun initVosk() {
        StorageService.unpack(context, "vosk-model-es", "model",
            { model ->
                this.model = model
                Log.d("STT", "Vosk model unpacked and loaded.")
            },
            { exception ->
                val errorMessage = "Failed to unpack Vosk model. Make sure the model assets are correct."
                Log.e("STT", errorMessage, exception)
                onError(IOException(errorMessage, exception))
            })
    }

    fun startListening() {
        if (model == null) {
            val errorMessage = "Vosk model is not loaded yet."
            Log.e("STT", errorMessage)
            onError(IOException(errorMessage))
            return
        }
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
            onListeningStarted()
            Log.d("STT", "Started listening.")
        } catch (e: IOException) {
            val errorMessage = "Failed to start listening."
            Log.e("STT", errorMessage, e)
            onError(e)
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        onListeningStopped()
        Log.d("STT", "Stopped listening.")
    }

    fun shutdown() {
        speechService?.stop()
        speechService?.shutdown()
        model = null
    }

    override fun onPartialResult(hypothesis: String?) {}

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            val resultText = it.substringAfter("\"text\" : \"").substringBefore("\"")
            if (resultText.isNotBlank()) {
                Log.d("STT", "Result: $resultText")
                onResult(resultText)
            }
        }
        stopListening()
    }

    override fun onFinalResult(hypothesis: String?) {
        onResult(hypothesis)
    }

    override fun onError(exception: Exception) {
        Log.e("STT", "Recognition error", exception)
        onError(exception)
        stopListening()
    }

    override fun onTimeout() {
        Log.d("STT", "Listening timeout.")
        stopListening()
    }
}
