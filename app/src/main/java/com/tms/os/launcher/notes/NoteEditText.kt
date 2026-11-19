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
import android.content.Intent
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined

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
    /** A link pressed while the note is only being read (not edited): it opens on release. */
    private var pressedLink: NoteLinkSpan? = null
    private var downX = 0f
    private var downY = 0f
    /** False while the EditText constructor still runs (it already reports selections then). */
    private var ready = false

    init {
        background = null
        // Room above and below the text for the iOS selection handles (dot above the start,
        // dot below the end); the editor overlaps this padding with its neighbours.
        setPadding(0, HANDLE_ROOM.toPx(), 0, HANDLE_ROOM.toPx())
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
            // Android's teardrop handles stay as invisible touch areas; the iOS handles
            // (line + dot) are drawn in onDraw.
            val grip = m.dp(26f).toInt()
            setTextSelectHandle(GradientDrawable().apply { setColor(0); setSize(grip, grip) })
            setTextSelectHandleLeft(GradientDrawable().apply { setColor(0); setSize(grip, grip) })
            setTextSelectHandleRight(GradientDrawable().apply { setColor(0); setSize(grip, grip) })
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
                    relink(s)
                    keepParagraphAt = -1
                } finally {
                    normalizing = false
                }
                onContentChanged?.invoke()
                reportCaret()
                // Typing closes the edit menu, as on iOS.
                hideEditMenu()
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

    /* --------------------------------------------------------------- links */

    /** Marks web addresses, e-mail addresses and phone numbers as links (display only). */
    private fun relink(e: Editable) {
        e.getSpans(0, e.length, NoteLinkSpan::class.java).forEach { e.removeSpan(it) }
        if (e.isEmpty()) return
        val probe = android.text.SpannableString(e.toString())
        if (!android.text.util.Linkify.addLinks(probe, android.text.util.Linkify.WEB_URLS or android.text.util.Linkify.EMAIL_ADDRESSES or android.text.util.Linkify.PHONE_NUMBERS)) return
        probe.getSpans(0, probe.length, android.text.style.URLSpan::class.java).forEach { u ->
            val a = probe.getSpanStart(u); val b = probe.getSpanEnd(u)
            // Numbers glued to words or underscores ("Titel_210625_185056") are no phone numbers.
            if (u.url.startsWith("tel:")) {
                val before = e.getOrNull(a - 1); val after = e.getOrNull(b)
                if ('_' in e.subSequence(a, b) || before?.let { it.isLetterOrDigit() || it == '_' } == true || after?.let { it.isLetterOrDigit() || it == '_' } == true) return@forEach
            }
            e.setSpan(NoteLinkSpan(u.url, m.accent), a, b, CHAR_FLAGS)
        }
    }

    /** The link under a touch point, if the point is really on the link's text. */
    private fun linkAt(x: Float, y: Float): NoteLinkSpan? {
        val l = layout ?: return null
        val e = text ?: return null
        val line = l.getLineForVertical((y - totalPaddingTop + scrollY).toInt())
        val lx = x - totalPaddingLeft + scrollX
        if (lx < l.getLineLeft(line) || lx > l.getLineRight(line)) return null
        val off = l.getOffsetForHorizontal(line, lx)
        return e.getSpans(off, off, NoteLinkSpan::class.java).firstOrNull { e.getSpanStart(it) <= off && off <= e.getSpanEnd(it) }
    }

    /** Writing anywhere in the note (any of its text parts has the cursor): links only take the cursor then. */
    private fun writing(): Boolean = hasFocus() || rootView?.findFocus() is EditText

    private fun open(link: NoteLinkSpan) {
        performHapticFeedback(HapticFeedbackType)
        runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link.url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
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
        // iOS: a tap on the cursor where it already is shows "Auswählen | Alles auswählen | Einsetzen".
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                caretTap = if (hasFocus() && selectionStart == selectionEnd) selectionStart else -1
                caretTapMenu = menuShown
                tapX = event.x; tapY = event.y; tapTime = event.eventTime
            }
            MotionEvent.ACTION_UP -> if (caretTap >= 0) {
                val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                val still = kotlin.math.abs(event.x - tapX) < slop && kotlin.math.abs(event.y - tapY) < slop && event.eventTime - tapTime < 350
                val wasShown = caretTapMenu
                val at = caretTap
                caretTap = -1
                if (still) post { if (selectionStart == at && selectionEnd == at) { if (wasShown) hideEditMenu() else showEditMenu() } }
            }
        }
        // Reading (keyboard closed): a tap on a link opens it, as on iOS. While
        // writing, a tap on a link just places the cursor.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (!writing()) linkAt(event.x, event.y)?.let { pressedLink = it; downX = event.x; downY = event.y; return true }
            MotionEvent.ACTION_MOVE -> pressedLink?.let {
                val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                if (kotlin.math.abs(event.x - downX) > slop || kotlin.math.abs(event.y - downY) > slop) pressedLink = null
                return true
            }
            MotionEvent.ACTION_UP -> pressedLink?.let { link -> pressedLink = null; if (linkAt(event.x, event.y) === link) open(link); return true }
            MotionEvent.ACTION_CANCEL -> if (pressedLink != null) { pressedLink = null; return true }
        }
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

    /* ------------------------------------------------------- iOS edit menu */

    /** The iOS edit menu to show (null: hide). Positions are in window pixels. */
    var onEditMenu: ((com.tms.os.launcher.ui.components.EditMenuSpec?) -> Unit)? = null
    /** "Datei anhängen" in the edit menu: the note's attach menu. */
    var onAttachRequest: (() -> Unit)? = null
    private var menuShown = false
    private var caretTap = -1
    private var caretTapMenu = false
    private var tapX = 0f
    private var tapY = 0f
    private var tapTime = 0L

    private val menuModeCallback = object : android.view.ActionMode.Callback2() {
        override fun onCreateActionMode(mode: android.view.ActionMode, menu: android.view.Menu) = true
        override fun onPrepareActionMode(mode: android.view.ActionMode, menu: android.view.Menu): Boolean {
            // An empty system toolbar is not shown; the iOS menu appears instead.
            menu.clear()
            post { showEditMenu() }
            return true
        }
        override fun onActionItemClicked(mode: android.view.ActionMode, item: android.view.MenuItem) = false
        override fun onDestroyActionMode(mode: android.view.ActionMode) { hideEditMenu() }
    }

    init {
        // Android's floating copy/paste toolbar is replaced by the iOS edit menu.
        customSelectionActionModeCallback = menuModeCallback
        if (Build.VERSION.SDK_INT >= 23) customInsertionActionModeCallback = menuModeCallback
    }

    fun hideEditMenu() {
        if (!menuShown) return
        menuShown = false
        onEditMenu?.invoke(null)
    }

    private fun showEditMenu() {
        if (!hasFocus()) return
        val spec = editMenuSpec() ?: return
        menuShown = true
        onEditMenu?.invoke(spec)
    }

    private fun editMenuSpec(): com.tms.os.launcher.ui.components.EditMenuSpec? {
        val l = layout ?: return null
        val e = text ?: return null
        val a = selectionStart.coerceIn(0, e.length); val b = selectionEnd.coerceIn(a, e.length)
        val loc = IntArray(2); getLocationInWindow(loc)
        val ox = loc[0] + totalPaddingLeft - scrollX
        val oy = loc[1] + totalPaddingTop - scrollY
        val la = l.getLineForOffset(a); val lb = l.getLineForOffset(b)
        val left = if (la == lb) l.getPrimaryHorizontal(a) else 0f
        val right = if (la == lb) maxOf(l.getPrimaryHorizontal(b), left + 1f) else l.width.toFloat()
        val anchor = androidx.compose.ui.geometry.Rect(ox + left, (oy + l.getLineTop(la)).toFloat(), ox + right, (oy + l.getLineBottom(lb)).toFloat())

        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val canPaste = clip.hasPrimaryClip()
        val I = androidx.compose.material.icons.Icons.Outlined
        fun item(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, destructive: Boolean = false, run: () -> Unit) =
            com.tms.os.launcher.ui.components.EditMenuItem(label, icon, destructive) { run() }
        val cut = item("Ausschneiden", I.ContentCut) { onTextContextMenuItem(android.R.id.cut) }
        val copy = item("Kopieren", I.ContentCopy) { onTextContextMenuItem(android.R.id.copy) }
        val paste = item("Einsetzen", I.ContentPaste) { onTextContextMenuItem(android.R.id.paste) }
        val all = item("Alles auswählen", I.SelectAll) { onTextContextMenuItem(android.R.id.selectAll) }

        if (a == b) {
            // Cursor only: "Auswählen | Alles auswählen | Einsetzen", as on iOS.
            val select = item("Auswählen", null) { selectWordAt(a) }
            val compact = listOfNotNull(select.takeIf { e.isNotBlank() }, all.takeIf { e.isNotEmpty() }, paste.takeIf { canPaste })
            if (compact.isEmpty()) return null
            return com.tms.os.launcher.ui.components.EditMenuSpec(anchor, compact,
                rows = listOfNotNull(onAttachRequest?.let { att -> item("Datei anhängen", I.AttachFile) { att() } }))
        }

        val selected = e.subSequence(a, b).toString()
        val rows = ArrayList<com.tms.os.launcher.ui.components.EditMenuItem>()
        rows += item("Löschen", I.Delete, destructive = true) { text?.delete(a, b) }
        rows += com.tms.os.launcher.ui.components.EditMenuItem("Format", I.TextFormat, submenu = listOf(
            item("Fett", androidx.compose.material.icons.Icons.Rounded.FormatBold) { toggleChar(CharStyle.BOLD) },
            item("Kursiv", androidx.compose.material.icons.Icons.Rounded.FormatItalic) { toggleChar(CharStyle.ITALIC) },
            item("Unterstrichen", androidx.compose.material.icons.Icons.Rounded.FormatUnderlined) { toggleChar(CharStyle.UNDERLINE) },
            item("Durchgestrichen", androidx.compose.material.icons.Icons.Rounded.FormatStrikethrough) { toggleChar(CharStyle.STRIKE) }
        ))
        onAttachRequest?.let { att -> rows += item("Datei anhängen", I.AttachFile) { att() } }
        rows += item("Auswahl suchen", I.Search) { searchWeb(selected) }
        rows += item("Teilen …", I.IosShare) { share(selected) }
        processTextApps().forEach { (label, intent) -> rows += item(label, if (label.contains("bersetz") || label.contains("ranslat")) I.Translate else I.Apps) { runCatching { context.startActivity(Intent(intent).putExtra(Intent.EXTRA_PROCESS_TEXT, selected).putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }
        rows += all
        return com.tms.os.launcher.ui.components.EditMenuSpec(
            anchor,
            compact = listOfNotNull(cut, copy, paste.takeIf { canPaste }),
            topRow = listOfNotNull(cut, copy, paste.takeIf { canPaste }),
            rows = rows
        )
    }

    /** Selects the word at [at] the way a double tap does, with handles (through the accessibility action). */
    private fun selectWordAt(at: Int) {
        val e = text ?: return
        val it = java.text.BreakIterator.getWordInstance()
        it.setText(e.toString())
        var end = it.following(at.coerceAtMost(e.length - 1).coerceAtLeast(0)).let { if (it == java.text.BreakIterator.DONE) e.length else it }
        var start = it.previous().let { if (it == java.text.BreakIterator.DONE) 0 else it }
        if (start == end || e.subSequence(start, end).isBlank()) { start = (at - 1).coerceAtLeast(0); end = at.coerceAtLeast(start + 1).coerceAtMost(e.length) }
        performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_SELECTION, android.os.Bundle().apply {
            putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
            putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        })
    }

    private fun searchWeb(q: String) {
        val web = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, q).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(web) }.onFailure {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=" + android.net.Uri.encode(q))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    private fun share(t: String) {
        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Apps that offer text actions ("Übersetzen" from Google Translate and the like), at most two. */
    private fun processTextApps(): List<Pair<String, Intent>> = cachedProcessText ?: runCatching {
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain"), 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .take(2)
            .map { ri -> ri.loadLabel(pm).toString() to Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain").setClassName(ri.activityInfo.packageName, ri.activityInfo.name) }
    }.getOrDefault(emptyList()).also { cachedProcessText = it }

    private var cachedProcessText: List<Pair<String, Intent>>? = null

    /* ------------------------------------------------------ iOS handles */

    private val handlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = m.accent }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val l = layout ?: return
        val a = selectionStart; val b = selectionEnd
        if (!hasFocus() || a < 0 || a == b) return
        val ox = totalPaddingLeft - scrollX.toFloat()
        val oy = totalPaddingTop - scrollY.toFloat()
        val w = m.dp(2f); val r = m.dp(5.5f)
        // Start: line with the dot above; end: line with the dot below (iOS lollipops).
        val la = l.getLineForOffset(a)
        val xa = ox + l.getPrimaryHorizontal(a)
        val ta = oy + l.getLineTop(la); val ba = oy + l.getLineBottom(la) - l.spacingAdd
        canvas.drawRect(xa - w / 2, ta, xa + w / 2, ba, handlePaint)
        canvas.drawCircle(xa, ta - r + m.dp(1f), r, handlePaint)
        // The end at the start of a line belongs to the end of the line before.
        var lb = l.getLineForOffset(b)
        var xb = ox + l.getPrimaryHorizontal(b)
        if (lb > 0 && b == l.getLineStart(lb) && b > a) { lb -= 1; xb = ox + l.getLineRight(lb) }
        val tb = oy + l.getLineTop(lb); val bb = oy + l.getLineBottom(lb) - l.spacingAdd
        canvas.drawRect(xb - w / 2, tb, xb + w / 2, bb, handlePaint)
        canvas.drawCircle(xb, bb + r - m.dp(1f), r, handlePaint)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) hideEditMenu()
    }

    /** Pasting brings in text, not another app's formatting. */
    override fun onTextContextMenuItem(id: Int): Boolean =
        super.onTextContextMenuItem(if (id == android.R.id.paste) android.R.id.pasteAsPlainText else id)

    private fun Float.toPx(): Int = m.dp(this).toInt()

    companion object {
        private val HapticFeedbackType = HapticFeedbackConstants.CLOCK_TICK
        /** Padding (dp) above and below the text for the iOS selection handles. */
        const val HANDLE_ROOM = 12f
    }
}
