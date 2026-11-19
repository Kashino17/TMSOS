package com.tms.os.launcher.notes

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/* -------------------------------------------------------------------------- */
/*                                   Model                                    */
/* -------------------------------------------------------------------------- */

data class NoteFolder(val id: String, val name: String, val createdAt: Long)

/** One piece of a note: rich text, a table, a picture (photo or scan) or a drawing. */
sealed class NoteBlock {
    abstract val id: String

    /** [rich] is the serialised Spannable (see [RichText]). */
    data class Text(override val id: String, val rich: RichText) : NoteBlock()
    data class Table(override val id: String, val cells: List<List<String>>) : NoteBlock()
    data class Image(override val id: String, val file: String, val scan: Boolean = false) : NoteBlock()
    /** [file] is the rendered PNG, [strokes] the editable vector data. */
    data class Drawing(override val id: String, val file: String, val strokes: String) : NoteBlock()
}

data class Note(
    val id: String,
    val folderId: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val pinned: Boolean = false,
    val locked: Boolean = false,
    /** Set while the note sits in "Zuletzt gelöscht". */
    val deletedAt: Long? = null,
    val blocks: List<NoteBlock> = emptyList()
) {
    private val lines: List<String> by lazy {
        blocks.flatMap { b ->
            when (b) {
                is NoteBlock.Text -> b.rich.text.split('\n')
                is NoteBlock.Table -> b.cells.flatten()
                else -> emptyList()
            }
        }.map { it.trim() }.filter { it.isNotEmpty() }
    }
    val title: String get() = lines.firstOrNull() ?: ""
    /** The line under the title in the list ("Kein zusätzlicher Text" when there is none). */
    val preview: String? get() = lines.getOrNull(1)
    val hasAttachment: Boolean get() = blocks.any { it is NoteBlock.Image || it is NoteBlock.Drawing }
    val isEmpty: Boolean get() = lines.isEmpty() && !hasAttachment
    val firstImage: String? get() = blocks.firstNotNullOfOrNull { (it as? NoteBlock.Image)?.file ?: (it as? NoteBlock.Drawing)?.file }
    fun searchText(): String = lines.joinToString(" ")
}

data class NotesState(
    val loaded: Boolean = false,
    val folders: List<NoteFolder> = emptyList(),
    val notes: List<Note> = emptyList()
) {
    val live: List<Note> get() = notes.filter { it.deletedAt == null }
    val deleted: List<Note> get() = notes.filter { it.deletedAt != null }
    fun inFolder(folderId: String): List<Note> = when (folderId) {
        NoteStore.ALL -> live
        NoteStore.DELETED -> deleted
        else -> live.filter { it.folderId == folderId }
    }
    fun folderName(id: String): String = folders.firstOrNull { it.id == id }?.name ?: ""
}

/* -------------------------------------------------------------------------- */
/*                                   Store                                    */
/* -------------------------------------------------------------------------- */

/**
 * All notes live in one JSON file in the app's private storage, attachments in
 * `notes/media`. Reads happen once; every change is written (debounced) in the
 * background, atomically through a temporary file. Shared by the Notes app and
 * the launcher's Notes widget.
 */
class NoteStore private constructor(context: Context) {

    companion object {
        const val DEFAULT_FOLDER = "notes"
        const val ALL = "__all__"
        const val DELETED = "__deleted__"
        private const val PURGE_AFTER_MS = 30L * 24 * 60 * 60 * 1000

        @Volatile private var instance: NoteStore? = null
        fun get(context: Context): NoteStore =
            instance ?: synchronized(this) { instance ?: NoteStore(context.applicationContext).also { instance = it } }
    }

