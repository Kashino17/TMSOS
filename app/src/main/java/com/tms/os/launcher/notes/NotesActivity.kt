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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tms.os.launcher.data.DeviceAuth

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
                    onDictate = ::startDictation
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
    }
}

private val IosNotesEase = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
