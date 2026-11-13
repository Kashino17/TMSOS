package com.tms.os.launcher.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The "Dark" icon style, done the way a designer would redraw an icon for a
 * dark home screen instead of just dimming it:
 *
 *  - **light plate** (white, light grey): the plate becomes the dark ground;
 *    colours stay, a black glyph turns light (Revolut, ChatGPT), and every
 *    anti-aliased edge is re-mixed against the new ground, so no pale halo;
 *  - **coloured plate with a one-colour glyph** (WhatsApp: white on green):
 *    plate and glyph swap roles — dark ground, glyph in the brand colour;
 *  - **coloured plate with real artwork** (bank logos, gradients): the plate
 *    is toned down to a deep shade of its own colour, the artwork is untouched;
 *  - **dark plate**: already right, left alone.
 *
 * Works on plain ARGB pixels so it can be unit-tested without Android.
 */
object DarkIconStyle {

    enum class Kind { LIGHT_PLATE, TWO_TONE, COLOURED_PLATE, DARK_PLATE }

    /** Top-to-bottom gradient of the dark ground, as (r, g, b) in 0..1. */
    private fun ground(y: Int, h: Int): Float {
        val t = y.toFloat() / (h - 1).coerceAtLeast(1)
        return (46f - t * 24f) / 255f
    }

    private const val PLATE_DIM = 0.58f
    private const val LIGHT_GLYPH = 0.94f

    fun apply(pixels: IntArray, w: Int, h: Int): Kind {
        val mask = plateMask(pixels, w, h)
        val plate = plateColour(pixels, mask, w, h)
        val luma = luma(plate[0], plate[1], plate[2])
        val chroma = max(plate[0], max(plate[1], plate[2])) - min(plate[0], min(plate[1], plate[2]))
        val saturation = chroma / max(plate[0], max(plate[1], plate[2])).coerceAtLeast(1e-3f)

        return when {
            luma < 0.28f -> Kind.DARK_PLATE
            saturation < 0.16f && luma > 0.72f -> { lightPlate(pixels, mask, w, h); Kind.LIGHT_PLATE }
            else -> {
                val glyph = if (saturation >= 0.22f && isFlat(pixels, mask, plate)) singleGlyphColour(pixels, mask, plate) else null
                if (glyph != null) { twoTone(pixels, w, h, plate, glyph); Kind.TWO_TONE }
                else { dimPlate(pixels, mask, w, h); Kind.COLOURED_PLATE }
            }
        }
    }

    /* ------------------------------------------------------------ light plate */

