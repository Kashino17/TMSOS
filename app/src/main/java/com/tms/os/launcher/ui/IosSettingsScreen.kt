package com.tms.os.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.BuildConfig
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.FoldPostureMode
import com.tms.os.launcher.model.IconThemeMode
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.model.LeftWidgetType
import com.tms.os.launcher.model.PageTransitionEffect
import com.tms.os.launcher.model.RightWidgetType
import com.tms.os.launcher.model.TintAccentColor
import com.tms.os.launcher.ui.components.IosAppIconView
import com.tms.os.launcher.ui.components.IosStatusBar
import com.tms.os.launcher.ui.theme.IosStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.iosStrings
import com.tms.os.launcher.viewmodel.LauncherViewModel
import kotlin.math.roundToInt

/* ========================================================================== */
/*                              iOS 18 Settings                               */
/* ========================================================================== */

/**
 * The settings app, rebuilt the way iOS builds it: grouped inset lists, a
 * collapsing large title, 17 pt rows, real iOS switches, real iOS sliders,
 * real iOS segmented controls and the system icon tiles.
 */
@Composable
fun IosSettingsScreen(
    viewModel: LauncherViewModel,
    onBackToHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val strings = iosStrings(state.settings.appLanguage)
    var page by remember { mutableStateOf(SettingsPage.Root) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The app draws its own iOS status bar, exactly like the launcher.
            IosStatusBar(lightContent = true)
            when (page) {
                SettingsPage.Root -> IosSettingsRoot(
                    viewModel = viewModel,
                    strings = strings,
                    onOpen = { page = it },
                    onBackToHome = onBackToHome
                )

                else -> IosSettingsSubPage(
                    page = page,
                    viewModel = viewModel,
                    strings = strings,
                    onBack = { page = SettingsPage.Root }
                )
            }
        }
    }
}

private enum class SettingsPage {
    Root,
    General,
    Appearance,
    Weather,
    Transition,
    Grid,
    Badges,
    Apps,
    Language
}

