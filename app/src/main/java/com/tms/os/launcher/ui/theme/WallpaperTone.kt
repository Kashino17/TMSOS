package com.tms.os.launcher.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.util.Log

/**
 * iOS flips the status bar between white and black glyphs so the clock stays
 * readable on every wallpaper. Reading the wallpaper bitmap requires a storage
 * permission, but the platform's extracted wallpaper colours do not - and that
 * is the same signal Android's own launcher uses for its status bar.
 */
object WallpaperTone {

    private const val TAG = "WallpaperTone"

    fun isTopBandLight(context: Context): Boolean {
        return try {
            val manager = WallpaperManager.getInstance(context)
            val colours = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?: return false
            val primary = colours.primaryColor
            val argb = primary.toArgb()
            val luminance = luminance(argb)
            Log.d(TAG, "primary=#${Integer.toHexString(argb)} luminance=$luminance")
            luminance > 0.56f
        } catch (t: Throwable) {
            Log.w(TAG, "Could not read wallpaper colours", t)
            false
        }
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
