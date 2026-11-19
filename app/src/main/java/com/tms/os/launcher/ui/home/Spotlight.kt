package com.tms.os.launcher.ui.home

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.data.FileHit
import com.tms.os.launcher.data.PhotoHit
import com.tms.os.launcher.data.SettingsHit
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppQuickAction
import com.tms.os.launcher.model.ContactItem
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosStrings
import com.tms.os.launcher.ui.theme.IosText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val HeaderColor = Color.White.copy(alpha = 0.62f)
private val Secondary = Color.White.copy(alpha = 0.58f)
private val Hairline = Color.White.copy(alpha = 0.14f)
/** The light frosted cards (suggestions, top hit) on the blurred home screen. */
private val CardFill = Color.White.copy(alpha = 0.10f)
private val CardRim = Color.White.copy(alpha = 0.10f)
private val ButtonFill = Color.White.copy(alpha = 0.15f)
private val Side = 16.dp
private val RowIcon = 58.dp
private val SmallIcon = 30.dp

/** Everything Spotlight can find besides apps, plus how to open it. */
class SpotlightExtras(
    val topActions: List<AppQuickAction> = emptyList(),
    val suggestedActions: List<AppQuickAction> = emptyList(),
    val settings: List<SettingsHit> = emptyList(),
    val files: List<FileHit> = emptyList(),
    val photos: List<PhotoHit> = emptyList(),
    val mediaAccess: Boolean = true,
    val iconOf: (String) -> ImageBitmap? = { null },
    val thumbnail: (Uri, Int) -> ImageBitmap? = { _, _ -> null },
    val onQuickAction: (AppQuickAction) -> Unit = {},
    val onSetting: (SettingsHit) -> Unit = {},
    val onFile: (FileHit) -> Unit = {},
    val onPhoto: (PhotoHit) -> Unit = {},
    val onFilesApp: () -> Unit = {},
    val onGallery: () -> Unit = {},
    val onAllowMedia: () -> Unit = {}
)