/* -------------------------------------------------------------------------- */
/*                                   Root                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun IosSettingsRoot(
    viewModel: LauncherViewModel,
    strings: IosStrings,
    onOpen: (SettingsPage) -> Unit,
    onBackToHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IosSettingsNavBar(
            title = strings.settings,
            compact = collapsed,
            backLabel = strings.homeScreen,
            onBack = onBackToHome
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            item(key = "title") {
                Text(
                    text = strings.settings,
                    color = Color.White,
                    style = IosText.largeTitle,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp)
                )
            }

            item(key = "group.general") {
                IosGroup {
                    IosNavRow(
                        icon = Icons.Default.Settings,
                        iconBg = Color(0xFF8E8E93),
                        title = strings.general,
                        value = strings.spotlight,
                        onClick = { onOpen(SettingsPage.General) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.Palette,
                        iconBg = Color(0xFFBF5AF2),
                        title = strings.appearance,
                        value = strings.iconStyle,
                        onClick = { onOpen(SettingsPage.Appearance) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.Cloud,
                        iconBg = Color(0xFF32ADE6),
                        title = strings.weather,
                        value = settings.weatherCity,
                        onClick = { onOpen(SettingsPage.Weather) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        iconBg = Color(0xFF5E5CE6),
                        title = strings.pageTransition,
                        onClick = { onOpen(SettingsPage.Transition) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.GridView,
                        iconBg = Color(0xFF5856D6),
                        title = strings.layout,
                        value = "${settings.coverColumns}×${settings.coverRows} · ${settings.mainUnfoldedColumns}×${settings.mainUnfoldedRows}",
                        onClick = { onOpen(SettingsPage.Grid) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.NotificationsNone,
                        iconBg = Color(0xFFFF453A),
                        title = strings.badges,
                        value = if (state.isBadgeServiceEnabled) strings.on else strings.off,
                        onClick = { onOpen(SettingsPage.Badges) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.Visibility,
                        iconBg = Color(0xFFFF9F0A),
                        title = strings.appLibraryCategories,
                        value = "${state.allApps.count { it.isHidden }}/${state.allApps.size}",
                        onClick = { onOpen(SettingsPage.Apps) }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.Language,
                        iconBg = Color(0xFF64D2FF),
                        title = strings.language,
                        value = settings.appLanguage.displayName,
                        onClick = { onOpen(SettingsPage.Language) }
                    )
                }
            }

            item(key = "group.system") {
                IosGroup {
                    IosNavRow(
                        icon = Icons.Default.Home,
                        iconBg = Color(0xFF30D158),
                        title = strings.defaultLauncher,
                        value = if (state.isDefaultLauncher) strings.on else strings.off,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_HOME_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                    IosDivider()
                    IosNavRow(
                        icon = Icons.Default.Wallpaper,
                        iconBg = Color(0xFF30D158),
                        title = strings.wallpaper,
                        onClick = { viewModel.openWallpaperPicker() }
                    )
                }
            }

            item(key = "group.actions") {
                IosGroup {
                    IosActionRow(
                        icon = Icons.Default.AutoAwesome,
                        iconBg = Color(0xFFFF9F0A),
                        title = strings.smartOrganize,
                        detail = strings.smartOrganizeSub,
                        onClick = { viewModel.autoOrganizeHomeWithSmartFolders() }
                    )
                    IosDivider()
                    IosActionRow(
                        icon = Icons.Default.DevicesFold,
                        iconBg = Color(0xFF0A84FF),
                        title = strings.foldDisplay,
                        detail = "${strings.coverScreen} ${settings.coverColumns}×${settings.coverRows} · ${strings.innerScreen} ${settings.mainUnfoldedColumns}×${settings.mainUnfoldedRows}",
                        onClick = { onOpen(SettingsPage.Grid) }
                    )
                }
            }

            item(key = "footer") {
                Text(
                    text = "TMS OS ${BuildConfig.VERSION_NAME} · ${strings.madeForFold}",
                    color = Color(0xFF8E8E93),
                    style = IosText.footnote,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 22.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                 Sub pages                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun IosSettingsSubPage(
    page: SettingsPage,
    viewModel: LauncherViewModel,
    strings: IosStrings,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 }
    }
    val title = when (page) {
        SettingsPage.General -> strings.general
        SettingsPage.Appearance -> strings.appearance
        SettingsPage.Weather -> strings.weather
        SettingsPage.Transition -> strings.pageTransition
        SettingsPage.Grid -> strings.layout
        SettingsPage.Badges -> strings.badges
        SettingsPage.Apps -> strings.appLibraryCategories
        SettingsPage.Language -> strings.language
        SettingsPage.Root -> strings.settings
    }

    // Hoisted state: the list builders are plain functions, so remember() has to
    // live in the composable scope.
    var cityInput by remember(settings.weatherCity) { mutableStateOf(settings.weatherCity) }
    var filterQuery by remember { mutableStateOf("") }
    var onlyHidden by remember { mutableStateOf(false) }
    var editingCategoryForPkg by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        IosSettingsNavBar(
            title = title,
            compact = collapsed,
            backLabel = strings.settings,
            onBack = onBack
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 4.dp, bottom = 44.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            item(key = "title") {
                Text(
                    text = title,
                    color = Color.White,
                    style = IosText.largeTitle,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp)
                )
            }

            when (page) {
                SettingsPage.General -> generalItems(
                    viewModel, strings, settings, state, context, backupStatus,
                    onBackupStatus = { backupStatus = it }
                )
                SettingsPage.Appearance -> appearanceItems(viewModel, strings, settings, state)
                SettingsPage.Weather -> weatherItems(
                    viewModel, strings, settings, state,
                    cityInput = cityInput,
                    onCityChange = { cityInput = it }
                )
                SettingsPage.Transition -> transitionItems(viewModel, settings)
                SettingsPage.Grid -> gridItems(viewModel, settings)
                SettingsPage.Badges -> badgeItems(viewModel, strings, settings, state, context)
                SettingsPage.Apps -> appItems(
                    viewModel, strings, state,
                    filterQuery = filterQuery,
                    onFilterChange = { filterQuery = it },
                    onlyHidden = onlyHidden,
                    onOnlyHiddenChange = { onlyHidden = it },
                    editingCategoryForPkg = editingCategoryForPkg,
                    onEditingCategoryChange = { editingCategoryForPkg = it }
                )
                SettingsPage.Language -> languageItems(viewModel, settings)
                SettingsPage.Root -> Unit
            }
        }
    }
}

private fun LazyListScope.generalItems(
    viewModel: LauncherViewModel,
    strings: IosStrings,
    settings: LauncherSettings,
    state: com.tms.os.launcher.viewmodel.LauncherUiState,
    context: Context,
    backupStatus: String?,
    onBackupStatus: (String?) -> Unit
) {
    item(key = "general.spotlight") {
        IosGroup {
            IosSwitchRow(
                title = strings.searchBarAtBottom,
                checked = settings.searchBarAtBottomInSpotlight,
                onCheckedChange = { v ->
                    viewModel.updateSettings { it.copy(searchBarAtBottomInSpotlight = v) }
                }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.swipeDownSpotlight,
                checked = settings.enableSwipeDownSpotlight,
                onCheckedChange = { v ->
                    viewModel.updateSettings { it.copy(enableSwipeDownSpotlight = v) }
                }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.autoKeyboard,
                checked = settings.autoOpenKeyboardInSpotlight,
                onCheckedChange = { v ->
                    viewModel.updateSettings { it.copy(autoOpenKeyboardInSpotlight = v) }
                }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.instantOpen,
                checked = settings.instantLaunchSingleSearchResult,
                onCheckedChange = { v ->
                    viewModel.updateSettings { it.copy(instantLaunchSingleSearchResult = v) }
                }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.showWebSearch,
                checked = settings.showWebSearchInSpotlight,
                onCheckedChange = { v ->
                    viewModel.updateSettings { it.copy(showWebSearchInSpotlight = v) }
                }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.autoCategory,
                checked = settings.autoSortNewAppsIntoCategories,
                onCheckedChange = { v ->
                    viewModel.updateSettings { it.copy(autoSortNewAppsIntoCategories = v) }
                }
            )
        }
    }

    item(key = "general.permissions") {
        IosHeader(strings.permissions)
        IosGroup {
            IosNavRow(
                icon = Icons.Default.NotificationsNone,
                iconBg = Color(0xFFFF453A),
                title = strings.notificationAccess,
                value = if (state.isBadgeServiceEnabled) strings.on else strings.off,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
            IosDivider()
            IosNavRow(
                icon = Icons.Default.Visibility,
                iconBg = Color(0xFF0A84FF),
                title = strings.usageAccess,
                value = if (state.isUsageStatsGranted) strings.on else strings.off,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }
        IosFooter(strings.permissionsFooter)
    }

    item(key = "general.organize") {
        IosHeader(strings.organize)
        IosGroup {
            IosActionRow(
                icon = Icons.Default.AutoAwesome,
                iconBg = Color(0xFFFF9F0A),
                title = strings.smartFolders,
                detail = strings.smartFoldersSub,
                onClick = { viewModel.autoOrganizeHomeWithSmartFolders() }
            )
            IosDivider()
            IosActionRow(
                icon = Icons.Default.GridView,
                iconBg = Color(0xFF5856D6),
                title = strings.flatSort,
                detail = strings.flatSortSub,
                onClick = { viewModel.autoOrganizeHomeFlatSorted() }
            )
        }
    }

    item(key = "general.backup") {
        IosHeader(strings.backup)
        IosGroup {
            IosActionRow(
                icon = Icons.Default.Refresh,
                iconBg = Color(0xFF30D158),
                title = strings.backupExport,
                detail = backupStatus ?: strings.backupSub,
                onClick = {
                    val json = viewModel.exportBackupJson()
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("TMS_OS_Backup", json))
                    onBackupStatus(strings.backupDone)
                }
            )
            IosDivider()
            IosActionRow(
                icon = Icons.Default.Download,
                iconBg = Color(0xFF0A84FF),
                title = strings.backupImport,
                detail = backupStatus ?: strings.backupImportSub,
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val text = cm?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                    onBackupStatus(
                        if (viewModel.importBackupJson(text)) strings.backupDone else strings.backupFailed
                    )
                }
            )
        }
    }
}

private fun LazyListScope.appearanceItems(
    viewModel: LauncherViewModel,
    strings: IosStrings,
    settings: LauncherSettings,
    state: com.tms.os.launcher.viewmodel.LauncherUiState
) {
    item(key = "appearance.preview") {
        IosPreviewCard(settings.iconThemeMode.titleDe, state.allApps.take(4), settings)
    }

    item(key = "appearance.style") {
        IosHeader(strings.iconStyle)
        IosGroup {
            IosSegmentedRow(
                labels = IconThemeMode.entries.map { it.titleDe },
                selectedIndex = IconThemeMode.entries.indexOf(settings.iconThemeMode),
                onSelect = { index ->
                    viewModel.updateSettings { it.copy(iconThemeMode = IconThemeMode.entries[index]) }
                }
            )
        }
    }

    if (settings.iconThemeMode == IconThemeMode.IOS_TINTED) {
        item(key = "appearance.accent") {
            IosHeader(strings.accentColor)
            IosGroup {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TintAccentColor.entries.toList()) { accent ->
                        IosAccentChip(
                            label = accent.titleDe,
                            color = Color(accent.colorHex),
                            selected = settings.tintAccentColor == accent,
                            onClick = {
                                viewModel.updateSettings { it.copy(tintAccentColor = accent) }
                            }
                        )
                    }
                }
            }
        }
    }

    item(key = "appearance.icons") {
        IosHeader(strings.appIcons)
        IosGroup {
            IosSwitchRow(
                title = strings.systemIcons,
                checked = settings.useIosSystemIcons,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(useIosSystemIcons = v) } }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.fullBleedIcons,
                checked = settings.fullBleedAdaptiveIcons,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(fullBleedAdaptiveIcons = v) } }
            )
        }
    }

    item(key = "appearance.size") {
        IosHeader(strings.sizeAndText)
        IosGroup {
            IosSliderRow(
                title = strings.iconSize,
                value = settings.iconSizeScale,
                valueRange = 0.80f..1.20f,
                valueLabel = "${(settings.iconSizeScale * 100).roundToInt()} %",
                onValueChange = { v -> viewModel.updateSettings { it.copy(iconSizeScale = v) } }
            )
            IosDivider()
            IosSliderRow(
                title = strings.labelSize,
                value = settings.labelFontSizeSp,
                valueRange = 9.5f..14.0f,
                valueLabel = "${settings.labelFontSizeSp.roundToInt()} pt",
                onValueChange = { v -> viewModel.updateSettings { it.copy(labelFontSizeSp = v) } }
            )
            IosDivider()
            IosSliderRow(
                title = strings.glassOpacity,
                value = settings.glassOpacity,
                valueRange = 0.45f..0.95f,
                valueLabel = "${(settings.glassOpacity * 100).roundToInt()} %",
                onValueChange = { v -> viewModel.updateSettings { it.copy(glassOpacity = v) } }
            )
        }
    }

    item(key = "appearance.labels") {
        IosHeader(strings.appNames)
        IosGroup {
            IosSwitchRow(
                title = strings.showAppLabels,
                checked = settings.showAppLabels,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(showAppLabels = v) } }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.showDockLabels,
                checked = settings.showDockLabels,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(showDockLabels = v) } }
            )
        }
    }
}

private fun LazyListScope.weatherItems(
    viewModel: LauncherViewModel,
    strings: IosStrings,
    settings: LauncherSettings,
    state: com.tms.os.launcher.viewmodel.LauncherUiState,
    cityInput: String,
    onCityChange: (String) -> Unit
) {
    item(key = "weather.city") {
        IosHeader(strings.weather)
        IosGroup {
            IosTextFieldRow(
                title = strings.city,
                value = cityInput,
                placeholder = settings.weatherCity,
                onValueChange = onCityChange
            )
            IosDivider()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Berlin", "Dubai", "München", "Frankfurt", "Hamburg", "Wien", "Zürich", "Istanbul")) { city ->
                    IosChip(
                        label = city,
                        selected = cityInput.equals(city, ignoreCase = true),
                        onClick = {
                            onCityChange(city)
                            viewModel.refreshLiveWeatherForCity(city)
                        }
                    )
                }
            }
            IosDivider()
            IosActionRow(
                icon = Icons.Default.Refresh,
                iconBg = Color(0xFF32ADE6),
                title = if (state.isWeatherRefreshing) strings.loading else strings.refresh,
                detail = "${settings.weatherCity} · ${settings.weatherTempCelsius}°C · ${settings.weatherCondition}",
                onClick = { viewModel.refreshLiveWeatherForCity(cityInput) }
            )
        }
    }

    item(key = "weather.widgets") {
        IosHeader(strings.widgets)
        IosGroup {
            IosSwitchRow(
                title = strings.showWidgets,
                checked = settings.showTopWidgetsOnFirstPage,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(showTopWidgetsOnFirstPage = v) } }
            )
            IosDivider()
            IosSwitchRow(
                title = strings.fahrenheit,
                checked = settings.useFahrenheit,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(useFahrenheit = v) } }
            )
        }
    }

    item(key = "weather.left") {
        IosHeader(strings.leftWidget)
        IosGroup {
            var first = true
            for (type in LeftWidgetType.entries) {
                if (!first) IosDivider()
                first = false
                IosCheckRow(
                    title = type.titleDe,
                    selected = settings.leftWidgetType == type,
                    onClick = { viewModel.updateSettings { it.copy(leftWidgetType = type) } }
                )
            }
        }
    }

    item(key = "weather.right") {
        IosHeader(strings.rightWidget)
        IosGroup {
            var first = true
            for (type in RightWidgetType.entries) {
                if (!first) IosDivider()
                first = false
                IosCheckRow(
                    title = type.titleDe,
                    selected = settings.rightWidgetType == type,
                    onClick = { viewModel.updateSettings { it.copy(rightWidgetType = type) } }
                )
            }
        }
    }
}

private fun LazyListScope.transitionItems(
    viewModel: LauncherViewModel,
    settings: LauncherSettings
) {
    item(key = "transition.list") {
        IosGroup {
            var first = true
            for (effect in PageTransitionEffect.entries) {
                if (!first) IosDivider()
                first = false
                IosCheckRow(
                    title = effect.titleDe,
                    detail = effect.descDe,
                    selected = settings.pageTransition == effect,
                    onClick = { viewModel.updateSettings { it.copy(pageTransition = effect) } }
                )
            }
        }
    }
}

private fun LazyListScope.gridItems(
    viewModel: LauncherViewModel,
    settings: LauncherSettings
) {
    item(key = "grid.fold") {
        IosHeader(stringsFoldDisplay)
        IosGroup {
            var first = true
            for (mode in FoldPostureMode.entries) {
                if (!first) IosDivider()
                first = false
                IosCheckRow(
                    title = mode.titleDe,
                    detail = mode.descDe,
                    selected = settings.foldMode == mode,
                    onClick = { viewModel.updateSettings { it.copy(foldMode = mode) } }
                )
            }
        }
    }

    item(key = "grid.cover") {
        IosHeader(stringsCoverScreen)
        IosGroup {
            IosSegmentedRow(
                label = stringsColumns,
                labels = listOf("3", "4", "5"),
                selectedIndex = listOf(3, 4, 5).indexOf(settings.coverColumns).coerceAtLeast(0),
                onSelect = { index -> viewModel.updateSettings { it.copy(coverColumns = listOf(3, 4, 5)[index]) } }
            )
            IosDivider()
            IosSegmentedRow(
                label = stringsRows,
                labels = listOf("4", "5", "6", "7"),
                selectedIndex = listOf(4, 5, 6, 7).indexOf(settings.coverRows).coerceAtLeast(0),
                onSelect = { index -> viewModel.updateSettings { it.copy(coverRows = listOf(4, 5, 6, 7)[index]) } }
            )
        }
    }

    item(key = "grid.inner") {
        IosHeader(stringsInnerScreen)
        IosGroup {
            IosSegmentedRow(
                label = stringsColumns,
                labels = listOf("5", "6", "7", "8"),
                selectedIndex = listOf(5, 6, 7, 8).indexOf(settings.mainUnfoldedColumns).coerceAtLeast(0),
                onSelect = { index ->
                    viewModel.updateSettings { it.copy(mainUnfoldedColumns = listOf(5, 6, 7, 8)[index]) }
                }
            )
            IosDivider()
            IosSegmentedRow(
                label = stringsRows,
                labels = listOf("4", "5", "6"),
                selectedIndex = listOf(4, 5, 6).indexOf(settings.mainUnfoldedRows).coerceAtLeast(0),
                onSelect = { index -> viewModel.updateSettings { it.copy(mainUnfoldedRows = listOf(4, 5, 6)[index]) } }
            )
        }
    }

    item(key = "grid.dock") {
        IosHeader(stringsDockApps)
        IosGroup {
            IosSegmentedRow(
                labels = listOf("4", "5", "6"),
                selectedIndex = listOf(4, 5, 6).indexOf(settings.dockIconCount).coerceAtLeast(0),
                onSelect = { index -> viewModel.updateSettings { it.copy(dockIconCount = listOf(4, 5, 6)[index]) } }
            )
            IosDivider()
            IosSwitchRow(
                title = stringsDockRecent,
                checked = settings.showRecentInUnfoldedDock,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(showRecentInUnfoldedDock = v) } }
            )
        }
    }
}

private fun LazyListScope.badgeItems(
    viewModel: LauncherViewModel,
    strings: IosStrings,
    settings: LauncherSettings,
    state: com.tms.os.launcher.viewmodel.LauncherUiState,
    context: Context
) {
    item(key = "badges.switch") {
        IosGroup {
            IosSwitchRow(
                title = strings.showBadges,
                checked = settings.enableBadges,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(enableBadges = v) } }
            )
        }
    }

    item(key = "badges.access") {
        IosHeader(strings.permissions)
        IosGroup {
            IosNavRow(
                icon = Icons.Default.NotificationsNone,
                iconBg = Color(0xFFFF453A),
                title = strings.notificationAccess,
                value = if (state.isBadgeServiceEnabled) strings.on else strings.off,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }
        IosFooter(strings.notificationFooter)
    }
}

private fun LazyListScope.appItems(
    viewModel: LauncherViewModel,
    strings: IosStrings,
    state: com.tms.os.launcher.viewmodel.LauncherUiState,
    filterQuery: String,
    onFilterChange: (String) -> Unit,
    onlyHidden: Boolean,
    onOnlyHiddenChange: (Boolean) -> Unit,
    editingCategoryForPkg: String?,
    onEditingCategoryChange: (String?) -> Unit
) {
    val filteredApps = state.allApps.filter { app ->
        (!onlyHidden || app.isHidden) &&
            (filterQuery.isBlank() ||
                app.displayLabel.contains(filterQuery, ignoreCase = true) ||
                app.category.titleDe.contains(filterQuery, ignoreCase = true))
    }

    item(key = "apps.search") {
        IosGroup {
            IosTextFieldRow(
                title = strings.search,
                value = filterQuery,
                placeholder = strings.search,
                leadingIcon = Icons.Default.Search,
                onValueChange = onFilterChange
            )
            IosDivider()
            IosSegmentedRow(
                labels = listOf(strings.all, strings.hiddenApps),
                selectedIndex = if (onlyHidden) 1 else 0,
                onSelect = { onOnlyHiddenChange(it == 1) }
            )
        }
    }

    items(filteredApps, key = { "app.${it.packageName}" }) { app ->
        IosGroup {
            IosAppManageRow(
                label = app.displayLabel,
                detail = app.category.titleDe,
                icon = app.iconBitmap,
                hidden = app.isHidden,
                hideLabel = strings.hide,
                unhideLabel = strings.unhide,
                onToggleHidden = { viewModel.hideOrUnhideApp(app.packageName, !app.isHidden) },
                onEditCategory = {
                    onEditingCategoryChange(
                        if (editingCategoryForPkg == app.packageName) null else app.packageName
                    )
                }
            )
            if (editingCategoryForPkg == app.packageName) {
                IosDivider()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        AppCategory.entries.filter { it != AppCategory.SUGGESTIONS }.toList()
                    ) { category ->
                        IosChip(
                            label = category.titleDe,
                            selected = category == app.category,
                            onClick = {
                                viewModel.changeAppCategory(app.packageName, category)
                                onEditingCategoryChange(null)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.languageItems(
    viewModel: LauncherViewModel,
    settings: LauncherSettings
) {
    item(key = "language.list") {
        IosGroup {
            var first = true
            for (language in AppLanguage.entries) {
                if (!first) IosDivider()
                first = false
                IosCheckRow(
                    title = language.displayName,
                    selected = settings.appLanguage == language,
                    onClick = { viewModel.updateSettings { it.copy(appLanguage = language) } }
                )
            }
        }
    }
}

/* ========================================================================== */
/*                             iOS 18 components                              */
/* ========================================================================== */

