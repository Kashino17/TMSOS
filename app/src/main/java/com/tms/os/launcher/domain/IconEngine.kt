package com.tms.os.launcher.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tms.os.launcher.model.IconThemeMode
import com.tms.os.launcher.model.TintAccentColor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders Android app icons as edge-to-edge squares that are later clipped by
 * the real iOS superellipse in Compose.
 *
 * The important part: adaptive icons are drawn at their *native* scale. Blowing
 * them up by 20 % (which the previous implementation did) destroys the optical
 * balance that the app designers built into the 108 dp adaptive canvas.
 */
object IconEngine {

    private const val ICON_PX = 256
    private const val RENDER_VERSION = 5

    fun renderIosIconBitmap(
        packageName: String,
        label: String,
        drawable: Drawable?,
        sizePx: Int = ICON_PX,
        useIosSystemIcons: Boolean = true,
        fullBleedAdaptive: Boolean = true,
        themeMode: IconThemeMode = IconThemeMode.STANDARD,
        tintAccent: TintAccentColor = TintAccentColor.IOS_BLUE
    ): ImageBitmap {
        val isDark = themeMode == IconThemeMode.IOS_DARK
        val nativeIos = if (useIosSystemIcons) {
            IosSystemIcons.render(sizePx, packageName, label, isDark)
        } else {
            null
        }
        val square = nativeIos ?: renderFullBleedSquare(drawable, sizePx, packageName, label)

        when (themeMode) {
            IconThemeMode.IOS_DARK -> if (nativeIos == null) applyDarkThemeInPlace(square)
            IconThemeMode.IOS_TINTED -> applyTintedThemeInPlace(square, tintAccent)
            IconThemeMode.STANDARD -> Unit
        }

        // A hair of inset-free overscan keeps the clip edge perfectly clean.
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(
            square,
            Rect(0, 0, square.width, square.height),
            RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
            paint
        )
        if (square !== output) square.recycle()
        return output.asImageBitmap()
    }

    private fun renderFullBleedSquare(
        drawable: Drawable?,
        size: Int,
        packageName: String,
        label: String
    ): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        if (drawable == null) {
            drawLetterFallback(canvas, size, label.ifBlank { packageName }, packageName)
            return output
        }

        if (drawable is AdaptiveIconDrawable) {
            // Draw the two layers ourselves and skip AdaptiveIconDrawable's
            // system mask, so every pixel can reach our iOS squircle.
            val background = drawable.background
            val foreground = drawable.foreground
            val expand = 1
            if (background != null) {
                background.setBounds(-expand, -expand, size + expand, size + expand)
                background.draw(canvas)
            } else {
                canvas.drawColor(Color.WHITE)
            }
            if (foreground != null) {
                foreground.setBounds(0, 0, size, size)
                foreground.draw(canvas)
            }
            return output
        }

        return try {
            val rawW = drawable.intrinsicWidth.takeIf { it > 0 } ?: size
            val rawH = drawable.intrinsicHeight.takeIf { it > 0 } ?: size
            val temp = Bitmap.createBitmap(rawW, rawH, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(temp)
            drawable.setBounds(0, 0, rawW, rawH)
            drawable.draw(tempCanvas)
            val trimmed = trimTransparentBorderUniformly(temp, size)
            if (temp !== trimmed) temp.recycle()
            output.recycle()
            trimmed
        } catch (_: Exception) {
            drawLetterFallback(canvas, size, label.ifBlank { packageName }, packageName)
            output
        }
    }

