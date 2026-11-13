package com.tms.os.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
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
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.ContactItem
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosMaterial
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.wallpaperGlass
import kotlinx.coroutines.delay

private val HeaderColor = Color.White.copy(alpha = 0.72f)
private val Hairline = Color.White.copy(alpha = 0.13f)
private val Side = 14.dp
private val RowIcon = 63.dp

/**
 * Spotlight. Everything sits on the darkened wallpaper; the search field is at
 * the bottom, right above the keyboard.
 *  - empty query: "Suggestions" card (four apps) and the search history chips;
 *  - typing: app rows, contacts with message/call buttons and the
 *    "Search in Apps" actions (web, store, maps).
 * [progress] (0..1) follows the pull-down finger.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun IosSpotlight(
    visible: Boolean,
    progress: () -> Float,
    query: String,
    results: List<AppInfo>,
    contacts: List<ContactItem>,
    mathResult: String?,
    history: List<AppInfo>,
    settings: LauncherSettings,
    strings: IosStrings,
    hs: HomeStrings,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onClearHistory: () -> Unit,
    onContactCall: (ContactItem) -> Unit,
    onContactMessage: (ContactItem) -> Unit,
    onWebSearch: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val shown by remember { derivedStateOf { visible || progress() > 0.002f } }
    if (!shown) return

    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        if (visible && settings.autoOpenKeyboardInSpotlight) {
            delay(60); runCatching { focus.requestFocus() }; keyboard?.show()
        } else if (!visible) { focusManager.clearFocus(); keyboard?.hide() }
    }
    if (visible) BackHandler(onBack = onClose)

    // The result list is a scroll container and would swallow the swipe. Whatever
    // upward drag it cannot use (list at its end, or nothing to scroll) closes Spotlight.
    val closeNow by rememberUpdatedState(onClose)
    val dismissPx = with(LocalDensity.current) { 52.dp.toPx() }
    val swipeUpToClose = remember(dismissPx) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f) {
                    pulled -= available.y
                    if (pulled > dismissPx) { pulled = 0f; closeNow() }
                    return available
                }
                pulled = 0f
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity { pulled = 0f; return Velocity.Zero }
        }
    }

    val q = query.trim()
    val top = results.firstOrNull()
    var moreSuggestions by remember { mutableStateOf(false) }
    var moreApps by remember(q) { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
            // Tap on empty space or swipe up: back to the home screen.
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -60.dp.toPx()) onClose() }
                )
            }
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
            // ------------------------------------------------------------ content
            Column(
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - progress().coerceIn(0f, 1f)) * -40.dp.toPx() }
                    .nestedScroll(swipeUpToClose)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 62.dp, bottom = 12.dp)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
            ) {
                // Suggestions and results cross-fade instead of cutting.
                AnimatedContent(
                    targetState = q.isEmpty(),
                    transitionSpec = { fadeIn(tween(200, delayMillis = 40)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false) },
                    label = "spotlightContent"
                ) { empty ->
                Column {
                if (empty) {
                    Header(strings.suggestions, if (results.size > 4) (if (moreSuggestions) hs.showLess else hs.showMore) else null, line = false) { moreSuggestions = !moreSuggestions }
                    Spacer(Modifier.height(10.dp))
                    SuggestionsCard(results.take(if (moreSuggestions) 8 else 4), onAppClick, onAppLongClick)

                    if (history.isNotEmpty()) {
                        Spacer(Modifier.height(30.dp))
                        Header(hs.history, hs.clear, onAction = onClearHistory)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(13.dp),
                            modifier = Modifier.padding(start = Side, end = Side, top = 14.dp)
                        ) {
                            history.forEach { app -> HistoryChip(app) { onAppClick(app) } }
                        }
                    }
                } else {
                    if (mathResult != null) {
                        // Tap the result to copy it; the expression is shown the way it was understood.
                        val calc = remember(q) { com.tms.os.launcher.domain.SpotlightCalculator.evaluate(q) }
                        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                        val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
                        var copied by remember(q) { mutableStateOf(false) }
                        LaunchedEffect(copied) { if (copied) { delay(1400); copied = false } }
                        Header(strings.calculator)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(calc?.plain ?: mathResult))
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    copied = true
                                }
                                .padding(horizontal = Side)
                                .height(91.dp)
                        ) {
                            Box(Modifier.size(RowIcon).clip(IosSquircle).background(Color(0xFF2C2C2E)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Calculate, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(34.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${calc?.expression ?: q} =", style = IosText.footnote.copy(color = HeaderColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(mathResult, style = IosText.title2.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                if (copied) hs.copied else hs.copy,
                                style = IosText.footnote.copy(color = if (copied) Color(0xFF30D158) else Color(0xFF0A84FF), fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (results.isNotEmpty()) {
                        Header(strings.apps, if (results.size > 3) (if (moreApps) hs.showLess else hs.showMore) else null) { moreApps = !moreApps }
                        val shownApps = results.take(if (moreApps) 12 else 3)
                        shownApps.forEachIndexed { i, app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(91.dp)
                                    .combinedClickable(remember { MutableInteractionSource() }, null, onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) })
                                    .padding(start = Side)
                            ) {
                                AppIconImage(app, RowIcon)
                                Spacer(Modifier.width(12.dp))
                                Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
                                    Text(app.displayLabel, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterStart))
                                    if (i != shownApps.lastIndex) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (contacts.isNotEmpty()) {
                        Header(strings.contacts)
                        contacts.take(3).forEachIndexed { i, c ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(91.dp).clickable(remember { MutableInteractionSource() }, null) { onContactCall(c) }.padding(start = Side)
                            ) {
                                Box(
                                    Modifier.size(RowIcon).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFA5A5AA), Color(0xFF7C7C81)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(c.name.trim().take(1).uppercase(), style = IosText.title1.copy(color = Color.White, fontWeight = FontWeight.Medium, fontSize = 28.sp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterStart)) {
                                        Column(Modifier.weight(1f)) {
                                            Text(c.name, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(c.phoneNumber, style = IosText.footnote.copy(color = Color.White.copy(alpha = 0.5f)), maxLines = 1)
                                        }
                                        RoundButton(Icons.Rounded.ChatBubble) { onContactMessage(c) }
                                        Spacer(Modifier.width(10.dp))
                                        RoundButton(Icons.Rounded.Call) { onContactCall(c) }
                                    }
                                    if (i != minOf(contacts.size, 3) - 1) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (settings.showWebSearchInSpotlight) {
                        Header(hs.searchInApps)
                        ActionRow(hs.searchWeb, Icons.Rounded.Explore, Color.White, Color(0xFF0A84FF), divider = true) { onWebSearch(q, "google") }
                        ActionRow(hs.searchInApps, Icons.Rounded.Widgets, Color(0xFF0A84FF), Color.White, divider = true) { onWebSearch(q, "play") }
                        ActionRow(hs.searchMaps, Icons.Rounded.NearMe, Color(0xFF34C759), Color.White, divider = false) { onWebSearch(q, "maps") }
                    }
                }
                }
                }
}

            // --------------------------------------------------------- search field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer { translationY = (1f - progress().coerceIn(0f, 1f)) * 90.dp.toPx() }
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 620.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .wallpaperGlass(CircleShape, tint = IosMaterial.field, rim = 0f)
                    .clickable(remember { MutableInteractionSource() }, null) { runCatching { focus.requestFocus() }; keyboard?.show() }
                    .padding(start = 14.dp, end = 12.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        if (query.isEmpty()) Text(strings.searchPlaceholder, style = IosText.body.copy(color = Color.White.copy(alpha = 0.55f)), maxLines = 1)
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = IosText.body.copy(color = Color.White),
                            cursorBrush = SolidColor(Color(0xFF0A84FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (top != null) onAppClick(top) else if (q.isNotEmpty()) onWebSearch(q, "google")
                            }),
                            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 2.dp).focusRequester(focus)
                        )
                    }
                    // Completion chip: the app that Return will open.
                    if (top != null && q.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            top.displayLabel,
                            style = IosText.body.copy(color = Color.White.copy(alpha = 0.9f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { onAppClick(top) }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                if (query.isNotEmpty()) {
                    Icon(Icons.Outlined.Cancel, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(21.dp).clickable(remember { MutableInteractionSource() }, null) { onQueryChange("") })
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, action: String? = null, line: Boolean = true, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 22.dp, end = 22.dp)) {
            Text(title, style = IosText.subheadline.copy(color = HeaderColor, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (action != null) Text(
                action,
                style = IosText.subheadline.copy(color = HeaderColor, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onAction)
            )
        }
        if (line) {
            Spacer(Modifier.height(9.dp))
            Box(Modifier.padding(horizontal = Side).fillMaxWidth().height(0.5.dp).background(Hairline))
        }
    }
}

@Composable
private fun AppIconImage(app: AppInfo, size: Dp) {
    Box(Modifier.size(size)) {
        app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionsCard(apps: List<AppInfo>, onClick: (AppInfo) -> Unit, onLongClick: (AppInfo) -> Unit) {
    if (apps.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(horizontal = Side)
            .fillMaxWidth()
            .wallpaperGlass(RoundedCornerShape(20.dp), tint = IosMaterial.card, rim = 0.08f)
            .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 380f))
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(vertical = 12.dp)
    ) {
        apps.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 4) {
                    val app = row.getOrNull(i)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).then(
                            if (app != null) Modifier.combinedClickable(remember { MutableInteractionSource() }, null, onClick = { onClick(app) }, onLongClick = { onLongClick(app) }) else Modifier
                        )
                    ) {
                        if (app != null) {
                            AppIconImage(app, RowIcon)
                            Spacer(Modifier.height(5.dp))
                            Text(app.displayLabel, style = IosText.iconLabel.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(app: AppInfo, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(27.dp)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp)
    ) {
        Box(Modifier.size(19.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
            app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(6.dp))
        Text(app.displayLabel, style = IosText.footnote.copy(color = Color.White), maxLines = 1)
    }
}

@Composable
private fun ActionRow(text: String, icon: ImageVector, tile: Color, glyph: Color, divider: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(start = Side)
    ) {
        Box(Modifier.size(29.dp).clip(RoundedCornerShape(7.dp)).background(tile), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = glyph, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
            Text(text, style = IosText.body.copy(color = Color.White), maxLines = 1, modifier = Modifier.align(Alignment.CenterStart))
            if (divider) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2C2C2E)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
}
