package com.ritsu.ai_assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manages Text-to-Speech (TTS) functionality for the application.
 * This class encapsulates the Android TextToSpeech engine, handles its
 * initialization, and provides a simple interface for speaking text.
 * It also includes callbacks for when speech starts and stops, which can be
 * used to trigger animations.
 */
class TtsManager(
    context: Context,
    private val onSpeechStarted: () -> Unit,
    private val onSpeechStopped: () -> Unit
) : TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            onSpeechStarted()
        }

        override fun onDone(utteranceId: String?) {
            onSpeechStopped()
        }

        override fun onError(utteranceId: String?) {
            onSpeechStopped()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                isReady = true
                tts.setOnUtteranceProgressListener(utteranceListener)
                Log.d("TTS", "TTS Engine is ready.")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    fun speak(text: String) {
        if (isReady) {
            val utteranceId = this.hashCode().toString() + ""
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            Log.e("TTS", "TTS not initialized yet.")
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
