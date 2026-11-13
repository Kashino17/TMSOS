package com.tms.os.launcher.ui.theme

import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.WidgetKind

/** Texts for widgets, Today View and home-screen editing, worded like iOS. */
data class HomeStrings(
    val edit: String,
    val done: String,
    val addWidget: String,
    val customize: String,
    val searchWidgets: String,
    val cancel: String,
    val remove: String,
    val removeWidgetTitle: String,
    val removeWidgetMessage: String,
    val removeAppTitle: String,
    val removeAppMessage: String,
    val deleteApp: String,
    val removeFromHome: String,
    val removeFolderTitle: String,
    val removeFolderMessage: String,
    val noMoreEvents: String,
    val now: String,
    val hourFormat: String,
    val notesTitle: String,
    val notesEmpty: String,
    val search: String,
    val local: String,
    val today: String,
    val shareApp: String,
    val topHit: String,
    val open: String,
    val searchInApp: String,
    val searchWith: String,
    val shortcuts: String,
    val showMore: String,
    val showLess: String,
    val history: String,
    val clear: String,
    val searchInApps: String,
    val searchWeb: String,
    val searchMaps: String,
    val widgetName: (WidgetKind) -> String,
    val widgetDescription: (WidgetKind) -> String,
    val copy: String = "Kopieren",
    val copied: String = "Kopiert",
    val screenTime: String = "Bildschirmzeit",
    val allowAccess: String = "Tippen, um den Zugriff zu erlauben",
    val daysLeft: String = "Noch %d Tage",
    val week: String = "KW %d",
    val hoursMinutes: String = "%d h %d min",
    val minutesOnly: String = "%d min",
    val battery: String = "Akku",
    val minimal: String = "Minimalistisch",
    val minimalToggle: String = "Minimalistischer Home-Bildschirm",
    val minimalHint: String = "Nur Uhr und deine wichtigsten Apps. Dein normaler Home-Bildschirm bleibt gespeichert.",
    val template: String = "Vorlage",
    val templateList: String = "Liste",
    val templateIcons: String = "Symbole",
    val templateFocus: String = "Fokus",
    val yourApps: String = "Deine Apps",
    val searchApps: String = "Apps suchen",
    val selectedOf: String = "%1\$d von %2\$d",
    val minimalEmpty: String = "Gedrückt halten, um Apps auszuwählen"
)

private val DE = HomeStrings(
    edit = "Bearbeiten",
    done = "Fertig",
    addWidget = "Widget hinzufügen",
    customize = "Anpassen",
    searchWidgets = "Widgets durchsuchen",
    cancel = "Abbrechen",
    remove = "Entfernen",
    removeWidgetTitle = "„%s“-Widget entfernen?",
    removeWidgetMessage = "Durch das Entfernen dieses Widgets von deinem Home-Bildschirm werden keine Daten gelöscht.",
    removeAppTitle = "„%s“ entfernen?",
    removeAppMessage = "Wenn du diese App vom Home-Bildschirm entfernst, bleibt sie in der App-Mediathek.",
    deleteApp = "App löschen",
    removeFromHome = "Vom Home-Bildschirm entfernen",
    removeFolderTitle = "Ordner „%s“ entfernen?",
    removeFolderMessage = "Die Apps in diesem Ordner bleiben in der App-Mediathek.",
    noMoreEvents = "Keine weiteren Termine heute",
    now = "Jetzt",
    hourFormat = "H 'Uhr'",
    notesTitle = "Notizen",
    notesEmpty = "Keine Notiz",
    search = "Suchen",
    local = "Lokal",
    today = "Heute",
    shareApp = "App teilen",
    topHit = "Top-Treffer",
    open = "Öffnen",
    searchInApp = "In App suchen",
    searchWith = "Mit %s suchen",
    shortcuts = "Kurzbefehle",
    showMore = "Mehr anzeigen",
    showLess = "Weniger anzeigen",
    history = "Verlauf",
    clear = "Löschen",
    searchInApps = "In Apps suchen",
    searchWeb = "Im Web suchen",
    searchMaps = "Auf Karten suchen",
    widgetName = {
        when (it) {
            WidgetKind.WEATHER -> "Wetter"
            WidgetKind.CALENDAR -> "Kalender"
            WidgetKind.CLOCK -> "Uhr"
            WidgetKind.BATTERY -> "Batterien"
            WidgetKind.SUGGESTIONS -> "Siri-Vorschläge"
            WidgetKind.NOTES -> "Notizen"
            WidgetKind.DIGITAL -> "Digitaluhr"
            WidgetKind.DATE -> "Datum"
            WidgetKind.YEAR -> "Jahr"
            WidgetKind.SCREENTIME -> "Bildschirmzeit"
            WidgetKind.MINIMAL -> "Minimal"
        }
    },
    widgetDescription = {
        when (it) {
            WidgetKind.WEATHER -> "Sieh dir die aktuellen Wetterbedingungen und die Vorhersage für einen Ort an."
            WidgetKind.CALENDAR -> "Sieh dir das heutige Datum und den Monat auf einen Blick an."
            WidgetKind.CLOCK -> "Sieh dir die aktuelle Uhrzeit in verschiedenen Städten an."
            WidgetKind.BATTERY -> "Behalte den Ladezustand deiner Geräte im Blick."
            WidgetKind.SUGGESTIONS -> "Apps, die du wahrscheinlich als Nächstes verwendest."
            WidgetKind.NOTES -> "Behalte eine Notiz schnell im Blick."
            WidgetKind.DIGITAL -> "Die Uhrzeit groß in Ziffern, dazu das Datum."
            WidgetKind.DATE -> "Wochentag, Tag und Kalenderwoche auf einen Blick."
            WidgetKind.YEAR -> "Wie viel vom Jahr schon vorbei ist."
            WidgetKind.SCREENTIME -> "Wie lange du dein Gerät heute genutzt hast und womit."
            WidgetKind.MINIMAL -> "Nur Schrift, kein Hintergrund: Uhrzeit, Datum, Wetter und Akku."
        }
    }
)

