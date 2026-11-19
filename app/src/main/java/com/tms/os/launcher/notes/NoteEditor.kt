@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.tms.os.launcher.notes

import android.content.Context
import android.net.Uri
import android.text.SpannableStringBuilder
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A note, as in iOS Notes: date on top, rich text, tables, pictures and
 * drawings in between. Editing shows the yellow "Fertig" button and the
 * toolbar above the keyboard; "Aa" swaps the keyboard for the Format panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: String,
    state: NotesState,
    store: NoteStore,
    focusOnOpen: Boolean,
    onBack: () -> Unit,
    onNewNote: () -> Unit,
    onShare: (Note) -> Unit,
    onToggleLock: (Note) -> Unit,
    authenticate: (String, (Boolean) -> Unit) -> Unit
) {
    val c = LocalNotesColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Saving on leave lives above every early return (locked note, missing note),
    // so it runs exactly once, when the editor really goes away, with the text
    // still in the editors (they are kept until then, see onRelease below).
    val saver = remember(noteId) { object { var snapshot: (() -> List<NoteBlock>)? = null; var readOnly = false; var hadContent: Boolean? = null } }
    DisposableEffect(noteId) {
        onDispose {
            saver.snapshot?.let { snap -> if (!saver.readOnly) store.updateBlocks(noteId, snap()) }
            store.discardIfEmpty(noteId, hadContent = saver.hadContent ?: true)
        }
    }
    val note = state.notes.firstOrNull { it.id == noteId }
    if (note == null) { LaunchedEffect(Unit) { onBack() }; return }
    if (saver.hadContent == null) saver.hadContent = !note.isEmpty

    var unlocked by remember(noteId) { mutableStateOf(!note.locked) }
    if (note.locked && !unlocked) {
        LockedNote(onBack) { authenticate("Notiz anzeigen") { ok -> if (ok) unlocked = true } }
        return
    }

    val metrics = remember(c.dark) { NoteMetrics(context, c.dark) }
    val readOnly = note.deletedAt != null
    val blocks = remember(noteId) { mutableStateListOf<NoteBlock>().apply { addAll(note.blocks.ifEmpty { listOf(NoteBlock.Text(store.newId(), RichText.EMPTY_TITLE)) }) } }
    val editors = remember(noteId) { mutableMapOf<String, NoteEditText>() }
    val tops = remember(noteId) { mutableStateMapOf<String, Float>() }
    val flags = remember(noteId) { object { var loading = false; var pendingUndo = false; var committed: List<NoteBlock> = note.blocks } }
    var version by remember(noteId) { mutableIntStateOf(0) }
    var dirty by remember(noteId) { mutableIntStateOf(0) }
    val undo = remember(noteId) { mutableStateListOf<List<NoteBlock>>() }
    var activeId by remember(noteId) { mutableStateOf<String?>(null) }
    var editorState by remember { mutableStateOf(EditorState()) }
    var focused by remember { mutableStateOf(false) }
    var formatPanel by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var attachMenu by remember { mutableStateOf(false) }
    var moveSheet by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf<NoteBlock.Drawing?>(null) }
    var newDrawing by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<String?>(null) }
    var pendingFocus by remember(noteId) { mutableStateOf(if (focusOnOpen) blocks.firstOrNull { it is NoteBlock.Text }?.id else null) }
    /** A table just inserted: the cursor goes into its first cell, as on iOS. */
    var focusTable by remember(noteId) { mutableStateOf<String?>(null) }
    val imeUp = WindowInsets.isImeVisible
    val editing = (focused || imeUp || formatPanel) && !readOnly
    val scroll = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    val imm = remember { context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    fun snapshot(): List<NoteBlock> = blocks.map { b ->
        if (b is NoteBlock.Text) NoteBlock.Text(b.id, editors[b.id]?.text?.toRichText() ?: b.rich) else b
    }
    saver.snapshot = ::snapshot
    saver.readOnly = readOnly
    fun markDirty() {
        if (!flags.pendingUndo) { undo.add(flags.committed); if (undo.size > 60) undo.removeAt(0); flags.pendingUndo = true }
        dirty++
    }
    LaunchedEffect(dirty) {
        if (dirty == 0) return@LaunchedEffect
        delay(350)
        val snap = snapshot()
        store.updateBlocks(noteId, snap)
        flags.committed = snap
        flags.pendingUndo = false
    }
    fun restore(list: List<NoteBlock>) {
        blocks.clear(); blocks.addAll(list)
        flags.committed = list
        version++
        store.updateBlocks(noteId, list)
    }
    fun hideKeyboard() {
        val v = editors.values.firstOrNull { it.hasFocus() } ?: editors.values.firstOrNull { it.isAttachedToWindow }
        v?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
    fun done() {
        formatPanel = false
        // Keyboard first, while an editor still has the focus; then the focus goes.
        hideKeyboard()
        editors.values.forEach { it.clearFocus() }
        focused = false
    }
    fun focusEnd() {
        val last = blocks.lastOrNull { it is NoteBlock.Text } ?: return
        editors[last.id]?.let { ed ->
            ed.requestFocus(); ed.setSelection(ed.text?.length ?: 0)
            // The keyboard is asked for once the field really has the input connection.
            ed.post { imm.showSoftInput(ed, 0) }
        }
    }

    /** Puts [block] where the cursor is: the text is split around it, and typing continues below it. */
    fun insertAtCursor(block: NoteBlock) {
        val idx = blocks.indexOfFirst { it.id == activeId }
        val ed = activeId?.let { editors[it] }
        if (idx < 0 || ed == null) {
            blocks.add(block)
            val after = NoteBlock.Text(store.newId(), RichText.EMPTY_BODY)
            blocks.add(after)
            if (block is NoteBlock.Table) focusTable = block.id else if (block !is NoteBlock.Drawing) pendingFocus = after.id
        } else {
            val text = ed.text ?: return
            val cur = ed.selectionEnd.coerceIn(0, text.length)
            val before = SpannableStringBuilder(text.subSequence(0, cur)).let { if (it.endsWith("\n")) it.delete(it.length - 1, it.length) else it }
            val after = SpannableStringBuilder(text.subSequence(cur, text.length)).let { if (it.startsWith("\n")) it.delete(0, 1) else it }
            flags.loading = true
            ed.setText(before)
            flags.loading = false
            blocks[idx] = NoteBlock.Text(blocks[idx].id, before.toRichText())
            val afterBlock = NoteBlock.Text(store.newId(), if (after.isEmpty()) RichText.EMPTY_BODY else after.toRichText())
            blocks.add(idx + 1, block)
            blocks.add(idx + 2, afterBlock)
            // After a drawing the keyboard stays away, as on iOS.
            if (block is NoteBlock.Table) focusTable = block.id else if (block !is NoteBlock.Drawing) pendingFocus = afterBlock.id
        }
        markDirty()
    }

    /** Removes a table/picture/drawing and joins the text around it again. */
    fun removeBlock(id: String) {
        val idx = blocks.indexOfFirst { it.id == id }
        if (idx < 0) return
        blocks.removeAt(idx)
        val prev = blocks.getOrNull(idx - 1) as? NoteBlock.Text
        val next = blocks.getOrNull(idx) as? NoteBlock.Text
        if (prev != null && next != null) {
            val a = SpannableStringBuilder(editors[prev.id]?.text ?: prev.rich.toSpannable(metrics))
            val b = editors[next.id]?.text ?: next.rich.toSpannable(metrics)
            if (a.isNotEmpty() && b.isNotEmpty()) a.append("\n")
            a.append(b)
            blocks[idx - 1] = NoteBlock.Text(prev.id, a.toRichText())
            blocks.removeAt(idx)
            editors[prev.id]?.let { flags.loading = true; it.setText(a); flags.loading = false }
        }
        if (blocks.none { it is NoteBlock.Text }) blocks.add(NoteBlock.Text(store.newId(), RichText.EMPTY_BODY))
        markDirty()
    }

    // ------------------------------------------------------------ attachments
    val media = remember { NoteMedia(context, store) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { media.importImage(uri)?.let { insertAtCursor(NoteBlock.Image(store.newId(), it)) } }
    }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null) scope.launch { media.importFile(f)?.let { insertAtCursor(NoteBlock.Image(store.newId(), it)) } }
        else f?.delete()
    }
    val scan = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val pages = NoteScanner.pages(r.resultCode, r.data)
        if (pages.isNotEmpty()) scope.launch { pages.forEach { u -> media.importImage(u)?.let { insertAtCursor(NoteBlock.Image(store.newId(), it, scan = true)) } } }
    }

    // Back leaves the note the same way the back button does: editing ends first.
    BackHandler { done(); onBack() }
    BackHandler(enabled = formatPanel) { formatPanel = false; activeId?.let { editors[it] }?.let { imm.showSoftInput(it, 0) } }
    // Closing the keyboard (back gesture, keyboard's own button) ends editing, as
    // on iOS: the cursor goes and the bottom bar with the paperclip comes back.
    var imeWasUp by remember { mutableStateOf(false) }
    LaunchedEffect(imeUp) {
        NotesDiagnostics.event("Tastatur sichtbar=$imeUp")
        if (imeUp) formatPanel = false
        else if (imeWasUp && !formatPanel && !attachMenu && drawing == null && !newDrawing) done()
        imeWasUp = imeUp
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        // -------------------------------------------------------------- content
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .onSizeChanged { viewport = it.height }
                // A tap anywhere that is not text, a table or a picture continues
                // writing at the end of the note, with the keyboard, as on iOS.
                .pointerInput(readOnly) { if (!readOnly) detectTapGestures(onTap = { NotesDiagnostics.event("Tipp auf freie Fläche"); focusEnd() }) }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 58.dp, bottom = 120.dp)
            ) {
                Text(longDate(note.modifiedAt), style = NotesText.footnote.copy(color = c.secondary, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                blocks.forEach { block ->
                    androidx.compose.runtime.key(block.id) {
                        when (block) {
                            is NoteBlock.Text -> AndroidView(
                                factory = { ctx ->
                                    NoteEditText(ctx, metrics).also { ed ->
                                        // A re-created editor continues with what was typed, not the text the note opened with.
                                        val latest = editors[block.id]?.text?.toRichText() ?: (blocks.firstOrNull { it.id == block.id } as? NoteBlock.Text)?.rich ?: block.rich
                                        editors[block.id] = ed
                                        ed.tag = version
                                        flags.loading = true
                                        ed.setText(latest.toSpannable(metrics))
                                        flags.loading = false
                                        ed.isFocusable = !readOnly
                                        ed.isFocusableInTouchMode = !readOnly
                                        ed.onContentChanged = { if (!flags.loading) markDirty() }
                                        ed.onStateChanged = { s -> if (ed.hasFocus()) editorState = s }
                                        ed.onCaret = { y ->
                                            val top = tops[block.id] ?: 0f
                                            val target = top + y + with(density) { 58.dp.toPx() }
                                            val margin = with(density) { 96.dp.toPx() }
                                            if (viewport > 0 && target > scroll.value + viewport - margin) scope.launch { scroll.animateScrollTo((target - viewport + margin).toInt()) }
                                        }
                                        ed.setOnFocusChangeListener { _, f ->
                                            NotesDiagnostics.event("Textfeld Fokus=$f")
                                            if (f) { activeId = block.id; editorState = ed.state() }
                                            focused = editors.values.any { it.hasFocus() }
                                        }
                                    }
                                },
                                update = { ed ->
                                    if (ed.tag != version) {
                                        ed.tag = version
                                        val rich = (blocks.firstOrNull { it.id == block.id } as? NoteBlock.Text)?.rich ?: block.rich
                                        flags.loading = true; ed.setText(rich.toSpannable(metrics)); flags.loading = false
                                    }
                                    if (pendingFocus == block.id) {
                                        pendingFocus = null
                                        ed.post { ed.requestFocus(); ed.setSelection(0); imm.showSoftInput(ed, 0) }
                                    }
                                },
                                // The editor stays in [editors]: the save on leaving the note reads its text after
                                // Compose has already released the views. Removed blocks are simply no longer in [blocks].
                                onRelease = { ed -> ed.onContentChanged = null; ed.onStateChanged = null; ed.onCaret = null; ed.onFocusChangeListener = null },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).onGloballyPositioned { tops[block.id] = it.positionInParent().y }
                            )
                            is NoteBlock.Table -> TableBlockView(
                                block, readOnly, autoFocus = focusTable == block.id, onAutoFocused = { focusTable = null },
                                onChange = { cells -> val i = blocks.indexOfFirst { it.id == block.id }; if (i >= 0) { blocks[i] = NoteBlock.Table(block.id, cells); markDirty() } },
                                onDelete = { removeBlock(block.id) },
                                onFocus = { activeId = null; focused = true }
                            )
                            is NoteBlock.Image -> PictureBlock(block.file, onOpen = { viewer = block.file }, onDelete = { removeBlock(block.id) }, readOnly = readOnly)
                            is NoteBlock.Drawing -> PictureBlock(block.file, onOpen = { if (!readOnly) drawing = block }, onDelete = { removeBlock(block.id) }, readOnly = readOnly, drawingBg = true)
                        }
                    }
                }
                // Tapping below the text continues writing at its end, as on iOS.
                Spacer(Modifier.fillMaxWidth().height(320.dp))
            }
        }

        // -------------------------------------------------------------- top bar
        NotesTopBar("", false, leading = { GlassCircle(Icons.AutoMirrored.Rounded.ArrowBackIos) { done(); onBack() } }) {
            if (readOnly) {
                GlassTextButton("Wiederherstellen", bold = true, color = c.accent) { store.move(listOf(noteId), note.folderId) }
            } else {
                if (editing && undo.isNotEmpty()) GlassCircle(Icons.AutoMirrored.Rounded.Undo) {
                    if (flags.pendingUndo) { store.updateBlocks(noteId, snapshot()); flags.pendingUndo = false }
                    undo.removeLastOrNull()?.let { restore(it) }
                }
                GlassCapsule {
                    CapsuleIcon(Icons.Outlined.IosShare) { onShare(note.copy(blocks = snapshot())) }
                    CapsuleIcon(Icons.Rounded.MoreHoriz) { menu = true }
                }
                if (editing) AccentCircle(Icons.Rounded.Check) { done() }
            }
        }

        // ------------------------------------------------------------ bottom bars
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
            if (!readOnly && !formatPanel) {
                if (imeUp && editing) KeyboardToolbar(
                    onFormat = { formatPanel = true; hideKeyboard() },
                    onChecklist = { activeId?.let { editors[it] }?.toggleList(ListKind.CHECK) ?: run { focusEnd() } },
                    onTable = { insertAtCursor(NoteBlock.Table(store.newId(), List(2) { List(2) { "" } })) },
                    onAttach = { attachMenu = true },
                    onDraw = { done(); newDrawing = true }
                ) else if (!imeUp) Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    GlassCapsule {
                        CapsuleIcon(Icons.Rounded.Checklist) { focusEnd(); scope.launch { delay(120); activeId?.let { editors[it] }?.toggleList(ListKind.CHECK) } }
                        CapsuleIcon(Icons.Rounded.AttachFile) { attachMenu = true }
                        CapsuleIcon(Icons.Outlined.Draw) { newDrawing = true }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(48.dp).pressable(onNewNote).notesGlass(c), contentAlignment = Alignment.Center) {
                        Icon(ComposeIcon, null, tint = c.text, modifier = Modifier.size(25.dp))
                    }
                }
            }
        }
        AnimatedVisibility(
            formatPanel,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 420f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(200)) { it } + fadeOut()
        ) {
            val ed = activeId?.let { editors[it] }
            FormatPanel(
                state = editorState,
                onClose = { formatPanel = false; ed?.let { it.requestFocus(); imm.showSoftInput(it, 0) } },
                onStyle = { ed?.setParaStyle(it); ed?.let { e -> editorState = e.state() } },
                onChar = { ed?.toggleChar(it); ed?.let { e -> editorState = e.state() } },
                onHighlight = { ed?.toggleHighlight(); ed?.let { e -> editorState = e.state() } },
                onColor = { ed?.setColor(it); ed?.let { e -> editorState = e.state() } },
                onList = { ed?.toggleList(it); ed?.let { e -> editorState = e.state() } },
                onIndent = { ed?.indent(it); ed?.let { e -> editorState = e.state() } },
                onQuote = { ed?.toggleQuote(); ed?.let { e -> editorState = e.state() } }
            )
        }

        NotesMenu(menu, listOf(
            MenuItem(if (note.pinned) "Notiz loslösen" else "Notiz anheften", Icons.Rounded.PushPin) { store.togglePin(noteId) },
            MenuItem(if (note.locked) "Sperre entfernen" else "Notiz sperren", if (note.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock) { onToggleLock(note) },
            MenuItem("Notiz bewegen", Icons.Outlined.DriveFileMove) { moveSheet = true },
            MenuItem("Löschen", Icons.Outlined.Delete, destructive = true) { done(); store.moveToTrash(listOf(noteId)); onBack() }
        ), onDismiss = { menu = false })

        NotesMenu(attachMenu, listOf(
            MenuItem("Foto auswählen", Icons.Outlined.Photo) { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            MenuItem("Foto aufnehmen", Icons.Outlined.PhotoCamera) {
                val f = media.newCameraFile(); cameraFile = f
                runCatching { takePhoto.launch(FileProvider.getUriForFile(context, "${context.packageName}.notes.files", f)) }
            },
            MenuItem("Dokumente scannen", Icons.Outlined.DocumentScanner) {
                NoteScanner.start(context) { sender -> scan.launch(IntentSenderRequest.Builder(sender).build()) }
            }
        ), onDismiss = { attachMenu = false }, alignTop = false, alignStart = !imeUp)

        MoveNotesSheet(
            visible = moveSheet,
            count = 1,
            folders = state.folders,
            currentFolder = note.folderId,
            onDismiss = { moveSheet = false },
            onMove = { target -> store.move(listOf(noteId), target); moveSheet = false },
            onCreateFolder = { name -> store.createFolder(name).id }
        )

        viewer?.let { file -> PictureViewer(file) { viewer = null } }

        // The drawing canvas lies over the note instead of replacing it, so the
        // text editors (and what was typed) stay alive underneath.
        if (drawing != null || newDrawing) {
            DrawingEditor(
                initial = drawing?.strokes,
                onCancel = { drawing = null; newDrawing = false },
                onDone = { strokesJson, png ->
                    scope.launch {
                        val file = media.saveDrawing(png, drawing?.file)
                        val old = drawing
                        if (old != null) {
                            val i = blocks.indexOfFirst { it.id == old.id }
                            if (i >= 0) blocks[i] = NoteBlock.Drawing(old.id, file, strokesJson)
                            markDirty()
                        } else if (strokesJson.isNotEmpty()) insertAtCursor(NoteBlock.Drawing(store.newId(), file, strokesJson))
                        drawing = null; newDrawing = false
                    }
                }
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                Locked note                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun LockedNote(onBack: () -> Unit, onUnlock: () -> Unit) {
    val c = LocalNotesColors.current
    LaunchedEffect(Unit) { onUnlock() }
    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Lock, null, tint = c.secondary, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("Diese Notiz ist gesperrt.", style = NotesText.headline.copy(color = c.text))
            Spacer(Modifier.height(6.dp))
            Text("Notiz anzeigen", style = NotesText.body.copy(color = c.accent), modifier = Modifier.clickable(onClick = onUnlock).padding(8.dp))
        }
        NotesTopBar("", false, leading = { GlassCircle(Icons.AutoMirrored.Rounded.ArrowBackIos, onClick = onBack) }) {}
    }
}

/* -------------------------------------------------------------------------- */
/*                              Keyboard toolbar                              */
/* -------------------------------------------------------------------------- */

@Composable
private fun KeyboardToolbar(onFormat: () -> Unit, onChecklist: () -> Unit, onTable: () -> Unit, onAttach: () -> Unit, onDraw: () -> Unit) {
    val c = LocalNotesColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(46.dp).notesGlass(c)
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).pressable(onFormat), contentAlignment = Alignment.Center) {
            Text("Aa", style = NotesText.body.copy(color = c.text, fontWeight = FontWeight.Medium, fontSize = 18.sp))
        }
        CapsuleIcon(Icons.Rounded.Checklist, onClick = onChecklist)
        CapsuleIcon(Icons.Outlined.TableChart, onClick = onTable)
        CapsuleIcon(Icons.Rounded.AttachFile, onClick = onAttach)
        CapsuleIcon(Icons.Outlined.Draw, onClick = onDraw)
    }
}