    private val dir = File(context.filesDir, "notes").apply { mkdirs() }
    val mediaDir = File(dir, "media").apply { mkdirs() }
    private val file = File(dir, "notes.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    private val _state = MutableStateFlow(NotesState())
    val state: StateFlow<NotesState> = _state.asStateFlow()

    private var loadedStamp = 0L
    /** Only the Notes app's own process writes; the launcher just reads (Spotlight, widget). */
    private val writer = runCatching { android.app.Application.getProcessName().endsWith(":notes") }.getOrDefault(true)

    init { scope.launch { load() } }

    /**
     * The Notes app runs in its own process: the launcher (Spotlight, widget)
     * re-reads the file when it changed on disk.
     */
    fun reloadIfChanged() {
        if (file.lastModified() != loadedStamp) scope.launch { load() }
    }

    private val backup = File(dir, "notes.json.bak")
    private val tmp = File(dir, "notes.json.tmp")
    /** All writes run one after another on this single lane, never two at once. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val writeLane = Dispatchers.IO.limitedParallelism(1)

    private fun readState(f: File): NotesState? =
        if (!f.exists() || f.length() == 0L) null else runCatching { parse(JSONObject(f.readText())) }.getOrNull()

    private fun load() {
        run {
            loadedStamp = file.lastModified()
            // The newest readable copy wins: the file, else the last good backup, else an unfinished write.
            val fromFile = readState(file)
            val loaded = fromFile ?: readState(backup) ?: readState(tmp)
                ?: NotesState(folders = listOf(NoteFolder(DEFAULT_FOLDER, "Notizen", System.currentTimeMillis())))
            if (writer) {
                if (fromFile != null) runCatching { file.copyTo(backup, overwrite = true) }
                // An unreadable file is kept aside, never overwritten, so nothing is lost for good.
                else if (file.exists() && file.length() > 0) runCatching { file.copyTo(File(dir, "notes.unreadable-${System.currentTimeMillis()}.json")) }
            }
            // "Zuletzt gelöscht" keeps notes for 30 days, as on iOS.
            val now = System.currentTimeMillis()
            val (expired, kept) = loaded.notes.partition { it.deletedAt != null && now - it.deletedAt > PURGE_AFTER_MS }
            expired.forEach { deleteMedia(it) }
            val folders = if (loaded.folders.none { it.id == DEFAULT_FOLDER }) listOf(NoteFolder(DEFAULT_FOLDER, "Notizen", now)) + loaded.folders else loaded.folders
            _state.value = NotesState(loaded = true, folders = folders, notes = kept)
            // Recovered from the backup: notes.json is rewritten right away.
            if (expired.isNotEmpty() || (fromFile == null && file.exists())) scheduleSave()
        }
    }

    /* ----------------------------------------------------------- mutations */

    fun newId(): String = UUID.randomUUID().toString()

    fun createNote(folderId: String): Note {
        val now = System.currentTimeMillis()
        val folder = if (folderId == ALL || folderId == DELETED) DEFAULT_FOLDER else folderId
        val note = Note(newId(), folder, now, now, blocks = listOf(NoteBlock.Text(newId(), RichText.EMPTY_TITLE)))
        _state.update { it.copy(notes = it.notes + note) }
        scheduleSave()
        return note
    }

    /** Adds imported notes (Samsung Notes and other apps). */
    fun addNotes(notes: List<Note>) {
        if (notes.isEmpty()) return
        _state.update { it.copy(notes = it.notes + notes) }
        scheduleSave()
    }

    /** Replaces the note's content; only bumps the date when something really changed. */
    fun updateBlocks(id: String, blocks: List<NoteBlock>) {
        _state.update { s ->
            s.copy(notes = s.notes.map { n -> if (n.id == id && n.blocks != blocks) n.copy(blocks = blocks, modifiedAt = System.currentTimeMillis()) else n })
        }
        scheduleSave()
    }

    fun update(id: String, transform: (Note) -> Note) {
        _state.update { s -> s.copy(notes = s.notes.map { if (it.id == id) transform(it) else it }) }
        scheduleSave()
    }

    /**
     * Leaving an empty note: a note that never had content is thrown away, as on
     * iOS; one that had content (and was emptied) goes to "Zuletzt gelöscht", so
     * nothing ever disappears without a trace.
     */
    fun discardIfEmpty(id: String, hadContent: Boolean = false) {
        val n = _state.value.notes.firstOrNull { it.id == id } ?: return
        if (!n.isEmpty || n.deletedAt != null) return
        if (hadContent) { moveToTrash(listOf(id)); return }
        _state.update { s -> s.copy(notes = s.notes.filterNot { it.id == id }) }
        deleteMedia(n)
        scheduleSave()
    }

    fun moveToTrash(ids: Collection<String>) {
        val now = System.currentTimeMillis()
        _state.update { s -> s.copy(notes = s.notes.map { if (it.id in ids) it.copy(deletedAt = now, pinned = false) else it }) }
        scheduleSave()
    }

    fun deleteForever(ids: Collection<String>) {
        val gone = _state.value.notes.filter { it.id in ids }
        _state.update { s -> s.copy(notes = s.notes.filterNot { it.id in ids }) }
        gone.forEach { deleteMedia(it) }
        scheduleSave()
    }

    fun move(ids: Collection<String>, folderId: String) {
        _state.update { s -> s.copy(notes = s.notes.map { if (it.id in ids) it.copy(folderId = folderId, deletedAt = null) else it }) }
        scheduleSave()
    }

    fun togglePin(id: String) = update(id) { it.copy(pinned = !it.pinned) }
    fun setLocked(id: String, locked: Boolean) = update(id) { it.copy(locked = locked) }

    fun createFolder(name: String): NoteFolder {
        val f = NoteFolder(newId(), name.trim().ifEmpty { "Neuer Ordner" }, System.currentTimeMillis())
        _state.update { it.copy(folders = it.folders + f) }
        scheduleSave()
        return f
    }

    fun renameFolder(id: String, name: String) {
        _state.update { s -> s.copy(folders = s.folders.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it }) }
        scheduleSave()
    }