private val EN = DE.copy(
    copy = "Copy", copied = "Copied", screenTime = "Screen Time", allowAccess = "Tap to allow access",
    daysLeft = "%d days left", week = "Week %d", hoursMinutes = "%d h %d min", minutesOnly = "%d min", battery = "Battery",
    minimal = "Minimal", minimalToggle = "Minimalist Home Screen",
    minimalHint = "Just the clock and your most important apps. Your regular Home Screen stays saved.",
    template = "Template", templateList = "List", templateIcons = "Icons", templateFocus = "Focus",
    yourApps = "Your Apps", searchApps = "Search Apps", selectedOf = "%1\$d of %2\$d", minimalEmpty = "Touch and hold to choose apps",
    edit = "Edit",
    done = "Done",
    addWidget = "Add Widget",
    customize = "Customize",
    searchWidgets = "Search Widgets",
    cancel = "Cancel",
    remove = "Remove",
    removeWidgetTitle = "Remove “%s” Widget?",
    removeWidgetMessage = "Removing this widget from your Home Screen will not delete any data.",
    removeAppTitle = "Remove “%s”?",
    removeAppMessage = "Removing from Home Screen will keep the app in your App Library.",
    deleteApp = "Delete App",
    removeFromHome = "Remove from Home Screen",
    removeFolderTitle = "Remove “%s” Folder?",
    removeFolderMessage = "The apps in this folder will stay in your App Library.",
    noMoreEvents = "No more events today",
    now = "Now",
    hourFormat = "h a",
    notesTitle = "Notes",
    notesEmpty = "No Note",
    search = "Search",
    local = "Local",
    today = "Today",
    shareApp = "Share App",
    topHit = "Top Hit",
    open = "Open",
    searchInApp = "Search in App",
    searchWith = "Search %s",
    shortcuts = "Shortcuts",
    showMore = "Show More",
    showLess = "Show Less",
    history = "Recent",
    clear = "Clear",
    searchInApps = "Search in Apps",
    searchWeb = "Search Web",
    searchMaps = "Search Maps",
    widgetName = {
        when (it) {
            WidgetKind.WEATHER -> "Weather"
            WidgetKind.CALENDAR -> "Calendar"
            WidgetKind.CLOCK -> "Clock"
            WidgetKind.BATTERY -> "Batteries"
            WidgetKind.SUGGESTIONS -> "Siri Suggestions"
            WidgetKind.NOTES -> "Notes"
            WidgetKind.DIGITAL -> "Digital Clock"
            WidgetKind.DATE -> "Date"
            WidgetKind.YEAR -> "Year"
            WidgetKind.SCREENTIME -> "Screen Time"
            WidgetKind.MINIMAL -> "Minimal"
        }
    },
    widgetDescription = {
        when (it) {
            WidgetKind.WEATHER -> "See the current conditions and forecast for a location."
            WidgetKind.CALENDAR -> "See today’s date and the month at a glance."
            WidgetKind.CLOCK -> "See the current time in different cities."
            WidgetKind.BATTERY -> "Keep track of the battery level of your devices."
            WidgetKind.SUGGESTIONS -> "Apps you are likely to use next."
            WidgetKind.NOTES -> "Keep a note at a glance."
            WidgetKind.DIGITAL -> "The time in large digits, with the date."
            WidgetKind.DATE -> "Weekday, day and week number at a glance."
            WidgetKind.YEAR -> "How much of the year has passed."
            WidgetKind.SCREENTIME -> "How long you used your device today, and for what."
            WidgetKind.MINIMAL -> "Text only, no background: time, date, weather and battery."
        }
    }
)

