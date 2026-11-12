package com.tms.os.launcher.ui

import android.app.ActivityOptions
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tms.os.launcher.domain.PlacedCell
import com.tms.os.launcher.domain.calculateHomeLayout
import com.tms.os.launcher.domain.packIntoPages
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.WidgetKind
import com.tms.os.launcher.ui.components.IosAppIcon
import com.tms.os.launcher.ui.components.IosAppLibraryPage
import com.tms.os.launcher.ui.components.IosLibraryCategoryView
import com.tms.os.launcher.ui.components.IosCustomizeSheet
import com.tms.os.launcher.ui.components.IosFolderIcon
import com.tms.os.launcher.ui.components.IosPageDots
import com.tms.os.launcher.ui.components.IosSearchButton
import com.tms.os.launcher.ui.home.IosSpotlight
import com.tms.os.launcher.ui.components.IosStatusBar
import com.tms.os.launcher.ui.home.AlertAction
import com.tms.os.launcher.ui.home.GridGeometry
import com.tms.os.launcher.ui.home.HomeDragState
import com.tms.os.launcher.ui.home.HomeGridPage
import com.tms.os.launcher.ui.home.Hover
import com.tms.os.launcher.ui.home.IosAlert
import com.tms.os.launcher.ui.home.IosAppSwitcher
import com.tms.os.launcher.ui.home.IosAppMenu
import com.tms.os.launcher.ui.home.IosFolderView
import com.tms.os.launcher.ui.home.IosEditTopBar
import com.tms.os.launcher.ui.home.IosWidgetGallery
import com.tms.os.launcher.ui.home.RemoveBadge
import com.tms.os.launcher.ui.home.TodayView
import com.tms.os.launcher.ui.home.distance
import com.tms.os.launcher.ui.home.homeDragGesture
import com.tms.os.launcher.ui.home.jiggle
import com.tms.os.launcher.ui.home.reorderFor
import com.tms.os.launcher.ui.home.span
import com.tms.os.launcher.ui.theme.IosMaterial
import com.tms.os.launcher.ui.theme.IosMetrics
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.LocalWallpaperBackdrop
import com.tms.os.launcher.ui.theme.WallpaperTone
import com.tms.os.launcher.ui.theme.homeStrings
import com.tms.os.launcher.ui.theme.iosStrings
import com.tms.os.launcher.ui.theme.rememberWallpaperBackdrop
import com.tms.os.launcher.ui.theme.wallpaperGlass
import com.tms.os.launcher.ui.widgets.IosWidget
import com.tms.os.launcher.ui.widgets.IosSmartStack
import com.tms.os.launcher.ui.widgets.WidgetEnv
import com.tms.os.launcher.ui.widgets.canonicalHeight
import com.tms.os.launcher.ui.widgets.canonicalWidth
import com.tms.os.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** How far the home screen is pushed "into" the display while an app is open. */
private const val IOS_ZOOM_OUT = 1.14f
/** Fast start, long gentle settle: the curve SpringBoard uses for app launch. */
private val IosLaunchEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
/** Search pill / page dots row above the dock: 10 dp + 24 dp pill + 16 dp gap. */
private val PillAreaHeight = 50.dp
/** Pull distance that fully reveals Spotlight. */
private val SpotlightPullDistance = 170.dp

