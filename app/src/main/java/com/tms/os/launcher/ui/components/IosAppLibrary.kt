@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tms.os.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.ui.theme.IosMaterial
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.wallpaperGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/* -------------------------------------------------------------------------- */
/*                              App Library page                              */
/* -------------------------------------------------------------------------- */

private data class LibraryPod(
    val id: String,
    val title: String,
    /** What the pod shows: three large icons plus up to four small ones. */
    val preview: List<AppInfo>,
    /** Everything in the category, shown when the pod is opened. */
    val all: List<AppInfo>
)

private val LibrarySideMargin = 33.dp
private val PodGap = 22.dp

/**
 * iOS App Library: a centred search pill and a two-column grid of category
 * pods. Large icons launch their app; the small 2×2 cluster (or the pod's
 * empty space) opens the whole category via [onOpenPod]. Tapping the search
 * pill switches to the alphabetical list with an A–Z index.
 */
@Composable
fun IosAppLibraryPage(
    allApps: List<AppInfo>,
    categorizedApps: Map<AppCategory, List<AppInfo>>,
    settings: LauncherSettings,
    strings: IosStrings,
    isUnfolded: Boolean,
    /** True while a category is open on top: the pods fade away. */
    hidden: Boolean,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onOpenPod: (String, List<AppInfo>) -> Unit
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val fieldFocus = remember { FocusRequester() }
    val closeSearch: () -> Unit = {
        query = ""
        searching = false
        focus.clearFocus()
        keyboard?.hide()
    }
    BackHandler(enabled = searching) { closeSearch() }
    LaunchedEffect(searching) {
        if (searching) { delay(60); runCatching { fieldFocus.requestFocus() }; keyboard?.show() }
    }
    val language = settings.appLanguage

    val pods = remember(categorizedApps, allApps, language) { buildLibraryPods(categorizedApps, allApps, strings, language) }
    val visibleApps = remember(allApps, query) {
        allApps.filter { !it.isHidden && it.displayLabel.contains(query.trim(), ignoreCase = true) }
            .sortedBy { it.displayLabel.lowercase(Locale.getDefault()) }
    }
    val contentAlpha by animateFloatAsState(if (hidden) 0f else 1f, tween(220), label = "libraryAlpha")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .padding(top = 9.dp)
    ) {
        // ----------------------------------------------------------- search pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = LibrarySideMargin)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (searching) Arrangement.Start else Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(43.dp)
                    .wallpaperGlass(CircleShape, tint = IosMaterial.pod)
                    .clickable(remember { MutableInteractionSource() }, null) { searching = true }
                    .padding(horizontal = 14.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(7.dp))
                if (searching) {
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text(strings.appLibrary, style = IosText.body.copy(color = Color.White.copy(alpha = 0.92f)), maxLines = 1)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = IosText.body.copy(color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus)
                        )
                    }
                } else {
                    Text(strings.appLibrary, style = IosText.body.copy(color = Color.White.copy(alpha = 0.92f)), maxLines = 1)
                }
            }
            if (searching) Text(
                strings.cancel,
                style = IosText.subheadline.copy(color = Color.White),
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = closeSearch).padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
            )
        }

        if (searching) {
            LibraryAlphabeticalList(
                apps = visibleApps,
                onAppClick = { app, b -> closeSearch(); onAppClick(app, b) },
                onAppLongClick = onAppLongClick,
                modifier = Modifier.weight(1f).imePadding()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isUnfolded) 4 else 2),
                contentPadding = PaddingValues(start = LibrarySideMargin, end = LibrarySideMargin, top = 34.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalArrangement = Arrangement.spacedBy(PodGap),
                modifier = Modifier.weight(1f)
            ) {
                items(pods, key = { it.id }) { pod ->
                    LibraryCategoryPod(
                        pod = pod,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        onOpen = { onOpenPod(pod.title, pod.all) }
                    )
                }
            }
        }
    }
}

