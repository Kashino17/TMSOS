@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tms.os.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
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

private const val HiddenPodId = "pod_hidden"
private val NeverAway: () -> Boolean = { false }
private val LibrarySideMargin = 33.dp
private val PodGap = 22.dp
/** Lighter than the dock: the pods read as frosted glass over the wallpaper, as on iOS. */
private val PodTint = Color(0x9E4A4A4F)

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
    /** 0..1 while a category grows out of its pod: the library zooms past and fades. */
    podProgress: () -> Float,
    podOrigin: Rect?,
    /** Title of the pod that is open right now: its icons are in flight, the pod shows them not. */
    openPodTitle: String? = null,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onOpenPod: (String, List<AppInfo>, Rect) -> Unit,
    /** False as soon as the user swipes away from the library (read lazily, no recomposition). */
    isActive: () -> Boolean = { true },
    /** Title of the "Hidden" pod; it always looks empty and opens only after Touch ID. */
    hiddenTitle: String = "",
    onOpenHidden: (Rect) -> Unit = {}
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
    // Swiping to another page ends the search and puts the keyboard away, as on iOS.
    val activeNow by rememberUpdatedState(isActive)
    LaunchedEffect(searching) {
        if (searching) androidx.compose.runtime.snapshotFlow { activeNow() }.collect { active -> if (!active) closeSearch() }
    }
    LaunchedEffect(searching) {
        if (searching) { delay(60); runCatching { fieldFocus.requestFocus() }; keyboard?.show() }
    }
    val language = settings.appLanguage

    val pods = remember(categorizedApps, allApps, language, hiddenTitle) {
        buildLibraryPods(categorizedApps, allApps, strings, language) +
            listOfNotNull(hiddenTitle.takeIf { it.isNotEmpty() }?.let { LibraryPod(HiddenPodId, it, emptyList(), emptyList()) })
    }
    val visibleApps = remember(allApps, query) {
        allApps.filter { !it.isHidden && it.displayLabel.contains(query.trim(), ignoreCase = true) }
            .sortedBy { it.displayLabel.lowercase(Locale.getDefault()) }
    }
    // iOS: pull the library down and its search opens, like Spotlight on the home screen.
    val pull = remember { Animatable(0f) }
    val pullScope = rememberCoroutineScope()
    val openSearchPx = with(LocalDensity.current) { 64.dp.toPx() }
    val searchingNow by rememberUpdatedState(searching)
    val pullToSearch = remember(openSearchPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Moving back up first releases the pull.
                if (pull.value > 0f && available.y < 0f) {
                    val used = maxOf(available.y, -pull.value / 0.4f)
                    pullScope.launch { pull.snapTo((pull.value + used * 0.4f).coerceAtLeast(0f)) }
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f || searchingNow) return Offset.Zero
                val next = pull.value + available.y * 0.4f
                pullScope.launch { pull.snapTo(next) }
                if (next > openSearchPx * 0.4f) searching = true
                return available
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull.value > 0f) pull.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 380f))
                return Velocity.Zero
            }
        }
    }
    LaunchedEffect(searching) { if (pull.value > 0f) pull.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 380f)) }
    // 0 = pods, 1 = alphabetical search list. One spring drives pill, button and content.
    val searchT by animateFloatAsState(if (searching) 1f else 0f, spring(dampingRatio = 0.9f, stiffness = 380f), label = "librarySearch")

    // The pods are recorded once per frame; the search pill shows them blurred
    // where they scroll underneath it, like the iOS material.
    val canBlur = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val gridLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val pillLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }
    var pillOrigin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val pillBlurPx = with(density) { 14.dp.toPx() }

    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { pageOrigin = it.positionInRoot() }
            .graphicsLayer {
                val p = podProgress().coerceIn(0f, 1f)
                if (p > 0f) {
                    // iOS: the library recedes - shrinks a little around the opened pod - and
                    // fades as the category comes forward, softening slightly.
                    alpha = (1f - p * p).coerceIn(0f, 1f)
                    val sc = 1f - 0.12f * p
                    scaleX = sc; scaleY = sc
                    podOrigin?.let { o ->
                        transformOrigin = TransformOrigin(((o.center.x - pageOrigin.x) / size.width).coerceIn(0f, 1f), ((o.center.y - pageOrigin.y) / size.height).coerceIn(0f, 1f))
                    }
                    if (canBlur) renderEffect = androidx.compose.ui.graphics.BlurEffect(p * 10.dp.toPx(), p * 10.dp.toPx(), androidx.compose.ui.graphics.TileMode.Decal)
                } else { renderEffect = null; scaleX = 1f; scaleY = 1f }
            }
    ) {
        // ------------------------------------------------------------- pods
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { gridOrigin = it.positionInRoot() }
                .graphicsLayer {
                    // Behind the search list the library stays, blurred and dimmed.
                    val t = searchT
                    alpha = 1f - 0.45f * t
                    renderEffect = if (canBlur && t > 0.01f) androidx.compose.ui.graphics.BlurEffect(t * 26.dp.toPx(), t * 26.dp.toPx(), androidx.compose.ui.graphics.TileMode.Decal) else null
                    // Pods fade out at the top edge instead of being cut off.
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    gridLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(gridLayer)
                    val fade = 12.dp.toPx()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = fade),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isUnfolded) 4 else 2),
                contentPadding = PaddingValues(start = LibrarySideMargin, end = LibrarySideMargin, top = 9.dp + 43.dp + 34.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalArrangement = Arrangement.spacedBy(PodGap),
                userScrollEnabled = !searching,
                modifier = Modifier.fillMaxSize().nestedScroll(pullToSearch).graphicsLayer { translationY = pull.value }
            ) {
                items(pods, key = { it.id }) { pod ->
                    LibraryCategoryPod(
                        pod = pod,
                        iconsAway = if (pod.title == openPodTitle) ({ podProgress() > 0f }) else NeverAway,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        onOpen = { origin -> if (pod.id == HiddenPodId) onOpenHidden(origin) else onOpenPod(pod.title, pod.all, origin) }
                    )
                }
            }
        }

        // ------------------------------------------------------ search list
        // Slides up over the blurred library; holds every touch while it is there.
        if (searching || searchT > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 9.dp + 43.dp)
                    .graphicsLayer {
                        alpha = searchT.coerceIn(0f, 1f)
                        translationY = (1f - searchT) * 90.dp.toPx()
                    }
                    .clickable(remember { MutableInteractionSource() }, null, enabled = searching) {}
            ) {
                LibraryAlphabeticalList(
                    apps = visibleApps,
                    onAppClick = { app, b -> closeSearch(); onAppClick(app, b) },
                    onAppLongClick = onAppLongClick,
                    onScroll = { focus.clearFocus(); keyboard?.hide() },
                    modifier = Modifier.fillMaxSize().imePadding()
                )
            }
        }

        // ----------------------------------------------------------- search pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp).padding(horizontal = LibrarySideMargin)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(43.dp)
                    .onGloballyPositioned { pillOrigin = it.positionInRoot() }
                    .clickable(remember { MutableInteractionSource() }, null) {
                        if (!searching) searching = true else { runCatching { fieldFocus.requestFocus() }; keyboard?.show() }
                    }
            ) {
                // The pill's glass: iOS drops it the moment a category opens; the label stays a little longer.
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = (1f - podProgress() * 6f).coerceIn(0f, 1f) }
                        .then(
                        if (canBlur) Modifier
                            .clip(CircleShape)
                            .wallpaperGlass(CircleShape, tint = IosMaterial.pod.copy(alpha = 0.55f), rim = 0f)
                            .drawBehind {
                                // The pods passing underneath, blurred.
                                if (searchT < 0.99f) {
                                    pillLayer.renderEffect = androidx.compose.ui.graphics.BlurEffect(pillBlurPx, pillBlurPx, androidx.compose.ui.graphics.TileMode.Clamp)
                                    pillLayer.record(size = androidx.compose.ui.unit.IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))) {
                                        translate(gridOrigin.x - pillOrigin.x, gridOrigin.y - pillOrigin.y) { drawLayer(gridLayer) }
                                    }
                                    pillLayer.alpha = 1f - searchT
                                    drawLayer(pillLayer)
                                }
                                drawRect(Color(0xFF2A2A2D).copy(alpha = 0.35f))
                            }
                            .border(0.6.dp, Color.White.copy(alpha = IosMaterial.hairline), CircleShape)
                        else Modifier.wallpaperGlass(CircleShape, tint = IosMaterial.pod)
                        )
                )
                // Centred at rest, gliding to the left edge when searching.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp).align(BiasAlignment(horizontalBias = -searchT, verticalBias = 0f))
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(7.dp))
                    Box {
                        if (query.isEmpty()) Text(strings.appLibrary, style = IosText.body.copy(color = Color.White.copy(alpha = 0.92f)), maxLines = 1)
                        if (searching) BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = IosText.body.copy(color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 2.dp).focusRequester(fieldFocus)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = searching,
                enter = expandHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f), expandFrom = Alignment.Start) + fadeIn(tween(200, delayMillis = 60)),
                exit = shrinkHorizontally(spring(dampingRatio = 1f, stiffness = 420f), shrinkTowards = Alignment.Start) + fadeOut(tween(120))
            ) {
                Text(
                    strings.cancel,
                    style = IosText.body.copy(color = Color.White),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = closeSearch).padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
                )
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
    iconsAway: () -> Boolean,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onOpen: (Rect) -> Unit
) {
    val apps = pod.preview
    var podBounds by remember { mutableStateOf(Rect.Zero) }
    val open = { onOpen(podBounds) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val podSize = maxWidth
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { podBounds = it.boundsInRoot() }
                    .wallpaperGlass(RoundedCornerShape(25.dp), tint = PodTint, rim = 0.16f)
                    .clickable(remember { MutableInteractionSource() }, null, onClick = open)
                    .padding(12.dp)
            ) {
                // "Hidden": never shows what is inside, only a closed eye.
                if (pod.id == HiddenPodId) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Rounded.VisibilityOff, null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.Center).size(podSize * 0.26f)
                    )
                    return@Box
                }
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
                                modifier = Modifier
                                    .size(large)
                                    .clickable(remember { MutableInteractionSource() }, null, onClick = open)
                                    // While its category is open these fly as part of it (iOS); the big ones stay.
                                    .graphicsLayer { alpha = if (iconsAway()) 0f else 1f }
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
            Box(Modifier.clip(IosSquircle)) { LibraryLetterIcon(app.displayLabel, size) }
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

