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

enum class IconThemeMode(val id: String, val titleDe: String, val descDe: String) {
    STANDARD("standard", "Standard (Farbig)", "Originale App-Farben im randlosen Apple-Squircle"),
    IOS_DARK("ios_dark", "iOS Dunkel (Dark Mode)", "Dunkelt helle Icon-Hintergründe automatisch auf edles iOS-Anthrazit ab"),
    IOS_TINTED("ios_tinted", "iOS Getönt (Tinted)", "Edler monochromer Apple-Farbverlauf mit deiner Wunsch-Akzentfarbe");

    companion object {
        fun fromId(id: String): IconThemeMode = entries.find { it.id == id } ?: STANDARD
    }
}

enum class TintAccentColor(val id: String, val titleDe: String, val colorHex: Long) {
    IOS_BLUE("blue", "Apple Blau", 0xFF0A84FF),
    EMERALD("emerald", "Smaragdgrün", 0xFF30D158),
    ROYAL_PURPLE("purple", "Violett", 0xFFBF5AF2),
    DESERT_GOLD("gold", "Wüstengold", 0xFFFF9F0A),
    CRIMSON_RED("red", "Karmesinrot", 0xFFFF375F),
    TITANIUM("titanium", "Titan Silber", 0xFF98989F);

    companion object {
        fun fromId(id: String): TintAccentColor = entries.find { it.id == id } ?: IOS_BLUE
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
    val usageDurationMinutes30d: Int = 0,
    val lastLaunchedTime: Long = 0L,
    val badgeCount: Int = 0,
    val isHidden: Boolean = false,
    val isPinnedOnPageOne: Boolean = false,
    val iconBitmap: ImageBitmap? = null
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label

    val effectivePriorityScore: Int
        get() = (launchCount * 20) + usageDurationMinutes30d
}

/** A real app shortcut published through LauncherApps (manifest or dynamic). */
data class AppQuickAction(
    val packageName: String,
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap?
)

data class AppShortcutItem(
    val packageName: String,
    val shortcutId: String,
    val shortLabel: String,
    val appLabel: String
)

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String
)

sealed class HomeGridItem(val id: String) {
    abstract val page: Int

    data class AppItem(
        val packageName: String,
        override val page: Int = 0,
        val position: Int = 0
    ) : HomeGridItem("app:$packageName")

    data class FolderItem(
        val folderId: String,
        val title: String,
        val appPackages: List<String>,
        override val page: Int = 1,
        val position: Int = 0,
        val categoryColorHex: Long = 0xFF0A84FF
    ) : HomeGridItem("folder:$folderId")

    /** A WidgetKit-style widget that occupies [size] grid cells, like on iOS. */
    data class WidgetItem(
        val widgetId: String,
        val kind: WidgetKind,
        val size: WidgetSize,
        override val page: Int = 0,
        /** Non-empty = Smart Stack: several widgets of the same size, swiped vertically. */
        val stack: List<WidgetKind> = emptyList()
    ) : HomeGridItem("widget:$widgetId") {
        val kinds: List<WidgetKind> get() = stack.ifEmpty { listOf(kind) }
        val isStack: Boolean get() = stack.size > 1
    }
}

/** iPhone widget families, measured in home-screen icon cells. */
enum class WidgetSize(val id: String, val cols: Int, val rows: Int) {
    SMALL("small", 2, 2),
    MEDIUM("medium", 4, 2),
    LARGE("large", 4, 4);

    companion object {
        fun fromId(id: String): WidgetSize = entries.find { it.id == id } ?: SMALL
    }
}