/** iOS navigation bar with a collapsing large title. */
@Composable
private fun IosSettingsNavBar(
    title: String,
    compact: Boolean,
    backLabel: String,
    onBack: () -> Unit
) {
    val compactAlpha by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = tween(160),
        label = "compactTitle"
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = tween(160),
        label = "navSurface"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000).copy(alpha = surfaceAlpha))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = 8.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = backLabel,
                    color = Color(0xFF0A84FF),
                    style = IosText.body.copy(fontSize = 16.sp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                color = Color.White,
                style = IosText.headline,
                modifier = Modifier.alpha(compactAlpha)
            )
            Spacer(Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.16f * surfaceAlpha))
        )
    }
}

/** iOS inset grouped list container. */
@Composable
private fun IosGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF1C1C1E))
    ) {
        content()
    }
}

@Composable
private fun IosDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(Color.White.copy(alpha = 0.12f))
    )
}

@Composable
private fun IosHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF8E8E93),
        style = IosText.footnote.copy(fontSize = 12.5.sp, letterSpacing = 0.4.sp),
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun IosFooter(text: String) {
    Text(
        text = text,
        color = Color(0xFF8E8E93),
        style = IosText.footnote,
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 7.dp, bottom = 4.dp)
    )
}

/** Navigation row with an iOS system icon tile, an optional value and a chevron. */
@Composable
private fun IosNavRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(29.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White,
            style = IosText.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = value,
                color = Color(0xFF8E8E93),
                style = IosText.body.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF48484A),
            modifier = Modifier.size(13.dp)
        )
    }
}

