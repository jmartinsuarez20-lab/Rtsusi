package com.ritsu.ai_assistant

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RitsuAccessibilityService : AccessibilityService() {

    private val TAG = "RitsuAccessibility"
    private val TARGET_PACKAGES = setOf("com.whatsapp", "org.telegram.messenger", "com.google.android.apps.messaging")

    companion object {
        const val ACTION_MESSAGE_RECEIVED = "com.ritsu.ai_assistant.MESSAGE_RECEIVED"
        const val EXTRA_SENDER = "SENDER"
        const val EXTRA_MESSAGE = "MESSAGE"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            if (event.packageName in TARGET_PACKAGES) {
                val notification = event.parcelableData as? android.app.Notification ?: return
                val extras = notification.extras

                val sender = extras.getString("android.title")
                val message = extras.getCharSequence("android.text")?.toString()

                if (sender != null && message != null) {
                    Log.d(TAG, "Message from $sender: $message")
                    broadcastMessage(sender, message)
                }
            }
        }
    }

    private fun broadcastMessage(sender: String, message: String) {
        val intent = Intent(ACTION_MESSAGE_RECEIVED).apply {
            putExtra(EXTRA_SENDER, sender)
            putExtra(EXTRA_MESSAGE, message)
        }
        sendBroadcast(intent)
    }


    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Ritsu Accessibility Service Connected")
        // You might want to configure the service here
        // serviceInfo = ...
    }
}