private val IndexLetters = ('A'..'Z').map { it.toString() } + "#"

private fun indexLetterOf(app: AppInfo): String {
    val first = java.text.Normalizer.normalize(app.displayLabel.trim().take(1), java.text.Normalizer.Form.NFD).firstOrNull()?.uppercaseChar() ?: '#'
    return if (first in 'A'..'Z') first.toString() else "#"
}

/**
 * The library's search list as on iOS: one flat alphabetical list (no letter
 * headings) over the blurred library, and the full A–Z index on the right that
 * can be tapped or scrubbed.
 */
@Composable
private fun LibraryAlphabeticalList(
    apps: List<AppInfo>,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // "#" (digits, symbols) goes last, as on iOS.
    val sorted = remember(apps) { apps.sortedWith(compareBy({ indexLetterOf(it) == "#" }, { it.displayLabel.lowercase(Locale.getDefault()) })) }
    val firstIndex = remember(sorted) {
        val m = HashMap<String, Int>()
        sorted.forEachIndexed { i, a -> m.putIfAbsent(indexLetterOf(a), i) }
        m
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dropKeyboard by rememberUpdatedState(onScroll)
    val keyboardAway = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 1f) dropKeyboard()
                return Offset.Zero
            }
        }
    }
    var lastLetter by remember { mutableStateOf("") }
    fun jumpTo(letter: String) {
        if (letter == lastLetter) return
        lastLetter = letter
        // Letters without apps jump to the next letter that has some.
        val from = IndexLetters.indexOf(letter).coerceAtLeast(0)
        val target = IndexLetters.drop(from).firstNotNullOfOrNull { firstIndex[it] } ?: (sorted.size - 1).coerceAtLeast(0)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { listState.scrollToItem(target) }
    }

    Box(modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize().nestedScroll(keyboardAway)
        ) {
            items(sorted, key = { it.packageName }) { app ->
                var bounds by remember { mutableStateOf(Rect.Zero) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onAppClick(app, bounds) },
                            onLongClick = { onAppLongClick(app, bounds) }
                        )
                        .padding(start = 22.dp)
                ) {
                    Box(Modifier.size(44.dp).onGloballyPositioned { bounds = it.boundsInRoot() }) {
                        if (app.iconBitmap != null) Image(app.iconBitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Box(Modifier.clip(IosSquircle)) { LibraryLetterIcon(app.displayLabel, 44.dp) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.weight(1f).fillMaxHeight().padding(end = 30.dp)) {
                        Text(
                            app.displayLabel,
                            style = IosText.body.copy(color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.16f)))
                    }
                }
            }
        }

        // A–Z index: tap a letter or slide along it.
        var indexHeight by remember { mutableStateOf(1) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(22.dp)
                .onGloballyPositioned { indexHeight = it.size.height.coerceAtLeast(1) }
                .pointerInput(sorted) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        lastLetter = ""
                        fun at(y: Float) = IndexLetters[((y / indexHeight) * IndexLetters.size).toInt().coerceIn(0, IndexLetters.lastIndex)]
                        jumpTo(at(down.position.y))
                        down.consume()
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull() ?: break
                            if (!c.pressed) break
                            jumpTo(at(c.position.y)); c.consume()
                        }
                    }
                }
        ) {
            IndexLetters.forEach { letter ->
                Text(
                    text = letter,
                    style = IosText.caption.copy(color = Color.White.copy(alpha = 0.78f), fontSize = 10.sp, lineHeight = 12.5.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                               Opened category                              */
/* -------------------------------------------------------------------------- */

/**
 * A category opened from the App Library, matched frame by frame to iOS:
 * - the whole category page (title, grid, names) starts as a see-through miniature whose
 *   grid exactly covers the pod's small 2 x 2 cluster, and grows to full screen;
 * - the cluster's four small icons zoom along with it as a group (top left -> first column,
 *   top right -> last column) and fade out while the real page becomes solid;
 * - closing runs the same thing backwards, into the cluster.
 * [progress] is owned by the caller so the library behind can recede and fade in step.
 * [fromPodIcons] is false for a pod that shows no icons (Hidden): it grows out of the pod's middle.
 */
@Composable
fun IosLibraryCategoryView(
    title: String,
    apps: List<AppInfo>,
    origin: Rect,
    progress: () -> Float,
    columns: Int,
    sidePadding: Dp,
    iconSize: Dp,
    labelFontSizeSp: Float,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onRequestClose: () -> Unit,
    fromPodIcons: Boolean = true
) {
    BackHandler(onBack = onRequestClose)
    val interactive by remember { androidx.compose.runtime.derivedStateOf { progress() > 0.3f } }
    val density = LocalDensity.current
    val iconPx = with(density) { iconSize.toPx() }
    // What the grid grows out of (root px): the span between the cluster's icon centres.
    val span = remember(origin, apps.size, fromPodIcons, density) { podSourceSpan(origin, apps.size, fromPodIcons, density) }
    // The cluster's small icons, which zoom with the page as a group and fade.
    val cluster = remember(origin, apps.size, fromPodIcons, density) {
        podIconSlots(origin, apps.size, fromPodIcons, density).withIndex().filter { (i, slot) -> slot.visible && i in 3..6 && apps.size > 4 }
    }

    var pageRoot by remember { mutableStateOf(Offset.Zero) }
    var inner by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var firstCell by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { pageRoot = it.positionInRoot() }
            // Only once it is (almost) there: a category caught at progress 0 must not block the screen.
            .clickable(remember { MutableInteractionSource() }, null, enabled = interactive, onClick = onRequestClose)
    ) {
        val pageW = constraints.maxWidth.toFloat()
        val pageH = constraints.maxHeight.toFloat()
        val cell = (maxWidth - sidePadding * 2) / columns
        val rows = (apps.size + columns - 1) / columns
        val cellPx = with(density) { cell.toPx() }
        val titleStyle = IosText.title1.copy(color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        val titleModifier = Modifier.padding(start = (cell - iconSize) / 2, end = 24.dp, bottom = 20.dp)
        val fits = 54.dp + cell * rows + 118.dp <= maxHeight

        // The grid's icon-centre span on the full-size page (page px): centre and width.
        val gridSpan: () -> Pair<Offset, Float>? = {
            val c = firstCell; val i = inner
            if (c == null || i == null || !c.isAttached || !i.isAttached) null
            else {
                val first = i.localPositionOf(c, Offset.Zero) + Offset(cellPx / 2f, iconPx / 2f)
                val lastY = minOf(first.y + (rows - 1) * cellPx, pageH - cellPx).coerceAtLeast(first.y)
                val w = (columns - 1).coerceAtLeast(1) * cellPx
                Offset(first.x + w / 2f, (first.y + lastY) / 2f) to w
            }
        }
        val sourceCentre = if (span != null) span.first else pageRoot + Offset(pageW / 2f, pageH / 2f)
        val startScaleFor: (Float) -> Float = { w -> ((span?.second ?: (w * 0.25f)) / w).coerceIn(0.05f, 1f) }

        @Composable
        fun Cell(index: Int, app: AppInfo) {
            Box(
                Modifier
                    .width(cell).height(cell)
                    .then(if (index == 0) Modifier.onGloballyPositioned { firstCell = it } else Modifier),
                contentAlignment = Alignment.TopCenter
            ) {
                IosAppIcon(
                    app = app,
                    iconSize = iconSize,
                    labelFontSizeSp = labelFontSizeSp,
                    showBadges = false,
                    onClick = { onAppClick(app, Rect.Zero) },
                    onClickWithBounds = { b -> onAppClick(app, b) },
                    onLongClick = { onAppLongClick(app, Rect.Zero) },
                    onLongClickWithBounds = { b -> onAppLongClick(app, b) }
                )
            }
        }

        // ------------------------------------------------------------- the page
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress()
                    val g = gridSpan()
                    if (g == null) { alpha = if (p > 0.99f) 1f else 0f; return@graphicsLayer }
                    val (gc, gw) = g
                    val s0 = startScaleFor(gw)
                    val z = s0 + (1f - s0) * p
                    scaleX = z; scaleY = z
                    transformOrigin = TransformOrigin((gc.x / size.width).coerceIn(0f, 1f), (gc.y / size.height).coerceIn(0f, 1f))
                    // The grid's centre travels from the cluster to its place.
                    val home = pageRoot + gc
                    val now = sourceCentre + (home - sourceCentre) * p
                    translationX = now.x - home.x; translationY = now.y - home.y
                    // iOS: faintly there at once, nearly solid by a third, solid a little past half.
                    val a = (p / 0.55f).coerceIn(0f, 1f)
                    alpha = 1f - (1f - a) * (1f - a)
                }
                .onGloballyPositioned { inner = it }
        ) {
            if (fits) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(start = sidePadding, end = sidePadding, top = 70.dp, bottom = 48.dp)
                ) {
                    Text(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = titleModifier)
                    apps.chunked(columns).forEachIndexed { r, row -> Row { row.forEachIndexed { c, app -> Cell(r * columns + c, app) } } }
                }
            } else {
                // Long categories: only what is on screen gets composed.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(start = sidePadding, end = sidePadding, top = 70.dp, bottom = 48.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "title") {
                        Text(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = titleModifier)
                    }
                    itemsIndexed(apps, key = { _, a -> a.packageName }) { i, a -> Cell(i, a) }
                }
            }
        }

        // ------------------------------------------- the pod's cluster, zooming along
        cluster.forEach { (index, slot) ->
            val app = apps[index]
            Box(
                Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        val p = progress()
                        if (p <= 0f) { alpha = 0f; return@graphicsLayer }
                        val g = gridSpan()
                        // Same mapping as the page: the cluster grows with it from its own place.
                        var z = 1f
                        val (now, grow) = if (g == null) sourceCentre to 1f else {
                            val s0 = startScaleFor(g.second)
                            z = s0 + (1f - s0) * p
                            (sourceCentre + (pageRoot + g.first - sourceCentre) * p) to (z / s0)
                        }
                        // Positions spread with the page's zoom; the icons themselves only grow
                        // to the size the page's icons have at that moment (iOS).
                        val centre = now + (slot.centre - sourceCentre) * grow
                        val edge = slot.size + (iconPx * z - slot.size) * p
                        scaleX = edge / iconPx; scaleY = edge / iconPx
                        translationX = centre.x - pageRoot.x - iconPx / 2f
                        translationY = centre.y - pageRoot.y - iconPx / 2f
                        // Solid at first, gone as the page arrives.
                        alpha = ((1f - p) / 0.75f).coerceIn(0f, 1f)
                    }
            ) {
                if (app.iconBitmap != null) Image(app.iconBitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.clip(IosSquircle)) { LibraryLetterIcon(app.displayLabel, iconSize) }
            }
        }
    }
}

