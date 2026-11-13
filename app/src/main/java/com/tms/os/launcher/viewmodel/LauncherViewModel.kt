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
    val solvado: com.tms.os.launcher.model.SolvadoState = com.tms.os.launcher.model.SolvadoState(),
    val storageRamInfo: SystemStorageRamInfo = SystemStorageRamInfo(6.4f, 12.0f, 128, 512),
    val isWeatherRefreshing: Boolean = false,
    /** The phone's position is being looked up for the weather (only ever on the user's tap). */
    val weatherLocating: Boolean = false,
    val weatherLocateError: Boolean = false
) {
    val isAnyOverlayOpen: Boolean
        get() = isSpotlightOpen || activeFolder != null || activeCategoryModal != null || selectedAppForMenu != null
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application.applicationContext)
    private val solvadoRepo = com.tms.os.launcher.data.SolvadoRepository(application.applicationContext)

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

        repository.layoutProfile = profileOf(_uiState.value.settings)
        refreshApps()
        refreshLiveWeatherIfNeeded()
        // Solvado widgets show the last known figures at once, fresh ones follow.
        _uiState.update { it.copy(solvado = solvadoRepo.cachedState()) }
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
        // A stored position is used as it is; only a bare city name is looked up again.
        if (!s.weatherLat.isNaN() && !s.weatherLon.isNaN()) loadWeather { WeatherService.fetchLiveWeatherAt(s.weatherLat.toDouble(), s.weatherLon.toDouble(), s.weatherCity) }
        else refreshLiveWeatherForCity(s.weatherCity)
    }

    fun refreshLiveWeatherForCity(cityQuery: String) = loadWeather { WeatherService.fetchLiveWeatherForCity(cityQuery) }

    private fun loadWeather(fetch: suspend () -> com.tms.os.launcher.domain.LiveWeatherData?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeatherRefreshing = true) }
            val data = fetch()
            if (data != null) {
                _uiState.update { it.copy(weatherForecast = data.forecast ?: it.weatherForecast) }
                updateSettings { current ->
                    current.copy(
                        weatherCity = data.resolvedCity,
                        weatherLat = data.latitude.toFloat(),
                        weatherLon = data.longitude.toFloat(),
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

    /**
     * Weather for where the phone is: the position is read once, now, because the
     * user asked for it. The caller has made sure the location permission is granted.
     */
    fun useCurrentLocationForWeather() {
        if (_uiState.value.weatherLocating) return
        _uiState.update { it.copy(weatherLocating = true, weatherLocateError = false) }
        viewModelScope.launch {
            val place = runCatching { com.tms.os.launcher.data.LocationOnce.find(getApplication()) }.getOrNull()
            if (place == null) {
                _uiState.update { it.copy(weatherLocating = false, weatherLocateError = true) }
                return@launch
            }
            val fallback = if (_uiState.value.settings.appLanguage == com.tms.os.launcher.model.AppLanguage.EN) "My Location" else "Mein Standort"
            val data = WeatherService.fetchLiveWeatherAt(place.latitude, place.longitude, place.name ?: fallback)
            _uiState.update { it.copy(weatherLocating = false, weatherLocateError = data == null) }
            if (data != null) loadWeather { data }
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

    /* ------------------------------------------- minimalist layout (iOS style) */

    /** The iOS-style minimalist home screen is a second, freely editable layout of its own. */
    private fun profileOf(settings: LauncherSettings) = if (settings.minimalMode && settings.minimalTemplate.ios) "min" else ""

    /** Every app the current layout holds: dock first, then the grid in order (folders opened up). */
    private fun layoutApps(): List<String> = (_uiState.value.dockPackages + _uiState.value.homeGridItems.flatMap {
        when (it) {
            is HomeGridItem.AppItem -> listOf(it.packageName)
            is HomeGridItem.FolderItem -> it.appPackages
            else -> emptyList()
        }
    }).distinct()

    /**
     * Lays the minimalist home screen out from a template: its widgets on top,
     * the first apps in the dock, the others on the grid. From then on it is
     * edited like any home screen (drag, add widgets, add apps).
     */
    private fun buildMinimalLayout(template: com.tms.os.launcher.model.MinimalTemplate, apps: List<String>) {
        val dockSize = _uiState.value.settings.dockIconCount.coerceIn(3, 6)
        fun widget(kind: com.tms.os.launcher.model.WidgetKind, size: com.tms.os.launcher.model.WidgetSize) =
            HomeGridItem.WidgetItem("w_${UUID.randomUUID()}", kind, size)
        val widgets = when (template) {
            com.tms.os.launcher.model.MinimalTemplate.IOS_WIDGETS -> listOf(
                widget(com.tms.os.launcher.model.WidgetKind.CALENDAR, com.tms.os.launcher.model.WidgetSize.SMALL),
                widget(com.tms.os.launcher.model.WidgetKind.WEATHER, com.tms.os.launcher.model.WidgetSize.SMALL)
            )
            com.tms.os.launcher.model.MinimalTemplate.IOS_LARGE -> listOf(widget(com.tms.os.launcher.model.WidgetKind.WEATHER, com.tms.os.launcher.model.WidgetSize.LARGE))
            // Free wallpaper on top: an invisible spacer keeps the upper rows empty.
            com.tms.os.launcher.model.MinimalTemplate.IOS_BOTTOM -> listOf(widget(com.tms.os.launcher.model.WidgetKind.SPACER, com.tms.os.launcher.model.WidgetSize.LARGE))
            else -> emptyList()
        }
        repository.saveDockPackages(apps.take(dockSize))
        repository.saveHomeGridItems(widgets + apps.drop(dockSize).map { HomeGridItem.AppItem(it) })
    }

    /** Called when a setting changed that decides which layout is shown. */
    private fun switchLayoutIfNeeded(old: LauncherSettings, new: LauncherSettings) {
        val target = profileOf(new)
        val templateChanged = target == "min" && profileOf(old) == "min" && old.minimalTemplate != new.minimalTemplate
        if (target == repository.layoutProfile && !templateChanged) return
        // What the minimalist layout starts from: its own apps when switching between its
        // templates, otherwise the apps chosen for the plain templates, otherwise the most used.
        val carried = if (templateChanged) layoutApps() else emptyList()
        repository.layoutProfile = target
        if (target == "min" && (templateChanged || !repository.hasStoredLayout())) {
            val apps = carried.ifEmpty { new.minimalApps }.ifEmpty {
                _uiState.value.allApps.filter { !it.isHidden && !it.packageName.contains("launcher") }
                    .sortedWith(compareByDescending<AppInfo> { it.effectivePriorityScore }.thenByDescending { it.lastLaunchedTime })
                    .take(8).map { it.packageName }
            }
            buildMinimalLayout(new.minimalTemplate, apps)
        }
        reloadLayout()
    }

    private fun reloadLayout() {
        val apps = _uiState.value.allApps
        val dock = repository.loadDockPackages(apps, _uiState.value.settings.dockIconCount)
        val grid = repository.loadHomeGridItems(apps, dock)
        _uiState.update { it.copy(dockPackages = dock, homeGridItems = grid, activeFolder = null, isJiggleMode = false) }
    }

    /** App picker of the iOS-style minimalist home screen: puts an app on it or takes it off. No limit. */
    fun toggleAppOnMinimalLayout(packageName: String) {
        val state = _uiState.value
        if (packageName in layoutApps()) {
            if (packageName in state.dockPackages) setDockOrder(state.dockPackages - packageName)
            commitHomeOrder(state.homeGridItems.mapNotNull {
                when (it) {
                    is HomeGridItem.AppItem -> if (it.packageName == packageName) null else it
                    is HomeGridItem.FolderItem -> (it.appPackages - packageName).let { rest -> if (rest.isEmpty()) null else it.copy(appPackages = rest) }
                    else -> it
                }
            })
        } else commitHomeOrder(state.homeGridItems + HomeGridItem.AppItem(packageName))
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val old = _uiState.value.settings
        val newSettings = transform(old)
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings) }
        switchLayoutIfNeeded(old, newSettings)

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

    /* ---------------------------------------------------------- TMS Solvado */

    private var solvadoJob: kotlinx.coroutines.Job? = null
    private var solvadoLoadedAt = 0L

    private fun solvadoWidgets(): List<HomeGridItem.WidgetItem> =
        (_uiState.value.homeGridItems.filterIsInstance<HomeGridItem.WidgetItem>() + _uiState.value.todayWidgets)
            .filter { it.kind == com.tms.os.launcher.model.WidgetKind.SOLVADO_SHOP || it.kind == com.tms.os.launcher.model.WidgetKind.SOLVADO_ADMIN }

    val solvadoServer: String get() = solvadoRepo.baseUrl

    fun solvadoLogin(email: String, password: String, server: String?) {
        if (_uiState.value.solvado.busy) return
        _uiState.update { it.copy(solvado = it.solvado.copy(busy = true, error = null)) }
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    if (!server.isNullOrBlank()) solvadoRepo.baseUrl = server
                    solvadoRepo.login(email, password)
                    solvadoRepo.loadAccount()
                }
            }
            result.onSuccess { account ->
                _uiState.update { it.copy(solvado = account) }
                // A shop widget placed before signing in gets the first shop.
                account.shops.firstOrNull()?.let { first ->
                    solvadoWidgets().filter { it.kind == com.tms.os.launcher.model.WidgetKind.SOLVADO_SHOP }.forEach { w ->
                        val cfg = com.tms.os.launcher.model.SolvadoWidgetConfig.decode(w.config, admin = false)
                        if (cfg.shopId.isEmpty()) setWidgetConfig(w.widgetId, cfg.copy(shopId = first.id).encode(), refresh = false)
                    }
                }
                refreshSolvado(force = true)
            }.onFailure { e ->
                _uiState.update { it.copy(solvado = it.solvado.copy(busy = false, error = solvadoMessage(e))) }
            }
        }
    }

    fun solvadoLogout() {
        solvadoJob?.cancel()
        solvadoRepo.logout()
        _uiState.update { it.copy(solvado = com.tms.os.launcher.model.SolvadoState()) }
    }

    /**
     * Fetches the figures the placed widgets need.
     *  - [force] = the refresh button on a widget (or a changed setting): now.
     *  - otherwise (the home screen came to the front): only if automatic refresh is
     *    on, and at most every 15 minutes. Nothing ever polls in the background.
     */
    fun refreshSolvado(force: Boolean = false) {
        if (!solvadoRepo.connected) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && !_uiState.value.settings.solvadoAutoRefresh) return
        if (!force && (solvadoJob?.isActive == true || (solvadoLoadedAt != 0L && now - solvadoLoadedAt < 15 * 60_000L))) return
        val widgets = solvadoWidgets()
        if (widgets.isEmpty() && !force) return
        solvadoLoadedAt = now
        solvadoJob?.cancel()
        _uiState.update { it.copy(solvado = it.solvado.copy(busy = true, error = null)) }
        solvadoJob = viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val account = solvadoRepo.loadAccount()
                    runCatching { solvadoRepo.loadMissingIcons(account.shops.map { it.id }) }
                    if (account.isAdmin && widgets.any { it.kind == com.tms.os.launcher.model.WidgetKind.SOLVADO_ADMIN }) solvadoRepo.loadAdmin()
                    widgets.filter { it.kind == com.tms.os.launcher.model.WidgetKind.SOLVADO_SHOP }
                        .map { com.tms.os.launcher.model.SolvadoWidgetConfig.decode(it.config, admin = false) }
                        .filter { it.shopId.isNotEmpty() }
                        .groupBy({ it.range }, { it.shopId })
                        .forEach { (range, ids) -> solvadoRepo.loadShops(ids, range) }
                    solvadoRepo.markUpdated()
                    solvadoRepo.cachedState()
                }
            }
            result.onSuccess { fresh -> _uiState.update { it.copy(solvado = fresh) } }
                .onFailure { e ->
                    // A rejected session means signed out; anything else keeps the last figures on screen.
                    if (e is com.tms.os.launcher.data.SolvadoRepository.ApiError && e.status == 401) solvadoLogout()
                    else _uiState.update { it.copy(solvado = it.solvado.copy(busy = false, error = solvadoMessage(e))) }
                }
        }
    }

    private fun solvadoMessage(e: Throwable): String = when (e) {
        is com.tms.os.launcher.data.SolvadoRepository.ApiError -> e.message ?: "Fehler"
        is java.io.IOException -> "Keine Verbindung zu Solvado."
        else -> "Solvado ist gerade nicht erreichbar."
    }

    /** Changes the size of a placed widget (from its "Edit Widget" sheet). */
    fun setWidgetSize(widgetId: String, size: com.tms.os.launcher.model.WidgetSize) {
        val today = _uiState.value.todayWidgets
        if (today.any { it.widgetId == widgetId }) {
            commitTodayOrder(today.map { if (it.widgetId == widgetId && size in it.kind.sizes) it.copy(size = size) else it })
        } else {
            commitHomeOrder(_uiState.value.homeGridItems.map {
                if (it is HomeGridItem.WidgetItem && it.widgetId == widgetId && size in it.kind.sizes) it.copy(size = size) else it
            })
        }
    }

    /** Saves what a configurable widget shows (home screen or Today view). */
    fun setWidgetConfig(widgetId: String, config: String, refresh: Boolean = true) {
        val today = _uiState.value.todayWidgets
        if (today.any { it.widgetId == widgetId }) {
            commitTodayOrder(today.map { if (it.widgetId == widgetId) it.copy(config = config) else it })
        } else {
            commitHomeOrder(_uiState.value.homeGridItems.map {
                if (it is HomeGridItem.WidgetItem && it.widgetId == widgetId) it.copy(config = config) else it
            })
        }
        val solvado = solvadoWidgets().any { it.widgetId == widgetId }
        if (refresh && solvado) refreshSolvado(force = true)
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
        val firstShop = _uiState.value.solvado.shops.firstOrNull()?.id.orEmpty()
        val config = when (kind) {
            com.tms.os.launcher.model.WidgetKind.SOLVADO_SHOP ->
                com.tms.os.launcher.model.SolvadoWidgetConfig(shopId = firstShop, metrics = com.tms.os.launcher.model.SolvadoMetric.shopDefault).encode()
            com.tms.os.launcher.model.WidgetKind.SOLVADO_ADMIN ->
                com.tms.os.launcher.model.SolvadoWidgetConfig(metrics = com.tms.os.launcher.model.SolvadoMetric.adminDefault).encode()
            else -> ""
        }
        val widget = HomeGridItem.WidgetItem("w_${UUID.randomUUID()}", kind, size, stack = stack, config = config)
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
