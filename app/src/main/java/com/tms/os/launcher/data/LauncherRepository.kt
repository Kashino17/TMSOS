package com.tms.os.launcher.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.tms.os.launcher.domain.IconEngine
import com.tms.os.launcher.domain.SmartCategorizer
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.ContactItem
import com.tms.os.launcher.model.FoldPostureMode
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.IconThemeMode
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.model.LeftWidgetType
import com.tms.os.launcher.model.PageTransitionEffect
import com.tms.os.launcher.model.RightWidgetType
import com.tms.os.launcher.model.TintAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class SystemStorageRamInfo(
    val usedRamGb: Float,
    val totalRamGb: Float,
    val usedStorageGb: Int,
    val totalStorageGb: Int
)

class LauncherRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("tms_os_launcher_prefs", Context.MODE_PRIVATE)
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    fun clearIconCache() {
        iconCache.clear()
    }

    fun loadSettings(): LauncherSettings {
        return LauncherSettings(
            coverColumns = prefs.getInt("coverColumns", 4),
            coverRows = prefs.getInt("coverRows", 6),
            mainUnfoldedColumns = prefs.getInt("mainUnfoldedColumns", 6),
            mainUnfoldedRows = prefs.getInt("mainUnfoldedRows", 5),
            dockIconCount = prefs.getInt("dockIconCount", 4),
            foldMode = FoldPostureMode.fromId(prefs.getString("foldMode_v4", FoldPostureMode.IPAD_GRID.id) ?: ""),
            pageTransition = PageTransitionEffect.fromId(prefs.getString("pageTransition", PageTransitionEffect.IOS_SLIDE.id) ?: ""),
            iconSizeScale = prefs.getFloat("iconSizeScale", 1.0f),
            labelFontSizeSp = prefs.getFloat("labelFontSizeSp", 11.5f),
            iconThemeMode = IconThemeMode.fromId(prefs.getString("iconThemeMode", IconThemeMode.STANDARD.id) ?: ""),
            tintAccentColor = TintAccentColor.fromId(prefs.getString("tintAccentColor", TintAccentColor.IOS_BLUE.id) ?: ""),
            useIosSystemIcons = prefs.getBoolean("useIosSystemIcons", true),
            fullBleedAdaptiveIcons = prefs.getBoolean("fullBleedAdaptiveIcons", true),
            showAppLabels = prefs.getBoolean("showAppLabels", true),
            showDockLabels = prefs.getBoolean("showDockLabels", false),
            showRecentInUnfoldedDock = prefs.getBoolean("showRecentInUnfoldedDock", true),
            showTopWidgetsOnFirstPage = prefs.getBoolean("showTopWidgetsOnFirstPage_v4", false),
            leftWidgetType = LeftWidgetType.fromId(prefs.getString("leftWidgetType", LeftWidgetType.CALENDAR_CLOCK.id) ?: ""),
            rightWidgetType = RightWidgetType.fromId(prefs.getString("rightWidgetType", RightWidgetType.LIVE_WEATHER.id) ?: ""),
            quickNoteText = prefs.getString("quickNoteText", "Tippen für Schnell-Notiz…") ?: "Tippen für Schnell-Notiz…",
            customAndroidWidgetId = prefs.getInt("customAndroidWidgetId", -1),
            enableBadges = prefs.getBoolean("enableBadges", true),
            enableSwipeDownSpotlight = prefs.getBoolean("enableSwipeDownSpotlight", true),
            searchBarAtBottomInSpotlight = prefs.getBoolean("searchBarAtBottomInSpotlight_v4", false),
            autoOpenKeyboardInSpotlight = prefs.getBoolean("autoOpenKeyboardInSpotlight", true),
            showWebSearchInSpotlight = prefs.getBoolean("showWebSearchInSpotlight", true),
            instantLaunchSingleSearchResult = prefs.getBoolean("instantLaunchSingleSearchResult", false),
            autoSortNewAppsIntoCategories = prefs.getBoolean("autoSortNewAppsIntoCategories", true),
            weatherCity = prefs.getString("weatherCity", "Berlin") ?: "Berlin",
            weatherTempCelsius = prefs.getInt("weatherTempCelsius", 21),
            weatherHighCelsius = prefs.getInt("weatherHighCelsius", 24),
            weatherLowCelsius = prefs.getInt("weatherLowCelsius", 14),
            weatherCondition = prefs.getString("weatherCondition", "Heiter & Sonnig") ?: "Heiter & Sonnig",
            weatherCode = prefs.getInt("weatherCode", 1),
            weatherLastUpdated = prefs.getLong("weatherLastUpdated", 0L),
            useFahrenheit = prefs.getBoolean("useFahrenheit", false),
            glassOpacity = prefs.getFloat("glassOpacity", 0.78f),
            blurIntensity = prefs.getFloat("blurIntensity", 0.88f),
            appLanguage = AppLanguage.fromCode(prefs.getString("appLanguage", AppLanguage.DE.code) ?: "de")
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit().apply {
            putInt("coverColumns", settings.coverColumns)
            putInt("coverRows", settings.coverRows)
            putInt("mainUnfoldedColumns", settings.mainUnfoldedColumns)
            putInt("mainUnfoldedRows", settings.mainUnfoldedRows)
            putInt("dockIconCount", settings.dockIconCount)
            putString("foldMode_v4", settings.foldMode.id)
            putString("pageTransition", settings.pageTransition.id)
            putFloat("iconSizeScale", settings.iconSizeScale)
            putFloat("labelFontSizeSp", settings.labelFontSizeSp)
            putString("iconThemeMode", settings.iconThemeMode.id)
            putString("tintAccentColor", settings.tintAccentColor.id)
            putBoolean("useIosSystemIcons", settings.useIosSystemIcons)
            putBoolean("fullBleedAdaptiveIcons", settings.fullBleedAdaptiveIcons)
            putBoolean("showAppLabels", settings.showAppLabels)
            putBoolean("showDockLabels", settings.showDockLabels)
            putBoolean("showRecentInUnfoldedDock", settings.showRecentInUnfoldedDock)
            putBoolean("showTopWidgetsOnFirstPage_v4", settings.showTopWidgetsOnFirstPage)
            putString("leftWidgetType", settings.leftWidgetType.id)
            putString("rightWidgetType", settings.rightWidgetType.id)
            putString("quickNoteText", settings.quickNoteText)
            putInt("customAndroidWidgetId", settings.customAndroidWidgetId)
            putBoolean("enableBadges", settings.enableBadges)
            putBoolean("enableSwipeDownSpotlight", settings.enableSwipeDownSpotlight)
            putBoolean("searchBarAtBottomInSpotlight_v4", settings.searchBarAtBottomInSpotlight)
            putBoolean("autoOpenKeyboardInSpotlight", settings.autoOpenKeyboardInSpotlight)
            putBoolean("showWebSearchInSpotlight", settings.showWebSearchInSpotlight)
            putBoolean("instantLaunchSingleSearchResult", settings.instantLaunchSingleSearchResult)
            putBoolean("autoSortNewAppsIntoCategories", settings.autoSortNewAppsIntoCategories)
            putString("weatherCity", settings.weatherCity)
            putInt("weatherTempCelsius", settings.weatherTempCelsius)
            putInt("weatherHighCelsius", settings.weatherHighCelsius)
            putInt("weatherLowCelsius", settings.weatherLowCelsius)
            putString("weatherCondition", settings.weatherCondition)
            putInt("weatherCode", settings.weatherCode)
            putLong("weatherLastUpdated", settings.weatherLastUpdated)
            putBoolean("useFahrenheit", settings.useFahrenheit)
            putFloat("glassOpacity", settings.glassOpacity)
            putFloat("blurIntensity", settings.blurIntensity)
            putString("appLanguage", settings.appLanguage.code)
            apply()
        }
    }

    suspend fun loadInstalledApps(
        badgeMap: Map<String, Int>,
        settings: LauncherSettings
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val customCategories = getCustomCategoryOverrides()
        val customLabels = getCustomLabels()
        val hiddenSet = getHiddenPackages()
        val pinnedSet = getPinnedPageOnePackages()
        val launchCounts = getLaunchCounts()
        val lastLaunched = getLastLaunchedTimes()
        val usageMinutesMap = getUsageStatsMinutesMap()

        val result = mutableListOf<AppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (ri in resolveInfos) {
            val activityInfo = ri.activityInfo ?: continue
            val pkg = activityInfo.packageName ?: continue
            if (!seenPackages.add(pkg)) continue

            val rawLabel = try {
                ri.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
            } catch (_: Exception) {
                pkg
            }

            val appInfo = activityInfo.applicationInfo
            val isSystem = if (appInfo != null) {
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            } else false

            val firstInstall = try {
                pm.getPackageInfo(pkg, 0).firstInstallTime
            } catch (_: Exception) {
                0L
            }

            val overrideCat = customCategories[pkg]
            val category = SmartCategorizer.categorizeApp(pkg, rawLabel, appInfo, overrideCat)

            val cacheKey = "${pkg}_${settings.useIosSystemIcons}_${settings.fullBleedAdaptiveIcons}_${settings.iconThemeMode.id}_${settings.tintAccentColor.id}"
            val iconBitmap = iconCache.getOrPut(cacheKey) {
                try {
                    val drawable = ri.loadIcon(pm)
                    IconEngine.renderIosIconBitmap(
                        packageName = pkg,
                        label = rawLabel,
                        drawable = drawable,
                        sizePx = 192,
                        useIosSystemIcons = settings.useIosSystemIcons,
                        fullBleedAdaptive = settings.fullBleedAdaptiveIcons,
                        themeMode = settings.iconThemeMode,
                        tintAccent = settings.tintAccentColor
                    )
                } catch (_: Exception) {
                    createFallbackBitmap(192)
                }
            }

            result.add(
                AppInfo(
                    packageName = pkg,
                    activityName = activityInfo.name ?: "",
                    label = rawLabel,
                    customLabel = customLabels[pkg],
                    category = category,
                    isSystemApp = isSystem,
                    firstInstallTime = firstInstall,
                    launchCount = launchCounts[pkg] ?: 0,
                    usageDurationMinutes30d = usageMinutesMap[pkg] ?: 0,
                    lastLaunchedTime = lastLaunched[pkg] ?: 0L,
                    badgeCount = badgeMap[pkg] ?: 0,
                    isHidden = hiddenSet.contains(pkg),
                    isPinnedOnPageOne = pinnedSet.contains(pkg),
                    iconBitmap = iconBitmap
                )
            )
        }

        result.sortedBy { it.displayLabel.lowercase(Locale.ROOT) }
    }

    private fun createFallbackBitmap(sizePx: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        return bitmap.asImageBitmap()
    }

    /**
     * Reads real 30-day Android foreground usage stats (in minutes) if PACKAGE_USAGE_STATS is granted.
     */
    fun getUsageStatsMinutesMap(): Map<String, Int> {
        if (!isUsageStatsPermissionGranted()) return emptyMap()
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
            val end = System.currentTimeMillis()
            val start = end - (30L * 24L * 60L * 60L * 1000L)
            val stats = usm.queryAndAggregateUsageStats(start, end)
            val map = mutableMapOf<String, Int>()
            for ((pkg, u) in stats) {
                val mins = (u.totalTimeInForeground / 60000L).toInt()
                if (mins > 0) map[pkg] = mins
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun isUsageStatsPermissionGranted(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun loadContactsIfPermitted(): List<ContactItem> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }
        val list = mutableListOf<ContactItem>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            val seenNames = mutableSetOf<String>()
            while (cursor != null && cursor.moveToNext() && list.size < 200) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1)?.trim() ?: continue
                val number = cursor.getString(2)?.trim() ?: continue
                if (name.isNotBlank() && seenNames.add(name)) {
                    list.add(ContactItem(id, name, number))
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
        list
    }

    fun getDeviceBatteryPercent(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 85
        } catch (_: Exception) {
            85
        }
    }

    fun getStorageAndRamInfo(): SystemStorageRamInfo {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalRamGb = (memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()
            val usedRamGb = ((memInfo.totalMem - memInfo.availMem).toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()

            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockSizeLong * stat.blockCountLong
            val availBytes = stat.blockSizeLong * stat.availableBlocksLong
            val totalStorageGb = (totalBytes / (1024L * 1024L * 1024L)).toInt()
            val usedStorageGb = ((totalBytes - availBytes) / (1024L * 1024L * 1024L)).toInt()

            SystemStorageRamInfo(usedRamGb, totalRamGb, usedStorageGb, totalStorageGb)
        } catch (_: Exception) {
            SystemStorageRamInfo(6.4f, 12.0f, 128, 512)
        }
    }

    fun sendMediaKeyEvent(keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
        } catch (_: Exception) {
        }
    }

    fun expandSystemNotificationShade() {
        try {
            val service = context.getSystemService("statusbar") ?: return
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusbarManager.getMethod("expandNotificationsPanel")
            expand.isAccessible = true
            expand.invoke(service)
        } catch (_: Exception) {
        }
    }

    fun recordAppLaunch(packageName: String) {
        val counts = getLaunchCounts().toMutableMap()
        counts[packageName] = (counts[packageName] ?: 0) + 1
        saveLaunchCounts(counts)

        val times = getLastLaunchedTimes().toMutableMap()
        times[packageName] = System.currentTimeMillis()
        saveLastLaunchedTimes(times)
    }

    fun setAppPinnedOnPageOne(packageName: String, pinned: Boolean) {
        val set = getPinnedPageOnePackages().toMutableSet()
        if (pinned) set.add(packageName) else set.remove(packageName)
        prefs.edit().putStringSet("pinned_page_one_packages", set).apply()
    }

    fun getPinnedPageOnePackages(): Set<String> {
        return prefs.getStringSet("pinned_page_one_packages", emptySet()) ?: emptySet()
    }

    fun setAppCategoryOverride(packageName: String, category: AppCategory?) {
        val map = getCustomCategoryOverrides().toMutableMap()
        if (category == null) {
            map.remove(packageName)
        } else {
            map[packageName] = category
        }
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v.id) }
        prefs.edit().putString("custom_categories", json.toString()).apply()
    }

    fun setCustomAppLabel(packageName: String, customLabel: String?) {
        val map = getCustomLabels().toMutableMap()
        if (customLabel.isNullOrBlank()) {
            map.remove(packageName)
        } else {
            map[packageName] = customLabel.trim()
        }
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString("custom_labels", json.toString()).apply()
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        val set = getHiddenPackages().toMutableSet()
        if (hidden) set.add(packageName) else set.remove(packageName)
        prefs.edit().putStringSet("hidden_packages", set).apply()
    }

    fun getHiddenPackages(): Set<String> {
        return prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()
    }

    private fun getCustomCategoryOverrides(): Map<String, AppCategory> {
        val raw = prefs.getString("custom_categories", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, AppCategory>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = AppCategory.fromId(obj.getString(k))
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun getCustomLabels(): Map<String, String> {
        val raw = prefs.getString("custom_labels", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun getLaunchCounts(): Map<String, Int> {
        val raw = prefs.getString("launch_counts", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getInt(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveLaunchCounts(map: Map<String, Int>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("launch_counts", obj.toString()).apply()
    }

    private fun getLastLaunchedTimes(): Map<String, Long> {
        val raw = prefs.getString("last_launched_times", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getLong(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveLastLaunchedTimes(map: Map<String, Long>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("last_launched_times", obj.toString()).apply()
    }

    fun loadDockPackages(allApps: List<AppInfo>, maxDockSize: Int = 4): List<String> {
        val saved = prefs.getString("dock_packages_v4_json", null)
        if (saved != null) {
            try {
                val arr = JSONArray(saved)
                val list = mutableListOf<String>()
                val validSet = allApps.map { it.packageName }.toSet()
                for (i in 0 until arr.length()) {
                    val pkg = arr.getString(i)
                    if (validSet.contains(pkg)) list.add(pkg)
                }
                if (list.isNotEmpty()) return list.take(maxDockSize.coerceIn(3, 6))
            } catch (_: Exception) {
            }
        }

        // Exact Dock defaults matching Screenshot 3 & 4: Phone, Chrome/Browser, WhatsApp/Messages, Calculator!
        val defaults = mutableListOf<String>()
        findBestApp(allApps, listOf("dialer", "phone", "telefon", "contacts"))?.let { defaults.add(it.packageName) }
        findBestApp(allApps, listOf("chrome", "sbrowser", "browser", "brave", "firefox"))?.let {
            if (!defaults.contains(it.packageName)) defaults.add(it.packageName)
        }
        findBestApp(allApps, listOf("whatsapp", "messaging", "messages", "mms", "telegram"))?.let {
            if (!defaults.contains(it.packageName)) defaults.add(it.packageName)
        }
        findBestApp(allApps, listOf("popupcalculator", "calculator", "rechner", "camera", "kamera"))?.let {
            if (defaults.size < 4 && !defaults.contains(it.packageName)) defaults.add(it.packageName)
        }

        for (app in allApps) {
            if (defaults.size >= 4) break
            if (!defaults.contains(app.packageName) && !app.isHidden && app.packageName != context.packageName) {
                defaults.add(app.packageName)
            }
        }

        saveDockPackages(defaults)
        return defaults
    }

    fun saveDockPackages(packages: List<String>) {
        val arr = JSONArray()
        packages.forEach { arr.put(it) }
        prefs.edit().putString("dock_packages_v4_json", arr.toString()).apply()
    }

    fun loadHomeGridItems(allApps: List<AppInfo>, dockPackages: List<String>): List<HomeGridItem> {
        val raw = prefs.getString("home_grid_items_v4_json", null)
        val validPackages = allApps.filter { !it.isHidden }.map { it.packageName }.toSet()

        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                val items = mutableListOf<HomeGridItem>()
                val placedPackages = mutableSetOf<String>()
                placedPackages.addAll(dockPackages)

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    when (obj.optString("type")) {
                        "app" -> {
                            val pkg = obj.getString("packageName")
                            if (validPackages.contains(pkg) && !placedPackages.contains(pkg)) {
                                placedPackages.add(pkg)
                                items.add(
                                    HomeGridItem.AppItem(
                                        packageName = pkg,
                                        page = obj.optInt("page", 0),
                                        position = obj.optInt("position", items.size)
                                    )
                                )
                            }
                        }
                        "folder" -> {
                            val folderId = obj.optString("folderId", UUID.randomUUID().toString())
                            val title = obj.optString("title", "Ordner")
                            val pkgArr = obj.optJSONArray("packages") ?: JSONArray()
                            val folderPkgs = mutableListOf<String>()
                            for (j in 0 until pkgArr.length()) {
                                val p = pkgArr.getString(j)
                                if (validPackages.contains(p) && !placedPackages.contains(p)) {
                                    placedPackages.add(p)
                                    folderPkgs.add(p)
                                }
                            }
                            if (folderPkgs.isNotEmpty()) {
                                items.add(
                                    HomeGridItem.FolderItem(
                                        folderId = folderId,
                                        title = title,
                                        appPackages = folderPkgs,
                                        page = obj.optInt("page", 1),
                                        position = obj.optInt("position", items.size),
                                        categoryColorHex = obj.optLong("colorHex", 0xFF0A84FF)
                                    )
                                )
                            }
                        }
                    }
                }

                for (app in allApps) {
                    if (!app.isHidden && !placedPackages.contains(app.packageName)) {
                        items.add(
                            HomeGridItem.AppItem(
                                packageName = app.packageName,
                                page = items.size / 24,
                                position = items.size
                            )
                        )
                        placedPackages.add(app.packageName)
                    }
                }

                return items
            } catch (_: Exception) {
            }
        }

        return generateFlatSortedHomeGrid(allApps, dockPackages)
    }

    /**
     * Exact structure chosen by the user:
     * - Page 0 (Seite 1): Up to 16 single daily-driver apps (Smart-Pinned + highest UsageStats/Launch score + essentials). NO folders on Page 0!
     * - Page 1 (Seite 2): ALL remaining apps cleanly bundled into Smart Category Folders!
     */
    fun generateSmartOrganizedHomeGrid(
        allApps: List<AppInfo>,
        dockPackages: List<String>,
        page0SingleAppCount: Int = 16
    ): List<HomeGridItem> {
        val dockSet = dockPackages.toSet()
        val availableApps = allApps.filter { !it.isHidden && !dockSet.contains(it.packageName) }

        val items = mutableListOf<HomeGridItem>()
        val placed = mutableSetOf<String>()

        // 1. Place user's manually pinned Page-1 apps first
        val pinnedApps = availableApps.filter { it.isPinnedOnPageOne }
        for (app in pinnedApps) {
            if (items.size >= page0SingleAppCount) break
            items.add(HomeGridItem.AppItem(app.packageName, page = 0, position = items.size))
            placed.add(app.packageName)
        }

        // 2. Place apps with actual high usage (UsageStatsManager 30d + LaunchCount)
        val byUsage = availableApps
            .filter { !placed.contains(it.packageName) && it.effectivePriorityScore > 0 }
            .sortedByDescending { it.effectivePriorityScore }

        for (app in byUsage) {
            if (items.size >= page0SingleAppCount) break
            items.add(HomeGridItem.AppItem(app.packageName, page = 0, position = items.size))
            placed.add(app.packageName)
        }

        // 3. Fill remaining slots on Page 0 with curated daily essentials
        val priorityKeywords = listOf(
            "camera", "kamera", "apps.photos", "gallery", "galerie", "whatsapp",
            "maps", "gmail", "calendar", "kalender", "deskclock", "clock", "uhr",
            "com.android.settings", "einstellungen", "vending", "youtube", "spotify",
            "instagram", "telegram", "binance", "revolut", "n26", "fab"
        )
        for (kw in priorityKeywords) {
            if (items.size >= page0SingleAppCount) break
            val match = availableApps.firstOrNull {
                !placed.contains(it.packageName) &&
                    (it.packageName.lowercase(Locale.ROOT).contains(kw) ||
                        it.displayLabel.lowercase(Locale.ROOT).contains(kw))
            }
            if (match != null) {
                items.add(HomeGridItem.AppItem(match.packageName, page = 0, position = items.size))
                placed.add(match.packageName)
            }
        }

        // If still under page0SingleAppCount, top up with remaining apps
        for (app in availableApps) {
            if (items.size >= page0SingleAppCount) break
            if (!placed.contains(app.packageName)) {
                items.add(HomeGridItem.AppItem(app.packageName, page = 0, position = items.size))
                placed.add(app.packageName)
            }
        }

        // 4. Page 1 (Seite 2): Group ALL remaining apps into Smart iOS Category Folders!
        val remaining = availableApps.filter { !placed.contains(it.packageName) }
        val byCategory = remaining.groupBy { it.category }

        val orderedCategories = listOf(
            AppCategory.SOCIAL,
            AppCategory.FINANCE,
            AppCategory.PRODUCTIVITY,
            AppCategory.ENTERTAINMENT,
            AppCategory.SHOPPING_FOOD,
            AppCategory.TRAVEL_MAPS,
            AppCategory.INFO_READING,
            AppCategory.CREATIVITY,
            AppCategory.HEALTH_FITNESS,
            AppCategory.GAMES,
            AppCategory.UTILITIES
        )

        for (cat in orderedCategories) {
            val catApps = byCategory[cat]
                ?.sortedWith(
                    compareByDescending<AppInfo> { it.effectivePriorityScore }
                        .thenBy { it.displayLabel.lowercase(Locale.ROOT) }
                )
                .orEmpty()

            if (catApps.isNotEmpty()) {
                items.add(
                    HomeGridItem.FolderItem(
                        folderId = "smart_${cat.id}",
                        title = cat.titleDe,
                        appPackages = catApps.map { it.packageName },
                        page = 1,
                        position = items.size
                    )
                )
            }
        }

        saveHomeGridItems(items)
        return items
    }

    fun generateFlatSortedHomeGrid(
        allApps: List<AppInfo>,
        dockPackages: List<String>
    ): List<HomeGridItem> {
        val dockSet = dockPackages.toSet()
        val sortedApps = allApps
            .filter { !it.isHidden && !dockSet.contains(it.packageName) }
            .sortedWith(
                compareByDescending<AppInfo> { it.isPinnedOnPageOne }
                    .thenByDescending { it.effectivePriorityScore }
                    .thenBy { it.category.ordinal }
                    .thenBy { it.displayLabel.lowercase(Locale.ROOT) }
            )

        val items = sortedApps.mapIndexed { idx, app ->
            HomeGridItem.AppItem(
                packageName = app.packageName,
                page = idx / 24,
                position = idx
            )
        }
        saveHomeGridItems(items)
        return items
    }

    fun saveHomeGridItems(items: List<HomeGridItem>) {
        val arr = JSONArray()
        items.forEachIndexed { index, item ->
            val obj = JSONObject()
            when (item) {
                is HomeGridItem.AppItem -> {
                    obj.put("type", "app")
                    obj.put("packageName", item.packageName)
                    obj.put("page", item.page)
                    obj.put("position", index)
                }
                is HomeGridItem.FolderItem -> {
                    obj.put("type", "folder")
                    obj.put("folderId", item.folderId)
                    obj.put("title", item.title)
                    obj.put("page", item.page)
                    obj.put("position", index)
                    obj.put("colorHex", item.categoryColorHex)
                    val pkgArr = JSONArray()
                    item.appPackages.forEach { pkgArr.put(it) }
                    obj.put("packages", pkgArr)
                }
            }
            arr.put(obj)
        }
        prefs.edit().putString("home_grid_items_v4_json", arr.toString()).apply()
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 3)
        root.put("dock", prefs.getString("dock_packages_json", "[]"))
        root.put("grid", prefs.getString("home_grid_items_v3_json", "[]"))
        root.put("custom_categories", prefs.getString("custom_categories", "{}"))
        root.put("custom_labels", prefs.getString("custom_labels", "{}"))
        root.put("weatherCity", prefs.getString("weatherCity", "Berlin"))
        root.put("iconThemeMode", prefs.getString("iconThemeMode", "standard"))
        root.put("foldMode", prefs.getString("foldMode", "book_split"))
        return root.toString(2)
    }

    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            prefs.edit().apply {
                if (root.has("dock")) putString("dock_packages_json", root.getString("dock"))
                if (root.has("grid")) putString("home_grid_items_v3_json", root.getString("grid"))
                if (root.has("custom_categories")) putString("custom_categories", root.getString("custom_categories"))
                if (root.has("custom_labels")) putString("custom_labels", root.getString("custom_labels"))
                if (root.has("weatherCity")) putString("weatherCity", root.getString("weatherCity"))
                if (root.has("iconThemeMode")) putString("iconThemeMode", root.getString("iconThemeMode"))
                if (root.has("foldMode")) putString("foldMode", root.getString("foldMode"))
                apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findBestApp(apps: List<AppInfo>, keywords: List<String>): AppInfo? {
        for (kw in keywords) {
            val found = apps.firstOrNull {
                it.packageName.lowercase(Locale.ROOT).contains(kw) ||
                    it.displayLabel.lowercase(Locale.ROOT).contains(kw)
            }
            if (found != null) return found
        }
        return null
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            if (packageName == context.packageName) {
                val intent = Intent(context, Class.forName("com.tms.os.launcher.SettingsActivity")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                recordAppLaunch(packageName)
                return true
            }
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                recordAppLaunch(packageName)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun dialPhoneNumber(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun launchWebSearch(query: String, engine: String = "google") {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = when (engine) {
                "youtube" -> "https://www.youtube.com/results?search_query=$encoded"
                "play" -> "https://play.google.com/store/search?q=$encoded&c=apps"
                else -> "https://www.google.com/search?q=$encoded"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openWallpaperPicker() {
        try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Hintergrundbild wählen").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
        }
    }

    fun openAppSystemSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun requestUninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentHomePackage = resolveInfo?.activityInfo?.packageName
        return currentHomePackage == context.packageName
    }

    fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(context, "com.tms.os.launcher.service.BadgeNotificationService")
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}
