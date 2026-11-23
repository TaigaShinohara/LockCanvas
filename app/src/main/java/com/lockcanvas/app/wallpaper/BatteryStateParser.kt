package com.lockcanvas.app.wallpaper

import android.content.Intent
import android.os.BatteryManager
import com.lockcanvas.app.model.AnimationStyle
import kotlin.math.roundToInt

data class BatteryState(
    val percent: Int,
    val isCharging: Boolean,
)

object BatteryStateParser {
    const val DEFAULT_BATTERY_PERCENT: Int = 50

    fun parse(
        level: Int,
        scale: Int,
        status: Int,
        fallbackPercent: Int = DEFAULT_BATTERY_PERCENT,
    ): BatteryState {
        val percent = if (level < 0 || scale <= 0) {
            fallbackPercent.coerceIn(0, 100)
        } else {
            ((level.toFloat() / scale.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(
            percent = percent,
            isCharging = isCharging,
        )
    }

    fun parseIntent(
        intent: Intent?,
        fallback: BatteryState = BatteryState(
            percent = DEFAULT_BATTERY_PERCENT,
            isCharging = false,
        ),
    ): BatteryState {
        if (intent == null) return fallback
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )
        return parse(
            level = level,
            scale = scale,
            status = status,
            fallbackPercent = fallback.percent,
        )
    }
}

object WallpaperFramePolicy {
    const val ANIMATED_FRAME_INTERVAL_MS: Long = 33L

    fun shouldRunAnimationLoop(
        isVisible: Boolean,
        hasValidSurface: Boolean,
        animationStyle: AnimationStyle,
    ): Boolean = isVisible && hasValidSurface && animationStyle != AnimationStyle.NONE

    fun frameIntervalMs(animationStyle: AnimationStyle): Long? = when (animationStyle) {
        AnimationStyle.NONE -> null
        AnimationStyle.FLOATING_PARTICLES,
        AnimationStyle.PULSE_GLOW,
        -> ANIMATED_FRAME_INTERVAL_MS
    }

    fun shouldDrawSingleFrame(
        isVisible: Boolean,
        hasValidSurface: Boolean,
        width: Int,
        height: Int,
    ): Boolean = isVisible && hasValidSurface && width > 0 && height > 0
}
