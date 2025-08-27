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

        // TODO: Add runtime permission request for RECORD_AUDIO
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
            Log.e(TAG, "Model not initialized")
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
        val result = hypothesis // This is the recognized text
        Log.d(TAG, "Vosk Result: $result")
        // TODO: Process this result to find commands like "answer" or "hang up"
    }

    override fun onFinalResult(hypothesis: String) {
        // Not used for continuous listening
    }

    override fun onPartialResult(hypothesis: String) {
        // Not used for this implementation
    }

    override fun onError(e: Exception) {
        Log.e(TAG, "Vosk Error", e)
    }

    override fun onTimeout() {
        // Restart listening if needed
        speechService?.startListening(this)
    }
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
