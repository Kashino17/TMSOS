package com.tms.os.launcher.notes

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.MotionEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Flight recorder for the Notes app, to find problems that only happen on the
 * phone (no debugger there). It keeps the last few minutes of:
 *  - every touch (down/up/cancel), how long it waited before the app saw it
 *    ("lag": the main thread was busy) and how long the app took to handle it;
 *  - every piece of main-thread work longer than [SLOW_MS], and for work longer
 *    than [SAMPLE_MS] the code that was running at that moment (stack sample);
 *  - screen changes, window focus (lost focus = touches go to another window),
 *    pauses/resumes, crashes.
 * It is written to `Download/TMS-Notizen-Diagnose.txt` every few seconds while
 * the app is open. Recording costs nothing noticeable (a timestamp per message).
 */
object NotesDiagnostics {
    private const val SLOW_MS = 48L
    private const val SAMPLE_MS = 120L
    private const val MAX_LINES = 5000
    private const val FILE_NAME = "TMS-Notizen-Diagnose.txt"

    @Volatile private var installed = false
    private lateinit var app: Context
    private val lines = ArrayDeque<String>()
    @Volatile private var dirty = false
    private val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.GERMANY)

    // The main-thread message currently running (set by the looper printer).
    @Volatile private var msgStart = 0L
    @Volatile private var msgText: String? = null
    @Volatile private var msgSample: String? = null

    fun log(text: String) {
        val line = "${clock.format(Date())}  $text"
        synchronized(lines) {
            lines.addLast(line)
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
        dirty = true
    }

    fun install(context: Context) {
        if (installed) return
        installed = true
        app = context.applicationContext
        log("=== Notizen gestartet · ${header()}")

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { log("ABSTURZ im Thread ${t.name}\n${e.stackTraceToString()}"); writeOut() }
            previous?.uncaughtException(t, e)
        }

        // Duration of every main-thread message; the slow ones are recorded.
        Looper.getMainLooper().setMessageLogging { s ->
            if (s.startsWith(">>>>>")) {
                msgText = s; msgSample = null; msgStart = SystemClock.uptimeMillis()
            } else if (s.startsWith("<<<<<")) {
                val start = msgStart
                msgStart = 0L
                val d = SystemClock.uptimeMillis() - start
                if (start != 0L && d >= SLOW_MS) {
                    val what = msgText?.substringAfter("} ")?.take(160) ?: "?"
                    log("LANGSAM ${d} ms auf dem Hauptthread: $what" + (msgSample?.let { "\n$it" } ?: ""))
                }
            }
        }

        // Samples the main thread's stack while a message runs long; also writes the log out.
        val main = Looper.getMainLooper().thread
        Thread({
            var lastWrite = SystemClock.uptimeMillis()
            while (true) {
                try { Thread.sleep(40) } catch (_: InterruptedException) { return@Thread }
                val start = msgStart
                if (start != 0L && msgSample == null && SystemClock.uptimeMillis() - start > SAMPLE_MS) {
                    msgSample = main.stackTrace.take(30).joinToString("\n") { "      at $it" }
                }
                if (dirty && SystemClock.uptimeMillis() - lastWrite > 4_000) {
                    lastWrite = SystemClock.uptimeMillis()
                    runCatching { writeOut() }
                }
            }
        }, "notes-recorder").apply { isDaemon = true; priority = Thread.MAX_PRIORITY; start() }
    }

    /** Called from the activity for every touch it receives. */
    fun touch(ev: MotionEvent, handled: Boolean, tookMs: Long) {
        val a = ev.actionMasked
        if (a == MotionEvent.ACTION_MOVE) return
        val name = when (a) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            MotionEvent.ACTION_POINTER_DOWN -> "POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "POINTER_UP"
            else -> "ACTION_$a"
        }
        val lag = SystemClock.uptimeMillis() - tookMs - ev.eventTime
        log("Touch $name x=${ev.x.toInt()} y=${ev.y.toInt()} Finger=${ev.pointerCount} Verzögerung=${lag} ms Verarbeitung=${tookMs} ms" +
            (if (!handled) " (nicht verarbeitet)" else ""))
    }

    fun event(text: String) = log(text)

    /** Writes the log out in the background (never on the main thread). */
    fun flush() { Thread { runCatching { writeOut() } }.start() }

    private fun header(): String {
        val version = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull()
        val dm = app.resources.displayMetrics
        val ime = runCatching { Settings.Secure.getString(app.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) }.getOrNull()
        val anim = runCatching { Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)
        return "TMS OS $version · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · " +
            "${dm.widthPixels}x${dm.heightPixels} @${dm.densityDpi}dpi · Animationen x$anim · Tastatur $ime"
    }

    @Synchronized private fun writeOut() {
        if (!::app.isInitialized) return
        val text = synchronized(lines) { dirty = false; lines.joinToString("\n") } + "\n"
        val bytes = text.toByteArray()
        runCatching { File(File(app.filesDir, "notes").apply { mkdirs() }, "diagnose.txt").writeBytes(bytes) }
        if (Build.VERSION.SDK_INT < 29) return
        val prefs = app.getSharedPreferences("notes_diag", Context.MODE_PRIVATE)
        val resolver = app.contentResolver
        val known = prefs.getString("uri", null)?.let(Uri::parse)
        val written = known != null && runCatching { resolver.openOutputStream(known, "wt")?.use { it.write(bytes) } != null }.getOrDefault(false)
        if (written) return
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }) ?: return
        resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
        prefs.edit().putString("uri", uri.toString()).apply()
    }
}

/** Wraps an activity's touch dispatch so every touch lands in the recorder. */
inline fun Activity.recordTouch(ev: MotionEvent, dispatch: () -> Boolean): Boolean {
    val t0 = SystemClock.uptimeMillis()
    val handled = dispatch()
    NotesDiagnostics.touch(ev, handled, SystemClock.uptimeMillis() - t0)
    return handled
}
