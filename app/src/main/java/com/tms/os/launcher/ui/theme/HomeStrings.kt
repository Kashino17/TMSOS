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
    val widgetName: (WidgetKind) -> String,
    val widgetDescription: (WidgetKind) -> String
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
    widgetName = {
        when (it) {
            WidgetKind.WEATHER -> "Wetter"
            WidgetKind.CALENDAR -> "Kalender"
            WidgetKind.CLOCK -> "Uhr"
            WidgetKind.BATTERY -> "Batterien"
            WidgetKind.SUGGESTIONS -> "Siri-Vorschläge"
            WidgetKind.NOTES -> "Notizen"
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
        }
    }
)

private val EN = DE.copy(
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
    widgetName = {
        when (it) {
            WidgetKind.WEATHER -> "Weather"
            WidgetKind.CALENDAR -> "Calendar"
            WidgetKind.CLOCK -> "Clock"
            WidgetKind.BATTERY -> "Batteries"
            WidgetKind.SUGGESTIONS -> "Siri Suggestions"
            WidgetKind.NOTES -> "Notes"
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
        }
    }
)

private val TR = DE.copy(
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
    widgetName = {
        when (it) {
            WidgetKind.WEATHER -> "Hava Durumu"
            WidgetKind.CALENDAR -> "Takvim"
            WidgetKind.CLOCK -> "Saat"
            WidgetKind.BATTERY -> "Piller"
            WidgetKind.SUGGESTIONS -> "Siri Önerileri"
            WidgetKind.NOTES -> "Notlar"
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
        }
    }
)

fun homeStrings(language: AppLanguage): HomeStrings = when (language) {
    AppLanguage.DE -> DE
    AppLanguage.EN -> EN
    AppLanguage.TR -> TR
}
