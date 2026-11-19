package com.tms.os.launcher.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Brings notes from other apps into TMS OS Notes, above all from Samsung Notes.
 * Samsung Notes keeps its notes in private storage that no other app can read,
 * but it shares or saves notes as files: a Word file keeps text, formatting,
 * lists, tables and pictures; a text file keeps the text; a PDF becomes pages
 * (pictures), as a scan. One file becomes one note.
 */
class NoteImporter(private val context: Context, private val store: NoteStore) {

    data class Result(val notes: List<Note>, val skipped: List<String>)

    private val media = NoteMedia(context, store)

    suspend fun read(uris: List<Uri>, sharedText: String?, subject: String?, folderId: String): Result = withContext(Dispatchers.IO) {
        val notes = ArrayList<Note>()
        val skipped = ArrayList<String>()
        val now = System.currentTimeMillis()
        uris.forEachIndexed { i, uri ->
            val name = displayName(uri)
            val title = name.substringBeforeLast('.').trim()
            val time = lastModified(uri) ?: (now - i)
            val type = (context.contentResolver.getType(uri) ?: "").lowercase()
            val ext = name.substringAfterLast('.', "").lowercase()
            val blocks: List<NoteBlock>? = runCatching {
                when {
                    ext == "docx" || type.contains("wordprocessingml") -> readDocx(uri, title)
                    ext == "txt" || type.startsWith("text/") -> fromText(readText(uri), title)
                    ext == "pdf" || type == "application/pdf" -> readPdf(uri, title)
                    type.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "heic", "gif") ->
                        media.importImage(uri)?.let { listOf(titleBlock(title), NoteBlock.Image(store.newId(), it), NoteBlock.Text(store.newId(), RichText.EMPTY_BODY)) }
                    else -> null
                }
            }.getOrNull()
            if (blocks == null || blocks.isEmpty()) skipped += name
            else notes += Note(store.newId(), folderId, time, time, blocks = blocks)
        }
        if (uris.isEmpty() && !sharedText.isNullOrBlank()) {
            val title = subject?.trim().orEmpty()
            notes += Note(store.newId(), folderId, now, now, blocks = fromText(sharedText, title))
        }
        Result(notes, skipped)
    }

    /* ----------------------------------------------------------------- files */

    private fun displayName(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Notiz"

    private fun lastModified(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0).takeIf { it > 0 } else null
        }
    }.getOrNull()

    private fun readText(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
        return when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> String(bytes, Charsets.UTF_16LE).drop(1)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> String(bytes, Charsets.UTF_16BE).drop(1)
            else -> String(bytes, Charsets.UTF_8).removePrefix("﻿")
        }.replace("\r\n", "\n").replace('\r', '\n')
    }

    /* ------------------------------------------------------------ rich text */

    private class Run(val text: String, val b: Boolean = false, val i: Boolean = false, val u: Boolean = false, val s: Boolean = false)

    /** Builds one text block paragraph by paragraph, with the character styles of each run. */
    private class TextBuilder {
        private val sb = StringBuilder()
        private val chars = ArrayList<SpanRec>()
        private val paras = ArrayList<Pair<Int, ParaAttrs>>()
        val isEmpty get() = paras.isEmpty()
        val hasText get() = sb.isNotBlank()

        fun para(runs: List<Run>, attrs: ParaAttrs) {
            // A line break inside a paragraph starts a new paragraph with the same style.
            var cur = ArrayList<Run>()
            val lines = ArrayList<List<Run>>()
            runs.forEach { r ->
                val parts = r.text.split('\n')
                parts.forEachIndexed { k, p ->
                    if (k > 0) { lines += cur; cur = ArrayList() }
                    if (p.isNotEmpty()) cur += Run(p, r.b, r.i, r.u, r.s)
                }
            }
            lines += cur
            lines.forEach { line ->
                var a = attrs
                var first = true
                if (paras.isNotEmpty()) sb.append('\n')
                val start = sb.length
                line.forEach { r ->
                    var t = r.text
                    // Checklist items come out of Word/Text exports as ☐ / ☑ characters.
                    if (first) {
                        val m = CHECK_PREFIX.find(t)
                        if (m != null && start == sb.length) {
                            a = a.copy(list = ListKind.CHECK, checked = m.groupValues[1] in CHECKED)
                            t = t.substring(m.range.last + 1)
                        }
                    }
                    first = false
                    val s = sb.length
                    sb.append(t)
                    val e = sb.length
                    if (e > s) {
                        if (r.b) chars += SpanRec("b", s, e)
                        if (r.i) chars += SpanRec("i", s, e)
                        if (r.u) chars += SpanRec("u", s, e)
                        if (r.s) chars += SpanRec("s", s, e)
                    }
                }
                paras += start to a
            }
        }

        fun build(): RichText {
            val text = sb.toString()
            val spans = ArrayList<SpanRec>()
            paras.forEachIndexed { k, (start, a) ->
                val end = if (k + 1 < paras.size) paras[k + 1].first else text.length
                spans += SpanRec("p", start, end, a.pack())
            }
            return RichText(text, spans + chars)
        }

        /** The first paragraph's text (the title in a note). */
        fun firstLine(): String = sb.toString().substringBefore('\n').trim()
    }

    private fun titleBlock(title: String): NoteBlock.Text {
        val t = title.ifBlank { "Notiz" }
        return NoteBlock.Text(store.newId(), RichText(t, listOf(SpanRec("p", 0, t.length, ParaAttrs(style = ParaStyle.TITLE).pack()))))
    }

    /** Whether the file name should be put in front as the title (it is not already the first line). */
    private fun needsTitle(fileTitle: String, firstLine: String): Boolean {
        if (fileTitle.isBlank()) return false
        val f = fileTitle.lowercase().replace('_', ' ').trim()
        val l = firstLine.lowercase().trim()
        if (l.isEmpty()) return true
        if (l == f || l.startsWith(f) || f.startsWith(l)) return false
        // Names the app made up (dates, numbers) are no title.
        return !AUTO_NAME.matches(fileTitle)
    }

    private fun fromText(text: String, fileTitle: String): List<NoteBlock> {
        val lines = text.trim('\n').split('\n')
        val b = TextBuilder()
        val first = lines.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val withTitle = needsTitle(fileTitle, first)
        if (withTitle) b.para(listOf(Run(fileTitle)), ParaAttrs(style = ParaStyle.TITLE))
        var titleDone = withTitle
        lines.forEach { line ->
            val style = if (!titleDone && line.isNotBlank()) { titleDone = true; ParaStyle.TITLE } else ParaStyle.BODY
            val bullet = BULLET_PREFIX.find(line)
            if (bullet != null && style == ParaStyle.BODY) b.para(listOf(Run(line.substring(bullet.range.last + 1))), ParaAttrs(list = ListKind.BULLET))
            else b.para(listOf(Run(line)), ParaAttrs(style = style))
        }
        return listOf(NoteBlock.Text(store.newId(), b.build()))
    }

    /* ----------------------------------------------------------------- Word */

    private suspend fun readDocx(uri: Uri, fileTitle: String): List<NoteBlock>? {
        var document: ByteArray? = null
        var rels: ByteArray? = null
        val files = HashMap<String, ByteArray>()
        var total = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val n = entry.name
                    when {
                        n == "word/document.xml" -> document = zip.readBytes()
                        n == "word/_rels/document.xml.rels" -> rels = zip.readBytes()
                        n.startsWith("word/media/") && total < 60L * 1024 * 1024 -> zip.readBytes().also { total += it.size; files[n] = it }
                    }
                }
            }
        }
        val doc = document ?: return null
        val targets = HashMap<String, String>()
        rels?.let { r ->
            val p = Xml.newPullParser().apply { setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false); setInput(r.inputStream(), null) }
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && p.name.endsWith("Relationship")) {
                    val id = p.getAttributeValue(null, "Id"); val target = p.getAttributeValue(null, "Target")
                    if (id != null && target != null) targets[id] = if (target.startsWith("/")) target.drop(1) else "word/$target"
                }
            }
        }

        val blocks = ArrayList<NoteBlock>()
        var text = TextBuilder()
        var titled = false
        fun flushText() {
            if (!text.isEmpty) blocks += NoteBlock.Text(store.newId(), text.build())
            text = TextBuilder()
        }

        val p = Xml.newPullParser().apply { setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false); setInput(doc.inputStream(), null) }
        var runs = ArrayList<Run>()
        var pStyle: String? = null
        var numbered = false
        var inRun = false
        var rb = false; var ri = false; var ru = false; var rs = false
        val images = ArrayList<String>()
        var tableDepth = 0
        var rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        var cell = StringBuilder()

        while (p.next() != XmlPullParser.END_DOCUMENT) {
            val tag = p.name ?: continue
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (tag) {
                    "w:tbl" -> { if (tableDepth == 0) { rows = ArrayList() }; tableDepth++ }
                    "w:tr" -> row = ArrayList()
                    "w:tc" -> cell = StringBuilder()
                    "w:p" -> { runs = ArrayList(); pStyle = null; numbered = false; images.clear() }
                    "w:pStyle" -> pStyle = p.getAttributeValue(null, "w:val")
                    "w:numPr" -> numbered = true
                    "w:r" -> { inRun = true; rb = false; ri = false; ru = false; rs = false }
                    "w:b" -> if (inRun) rb = on(p)
                    "w:i" -> if (inRun) ri = on(p)
                    "w:u" -> if (inRun) ru = p.getAttributeValue(null, "w:val")?.let { it != "none" } ?: true
                    "w:strike", "w:dstrike" -> if (inRun) rs = on(p)
                    "w:t" -> runs += Run(p.nextText(), rb, ri, ru, rs)
                    "w:tab" -> if (inRun) runs += Run("\t")
                    "w:br", "w:cr" -> if (inRun) runs += Run("\n")
                    "w:sym" -> runs += Run(symbol(p.getAttributeValue(null, "w:char")))
                    "a:blip" -> p.getAttributeValue(null, "r:embed")?.let { images += it }
                    "v:imagedata" -> p.getAttributeValue(null, "r:id")?.let { images += it }
                }
                XmlPullParser.END_TAG -> when (tag) {
                    "w:r" -> inRun = false
                    "w:p" -> {
                        val line = runs.joinToString("") { it.text }
                        if (tableDepth > 0) {
                            if (cell.isNotEmpty()) cell.append('\n')
                            cell.append(line)
                        } else {
                            val style = paraStyle(pStyle)
                            var attrs = if (numbered) ParaAttrs(style = style, list = ListKind.BULLET) else ParaAttrs(style = style)
                            if (!titled && line.isNotBlank()) {
                                titled = true
                                // The note's title: the file name in front, unless the text already starts with it.
                                if (needsTitle(fileTitle, line)) text.para(listOf(Run(fileTitle)), ParaAttrs(style = ParaStyle.TITLE))
                                else attrs = ParaAttrs(style = ParaStyle.TITLE)
                            }
                            // Empty lines before the title are dropped.
                            if (titled && (line.isNotEmpty() || images.isEmpty())) text.para(runs, attrs)
                            images.forEach { id ->
                                val bytes = targets[id]?.let { files[it] } ?: return@forEach
                                val tmp = File(context.cacheDir, "import_${UUID.randomUUID()}")
                                tmp.writeBytes(bytes)
                                media.importFile(tmp)?.let { f ->
                                    flushText()
                                    blocks += NoteBlock.Image(store.newId(), f)
                                }
                            }
                        }
                    }
                    "w:tc" -> row += cell.toString().trim()
                    "w:tr" -> if (tableDepth == 1) rows += row
                    "w:tbl" -> {
                        tableDepth--
                        if (tableDepth == 0 && rows.isNotEmpty()) {
                            val cols = rows.maxOf { it.size }.coerceAtLeast(1)
                            flushText()
                            blocks += NoteBlock.Table(store.newId(), rows.map { r -> List(cols) { c -> r.getOrElse(c) { "" } } })
                        }
                    }
                }
            }
        }
        flushText()
        if (blocks.isEmpty()) return listOf(titleBlock(fileTitle))
        if (blocks.first() !is NoteBlock.Text) blocks.add(0, titleBlock(fileTitle))
        if (blocks.last() !is NoteBlock.Text) blocks += NoteBlock.Text(store.newId(), RichText.EMPTY_BODY)
        return blocks
    }

    private fun on(p: XmlPullParser): Boolean = p.getAttributeValue(null, "w:val")?.let { it != "0" && it != "false" } ?: true

    private fun paraStyle(id: String?): ParaStyle {
        val s = id?.lowercase()?.replace(" ", "") ?: return ParaStyle.BODY
        return when {
            s.contains("title") || s.contains("titel") -> ParaStyle.TITLE
            s.endsWith("heading1") || s.endsWith("berschrift1") -> ParaStyle.HEADING
            s.contains("heading") || s.contains("berschrift") -> ParaStyle.SUBHEADING
            else -> ParaStyle.BODY
        }
    }

    /** Wingdings check boxes (w:sym) used for checklists. */
    private fun symbol(code: String?): String = when (code?.uppercase()) {
        "F0A8", "F06F", "F071" -> "☐ "
        "F0FE", "F078", "F0FC", "F0FB" -> "☑ "
        else -> ""
    }

    /* ------------------------------------------------------------------ PDF */

    private suspend fun readPdf(uri: Uri, fileTitle: String): List<NoteBlock>? {
        val pages = ArrayList<String>()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            PdfRenderer(fd).use { pdf ->
                for (i in 0 until minOf(pdf.pageCount, 40)) {
                    pdf.openPage(i).use { page ->
                        val w = 1600
                        val h = (w.toFloat() * page.height / page.width.coerceAtLeast(1)).toInt().coerceIn(1, 4000)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val name = "${UUID.randomUUID()}.jpg"
                        store.mediaFile(name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 86, it) }
                        bmp.recycle()
                        pages += name
                    }
                }
            }
        } ?: return null
        if (pages.isEmpty()) return null
        return listOf(titleBlock(fileTitle)) + pages.map { NoteBlock.Image(store.newId(), it, scan = true) } + NoteBlock.Text(store.newId(), RichText.EMPTY_BODY)
    }

    private companion object {
        val CHECK_PREFIX = Regex("^\\s*([☐☑☒□■✓✔✅⬜])\\s*")
        val CHECKED = setOf("☑", "☒", "■", "✓", "✔", "✅")
        val BULLET_PREFIX = Regex("^\\s*[•·▪◦‣-]\\s+")
        val AUTO_NAME = Regex("^[\\d_\\-\\s.:]+$|^(note|notiz|notes|text|samsung ?notes?)[\\s_\\-]*[\\d_\\-\\s.:]*$", RegexOption.IGNORE_CASE)
    }
}
