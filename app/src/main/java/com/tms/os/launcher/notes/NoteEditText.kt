package com.tms.os.launcher.notes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText

/** What the Format panel and the toolbar show for the current cursor or selection. */
data class EditorState(
    val para: ParaAttrs = ParaAttrs(),
    val chars: Set<CharStyle> = emptySet(),
    val color: Int? = null,
    val highlight: Boolean = false,
    val hasSelection: Boolean = false
)

/**
 * The text part of a note, behaving like the iOS Notes editor:
 *  - one [NoteParaSpan] per paragraph (kept split at every line break);
 *  - Return continues a list, Return on an empty list item ends it, and a line
 *    after a Title/Heading/Subheading starts as Body;
 *  - tapping a checklist circle ticks it without moving the cursor;
 *  - B/I/U/S with no selection apply to what is typed next.
 */
@SuppressLint("ViewConstructor")
class NoteEditText(context: Context, val m: NoteMetrics) : EditText(context) {

    var onContentChanged: (() -> Unit)? = null
    var onStateChanged: ((EditorState) -> Unit)? = null
    /** Bottom of the caret's line in this view's coordinates, to keep it above the keyboard. */
    var onCaret: ((Int) -> Unit)? = null

    private val typing = mutableSetOf<CharStyle>()
    private var typingColor: Int? = null
    private var normalizing = false
    private var endListAt = -1
    private var keepParagraphAt = -1
    private var pressedCheck: NoteParaSpan? = null
    /** False while the EditText constructor still runs (it already reports selections then). */
    private var ready = false

