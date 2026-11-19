@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.tms.os.launcher.notes

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* -------------------------------------------------------------------------- */
/*                                Date helpers                                */
/* -------------------------------------------------------------------------- */

private val de = Locale.GERMANY

private fun dayStart(ms: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** "22:19" today, "Gestern", "Montag" this week, else "07.08.26". */
fun listDate(ms: Long, now: Long = System.currentTimeMillis()): String {
    val days = ((dayStart(now) - dayStart(ms)) / 86_400_000L).toInt()
    return when {
        days == 0 -> SimpleDateFormat("HH:mm", de).format(Date(ms))
        days == 1 -> "Gestern"
        days in 2..6 -> SimpleDateFormat("EEEE", de).format(Date(ms))
        else -> SimpleDateFormat("dd.MM.yy", de).format(Date(ms))
    }
}

/** "7. August 2026 um 22:19", shown above a note. */
fun longDate(ms: Long): String = SimpleDateFormat("d. MMMM yyyy 'um' HH:mm", de).format(Date(ms))

private fun groupOf(ms: Long, now: Long): String {
    val days = ((dayStart(now) - dayStart(ms)) / 86_400_000L).toInt()
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    val n = Calendar.getInstance().apply { timeInMillis = now }
    return when {
        days < 0 -> "Zukünftig"
        days == 0 -> "Heute"
        days == 1 -> "Gestern"
        days < 7 -> "Vorherige 7 Tage"
        days < 30 -> "Vorherige 30 Tage"
        c.get(Calendar.YEAR) == n.get(Calendar.YEAR) -> SimpleDateFormat("MMMM", de).format(Date(ms))
        else -> c.get(Calendar.YEAR).toString()
    }
}

enum class NoteSort(val label: String) { EDITED("Bearbeitungsdatum"), CREATED("Erstellungsdatum"), TITLE("Titel") }

class NotesPrefs(context: Context) {
    private val p = context.getSharedPreferences("tms_notes", Context.MODE_PRIVATE)
    var sort: NoteSort
        get() = NoteSort.entries.getOrElse(p.getInt("sort", 0)) { NoteSort.EDITED }
        set(v) = p.edit().putInt("sort", v.ordinal).apply()
    var groupByDate: Boolean
        get() = p.getBoolean("group", true)
        set(v) = p.edit().putBoolean("group", v).apply()
    var sectionOpen: Boolean
        get() = p.getBoolean("section_open", true)
        set(v) = p.edit().putBoolean("section_open", v).apply()
    var lastFolder: String
        get() = p.getString("last_folder", NoteStore.DEFAULT_FOLDER) ?: NoteStore.DEFAULT_FOLDER
        set(v) = p.edit().putString("last_folder", v).apply()
}

/* -------------------------------------------------------------------------- */
/*                                  Top bar                                   */
/* -------------------------------------------------------------------------- */

/** Bar over the content: the content fades out underneath, the small title appears once the large one scrolled away. */
@Composable
fun NotesTopBar(
    title: String,
    showTitle: Boolean,
    leading: (@Composable () -> Unit)?,
    trailing: @Composable () -> Unit
) {
    val c = LocalNotesColors.current
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, tween(180), label = "smallTitle")
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to c.background, 0.7f to c.background.copy(alpha = 0.85f), 1f to c.background.copy(alpha = 0f)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 14.dp)
            .height(44.dp)
    ) {
        Box(Modifier.align(Alignment.CenterStart)) { leading?.invoke() }
        Text(title, style = NotesText.headline.copy(color = c.text), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 100.dp).graphicsLayer { alpha = titleAlpha })
        Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

@Composable
private fun LargeTitle(title: String, subtitle: String?) {
    val c = LocalNotesColors.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp)) {
        Text(title, style = NotesText.largeTitle.copy(color = c.text), maxLines = 2, overflow = TextOverflow.Ellipsis)
        subtitle?.let { Text(it, style = NotesText.subhead.copy(color = c.secondary)) }
    }
}

private fun LazyListState.scrolledPastTitle() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 120

