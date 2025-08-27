package com.ritsu.ai_assistant

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gg.gger.llama.cpp.java.bindings.LlamaContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class CallActivity : ComponentActivity(), RecognitionListener {

    private val TAG = "CallActivity"
    private var model: Model? = null
    private var speechService: SpeechService? = null

    // Placeholder for LLM context. In a real app, this would be injected or retrieved
    // from a shared component, similar to how ChatViewModel gets it.
    private var llamaContext: LlamaContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RitsuAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CallScreen()
                }
            }
        }

        // TODO: This is a placeholder initialization. We need a proper way to access the LLM.
        // For now, we assume it's magically available for the logic.

        initVosk()
    }

    private fun initVosk() {
        StorageService.unpack(this, "vosk-model-es", "model",
            { model: Model? ->
                this.model = model
                recognizeMicrophone()
            },
            { exception: IOException ->
                Log.e(TAG, "Failed to unpack model", exception)
            })
    }

    private fun recognizeMicrophone() {
        if (model == null) {
            Log.e(TAG, "Vosk model not initialized")
            return
        }
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing recognizer", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        speechService?.shutdown()
    }

    // --- RecognitionListener Callbacks ---

    override fun onResult(hypothesis: String) {
        // Vosk returns a JSON string. e.g., { "text": "my command" }
        // We need to parse the actual text.
        try {
            val key = "\"text\""
            if (hypothesis.contains(key)) {
                val result = hypothesis.substringAfter(key).split("\"")[1]
                Log.d(TAG, "Vosk Result: $result")
                if (result.isNotBlank()) {
                    parseCommandWithLLM(result)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not parse Vosk result", e)
        }
    }

    private fun parseCommandWithLLM(command: String) {
        if (llamaContext == null) {
            Log.e(TAG, "LLM context is null, cannot parse command. Falling back to keyword matching.")
            // Fallback to simple keyword matching if LLM is not available
            if (command.contains("contesta", ignoreCase = true) || command.contains("answer", ignoreCase = true)) {
                CallManager.answer()
            } else if (command.contains("cuelga", ignoreCase = true) || command.contains("hang up", ignoreCase = true)) {
                CallManager.hangup()
            }
            return
        }

        val prompt = """
        From the user's command, identify the action to take. The possible actions are: ANSWER, REJECT, SPEAKER_ON, SPEAKER_OFF, UNKNOWN. Respond with only one of these actions.

        User command: "Cuelga la llamada."
        Action: REJECT

        User command: "Ok, descuelga, hablo yo."
        Action: ANSWER

        User command: "Pon el altavoz."
        Action: SPEAKER_ON

        User command: "$command"
        Action:
        """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val intent = llamaContext?.complete(prompt)?.trim()
                Log.d(TAG, "LLM Intent: $intent")
                when (intent) {
                    "ANSWER" -> CallManager.answer()
                    "REJECT" -> CallManager.hangup()
                    "SPEAKER_ON" -> { /* TODO: CallManager.setSpeaker(true) */ }
                    // Add other cases here
                    else -> Log.w(TAG, "Unknown intent: $intent")
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM intent parsing failed", e)
            }
        }
    }

    override fun onFinalResult(hypothesis: String) { /* Not used */ }
    override fun onPartialResult(hypothesis: String) { /* Not used */ }
    override fun onError(e: Exception) { Log.e(TAG, "Vosk Error", e) }
    override fun onTimeout() { speechService?.startListening(this) }
}

@Composable
fun CallScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Incoming Call...", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Listening for your command...", style = MaterialTheme.typography.bodyLarge)

        // Buttons can be removed once voice commands are fully trusted
        Spacer(modifier = Modifier.height(100.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = { CallManager.answer() }) {
                Text("Answer")
            }
            Button(onClick = { CallManager.hangup() }) {
                Text("Hang Up")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallScreenPreview() {
    RitsuAITheme {
        CallScreen()
    }
}
