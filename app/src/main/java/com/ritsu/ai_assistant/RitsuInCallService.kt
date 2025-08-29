package com.ritsu.ai_assistant

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class RitsuInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d("RitsuInCallService", "Llamada añadida: ${call.details}")

        // Inicia tu actividad de llamada personalizada
        // NOTA: CallActivity.kt está desactivado, por lo que estas líneas se comentan
        // para evitar un error de compilación. Necesitarás volver a activarlas
        // y arreglar CallActivity.kt para que esta funcionalidad trabaje.
        // val intent = Intent(this, CallActivity::class.java)
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("RitsuInCallService", "Llamada eliminada: ${call.details}")
    }
}