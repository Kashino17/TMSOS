package com.tms.os.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tms.os.launcher.data.SystemStorageRamInfo
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.FoldPostureMode
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.LeftWidgetType
import com.tms.os.launcher.model.PageTransitionEffect
import com.tms.os.launcher.model.RightWidgetType
import com.tms.os.launcher.ui.components.IosAppIconView
import com.tms.os.launcher.ui.components.IosAppLibraryScreen
import com.tms.os.launcher.ui.components.IosCategoryExpandedModal
import com.tms.os.launcher.ui.components.IosExpandedFolderModal
import com.tms.os.launcher.ui.components.IosFolderGridIconView
import com.tms.os.launcher.ui.components.IosFrostedDock
import com.tms.os.launcher.ui.components.IosHapticTouchContextMenu
import com.tms.os.launcher.ui.components.IosTopWidgetsHeader
import com.tms.os.launcher.ui.components.IosWidgetGalleryBottomSheet
import com.tms.os.launcher.ui.components.SpotlightSearchOverlay
import com.tms.os.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettingsActivity: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val settings = state.settings
    var showWidgetGallerySheet by remember { mutableStateOf(false) }

    val anyModalOpen = state.isAnyOverlayOpen || showWidgetGallerySheet

    // Authentic iOS Depth-of-Field Scale & Hardware Blur when any overlay opens
    val homeScale by animateFloatAsState(
        targetValue = if (state.isSpotlightOpen || state.activeFolder != null || state.activeCategoryModal != null || state.selectedAppForMenu != null) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "homeDepthScale"
    )

    val homeBlurDp by animateDpAsState(
        targetValue = if (state.isSpotlightOpen || state.activeFolder != null || state.activeCategoryModal != null || state.selectedAppForMenu != null) (20.dp * settings.blurIntensity) else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "homeBlur"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.14f),
                        Color.Black.copy(alpha = 0.06f),
                        Color.Black.copy(alpha = 0.24f)
                    )
                )
            )
    ) {
        val isUnfolded = maxWidth >= 580.dp

        val columns = when {
            !isUnfolded -> settings.coverColumns
            settings.foldMode == FoldPostureMode.IPAD_GRID -> settings.mainUnfoldedColumns
            else -> settings.coverColumns
        }
        val rows = when {
            !isUnfolded -> settings.coverRows
            settings.foldMode == FoldPostureMode.IPAD_GRID -> settings.mainUnfoldedRows
            else -> settings.coverRows
        }

        val itemsPerPage = max(columns * rows, 12)
        val firstPageRows = if (settings.showTopWidgetsOnFirstPage) max(rows - 2, 2) else rows
        val page0Capacity = max(columns * firstPageRows, 8)

        val pages: List<List<HomeGridItem>> = remember(
            state.homeGridItems,
            page0Capacity,
            itemsPerPage,
            settings.showTopWidgetsOnFirstPage
        ) {
            if (state.homeGridItems.isEmpty()) {
                listOf(emptyList())
            } else {
                val hasFolders = state.homeGridItems.any { it is HomeGridItem.FolderItem }
                val explicitPage0 = state.homeGridItems.filter { it.page == 0 }
                val explicitPage1Plus = state.homeGridItems.filter { it.page >= 1 }

                if (hasFolders && explicitPage0.isNotEmpty() && explicitPage1Plus.isNotEmpty()) {
                    val result = mutableListOf<List<HomeGridItem>>()
                    result.add(explicitPage0.take(page0Capacity))
                    val overflowPage0 = explicitPage0.drop(page0Capacity)
                    val combinedRest = overflowPage0 + explicitPage1Plus
                    combinedRest.chunked(itemsPerPage).forEach { result.add(it) }
                    result
                } else {
                    val result = mutableListOf<List<HomeGridItem>>()
                    val firstPage = state.homeGridItems.take(page0Capacity)
                    result.add(firstPage)
                    val rest = state.homeGridItems.drop(page0Capacity)
                    rest.chunked(itemsPerPage).forEach { chunk ->
                        result.add(chunk)
                    }
                    result
                }
            }
        }

        val isBookSplitMode = isUnfolded && settings.foldMode == FoldPostureMode.BOOK_SPLIT
        val homeSpreadsCount = if (isBookSplitMode) {
            max((pages.size + 1) / 2, 1)
        } else {
            pages.size
        }

        val totalPagerPages = homeSpreadsCount + 1
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { totalPagerPages }
        )

        val isOnAppLibraryPage = pagerState.currentPage == totalPagerPages - 1

        // Dynamic Full-Size iOS Icon Calculation matching Screenshot 3 & 4 (68.dp on 4-column cover screen!)
        val effectiveCols = if (isBookSplitMode) columns * 2 else columns
        val cellWidth = (maxWidth - 28.dp) / effectiveCols.coerceAtLeast(3)
        val baseIconSize = (cellWidth * 0.74f).coerceIn(62.dp, 74.dp) * settings.iconSizeScale

        val suggestedApps = remember(state.categorizedApps) {
            state.categorizedApps[AppCategory.SUGGESTIONS].orEmpty().take(4)
        }

        // BACKGROUND HOME LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = homeScale
                    scaleY = homeScale
                }
                .blur(homeBlurDp)
                .statusBarsPadding()
                .navigationBarsPadding()
                // Intercept downward swipe in Initial pass so swiping down ANYWHERE (even on top of an icon!) opens Spotlight reliably!
                .pointerInput(
                    anyModalOpen,
                    state.isJiggleMode,
                    isOnAppLibraryPage,
                    settings.enableSwipeDownSpotlight
                ) {
                    if (anyModalOpen || state.isJiggleMode || isOnAppLibraryPage) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val startPos = down.position
                        var totalDx = 0f
                        var totalDy = 0f
                        var triggered = false

                        while (!triggered) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            totalDx = change.position.x - startPos.x
                            totalDy = change.position.y - startPos.y

                            if (totalDy > 54f && totalDy > totalDx.absoluteValue * 1.35f) {
                                triggered = true
                                change.consume()
                                if (startPos.y < 90f) {
                                    viewModel.expandStatusBar()
                                } else if (settings.enableSwipeDownSpotlight) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.openSpotlight()
                                }
                            }
                        }
                    }
                }
        ) {
            // 1:1 Authentic iOS Jiggle Mode Top Bar: "Bearbeiten" (left) & "Fertig" (right) matching Screenshot 3!
            AnimatedVisibility(visible = state.isJiggleMode && !isOnAppLibraryPage) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    // Left Pill: "Bearbeiten" -> Opens the iOS Widget Gallery Sheet (Screenshot 1!)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x992C2C2E))
                            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showWidgetGallerySheet = true
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Bearbeiten",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Right Pill: "Fertig"
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x992C2C2E))
                            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                            .clickable {
                                viewModel.setJiggleMode(false)
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Fertig",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Horizontal Pager (Home Screen Pages + App-Bibliothek on far right)
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !state.isJiggleMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val rawOffset = (pageIndex - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                val absOffset = rawOffset.absoluteValue

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            when (settings.pageTransition) {
                                PageTransitionEffect.IOS_SLIDE -> {
                                    alpha = 1f
                                }
                                PageTransitionEffect.CUBE_3D -> {
                                    rotationY = rawOffset * 32f
                                    cameraDistance = 14f * density
                                }
                                PageTransitionEffect.DEPTH_STACK -> {
                                    val scale = 1f - (absOffset * 0.14f).coerceIn(0f, 0.22f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (absOffset * 0.35f).coerceIn(0f, 0.55f)
                                }
                                PageTransitionEffect.ZOOM_FADE -> {
                                    val scale = 0.86f + (1f - absOffset.coerceIn(0f, 1f)) * 0.14f
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (absOffset * 0.45f).coerceIn(0f, 0.65f)
                                }
                                PageTransitionEffect.CAROUSEL_ROTATE -> {
                                    rotationZ = rawOffset * 8f
                                    translationY = absOffset * 36f
                                }
                            }
                        }
                ) {
                    if (pageIndex == totalPagerPages - 1) {
                        IosAppLibraryScreen(
                            categorizedApps = state.categorizedApps,
                            allApps = state.allApps,
                            isUnfolded = isUnfolded,
                            glassOpacity = settings.glassOpacity,
                            appLanguage = settings.appLanguage,
                            onAppClick = { viewModel.launchApp(it.packageName) },
                            onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                            onCategoryExpandClick = { viewModel.openCategoryModal(it) },
                            onOpenSettingsClick = onOpenSettingsActivity
                        )
                    } else if (isBookSplitMode) {
                        val leftIdx = pageIndex * 2
                        val rightIdx = leftIdx + 1
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                val showWidgetsLeft = (leftIdx == 0 && settings.showTopWidgetsOnFirstPage)
                                HomeGridPageContent(
                                    pageItems = pages.getOrNull(leftIdx).orEmpty(),
                                    showWidgets = showWidgetsLeft,
                                    columns = columns,
                                    slotRows = if (showWidgetsLeft) firstPageRows else rows,
                                    iconSize = baseIconSize,
                                    labelFontSizeSp = settings.labelFontSizeSp,
                                    glassOpacity = settings.glassOpacity,
                                    showLabels = settings.showAppLabels,
                                    showBadges = settings.enableBadges,
                                    isJiggleMode = state.isJiggleMode,
                                    leftWidgetType = settings.leftWidgetType,
                                    rightWidgetType = settings.rightWidgetType,
                                    batteryPercent = state.batteryPercent,
                                    storageRamInfo = state.storageRamInfo,
                                    quickNoteText = settings.quickNoteText,
                                    suggestedApps = suggestedApps,
                                    weatherCity = settings.weatherCity,
                                    weatherTemp = settings.weatherTempCelsius,
                                    weatherHigh = settings.weatherHighCelsius,
                                    weatherLow = settings.weatherLowCelsius,
                                    weatherCondition = settings.weatherCondition,
                                    useFahrenheit = settings.useFahrenheit,
                                    isWeatherRefreshing = state.isWeatherRefreshing,
                                    appMap = state.appMap,
                                    onAppClick = { app ->
                                        if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                                        else viewModel.launchApp(app.packageName)
                                    },
                                    onAppLongClick = { app ->
                                        viewModel.selectAppForContextMenu(app)
                                    },
                                    onFolderClickWithOrigin = { folder, origin ->
                                        viewModel.openFolder(folder, origin)
                                    },
                                    onFolderLongClickWithOrigin = { folder, origin ->
                                        viewModel.openFolder(folder, origin)
                                    },
                                    onDragDropItem = { src, dst, merge ->
                                        viewModel.onDragDropGridItem(src, dst, merge)
                                    },
                                    onEmptySpaceClick = {
                                        if (state.isJiggleMode) viewModel.setJiggleMode(false)
                                    },
                                    onEmptySpaceLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setJiggleMode(!state.isJiggleMode)
                                    },
                                    onCycleLeftWidget = { dir -> viewModel.cycleLeftWidget(dir) },
                                    onCycleRightWidget = { dir -> viewModel.cycleRightWidget(dir) },
                                    onQuickNoteChange = { txt -> viewModel.updateQuickNote(txt) },
                                    onMediaPlayPause = { viewModel.mediaPlayPause() },
                                    onMediaNext = { viewModel.mediaNext() },
                                    onMediaPrev = { viewModel.mediaPrevious() },
                                    onWeatherClick = { showWidgetGallerySheet = true },
                                    onRefreshWeatherClick = { viewModel.refreshLiveWeatherIfNeeded(force = true) }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight(0.88f)
                                    .align(Alignment.CenterVertically)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                HomeGridPageContent(
                                    pageItems = pages.getOrNull(rightIdx).orEmpty(),
                                    showWidgets = false,
                                    columns = columns,
                                    slotRows = rows,
                                    iconSize = baseIconSize,
                                    labelFontSizeSp = settings.labelFontSizeSp,
                                    glassOpacity = settings.glassOpacity,
                                    showLabels = settings.showAppLabels,
                                    showBadges = settings.enableBadges,
                                    isJiggleMode = state.isJiggleMode,
                                    leftWidgetType = settings.leftWidgetType,
                                    rightWidgetType = settings.rightWidgetType,
                                    batteryPercent = state.batteryPercent,
                                    storageRamInfo = state.storageRamInfo,
                                    quickNoteText = settings.quickNoteText,
                                    suggestedApps = suggestedApps,
                                    weatherCity = settings.weatherCity,
                                    weatherTemp = settings.weatherTempCelsius,
                                    weatherHigh = settings.weatherHighCelsius,
                                    weatherLow = settings.weatherLowCelsius,
                                    weatherCondition = settings.weatherCondition,
                                    useFahrenheit = settings.useFahrenheit,
                                    isWeatherRefreshing = state.isWeatherRefreshing,
                                    appMap = state.appMap,
                                    onAppClick = { app ->
                                        if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                                        else viewModel.launchApp(app.packageName)
                                    },
                                    onAppLongClick = { app ->
                                        viewModel.selectAppForContextMenu(app)
                                    },
                                    onFolderClickWithOrigin = { folder, origin ->
                                        viewModel.openFolder(folder, origin)
                                    },
                                    onFolderLongClickWithOrigin = { folder, origin ->
                                        viewModel.openFolder(folder, origin)
                                    },
                                    onDragDropItem = { src, dst, merge ->
                                        viewModel.onDragDropGridItem(src, dst, merge)
                                    },
                                    onEmptySpaceClick = {
                                        if (state.isJiggleMode) viewModel.setJiggleMode(false)
                                    },
                                    onEmptySpaceLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setJiggleMode(!state.isJiggleMode)
                                    },
                                    onCycleLeftWidget = {},
                                    onCycleRightWidget = {},
                                    onQuickNoteChange = {},
                                    onMediaPlayPause = {},
                                    onMediaNext = {},
                                    onMediaPrev = {},
                                    onWeatherClick = { showWidgetGallerySheet = true },
                                    onRefreshWeatherClick = { viewModel.refreshLiveWeatherIfNeeded(force = true) }
                                )
                            }
                        }
                    } else {
                        val showWidgetsOnPage = (pageIndex == 0 && settings.showTopWidgetsOnFirstPage)
                        HomeGridPageContent(
                            pageItems = pages.getOrNull(pageIndex).orEmpty(),
                            showWidgets = showWidgetsOnPage,
                            columns = columns,
                            slotRows = if (showWidgetsOnPage) firstPageRows else rows,
                            iconSize = baseIconSize,
                            labelFontSizeSp = settings.labelFontSizeSp,
                            glassOpacity = settings.glassOpacity,
                            showLabels = settings.showAppLabels,
                            showBadges = settings.enableBadges,
                            isJiggleMode = state.isJiggleMode,
                            leftWidgetType = settings.leftWidgetType,
                            rightWidgetType = settings.rightWidgetType,
                            batteryPercent = state.batteryPercent,
                            storageRamInfo = state.storageRamInfo,
                            quickNoteText = settings.quickNoteText,
                            suggestedApps = suggestedApps,
                            weatherCity = settings.weatherCity,
                            weatherTemp = settings.weatherTempCelsius,
                            weatherHigh = settings.weatherHighCelsius,
                            weatherLow = settings.weatherLowCelsius,
                            weatherCondition = settings.weatherCondition,
                            useFahrenheit = settings.useFahrenheit,
                            isWeatherRefreshing = state.isWeatherRefreshing,
                            appMap = state.appMap,
                            onAppClick = { app ->
                                if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                                else viewModel.launchApp(app.packageName)
                            },
                            onAppLongClick = { app ->
                                viewModel.selectAppForContextMenu(app)
                            },
                            onFolderClickWithOrigin = { folder, origin ->
                                viewModel.openFolder(folder, origin)
                            },
                            onFolderLongClickWithOrigin = { folder, origin ->
                                viewModel.openFolder(folder, origin)
                            },
                            onDragDropItem = { src, dst, merge ->
                                viewModel.onDragDropGridItem(src, dst, merge)
                            },
                            onEmptySpaceClick = {
                                if (state.isJiggleMode) viewModel.setJiggleMode(false)
                            },
                            onEmptySpaceLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setJiggleMode(!state.isJiggleMode)
                            },
                            onCycleLeftWidget = { dir -> viewModel.cycleLeftWidget(dir) },
                            onCycleRightWidget = { dir -> viewModel.cycleRightWidget(dir) },
                            onQuickNoteChange = { txt -> viewModel.updateQuickNote(txt) },
                            onMediaPlayPause = { viewModel.mediaPlayPause() },
                            onMediaNext = { viewModel.mediaNext() },
                            onMediaPrev = { viewModel.mediaPrevious() },
                            onWeatherClick = { showWidgetGallerySheet = true },
                            onRefreshWeatherClick = { viewModel.refreshLiveWeatherIfNeeded(force = true) }
                        )
                    }
                }
            }

            // Bottom Section: Authentic iOS Page Dots Pill (`• • •`) + Frosted Glass Dock (Exact match to Screenshot 3 & 4!)
            if (!isOnAppLibraryPage) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0x882C2C2E))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable { viewModel.openSpotlight() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        for (i in 0 until totalPagerPages) {
                            val selected = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.5.dp)
                                    .size(6.5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) Color.White
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }

                    val dockApps = remember(state.dockPackages, state.appMap) {
                        state.dockPackages.mapNotNull { state.appMap[it] }
                    }
                    val recentApps = remember(state.recentPackages, state.appMap) {
                        state.recentPackages.mapNotNull { state.appMap[it] }
                    }

                    IosFrostedDock(
                        dockApps = dockApps,
                        recentApps = recentApps,
                        isUnfolded = isUnfolded,
                        showRecentInUnfolded = settings.showRecentInUnfoldedDock,
                        showLabels = settings.showDockLabels,
                        showBadges = settings.enableBadges,
                        glassOpacity = settings.glassOpacity,
                        iconSize = baseIconSize,
                        labelFontSizeSp = settings.labelFontSizeSp,
                        isJiggleMode = state.isJiggleMode,
                        onAppClick = { app ->
                            if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                            else viewModel.launchApp(app.packageName)
                        },
                        onAppLongClick = { viewModel.selectAppForContextMenu(it) }
                    )
                }
            }
        }

        // OVERLAYS

        // 0. Authentic iOS Widget Gallery Bottom Sheet (Screenshot 1!)
        AnimatedVisibility(
            visible = showWidgetGallerySheet,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            IosWidgetGalleryBottomSheet(
                weatherCity = settings.weatherCity,
                weatherTempCelsius = settings.weatherTempCelsius,
                weatherHighCelsius = settings.weatherHighCelsius,
                weatherLowCelsius = settings.weatherLowCelsius,
                weatherCondition = settings.weatherCondition,
                useFahrenheit = settings.useFahrenheit,
                batteryPercent = state.batteryPercent,
                showWidgetsOnHome = settings.showTopWidgetsOnFirstPage,
                onSelectWidgetsForHome = { leftW, rightW ->
                    viewModel.updateSettings {
                        it.copy(
                            showTopWidgetsOnFirstPage = true,
                            leftWidgetType = leftW,
                            rightWidgetType = rightW
                        )
                    }
                },
                onToggleWidgetsOnHome = { show ->
                    viewModel.updateSettings { it.copy(showTopWidgetsOnFirstPage = show) }
                },
                onAutoSortSmartFolders = {
                    viewModel.autoOrganizeHomeWithSmartFolders()
                },
                onOpenSettings = onOpenSettingsActivity,
                onDismiss = { showWidgetGallerySheet = false }
            )
        }

        // 1. Expanded Folder Overlay (with Origin-based iOS Spring Zoom!)
        val folderOrigin = TransformOrigin(
            state.folderOriginNormalized.x.coerceIn(0.1f, 0.9f),
            state.folderOriginNormalized.y.coerceIn(0.1f, 0.9f)
        )
        AnimatedVisibility(
            visible = state.activeFolder != null,
            enter = fadeIn() + scaleIn(
                initialScale = 0.25f,
                transformOrigin = folderOrigin,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut() + scaleOut(
                targetScale = 0.25f,
                transformOrigin = folderOrigin,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            )
        ) {
            state.activeFolder?.let { folder ->
                IosExpandedFolderModal(
                    folder = folder,
                    appMap = state.appMap,
                    originNormalized = state.folderOriginNormalized,
                    onAppClick = { viewModel.launchApp(it.packageName) },
                    onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                    onRenameFolder = { newName -> viewModel.renameFolder(folder.folderId, newName) },
                    onUnpackFolder = { viewModel.removeFolderAndUnpackApps(folder.folderId) },
                    onDismiss = { viewModel.openFolder(null) }
                )
            }
        }

        // 2. Expanded Smart Category Overlay (from App Library)
        AnimatedVisibility(
            visible = state.activeCategoryModal != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            state.activeCategoryModal?.let { category ->
                val catApps = state.categorizedApps[category].orEmpty()
                IosCategoryExpandedModal(
                    category = category,
                    apps = catApps,
                    appLanguage = settings.appLanguage,
                    onAppClick = { viewModel.launchApp(it.packageName) },
                    onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                    onDismiss = { viewModel.openCategoryModal(null) }
                )
            }
        }

        // 3. Spotlight Search Overlay (Swipe Down or Page Pill Tap)
        AnimatedVisibility(
            visible = state.isSpotlightOpen,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { if (settings.searchBarAtBottomInSpotlight) it / 6 else -it / 5 }
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { if (settings.searchBarAtBottomInSpotlight) it / 6 else -it / 5 }
            )
        ) {
            SpotlightSearchOverlay(
                query = state.spotlightQuery,
                results = state.spotlightResults,
                shortcuts = state.spotlightShortcuts,
                contacts = state.spotlightContacts,
                mathResult = state.spotlightMathResult,
                autoOpenKeyboard = settings.autoOpenKeyboardInSpotlight,
                showWebSearch = settings.showWebSearchInSpotlight,
                searchBarAtBottom = settings.searchBarAtBottomInSpotlight,
                onQueryChange = { viewModel.updateSpotlightQuery(it) },
                onAppClick = { viewModel.launchApp(it.packageName) },
                onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                onContactClick = { contact -> viewModel.dialContact(contact.phoneNumber) },
                onWebSearchClick = { q, engine -> viewModel.searchWeb(q, engine) },
                onClose = { viewModel.closeSpotlight() }
            )
        }

        // 4. Haptic Touch App Context Menu Overlay
        AnimatedVisibility(
            visible = state.selectedAppForMenu != null,
            enter = fadeIn() + scaleIn(initialScale = 0.90f),
            exit = fadeOut() + scaleOut(targetScale = 0.90f)
        ) {
            state.selectedAppForMenu?.let { app ->
                val existingFolders = remember(state.homeGridItems) {
                    state.homeGridItems.filterIsInstance<HomeGridItem.FolderItem>()
                }
                IosHapticTouchContextMenu(
                    app = app,
                    isPinnedInDock = state.dockPackages.contains(app.packageName),
                    isPinnedOnPageOne = app.isPinnedOnPageOne,
                    existingFolders = existingFolders,
                    onLaunchApp = { viewModel.launchApp(app.packageName) },
                    onToggleDockPin = {
                        viewModel.togglePinToDock(app.packageName)
                        viewModel.selectAppForContextMenu(null)
                    },
                    onTogglePinPageOne = {
                        viewModel.togglePinOnPageOne(app.packageName)
                    },
                    onMovePositionLeft = { viewModel.moveGridItemPosition("app:${app.packageName}", -1) },
                    onMovePositionRight = { viewModel.moveGridItemPosition("app:${app.packageName}", +1) },
                    onEnterJiggleMode = { viewModel.setJiggleMode(true) },
                    onChangeCategory = { newCat -> viewModel.changeAppCategory(app.packageName, newCat) },
                    onMoveToFolder = { folderId -> viewModel.moveAppIntoFolder(app.packageName, folderId) },
                    onCreateNewFolder = { title -> viewModel.createNewFolderWithApp(app.packageName, title) },
                    onRenameApp = { newName -> viewModel.renameApp(app.packageName, newName) },
                    onHideApp = {
                        viewModel.hideOrUnhideApp(app.packageName, true)
                        viewModel.selectAppForContextMenu(null)
                    },
                    onOpenAppInfo = { viewModel.openAppInfo(app.packageName) },
                    onUninstallApp = { viewModel.uninstallApp(app.packageName) },
                    onDismiss = { viewModel.selectAppForContextMenu(null) }
                )
            }
        }
    }
}

