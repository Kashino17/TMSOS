@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.tms.os.launcher.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/* -------------------------------------------------------------------------- */
/*                                   Icons                                    */
/* -------------------------------------------------------------------------- */

/** SF "square.and.pencil": the compose button of Notes. */
val ComposeIcon: ImageVector by lazy {
    ImageVector.Builder("compose", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(11f, 4.5f); lineTo(6.5f, 4.5f)
            curveTo(5.1f, 4.5f, 4f, 5.6f, 4f, 7f); lineTo(4f, 17.5f)
            curveTo(4f, 18.9f, 5.1f, 20f, 6.5f, 20f); lineTo(17f, 20f)
            curveTo(18.4f, 20f, 19.5f, 18.9f, 19.5f, 17.5f); lineTo(19.5f, 13f)
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(18.2f, 3.6f); lineTo(20.4f, 5.8f); lineTo(12.3f, 13.9f); lineTo(9.3f, 14.7f); lineTo(10.1f, 11.7f); close()
        }
    }.build()
}

/* -------------------------------------------------------------------------- */
/*                               Bottom search bar                            */
/* -------------------------------------------------------------------------- */

/** "Suchen" capsule with microphone, and the compose button next to it. */
@Composable
fun NotesBottomBar(
    query: String,
    onQuery: (String) -> Unit,
    onCompose: (() -> Unit)?,
    onDictate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalNotesColors.current
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).height(48.dp).notesGlass(c).padding(start = 14.dp, end = 12.dp)
        ) {
            Icon(Icons.Rounded.Search, null, tint = c.secondary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Suchen", style = NotesText.body.copy(color = c.secondary))
                BasicTextField(
                    value = query, onValueChange = onQuery, singleLine = true,
                    textStyle = NotesText.body.copy(color = c.text),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) Icon(Icons.Rounded.Cancel, null, tint = c.secondary, modifier = Modifier.size(20.dp).clickable { onQuery("") })
            else Icon(Icons.Rounded.Mic, null, tint = c.secondary, modifier = Modifier.size(22.dp).clickable(onClick = onDictate))
        }
        if (onCompose != null) Box(Modifier.size(48.dp).pressable(onCompose).notesGlass(c), contentAlignment = Alignment.Center) {
            Icon(ComposeIcon, null, tint = c.text, modifier = Modifier.size(25.dp))
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                               Swipe actions                                */
/* -------------------------------------------------------------------------- */

data class SwipeAction(val icon: ImageVector, val color: Color, val label: String, val onClick: () -> Unit)

/**
 * A list row with iOS swipe actions: swipe left for [trailing] (the first one,
 * e.g. delete, runs on a full swipe), right for [leading] (pin).
 * Only one row is open at a time: [openId]/[onOpen] belong to the list.
 */
@Composable
fun SwipeRow(
    id: String,
    openId: String?,
    onOpen: (String?) -> Unit,
    leading: List<SwipeAction>,
    trailing: List<SwipeAction>,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val btn = with(density) { 74.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableStateOf(1f) }
    val maxLeft = -btn * trailing.size
    val maxRight = btn * leading.size
    LaunchedEffect(openId) { if (openId != id && offset.value != 0f) offset.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 500f)) }

    fun settle(velocity: Float) = scope.launch {
        val v = offset.value
        val target = when {
            trailing.isNotEmpty() && v < -width * 0.55f -> { trailing.first().onClick(); 0f }
            leading.isNotEmpty() && v > width * 0.55f -> { leading.first().onClick(); 0f }
            v < maxLeft / 2 || velocity < -900f -> if (trailing.isEmpty()) 0f else maxLeft
            v > maxRight / 2 || velocity > 900f -> if (leading.isEmpty()) 0f else maxRight
            else -> 0f
        }
        onOpen(if (target != 0f) id else null)
        offset.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 420f))
    }

    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min).onSizeChanged { width = it.width.toFloat() }) {
        // Buttons behind the row; the first trailing one grows with a long swipe.
        if (trailing.isNotEmpty()) Row(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
            trailing.asReversed().forEach { a ->
                val w = with(density) { (if (a == trailing.first()) maxOf(btn, -offset.value - btn * (trailing.size - 1)) else btn).toDp() }
                ActionButton(a, Modifier.width(w).fillMaxHeight()) { scope.launch { offset.animateTo(0f) }; onOpen(null); a.onClick() }
            }
        }
        if (leading.isNotEmpty()) Row(Modifier.align(Alignment.CenterStart).fillMaxHeight()) {
            leading.forEach { a ->
                val w = with(density) { maxOf(btn, offset.value).toDp() }
                ActionButton(a, Modifier.width(w).fillMaxHeight()) { scope.launch { offset.animateTo(0f) }; onOpen(null); a.onClick() }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { d ->
                        scope.launch { offset.snapTo((offset.value + d).coerceIn(if (trailing.isEmpty()) 0f else -width, if (leading.isEmpty()) 0f else width)) }
                    },
                    onDragStarted = { onOpen(id) },
                    onDragStopped = { v -> settle(v) }
                )
        ) { content() }
    }
}

