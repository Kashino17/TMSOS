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
 * Adaptive icons are cropped to the 72 dp viewport like every launcher does
 * (the layers are 108 dp), and small free-standing logos are enlarged so the
 * artwork fills the icon as on iOS.
 */
object IconEngine {

    private const val ICON_PX = 256
    private const val RENDER_VERSION = 6

    /** 108 dp layers vs. 72 dp visible viewport: 18 dp (25 % of the icon) hidden per side. */
    private const val ADAPTIVE_OVERSCAN = 0.25f
    /** Artwork smaller than this share of the icon gets enlarged … */
    private const val FIT_THRESHOLD = 0.64f
    /** … to this share, */
    private const val FIT_TARGET = 0.70f
    /** but never by more than this factor. */
    private const val MAX_FIT_ZOOM = 1.34f

    fun renderIosIconBitmap(
        packageName: String,
        label: String,
        drawable: Drawable?,
        sizePx: Int = ICON_PX,
        useIosSystemIcons: Boolean = true,
        fullBleedAdaptive: Boolean = true,
        themeMode: IconThemeMode = IconThemeMode.STANDARD,
        tintAccent: TintAccentColor = TintAccentColor.IOS_BLUE,
        locale: java.util.Locale = java.util.Locale.getDefault()
    ): ImageBitmap {
        val isDark = themeMode != IconThemeMode.STANDARD
        val nativeIos = if (useIosSystemIcons) {
            IosSystemIcons.render(sizePx, packageName, label, isDark, locale)
        } else {
            null
        }
        val square = nativeIos ?: renderFullBleedSquare(drawable, sizePx, packageName, label)

        when (themeMode) {
            IconThemeMode.IOS_DARK -> if (nativeIos == null) applyDarkThemeInPlace(square)
            IconThemeMode.IOS_TINTED -> applyTintedThemeInPlace(square, tintAccent)
            IconThemeMode.STANDARD -> Unit
        }

        // The iOS superellipse is baked into the bitmap once, here, so the UI
        // never has to path-clip an icon while it animates or scrolls.
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val shader = android.graphics.BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        if (square.width != sizePx || square.height != sizePx) {
            shader.setLocalMatrix(Matrix().apply { setScale(sizePx / square.width.toFloat(), sizePx / square.height.toFloat()) })
        }
        paint.shader = shader
        canvas.drawPath(squirclePath(sizePx), paint)
        if (square !== output) square.recycle()
        return output.asImageBitmap()
    }

    private val squirclePaths = java.util.concurrent.ConcurrentHashMap<Int, android.graphics.Path>()

