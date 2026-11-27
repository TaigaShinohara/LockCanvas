package com.lockcanvas.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.widget.RemoteViews
import com.lockcanvas.app.MainActivity
import com.lockcanvas.app.R
import java.util.concurrent.Executors

/**
 * Widget that can live on the Android 16 lock screen (widgetCategory=keyguard)
 * as well as the home screen. Shows a live clock (TextClock updates itself),
 * the battery level, and two real buttons: a flashlight toggle that works
 * without unlocking, and an "open app" button (which naturally asks for
 * unlock because it starts an activity).
 */
class LockCanvasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // Refresh calendar/weather off the main thread, then re-render.
            val pendingResult = goAsync()
            refreshExecutor.execute {
                try {
                    refreshData(context)
                    pushAllWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TORCH) {
            toggleTorch(context)
            pushAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.lockcanvas.app.action.TOGGLE_TORCH"
        private const val PREFS = "widget_state"
        private const val KEY_TORCH = "torch_on"
        private const val KEY_EVENT = "next_event_text"
        private const val KEY_WEATHER = "weather_text"
        private const val KEY_WEATHER_AT = "weather_fetched_at"
        private const val WEATHER_TTL_MS = 30L * 60 * 1000

        private val refreshExecutor = Executors.newSingleThreadExecutor()

        fun pushAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LockCanvasWidgetProvider::class.java),
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        /** Fetches the next event and (if stale) the weather into the cache. */
        fun refreshData(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            LockCanvasWidgetData.fetchNextEventText(context)?.let {
                editor.putString(KEY_EVENT, it)
            }
            val weatherAge = System.currentTimeMillis() -
                prefs.getLong(KEY_WEATHER_AT, 0L)
            if (weatherAge > WEATHER_TTL_MS) {
                LockCanvasWidgetData.fetchWeatherText(context)?.let {
                    editor.putString(KEY_WEATHER, it)
                    editor.putLong(KEY_WEATHER_AT, System.currentTimeMillis())
                }
            }
            editor.apply()
        }

        fun isTorchOn(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_TORCH, false)

        private fun setTorchOn(context: Context, on: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_TORCH, on).apply()
        }

        fun toggleTorch(context: Context) {
            val cameraManager = context.getSystemService(CameraManager::class.java) ?: return
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return
                val next = !isTorchOn(context)
                cameraManager.setTorchMode(cameraId, next)
                setTorchOn(context, next)
            } catch (e: Exception) {
                // Camera busy or torch unavailable: leave the state unchanged.
            }
        }

        fun batteryPercent(context: Context): Int {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            return if (level >= 0 && scale > 0) (level * 100) / scale else -1
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_lockcanvas)
            val pct = batteryPercent(context)
            views.setTextViewText(
                R.id.widget_battery,
                if (pct >= 0) "電池 $pct%" else "電池 --%",
            )
            views.setTextViewText(
                R.id.widget_torch_button,
                if (isTorchOn(context)) "ライト ON" else "ライト OFF",
            )
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            views.setTextViewText(
                R.id.widget_event,
                prefs.getString(KEY_EVENT, null) ?: LockCanvasWidgetData.EVENT_NONE,
            )
            views.setTextViewText(
                R.id.widget_weather,
                prefs.getString(KEY_WEATHER, null) ?: LockCanvasWidgetData.WEATHER_UNKNOWN,
            )
            val torchIntent = Intent(context, LockCanvasWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TORCH
            }
            val torchPending = PendingIntent.getBroadcast(
                context,
                0,
                torchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_torch_button, torchPending)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_open_button, openPending)

            val cameraPending = PendingIntent.getActivity(
                context,
                2,
                Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_camera_button, cameraPending)

            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, 300)
                putExtra(AlarmClock.EXTRA_MESSAGE, "LockCanvas")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
            val timerPending = PendingIntent.getActivity(
                context,
                3,
                timerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_timer_button, timerPending)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