    /** Deleting a folder moves its notes to "Zuletzt gelöscht". */
    fun deleteFolder(id: String) {
        if (id == DEFAULT_FOLDER) return
        val now = System.currentTimeMillis()
        _state.update { s ->
            s.copy(
                folders = s.folders.filterNot { it.id == id },
                notes = s.notes.map { if (it.folderId == id) it.copy(folderId = DEFAULT_FOLDER, deletedAt = it.deletedAt ?: now) else it }
            )
        }
        scheduleSave()
    }

    fun mediaFile(name: String): File = File(mediaDir, name)

    private fun deleteMedia(n: Note) {
        n.blocks.forEach { b ->
            when (b) {
                is NoteBlock.Image -> mediaFile(b.file).delete()
                is NoteBlock.Drawing -> mediaFile(b.file).delete()
                else -> Unit
            }
        }
    }

    /* --------------------------------------------------------- persistence */

    private fun scheduleSave() {
        if (!writer || !_state.value.loaded) return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(350)
            writeNow()
        }
    }

    /** Writes immediately (called when the app goes to the background). */
    fun flush() {
        if (!writer || !_state.value.loaded) return
        saveJob?.cancel()
        scope.launch { writeNow() }
    }

    /**
     * One write at a time (single lane): the full state goes to a temporary file,
     * is synced to the disk, and then atomically replaces notes.json. The old
     * file is never deleted first, so a failed or interrupted write leaves the
     * previous version in place.
     */
    private suspend fun writeNow() = kotlinx.coroutines.withContext(writeLane + kotlinx.coroutines.NonCancellable) {
        val json = serialize(_state.value).toString()
        runCatching {
            java.io.FileOutputStream(tmp).use { out -> out.write(json.toByteArray()); out.fd.sync() }
            java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
            loadedStamp = file.lastModified()
        }
    }

    private fun serialize(s: NotesState): JSONObject = JSONObject().apply {
        put("version", 1)
        put("folders", JSONArray().apply {
            s.folders.forEach { f -> put(JSONObject().put("id", f.id).put("name", f.name).put("created", f.createdAt)) }
        })
        put("notes", JSONArray().apply {
            s.notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id); put("folder", n.folderId); put("created", n.createdAt); put("modified", n.modifiedAt)
                    put("pinned", n.pinned); put("locked", n.locked)
                    n.deletedAt?.let { put("deleted", it) }
                    put("blocks", JSONArray().apply { n.blocks.forEach { put(blockToJson(it)) } })
                })
            }
        })
    }

    private fun blockToJson(b: NoteBlock): JSONObject = when (b) {
        is NoteBlock.Text -> JSONObject().put("t", "text").put("id", b.id).put("rich", b.rich.toJson())
        is NoteBlock.Table -> JSONObject().put("t", "table").put("id", b.id)
            .put("cells", JSONArray().apply { b.cells.forEach { row -> put(JSONArray().apply { row.forEach { put(it) } }) } })
        is NoteBlock.Image -> JSONObject().put("t", "image").put("id", b.id).put("file", b.file).put("scan", b.scan)
        is NoteBlock.Drawing -> JSONObject().put("t", "drawing").put("id", b.id).put("file", b.file).put("strokes", b.strokes)
    }

    private fun parse(o: JSONObject): NotesState {
        val folders = o.optJSONArray("folders")?.let { a ->
            (0 until a.length()).map { i -> a.getJSONObject(i).let { NoteFolder(it.getString("id"), it.getString("name"), it.optLong("created")) } }
        }.orEmpty()
        val notes = o.optJSONArray("notes")?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                runCatching {
                    val n = a.getJSONObject(i)
                    val blocks = n.optJSONArray("blocks")?.let { ba -> (0 until ba.length()).mapNotNull { j -> blockFromJson(ba.getJSONObject(j)) } }.orEmpty()
                    Note(
                        id = n.getString("id"),
                        folderId = n.optString("folder", DEFAULT_FOLDER),
                        createdAt = n.optLong("created"),
                        modifiedAt = n.optLong("modified"),
                        pinned = n.optBoolean("pinned"),
                        locked = n.optBoolean("locked"),
                        deletedAt = if (n.has("deleted")) n.getLong("deleted") else null,
                        blocks = blocks
                    )
                }.getOrNull()
            }
        }.orEmpty()
        return NotesState(folders = folders, notes = notes)
    }

    private fun blockFromJson(o: JSONObject): NoteBlock? = when (o.optString("t")) {
        "text" -> NoteBlock.Text(o.getString("id"), RichText.fromJson(o.getJSONObject("rich")))
        "table" -> NoteBlock.Table(o.getString("id"), o.getJSONArray("cells").let { rows ->
            (0 until rows.length()).map { r -> rows.getJSONArray(r).let { c -> (0 until c.length()).map { c.getString(it) } } }
        })
        "image" -> NoteBlock.Image(o.getString("id"), o.getString("file"), o.optBoolean("scan"))
        "drawing" -> NoteBlock.Drawing(o.getString("id"), o.getString("file"), o.optString("strokes"))
        else -> null
    }
}