private fun buildLibraryPods(
    categorizedApps: Map<AppCategory, List<AppInfo>>,
    allApps: List<AppInfo>,
    strings: IosStrings,
    language: com.tms.os.launcher.model.AppLanguage
): List<LibraryPod> {
    val pods = mutableListOf<LibraryPod>()
    val visible = allApps.filter { !it.isHidden }

    val suggestions = categorizedApps[AppCategory.SUGGESTIONS].orEmpty().ifEmpty { visible.take(4) }
    if (suggestions.isNotEmpty()) pods.add(LibraryPod("pod_suggestions", strings.suggestions, suggestions.take(4), suggestions.take(4)))

    val recent = visible.sortedByDescending { it.firstInstallTime }
    if (recent.isNotEmpty()) pods.add(LibraryPod("pod_recent", strings.recentlyAdded, recent.take(7), recent.take(24)))

    for (category in AppCategory.entries) {
        if (category == AppCategory.SUGGESTIONS) continue
        val apps = categorizedApps[category].orEmpty()
        if (apps.isEmpty()) continue
        pods.add(LibraryPod("pod_${category.id}", category.localizedTitle(language), apps.take(7), apps))
    }
    return pods
}

/* -------------------------------------------------------------------------- */
/*                               Category pod                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun LibraryCategoryPod(
    pod: LibraryPod,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onOpen: () -> Unit
) {
    val apps = pod.preview
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val podSize = maxWidth
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .wallpaperGlass(RoundedCornerShape(25.dp), tint = IosMaterial.pod)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onOpen)
                    .padding(12.dp)
            ) {
                val gap = 12.dp
                val large = (podSize - 24.dp - gap) / 2
                val mini = (large - 9.dp) / 2
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        LibraryAppIcon(apps.getOrNull(0), large, onAppClick, onAppLongClick)
                        LibraryAppIcon(apps.getOrNull(1), large, onAppClick, onAppLongClick)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        LibraryAppIcon(apps.getOrNull(2), large, onAppClick, onAppLongClick)
                        if (apps.size <= 4) {
                            LibraryAppIcon(apps.getOrNull(3), large, onAppClick, onAppLongClick)
                        } else {
                            // The small icons are not buttons: the cluster opens the category.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(large).clickable(remember { MutableInteractionSource() }, null, onClick = onOpen)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        for (col in 0..1) LibraryAppIcon(apps.getOrNull(3 + row * 2 + col), mini, onAppClick, onAppLongClick, interactive = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            // iOS lets long titles run a little wider than the pod before truncating.
            Text(
                text = pod.title,
                style = IosText.footnote.copy(color = Color.White, fontSize = 13.sp, lineHeight = 17.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.requiredWidth(podSize + 20.dp)
            )
        }
    }
}

@Composable
private fun LibraryAppIcon(
    app: AppInfo?,
    size: Dp,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    interactive: Boolean = true
) {
    if (app == null) {
        Spacer(Modifier.size(size))
        return
    }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .clip(IosSquircle)
            .then(
                if (interactive) Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAppClick(app, bounds) },
                    onLongClick = { onAppLongClick(app, bounds) }
                ) else Modifier
            )
    ) {
        if (app.iconBitmap != null) {
            Image(app.iconBitmap, app.displayLabel, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            LibraryLetterIcon(app.displayLabel, size)
        }
    }
}

@Composable
private fun LibraryLetterIcon(label: String, size: Dp) {
    val hue = remember(label) { ((label.hashCode() % 360) + 360) % 360 }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(Brush.verticalGradient(listOf(Color.hsv(hue.toFloat(), 0.55f, 0.94f), Color.hsv(((hue + 24) % 360).toFloat(), 0.72f, 0.62f))))
    ) {
        Text(label.trim().take(1).uppercase(Locale.getDefault()), color = Color.White, style = IosText.title3.copy(fontSize = (size.value * 0.38f).sp))
    }
}

/* -------------------------------------------------------------------------- */
/*                              Alphabetical list                             */
/* -------------------------------------------------------------------------- */

private data class LibraryLetterGroup(val letter: String, val apps: List<AppInfo>)

