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
) {
    companion object {
        fun sample(
            timeText: String = "09:41",
            dateText: String = "Mon, Nov 23",
            hourOfDay: Int = 9,
            batteryPercent: Int = 82,
            isCharging: Boolean = false,
            chargingLabel: String = if (isCharging) "Charging" else "On Battery",
            monthOfYear: Int = 11,
            dayOfMonth: Int = 23,
        ): DynamicDataSnapshot = DynamicDataSnapshot(
            timeText = timeText,
            dateText = dateText,
            hourOfDay = hourOfDay.coerceIn(0, 23),
            batteryPercent = batteryPercent.coerceIn(0, 100),
            isCharging = isCharging,
            chargingLabel = chargingLabel,
            monthOfYear = monthOfYear.coerceIn(1, 12),
            dayOfMonth = dayOfMonth.coerceIn(1, 31),
        )

        fun fromSystem(
            batteryPercent: Int,
            isCharging: Boolean,
            nowMillis: Long = System.currentTimeMillis(),
            locale: Locale = Locale.getDefault(),
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
            )
        }
    }
}
