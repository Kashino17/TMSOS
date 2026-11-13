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
    val templatesPlain: String = "Schlicht",
    val templatesIos: String = "Im iOS-Stil",
    val templateIosWidgets: String = "Widgets",
    val templateIosLarge: String = "Groß",
    val templateIosBottom: String = "Unten",
    val yourApps: String = "Deine Apps",
    val searchApps: String = "Apps suchen",
    val selectedOf: String = "%1\$d von %2\$d",
    val minimalEmpty: String = "Tippen, um Apps auszuwählen",
    val solvadoConnect: String = "Mit Solvado verbinden",
    val solvadoConnectHint: String = "Melde dich mit deinem TMS-Solvado-Konto an. Das Widget liest nur Zahlen, es kann nichts ändern.",
    val solvadoTapToConnect: String = "Tippen, um Solvado zu verbinden",
    val solvadoTapToChoose: String = "Tippen, um einen Shop zu wählen",
    val email: String = "E-Mail",
    val password: String = "Passwort",
    val signIn: String = "Anmelden",
    val signOut: String = "Abmelden",
    val server: String = "Server",
    val shop: String = "Shop",
    val period: String = "Zeitraum",
    val last7: String = "7 Tage",
    val last30: String = "30 Tage",
    val figures: String = "Werte",
    val figuresHint: String = "Klein zeigt die ersten 2, Mittel 4, Groß 8.",
    val editWidget: String = "Widget bearbeiten",
    val updatedAt: String = "Stand %s",
    val incomplete: String = "* unvollständig gemessen",
    val noShops: String = "In diesem Konto gibt es noch keinen Shop."
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
            WidgetKind.SOLVADO_SHOP -> "Solvado Shop"
            WidgetKind.SOLVADO_ADMIN -> "Solvado Admin"
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
            WidgetKind.SOLVADO_SHOP -> "Umsatz, Gewinn, Conversion und Kosten eines deiner Shops – du wählst, welche Werte."
            WidgetKind.SOLVADO_ADMIN -> "Abonnenten, Credit-Umsatz und neue Accounts der Plattform."
        }
    }
)

private val EN = DE.copy(
    solvadoConnect = "Connect Solvado", solvadoConnectHint = "Sign in with your TMS Solvado account. The widget only reads figures, it cannot change anything.",
    solvadoTapToConnect = "Tap to connect Solvado", solvadoTapToChoose = "Tap to choose a shop",
    email = "Email", password = "Password", signIn = "Sign In", signOut = "Sign Out", server = "Server", shop = "Shop",
    period = "Period", last7 = "7 days", last30 = "30 days", figures = "Figures", figuresHint = "Small shows the first 2, medium 4, large 8.",
    editWidget = "Edit Widget", updatedAt = "As of %s", incomplete = "* not fully measured", noShops = "This account has no shop yet.",
    copy = "Copy", copied = "Copied", screenTime = "Screen Time", allowAccess = "Tap to allow access",
    daysLeft = "%d days left", week = "Week %d", hoursMinutes = "%d h %d min", minutesOnly = "%d min", battery = "Battery",
    minimal = "Minimal", minimalToggle = "Minimalist Home Screen",
    minimalHint = "Just the clock and your most important apps. Your regular Home Screen stays saved.",
    template = "Template", templateList = "List", templateIcons = "Icons", templateFocus = "Focus", templatesPlain = "Plain", templatesIos = "iOS Style", templateIosWidgets = "Widgets", templateIosLarge = "Large", templateIosBottom = "Bottom",
    yourApps = "Your Apps", searchApps = "Search Apps", selectedOf = "%1\$d of %2\$d", minimalEmpty = "Tap to choose apps",
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
            WidgetKind.SOLVADO_SHOP -> "Solvado Shop"
            WidgetKind.SOLVADO_ADMIN -> "Solvado Admin"
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
            WidgetKind.SOLVADO_SHOP -> "Revenue, profit, conversion and costs of one of your shops – you choose the figures."
            WidgetKind.SOLVADO_ADMIN -> "Subscribers, credit revenue and new accounts of the platform."
        }
    }
)

private val TR = DE.copy(
    solvadoConnect = "Solvado'ya bağlan", solvadoConnectHint = "TMS Solvado hesabınızla giriş yapın. Araç yalnızca rakamları okur, hiçbir şeyi değiştiremez.",
    solvadoTapToConnect = "Solvado'ya bağlanmak için dokunun", solvadoTapToChoose = "Mağaza seçmek için dokunun",
    email = "E-posta", password = "Parola", signIn = "Giriş Yap", signOut = "Çıkış Yap", server = "Sunucu", shop = "Mağaza",
    period = "Dönem", last7 = "7 gün", last30 = "30 gün", figures = "Değerler", figuresHint = "Küçük ilk 2, orta 4, büyük 8 değeri gösterir.",
    editWidget = "Aracı Düzenle", updatedAt = "%s itibarıyla", incomplete = "* eksik ölçüldü", noShops = "Bu hesapta henüz mağaza yok.",
    copy = "Kopyala", copied = "Kopyalandı", screenTime = "Ekran Süresi", allowAccess = "Erişime izin vermek için dokunun",
    daysLeft = "%d gün kaldı", week = "%d. hafta", hoursMinutes = "%d sa %d dk", minutesOnly = "%d dk", battery = "Pil",
    minimal = "Minimal", minimalToggle = "Minimalist Ana Ekran",
    minimalHint = "Yalnızca saat ve en önemli uygulamalarınız. Normal ana ekranınız kayıtlı kalır.",
    template = "Şablon", templateList = "Liste", templateIcons = "Simgeler", templateFocus = "Odak", templatesPlain = "Sade", templatesIos = "iOS Tarzı", templateIosWidgets = "Araçlar", templateIosLarge = "Büyük", templateIosBottom = "Alt",
    yourApps = "Uygulamalarınız", searchApps = "Uygulama ara", selectedOf = "%1\$d / %2\$d", minimalEmpty = "Uygulama seçmek için dokunun",
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
            WidgetKind.SOLVADO_SHOP -> "Solvado Mağaza"
            WidgetKind.SOLVADO_ADMIN -> "Solvado Yönetici"
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
            WidgetKind.SOLVADO_SHOP -> "Mağazalarınızdan birinin cirosu, kârı, dönüşümü ve giderleri – değerleri siz seçersiniz."
            WidgetKind.SOLVADO_ADMIN -> "Platformun aboneleri, kredi cirosu ve yeni hesapları."
        }
    }
)

fun homeStrings(language: AppLanguage): HomeStrings = when (language) {
    AppLanguage.DE -> DE
    AppLanguage.EN -> EN
    AppLanguage.TR -> TR
}
