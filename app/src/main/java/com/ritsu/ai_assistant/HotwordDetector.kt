package com.ritsu.ai_assistant

import android.content.Context
import android.util.Log
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class HotwordDetector(
    private val context: Context,
    private val onHotwordDetected: () -> Unit
) : RecognitionListener {

    private val TAG = "HotwordDetector"
    private var speechRecognizer: SpeechRecognizer? = null

    fun setup() {
        try {
            val assets = Assets(context)
            val assetDir = assets.syncAssets()
            val setup = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetDir, "en-us-ptm"))
                .setDictionary(File(assetDir, "cmudict-en-us.dict"))
                //.setRawLogDir(assetDir) // Optional for debugging
                .setKeywordThreshold(1e-45f)

            speechRecognizer = setup.recognizer
            speechRecognizer?.addListener(this)

            // Create a search file for the keyword
            val keywordFile = File(assetDir, "keywords.txt")
            // The keyword file should be in the assets, let's assume it is.
            // In a real app, we would generate this or ensure it's in the sync dir.
            speechRecognizer?.addKeywordSearch("HOTWORD", File(assetDir, "models/keywords.txt"))

        } catch (e: IOException) {
            Log.e(TAG, "Failed to set up PocketSphinx", e)
        }
    }

    fun startListening() {
        if (speechRecognizer == null) setup()
        speechRecognizer?.startListening("HOTWORD")
        Log.d(TAG, "PocketSphinx is listening for 'hey ritsu'")
    }

    fun stopListening() {
        speechRecognizer?.stop()
        Log.d(TAG, "PocketSphinx has stopped listening")
    }

    fun destroy() {
        speechRecognizer?.cancel()
        speechRecognizer?.shutdown()
    }

    override fun onBeginningOfSpeech() {
        // Not used for keyword spotting
    }

    override fun onEndOfSpeech() {
        // Restart listening after speech ends
        speechRecognizer?.startListening("HOTWORD", 10000)
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {
        hypothesis?.let {
            val text = it.hypstr
            if (text == "hey ritsu") {
                Log.d(TAG, "Hotword 'hey ritsu' detected!")
                onHotwordDetected()
                // Stop listening for hotword to let main STT take over
                speechRecognizer?.stop()
            }
        }
    }

    override fun onResult(hypothesis: Hypothesis?) {
        // Not used for keyword spotting
    }

    override fun onError(error: Exception?) {
        Log.e(TAG, "PocketSphinx error", error)
    }

    override fun onTimeout() {
        speechRecognizer?.startListening("HOTWORD")
    }
}
