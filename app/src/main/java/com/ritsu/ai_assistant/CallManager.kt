package com.ritsu.ai_assistant

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallManager {
    private val _call = MutableStateFlow<Call?>(null)
    val call = _call.asStateFlow()

    fun onCallAdded(call: Call) {
        _call.value = call
    }

    fun onCallRemoved() {
        _call.value = null
    }

    fun answer() {
        _call.value?.answer(0)
    }

    fun hangup() {
        _call.value?.disconnect()
    }
}
