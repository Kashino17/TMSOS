package com.tms.os.launcher.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tms.os.launcher.data.LauncherRepository
import com.tms.os.launcher.domain.SpotlightSearchEngine
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
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
    val homeGridItems: List<HomeGridItem> = emptyList(),
    val dockPackages: List<String> = emptyList(),
    val recentPackages: List<String> = emptyList(),
    val categorizedApps: Map<AppCategory, List<AppInfo>> = emptyMap(),
    val settings: LauncherSettings = LauncherSettings(),
    val isJiggleMode: Boolean = false,
    val isSpotlightOpen: Boolean = false,
    val spotlightQuery: String = "",
    val spotlightResults: List<AppInfo> = emptyList(),
    val spotlightMathResult: String? = null,
    val activeFolder: HomeGridItem.FolderItem? = null,
    val activeCategoryModal: AppCategory? = null,
    val selectedAppForMenu: AppInfo? = null,
    val isDefaultLauncher: Boolean = false,
    val isBadgeServiceEnabled: Boolean = false
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        LauncherUiState(
            settings = repository.loadSettings(),
            isDefaultLauncher = repository.isDefaultLauncher(),
            isBadgeServiceEnabled = repository.isNotificationListenerEnabled()
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

        // Observe live notification badges
        viewModelScope.launch {
            BadgeNotificationService.badgeCounts.collect { badgeMap ->
                updateBadgesInMemory(badgeMap)
            }
        }

        refreshApps()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
        }
    }

    fun refreshSystemStatus() {
        _uiState.update {
            it.copy(
                settings = repository.loadSettings(),
                isDefaultLauncher = repository.isDefaultLauncher(),
                isBadgeServiceEnabled = repository.isNotificationListenerEnabled()
            )
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val badges = BadgeNotificationService.badgeCounts.value
            val settings = repository.loadSettings()
            val apps = repository.loadInstalledApps(badges)
            val appMap = apps.associateBy { it.packageName }
            val dock = repository.loadDockPackages(apps)
            val grid = repository.loadHomeGridItems(apps, dock)
            val recents = computeRecentPackages(apps, dock)
            val categorized = buildCategoryMap(apps)

            _uiState.update { state ->
                val updatedFolder = state.activeFolder?.let { f ->
                    grid.filterIsInstance<HomeGridItem.FolderItem>().find { it.folderId == f.folderId }
                }
                state.copy(
                    isLoading = false,
                    allApps = apps,
                    appMap = appMap,
                    homeGridItems = grid,
                    dockPackages = dock,
                    recentPackages = recents,
                    categorizedApps = categorized,
                    settings = settings,
                    activeFolder = updatedFolder,
                    spotlightResults = SpotlightSearchEngine.searchApps(state.spotlightQuery, apps),
                    isDefaultLauncher = repository.isDefaultLauncher(),
                    isBadgeServiceEnabled = repository.isNotificationListenerEnabled()
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
            .filter { !it.isHidden && !dockSet.contains(it.packageName) && it.lastLaunchedTime > 0L }
            .sortedByDescending { it.lastLaunchedTime }
            .take(3)
            .map { it.packageName }
    }

    private fun buildCategoryMap(apps: List<AppInfo>): Map<AppCategory, List<AppInfo>> {
        val visible = apps.filter { !it.isHidden }
        val map = LinkedHashMap<AppCategory, List<AppInfo>>()

        // 1. Suggestions (top used + recently installed)
        val suggestions = visible
            .sortedWith(
                compareByDescending<AppInfo> { it.launchCount }
                    .thenByDescending { it.lastLaunchedTime }
                    .thenByDescending { it.firstInstallTime }
            )
            .take(8)
        if (suggestions.isNotEmpty()) {
            map[AppCategory.SUGGESTIONS] = suggestions
        }

        // 2. Group by Smart Category and sort each category by usage frequency then label
        val grouped = visible.groupBy { it.category }
        for (cat in AppCategory.entries) {
            if (cat == AppCategory.SUGGESTIONS) continue
            val list = grouped[cat]
                ?.sortedWith(
                    compareByDescending<AppInfo> { it.launchCount }
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

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val newSettings = transform(_uiState.value.settings)
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings) }
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
                spotlightMathResult = null
            )
        }
    }

    fun updateSpotlightQuery(query: String) {
        val state = _uiState.value
        val results = SpotlightSearchEngine.searchApps(query, state.allApps)
        val mathResult = SpotlightSearchEngine.evaluateMathOrConversion(query)

        _uiState.update {
            it.copy(
                spotlightQuery = query,
                spotlightResults = results,
                spotlightMathResult = mathResult
            )
        }

        // Instant launch if enabled and query is at least 2 chars and unambiguous
        if (state.settings.instantLaunchSingleSearchResult &&
            query.trim().length >= 2 &&
            results.size == 1 &&
            mathResult == null
        ) {
            launchApp(results[0].packageName)
        }
    }

    fun openFolder(folder: HomeGridItem.FolderItem?) {
        _uiState.update { it.copy(activeFolder = folder, selectedAppForMenu = null) }
    }

    fun openCategoryModal(category: AppCategory?) {
        _uiState.update { it.copy(activeCategoryModal = category, selectedAppForMenu = null) }
    }

    fun selectAppForContextMenu(app: AppInfo?) {
        _uiState.update { it.copy(selectedAppForMenu = app) }
    }

    fun autoOrganizeHomeWithSmartFolders() {
        val state = _uiState.value
        val newGrid = repository.generateSmartOrganizedHomeGrid(state.allApps, state.dockPackages)
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
        // Remove standalone AppItem or from existing folder
        current.removeAll { it is HomeGridItem.AppItem && it.packageName == packageName }
        for (i in current.indices) {
            val item = current[i]
            if (item is HomeGridItem.FolderItem && item.folderId != targetFolderId && item.appPackages.contains(packageName)) {
                val remaining = item.appPackages - packageName
                current[i] = item.copy(appPackages = remaining)
            }
        }
        // Add to target folder
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
            page = 0,
            position = if (existingIdx >= 0) existingIdx else 0
        )
        if (existingIdx in 0..current.size) {
            current.add(existingIdx, newFolder)
        } else {
            current.add(0, newFolder)
        }
        repository.saveHomeGridItems(current)
        _uiState.update { it.copy(homeGridItems = current, selectedAppForMenu = null) }
    }

    fun togglePinToDock(packageName: String) {
        val currentDock = _uiState.value.dockPackages.toMutableList()
        if (currentDock.contains(packageName)) {
            if (currentDock.size > 1) {
                currentDock.remove(packageName)
            }
        } else {
            if (currentDock.size >= 6) {
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
