package com.ritsu.ai_assistant

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.provider.CalendarContract
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Handles the logic for Ritsu's proactive features, such as the morning routine.
 * This class is responsible for checking conditions (e.g., time of day),
 * gathering data from various sources (location, weather, calendar), and
 * constructing the briefing text.
 */
class ProactiveManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("RitsuProactivePrefs", Context.MODE_PRIVATE)
    private val TAG = "ProactiveManager"

    companion object {
        private const val LAST_BRIEFING_TIMESTAMP_KEY = "last_briefing_timestamp"
    }

    fun runMorningRoutineIfNeeded(onBriefingReady: (String) -> Unit) {
        if (shouldRunMorningRoutine()) {
            Log.d(TAG, "Conditions met for morning routine. Generating briefing.")
            fetchLocationAndBuildBriefing(onBriefingReady)
        } else {
            Log.d(TAG, "Conditions not met for morning routine. Skipping.")
        }
    }

    private fun fetchLocationAndBuildBriefing(onBriefingReady: (String) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted. Skipping weather.")
            // Continue without weather data
            buildAndDeliverBriefing(null, onBriefingReady)
            return
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            Log.w(TAG, "Could not get last known location. Skipping weather.")
            buildAndDeliverBriefing(null, onBriefingReady)
        } else {
            buildAndDeliverBriefing(location, onBriefingReady)
        }
    }

    private fun buildAndDeliverBriefing(location: Location?, onBriefingReady: (String) -> Unit) {
        // TODO: Replace placeholder with real network call to weather API
        val weather = getWeatherData(location)
        val calendarEvents = getCalendarEvents()

        val briefing = buildBriefing(weather, calendarEvents)
        onBriefingReady(briefing)

        prefs.edit().putLong(LAST_BRIEFING_TIMESTAMP_KEY, System.currentTimeMillis()).apply()
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

    private fun getWeatherData(location: Location?): String {
        if (location == null) {
            return "I couldn't determine your location to get the weather."
        }
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
            val response = reader.readText()
            reader.close()

            val jsonResponse = JSONObject(response)
            val currentWeather = jsonResponse.getJSONObject("current_weather")
            val temp = currentWeather.getDouble("temperature")
            val weatherCode = currentWeather.getInt("weathercode")

            val weatherDescription = weatherCodeToString(weatherCode)
            return "It's currently ${temp.toInt()} degrees and $weatherDescription."
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather data", e)
            return "I had trouble fetching the weather."
        }
    }

    private fun weatherCodeToString(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear, partly cloudy, or overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzling"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Raining"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snowing"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "the weather is unusual"
        }
    }

    private fun getCalendarEvents(): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return "I can't access your calendar."
        }

        val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
        val selection: String
        val selectionArgs: Array<String>

        // Get events for today
        val beginTime = Calendar.getInstance()
        beginTime.set(Calendar.HOUR_OF_DAY, 0)
        beginTime.set(Calendar.MINUTE, 0)
        beginTime.set(Calendar.SECOND, 0)
        val endTime = Calendar.getInstance()
        endTime.set(Calendar.HOUR_OF_DAY, 23)
        endTime.set(Calendar.MINUTE, 59)
        endTime.set(Calendar.SECOND, 59)

        selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        selectionArgs = arrayOf(beginTime.timeInMillis.toString(), endTime.timeInMillis.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            CalendarContract.Events.DTSTART + " ASC"
        )

        val events = mutableListOf<Pair<String, Long>>()
        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(0)
                val dtstart = it.getLong(1)
                events.add(Pair(title, dtstart))
            }
        }

        return if (events.isEmpty()) {
            "You have no events scheduled for today."
        } else {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val eventStrings = events.map { "${it.first} at ${timeFormat.format(Date(it.second))}" }
            "You have ${events.size} event(s) today: ${eventStrings.joinToString(", ")}."
        }
    }

    private fun buildBriefing(weather: String, calendarEvents: String): String {
        return "Good morning! Here is your daily briefing. $weather $calendarEvents Have a great day!"
    }
}
