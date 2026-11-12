package com.tms.os.launcher.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

enum class AppLanguage(val code: String, val displayName: String) {
    DE("de", "Deutsch"),
    EN("en", "English"),
    TR("tr", "Türkçe");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: DE
    }
}

enum class AppCategory(
    val id: String,
    val titleDe: String,
    val titleEn: String,
    val titleTr: String,
    val subtitleDe: String,
    val accentColor: Color
) {
    SUGGESTIONS(
        "suggestions",
        "Vorschläge",
        "Suggestions",
        "Öneriler",
        "Intelligent & Häufig genutzt",
        Color(0xFF0A84FF)
    ),
    SOCIAL(
        "social",
        "Soziale Netze",
        "Social",
        "Sosyal",
        "Chat, Messenger & Community",
        Color(0xFF30D158)
    ),
    FINANCE(
        "finance",
        "Finanzen & Krypto",
        "Finance & Crypto",
        "Finans & Kripto",
        "Banken, Wallets & Bezahlen",
        Color(0xFFFF9F0A)
    ),
    PRODUCTIVITY(
        "productivity",
        "Produktivität & Büro",
        "Productivity",
        "Üretkenlik",
        "Mail, Kalender, Notizen & Arbeit",
        Color(0xFF0A84FF)
    ),
    ENTERTAINMENT(
        "entertainment",
        "Unterhaltung & Medien",
        "Entertainment",
        "Eğlence & Medya",
        "Video, Musik & Streaming",
        Color(0xFFFF375F)
    ),
    CREATIVITY(
        "creativity",
        "Kreativität & Foto",
        "Creativity & Photo",
        "Fotoğraf & Tasarım",
        "Kamera, Galerie & Design",
        Color(0xFFBF5AF2)
    ),
    SHOPPING_FOOD(
        "shopping",
        "Shopping & Essen",
        "Shopping & Food",
        "Alışveriş & Yemek",
        "Shops, Lieferdienste & Angebote",
        Color(0xFFFF9F0A)
    ),
    TRAVEL_MAPS(
        "travel",
        "Reisen & Navigation",
        "Travel & Maps",
        "Seyahat & Harita",
        "Karten, Bahn, Flüge & Mobilität",
        Color(0xFF64D2FF)
    ),
    INFO_READING(
        "reading",
        "KI, Browser & Wissen",
        "AI, Web & Info",
        "Yapay Zeka & Web",
        "KI-Assistenten, Browser & News",
        Color(0xFF5AC8FA)
    ),
    HEALTH_FITNESS(
        "health",
        "Gesundheit & Fitness",
        "Health & Fitness",
        "Sağlık & Fitness",
        "Sport, Medizin & Wohlbefinden",
        Color(0xFFFF2D55)
    ),
    GAMES(
        "games",
        "Spiele",
        "Games",
        "Oyunlar",
        "Gaming & Unterhaltung",
        Color(0xFF5E5CE6)
    ),
    UTILITIES(
        "utilities",
        "Dienstprogramme",
        "Utilities",
        "Araçlar & Sistem",
        "System, Sicherheit & Werkzeuge",
        Color(0xFF8E8E93)
    );

    fun localizedTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.DE -> titleDe
        AppLanguage.EN -> titleEn
        AppLanguage.TR -> titleTr
    }

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

enum class PageTransitionEffect(val id: String, val titleDe: String, val descDe: String) {
    IOS_SLIDE("ios_slide", "iOS 18 Spring Slide", "Authentischer, butterweicher Apple-Seitenübergang"),
    CUBE_3D("cube_3d", "3D Perspektiv-Würfel", "Dreht die Seiten räumlich wie einen 3D-Würfel"),
    DEPTH_STACK("depth_stack", "Tiefen-Stapel (Depth)", "Hintere Seite gleitet mit Tiefen-Skalierung hervor"),
    ZOOM_FADE("zoom_fade", "Sanfter Zoom & Blend", "Sanftes Ein- und Ausblenden mit Skalierung"),
    CAROUSEL_ROTATE("carousel", "Karussell-Schwung", "Neigt die Seiten beim Wischen leicht im Bogen");

    companion object {
        fun fromId(id: String): PageTransitionEffect = entries.find { it.id == id } ?: IOS_SLIDE
    }
}

enum class LeftWidgetType(val id: String, val titleDe: String) {
    CALENDAR_CLOCK("calendar_clock", "iOS Kalender & Live-Uhr"),
    BATTERY_STATUS("battery_status", "iOS Batterie & Geräte-Status"),
    SIRI_SUGGESTIONS("siri_suggestions", "Siri Top-4 App-Vorschläge");

    companion object {
        fun fromId(id: String): LeftWidgetType = entries.find { it.id == id } ?: CALENDAR_CLOCK
    }
}

data class LauncherSettings(
    val coverColumns: Int = 4,
    val coverRows: Int = 6,
    val mainUnfoldedColumns: Int = 6,
    val mainUnfoldedRows: Int = 5,
    val dockIconCount: Int = 4,
    val foldMode: FoldPostureMode = FoldPostureMode.IPAD_GRID,
    val pageTransition: PageTransitionEffect = PageTransitionEffect.IOS_SLIDE,
    val iconSizeScale: Float = 1.0f, // 0.80f .. 1.20f
    val labelFontSizeSp: Float = 11.5f,
    val useIosSystemIcons: Boolean = true,
    val fullBleedAdaptiveIcons: Boolean = true,
    val showAppLabels: Boolean = true,
    val showDockLabels: Boolean = false,
    val showRecentInUnfoldedDock: Boolean = true,
    val showTopWidgetsOnFirstPage: Boolean = true,
    val leftWidgetType: LeftWidgetType = LeftWidgetType.CALENDAR_CLOCK,
    val enableBadges: Boolean = true,
    val enableSwipeDownSpotlight: Boolean = true,
    val autoOpenKeyboardInSpotlight: Boolean = true,
    val showWebSearchInSpotlight: Boolean = true,
    val instantLaunchSingleSearchResult: Boolean = false,
    val autoSortNewAppsIntoCategories: Boolean = true,
    val weatherCity: String = "Berlin",
    val weatherTempCelsius: Int = 21,
    val weatherHighCelsius: Int = 24,
    val weatherLowCelsius: Int = 14,
    val weatherCondition: String = "Heiter & Sonnig",
    val weatherCode: Int = 1,
    val weatherLastUpdated: Long = 0L,
    val useFahrenheit: Boolean = false,
    val glassOpacity: Float = 0.78f,
    val blurIntensity: Float = 0.85f,
    val appLanguage: AppLanguage = AppLanguage.DE
)
