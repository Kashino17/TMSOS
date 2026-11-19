package com.tms.os.launcher.notes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.core.content.res.ResourcesCompat
import com.tms.os.launcher.R
import org.json.JSONArray
import org.json.JSONObject

/* -------------------------------------------------------------------------- */
/*                              Paragraph styles                              */
/* -------------------------------------------------------------------------- */

/** The styles of the iOS "Format" panel. */
enum class ParaStyle { TITLE, HEADING, SUBHEADING, BODY, MONO }

enum class ListKind { NONE, BULLET, DASH, NUMBER, CHECK }

data class ParaAttrs(
    val style: ParaStyle = ParaStyle.BODY,
    val list: ListKind = ListKind.NONE,
    val checked: Boolean = false,
    val indent: Int = 0,
    val quote: Boolean = false
) {
    fun pack(): Long = style.ordinal.toLong() or (list.ordinal.toLong() shl 3) or ((if (checked) 1L else 0L) shl 6) or
        (indent.coerceIn(0, 15).toLong() shl 7) or ((if (quote) 1L else 0L) shl 11)

    companion object {
        fun unpack(v: Long) = ParaAttrs(
            style = ParaStyle.entries.getOrElse((v and 7).toInt()) { ParaStyle.BODY },
            list = ListKind.entries.getOrElse(((v shr 3) and 7).toInt()) { ListKind.NONE },
            checked = ((v shr 6) and 1L) == 1L,
            indent = ((v shr 7) and 15).toInt(),
            quote = ((v shr 11) and 1L) == 1L
        )
    }
}

/* -------------------------------------------------------------------------- */
/*                         Serialised rich text (model)                       */
/* -------------------------------------------------------------------------- */

/** One formatting run: "p" paragraph, "b" bold, "i" italic, "u", "s", "fg" text colour, "hl" highlight. */
data class SpanRec(val kind: String, val start: Int, val end: Int, val v: Long = 0)

data class RichText(val text: String, val spans: List<SpanRec>) {
    /** This text, a line break, then [other]; the formatting of both is kept. */
    fun joinedWith(other: RichText): RichText {
        if (text.isEmpty()) return other
        if (other.text.isEmpty()) return this
        val shift = text.length + 1
        val mine = spans.map { if (it.kind == "p" && it.end >= text.length) it.copy(end = text.length + 1) else it }
        return RichText(text + "\n" + other.text, mine + other.spans.map { it.copy(start = it.start + shift, end = it.end + shift) })
    }

    fun toJson(): JSONObject = JSONObject().put("text", text).put("spans", JSONArray().apply {
        spans.forEach { put(JSONArray().put(it.kind).put(it.start).put(it.end).put(it.v)) }
    })

    companion object {
        /** A new note starts in the Title style, as on the iPhone. */
        val EMPTY_TITLE = RichText("", listOf(SpanRec("p", 0, 0, ParaAttrs(style = ParaStyle.TITLE).pack())))
        val EMPTY_BODY = RichText("", listOf(SpanRec("p", 0, 0, ParaAttrs().pack())))

        fun fromJson(o: JSONObject): RichText {
            val a = o.optJSONArray("spans") ?: JSONArray()
            return RichText(o.optString("text"), (0 until a.length()).map { i ->
                a.getJSONArray(i).let { SpanRec(it.getString(0), it.getInt(1), it.getInt(2), it.optLong(3)) }
            })
        }

        fun plain(text: String) = RichText(text, listOf(SpanRec("p", 0, text.length, ParaAttrs().pack())))
    }
}

/* -------------------------------------------------------------------------- */
/*                                Look and feel                               */
/* -------------------------------------------------------------------------- */

/** Everything a span needs to draw: sizes in px, faces and the theme's colours. */
class NoteMetrics(context: Context, dark: Boolean) {
    private val dm = context.resources.displayMetrics
    fun dp(v: Float) = v * dm.density
    fun sp(v: Float) = v * dm.scaledDensity

    val regular: Typeface = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT
    val medium: Typeface = ResourcesCompat.getFont(context, R.font.inter_medium) ?: regular
    val semibold: Typeface = ResourcesCompat.getFont(context, R.font.inter_semibold) ?: regular
    val bold: Typeface = ResourcesCompat.getFont(context, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD
    val displayBold: Typeface = ResourcesCompat.getFont(context, R.font.inter_display_bold) ?: bold
    val mono: Typeface = Typeface.MONOSPACE
    val monoBold: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    val text = if (dark) Color.WHITE else Color.BLACK
    val secondary = if (dark) Color.argb(150, 235, 235, 245) else Color.argb(150, 60, 60, 67)
    val accent = if (dark) Color.rgb(255, 214, 10) else Color.rgb(227, 170, 0)
    val checkMark = if (dark) Color.BLACK else Color.WHITE

    fun size(style: ParaStyle) = sp(when (style) {
        ParaStyle.TITLE -> 28f
        ParaStyle.HEADING -> 22f
        ParaStyle.SUBHEADING -> 17f
        ParaStyle.BODY -> 17f
        ParaStyle.MONO -> 15f
    })

    fun face(style: ParaStyle) = when (style) {
        ParaStyle.TITLE -> displayBold
        ParaStyle.HEADING -> bold
        ParaStyle.SUBHEADING -> semibold
        ParaStyle.BODY -> regular
        ParaStyle.MONO -> mono
    }

    fun boldOf(face: Typeface?): Typeface = when (face) {
        mono, monoBold -> monoBold
        displayBold -> displayBold
        else -> bold
    }
}

/**
 * The style of one paragraph: its size and face, the list marker (•, –, 1.,
 * checklist circle), indentation and the block-quote bar. One span per
 * paragraph; [NoteEditText] keeps them split at line breaks.
 */
class NoteParaSpan(val attrs: ParaAttrs, private val m: NoteMetrics) : MetricAffectingSpan(), LeadingMarginSpan, android.text.style.LineHeightSpan {