@Composable
private fun ActionButton(a: SwipeAction, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.background(a.color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(a.icon, a.label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/* -------------------------------------------------------------------------- */
/*                              Menus and dialogs                             */
/* -------------------------------------------------------------------------- */

data class MenuItem(val label: String, val icon: ImageVector? = null, val destructive: Boolean = false, val onClick: () -> Unit)

/** An iOS context menu: a glass card listing actions, over a dimmed screen. */
@Composable
fun NotesMenu(visible: Boolean, items: List<MenuItem>, onDismiss: () -> Unit, alignTop: Boolean = true, alignStart: Boolean = false) {
    val c = LocalNotesColors.current
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (c.dark) 0.35f else 0.15f)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    }
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 500f), initialScale = 0.85f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (alignStart) 0f else 1f, if (alignTop) 0f else 1f)),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (alignStart) 0f else 1f, if (alignTop) 0f else 1f))
    ) {
        // Bottom menus sit above the keyboard when it is open (and above the toolbar on it).
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)).padding(top = if (alignTop) 96.dp else 0.dp, bottom = if (alignTop) 0.dp else 76.dp, start = 16.dp, end = 16.dp)) {
            Column(
                Modifier
                    .align(when { alignTop && alignStart -> Alignment.TopStart; alignTop -> Alignment.TopEnd; alignStart -> Alignment.BottomStart; else -> Alignment.BottomEnd })
                    .widthIn(min = 230.dp, max = 290.dp)
                    .notesGlass(c, RoundedCornerShape(24.dp))
                    .padding(vertical = 6.dp)
            ) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onDismiss(); item.onClick() }.padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        item.icon?.let { Icon(it, null, tint = if (item.destructive) c.destructive else c.text, modifier = Modifier.size(21.dp)) }
                        if (item.icon != null) Spacer(Modifier.width(14.dp))
                        Text(item.label, style = NotesText.body.copy(color = if (item.destructive) c.destructive else c.text))
                    }
                }
            }
        }
    }
}

/** iOS alert: title, optional message and text field, and two buttons. */
@Composable
fun NotesAlert(
    title: String,
    message: String? = null,
    field: String? = null,
    placeholder: String = "",
    confirm: String,
    destructive: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalNotesColors.current
    var value by remember { mutableStateOf(field ?: "") }
    val focus = remember { FocusRequester() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) { if (field != null) runCatching { focus.requestFocus() } }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(290.dp).clip(RoundedCornerShape(26.dp)).background(c.elevated).padding(top = 20.dp)
        ) {
            Text(title, style = NotesText.headline.copy(color = c.text, textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = 18.dp))
            message?.let { Text(it, style = NotesText.footnote.copy(color = c.text, textAlign = TextAlign.Center), modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp)) }
            if (field != null) {
                Box(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.card).padding(horizontal = 10.dp, vertical = 9.dp)) {
                    if (value.isEmpty()) Text(placeholder, style = NotesText.body.copy(color = c.tertiary))
                    BasicTextField(
                        value = value, onValueChange = { value = it }, singleLine = true,
                        textStyle = NotesText.body.copy(color = c.text), cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onConfirm(value) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                }
            } else Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
            Row(Modifier.fillMaxWidth().height(48.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("Abbrechen", style = NotesText.body.copy(color = c.accent))
                }
                Box(Modifier.width(0.5.dp).fillMaxHeight().background(c.separator))
                Box(Modifier.weight(1f).fillMaxHeight().clickable(enabled = field == null || value.isNotBlank()) { onConfirm(value) }, contentAlignment = Alignment.Center) {
                    Text(confirm, style = NotesText.body.copy(color = if (destructive) c.destructive else c.accent.copy(alpha = if (field == null || value.isNotBlank()) 1f else 0.4f), fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

/**
 * An iOS sheet: as tall as its content (at most 88 % of the screen), docked to
 * the bottom with rounded corners and a grabber; title bar with "Abbrechen".
 */
@Composable
fun NotesSheet(visible: Boolean, title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalNotesColors.current
    if (visible) BackHandler(onBack = onDismiss)
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically(spring(dampingRatio = 0.92f, stiffness = 380f)) { it },
        exit = slideOutVertically(tween(220)) { it }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.88f)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(if (c.dark) c.card else c.background)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Box(Modifier.padding(top = 6.dp).align(Alignment.CenterHorizontally).size(36.dp, 5.dp).clip(CircleShape).background(c.tertiary))
                Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
                    Text("Abbrechen", style = NotesText.body.copy(color = c.accent), modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss))
                    Text(title, style = NotesText.headline.copy(color = c.text), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp))
                }
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) { content() }
            }
        }
    }
}