@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettingsActivity: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var revision by remember { mutableStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val backdrop = rememberWallpaperBackdrop(
            IntSize(constraints.maxWidth, constraints.maxHeight), state.settings.useSystemWallpaper, revision
        )
        CompositionLocalProvider(
            LocalWallpaperBackdrop provides backdrop,
            com.tms.os.launcher.ui.theme.LocalLauncherSettings provides state.settings
        ) {
            LauncherHomeContent(viewModel, onOpenSettingsActivity)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherHomeContent(viewModel: LauncherViewModel, onOpenSettingsActivity: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val strings = remember(settings.appLanguage) { iosStrings(settings.appLanguage) }
    val hs = remember(settings.appLanguage) { homeStrings(settings.appLanguage) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<HomeGridItem?>(null) }
    var menuAnchor by remember { mutableStateOf<Pair<String, Rect>?>(null) }
    var folderBounds by remember { mutableStateOf<Rect?>(null) }
    /** Category opened from the App Library: title and its apps. */
    var openPod by remember { mutableStateOf<Pair<String, List<AppInfo>>?>(null) }
    var statusBarLightContent by remember { mutableStateOf(true) }

    val homeZoom = remember { Animatable(1f) }
    val spotlight = remember { Animatable(0f) }
    val switcher = remember { Animatable(0f) }
    var leftToApp by remember { mutableStateOf(false) }
    val drag = remember { HomeDragState() }
    // Smart Stacks scroll vertically; a pull-down that starts on one must not open Spotlight.
    val stackRects = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }

    suspend fun refreshStatusBarTone() {
        statusBarLightContent = withContext(Dispatchers.IO) { !WallpaperTone.isTopBandLight(context) }
    }
    LaunchedEffect(Unit) { refreshStatusBarTone() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> leftToApp = true
                Lifecycle.Event.ON_RESUME -> {
                    scope.launch { refreshStatusBarTone() }
                    if (leftToApp || homeZoom.value != 1f) {
                        leftToApp = false
                        scope.launch {
                            homeZoom.snapTo(IOS_ZOOM_OUT)
                            homeZoom.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 230f))
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.isSwitcherOpen) {
        switcher.animateTo(if (state.isSwitcherOpen) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 300f))
    }
    // Spotlight follows the finger; the committed open/closed state lives in the VM.
    LaunchedEffect(state.isSpotlightOpen) {
        spotlight.animateTo(if (state.isSpotlightOpen) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 320f))
    }

    val suggestions = remember(state.categorizedApps) { state.categorizedApps[AppCategory.SUGGESTIONS].orEmpty().take(16) }
    val widgetEnv = WidgetEnv(
        strings = hs,
        language = settings.appLanguage,
        dark = isSystemInDarkTheme(),
        city = settings.weatherCity,
        tempC = settings.weatherTempCelsius,
        highC = settings.weatherHighCelsius,
        lowC = settings.weatherLowCelsius,
        weatherCode = settings.weatherCode,
        condition = settings.weatherCondition,
        fahrenheit = settings.useFahrenheit,
        forecast = state.weatherForecast,
        batteryPercent = state.batteryPercent,
        note = settings.quickNoteText,
        suggestedApps = suggestions,
        onAppClick = { viewModel.launchApp(it.packageName) }
    )

    fun appForWidget(kind: WidgetKind): AppInfo? {
        val keys = when (kind) {
            WidgetKind.WEATHER -> listOf("weather")
            WidgetKind.CALENDAR -> listOf("calendar")
            WidgetKind.CLOCK -> listOf("deskclock", "clock")
            WidgetKind.NOTES -> listOf("keep", "notes", "memo")
            WidgetKind.BATTERY, WidgetKind.SUGGESTIONS -> emptyList()
        }
        return keys.firstNotNullOfOrNull { k -> state.allApps.firstOrNull { it.packageName.contains(k) } }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val isUnfolded = maxWidth >= 600.dp
        val bookMode = isUnfolded && settings.foldMode == com.tms.os.launcher.model.FoldPostureMode.BOOK_SPLIT
        val perScreen = if (bookMode) 2 else 1
        val layout = calculateHomeLayout(
            width = maxWidth.value / perScreen, height = maxHeight.value,
            columns = if (isUnfolded && !bookMode && settings.foldMode != com.tms.os.launcher.model.FoldPostureMode.CLASSIC_SCALED) settings.mainUnfoldedColumns else settings.coverColumns,
            requestedRows = if (isUnfolded && !bookMode) settings.mainUnfoldedRows else settings.coverRows,
            iconScale = settings.iconSizeScale, labelSize = settings.labelFontSizeSp,
            fontScale = density.fontScale
        )
        val columns = layout.columns
        val rows = layout.rows
        val iconSize = layout.iconSize.dp
        val sidePadding = layout.sidePadding.dp
        val dockCount = settings.dockIconCount.coerceIn(3, 6)
        val dockIconSize = minOf(iconSize, (maxWidth - 48.dp) / dockCount - 12.dp)
        val dockHeight = dockIconSize + 32.dp
        val navInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        // The dock floats above the gesture handle (28 dp from the screen edge);
        // with 3-button navigation it clears the buttons.
        val dockBottomGap = maxOf(navInset + 6.dp, 28.dp)
        val dockMargin = 14.dp
        // Search pill row + dock + gap: home pages keep clear of it, Today View
        // and App Library run underneath it to the bottom edge like on iOS.
        val chromeHeight = PillAreaHeight + dockHeight + dockBottomGap

        val displayOrder = drag.order ?: state.homeGridItems
        val pages = remember(displayOrder, columns, rows) {
            packIntoPages(displayOrder, columns, { rows }) { it.span() }
        }
        val homePageCount = ((pages.size + perScreen - 1) / perScreen).coerceAtLeast(1)
        // Pager: 0 = Today View, 1..n = home pages, n+1 = App Library.
        val pagerState = rememberPagerState(initialPage = 1, pageCount = { homePageCount + 2 })
        val libraryPage = homePageCount + 1
        val isAppLibrary = pagerState.currentPage == libraryPage
        val isToday = pagerState.currentPage == 0
        val homePageIndex = (pagerState.currentPage - 1).coerceIn(0, homePageCount - 1)

        LaunchedEffect(state.homePressGeneration) {
            if (state.homePressGeneration > 0) {
                showCustomizeSheet = false; showGallery = false; openPod = null
                if (pagerState.currentPage != 1) pagerState.animateScrollToPage(1, animationSpec = spring(dampingRatio = 1f, stiffness = 300f))
            }
        }
        BackHandler(enabled = showCustomizeSheet || pagerState.currentPage != 1 || state.isAnyOverlayOpen || state.isJiggleMode) {
            when {
                showCustomizeSheet -> showCustomizeSheet = false
                state.isAnyOverlayOpen || state.isJiggleMode -> viewModel.handleBackOrHomePress()
                else -> scope.launch { pagerState.animateScrollToPage(1) }
            }
        }

        val anyOverlay = state.isSpotlightOpen || state.activeFolder != null ||
            state.activeCategoryModal != null || state.selectedAppForMenu != null || showCustomizeSheet || showGallery || openPod != null
        val overlayBlur = animateDpAsState(
            if (state.activeFolder != null || state.activeCategoryModal != null || state.selectedAppForMenu != null || showGallery) 18.dp else 0.dp,
            tween(220), label = "overlayBlur"
        )
        val homeScale by animateFloatAsState(
            if (state.selectedAppForMenu != null) 0.985f else 1f, spring(dampingRatio = 0.9f, stiffness = 420f), label = "homeScale"
        )
        val overlayGlass = animateFloatAsState(if (anyOverlay) 1f else 0f, tween(220), label = "glass")

        /** 1 on Today View / App Library, 0 on a home page; read only in draw. */
        fun edgeFraction(): Float {
            val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
            return when {
                pos < 1f -> 1f - pos
                pos > homePageCount -> pos - homePageCount
                else -> 0f
            }.coerceIn(0f, 1f)
        }

        val edgeFractionRef = androidx.compose.runtime.rememberUpdatedState(object : () -> Float {
            override fun invoke() = edgeFraction()
        })

        val openAppWithAnimation: (AppInfo, Rect) -> Unit = { app, bounds ->
            if (!state.isJiggleMode) {
                val options = if (bounds.width > 0 && bounds.height > 0) {
                    ActivityOptions.makeScaleUpAnimation(view, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt()).toBundle()
                } else null
                leftToApp = true
                scope.launch { homeZoom.animateTo(IOS_ZOOM_OUT, tween(320, easing = IosLaunchEasing)) }
                viewModel.launchApp(app.packageName, options)
            }
        }

        fun openFolder(folder: HomeGridItem.FolderItem, bounds: Rect) {
            folderBounds = bounds
            val width = configuration.screenWidthDp.toFloat().coerceAtLeast(1f)
            val height = configuration.screenHeightDp.toFloat().coerceAtLeast(1f)
            viewModel.openFolder(
                folder,
                Offset(
                    (bounds.center.x / density.density / width).coerceIn(0.12f, 0.88f),
                    (bounds.center.y / density.density / height).coerceIn(0.12f, 0.88f)
                )
            )
        }

        val dockPkgs = drag.dockOrder ?: state.dockPackages
        val dockApps = remember(dockPkgs, state.appMap) { dockPkgs.take(dockCount).mapNotNull { state.appMap[it] } }
        var dockRect by remember { mutableStateOf(Rect.Zero) }
        val dockIconRects = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }
        var folderForDrag by remember { mutableStateOf<HomeGridItem.FolderItem?>(null) }
        val recentApps = remember(state.recentPackages, state.appMap) { state.recentPackages.mapNotNull { state.appMap[it] } }

        // ------------------------------------------------------------ wallpaper
        val backdrop = LocalWallpaperBackdrop.current
        if (!settings.useSystemWallpaper) Box(Modifier.fillMaxSize().background(Color(0xFF202132)))
        backdrop.image?.let {
            Image(it, null, contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val s = 1f + (homeZoom.value - 1f) * 0.35f
                    scaleX = s; scaleY = s
                })
        }
        backdrop.glass?.let {
            Image(it, null, contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = maxOf(overlayGlass.value, spotlight.value.coerceIn(0f, 1f), edgeFraction(), switcher.value.coerceIn(0f, 1f))
                })
        }
        // Today View and App Library also dim the wallpaper a little, like iOS.
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = maxOf(edgeFraction() * 0.62f, spotlight.value.coerceIn(0f, 1f) * 0.72f, overlayGlass.value * 0.45f, switcher.value.coerceIn(0f, 1f) * 0.3f)
        }.background(Color.Black))

        // ------------------------------------------------------ grid geometry
        var gridOrigin by remember { mutableStateOf(Offset.Zero) }
        var pageSizePx by remember { mutableStateOf(IntSize.Zero) }
        val topPad = 9.dp
        val bottomPad = 4.dp
        val geometry = remember(pageSizePx, columns, rows, iconSize, sidePadding, perScreen, chromeHeight) {
            with(density) {
                val w = pageSizePx.width.toFloat() / perScreen
                val h = (pageSizePx.height - (topPad + bottomPad + chromeHeight).toPx()).coerceAtLeast(1f)
                GridGeometry(
                    sidePx = sidePadding.toPx(),
                    cellW = (w - 2 * sidePadding.toPx()).coerceAtLeast(1f) / columns,
                    // Square cells, top-aligned: rows never stretch to fill a tall screen.
                    cellH = minOf(h / rows, (w - 2 * sidePadding.toPx()).coerceAtLeast(1f) / columns),
                    iconPx = iconSize.toPx(),
                    columns = columns,
                    rows = rows
                )
            }
        }
        val pageWidthPx = pageSizePx.width.toFloat() / perScreen
        fun logicalPageAt(pagerPage: Int, local: Offset): Pair<Int, Offset> {
            val half = if (perScreen == 2 && local.x >= pageWidthPx) 1 else 0
            return ((pagerPage - 1) * perScreen + half) to Offset(local.x - half * pageWidthPx, local.y)
        }
        fun rootOfPlaced(p: PlacedCell<HomeGridItem>): Offset? {
            val screen = p.page / perScreen + 1
            if (screen != pagerState.currentPage) return null
            val half = p.page % perScreen
            return gridOrigin + Offset(half * pageWidthPx, 0f) + geometry.frameOffset(p)
        }
        fun placedOf(id: String): PlacedCell<HomeGridItem>? = pages.asSequence().flatten().firstOrNull { it.item.id == id }

        // ------------------------------------------------------ drag callbacks
        val edgePx = with(density) { 22.dp.toPx() }
        val screenWPx = with(density) { screenW.toPx() }

        fun dragStart(item: HomeGridItem, downAt: Offset, pointer: Offset) {
            if (!state.isJiggleMode) {
                viewModel.selectAppForContextMenu(null)
                viewModel.setJiggleMode(true)
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val p = placedOf(item.id) ?: return
            val topLeft = rootOfPlaced(p) ?: return
            // Grab point = where the finger first touched the icon, so the icon
            // stays under the finger however far the first move event jumps.
            drag.grab = downAt - topLeft
            drag.pointer = pointer
            drag.order = state.homeGridItems
            drag.item = item
        }

        /** Drag that starts in the dock or in an open folder; [iconTopLeft] is the icon image's corner. */
        fun externalDragStart(pkg: String, iconTopLeft: Offset, downAt: Offset, pointer: Offset) {
            if (!state.isJiggleMode) {
                viewModel.selectAppForContextMenu(null)
                viewModel.setJiggleMode(true)
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val item = HomeGridItem.AppItem(pkg)
            // The carried visual is a grid cell with the icon centred at the top.
            val frameTopLeft = iconTopLeft - Offset((geometry.cellW - geometry.iconPx) / 2f, 0f)
            drag.grab = downAt - frameTopLeft
            drag.pointer = pointer
            drag.external = true
            drag.order = state.homeGridItems.filterNot { it.id == item.id }
            drag.dockOrder = state.dockPackages.filterNot { it == pkg }
            drag.item = item
        }

        fun dragMove(pointer: Offset) {
            val item = drag.item ?: return
            drag.pointer = pointer
            // Over the dock: apps slot into it (if there is room), the grid closes its gap.
            if (item is HomeGridItem.AppItem && dockRect.contains(pointer)) {
                val pkg = item.packageName
                val base = (drag.dockOrder ?: state.dockPackages).filterNot { it == pkg }
                drag.overDock = true
                drag.edge = 0
                drag.hover = null
                drag.folderTarget = null
                // The grid keeps the item until the drop: removing it now would dispose
                // the composable that owns this very gesture.
                if (base.size < dockCount) {
                    val slot = ((pointer.x - dockRect.left) / (dockRect.width / (base.size + 1))).toInt().coerceIn(0, base.size)
                    val next = base.toMutableList().apply { add(slot, pkg) }
                    if (next != drag.dockOrder) drag.dockOrder = next
                } else drag.dockOrder = base
                return
            } else if (drag.overDock) {
                drag.overDock = false
                if (item is HomeGridItem.AppItem) drag.dockOrder = drag.dockOrder?.filterNot { it == item.packageName }
            }
            drag.edge = when {
                pointer.x < edgePx -> -1
                pointer.x > screenWPx - edgePx -> 1
                else -> 0
            }
            val topLeft = pointer - drag.grab - gridOrigin
            val probe = if (item.span().cols == 1) topLeft + Offset(geometry.cellW / 2f, geometry.iconPx / 2f)
            else topLeft + Offset(geometry.iconPx / 2f, geometry.iconPx / 2f)
            val (logical, local) = logicalPageAt(pagerState.currentPage, probe)
            val (c, r) = geometry.cellAt(local)
            val over = pages.getOrNull(logical)?.firstOrNull {
                c >= it.col && c < it.col + it.span.cols && r >= it.row && r < it.row + it.span.rows
            }
            val folder = over != null && over.item.id != item.id && when {
                item is HomeGridItem.AppItem ->
                    (over.item is HomeGridItem.AppItem || over.item is HomeGridItem.FolderItem) &&
                        distance(local, geometry.iconCenter(over)) < geometry.iconPx * 0.36f
                // Widget on a same-size widget, near its centre: Smart Stack.
                item is HomeGridItem.WidgetItem && over.item is HomeGridItem.WidgetItem && (over.item as HomeGridItem.WidgetItem).size == item.size -> {
                    val o = geometry.frameOffset(over)
                    val centre = o + Offset(geometry.frameWidth(over) / 2f, geometry.frameHeight(over) / 2f)
                    val probeCentre = local - Offset(geometry.iconPx / 2f, geometry.iconPx / 2f) + Offset(geometry.frameWidth(over) / 2f, geometry.frameHeight(over) / 2f)
                    distance(probeCentre, centre) < geometry.frameWidth(over) * 0.3f
                }
                else -> false
            }
            val h = Hover(logical, c, r, over?.item?.id, folder)
            if (h != drag.hover) {
                if (!(folder && drag.folderTarget == over?.item?.id)) drag.folderTarget = null
                drag.hover = h
            }
        }

        fun dragEnd() {
            val item = drag.item ?: return
            val folderTarget = drag.folderTarget
            var finalOrder = drag.order ?: state.homeGridItems
            drag.edge = 0
            val pkg = (item as? HomeGridItem.AppItem)?.packageName
            val intoDock = pkg != null && drag.overDock && drag.dockOrder?.contains(pkg) == true
            if (intoDock) {
                viewModel.setDockOrder(drag.dockOrder!!)
                viewModel.commitHomeOrder(finalOrder.filterNot { it.id == item.id })
                folderForDrag = null
                drag.clear()
                return
            }
            // Never lose an app: if it never reached a grid slot, it goes to the end of this page.
            if (folderTarget == null && finalOrder.none { it.id == item.id }) {
                finalOrder = reorderFor(finalOrder + item, item, Hover(homePageIndex * perScreen, columns - 1, rows - 1, null, false), pages)
            }
            if (pkg != null && state.dockPackages.contains(pkg)) viewModel.setDockOrder(state.dockPackages - pkg)
            folderForDrag = null
            scope.launch {
                drag.dropping = true
                drag.dropAnim.snapTo(drag.pointer - drag.grab)
                if (folderTarget != null) {
                    val target = placedOf(folderTarget)?.let { rootOfPlaced(it) }
                    launch { animateFloat(1f, 0.4f) { drag.dropScale = it } }
                    if (target != null) drag.dropAnim.animateTo(target, spring(0.85f, 420f))
                    if (item is HomeGridItem.WidgetItem) viewModel.mergeWidgets(item.id, folderTarget)
                    else viewModel.mergeIntoFolder(item.id, folderTarget)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    viewModel.commitHomeOrder(finalOrder)
                    val finalPages = packIntoPages(finalOrder, columns, { rows }) { it.span() }
                    val p = finalPages.asSequence().flatten().firstOrNull { it.item.id == item.id }
                    val target = p?.let { rootOfPlaced(it) }
                    if (target != null) drag.dropAnim.animateTo(target, spring(0.78f, 380f))
                }
                drag.clear()
            }
        }

        // Gesture callbacks outlive compositions (and Compose memoises lambdas), so
        // they must always reach the *current* functions with current geometry.
        // Anonymous objects on purpose: the Compose compiler memoises lambdas and
        // references to local functions, which froze the first (empty) geometry.
        val dragStartRef = androidx.compose.runtime.rememberUpdatedState(object : (HomeGridItem, Offset, Offset) -> Unit {
            override fun invoke(i: HomeGridItem, d: Offset, p: Offset) = dragStart(i, d, p)
        })
        val externalStartRef = androidx.compose.runtime.rememberUpdatedState(object : (String, Offset, Offset, Offset) -> Unit {
            override fun invoke(pkg: String, t: Offset, d: Offset, p: Offset) = externalDragStart(pkg, t, d, p)
        })
        val dragMoveRef = androidx.compose.runtime.rememberUpdatedState(object : (Offset) -> Unit {
            override fun invoke(p: Offset) = dragMove(p)
        })
        val dragEndRef = androidx.compose.runtime.rememberUpdatedState(object : () -> Unit {
            override fun invoke() = dragEnd()
        })
        val onMove: (Offset) -> Unit = remember { { p -> dragMoveRef.value(p) } }
        val onEnd: () -> Unit = remember { { dragEndRef.value() } }

        // Dwell before reflowing (iOS waits a beat), longer dwell to form a folder.
        LaunchedEffect(drag.hover) {
            val h = drag.hover ?: return@LaunchedEffect
            val item = drag.item ?: return@LaunchedEffect
            if (h.overId == item.id) return@LaunchedEffect
            if (h.folder) {
                delay(420)
                drag.folderTarget = h.overId
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                delay(140)
                val order = drag.order ?: return@LaunchedEffect
                val next = reorderFor(order, item, h, pages)
                if (next != order) drag.order = next
            }
        }
        // Hold at the screen edge to carry the item to the neighbouring page.
        LaunchedEffect(drag.edge, drag.item) {
            if (drag.item == null || drag.edge == 0) return@LaunchedEffect
            while (true) {
                delay(620)
                val target = (pagerState.currentPage + drag.edge).coerceIn(1, homePageCount)
                if (target == pagerState.currentPage) break
                pagerState.animateScrollToPage(target, animationSpec = spring(dampingRatio = 1f, stiffness = 260f))
                drag.hover = Hover((target - 1) * perScreen, 0, 0, null, false)
                delay(300)
            }
        }

        // --------------------------------------------------------- root gesture
        val pullPx = with(density) { SpotlightPullDistance.toPx() }
        val chromeHeightPx = with(density) { chromeHeight.toPx() }
        val shadeZonePx = with(density) { 52.dp.toPx() }
        val homeInteractive = !anyOverlay && !state.isJiggleMode && !isAppLibrary && !isToday && !drag.active
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(homeInteractive, settings.enableSwipeDownSpotlight) {
                    if (!homeInteractive) return@pointerInput
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var pulling = false
                        var lifting = false
                        val onStack = stackRects.values.any { it.contains(down.position) }
                        val fromDock = down.position.y > size.height - chromeHeightPx
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            tracker.addPosition(c.uptimeMillis, c.position)
                            val dx = c.position.x - down.position.x
                            val dy = c.position.y - down.position.y
                            if (!c.pressed) {
                                if (lifting) {
                                    val v = tracker.calculateVelocity().y
                                    // iOS: swipe up and pause (or a deliberate drag) opens the switcher.
                                    if (switcher.value > 0.3f || v < -900f) {
                                        if (!viewModel.openSwitcher()) scope.launch { switcher.animateTo(0f, spring(0.9f, 400f)) }
                                    } else scope.launch { switcher.animateTo(0f, spring(0.9f, 400f)) }
                                }
                                if (pulling) {
                                    val v = tracker.calculateVelocity().y
                                    if (spotlight.value > 0.35f || v > 1200f) viewModel.openSpotlight()
                                    else scope.launch { spotlight.animateTo(0f, spring(0.9f, 400f)) }
                                }
                                break
                            }
                            // iOS: hold first, then move = pick up the icon. Leave such
                            // gestures to the icon/drag handlers instead of Spotlight/switcher.
                            if (!pulling && !lifting && c.uptimeMillis - down.uptimeMillis > viewConfiguration.longPressTimeoutMillis) break
                            if (!pulling && !lifting && fromDock && -dy > slop && -dy > abs(dx) * 1.3f) {
                                lifting = true
                                viewModel.prepareSwitcher()
                            }
                            if (lifting) {
                                c.consume()
                                scope.launch { switcher.snapTo(((-dy - slop) / pullPx).coerceIn(0f, 1.15f)) }
                                continue
                            }
                            if (!pulling) {
                                if (dy > slop && dy > abs(dx) * 1.3f) {
                                    if (down.position.y < shadeZonePx) {
                                        viewModel.expandStatusBar(); c.consume(); break
                                    }
                                    if (!settings.enableSwipeDownSpotlight || onStack) break
                                    pulling = true
                                } else if (abs(dx) > slop || dy < -slop) break
                            }
                            if (pulling) {
                                c.consume()
                                // Rubber-band past the full reveal, like a UIScrollView.
                                val raw = (dy - slop) / pullPx
                                val p = if (raw <= 1f) raw else 1f + (raw - 1f) * 0.25f
                                scope.launch { spotlight.snapTo(p.coerceAtLeast(0f)) }
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val z = homeZoom.value
                        val sw = switcher.value.coerceIn(0f, 1f)
                        scaleX = homeScale * z * (1f - 0.08f * sw)
                        scaleY = homeScale * z * (1f - 0.08f * sw)
                        alpha = (1f - ((z - 1f) / (IOS_ZOOM_OUT - 1f)).coerceIn(0f, 1f)) * (1f - sw)
                    }
            ) {
                Spacer(Modifier.height(IosMetrics.statusBarHeight))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val sp = spotlight.value.coerceIn(0f, 1f)
                            val r = max(overlayBlur.value.toPx(), sp * 22.dp.toPx())
                            renderEffect = if (r > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurEffect(r, r, TileMode.Decal) else null
                            alpha = 1f - sp
                            val s = 1f - sp * 0.03f
                            scaleX = s; scaleY = s
                        }
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = !anyOverlay && !drag.active,
                            flingBehavior = PagerDefaults.flingBehavior(state = pagerState, pagerSnapDistance = PagerSnapDistance.atMost(1)),
                            beyondViewportPageCount = 1,
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned {
                                    pageSizePx = it.size
                                    gridOrigin = it.boundsInRoot().topLeft + Offset(0f, with(density) { topPad.toPx() })
                                }
                        ) { pageIndex ->
                            when (pageIndex) {
                                0 -> TodayView(
                                    widgets = state.todayWidgets,
                                    env = widgetEnv,
                                    strings = hs,
                                    jiggle = state.isJiggleMode,
                                    onWidgetTap = { w -> appForWidget(w.kind)?.let { viewModel.launchApp(it.packageName) } },
                                    onRemove = { removeTarget = it },
                                    onEnterJiggle = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setJiggleMode(true) },
                                    onExitJiggle = { viewModel.setJiggleMode(false) }
                                )
                                libraryPage -> IosAppLibraryPage(
                                    allApps = state.allApps,
                                    categorizedApps = state.categorizedApps,
                                    settings = settings,
                                    strings = strings,
                                    isUnfolded = isUnfolded,
                                    hidden = openPod != null,
                                    onAppClick = { app, bounds -> openAppWithAnimation(app, bounds) },
                                    onAppLongClick = { app, bounds -> menuAnchor = app.packageName to bounds; viewModel.selectAppForContextMenu(app) },
                                    onOpenPod = { title, apps -> openPod = title to apps }
                                )
                                else -> Row(
                                    Modifier
                                        .fillMaxSize()
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { if (state.isJiggleMode) viewModel.setJiggleMode(false) },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.setJiggleMode(true)
                                            }
                                        )
                                        .padding(top = topPad, bottom = bottomPad + chromeHeight)
                                ) {
                                    for (half in 0 until perScreen) {
                                        val logical = (pageIndex - 1) * perScreen + half
                                        HomeGridPage(
                                            placed = pages.getOrNull(logical).orEmpty(),
                                            geometry = geometry,
                                            hiddenId = drag.item?.id,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        ) { p, w, h ->
                                            GridItem(
                                                p = p, w = w, h = h,
                                                state = state,
                                                iconSize = iconSize,
                                                labelSize = settings.labelFontSizeSp,
                                                showLabels = settings.showAppLabels,
                                                showBadges = settings.enableBadges,
                                                jiggle = state.isJiggleMode,
                                                folderTarget = drag.folderTarget == p.item.id,
                                                env = widgetEnv,
                                                widgetLabel = (p.item as? HomeGridItem.WidgetItem)?.let { hs.widgetName(it.kind) },
                                                dragModifier = Modifier.homeDragGesture(
                                                    key = p.item.id,
                                                    jiggle = state.isJiggleMode,
                                                    enabled = !anyOverlay || state.selectedAppForMenu != null,
                                                    onStart = { downAt, at -> dragStartRef.value(p.item, downAt, at) },
                                                    onMove = onMove,
                                                    onEnd = onEnd
                                                ),
                                                onAppClick = openAppWithAnimation,
                                                onAppLongClick = { app, bounds -> menuAnchor = app.packageName to bounds; viewModel.selectAppForContextMenu(app) },
                                                onFolderClick = { f, b -> openFolder(f, b) },
                                                onWidgetTap = { k -> appForWidget(k)?.let { viewModel.launchApp(it.packageName) } },
                                                onEnterJiggle = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setJiggleMode(true)
                                                },
                                                onRemove = { removeTarget = p.item },
                                                onStackBounds = { id, r -> if (r == null) stackRects.remove(id) else stackRects[id] = r }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeChrome(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        dockModifier = Modifier
                            .onGloballyPositioned { dockRect = it.boundsInRoot() }
                            .homeDragGesture(
                                key = "dock",
                                jiggle = state.isJiggleMode,
                                enabled = !anyOverlay || state.selectedAppForMenu != null,
                                onStart = { downAt, at ->
                                    dockIconRects.entries.firstOrNull { it.value.contains(downAt) }?.let { (pkg, r) ->
                                        externalStartRef.value(pkg, r.topLeft, downAt, at)
                                    }
                                },
                                onMove = onMove,
                                onEnd = onEnd
                            ),
                        onDockIconBounds = { pkg, r -> dockIconRects[pkg] = r },
                        hiddenPkg = (drag.item as? HomeGridItem.AppItem)?.packageName?.takeIf { drag.active && !drag.dropping },
                        bottomGap = dockBottomGap,
                        dockMargin = dockMargin,
                        // Dock icons stand in the grid columns when the counts match.
                        slotWidth = if (!isUnfolded && dockCount == columns) with(density) { geometry.cellW.toDp() } else Dp.Unspecified,
                        pageCount = homePageCount,
                        currentPage = homePageIndex,
                        strings = strings,
                        showSearch = !pagerState.isScrollInProgress && !state.isJiggleMode && !drag.active,
                        dockApps = dockApps,
                        recentApps = recentApps,
                        isUnfolded = isUnfolded,
                        showRecent = settings.showRecentInUnfoldedDock,
                        showBadges = settings.enableBadges,
                        iconSize = dockIconSize,
                        dockHeight = dockHeight,
                        glassOpacity = settings.glassOpacity,
                        isJiggleMode = state.isJiggleMode,
                        fade = edgeFractionRef.value,
                        onSearch = { viewModel.openSpotlight() },
                        onAppClick = openAppWithAnimation,
                        onAppLongClick = { app, bounds -> menuAnchor = app.packageName to bounds; viewModel.selectAppForContextMenu(app) }
                    )
                    }
                }
            }
        }

        // ------------------------------------------------- carried item (drag)
        drag.item?.let { item ->
            val p = placedOf(item.id) ?: PlacedCell(item, 0, 0, 0, item.span())
            run {
                val w = with(density) { geometry.frameWidth(p).toDp() }
                val h = with(density) { geometry.frameHeight(p).toDp() }
                val lift by animateFloatAsState(if (drag.dropping) 1f else 1.12f, spring(0.7f, 500f), label = "lift")
                Box(
                    Modifier
                        .zIndex(50f)
                        .offset {
                            val o = if (drag.dropping) drag.dropAnim.value else drag.pointer - drag.grab
                            IntOffset(o.x.roundToInt(), o.y.roundToInt())
                        }
                        .size(w, h)
                        .graphicsLayer {
                            val s = lift * drag.dropScale
                            scaleX = s; scaleY = s
                            alpha = if (drag.dropScale < 1f) drag.dropScale else 1f
                        }
                ) {
                    GridItem(
                        p = p, w = w, h = h, state = state, iconSize = iconSize,
                        labelSize = settings.labelFontSizeSp, showLabels = false, showBadges = false,
                        jiggle = false, folderTarget = false, env = widgetEnv, widgetLabel = null,
                        dragModifier = Modifier, onAppClick = { _, _ -> }, onAppLongClick = { _, _ -> },
                        onFolderClick = { _, _ -> }, onWidgetTap = {}, onEnterJiggle = {}, onRemove = {}, onStackBounds = { _, _ -> }
                    )
                }
            }
        }

        // ---------------------------------------------------------- overlays
        if (state.isSwitcherOpen || switcher.value > 0.002f) {
            IosAppSwitcher(
                apps = state.switcherApps,
                progress = { switcher.value },
                onOpen = { app ->
                    leftToApp = true
                    val opts = ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    viewModel.launchApp(app.packageName, opts)
                    viewModel.closeSwitcher()
                    scope.launch { switcher.snapTo(0f) }
                },
                onDismissApp = { app -> viewModel.dismissFromSwitcher(app.packageName) },
                onClose = { viewModel.closeSwitcher() }
            )
        }
        IosSpotlight(
            visible = state.isSpotlightOpen,
            progress = { spotlight.value },
            query = state.spotlightQuery,
            results = state.spotlightResults,
            contacts = state.spotlightContacts,
            mathResult = state.spotlightMathResult,
            history = remember(state.spotlightHistory, state.appMap) { state.spotlightHistory.mapNotNull { state.appMap[it] } },
            settings = settings,
            strings = strings,
            hs = hs,
            onQueryChange = viewModel::updateSpotlightQuery,
            onAppClick = { app -> leftToApp = true; viewModel.launchFromSpotlight(app.packageName) },
            onAppLongClick = { app -> viewModel.selectAppForContextMenu(app) },
            onClearHistory = { viewModel.clearSpotlightHistory() },
            onContactCall = { contact -> viewModel.dialContact(contact.phoneNumber) },
            onContactMessage = { contact -> viewModel.messageContact(contact.phoneNumber) },
            onWebSearch = { query, engine -> viewModel.searchWeb(query, engine) },
            onClose = { viewModel.closeSpotlight() }
        )

        (folderForDrag ?: state.activeFolder)?.let { folder ->
            IosFolderView(
                folder = folder,
                hidden = folderForDrag != null,
                onDragOutStart = { app, iconTopLeft, downAt, at ->
                    folderForDrag = folder
                    viewModel.takeAppOutOfFolder(folder.folderId, app.packageName)
                    externalStartRef.value(app.packageName, iconTopLeft, downAt, at)
                },
                onDragMove = onMove,
                onDragEnd = onEnd,
                appMap = state.appMap,
                iconBounds = folderBounds,
                iconSize = if (isUnfolded) 64.dp else minOf(iconSize, 62.dp),
                labelFontSizeSp = settings.labelFontSizeSp,
                jiggle = state.isJiggleMode,
                onAppClick = { app, bounds -> openAppWithAnimation(app, bounds) },
                onAppLongClick = { app, bounds -> menuAnchor = app.packageName to bounds; viewModel.selectAppForContextMenu(app) },
                onRename = { name -> viewModel.renameFolder(folder.folderId, name) },
                onEnterJiggle = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setJiggleMode(true) },
                onDismiss = { viewModel.openFolder(null) }
            )
        }

        // App Library category: full screen, title + grid, like iOS.
        openPod?.let { (title, apps) ->
            IosLibraryCategoryView(
                title = title,
                apps = apps,
                columns = columns,
                sidePadding = sidePadding,
                iconSize = iconSize,
                labelFontSizeSp = settings.labelFontSizeSp,
                onAppClick = { app, bounds -> openAppWithAnimation(app, bounds) },
                onAppLongClick = { app, bounds -> menuAnchor = app.packageName to bounds; viewModel.selectAppForContextMenu(app) },
                onDismiss = { openPod = null }
            )
        }

        if (!drag.active) state.selectedAppForMenu?.let { app ->
            val folders = remember(state.homeGridItems) { state.homeGridItems.filterIsInstance<HomeGridItem.FolderItem>() }
            IosAppMenu(
                app = app,
                anchor = menuAnchor?.takeIf { it.first == app.packageName }?.second,
                iconSize = iconSize,
                strings = strings,
                hs = hs,
                language = settings.appLanguage,
                isPinnedInDock = state.dockPackages.contains(app.packageName),
                folders = folders,
                quickActions = state.menuQuickActions,
                onQuickAction = { a -> leftToApp = true; viewModel.runQuickAction(a) },
                onEditHome = { viewModel.setJiggleMode(true) },
                onShare = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${app.packageName}")
                    }
                    runCatching { context.startActivity(android.content.Intent.createChooser(send, app.displayLabel).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                onRemove = { removeTarget = HomeGridItem.AppItem(app.packageName) },
                onToggleDock = { viewModel.togglePinToDock(app.packageName) },
                onAppInfo = { viewModel.openAppInfo(app.packageName) },
                onHide = { viewModel.hideOrUnhideApp(app.packageName, true) },
                onRename = { name -> viewModel.renameApp(app.packageName, name) },
                onChangeCategory = { cat -> viewModel.changeAppCategory(app.packageName, cat) },
                onMoveToFolder = { folderId -> viewModel.moveAppIntoFolder(app.packageName, folderId) },
                onCreateFolder = { title -> viewModel.createNewFolderWithApp(app.packageName, title) },
                onLaunch = { viewModel.launchApp(app.packageName) },
                onDismiss = { viewModel.selectAppForContextMenu(null) }
            )
        }

        IosCustomizeSheet(
            visible = showCustomizeSheet,
            settings = settings,
            strings = strings,
            onSettingsChange = { transform -> viewModel.updateSettings(transform) },
            onSmartOrganize = { viewModel.autoOrganizeHomeWithSmartFolders() },
            onOpenSettings = onOpenSettingsActivity,
            onDismiss = { showCustomizeSheet = false }
        )

        IosWidgetGallery(
            visible = showGallery,
            env = widgetEnv,
            strings = hs,
            onAddStack = { kinds, size ->
                showGallery = false
                if (isToday) viewModel.addWidget(kinds.first(), size, toToday = true, index = 0, stack = kinds)
                else {
                    val first = pages.getOrNull(homePageIndex * perScreen)?.firstOrNull()?.item
                    val index = first?.let { f -> state.homeGridItems.indexOfFirst { it.id == f.id } } ?: state.homeGridItems.size
                    viewModel.addWidget(kinds.first(), size, toToday = false, index = index.coerceAtLeast(0), stack = kinds)
                }
            },
            onAdd = { kind, size ->
                showGallery = false
                if (isToday) viewModel.addWidget(kind, size, toToday = true, index = 0)
                else {
                    val first = pages.getOrNull(homePageIndex * perScreen)?.firstOrNull()?.item
                    val index = first?.let { f -> state.homeGridItems.indexOfFirst { it.id == f.id } } ?: state.homeGridItems.size
                    viewModel.addWidget(kind, size, toToday = false, index = index.coerceAtLeast(0))
                }
            },
            onDismiss = { showGallery = false }
        )

        // ----------------------------------------------------- removal alert
        val target = removeTarget
        val (alertTitle, alertMessage, alertActions) = when (target) {
            is HomeGridItem.AppItem -> {
                val label = state.appMap[target.packageName]?.displayLabel ?: ""
                Triple(hs.removeAppTitle.format(label), hs.removeAppMessage, listOf(
                    AlertAction(hs.deleteApp, destructive = true) { removeTarget = null; viewModel.uninstallApp(target.packageName) },
                    AlertAction(hs.removeFromHome) { removeTarget = null; viewModel.removeAppFromHome(target.packageName) },
                    AlertAction(hs.cancel, cancel = true) { removeTarget = null }
                ))
            }
            is HomeGridItem.FolderItem -> Triple(hs.removeFolderTitle.format(target.title), hs.removeFolderMessage, listOf(
                AlertAction(hs.removeFromHome, destructive = true) { removeTarget = null; viewModel.removeFolderFromHome(target.folderId) },
                AlertAction(hs.cancel, cancel = true) { removeTarget = null }
            ))
            is HomeGridItem.WidgetItem -> Triple(hs.removeWidgetTitle.format(hs.widgetName(target.kind)), hs.removeWidgetMessage, listOf(
                AlertAction(hs.cancel, cancel = true) { removeTarget = null },
                AlertAction(hs.remove, destructive = true) { removeTarget = null; viewModel.removeWidget(target.widgetId) }
            ))
            null -> Triple("", "", emptyList())
        }
        IosAlert(visible = target != null, title = alertTitle, message = alertMessage, actions = alertActions, onDismiss = { removeTarget = null })

        // Status bar on top of everything; in edit mode the Edit/Done pills take its place.
        val statusAlpha by animateFloatAsState(if (state.isJiggleMode && !isAppLibrary) 0f else 1f, tween(180), label = "status")
        IosStatusBar(
            lightContent = if (settings.useSystemWallpaper) statusBarLightContent else true,
            language = settings.appLanguage,
            modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { alpha = statusAlpha }
        )
        IosEditTopBar(
            visible = state.isJiggleMode && !isAppLibrary && !showGallery,
            strings = hs,
            onAddWidget = { showGallery = true },
            onCustomize = { showCustomizeSheet = true },
            onDone = { viewModel.setJiggleMode(false) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        )
    }
}