    /** Checklist items stand a little apart, as on the iPhone; titles get air below. */
    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: Paint.FontMetricsInt) {
        val extra = when {
            attrs.list == ListKind.CHECK -> m.dp(4f).toInt()
            attrs.style == ParaStyle.TITLE -> m.dp(2f).toInt()
            else -> 0
        }
        if (extra == 0) return
        fm.ascent -= extra; fm.top -= extra
        fm.descent += extra; fm.bottom += extra
    }

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun updateMeasureState(tp: TextPaint) = apply(tp)
    override fun updateDrawState(tp: TextPaint) = apply(tp)

    private fun apply(tp: TextPaint) {
        tp.textSize = m.size(attrs.style)
        tp.typeface = m.face(attrs.style)
        if (attrs.style == ParaStyle.TITLE) tp.letterSpacing = -0.012f
    }

    val indentPx get() = m.dp(28f) * attrs.indent
    private val quotePx get() = if (attrs.quote) m.dp(16f) else 0f
    private val listPx get() = when (attrs.list) {
        ListKind.NONE -> 0f
        ListKind.CHECK -> m.dp(32f)
        ListKind.NUMBER -> m.dp(28f)
        else -> m.dp(22f)
    }

    /** Where the checklist circle sits, relative to the line's left edge (for taps). */
    fun checkRange(): ClosedFloatingPointRange<Float> = (indentPx + quotePx - m.dp(4f))..(indentPx + quotePx + m.dp(30f))

    override fun getLeadingMargin(first: Boolean): Int = (indentPx + quotePx + listPx).toInt()

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout?
    ) {
        val left = x + indentPx * dir
        if (attrs.quote) {
            markerPaint.color = m.secondary
            markerPaint.style = Paint.Style.FILL
            c.drawRoundRect(RectF(left, top.toFloat(), left + m.dp(3f), bottom.toFloat()), m.dp(1.5f), m.dp(1.5f), markerPaint)
        }
        if (!first || attrs.list == ListKind.NONE) return
        val mx = left + quotePx
        val size = m.size(attrs.style)
        when (attrs.list) {
            ListKind.CHECK -> {
                val r = m.dp(10.5f)
                val cx = mx + r + m.dp(1f)
                val cy = baseline - size * 0.36f
                if (attrs.checked) {
                    markerPaint.style = Paint.Style.FILL
                    markerPaint.color = m.accent
                    c.drawCircle(cx, cy, r, markerPaint)
                    markerPaint.style = Paint.Style.STROKE
                    markerPaint.strokeWidth = m.dp(2f)
                    markerPaint.strokeCap = Paint.Cap.ROUND
                    markerPaint.strokeJoin = Paint.Join.ROUND
                    markerPaint.color = m.checkMark
                    val path = Path().apply {
                        moveTo(cx - r * 0.42f, cy + r * 0.02f)
                        lineTo(cx - r * 0.1f, cy + r * 0.34f)
                        lineTo(cx + r * 0.45f, cy - r * 0.3f)
                    }
                    c.drawPath(path, markerPaint)
                } else {
                    markerPaint.style = Paint.Style.STROKE
                    markerPaint.strokeWidth = m.dp(1.4f)
                    markerPaint.color = m.secondary
                    c.drawCircle(cx, cy, r, markerPaint)
                }
            }
            else -> {
                markerPaint.style = Paint.Style.FILL
                markerPaint.color = m.text
                markerPaint.typeface = if (attrs.style == ParaStyle.MONO) m.mono else m.regular
                markerPaint.textSize = size
                val label = when (attrs.list) {
                    ListKind.BULLET -> "•"
                    ListKind.DASH -> "–"
                    ListKind.NUMBER -> "${numberOf(text, start)}."
                    else -> ""
                }
                c.drawText(label, mx + m.dp(2f), baseline.toFloat(), markerPaint)
            }
        }
    }

    /** Position in the run of numbered paragraphs at the same indent directly above. */
    private fun numberOf(text: CharSequence, start: Int): Int {
        if (text !is Spanned) return 1
        val above = text.getSpans(0, start, NoteParaSpan::class.java)
            .filter { text.getSpanStart(it) < start }
            .sortedByDescending { text.getSpanStart(it) }
        var n = 1
        for (s in above) {
            if (s.attrs.list == ListKind.NUMBER && s.attrs.indent == attrs.indent) n++
            else if (s.attrs.indent > attrs.indent) continue
            else break
        }
        return n
    }
}

