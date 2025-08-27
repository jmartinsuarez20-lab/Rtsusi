package com.ritsu.ai_assistant

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class RitsuInCallService : InCallService() {

    private val TAG = "RitsuInCallService"

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call Added: $call")
        // Here we would launch our own call UI
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call Removed: $call")
    }
}
