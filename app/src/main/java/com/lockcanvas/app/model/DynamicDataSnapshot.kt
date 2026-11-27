package com.lockcanvas.app.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DynamicDataSnapshot(
    val timeText: String,
    val dateText: String,
    val hourOfDay: Int,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val chargingLabel: String,
    val monthOfYear: Int = 1,
    val dayOfMonth: Int = 1,
    val weatherKind: String = WEATHER_UNKNOWN,
    val weatherTempText: String = "",
    val nextEventText: String = "",
    val minutesToNextEvent: Int = -1,
) {
    companion object {
        const val WEATHER_UNKNOWN = "unknown"
        const val WEATHER_CLEAR = "clear"
        const val WEATHER_CLOUDY = "cloudy"
        const val WEATHER_RAIN = "rain"
        const val WEATHER_SNOW = "snow"

        fun sample(
            timeText: String = "09:41",
            dateText: String = "Mon, Nov 23",
            hourOfDay: Int = 9,
            batteryPercent: Int = 82,
            isCharging: Boolean = false,
            chargingLabel: String = if (isCharging) "Charging" else "On Battery",
            monthOfYear: Int = 11,
            dayOfMonth: Int = 23,
            weatherKind: String = WEATHER_CLEAR,
            weatherTempText: String = "18℃",
            nextEventText: String = "14:00 会議",
            minutesToNextEvent: Int = 120,
        ): DynamicDataSnapshot = DynamicDataSnapshot(
            timeText = timeText,
            dateText = dateText,
            hourOfDay = hourOfDay.coerceIn(0, 23),
            batteryPercent = batteryPercent.coerceIn(0, 100),
            isCharging = isCharging,
            chargingLabel = chargingLabel,
            monthOfYear = monthOfYear.coerceIn(1, 12),
            dayOfMonth = dayOfMonth.coerceIn(1, 31),
            weatherKind = weatherKind,
            weatherTempText = weatherTempText,
            nextEventText = nextEventText,
            minutesToNextEvent = minutesToNextEvent,
        )

        fun fromSystem(
            batteryPercent: Int,
            isCharging: Boolean,
            nowMillis: Long = System.currentTimeMillis(),
            locale: Locale = Locale.getDefault(),
            weatherKind: String = WEATHER_UNKNOWN,
            weatherTempText: String = "",
            nextEventText: String = "",
            minutesToNextEvent: Int = -1,
        ): DynamicDataSnapshot {
            val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val date = Date(nowMillis)
            val isJapanese = locale.language == Locale.JAPANESE.language
            val timeFormat = SimpleDateFormat("HH:mm", locale)
            val dateFormat = SimpleDateFormat(
                if (isJapanese) "M月d日 (E)" else "EEE, MMM d",
                locale,
            )
            val clampedBattery = batteryPercent.coerceIn(0, 100)
            val label = if (isJapanese) {
                when {
                    isCharging && clampedBattery >= 100 -> "充電完了"
                    isCharging -> "充電中"
                    clampedBattery <= 15 -> "バッテリー低下"
                    else -> "バッテリー駆動"
                }
            } else {
                when {
                    isCharging && clampedBattery >= 100 -> "Charged"
                    isCharging -> "Charging"
                    clampedBattery <= 15 -> "Low Battery"
                    else -> "On Battery"
                }
            }
            return DynamicDataSnapshot(
                timeText = timeFormat.format(date),
                dateText = dateFormat.format(date),
                hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
                batteryPercent = clampedBattery,
                isCharging = isCharging,
                chargingLabel = label,
                monthOfYear = calendar.get(Calendar.MONTH) + 1,
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                weatherKind = weatherKind,
                weatherTempText = weatherTempText,
                nextEventText = nextEventText,
                minutesToNextEvent = minutesToNextEvent,
            )
        }
    }
}