/** Row that performs an action and shows a description under the title. */
@Composable
private fun IosActionRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(29.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = IosText.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = Color(0xFF8E8E93),
                style = IosText.footnote,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Switch row, iOS style: label on the left, green switch on the right. */
@Composable
private fun IosSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            style = IosText.body,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        IosSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** The iOS switch: 51 × 31 pt track, 27 pt knob, green when on. */
@Composable
private fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF30D158) else Color(0xFF39393D),
        animationSpec = tween(180),
        label = "switchTrack"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 700f
        ),
        label = "switchKnob"
    )

    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .padding(top = 2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(1.dp, CircleShape)
        )
    }
}

/** Single selection row with the iOS check mark. */
@Composable
private fun IosCheckRow(
    title: String,
    selected: Boolean,
    detail: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = IosText.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = Color(0xFF8E8E93),
                    style = IosText.footnote,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF0A84FF),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/** iOS segmented control. */
@Composable
private fun IosSegmentedRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    label: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                color = Color(0xFF8E8E93),
                style = IosText.footnote,
                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF3A3A3C))
        ) {
            labels.forEachIndexed { index, text ->
                val selected = index == selectedIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selected) Color(0xFF636366) else Color.Transparent)
                        .clickable { onSelect(index) }
                ) {
                    Text(
                        text = text,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        style = IosText.footnote.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

/** iOS slider: thin track, big white knob, blue fill. */
@Composable
private fun IosSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = Color.White,
                style = IosText.body,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                color = Color(0xFF8E8E93),
                style = IosText.body.copy(fontSize = 16.sp)
            )
        }
        Spacer(Modifier.height(8.dp))
        IosSlider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange
        )
    }
}

