package com.ritsu.ai_assistant

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.ritsu.ai_assistant.live2d.LAppLive2DManager
import com.ritsu.ai_assistant.live2d.LAppPal
import com.ritsu.ai_assistant.live2d.LAppView
import com.ritsu.ai_assistant.ui.theme.RitsuAITheme
import gg.gger.llama.cpp.java.bindings.LlamaContext
import gg.gger.llama.cpp.java.bindings.LlamaContextParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// --- Data Model ---
enum class Author {
    USER, BOT
}
data class ChatMessage(
    val text: String,
    val author: Author,
    val timestamp: Long = System.currentTimeMillis()
)

// --- ViewModel ---
class ChatViewModel(private val context: Context) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        mutableListOf(ChatMessage("Model is loading... please wait.", Author.BOT))
    )
    val messages = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking = _isThinking.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val animationManager: AnimationManager = AnimationManager.getInstance()
    private val ttsManager: TtsManager by lazy {
        TtsManager(
            context,
            onSpeechStarted = { animationManager.startMotion(AnimationManager.Motion.SPEAKING) },
            onSpeechStopped = { animationManager.startMotion(AnimationManager.Motion.IDLE) }
        )
    }
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "ritsu-memory-db"
        ).build()
    }
    private val memoryDao by lazy { database.memoryDao() }

    init {
        loadInstalledApps()
        viewModelScope.launch(Dispatchers.IO) {
            LlamaManager.init(context)
            if (LlamaManager.get() != null) {
                val initialMessage = "Hello! I am Ritsu. I can learn from our conversation. How can I help you?"
                _messages.value = listOf(ChatMessage(initialMessage, Author.BOT))
                ttsManager.speak(initialMessage)
                animationManager.startMotion(AnimationManager.Motion.IDLE)
            } else {
                _messages.value = listOf(ChatMessage("Error loading AI model.", Author.BOT))
            }
        }

        viewModelScope.launch {
            isThinking.collect { thinking ->
                if (thinking) {
                    animationManager.startMotion(AnimationManager.Motion.THINKING)
                } else {
                    // Return to idle only if not speaking. The TTS callback will handle this.
                }
            }
        }
    }

    private fun loadInstalledApps() {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(intent, 0)
        _installedApps.value = apps.map { resolveInfo ->
            AppInfo(
                label = resolveInfo.loadLabel(packageManager),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }.sortedBy { it.label.toString() }
    }

    fun sendMessage(text: String) {
        val llamaContext = LlamaManager.get()
        if (llamaContext == null) {
            _messages.value = _messages.value + ChatMessage("Model is not ready yet.", Author.BOT)
            return
        }

        val userMessage = ChatMessage(text, Author.USER)
        _messages.value = _messages.value + userMessage
        val thinkingMessage = ChatMessage("...", Author.BOT)
        _messages.value = _messages.value + thinkingMessage
        _isThinking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Analyze sentiment
                val sentimentPrompt = """
                    Analyze the sentiment of the following user message. Respond with only one word: HAPPY, SAD, JOKING, ANGRY, or NEUTRAL.
                    Message: "$text"
                    Sentiment:
                """.trimIndent()
                val sentiment = llamaContext.complete(sentimentPrompt)?.trim()?.uppercase() ?: "NEUTRAL"

                // Step 2: Learn new facts (can run in parallel)
                learnNewFact(text)

                // Step 3: Generate response based on sentiment
                val memories = memoryDao.getAllMemoriesList()
                val memoryContext = if (memories.isNotEmpty()) {
                    "You remember these facts about the user:\n" + memories.joinToString("\n") { "- ${it.fact}" }
                } else { "" }

                val personalityMap = mapOf(
                    "HAPPY" to "cheerful and enthusiastic",
                    "SAD" to "kind and empathetic",
                    "JOKING" to "playful and witty",
                    "ANGRY" to "calm and reassuring",
                    "NEUTRAL" to "helpful and direct"
                )
                val personality = personalityMap[sentiment] ?: "helpful and direct"

                val responsePrompt = """
                You are Ritsu, a $personality AI assistant.
                $memoryContext
                Current conversation:
                User: $text
                Assistant:
                """.trimIndent()

                val response = llamaContext.complete(responsePrompt) ?: "Sorry, I could not generate a response."
                _messages.value = _messages.value.dropLast(1) + ChatMessage(response, Author.BOT)
                ttsManager.speak(response)

            } catch (e: Exception) {
                val errorMessage = "Error: ${e.message}"
                _messages.value = _messages.value.dropLast(1) + ChatMessage(errorMessage, Author.BOT)
                ttsManager.speak(errorMessage)
            } finally {
                _isThinking.value = false
            }
        }
    }

    private suspend fun learnNewFact(userInput: String) {
        val llamaContext = LlamaManager.get() ?: return
        val factExtractionPrompt = """
        From the following user statement, extract one key fact to remember for the future as a short sentence. If no important fact is present, respond with only the word "NONE".
        Examples:
        User statement: "My favorite color is blue."
        Fact: User's favorite color is blue.
        User statement: "I live in Paris."
        Fact: User lives in Paris.
        User statement: "ok that sounds good"
        Fact: NONE
        User statement: "$userInput"
        Fact:
        """.trimIndent()
        try {
            val potentialFact = llamaContext.complete(factExtractionPrompt)?.trim()
            if (!potentialFact.isNullOrBlank() && !potentialFact.equals("NONE", ignoreCase = true)) {
                memoryDao.insert(Memory(fact = potentialFact))
            }
        } catch (e: Exception) { /* Failed to extract a fact, which is fine. */ }
    }

    override fun onCleared() {
        super.onCleared()
        LlamaManager.close()
        ttsManager.shutdown()
        database.close()
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }
}

