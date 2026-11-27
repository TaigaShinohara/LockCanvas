package com.lockcanvas.app.wallpaper

import android.content.Context
import android.location.LocationManager
import android.provider.CalendarContract
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.widget.LockCanvasWidgetData
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Environment data for the wallpaper engine: the current weather kind and
 * the next calendar event, feeding the 空模様 and 次の予定 presets through
 * [DynamicDataSnapshot]. Mirrors the widget's sources (Open-Meteo, the
 * device calendar) but returns structured values instead of display text.
 * Every failure returns null so the engine keeps its previous values.
 */
object EnvironmentDataProvider {

    data class Weather(val kind: String, val tempText: String)

    data class NextEvent(val text: String, val startMs: Long)

    fun fetchWeather(context: Context): Weather? {
        if (!LockCanvasWidgetData.hasLocationPermission(context)) return null
        return runCatching {
            val locationManager =
                context.getSystemService(LocationManager::class.java) ?: return null
            val location = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
            ).firstNotNullOfOrNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            } ?: return null

            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}&longitude=${location.longitude}" +
                    "&current=temperature_2m,weather_code&timezone=auto",
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val code = current.getInt("weather_code")
            Weather(kind = weatherKind(code), tempText = "${temp.toInt()}℃")
        }.getOrNull()
    }

    fun weatherKind(code: Int): String = when (code) {
        0, 1 -> DynamicDataSnapshot.WEATHER_CLEAR
        2, 3, 45, 48 -> DynamicDataSnapshot.WEATHER_CLOUDY
        71, 73, 75, 77, 85, 86 -> DynamicDataSnapshot.WEATHER_SNOW
        else -> DynamicDataSnapshot.WEATHER_RAIN
    }

    /** Next event within 7 days as ("14:00 会議", minutes until it starts). */
    fun fetchNextEvent(context: Context): NextEvent? {
        if (!LockCanvasWidgetData.hasCalendarPermission(context)) return null
        return runCatching {
            val now = System.currentTimeMillis()
            val end = now + 7L * 24 * 60 * 60 * 1000
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
                appendPath(now.toString())
                appendPath(end.toString())
            }.build()
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            )
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use NextEvent(text = "次の予定なし", startMs = -1L)
                }
                val title = cursor.getString(0)?.ifBlank { "予定" } ?: "予定"
                val begin = cursor.getLong(1)
                val allDay = cursor.getInt(2) == 1
                NextEvent(
                    text = formatEvent(begin, title, allDay),
                    startMs = if (allDay) -1L else begin,
                )
            }
        }.getOrNull()
    }

    private fun formatEvent(beginMs: Long, title: String, allDay: Boolean): String {
        val begin = Calendar.getInstance().apply { timeInMillis = beginMs }
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        fun sameDay(a: Calendar, b: Calendar): Boolean =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        val timePart = when {
            allDay -> "終日"
            else -> SimpleDateFormat("H:mm", Locale.JAPAN).format(Date(beginMs))
        }
        val dayPart = when {
            sameDay(begin, today) -> ""
            sameDay(begin, tomorrow) -> "明日 "
            else -> SimpleDateFormat("M/d ", Locale.JAPAN).format(Date(beginMs))
        }
        return "$dayPart$timePart $title"
    }
}
