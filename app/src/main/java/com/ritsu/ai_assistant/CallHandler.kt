package com.ritsu.ai_assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import gg.gger.llama.cpp.java.bindings.LlamaContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.IOException
import java.util.*

/**
 * Manages the AI conversation for a single phone call.
 * This class should be instantiated by RitsuInCallService.
 */
class CallHandler(
    private val context: Context,
    private val callAudioStream: Int, // TODO: Figure out how to get this from InCallService
    private val onHangup: () -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val TAG = "CallHandler"
    private var voskModel: Model? = null
    private var speechService: SpeechService? = null
    private var tts: TextToSpeech? = null
    private var llamaContext: LlamaContext? = null // This should be shared, not created here.

    private val conversationHistory = mutableListOf<String>()

    init {
        // TODO: This is problematic. The LlamaContext is heavy and should be a singleton.
        // For now, we'll assume it's passed in or retrieved from a central place.
        // llamaContext = ...

        tts = TextToSpeech(context, this)
        // initVosk() // Vosk model should be passed in or loaded from a central manager.
    }

    // --- TTS ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            speak("Hello, you have reached the personal assistant for this number. How may I help you?")
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
        }
    }

    private fun speak(text: String) {
        conversationHistory.add("Ritsu: $text")
        // TODO: Set TTS to use the call audio stream
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // --- STT ---
    fun startListening() {
        if (voskModel == null) {
            Log.e(TAG, "Vosk model not initialized")
            return
        }
        try {
            // TODO: Ensure this recognizer uses the call's audio stream, not the mic.
            val rec = Recognizer(voskModel, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing recognizer", e)
        }
    }

    override fun onResult(hypothesis: String) {
        val result = hypothesis.substringAfter("\"text\" : \"").substringBefore("\"")
        if (result.isNotBlank()) {
            Log.d(TAG, "Caller said: $result")
            conversationHistory.add("Caller: $result")
            generateResponse(result)
        }
    }

    // --- LLM ---
    private fun generateResponse(callerText: String) {
        if (llamaContext == null) {
            speak("I'm sorry, I'm having trouble connecting to my brain right now. Please call back later.")
            onHangup()
            return
        }

        val prompt = """
        You are Ritsu, a personal AI assistant screening a phone call for your user.
        The user is busy. Be polite and helpful. Take a message or determine if the call is urgent.
        Conversation History:
        ${conversationHistory.joinToString("\n")}

        New message from caller: "$callerText"
        Ritsu's response:
        """.trimIndent()

        // This needs to be on a background thread.
        try {
            val response = llamaContext?.complete(prompt)?.trim() ?: "I'm sorry, I don't know how to respond to that."
            speak(response)
        } catch (e: Exception) {
            Log.e(TAG, "LLM response generation failed", e)
            speak("I'm sorry, an error occurred.")
        }
    }

    fun shutdown() {
        speechService?.stop()
        speechService?.shutdown()
        tts?.stop()
        tts?.shutdown()
        // Do not close LlamaContext here if it's shared.
    }

    // Unused RecognitionListener methods
    override fun onPartialResult(hypothesis: String?) {}
    override fun onFinalResult(hypothesis: String?) {}
    override fun onError(exception: Exception) { Log.e(TAG, "Vosk Error", exception) }
    override fun onTimeout() { startListening() } // Continuous listening
}
