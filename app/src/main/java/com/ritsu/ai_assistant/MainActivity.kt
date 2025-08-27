package com.ritsu.ai_assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import coil.compose.rememberAsyncImagePainter
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

    private var llamaContext: LlamaContext? = null
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
            try {
                val modelPath = getModelPathFromAssets()
                val params = LlamaContextParams()
                llamaContext = LlamaContext(modelPath, params)
                _messages.value = listOf(ChatMessage("Hello! I am Ritsu. I can learn from our conversation. How can I help you?", Author.BOT))
            } catch (e: Exception) {
                _messages.value = listOf(ChatMessage("Error loading model: ${e.message}", Author.BOT))
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

    private fun getModelPathFromAssets(): String {
        val modelName = "placeholder_model.gguf"
        val modelFile = File(context.filesDir, modelName)
        if (!modelFile.exists()) {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return modelFile.absolutePath
    }

    fun sendMessage(text: String) {
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
                learnNewFact(text)
                val memories = memoryDao.getAllMemoriesList()
                val memoryContext = if (memories.isNotEmpty()) {
                    "You remember these facts about the user:\n" + memories.joinToString("\n") { "- ${it.fact}" }
                } else { "" }

                val prompt = """
                $memoryContext
                Current conversation:
                User: $text
                Assistant:
                """.trimIndent()
                val response = llamaContext?.complete(prompt) ?: "Sorry, I could not generate a response."
                _messages.value = _messages.value.dropLast(1) + ChatMessage(response, Author.BOT)
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1) + ChatMessage("Error: ${e.message}", Author.BOT)
            } finally {
                _isThinking.value = false
            }
        }
    }

    private suspend fun learnNewFact(userInput: String) {
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
            val potentialFact = llamaContext?.complete(factExtractionPrompt)?.trim()
            if (!potentialFact.isNullOrBlank() && !potentialFact.equals("NONE", ignoreCase = true)) {
                memoryDao.insert(Memory(fact = potentialFact))
            }
        } catch (e: Exception) { /* Failed to extract a fact, which is fine. */ }
    }

    override fun onCleared() {
        super.onCleared()
        llamaContext?.close()
        database.close()
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LAppPal.initialize(this)
        setContent {
            RitsuAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val chatViewModel: ChatViewModel = viewModel(
                        factory = ChatViewModelFactory(context.applicationContext)
                    )
                    LauncherScreen(chatViewModel)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        LAppPal.release()
    }
}

@Composable
fun LauncherScreen(chatViewModel: ChatViewModel) {
    val apps by chatViewModel.installedApps.collectAsState()
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
    }
    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasPermissions) {
            Box(modifier = Modifier.height(250.dp)) {
                ConversationView(chatViewModel = chatViewModel)
            }
            Button(onClick = {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                context.startActivity(intent)
            }) { Text("Set as Default Phone App") }
            Button(onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }) { Text("Enable Messaging Service") }
            AppDrawer(apps = apps, modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ritsu needs permissions to function properly.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { launcher.launch(permissions) }) { Text("Grant Permissions") }
            }
        }
    }
}

@Composable
fun ConversationView(chatViewModel: ChatViewModel) {
    val messages by chatViewModel.messages.collectAsState()
    val isThinking by chatViewModel.isThinking.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val avatarAlpha by animateFloatAsState(targetValue = if (isThinking) 0.5f else 1.0f, label = "avatarAlpha")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    LAppView(context).apply {
                        LAppLive2DManager.getInstance().changeScene("haru")
                    }
                },
                modifier = Modifier
                    .size(200.dp)
                    .alpha(avatarAlpha)
            ) { view ->
                view.onUpdate()
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(8.dp), reverseLayout = true) {
                items(messages.reversed()) { message -> MessageBubble(message) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value =inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Or type a message...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    chatViewModel.sendMessage(inputText)
                    inputText = ""
                }
            }) { Text("Send") }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val horizontalArrangement = if (message.author == Author.USER) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.author == Author.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = horizontalArrangement) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Text(text = message.text, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
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
