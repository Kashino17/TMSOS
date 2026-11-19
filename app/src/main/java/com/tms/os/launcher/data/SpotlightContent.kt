package com.tms.os.launcher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A settings page Spotlight can open: "WLAN", "Einstellungen für Gmail öffnen" … */
data class SettingsHit(
    val title: String,
    /** Breadcrumb under the title: "Apps → Gmail". Empty for top-level pages. */
    val path: List<String>,
    val action: String,
    /** For app pages: the package whose details page opens. */
    val packageName: String? = null
)

/** A document found on the device (not a photo: those have their own section). */
data class FileHit(val uri: Uri, val name: String, val meta: String, val mime: String)

/** A photo whose name or album matches. */
data class PhotoHit(val uri: Uri, val id: Long)

/**
 * The non-app content Spotlight searches: system settings pages, documents and
 * photos. Files and photos are read from MediaStore only when the user has
 * granted access; without it the sections simply stay empty.
 */
class SpotlightContent(private val context: Context) {

    private val settingsIndex: List<Pair<SettingsHit, List<String>>> = listOf(
        entry("WLAN", Settings.ACTION_WIFI_SETTINGS, "wifi", "wi-fi", "wlan", "internet", "netzwerk"),
        entry("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, "bluetooth", "kopfhörer", "airpods"),
        entry("Flugmodus", Settings.ACTION_AIRPLANE_MODE_SETTINGS, "flugmodus", "flugzeug", "airplane"),
        entry("Mobile Daten", Settings.ACTION_DATA_ROAMING_SETTINGS, "mobil", "daten", "roaming", "sim", "lte", "5g"),
        entry("Persönlicher Hotspot", Settings.ACTION_WIRELESS_SETTINGS, "hotspot", "tethering", "teilen"),
        entry("NFC", Settings.ACTION_NFC_SETTINGS, "nfc", "bezahlen", "kontaktlos"),
        entry("VPN", Settings.ACTION_VPN_SETTINGS, "vpn"),
        entry("Anzeige & Helligkeit", Settings.ACTION_DISPLAY_SETTINGS, "anzeige", "display", "helligkeit", "dunkel", "dark", "bildschirm", "schrift"),
        entry("Hintergrundbild", Intent.ACTION_SET_WALLPAPER, "hintergrund", "wallpaper"),
        entry("Töne & Haptik", Settings.ACTION_SOUND_SETTINGS, "töne", "ton", "klingelton", "lautstärke", "vibration", "haptik", "sound"),
        entry("Mitteilungen", "android.settings.NOTIFICATION_SETTINGS", "mitteilungen", "benachrichtigungen", "notification"),
        entry("Nicht stören", "android.settings.ZEN_MODE_SETTINGS", "nicht stören", "fokus", "ruhe", "dnd"),
        entry("Batterie", Intent.ACTION_POWER_USAGE_SUMMARY, "batterie", "akku", "battery", "energie", "laden"),
        entry("Speicher", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "speicher", "storage", "platz"),
        entry("Ortungsdienste", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "ortung", "standort", "gps", "location"),
        entry("Datenschutz & Sicherheit", Settings.ACTION_SECURITY_SETTINGS, "datenschutz", "sicherheit", "privacy", "passwort", "code", "fingerabdruck", "face"),
        entry("Accounts", Settings.ACTION_SYNC_SETTINGS, "accounts", "konten", "konto", "google", "sync", "e-mail", "mail"),
        entry("Apps", Settings.ACTION_APPLICATION_SETTINGS, "apps", "anwendungen"),
        entry("Standard-Apps", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, "standard", "default", "browser", "launcher"),
        entry("Sprache & Region", Settings.ACTION_LOCALE_SETTINGS, "sprache", "region", "language"),
        entry("Tastatur", Settings.ACTION_INPUT_METHOD_SETTINGS, "tastatur", "keyboard", "eingabe"),
        entry("Datum & Uhrzeit", Settings.ACTION_DATE_SETTINGS, "datum", "uhrzeit", "zeit", "zeitzone"),
        entry("Bedienungshilfen", Settings.ACTION_ACCESSIBILITY_SETTINGS, "bedienungshilfen", "barrierefreiheit", "accessibility"),
        entry("Bildschirmzeit", "android.settings.USAGE_ACCESS_SETTINGS", "bildschirmzeit", "wellbeing", "nutzung"),
        entry("Info", Settings.ACTION_DEVICE_INFO_SETTINGS, "info", "über", "telefon", "modell", "version"),
        entry("Softwareupdate", "android.settings.SYSTEM_UPDATE_SETTINGS", "update", "software", "aktualisierung"),
        entry("Entwickler", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "entwickler", "developer", "usb-debugging")
    )

    private fun entry(title: String, action: String, vararg keys: String) =
        SettingsHit(title, listOf("Einstellungen"), action) to keys.toList()

    /** Settings pages for [query]; [appLabels] adds "Einstellungen für X öffnen" for matching apps. */
    fun searchSettings(query: String, apps: List<Pair<String, String>>): List<SettingsHit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val pages = settingsIndex.filter { (hit, keys) ->
            hit.title.lowercase().startsWith(q) || hit.title.lowercase().contains(" $q") || keys.any { it.startsWith(q) }
        }.map { it.first }
        val appPages = apps.filter { (_, label) -> label.lowercase().startsWith(q) || label.lowercase().contains(" $q") }
            .take(2)
            .map { (pkg, label) -> SettingsHit("Einstellungen für $label öffnen", listOf("Apps", label), Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg) }
        return (appPages + pages).take(6)
    }