class NoteBoldSpan(private val m: NoteMetrics) : MetricAffectingSpan() {
    override fun updateMeasureState(tp: TextPaint) { tp.typeface = m.boldOf(tp.typeface) }
    override fun updateDrawState(tp: TextPaint) { tp.typeface = m.boldOf(tp.typeface) }
}

/** Inter has no italic cut: slanted like iOS's synthetic oblique. */
class NoteItalicSpan : MetricAffectingSpan() {
    override fun updateMeasureState(tp: TextPaint) { tp.textSkewX = -0.22f }
    override fun updateDrawState(tp: TextPaint) { tp.textSkewX = -0.22f }
}

/* -------------------------------------------------------------------------- */
/*                         Spannable <-> RichText                             */
/* -------------------------------------------------------------------------- */

/** Paragraph spans are applied first, character styles on top of them. */
const val PARA_FLAGS = Spanned.SPAN_INCLUSIVE_INCLUSIVE or (200 shl Spanned.SPAN_PRIORITY_SHIFT)
const val CHAR_FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

fun RichText.toSpannable(m: NoteMetrics): SpannableStringBuilder {
    val sb = SpannableStringBuilder(text)
    val len = text.length
    spans.forEach { r ->
        val s = r.start.coerceIn(0, len)
        val e = r.end.coerceIn(s, len)
        val span: Any? = when (r.kind) {
            "p" -> NoteParaSpan(ParaAttrs.unpack(r.v), m)
            "b" -> NoteBoldSpan(m)
            "i" -> NoteItalicSpan()
            "u" -> UnderlineSpan()
            "s" -> StrikethroughSpan()
            "fg" -> ForegroundColorSpan(r.v.toInt())
            "hl" -> BackgroundColorSpan(r.v.toInt())
            else -> null
        }
        if (span != null && (r.kind == "p" || e > s)) sb.setSpan(span, s, e, if (r.kind == "p") PARA_FLAGS else CHAR_FLAGS)
    }
    return sb
}

fun Spanned.toRichText(): RichText {
    val out = mutableListOf<SpanRec>()
    getSpans(0, length, Any::class.java).forEach { sp ->
        val s = getSpanStart(sp); val e = getSpanEnd(sp)
        if (s < 0) return@forEach
        // The keyboard's own underline while composing a word is not formatting.
        if ((getSpanFlags(sp) and Spanned.SPAN_COMPOSING) != 0) return@forEach
        when (sp) {
            is NoteParaSpan -> out += SpanRec("p", s, e, sp.attrs.pack())
            is NoteBoldSpan -> out += SpanRec("b", s, e)
            is NoteItalicSpan -> out += SpanRec("i", s, e)
            is UnderlineSpan -> if (sp.javaClass == UnderlineSpan::class.java) out += SpanRec("u", s, e)
            is StrikethroughSpan -> out += SpanRec("s", s, e)
            is ForegroundColorSpan -> out += SpanRec("fg", s, e, sp.foregroundColor.toLong())
            is BackgroundColorSpan -> out += SpanRec("hl", s, e, sp.backgroundColor.toLong())
        }
    }
    return RichText(toString(), out.sortedWith(compareBy({ it.start }, { it.kind })))
}

/**
 * A detected link (web address, e-mail, phone number), drawn in the accent
 * colour and underlined, as in iOS Notes. It is display only: it is found
 * again after every change and never saved (toRichText ignores it).
 */
class NoteLinkSpan(val url: String, private val color: Int) : android.text.style.CharacterStyle(), android.text.style.UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.color = color
        tp.isUnderlineText = true
    }
}

/** Character-style keys used by the editor and the Format panel. */
enum class CharStyle { BOLD, ITALIC, UNDERLINE, STRIKE }

fun Spannable.charSpansOf(style: CharStyle, start: Int, end: Int): List<Any> = when (style) {
    CharStyle.BOLD -> getSpans(start, end, NoteBoldSpan::class.java).toList()
    CharStyle.ITALIC -> getSpans(start, end, NoteItalicSpan::class.java).toList()
    CharStyle.UNDERLINE -> getSpans(start, end, UnderlineSpan::class.java).filter { it.javaClass == UnderlineSpan::class.java }
    CharStyle.STRIKE -> getSpans(start, end, StrikethroughSpan::class.java).toList()
}

fun newCharSpan(style: CharStyle, m: NoteMetrics): Any = when (style) {
    CharStyle.BOLD -> NoteBoldSpan(m)
    CharStyle.ITALIC -> NoteItalicSpan()
    CharStyle.UNDERLINE -> UnderlineSpan()
    CharStyle.STRIKE -> StrikethroughSpan()
}