@Composable
private fun IosSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val trackHeight = 4.dp
    val knobSize = 28.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(knobSize)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val usableWidth = maxWidth
        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        val knobOffset by animateDpAsState(
            targetValue = (usableWidth - knobSize) * fraction,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f),
            label = "sliderKnob"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color(0xFF39393D))
        )
        Box(
            modifier = Modifier
                .width(knobOffset + knobSize / 2)
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color(0xFF0A84FF))
        )
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(2.dp, CircleShape)
        )
    }
}

/** Text field row, iOS style: label on the left, field on the right. */
@Composable
private fun IosTextFieldRow(
    title: String,
    value: String,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    onValueChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = Color.White,
            style = IosText.body,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Box(contentAlignment = Alignment.CenterEnd) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = IosText.body.copy(
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                modifier = Modifier.widthIn(min = 110.dp)
            )
            if (value.isBlank()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF8E8E93),
                    style = IosText.body.copy(fontSize = 16.sp),
                    modifier = Modifier.offset(x = (-6).dp)
                )
            }
        }
    }
}

/** Chip used for city presets and categories. */
@Composable
private fun IosChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFF0A84FF) else Color(0xFF2C2C2E))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = IosText.footnote.copy(fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

/** Accent colour chip with a colour dot. */
@Composable
private fun IosAccentChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFF2C2C2E) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) color else Color.White.copy(alpha = 0.16f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = Color.White,
            style = IosText.footnote.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/** Live preview of the current icon style. */
@Composable
private fun IosPreviewCard(
    title: String,
    apps: List<AppInfo>,
    settings: LauncherSettings
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1F2430), Color(0xFF12141C)))
            )
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = Color.White.copy(alpha = 0.45f),
            style = IosText.caption.copy(letterSpacing = 0.9.sp)
        )
        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            apps.forEach { app ->
                IosAppIconView(
                    app = app,
                    iconSize = 58.dp * settings.iconSizeScale,
                    labelFontSizeSp = settings.labelFontSizeSp,
                    showLabel = settings.showAppLabels,
                    showBadges = settings.enableBadges,
                    isJiggleMode = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }
}

