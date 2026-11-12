package com.tms.os.launcher.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object IconEngine {

    /**
     * Creates an authentic Apple iOS continuous curvature squircle path for [0, 0, size, size].
     */
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
        fullBleedAdaptive: Boolean = true
    ): ImageBitmap {
        if (useIosSystemIcons) {
            val nativeIos = tryRenderNativeIosSystemIcon(packageName, label, sizePx)
            if (nativeIos != null) {
                return nativeIos.asImageBitmap()
            }
        }

        val sizeF = sizePx.toFloat()
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val squirclePath = createAppleSquirclePath(sizeF)

        // Render raw icon content onto an unmasked square bitmap first
        val contentBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val contentCanvas = Canvas(contentBitmap)

        if (fullBleedAdaptive && drawable is AdaptiveIconDrawable) {
            val bg = drawable.background
            val fg = drawable.foreground
            // AdaptiveIcon layers have an 18-25% safe-zone inset around the 72dp viewport (108dp total)
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
            // If Samsung's package manager still left transparent corners inside the square,
            // auto-crop and scale to fill 100% of the squircle
            ensureFullBleedFill(contentBitmap, sizePx)
        } else {
            // Legacy drawable or pre-masked Samsung icon:
            val rawSize = sizePx
            val temp = Bitmap.createBitmap(rawSize, rawSize, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(temp)
            drawable.setBounds(0, 0, rawSize, rawSize)
            drawable.draw(tempCanvas)

            val bounds = findNonTransparentBounds(temp)
            val dominantCornerColor = sampleDominantEdgeColor(temp, bounds)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            contentCanvas.drawColor(dominantCornerColor)

            // Zoom slightly beyond the non-transparent bounding box so pre-rounded corners fill our squircle
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

        // Clip to the Apple Squircle Path with anti-aliasing
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawPath(squirclePath, maskPaint)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(contentBitmap, 0f, 0f, maskPaint)
        maskPaint.xfermode = null

        // Subtle iOS inner glass border highlight
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, sizeF * 0.01f)
            color = Color.argb(38, 255, 255, 255)
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

    private fun ensureFullBleedFill(bitmap: Bitmap, sizePx: Int) {
        // Check if corner region at (12%, 12%) is transparent (happens when OEM wraps AdaptiveIcon inside another shape)
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

    /**
     * Renders ultra-crisp native iOS 18 icons for core system apps so the Home Screen
     * looks indistinguishable from real iOS 18!
     */
    private fun tryRenderNativeIosSystemIcon(
        packageName: String,
        label: String,
        sizePx: Int
    ): Bitmap? {
        val pkg = packageName.lowercase(Locale.ROOT)
        val lbl = label.lowercase(Locale.ROOT)

        // Never override TMS OS own rainbow icon
        if (pkg == "com.tms.os.launcher") return null
        // Never override third-party giants like WhatsApp, Google Photos, Chrome, YouTube, Gmail, Maps
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
        val squircle = createAppleSquirclePath(s)
        canvas.clipPath(squircle)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (kind) {
            "calendar" -> {
                // Real iOS Live Calendar Icon (White background, Red weekday, Bold Black date number)
                canvas.drawColor(Color.WHITE)
                val now = Date()
                val dayName = SimpleDateFormat("EEE", Locale.GERMAN)
                    .format(now)
                    .uppercase(Locale.GERMAN)
                    .trimEnd('.')
                val dayNum = SimpleDateFormat("d", Locale.GERMAN).format(now)

                p.color = Color.rgb(255, 59, 48) // iOS Red
                p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                p.textAlign = Paint.Align.CENTER
                p.textSize = s * 0.19f
                canvas.drawText(dayName, s * 0.5f, s * 0.31f, p)

                p.color = Color.rgb(20, 20, 22)
                p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                p.textSize = s * 0.54f
                canvas.drawText(dayNum, s * 0.5f, s * 0.81f, p)
            }

            "clock" -> {
                // Real iOS Clock Icon (Black background, white circular dial, numbers & orange second hand)
                canvas.drawColor(Color.rgb(28, 28, 30))
                p.color = Color.WHITE
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.41f, p)

                // Hour ticks
                p.color = Color.rgb(20, 20, 22)
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
                // Hands (10:10 iOS classic pose)
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = s * 0.042f
                canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.33f, s * 0.38f, p) // Hour
                p.strokeWidth = s * 0.034f
                canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.68f, s * 0.32f, p) // Minute
                // Orange second hand
                p.color = Color.rgb(255, 149, 0)
                p.strokeWidth = s * 0.018f
                canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.5f, s * 0.78f, p)
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.032f, p)
            }

            "camera" -> {
                // iOS Metallic Camera Icon
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    Color.rgb(218, 220, 224),
                    Color.rgb(154, 158, 166),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                // Camera Body
                p.color = Color.rgb(44, 44, 46)
                val bodyRect = RectF(s * 0.14f, s * 0.28f, s * 0.86f, s * 0.78f)
                canvas.drawRoundRect(bodyRect, s * 0.10f, s * 0.10f, p)

                // Top hump
                val humpRect = RectF(s * 0.34f, s * 0.20f, s * 0.66f, s * 0.34f)
                canvas.drawRoundRect(humpRect, s * 0.05f, s * 0.05f, p)

                // Lens ring
                p.color = Color.rgb(180, 184, 190)
                canvas.drawCircle(s * 0.5f, s * 0.53f, s * 0.19f, p)
                p.color = Color.rgb(20, 24, 34)
                canvas.drawCircle(s * 0.5f, s * 0.53f, s * 0.14f, p)
                p.color = Color.rgb(50, 130, 240)
                canvas.drawCircle(s * 0.46f, s * 0.49f, s * 0.035f, p)

                // Yellow indicator dot
                p.color = Color.rgb(255, 204, 0)
                canvas.drawCircle(s * 0.73f, s * 0.36f, s * 0.025f, p)
            }

            "photos" -> {
                // iOS Photos 8-petal colorful flower on crisp white background
                canvas.drawColor(Color.WHITE)
                val petalColors = intArrayOf(
                    Color.argb(210, 255, 59, 48),
                    Color.argb(210, 255, 149, 0),
                    Color.argb(210, 255, 204, 0),
                    Color.argb(210, 52, 199, 89),
                    Color.argb(210, 90, 200, 250),
                    Color.argb(210, 0, 122, 255),
                    Color.argb(210, 88, 86, 214),
                    Color.argb(210, 255, 45, 85)
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
                // iOS Vibrant Green Phone Icon
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    Color.rgb(101, 229, 118),
                    Color.rgb(42, 193, 78),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = Color.WHITE
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
                // iOS Green Messages Bubble Icon
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    Color.rgb(101, 229, 118),
                    Color.rgb(42, 193, 78),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                p.color = Color.WHITE
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
                // iOS Metallic Gear Settings Icon
                p.shader = LinearGradient(
                    0f, 0f, 0f, s,
                    Color.rgb(168, 174, 184),
                    Color.rgb(108, 112, 120),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, s, s, p)
                p.shader = null

                // Outer gear teeth
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
                canvas.drawColor(Color.WHITE)
                p.color = Color.rgb(255, 204, 0)
                canvas.drawRect(0f, 0f, s, s * 0.28f, p)
                p.color = Color.rgb(220, 220, 225)
                p.strokeWidth = s * 0.015f
                canvas.drawLine(0f, s * 0.46f, s, s * 0.46f, p)
                canvas.drawLine(0f, s * 0.64f, s, s * 0.64f, p)
                canvas.drawLine(0f, s * 0.82f, s, s * 0.82f, p)
            }
        }

        return bmp
    }
}
