package com.lockcanvas.app.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WallpaperLauncher {

    fun createLiveWallpaperIntent(context: Context): Intent {
        return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context.applicationContext, LockCanvasWallpaperService::class.java),
            )
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun launchLiveWallpaperPicker(context: Context): Boolean {
        val primaryIntent = createLiveWallpaperIntent(context)
        val launchedPrimary = runCatching {
            context.startActivity(primaryIntent)
            true
        }.getOrDefault(false)
        if (launchedPrimary) return true

        val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return runCatching {
            context.startActivity(fallbackIntent)
            true
        }.getOrDefault(false)
    }

    fun isLockCanvasActive(context: Context): Boolean {
        return runCatching {
            val manager = WallpaperManager.getInstance(context.applicationContext)
            val target = LockCanvasWallpaperService::class.java.name
            // wallpaperInfo alone only reflects the home-screen wallpaper; a
            // live wallpaper applied to the lock screen only is invisible to
            // it, so check the lock slot explicitly on Android 14+.
            if (manager.wallpaperInfo?.serviceName == target) return@runCatching true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.serviceName == target) {
                    return@runCatching true
                }
                if (manager.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM)?.serviceName == target) {
                    return@runCatching true
                }
            }
            false
        }.getOrDefault(false)
    }
}
