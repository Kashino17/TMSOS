package com.tms.os.launcher.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tms.os.launcher.data.DeviceAuth

/** Files or text shared to Notes (from Samsung Notes or any app), waiting for a folder. */
private data class ImportRequest(val uris: List<android.net.Uri>, val text: String?, val subject: String?)

private const val SAMSUNG_NOTES = "com.samsung.android.app.notes"

private fun Intent.importRequest(): ImportRequest? {
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
    val uris = ArrayList<android.net.Uri>()
    if (action == Intent.ACTION_SEND_MULTIPLE) {
        @Suppress("DEPRECATION") getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { uris += it }
    } else {
        @Suppress("DEPRECATION") (getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))?.let { uris += it }
    }
    // Some apps put the files only into the clip data.
    if (uris.isEmpty()) clipData?.let { c -> for (i in 0 until c.itemCount) c.getItemAt(i).uri?.let { uris += it } }
    val text = getStringExtra(Intent.EXTRA_TEXT)
    if (uris.isEmpty() && text.isNullOrBlank()) return null
    return ImportRequest(uris, text, getStringExtra(Intent.EXTRA_SUBJECT))
}

private sealed class Route {
    data object Folders : Route()
    data class Folder(val id: String) : Route()
    data class Editor(val id: String, val focus: Boolean) : Route()
}

/**
 * TMS OS Notes: an own app inside the launcher's APK, so it is always there.
 * Folders → notes → note, with the iOS 26 Notes look and behaviour.
 */
class NotesActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NOTE = "note_id"
        const val EXTRA_NEW = "new_note"

        fun open(context: Context, noteId: String? = null) {
            context.startActivity(Intent(context, NotesActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                noteId?.let { putExtra(EXTRA_NOTE, it) }
            })
        }
    }

    private var pendingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotesDiagnostics.install(this)
        enableEdgeToEdge()
        pendingIntent = intent
        val store = NoteStore.get(this)
        setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
                LocalNotesColors provides if (isSystemInDarkTheme()) DarkNotes else LightNotes
            ) {
                NotesApp(this, store, pendingIntent) { pendingIntent = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntent = intent
    }

    override fun onPause() {
        super.onPause()
        NoteStore.get(this).flush()
        NotesDiagnostics.event("Pause")
        NotesDiagnostics.flush()
    }

    override fun onResume() {
        super.onResume()
        NotesDiagnostics.event("Fortgesetzt")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        NotesDiagnostics.event(if (hasFocus) "Fenster hat Fokus" else "Fenster hat Fokus VERLOREN (Berührungen gehen an ein anderes Fenster)")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        NotesDiagnostics.event("Bildschirm geändert: ${newConfig.screenWidthDp}x${newConfig.screenHeightDp} dp")
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = recordTouch(ev) { super.dispatchTouchEvent(ev) }
}

@Composable
private fun NotesApp(activity: ComponentActivity, store: NoteStore, intent: Intent?, onIntentHandled: () -> Unit) {
    val colors = LocalNotesColors.current
    val state by store.state.collectAsState()
    val prefs = remember { NotesPrefs(activity) }
    val stack = remember { mutableStateListOf<Route>(Route.Folders, Route.Folder(prefs.lastFolder)) }
    var forward by remember { mutableStateOf(true) }

    fun push(r: Route) { NotesDiagnostics.event("Öffne $r"); forward = true; stack.add(r); (r as? Route.Folder)?.let { prefs.lastFolder = it.id } }
    fun pop() { NotesDiagnostics.event("Zurück von ${stack.lastOrNull()}"); if (stack.size > 1) { forward = false; stack.removeAt(stack.lastIndex) } else activity.finish() }
    fun compose(folderId: String) {
        val n = store.createNote(folderId)
        push(Route.Editor(n.id, focus = true))
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var importRequest by remember { mutableStateOf<ImportRequest?>(null) }
    var importing by remember { mutableStateOf<String?>(null) }
    var samsungSheet by remember { mutableStateOf(false) }
    val samsungInstalled = remember { activity.packageManager.getLaunchIntentForPackage(SAMSUNG_NOTES) != null }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) importRequest = ImportRequest(uris, null, null)
    }
    fun runImport(req: ImportRequest, folderId: String) {
        importRequest = null
        importing = if (req.uris.size > 1) "${req.uris.size} Dateien werden importiert …" else "Wird importiert …"
        scope.launch {
            val result = runCatching { NoteImporter(activity, store).read(req.uris, req.text, req.subject, folderId) }.getOrNull()
            importing = null
            if (result == null) { Toast.makeText(activity, "Import fehlgeschlagen.", Toast.LENGTH_LONG).show(); return@launch }
            store.addNotes(result.notes)
            NotesDiagnostics.event("Import: ${result.notes.size} Notizen, übersprungen: ${result.skipped}")
            val msg = buildString {
                append(if (result.notes.size == 1) "1 Notiz importiert" else "${result.notes.size} Notizen importiert")
                if (result.skipped.isNotEmpty()) {
                    append(". Nicht lesbar: ${result.skipped.size}")
                    if (result.skipped.any { it.endsWith(".sdocx", true) }) append(" (Samsung-Notes-Dateien: bitte als Word- oder Textdatei teilen)")
                }
            }
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            if (result.notes.isNotEmpty()) { stack.clear(); stack.addAll(listOf(Route.Folders, Route.Folder(folderId))); forward = true; prefs.lastFolder = folderId }
        }
    }

    // Shared to Notes (Samsung Notes "Teilen", or any other app): choose the folder, then import.
    if (intent != null && state.loaded) intent.importRequest()?.let { req ->
        importRequest = req
        onIntentHandled()
        return@let
    }

    // Launcher widget / Spotlight: open a note directly.
    if (intent != null && state.loaded) {
        val id = intent.getStringExtra(NotesActivity.EXTRA_NOTE)
        val new = intent.getBooleanExtra(NotesActivity.EXTRA_NEW, false)
        val n = id?.let { nid -> state.notes.firstOrNull { it.id == nid } }
        if (n != null) {
            stack.clear(); stack.addAll(listOf(Route.Folders, Route.Folder(n.folderId), Route.Editor(n.id, false))); forward = true
        } else if (new) {
            stack.clear(); stack.addAll(listOf(Route.Folders, Route.Folder(prefs.lastFolder)))
            compose(prefs.lastFolder)
        }
        onIntentHandled()
    }

    var onDictated by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val dictate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { onDictated?.invoke(it) }
    }
    fun startDictation(cb: (String) -> Unit) {
        onDictated = cb
        runCatching { dictate.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) }
    }
    fun share(n: Note) {
        val text = n.blocks.joinToString("\n\n") { b ->
            when (b) {
                is NoteBlock.Text -> b.rich.text
                is NoteBlock.Table -> b.cells.joinToString("\n") { it.joinToString("\t") }
                else -> ""
            }
        }.trim()
        activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), n.title))
    }
    fun authenticate(title: String, done: (Boolean) -> Unit) {
        if (!DeviceAuth.available(activity)) {
            Toast.makeText(activity, "Richte zuerst eine Displaysperre ein, um Notizen zu sperren.", Toast.LENGTH_LONG).show()
            done(false); return
        }
        DeviceAuth.authenticate(activity, title, null, done)
    }
    fun toggleLock(n: Note) = authenticate(if (n.locked) "Sperre entfernen" else "Notiz sperren") { ok -> if (ok) store.setLocked(n.id, !n.locked) }

    BackHandler(enabled = stack.size > 1) { pop() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        if (!state.loaded) return@Box
        AnimatedContent(
            targetState = stack.last(),
            transitionSpec = {
                // iOS push: the new screen slides in from the right, the old one moves a third to the left.
                if (forward) (slideInHorizontally(tween(380, easing = IosNotesEase)) { it } togetherWith
                    slideOutHorizontally(tween(380, easing = IosNotesEase)) { -it / 3 } + fadeOut(tween(380), targetAlpha = 0.6f))
                else (slideInHorizontally(tween(340, easing = IosNotesEase)) { -it / 3 } + fadeIn(tween(340), initialAlpha = 0.6f) togetherWith
                    slideOutHorizontally(tween(340, easing = IosNotesEase)) { it })
            },
            label = "notesNav"
        ) { route ->
            // Only the screen on top may navigate back: a screen that is still sliding
            // out (or a second quick tap) must not pop the one below it as well.
            fun popIfTop() { if (stack.lastOrNull() == route) pop() }
            val leaving = transition.targetState == EnterExitState.PostExit
            Box(Modifier.fillMaxSize()) {
            when (route) {
                Route.Folders -> FoldersScreen(
                    state, store, prefs,
                    onOpenFolder = { push(Route.Folder(it)) },
                    onOpenNote = { push(Route.Editor(it, false)) },
                    onCompose = { compose(NoteStore.DEFAULT_FOLDER) },
                    onDictate = ::startDictation,
                    onImport = { samsungSheet = true }
                )
                is Route.Folder -> NoteListScreen(
                    route.id, state, store, prefs,
                    onBack = ::popIfTop,
                    onOpenNote = { push(Route.Editor(it, false)) },
                    onCompose = { compose(route.id) },
                    onDictate = ::startDictation,
                    onShare = ::share,
                    onToggleLock = ::toggleLock
                )
                is Route.Editor -> NoteEditorScreen(
                    route.id, state, store, route.focus,
                    onBack = ::popIfTop,
                    onNewNote = onNew@{
                        if (stack.lastOrNull() != route) return@onNew
                        val folder = state.notes.firstOrNull { it.id == route.id }?.folderId ?: NoteStore.DEFAULT_FOLDER
                        forward = true
                        stack.removeAt(stack.lastIndex)
                        compose(folder)
                    },
                    onShare = ::share,
                    onToggleLock = ::toggleLock,
                    authenticate = ::authenticate
                )
            }
            // A screen on its way out takes no touches.
            if (leaving) Box(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
            })
            }
        }

        SamsungImportSheet(
            visible = samsungSheet,
            samsungInstalled = samsungInstalled,
            onDismiss = { samsungSheet = false },
            onOpenSamsung = {
                samsungSheet = false
                activity.packageManager.getLaunchIntentForPackage(SAMSUNG_NOTES)?.let { runCatching { activity.startActivity(it) } }
            },
            onPickFiles = {
                samsungSheet = false
                runCatching {
                    pickFiles.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain",
                        "application/pdf", "image/*", "application/octet-stream"
                    ))
                }
            }
        )

        val req = importRequest
        MoveNotesSheet(
            visible = req != null,
            count = req?.uris?.size ?: 1,
            folders = state.folders,
            currentFolder = null,
            onDismiss = { importRequest = null },
            onMove = { folderId -> req?.let { runImport(it, folderId) } },
            onCreateFolder = { name -> store.createFolder(name).id },
            title = when (val n = req?.uris?.size ?: 0) { 0, 1 -> "Importieren"; else -> "$n Dateien importieren" }
        )

        importing?.let { message ->
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)).pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
            }, contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.foundation.layout.Column(
                    Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).background(colors.elevated).padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                    androidx.compose.material3.Text(message, style = NotesText.body.copy(color = colors.text))
                }
            }
        }
    }
}

private val IosNotesEase = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
