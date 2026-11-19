package com.tms.os.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* -------------------------------------------------------------------------- */
/*                     iOS edit menu (text selection menu)                    */
/* -------------------------------------------------------------------------- */

/** One entry of the edit menu. [submenu] opens a second page ("Format ›"). */
data class EditMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val submenu: List<EditMenuItem>? = null,
    val onClick: () -> Unit = {}
)

/**
 * What the menu shows and where: [anchor] is the selection (or cursor) in root
 * pixels; [compact] are the words in the capsule ("Ausschneiden | Kopieren"),
 * [topRow] the three icon buttons and [rows] the list of the expanded card
 * behind the "›" button.
 */
data class EditMenuSpec(
    val anchor: Rect,
    val compact: List<EditMenuItem>,
    val topRow: List<EditMenuItem> = emptyList(),
    val rows: List<EditMenuItem> = emptyList()
)

private val Destructive = Color(0xFFFF453A)

/**
 * Draws the iOS 26 edit menu over the screen: a glass capsule above the
 * selection (below it when there is no room), "›" opens the card with icon
 * buttons and rows. Touches outside pass through to the text.
 */
@Composable
fun IosEditMenuHost(spec: EditMenuSpec?, onDismiss: () -> Unit, dark: Boolean = isSystemInDarkTheme()) {
    if (spec == null) return
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val imeInset = WindowInsets.ime.getBottom(density)
    // A new selection starts with the capsule again.
    var expanded by remember(spec.compact.map { it.label }, spec.anchor.top.toInt() / 200) { mutableStateOf(false) }
    var page by remember(expanded) { mutableStateOf<EditMenuItem?>(null) }
    val glass = if (dark) Color(0xF52C2C2E) else Color(0xF7FFFFFF)
    val fg = if (dark) Color.White else Color.Black
    val hair = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)

    Layout(
        content = {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { (fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.82f, stiffness = 600f), initialScale = 0.9f)) togetherWith fadeOut(tween(90)) },
                label = "editMenu"
            ) { open ->
                val run: (EditMenuItem) -> Unit = { item ->
                    if (item.submenu != null) { expanded = true; page = item } else { item.onClick(); onDismiss() }
                }
                if (!open) Capsule(spec, glass, fg, hair, onItem = run, onMore = { expanded = true })
                else Card(spec, page, glass, fg, hair, onItem = run, onBack = { page = null })
            }
        }
    ) { measurables, c ->
        val margin = with(density) { 12.dp.roundToPx() }
        val gap = with(density) { 10.dp.roundToPx() }
        val handleRoom = with(density) { 26.dp.roundToPx() }
        val p = measurables.first().measure(Constraints(maxWidth = (c.maxWidth - 2 * margin).coerceAtLeast(0), maxHeight = c.maxHeight))
        val x = (spec.anchor.center.x.toInt() - p.width / 2).coerceIn(margin, (c.maxWidth - margin - p.width).coerceAtLeast(margin))
        val above = spec.anchor.top.toInt() - gap - p.height
        val bottomLimit = c.maxHeight - imeInset - margin
        val y = when {
            above >= topInset + margin -> above
            spec.anchor.bottom.toInt() + handleRoom + p.height <= bottomLimit -> spec.anchor.bottom.toInt() + handleRoom
            else -> (bottomLimit - p.height).coerceAtLeast(topInset + margin)
        }
        layout(c.maxWidth, c.maxHeight) { p.place(x, y) }
    }
}

