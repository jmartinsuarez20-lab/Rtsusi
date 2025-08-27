package com.ritsu.ai_assistant

import android.os.Bundle
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

class CallActivity : ComponentActivity() {
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
    }
}

@Composable
fun CallScreen() {
    // In a real app, we would collect the call state and update the UI accordingly
    // For example, show caller ID, call duration, etc.
    // val call = CallManager.call.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Incoming Call...", style = MaterialTheme.typography.headlineMedium)
        // TODO: Display caller number from CallManager.call

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