/**
 * The span the category grid grows out of (root px): centre and the distance between the
 * outer icon centres. With five apps or more that is the small 2 x 2 cluster; otherwise
 * (and for a pod without icons) the two large columns.
 */
private fun podSourceSpan(pod: Rect, count: Int, withIcons: Boolean, density: androidx.compose.ui.unit.Density): Pair<Offset, Float>? {
    if (pod.width <= 0f) return null
    with(density) {
        val pad = 12.dp.toPx(); val gap = 12.dp.toPx()
        val large = (pod.width - 2 * pad - gap) / 2f
        if (!withIcons || count <= 4) return pod.center to (large + gap)
        val mini = (large - 9.dp.toPx()) / 2f
        val clusterCentre = Offset(pod.left + pad + large + gap + large / 2f, pod.top + pad + large + gap + large / 2f)
        return clusterCentre to (mini + 5.dp.toPx())
    }
}

/** One app's place in a pod: icon centre and edge length in root pixels; [visible] = drawn in the pod. */
private class PodSlot(val centre: Offset, val size: Float, val visible: Boolean)

/**
 * Mirrors [LibraryCategoryPod]'s layout: 12 dp padding, 12 dp gap, two large icons per row;
 * from five apps on, the fourth quarter is a 2 x 2 cluster of small icons (5 dp apart).
 * Apps after the seventh are not in the pod: they start as tiny icons in the cluster's middle.
 */
