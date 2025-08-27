package com.ritsu.ai_assistant

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

class ProactiveManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("RitsuProactivePrefs", Context.MODE_PRIVATE)
    private val TAG = "ProactiveManager"

    companion object {
        private const val LAST_BRIEFING_TIMESTAMP_KEY = "last_briefing_timestamp"
    }

    fun runMorningRoutineIfNeeded(onBriefingReady: (String) -> Unit) {
        if (shouldRunMorningRoutine()) {
            Log.d(TAG, "Conditions met for morning routine. Generating briefing.")
            val weather = getWeatherData()
            val calendarEvents = getCalendarEvents()

            val briefing = buildBriefing(weather, calendarEvents)
            onBriefingReady(briefing)

            prefs.edit().putLong(LAST_BRIEFING_TIMESTAMP_KEY, System.currentTimeMillis()).apply()
        } else {
            Log.d(TAG, "Conditions not met for morning routine. Skipping.")
        }
    }

    private fun shouldRunMorningRoutine(): Boolean {
        val lastBriefingTimestamp = prefs.getLong(LAST_BRIEFING_TIMESTAMP_KEY, 0)
        val now = Calendar.getInstance()
        val lastBriefing = Calendar.getInstance().apply { timeInMillis = lastBriefingTimestamp }

        val isNewDay = now.get(Calendar.DAY_OF_YEAR) != lastBriefing.get(Calendar.DAY_OF_YEAR) ||
                       now.get(Calendar.YEAR) != lastBriefing.get(Calendar.YEAR)

        // Only run between 6 AM and 11 AM
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val isMorning = hour in 6..11

        return isNewDay && isMorning
    }

    private fun getWeatherData(): String {
        // Placeholder: In a real implementation, this would fetch data from a weather API
        // using the user's location.
        Log.d(TAG, "Fetching weather data (Placeholder)")
        return "It's currently 15 degrees and sunny."
    }

    private fun getCalendarEvents(): String {
        // Placeholder: In a real implementation, this would query the CalendarProvider.
        Log.d(TAG, "Fetching calendar events (Placeholder)")
        return "You have 2 events today. A meeting at 10 AM, and lunch at 1 PM."
    }

    private fun buildBriefing(weather: String, calendarEvents: String): String {
        return "Good morning! Here is your daily briefing. $weather $calendarEvents Have a great day!"
    }
}