/* -------------------------------------------------------------------------- */
/*                                  Folders                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun FoldersScreen(
    state: NotesState,
    store: NoteStore,
    prefs: NotesPrefs,
    onOpenFolder: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onCompose: () -> Unit,
    onDictate: ((String) -> Unit) -> Unit
) {
    val c = LocalNotesColors.current
    val list = rememberLazyListState()
    var editing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sectionOpen by remember { mutableStateOf(prefs.sectionOpen) }
    var newFolder by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<NoteFolder?>(null) }
    var deleteFolder by remember { mutableStateOf<NoteFolder?>(null) }
    val pastTitle by remember { derivedStateOf { list.scrolledPastTitle() } }

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 110.dp, bottom = 90.dp)
        ) {
            item("title") { LargeTitle("Ordner", null) }
            if (query.isNotBlank()) {
                val hits = searchNotes(state.live, query)
                item("hits") {
                    Text("${hits.size} Treffer", style = NotesText.sectionTitle.copy(color = c.text), modifier = Modifier.padding(start = 20.dp, bottom = 10.dp))
                    InsetGroup {
                        hits.forEachIndexed { i, n ->
                            NoteRowContent(n, state.folderName(n.folderId), showFolder = true, selected = null, onClick = { onOpenNote(n.id) }, onLongClick = {})
                            if (i != hits.lastIndex) RowSeparator(20)
                        }
                    }
                }
            } else {
                item("header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) {
                            sectionOpen = !sectionOpen; prefs.sectionOpen = sectionOpen
                        }.padding(start = 20.dp, end = 22.dp, top = 6.dp, bottom = 10.dp)
                    ) {
                        Text("Auf meinem Gerät", style = NotesText.sectionTitle.copy(color = c.text), modifier = Modifier.weight(1f))
                        val rot by animateFloatAsState(if (sectionOpen) 0f else -90f, label = "chevron")
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.accent, modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = rot })
                    }
                }
                item("folders") {
                    AnimatedVisibility(sectionOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        InsetGroup {
                            val rows = buildList {
                                add(Triple(NoteStore.ALL, "Alle auf meinem Gerät", state.live.size))
                                state.folders.forEach { f -> add(Triple(f.id, f.name, state.live.count { it.folderId == f.id })) }
                                if (state.deleted.isNotEmpty()) add(Triple(NoteStore.DELETED, "Zuletzt gelöscht", state.deleted.size))
                            }
                            rows.forEachIndexed { i, (id, name, count) ->
                                val folder = state.folders.firstOrNull { it.id == id }
                                val deletable = editing && folder != null && id != NoteStore.DEFAULT_FOLDER
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().height(56.dp).clickable {
                                        if (editing) { if (folder != null && id != NoteStore.DEFAULT_FOLDER) rename = folder } else onOpenFolder(id)
                                    }.padding(start = 16.dp, end = 14.dp)
                                ) {
                                    AnimatedVisibility(deletable) {
                                        Icon(Icons.Rounded.RemoveCircle, null, tint = c.destructive, modifier = Modifier.padding(end = 12.dp).size(24.dp).clickable { deleteFolder = folder })
                                    }
                                    Icon(if (id == NoteStore.DELETED) Icons.Outlined.Delete else Icons.Outlined.Folder, null, tint = c.folder, modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Box(Modifier.weight(1f)) {
                                        Text(name, style = NotesText.body.copy(color = c.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (!editing) {
                                        Text("$count", style = NotesText.body.copy(color = c.secondary))
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.tertiary, modifier = Modifier.size(24.dp))
                                    } else if (folder != null && id != NoteStore.DEFAULT_FOLDER) {
                                        Icon(Icons.Rounded.Edit, null, tint = c.accent, modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (i != rows.lastIndex) RowSeparator(56)
                            }
                        }
                    }
                }
            }
        }

        NotesTopBar("Ordner", pastTitle, leading = null) {
            GlassCircle(Icons.Outlined.CreateNewFolder, tint = c.accent) { newFolder = true }
            GlassTextButton(if (editing) "Fertig" else "Bearbeiten", bold = editing, color = if (editing) c.accent else null) { editing = !editing }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(c.background.copy(alpha = 0f), c.background)))
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
            NotesBottomBar(query, { query = it }, onCompose, onDictate = { onDictate { query = it } })
        }

        if (newFolder) NotesAlert("Neuer Ordner", "Gib einen Namen für diesen Ordner ein.", field = "", placeholder = "Name", confirm = "Sichern",
            onConfirm = { store.createFolder(it); newFolder = false }, onDismiss = { newFolder = false })
        rename?.let { f ->
            NotesAlert("Ordner umbenennen", null, field = f.name, placeholder = "Name", confirm = "Sichern",
                onConfirm = { store.renameFolder(f.id, it); rename = null }, onDismiss = { rename = null })
        }
        deleteFolder?.let { f ->
            NotesAlert("Ordner löschen?", "Der Ordner wird gelöscht und seine Notizen werden nach „Zuletzt gelöscht“ bewegt.", confirm = "Löschen", destructive = true,
                onConfirm = { store.deleteFolder(f.id); deleteFolder = null }, onDismiss = { deleteFolder = null })
        }
    }
}

fun searchNotes(notes: List<Note>, query: String): List<Note> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    // Locked notes are found by their title only, like on the iPhone.
    return notes.filter { n -> if (n.locked) n.title.lowercase().contains(q) else n.searchText().lowercase().contains(q) }
        .sortedByDescending { it.modifiedAt }
}

/* -------------------------------------------------------------------------- */
/*                                 Note list                                  */
/* -------------------------------------------------------------------------- */

