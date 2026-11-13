package com.tms.os.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DarkIconStyleTest {
    private val size = 96
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun lum(c: Int) = (0.299 * ((c ushr 16) and 0xFF) + 0.587 * ((c ushr 8) and 0xFF) + 0.114 * (c and 0xFF)) / 255.0

    /** A plate with a centred square glyph. */
    private fun icon(plate: Int, glyph: Int, extra: Int? = null): IntArray = IntArray(size * size) { i ->
        val x = i % size; val y = i / size
        when {
            extra != null && x in 60..80 && y in 60..80 -> extra
            x in 30..65 && y in 30..65 -> glyph
            else -> plate
        }
    }
    private val centre = 48 * size + 48
    private val corner = 4 * size + 4

    @Test
    fun `black glyph on a white plate turns light on a dark plate`() {
        val px = icon(rgb(255, 255, 255), rgb(0, 0, 0))
        assertEquals(DarkIconStyle.Kind.LIGHT_PLATE, DarkIconStyle.apply(px, size, size))
        assertTrue(lum(px[corner]) < 0.2)
        assertTrue(lum(px[centre]) > 0.85)
    }

    @Test
    fun `colour on a white plate keeps its colour`() {
        val px = icon(rgb(255, 255, 255), rgb(234, 67, 53))
        DarkIconStyle.apply(px, size, size)
        assertTrue(lum(px[corner]) < 0.2)
        val c = px[centre]
        assertTrue(((c ushr 16) and 0xFF) > 210 && ((c ushr 8) and 0xFF) < 110)
    }

    @Test
    fun `white glyph on a brand colour swaps to a brand glyph on dark`() {
        val px = icon(rgb(37, 211, 102), rgb(255, 255, 255))
        assertEquals(DarkIconStyle.Kind.TWO_TONE, DarkIconStyle.apply(px, size, size))
        assertTrue(lum(px[corner]) < 0.2)
        val c = px[centre]
        assertTrue(((c ushr 8) and 0xFF) > 180 && ((c ushr 16) and 0xFF) < 90)
    }

    @Test
    fun `artwork on a coloured plate stays and the plate deepens`() {
        val plate = rgb(20, 80, 180)
        val px = icon(plate, rgb(255, 255, 255), extra = rgb(220, 30, 40))
        assertEquals(DarkIconStyle.Kind.COLOURED_PLATE, DarkIconStyle.apply(px, size, size))
        assertTrue(lum(px[corner]) < lum(plate) * 0.7)
        assertEquals(rgb(255, 255, 255), px[centre])
        assertEquals(rgb(220, 30, 40), px[70 * size + 70])
    }

    @Test
    fun `white inside a colourful logo stays white and black inside colour stays black`() {
        // White plate, red disc-like block with a white mark; green block with a black mark.
        val px = IntArray(size * size) { i ->
            val x = i % size; val y = i / size
            when {
                x in 10..44 && y in 20..75 -> if (x in 22..32 && y in 40..55) rgb(255, 255, 255) else rgb(230, 0, 35)
                x in 52..86 && y in 20..75 -> if (x in 62..74 && y in 40..55) rgb(0, 0, 0) else rgb(30, 215, 96)
                else -> rgb(255, 255, 255)
            }
        }
        DarkIconStyle.apply(px, size, size)
        assertTrue(lum(px[corner]) < 0.2)
        assertTrue(lum(px[47 * size + 27]) > 0.95)
        assertTrue(lum(px[47 * size + 68]) < 0.05)
    }

    @Test
    fun `a dark icon is left alone`() {
        val px = icon(rgb(18, 22, 40), rgb(255, 255, 255))
        val before = px.copyOf()
        assertEquals(DarkIconStyle.Kind.DARK_PLATE, DarkIconStyle.apply(px, size, size))
        assertTrue(before.contentEquals(px))
    }

    /**
     * Not a test of behaviour: writes before/after pixels for sample icons
     * (168×168 raw RGB files in DARK_ICON_SAMPLES) so the result can be looked at.
     */
    @Test
    fun `preview for sample icons`() {
        val dir = File(System.getenv("DARK_ICON_SAMPLES") ?: return)
        val n = 168
        fun ring(plate: Int, ink: Int) = IntArray(n * n) { i ->
            // Anti-aliased ring, 4×4 supersampled.
            var hit = 0
            for (sy in 0..3) for (sx in 0..3) {
                val x = i % n + sx / 4.0 - n / 2.0; val y = i / n + sy / 4.0 - n / 2.0
                val d = Math.hypot(x, y)
                if (d in 30.0..52.0) hit++
            }
            val t = hit / 16.0
            fun mix(a: Int, b: Int) = (a + (b - a) * t).toInt()
            rgb(mix((plate ushr 16) and 0xFF, (ink ushr 16) and 0xFF), mix((plate ushr 8) and 0xFF, (ink ushr 8) and 0xFF), mix(plate and 0xFF, ink and 0xFF))
        }
        val samples = linkedMapOf<String, IntArray>()
        dir.listFiles { f -> f.name.endsWith(".rgb") && !f.name.startsWith("out_") }?.sortedBy { it.name }?.forEach { f ->
            val bytes = f.readBytes()
            val px = IntArray(n * n) { i -> rgb(bytes[i * 3].toInt() and 0xFF, bytes[i * 3 + 1].toInt() and 0xFF, bytes[i * 3 + 2].toInt() and 0xFF) }
            // The crops come from screenshots: outside the rounded shape is wallpaper. Give it the plate colour back.
            val c = (n - 1) / 2.0
            val fixed = px.copyOf()
            for (i in px.indices) {
                val dx = (i % n - c) / c; val dy = (i / n - c) / c
                val d = Math.pow(Math.pow(Math.abs(dx), 4.2) + Math.pow(Math.abs(dy), 4.2), 1 / 4.2)
                if (d > 0.92) {
                    val s = 0.88 / d
                    fixed[i] = px[Math.round(c + dy * c * s).toInt() * n + Math.round(c + dx * c * s).toInt()]
                }
            }
            samples[f.name.removeSuffix(".rgb")] = fixed
        }
        samples["90_black_on_white"] = ring(rgb(255, 255, 255), rgb(0, 0, 0))
        samples["91_navy_on_white"] = ring(rgb(255, 255, 255), rgb(0, 48, 135))
        samples["92_black_on_yellow"] = ring(rgb(254, 229, 0), rgb(30, 20, 20))
        samples["93_white_on_blue"] = ring(rgb(24, 119, 242), rgb(255, 255, 255))
        // Black lines enclosed in a coloured disc on a white plate (must stay black), and a white mark in a coloured disc.
        samples["94_black_in_green"] = IntArray(n * n) { i ->
            val x = i % n - n / 2.0; val y = i / n - n / 2.0; val d = Math.hypot(x, y)
            when { d > 62 -> rgb(255, 255, 255); Math.abs(y) < 7 && Math.abs(x) < 36 -> rgb(0, 0, 0); else -> rgb(30, 215, 96) }
        }
        samples["95_white_in_red"] = IntArray(n * n) { i ->
            val x = i % n - n / 2.0; val y = i / n - n / 2.0; val d = Math.hypot(x, y)
            when { d > 62 -> rgb(255, 255, 255); Math.abs(y) < 22 && Math.abs(x) < 22 -> rgb(255, 255, 255); else -> rgb(230, 0, 35) }
        }
        // Black word and a coloured mark side by side on white.
        samples["96_black_and_colour"] = IntArray(n * n) { i ->
            val x = i % n; val y = i / n
            when { y in 50..80 && x in 20..148 -> rgb(0, 0, 0); y in 100..120 && x in 40..128 -> rgb(255, 153, 0); else -> rgb(255, 255, 255) }
        }
        fun write(name: String, px: IntArray) = File(dir, name).writeBytes(ByteArray(n * n * 3) { j ->
            val c = px[j / 3]; (when (j % 3) { 0 -> (c ushr 16) and 0xFF; 1 -> (c ushr 8) and 0xFF; else -> c and 0xFF }).toByte()
        })
        samples.forEach { (name, px) ->
            write("out_a_$name.rgb", px)
            val out = px.copyOf()
            println("$name: ${DarkIconStyle.apply(out, n, n)}")
            write("out_b_$name.rgb", out)
        }
    }
}