class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- UI ---
class MainActivity : ComponentActivity() {

    private lateinit var hotwordDetector: HotwordDetector
    private lateinit var sttManager: SttManager
    private lateinit var chatViewModel: ChatViewModel
    private val isListening = mutableStateOf(false)

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == RitsuAccessibilityService.ACTION_MESSAGE_RECEIVED) {
                val sender = intent.getStringExtra(RitsuAccessibilityService.EXTRA_SENDER)
                val message = intent.getStringExtra(RitsuAccessibilityService.EXTRA_MESSAGE)
                if (sender != null && message != null) {
                    val announcement = "You have a new message from $sender. It says: $message"
                    chatViewModel.speak(announcement)
                }
            }
        }
    }

    private val briefingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MorningRoutineReceiver.ACTION_SPEAK_BRIEFING) {
                val briefing = intent.getStringExtra(MorningRoutineReceiver.EXTRA_BRIEFING_TEXT)
                if (briefing != null) {
                    chatViewModel.speak(briefing)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LAppPal.initialize(this)

        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(applicationContext)).get(ChatViewModel::class.java)

        sttManager = SttManager(
            context = this,
            onResult = { result ->
                chatViewModel.sendMessage(result)
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "STT Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            },
            onListeningStarted = {
                isListening.value = true
                AnimationManager.getInstance().startMotion(AnimationManager.Motion.LISTENING)
            },
            onListeningStopped = {
                isListening.value = false
                AnimationManager.getInstance().startMotion(AnimationManager.Motion.IDLE)
            }
        )

        hotwordDetector = HotwordDetector(this) {
            runOnUiThread {
                sttManager.startListening()
            }
        }

        setContent {
            RitsuAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LauncherScreen(chatViewModel, hotwordDetector, isListening.value)
                }
            }
        }

        // Register receivers
        registerReceiver(messageReceiver, IntentFilter(RitsuAccessibilityService.ACTION_MESSAGE_RECEIVED))
        registerReceiver(briefingReceiver, IntentFilter(MorningRoutineReceiver.ACTION_SPEAK_BRIEFING))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receivers
        unregisterReceiver(messageReceiver)
        unregisterReceiver(briefingReceiver)

        // Shutdown components
        LAppPal.release()
        hotwordDetector.destroy()
        sttManager.shutdown()
    }
}

@Composable
fun LauncherScreen(chatViewModel: ChatViewModel, hotwordDetector: HotwordDetector, isListening: Boolean) {
    val apps by chatViewModel.installedApps.collectAsState()
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    val permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        hasPermissions = allGranted
        if (allGranted) {
            hotwordDetector.startListening()
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppDrawer(apps = apps, modifier = Modifier.fillMaxSize())

        if (hasPermissions) {
            Box(modifier = Modifier.align(Alignment.TopCenter).height(300.dp).padding(8.dp)) {
                ConversationView(chatViewModel = chatViewModel, isListening = isListening)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to Ritsu AI. To get started, please grant the required permissions. Ritsu needs access to your microphone for voice commands, phone status for call handling, and calendar/location for proactive assistance.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { launcher.launch(permissions) }) { Text("Grant Permissions") }
            }
        }
    }
}

@Composable
fun ConversationView(chatViewModel: ChatViewModel, isListening: Boolean) {
    val isThinking by chatViewModel.isThinking.collectAsState()
    val avatarAlpha by animateFloatAsState(targetValue = if (isThinking) 0.5f else 1.0f, label = "avatarAlpha")

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    LAppView(context).apply {
                        LAppLive2DManager.getInstance().changeScene("haru")
                    }
                },
                modifier = Modifier
                    .size(300.dp)
                    .alpha(avatarAlpha)
            ) { view ->
                view.onUpdate()
            }

            if (isListening) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mic),
                    contentDescription = "Listening...",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConversationViewPreview() {
    RitsuAITheme {
        Text("Preview for Conversation View")
    }
}
