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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.drawWithContent
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
import com.tms.os.launcher.domain.packAnchored
import com.tms.os.launcher.domain.moveAnchored
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
import com.tms.os.launcher.ui.home.AlertAction
import com.tms.os.launcher.ui.home.GridGeometry
import com.tms.os.launcher.ui.home.HomeDragState
import com.tms.os.launcher.ui.home.FolderDragSession
import com.tms.os.launcher.ui.home.MinimalDragState
import com.tms.os.launcher.ui.home.MinimalEdit
import com.tms.os.launcher.ui.home.FolderSlots
import com.tms.os.launcher.ui.home.withoutFolderApp
import com.tms.os.launcher.ui.home.HomeGridPage
import com.tms.os.launcher.ui.home.Hover
import com.tms.os.launcher.ui.home.IosAlert
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
// Gentle start, long settle: the home screen eases away under the opening app
// instead of vanishing within the first two frames.
private val IosLaunchEasing = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)
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
    var hiddenOrigin by remember { mutableStateOf<Rect?>(null) }
    var showMinimalSetup by remember { mutableStateOf(false) }
    // Id of the Solvado widget whose settings are open.
    var solvadoWidgetId by remember { mutableStateOf<String?>(null) }
    var showGallery by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<HomeGridItem?>(null) }
    var menuAnchor by remember { mutableStateOf<Pair<String, Rect>?>(null) }
    // Where each open folder grows out of / shrinks back into.
    val folderOrigins = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }
    /** Category opened from the App Library: title and its apps. */
    var openPod by remember { mutableStateOf<OpenPod?>(null) }
    val podProgress = remember { Animatable(0f) }
    val podProgressRead = remember { { podProgress.value } }
    var statusBarLightContent by remember { mutableStateOf(true) }

    // After a restart of the launcher the icons are not there yet: start zoomed out and
    // invisible, and fly in once they are loaded – the same motion as returning from an app.
    val homeZoom = remember { Animatable(if (state.isLoading) IOS_ZOOM_OUT else 1f) }
    val loadingNow by androidx.compose.runtime.rememberUpdatedState(state.isLoading)
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading && homeZoom.value != 1f) homeZoom.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 230f))
    }
    val homeZoomRead = remember { { homeZoom.value } }
    val spotlight = remember { Animatable(0f) }
    var leftToApp by remember { mutableStateOf(false) }
    val drag = remember { HomeDragState() }
    // Folder that is open underneath the running drag (started inside it, or sprung open).
    val fs = remember { FolderDragSession() }
    val folderSlots = remember { mutableMapOf<String, Pair<FolderSlots, Int>>() }
    var sessionFolder by remember { mutableStateOf<HomeGridItem.FolderItem?>(null) }
    // Reordering on the minimalist home screen.
    val md = remember { MinimalDragState() }
    val minimalRects = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }
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
                    viewModel.refreshScreenTime()
                    viewModel.refreshSolvado()
                    scope.launch { refreshStatusBarTone() }
                    if (loadingNow) leftToApp = false
                    else if (leftToApp || homeZoom.value != 1f) {
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
        onAppClick = { viewModel.launchApp(it.packageName) },
        screenMinutes = state.screenTime.takeIf { it.granted }?.totalMinutes,
        screenTop = remember(state.screenTime, state.appMap) { state.screenTime.top.mapNotNull { (pkg, m) -> state.appMap[pkg]?.let { it to m } } },
        solvado = state.solvado,
        onSolvadoRefresh = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.refreshSolvado(force = true) },
        editing = state.isJiggleMode
    )

    fun appForWidget(kind: WidgetKind): AppInfo? {
        val keys = when (kind) {
            WidgetKind.WEATHER -> listOf("weather")
            WidgetKind.CALENDAR -> listOf("calendar")
            WidgetKind.NOTES -> listOf("keep", "notes", "memo")
            WidgetKind.CLOCK, WidgetKind.DIGITAL, WidgetKind.MINIMAL -> listOf("deskclock", "clock")
            WidgetKind.DATE, WidgetKind.YEAR -> listOf("calendar")
            WidgetKind.BATTERY, WidgetKind.SUGGESTIONS, WidgetKind.SCREENTIME, WidgetKind.SOLVADO_SHOP, WidgetKind.SOLVADO_ADMIN, WidgetKind.SPACER -> emptyList()
        }
        return keys.firstNotNullOfOrNull { k -> state.allApps.firstOrNull { it.packageName.contains(k) } }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val isUnfolded = maxWidth >= 600.dp
        val bookMode = isUnfolded && settings.foldMode == com.tms.os.launcher.model.FoldPostureMode.BOOK_SPLIT
        val perScreen = if (bookMode) 2 else 1
        // The grid is measured against the window, not this box: the box shrinks while a
        // keyboard is up (adjustResize), and the home pages must not repack because of that.
        val windowHeight = remember(configuration.screenWidthDp, configuration.screenHeightDp, maxWidth) {
            val activity = generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }.filterIsInstance<android.app.Activity>().firstOrNull()
            activity?.let { with(density) { androidx.window.layout.WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(it).bounds.height().toDp() } } ?: 0.dp
        }
        val gridHeight = if (windowHeight > maxHeight) windowHeight else maxHeight
        val layout = calculateHomeLayout(
            width = maxWidth.value / perScreen, height = gridHeight.value,
            columns = if (isUnfolded && !bookMode && settings.foldMode != com.tms.os.launcher.model.FoldPostureMode.CLASSIC_SCALED) settings.mainUnfoldedColumns else settings.coverColumns,
            requestedRows = if (isUnfolded && !bookMode) settings.mainUnfoldedRows else settings.coverRows,
            iconScale = settings.iconSizeScale, labelSize = settings.labelFontSizeSp,
            fontScale = density.fontScale
        )
        val columns = layout.columns
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
        // As many square rows as the page really has room for: with free placement the
        // space down to the dock must be usable, not a strip below the last row.
        val rows = run {
            val pageHeight = gridHeight - IosMetrics.statusBarHeight - chromeHeight - 9.dp - 4.dp
            // A row needs room for the icon, its label and a little air – it does not have to
            // be square. Rows get a bit lower when that lets one more fit above the dock.
            val label = with(density) { (settings.labelFontSizeSp * 1.3f).sp.toDp() }
            val minRow = iconSize + 5.dp + label + 2.dp
            maxOf(layout.rows, if (minRow > 0.dp) (pageHeight / minRow).toInt() else layout.rows)
        }

        // Minimalist mode: one calm page instead of the icon grid. The grid itself stays
        // stored untouched, so switching back brings everything back.
        // The plain templates replace the grid with their own page; the iOS-style ones are a
        // second, ordinary home-screen layout and need nothing special here.
        val minimal = settings.minimalMode && !settings.minimalTemplate.ios
        val minimalOrder = md.order ?: settings.minimalApps
        val minimalApps = remember(minimalOrder, state.appMap) { minimalOrder.mapNotNull { state.appMap[it] } }
        // Templates with a dock put the first apps there; the rest go on the page.
        val minimalDock = minimal && settings.minimalTemplate.usesDock
        val displayOrder = if (minimal) emptyList() else drag.order ?: state.homeGridItems
        // Free placement (iOS 18): each grid shape keeps its own cells; none = reading order.
        val shape = "${columns}x$rows"
        // A placement saved for the same number of columns still fits when rows were added.
        val savedAnchors = state.gridAnchors[shape] ?: state.gridAnchors.entries.firstOrNull { it.key.startsWith("${columns}x") }?.value.orEmpty()
        val displayAnchors = drag.anchors ?: savedAnchors
        val pages = remember(displayOrder, columns, rows, displayAnchors) {
            packAnchored(displayOrder, columns, { rows }, { it.span() }) { displayAnchors[it.id] }
        }
        val homePageCount = ((pages.size + perScreen - 1) / perScreen).coerceAtLeast(1)
        // Pager: 0 = Today View, 1..n = home pages, n+1 = App Library.
        val pagerState = rememberPagerState(initialPage = 1, pageCount = { homePageCount + 2 })
        // The App Library is the last page, so its index moves with the number of home
        // pages. That number can change for a moment while a system sheet (Touch ID)
        // changes the window height; whoever was on the library stays on the library.
        var lastHomePageCount by remember { mutableStateOf(homePageCount) }
        LaunchedEffect(homePageCount) {
            val was = lastHomePageCount
            lastHomePageCount = homePageCount
            // On the old library index (or clamped onto the new one): go to the library's new index.
            if (was != homePageCount && (pagerState.currentPage == was + 1 || pagerState.currentPage > homePageCount + 1)) {
                pagerState.scrollToPage(homePageCount + 1)
            }
        }
        // A touch that interrupts a programmatic page change (Home button, a tap mid-animation)
        // ends the scroll without a fling, which would leave the pager between two pages.
        // Whenever scrolling stops off a page boundary, settle on the nearest page.
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
                if (!scrolling && kotlin.math.abs(pagerState.currentPageOffsetFraction) > 0.001f) {
                    runCatching { pagerState.animateScrollToPage(pagerState.currentPage) }
                }
            }
        }
        val libraryPage = homePageCount + 1
        // The *settled* page on purpose: reading currentPage here would rebuild this
        // whole screen in the middle of every page swipe. Anything that must follow
        // the finger (dots, dock fade) reads the pager inside its own small scope.
        val isAppLibrary = pagerState.settledPage == libraryPage
        // The library is "active" only while it rests on screen; the first move of a page swipe ends that.
        val libraryActive = remember(pagerState, libraryPage) { { pagerState.settledPage == libraryPage && !pagerState.isScrollInProgress } }
        val isToday = pagerState.settledPage == 0
        val homePageIndex = (pagerState.settledPage - 1).coerceIn(0, homePageCount - 1)

        LaunchedEffect(state.homePressGeneration) {
            if (state.homePressGeneration > 0) {
                showCustomizeSheet = false; showGallery = false; showMinimalSetup = false; solvadoWidgetId = null
                if (openPod != null) { podProgress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 260f, visibilityThreshold = 0.004f)); openPod = null }
                if (pagerState.currentPage != 1) pagerState.animateScrollToPage(1, animationSpec = spring(dampingRatio = 1f, stiffness = 300f))
            }
        }
        BackHandler(enabled = showCustomizeSheet || showMinimalSetup || solvadoWidgetId != null || pagerState.settledPage != 1 || state.isAnyOverlayOpen || state.isJiggleMode) {
            when {
                solvadoWidgetId != null -> solvadoWidgetId = null
                showMinimalSetup -> showMinimalSetup = false
                showCustomizeSheet -> showCustomizeSheet = false
                state.isAnyOverlayOpen || state.isJiggleMode -> viewModel.handleBackOrHomePress()
                else -> scope.launch { pagerState.animateScrollToPage(1) }
            }
        }

        val anyOverlay = state.isSpotlightOpen || state.activeFolder != null ||
            state.activeCategoryModal != null || state.selectedAppForMenu != null || showCustomizeSheet || showMinimalSetup || solvadoWidgetId != null || showGallery || openPod != null
        // Folder, menu and gallery sit on a frosted home screen. The frost is the
        // home content redrawn at 1/8 size and blurred there (64x fewer pixels than a
        // full-size blur, and nothing is read back from the GPU), then stretched back.
        val needsFrost = state.activeFolder != null || fs.active || state.selectedAppForMenu != null || showGallery
        val frost = animateFloatAsState(if (needsFrost) 1f else 0f, tween(280, easing = IosEase), label = "frost")
        val canFrost = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val showFrost by remember { androidx.compose.runtime.derivedStateOf { frost.value > 0.002f } }
        val homeLayer = rememberGraphicsLayer()
        val frostLayer = rememberGraphicsLayer()
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

        val openAppWithAnimation: (AppInfo, Rect) -> Unit = { app, bounds ->
            // A secured app waits for Touch ID first: no zoom-away until it really opens.
            if (!state.isJiggleMode && app.packageName in state.securedApps) viewModel.launchApp(app.packageName)
            else if (!state.isJiggleMode) {
                val options = if (bounds.width > 0 && bounds.height > 0) {
                    ActivityOptions.makeScaleUpAnimation(view, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt()).toBundle()
                } else null
                leftToApp = true
                scope.launch { homeZoom.animateTo(IOS_ZOOM_OUT, tween(380, easing = IosLaunchEasing)) }
                viewModel.launchApp(app.packageName, options)
            }
        }

        fun openFolder(folder: HomeGridItem.FolderItem, bounds: Rect) {
            folderOrigins[folder.folderId] = bounds
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
        val dockApps = if (minimalDock) minimalApps.take(dockCount)
        else remember(dockPkgs, state.appMap) { dockPkgs.take(dockCount).mapNotNull { state.appMap[it] } }
        var dockRect by remember { mutableStateOf(Rect.Zero) }
        val dockIconRects = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }
        // Folder whose panel owns the running gesture: it must stay composed until the drop.
        var ownerFolder by remember { mutableStateOf<HomeGridItem.FolderItem?>(null) }
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
                    alpha = maxOf(overlayGlass.value, spotlight.value.coerceIn(0f, 1f), edgeFraction())
                })
        }
        // Today View and App Library also dim the wallpaper a little, like iOS.
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = maxOf(edgeFraction() * 0.62f, spotlight.value.coerceIn(0f, 1f) * 0.72f, overlayGlass.value * 0.45f)
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
            // A folder is open under the finger: the app is arranged inside it.
            val sessionId = fs.folderId
            if (sessionId != null && item is HomeGridItem.AppItem) {
                drag.edge = 0; drag.hover = null; drag.folderTarget = null
                if (drag.overDock) {
                    drag.overDock = false
                    drag.dockOrder = drag.dockOrder?.filterNot { it == item.packageName }
                }
                val (slots, page) = folderSlots[sessionId] ?: return
                if (slots.panel.inflate(edgePx).contains(pointer)) {
                    fs.armed = true; fs.entered = true
                    fs.outside = false
                    fs.edge = when {
                        pointer.x < slots.panel.left + edgePx -> -1
                        pointer.x > slots.panel.right - edgePx -> 1
                        else -> 0
                    }
                    val idx = (page * 9 + slots.slotAt(pointer)).coerceIn(0, (fs.order.size - 1).coerceAtLeast(0))
                    if (idx != fs.hoverIndex) fs.hoverIndex = idx
                } else {
                    // A folder that just sprang open stays until the finger moves on.
                    if (!fs.armed && distance(pointer, fs.openedAt) > edgePx * 2f) fs.armed = true
                    fs.edge = 0
                    fs.hoverIndex = -1
                    fs.outside = fs.armed
                }
                return
            }
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

        fun placeDragged(h: Hover) {
            val item = drag.item ?: return
            val order = drag.order ?: return
            // Always measured against the layout as it was when the drag began: moving on
            // undoes the previous push instead of piling one onto the other.
            val base = drag.basePages ?: pages.also { drag.basePages = it }
            val moved = moveAnchored(base, { it.id }, item, com.tms.os.launcher.domain.GridAnchor(h.page, h.col, h.row), columns, { rows }) { it.span() } ?: return
            if (order.none { it.id == item.id }) drag.order = order + item
            if (moved != drag.anchors) drag.anchors = moved
        }

        fun dragEnd() {
            val item = drag.item ?: return
            val folderTarget = drag.folderTarget
            var finalOrder = drag.order ?: state.homeGridItems
            drag.edge = 0
            val pkg = (item as? HomeGridItem.AppItem)?.packageName
            // Dropped inside the open folder: it takes the app at the previewed slot.
            val sessionId = fs.folderId
            if (sessionId != null && pkg != null) {
                val apps = fs.order.let { if (pkg in it) it else it + pkg }
                val next = finalOrder.filterNot { it.id == item.id }.withoutFolderApp(pkg, keep = sessionId)
                    .map { if (it is HomeGridItem.FolderItem && it.folderId == sessionId) it.copy(appPackages = apps) else it }
                val folder = next.firstOrNull { it is HomeGridItem.FolderItem && it.folderId == sessionId } as? HomeGridItem.FolderItem
                if (state.dockPackages.contains(pkg)) viewModel.setDockOrder(state.dockPackages - pkg)
                viewModel.commitHomeOrder(next)
                viewModel.openFolder(folder)
                scope.launch {
                    try {
                        drag.dropping = true
                        drag.dropAnim.snapTo(drag.pointer - drag.grab)
                        val idx = apps.indexOf(pkg)
                        val target = folderSlots[sessionId]?.takeIf { it.second == idx / 9 }?.first?.iconTopLeft(idx % 9)
                        if (target != null) drag.dropAnim.animateTo(target - Offset((geometry.cellW - geometry.iconPx) / 2f, 0f), spring(0.8f, 400f))
                        else delay(120)
                    } finally {
                        // Even if the drop animation is interrupted: nothing may stay "being dragged".
                        fs.clear(); sessionFolder = null; ownerFolder = null
                        if (drag.dropping) drag.clear()
                    }
                }
                return
            }
            val intoDock = pkg != null && drag.overDock && drag.dockOrder?.contains(pkg) == true
            // The cell under the finger at the moment of release is where it goes.
            drag.hover?.let { h -> if (!h.folder && folderTarget == null && !drag.overDock && fs.folderId == null) placeDragged(h) }
            // Whatever moved aside during the drag stays where it is now.
            drag.anchors?.let { viewModel.commitAnchors(shape, it - item.id) }
            if (intoDock) {
                viewModel.setDockOrder(drag.dockOrder!!)
                viewModel.commitHomeOrder(finalOrder.filterNot { it.id == item.id })
                ownerFolder = null
                drag.clear()
                return
            }
            // Never lose an app: if it never reached a grid slot, it goes to the end of this page.
            if (folderTarget == null && finalOrder.none { it.id == item.id }) {
                finalOrder = reorderFor(finalOrder + item, item, Hover(homePageIndex * perScreen, columns - 1, rows - 1, null, false), pages)
            }
            if (pkg != null && state.dockPackages.contains(pkg)) viewModel.setDockOrder(state.dockPackages - pkg)
            ownerFolder = null
            scope.launch {
              try {
                drag.dropping = true
                drag.dropAnim.snapTo(drag.pointer - drag.grab)
                if (folderTarget != null) {
                    val target = placedOf(folderTarget)?.let { rootOfPlaced(it) }
                    launch { animateFloat(1f, 0.4f) { drag.dropScale = it } }
                    if (target != null) drag.dropAnim.animateTo(target, spring(0.85f, 420f))
                    if (item is HomeGridItem.WidgetItem) viewModel.mergeWidgets(item.id, folderTarget)
                    else {
                        // The preview order first: it no longer lists the app in the folder it came from.
                        viewModel.commitHomeOrder(finalOrder)
                        viewModel.mergeIntoFolder(item.id, folderTarget)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    viewModel.commitHomeOrder(finalOrder)
                    val finalAnchors = drag.anchors ?: savedAnchors
                    drag.anchors?.let { viewModel.commitAnchors(shape, it) }
                    val finalPages = packAnchored(finalOrder, columns, { rows }, { it.span() }) { finalAnchors[it.id] }
                    val p = finalPages.asSequence().flatten().firstOrNull { it.item.id == item.id }
                    val target = p?.let { rootOfPlaced(it) }
                    if (target != null) drag.dropAnim.animateTo(target, spring(0.78f, 380f))
                }
              } finally {
                // Even if the drop animation is interrupted: nothing may stay "being dragged",
                // which would keep the pager and every home gesture switched off.
                if (drag.dropping) drag.clear()
              }
            }
        }

        // Everything the pages, dock and overlays can ask for. A fresh object every
        // composition (so it always sees current state) behind one stable handle:
        // children receive [actions] and are skipped when nothing they show changed.
        // (An anonymous object on purpose: Compose memoises lambdas and references
        // to local functions, which once froze the first, empty grid geometry.)
        val ops = androidx.compose.runtime.rememberUpdatedState<HomeOps>(object : HomeOps {
            override fun openApp(app: AppInfo, bounds: Rect) = openAppWithAnimation(app, bounds)
            override fun appMenu(app: AppInfo, bounds: Rect) { menuAnchor = app.packageName to bounds; viewModel.selectAppForContextMenu(app) }
            override fun folder(folder: HomeGridItem.FolderItem, bounds: Rect) = openFolder(folder, bounds)
            override fun widgetTap(kind: WidgetKind) {
                if (kind == WidgetKind.SCREENTIME) { if (!state.screenTime.granted) viewModel.openUsageAccessSettings() }
                else appForWidget(kind)?.let { leftToApp = true; viewModel.launchApp(it.packageName) }
            }
            override fun enterJiggle() { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setJiggleMode(true) }
            override fun exitJiggle() { if (state.isJiggleMode) viewModel.setJiggleMode(false) }
            override fun remove(item: HomeGridItem) { removeTarget = item }
            override fun stackBounds(id: String, r: Rect?) { if (r == null) stackRects.remove(id) else stackRects[id] = r }
            override fun startDrag(item: HomeGridItem, downAt: Offset, at: Offset) = dragStart(item, downAt, at)
            override fun startExternalDrag(pkg: String, iconTopLeft: Offset, downAt: Offset, at: Offset) = externalDragStart(pkg, iconTopLeft, downAt, at)
            override fun moveDrag(at: Offset) {
                if (!md.active) return dragMove(at)
                md.pointer = at
                // The position under the finger is where the app wants to go.
                val nearest = md.slots.indices.filter { md.slots[it] != null }.minByOrNull { distance(md.slots[it]!!, at) } ?: return
                if (distance(md.slots[nearest]!!, at) < geometry.cellW * 1.4f && nearest != md.hover) md.hover = nearest
            }
            override fun endDrag() {
                val pkg = md.pkg ?: return dragEnd()
                val order = md.order ?: settings.minimalApps
                viewModel.updateSettings { it.copy(minimalApps = order) }
                scope.launch {
                    try {
                        val target = md.slots.getOrNull(order.indexOf(pkg))
                        if (target != null && settings.minimalTemplate != com.tms.os.launcher.model.MinimalTemplate.LIST) {
                            md.dropping = true
                            md.dropAnim.snapTo(md.pointer - md.grab)
                            md.dropAnim.animateTo(target - Offset(geometry.iconPx / 2f, geometry.iconPx / 2f), spring(0.8f, 400f))
                        }
                    } finally {
                        if (md.pkg == pkg) md.clear()
                    }
                }
            }
            override fun minimalDragStart(pkg: String, topLeft: Offset, downAt: Offset, at: Offset) {
                if (!state.isJiggleMode) {
                    viewModel.selectAppForContextMenu(null)
                    viewModel.setJiggleMode(true)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val order = settings.minimalApps
                val onPage = if (minimalDock) settings.minimalTemplate.maxApps - dockCount else settings.minimalTemplate.maxApps
                md.slots = order.mapIndexed { i, p ->
                    when {
                        minimalDock && i < dockCount -> dockIconRects[p]?.center
                        (if (minimalDock) i - dockCount else i) < onPage -> minimalRects[p]?.center
                        else -> null
                    }
                }
                md.grab = downAt - topLeft
                md.pointer = at
                md.order = order
                md.hover = order.indexOf(pkg)
                md.pkg = pkg
            }
            override fun minimalRemove(pkg: String) { viewModel.updateSettings { it.copy(minimalApps = it.minimalApps - pkg) } }
            override fun springFolder(itemId: String) {
                val app = drag.item as? HomeGridItem.AppItem ?: return
                val folder = (drag.order ?: state.homeGridItems).firstOrNull { it.id == itemId } as? HomeGridItem.FolderItem ?: return
                placedOf(itemId)?.let { p ->
                    rootOfPlaced(p)?.let { tl ->
                        folderOrigins[folder.folderId] = Rect(
                            tl + Offset((geometry.cellW - geometry.iconPx) / 2f, 0f),
                            androidx.compose.ui.geometry.Size(geometry.iconPx, geometry.iconPx)
                        )
                    }
                }
                sessionFolder = folder
                fs.open(folder.folderId, folder.appPackages - app.packageName + app.packageName, drag.pointer, armed = false)
                drag.folderTarget = null; drag.hover = null
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            override fun leaveFolder() {
                val id = fs.folderId ?: return
                val app = drag.item as? HomeGridItem.AppItem ?: return
                // From here on the app is only carried: no folder lists it any more.
                drag.order = (drag.order ?: state.homeGridItems).withoutFolderApp(app.packageName)
                fs.clear()
                if (state.activeFolder?.folderId == id) viewModel.openFolder(null)
            }
            override fun widgetSettings(widgetId: String) { solvadoWidgetId = widgetId }
            override fun minimalSetup() {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setJiggleMode(false)
                showMinimalSetup = true
            }
            override fun placeAt(h: Hover) = placeDragged(h)
            override fun spotlight() { viewModel.openSpotlight() }
            override fun pod(title: String, apps: List<AppInfo>, origin: Rect) {
                openPod = OpenPod(title, apps, origin)
                scope.launch { podProgress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 190f)) }
            }
            override fun dockIconBounds(pkg: String, r: Rect) { dockIconRects[pkg] = r }
            override fun edge(): Float = edgeFraction()
            override fun pageForDots(): Int = (pagerState.currentPage - 1).coerceIn(0, (homePageCount - 1).coerceAtLeast(0))
            override fun isPaging(): Boolean = pagerState.isScrollInProgress
        })
        val actions = remember { HomeActions(ops) }

        // Touch ID: whatever waits for it (a secured app, the hidden apps, securing an app)
        // is answered by the system's own fingerprint sheet with the device code as fallback.
        LaunchedEffect(state.authRequest) {
            val request = state.authRequest ?: return@LaunchedEffect
            val activity = generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
                .filterIsInstance<android.app.Activity>().firstOrNull()
            if (activity == null || !com.tms.os.launcher.data.DeviceAuth.available(context)) {
                viewModel.authAnswered(false)
                android.widget.Toast.makeText(context, hs.authNoLock, android.widget.Toast.LENGTH_LONG).show()
                return@LaunchedEffect
            }
            val label = { pkg: String -> state.appMap[pkg]?.displayLabel ?: pkg }
            val title = when (request) {
                is com.tms.os.launcher.viewmodel.AuthRequest.Launch -> String.format(hs.authOpen, label(request.packageName))
                is com.tms.os.launcher.viewmodel.AuthRequest.QuickAction -> String.format(hs.authOpen, label(request.action.packageName))
                is com.tms.os.launcher.viewmodel.AuthRequest.ToggleSecure ->
                    String.format(if (request.packageName in state.securedApps) hs.authUnsecure else hs.authSecure, label(request.packageName))
                com.tms.os.launcher.viewmodel.AuthRequest.ShowHidden -> hs.authHidden
            }
            com.tms.os.launcher.data.DeviceAuth.authenticate(activity, title, null) { ok ->
                val done = viewModel.authAnswered(ok)
                if (done is com.tms.os.launcher.viewmodel.AuthRequest.Launch || done is com.tms.os.launcher.viewmodel.AuthRequest.QuickAction) {
                    leftToApp = true
                    scope.launch { homeZoom.animateTo(IOS_ZOOM_OUT, tween(380, easing = IosLaunchEasing)) }
                } else if (homeZoom.value != 1f) scope.launch { homeZoom.animateTo(1f, spring(0.9f, 300f)) }
                if (done == com.tms.os.launcher.viewmodel.AuthRequest.ShowHidden) {
                    val hidden = viewModel.uiState.value.allApps.filter { it.isHidden }.sortedBy { it.displayLabel.lowercase() }
                    ops.value.pod(hs.hidden, hidden, hiddenOrigin ?: Rect.Zero)
                }
            }
        }

        // Dwell before reflowing (iOS waits a beat), longer dwell to form a folder.
        LaunchedEffect(drag.hover) {
            val h = drag.hover ?: return@LaunchedEffect
            val item = drag.item ?: return@LaunchedEffect
            // Back over its own place: undo whatever made room before.
            if (h.overId == item.id) { ops.value.placeAt(h); return@LaunchedEffect }
            if (h.folder) {
                delay(420)
                drag.folderTarget = h.overId
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                // iOS: keep holding an app over a folder and the folder opens, so the
                // app can be placed inside it.
                val over = h.overId
                if (over != null && over.startsWith("folder:") && item is HomeGridItem.AppItem) {
                    delay(650)
                    ops.value.springFolder(over)
                }
            } else {
                // An empty cell takes the item at once; only when something has to make room
                // does it wait a beat, so icons move where the item rests, not where it passes.
                if (h.overId != null) delay(260)
                ops.value.placeAt(h)
            }
        }
        // Minimalist page: the others make room after a short dwell.
        LaunchedEffect(md.hover, md.pkg) {
            val pkg = md.pkg ?: return@LaunchedEffect
            val order = md.order ?: return@LaunchedEffect
            if (md.hover < 0 || order.indexOf(pkg) == md.hover) return@LaunchedEffect
            delay(110)
            md.order = (order - pkg).toMutableList().apply { add(md.hover.coerceIn(0, size), pkg) }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        // Inside an open folder: the other apps make room after a short dwell.
        LaunchedEffect(fs.hoverIndex, fs.folderId) {
            val i = fs.hoverIndex
            val pkg = (drag.item as? HomeGridItem.AppItem)?.packageName ?: return@LaunchedEffect
            if (!fs.active || i < 0) return@LaunchedEffect
            delay(110)
            val cur = fs.order
            if (cur.indexOf(pkg) != i) fs.order = (cur - pkg).toMutableList().apply { add(i.coerceIn(0, size), pkg) }
        }
        // Carried out of the panel: the folder closes and the app is back on the home screen.
        LaunchedEffect(fs.outside, fs.folderId) {
            if (fs.active && fs.outside) {
                delay(if (fs.entered) 380L else 900L)
                ops.value.leaveFolder()
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
                // The next finger move picks the cell on the new page.
                drag.hover = null
                delay(300)
            }
        }

        // --------------------------------------------------------- root gesture
        val pullPx = with(density) { SpotlightPullDistance.toPx() }
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
                        var opened = false
                        val onStack = stackRects.values.any { it.contains(down.position) }
                        // However the pull ends – release, a second finger, the system taking the
                        // gesture, this handler being restarted – Spotlight ends up open or closed.
                        try { while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            tracker.addPosition(c.uptimeMillis, c.position)
                            val dx = c.position.x - down.position.x
                            val dy = c.position.y - down.position.y
                            if (!c.pressed) {
                                if (pulling) {
                                    val v = tracker.calculateVelocity().y
                                    if (spotlight.value > 0.35f || v > 1200f) { opened = true; viewModel.openSpotlight() }
                                }
                                break
                            }
                            // iOS: hold first, then move = pick up the icon. Leave such
                            // gestures to the icon/drag handlers instead of Spotlight.
                            if (!pulling && c.uptimeMillis - down.uptimeMillis > viewConfiguration.longPressTimeoutMillis) break
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
                        } } finally {
                            if (pulling && !opened) scope.launch { spotlight.animateTo(0f, spring(0.9f, 400f)) }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val z = homeZoom.value
                        scaleX = homeScale * z
                        scaleY = homeScale * z
                        alpha = 1f - ((z - 1f) / (IOS_ZOOM_OUT - 1f)).coerceIn(0f, 1f)
                    }
            ) {
                Spacer(Modifier.height(IosMetrics.statusBarHeight))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val sp = spotlight.value.coerceIn(0f, 1f)
                            // Fades out under Spotlight; cross-fades to the frosted snapshot
                            // under folders and menus (stays faintly visible until it is ready).
                            val f = frost.value
                            // No offscreen buffer for the fade: alpha goes straight onto the draws.
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                            alpha = (1f - sp) * (if (canFrost) 1f - f else 1f - f * 0.75f)
                            val s = 1f - sp * 0.03f - f * 0.02f
                            scaleX = s; scaleY = s
                        }
                        .drawWithContent {
                            homeLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(homeLayer)
                        }
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = !anyOverlay && !drag.active && !md.active,
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
                                    onWidgetTap = actions.todayWidgetTap,
                                    onWidgetSettings = { w -> actions.widgetSettings(w.widgetId) },
                                    onRemove = actions.remove,
                                    onEnterJiggle = actions.enterJiggle,
                                    onExitJiggle = actions.exitJiggle
                                )
                                libraryPage -> IosAppLibraryPage(
                                    allApps = state.allApps,
                                    categorizedApps = state.categorizedApps,
                                    settings = settings,
                                    strings = strings,
                                    isUnfolded = isUnfolded,
                                    podProgress = podProgressRead,
                                    podOrigin = openPod?.origin,
                                    onAppClick = actions.openApp,
                                    onAppLongClick = actions.appMenu,
                                    onOpenPod = actions.pod,
                                    isActive = libraryActive,
                                    hiddenTitle = hs.hidden,
                                    onOpenHidden = { origin -> hiddenOrigin = origin; viewModel.requestHiddenApps() }
                                )
                                else -> if (minimal) com.tms.os.launcher.ui.home.MinimalHomePage(
                                    template = settings.minimalTemplate,
                                    apps = if (minimalDock) minimalApps.drop(dockCount) else minimalApps,
                                    labelFontSizeSp = settings.labelFontSizeSp,
                                    env = widgetEnv,
                                    iconSize = iconSize,
                                    // Without a dock only the search pill is left at the bottom.
                                    bottomInset = if (minimalDock) chromeHeight else PillAreaHeight + dockBottomGap,
                                    onAppClick = actions.openApp,
                                    onAppLongClick = actions.appMenu,
                                    onClock = actions.clock,
                                    onSetup = actions.minimalSetup,
                                    edit = MinimalEdit(
                                        jiggle = state.isJiggleMode,
                                        hiddenPkg = md.pkg,
                                        onBounds = { pkg, r -> minimalRects[pkg] = r },
                                        onRemove = actions.minimalRemove,
                                        onDragStart = actions.minimalDragStart,
                                        onDragMove = actions.moveDrag,
                                        onDragEnd = actions.endDrag,
                                        onEnterJiggle = actions.enterJiggle,
                                        onExitJiggle = actions.exitJiggle
                                    )
                                ) else Row(
                                    Modifier
                                        .fillMaxSize()
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = actions.exitJiggle,
                                            onLongClick = actions.enterJiggle
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
                                                appMap = state.appMap,
                                                iconSize = iconSize,
                                                labelSize = settings.labelFontSizeSp,
                                                showLabels = settings.showAppLabels,
                                                showBadges = settings.enableBadges,
                                                jiggle = state.isJiggleMode,
                                                folderTarget = drag.folderTarget == p.item.id,
                                                env = widgetEnv,
                                                // A spacer is invisible: it has a name only while editing.
                                                widgetLabel = (p.item as? HomeGridItem.WidgetItem)?.takeIf {
                                                    // Solvado widgets name themselves (shop or "TMS Solvado") – no label under them.
                                                    it.kind != WidgetKind.SOLVADO_SHOP && it.kind != WidgetKind.SOLVADO_ADMIN &&
                                                        (it.kind != WidgetKind.SPACER || state.isJiggleMode)
                                                }?.let { hs.widgetName(it.kind) },
                                                dragEnabled = !anyOverlay || state.selectedAppForMenu != null,
                                                actions = actions
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
                                    dockApps.firstNotNullOfOrNull { a -> dockIconRects[a.packageName]?.takeIf { it.contains(downAt) }?.let { a.packageName to it } }?.let { (pkg, r) ->
                                        if (minimal) actions.minimalDragStart(pkg, r.topLeft, downAt, at)
                                        else actions.startExternalDrag(pkg, r.topLeft, downAt, at)
                                    }
                                },
                                onMove = actions.moveDrag,
                                onEnd = actions.endDrag
                            ),
                        onDockIconBounds = actions.dockIconBounds,
                        hiddenPkg = if (minimal) md.pkg else (drag.item as? HomeGridItem.AppItem)?.packageName?.takeIf { drag.active && !drag.dropping },
                        onRemove = if (minimal) actions.minimalRemove else null,
                        bottomGap = dockBottomGap,
                        dockMargin = dockMargin,
                        // Dock icons stand in the grid columns when the counts match.
                        slotWidth = if (!isUnfolded && dockCount == columns) with(density) { geometry.cellW.toDp() } else Dp.Unspecified,
                        showDock = !minimal || minimalDock,
                        pageCount = homePageCount,
                        currentPage = actions.pageForDots,
                        isPaging = actions.isPaging,
                        strings = strings,
                        showSearch = !state.isJiggleMode && !drag.active,
                        dockApps = dockApps,
                        recentApps = recentApps,
                        isUnfolded = isUnfolded,
                        showRecent = settings.showRecentInUnfoldedDock,
                        showBadges = settings.enableBadges,
                        iconSize = dockIconSize,
                        dockHeight = dockHeight,
                        glassOpacity = settings.glassOpacity,
                        isJiggleMode = state.isJiggleMode,
                        fade = actions.edgeFraction,
                        onSearch = actions.spotlight,
                        onAppClick = actions.openApp,
                        onAppLongClick = actions.appMenu
                    )
                    }
                }
            }
        }

        if (canFrost && (showFrost || needsFrost)) {
            Box(
                Modifier
                    .padding(top = IosMetrics.statusBarHeight)
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                        alpha = frost.value * 0.9f
                        val s = 1.03f - 0.03f * frost.value
                        scaleX = s; scaleY = s
                    }
                    .drawBehind {
                        val k = FrostDownscale
                        frostLayer.renderEffect = BlurEffect(7f, 7f, TileMode.Clamp)
                        frostLayer.record(size = IntSize((size.width / k).roundToInt().coerceAtLeast(1), (size.height / k).roundToInt().coerceAtLeast(1))) {
                            scale(1f / k, 1f / k, pivot = Offset.Zero) { drawLayer(homeLayer) }
                        }
                        scale(k, k, pivot = Offset.Zero) { drawLayer(frostLayer) }
                    }
            )
        }

        // ------------------------------------- carried app (minimalist home screen)
        md.pkg?.let { pkg ->
            state.appMap[pkg]?.let { app ->
                val lift by animateFloatAsState(if (md.dropping) 1f else 1.12f, spring(0.7f, 500f), label = "minimalLift")
                Box(
                    Modifier
                        .zIndex(50f)
                        .offset {
                            val o = if (md.dropping) md.dropAnim.value else md.pointer - md.grab
                            IntOffset(o.x.roundToInt(), o.y.roundToInt())
                        }
                        .graphicsLayer { scaleX = lift; scaleY = lift }
                ) {
                    if (settings.minimalTemplate == com.tms.os.launcher.model.MinimalTemplate.LIST && !(minimalDock && minimalOrder.indexOf(pkg) < dockCount)) {
                        Text(
                            app.displayLabel,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = com.tms.os.launcher.ui.theme.SfFamily, fontSize = 27.sp, color = Color.White,
                                shadow = com.tms.os.launcher.ui.widgets.MinimalShadow
                            ),
                            maxLines = 1
                        )
                    } else app.iconBitmap?.let { Image(it, null, modifier = Modifier.size(iconSize)) }
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
                        p = p, w = w, h = h, appMap = state.appMap, iconSize = iconSize,
                        labelSize = settings.labelFontSizeSp, showLabels = false, showBadges = false,
                        jiggle = false, folderTarget = false, env = widgetEnv, widgetLabel = null,
                        dragEnabled = false, actions = InertActions
                    )
                }
            }
        }

        // ---------------------------------------------------------- overlays
        IosSpotlight(
            visible = state.isSpotlightOpen,
            progress = { spotlight.value },
            query = state.spotlightQuery,
            results = state.spotlightResults,
            contacts = state.spotlightContacts,
            mathResult = state.spotlightMathResult,
            history = remember(state.spotlightHistory, state.appMap) { state.spotlightHistory.filterNot { it == context.packageName }.mapNotNull { state.appMap[it] } },
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

        val carriedPkg = (drag.item as? HomeGridItem.AppItem)?.packageName
        listOfNotNull(ownerFolder, sessionFolder, state.activeFolder).distinctBy { it.folderId }.forEach { snap ->
            androidx.compose.runtime.key(snap.folderId) {
                val folder = displayOrder.firstOrNull { it is HomeGridItem.FolderItem && it.folderId == snap.folderId } as? HomeGridItem.FolderItem ?: snap
                val inSession = fs.folderId == folder.folderId
                IosFolderView(
                    folder = folder,
                    order = if (inSession) fs.order else folder.appPackages,
                    open = inSession || state.activeFolder?.folderId == folder.folderId,
                    dragPkg = carriedPkg,
                    dragEdge = if (inSession) fs.edge else 0,
                    onSlots = { slots, page -> folderSlots[folder.folderId] = slots to page },
                    onClosed = { if (fs.folderId != folder.folderId && sessionFolder?.folderId == folder.folderId) sessionFolder = null },
                    onDragStart = { app, iconTopLeft, downAt, at ->
                        ownerFolder = folder
                        actions.startExternalDrag(app.packageName, iconTopLeft, downAt, at)
                        fs.open(folder.folderId, folder.appPackages, at, armed = true)
                    },
                    onDragMove = actions.moveDrag,
                    onDragEnd = actions.endDrag,
                    zoom = homeZoomRead,
                    appMap = state.appMap,
                    iconBounds = folderOrigins[folder.folderId],
                    iconSize = if (isUnfolded) 64.dp else minOf(iconSize, 62.dp),
                    labelFontSizeSp = settings.labelFontSizeSp,
                    jiggle = state.isJiggleMode,
                    onAppClick = actions.openApp,
                    onAppLongClick = actions.appMenu,
                    onRename = { name -> viewModel.renameFolder(folder.folderId, name) },
                    onEnterJiggle = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setJiggleMode(true) },
                    onDismiss = { viewModel.openFolder(null) }
                )
            }
        }

        // App Library category: grows out of its pod to full screen, like iOS.
        openPod?.let { pod ->
            IosLibraryCategoryView(
                title = pod.title,
                apps = pod.apps,
                origin = pod.origin,
                progress = podProgressRead,
                columns = columns,
                sidePadding = sidePadding,
                iconSize = iconSize,
                labelFontSizeSp = settings.labelFontSizeSp,
                onAppClick = actions.openApp,
                onAppLongClick = actions.appMenu,
                onRequestClose = {
                    scope.launch {
                        try {
                            podProgress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 260f, visibilityThreshold = 0.004f))
                        } finally {
                            // Interrupted on its way out: still gone, never a hidden layer over the screen.
                            if (podProgress.value < 0.5f) openPod = null
                        }
                    }
                }
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
                onHide = { viewModel.hideOrUnhideApp(app.packageName, !app.isHidden) },
                isSecured = app.packageName in state.securedApps,
                onToggleSecure = { viewModel.toggleSecured(app.packageName) },
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

        // The position is read once, when the user asks for it in the weather widget's settings.
        val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted -> if (granted) viewModel.useCurrentLocationForWeather() }
        com.tms.os.launcher.ui.widgets.WidgetEditSheet(
            widget = solvadoWidgetId?.let { id ->
                state.todayWidgets.firstOrNull { it.widgetId == id }
                    ?: state.homeGridItems.firstOrNull { it is HomeGridItem.WidgetItem && it.widgetId == id } as? HomeGridItem.WidgetItem
            },
            solvado = state.solvado,
            settings = settings,
            strings = hs,
            solvadoServer = viewModel.solvadoServer,
            locating = state.weatherLocating,
            locateFailed = state.weatherLocateError,
            actions = remember {
                com.tms.os.launcher.ui.widgets.WidgetEditActions(
                    onConfig = { config -> solvadoWidgetId?.let { viewModel.setWidgetConfig(it, config) } },
                    onSize = { size -> solvadoWidgetId?.let { viewModel.setWidgetSize(it, size) } },
                    onSettings = { transform -> viewModel.updateSettings(transform) },
                    onSolvadoLogin = { email, password, server -> viewModel.solvadoLogin(email, password, server) },
                    onSolvadoLogout = { viewModel.solvadoLogout() },
                    onUseLocation = {
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.useCurrentLocationForWeather() else askLocation.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onCity = { city -> viewModel.refreshLiveWeatherForCity(city) },
                    onDismiss = { solvadoWidgetId = null }
                )
            }
        )

        com.tms.os.launcher.ui.home.MinimalSetupSheet(
            visible = showMinimalSetup,
            settings = settings,
            allApps = state.allApps,
            strings = hs,
            onSettingsChange = { transform -> viewModel.updateSettings(transform) },
            onDismiss = { showMinimalSetup = false },
            layoutApps = if (settings.minimalMode && settings.minimalTemplate.ios) remember(state.dockPackages, state.homeGridItems) {
                (state.dockPackages + state.homeGridItems.flatMap {
                    when (it) {
                        is HomeGridItem.AppItem -> listOf(it.packageName)
                        is HomeGridItem.FolderItem -> it.appPackages
                        else -> emptyList()
                    }
                }).distinct()
            } else null,
            onToggleLayoutApp = { pkg -> viewModel.toggleAppOnMinimalLayout(pkg) }
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

        // The real system status bar stays visible (clock, network, notifications).
        // Only in edit mode it steps aside so the Edit/Done pills can take its place,
        // as on iOS. Icon colour follows the wallpaper behind it.
        val hideStatusBar = state.isJiggleMode && !isAppLibrary
        val lightStatusContent = if (settings.useSystemWallpaper && !anyOverlay && !isToday && !isAppLibrary) statusBarLightContent else true
        LaunchedEffect(hideStatusBar, lightStatusContent) {
            val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
            androidx.core.view.WindowInsetsControllerCompat(window, view).apply {
                if (hideStatusBar) hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                else show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                isAppearanceLightStatusBars = !lightStatusContent
            }
        }
        IosEditTopBar(
            visible = state.isJiggleMode && !isAppLibrary && !showGallery,
            strings = hs,
            onAddWidget = { showGallery = true },
            onCustomize = { showCustomizeSheet = true },
            onMinimal = actions.minimalSetup,
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
    appMap: Map<String, AppInfo>,
    iconSize: Dp,
    labelSize: Float,
    showLabels: Boolean,
    showBadges: Boolean,
    jiggle: Boolean,
    folderTarget: Boolean,
    env: WidgetEnv,
    widgetLabel: String?,
    dragEnabled: Boolean,
    actions: HomeActions
) {
    val item0 = p.item
    // [dragEnabled] is passed in, not used to add/remove the modifier: a folder that
    // springs open mid-drag must not tear down the gesture that is running.
    val dragModifier = if (actions === InertActions) Modifier else Modifier.homeDragGesture(
        key = item0.id,
        jiggle = jiggle,
        enabled = dragEnabled,
        onStart = { downAt, at -> actions.startDrag(item0, downAt, at) },
        onMove = actions.moveDrag,
        onEnd = actions.endDrag
    )
    val onAppClick = actions.openApp
    val onAppLongClick = actions.appMenu
    val onFolderClick = actions.folder
    val onWidgetTap = actions.widgetTap
    val onEnterJiggle = actions.enterJiggle
    val onRemove = { actions.remove(item0) }
    val onStackBounds = actions.stackBounds
    when (val item = p.item) {
        is HomeGridItem.AppItem -> appMap[item.packageName]?.let { app ->
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
                appMap = appMap,
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
                            // Settings open only in edit mode; otherwise a tap does what the widget does.
                            onClick = {
                                if (jiggle) { if (item.kind.configurable && !item.isStack) actions.widgetSettings(item.widgetId) }
                                else onWidgetTap(current)
                            },
                            onLongClick = { if (!jiggle) onEnterJiggle() }
                        )
                    if (item.isStack) IosSmartStack(
                        item.kinds, item.size, env,
                        surface.onGloballyPositioned { onStackBounds(item.widgetId, it.boundsInRoot()) },
                        onCurrent = { current = it }
                    ) else IosWidget(item.kind, item.size, env, surface, config = item.config)
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
    onRemove: ((AppInfo) -> Unit)?,
    bottomGap: Dp,
    dockMargin: Dp,
    slotWidth: Dp,
    showDock: Boolean,
    pageCount: Int,
    currentPage: () -> Int,
    isPaging: () -> Boolean,
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
            // Pager state is read here, in this small scope, not in the whole screen.
            androidx.compose.animation.Crossfade(targetState = showSearch && !isPaging(), animationSpec = tween(200), label = "pageControl") { search ->
                if (search) IosSearchButton(text = strings.search, onClick = onSearch)
                else IosPageDots(count = pageCount, selected = currentPage())
            }
        }

        if (showDock) BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = dockMargin), contentAlignment = Alignment.Center) {
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
                                    onLongClickWithBounds = { if (!isJiggleMode) onAppLongClick(app, it) },
                                    onDeleteOrMenuClick = onRemove?.let { remove -> { remove(app) } }
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


/** What pages, dock and overlays can ask the home screen to do. */
private interface HomeOps {
    fun openApp(app: AppInfo, bounds: Rect)
    fun appMenu(app: AppInfo, bounds: Rect)
    fun folder(folder: HomeGridItem.FolderItem, bounds: Rect)
    fun widgetTap(kind: WidgetKind)
    fun enterJiggle()
    fun exitJiggle()
    fun remove(item: HomeGridItem)
    fun stackBounds(id: String, r: Rect?)
    fun startDrag(item: HomeGridItem, downAt: Offset, at: Offset)
    fun startExternalDrag(pkg: String, iconTopLeft: Offset, downAt: Offset, at: Offset)
    fun moveDrag(at: Offset)
    fun endDrag()
    fun springFolder(itemId: String)
    fun leaveFolder()
    fun widgetSettings(widgetId: String)
    fun minimalSetup()
    fun placeAt(h: Hover)
    fun minimalDragStart(pkg: String, topLeft: Offset, downAt: Offset, at: Offset)
    fun minimalRemove(pkg: String)
    fun spotlight()
    fun pod(title: String, apps: List<AppInfo>, origin: Rect)
    fun dockIconBounds(pkg: String, r: Rect)
    fun edge(): Float
    fun pageForDots(): Int
    fun isPaging(): Boolean
}

/**
 * Stable handle on [HomeOps]: the lambdas never change identity, so composables
 * that take them are skipped during unrelated updates, yet every call reaches
 * the newest state.
 */
@androidx.compose.runtime.Stable
private class HomeActions(ops: androidx.compose.runtime.State<HomeOps>?) {
    val openApp: (AppInfo, Rect) -> Unit = { a, r -> ops?.value?.openApp(a, r) }
    val appMenu: (AppInfo, Rect) -> Unit = { a, r -> ops?.value?.appMenu(a, r) }
    val folder: (HomeGridItem.FolderItem, Rect) -> Unit = { f, r -> ops?.value?.folder(f, r) }
    val widgetTap: (WidgetKind) -> Unit = { k -> ops?.value?.widgetTap(k) }
    val todayWidgetTap: (HomeGridItem.WidgetItem) -> Unit = { w ->
        ops?.value?.widgetTap(w.kind)
    }
    val widgetSettings: (String) -> Unit = { id -> ops?.value?.widgetSettings(id) }
    val enterJiggle: () -> Unit = { ops?.value?.enterJiggle() }
    val exitJiggle: () -> Unit = { ops?.value?.exitJiggle() }
    val remove: (HomeGridItem) -> Unit = { i -> ops?.value?.remove(i) }
    val stackBounds: (String, Rect?) -> Unit = { id, r -> ops?.value?.stackBounds(id, r) }
    val startDrag: (HomeGridItem, Offset, Offset) -> Unit = { i, d, a -> ops?.value?.startDrag(i, d, a) }
    val startExternalDrag: (String, Offset, Offset, Offset) -> Unit = { p, t, d, a -> ops?.value?.startExternalDrag(p, t, d, a) }
    val moveDrag: (Offset) -> Unit = { a -> ops?.value?.moveDrag(a) }
    val endDrag: () -> Unit = { ops?.value?.endDrag() }
    val spotlight: () -> Unit = { ops?.value?.spotlight() }
    val minimalSetup: () -> Unit = { ops?.value?.minimalSetup() }
    val minimalDragStart: (String, Offset, Offset, Offset) -> Unit = { p, t, d, a -> ops?.value?.minimalDragStart(p, t, d, a) }
    val minimalRemove: (AppInfo) -> Unit = { a -> ops?.value?.minimalRemove(a.packageName) }
    val clock: () -> Unit = { ops?.value?.widgetTap(WidgetKind.CLOCK) }
    val pod: (String, List<AppInfo>, Rect) -> Unit = { t, a, o -> ops?.value?.pod(t, a, o) }
    val dockIconBounds: (String, Rect) -> Unit = { p, r -> ops?.value?.dockIconBounds(p, r) }
    val edgeFraction: () -> Float = { ops?.value?.edge() ?: 0f }
    val pageForDots: () -> Int = { ops?.value?.pageForDots() ?: 0 }
    val isPaging: () -> Boolean = { ops?.value?.isPaging() ?: false }
}

/** For the carried copy of an item while dragging: looks the same, does nothing. */
private val InertActions = HomeActions(null)

/** A category opened from the App Library and the pod it grows out of. */
private data class OpenPod(val title: String, val apps: List<AppInfo>, val origin: Rect)

/** iOS' standard ease curve for fades. */
private val IosEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** The frosted backdrop is rendered at 1/8 of the screen size. */
private const val FrostDownscale = 8f