/* -------------------------------------------------------------------------- */
/*                                Format panel                                */
/* -------------------------------------------------------------------------- */

private val TextColors = listOf(
    Color(0xFFFF6482) to "Pink", Color(0xFFBF5AF2) to "Lila", Color(0xFFFF9F0A) to "Orange", Color(0xFF63E6BE) to "Mint", Color(0xFF64D2FF) to "Blau"
)

@Composable
private fun FormatPanel(
    state: EditorState,
    onClose: () -> Unit,
    onStyle: (ParaStyle) -> Unit,
    onChar: (CharStyle) -> Unit,
    onHighlight: () -> Unit,
    onColor: (Int?) -> Unit,
    onList: (ListKind) -> Unit,
    onIndent: (Int) -> Unit,
    onQuote: () -> Unit
) {
    val c = LocalNotesColors.current
    var colors by remember { mutableStateOf(false) }
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(10.dp)
            .fillMaxWidth()
            .notesGlass(c, RoundedCornerShape(30.dp))
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(vertical = 14.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Text("Format", style = NotesText.headline.copy(color = c.text, fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.CenterStart))
            Box(Modifier.align(Alignment.CenterEnd).size(30.dp).clip(CircleShape).background(c.text.copy(alpha = 0.1f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, tint = c.secondary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        // Paragraph styles, each written in its own style.
        val styleScroll = rememberScrollState()
        // The current style is always in view (Text and Monostil sit at the end of the row).
        LaunchedEffect(state.para.style, styleScroll.maxValue) {
            styleScroll.animateScrollTo(if (state.para.style.ordinal >= ParaStyle.BODY.ordinal) styleScroll.maxValue else 0)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(styleScroll).padding(horizontal = 12.dp)
        ) {
            listOf(
                ParaStyle.TITLE to ("Titel" to NotesText.largeTitle.copy(fontSize = 24.sp, lineHeight = 28.sp)),
                ParaStyle.HEADING to ("Überschrift" to NotesText.headline.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold)),
                ParaStyle.SUBHEADING to ("Unterüberschrift" to NotesText.headline),
                ParaStyle.BODY to ("Text" to NotesText.body),
                ParaStyle.MONO to ("Monostil" to TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp))
            ).forEach { (style, pair) ->
                val sel = state.para.style == style
                Box(
                    Modifier.height(44.dp).clip(CircleShape).background(if (sel) c.accent else Color.Transparent).clickable { onStyle(style) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(pair.first, style = pair.second.copy(color = if (sel) c.onAccent else c.text), maxLines = 1) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
            Segment(Modifier.weight(1f)) {
                SegButton(Icons.Rounded.FormatBold, CharStyle.BOLD in state.chars) { onChar(CharStyle.BOLD) }
                SegButton(Icons.Rounded.FormatItalic, CharStyle.ITALIC in state.chars) { onChar(CharStyle.ITALIC) }
                SegButton(Icons.Rounded.FormatUnderlined, CharStyle.UNDERLINE in state.chars) { onChar(CharStyle.UNDERLINE) }
                SegButton(Icons.Rounded.FormatStrikethrough, CharStyle.STRIKE in state.chars) { onChar(CharStyle.STRIKE) }
            }
            Spacer(Modifier.width(10.dp))
            Segment {
                SegButton(Icons.Rounded.BorderColor, state.highlight, enabled = state.hasSelection, onClick = onHighlight)
                Box(Modifier.size(46.dp, 40.dp).clickable { colors = !colors }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(state.color?.let { Color(it) } ?: Color(0xFFBF5AF2)).border(2.dp, c.text.copy(alpha = 0.25f), CircleShape))
                }
            }
        }
        AnimatedVisibility(colors) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp)) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(c.text).clickable { onColor(null); colors = false }.border(if (state.color == null) 3.dp else 0.dp, c.accent, CircleShape))
                TextColors.forEach { (col, _) ->
                    Box(Modifier.size(30.dp).clip(CircleShape).background(col).clickable { onColor(col.toArgbInt()); colors = false }
                        .border(if (state.color == col.toArgbInt()) 3.dp else 0.dp, c.text, CircleShape))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
            Segment(Modifier.weight(1f)) {
                SegButton(Icons.AutoMirrored.Rounded.FormatListBulleted, state.para.list == ListKind.BULLET) { onList(ListKind.BULLET) }
                SegButton(Icons.Rounded.Remove, state.para.list == ListKind.DASH) { onList(ListKind.DASH) }
                SegButton(Icons.Rounded.FormatListNumbered, state.para.list == ListKind.NUMBER) { onList(ListKind.NUMBER) }
            }
            Spacer(Modifier.width(10.dp))
            Segment {
                SegButton(Icons.AutoMirrored.Rounded.FormatIndentDecrease, false, enabled = state.para.indent > 0) { onIndent(-1) }
                SegButton(Icons.AutoMirrored.Rounded.FormatIndentIncrease, false) { onIndent(1) }
            }
            Spacer(Modifier.width(10.dp))
            Segment { SegButton(Icons.Rounded.FormatQuote, state.para.quote, onClick = onQuote) }
        }
    }
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

@Composable
private fun Segment(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = LocalNotesColors.current
    Row(modifier.height(44.dp).clip(CircleShape).background(c.text.copy(alpha = if (c.dark) 0.1f else 0.06f)).padding(2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) { content() }
}

@Composable
private fun SegButton(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalNotesColors.current
    Box(
        Modifier.size(46.dp, 40.dp).clip(CircleShape).background(if (selected) c.accent else Color.Transparent).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = (if (selected) c.onAccent else c.text).copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(22.dp)) }
}

/* -------------------------------------------------------------------------- */
/*                                   Tables                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun TableBlockView(block: NoteBlock.Table, readOnly: Boolean, autoFocus: Boolean, onAutoFocused: () -> Unit, onChange: (List<List<String>>) -> Unit, onDelete: () -> Unit, onFocus: () -> Unit) {
    val c = LocalNotesColors.current
    val cells = block.cells
    val rows = cells.size
    val cols = cells.firstOrNull()?.size ?: 0
    var focus by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var rowMenu by remember { mutableStateOf(false) }
    var colMenu by remember { mutableStateOf(false) }
    fun edit(transform: (MutableList<MutableList<String>>) -> Unit) {
        val m = cells.map { it.toMutableList() }.toMutableList(); transform(m); onChange(m)
    }
    val firstCell = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) { delay(80); runCatching { firstCell.requestFocus() }; onAutoFocused() } }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 6.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)) {
    // Few columns share the full width; many columns scroll sideways.
    val cellW = maxOf(120.dp, (maxWidth - 14.dp) / cols.coerceAtLeast(1))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            // Row handle "⋮" beside the focused row.
            Column(Modifier.width(14.dp).padding(top = 16.dp)) {
                for (r in 0 until rows) Box(Modifier.height(44.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (focus?.first == r && !readOnly) Text("⋮", style = NotesText.headline.copy(color = c.accent), modifier = Modifier.clickable { rowMenu = true })
                }
            }
            Column {
                // Column handle "•••" above the focused column.
                Row(Modifier.height(16.dp)) {
                    for (col in 0 until cols) Box(Modifier.width(cellW).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (focus?.second == col && !readOnly) Text("•••", style = NotesText.caption.copy(color = c.accent), modifier = Modifier.clickable { colMenu = true })
                    }
                }
                Column(Modifier.border(0.6.dp, c.separator.copy(alpha = 0.9f), RoundedCornerShape(2.dp))) {
                    for (r in 0 until rows) Row {
                        for (col in 0 until cols) {
                            Box(Modifier.width(cellW).heightIn(min = 44.dp).border(0.3.dp, c.separator).padding(horizontal = 8.dp, vertical = 10.dp)) {
                                BasicTextField(
                                    value = cells[r][col],
                                    onValueChange = { v -> edit { it[r][col] = v } },
                                    readOnly = readOnly,
                                    textStyle = NotesText.body.copy(color = c.text, fontWeight = if (r == 0) FontWeight.SemiBold else FontWeight.Normal),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                                    modifier = Modifier.fillMaxWidth().then(if (r == 0 && col == 0) Modifier.focusRequester(firstCell) else Modifier).onFocusChanged { st -> val f = st.isFocused; if (f) { focus = r to col; onFocus() } else if (focus == r to col) focus = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val fr = focus
    NotesMenu(rowMenu && fr != null, listOf(
        MenuItem("Zeile darüber einfügen") { fr?.let { (r, _) -> edit { it.add(r, MutableList(cols) { "" }) } } },
        MenuItem("Zeile darunter einfügen") { fr?.let { (r, _) -> edit { it.add(r + 1, MutableList(cols) { "" }) } } },
        MenuItem("Zeile löschen", destructive = true) { fr?.let { (r, _) -> if (rows <= 1) onDelete() else edit { it.removeAt(r) } } },
        MenuItem("Tabelle löschen", Icons.Outlined.Delete, destructive = true) { onDelete() }
    ), onDismiss = { rowMenu = false }, alignTop = false)
    NotesMenu(colMenu && fr != null, listOf(
        MenuItem("Spalte links einfügen") { fr?.let { (_, col) -> edit { m -> m.forEach { it.add(col, "") } } } },
        MenuItem("Spalte rechts einfügen") { fr?.let { (_, col) -> edit { m -> m.forEach { it.add(col + 1, "") } } } },
        MenuItem("Spalte löschen", destructive = true) { fr?.let { (_, col) -> if (cols <= 1) onDelete() else edit { m -> m.forEach { it.removeAt(col) } } } },
        MenuItem("Tabelle löschen", Icons.Outlined.Delete, destructive = true) { onDelete() }
    ), onDismiss = { colMenu = false }, alignTop = false)
}

/* -------------------------------------------------------------------------- */
/*                             Pictures and drawings                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun PictureBlock(file: String, onOpen: () -> Unit, onDelete: () -> Unit, readOnly: Boolean, drawingBg: Boolean = false) {
    val c = LocalNotesColors.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val bmp by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { loadBitmap(NoteStore.get(context).mediaFile(file).path, 1600) }
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        val ratio = bmp?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1.4f
        Box(
            Modifier.fillMaxWidth().aspectRatio(ratio.coerceIn(0.4f, 3f)).clip(RoundedCornerShape(10.dp))
                .background(if (drawingBg) Color.Transparent else c.elevated)
                .then(if (drawingBg) Modifier.border(0.5.dp, c.separator, RoundedCornerShape(10.dp)) else Modifier)
                .combinedClickable(onClick = onOpen, onLongClick = { if (!readOnly) menu = true })
        ) { bmp?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) } }
    }
    NotesMenu(menu, listOf(MenuItem("Löschen", Icons.Outlined.Delete, destructive = true, onClick = onDelete)), onDismiss = { menu = false }, alignTop = false)
}

@Composable
private fun PictureViewer(file: String, onClose: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onClose)
    val bmp by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { loadBitmap(NoteStore.get(context).mediaFile(file).path, 2600) }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).clickable(remember { MutableInteractionSource() }, null, onClick = onClose)) {
        bmp?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(16.dp)) {
            GlassCircle(Icons.Rounded.Close, onClick = onClose)
        }
    }
}
