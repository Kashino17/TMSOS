package com.tms.os.launcher.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Resolution-independent system icon artwork.
 *
 * Everything is painted in a normalised 1 x 1 coordinate space and then scaled
 * to the target bitmap, so the same artwork stays razor sharp at 62 dp on the
 * cover screen and 72 dp unfolded. Compose clips the result with the real iOS
 * superellipse, therefore these bitmaps are deliberately full-bleed squares.
 */
internal object IosSystemIcons {

    private enum class Kind {
        PHONE, MESSAGES, CAMERA, CLOCK, CALENDAR, SETTINGS, FILES,
        CONTACTS, CALCULATOR, PHOTOS, NOTES, MAIL, MUSIC, WEATHER
    }

    private const val WHITE = 0xFFFFFFFF.toInt()

    fun render(sizePx: Int, packageName: String, label: String, dark: Boolean, locale: Locale = Locale.getDefault()): Bitmap? {
        val kind = kindFor(packageName, label) ?: return null
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(sizePx.toFloat(), sizePx.toFloat())

        when (kind) {
            Kind.PHONE -> drawPhone(canvas, dark)
            Kind.MESSAGES -> drawMessages(canvas, dark)
            Kind.CAMERA -> drawCamera(canvas, dark)
            Kind.CLOCK -> drawClock(canvas, dark)
            Kind.CALENDAR -> drawCalendar(canvas, dark, locale)
            Kind.SETTINGS -> drawSettings(canvas, dark)
            Kind.FILES -> drawFiles(canvas, dark)
            Kind.CONTACTS -> drawContacts(canvas, dark)
            Kind.CALCULATOR -> drawCalculator(canvas, dark)
            Kind.PHOTOS -> drawPhotos(canvas, dark)
            Kind.NOTES -> drawNotes(canvas, dark)
            Kind.MAIL -> drawMail(canvas, dark)
            Kind.MUSIC -> drawMusic(canvas, dark)
            Kind.WEATHER -> drawWeather(canvas, dark)
        }
        return bitmap
    }

    fun localizedLabel(packageName: String, label: String, language: com.tms.os.launcher.model.AppLanguage): String {
        val kind = kindFor(packageName, label) ?: return label
        val names = when (language) {
            com.tms.os.launcher.model.AppLanguage.DE -> listOf("Telefon", "Nachrichten", "Kamera", "Uhr", "Kalender", "Einstellungen", "Dateien", "Kontakte", "Rechner", "Fotos", "Notizen", "Mail", "Musik", "Wetter")
            com.tms.os.launcher.model.AppLanguage.EN -> listOf("Phone", "Messages", "Camera", "Clock", "Calendar", "Settings", "Files", "Contacts", "Calculator", "Photos", "Notes", "Mail", "Music", "Weather")
            com.tms.os.launcher.model.AppLanguage.TR -> listOf("Telefon", "Mesajlar", "Kamera", "Saat", "Takvim", "Ayarlar", "Dosyalar", "Kişiler", "Hesap Makinesi", "Fotoğraflar", "Notlar", "Mail", "Müzik", "Hava Durumu")
        }
        return names[kind.ordinal]
    }

    /* ------------------------------------------------------------------ */
    /*                            App matching                            */
    /* ------------------------------------------------------------------ */

    private val excludedPackages = setOf(
        "com.android.chrome",
        "com.google.android.gm",
        "com.google.android.apps.docs",
        "com.google.android.apps.maps",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.google.android.googlequicksearchbox",
        "com.android.vending",
        "com.whatsapp",
        "com.tms.os.launcher",
        // Third-party apps whose localized name collides with an Apple app ("Fotos").
        "com.google.android.apps.photos"
    )

