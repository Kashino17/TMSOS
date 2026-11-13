package com.tms.os.launcher.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tms.os.launcher.data.LauncherRepository
import com.tms.os.launcher.data.SystemStorageRamInfo
import com.tms.os.launcher.domain.SpotlightSearchEngine
import com.tms.os.launcher.domain.WeatherService
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppShortcutItem
import com.tms.os.launcher.model.ContactItem
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.service.BadgeNotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class LauncherUiState(
    val isLoading: Boolean = true,
    val homePressGeneration: Int = 0,
    val allApps: List<AppInfo> = emptyList(),
    val appMap: Map<String, AppInfo> = emptyMap(),
    val allContacts: List<ContactItem> = emptyList(),
    val homeGridItems: List<HomeGridItem> = emptyList(),
    val dockPackages: List<String> = emptyList(),
    val recentPackages: List<String> = emptyList(),
    val categorizedApps: Map<AppCategory, List<AppInfo>> = emptyMap(),
    val settings: LauncherSettings = LauncherSettings(),
    val isJiggleMode: Boolean = false,
    val isSpotlightOpen: Boolean = false,
    val spotlightQuery: String = "",
    val spotlightResults: List<AppInfo> = emptyList(),
    val spotlightShortcuts: List<AppShortcutItem> = emptyList(),
    val spotlightContacts: List<ContactItem> = emptyList(),
    val spotlightMathResult: String? = null,
    val activeFolder: HomeGridItem.FolderItem? = null,
    val folderOriginNormalized: Offset = Offset(0.5f, 0.5f),
    val activeCategoryModal: AppCategory? = null,
    val selectedAppForMenu: AppInfo? = null,
    val todayWidgets: List<HomeGridItem.WidgetItem> = emptyList(),
    val menuQuickActions: List<com.tms.os.launcher.model.AppQuickAction> = emptyList(),
    /** Real shortcuts of Spotlight's top hit ("New Tab" for Chrome, …). */
    val spotlightTopActions: List<com.tms.os.launcher.model.AppQuickAction> = emptyList(),
    /** Packages recently opened from Spotlight, newest first ("Verlauf"). */
    val spotlightHistory: List<String> = emptyList(),
    val weatherForecast: com.tms.os.launcher.domain.WeatherForecast? = null,
    val isDefaultLauncher: Boolean = false,
    val isBadgeServiceEnabled: Boolean = false,
    val isUsageStatsGranted: Boolean = false,
    val batteryPercent: Int = 85,
    val screenTime: com.tms.os.launcher.model.ScreenTime = com.tms.os.launcher.model.ScreenTime(),
    val storageRamInfo: SystemStorageRamInfo = SystemStorageRamInfo(6.4f, 12.0f, 128, 512),
    val isWeatherRefreshing: Boolean = false
) {
    val isAnyOverlayOpen: Boolean
        get() = isSpotlightOpen || activeFolder != null || activeCategoryModal != null || selectedAppForMenu != null
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        LauncherUiState(
            settings = repository.loadSettings(),
            isDefaultLauncher = repository.isDefaultLauncher(),
            isBadgeServiceEnabled = repository.isNotificationListenerEnabled(),
            isUsageStatsGranted = repository.isUsageStatsPermissionGranted(),
            batteryPercent = repository.getDeviceBatteryPercent(),
            storageRamInfo = repository.getStorageAndRamInfo()
        )
    )
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var packageRefresh: kotlinx.coroutines.Job? = null
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // An app update arrives as several broadcasts in a row: reload once, afterwards.
            packageRefresh?.cancel()
            packageRefresh = viewModelScope.launch { kotlinx.coroutines.delay(600); refreshApps() }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            application.registerReceiver(packageReceiver, filter)
        }

        viewModelScope.launch {
            BadgeNotificationService.badgeCounts.collect { badgeMap ->
                updateBadgesInMemory(badgeMap)
            }
        }

        refreshApps()
        refreshLiveWeatherIfNeeded()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
        }
    }

    fun refreshSystemStatus() {
        val loadedSettings = repository.loadSettings()
        val prevSettings = _uiState.value.settings
        val iconsChanged = prevSettings.useIosSystemIcons != loadedSettings.useIosSystemIcons ||
            prevSettings.fullBleedAdaptiveIcons != loadedSettings.fullBleedAdaptiveIcons ||
            prevSettings.iconThemeMode != loadedSettings.iconThemeMode ||
            prevSettings.tintAccentColor != loadedSettings.tintAccentColor

        _uiState.update { it.copy(settings = loadedSettings) }
        // System queries are cross-process calls: not on the main thread while the
        // return-to-home animation is running.
        viewModelScope.launch {
            val status = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SystemStatus(
                    repository.isDefaultLauncher(), repository.isNotificationListenerEnabled(), repository.isUsageStatsPermissionGranted(),
                    repository.getDeviceBatteryPercent(), repository.getStorageAndRamInfo()
                )
            }
            _uiState.update {
                it.copy(
                    isDefaultLauncher = status.defaultLauncher,
                    isBadgeServiceEnabled = status.badgeService,
                    isUsageStatsGranted = status.usageStats,
                    batteryPercent = status.battery,
                    storageRamInfo = status.storage
                )
            }
        }

        if (iconsChanged) {
            repository.clearIconCache()
            refreshApps()
        }
        refreshLiveWeatherIfNeeded()
    }

    fun refreshLiveWeatherIfNeeded(force: Boolean = false) {
        val s = _uiState.value.settings
        val elapsed = System.currentTimeMillis() - s.weatherLastUpdated
        val hasForecast = _uiState.value.weatherForecast != null
        if (!force && hasForecast && elapsed < 20 * 60 * 1000L && s.weatherLastUpdated > 0L) return
        refreshLiveWeatherForCity(s.weatherCity)
    }

    fun refreshLiveWeatherForCity(cityQuery: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherRefreshing = true) }
            val data = WeatherService.fetchLiveWeatherForCity(cityQuery)
            if (data != null) {
                _uiState.update { it.copy(weatherForecast = data.forecast ?: it.weatherForecast) }
                updateSettings { current ->
                    current.copy(
                        weatherCity = data.resolvedCity,
                        weatherTempCelsius = data.tempCelsius,
                        weatherHighCelsius = data.highCelsius,
                        weatherLowCelsius = data.lowCelsius,
                        weatherCode = data.weatherCode,
                        weatherCondition = data.conditionDe,
                        weatherLastUpdated = System.currentTimeMillis()
                    )
                }
            }
            _uiState.update { it.copy(isWeatherRefreshing = false) }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val badges = BadgeNotificationService.badgeCounts.value
            val settings = repository.loadSettings()
            val apps = repository.loadInstalledApps(badges, settings)
            val contacts = repository.loadContactsIfPermitted()
            val appMap = apps.associateBy { it.packageName }
            val dock = repository.loadDockPackages(apps, settings.dockIconCount)
            val grid = repository.loadHomeGridItems(apps, dock)
            val recents = computeRecentPackages(apps, dock)
            val categorized = buildCategoryMap(apps)

            _uiState.update { state ->
                val updatedFolder = state.activeFolder?.let { f ->
                    grid.filterIsInstance<HomeGridItem.FolderItem>().find { it.folderId == f.folderId }
                }
                val topResults = SpotlightSearchEngine.searchApps(state.spotlightQuery, apps)
                state.copy(
                    isLoading = false,
                    allApps = apps,
                    appMap = appMap,
                    allContacts = contacts,
                    homeGridItems = grid,
                    todayWidgets = repository.loadTodayWidgets(),
                    spotlightHistory = repository.loadSpotlightHistory(),
                    dockPackages = dock,
                    recentPackages = recents,
                    categorizedApps = categorized,
                    settings = settings,
                    activeFolder = updatedFolder,
                    spotlightResults = topResults,
                    spotlightShortcuts = SpotlightSearchEngine.generateQuickShortcuts(state.spotlightQuery, topResults),
                    spotlightContacts = SpotlightSearchEngine.searchContacts(state.spotlightQuery, contacts),
                    isDefaultLauncher = repository.isDefaultLauncher(),
                    isBadgeServiceEnabled = repository.isNotificationListenerEnabled(),
                    isUsageStatsGranted = repository.isUsageStatsPermissionGranted(),
                    batteryPercent = repository.getDeviceBatteryPercent(),
                    storageRamInfo = repository.getStorageAndRamInfo()
                )
            }
        }
    }

    private fun updateBadgesInMemory(badgeMap: Map<String, Int>) {
        _uiState.update { state ->
            if (state.allApps.isEmpty()) return@update state
            val updatedApps = state.allApps.map { app ->
                app.copy(badgeCount = badgeMap[app.packageName] ?: 0)
            }
            val map = updatedApps.associateBy { it.packageName }
            state.copy(
                allApps = updatedApps,
                appMap = map,
                categorizedApps = buildCategoryMap(updatedApps)
            )
        }
    }

    private fun computeRecentPackages(apps: List<AppInfo>, dockPackages: List<String>): List<String> {
        val dockSet = dockPackages.toSet()
        return apps
            .filter { !it.isHidden && !dockSet.contains(it.packageName) && (it.lastLaunchedTime > 0L || it.usageDurationMinutes30d > 0) }
            .sortedWith(
                compareByDescending<AppInfo> { it.lastLaunchedTime }
                    .thenByDescending { it.usageDurationMinutes30d }
            )
            .take(3)
            .map { it.packageName }
    }

    private fun buildCategoryMap(apps: List<AppInfo>): Map<AppCategory, List<AppInfo>> {
        val visible = apps.filter { !it.isHidden }
        val map = LinkedHashMap<AppCategory, List<AppInfo>>()

        val suggestions = visible
            .sortedWith(
                compareByDescending<AppInfo> { it.effectivePriorityScore }
                    .thenByDescending { it.lastLaunchedTime }
                    .thenByDescending { it.firstInstallTime }
            )
            .take(8)
        if (suggestions.isNotEmpty()) {
            map[AppCategory.SUGGESTIONS] = suggestions
        }

        val grouped = visible.groupBy { it.category }
        for (cat in AppCategory.entries) {
            if (cat == AppCategory.SUGGESTIONS) continue
            val list = grouped[cat]
                ?.sortedWith(
                    compareByDescending<AppInfo> { it.effectivePriorityScore }
                        .thenBy { it.displayLabel.lowercase(Locale.ROOT) }
                )
                .orEmpty()
            if (list.isNotEmpty()) {
                map[cat] = list
            }
        }
        return map
    }

    fun launchApp(packageName: String, options: android.os.Bundle? = null) {
        val ok = repository.launchApp(packageName, options)
        if (ok) {
            _uiState.update { state ->
                val updatedApps = state.allApps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(
                            launchCount = app.launchCount + 1,
                            lastLaunchedTime = System.currentTimeMillis()
                        )
                    } else app
                }
                val appMap = updatedApps.associateBy { it.packageName }
                state.copy(
                    allApps = updatedApps,
                    appMap = appMap,
                    recentPackages = computeRecentPackages(updatedApps, state.dockPackages),
                    categorizedApps = buildCategoryMap(updatedApps),
                    isSpotlightOpen = false,
                    spotlightQuery = "",
                    activeFolder = null,
                    activeCategoryModal = null,
                    selectedAppForMenu = null
                )
            }
        }
    }

    fun dialContact(phone: String) {
        repository.dialPhoneNumber(phone)
        closeSpotlight()
    }

    fun messageContact(phone: String) {
        repository.messagePhoneNumber(phone)
        closeSpotlight()
    }

    fun searchWeb(query: String, engine: String = "google") {
        repository.launchWebSearch(query, engine)
        closeSpotlight()
    }

    fun openWallpaperPicker() {
        repository.openWallpaperPicker()
    }

    fun expandStatusBar() {
        repository.expandSystemNotificationShade()
    }

    fun mediaPlayPause() {
        repository.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun mediaNext() {
        repository.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun mediaPrevious() {
        repository.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun cycleLeftWidget(delta: Int) {
        val entries = com.tms.os.launcher.model.LeftWidgetType.entries
        val cur = _uiState.value.settings.leftWidgetType
        val idx = (entries.indexOf(cur) + delta).mod(entries.size)
        updateSettings { it.copy(leftWidgetType = entries[idx]) }
    }

    fun cycleRightWidget(delta: Int) {
        val entries = com.tms.os.launcher.model.RightWidgetType.entries
        val cur = _uiState.value.settings.rightWidgetType
        val idx = (entries.indexOf(cur) + delta).mod(entries.size)
        updateSettings { it.copy(rightWidgetType = entries[idx]) }
    }

    fun updateQuickNote(text: String) {
        updateSettings { it.copy(quickNoteText = text) }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val old = _uiState.value.settings
        val newSettings = transform(old)
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings) }

        if (old.useIosSystemIcons != newSettings.useIosSystemIcons ||
            old.fullBleedAdaptiveIcons != newSettings.fullBleedAdaptiveIcons ||
            old.iconThemeMode != newSettings.iconThemeMode ||
            old.tintAccentColor != newSettings.tintAccentColor ||
            old.dockIconCount != newSettings.dockIconCount
        ) {
            repository.clearIconCache()
            refreshApps()
        }
    }

    fun setJiggleMode(enabled: Boolean) {
        _uiState.update { it.copy(isJiggleMode = enabled, selectedAppForMenu = null) }
    }

    /** Opens an app from Spotlight and remembers it in the search history. */
    fun launchFromSpotlight(packageName: String) {
        val history = (listOf(packageName) + _uiState.value.spotlightHistory).distinct().take(8)
        repository.saveSpotlightHistory(history)
        _uiState.update { it.copy(spotlightHistory = history) }
        launchApp(packageName)
    }

    fun clearSpotlightHistory() {
        repository.saveSpotlightHistory(emptyList())
        _uiState.update { it.copy(spotlightHistory = emptyList()) }
    }

    fun openSpotlight() {
        val apps = _uiState.value.allApps
        _uiState.update {
            it.copy(
                isSpotlightOpen = true,
                spotlightQuery = "",
                spotlightResults = SpotlightSearchEngine.searchApps("", apps.forSpotlight()),
                spotlightShortcuts = emptyList(),
                spotlightContacts = emptyList(),
                spotlightMathResult = null,
                selectedAppForMenu = null
            )
        }
    }

    /** Screen Time widget: re-read whenever the home screen comes back to the front. */
    fun refreshScreenTime() {
        viewModelScope.launch {
            val st = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.todayScreenTime() }
            _uiState.update { it.copy(screenTime = st, batteryPercent = repository.getDeviceBatteryPercent()) }
        }
    }

    fun openUsageAccessSettings() = repository.openUsageAccessSettings()

    /** The launcher itself is found in the App Library, not in Spotlight. */
    private fun List<AppInfo>.forSpotlight() = filterNot { it.packageName == getApplication<Application>().packageName }

    fun closeSpotlight() {
        _uiState.update {
            it.copy(
                isSpotlightOpen = false,
                spotlightQuery = "",
                spotlightShortcuts = emptyList(),
                spotlightContacts = emptyList(),
                spotlightMathResult = null
            )
        }
    }

    fun updateSpotlightQuery(query: String) {
        val state = _uiState.value
        val results = SpotlightSearchEngine.searchApps(query, state.allApps.forSpotlight())
        val shortcuts = SpotlightSearchEngine.generateQuickShortcuts(query, results)
        val contacts = SpotlightSearchEngine.searchContacts(query, state.allContacts)
        val mathResult = SpotlightSearchEngine.evaluateMathOrConversion(query)

        _uiState.update {
            it.copy(
                spotlightQuery = query,
                spotlightResults = results,
                spotlightShortcuts = shortcuts,
                spotlightContacts = contacts,
                spotlightMathResult = mathResult,
                spotlightTopActions = if (results.firstOrNull()?.packageName == it.spotlightResults.firstOrNull()?.packageName && query.isNotBlank()) it.spotlightTopActions else emptyList()
            )
        }
        val top = results.firstOrNull()
        if (top != null && query.isNotBlank()) viewModelScope.launch {
            val actions = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.quickActions(top.packageName, limit = 3) }
            _uiState.update { s -> if (s.spotlightResults.firstOrNull()?.packageName == top.packageName) s.copy(spotlightTopActions = actions) else s }
        }

        if (state.settings.instantLaunchSingleSearchResult &&
            query.trim().length >= 2 &&
            results.size == 1 &&
            mathResult == null
        ) {
            launchApp(results[0].packageName)
        }
    }

    fun openFolder(
        folder: HomeGridItem.FolderItem?,
        originNormalized: Offset = Offset(0.5f, 0.5f)
    ) {
        _uiState.update {
            it.copy(
                activeFolder = folder,
                folderOriginNormalized = originNormalized,
                selectedAppForMenu = null
            )
        }
    }

    fun openCategoryModal(category: AppCategory?) {
        _uiState.update { it.copy(activeCategoryModal = category, selectedAppForMenu = null) }
    }

    fun selectAppForContextMenu(app: AppInfo?) {
        _uiState.update { it.copy(selectedAppForMenu = app, menuQuickActions = emptyList()) }
        if (app != null) viewModelScope.launch {
            val actions = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.quickActions(app.packageName) }
            _uiState.update { if (it.selectedAppForMenu?.packageName == app.packageName) it.copy(menuQuickActions = actions) else it }
        }
    }

    fun runQuickAction(action: com.tms.os.launcher.model.AppQuickAction) {
        repository.startQuickAction(action)
        _uiState.update { it.copy(selectedAppForMenu = null) }
    }

    fun autoOrganizeHomeWithSmartFolders() {
        val state = _uiState.value
        val s = state.settings
        val page0SingleRows = if (s.showTopWidgetsOnFirstPage) (s.coverRows - 2).coerceAtLeast(2) else s.coverRows
        val page0Capacity = (s.coverColumns * page0SingleRows).coerceIn(8, 20)
        val newGrid = repository.generateSmartOrganizedHomeGrid(state.allApps, state.dockPackages, page0Capacity)
        _uiState.update {
            it.copy(
                homeGridItems = newGrid,
                activeFolder = null,
                isJiggleMode = false
            )
        }
    }

    fun autoOrganizeHomeFlatSorted() {
        val state = _uiState.value
        val newGrid = repository.generateFlatSortedHomeGrid(state.allApps, state.dockPackages)
        _uiState.update {
            it.copy(
                homeGridItems = newGrid,
                activeFolder = null,
                isJiggleMode = false
            )
        }
    }

    /**
     * Handles real Finger Drag-&-Drop on the Home Screen Grid!
     * - If [mergeAsFolder] is true: merges dragged AppItem into target FolderItem (or creates a new Folder from 2 AppItems).
     * - If [mergeAsFolder] is false: swaps/reorders the dragged item to the target position & page (and pins it if on Page 0!).
     */
    /** Persist the order the user produced by dragging (icons, folders, widgets). */
    fun commitHomeOrder(items: List<HomeGridItem>) {
        repository.saveHomeGridItems(items)
        _uiState.update { it.copy(homeGridItems = items) }
    }

    /**
     * iOS folder gesture: hold an app over another app (new folder) or over a
     * folder (add to it). Returns the folder that now contains the app.
     */
    fun mergeIntoFolder(draggedItemId: String, targetItemId: String) {
        val current = _uiState.value.homeGridItems.toMutableList()
        // The app may come from the dock or a folder and not be in the grid yet.
        val dragged = current.firstOrNull { it.id == draggedItemId } as? HomeGridItem.AppItem
            ?: if (draggedItemId.startsWith("app:")) HomeGridItem.AppItem(draggedItemId.removePrefix("app:")) else return
        val toIdx = current.indexOfFirst { it.id == targetItemId }
        if (toIdx < 0 || draggedItemId == targetItemId) return
        when (val target = current[toIdx]) {
            is HomeGridItem.FolderItem -> {
                if (!target.appPackages.contains(dragged.packageName)) {
                    current[toIdx] = target.copy(appPackages = target.appPackages + dragged.packageName)
                }
            }
            is HomeGridItem.AppItem -> {
                val targetApp = _uiState.value.appMap[target.packageName]
                current[toIdx] = HomeGridItem.FolderItem(
                    folderId = "folder_${UUID.randomUUID()}",
                    title = targetApp?.category?.titleDe ?: "Ordner",
                    appPackages = listOf(target.packageName, dragged.packageName).distinct(),
                    page = target.page,
                    position = toIdx
                )
            }
            is HomeGridItem.WidgetItem -> return
        }
        current.removeAll { it.id == draggedItemId }
        repository.saveHomeGridItems(current)
        _uiState.update { it.copy(homeGridItems = current) }
    }

    /** iOS: drop a widget onto another one of the same size to build a Smart Stack. */
    fun mergeWidgets(draggedId: String, targetId: String) {
        val list = _uiState.value.homeGridItems.toMutableList()
        val dragged = list.firstOrNull { it.id == draggedId } as? HomeGridItem.WidgetItem ?: return
        val idx = list.indexOfFirst { it.id == targetId }
        val target = list.getOrNull(idx) as? HomeGridItem.WidgetItem ?: return
        if (dragged.size != target.size || dragged.id == target.id) return
        list[idx] = target.copy(stack = (target.kinds + dragged.kinds).distinct())
        list.removeAll { it.id == draggedId }
        commitHomeOrder(list)
    }

    /** Adds a widget like the iOS gallery does: at [index] of the home order, or to Today. */
    fun addWidget(
        kind: com.tms.os.launcher.model.WidgetKind,
        size: com.tms.os.launcher.model.WidgetSize,
        toToday: Boolean,
        index: Int,
        stack: List<com.tms.os.launcher.model.WidgetKind> = emptyList()
    ) {
        val widget = HomeGridItem.WidgetItem("w_${UUID.randomUUID()}", kind, size, stack = stack)
        if (toToday) {
            val list = listOf(widget) + _uiState.value.todayWidgets
            repository.saveTodayWidgets(list)
            _uiState.update { it.copy(todayWidgets = list) }
        } else {
            val list = _uiState.value.homeGridItems.toMutableList()
            list.add(index.coerceIn(0, list.size), widget)
            commitHomeOrder(list)
        }
    }

    fun removeWidget(widgetId: String) {
        val today = _uiState.value.todayWidgets
        if (today.any { it.widgetId == widgetId }) {
            val list = today.filterNot { it.widgetId == widgetId }
            repository.saveTodayWidgets(list)
            _uiState.update { it.copy(todayWidgets = list) }
        } else {
            commitHomeOrder(_uiState.value.homeGridItems.filterNot { it is HomeGridItem.WidgetItem && it.widgetId == widgetId })
        }
    }

    fun commitTodayOrder(list: List<HomeGridItem.WidgetItem>) {
        repository.saveTodayWidgets(list)
        _uiState.update { it.copy(todayWidgets = list) }
    }

    fun togglePinOnPageOne(packageName: String) {
        repository.setAppRemovedFromHome(packageName, false)
        val app = _uiState.value.appMap[packageName] ?: return
        val newPinned = !app.isPinnedOnPageOne
        repository.setAppPinnedOnPageOne(packageName, newPinned)

        // If pinning to Page 1 from a folder, also extract it onto Page 0!
        if (newPinned) {
            val current = _uiState.value.homeGridItems.toMutableList()
            for (i in current.indices) {
                val item = current[i]
                if (item is HomeGridItem.FolderItem && item.appPackages.contains(packageName)) {
                    current[i] = item.copy(appPackages = item.appPackages - packageName)
                }
            }
            if (current.none { it is HomeGridItem.AppItem && it.packageName == packageName }) {
                current.add(0, HomeGridItem.AppItem(packageName, page = 0, position = 0))
            }
            val cleaned = current.filter { !(it is HomeGridItem.FolderItem && it.appPackages.isEmpty()) }
            repository.saveHomeGridItems(cleaned)
        }
        refreshApps()
    }

    fun changeAppCategory(packageName: String, newCategory: AppCategory) {
        repository.setAppCategoryOverride(packageName, newCategory)
        refreshApps()
    }

    fun renameApp(packageName: String, newName: String?) {
        repository.setCustomAppLabel(packageName, newName)
        refreshApps()
    }

    fun renameFolder(folderId: String, newTitle: String) {
        val updated = _uiState.value.homeGridItems.map { item ->
            if (item is HomeGridItem.FolderItem && item.folderId == folderId) {
                item.copy(title = newTitle.trim().ifEmpty { "Ordner" })
            } else item
        }
        repository.saveHomeGridItems(updated)
        val updatedFolder = updated.filterIsInstance<HomeGridItem.FolderItem>().find { it.folderId == folderId }
        _uiState.update { it.copy(homeGridItems = updated, activeFolder = updatedFolder) }
    }

    fun removeFolderAndUnpackApps(folderId: String) {
        val current = _uiState.value.homeGridItems.toMutableList()
        val idx = current.indexOfFirst { it is HomeGridItem.FolderItem && it.folderId == folderId }
        if (idx >= 0) {
            val folder = current.removeAt(idx) as HomeGridItem.FolderItem
            folder.appPackages.reversed().forEach { pkg ->
                current.add(idx, HomeGridItem.AppItem(pkg, page = folder.page, position = idx))
            }
            repository.saveHomeGridItems(current)
            _uiState.update { it.copy(homeGridItems = current, activeFolder = null) }
        }
    }

    fun moveAppIntoFolder(packageName: String, targetFolderId: String) {
        repository.setAppRemovedFromHome(packageName, false)
        val current = _uiState.value.homeGridItems.toMutableList()
        current.removeAll { it is HomeGridItem.AppItem && it.packageName == packageName }
        for (i in current.indices) {
            val item = current[i]
            if (item is HomeGridItem.FolderItem && item.folderId != targetFolderId && item.appPackages.contains(packageName)) {
                val remaining = item.appPackages - packageName
                current[i] = item.copy(appPackages = remaining)
            }
        }
        for (i in current.indices) {
            val item = current[i]
            if (item is HomeGridItem.FolderItem && item.folderId == targetFolderId) {
                if (!item.appPackages.contains(packageName)) {
                    current[i] = item.copy(appPackages = item.appPackages + packageName)
                }
            }
        }
        val cleaned = current.filter { !(it is HomeGridItem.FolderItem && it.appPackages.isEmpty()) }
        repository.saveHomeGridItems(cleaned)
        _uiState.update { it.copy(homeGridItems = cleaned, selectedAppForMenu = null) }
    }

    fun createNewFolderWithApp(packageName: String, folderName: String) {
        repository.setAppRemovedFromHome(packageName, false)
        val current = _uiState.value.homeGridItems.toMutableList()
        val existingIdx = current.indexOfFirst { it is HomeGridItem.AppItem && it.packageName == packageName }
        current.removeAll { it is HomeGridItem.AppItem && it.packageName == packageName }
        val newFolder = HomeGridItem.FolderItem(
            folderId = "folder_${UUID.randomUUID()}",
            title = folderName.ifBlank { "Neuer Ordner" },
            appPackages = listOf(packageName),
            page = 1,
            position = if (existingIdx >= 0) existingIdx else current.size
        )
        current.add(newFolder)
        repository.saveHomeGridItems(current)
        _uiState.update { it.copy(homeGridItems = current, selectedAppForMenu = null) }
    }

    /** Dock order produced by dragging (an app dragged into the dock leaves the grid). */
    fun setDockOrder(packages: List<String>) {
        val max = _uiState.value.settings.dockIconCount.coerceIn(3, 6)
        val dock = packages.distinct().take(max)
        repository.saveDockPackages(dock)
        _uiState.update { it.copy(dockPackages = dock) }
    }

    fun togglePinToDock(packageName: String) {
        val maxDock = _uiState.value.settings.dockIconCount.coerceIn(3, 6)
        val currentDock = _uiState.value.dockPackages.toMutableList()
        if (currentDock.contains(packageName)) {
            if (currentDock.size > 1) {
                currentDock.remove(packageName)
            }
        } else {
            if (currentDock.size >= maxDock) {
                currentDock.removeAt(currentDock.lastIndex)
            }
            currentDock.add(packageName)
        }
        repository.saveDockPackages(currentDock)
        refreshApps()
    }

    fun moveGridItemPosition(itemId: String, delta: Int) {
        val list = _uiState.value.homeGridItems.toMutableList()
        val idx = list.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, list.lastIndex)
        if (target == idx) return
        val item = list.removeAt(idx)
        list.add(target, item)
        repository.saveHomeGridItems(list)
        _uiState.update { it.copy(homeGridItems = list) }
    }

    fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(json: String): Boolean {
        val ok = repository.importBackupJson(json)
        if (ok) refreshApps()
        return ok
    }

    fun removeAppFromHome(packageName: String) {
        repository.setAppRemovedFromHome(packageName, true)
        val dock = _uiState.value.dockPackages - packageName
        repository.saveDockPackages(dock)
        refreshApps()
    }

    fun hideOrUnhideApp(packageName: String, hidden: Boolean) {
        repository.setAppHidden(packageName, hidden)
        refreshApps()
    }

    fun openAppInfo(packageName: String) {
        repository.openAppSystemSettings(packageName)
        _uiState.update { it.copy(selectedAppForMenu = null) }
    }

    /** iOS "Remove Folder": its apps leave the home screen but stay in the App Library. */
    fun removeFolderFromHome(folderId: String) {
        val folder = _uiState.value.homeGridItems.firstOrNull { it is HomeGridItem.FolderItem && it.folderId == folderId } as? HomeGridItem.FolderItem ?: return
        folder.appPackages.forEach { repository.setAppRemovedFromHome(it, true) }
        commitHomeOrder(_uiState.value.homeGridItems.filterNot { it.id == folder.id })
    }

    fun uninstallApp(packageName: String) {
        repository.requestUninstallApp(packageName)
        _uiState.update { it.copy(selectedAppForMenu = null) }
    }

    fun handleBackOrHomePress(): Boolean {
        val state = _uiState.value
        return when {
            state.selectedAppForMenu != null -> {
                _uiState.update { it.copy(selectedAppForMenu = null) }
                true
            }
            state.activeFolder != null -> {
                _uiState.update { it.copy(activeFolder = null) }
                true
            }
            state.activeCategoryModal != null -> {
                _uiState.update { it.copy(activeCategoryModal = null) }
                true
            }
            state.isSpotlightOpen -> {
                closeSpotlight()
                true
            }
            state.isJiggleMode -> {
                setJiggleMode(false)
                true
            }
            else -> false
        }
    }

    fun returnHome() {
        _uiState.update {
            it.copy(homePressGeneration = it.homePressGeneration + 1,
                isSpotlightOpen = false, spotlightQuery = "", activeFolder = null,
                activeCategoryModal = null, selectedAppForMenu = null, isJiggleMode = false)
        }
    }
}


private class SystemStatus(
    val defaultLauncher: Boolean,
    val badgeService: Boolean,
    val usageStats: Boolean,
    val battery: Int,
    val storage: SystemStorageRamInfo
)
