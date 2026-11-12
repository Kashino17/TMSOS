package com.tms.os.launcher.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tms.os.launcher.model.IconThemeMode
import com.tms.os.launcher.model.TintAccentColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object IconEngine {

    private const val ICON_PX = 216

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
        return createIosSquircleIcon(
            drawable = drawable,
            packageName = packageName,
            label = label,
            useIosSystemIcons = useIosSystemIcons,
            fullBleedAdaptive = fullBleedAdaptive,
            iconThemeMode = themeMode,
            tintAccentColor = tintAccent
        )
    }

    /**
     * Renders any Android/Samsung app icon as a crisp, edge-to-edge iOS 18 Squircle
     * with 100% preserved 1:1 aspect ratio and zero dark wrapper boxes.
     */
    fun createIosSquircleIcon(
        drawable: Drawable?,
        packageName: String = "",
        label: String = "",
        useIosSystemIcons: Boolean = true,
        fullBleedAdaptive: Boolean = true,
        iconThemeMode: IconThemeMode = IconThemeMode.STANDARD,
        tintAccentColor: TintAccentColor = TintAccentColor.IOS_BLUE
    ): ImageBitmap {
        val size = ICON_PX
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val squirclePath = createSmoothSquirclePath(size.toFloat())
        val isDarkTheme = iconThemeMode == IconThemeMode.IOS_DARK

        // 1. Optional Native iOS System Icons for core phone utilities (Settings, Phone, Messages, Calendar, Camera, Calculator)
        if (useIosSystemIcons) {
            val renderedNative = tryRenderNativeIosSystemIcon(
                canvas = canvas,
                size = size,
                squirclePath = squirclePath,
                packageName = packageName.lowercase(Locale.ROOT),
                label = label.lowercase(Locale.ROOT),
                isDarkMode = isDarkTheme
            )
            if (renderedNative) {
                if (iconThemeMode == IconThemeMode.IOS_TINTED) {
                    applyIos18TintedThemeInPlace(output, squirclePath, tintAccentColor)
                }
                drawSubtleSquircleRim(canvas, squirclePath)
                return output.asImageBitmap()
            }
        }

        // 2. Extract full-bleed square bitmap from AdaptiveIconDrawable or Legacy Drawable
        val rawSquare = renderFullBleedSquare(drawable, size, fullBleedAdaptive)

        if (iconThemeMode == IconThemeMode.IOS_DARK) {
            applyIos18DarkThemeInPlace(rawSquare)
        } else if (iconThemeMode == IconThemeMode.IOS_TINTED) {
            applyIos18TintedThemeInPlace(rawSquare, null, tintAccentColor)
        }

        // 3. Mask cleanly to the iOS Squircle path using hardware-quality anti-aliasing
        val saveCount = canvas.saveLayer(0f, 0f, size.toFloat(), size.toFloat(), null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawPath(squirclePath, maskPaint)

        val srcInPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(rawSquare, 0f, 0f, srcInPaint)
        canvas.restoreToCount(saveCount)

        drawSubtleSquircleRim(canvas, squirclePath)
        return output.asImageBitmap()
    }

    private fun drawSubtleSquircleRim(canvas: Canvas, squirclePath: Path) {
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            color = Color.argb(26, 255, 255, 255)
        }
        canvas.drawPath(squirclePath, rimPaint)
    }

    /**
     * Renders any Drawable so that it fills the entire [size] x [size] square edge-to-edge
     * while strictly maintaining a 1:1 aspect ratio (never stretching horizontally or vertically).
     */
    private fun renderFullBleedSquare(
        drawable: Drawable?,
        size: Int,
        fullBleedAdaptive: Boolean
    ): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (drawable == null) {
            c.drawColor(Color.rgb(44, 44, 46))
            return bmp
        }

        if (fullBleedAdaptive && drawable is AdaptiveIconDrawable) {
            val bg = drawable.background
            val fg = drawable.foreground
            // AdaptiveIconDrawable has an 18% safe-zone inset on each side.
            // Expanding bounds by 20% fills the entire square edge-to-edge!
            val expand = (size * 0.20f).roundToInt()
            if (bg != null) {
                bg.setBounds(-expand, -expand, size + expand, size + expand)
                bg.draw(c)
            } else {
                c.drawColor(Color.WHITE)
            }
            if (fg != null) {
                fg.setBounds(-expand, -expand, size + expand, size + expand)
                fg.draw(c)
            }
            return trimTransparentBorderUniformly(bmp, size)
        }

        // Standard / Samsung / Legacy Drawable
        val rawW = drawable.intrinsicWidth.takeIf { it > 0 } ?: size
        val rawH = drawable.intrinsicHeight.takeIf { it > 0 } ?: size
        val temp = Bitmap.createBitmap(rawW, rawH, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(temp)
        drawable.setBounds(0, 0, rawW, rawH)
        drawable.draw(tempCanvas)

        return trimTransparentBorderUniformly(temp, size)
    }

    /**
     * Finds the non-transparent bounding box of [src] and crops a centered SQUARE
     * slightly inside the rounded edges so that when clipped to our iOS Squircle,
     * there are zero transparent corners and zero aspect-ratio distortion.
     */
    private fun trimTransparentBorderUniformly(src: Bitmap, targetSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
                if (alpha > 180) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX <= minX || maxY <= minY) {
            return Bitmap.createScaledBitmap(src, targetSize, targetSize, true)
        }

        val contentW = maxX - minX + 1
        val contentH = maxY - minY + 1
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f

        // Use uniform square side; zoom in by 4% to eliminate pre-masked rounded corner gaps
        val halfSide = (max(contentW, contentH) / 2f) * 0.95f

        val srcRect = RectF(
            cx - halfSide,
            cy - halfSide,
            cx + halfSide,
            cy + halfSide
        )
        val dstRect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())

        val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Fill background with edge dominant color in case the icon has transparent corners
        val edgeColor = sampleDominantEdgeColor(pixels, w, h, minX, minY, maxX, maxY)
        canvas.drawColor(edgeColor)

        val matrix = Matrix()
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, matrix, paint)
        return out
    }

    private fun sampleDominantEdgeColor(
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
        val insetX = ((maxX - minX) * 0.12f).roundToInt()
        val insetY = ((maxY - minY) * 0.12f).roundToInt()

        val candidates = intArrayOf(
            pixels[minY.coerceAdd(insetY, h) * w + midX],
            pixels[maxY.coerceSub(insetY) * w + midX],
            pixels[midY * w + minX.coerceAdd(insetX, w)],
            pixels[midY * w + maxX.coerceSub(insetX)]
        )
        for (c in candidates) {
            if (((c ushr 24) and 0xFF) > 220) {
                return c or (0xFF shl 24)
            }
        }
        return Color.WHITE
    }

    private fun Int.coerceAdd(delta: Int, limit: Int): Int = (this + delta).coerceIn(0, limit - 1)
    private fun Int.coerceSub(delta: Int): Int = (this - delta).coerceAtLeast(0)

    private fun applyIos18DarkThemeInPlace(bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val hsv = FloatArray(3)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            if (a < 20) continue
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val v = hsv[2]
            if (sat < 0.16f && v > 0.72f) {
                val darkVal = (28 + ((1f - v) * 40f)).roundToInt().coerceIn(22, 52)
                pixels[i] = Color.argb(a, darkVal, darkVal, darkVal + 3)
            } else if (sat >= 0.16f) {
                hsv[1] = (sat * 1.12f).coerceAtMost(1f)
                hsv[2] = (v * 0.95f).coerceIn(0.25f, 1f)
                pixels[i] = Color.HSVToColor(a, hsv)
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyIos18TintedThemeInPlace(
        bmp: Bitmap,
        squirclePath: Path?,
        accent: TintAccentColor
    ) {
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
            if (a < 20) continue
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            val contrast = ((lum - 0.5f) * 1.35f + 0.5f).coerceIn(0f, 1f)
            val nr = (20 + contrast * (ar - 16)).roundToInt().coerceIn(0, 255)
            val ng = (20 + contrast * (ag - 16)).roundToInt().coerceIn(0, 255)
            val nb = (24 + contrast * (ab - 16)).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.argb(a, nr, ng, nb)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Renders high-detail reference-grade iOS icons for core system apps
     * matching the exact look in the user's screenshots (Settings gear, Phone, Calculator, Calendar).
     */
    private fun tryRenderNativeIosSystemIcon(
        canvas: Canvas,
        size: Int,
        squirclePath: Path,
        packageName: String,
        label: String,
        isDarkMode: Boolean
    ): Boolean {
        val s = size.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        return when {
            // 0. TMS OS Rainbow "OS" Icon (exact match to Screenshot 2 & 4!)
            packageName == "com.tms.os.launcher" -> {
                canvas.save()
                canvas.clipPath(squirclePath)
                p.color = Color.rgb(18, 18, 22)
                canvas.drawRect(0f, 0f, s, s, p)

                p.shader = LinearGradient(
                    s * 0.15f, s * 0.3f, s * 0.85f, s * 0.7f,
                    intArrayOf(
                        Color.rgb(166, 255, 0),
                        Color.rgb(255, 149, 0),
                        Color.rgb(255, 45, 85),
                        Color.rgb(175, 82, 222)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                p.textAlign = Paint.Align.CENTER
                p.textSize = s * 0.46f
                val fm = p.fontMetrics
                val baseOffset = -(fm.ascent + fm.descent) / 2f
                canvas.drawText("OS", s * 0.5f, s * 0.5f + baseOffset, p)
                p.shader = null
                canvas.restore()
                true
            }

            // 1. iOS Live Calendar Icon
            packageName.contains("calendar") || label == "kalender" || label == "calendar" -> {
                canvas.save()
                canvas.clipPath(squirclePath)
                p.color = if (isDarkMode) Color.rgb(28, 28, 30) else Color.WHITE
                canvas.drawRect(0f, 0f, s, s, p)

                val now = Date()
                val dayName = SimpleDateFormat("EEE", Locale.GERMAN).format(now).uppercase(Locale.GERMAN).take(2)
                val dayNum = SimpleDateFormat("d", Locale.GERMAN).format(now)

                p.color = Color.rgb(255, 59, 48)
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                p.textAlign = Paint.Align.CENTER
                p.textSize = s * 0.18f
                canvas.drawText(dayName, s * 0.5f, s * 0.31f, p)

                p.color = if (isDarkMode) Color.WHITE else Color.rgb(20, 20, 22)
                p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                p.textSize = s * 0.50f
                canvas.drawText(dayNum, s * 0.5f, s * 0.79f, p)
                canvas.restore()
                true
            }

            // 2. Detailed Metallic iOS Settings Gear (like in Screenshot 3)
            packageName == "com.android.settings" -> {
                canvas.save()
                canvas.clipPath(squirclePath)
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    if (isDarkMode) intArrayOf(Color.rgb(58, 58, 62), Color.rgb(32, 32, 36))
                    else intArrayOf(Color.rgb(154, 158, 166), Color.rgb(108, 112, 120)),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                val cx = s * 0.5f
                val cy = s * 0.5f

                // Outer recessed dark ring
                p.style = Paint.Style.FILL
                p.color = Color.rgb(64, 66, 70)
                canvas.drawCircle(cx, cy, s * 0.39f, p)

                // 36 fine outer gear teeth
                p.style = Paint.Style.STROKE
                p.color = Color.rgb(208, 212, 218)
                p.strokeWidth = s * 0.022f
                for (i in 0 until 36) {
                    val angle = Math.toRadians((i * 10).toDouble())
                    val x1 = cx + cos(angle).toFloat() * (s * 0.33f)
                    val y1 = cy + sin(angle).toFloat() * (s * 0.33f)
                    val x2 = cx + cos(angle).toFloat() * (s * 0.39f)
                    val y2 = cy + sin(angle).toFloat() * (s * 0.39f)
                    canvas.drawLine(x1, y1, x2, y2, p)
                }

                // Metallic gear rim
                p.strokeWidth = s * 0.055f
                p.color = Color.rgb(195, 199, 206)
                canvas.drawCircle(cx, cy, s * 0.31f, p)

                // 6 inner spokes (exact iOS Settings gear geometry)
                p.strokeWidth = s * 0.042f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60).toDouble())
                    val x1 = cx + cos(angle).toFloat() * (s * 0.11f)
                    val y1 = cy + sin(angle).toFloat() * (s * 0.11f)
                    val x2 = cx + cos(angle).toFloat() * (s * 0.30f)
                    val y2 = cy + sin(angle).toFloat() * (s * 0.30f)
                    canvas.drawLine(x1, y1, x2, y2, p)
                }

                // Inner hub ring
                p.strokeWidth = s * 0.045f
                canvas.drawCircle(cx, cy, s * 0.12f, p)

                p.style = Paint.Style.FILL
                p.color = Color.rgb(58, 60, 64)
                canvas.drawCircle(cx, cy, s * 0.07f, p)
                canvas.restore()
                true
            }

            // 3. iOS Phone Icon (exact green gradient + white handset)
            packageName.contains("dialer") || packageName == "com.samsung.android.dialer" -> {
                canvas.save()
                canvas.clipPath(squirclePath)
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    intArrayOf(Color.rgb(90, 230, 115), Color.rgb(40, 195, 72)),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = Color.WHITE
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.12f
                p.strokeCap = Paint.Cap.ROUND
                val arcRect = RectF(s * 0.25f, s * 0.25f, s * 0.82f, s * 0.82f)
                canvas.drawArc(arcRect, 128f, 94f, false, p)
                p.style = Paint.Style.FILL
                canvas.drawCircle(s * 0.33f, s * 0.36f, s * 0.085f, p)
                canvas.drawCircle(s * 0.64f, s * 0.67f, s * 0.085f, p)
                canvas.restore()
                true
            }

            // 4. iOS Calculator Icon (like in the Dock of Screenshot 3 & 4)
            packageName.contains("calculator") || packageName.contains("popupcalculator") -> {
                canvas.save()
                canvas.clipPath(squirclePath)
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    intArrayOf(Color.rgb(212, 214, 218), Color.rgb(168, 170, 176)),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                // Inner dark calculator body
                val bodyRect = RectF(s * 0.20f, s * 0.14f, s * 0.80f, s * 0.86f)
                p.color = Color.rgb(44, 44, 46)
                canvas.drawRoundRect(bodyRect, s * 0.09f, s * 0.09f, p)

                // Display screen
                p.color = Color.rgb(72, 74, 78)
                canvas.drawRoundRect(
                    RectF(s * 0.26f, s * 0.20f, s * 0.74f, s * 0.36f),
                    s * 0.04f,
                    s * 0.04f,
                    p
                )

                // 3x3 button dots (right column orange)
                val cols = floatArrayOf(s * 0.34f, s * 0.50f, s * 0.66f)
                val rows = floatArrayOf(s * 0.47f, s * 0.61f, s * 0.75f)
                for (r in rows.indices) {
                    for (c in cols.indices) {
                        p.color = if (c == 2) Color.rgb(255, 159, 10) else Color.rgb(174, 174, 178)
                        canvas.drawCircle(cols[c], rows[r], s * 0.048f, p)
                    }
                }
                canvas.restore()
                true
            }

            else -> false
        }
    }

    private fun createSmoothSquirclePath(size: Float): Path {
        val path = Path()
        val r = size * 0.225f
        path.addRoundRect(RectF(0f, 0f, size, size), r, r, Path.Direction.CW)
        return path
    }
}