    /**
     * Every pixel is read as white + pure colour + black.
     *  - White that is plate becomes the dark ground: the plate itself, the holes
     *    of a black glyph, and the white mixed into the soft edges next to them
     *    (so no pale halo is left). White inside a colourful logo (a play
     *    triangle, a highlight) is artwork and stays.
     *  - Colour stays; dark colours are lifted a little so they read on the ground.
     *  - Black that stands on the plate turns light, or it would disappear. Black
     *    enclosed in colour (lines on a coloured disc) is artwork and stays.
     */
    private fun lightPlate(px: IntArray, mask: BooleanArray, w: Int, h: Int) {
        val n = px.size
        val src = px.copyOf()
        fun white(c: Int) = min((c ushr 16) and 0xFF, min((c ushr 8) and 0xFF, c and 0xFF)) / 255f
        fun top(c: Int) = max((c ushr 16) and 0xFF, max((c ushr 8) and 0xFF, c and 0xFF)) / 255f
        fun isDarkNeutral(c: Int) = top(c) < 0.5f && top(c) - white(c) < 0.16f
        fun isLightNeutral(c: Int) = white(c) > 0.78f && top(c) - white(c) < 0.12f

        // Black standing on the plate: dark neutral pixels reachable from the plate's edge.
        val exposed = BooleanArray(n)
        val queue = IntArray(n)
        var head = 0; var tail = 0
        fun near(i: Int, radius: Int, test: (Int) -> Boolean): Boolean {
            val x = i % w; val y = i / w
            for (dy in -radius..radius) for (dx in -radius..radius) {
                val nx = x + dx; val ny = y + dy
                if (nx in 0 until w && ny in 0 until h && test(ny * w + nx)) return true
            }
            return false
        }
        for (i in 0 until n) if (!mask[i] && isDarkNeutral(src[i]) && near(i, 2) { mask[it] }) { exposed[i] = true; queue[tail++] = i }
        while (head < tail) {
            val i = queue[head++]
            val x = i % w
            fun visit(j: Int) { if (!exposed[j] && !mask[j] && isDarkNeutral(src[j])) { exposed[j] = true; queue[tail++] = j } }
            if (x > 0) visit(i - 1)
            if (x < w - 1) visit(i + 1)
            if (i >= w) visit(i - w)
            if (i < n - w) visit(i + w)
        }

        // Plate including holes: white regions that touch such a glyph.
        val plate = mask.copyOf()
        head = 0; tail = 0
        for (i in 0 until n) if (!plate[i] && isLightNeutral(src[i]) && near(i, 2) { exposed[it] }) { plate[i] = true; queue[tail++] = i }
        while (head < tail) {
            val i = queue[head++]
            val x = i % w
            fun visit(j: Int) { if (!plate[j] && isLightNeutral(src[j])) { plate[j] = true; queue[tail++] = j } }
            if (x > 0) visit(i - 1)
            if (x < w - 1) visit(i + 1)
            if (i >= w) visit(i - w)
            if (i < n - w) visit(i + w)
        }

        // Tiny enclosed whites are holes too (the bowl of a coloured letter), whatever surrounds them.
        val seen = plate.copyOf()
        for (start in 0 until n) {
            if (seen[start] || !isLightNeutral(src[start])) continue
            head = 0; tail = 0
            seen[start] = true; queue[tail++] = start
            while (head < tail) {
                val i = queue[head++]
                val x = i % w
                fun visit(j: Int) { if (!seen[j] && isLightNeutral(src[j])) { seen[j] = true; queue[tail++] = j } }
                if (x > 0) visit(i - 1)
                if (x < w - 1) visit(i + 1)
                if (i >= w) visit(i - w)
                if (i < n - w) visit(i + w)
            }
            if (tail < n * 0.008f) for (k in 0 until tail) plate[queue[k]] = true
        }

        for (i in 0 until n) {
            val c = src[i]
            val r = ((c ushr 16) and 0xFF) / 255f; val g = ((c ushr 8) and 0xFF) / 255f; val b = (c and 0xFF) / 255f
            val wh = min(r, min(g, b))
            val mx = max(r, max(g, b))
            val chroma = mx - wh
            val black = 1f - mx
            val gr = ground(i / w, h)

            // Plate share of this pixel. On a soft edge next to plate it is the white in
            // excess of the artwork right beside it: that part is plate mixed in.
            var share = if (plate[i]) wh else 0f
            if (!plate[i] && wh > 0.02f) {
                val x = i % w; val y = i / w
                var nextToPlate = false
                var artworkWhite = wh
                for (dy in -2..2) for (dx in -2..2) {
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until w || ny !in 0 until h) continue
                    val j = ny * w + nx
                    if (plate[j]) nextToPlate = true else artworkWhite = min(artworkWhite, white(src[j]))
                }
                if (nextToPlate) share = ((wh - artworkWhite) / (1f - artworkWhite).coerceAtLeast(1e-3f)).coerceIn(0f, 1f)
            }
            val swap = share * (1f - gr)        // white -> ground

            // The black share: light where the black stands on the plate, a brighter shade
            // of the hue where the pixel is a (dark) colour, untouched where it is enclosed.
            val cw = if (wh < 0.999f) smooth(0.06f, 0.22f, chroma / (1f - wh)) else 0f
            val onPlate = if (exposed[i] || (black > 0.02f && near(i, 1) { exposed[it] })) LIGHT_GLYPH else 0f
            var ar = onPlate; var ag = onPlate; var ab = onPlate
            if (chroma > 1e-4f) {
                ar = lerp(onPlate, (r - wh) / chroma * 0.45f, cw)
                ag = lerp(onPlate, (g - wh) / chroma * 0.45f, cw)
                ab = lerp(onPlate, (b - wh) / chroma * 0.45f, cw)
            }
            var or = r - swap + black * ar
            var og = g - swap + black * ag
            var ob = b - swap * (1f - 2f / 255f) + black * ab

            // A dark colour (navy, deep red) must still read on the dark ground.
            val l = luma(or, og, ob)
            val isArtwork = cw * (1f - share)
            if (l < 0.27f && isArtwork > 0f) {
                val t = (0.27f - l) / (1f - l) * isArtwork
                or += (1f - or) * t; og += (1f - og) * t; ob += (1f - ob) * t
            }
            px[i] = argb(c, or, og, ob)
        }
    }

    /* --------------------------------------------------------------- two-tone */

    /** Plate colour and glyph colour swap roles: dark ground, glyph in the brand colour. */
    private fun twoTone(px: IntArray, w: Int, h: Int, plate: FloatArray, glyph: FloatArray) {
        // The brand colour must read on the dark ground.
        val brand = plate.copyOf()
        val l = luma(brand[0], brand[1], brand[2])
        if (l < 0.42f) {
            val t = (0.42f - l) / (1f - l)
            for (k in 0..2) brand[k] += (1f - brand[k]) * t
        }
        val dr = glyph[0] - plate[0]; val dg = glyph[1] - plate[1]; val db = glyph[2] - plate[2]
        val len2 = (dr * dr + dg * dg + db * db).coerceAtLeast(1e-4f)
        for (i in px.indices) {
            val c = px[i]
            val r = ((c ushr 16) and 0xFF) / 255f; val g = ((c ushr 8) and 0xFF) / 255f; val b = (c and 0xFF) / 255f
            // Where the pixel sits between plate (0) and glyph (1) …
            val a = (((r - plate[0]) * dr + (g - plate[1]) * dg + (b - plate[2]) * db) / len2).coerceIn(0f, 1f)
            // … and how far it is from that line (a third colour: keep it as it is).
            val er = r - (plate[0] + a * dr); val eg = g - (plate[1] + a * dg); val eb = b - (plate[2] + a * db)
            val keep = smooth(0.14f, 0.32f, sqrt(er * er + eg * eg + eb * eb))
            val gr = ground(i / w, h)
            px[i] = argb(
                c,
                lerp(lerp(gr, brand[0], a), r, keep),
                lerp(lerp(gr, brand[1], a), g, keep),
                lerp(lerp(gr + 2f / 255f, brand[2], a), b, keep)
            )
        }
    }

    /* ---------------------------------------------------------- coloured plate */

    /** The plate goes to a deep shade of its own colour; the artwork stays as it is. */
    private fun dimPlate(px: IntArray, mask: BooleanArray, w: Int, h: Int) {
        val src = px.copyOf()
        for (i in src.indices) {
            val c = src[i]
            val factor = if (mask[i]) PLATE_DIM else {
                // Soft rim of the artwork: dim as far as the pixel still is plate colour.
                val near = nearestPlate(src, mask, w, h, i) ?: continue
                val sim = 1f - smooth(0.05f, 0.30f, distance(c, near))
                if (sim <= 0f) continue
                1f - sim * (1f - PLATE_DIM)
            }
            px[i] = argb(c, ((c ushr 16) and 0xFF) / 255f * factor, ((c ushr 8) and 0xFF) / 255f * factor, (c and 0xFF) / 255f * factor)
        }
    }

    private fun nearestPlate(px: IntArray, mask: BooleanArray, w: Int, h: Int, i: Int): Int? {
        val x = i % w; val y = i / w
        for (radius in 1..2) for (dy in -radius..radius) for (dx in -radius..radius) {
            if (max(abs(dx), abs(dy)) != radius) continue
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until w && ny in 0 until h && mask[ny * w + nx]) return px[ny * w + nx]
        }
        return null
    }

    /* ------------------------------------------------------------- analysis */

    /** Mean colour of the plate (flood-filled from the corners); the border ring if that finds too little. */
    private fun plateColour(px: IntArray, mask: BooleanArray, w: Int, h: Int): FloatArray {
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (i in px.indices) if (mask[i]) { val c = px[i]; r += (c ushr 16) and 0xFF; g += (c ushr 8) and 0xFF; b += c and 0xFF; n++ }
        if (n < px.size * 0.10) {
            r = 0; g = 0; b = 0; n = 0
            fun add(i: Int) { val c = px[i]; r += (c ushr 16) and 0xFF; g += (c ushr 8) and 0xFF; b += c and 0xFF; n++ }
            for (x in 0 until w step 2) { add(x); add((h - 1) * w + x) }
            for (y in 0 until h step 2) { add(y * w); add(y * w + w - 1) }
        }
        return floatArrayOf(r / (n * 255f), g / (n * 255f), b / (n * 255f))
    }

    /** One flat colour (WhatsApp green), not a gradient. */
    private fun isFlat(px: IntArray, mask: BooleanArray, plate: FloatArray): Boolean {
        var total = 0; var close = 0
        for (i in px.indices step 3) if (mask[i]) {
            total++
            if (distance(px[i], plate) < 0.12f) close++
        }
        return total > 0 && close >= total * 0.85f
    }

    /**
     * The glyph colour if everything that is not plate is one neutral colour
     * (white or black) covering a sensible share of the icon; null otherwise.
     */
    private fun singleGlyphColour(px: IntArray, mask: BooleanArray, plate: FloatArray): FloatArray? {
        var r = 0f; var g = 0f; var b = 0f; var n = 0; var sampled = 0
        for (i in px.indices step 2) {
            sampled++
            if (mask[i] || distance(px[i], plate) < 0.35f) continue
            val c = px[i]
            r += ((c ushr 16) and 0xFF) / 255f; g += ((c ushr 8) and 0xFF) / 255f; b += (c and 0xFF) / 255f; n++
        }
        if (n < sampled * 0.03f || n > sampled * 0.6f) return null
        val glyph = floatArrayOf(r / n, g / n, b / n)
        val chroma = max(glyph[0], max(glyph[1], glyph[2])) - min(glyph[0], min(glyph[1], glyph[2]))
        val l = luma(glyph[0], glyph[1], glyph[2])
        if (chroma > 0.14f || (l in 0.22f..0.8f)) return null
        var close = 0; var total = 0
        for (i in px.indices step 2) {
            if (mask[i] || distance(px[i], plate) < 0.35f) continue
            total++
            if (distance(px[i], glyph) < 0.2f) close++
        }
        return if (total > 0 && close >= total * 0.85f) glyph else null
    }

    /**
     * [backgroundMask], minus corner regions that are much smaller than the largest
     * one: artwork that reaches into a corner (a flag across the top) is not plate.
     */
    private fun plateMask(pixels: IntArray, w: Int, h: Int): BooleanArray {
        val corners = intArrayOf(0, w - 1, (h - 1) * w, h * w - 1)
        val region = IntArray(w * h)        // 0 = none, 1..4 = reached from that corner first
        val sizes = IntArray(5)
        val queue = IntArray(w * h)
        for ((k, start) in corners.withIndex()) {
            if (region[start] != 0) continue
            var head = 0; var tail = 0
            region[start] = k + 1; queue[tail++] = start
            while (head < tail) {
                val i = queue[head++]
                sizes[k + 1]++
                val c = pixels[i]
                val r = (c ushr 16) and 0xFF; val g = (c ushr 8) and 0xFF; val b = c and 0xFF
                val x = i % w
                fun visit(n: Int) {
                    if (region[n] != 0) return
                    val d = pixels[n]
                    val delta = abs(((d ushr 16) and 0xFF) - r) + abs(((d ushr 8) and 0xFF) - g) + abs((d and 0xFF) - b)
                    if (delta <= 22) { region[n] = k + 1; queue[tail++] = n }
                }
                if (x > 0) visit(i - 1)
                if (x < w - 1) visit(i + 1)
                if (i >= w) visit(i - w)
                if (i < w * (h - 1)) visit(i + w)
            }
        }
        val largest = sizes.max()
        return BooleanArray(w * h) { region[it] != 0 && sizes[region[it]] >= largest * 0.5f }
    }

    /**
     * The plate behind the artwork: everything reachable from the four corners
     * through similar neighbours. The corners lie outside the rounded icon
     * shape and are always plate, whereas artwork may touch an edge.
     */
    fun backgroundMask(pixels: IntArray, w: Int, h: Int): BooleanArray {
        val mask = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0
        fun seed(i: Int) { if (!mask[i]) { mask[i] = true; queue[tail++] = i } }
        seed(0); seed(w - 1); seed((h - 1) * w); seed(h * w - 1)
        while (head < tail) {
            val i = queue[head++]
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF; val g = (c ushr 8) and 0xFF; val b = c and 0xFF
            val x = i % w
            fun visit(n: Int) {
                if (mask[n]) return
                val d = pixels[n]
                val delta = abs(((d ushr 16) and 0xFF) - r) + abs(((d ushr 8) and 0xFF) - g) + abs((d and 0xFF) - b)
                if (delta <= 22) { mask[n] = true; queue[tail++] = n }
            }
            if (x > 0) visit(i - 1)
            if (x < w - 1) visit(i + 1)
            if (i >= w) visit(i - w)
            if (i < w * (h - 1)) visit(i + w)
        }
        return mask
    }

    /* ---------------------------------------------------------------- maths */

    private fun luma(r: Float, g: Float, b: Float) = 0.299f * r + 0.587f * g + 0.114f * b
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun smooth(lo: Float, hi: Float, x: Float): Float {
        val t = ((x - lo) / (hi - lo)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun distance(c: Int, to: FloatArray): Float {
        val r = ((c ushr 16) and 0xFF) / 255f - to[0]; val g = ((c ushr 8) and 0xFF) / 255f - to[1]; val b = (c and 0xFF) / 255f - to[2]
        return sqrt(r * r + g * g + b * b)
    }

    private fun distance(c: Int, d: Int): Float {
        val r = (((c ushr 16) and 0xFF) - ((d ushr 16) and 0xFF)) / 255f
        val g = (((c ushr 8) and 0xFF) - ((d ushr 8) and 0xFF)) / 255f
        val b = ((c and 0xFF) - (d and 0xFF)) / 255f
        return sqrt(r * r + g * g + b * b)
    }

    private fun argb(alphaOf: Int, r: Float, g: Float, b: Float): Int =
        (alphaOf and 0xFF000000.toInt()) or
            ((r * 255f).roundToInt().coerceIn(0, 255) shl 16) or
            ((g * 255f).roundToInt().coerceIn(0, 255) shl 8) or
            (b * 255f).roundToInt().coerceIn(0, 255)
}
