package com.tms.os.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.ui.components.IosAppIcon
import com.tms.os.launcher.ui.components.IosAppLibraryPage
import com.tms.os.launcher.ui.components.IosCategoryExpandedView
import com.tms.os.launcher.ui.components.IosContextMenu
import com.tms.os.launcher.ui.components.IosCustomizeSheet
import com.tms.os.launcher.ui.components.IosFolderIcon
import com.tms.os.launcher.ui.components.IosFolderOverlay
import com.tms.os.launcher.ui.components.IosHomeWidget
import com.tms.os.launcher.ui.components.IosPageDots
import com.tms.os.launcher.ui.components.IosSearchButton
import com.tms.os.launcher.ui.components.IosSpotlightOverlay
import com.tms.os.launcher.ui.components.IosStatusBar
import com.tms.os.launcher.ui.components.IosTopEditBar
import com.tms.os.launcher.ui.theme.IosMetrics
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.WallpaperTone
import com.tms.os.launcher.ui.theme.iosStrings
import com.tms.os.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private data class LaunchTarget(val app: AppInfo, val bounds: Rect)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettingsActivity: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val strings = remember(settings.appLanguage) { iosStrings(settings.appLanguage) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showCustomizeSheet by remember { mutableStateOf(false) }
    var launchTarget by remember { mutableStateOf<LaunchTarget?>(null) }
    var statusBarLightContent by remember { mutableStateOf(true) }
    val launchProgress = remember { Animatable(0f) }

    val lifecycleOwner = LocalLifecycleOwner.current

    suspend fun refreshStatusBarTone() {
        statusBarLightContent = withContext(Dispatchers.IO) {
            !WallpaperTone.isTopBandLight(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshStatusBarTone()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                launchTarget = null
                scope.launch { refreshStatusBarTone() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(launchTarget) {
        val target = launchTarget ?: return@LaunchedEffect
        launchProgress.snapTo(0f)
        launchProgress.animateTo(1f, tween(310, easing = androidx.compose.animation.core.CubicBezierEasing(0.20f, 0.85f, 0.25f, 1f)))
        viewModel.launchApp(target.app.packageName)
        launchTarget = null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isUnfolded = maxWidth >= 600.dp
        val columns = if (isUnfolded) settings.mainUnfoldedColumns.coerceIn(5, 7) else settings.coverColumns.coerceIn(4, 6)
        val rows = if (isUnfolded) settings.mainUnfoldedRows.coerceIn(4, 6) else settings.coverRows.coerceIn(5, 7)
        val showWidgets = settings.showTopWidgetsOnFirstPage
        val widgetRows = 2
        val page0Capacity = if (showWidgets) columns * (rows - widgetRows).coerceAtLeast(2) else columns * rows
        val pageCapacity = columns * rows

        val contentWidth = maxWidth - 46.dp
        val minGap = 15.dp
        val baseIcon = if (isUnfolded) 72.dp else 62.dp
        val widthLimited = (contentWidth - minGap * (columns - 1)) / columns
        val iconSize = minOf(baseIcon, widthLimited) * settings.iconSizeScale
        val dockIconSize = iconSize
        val dockHeight = dockIconSize + 20.dp

        val pages = remember(state.homeGridItems, page0Capacity, pageCapacity) {
            paginateHomeItems(state.homeGridItems, page0Capacity, pageCapacity)
        }

        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { pages.size + 1 }
        )
        val isAppLibrary = pagerState.currentPage == pages.size
        val homePageIndex = pagerState.currentPage.coerceAtMost(pages.lastIndex)

        val anyOverlay = state.isSpotlightOpen || state.activeFolder != null ||
            state.activeCategoryModal != null || state.selectedAppForMenu != null || showCustomizeSheet
        val overlayBlur by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (state.isSpotlightOpen || state.activeFolder != null ||
                state.activeCategoryModal != null || state.selectedAppForMenu != null
            ) 18.dp else 0.dp,
            animationSpec = tween(durationMillis = 200),
            label = "overlayBlur"
        )
        val homeScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (state.selectedAppForMenu != null) 0.96f else 1f,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f),
            label = "homeScale"
        )

        val openAppWithAnimation: (AppInfo, Rect) -> Unit = { app, bounds ->
            if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
            else launchTarget = LaunchTarget(app, bounds)
        }

        fun openFolder(folder: HomeGridItem.FolderItem, bounds: Rect) {
            val width = configuration.screenWidthDp.toFloat().coerceAtLeast(1f)
            val height = configuration.screenHeightDp.toFloat().coerceAtLeast(1f)
            val origin = Offset(
                (bounds.center.x / density.density / width).coerceIn(0.12f, 0.88f),
                (bounds.center.y / density.density / height).coerceIn(0.12f, 0.88f)
            )
            viewModel.openFolder(folder, origin)
        }

        val dockApps = remember(state.dockPackages, state.appMap) {
            state.dockPackages.mapNotNull { state.appMap[it] }
        }
        val recentApps = remember(state.recentPackages, state.appMap) {
            state.recentPackages.mapNotNull { state.appMap[it] }
        }
        val suggestedApps = remember(state.categorizedApps) {
            state.categorizedApps[AppCategory.SUGGESTIONS].orEmpty().take(4)
        }

        // Root downward gesture: notification shade at the very top, Spotlight below.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(anyOverlay, state.isJiggleMode, isAppLibrary, settings.enableSwipeDownSpotlight) {
                    if (anyOverlay || state.isJiggleMode || isAppLibrary) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val start = down.position
                        var totalDx = 0f
                        var totalDy = 0f
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            totalDx = change.position.x - start.x
                            totalDy = change.position.y - start.y
                            if (totalDy > 34f && totalDy > abs(totalDx) * 1.25f) {
                                if (start.y < 52f * density.density) {
                                    viewModel.expandStatusBar()
                                    change.consume()
                                    break
                                } else if (totalDy > 64f * density.density && settings.enableSwipeDownSpotlight) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.openSpotlight()
                                    change.consume()
                                    break
                                }
                            }
                        }
                    }
                }
        ) {
            // Readability scrim for the status bar. iOS decides the glyph colour from
            // the wallpaper; when the wallpaper is ambiguous (bright sky at the top,
            // dark overall) white glyphs would disappear. Drawn behind the content,
            // so icons, widgets and labels are never dimmed.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.30f),
                            0.5f to Color.Black.copy(alpha = 0.15f),
                            1f to Color.Transparent
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = homeScale
                        scaleY = homeScale
                    }
            ) {
                // The status bar itself is drawn last, on top of every overlay, so
                // it stays razor sharp like on iOS. Here we only reserve its space.
                Spacer(Modifier.height(IosMetrics.statusBarHeight))

                // Everything below the status bar blurs behind iOS overlays,
                // while the clock and battery stay razor sharp like on iOS.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .blur(
                            radius = overlayBlur,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                ) {
                    AnimatedVisibility(
                        visible = state.isJiggleMode && !isAppLibrary,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 3 })
                    ) {
                        IosTopEditBar(
                            editLabel = strings.edit,
                            doneLabel = strings.done,
                            onEdit = { showCustomizeSheet = true },
                            onDone = { viewModel.setJiggleMode(false) }
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !state.isJiggleMode,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = PagerSnapDistance.atMost(1)
                        ),
                        beyondViewportPageCount = 1,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { pageIndex ->
                        if (pageIndex == pages.size) {
                            IosAppLibraryPage(
                                allApps = state.allApps,
                                categorizedApps = state.categorizedApps,
                                settings = settings,
                                strings = strings,
                                isUnfolded = isUnfolded,
                                onAppClick = { app -> viewModel.launchApp(app.packageName) },
                                onAppLongClick = { app -> viewModel.selectAppForContextMenu(app) },
                                onCategoryClick = { category -> viewModel.openCategoryModal(category) },
                                onOpenSpotlight = { viewModel.openSpotlight() },
                                onOpenSettings = onOpenSettingsActivity
                            )
                        } else {
                            HomePageContent(
                                items = pages.getOrNull(pageIndex).orEmpty(),
                                columns = columns,
                                rows = if (pageIndex == 0 && showWidgets) (rows - widgetRows).coerceAtLeast(2) else rows,
                                pageIndex = pageIndex,
                                iconSize = iconSize,
                                labelFontSizeSp = settings.labelFontSizeSp,
                                showLabels = settings.showAppLabels,
                                showBadges = settings.enableBadges,
                                isJiggleMode = state.isJiggleMode,
                                showWidgets = pageIndex == 0 && showWidgets,
                                strings = strings,
                                settings = settings,
                                batteryPercent = state.batteryPercent,
                                storageRamInfo = state.storageRamInfo,
                                suggestedApps = suggestedApps,
                                appMap = state.appMap,
                                onAppClickWithBounds = openAppWithAnimation,
                                onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                                onFolderClick = { folder, bounds -> openFolder(folder, bounds) },
                                onFolderLongClick = { folder -> viewModel.selectAppForContextMenu(null) },
                                onDragDrop = { src, dst, merge -> viewModel.onDragDropGridItem(src, dst, merge) },
                                onEmptyClick = { if (state.isJiggleMode) viewModel.setJiggleMode(false) },
                                onEmptyLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setJiggleMode(true)
                                },
                                onWidgetClick = { showCustomizeSheet = true },
                                onWidgetAppClick = { app -> viewModel.launchApp(app.packageName) },
                                onMediaPlayPause = { viewModel.mediaPlayPause() },
                                onMediaNext = { viewModel.mediaNext() }
                            )
                        }
                    }

                    if (!isAppLibrary) {
                        HomeChrome(
                            pageCount = pages.size,
                            currentPage = homePageIndex,
                            strings = strings,
                            showSearch = true,
                            dockApps = dockApps,
                            recentApps = recentApps,
                            isUnfolded = isUnfolded,
                            showRecent = settings.showRecentInUnfoldedDock,
                            showBadges = settings.enableBadges,
                            iconSize = dockIconSize,
                            dockHeight = dockHeight,
                            isJiggleMode = state.isJiggleMode,
                            onSearch = { viewModel.openSpotlight() },
                            onAppClick = openAppWithAnimation,
                            onAppLongClick = { viewModel.selectAppForContextMenu(it) }
                        )
                    }
                }
            }

            // Launch animation (iOS zoom-from-icon)
            launchTarget?.let { target ->
                val p = launchProgress.value
                val screenW = this@BoxWithConstraints.constraints.maxWidth.toFloat()
                val screenH = this@BoxWithConstraints.constraints.maxHeight.toFloat()
                val b = target.bounds
                if (b.width > 0f && b.height > 0f) {
                    val maxScale = max(screenW / b.width, screenH / b.height) * 1.24f
                    val scale = 1f + (maxScale - 1f) * p
                    val dx = (screenW / 2f - b.center.x) * p
                    val dy = (screenH / 2f - b.center.y) * p
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (p * 0.92f).coerceIn(0f, 1f)))
                            .zIndex(40f)
                    ) {
                        val sizeDp = with(density) { b.width.toDp() }
                        val leftDp = with(density) { b.left.toDp() }
                        val topDp = with(density) { b.top.toDp() }
                        if (target.app.iconBitmap != null) {
                            Image(
                                bitmap = target.app.iconBitmap!!,
                                contentDescription = null,
                                modifier = Modifier
                                    .offset(x = leftDp, y = topDp)
                                    .size(sizeDp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = dx
                                        translationY = dy
                                    }
                                    .clip(IosSquircle)
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------- Overlays ----------------------------

        IosSpotlightOverlay(
            visible = state.isSpotlightOpen,
            query = state.spotlightQuery,
            results = state.spotlightResults,
            shortcuts = state.spotlightShortcuts,
            contacts = state.spotlightContacts,
            mathResult = state.spotlightMathResult,
            allApps = state.allApps,
            settings = settings,
            strings = strings,
            onQueryChange = viewModel::updateSpotlightQuery,
            onAppClick = { app -> viewModel.launchApp(app.packageName) },
            onAppLongClick = { app -> viewModel.selectAppForContextMenu(app) },
            onContactClick = { contact -> viewModel.dialContact(contact.phoneNumber) },
            onWebSearch = { query, engine -> viewModel.searchWeb(query, engine) },
            onClose = { viewModel.closeSpotlight() }
        )

        state.activeFolder?.let { folder ->
            IosFolderOverlay(
                visible = true,
                folder = folder,
                appMap = state.appMap,
                originNormalized = state.folderOriginNormalized,
                strings = strings,
                iconSize = if (isUnfolded) 62.dp else 58.dp,
                labelFontSizeSp = settings.labelFontSizeSp,
                onAppClick = { app -> viewModel.launchApp(app.packageName) },
                onAppLongClick = { app -> viewModel.selectAppForContextMenu(app) },
                onRenameFolder = { name -> viewModel.renameFolder(folder.folderId, name) },
                onDissolve = { viewModel.removeFolderAndUnpackApps(folder.folderId) },
                onDismiss = { viewModel.openFolder(null) }
            )
        }

        state.activeCategoryModal?.let { category ->
            IosCategoryExpandedView(
                visible = true,
                category = category,
                apps = state.categorizedApps[category].orEmpty(),
                strings = strings,
                settings = settings,
                onAppClick = { app -> viewModel.launchApp(app.packageName) },
                onAppLongClick = { app -> viewModel.selectAppForContextMenu(app) },
                onDismiss = { viewModel.openCategoryModal(null) }
            )
        }

        state.selectedAppForMenu?.let { app ->
            val folders = remember(state.homeGridItems) {
                state.homeGridItems.filterIsInstance<HomeGridItem.FolderItem>()
            }
            IosContextMenu(
                visible = true,
                app = app,
                isPinnedInDock = state.dockPackages.contains(app.packageName),
                existingFolders = folders,
                strings = strings,
                appLanguage = settings.appLanguage,
                onLaunch = { viewModel.launchApp(app.packageName) },
                onEditHome = {
                    viewModel.selectAppForContextMenu(null)
                    viewModel.setJiggleMode(true)
                },
                onRemoveFromHome = {
                    viewModel.hideOrUnhideApp(app.packageName, true)
                    viewModel.selectAppForContextMenu(null)
                },
                onAppInfo = { viewModel.openAppInfo(app.packageName) },
                onToggleDock = { viewModel.togglePinToDock(app.packageName); viewModel.selectAppForContextMenu(null) },
                onHide = { viewModel.hideOrUnhideApp(app.packageName, true); viewModel.selectAppForContextMenu(null) },
                onUninstall = { viewModel.uninstallApp(app.packageName) },
                onRename = { name -> viewModel.renameApp(app.packageName, name) },
                onChangeCategory = { cat -> viewModel.changeAppCategory(app.packageName, cat) },
                onMoveToFolder = { folderId -> viewModel.moveAppIntoFolder(app.packageName, folderId) },
                onCreateFolder = { title -> viewModel.createNewFolderWithApp(app.packageName, title) },
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

        // The iOS status bar always sits on top of everything — home screen,
        // Spotlight, folders, context menu and sheets alike.
        IosStatusBar(
            lightContent = statusBarLightContent,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    BackHandler(enabled = anyModalHandled(state.isSpotlightOpen, state.activeFolder != null, state.activeCategoryModal != null, state.selectedAppForMenu != null, state.isJiggleMode)) {
        viewModel.handleBackOrHomePress()
    }
}

private fun anyModalHandled(
    spotlight: Boolean,
    folder: Boolean,
    category: Boolean,
    menu: Boolean,
    jiggle: Boolean
): Boolean = spotlight || folder || category || menu || jiggle

private fun paginateHomeItems(
    items: List<HomeGridItem>,
    firstPageCapacity: Int,
    pageCapacity: Int
): List<List<HomeGridItem>> {
    if (items.isEmpty()) return listOf(emptyList())
    val sorted = items.sortedWith(
        compareBy<HomeGridItem>(
            { it.page },
            {
                when (it) {
                    is HomeGridItem.AppItem -> it.position
                    is HomeGridItem.FolderItem -> it.position
                }
            }
        )
    )
    val pages = mutableListOf<List<HomeGridItem>>()
    pages.add(sorted.take(firstPageCapacity))
    val rest = sorted.drop(firstPageCapacity)
    rest.chunked(pageCapacity.coerceAtLeast(1)).forEach { pages.add(it) }
    return pages
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePageContent(
    items: List<HomeGridItem>,
    columns: Int,
    rows: Int,
    pageIndex: Int,
    iconSize: Dp,
    labelFontSizeSp: Float,
    showLabels: Boolean,
    showBadges: Boolean,
    isJiggleMode: Boolean,
    showWidgets: Boolean,
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    settings: com.tms.os.launcher.model.LauncherSettings,
    batteryPercent: Int,
    storageRamInfo: com.tms.os.launcher.data.SystemStorageRamInfo,
    suggestedApps: List<AppInfo>,
    appMap: Map<String, AppInfo>,
    onAppClickWithBounds: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderClick: (HomeGridItem.FolderItem, Rect) -> Unit,
    onFolderLongClick: (HomeGridItem.FolderItem) -> Unit,
    onDragDrop: (String, String, Boolean) -> Unit,
    onEmptyClick: () -> Unit,
    onEmptyLongClick: () -> Unit,
    onWidgetClick: () -> Unit,
    onWidgetAppClick: (AppInfo) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val slotBounds = remember(pageIndex) { mutableStateMapOf<Int, Rect>() }
    var draggedIndex by remember(pageIndex) { mutableStateOf<Int?>(null) }
    var dragTranslation by remember(pageIndex) { mutableStateOf(Offset.Zero) }
    var hoveredIndex by remember(pageIndex) { mutableStateOf<Int?>(null) }
    var mergeHover by remember(pageIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEmptyClick,
                onLongClick = onEmptyLongClick
            )
            .padding(horizontal = 23.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        if (showWidgets) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                IosHomeWidget(
                    leftType = settings.leftWidgetType,
                    rightType = settings.rightWidgetType,
                    isLeft = true,
                    strings = strings,
                    weatherCity = settings.weatherCity,
                    weatherTemp = settings.weatherTempCelsius,
                    weatherHigh = settings.weatherHighCelsius,
                    weatherLow = settings.weatherLowCelsius,
                    weatherCondition = settings.weatherCondition,
                    weatherCode = settings.weatherCode,
                    useFahrenheit = settings.useFahrenheit,
                    batteryPercent = batteryPercent,
                    quickNoteText = settings.quickNoteText,
                    suggestedApps = suggestedApps,
                    usedRamGb = storageRamInfo.usedRamGb,
                    totalRamGb = storageRamInfo.totalRamGb,
                    usedStorageGb = storageRamInfo.usedStorageGb,
                    totalStorageGb = storageRamInfo.totalStorageGb,
                    modifier = Modifier.weight(1f),
                    onClick = onWidgetClick,
                    onClickApp = onWidgetAppClick,
                    onMediaPlayPause = onMediaPlayPause,
                    onMediaNext = onMediaNext
                )
                IosHomeWidget(
                    leftType = settings.leftWidgetType,
                    rightType = settings.rightWidgetType,
                    isLeft = false,
                    strings = strings,
                    weatherCity = settings.weatherCity,
                    weatherTemp = settings.weatherTempCelsius,
                    weatherHigh = settings.weatherHighCelsius,
                    weatherLow = settings.weatherLowCelsius,
                    weatherCondition = settings.weatherCondition,
                    weatherCode = settings.weatherCode,
                    useFahrenheit = settings.useFahrenheit,
                    batteryPercent = batteryPercent,
                    quickNoteText = settings.quickNoteText,
                    suggestedApps = suggestedApps,
                    usedRamGb = storageRamInfo.usedRamGb,
                    totalRamGb = storageRamInfo.totalRamGb,
                    usedStorageGb = storageRamInfo.usedStorageGb,
                    totalStorageGb = storageRamInfo.totalStorageGb,
                    modifier = Modifier.weight(1f),
                    onClick = onWidgetClick,
                    onClickApp = onWidgetAppClick,
                    onMediaPlayPause = onMediaPlayPause,
                    onMediaNext = onMediaNext
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            for (row in 0 until rows) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        val item = items.getOrNull(index)
                        val isDragged = draggedIndex == index
                        val isTarget = hoveredIndex == index && !isDragged

                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (isDragged) 20f else 0f)
                                .onGloballyPositioned { coords ->
                                    slotBounds[index] = coords.boundsInRoot()
                                }
                                .graphicsLayer {
                                    if (isDragged) {
                                        translationX = dragTranslation.x
                                        translationY = dragTranslation.y
                                        scaleX = 1.13f
                                        scaleY = 1.13f
                                        alpha = 0.92f
                                    }
                                }
                                .pointerInput(isJiggleMode, item?.id) {
                                    if (!isJiggleMode || item == null) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedIndex = index
                                            dragTranslation = Offset.Zero
                                            hoveredIndex = null
                                            mergeHover = false
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragTranslation += amount
                                            val startRect = slotBounds[index]
                                            if (startRect != null) {
                                                val center = startRect.center + dragTranslation
                                                var target: Int? = null
                                                var merge = false
                                                for ((idx, rect) in slotBounds) {
                                                    if (idx != index && rect.contains(center)) {
                                                        target = idx
                                                        merge = hypot(
                                                            center.x - rect.center.x,
                                                            center.y - rect.center.y
                                                        ) <= rect.width * 0.26f
                                                        break
                                                    }
                                                }
                                                hoveredIndex = target
                                                mergeHover = merge
                                            }
                                        },
                                        onDragEnd = {
                                            val targetIdx = hoveredIndex
                                            val targetItem = if (targetIdx != null) items.getOrNull(targetIdx) else null
                                            if (targetItem != null && item.id != targetItem.id) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDragDrop(item.id, targetItem.id, mergeHover)
                                            }
                                            draggedIndex = null
                                            dragTranslation = Offset.Zero
                                            hoveredIndex = null
                                            mergeHover = false
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragTranslation = Offset.Zero
                                            hoveredIndex = null
                                            mergeHover = false
                                        }
                                    )
                                }
                        ) {
                            when (item) {
                                is HomeGridItem.AppItem -> {
                                    appMap[item.packageName]?.let { app ->
                                        IosAppIcon(
                                            app = app,
                                            iconSize = iconSize,
                                            labelFontSizeSp = labelFontSizeSp,
                                            showLabel = showLabels,
                                            showBadges = showBadges,
                                            isJiggleMode = isJiggleMode,
                                            isHighlightedTarget = isTarget && mergeHover,
                                            onClick = { onAppClickWithBounds(app, slotBounds[index] ?: Rect.Zero) },
                                            onClickWithBounds = { bounds -> onAppClickWithBounds(app, bounds) },
                                            onLongClick = { onAppLongClick(app) },
                                            onDeleteOrMenuClick = { onAppLongClick(app) }
                                        )
                                    }
                                }
                                is HomeGridItem.FolderItem -> {
                                    IosFolderIcon(
                                        folder = item,
                                        appMap = appMap,
                                        iconSize = iconSize,
                                        labelFontSizeSp = labelFontSizeSp,
                                        showLabel = showLabels,
                                        showBadges = showBadges,
                                        isJiggleMode = isJiggleMode,
                                        isHighlightedTarget = isTarget,
                                        onClickWithBounds = { bounds -> onFolderClick(item, bounds) },
                                        onLongClick = { onFolderLongClick(item) }
                                    )
                                }
                                null -> {
                                    Spacer(Modifier.size(iconSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeChrome(
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
    isJiggleMode: Boolean,
    onSearch: () -> Unit,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 2.dp)
    ) {
        IosPageDots(count = pageCount, selected = currentPage)

        Spacer(Modifier.height(7.dp))

        // iOS keeps the space when the search pill is only shown on page one.
        if (currentPage == 0 && showSearch) {
            IosSearchButton(text = strings.search, onClick = onSearch)
        } else {
            Spacer(Modifier.height(34.dp))
        }

        Spacer(Modifier.height(9.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .then(if (isUnfolded) Modifier.fillMaxWidth(0.84f) else Modifier.fillMaxWidth())
                    .height(dockHeight)
                    .shadow(18.dp, RoundedCornerShape(39.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.22f))
                    .clip(RoundedCornerShape(39.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .background(Color(0x4D1A1A1F))
                    .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(39.dp))
                    .padding(horizontal = 8.dp)
            ) {
                dockApps.forEach { app ->
                    IosAppIcon(
                        app = app,
                        iconSize = iconSize,
                        showLabel = false,
                        showBadges = showBadges,
                        isJiggleMode = isJiggleMode,
                        onClick = { onAppClick(app, Rect.Zero) },
                        onClickWithBounds = { bounds -> onAppClick(app, bounds) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
                if (isUnfolded && showRecent && recentApps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(iconSize * 0.55f)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    recentApps.take(3).forEach { app ->
                        IosAppIcon(
                            app = app,
                            iconSize = iconSize,
                            showLabel = false,
                            showBadges = showBadges,
                            isJiggleMode = false,
                            onClick = { onAppClick(app, Rect.Zero) },
                            onClickWithBounds = { bounds -> onAppClick(app, bounds) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    }
                }
            }
        }
    }
}