@Composable
private fun Capsule(spec: EditMenuSpec, glass: Color, fg: Color, hair: Color, onItem: (EditMenuItem) -> Unit, onMore: () -> Unit) {
    val more = spec.topRow.isNotEmpty() || spec.rows.isNotEmpty()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(18.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(glass)
            .border(0.6.dp, hair, CircleShape)
            .height(44.dp)
            .padding(start = 4.dp, end = if (more) 4.dp else 4.dp)
    ) {
        spec.compact.forEachIndexed { i, item ->
            if (i > 0) Box(Modifier.width(0.8.dp).height(20.dp).background(hair))
            Box(
                Modifier.height(44.dp).clickable { onItem(item) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(item.label, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (item.destructive) Destructive else fg), maxLines = 1)
            }
        }
        if (more) {
            Spacer(Modifier.width(2.dp))
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(fg.copy(alpha = 0.1f)).clickable(onClick = onMore),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun Card(spec: EditMenuSpec, page: EditMenuItem?, glass: Color, fg: Color, hair: Color, onItem: (EditMenuItem) -> Unit, onBack: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .width(260.dp)
            .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(glass)
            .border(0.6.dp, hair, shape)
            .heightIn(max = 330.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp)
    ) {
        if (page != null) {
            // Second page, e.g. "Format": back row, then its entries.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onBack).padding(horizontal = 14.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = fg, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(page.label, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg))
            }
            Box(Modifier.fillMaxWidth().height(0.6.dp).background(hair))
            page.submenu.orEmpty().forEachIndexed { i, item ->
                MenuRow(item, fg, onItem)
                if (i != page.submenu.orEmpty().lastIndex) Box(Modifier.padding(start = 50.dp).fillMaxWidth().height(0.6.dp).background(hair))
            }
            return@Column
        }
        if (spec.topRow.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                spec.topRow.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onItem(item) }.padding(vertical = 10.dp)
                    ) {
                        item.icon?.let { Icon(it, null, tint = if (item.destructive) Destructive else fg, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.height(5.dp))
                        Text(item.label, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (item.destructive) Destructive else fg, textAlign = TextAlign.Center), maxLines = 2)
                    }
                }
            }
            if (spec.rows.isNotEmpty()) Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(0.6.dp).background(hair))
        }
        spec.rows.forEachIndexed { i, item ->
            MenuRow(item, fg, onItem)
            if (i != spec.rows.lastIndex) Box(Modifier.padding(start = 50.dp).fillMaxWidth().height(0.6.dp).background(hair))
        }
    }
}

@Composable
private fun MenuRow(item: EditMenuItem, fg: Color, onItem: (EditMenuItem) -> Unit) {
    val color = if (item.destructive) Destructive else fg
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onItem(item) }.padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
            item.icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Text(item.label, style = TextStyle(fontSize = 16.sp, color = color), modifier = Modifier.weight(1f))
        if (item.submenu != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

/* -------------------------------------------------------------------------- */
/*            The same menu for every Compose text field of TMS OS            */
/* -------------------------------------------------------------------------- */

/**
 * Replaces Android's text toolbar for Compose text fields (Spotlight, search
 * fields, settings, dialogs) with the iOS edit menu.
 */
class IosTextToolbar : TextToolbar {
    var spec by mutableStateOf<EditMenuSpec?>(null)
        private set
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        val cut = onCutRequested?.let { EditMenuItem("Ausschneiden", Icons.Outlined.ContentCut, onClick = it) }
        val copy = onCopyRequested?.let { EditMenuItem("Kopieren", Icons.Outlined.ContentCopy, onClick = it) }
        val paste = onPasteRequested?.let { EditMenuItem("Einsetzen", Icons.Outlined.ContentPaste, onClick = it) }
        val all = onSelectAllRequested?.let { EditMenuItem("Alles auswählen", Icons.Outlined.SelectAll, onClick = it) }
        val selected = cut != null || copy != null
        val compact = if (selected) listOfNotNull(cut, copy, paste) else listOfNotNull(all, paste)
        if (compact.isEmpty() && all == null) { hide(); return }
        spec = EditMenuSpec(
            anchor = rect,
            compact = compact.ifEmpty { listOfNotNull(all) },
            topRow = if (selected) listOfNotNull(cut, copy, paste) else emptyList(),
            rows = if (selected) listOfNotNull(all) else emptyList()
        )
        status = TextToolbarStatus.Shown
    }

    override fun hide() {
        spec = null
        status = TextToolbarStatus.Hidden
    }
}

/**
 * Wraps a screen so all its Compose text fields use the iOS edit menu and
 * the iOS selection colours ([accent]: blue in TMS OS, yellow in Notes).
 */
@Composable
fun IosTextEditing(accent: Color = Color(0xFF0A84FF), dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val toolbar = remember { IosTextToolbar() }
    val colors = remember(accent) { TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(alpha = 0.32f)) }
    CompositionLocalProvider(LocalTextToolbar provides toolbar, LocalTextSelectionColors provides colors) {
        Box {
            content()
            IosEditMenuHost(toolbar.spec, onDismiss = { toolbar.hide() }, dark = dark)
        }
    }
}
