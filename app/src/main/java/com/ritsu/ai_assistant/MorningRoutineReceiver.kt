package com.ritsu.ai_assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MorningRoutineReceiver : BroadcastReceiver() {

    private val TAG = "MorningRoutineReceiver"
    companion object {
        const val ACTION_SPEAK_BRIEFING = "com.ritsu.ai_assistant.SPEAK_BRIEFING"
        const val EXTRA_BRIEFING_TEXT = "BRIEFING_TEXT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d(TAG, "User present event received. Checking for morning routine.")
            // Using goAsync to allow for asynchronous work off the main thread.
            val pendingResult = goAsync()
            Thread {
                try {
                    val proactiveManager = ProactiveManager(context)
                    proactiveManager.runMorningRoutineIfNeeded { briefingText ->
                        val speakIntent = Intent(ACTION_SPEAK_BRIEFING).apply {
                            putExtra(EXTRA_BRIEFING_TEXT, briefingText)
                            // Important: Set package to ensure only our app receives it.
                            `package` = context.packageName
                        }
                        context.sendBroadcast(speakIntent)
                        Log.d(TAG, "Broadcast sent to speak the briefing.")
                    }
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }
}