private suspend fun animateFloat(from: Float, to: Float, onValue: (Float) -> Unit) {
    val a = Animatable(from)
    a.animateTo(to, tween(220)) { onValue(value) }
}

/** One grid cell: app icon, folder or widget, wired for tap, menu and drag. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridItem(
    p: PlacedCell<HomeGridItem>,
    w: Dp,
    h: Dp,
    state: com.tms.os.launcher.viewmodel.LauncherUiState,
    iconSize: Dp,
    labelSize: Float,
    showLabels: Boolean,
    showBadges: Boolean,
    jiggle: Boolean,
    folderTarget: Boolean,
    env: WidgetEnv,
    widgetLabel: String?,
    dragModifier: Modifier,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onFolderClick: (HomeGridItem.FolderItem, Rect) -> Unit,
    onWidgetTap: (WidgetKind) -> Unit,
    onEnterJiggle: () -> Unit,
    onRemove: () -> Unit,
    onStackBounds: (String, Rect?) -> Unit
) {
    when (val item = p.item) {
        is HomeGridItem.AppItem -> state.appMap[item.packageName]?.let { app ->
            Box(Modifier.size(w, h).then(dragModifier), contentAlignment = Alignment.TopCenter) {
                IosAppIcon(
                    app = app,
                    iconSize = iconSize,
                    labelFontSizeSp = labelSize,
                    showLabel = showLabels,
                    showBadges = showBadges,
                    isJiggleMode = jiggle,
                    isHighlightedTarget = folderTarget,
                    onClick = { onAppClick(app, Rect.Zero) },
                    onClickWithBounds = { b -> onAppClick(app, b) },
                    onLongClick = { onAppLongClick(app, Rect.Zero) },
                    onLongClickWithBounds = { b -> if (!jiggle) onAppLongClick(app, b) },
                    onDeleteOrMenuClick = onRemove
                )
            }
        }
        is HomeGridItem.FolderItem -> Box(Modifier.size(w, h).then(dragModifier), contentAlignment = Alignment.TopCenter) {
            IosFolderIcon(
                folder = item,
                appMap = state.appMap,
                iconSize = iconSize,
                labelFontSizeSp = labelSize,
                showLabel = showLabels,
                showBadges = showBadges,
                isJiggleMode = jiggle,
                isHighlightedTarget = folderTarget,
                onClickWithBounds = { b -> onFolderClick(item, b) },
                onLongClick = onEnterJiggle,
                onRemove = onRemove
            )
        }
        is HomeGridItem.WidgetItem -> {
            // iOS widgets keep Apple's aspect ratio and hang from the top of their
            // cells; the name sits 6 pt under the widget, like an icon label.
            val wh = minOf(h, w * (item.size.canonicalHeight() / item.size.canonicalWidth()))
            var current by remember(item.widgetId) { mutableStateOf(item.kinds.first()) }
            val hoverScale by animateFloatAsState(if (folderTarget) 1.05f else 1f, spring(0.7f, 500f), label = "stackHover")
            DisposableEffect(item.widgetId) { onDispose { onStackBounds(item.widgetId, null) } }
            Box(Modifier.size(w, h).then(dragModifier)) {
                Box(Modifier.size(w, wh).jiggle(jiggle, item.widgetId.hashCode(), 0.7f).graphicsLayer { scaleX = hoverScale; scaleY = hoverScale }) {
                    val surface = Modifier
                        .fillMaxSize()
                        .shadow(6.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.18f))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (!jiggle) onWidgetTap(current) },
                            onLongClick = { if (!jiggle) onEnterJiggle() }
                        )
                    if (item.isStack) IosSmartStack(
                        item.kinds, item.size, env,
                        surface.onGloballyPositioned { onStackBounds(item.widgetId, it.boundsInRoot()) },
                        onCurrent = { current = it }
                    ) else IosWidget(item.kind, item.size, env, surface)
                    RemoveBadge(jiggle, Modifier.offset((-8).dp, (-8).dp), onClick = onRemove)
                }
                if (widgetLabel != null && showLabels) {
                    Text(
                        if (item.isStack) env.strings.widgetName(current) else widgetLabel,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = IosText.iconLabel.copy(fontSize = labelSize.sp, shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.32f), Offset(0f, 1f), 2.5f)),
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = wh + 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeChrome(
    modifier: Modifier,
    dockModifier: Modifier,
    onDockIconBounds: (String, Rect) -> Unit,
    hiddenPkg: String?,
    bottomGap: Dp,
    dockMargin: Dp,
    slotWidth: Dp,
    pageCount: Int,
    currentPage: Int,
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    showSearch: Boolean,
    dockApps: List<AppInfo>,
    recentApps: List<AppInfo>,
    isUnfolded: Boolean,
    showRecent: Boolean,
    showBadges: Boolean,
    iconSize: Dp,
    dockHeight: Dp,
    glassOpacity: Float,
    isJiggleMode: Boolean,
    fade: () -> Float,
    onSearch: () -> Unit,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Dock and search pill slide away on Today View and App Library.
                val f = fade()
                alpha = 1f - f
                translationY = f * 60.dp.toPx()
            }
            .padding(bottom = bottomGap)
    ) {
        Box(Modifier.height(PillAreaHeight).padding(top = 10.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.Crossfade(targetState = showSearch, animationSpec = tween(160), label = "pageControl") { search ->
                if (search) IosSearchButton(text = strings.search, onClick = onSearch)
                else IosPageDots(count = pageCount, selected = currentPage)
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = dockMargin), contentAlignment = Alignment.Center) {
            val dockWidth = if (isUnfolded) maxWidth * 0.84f else maxWidth
            val slot = if (slotWidth != Dp.Unspecified) minOf(slotWidth, dockWidth / dockApps.size.coerceAtLeast(1)) else Dp.Unspecified
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(dockWidth)
                    .height(dockHeight)
                    .wallpaperGlass(RoundedCornerShape(28.dp), tint = IosMaterial.dock.copy(alpha = 0.45f + glassOpacity.coerceIn(0f, 1f) * 0.26f))
                    .then(dockModifier)
            ) {
                // LazyRow only for animateItem(): dock icons slide aside when an app is dragged in.
                androidx.compose.foundation.lazy.LazyRow(
                    userScrollEnabled = false,
                    horizontalArrangement = if (slot != Dp.Unspecified) Arrangement.Center else Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    items(dockApps.size, key = { dockApps[it].packageName }) { i ->
                        val app = dockApps[i]
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .animateItem(fadeInSpec = tween(150), placementSpec = spring(dampingRatio = 0.8f, stiffness = 380f), fadeOutSpec = tween(120))
                                .then(if (slot != Dp.Unspecified) Modifier.width(slot) else Modifier)
                        ) {
                            Box(
                                Modifier
                                    .onGloballyPositioned { onDockIconBounds(app.packageName, it.boundsInRoot()) }
                                    .graphicsLayer { alpha = if (app.packageName == hiddenPkg) 0f else 1f }
                            ) {
                                IosAppIcon(
                                    app = app,
                                    iconSize = iconSize,
                                    showLabel = false,
                                    showBadges = showBadges,
                                    isJiggleMode = isJiggleMode,
                                    onClick = { onAppClick(app, Rect.Zero) },
                                    onClickWithBounds = { bounds -> onAppClick(app, bounds) },
                                    onLongClick = { onAppLongClick(app, Rect.Zero) },
                                    onLongClickWithBounds = { if (!isJiggleMode) onAppLongClick(app, it) }
                                )
                            }
                        }
                    }
                }
                if (isUnfolded && showRecent && recentApps.isNotEmpty()) {
                    Box(Modifier.width(1.dp).height(iconSize * 0.55f).background(Color.White.copy(alpha = 0.18f)))
                    recentApps.filter { recent -> dockApps.none { it.packageName == recent.packageName } }.take(2).forEach { app ->
                        IosAppIcon(
                            app = app,
                            iconSize = iconSize,
                            showLabel = false,
                            showBadges = showBadges,
                            isJiggleMode = false,
                            onClick = { onAppClick(app, Rect.Zero) },
                            onClickWithBounds = { bounds -> onAppClick(app, bounds) },
                            onLongClick = { onAppLongClick(app, Rect.Zero) },
                            onLongClickWithBounds = { onAppLongClick(app, it) }
                        )
                    }
                }
            }
        }
    }
}