    private fun kindFor(packageName: String, rawLabel: String): Kind? {
        val pkg = packageName.lowercase(Locale.ROOT)
        if (pkg in excludedPackages) return null
        if (pkg.contains("launcher")) return null
        val label = rawLabel.lowercase(Locale.ROOT).trim()

        // Package identity wins. It is stable and survives every locale.
        when {
            pkg.contains("dialer") || pkg.contains("telecom") ||
                pkg.endsWith(".phone") || pkg.contains("call") ->
                return Kind.PHONE

            pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("messages") ->
                return Kind.MESSAGES

            pkg.contains("camera") || pkg.contains("gcamera") ->
                return Kind.CAMERA

            pkg.contains("deskclock") || pkg.contains("clockpackage") ||
                pkg.endsWith(".clock") ->
                return Kind.CLOCK

            pkg.contains("calendar") ->
                return Kind.CALENDAR

            pkg == "com.android.settings" || pkg.contains("settings") ->
                return Kind.SETTINGS

            pkg.contains("documentsui") || pkg.contains("myfiles") ||
                pkg.contains("nbu.files") || pkg.contains("filemanager") ->
                return Kind.FILES

            pkg.contains("contacts") || pkg.contains("phonebook") ->
                return Kind.CONTACTS

            pkg.contains("calculator") ->
                return Kind.CALCULATOR

            pkg.contains("gallery") ->
                return Kind.PHOTOS

            pkg.contains("notes") ->
                return Kind.NOTES

            pkg.contains("sec.android.email") || pkg.contains("samsung.android.email") ->
                return Kind.MAIL

            pkg.contains("sec.android.app.music") || pkg.contains("samsung.android.music") ->
                return Kind.MUSIC

            pkg.contains("sec.android.daemonapp") || pkg.contains("weather") ->
                return Kind.WEATHER
        }

        // Label fallback for ROMs that ship opaque package names.
        val token = label.substringBefore(' ').trim()
        return when {
            token in setOf(
                "telefon", "phone", "telefono", "الهاتف", "هاتف",
                "arama", "appel"
            ) -> Kind.PHONE

            token in setOf(
                "nachrichten", "messages", "messaging", "mesajlar",
                "الرسائل", "رسائل", "sms"
            ) -> Kind.MESSAGES

            token in setOf("kamera", "camera", "الكاميرا", "fotoğraf") -> Kind.CAMERA

            token in setOf("uhr", "clock", "saat", "الساعة", "reloj", "horloge") -> Kind.CLOCK

            token in setOf("kalender", "calendar", "takvim", "التقويم", "agenda") -> Kind.CALENDAR

            token in setOf(
                "einstellungen", "settings", "ayarlar", "الإعدادات", "réglages",
                "impostazioni", "ajustes"
            ) -> Kind.SETTINGS

            token in setOf(
                "dateien", "files", "dosyalar", "الملفات", "fichiers", "archivos"
            ) -> Kind.FILES

            token in setOf(
                "kontakte", "contacts", "kişiler", "جهات", "contactos", "kontakte"
            ) -> Kind.CONTACTS

            token in setOf(
                "rechner", "calculator", "hesap", "الآلة", "calculadora", "calculatrice"
            ) -> Kind.CALCULATOR

            token in setOf(
                "galerie", "gallery", "galeri", "المعرض", "photos", "fotos", "galería"
            ) -> Kind.PHOTOS

            token in setOf("notizen", "notes", "notlar", "الملاحظات", "notas", "notes") ->
                Kind.NOTES

            token in setOf("mail", "e-mail", "email", "posta", "البريد", "courrier") ->
                Kind.MAIL

            token in setOf("musik", "music", "müzik", "الموسيقى", "música", "musique") ->
                Kind.MUSIC

            token in setOf("wetter", "weather", "hava", "الطقس", "météo", "clima") ->
                Kind.WEATHER

            else -> null
        }
    }

    /* ------------------------------------------------------------------ */
    /*                              Helpers                               */
    /* ------------------------------------------------------------------ */