    /**
     * Crops a legacy icon to its content, maintains the aspect ratio and fills
     * any transparent corner area with the icon's own edge colour.
     */
    private fun trimTransparentBorderUniformly(src: Bitmap, targetSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if (((pixels[row + x] ushr 24) and 0xFF) > 24) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val contentW = (maxX - minX + 1).coerceAtLeast(1)
        val contentH = (maxY - minY + 1).coerceAtLeast(1)
        val isFullyOpaque = contentW == w && contentH == h

        // If the icon is already a full opaque square (most legacy/pre-masked
        // icons), keep it and let the iOS clip do the shaping.
        if (isFullyOpaque) {
            return Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
        }

        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val halfSide = max(contentW, contentH) / 2f
        val fillColor = dominantEdgeColor(pixels, w, h, minX, minY, maxX, maxY)

        val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(fillColor)
        val matrix = Matrix()
        matrix.setRectToRect(
            RectF(cx - halfSide, cy - halfSide, cx + halfSide, cy + halfSide),
            RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat()),
            Matrix.ScaleToFit.CENTER
        )
        canvas.drawBitmap(src, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun dominantEdgeColor(
        pixels: IntArray,
        w: Int,
        h: Int,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): Int {
        val midX = ((minX + maxX) / 2).coerceIn(0, w - 1)
        val midY = ((minY + maxY) / 2).coerceIn(0, h - 1)
        val insetX = ((maxX - minX) * 0.14f).roundToInt()
        val insetY = ((maxY - minY) * 0.14f).roundToInt()
        val candidates = intArrayOf(
            pixels[(minY + insetY).coerceIn(0, h - 1) * w + midX],
            pixels[(maxY - insetY).coerceIn(0, h - 1) * w + midX],
            pixels[midY * w + (minX + insetX).coerceIn(0, w - 1)],
            pixels[midY * w + (maxX - insetX).coerceIn(0, w - 1)]
        )
        for (c in candidates) {
            if (((c ushr 24) and 0xFF) > 200) return c or (0xFF shl 24)
        }
        return Color.WHITE
    }

    private fun applyDarkThemeInPlace(bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            if (a < 12) continue
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]

            if (sat < 0.14f && value > 0.70f) {
                // Neutral bright surfaces become the iOS dark icon anthracite.
                val v = (28 + ((1f - value) * 18f)).roundToInt().coerceIn(26, 46)
                pixels[i] = Color.argb(a, v, v, (v + 3).coerceAtMost(255))
            } else if (sat >= 0.14f) {
                hsv[1] = (sat * 1.06f).coerceAtMost(1f)
                hsv[2] = (value * 0.93f).coerceIn(0.18f, 1f)
                pixels[i] = Color.HSVToColor(a, hsv)
            } else if (value in 0.18f..0.70f) {
                hsv[2] = (value * 0.82f).coerceAtLeast(0.12f)
                pixels[i] = Color.HSVToColor(a, hsv)
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyTintedThemeInPlace(bmp: Bitmap, accent: TintAccentColor) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val accentInt = accent.colorHex.toInt()
        val ar = (accentInt ushr 16) and 0xFF
        val ag = (accentInt ushr 8) and 0xFF
        val ab = accentInt and 0xFF

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            if (a < 12) continue
            val y = i / w
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            // Apple's tinted mode is two-tone: deep accent shadows and a
            // brighter accent for highlights, with a gentle vertical gradient.
            val gradient = 0.82f + (y.toFloat() / h) * 0.30f
            val shadowAmount = (1f - lum).coerceIn(0f, 1f)
            val base = 0.16f * shadowAmount * gradient
            val highlight = lum * 0.92f
            val intensity = (base + highlight).coerceIn(0.10f, 1.18f)

            val nr = (ar * intensity).roundToInt().coerceIn(0, 255)
            val ng = (ag * intensity).roundToInt().coerceIn(0, 255)
            val nb = (ab * intensity).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.argb(a, nr, ng, nb)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun drawLetterFallback(canvas: Canvas, size: Int, label: String, seed: String) {
        val hue = ((seed.hashCode() % 360) + 360) % 360
        val top = Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.62f, 0.92f))
        val bottom = Color.HSVToColor(floatArrayOf(((hue + 24) % 360).toFloat(), 0.78f, 0.62f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            top, bottom, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        paint.color = Color.WHITE
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.40f
        val metrics = paint.fontMetrics
        val baseline = size * 0.5f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(label.trim().take(1).uppercase(), size * 0.5f, baseline, paint)
    }

    /**
     * Used by the settings preview only.
     */
    fun themePreviewColor(theme: IconThemeMode, accent: TintAccentColor): Int {
        return when (theme) {
            IconThemeMode.STANDARD -> 0xFF0A84FF.toInt()
            IconThemeMode.IOS_DARK -> 0xFF2C2C2E.toInt()
            IconThemeMode.IOS_TINTED -> accent.colorHex.toInt()
        }
    }
}
