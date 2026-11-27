package com.lockcanvas.app.wallpaper

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.lockcanvas.app.data.DesignRepository
import com.lockcanvas.app.engine.CanvasSceneRenderer
import com.lockcanvas.app.engine.RuleEvaluator
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.PresetDesigns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LockCanvasWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = LockCanvasEngine()

    inner class LockCanvasEngine : Engine() {

        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val renderer = CanvasSceneRenderer(applicationContext)
        private val repository: DesignRepository by lazy {
            DesignRepository.getInstance(applicationContext)
        }

        private var activeDesign: CanvasDesign = PresetDesigns.defaultDesign()
        private var batteryState: BatteryState = BatteryState(
            percent = BatteryStateParser.DEFAULT_BATTERY_PERCENT,
            isCharging = false,
        )

        private var isSurfaceValid: Boolean = false
        private var surfaceWidth: Int = 0
        private var surfaceHeight: Int = 0
        private var engineVisible: Boolean = false
        private var receiversRegistered: Boolean = false

        private var animationJob: Job? = null
        private var animationStartNanos: Long = System.nanoTime()
        private var lastColorSignature: ColorSignature? = null

        // Gyro parallax state. Smoothed tilt in -1..1; redraws happen only
        // while the tilt is actually changing, so a still phone costs nothing.
        private var parallaxX: Float = 0f
        private var parallaxY: Float = 0f
        private var tiltRegistered: Boolean = false
        private var lastParallaxDrawMs: Long = 0L

        private val tiltListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                // Upright portrait hold reads about (0, 9.81, 0): roll comes
                // from X, pitch from Z, both near zero at the neutral pose.
                val targetX = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val targetY = (event.values[2] / 9.81f).coerceIn(-1f, 1f)
                val prevX = parallaxX
                val prevY = parallaxY
                parallaxX += (targetX - parallaxX) * 0.12f
                parallaxY += (targetY - parallaxY) * 0.12f
                val moved = kotlin.math.abs(parallaxX - prevX) +
                    kotlin.math.abs(parallaxY - prevY)
                val now = System.currentTimeMillis()
                if (engineVisible && moved > 0.004f && now - lastParallaxDrawMs >= 33L) {
                    lastParallaxDrawMs = now
                    drawFrame(currentAnimationTimeSec())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        private val systemStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                when (action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        batteryState = BatteryStateParser.parseIntent(intent, fallback = batteryState)
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        batteryState = batteryState.copy(isCharging = true)
                        refreshStickyBatteryState()
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        batteryState = batteryState.copy(isCharging = false)
                        refreshStickyBatteryState()
                    }
                    Intent.ACTION_TIME_TICK,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    -> {
                        // Snapshot time/date is computed from current system clock during render.
                    }
                }
                maybeNotifyColorsChanged()
                if (engineVisible) {
                    drawFrame(currentAnimationTimeSec())
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            refreshStickyBatteryState()
            observeActiveDesign()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            isSurfaceValid = holder.surface?.isValid == true
            val frame = holder.surfaceFrame
            if (frame != null && frame.width() > 0 && frame.height() > 0) {
                surfaceWidth = frame.width()
                surfaceHeight = frame.height()
            }
            val startedLoop = syncAnimationLoop()
            if (engineVisible && !startedLoop) {
                drawFrame(currentAnimationTimeSec())
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            isSurfaceValid = holder.surface?.isValid == true
            val startedLoop = syncAnimationLoop()
            if (engineVisible && !startedLoop) {
                drawFrame(currentAnimationTimeSec())
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isSurfaceValid = false
            stopAnimationLoop()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            engineVisible = visible
            if (visible) {
                registerReceiversIfNeeded()
                registerTiltIfNeeded()
                refreshStickyBatteryState()
                maybeNotifyColorsChanged()
                val startedLoop = syncAnimationLoop()
                if (!startedLoop) {
                    drawFrame(currentAnimationTimeSec())
                }
            } else {
                stopAnimationLoop()
                unregisterReceiversSafely()
                unregisterTiltSafely()
            }
        }

        private fun registerTiltIfNeeded() {
            if (tiltRegistered) return
            val manager = getSystemService(SensorManager::class.java) ?: return
            val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            tiltRegistered = manager.registerListener(
                tiltListener,
                sensor,
                SensorManager.SENSOR_DELAY_UI,
            )
        }

        private fun unregisterTiltSafely() {
            if (!tiltRegistered) return
            runCatching {
                getSystemService(SensorManager::class.java)?.unregisterListener(tiltListener)
            }
            tiltRegistered = false
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors {
            val effective = resolveEffectiveDesign()
            val primary = Color.valueOf(effective.backgroundColorArgb.toInt())
            val secondary = if (effective.backgroundGradientEnabled) {
                Color.valueOf(effective.secondaryBackgroundColorArgb.toInt())
            } else {
                null
            }
            return WallpaperColors(primary, secondary, null)
        }

        override fun onDestroy() {
            engineVisible = false
            isSurfaceValid = false
            stopAnimationLoop()
            unregisterReceiversSafely()
            unregisterTiltSafely()
            engineScope.cancel()
            renderer.clearImageCache()
            super.onDestroy()
        }

        private fun observeActiveDesign() {
            engineScope.launch {
                repository.activeDesignFlow.collect { design ->
                    activeDesign = design
                    maybeNotifyColorsChanged()
                    val startedLoop = syncAnimationLoop()
                    if (engineVisible && !startedLoop) {
                        drawFrame(currentAnimationTimeSec())
                    }
                }
            }
        }

        private fun registerReceiversIfNeeded() {
            if (receiversRegistered) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            val registered = runCatching {
                ContextCompat.registerReceiver(
                    applicationContext,
                    systemStateReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                true
            }.getOrDefault(false)
            receiversRegistered = registered
        }

        private fun unregisterReceiversSafely() {
            if (!receiversRegistered) return
            runCatching {
                applicationContext.unregisterReceiver(systemStateReceiver)
            }
            receiversRegistered = false
        }

        private fun refreshStickyBatteryState() {
            val stickyIntent = runCatching {
                ContextCompat.registerReceiver(
                    applicationContext,
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            }.getOrNull()
            if (stickyIntent != null) {
                batteryState = BatteryStateParser.parseIntent(stickyIntent, fallback = batteryState)
            }
        }

        private fun syncAnimationLoop(): Boolean {
            val shouldAnimate = WallpaperFramePolicy.shouldRunAnimationLoop(
                isVisible = engineVisible,
                hasValidSurface = isSurfaceValid,
                animationStyle = activeDesign.animationStyle,
            )
            if (!shouldAnimate) {
                stopAnimationLoop()
                return false
            }
            if (animationJob?.isActive == true) return false

            animationStartNanos = System.nanoTime()
            animationJob = engineScope.launch {
                while (
                    isActive &&
                    WallpaperFramePolicy.shouldRunAnimationLoop(
                        isVisible = engineVisible,
                        hasValidSurface = isSurfaceValid,
                        animationStyle = activeDesign.animationStyle,
                    )
                ) {
                    drawFrame(currentAnimationTimeSec())
                    val intervalMs = WallpaperFramePolicy.frameIntervalMs(activeDesign.animationStyle)
                        ?: break
                    delay(intervalMs)
                }
            }
            return true
        }

        private fun stopAnimationLoop() {
            animationJob?.cancel()
            animationJob = null
        }

        private fun currentAnimationTimeSec(): Float {
            if (WallpaperFramePolicy.frameIntervalMs(activeDesign.animationStyle) == null) {
                return 0f
            }
            return ((System.nanoTime() - animationStartNanos) / 1_000_000_000.0).toFloat()
        }

        private fun currentSnapshot(): DynamicDataSnapshot =
            DynamicDataSnapshot.fromSystem(
                batteryPercent = batteryState.percent,
                isCharging = batteryState.isCharging,
            )

        private fun resolveEffectiveDesign(): CanvasDesign =
            RuleEvaluator.evaluate(activeDesign, currentSnapshot()).design

        private fun maybeNotifyColorsChanged() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
            val effective = resolveEffectiveDesign()
            val signature = ColorSignature(
                primaryArgb = effective.backgroundColorArgb,
                secondaryArgb = effective.secondaryBackgroundColorArgb,
                gradientEnabled = effective.backgroundGradientEnabled,
            )
            if (signature != lastColorSignature) {
                lastColorSignature = signature
                runCatching { notifyColorsChanged() }
            }
        }

        private fun drawFrame(animationTimeSec: Float) {
            if (!WallpaperFramePolicy.shouldDrawSingleFrame(
                    isVisible = engineVisible,
                    hasValidSurface = isSurfaceValid,
                    width = surfaceWidth,
                    height = surfaceHeight,
                )
            ) {
                return
            }

            val holder = surfaceHolder ?: return
            if (!holder.surface.isValid) {
                isSurfaceValid = false
                stopAnimationLoop()
                return
            }

            val canvas = try {
                holder.lockHardwareCanvas()
            } catch (_: Throwable) {
                try {
                    holder.lockCanvas()
                } catch (_: Throwable) {
                    null
                }
            } ?: return

            try {
                renderer.drawScene(
                    canvas = canvas,
                    width = surfaceWidth,
                    height = surfaceHeight,
                    design = activeDesign,
                    snapshot = currentSnapshot(),
                    animationTimeSec = animationTimeSec,
                    selectedElementId = null,
                    showSafeAreaGuides = false,
                    parallaxX = parallaxX,
                    parallaxY = parallaxY,
                )
            } finally {
                runCatching {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private data class ColorSignature(
        val primaryArgb: Long,
        val secondaryArgb: Long,
        val gradientEnabled: Boolean,
    )
}