/**
 * "Notiz bewegen": a sheet with "Neuer Ordner" on top (creates the folder and
 * moves the notes into it) and the folders below, the current one ticked.
 */
@Composable
fun MoveNotesSheet(
    visible: Boolean,
    count: Int,
    folders: List<NoteFolder>,
    currentFolder: String?,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
    onCreateFolder: (String) -> String,
    title: String? = null
) {
    val c = LocalNotesColors.current
    var naming by remember { mutableStateOf(false) }
    NotesSheet(visible, title ?: if (count == 1) "Notiz bewegen" else "$count Notizen bewegen", onDismiss) {
        InsetGroup(color = if (c.dark) c.elevated else c.card) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(54.dp).clickable { naming = true }.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Outlined.CreateNewFolder, null, tint = c.accent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                Text("Neuer Ordner", style = NotesText.body.copy(color = c.accent))
            }
        }
        Text("Auf meinem Gerät", style = NotesText.headline.copy(color = c.text, fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp))
        InsetGroup(color = if (c.dark) c.elevated else c.card) {
            folders.forEachIndexed { i, f ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(54.dp).clickable { onMove(f.id) }.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Outlined.Folder, null, tint = c.folder, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(f.name, style = NotesText.body.copy(color = c.text), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (f.id == currentFolder) Icon(Icons.Rounded.Check, null, tint = c.accent, modifier = Modifier.size(20.dp))
                }
                if (i != folders.lastIndex) RowSeparator(56)
            }
        }
    }
    if (naming) NotesAlert("Neuer Ordner", "Gib einen Namen für diesen Ordner ein.", field = "", placeholder = "Name", confirm = "Sichern",
        onConfirm = { name -> naming = false; onMove(onCreateFolder(name)) }, onDismiss = { naming = false })
}

/** Rounded inset group of rows ("insetGrouped" table style). */
@Composable
fun InsetGroup(modifier: Modifier = Modifier, color: Color? = null, content: @Composable () -> Unit) {
    val c = LocalNotesColors.current
    Column(modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(color ?: c.card)) { content() }
}

@Composable
fun RowSeparator(start: Int = 16) {
    val c = LocalNotesColors.current
    Box(Modifier.padding(start = start.dp).fillMaxWidth().height(0.5.dp).background(c.separator))
}

@Composable
fun Circle(color: Color, size: Int, modifier: Modifier = Modifier) = Box(modifier.size(size.dp).clip(CircleShape).background(color))

/**
 * "Aus Samsung Notes importieren": Samsung Notes keeps its notes where no other
 * app can read them, so they come over as files (shared or saved from Samsung
 * Notes). The sheet explains the steps and offers to open Samsung Notes or to
 * pick saved files.
 */
@Composable
fun SamsungImportSheet(visible: Boolean, samsungInstalled: Boolean, onDismiss: () -> Unit, onOpenSamsung: () -> Unit, onPickFiles: () -> Unit) {
    val c = LocalNotesColors.current
    NotesSheet(visible, "Samsung Notes", onDismiss) {
        Text(
            "Samsung Notes gibt seine Notizen nicht direkt an andere Apps weiter. Sie kommen als Dateien herüber, ein Ordner nach dem anderen:",
            style = NotesText.subhead.copy(color = c.secondary),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
        )
        InsetGroup(color = if (c.dark) c.elevated else c.card) {
            listOf(
                "Öffne in Samsung Notes einen Ordner.",
                "Halte eine Notiz gedrückt und tippe oben auf „Alle“.",
                "Tippe auf „Teilen“ und wähle „Microsoft Word-Datei“ (mit Bildern und Formatierung) oder „Textdatei“.",
                "Wähle „Notizen“ (TMS OS) und dann den Ordner, in den die Notizen kommen."
            ).forEachIndexed { i, step ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = NotesText.caption.copy(color = c.onAccent))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(step, style = NotesText.body.copy(color = c.text), modifier = Modifier.weight(1f))
                }
                if (i < 3) RowSeparator(52)
            }
        }
        Text(
            "Hast du Notizen schon als Dateien gespeichert („Als Datei speichern“), wähle sie direkt aus. Word, Text, PDF und Bilder werden gelesen.",
            style = NotesText.footnote.copy(color = c.secondary),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (samsungInstalled) SheetButton("Samsung Notes öffnen", filled = true, onClick = onOpenSamsung)
            SheetButton("Dateien auswählen", filled = !samsungInstalled, onClick = onPickFiles)
        }
    }
}

@Composable
private fun SheetButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val c = LocalNotesColors.current
    Box(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp))
            .background(if (filled) c.accent else c.text.copy(alpha = 0.1f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, style = NotesText.headline.copy(color = if (filled) c.onAccent else c.accent)) }
}
