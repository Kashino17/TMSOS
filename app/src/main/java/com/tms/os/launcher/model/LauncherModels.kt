package com.tms.os.launcher.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

enum class AppCategory(
    val id: String,
    val titleDe: String,
    val subtitleDe: String,
    val accentColor: Color
) {
    SUGGESTIONS("suggestions", "Vorschläge", "Intelligent & Häufig genutzt", Color(0xFF0A84FF)),
    SOCIAL("social", "Soziale Netze", "Chat, Messenger & Community", Color(0xFF30D158)),
    PRODUCTIVITY("productivity", "Produktivität & Büro", "Mail, Kalender, Notizen & Arbeit", Color(0xFF0A84FF)),
    FINANCE("finance", "Finanzen & Banking", "Banken, Krypto & Bezahlen", Color(0xFFFF9F0A)),
    ENTERTAINMENT("entertainment", "Unterhaltung & Medien", "Video, Musik & Streaming", Color(0xFFFF375F)),
    CREATIVITY("creativity", "Kreativität & Foto", "Kamera, Galerie & Design", Color(0xFFBF5AF2)),
    SHOPPING_FOOD("shopping", "Shopping & Essen", "Shops, Lieferdienste & Angebote", Color(0xFFFF9F0A)),
    TRAVEL_MAPS("travel", "Reisen & Navigation", "Karten, Bahn, Flüge & Mobilität", Color(0xFF64D2FF)),
    GAMES("games", "Spiele", "Gaming & Unterhaltung", Color(0xFF5E5CE6)),
    HEALTH_FITNESS("health", "Gesundheit & Fitness", "Sport, Medizin & Wohlbefinden", Color(0xFFFF2D55)),
    INFO_READING("reading", "Wissen & Nachrichten", "News, Bücher, Browser & KI", Color(0xFF5AC8FA)),
    UTILITIES("utilities", "Dienstprogramme", "System, Werkzeuge & Einstellungen", Color(0xFF8E8E93));

    companion object {
        fun fromId(id: String): AppCategory {
            return entries.find { it.id == id } ?: UTILITIES
        }
    }
}

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val customLabel: String? = null,
    val category: AppCategory,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val launchCount: Int = 0,
    val lastLaunchedTime: Long = 0L,
    val badgeCount: Int = 0,
    val isHidden: Boolean = false,
    val iconBitmap: ImageBitmap? = null
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

sealed class HomeGridItem(val id: String) {
    data class AppItem(
        val packageName: String,
        val page: Int = 0,
        val position: Int = 0
    ) : HomeGridItem("app:$packageName")

    data class FolderItem(
        val folderId: String,
        val title: String,
        val appPackages: List<String>,
        val page: Int = 0,
        val position: Int = 0,
        val categoryColorHex: Long = 0xFF0A84FF
    ) : HomeGridItem("folder:$folderId")
}

enum class FoldPostureMode(val id: String, val titleDe: String, val descDe: String) {
    IPAD_GRID("ipad_grid", "iPadOS Breitbild-Raster", "Großzügiges 6x5 Raster mit erweitertem Dock & Zuletzt-Genutzt-Leiste"),
    BOOK_SPLIT("book_split", "Dual-Page Buch-Modus", "Zeigt zwei iPhone-Seiten nebeneinander wie ein aufgeschlagenes Buch"),
    CLASSIC_SCALED("classic_scaled", "Klassisch (Skaliert)", "Behält das gleiche Seiten-Layout wie auf dem Frontdisplay bei");

    companion object {
        fun fromId(id: String): FoldPostureMode = entries.find { it.id == id } ?: IPAD_GRID
    }
}

enum class PageTransitionEffect(val id: String, val titleDe: String) {
    IOS_SLIDE("ios_slide", "Klassisch (iOS Slide)"),
    CUBE_3D("cube_3d", "3D Würfel"),
    DEPTH_STACK("depth_stack", "Tiefen-Stapel"),
    ZOOM_FADE("zoom_fade", "Sanfter Zoom");

    companion object {
        fun fromId(id: String): PageTransitionEffect = entries.find { it.id == id } ?: IOS_SLIDE
    }
}

data class LauncherSettings(
    val coverColumns: Int = 4,
    val coverRows: Int = 6,
    val mainUnfoldedColumns: Int = 6,
    val mainUnfoldedRows: Int = 5,
    val foldMode: FoldPostureMode = FoldPostureMode.IPAD_GRID,
    val pageTransition: PageTransitionEffect = PageTransitionEffect.IOS_SLIDE,
    val iconSizeScale: Float = 1.0f, // 0.85f .. 1.15f
    val showAppLabels: Boolean = true,
    val showDockLabels: Boolean = false,
    val showRecentInUnfoldedDock: Boolean = true,
    val showTopWidgetsOnFirstPage: Boolean = true,
    val enableBadges: Boolean = true,
    val instantLaunchSingleSearchResult: Boolean = false,
    val autoSortNewAppsIntoCategories: Boolean = true,
    val weatherCity: String = "Berlin",
    val weatherTempCelsius: Int = 21,
    val weatherCondition: String = "Heiter & Sonnig",
    val blurIntensity: Float = 0.85f
)
