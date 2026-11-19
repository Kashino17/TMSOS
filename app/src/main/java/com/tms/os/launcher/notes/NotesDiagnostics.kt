package com.tms.os.launcher.notes

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records hangs and crashes of the Notes app, so a problem on a phone without a
 * debugger can be looked at: when the main thread is blocked for more than
 * [HANG_MS] (the app "hangs"), or the app crashes, the main thread's stack is
 * written to `Download/TMS-Notizen-Diagnose.txt`. Nothing is written while
 * everything works.
 */
object NotesDiagnostics {
    private const val HANG_MS = 5_000L
    private const val FILE_NAME = "TMS-Notizen-Diagnose.txt"
    private const val MAX_BYTES = 256 * 1024
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val log = File(File(app.filesDir, "notes").apply { mkdirs() }, "diagnose.txt")

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { record(app, log, "ABSTURZ im Thread ${t.name}", e.stackTraceToString()) }
            previous?.uncaughtException(t, e)
        }

        val main = Handler(Looper.getMainLooper())
        Thread({
            var lastBeat = SystemClock.uptimeMillis()
            var reported = false
            while (true) {
                main.post { lastBeat = SystemClock.uptimeMillis() }
                try { Thread.sleep(1_000) } catch (_: InterruptedException) { return@Thread }
                val blocked = SystemClock.uptimeMillis() - lastBeat
                if (blocked > HANG_MS && !reported) {
                    reported = true
                    val stack = Looper.getMainLooper().thread.stackTrace.joinToString("\n") { "    at $it" }
                    runCatching { record(app, log, "HÄNGER: Hauptthread seit ${blocked / 1000} s blockiert", stack) }
                } else if (blocked < 2_000) reported = false
            }
        }, "notes-watchdog").apply { isDaemon = true; start() }
    }

    private fun record(context: Context, log: File, title: String, body: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY).format(Date())
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        val entry = "=== $time · TMS OS $version · ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n$title\n$body\n\n"
        val old = if (log.exists()) log.readText().takeLast(MAX_BYTES) else ""
        log.writeText(old + entry)
        exportToDownloads(context, log.readBytes())
    }

    /** Keeps one copy in the Downloads folder up to date (no permission needed on Android 10+). */
    private fun exportToDownloads(context: Context, bytes: ByteArray) {
        if (Build.VERSION.SDK_INT < 29) return
        val prefs = context.getSharedPreferences("notes_diag", Context.MODE_PRIVATE)
        val resolver = context.contentResolver
        val known = prefs.getString("uri", null)?.let(Uri::parse)
        val written = known != null && runCatching {
            resolver.openOutputStream(known, "wt")?.use { it.write(bytes) } != null
        }.getOrDefault(false)
        if (written) return
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }) ?: return
        resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
        prefs.edit().putString("uri", uri.toString()).commit()
    }
}