    init {
        background = null
        setPadding(0, 0, 0, 0)
        typeface = m.regular
        setTextSize(TypedValue.COMPLEX_UNIT_PX, m.size(ParaStyle.BODY))
        setTextColor(m.text)
        highlightColor = (m.accent and 0x00FFFFFF) or 0x55000000
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        isSingleLine = false
        setHorizontallyScrolling(false)
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        setLineSpacing(m.dp(3f), 1f)
        if (Build.VERSION.SDK_INT >= 29) {
            textCursorDrawable = GradientDrawable().apply { setColor(m.accent); setSize(m.dp(2.2f).toInt(), 1) }
            textSelectHandle?.setTint(m.accent)
            textSelectHandleLeft?.setTint(m.accent)
            textSelectHandleRight?.setTint(m.accent)
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!ready || normalizing) return
                val e = text ?: return
                if (count > 0 && s.subSequence(start, (start + count).coerceAtMost(s.length)).contains('\n')) lastNewlineAt = android.os.SystemClock.uptimeMillis()
                if (count == 1 && before == 0 && s.getOrNull(start) == '\n') {
                    val pStart = paragraphStart(e, start)
                    val pEnd = start
                    val span = paraSpanAt(e, pStart)
                    // Return on an empty list item: the item stops being a list item.
                    if (pStart == pEnd && span != null && span.attrs.list != ListKind.NONE) endListAt = start
                    // Return at the very start of a line: the line itself keeps its style.
                    else if (start == pStart) keepParagraphAt = start + 1
                }
                if (count > 0 && (typing.isNotEmpty() || typingColor != null)) applyTyping(e, start, start + count)
            }
            override fun afterTextChanged(s: Editable) {
                if (normalizing) return
                normalizing = true
                try {
                    if (endListAt >= 0 && endListAt < s.length && s[endListAt] == '\n') {
                        val at = endListAt
                        s.delete(at, at + 1)
                        paraSpanAt(s, paragraphStart(s, at))?.let { sp ->
                            replacePara(s, sp, sp.attrs.copy(list = ListKind.NONE, checked = false, indent = 0))
                        }
                    }
                    endListAt = -1
                    normalize(s)
                    keepParagraphAt = -1
                } finally {
                    normalizing = false
                }
                onContentChanged?.invoke()
                reportCaret()
            }
        })
        ready = true
    }

    /* --------------------------------------------------------- paragraphs */

    private fun paragraphStart(t: CharSequence, at: Int): Int {
        var i = (at - 1).coerceAtMost(t.length - 1)
        while (i >= 0 && t[i] != '\n') i--
        return i + 1
    }

    private fun paragraphEnd(t: CharSequence, at: Int): Int {
        var i = at
        while (i < t.length && t[i] != '\n') i++
        return if (i < t.length) i + 1 else i
    }

    private fun paraSpanAt(s: Spannable, pStart: Int): NoteParaSpan? {
        val spans = s.getSpans(pStart, pStart, NoteParaSpan::class.java)
        return spans.firstOrNull { s.getSpanStart(it) == pStart } ?: spans.firstOrNull()
    }

    private fun replacePara(s: Spannable, old: NoteParaSpan, attrs: ParaAttrs) {
        val st = s.getSpanStart(old); val en = s.getSpanEnd(old)
        s.removeSpan(old)
        s.setSpan(NoteParaSpan(attrs, m), st, en, PARA_FLAGS)
    }

    /** One paragraph span per paragraph, exactly covering it (including its line break). */
    private fun normalize(s: Editable) {
        val ranges = ArrayList<Pair<Int, Int>>()
        var st = 0
        for (i in s.indices) if (s[i] == '\n') { ranges += st to i + 1; st = i + 1 }
        ranges += st to s.length
        val old = s.getSpans(0, s.length, NoteParaSpan::class.java)
        val noneAtAll = old.isEmpty()
        val keep = HashSet<NoteParaSpan>()
        val add = ArrayList<Triple<ParaAttrs, Int, Int>>()
        ranges.forEachIndexed { index, (ps, pe) ->
            val own = old.firstOrNull { s.getSpanStart(it) == ps }
            if (own != null) {
                if (s.getSpanEnd(own) == pe && own !in keep) { keep += own; return@forEachIndexed }
                add += Triple(own.attrs, ps, pe)
                return@forEachIndexed
            }
            // A new line made by Return inside an earlier paragraph: it continues that paragraph's list.
            val covering = old.filter { s.getSpanStart(it) < ps && s.getSpanEnd(it) >= ps }.maxByOrNull { s.getSpanStart(it) }
            val attrs = when {
                covering == null -> if (noneAtAll && index == 0) ParaAttrs(style = ParaStyle.TITLE) else ParaAttrs()
                ps == keepParagraphAt -> covering.attrs
                else -> covering.attrs.copy(
                    // After a Title, Heading or Subheading the next line is Body text.
                    style = if (covering.attrs.style == ParaStyle.MONO) ParaStyle.MONO else ParaStyle.BODY,
                    checked = false
                )
            }
            add += Triple(attrs, ps, pe)
        }
        old.forEach { if (it !in keep) s.removeSpan(it) }
        add.forEach { (a, ps, pe) -> s.setSpan(NoteParaSpan(a, m), ps, pe, PARA_FLAGS) }
    }

    private fun selectedParagraphs(): List<NoteParaSpan> {
        val e = text ?: return emptyList()
        val a = selectionStart.coerceAtLeast(0); val b = selectionEnd.coerceAtLeast(a)
        val first = paragraphStart(e, a)
        val out = ArrayList<NoteParaSpan>()
        var p = first
        while (true) {
            paraSpanAt(e, p)?.let { if (it !in out) out += it }
            val next = paragraphEnd(e, p)
            if (next >= b.coerceAtLeast(first + 1) || next >= e.length || next == p) break
            p = next
        }
        return out
    }

    private fun editParagraphs(transform: (List<NoteParaSpan>) -> (ParaAttrs) -> ParaAttrs) {
        val e = text ?: return
        val targets = selectedParagraphs()
        if (targets.isEmpty()) return
        val f = transform(targets)
        normalizing = true
        try { targets.forEach { replacePara(e, it, f(it.attrs)) } } finally { normalizing = false }
        invalidateText()
    }

    fun setParaStyle(style: ParaStyle) = editParagraphs { { a -> a.copy(style = style) } }

    fun toggleList(kind: ListKind) = editParagraphs { spans ->
        val all = spans.all { it.attrs.list == kind }
        ({ a -> a.copy(list = if (all) ListKind.NONE else kind, checked = false) })
    }

    fun indent(delta: Int) = editParagraphs { { a -> a.copy(indent = (a.indent + delta).coerceIn(0, 8)) } }

    fun toggleQuote() = editParagraphs { spans ->
        val all = spans.all { it.attrs.quote }
        ({ a -> a.copy(quote = !all) })
    }

    /* ------------------------------------------------------ character styles */

    private fun applyTyping(e: Editable, a: Int, b: Int) {
        typing.forEach { st ->
            val before = e.charSpansOf(st, a, a).firstOrNull { e.getSpanEnd(it) == a }
            if (before != null) {
                val s0 = e.getSpanStart(before); e.removeSpan(before); e.setSpan(newCharSpan(st, m), s0, b, CHAR_FLAGS)
            } else e.setSpan(newCharSpan(st, m), a, b, CHAR_FLAGS)
        }
        typingColor?.let { c -> e.setSpan(ForegroundColorSpan(c), a, b, CHAR_FLAGS) }
    }

    private fun covered(e: Spannable, spans: List<Any>, a: Int, b: Int): Boolean {
        if (b <= a) return false
        var pos = a
        val sorted = spans.sortedBy { e.getSpanStart(it) }
        for (sp in sorted) {
            if (e.getSpanStart(sp) > pos) return false
            pos = maxOf(pos, e.getSpanEnd(sp))
            if (pos >= b) return true
        }
        return pos >= b
    }

    private fun removeRange(e: Spannable, spans: List<Any>, a: Int, b: Int, copy: (Any) -> Any) {
        spans.forEach { sp ->
            val s0 = e.getSpanStart(sp); val e0 = e.getSpanEnd(sp)
            e.removeSpan(sp)
            if (s0 < a) e.setSpan(copy(sp), s0, a, CHAR_FLAGS)
            if (e0 > b) e.setSpan(copy(sp), b, e0, CHAR_FLAGS)
        }
    }

    fun toggleChar(style: CharStyle) {
        val e = text ?: return
        val a = selectionStart; val b = selectionEnd
        if (a == b) {
            if (!typing.remove(style)) typing += style
            report()
            return
        }
        val spans = e.charSpansOf(style, a, b)
        if (covered(e, spans, a, b)) removeRange(e, spans, a, b) { newCharSpan(style, m) }
        else { removeRange(e, spans, a, b) { newCharSpan(style, m) }; e.setSpan(newCharSpan(style, m), a, b, CHAR_FLAGS) }
        changed()
    }

    fun setColor(color: Int?) {
        val e = text ?: return
        val a = selectionStart; val b = selectionEnd
        if (a == b) { typingColor = color; report(); return }
        removeRange(e, e.getSpans(a, b, ForegroundColorSpan::class.java).toList(), a, b) { ForegroundColorSpan((it as ForegroundColorSpan).foregroundColor) }
        if (color != null) e.setSpan(ForegroundColorSpan(color), a, b, CHAR_FLAGS)
        changed()
    }

    fun toggleHighlight() {
        val e = text ?: return
        val a = selectionStart; val b = selectionEnd
        if (a == b) return
        val spans = e.getSpans(a, b, BackgroundColorSpan::class.java).toList()
        val had = covered(e, spans, a, b)
        removeRange(e, spans, a, b) { BackgroundColorSpan((it as BackgroundColorSpan).backgroundColor) }
        if (!had) e.setSpan(BackgroundColorSpan((m.accent and 0x00FFFFFF) or 0x66000000), a, b, CHAR_FLAGS)
        changed()
    }

    private fun changed() {
        invalidateText()
        onContentChanged?.invoke()
        report()
    }

    private fun invalidateText() {
        // Re-measures paragraphs whose size changed and redraws list numbers.
        val e = text ?: return
        val sel = selectionStart to selectionEnd
        normalizing = true
        try { e.replace(0, 0, "") } finally { normalizing = false }
        requestLayout(); invalidate()
        if (sel.first >= 0) setSelection(sel.first.coerceAtMost(e.length), sel.second.coerceAtMost(e.length))
        report()
    }

    /* ------------------------------------------------------------- reports */

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!ready || normalizing) return
        val e = text ?: return
        if (selStart == selEnd) {
            // What is typed next looks like the character before the cursor.
            typing.clear()
            typingColor = null
            if (selStart > 0 && e[selStart - 1] != '\n') {
                CharStyle.entries.forEach { st -> if (e.charSpansOf(st, selStart - 1, selStart).isNotEmpty()) typing += st }
                typingColor = e.getSpans(selStart - 1, selStart, ForegroundColorSpan::class.java).lastOrNull()?.foregroundColor
            }
        }
        report()
        reportCaret()
    }

    fun state(): EditorState {
        val e = text ?: return EditorState()
        val a = selectionStart.coerceAtLeast(0); val b = selectionEnd.coerceAtLeast(a)
        val para = paraSpanAt(e, paragraphStart(e, a))?.attrs ?: ParaAttrs()
        val chars = if (a == b) typing.toSet() else CharStyle.entries.filter { covered(e, e.charSpansOf(it, a, b), a, b) }.toSet()
        val color = if (a == b) typingColor else e.getSpans(a, b, ForegroundColorSpan::class.java).firstOrNull()?.foregroundColor
        val hl = a != b && covered(e, e.getSpans(a, b, BackgroundColorSpan::class.java).toList(), a, b)
        return EditorState(para, chars, color, hl, a != b)
    }

    private fun report() { onStateChanged?.invoke(state()) }

    fun reportCaret() {
        val l = layout ?: return
        val pos = selectionEnd.coerceAtLeast(0)
        val line = l.getLineForOffset(pos.coerceAtMost(text?.length ?: 0))
        onCaret?.invoke(l.getLineBottom(line) + totalPaddingTop)
    }

    /* -------------------------------------------------------------- touch */

    private fun checkAt(x: Float, y: Float): NoteParaSpan? {
        val l = layout ?: return null
        val e = text ?: return null
        val line = l.getLineForVertical((y - totalPaddingTop + scrollY).toInt())
        val ls = l.getLineStart(line)
        if (paragraphStart(e, ls) != ls) return null
        val span = paraSpanAt(e, ls) ?: return null
        if (span.attrs.list != ListKind.CHECK) return null
        return span.takeIf { (x - totalPaddingLeft) in span.checkRange() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> checkAt(event.x, event.y)?.let { pressedCheck = it; return true }
            MotionEvent.ACTION_UP -> pressedCheck?.let { sp ->
                pressedCheck = null
                val e = text
                if (e != null && checkAt(event.x, event.y) === sp) {
                    performHapticFeedback(HapticFeedbackType)
                    normalizing = true
                    try { replacePara(e, sp, sp.attrs.copy(checked = !sp.attrs.checked)) } finally { normalizing = false }
                    invalidate()
                    onContentChanged?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> pressedCheck = null
        }
        return super.onTouchEvent(event)
    }

    /* ---------------------------------------------------------- Return key */

    private var lastNewlineAt = 0L

    /**
     * Return always means "new line", whatever the keyboard. Some keyboards
     * (Samsung) also send an editor action, or an Enter key marked as one, which
     * an EditText answers by closing the keyboard ("Fertig").
     */
    override fun onCreateInputConnection(outAttrs: EditorInfo): android.view.inputmethod.InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        outAttrs.imeOptions = (outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION.inv()) or
            EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        return ic
    }

    override fun onEditorAction(actionCode: Int) {
        // Never close the keyboard. If the keyboard sent only the action (no line
        // break), the line break is added here; if it sent both, nothing happens.
        val at = android.os.SystemClock.uptimeMillis()
        postDelayed({ if (lastNewlineAt < at - 50) insertNewline() }, 80)
    }

    private fun insertNewline() {
        val e = text ?: return
        val a = selectionStart.coerceAtLeast(0); val b = selectionEnd.coerceAtLeast(a)
        e.replace(a, b, "\n")
    }

    private fun plainEnter(event: KeyEvent): KeyEvent =
        if (event.flags and KeyEvent.FLAG_EDITOR_ACTION != 0) KeyEvent.changeFlags(event, event.flags and KeyEvent.FLAG_EDITOR_ACTION.inv()) else event

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) super.onKeyDown(keyCode, plainEnter(event))
        else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) super.onKeyUp(keyCode, plainEnter(event))
        else super.onKeyUp(keyCode, event)

    /** Pasting brings in text, not another app's formatting. */
    override fun onTextContextMenuItem(id: Int): Boolean =
        super.onTextContextMenuItem(if (id == android.R.id.paste) android.R.id.pasteAsPlainText else id)

    private companion object {
        val HapticFeedbackType = HapticFeedbackConstants.CLOCK_TICK
    }
}
