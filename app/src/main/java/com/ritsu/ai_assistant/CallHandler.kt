package com.ritsu.ai_assistant

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.Vosk
import java.io.IOException
import java.util.*

/**
 * Manages the AI conversation for a single phone call.
 * This class encapsulates the logic for Text-to-Speech, Speech-to-Text from
 * the call audio stream, and interaction with the LLM to generate responses.
 * It is designed to be instantiated by `RitsuInCallService` for each incoming call.
 *
 * Note: Capturing call audio via `VOICE_COMMUNICATION` is not guaranteed to
 * work on all Android devices and versions due to OS and manufacturer restrictions.
 */
@SuppressLint("MissingPermission")
class CallHandler(
    private val context: Context,
    private val onHangup: () -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "CallHandler"
    private var voskModel: Model? = null
    private var recognizer: Recognizer? = null
    private var tts: TextToSpeech? = null
    private var audioRecord: AudioRecord? = null
    private var recognitionThread: Thread? = null
    @Volatile private var isRunning = false

    private val conversationHistory = mutableListOf<String>()

    init {
        // Models should be pre-loaded and passed in, or loaded from a manager.
        // For now, we will load them here for simplicity.
        try {
            val modelPath = StorageService.unpack(context, "vosk-model-es", "model")
            voskModel = Model(modelPath)
            Vosk.setLogLevel(0)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to unpack Vosk model", e)
        }

        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            speak("Hello, you have reached the personal assistant for this number. How may I help you?")
            startRecognition()
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
        }
    }

    private fun startRecognition() {
        if (voskModel == null) {
            Log.e(TAG, "Vosk model not loaded, cannot start recognition.")
            return
        }
        recognizer = Recognizer(voskModel, 16000.0f)
        setupAudioRecord()
        isRunning = true
        recognitionThread = Thread {
            val buffer = ShortArray(4096)
            while (isRunning) {
                val nread = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (nread > 0) {
                    if (recognizer?.acceptWaveForm(buffer, nread) == true) {
                        val result = recognizer?.result?.substringAfter("\"text\" : \"")?.substringBefore("\"")
                        if (!result.isNullOrBlank()) {
                            handleRecognitionResult(result)
                        }
                    }
                }
            }
        }
        recognitionThread?.start()
        audioRecord?.startRecording()
        Log.d(TAG, "Call recognition started.")
    }

    private fun handleRecognitionResult(text: String) {
        Log.d(TAG, "Caller said: $text")
        conversationHistory.add("Caller: $text")
        generateResponse(text)
    }

    private fun setupAudioRecord() {
        val source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(source, sampleRate, channelConfig, audioFormat, bufferSize)
    }

    private fun speak(text: String) {
        conversationHistory.add("Ritsu: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun generateResponse(callerText: String) {
        // This should run on a background thread.
        val llamaContext = LlamaManager.get()
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

        try {
            val response = llamaContext.complete(prompt)?.trim() ?: "I'm sorry, I don't know how to respond to that."
            speak(response)
        } catch (e: Exception) {
            Log.e(TAG, "LLM response generation failed", e)
            speak("I'm sorry, an error occurred.")
        }
    }

    fun shutdown() {
        isRunning = false
        recognitionThread?.interrupt()
        recognitionThread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        tts?.stop()
        tts?.shutdown()

        recognizer = null
        voskModel = null
        Log.d(TAG, "CallHandler shut down.")
    }
}
