package com.ritsu.ai_assistant

import android.content.Context
import android.util.Log
import gg.gger.llama.cpp.java.bindings.LlamaContext
import gg.gger.llama.cpp.java.bindings.LlamaContextParams
import java.io.File
import java.io.FileOutputStream

/**
 * Manages the lifecycle of the LlamaContext to ensure the heavy AI model is
 * loaded into memory only once. This singleton provides a central point of access
 * for any component that needs to interact with the LLM.
 */
object LlamaManager {
    private var llamaContext: LlamaContext? = null
    private var isInitialized = false
    private val TAG = "LlamaManager"

    fun get(): LlamaContext? {
        if (!isInitialized) {
            Log.e(TAG, "LlamaManager has not been initialized!")
            return null
        }
        return llamaContext
    }

    fun init(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "LlamaManager is already initialized.")
            return
        }
        try {
            val modelPath = getModelPathFromAssets(context)
            val params = LlamaContextParams()
            // TODO: Configure params for better performance (e.g., thread count)
            llamaContext = LlamaContext(modelPath, params)
            isInitialized = true
            Log.d(TAG, "LlamaManager initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LlamaManager", e)
        }
    }

    private fun getModelPathFromAssets(context: Context): String {
        val modelName = "ritsu_model.gguf"
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

    fun close() {
        if (isInitialized) {
            llamaContext?.close()
            llamaContext = null
            isInitialized = false
            Log.d(TAG, "LlamaManager closed.")
        }
    }
}
