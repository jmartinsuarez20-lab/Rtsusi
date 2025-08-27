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
        CallManager.onCallAdded(call)
        val intent = Intent(this, CallActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call Removed: $call")
        CallManager.onCallRemoved()
    }
}
