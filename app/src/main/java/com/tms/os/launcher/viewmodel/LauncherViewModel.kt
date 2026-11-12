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
    val isDefaultLauncher: Boolean = false,
    val isBadgeServiceEnabled: Boolean = false,
    val isUsageStatsGranted: Boolean = false,
    val batteryPercent: Int = 85,
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

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshApps()
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

        _uiState.update {
            it.copy(
                settings = loadedSettings,
                isDefaultLauncher = repository.isDefaultLauncher(),
                isBadgeServiceEnabled = repository.isNotificationListenerEnabled(),
                isUsageStatsGranted = repository.isUsageStatsPermissionGranted(),
                batteryPercent = repository.getDeviceBatteryPercent(),
                storageRamInfo = repository.getStorageAndRamInfo()
            )
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
        if (!force && elapsed < 20 * 60 * 1000L && s.weatherLastUpdated > 0L) return
        refreshLiveWeatherForCity(s.weatherCity)
    }

    fun refreshLiveWeatherForCity(cityQuery: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherRefreshing = true) }
            val data = WeatherService.fetchLiveWeatherForCity(cityQuery)
            if (data != null) {
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

    fun launchApp(packageName: String) {
        val ok = repository.launchApp(packageName)
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
                    selectedAppForMenu = null
                )
            }
        }
    }

    fun dialContact(phone: String) {
        repository.dialPhoneNumber(phone)
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

    fun openSpotlight() {
        val apps = _uiState.value.allApps
        _uiState.update {
            it.copy(
                isSpotlightOpen = true,
                spotlightQuery = "",
                spotlightResults = SpotlightSearchEngine.searchApps("", apps),
                spotlightShortcuts = emptyList(),
                spotlightContacts = emptyList(),
                spotlightMathResult = null,
                selectedAppForMenu = null
            )
        }
    }

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
        val results = SpotlightSearchEngine.searchApps(query, state.allApps)
        val shortcuts = SpotlightSearchEngine.generateQuickShortcuts(query, results)
        val contacts = SpotlightSearchEngine.searchContacts(query, state.allContacts)
        val mathResult = SpotlightSearchEngine.evaluateMathOrConversion(query)

        _uiState.update {
            it.copy(
                spotlightQuery = query,
                spotlightResults = results,
                spotlightShortcuts = shortcuts,
                spotlightContacts = contacts,
                spotlightMathResult = mathResult
            )
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
        _uiState.update { it.copy(selectedAppForMenu = app) }
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
    fun onDragDropGridItem(
        draggedItemId: String,
        targetItemId: String,
        mergeAsFolder: Boolean
    ) {
        if (draggedItemId == targetItemId) return
        val current = _uiState.value.homeGridItems.toMutableList()
        val fromIdx = current.indexOfFirst { it.id == draggedItemId }
        val toIdx = current.indexOfFirst { it.id == targetItemId }
        if (fromIdx < 0 || toIdx < 0) return

        val dragged = current[fromIdx]
        val target = current[toIdx]

        if (mergeAsFolder && dragged is HomeGridItem.AppItem) {
            when (target) {
                is HomeGridItem.FolderItem -> {
                    current.removeAt(fromIdx)
                    val newToIdx = current.indexOfFirst { it.id == targetItemId }
                    if (newToIdx >= 0 && !target.appPackages.contains(dragged.packageName)) {
                        current[newToIdx] = target.copy(appPackages = target.appPackages + dragged.packageName)
                    }
                    repository.saveHomeGridItems(current)
                    _uiState.update { it.copy(homeGridItems = current) }
                    return
                }
                is HomeGridItem.AppItem -> {
                    val targetApp = _uiState.value.appMap[target.packageName]
                    val folderTitle = targetApp?.category?.titleDe ?: "Neuer Ordner"
                    val newFolder = HomeGridItem.FolderItem(
                        folderId = "folder_${UUID.randomUUID()}",
                        title = folderTitle,
                        appPackages = listOf(target.packageName, dragged.packageName).distinct(),
                        page = target.page,
                        position = toIdx
                    )
                    current[toIdx] = newFolder
                    current.removeAt(fromIdx)
                    repository.saveHomeGridItems(current)
                    _uiState.update { it.copy(homeGridItems = current) }
                    return
                }
            }
        }

        // Standard Reorder / Swap between slots
        val targetPage = when (target) {
            is HomeGridItem.AppItem -> target.page
            is HomeGridItem.FolderItem -> target.page
        }
        val movedItem = when (dragged) {
            is HomeGridItem.AppItem -> {
                if (targetPage == 0) {
                    repository.setAppPinnedOnPageOne(dragged.packageName, true)
                }
                dragged.copy(page = targetPage)
            }
            is HomeGridItem.FolderItem -> dragged.copy(page = targetPage)
        }

        current.removeAt(fromIdx)
        current.add(toIdx.coerceIn(0, current.size), movedItem)
        repository.saveHomeGridItems(current)
        _uiState.update { it.copy(homeGridItems = current) }
    }

    fun togglePinOnPageOne(packageName: String) {
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

    fun hideOrUnhideApp(packageName: String, hidden: Boolean) {
        repository.setAppHidden(packageName, hidden)
        refreshApps()
    }

    fun openAppInfo(packageName: String) {
        repository.openAppSystemSettings(packageName)
        _uiState.update { it.copy(selectedAppForMenu = null) }
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
}
