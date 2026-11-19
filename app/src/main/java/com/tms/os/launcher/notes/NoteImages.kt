package com.tms.os.launcher.notes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.File

/** Picture checks shared by the importer and the one-time clean-up of imported notes. */
object NoteImages {

    /**
     * A picture with nothing on it: fully transparent, or plain white (paper).
     * Samsung Notes puts such pictures (its empty drawing layer) into every Word
     * export. A drawing with even a few strokes, or a picture in any other colour,
     * is not blank.
     */
    fun isBlank(bmp: Bitmap): Boolean {
        val w = bmp.width; val h = bmp.height
        if (w <= 0 || h <= 0) return true
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        var opaque = 0
        var r0 = -1; var g0 = 0; var b0 = 0
        var differing = 0
        for (c in px) {
            if (Color.alpha(c) < 24) continue
            opaque++
            val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            if (r0 < 0) { r0 = r; g0 = g; b0 = b; continue }
            if (kotlin.math.abs(r - r0) + kotlin.math.abs(g - g0) + kotlin.math.abs(b - b0) > 36) differing++
        }
        if (opaque < px.size / 500) return true
        val paper = r0 >= 235 && g0 >= 235 && b0 >= 235
        return paper && differing < maxOf(3, opaque / 1000)
    }

    /** Decodes a small copy (about 256 px) for [isBlank]. */
    fun isBlank(bytes: ByteArray): Boolean = decodeSmall { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it) }?.let { isBlank(it) } ?: false

    fun isBlankFile(f: File): Boolean = f.exists() && (decodeSmall { BitmapFactory.decodeFile(f.path, it) }?.let { isBlank(it) } ?: false)

    private fun decodeSmall(decode: (BitmapFactory.Options?) -> Bitmap?): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 256) sample *= 2
        decode(BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    /** Transparent parts become white paper (a JPEG has no transparency; they would turn black). */
    fun onWhite(bmp: Bitmap): Bitmap {
        if (!bmp.hasAlpha()) return bmp
        val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply { drawColor(Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
        return out
    }

    /** Samsung Notes names exported files "Title_YYMMDD_HHMMSS". */
    val SAMSUNG_STAMP = Regex("[_\\s-]+\\d{6}[_\\s-]+\\d{6}$")

    fun cleanTitle(fileTitle: String): String = fileTitle.replace(SAMSUNG_STAMP, "").trim()
}