    fun open(hit: SettingsHit) {
        val intent = Intent(hit.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (hit.packageName != null) intent.data = Uri.fromParts("package", hit.packageName, null)
        runCatching { context.startActivity(intent) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    /* --------------------------------------------------------------- access */

    fun hasPhotoAccess(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        return context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
    }

    /** Documents (PDF, text …) need "all files" access from Android 11 on. */
    fun hasFileAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun openFileAccessSettings() {
        if (Build.VERSION.SDK_INT < 30) return
        val pkg = Uri.fromParts("package", context.packageName, null)
        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    /* ---------------------------------------------------------------- files */

    fun searchFiles(query: String, limit: Int): List<FileHit> {
        val q = query.trim()
        if (q.length < 2 || !hasFileAccess()) return emptyList()
        val uri = MediaStore.Files.getContentUri("external")
        val cols = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        // Photos and videos have their own section; folders have no MIME type.
        val sel = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} NOT IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) AND " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL"
        val out = mutableListOf<FileHit>()
        runCatching {
            context.contentResolver.query(uri, cols, sel, arrayOf("%$q%"), "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2) ?: "application/octet-stream"
                    val meta = listOf(kindOf(mime, name), sizeText(c.getLong(3)), dateText(c.getLong(4) * 1000)).joinToString(" · ")
                    out += FileHit(ContentUris.withAppendedId(uri, id), name, meta, mime)
                }
            }
        }
        return out
    }

    fun openFile(hit: FileHit) {
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(hit.uri, hit.mime)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(view) }.onFailure { openFilesApp() }
    }

    /** The phone's file manager ("Eigene Dateien" on Samsung, "Dateien" elsewhere). */
    fun openFilesApp() {
        val pm = context.packageManager
        val candidates = listOf("com.sec.android.app.myfiles", "com.google.android.apps.nbu.files", "com.google.android.documentsui", "com.android.documentsui")
        val launch = candidates.firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) }
            ?: Intent(Intent.ACTION_VIEW).setType("resource/folder")
        runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /* --------------------------------------------------------------- photos */

    fun searchPhotos(query: String, limit: Int): List<PhotoHit> {
        val q = query.trim()
        if (q.length < 2 || !hasPhotoAccess()) return emptyList()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sel = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ?"
        val out = mutableListOf<PhotoHit>()
        runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), sel, arrayOf("%$q%", "%$q%"), "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    val id = c.getLong(0)
                    out += PhotoHit(ContentUris.withAppendedId(uri, id), id)
                }
            }
        }
        return out
    }

    fun thumbnail(uri: Uri, px: Int): ImageBitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= 29) context.contentResolver.loadThumbnail(uri, Size(px, px), null).asImageBitmap()
        else null
    }.getOrNull()

    fun openPhoto(hit: PhotoHit) {
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(hit.uri, "image/*")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(view) }
    }

    /** The gallery app (Samsung Galerie, Google Fotos …). */
    fun openGallery() {
        val pm = context.packageManager
        val launch = listOf("com.sec.android.gallery3d", "com.google.android.apps.photos", "com.android.gallery3d")
            .firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) }
            ?: Intent(Intent.ACTION_VIEW).setType("image/*")
        runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /* -------------------------------------------------------------- helpers */

    private fun kindOf(mime: String, name: String): String = when {
        mime == "application/pdf" -> "PDF-Dokument"
        mime.startsWith("text/") -> "Text"
        mime.startsWith("audio/") -> "Audio"
        mime.contains("word") || mime.contains("opendocument.text") -> "Word-Dokument"
        mime.contains("sheet") || mime.contains("excel") -> "Tabelle"
        mime.contains("presentation") || mime.contains("powerpoint") -> "Präsentation"
        mime.contains("zip") || mime.contains("compressed") -> "Archiv"
        mime == "application/vnd.android.package-archive" -> "App-Paket"
        else -> name.substringAfterLast('.', "").uppercase().takeIf { it.isNotEmpty() && it.length <= 5 }?.let { "$it-Datei" } ?: "Datei"
    }

    private fun sizeText(bytes: Long): String = when {
        bytes < 1000 -> "$bytes Byte"
        bytes < 1_000_000 -> "${(bytes / 1000.0).let { if (it < 10) String.format(Locale.GERMANY, "%.1f", it) else it.toInt().toString() }} KB"
        bytes < 1_000_000_000 -> "${String.format(Locale.GERMANY, "%.1f", bytes / 1_000_000.0)} MB"
        else -> "${String.format(Locale.GERMANY, "%.1f", bytes / 1_000_000_000.0)} GB"
    }

    private fun dateText(ms: Long): String = SimpleDateFormat("dd.MM.yy, HH:mm", Locale.GERMANY).format(Date(ms))
}
