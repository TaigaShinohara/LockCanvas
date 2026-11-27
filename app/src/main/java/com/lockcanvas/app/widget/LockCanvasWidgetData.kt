package com.lockcanvas.app.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data sources for the expanded widget: the next calendar event and the
 * current weather (Open-Meteo, no API key needed). Both results are cached
 * in SharedPreferences by [LockCanvasWidgetProvider]; this object only knows
 * how to fetch a fresh value. Every failure returns null so the caller keeps
 * the previous cache.
 */
object LockCanvasWidgetData {

    const val EVENT_NEEDS_PERMISSION = "次の予定：アプリで許可してね"
    const val EVENT_NONE = "次の予定なし"
    const val WEATHER_UNKNOWN = "天気 --"

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /** Next event within 7 days, formatted like "14:00 会議" or "11/30 9:30 歯医者". */
    fun fetchNextEventText(context: Context): String? {
        if (!hasCalendarPermission(context)) return EVENT_NEEDS_PERMISSION
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
                if (!cursor.moveToFirst()) return@use EVENT_NONE
                val title = cursor.getString(0)?.ifBlank { "予定" } ?: "予定"
                val begin = cursor.getLong(1)
                val allDay = cursor.getInt(2) == 1
                formatEvent(begin, title, allDay)
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
        return "次の予定：$dayPart$timePart $title"
    }

    /** Current weather like "晴れ 18℃" from Open-Meteo. Null on any failure. */
    fun fetchWeatherText(context: Context): String? {
        if (!hasLocationPermission(context)) return null
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
            "${weatherLabel(code)} ${temp.toInt()}℃"
        }.getOrNull()
    }

    private fun weatherLabel(code: Int): String = when (code) {
        0 -> "晴れ"
        1 -> "晴れ時々曇り"
        2 -> "曇りがち"
        3 -> "曇り"
        45, 48 -> "霧"
        51, 53, 55 -> "霧雨"
        56, 57 -> "雨"
        61 -> "小雨"
        63 -> "雨"
        65 -> "大雨"
        66, 67 -> "雨"
        71, 73, 75, 77 -> "雪"
        80, 81, 82 -> "にわか雨"
        85, 86 -> "雪"
        95 -> "雷雨"
        96, 99 -> "雷雨"
        else -> "天気"
    }
}
