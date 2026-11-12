package com.tms.os.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppQuickAction
import com.tms.os.launcher.model.ContactItem
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.wallpaperGlass
import kotlinx.coroutines.delay

private val Secondary = Color.White.copy(alpha = 0.6f)
private val Hairline = Color.White.copy(alpha = 0.14f)

/**
 * iOS 26 Spotlight: results on top, the glass search field at the bottom right
 * above the keyboard. Typing shows a Top Hit card (with the app's real
 * shortcuts), suggestion rows with ↗, more apps and contacts. With an empty
 * query it shows Siri Suggestions. [progress] follows the pull-down finger.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosSpotlight(
    visible: Boolean,
    progress: () -> Float,
    query: String,
    results: List<AppInfo>,
    contacts: List<ContactItem>,
    mathResult: String?,
    topActions: List<AppQuickAction>,
    settings: LauncherSettings,
    strings: IosStrings,
    hs: HomeStrings,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onQuickAction: (AppQuickAction) -> Unit,
    onContactClick: (ContactItem) -> Unit,
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

    val top = results.firstOrNull()
    val q = query.trim()

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
            // Swipe up anywhere (outside the scrolling results) to close, as on iOS.
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
            // ------------------------------------------------------------ results
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - progress().coerceIn(0f, 1f)) * -48.dp.toPx() }
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 62.dp, bottom = 12.dp)
                    // Empty space: close, like tapping outside the results on iOS.
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
            ) {
                if (q.isEmpty()) {
                    SectionHeader(strings.siriSuggestions)
                    AppGridCard(results.take(8), onAppClick, onAppLongClick)
                } else {
                    if (mathResult != null) {
                        SectionHeader(strings.calculator)
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                                Box(Modifier.size(44.dp).clip(IosSquircle).background(Color(0xFF333336)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Calculate, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("$q =", style = IosText.footnote.copy(color = Secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(mathResult, style = IosText.title2.copy(color = Color.White))
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    // Top Hit: app (with its own shortcuts) or a contact.
                    val topContact = contacts.firstOrNull().takeIf { top == null }
                    if (top != null || topContact != null) {
                        SectionHeader(hs.topHit)
                        GlassCard {
                            if (top != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(onClick = { onAppClick(top) }, onLongClick = { onAppLongClick(top) })
                                        .padding(14.dp)
                                ) {
                                    AppIconImage(top, 56)
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(top.displayLabel, style = IosText.headline.copy(color = Color.White, fontSize = 19.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(top.category.localizedTitle(settings.appLanguage), style = IosText.footnote.copy(color = Secondary), maxLines = 1)
                                    }
                                }
                                topActions.forEach { a ->
                                    Box(Modifier.padding(start = 84.dp).fillMaxWidth().height(0.5.dp).background(Hairline))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { onQuickAction(a) }.padding(horizontal = 14.dp, vertical = 11.dp)
                                    ) {
                                        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                                            a.icon?.let { Image(it, null, modifier = Modifier.size(24.dp)) }
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Text(a.label, style = IosText.body.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            } else if (topContact != null) {
                                ContactRow(topContact, big = true, onClick = onContactClick)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    // Suggestions: the query in the web, YouTube and the store.
                    if (settings.showWebSearchInSpotlight) {
                        SectionHeader(strings.suggestions)
                        SuggestionRow(q, "Google", Icons.Rounded.Explore, Color(0xFF0A84FF)) { onWebSearch(q, "google") }
                        SuggestionRow(q, "YouTube", Icons.Rounded.SmartDisplay, Color(0xFFFF453A)) { onWebSearch(q, "youtube") }
                        SuggestionRow(q, "Play Store", Icons.Rounded.Shop, Color(0xFF30D158)) { onWebSearch(q, "play") }
                        Spacer(Modifier.height(18.dp))
                    }

                    val moreApps = results.drop(1)
                    if (moreApps.isNotEmpty()) {
                        SectionHeader(strings.apps)
                        AppGridCard(moreApps.take(8), onAppClick, onAppLongClick)
                        Spacer(Modifier.height(18.dp))
                    }

                    val restContacts = if (top == null) contacts.drop(1) else contacts
                    if (restContacts.isNotEmpty()) {
                        SectionHeader(strings.contacts, trailing = hs.searchInApp)
                        restContacts.take(4).forEachIndexed { i, c ->
                            if (i > 0) Box(Modifier.padding(start = 52.dp).fillMaxWidth().height(0.5.dp).background(Hairline))
                            ContactRow(c, big = false, onClick = onContactClick)
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    if (top == null && contacts.isEmpty() && mathResult == null) {
                        Text(strings.noResults, style = IosText.headline.copy(color = Color.White), modifier = Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally))
                    }
                }
            }

            // --------------------------------------------------------- search field
            val completion = top?.displayLabel?.takeIf { q.isNotEmpty() && it.startsWith(q, ignoreCase = true) && it.length > q.length }?.substring(q.length)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer { translationY = (1f - progress().coerceIn(0f, 1f)) * 80.dp.toPx() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .wallpaperGlass(CircleShape, tint = Color.White.copy(alpha = 0.14f), rim = 0.5f)
                    .clickable(remember { MutableInteractionSource() }, null) { runCatching { focus.requestFocus() }; keyboard?.show() }
                    .padding(start = 16.dp, end = 10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        if (query.isEmpty()) Text(strings.searchPlaceholder, style = IosText.body.copy(color = Color.White.copy(alpha = 0.5f)), maxLines = 1)
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = IosText.body.copy(color = Color.White),
                            cursorBrush = SolidColor(Color(0xFF0A84FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (top != null) onAppClick(top) else if (q.isNotEmpty()) onWebSearch(q, "google")
                            }),
                            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 2.dp).focusRequester(focus)
                        )
                    }
                    // Inline completion like iOS: "go|ogle — Öffnen".
                    if (top != null && q.isNotEmpty()) {
                        Text(
                            (completion ?: "") + " — " + hs.open,
                            style = IosText.body.copy(color = Color.White.copy(alpha = 0.45f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 2.dp)
                        )
                    }
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Cancel, null, tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp).clickable { onQueryChange("") }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = IosText.subheadline.copy(color = Color.White, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = IosText.subheadline.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold))
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Hairline))
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().wallpaperGlass(RoundedCornerShape(28.dp), tint = Color.White.copy(alpha = 0.10f), rim = 0.4f)) { content() }
}

@Composable
private fun AppIconImage(app: AppInfo, size: Int) {
    Box(Modifier.size(size.dp).clip(IosSquircle).background(Color.White.copy(alpha = 0.1f))) {
        app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridCard(apps: List<AppInfo>, onClick: (AppInfo) -> Unit, onLongClick: (AppInfo) -> Unit) {
    if (apps.isEmpty()) return
    GlassCard {
        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                AppIconImage(app, 58)
                                Spacer(Modifier.height(6.dp))
                                Text(app.displayLabel, style = IosText.iconLabel.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(text: String, source: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp)
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = IosText.body.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("  $source", style = IosText.footnote.copy(color = Secondary), maxLines = 1, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.CallMade, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ContactRow(c: ContactItem, big: Boolean, onClick: (ContactItem) -> Unit) {
    val avatar = if (big) 56.dp else 38.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onClick(c) }.padding(horizontal = if (big) 14.dp else 0.dp, vertical = if (big) 14.dp else 9.dp)
    ) {
        Box(Modifier.size(avatar).clip(CircleShape).background(Color(0xFF8E8E93)), contentAlignment = Alignment.Center) {
            Text(c.name.split(' ').mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""), style = IosText.headline.copy(color = Color.White, fontSize = if (big) 22.sp else 15.sp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, style = IosText.headline.copy(color = Color.White, fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!big) Text(c.phoneNumber, style = IosText.footnote.copy(color = Secondary), maxLines = 1)
        }
        if (big) {
            RoundGlassButton(Icons.Rounded.ChatBubble) { onClick(c) }
            Spacer(Modifier.width(10.dp))
        }
        RoundGlassButton(Icons.Rounded.Call) { onClick(c) }
    }
}

@Composable
private fun RoundGlassButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
}