/**
 * Spotlight, as on the iPhone: the home screen blurs behind it (drawn by the
 * home screen), the search field sits right above the keyboard and becomes a
 * bar at the bottom once the results are scrolled.
 *  - empty query: "Siri-Vorschläge" (eight apps) and shortcut suggestions;
 *  - typing: Toptreffer with its quick actions, web suggestion, settings,
 *    apps, contacts, files, photos and "In Apps suchen".
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
    settings: LauncherSettings,
    strings: IosStrings,
    hs: HomeStrings,
    extras: SpotlightExtras,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
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
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible && settings.autoOpenKeyboardInSpotlight) {
            delay(40); runCatching { focus.requestFocus() }; keyboard?.show()
        } else if (!visible) { focusManager.clearFocus(); keyboard?.hide() }
    }
    if (visible) BackHandler(onBack = onClose)

    val imeUp = WindowInsets.isImeVisible
    val dropKeyboard by rememberUpdatedState { if (focused) { focusManager.clearFocus(); keyboard?.hide() } }

    // Scrolling the results puts the keyboard away (the field drops to the bottom bar).
    // Whatever upward drag the list cannot use (at its end, or nothing to scroll) closes Spotlight.
    val closeNow by rememberUpdatedState(onClose)
    // Only the suggestions close when pushed past their end; results just stop, as on iOS.
    val closeOnOverscroll by rememberUpdatedState(query.isBlank())
    val dismissPx = with(LocalDensity.current) { 52.dp.toPx() }
    val scrollLink = remember(dismissPx) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 1f) dropKeyboard()
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !closeOnOverscroll) return Offset.Zero
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

    val context = LocalContext.current
    val dictate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onQueryChange)
    }
    val browserPkg = remember {
        runCatching {
            context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")), 0)?.activityInfo?.packageName
        }.getOrNull()
    }

    val q = query.trim()
    val top = results.firstOrNull()
    var fewerSuggestions by remember { mutableStateOf(false) }
    var moreResults by remember(q) { mutableStateOf(false) }
    var moreApps by remember(q) { mutableStateOf(false) }
    val p = { progress().coerceIn(0f, 1f) }

    Box(
        Modifier
            .fillMaxSize()
            // The blurred home screen lies underneath; this only darkens it a little more.
            .background(Color.Black.copy(alpha = 0.001f))
            .drawWithContent {
                drawRect(Color(0xFF8E8E93).copy(alpha = 0.10f * p()))
                drawContent()
            }
            // Tap on empty space or swipe up: back to the home screen. Only while it is open:
            // a half-pulled, nearly invisible Spotlight must never swallow the home screen's touches.
            .clickable(remember { MutableInteractionSource() }, null, enabled = visible, onClick = onClose)
            .pointerInput(visible) {
                if (!visible) return@pointerInput
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
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Results slide away under the status bar instead of being cut off.
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        // The results appear once the field has arrived, like on the iPhone.
                        alpha = ((p() - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationY = (1f - p()) * 24.dp.toPx()
                    }
                    .drawWithContent {
                        drawContent()
                        val fade = 64.dp.toPx()
                        drawRect(
                            Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, startY = 0f, endY = fade),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                Column(
                    Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 620.dp)
                        .fillMaxWidth()
                        .nestedScroll(scrollLink)
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 18.dp, bottom = 14.dp)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                ) {
                    AnimatedContent(
                        targetState = q.isEmpty(),
                        transitionSpec = { fadeIn(tween(180, delayMillis = 30)) togetherWith fadeOut(tween(110)) using SizeTransform(clip = false) },
                        label = "spotlightContent"
                    ) { empty ->
                        Column {
                            if (empty) {
                                Header(hs.siriSuggestions, if (results.size > 4) (if (fewerSuggestions) hs.spotMore else hs.spotLess) else null, line = false) { fewerSuggestions = !fewerSuggestions }
                                Spacer(Modifier.height(9.dp))
                                SuggestionsCard(results.take(if (fewerSuggestions) 4 else 8), onAppClick, onAppLongClick)
                                if (extras.suggestedActions.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    extras.suggestedActions.forEachIndexed { i, a ->
                                        SuggestedActionRow(a, extras.iconOf(a.packageName), divider = i != extras.suggestedActions.lastIndex) { extras.onQuickAction(a) }
                                    }
                                }
                            } else {
                                Results(
                                    q = q, top = top, results = results, contacts = contacts, mathResult = mathResult,
                                    settings = settings, strings = strings, hs = hs, extras = extras, browserPkg = browserPkg,
                                    moreResults = moreResults, onMoreResults = { moreResults = !moreResults },
                                    moreApps = moreApps || moreResults, onMoreApps = { moreApps = !moreApps },
                                    onAppClick = onAppClick, onAppLongClick = onAppLongClick,
                                    onContactCall = onContactCall, onContactMessage = onContactMessage, onWebSearch = onWebSearch
                                )
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------------- search field
            // Above the keyboard while typing; a bar at the very bottom once the keyboard is away.
            val idle = !imeUp && !focused && q.isNotEmpty()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer {
                        // Grows out of the "Suchen" pill above the dock.
                        val t = p()
                        alpha = (t * 2.2f).coerceAtMost(1f)
                        val s = 0.42f + 0.58f * t
                        scaleX = s
                        scaleY = 0.8f + 0.2f * t
                        translationY = (1f - t) * -28.dp.toPx()
                    }
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 620.dp)
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = if (imeUp) 8.dp else 6.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3D).copy(alpha = 0.72f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null) { runCatching { focus.requestFocus() }; keyboard?.show() }
                    .padding(start = 13.dp, end = 11.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.62f), modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(7.dp))
                Box(Modifier.weight(1f), contentAlignment = if (idle) Alignment.Center else Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            if (query.isEmpty()) Text(strings.searchPlaceholder, style = IosText.body.copy(color = Color.White.copy(alpha = 0.5f)), maxLines = 1)
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = IosText.body.copy(color = Color.White, textAlign = if (idle) TextAlign.Center else TextAlign.Start),
                                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    if (top != null) onAppClick(top) else if (q.isNotEmpty()) onWebSearch(q, "google")
                                }),
                                modifier = Modifier
                                    .width(IntrinsicSize.Min)
                                    .widthIn(min = 2.dp)
                                    .focusRequester(focus)
                                    .onFocusChanged { focused = it.isFocused }
                            )
                        }
                        // Completion: the app that Return opens, "gm — Google Maps".
                        if (top != null && q.isNotEmpty() && !idle) {
                            Text(
                                "— ${top.displayLabel}",
                                style = IosText.body.copy(color = Color.White.copy(alpha = 0.45f)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = 5.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.09f))
                                    .clickable { onAppClick(top) }
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Cancel, null, tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp).clickable(remember { MutableInteractionSource() }, null) {
                            onQueryChange(""); runCatching { focus.requestFocus() }; keyboard?.show()
                        }
                    )
                } else {
                    Icon(
                        Icons.Rounded.Mic, null, tint = Color.White.copy(alpha = 0.62f),
                        modifier = Modifier.size(22.dp).clickable(remember { MutableInteractionSource() }, null) {
                            runCatching {
                                dictate.launch(
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Results(
    q: String,
    top: AppInfo?,
    results: List<AppInfo>,
    contacts: List<ContactItem>,
    mathResult: String?,
    settings: LauncherSettings,
    strings: IosStrings,
    hs: HomeStrings,
    extras: SpotlightExtras,
    browserPkg: String?,
    moreResults: Boolean,
    onMoreResults: () -> Unit,
    moreApps: Boolean,
    onMoreApps: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onContactCall: (ContactItem) -> Unit,
    onContactMessage: (ContactItem) -> Unit,
    onWebSearch: (String, String) -> Unit
) {
    var truncated = false

    if (mathResult != null) {
        Calculator(q, mathResult, strings, hs)
        Spacer(Modifier.height(18.dp))
    }

    // ------------------------------------------------------------- Toptreffer
    if (top != null) {
        Header(hs.topHit, line = false)
        Spacer(Modifier.height(9.dp))
        TopHitCard(top, extras.topActions, onAppClick, onAppLongClick, extras.onQuickAction)
        Spacer(Modifier.height(22.dp))
    }

    // --------------------------------------------------------------- Kontakte
    if (contacts.isNotEmpty()) {
        Header(strings.contacts)
        val shown = contacts.take(if (moreResults) 6 else 3)
        if (contacts.size > shown.size) truncated = true
        shown.forEachIndexed { i, c ->
            ContactRow(c, divider = i != shown.lastIndex, onCall = { onContactCall(c) }, onMessage = { onContactMessage(c) })
        }
        Spacer(Modifier.height(18.dp))
    }

    // ------------------------------------------------------------- Vorschläge
    if (settings.showWebSearchInSpotlight) {
        Header(hs.spotSuggestions)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable(remember { MutableInteractionSource() }, null) { onWebSearch(q, "google") }
                .padding(horizontal = Side)
        ) {
            SmallTile(browserPkg?.let(extras.iconOf), Icons.Rounded.Explore, Color.White, Color(0xFF0A84FF))
            Spacer(Modifier.width(13.dp))
            Text(q, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.NorthEast, null, tint = Secondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(16.dp))
    }

    // ---------------------------------------------------------- Einstellungen
    if (extras.settings.isNotEmpty()) {
        Header(hs.spotSettings)
        val shown = extras.settings.take(if (moreResults) 6 else 2)
        if (extras.settings.size > shown.size) truncated = true
        val icon = extras.iconOf("com.android.settings")
        shown.forEachIndexed { i, hit ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, null) { extras.onSetting(hit) }
                    .padding(start = Side)
            ) {
                Box(Modifier.padding(vertical = 11.dp)) {
                    val appIcon = hit.packageName?.let(extras.iconOf)
                    BigIcon(icon, Icons.Rounded.Settings)
                    // "Settings for Gmail": the app's own icon sits small on the corner.
                    if (appIcon != null) Image(appIcon, null, modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).padding(1.dp))
                }
                Spacer(Modifier.width(13.dp))
                Box(Modifier.weight(1f).padding(end = Side)) {
                    Column(Modifier.padding(vertical = 13.dp)) {
                        Text(hit.title, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (hit.path.isNotEmpty()) Text(hit.path.joinToString(" → "), style = IosText.subheadline.copy(color = Secondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (i != shown.lastIndex) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // ------------------------------------------------------------------- Apps
    val others = results.drop(1)
    if (others.isNotEmpty()) {
        Header(strings.apps, if (others.size > 3) (if (moreApps) hs.spotLess else hs.spotMore) else null, onAction = onMoreApps)
        val shown = others.take(if (moreApps) 12 else 3)
        if (others.size > shown.size && !moreApps) truncated = true
        shown.forEachIndexed { i, app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .combinedClickable(remember { MutableInteractionSource() }, null, onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) })
                    .padding(start = Side)
            ) {
                BigIcon(app.iconBitmap, null)
                Spacer(Modifier.width(13.dp))
                Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
                    Text(app.displayLabel, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterStart))
                    if (i != shown.lastIndex) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // ---------------------------------------------------------------- Dateien
    if (extras.files.isNotEmpty()) {
        Header(hs.spotFiles, hs.searchInApp, onAction = extras.onFilesApp)
        val shown = extras.files.take(if (moreResults) 12 else 3)
        if (extras.files.size > shown.size) truncated = true
        shown.forEachIndexed { i, f ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, null) { extras.onFile(f) }
                    .padding(start = Side)
            ) {
                Box(Modifier.size(RowIcon), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.width(13.dp))
                Box(Modifier.weight(1f).padding(end = Side)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(f.meta, style = IosText.subheadline.copy(color = Secondary), maxLines = 2)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(ButtonFill).clickable(onClick = extras.onFilesApp),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                    if (i != shown.lastIndex) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    // ------------------------------------------------------------------ Fotos
    if (extras.photos.isNotEmpty()) {
        Header(hs.spotPhotos, hs.searchInApp, line = false, onAction = extras.onGallery)
        Spacer(Modifier.height(10.dp))
        val shown = extras.photos.take(if (moreResults) 24 else 12)
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Side)
        ) {
            shown.chunked(2).forEach { pair ->
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEach { ph -> PhotoThumb(ph, extras) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    // ------------------------------------------------- Mehr Ergebnisse anzeigen
    if (truncated || moreResults) {
        Box(
            Modifier
                .padding(horizontal = Side)
                .fillMaxWidth()
                .height(50.dp)
                .clip(CircleShape)
                .background(ButtonFill)
                .clickable(onClick = onMoreResults),
            contentAlignment = Alignment.Center
        ) {
            Text(if (moreResults) hs.showLess else hs.spotMoreResults, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(22.dp))
    }

    // --------------------------------------------------------- In Apps suchen
    if (settings.showWebSearchInSpotlight) {
        Header(hs.searchInApps, line = false)
        Spacer(Modifier.height(4.dp))
        AppSearchRow(hs.searchWeb, extras.iconOf(browserPkg ?: ""), Icons.Rounded.Explore, Color.White, Color(0xFF0A84FF), divider = true) { onWebSearch(q, "google") }
        AppSearchRow(hs.spotSearchStore, extras.iconOf("com.android.vending"), Icons.Rounded.Shop, Color(0xFF0A84FF), Color.White, divider = true) { onWebSearch(q, "play") }
        AppSearchRow(hs.spotSearchInNamed.format(mapsLabel(extras)), extras.iconOf("com.google.android.apps.maps"), Icons.Rounded.NearMe, Color(0xFF34C759), Color.White, divider = false) { onWebSearch(q, "maps") }
    }

    if (!extras.mediaAccess) {
        Spacer(Modifier.height(22.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Side).fillMaxWidth()
        ) {
            Text(hs.spotAllowMedia, style = IosText.footnote.copy(color = Secondary), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(
                hs.spotAllow,
                style = IosText.footnote.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clip(CircleShape).background(ButtonFill).clickable(onClick = extras.onAllowMedia).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private fun mapsLabel(extras: SpotlightExtras) = if (extras.iconOf("com.google.android.apps.maps") != null) "Maps" else "Karten"

@Composable
private fun Header(title: String, action: String? = null, line: Boolean = true, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Side)) {
            Text(title, style = IosText.subheadline.copy(color = HeaderColor, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
            if (action != null) Text(
                action,
                style = IosText.subheadline.copy(color = HeaderColor, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onAction)
            )
        }
        if (line) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.padding(horizontal = Side).fillMaxWidth().height(0.5.dp).background(Hairline))
        }
    }
}

@Composable
private fun BigIcon(bitmap: ImageBitmap?, fallback: ImageVector?) {
    Box(Modifier.size(RowIcon), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (fallback != null) Box(Modifier.fillMaxSize().clip(IosSquircle).background(Color(0xFF8E8E93)), contentAlignment = Alignment.Center) {
            Icon(fallback, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun SmallTile(bitmap: ImageBitmap?, fallback: ImageVector, tile: Color, glyph: Color) {
    if (bitmap != null) Image(bitmap, null, modifier = Modifier.size(SmallIcon))
    else Box(Modifier.size(SmallIcon).clip(RoundedCornerShape(7.dp)).background(tile), contentAlignment = Alignment.Center) {
        Icon(fallback, null, tint = glyph, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionsCard(apps: List<AppInfo>, onClick: (AppInfo) -> Unit, onLongClick: (AppInfo) -> Unit) {
    if (apps.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardFill)
            .border(0.6.dp, CardRim, RoundedCornerShape(26.dp))
            .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 380f))
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(horizontal = 4.dp, vertical = 12.dp)
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
                            BigIcon(app.iconBitmap, null)
                            Spacer(Modifier.height(5.dp))
                            Text(app.displayLabel, style = IosText.iconLabel.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

/** "Selfie", "Neuer Tab" …: a shortcut of a suggested app, with the app's icon in front. */
@Composable
private fun SuggestedActionRow(action: AppQuickAction, appIcon: ImageBitmap?, divider: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(52.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(start = Side)
    ) {
        SmallTile(appIcon, Icons.Rounded.Search, Color(0xFF3A3A3C), Color.White)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
            Text(action.label, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterStart).padding(end = 34.dp))
            when {
                action.glyph != null -> Image(action.glyph, null, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.75f)), modifier = Modifier.align(Alignment.CenterEnd).size(22.dp))
                action.icon != null -> Image(action.icon, null, modifier = Modifier.align(Alignment.CenterEnd).size(22.dp).graphicsLayer { alpha = 0.85f })
            }
            if (divider) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
        }
    }
}