/** Row used in the app manager: icon, name, category and a hide/unhide button. */
@Composable
private fun IosAppManageRow(
    label: String,
    detail: String,
    icon: ImageBitmap?,
    hidden: Boolean,
    hideLabel: String,
    unhideLabel: String,
    onToggleHidden: () -> Unit,
    onEditCategory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF2C2C2E))
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color.White,
                    style = IosText.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    color = Color(0xFF0A84FF),
                    style = IosText.footnote,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onEditCategory)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (hidden) unhideLabel else hideLabel,
                color = if (hidden) Color(0xFF30D158) else Color.White,
                style = IosText.body.copy(fontSize = 15.sp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hidden) Color(0xFF30D158).copy(alpha = 0.18f) else Color(0xFF2C2C2E))
                    .clickable(onClick = onToggleHidden)
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            )
        }
    }
}

/* Small local copies of the grouped-list labels used by the grid section. */
private const val stringsFoldDisplay = "Faltdisplay"
private const val stringsCoverScreen = "Außendisplay"
private const val stringsInnerScreen = "Innendisplay"
private const val stringsDockApps = "Apps im Dock"
private const val stringsDockRecent = "Zuletzt verwendete im Dock"
private const val stringsColumns = "Spalten"
private const val stringsRows = "Zeilen"