enum class WidgetKind(val id: String, val sizes: List<WidgetSize>) {
    WEATHER("weather", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM, WidgetSize.LARGE)),
    CALENDAR("calendar", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM, WidgetSize.LARGE)),
    CLOCK("clock", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    BATTERY("battery", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    SUGGESTIONS("suggestions", listOf(WidgetSize.MEDIUM, WidgetSize.LARGE)),
    NOTES("notes", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    DIGITAL("digital", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    DATE("date", listOf(WidgetSize.SMALL)),
    YEAR("year", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    SCREENTIME("screentime", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    /** Text only, no plate: time, date and a line of weather and battery. */
    MINIMAL("minimal", listOf(WidgetSize.SMALL, WidgetSize.MEDIUM, WidgetSize.LARGE));

    companion object {
        fun fromId(id: String): WidgetKind? = entries.find { it.id == id }
    }
}

enum class FoldPostureMode(val id: String, val titleDe: String, val descDe: String) {
    BOOK_SPLIT(
        "book_split",
        "Smart Dual-Page Dashboard (Empfohlen)",
        "Links Seite 1 (Smart-Stack Widgets + Top-Apps) & Rechts Seite 2 (Alle Smart-Kategorie-Ordner) gleichzeitig!"
    ),
    IPAD_GRID(
        "ipad_grid",
        "iPadOS Breitbild-Raster",
        "Großzügiges 6x5 Raster über die gesamte Breite mit erweitertem iPadOS-Dock"
    ),
    CLASSIC_SCALED(
        "classic_scaled",
        "Klassisch (Skaliert)",
        "Behält das gleiche Seiten-Layout wie auf dem Frontdisplay bei"
    );

    companion object {
        fun fromId(id: String): FoldPostureMode = entries.find { it.id == id } ?: BOOK_SPLIT
    }
}

enum class PageTransitionEffect(val id: String, val titleDe: String, val descDe: String) {
    IOS_SLIDE("ios_slide", "iOS Spring Slide", "Authentischer, butterweicher Apple-Seitenübergang"),
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
    SIRI_SUGGESTIONS("siri_suggestions", "Siri Top-4 App-Vorschläge"),
    QUICK_NOTE("quick_note", "iOS Schnell-Notiz & Aufgaben");

    companion object {
        fun fromId(id: String): LeftWidgetType = entries.find { it.id == id } ?: CALENDAR_CLOCK
    }
}

enum class RightWidgetType(val id: String, val titleDe: String) {
    LIVE_WEATHER("live_weather", "iOS Live-Wetter (Open-Meteo)"),
    MEDIA_CONTROLLER("media_controller", "iOS Musik- & Medien-Steuerung"),
    STORAGE_RAM("storage_ram", "System- & Speicher-Monitor");

    companion object {
        fun fromId(id: String): RightWidgetType = entries.find { it.id == id } ?: LIVE_WEATHER
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
    val iconSizeScale: Float = 1.0f,
    val labelFontSizeSp: Float = 12f,
    val iconThemeMode: IconThemeMode = IconThemeMode.STANDARD,
    val tintAccentColor: TintAccentColor = TintAccentColor.IOS_BLUE,
    val useIosSystemIcons: Boolean = true,
    val fullBleedAdaptiveIcons: Boolean = true,
    val showAppLabels: Boolean = true,
    val showDockLabels: Boolean = false,
    val showRecentInUnfoldedDock: Boolean = true,
    val showTopWidgetsOnFirstPage: Boolean = false,
    val useSystemWallpaper: Boolean = false,
    val leftWidgetType: LeftWidgetType = LeftWidgetType.CALENDAR_CLOCK,
    val rightWidgetType: RightWidgetType = RightWidgetType.LIVE_WEATHER,
    val quickNoteText: String = "Tippen für Schnell-Notiz…",
    val customAndroidWidgetId: Int = -1,
    val enableBadges: Boolean = true,
    val enableSwipeDownSpotlight: Boolean = true,
    val searchBarAtBottomInSpotlight: Boolean = false,
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
    val blurIntensity: Float = 0.88f,
    val appLanguage: AppLanguage = AppLanguage.DE,
    /** Minimalist home screen: one calm page built from [minimalApps] instead of the icon grid. */
    val minimalMode: Boolean = false,
    val minimalTemplate: MinimalTemplate = MinimalTemplate.LIST,
    val minimalApps: List<String> = emptyList()
)


/** Layouts of the minimalist home screen. */
enum class MinimalTemplate(val id: String, val maxApps: Int) {
    /** Clock on top, the apps as a plain list of names. */
    LIST("list", 10),
    /** Clock on top, a few rows of icons without labels at the bottom. */
    ICONS("icons", 12),
    /** Only the clock; the apps sit in the dock. */
    FOCUS("focus", 4);

    companion object {
        fun fromId(id: String?): MinimalTemplate = entries.find { it.id == id } ?: LIST
    }
}

/** Today's screen time; [granted] is false until usage access is allowed. */
data class ScreenTime(val granted: Boolean = false, val totalMinutes: Int = 0, val top: List<Pair<String, Int>> = emptyList())