/** The top hit: the app plus up to three of its quick actions as round glass buttons. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopHitCard(app: AppInfo, actions: List<AppQuickAction>, onClick: (AppInfo) -> Unit, onLongClick: (AppInfo) -> Unit, onAction: (AppQuickAction) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardFill)
            .border(0.6.dp, CardRim, RoundedCornerShape(26.dp))
            .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 380f))
            .padding(horizontal = 4.dp, vertical = 13.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).combinedClickable(remember { MutableInteractionSource() }, null, onClick = { onClick(app) }, onLongClick = { onLongClick(app) })
        ) {
            BigIcon(app.iconBitmap, null)
            Spacer(Modifier.height(5.dp))
            Text(app.displayLabel, style = IosText.iconLabel.copy(color = Color.White, fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 3.dp))
        }
        for (i in 0 until 3) {
            val a = actions.getOrNull(i)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).then(if (a != null) Modifier.clickable(remember { MutableInteractionSource() }, null) { onAction(a) } else Modifier)
            ) {
                if (a != null) {
                    Box(Modifier.size(RowIcon), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(ButtonFill), contentAlignment = Alignment.Center) {
                            when {
                                a.glyph != null -> Image(a.glyph, null, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White), modifier = Modifier.size(28.dp))
                                a.icon != null -> Image(a.icon, null, modifier = Modifier.size(30.dp))
                                else -> Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(a.label, style = IosText.iconLabel.copy(color = Color.White, fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactRow(c: ContactItem, divider: Boolean, onCall: () -> Unit, onMessage: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(82.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onCall).padding(start = Side)
    ) {
        Box(
            Modifier.size(RowIcon).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFA5A5AA), Color(0xFF7C7C81)))),
            contentAlignment = Alignment.Center
        ) {
            Text(c.name.trim().take(1).uppercase(), style = IosText.title1.copy(color = Color.White, fontWeight = FontWeight.Medium, fontSize = 26.sp))
        }
        Spacer(Modifier.width(13.dp))
        Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterStart)) {
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(c.phoneNumber, style = IosText.subheadline.copy(color = Secondary), maxLines = 1)
                }
                RoundButton(Icons.Rounded.ChatBubble, onMessage)
                Spacer(Modifier.width(10.dp))
                RoundButton(Icons.Rounded.Call, onCall)
            }
            if (divider) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
        }
    }
}

@Composable
private fun PhotoThumb(photo: PhotoHit, extras: SpotlightExtras) {
    val px = with(LocalDensity.current) { 84.dp.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(null, photo.id) {
        value = withContext(Dispatchers.IO) { extras.thumbnail(photo.uri, px) }
    }
    Box(
        Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { extras.onPhoto(photo) }
    ) {
        bitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun Calculator(q: String, mathResult: String, strings: IosStrings, hs: HomeStrings) {
    // Tap the result to copy it; the expression is shown the way it was understood.
    val calc = remember(q) { com.tms.os.launcher.domain.SpotlightCalculator.evaluate(q) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var copied by remember(q) { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1400); copied = false } }
    Header(strings.calculator, line = false)
    Spacer(Modifier.height(9.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CardFill)
            .border(0.6.dp, CardRim, RoundedCornerShape(26.dp))
            .clickable(remember { MutableInteractionSource() }, null) {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(calc?.plain ?: mathResult))
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                copied = true
            }
            .padding(14.dp)
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
            style = IosText.footnote.copy(color = if (copied) Color(0xFF30D158) else Color.White, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clip(CircleShape).background(ButtonFill).padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AppSearchRow(text: String, icon: ImageBitmap?, fallback: ImageVector, tile: Color, glyph: Color, divider: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(52.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(start = Side)
    ) {
        SmallTile(icon, fallback, tile, glyph)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f).fillMaxHeight().padding(end = Side)) {
            Text(text, style = IosText.headline.copy(color = Color.White, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterStart))
            if (divider) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Hairline))
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(ButtonFill).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
}