private val TR = DE.copy(
    copy = "Kopyala", copied = "Kopyalandı", screenTime = "Ekran Süresi", allowAccess = "Erişime izin vermek için dokunun",
    daysLeft = "%d gün kaldı", week = "%d. hafta", hoursMinutes = "%d sa %d dk", minutesOnly = "%d dk", battery = "Pil",
    minimal = "Minimal", minimalToggle = "Minimalist Ana Ekran",
    minimalHint = "Yalnızca saat ve en önemli uygulamalarınız. Normal ana ekranınız kayıtlı kalır.",
    template = "Şablon", templateList = "Liste", templateIcons = "Simgeler", templateFocus = "Odak",
    yourApps = "Uygulamalarınız", searchApps = "Uygulama ara", selectedOf = "%1\$d / %2\$d", minimalEmpty = "Uygulama seçmek için basılı tutun",
    edit = "Düzenle",
    done = "Bitti",
    addWidget = "Widget Ekle",
    customize = "Özelleştir",
    searchWidgets = "Widget’larda Ara",
    cancel = "Vazgeç",
    remove = "Sil",
    removeWidgetTitle = "“%s” Widget’ı Silinsin mi?",
    removeWidgetMessage = "Bu widget’ı Ana Ekran’dan silmek hiçbir veriyi silmez.",
    removeAppTitle = "“%s” Silinsin mi?",
    removeAppMessage = "Ana Ekran’dan silinen uygulama Uygulama Arşivi’nde kalır.",
    deleteApp = "Uygulamayı Sil",
    removeFromHome = "Ana Ekran’dan Sil",
    removeFolderTitle = "“%s” Klasörü Silinsin mi?",
    removeFolderMessage = "Bu klasördeki uygulamalar Uygulama Arşivi’nde kalır.",
    noMoreEvents = "Bugün başka etkinlik yok",
    now = "Şimdi",
    hourFormat = "HH",
    notesTitle = "Notlar",
    notesEmpty = "Not yok",
    search = "Ara",
    local = "Yerel",
    today = "Bugün",
    shareApp = "Uygulamayı Paylaş",
    topHit = "En İyi Sonuç",
    open = "Aç",
    searchInApp = "Uygulamada Ara",
    searchWith = "%s ile Ara",
    shortcuts = "Kestirmeler",
    showMore = "Daha Fazla Göster",
    showLess = "Daha Az Göster",
    history = "Geçmiş",
    clear = "Sil",
    searchInApps = "Uygulamalarda Ara",
    searchWeb = "Web’de Ara",
    searchMaps = "Harita’da Ara",
    widgetName = {
        when (it) {
            WidgetKind.WEATHER -> "Hava Durumu"
            WidgetKind.CALENDAR -> "Takvim"
            WidgetKind.CLOCK -> "Saat"
            WidgetKind.BATTERY -> "Piller"
            WidgetKind.SUGGESTIONS -> "Siri Önerileri"
            WidgetKind.NOTES -> "Notlar"
            WidgetKind.DIGITAL -> "Dijital Saat"
            WidgetKind.DATE -> "Tarih"
            WidgetKind.YEAR -> "Yıl"
            WidgetKind.SCREENTIME -> "Ekran Süresi"
            WidgetKind.MINIMAL -> "Minimal"
        }
    },
    widgetDescription = {
        when (it) {
            WidgetKind.WEATHER -> "Bir konumun güncel hava durumunu ve tahminini görün."
            WidgetKind.CALENDAR -> "Bugünün tarihini ve ayı bir bakışta görün."
            WidgetKind.CLOCK -> "Farklı şehirlerdeki saati görün."
            WidgetKind.BATTERY -> "Aygıtlarınızın pil düzeyini takip edin."
            WidgetKind.SUGGESTIONS -> "Büyük olasılıkla sonra kullanacağınız uygulamalar."
            WidgetKind.NOTES -> "Bir notu bir bakışta görün."
            WidgetKind.DIGITAL -> "Saat büyük rakamlarla, tarihle birlikte."
            WidgetKind.DATE -> "Gün, tarih ve hafta numarası bir bakışta."
            WidgetKind.YEAR -> "Yılın ne kadarı geçti."
            WidgetKind.SCREENTIME -> "Bugün aygıtınızı ne kadar ve neyle kullandığınız."
            WidgetKind.MINIMAL -> "Yalnızca yazı, arka plan yok: saat, tarih, hava ve pil."
        }
    }
)

fun homeStrings(language: AppLanguage): HomeStrings = when (language) {
    AppLanguage.DE -> DE
    AppLanguage.EN -> EN
    AppLanguage.TR -> TR
}