    private fun paint(color: Int, style: Paint.Style = Paint.Style.FILL): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = style
        }

    // Font shaping uses real pixel sizes. Subpixel text sizes in a normalized
    // canvas otherwise round glyph advances to zero (e.g. "13" becomes a stack).
    private fun Canvas.normalizedText(text: String, x: Float, y: Float, source: Paint) {
        val textPaint = Paint(source).apply { textSize = source.textSize * 256f; isSubpixelText = true }
        save()
        scale(1f / 256f, 1f / 256f)
        drawText(text, x * 256f, y * 256f, textPaint)
        restore()
    }

    private fun Paint.normalizedTextWidth(text: String): Float =
        Paint(this).apply { textSize *= 256f }.measureText(text) / 256f

    private fun Canvas.background(top: Int, bottom: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(
            0f, 0f, 0f, 1f, top, bottom, Shader.TileMode.CLAMP
        )
        drawRect(0f, 0f, 1f, 1f, p)
    }

    private fun Canvas.radial(
        cx: Float,
        cy: Float,
        radius: Float,
        colors: IntArray,
        stops: FloatArray? = null
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(cx, cy, radius, colors, stops, Shader.TileMode.CLAMP)
        drawRect(0f, 0f, 1f, 1f, p)
    }

    private fun Canvas.rr(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        color: Int
    ) {
        drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint(color))
    }

    private fun Canvas.rrGradient(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        topColor: Int,
        bottomColor: Int
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(
            left, top, left, bottom, topColor, bottomColor, Shader.TileMode.CLAMP
        )
        drawRoundRect(RectF(left, top, right, bottom), radius, radius, p)
    }

    private inline fun Canvas.rotated(degrees: Float, cx: Float, cy: Float, block: () -> Unit) {
        save()
        rotate(degrees, cx, cy)
        block()
        restore()
    }

    private fun Canvas.hand(
        cx: Float,
        cy: Float,
        degrees: Float,
        length: Float,
        width: Float,
        color: Int
    ) {
        val rad = Math.toRadians(degrees.toDouble())
        val ex = cx + sin(rad).toFloat() * length
        val ey = cy - cos(rad).toFloat() * length
        val p = paint(color, Paint.Style.STROKE).apply {
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
        }
        drawLine(cx, cy, ex, ey, p)
    }

    /* ------------------------------------------------------------------ */
    /*                               Phone                                */
    /* ------------------------------------------------------------------ */

    private fun drawPhone(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF2F7C3E.toInt() else 0xFF5BE96D.toInt(),
            if (dark) 0xFF0B4218.toInt() else 0xFF0BB52B.toInt()
        )

        val receiver = Path().apply {
            moveTo(0.283f, 0.175f)
            cubicTo(0.258f, 0.157f, 0.228f, 0.171f, 0.196f, 0.206f)
            cubicTo(0.108f, 0.293f, 0.194f, 0.481f, 0.356f, 0.644f)
            cubicTo(0.519f, 0.806f, 0.707f, 0.892f, 0.794f, 0.804f)
            cubicTo(0.829f, 0.772f, 0.843f, 0.742f, 0.825f, 0.717f)
            lineTo(0.689f, 0.594f)
            cubicTo(0.670f, 0.577f, 0.646f, 0.579f, 0.623f, 0.598f)
            lineTo(0.573f, 0.642f)
            cubicTo(0.510f, 0.618f, 0.382f, 0.490f, 0.358f, 0.427f)
            lineTo(0.402f, 0.377f)
            cubicTo(0.421f, 0.354f, 0.423f, 0.330f, 0.406f, 0.311f)
            close()
        }
        c.drawPath(receiver, paint(WHITE))
    }

    /* ------------------------------------------------------------------ */
    /*                              Messages                              */
    /* ------------------------------------------------------------------ */

    private fun drawMessages(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF2F7C3E.toInt() else 0xFF5BF675.toInt(),
            if (dark) 0xFF0B4218.toInt() else 0xFF00C42B.toInt()
        )

        val bubble = paint(WHITE)
        c.drawRoundRect(
            RectF(0.180f, 0.232f, 0.820f, 0.700f), 0.232f, 0.232f, bubble
        )

        val tail = Path().apply {
            moveTo(0.300f, 0.618f)
            cubicTo(0.286f, 0.688f, 0.262f, 0.748f, 0.230f, 0.795f)
            cubicTo(0.306f, 0.782f, 0.386f, 0.752f, 0.446f, 0.712f)
            close()
        }
        c.drawPath(tail, bubble)
    }

    /* ------------------------------------------------------------------ */
    /*                               Camera                               */
    /* ------------------------------------------------------------------ */

    private fun drawCamera(c: Canvas, dark: Boolean) {
        c.background(if (dark) 0xFF3D3D42.toInt() else 0xFFE9E9EE.toInt(), if (dark) 0xFF121215.toInt() else 0xFFB9BAC2.toInt())
        val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0.2f, 0.15f, 0.8f, 0.9f,
                intArrayOf(0xFF9C9FA6.toInt(), 0xFF22242A.toInt(), 0xFF575B67.toInt()), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(0.5f, 0.5f, 0.385f, metal)
        c.drawCircle(0.5f, 0.5f, 0.355f, paint(0xFF080A0F.toInt()))
        c.drawCircle(0.5f, 0.5f, 0.306f, paint(0xFF3E424F.toInt()))
        c.drawCircle(0.5f, 0.5f, 0.290f, paint(0xFF080B14.toInt()))
        val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(0.43f, 0.37f, 0.36f,
                intArrayOf(0xFF4D6095.toInt(), 0xFF142754.toInt(), 0xFF0B101E.toInt(), 0xFF382C69.toInt()),
                floatArrayOf(0f, 0.4f, 0.76f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawCircle(0.5f, 0.5f, 0.266f, glass)
        c.drawCircle(0.5f, 0.5f, 0.137f, paint(0xFF090C14.toInt()))
        c.drawOval(RectF(0.365f, 0.294f, 0.49f, 0.36f), paint(0x668EB6FF))
        c.drawOval(RectF(0.52f, 0.64f, 0.66f, 0.71f), paint(0x445F78F6))
        c.drawCircle(0.5f, 0.5f, 0.354f, paint(0x448C919F, Paint.Style.STROKE).apply { strokeWidth = 0.008f })
    }

    /* ------------------------------------------------------------------ */
    /*                               Clock                                */
    /* ------------------------------------------------------------------ */

    private fun drawClock(c: Canvas, dark: Boolean) {
        c.background(0xFF161619.toInt(), 0xFF030304.toInt())
        c.drawCircle(0.5f, 0.5f, 0.441f, paint(WHITE))
        val ink = 0xFF151518.toInt()
        val numeral = paint(ink).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 0.142f
        }
        for (i in 1..12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            c.normalizedText(i.toString(), 0.5f + cos(a).toFloat() * 0.332f,
                0.5f + sin(a).toFloat() * 0.332f + 0.050f, numeral)
        }
        val now = java.util.Calendar.getInstance()
        c.hand(0.5f, 0.5f, now.get(java.util.Calendar.HOUR) * 30f + now.get(java.util.Calendar.MINUTE) * 0.5f, 0.205f, 0.033f, ink)
        c.hand(0.5f, 0.5f, now.get(java.util.Calendar.MINUTE) * 6f, 0.286f, 0.024f, ink)
        c.hand(0.5f, 0.5f, now.get(java.util.Calendar.SECOND) * 6f, 0.325f, 0.012f, 0xFFFF9500.toInt())
        c.drawCircle(0.5f, 0.5f, 0.025f, paint(0xFFFF9500.toInt()))
    }

    /* ------------------------------------------------------------------ */
    /*                              Calendar                              */
    /* ------------------------------------------------------------------ */

    private fun drawCalendar(c: Canvas, dark: Boolean, locale: Locale) {
        c.background(
            if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt(),
            if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
        )

        val now = Date()
        val rawWeekday = SimpleDateFormat("EEE", locale)
            .format(now)
            .trimEnd('.')

        // Arabic (and Hebrew) script must stay a complete word: truncating it
        // breaks the joining and renders as scattered letter fragments.
        val isRtlScript = rawWeekday.any { it.code in 0x0590..0x06FF }
        val weekday = if (isRtlScript) {
            rawWeekday
        } else {
            rawWeekday.uppercase(locale).take(3)
        }

        val red = paint(if (dark) 0xFFFF453A.toInt() else 0xFFFF3B30.toInt()).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = if (isRtlScript) 0f else 0.03f
        }
        var weekdaySize = 0.160f
        red.textSize = weekdaySize
        while (weekdaySize > 0.055f && red.normalizedTextWidth(weekday) > 0.80f) {
            weekdaySize *= 0.94f
            red.textSize = weekdaySize
        }
        c.normalizedText(weekday, 0.5f, 0.330f, red)

        val number = SimpleDateFormat("d", locale).format(now)
        val numeral = paint(if (dark) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt()).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textSize = if (number.length >= 2) 0.470f else 0.545f
        }
        c.normalizedText(number, 0.5f, 0.808f, numeral)
    }

    /* ------------------------------------------------------------------ */
    /*                              Settings                              */
    /* ------------------------------------------------------------------ */

    private fun drawSettings(c: Canvas, dark: Boolean) {
        c.background(if (dark) 0xFF565961.toInt() else 0xFFD9DCE2.toInt(), if (dark) 0xFF191B22.toInt() else 0xFF949AA5.toInt())
        c.drawCircle(0.5f, 0.5f, 0.427f, paint(0xFF343842.toInt()))
        val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0.2f, 0.1f, 0.7f, 0.9f,
                intArrayOf(0xFFF0F1F4.toInt(), 0xFF89909C.toInt(), 0xFFC6CAD1.toInt()), null, Shader.TileMode.CLAMP)
            style = Paint.Style.STROKE
            strokeWidth = 0.028f
        }
        c.drawCircle(0.5f, 0.5f, 0.406f, metal)
        for (i in 0 until 28) {
            c.rotated(i * 360f / 28, 0.5f, 0.5f) {
                c.drawLine(0.5f, 0.117f, 0.5f, 0.167f, paint(0xFFC4C8CE.toInt(), Paint.Style.STROKE).apply { strokeWidth = 0.019f })
            }
        }
        metal.strokeWidth = 0.035f
        c.drawCircle(0.5f, 0.5f, 0.312f, metal)
        metal.strokeWidth = 0.016f
        c.drawCircle(0.5f, 0.5f, 0.264f, metal)
        for (i in 0..2) c.rotated(i * 120f, 0.5f, 0.5f) {
            c.drawLine(0.5f, 0.25f, 0.5f, 0.5f, paint(0xFFB5BBC5.toInt(), Paint.Style.STROKE).apply { strokeWidth = 0.065f })
        }
        c.drawCircle(0.5f, 0.5f, 0.115f, paint(0xFFCFD3DA.toInt()))
        c.drawCircle(0.5f, 0.5f, 0.075f, paint(0xFF525865.toInt()))
    }

    /* ------------------------------------------------------------------ */
    /*                               Files                                */
    /* ------------------------------------------------------------------ */

    private fun drawFiles(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF2C2C2E.toInt() else 0xFFF7F8FA.toInt(),
            if (dark) 0xFF121214.toInt() else 0xFFE1E3E8.toInt()
        )

        // Sheet of paper peeking out of the folder.
        c.rr(0.270f, 0.212f, 0.730f, 0.610f, 0.034f, if (dark) 0xFFD9D9DE.toInt() else 0xFFFFFFFF.toInt())
        c.rr(0.270f, 0.212f, 0.730f, 0.610f, 0.034f, 0x14000000)

        // Folder back, tab and front.
        c.rr(0.158f, 0.250f, 0.470f, 0.362f, 0.042f, 0xFF0A5CCB.toInt())
        c.rr(0.158f, 0.302f, 0.842f, 0.762f, 0.090f, 0xFF0A63D8.toInt())
        c.rrGradient(
            0.148f, 0.372f, 0.852f, 0.788f, 0.092f,
            0xFF75C4FF.toInt(), 0xFF0A6FE8.toInt()
        )
        c.rr(0.286f, 0.404f, 0.714f, 0.416f, 0.006f, 0x4DFFFFFF)
    }

    /* ------------------------------------------------------------------ */
    /*                              Contacts                              */
    /* ------------------------------------------------------------------ */

    private fun drawContacts(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF2C2C2E.toInt() else 0xFFF2F2F7.toInt(),
            if (dark) 0xFF0E0E10.toInt() else 0xFFD6D6DE.toInt()
        )

        val ink = if (dark) 0xFF707075.toInt() else 0xFF8E8E93.toInt()
        c.drawCircle(0.5f, 0.362f, 0.138f, paint(ink))

        val shoulders = Path().apply {
            moveTo(0.150f, 1.060f)
            cubicTo(0.176f, 0.690f, 0.318f, 0.566f, 0.500f, 0.566f)
            cubicTo(0.682f, 0.566f, 0.824f, 0.690f, 0.850f, 1.060f)
            close()
        }
        c.drawPath(shoulders, paint(ink))
    }

    /* ------------------------------------------------------------------ */
    /*                             Calculator                             */
    /* ------------------------------------------------------------------ */

    private fun drawCalculator(c: Canvas, dark: Boolean) {
        c.background(0xFF2C2C2E.toInt(), 0xFF0B0B0D.toInt())

        c.rr(0.132f, 0.122f, 0.868f, 0.318f, 0.036f, 0xFF3E3E42.toInt())

        val key = if (dark) 0xFF626267.toInt() else 0xFF5C5C60.toInt()
        val orange = 0xFFFF9F0A.toInt()

        val startX = 0.132f
        val startY = 0.360f
        val endX = 0.868f
        val endY = 0.880f
        val gap = 0.028f
        val keyW = (endX - startX - gap * 3f) / 4f
        val keyH = (endY - startY - gap * 3f) / 4f

        val zero = paint(0xE6FFFFFF.toInt()).apply {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textSize = 0.128f
        }
        c.normalizedText("0", 0.828f, 0.272f, zero)

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val left = startX + col * (keyW + gap)
                val top = startY + row * (keyH + gap)
                c.rr(
                    left, top, left + keyW, top + keyH, 0.030f,
                    if (col == 3) orange else key
                )
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*                               Photos                               */
    /* ------------------------------------------------------------------ */

    private fun drawPhotos(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
            if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        )

        val petals = intArrayOf(
            0xE6FF3B30.toInt(),
            0xE6FF9500.toInt(),
            0xE6FFCC00.toInt(),
            0xE634C759.toInt(),
            0xE65AC8FA.toInt(),
            0xE6007AFF.toInt(),
            0xE65856D6.toInt(),
            0xE6FF2D55.toInt()
        )
        for (i in 0 until 8) {
            c.save()
            c.rotate(i * 45f, 0.5f, 0.5f)
            c.drawOval(
                RectF(0.358f, 0.115f, 0.642f, 0.553f),
                paint(petals[i])
            )
            c.restore()
        }
    }

    /* ------------------------------------------------------------------ */
    /*                               Notes                                */
    /* ------------------------------------------------------------------ */

    private fun drawNotes(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt(),
            if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
        )

        c.rrGradient(
            0f, 0f, 1f, 0.246f, 0f,
            0xFFFFD60A.toInt(), 0xFFFFC300.toInt()
        )
        c.rr(0f, 0.238f, 1f, 0.246f, 0f, 0x1A9A6B00)

        val line = if (dark) 0xFF3A3A3C.toInt() else 0xFFD8D8DC.toInt()
        for (i in 0 until 4) {
            val y = 0.390f + i * 0.162f
            c.rr(0f, y, 1f, y + 0.012f, 0f, line)
        }
    }

    /* ------------------------------------------------------------------ */
    /*                                Mail                                */
    /* ------------------------------------------------------------------ */

    private fun drawMail(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF1D63AC.toInt() else 0xFF2BA7FF.toInt(),
            if (dark) 0xFF063A78.toInt() else 0xFF0066D6.toInt()
        )

        c.rr(0.132f, 0.282f, 0.868f, 0.718f, 0.078f, WHITE)

        val flap = Path().apply {
            moveTo(0.158f, 0.316f)
            lineTo(0.500f, 0.560f)
            lineTo(0.842f, 0.316f)
        }
        c.drawPath(flap, paint(
            if (dark) 0xFF1760A8.toInt() else 0xFF0F8AF0.toInt(),
            Paint.Style.STROKE
        ).apply {
            strokeWidth = 0.030f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    /* ------------------------------------------------------------------ */
    /*                                Music                               */
    /* ------------------------------------------------------------------ */

    private fun drawMusic(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt(),
            if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
        )

        val note = Paint(Paint.ANTI_ALIAS_FLAG)
        note.shader = LinearGradient(
            0.22f, 0.24f, 0.76f, 0.78f,
            0xFFFF6C86.toInt(), 0xFFFA114F.toInt(), Shader.TileMode.CLAMP
        )

        // Two stems.
        c.drawRoundRect(RectF(0.408f, 0.306f, 0.458f, 0.700f), 0.025f, 0.025f, note)
        c.drawRoundRect(RectF(0.708f, 0.246f, 0.758f, 0.640f), 0.025f, 0.025f, note)

        // Beam.
        val beam = Path().apply {
            moveTo(0.408f, 0.286f)
            lineTo(0.758f, 0.226f)
            lineTo(0.758f, 0.322f)
            lineTo(0.408f, 0.382f)
            close()
        }
        c.drawPath(beam, note)

        // Note heads.
        c.rotated(-20f, 0.348f, 0.700f) {
            c.drawOval(RectF(0.246f, 0.622f, 0.450f, 0.778f), note)
        }
        c.rotated(-20f, 0.648f, 0.640f) {
            c.drawOval(RectF(0.546f, 0.562f, 0.750f, 0.718f), note)
        }
    }

    /* ------------------------------------------------------------------ */
    /*                              Weather                               */
    /* ------------------------------------------------------------------ */

    private fun drawWeather(c: Canvas, dark: Boolean) {
        c.background(
            if (dark) 0xFF2A6FBF.toInt() else 0xFF4AA9FF.toInt(),
            if (dark) 0xFF08306B.toInt() else 0xFF0A63E8.toInt()
        )

        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.shader = RadialGradient(
            0.640f, 0.330f, 0.260f,
            intArrayOf(0x59FFFFFF, 0x00FFFFFF),
            null,
            Shader.TileMode.CLAMP
        )
        c.drawCircle(0.640f, 0.330f, 0.260f, glow)
        c.drawCircle(0.640f, 0.330f, 0.150f, paint(WHITE))

        val cloud = paint(WHITE)
        c.drawCircle(0.362f, 0.578f, 0.136f, cloud)
        c.drawCircle(0.522f, 0.502f, 0.180f, cloud)
        c.drawCircle(0.664f, 0.580f, 0.126f, cloud)
        c.drawRoundRect(RectF(0.300f, 0.552f, 0.740f, 0.724f), 0.086f, 0.086f, cloud)
    }
}