    /** Same superellipse (n = 4.2) as [com.tms.os.launcher.ui.theme.IosSquircleShape]. */
    private fun squirclePath(size: Int): android.graphics.Path = squirclePaths.getOrPut(size) {
        val path = android.graphics.Path()
        val exponent = 2.0 / 4.2
        val c = size / 2f
        val steps = 240
        for (i in 0..steps) {
            val t = i.toDouble() / steps * 2.0 * Math.PI
            val cos = Math.cos(t); val sin = Math.sin(t)
            val x = c + (Math.signum(cos) * Math.pow(Math.abs(cos), exponent)).toFloat() * c
            val y = c + (Math.signum(sin) * Math.pow(Math.abs(sin), exponent)).toFloat() * c
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        path
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
            val fit = measureForegroundFit(foreground)
            canvas.save()
            // Bring the artwork's centre to the icon centre and enlarge it so it
            // fills the icon the way iOS app icons do.
            canvas.translate(size / 2f, size / 2f)
            canvas.scale(fit.zoom, fit.zoom)
            canvas.translate(-fit.centerX * size, -fit.centerY * size)
            // A launcher shows only the central 72 dp of the 108 dp layers, i.e.
            // the layers extend 25 % beyond the visible icon on every side.
            val over = (size * ADAPTIVE_OVERSCAN).roundToInt()
            if (background != null) {
                background.setBounds(-over, -over, size + over, size + over)
                background.draw(canvas)
            } else {
                canvas.drawColor(Color.WHITE)
            }
            if (foreground != null) {
                foreground.setBounds(-over, -over, size + over, size + over)
                foreground.draw(canvas)
            }
            canvas.restore()
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

    private class ForegroundFit(val zoom: Float, val centerX: Float, val centerY: Float)

    /**
     * Looks at the foreground layer inside the visible viewport. Android logos
     * usually cover only about half of the icon; iOS icons fill roughly 70 %.
     * Small, free-standing artwork is therefore centred and enlarged up to
     * [MAX_FIT_ZOOM]; foregrounds that already reach the edges are left alone.
     */
    private fun measureForegroundFit(foreground: Drawable?): ForegroundFit {
        val none = ForegroundFit(1f, 0.5f, 0.5f)
        if (foreground == null) return none
        return try {
            val p = 96
            val probe = Bitmap.createBitmap(p, p, Bitmap.Config.ARGB_8888)
            val over = (p * ADAPTIVE_OVERSCAN).roundToInt()
            foreground.setBounds(-over, -over, p + over, p + over)
            foreground.draw(Canvas(probe))
            val pixels = IntArray(p * p)
            probe.getPixels(pixels, 0, p, 0, 0, p, p)
            probe.recycle()
            var minX = p; var minY = p; var maxX = -1; var maxY = -1
            for (y in 0 until p) {
                val row = y * p
                for (x in 0 until p) {
                    if (((pixels[row + x] ushr 24) and 0xFF) > 64) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            if (maxX < 0) return none
            // Touching the viewport edge = full-bleed artwork: keep as designed.
            if (minX <= 1 || minY <= 1 || maxX >= p - 2 || maxY >= p - 2) return none
            val fraction = max(maxX - minX + 1, maxY - minY + 1) / p.toFloat()
            if (fraction >= FIT_THRESHOLD) return none
            val zoom = (FIT_TARGET / fraction).coerceIn(1f, MAX_FIT_ZOOM)
            ForegroundFit(zoom, (minX + maxX + 1) / 2f / p, (minY + maxY + 1) / 2f / p)
        } catch (_: Exception) {
            none
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

    /**
     * The icon's background: every pixel reachable from the corners through
     * gradual colour changes (flat plates and gradients), stopping at the crisp
     * edge of the artwork. Whites enclosed by the artwork are not part of it.
     */
    private fun backgroundMask(pixels: IntArray, w: Int, h: Int): BooleanArray {
        val mask = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0
        fun seed(i: Int) { if (!mask[i]) { mask[i] = true; queue[tail++] = i } }
        // Start from the four corners only: they lie outside the rounded icon shape
        // and are always background, whereas artwork may touch an edge (a figure
        // standing on the bottom, for example).
        seed(0); seed(w - 1); seed((h - 1) * w); seed(h * w - 1)
        while (head < tail) {
            val i = queue[head++]
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF; val g = (c ushr 8) and 0xFF; val b = c and 0xFF
            val x = i % w
            fun visit(n: Int) {
                if (mask[n]) return
                val d = pixels[n]
                val delta = kotlin.math.abs(((d ushr 16) and 0xFF) - r) + kotlin.math.abs(((d ushr 8) and 0xFF) - g) + kotlin.math.abs((d and 0xFF) - b)
                if (delta <= 22) { mask[n] = true; queue[tail++] = n }
            }
            if (x > 0) visit(i - 1)
            if (x < w - 1) visit(i + 1)
            if (i >= w) visit(i - w)
            if (i < w * (h - 1)) visit(i + w)
        }
        return mask
    }

    /** iOS dark icon ground: a soft anthracite gradient. */
    private fun darkGround(y: Int, h: Int): Int {
        val t = y.toFloat() / (h - 1).coerceAtLeast(1)
        val v = (46 - t * 24).roundToInt()
        return Color.rgb(v, v, v + 2)
    }

    /**
     * iOS "Dark" icons: a light plate becomes anthracite, the artwork keeps its
     * colours. Icons that already have a coloured or dark background, and any
     * white that belongs to the artwork itself, are left untouched.
     */
    private fun applyDarkThemeInPlace(bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val mask = backgroundMask(pixels, w, h)
        val hsv = FloatArray(3)

        fun isLightNeutral(c: Int): Boolean {
            Color.RGBToHSV((c ushr 16) and 0xFF, (c ushr 8) and 0xFF, c and 0xFF, hsv)
            return hsv[1] < 0.16f && hsv[2] > 0.72f
        }
        // Only act when the plate really is light (sampled along the border).
        var light = 0; var total = 0
        for (x in 0 until w step 4) { total += 2; if (isLightNeutral(pixels[x])) light++; if (isLightNeutral(pixels[(h - 1) * w + x])) light++ }
        for (y in 0 until h step 4) { total += 2; if (isLightNeutral(pixels[y * w])) light++; if (isLightNeutral(pixels[y * w + w - 1])) light++ }
        if (light < total * 0.6f) return

        val out = pixels.copyOf()
        for (i in pixels.indices) {
            val y = i / w
            if (mask[i]) {
                if (isLightNeutral(pixels[i])) out[i] = darkGround(y, h)
                continue
            }
            // Anti-aliased rim of the artwork: swap the white that is mixed in for the dark ground.
            val x = i % w
            var nearPlate = false
            loop@ for (dy in -3..3) for (dx in -3..3) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h && mask[ny * w + nx]) { nearPlate = true; break@loop }
            }
            if (!nearPlate) continue
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF; val g = (c ushr 8) and 0xFF; val b = c and 0xFF
            val white = minOf(r, g, b) / 255f
            if (white < 0.22f) continue
            val ground = darkGround(y, h)
            val sub = white
            out[i] = Color.argb(
                (c ushr 24) and 0xFF,
                (r - sub * (255 - Color.red(ground))).roundToInt().coerceIn(0, 255),
                (g - sub * (255 - Color.green(ground))).roundToInt().coerceIn(0, 255),
                (b - sub * (255 - Color.blue(ground))).roundToInt().coerceIn(0, 255)
            )
        }
        bmp.setPixels(out, 0, w, 0, 0, w, h)
    }

    /**
     * iOS "Tinted" icons: dark ground, the artwork drawn in the accent colour
     * with its own light/dark structure preserved.
     */
    private fun applyTintedThemeInPlace(bmp: Bitmap, accent: TintAccentColor) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val mask = backgroundMask(pixels, w, h)

        val accentInt = accent.colorHex.toInt()
        val ar = (accentInt ushr 16) and 0xFF
        val ag = (accentInt ushr 8) and 0xFF
        val ab = accentInt and 0xFF

        fun lum(c: Int) = (0.299f * ((c ushr 16) and 0xFF) + 0.587f * ((c ushr 8) and 0xFF) + 0.114f * (c and 0xFF)) / 255f

        // Rings around the artwork: 1 = touches the background, 2 = next to ring 1.
        // Those anti-aliased pixels are a blend with the old plate colour, so they
        // borrow the brightness of the artwork's interior instead of their own.
        val ring = ByteArray(w * h)
        var artwork = 0
        for (i in pixels.indices) if (!mask[i]) artwork++
        // Photo-like icons have no separable artwork: tint the whole picture instead.
        val whole = artwork < pixels.size * 0.03f || artwork > pixels.size * 0.97f
        if (!whole) {
            for (pass in 1..2) for (i in pixels.indices) {
                if (mask[i] || ring[i].toInt() != 0) continue
                val x = i % w
                fun edge(n: Int) = if (pass == 1) mask[n] else ring[n].toInt() == 1
                if ((x > 0 && edge(i - 1)) || (x < w - 1 && edge(i + 1)) || (i >= w && edge(i - w)) || (i < w * (h - 1) && edge(i + w))) ring[i] = pass.toByte()
            }
        }
        // Brightness scale from the artwork's interior only; the washed-out rim
        // pixels would otherwise make every solid glyph look dark.
        var minL = 1f; var maxL = 0f; var interior = 0
        for (i in pixels.indices) {
            if (!whole && (mask[i] || ring[i].toInt() != 0)) continue
            interior++
            val l = lum(pixels[i]); if (l < minL) minL = l; if (l > maxL) maxL = l
        }
        if (interior == 0) { minL = 0f; maxL = 0f }
        val range = maxL - minL
        fun tone(c: Int): Float = if (range < 0.08f) 1f else ((lum(c) - minL) / range).coerceIn(0f, 1f)

        val out = IntArray(w * h)
        for (i in pixels.indices) {
            val y = i / w
            val ground = darkGround(y, h)
            if (!whole && mask[i]) { out[i] = ground; continue }
            var t = tone(pixels[i])
            val r = ring[i].toInt()
            if (r != 0) {
                val x = i % w
                var found = false
                search@ for (dy in -3..3) for (dx in -3..3) {
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until w || ny !in 0 until h) continue
                    val n = ny * w + nx
                    if (!mask[n] && ring[n].toInt() == 0) { t = tone(pixels[n]); found = true; break@search }
                }
                if (!found) t = 1f
            }
            val k = if (whole) 0.12f + 0.88f * t else 0.52f + 0.48f * t
            var cr = ar * k; var cg = ag * k; var cb = ab * k
            if (r == 1) {
                cr = (cr + Color.red(ground)) / 2f; cg = (cg + Color.green(ground)) / 2f; cb = (cb + Color.blue(ground)) / 2f
            }
            out[i] = Color.rgb(cr.roundToInt(), cg.roundToInt(), cb.roundToInt())
        }
        System.arraycopy(out, 0, pixels, 0, out.size)
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