@Composable
fun NoteListScreen(
    folderId: String,
    state: NotesState,
    store: NoteStore,
    prefs: NotesPrefs,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onCompose: () -> Unit,
    onDictate: ((String) -> Unit) -> Unit,
    onShare: (Note) -> Unit,
    onToggleLock: (Note) -> Unit
) {
    val c = LocalNotesColors.current
    val list = rememberLazyListState()
    val title = when (folderId) {
        NoteStore.ALL -> "Alle auf meinem Gerät"
        NoteStore.DELETED -> "Zuletzt gelöscht"
        else -> state.folderName(folderId)
    }
    val isTrash = folderId == NoteStore.DELETED
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(prefs.sort) }
    var grouped by remember { mutableStateOf(prefs.groupByDate) }
    var menu by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var openRow by remember { mutableStateOf<String?>(null) }
    var moveIds by remember { mutableStateOf<List<String>?>(null) }
    var rowMenu by remember { mutableStateOf<Note?>(null) }
    var renameFolder by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    val pastTitle by remember { derivedStateOf { list.scrolledPastTitle() } }
    val now = System.currentTimeMillis()

    val notes = state.inFolder(folderId).let { all -> if (query.isBlank()) all else searchNotes(all, query) }.let { ns ->
        when (sort) {
            NoteSort.EDITED -> ns.sortedByDescending { it.modifiedAt }
            NoteSort.CREATED -> ns.sortedByDescending { it.createdAt }
            NoteSort.TITLE -> ns.sortedBy { it.title.lowercase() }
        }
    }
    val pinned = if (isTrash) emptyList() else notes.filter { it.pinned }
    val rest = notes - pinned.toSet()
    val sections: List<Pair<String?, List<Note>>> = buildList {
        if (pinned.isNotEmpty()) add("Angeheftet" to pinned)
        if (grouped && sort != NoteSort.TITLE) {
            rest.groupBy { groupOf(if (sort == NoteSort.CREATED) it.createdAt else it.modifiedAt, now) }.forEach { (g, ns) -> add(g to ns) }
        } else if (rest.isNotEmpty()) add((if (pinned.isNotEmpty()) "Notizen" else null) to rest)
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 110.dp, bottom = 96.dp)
        ) {
            item("title") { LargeTitle(title, if (notes.size == 1) "1 Notiz" else "${notes.size} Notizen") }
            if (isTrash) item("trashInfo") {
                Text("Notizen werden nach bis zu 30 Tagen endgültig gelöscht.", style = NotesText.footnote.copy(color = c.secondary), modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp))
            }
            if (notes.isEmpty()) item("empty") {
                Text(if (query.isBlank()) "Keine Notizen" else "Keine Treffer", style = NotesText.body.copy(color = c.secondary), modifier = Modifier.fillMaxWidth().padding(top = 80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            sections.forEach { (header, ns) ->
                item("h_${header}_${ns.first().id}") {
                    if (header != null) Text(header, style = NotesText.sectionTitle.copy(color = c.text), modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp))
                    InsetGroup(Modifier.padding(bottom = 18.dp).animateContentSize()) {
                        ns.forEachIndexed { i, n ->
                            androidx.compose.runtime.key(n.id) {
                                SwipeRow(
                                    id = n.id, openId = openRow, onOpen = { openRow = it },
                                    leading = if (isTrash || selecting) emptyList() else listOf(SwipeAction(Icons.Rounded.PushPin, c.orange, if (n.pinned) "Loslösen" else "Anheften") { store.togglePin(n.id) }),
                                    trailing = if (selecting) emptyList() else if (isTrash) listOf(
                                        SwipeAction(Icons.Outlined.Delete, c.destructive, "Löschen") { store.deleteForever(listOf(n.id)) },
                                        SwipeAction(Icons.Outlined.RestoreFromTrash, c.purple, "Wiederherstellen") { moveIds = listOf(n.id) }
                                    ) else listOf(
                                        SwipeAction(Icons.Outlined.Delete, c.destructive, "Löschen") { store.moveToTrash(listOf(n.id)) },
                                        SwipeAction(Icons.Outlined.DriveFileMove, c.purple, "Bewegen") { moveIds = listOf(n.id) }
                                    )
                                ) {
                                    Box(Modifier.background(c.card)) {
                                        NoteRowContent(
                                            n, state.folderName(n.folderId), showFolder = folderId == NoteStore.ALL || query.isNotBlank(),
                                            selected = if (selecting) n.id in selected else null,
                                            onClick = {
                                                when {
                                                    openRow != null -> openRow = null
                                                    selecting -> if (!selected.remove(n.id)) selected += n.id
                                                    else -> onOpenNote(n.id)
                                                }
                                            },
                                            onLongClick = { if (!selecting) rowMenu = n }
                                        )
                                    }
                                }
                                if (i != ns.lastIndex) RowSeparator(if (selecting) 56 else 20)
                            }
                        }
                    }
                }
            }
        }

        NotesTopBar(title, pastTitle, leading = { GlassCircle(Icons.AutoMirrored.Rounded.ArrowBackIos, onClick = onBack) }) {
            if (selecting) GlassTextButton("Fertig", bold = true, color = c.accent) { selecting = false; selected.clear() }
            else GlassCircle(Icons.Rounded.MoreHoriz) { menu = true }
        }

        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(c.background.copy(alpha = 0f), c.background)))
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
            if (selecting) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    GlassTextButton(if (selected.isEmpty()) "Alle bewegen" else "Bewegen", color = c.accent) {
                        moveIds = (if (selected.isEmpty()) notes.map { it.id } else selected.toList())
                    }
                    GlassTextButton(if (selected.isEmpty()) "Alle löschen" else "Löschen", color = c.accent) {
                        val ids = if (selected.isEmpty()) notes.map { it.id } else selected.toList()
                        if (isTrash) store.deleteForever(ids) else store.moveToTrash(ids)
                        selected.clear(); selecting = false
                    }
                }
            } else NotesBottomBar(query, { query = it }, if (isTrash) null else onCompose, onDictate = { onDictate { query = it } })
        }

        val folder = state.folders.firstOrNull { it.id == folderId }
        NotesMenu(menu, buildList {
            add(MenuItem("Notizen auswählen", Icons.Outlined.CheckCircle) { selecting = true })
            NoteSort.entries.forEach { s ->
                add(MenuItem((if (sort == s) "✓  " else "     ") + "Sortieren nach ${s.label}", if (s == NoteSort.EDITED) Icons.Outlined.Sort else null) { sort = s; prefs.sort = s })
            }
            add(MenuItem((if (grouped) "✓  " else "     ") + "Nach Datum gruppieren", Icons.Outlined.CalendarMonth) { grouped = !grouped; prefs.groupByDate = grouped })
            if (folder != null && folder.id != NoteStore.DEFAULT_FOLDER) {
                add(MenuItem("Ordner umbenennen", Icons.Rounded.Edit) { renameFolder = true })
            }
            if (isTrash && notes.isNotEmpty()) add(MenuItem("Alle löschen", Icons.Outlined.Delete, destructive = true) { confirmDeleteAll = true })
        }, onDismiss = { menu = false })

        rowMenu?.let { n ->
            NotesMenu(true, buildList {
                if (!isTrash) {
                    add(MenuItem(if (n.pinned) "Notiz loslösen" else "Notiz anheften", Icons.Rounded.PushPin) { store.togglePin(n.id) })
                    add(MenuItem(if (n.locked) "Sperre entfernen" else "Notiz sperren", Icons.Rounded.Lock) { onToggleLock(n) })
                    if (!n.locked) add(MenuItem("Notiz teilen", Icons.Outlined.ChecklistRtl) { onShare(n) })
                }
                add(MenuItem(if (isTrash) "Wiederherstellen" else "Bewegen", Icons.Outlined.DriveFileMove) { moveIds = listOf(n.id) })
                add(MenuItem("Löschen", Icons.Outlined.Delete, destructive = true) { if (isTrash) store.deleteForever(listOf(n.id)) else store.moveToTrash(listOf(n.id)) })
            }, onDismiss = { rowMenu = null })
        }

        MoveNotesSheet(
            visible = moveIds != null,
            count = moveIds?.size ?: 0,
            folders = state.folders,
            currentFolder = folderId.takeIf { it != NoteStore.ALL && it != NoteStore.DELETED },
            onDismiss = { moveIds = null },
            onMove = { target -> moveIds?.let { store.move(it, target) }; moveIds = null; selecting = false; selected.clear() },
            onCreateFolder = { name -> store.createFolder(name).id }
        )

        if (renameFolder && folder != null) NotesAlert("Ordner umbenennen", null, field = folder.name, placeholder = "Name", confirm = "Sichern",
            onConfirm = { store.renameFolder(folder.id, it); renameFolder = false }, onDismiss = { renameFolder = false })
        if (confirmDeleteAll) NotesAlert("Alle löschen?", "Diese Notizen werden endgültig gelöscht. Das kann nicht widerrufen werden.", confirm = "Löschen", destructive = true,
            onConfirm = { store.deleteForever(notes.map { it.id }); confirmDeleteAll = false }, onDismiss = { confirmDeleteAll = false })
    }
}

