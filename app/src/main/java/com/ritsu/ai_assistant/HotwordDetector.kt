package com.ritsu.ai_assistant

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class HotwordDetector(
    private val context: Context,
    private val onHotwordDetected: () -> Unit
) : RecognitionListener {

    private val TAG = "HotwordDetector"
    private var model: Model? = null
    private var speechService: SpeechService? = null

    init {
        // Model loading should be done off the main thread.
        // For simplicity, we do it here, but in a real app, use a background thread.
        try {
            StorageService.unpack(context, "model-en-us", "model",
                { model ->
                    this.model = model
                },
                { exception ->
                    Log.e(TAG, "Failed to unpack model", exception)
                })
        } catch (e: IOException) {
            Log.e(TAG, "Failed to unpack model", e)
        }
    }

    fun startListening() {
        if (model == null) {
            Log.e(TAG, "Model not loaded yet, cannot start listening.")
            return
        }
        try {
            // The recognizer should be created with a specific grammar for the hotword
            // For Vosk, you provide a JSON string of accepted phrases.
            val recognizer = Recognizer(model, 16000.0f, "[\"hey ritsu\"]")
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            Log.d(TAG, "Vosk is listening for 'hey ritsu'")
        } catch (e: IOException) {
            Log.e(TAG, "Error starting Vosk listener", e)
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        Log.d(TAG, "Vosk has stopped listening")
    }

    fun destroy() {
        speechService?.stop()
        speechService?.shutdown()
        model = null // Vosk models are not explicitly closed
    }

    override fun onPartialResult(hypothesis: String?) {
        // In keyword spotting mode, we often get the result in onPartialResult
        hypothesis?.let {
            if (it.contains("hey ritsu")) {
                Log.d(TAG, "Hotword 'hey ritsu' detected!")
                onHotwordDetected()
                // Stop listening for the hotword to let the main STT take over
                stopListening()
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        // Final result, can also be used
    }

    override fun onFinalResult(hypothesis: String?) {
        // Not typically used for continuous keyword spotting
    }

    override fun onError(exception: Exception?) {
        Log.e(TAG, "Vosk recognition error", exception)
        // May need to restart listening after an error
    }

    override fun onTimeout() {
        // Can be used to restart listening
    }
}
