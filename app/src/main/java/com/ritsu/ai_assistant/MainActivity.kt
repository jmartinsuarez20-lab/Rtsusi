@file:OptIn(ExperimentalMaterial3Api::class)

package com.ritsu.ai_assistant

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritsu.ai_assistant.ui.theme.RitsuAITheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- ViewModel ---
class ChatViewModel(private val context: Context) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        _messages.value = listOf(Message("Ritsu", "¡Hola! Soy Ritsu. La IA está desactivada, te responderé como un eco.", Message.Author.ASSISTANT))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _messages.value = _messages.value + Message("Tú", text, Message.Author.USER)
        _isTyping.value = true

        viewModelScope.launch {
            delay(1000) // Simula que está "pensando"
            val response = "Has dicho: '$text'"
            _messages.value = _messages.value + Message("Ritsu", response, Message.Author.ASSISTANT)
            _isTyping.value = false
        }
    }
}

// --- Data Classes ---
data class Message(val author: String, val content: String, val type: Author) {
    enum class Author { USER, ASSISTANT }
}

// --- MainActivity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RitsuAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val viewModel = remember { ChatViewModel(context) }
                    MainScreen(viewModel)
                }
            }
        }
    }
}

// --- UI Composables ---
@Composable
fun MainScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ritsu AI Assistant") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            MessageList(messages = messages, modifier = Modifier.weight(1f))
            if (isTyping) {
                TypingIndicator()
            }
            MessageInput(onMessageSent = { viewModel.sendMessage(it) })
        }
    }
}

@Composable
fun MessageList(messages: List<Message>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(8.dp), reverseLayout = true) {
        items(messages.reversed()) { message ->
            MessageBubble(message)
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.type == Message.Author.USER) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (message.type == Message.Author.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Ritsu está escribiendo...")
        Spacer(modifier = Modifier.width(8.dp))
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
    }
}

@Composable
fun MessageInput(onMessageSent: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Escribe un mensaje...") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = {
            if (text.isNotBlank()) {
                onMessageSent(text)
                text = ""
            }
        }),
        trailingIcon = {
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    onMessageSent(text)
                    text = ""
                }
            }) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar mensaje")
            }
        }
    )
}