/** One note in a list: bold title, date and first line, the folder (in "Alle"), a thumbnail when there is a picture. */
@Composable
fun NoteRowContent(note: Note, folderName: String, showFolder: Boolean, selected: Boolean?, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = LocalNotesColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 20.dp, end = 16.dp, top = 11.dp, bottom = 11.dp)
    ) {
        AnimatedVisibility(selected != null) {
            Box(Modifier.padding(end = 14.dp)) {
                if (selected == true) Icon(Icons.Rounded.CheckCircle, null, tint = c.accent, modifier = Modifier.size(24.dp))
                else Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Transparent)
                    .then(Modifier.padding(1.dp)).background(c.tertiary.copy(alpha = 0.0f))) {
                    Box(Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape).background(c.tertiary.copy(alpha = 0.35f)).padding(1.5.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.card))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(note.title.ifEmpty { "Neue Notiz" }, style = NotesText.headline.copy(color = c.text, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(listDate(note.modifiedAt), style = NotesText.subhead.copy(color = c.text))
                Spacer(Modifier.width(7.dp))
                if (note.locked) {
                    Icon(Icons.Rounded.Lock, null, tint = c.secondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Gesperrt", style = NotesText.subhead.copy(color = c.secondary), maxLines = 1)
                } else Text(note.preview ?: "Kein zusätzlicher Text", style = NotesText.subhead.copy(color = c.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showFolder && folderName.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                Icon(Icons.Outlined.Folder, null, tint = c.secondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(folderName, style = NotesText.subhead.copy(color = c.secondary), maxLines = 1)
            }
        }
        if (!note.locked) note.firstImage?.let { file ->
            Spacer(Modifier.width(10.dp))
            NoteThumb(file, 52)
        }
    }
}

@Composable
fun NoteThumb(file: String, sizeDp: Int) {
    val c = LocalNotesColors.current
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { loadBitmap(NoteStore.get(context).mediaFile(file).path, sizeDp * 3) }
    }
    Box(Modifier.size(sizeDp.dp).clip(RoundedCornerShape(8.dp)).background(c.elevated)) {
        bmp?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}

/** Decodes a picture no bigger than needed for [maxPx]. */
fun loadBitmap(path: String, maxPx: Int): ImageBitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, o)
    var sample = 1
    while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= maxPx) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()