private fun podIconSlots(pod: Rect, count: Int, withIcons: Boolean, density: androidx.compose.ui.unit.Density): List<PodSlot> {
    if (pod.width <= 0f || count == 0) return emptyList()
    with(density) {
        val pad = 12.dp.toPx(); val gap = 12.dp.toPx(); val miniGap = 5.dp.toPx()
        val large = (pod.width - 2 * pad - gap) / 2f
        val mini = (large - 9.dp.toPx()) / 2f
        fun largeAt(col: Int, row: Int) = Offset(pod.left + pad + col * (large + gap) + large / 2f, pod.top + pad + row * (large + gap) + large / 2f)
        val clusterCentre = largeAt(1, 1)
        if (!withIcons) return List(count) { PodSlot(pod.center, mini * 0.6f, false) }
        return List(count) { i ->
            when {
                i < 3 -> PodSlot(largeAt(i % 2, i / 2), large, true)
                i == 3 && count <= 4 -> PodSlot(clusterCentre, large, true)
                i < 7 -> {
                    val c = (i - 3) % 2; val r = (i - 3) / 2
                    val off = (mini + miniGap) / 2f
                    PodSlot(clusterCentre + Offset(if (c == 0) -off else off, if (r == 0) -off else off), mini, true)
                }
                else -> PodSlot(clusterCentre, mini * 0.6f, false)
            }
        }
    }
}

private val LibraryEase = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
