package com.tms.os.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.FoldPostureMode
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.PageTransitionEffect
import com.tms.os.launcher.ui.components.IosAppIconView
import com.tms.os.launcher.ui.components.IosAppLibraryScreen
import com.tms.os.launcher.ui.components.IosCategoryExpandedModal
import com.tms.os.launcher.ui.components.IosExpandedFolderModal
import com.tms.os.launcher.ui.components.IosFolderGridIconView
import com.tms.os.launcher.ui.components.IosFrostedDock
import com.tms.os.launcher.ui.components.IosHapticTouchContextMenu
import com.tms.os.launcher.ui.components.IosTopWidgetsHeader
import com.tms.os.launcher.ui.components.SpotlightSearchOverlay
import com.tms.os.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettingsActivity: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Subtle translucent vignette over the live system wallpaper for crisp icon legibility
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.22f),
                        Color.Black.copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.32f)
                    )
                )
            )
            .pointerInput(state.isSpotlightOpen, state.activeFolder) {
                var accumulatedDy = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDy = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDy += dragAmount
                        if (accumulatedDy > 72f && !state.isSpotlightOpen && state.activeFolder == null) {
                            accumulatedDy = 0f
                            viewModel.openSpotlight()
                        }
                    }
                )
            }
    ) {
        // Detect if Samsung Galaxy Fold is unfolded (Main inner screen >= 580.dp width)
        val isUnfolded = maxWidth >= 580.dp
        val settings = state.settings

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
        val page0Capacity = if (settings.showTopWidgetsOnFirstPage) {
            max(columns * max(rows - 2, 2), 8)
        } else {
            itemsPerPage
        }

        // Partition home grid items into pages
        val pages: List<List<HomeGridItem>> = remember(
            state.homeGridItems,
            page0Capacity,
            itemsPerPage
        ) {
            if (state.homeGridItems.isEmpty()) {
                listOf(emptyList())
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

        val isBookSplitMode = isUnfolded && settings.foldMode == FoldPostureMode.BOOK_SPLIT
        val homeSpreadsCount = if (isBookSplitMode) {
            max((pages.size + 1) / 2, 1)
        } else {
            pages.size
        }

        // Last page in Pager is always the Smart App Library (just like iOS!)
        val totalPagerPages = homeSpreadsCount + 1
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { totalPagerPages }
        )

        val isOnAppLibraryPage = pagerState.currentPage == totalPagerPages - 1
        val baseIconSize = (if (isUnfolded) 62.dp else 58.dp) * settings.iconSizeScale

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Jiggle Mode Edit Bar (when long-pressing home screen / editing icons)
            AnimatedVisibility(visible = state.isJiggleMode) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // One-click Smart Folder Auto-Sort button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xDD2C2C2E))
                                .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                .clickable { viewModel.autoOrganizeHomeWithSmartFolders() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD60A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Smart-Ordner",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Settings Shortcut
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xDD2C2C2E))
                                .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                .clickable { onOpenSettingsActivity() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "TMS OS",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Done ("Fertig") pill button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0A84FF))
                            .clickable { viewModel.setJiggleMode(false) }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fertig",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Main Horizontal Pager (Home Screen Pages + Smart App Library on far right)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val pageOffset = (
                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    ).absoluteValue

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            when (settings.pageTransition) {
                                PageTransitionEffect.IOS_SLIDE -> {
                                    alpha = 1f
                                }
                                PageTransitionEffect.CUBE_3D -> {
                                    rotationY = (pageIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction) * 28f
                                    cameraDistance = 12f * density
                                }
                                PageTransitionEffect.DEPTH_STACK -> {
                                    val scale = 1f - (pageOffset * 0.12f).coerceIn(0f, 0.2f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (pageOffset * 0.35f).coerceIn(0f, 0.5f)
                                }
                                PageTransitionEffect.ZOOM_FADE -> {
                                    val scale = 0.88f + (1f - pageOffset.coerceIn(0f, 1f)) * 0.12f
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (pageOffset * 0.4f).coerceIn(0f, 0.6f)
                                }
                            }
                        }
                ) {
                    if (pageIndex == totalPagerPages - 1) {
                        // Far Right Page: iOS Smart App Library
                        IosAppLibraryScreen(
                            categorizedApps = state.categorizedApps,
                            allApps = state.allApps,
                            isUnfolded = isUnfolded,
                            onAppClick = { viewModel.launchApp(it.packageName) },
                            onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                            onCategoryExpandClick = { viewModel.openCategoryModal(it) },
                            onOpenSettingsClick = onOpenSettingsActivity
                        )
                    } else if (isBookSplitMode) {
                        // Galaxy Fold Unfolded Book Split Mode: Two iPhone pages side by side!
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
                                HomeGridPageContent(
                                    pageItems = pages.getOrNull(leftIdx).orEmpty(),
                                    isFirstPage = (leftIdx == 0 && settings.showTopWidgetsOnFirstPage),
                                    columns = columns,
                                    iconSize = baseIconSize,
                                    showLabels = settings.showAppLabels,
                                    showBadges = settings.enableBadges,
                                    isJiggleMode = state.isJiggleMode,
                                    weatherCity = settings.weatherCity,
                                    weatherTemp = settings.weatherTempCelsius,
                                    weatherCondition = settings.weatherCondition,
                                    appMap = state.appMap,
                                    onAppClick = { app ->
                                        if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                                        else viewModel.launchApp(app.packageName)
                                    },
                                    onAppLongClick = { app ->
                                        viewModel.selectAppForContextMenu(app)
                                    },
                                    onFolderClick = { folder ->
                                        viewModel.openFolder(folder)
                                    },
                                    onFolderLongClick = { folder ->
                                        viewModel.openFolder(folder)
                                    },
                                    onWeatherClick = onOpenSettingsActivity,
                                    onSearchClick = { viewModel.openSpotlight() }
                                )
                            }

                            // Subtle center fold spine divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight(0.88f)
                                    .align(Alignment.CenterVertically)
                                    .background(Color.White.copy(alpha = 0.10f))
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                HomeGridPageContent(
                                    pageItems = pages.getOrNull(rightIdx).orEmpty(),
                                    isFirstPage = false,
                                    columns = columns,
                                    iconSize = baseIconSize,
                                    showLabels = settings.showAppLabels,
                                    showBadges = settings.enableBadges,
                                    isJiggleMode = state.isJiggleMode,
                                    weatherCity = settings.weatherCity,
                                    weatherTemp = settings.weatherTempCelsius,
                                    weatherCondition = settings.weatherCondition,
                                    appMap = state.appMap,
                                    onAppClick = { app ->
                                        if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                                        else viewModel.launchApp(app.packageName)
                                    },
                                    onAppLongClick = { app ->
                                        viewModel.selectAppForContextMenu(app)
                                    },
                                    onFolderClick = { folder ->
                                        viewModel.openFolder(folder)
                                    },
                                    onFolderLongClick = { folder ->
                                        viewModel.openFolder(folder)
                                    },
                                    onWeatherClick = onOpenSettingsActivity,
                                    onSearchClick = { viewModel.openSpotlight() }
                                )
                            }
                        }
                    } else {
                        // Single Page Grid (Cover Screen or iPadOS Unfolded Grid)
                        HomeGridPageContent(
                            pageItems = pages.getOrNull(pageIndex).orEmpty(),
                            isFirstPage = (pageIndex == 0 && settings.showTopWidgetsOnFirstPage),
                            columns = columns,
                            iconSize = baseIconSize,
                            showLabels = settings.showAppLabels,
                            showBadges = settings.enableBadges,
                            isJiggleMode = state.isJiggleMode,
                            weatherCity = settings.weatherCity,
                            weatherTemp = settings.weatherTempCelsius,
                            weatherCondition = settings.weatherCondition,
                            appMap = state.appMap,
                            onAppClick = { app ->
                                if (state.isJiggleMode) viewModel.selectAppForContextMenu(app)
                                else viewModel.launchApp(app.packageName)
                            },
                            onAppLongClick = { app ->
                                viewModel.selectAppForContextMenu(app)
                            },
                            onFolderClick = { folder ->
                                viewModel.openFolder(folder)
                            },
                            onFolderLongClick = { folder ->
                                viewModel.openFolder(folder)
                            },
                            onWeatherClick = onOpenSettingsActivity,
                            onSearchClick = { viewModel.openSpotlight() }
                        )
                    }
                }
            }

            // Bottom Section: iOS Search Pill / Page Dots + Frosted Glass Dock
            if (!isOnAppLibraryPage) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // iOS 16/17/18 "Suchen" Pill + Page Indicator Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA252529))
                            .border(0.6.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { viewModel.openSpotlight() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Suchen",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Suchen",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        for (i in 0 until totalPagerPages) {
                            val selected = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.5.dp)
                                    .size(if (selected) 6.5.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) Color.White
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(i) }
                                    }
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
                        iconSize = baseIconSize * 0.96f,
                        isJiggleMode = state.isJiggleMode,
                        onAppClick = { viewModel.launchApp(it.packageName) },
                        onAppLongClick = { viewModel.selectAppForContextMenu(it) }
                    )
                }
            }
        }

        // Expanded Folder Overlay
        state.activeFolder?.let { folder ->
            IosExpandedFolderModal(
                folder = folder,
                appMap = state.appMap,
                onAppClick = { viewModel.launchApp(it.packageName) },
                onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                onRenameFolder = { newName -> viewModel.renameFolder(folder.folderId, newName) },
                onUnpackFolder = { viewModel.removeFolderAndUnpackApps(folder.folderId) },
                onDismiss = { viewModel.openFolder(null) }
            )
        }

        // Expanded Smart Category Overlay (from App Library)
        state.activeCategoryModal?.let { category ->
            val catApps = state.categorizedApps[category].orEmpty()
            IosCategoryExpandedModal(
                category = category,
                apps = catApps,
                onAppClick = { viewModel.launchApp(it.packageName) },
                onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                onDismiss = { viewModel.openCategoryModal(null) }
            )
        }

        // Spotlight Search Overlay (Swipe Down or "Suchen" Pill)
        AnimatedVisibility(
            visible = state.isSpotlightOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
        ) {
            SpotlightSearchOverlay(
                query = state.spotlightQuery,
                results = state.spotlightResults,
                mathResult = state.spotlightMathResult,
                onQueryChange = { viewModel.updateSpotlightQuery(it) },
                onAppClick = { viewModel.launchApp(it.packageName) },
                onAppLongClick = { viewModel.selectAppForContextMenu(it) },
                onClose = { viewModel.closeSpotlight() }
            )
        }

        // Haptic Touch App Context Menu Overlay
        state.selectedAppForMenu?.let { app ->
            val existingFolders = remember(state.homeGridItems) {
                state.homeGridItems.filterIsInstance<HomeGridItem.FolderItem>()
            }
            IosHapticTouchContextMenu(
                app = app,
                isPinnedInDock = state.dockPackages.contains(app.packageName),
                existingFolders = existingFolders,
                onLaunchApp = { viewModel.launchApp(app.packageName) },
                onToggleDockPin = {
                    viewModel.togglePinToDock(app.packageName)
                    viewModel.selectAppForContextMenu(null)
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

@Composable
private fun HomeGridPageContent(
    pageItems: List<HomeGridItem>,
    isFirstPage: Boolean,
    columns: Int,
    iconSize: Dp,
    showLabels: Boolean,
    showBadges: Boolean,
    isJiggleMode: Boolean,
    weatherCity: String,
    weatherTemp: Int,
    weatherCondition: String,
    appMap: Map<String, AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderClick: (HomeGridItem.FolderItem) -> Unit,
    onFolderLongClick: (HomeGridItem.FolderItem) -> Unit,
    onWeatherClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        if (isFirstPage) {
            IosTopWidgetsHeader(
                weatherCity = weatherCity,
                weatherTempCelsius = weatherTemp,
                weatherCondition = weatherCondition,
                onWeatherClick = onWeatherClick,
                onSearchPillClick = onSearchClick
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        val rows = remember(pageItems, columns) {
            pageItems.chunked(columns)
        }

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
        ) {
            for (rowItems in rows) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (colIdx in 0 until columns) {
                        val item = rowItems.getOrNull(colIdx)
                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier.weight(1f)
                        ) {
                            when (item) {
                                is HomeGridItem.AppItem -> {
                                    val app = appMap[item.packageName]
                                    if (app != null) {
                                        IosAppIconView(
                                            app = app,
                                            iconSize = iconSize,
                                            showLabel = showLabels,
                                            showBadges = showBadges,
                                            isJiggleMode = isJiggleMode,
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
                                        showLabel = showLabels,
                                        showBadges = showBadges,
                                        isJiggleMode = isJiggleMode,
                                        onClick = { onFolderClick(item) },
                                        onLongClick = { onFolderLongClick(item) }
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
