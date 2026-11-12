package com.tms.os.launcher.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object IconEngine {

    fun createAppleSquirclePath(size: Float): Path {
        val path = Path()
        val r = size * 0.225f
        path.addRoundRect(
            RectF(0f, 0f, size, size),
            r,
            r,
            Path.Direction.CW
        )
        return path
    }

    fun renderIosIconBitmap(
        packageName: String,
        label: String,
        drawable: Drawable,
        sizePx: Int = 192,
        useIosSystemIcons: Boolean = true,
        fullBleedAdaptive: Boolean = true,
        themeMode: IconThemeMode = IconThemeMode.STANDARD,
        tintAccent: TintAccentColor = TintAccentColor.IOS_BLUE
    ): ImageBitmap {
        val sizeF = sizePx.toFloat()
        val contentBitmap: Bitmap = if (useIosSystemIcons) {
            tryRenderNativeIosSystemIcon(packageName, label, sizePx, themeMode == IconThemeMode.IOS_DARK)
        } else {
            null
        } ?: renderExtractedAppBitmap(drawable, sizePx, fullBleedAdaptive)

        // Apply iOS 18 Dark Mode or Tinted Mode transformation if selected
        when (themeMode) {
            IconThemeMode.STANDARD -> Unit
            IconThemeMode.IOS_DARK -> applyIos18DarkThemeInPlace(contentBitmap)
            IconThemeMode.IOS_TINTED -> applyIos18TintedThemeInPlace(contentBitmap, tintAccent.colorHex)
        }

        // Clip to the Apple Squircle Path with anti-aliasing
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val squirclePath = createAppleSquirclePath(sizeF)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawPath(squirclePath, maskPaint)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(contentBitmap, 0f, 0f, maskPaint)
        maskPaint.xfermode = null

        // Subtle iOS inner glass border highlight
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, sizeF * 0.011f)
            color = if (themeMode == IconThemeMode.STANDARD) {
                Color.argb(40, 255, 255, 255)
            } else {
                Color.argb(65, 255, 255, 255)
            }
        }
        val r = sizeF * 0.225f
        val halfStroke = borderPaint.strokeWidth / 2f
        canvas.drawRoundRect(
            RectF(halfStroke, halfStroke, sizeF - halfStroke, sizeF - halfStroke),
            r,
            r,
            borderPaint
        )

        return output.asImageBitmap()
    }

    private fun renderExtractedAppBitmap(
        drawable: Drawable,
        sizePx: Int,
        fullBleedAdaptive: Boolean
    ): Bitmap {
        val contentBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val contentCanvas = Canvas(contentBitmap)

        if (fullBleedAdaptive && drawable is AdaptiveIconDrawable) {
            val bg = drawable.background
            val fg = drawable.foreground
            val inset = (sizePx * 0.21f).toInt()
            if (bg != null) {
                bg.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                bg.draw(contentCanvas)
            } else {
                contentCanvas.drawColor(Color.WHITE)
            }
            if (fg != null) {
                fg.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                fg.draw(contentCanvas)
            }
            ensureFullBleedFill(contentBitmap, sizePx)
        } else {
            val temp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(temp)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(tempCanvas)

            val bounds = findNonTransparentBounds(temp)
            val dominantCornerColor = sampleDominantEdgeColor(temp, bounds)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            contentCanvas.drawColor(dominantCornerColor)

            val bw = max(1, bounds.width())
            val bh = max(1, bounds.height())
            val expand = (min(bw, bh) * 0.06f).toInt()
            val srcRect = Rect(
                (bounds.left + expand).coerceAtMost(bounds.right - 1),
                (bounds.top + expand).coerceAtMost(bounds.bottom - 1),
                (bounds.right - expand).coerceAtLeast(bounds.left + 1),
                (bounds.bottom - expand).coerceAtLeast(bounds.top + 1)
            )
            contentCanvas.drawBitmap(temp, srcRect, Rect(0, 0, sizePx, sizePx), paint)
        }
        return contentBitmap
    }

    /**
     * iOS 18 Dark Mode Icon Shader:
     * Replaces bright/white icon backgrounds with sleek iOS 18 dark charcoal (#1B1B1F -> #121215)
     * while keeping colorful foreground symbols vibrant!
     */
    private fun applyIos18DarkThemeInPlace(bitmap: Bitmap) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hsv = FloatArray(3)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            if (a < 20) continue
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val v = hsv[2]

            // Bright & low-saturation pixels (white/light grey icon backgrounds) become dark iOS 18 charcoal
            if (v > 0.78f && sat < 0.18f) {
                val yRatio = (i / w).toFloat() / h.toFloat()
                val darkVal = (30 - (yRatio * 10)).toInt().coerceIn(16, 32)
                pixels[i] = Color.argb(a, darkVal, darkVal, darkVal + 3)
            } else if (sat >= 0.25f) {
                // Boost foreground color vibrancy slightly on dark background
                hsv[1] = (sat * 1.12f).coerceAtMost(1f)
                hsv[2] = (v * 1.05f).coerceAtMost(1f)
                pixels[i] = Color.HSVToColor(a, hsv)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * iOS 18 Tinted Icon Shader:
     * Renders the icon in a monochrome dark-to-accent gradient matching Apple's iOS 18 Tinted Mode.
     */
    private fun applyIos18TintedThemeInPlace(bitmap: Bitmap, accentHex: Long) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val accentR = ((accentHex shr 16) and 0xFF).toInt()
        val accentG = ((accentHex shr 8) and 0xFF).toInt()
        val accentB = (accentHex and 0xFF).toInt()

        // Detect whether the icon background at the edges is bright or dark so foreground always contrasts
        val edgeSample = pixels[(h / 2) * w + (w / 10)]
        val edgeLum = (0.299f * Color.red(edgeSample) + 0.587f * Color.green(edgeSample) + 0.114f * Color.blue(edgeSample)) / 255f
        val invertLum = edgeLum > 0.72f

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            if (a < 20) continue
            val lumRaw = (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f
            val lum = if (invertLum) (1f - lumRaw) else lumRaw

            // Map luminance: dark base (#16161A) -> vibrant tint accent
            val t = (lum * 1.15f).coerceIn(0f, 1f)
            val nr = (20 + (accentR - 20) * t).toInt().coerceIn(0, 255)
            val ng = (20 + (accentG - 20) * t).toInt().coerceIn(0, 255)
            val nb = (24 + (accentB - 24) * t).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(a, nr, ng, nb)
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun ensureFullBleedFill(bitmap: Bitmap, sizePx: Int) {
        val probe = (sizePx * 0.11f).toInt().coerceIn(2, sizePx / 2)
        val alphaTopLeft = Color.alpha(bitmap.getPixel(probe, probe))
        val alphaBottomRight = Color.alpha(bitmap.getPixel(sizePx - probe, sizePx - probe))
        if (alphaTopLeft >= 230 && alphaBottomRight >= 230) return

        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val bounds = findNonTransparentBounds(copy)
        val edgeColor = sampleDominantEdgeColor(copy, bounds)
        val canvas = Canvas(bitmap)
        canvas.drawColor(edgeColor)

        val bw = max(1, bounds.width())
        val bh = max(1, bounds.height())
        val cropInset = (min(bw, bh) * 0.07f).toInt()
        val srcRect = Rect(
            (bounds.left + cropInset).coerceAtMost(bounds.right - 1),
            (bounds.top + cropInset).coerceAtMost(bounds.bottom - 1),
            (bounds.right - cropInset).coerceAtLeast(bounds.left + 1),
            (bounds.bottom - cropInset).coerceAtLeast(bounds.top + 1)
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(copy, srcRect, Rect(0, 0, sizePx, sizePx), paint)
    }

    private fun findNonTransparentBounds(bitmap: Bitmap): Rect {
        val w = bitmap.width
        val h = bitmap.height
        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0

        for (y in 0 until h step 2) {
            for (x in 0 until w step 2) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 40) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (minX >= maxX || minY >= maxY) {
            return Rect(0, 0, w, h)
        }
        return Rect(minX, minY, maxX, maxY)
    }

    private fun sampleDominantEdgeColor(bitmap: Bitmap, bounds: Rect): Int {
        val cx = bounds.centerX().coerceIn(0, bitmap.width - 1)
        val cy = bounds.centerY().coerceIn(0, bitmap.height - 1)
        val insetX = (bounds.width() * 0.14f).toInt()
        val insetY = (bounds.height() * 0.14f).toInt()

        val candidates = listOf(
            bitmap.getPixel((bounds.left + insetX).coerceIn(0, bitmap.width - 1), cy),
            bitmap.getPixel((bounds.right - insetX).coerceIn(0, bitmap.width - 1), cy),
            bitmap.getPixel(cx, (bounds.top + insetY).coerceIn(0, bitmap.height - 1)),
            bitmap.getPixel(cx, (bounds.bottom - insetY).coerceIn(0, bitmap.height - 1))
        ).filter { Color.alpha(it) > 200 }

        if (candidates.isEmpty()) return Color.WHITE
        val first = candidates.first()
        return Color.rgb(Color.red(first), Color.green(first), Color.blue(first))
    }

    private fun tryRenderNativeIosSystemIcon(
        packageName: String,
        label: String,
        sizePx: Int,
        isDarkMode: Boolean
    ): Bitmap? {
        val pkg = packageName.lowercase(Locale.ROOT)
        val lbl = label.lowercase(Locale.ROOT)

        if (pkg == "com.tms.os.launcher") return null
        if (pkg.contains("whatsapp") || pkg.contains("chrome") || pkg.contains("youtube") ||
            pkg.contains("gm") || pkg.contains("maps") || pkg.contains("vending") ||
            pkg.contains("photos") || pkg.contains("edgegallery") || pkg.contains("microg")
        ) {
            return null
        }

        val kind = when {
            pkg.contains("camera") || lbl == "kamera" || lbl == "camera" || lbl == "الكاميرا" -> "camera"
            pkg.contains("calendar") || lbl == "kalender" || lbl == "calendar" || lbl == "التقويم" -> "calendar"
            pkg.contains("deskclock") || pkg.contains("clock") || lbl == "uhr" || lbl == "clock" || lbl == "الساعة" -> "clock"
            (pkg == "com.android.settings" || lbl == "einstellungen" || lbl == "settings" || lbl == "الإعدادات") -> "settings"
            pkg.contains("dialer") || pkg.contains("telecom") || lbl == "telefon" || lbl == "phone" -> "phone"
            pkg.contains("messaging") || pkg.contains("mms") || lbl == "nachrichten" || lbl == "messages" -> "messages"
            pkg.contains("gallery3d") || pkg.contains("sec.android.gallery3d") || lbl == "galerie" || lbl == "fotos" -> "photos"
            pkg.contains("calculator") || lbl == "rechner" || lbl == "calculator" -> "calculator"
            pkg.contains("notes") || lbl == "notizen" || lbl == "notes" -> "notes"
            else -> return null
        }

        val s = sizePx.toFloat()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (kind) {
            "calendar" -> {
                canvas.drawColor(if (isDarkMode) Color.rgb(24, 24, 28) else Color.WHITE)
                val now = Date()
                val dayName = SimpleDateFormat("EEE", Locale.GERMAN)
                    .format(now)
                    .uppercase(Locale.GERMAN)
                    .trimEnd('.')
                val dayNum = SimpleDateFormat("d", Locale.GERMAN).format(now)

                p.color = Color.rgb(255, 69, 58)
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                p.textAlign = Paint.Align.CENTER
                p.textSize = s * 0.19f
                canvas.drawText(dayName, s * 0.5f, s * 0.31f, p)

                p.color = if (isDarkMode) Color.WHITE else Color.rgb(20, 20, 22)
                p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                p.textSize = s * 0.54f
                canvas.drawText(dayNum, s * 0.5f, s * 0.81f, p)
            }

            "clock" -> {
                canvas.drawColor(Color.rgb(24, 24, 26))
                p.color = if (isDarkMode) Color.rgb(42, 42, 46) else Color.WHITE
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.41f, p)

                p.color = if (isDarkMode) Color.WHITE else Color.rgb(20, 20, 22)
                p.strokeWidth = s * 0.022f
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 - 90).toDouble())
                    val r1 = s * 0.33f
                    val r2 = s * 0.38f
                    canvas.drawLine(
                        (s * 0.5f + cos(angle) * r1).toFloat(),
                        (s * 0.5f + sin(angle) * r1).toFloat(),
                        (s * 0.5f + cos(angle) * r2).toFloat(),
                        (s * 0.5f + sin(angle) * r2).toFloat(),
                        p
                    )
                }
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = s * 0.042f
                canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.33f, s * 0.38f, p)
                p.strokeWidth = s * 0.034f
                canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.68f, s * 0.32f, p)
                p.color = Color.rgb(255, 149, 0)
                p.strokeWidth = s * 0.018f
                canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.5f, s * 0.78f, p)
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.032f, p)
            }

            "camera" -> {
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    if (isDarkMode) Color.rgb(46, 46, 52) else Color.rgb(218, 220, 224),
                    if (isDarkMode) Color.rgb(26, 26, 30) else Color.rgb(154, 158, 166),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = if (isDarkMode) Color.rgb(74, 74, 80) else Color.rgb(44, 44, 46)
                val bodyRect = RectF(s * 0.14f, s * 0.28f, s * 0.86f, s * 0.78f)
                canvas.drawRoundRect(bodyRect, s * 0.10f, s * 0.10f, p)

                val humpRect = RectF(s * 0.34f, s * 0.20f, s * 0.66f, s * 0.34f)
                canvas.drawRoundRect(humpRect, s * 0.05f, s * 0.05f, p)

                p.color = Color.rgb(180, 184, 190)
                canvas.drawCircle(s * 0.5f, s * 0.53f, s * 0.19f, p)
                p.color = Color.rgb(20, 24, 34)
                canvas.drawCircle(s * 0.5f, s * 0.53f, s * 0.14f, p)
                p.color = Color.rgb(50, 130, 240)
                canvas.drawCircle(s * 0.46f, s * 0.49f, s * 0.035f, p)

                p.color = Color.rgb(255, 204, 0)
                canvas.drawCircle(s * 0.73f, s * 0.36f, s * 0.025f, p)
            }

            "photos" -> {
                canvas.drawColor(if (isDarkMode) Color.rgb(24, 24, 28) else Color.WHITE)
                val petalColors = intArrayOf(
                    Color.argb(215, 255, 59, 48),
                    Color.argb(215, 255, 149, 0),
                    Color.argb(215, 255, 204, 0),
                    Color.argb(215, 52, 199, 89),
                    Color.argb(215, 90, 200, 250),
                    Color.argb(215, 0, 122, 255),
                    Color.argb(215, 88, 86, 214),
                    Color.argb(215, 255, 45, 85)
                )
                for (i in 0 until 8) {
                    canvas.save()
                    canvas.rotate(i * 45f, s * 0.5f, s * 0.5f)
                    p.color = petalColors[i]
                    val oval = RectF(s * 0.39f, s * 0.14f, s * 0.61f, s * 0.48f)
                    canvas.drawRoundRect(oval, s * 0.11f, s * 0.11f, p)
                    canvas.restore()
                }
            }

            "phone" -> {
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    if (isDarkMode) Color.rgb(24, 30, 26) else Color.rgb(101, 229, 118),
                    if (isDarkMode) Color.rgb(18, 22, 20) else Color.rgb(42, 193, 78),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = if (isDarkMode) Color.rgb(48, 209, 88) else Color.WHITE
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.12f
                p.strokeCap = Paint.Cap.ROUND
                val arcRect = RectF(s * 0.26f, s * 0.26f, s * 0.74f, s * 0.74f)
                canvas.drawArc(arcRect, 115f, 110f, false, p)
                p.style = Paint.Style.FILL
                canvas.drawCircle(s * 0.33f, s * 0.34f, s * 0.085f, p)
                canvas.drawCircle(s * 0.66f, s * 0.67f, s * 0.085f, p)
            }

            "messages" -> {
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    if (isDarkMode) Color.rgb(24, 30, 26) else Color.rgb(101, 229, 118),
                    if (isDarkMode) Color.rgb(18, 22, 20) else Color.rgb(42, 193, 78),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = if (isDarkMode) Color.rgb(48, 209, 88) else Color.WHITE
                val bubble = RectF(s * 0.18f, s * 0.22f, s * 0.82f, s * 0.72f)
                canvas.drawOval(bubble, p)
                val tail = Path().apply {
                    moveTo(s * 0.30f, s * 0.64f)
                    lineTo(s * 0.22f, s * 0.80f)
                    lineTo(s * 0.45f, s * 0.69f)
                    close()
                }
                canvas.drawPath(tail, p)
            }

            "settings" -> {
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    if (isDarkMode) Color.rgb(38, 40, 44) else Color.rgb(168, 174, 184),
                    if (isDarkMode) Color.rgb(22, 24, 28) else Color.rgb(108, 112, 120),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = Color.rgb(54, 56, 60)
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.36f, p)

                p.color = Color.rgb(212, 216, 222)
                p.strokeWidth = s * 0.06f
                p.strokeCap = Paint.Cap.ROUND
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30).toDouble())
                    val r1 = s * 0.22f
                    val r2 = s * 0.34f
                    canvas.drawLine(
                        (s * 0.5f + cos(angle) * r1).toFloat(),
                        (s * 0.5f + sin(angle) * r1).toFloat(),
                        (s * 0.5f + cos(angle) * r2).toFloat(),
                        (s * 0.5f + sin(angle) * r2).toFloat(),
                        p
                    )
                }
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.065f
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.24f, p)
                p.style = Paint.Style.FILL
                p.color = Color.rgb(54, 56, 60)
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.11f, p)
            }

            "calculator" -> {
                canvas.drawColor(Color.rgb(28, 28, 30))
                p.color = Color.rgb(255, 159, 10)
                canvas.drawRoundRect(RectF(s * 0.18f, s * 0.18f, s * 0.82f, s * 0.82f), s * 0.14f, s * 0.14f, p)
                p.color = Color.WHITE
                p.textSize = s * 0.42f
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("±=", s * 0.5f, s * 0.64f, p)
            }

            "notes" -> {
                canvas.drawColor(if (isDarkMode) Color.rgb(28, 28, 30) else Color.WHITE)
                p.color = Color.rgb(255, 204, 0)
                canvas.drawRect(0f, 0f, s, s * 0.28f, p)
                p.color = if (isDarkMode) Color.rgb(68, 68, 72) else Color.rgb(220, 220, 225)
                p.strokeWidth = s * 0.015f
                canvas.drawLine(0f, s * 0.46f, s, s * 0.46f, p)
                canvas.drawLine(0f, s * 0.64f, s, s * 0.64f, p)
                canvas.drawLine(0f, s * 0.82f, s, s * 0.82f, p)
            }
        }

        return bmp
    }
}
