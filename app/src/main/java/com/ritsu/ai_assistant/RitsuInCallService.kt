package com.ritsu.ai_assistant

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class RitsuInCallService : InCallService() {

    private val TAG = "RitsuInCallService"
    private var callHandler: CallHandler? = null

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call Added: $call")
        CallManager.onCallAdded(call)

        // TODO: Add a setting to enable/disable this feature
        // For now, automatically answer and let Ritsu handle it.
        call.answer(0)

        callHandler = CallHandler(
            context = this,
            onHangup = { CallManager.hangup() }
        )

        // We still launch the activity so the user has a UI to see the call status
        val intent = Intent(this, CallActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call Removed: $call")
        callHandler?.shutdown()
        callHandler = null
        CallManager.onCallRemoved()
    }
}
