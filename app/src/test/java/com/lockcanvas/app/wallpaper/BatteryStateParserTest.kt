package com.lockcanvas.app.wallpaper

import android.os.BatteryManager
import com.lockcanvas.app.model.AnimationStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStateParserTest {

    @Test
    fun parse_normalLevels_computesRoundedPercentage() {
        val half = BatteryStateParser.parse(
            level = 50,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
        )
        assertEquals(50, half.percent)
        assertFalse(half.isCharging)

        val scaled = BatteryStateParser.parse(
            level = 3,
            scale = 4,
            status = BatteryManager.BATTERY_STATUS_NOT_CHARGING,
        )
        assertEquals(75, scaled.percent)
        assertFalse(scaled.isCharging)
    }

    @Test
    fun parse_boundaryZeroAndHundredPercent() {
        val empty = BatteryStateParser.parse(
            level = 0,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
        )
        assertEquals(0, empty.percent)
        assertFalse(empty.isCharging)

        val full = BatteryStateParser.parse(
            level = 100,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_FULL,
        )
        assertEquals(100, full.percent)
        assertTrue(full.isCharging)
    }

    @Test
    fun parse_invalidOrNegativeScaleAndLevel_usesClampedFallback() {
        val zeroScale = BatteryStateParser.parse(
            level = 40,
            scale = 0,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
            fallbackPercent = 65,
        )
        assertEquals(65, zeroScale.percent)
        assertFalse(zeroScale.isCharging)

        val negativeScale = BatteryStateParser.parse(
            level = 40,
            scale = -100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            fallbackPercent = 120,
        )
        assertEquals(100, negativeScale.percent)
        assertTrue(negativeScale.isCharging)

        val negativeLevel = BatteryStateParser.parse(
            level = -1,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            fallbackPercent = -10,
        )
        assertEquals(0, negativeLevel.percent)
        assertFalse(negativeLevel.isCharging)
    }

    @Test
    fun parse_levelExceedingScale_clampsToHundred() {
        val over = BatteryStateParser.parse(
            level = 125,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
        )
        assertEquals(100, over.percent)
        assertTrue(over.isCharging)
    }

    @Test
    fun parse_chargingFullDischargingStates() {
        assertTrue(
            BatteryStateParser.parse(50, 100, BatteryManager.BATTERY_STATUS_CHARGING).isCharging,
        )
        assertTrue(
            BatteryStateParser.parse(100, 100, BatteryManager.BATTERY_STATUS_FULL).isCharging,
        )
        assertFalse(
            BatteryStateParser.parse(50, 100, BatteryManager.BATTERY_STATUS_DISCHARGING).isCharging,
        )
        assertFalse(
            BatteryStateParser.parse(50, 100, BatteryManager.BATTERY_STATUS_NOT_CHARGING).isCharging,
        )
        assertFalse(
            BatteryStateParser.parse(50, 100, BatteryManager.BATTERY_STATUS_UNKNOWN).isCharging,
        )
    }

    @Test
    fun framePolicy_stopsLoopWhenHiddenOrStaticOrInvalidSurface() {
        assertFalse(
            WallpaperFramePolicy.shouldRunAnimationLoop(
                isVisible = false,
                hasValidSurface = true,
                animationStyle = AnimationStyle.FLOATING_PARTICLES,
            ),
        )
        assertFalse(
            WallpaperFramePolicy.shouldRunAnimationLoop(
                isVisible = true,
                hasValidSurface = false,
                animationStyle = AnimationStyle.PULSE_GLOW,
            ),
        )
        assertFalse(
            WallpaperFramePolicy.shouldRunAnimationLoop(
                isVisible = true,
                hasValidSurface = true,
                animationStyle = AnimationStyle.NONE,
            ),
        )
        assertTrue(
            WallpaperFramePolicy.shouldRunAnimationLoop(
                isVisible = true,
                hasValidSurface = true,
                animationStyle = AnimationStyle.FLOATING_PARTICLES,
            ),
        )
        assertTrue(
            WallpaperFramePolicy.shouldRunAnimationLoop(
                isVisible = true,
                hasValidSurface = true,
                animationStyle = AnimationStyle.PULSE_GLOW,
            ),
        )
    }

    @Test
    fun framePolicy_intervalAndSingleFrameGuards() {
        assertNull(WallpaperFramePolicy.frameIntervalMs(AnimationStyle.NONE))
        assertEquals(
            WallpaperFramePolicy.ANIMATED_FRAME_INTERVAL_MS,
            WallpaperFramePolicy.frameIntervalMs(AnimationStyle.FLOATING_PARTICLES),
        )
        assertEquals(
            WallpaperFramePolicy.ANIMATED_FRAME_INTERVAL_MS,
            WallpaperFramePolicy.frameIntervalMs(AnimationStyle.PULSE_GLOW),
        )

        assertTrue(
            WallpaperFramePolicy.shouldDrawSingleFrame(
                isVisible = true,
                hasValidSurface = true,
                width = 1080,
                height = 2400,
            ),
        )
        assertFalse(
            WallpaperFramePolicy.shouldDrawSingleFrame(
                isVisible = false,
                hasValidSurface = true,
                width = 1080,
                height = 2400,
            ),
        )
        assertFalse(
            WallpaperFramePolicy.shouldDrawSingleFrame(
                isVisible = true,
                hasValidSurface = false,
                width = 1080,
                height = 2400,
            ),
        )
        assertFalse(
            WallpaperFramePolicy.shouldDrawSingleFrame(
                isVisible = true,
                hasValidSurface = true,
                width = 0,
                height = 2400,
            ),
        )
    }
}