@Composable
private fun LibraryAlphabeticalList(
    apps: List<AppInfo>,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember(apps) {
        val map = LinkedHashMap<String, MutableList<AppInfo>>()
        for (app in apps) {
            val first = app.displayLabel.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (first in 'A'..'Z') first.toString() else "#"
            map.getOrPut(key) { mutableListOf() }.add(app)
        }
        map.map { (letter, list) -> LibraryLetterGroup(letter, list) }.sortedBy { it.letter }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Box(modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groups.forEach { group ->
                item(key = "letter_${group.letter}") {
                    Text(
                        group.letter,
                        style = IosText.headline.copy(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = LibrarySideMargin, top = 10.dp, bottom = 6.dp)
                    )
                }
                items(group.apps, key = { it.packageName }) { app ->
                    var bounds by remember { mutableStateOf(Rect.Zero) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(77.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onAppClick(app, bounds) },
                                onLongClick = { onAppLongClick(app, bounds) }
                            )
                            .padding(start = LibrarySideMargin)
                    ) {
                        Box(Modifier.size(51.dp).onGloballyPositioned { bounds = it.boundsInRoot() }.clip(IosSquircle)) {
                            if (app.iconBitmap != null) Image(app.iconBitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else LibraryLetterIcon(app.displayLabel, 51.dp)
                        }
                        Spacer(Modifier.width(24.dp))
                        Box(Modifier.weight(1f).fillMaxHeight().padding(end = 22.dp)) {
                            Text(
                                app.displayLabel,
                                style = IosText.body.copy(color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.14f)))
                        }
                    }
                }
            }
        }

        // A–Z index
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.CenterEnd).width(20.dp).fillMaxHeight()
        ) {
            groups.forEach { group ->
                Text(
                    text = group.letter,
                    style = IosText.caption.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        var index = 0
                        for (g in groups) {
                            if (g.letter == group.letter) break
                            index += 1 + g.apps.size
                        }
                        scope.launch { listState.scrollToItem(index) }
                    }
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                               Opened category                              */
/* -------------------------------------------------------------------------- */

/**
 * A category opened from the App Library, as on iOS: full screen, the title
 * top-left and all apps in the home-screen grid. Short categories sit centred
 * vertically, long ones scroll from the top. Tap outside or go back to close.
 */
@Composable
fun IosLibraryCategoryView(
    title: String,
    apps: List<AppInfo>,
    columns: Int,
    sidePadding: Dp,
    iconSize: Dp,
    labelFontSizeSp: Float,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(240)) }
    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch { progress.animateTo(0f, tween(180)); onDismiss(); then() }
    }
    BackHandler { close() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                alpha = p
                val s = 0.94f + 0.06f * p
                scaleX = s; scaleY = s
            }
            .clickable(remember { MutableInteractionSource() }, null) { close() }
    ) {
        val cell = (maxWidth - sidePadding * 2) / columns
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(top = 70.dp, bottom = 48.dp)
        ) {
            Text(
                title,
                style = IosText.headline.copy(color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = sidePadding + (cell - iconSize) / 2, end = 24.dp, bottom = 20.dp)
            )
            apps.chunked(columns).forEach { row ->
                Row(Modifier.padding(horizontal = sidePadding).height(cell)) {
                    for (i in 0 until columns) {
                        val app = row.getOrNull(i)
                        Box(Modifier.width(cell), contentAlignment = Alignment.TopCenter) {
                            if (app != null) IosAppIcon(
                                app = app,
                                iconSize = iconSize,
                                labelFontSizeSp = labelFontSizeSp,
                                showBadges = false,
                                onClick = { close { onAppClick(app, Rect.Zero) } },
                                onClickWithBounds = { b -> close { onAppClick(app, b) } },
                                onLongClick = { onAppLongClick(app, Rect.Zero) },
                                onLongClickWithBounds = { b -> onAppLongClick(app, b) }
                            )
                        }
                    }
                }
            }
        }
    }
}