/**
 * Top-aligned, fixed-rhythm iOS Grid matching Screenshot 3 & 4 (1794491112605_643096.jpg & 1794491112481_643094.jpg)!
 * Never stretches rows with weight(1f) so icons sit at their exact iOS coordinates from the top down.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeGridPageContent(
    pageItems: List<HomeGridItem>,
    showWidgets: Boolean,
    columns: Int,
    slotRows: Int,
    iconSize: Dp,
    labelFontSizeSp: Float,
    glassOpacity: Float,
    showLabels: Boolean,
    showBadges: Boolean,
    isJiggleMode: Boolean,
    leftWidgetType: LeftWidgetType,
    rightWidgetType: RightWidgetType,
    batteryPercent: Int,
    storageRamInfo: SystemStorageRamInfo,
    quickNoteText: String,
    suggestedApps: List<AppInfo>,
    weatherCity: String,
    weatherTemp: Int,
    weatherHigh: Int,
    weatherLow: Int,
    weatherCondition: String,
    useFahrenheit: Boolean,
    isWeatherRefreshing: Boolean,
    appMap: Map<String, AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderClickWithOrigin: (HomeGridItem.FolderItem, Offset) -> Unit,
    onFolderLongClickWithOrigin: (HomeGridItem.FolderItem, Offset) -> Unit,
    onDragDropItem: (String, String, Boolean) -> Unit,
    onEmptySpaceClick: () -> Unit,
    onEmptySpaceLongClick: () -> Unit,
    onCycleLeftWidget: (Int) -> Unit,
    onCycleRightWidget: (Int) -> Unit,
    onQuickNoteChange: (String) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onWeatherClick: () -> Unit,
    onRefreshWeatherClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var draggedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var dragTranslation by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetSlot by remember { mutableStateOf<Int?>(null) }
    var isMergeHover by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEmptySpaceClick,
                onLongClick = onEmptySpaceLongClick
            )
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        if (showWidgets) {
            IosTopWidgetsHeader(
                leftWidgetType = leftWidgetType,
                rightWidgetType = rightWidgetType,
                batteryPercent = batteryPercent,
                storageRamInfo = storageRamInfo,
                quickNoteText = quickNoteText,
                suggestedApps = suggestedApps,
                weatherCity = weatherCity,
                weatherTempCelsius = weatherTemp,
                weatherHighCelsius = weatherHigh,
                weatherLowCelsius = weatherLow,
                weatherCondition = weatherCondition,
                useFahrenheit = useFahrenheit,
                isWeatherRefreshing = isWeatherRefreshing,
                onCycleLeftWidget = onCycleLeftWidget,
                onCycleRightWidget = onCycleRightWidget,
                onQuickNoteChange = onQuickNoteChange,
                onSuggestedAppClick = onAppClick,
                onMediaPlayPause = onMediaPlayPause,
                onMediaNext = onMediaNext,
                onMediaPrev = onMediaPrev,
                onWeatherClick = onWeatherClick,
                onRefreshWeatherClick = onRefreshWeatherClick
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Top-aligned rows with crisp 14.dp vertical spacing (exact match to Screenshot 3 & 4!)
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            for (rowIdx in 0 until slotRows) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (colIdx in 0 until columns) {
                        val itemIndex = rowIdx * columns + colIdx
                        val item = pageItems.getOrNull(itemIndex)
                        val isBeingDragged = draggedSlotIndex == itemIndex
                        val isTargetHighlighted = hoveredTargetSlot == itemIndex && !isBeingDragged

                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (isBeingDragged) 20f else 0f)
                                .onGloballyPositioned { coords ->
                                    slotBounds[itemIndex] = coords.boundsInRoot()
                                }
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationX = dragTranslation.x
                                        translationY = dragTranslation.y
                                        scaleX = 1.15f
                                        scaleY = 1.15f
                                        alpha = 0.92f
                                    }
                                }
                                .pointerInput(isJiggleMode, item?.id) {
                                    if (!isJiggleMode || item == null) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedSlotIndex = itemIndex
                                            dragTranslation = Offset.Zero
                                            hoveredTargetSlot = null
                                            isMergeHover = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragTranslation += dragAmount
                                            val startRect = slotBounds[itemIndex]
                                            if (startRect != null) {
                                                val currentCenter = startRect.center + dragTranslation
                                                var foundSlot: Int? = null
                                                var merge = false
                                                for ((idx, rect) in slotBounds) {
                                                    if (idx != itemIndex && rect.contains(currentCenter)) {
                                                        foundSlot = idx
                                                        val dist = hypot(
                                                            currentCenter.x - rect.center.x,
                                                            currentCenter.y - rect.center.y
                                                        )
                                                        val mergeRadius = rect.width * 0.28f
                                                        merge = dist <= mergeRadius
                                                        break
                                                    }
                                                }
                                                hoveredTargetSlot = foundSlot
                                                isMergeHover = merge
                                            }
                                        },
                                        onDragEnd = {
                                            val targetIdx = hoveredTargetSlot
                                            val targetItem = if (targetIdx != null) pageItems.getOrNull(targetIdx) else null
                                            if (targetItem != null && targetItem.id != item.id) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDragDropItem(item.id, targetItem.id, isMergeHover)
                                            }
                                            draggedSlotIndex = null
                                            dragTranslation = Offset.Zero
                                            hoveredTargetSlot = null
                                            isMergeHover = false
                                        },
                                        onDragCancel = {
                                            draggedSlotIndex = null
                                            dragTranslation = Offset.Zero
                                            hoveredTargetSlot = null
                                            isMergeHover = false
                                        }
                                    )
                                }
                        ) {
                            when (item) {
                                is HomeGridItem.AppItem -> {
                                    val app = appMap[item.packageName]
                                    if (app != null) {
                                        IosAppIconView(
                                            app = app,
                                            iconSize = iconSize,
                                            labelFontSizeSp = labelFontSizeSp,
                                            showLabel = showLabels,
                                            showBadges = showBadges,
                                            isJiggleMode = isJiggleMode,
                                            isHighlightedTarget = isTargetHighlighted && isMergeHover,
                                            onClick = { onAppClick(app) },
                                            onLongClick = { onAppLongClick(app) },
                                            onDeleteOrMenuClick = { onAppLongClick(app) }
                                        )
                                    }
                                }
                                is HomeGridItem.FolderItem -> {
                                    IosFolderGridIconView(
                                        folder = item,
                                        appMap = appMap,
                                        iconSize = iconSize,
                                        labelFontSizeSp = labelFontSizeSp,
                                        glassOpacity = glassOpacity,
                                        showLabel = showLabels,
                                        showBadges = showBadges,
                                        isJiggleMode = isJiggleMode,
                                        isHighlightedTarget = isTargetHighlighted,
                                        onClickWithOrigin = { origin -> onFolderClickWithOrigin(item, origin) },
                                        onLongClickWithOrigin = { origin -> onFolderLongClickWithOrigin(item, origin) }
                                    )
                                }
                                null -> {
                                    Spacer(modifier = Modifier.size(iconSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
