package com.tms.os.launcher.notes

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

/** Pen, marker, pencil and eraser of the iOS markup palette. */
enum class DrawTool(val width: Float, val alpha: Float) { PEN(0.008f, 1f), MARKER(0.035f, 0.35f), PENCIL(0.005f, 0.85f), ERASER(0.03f, 1f) }

/** Points are stored relative to the canvas width, so the drawing scales with the screen. */
data class DrawStroke(val tool: DrawTool, val color: Int, val points: List<Offset>)

private fun strokesToJson(strokes: List<DrawStroke>, aspect: Float): String = JSONObject().put("aspect", aspect.toDouble()).put("strokes", JSONArray().apply {
    strokes.forEach { s ->
        put(JSONObject().put("t", s.tool.ordinal).put("c", s.color).put("p", JSONArray().apply { s.points.forEach { put(it.x.toDouble()); put(it.y.toDouble()) } }))
    }
}).toString()

private fun strokesFromJson(json: String?): List<DrawStroke> = runCatching {
    val a = JSONObject(json ?: return emptyList()).getJSONArray("strokes")
    (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        val p = o.getJSONArray("p")
        DrawStroke(DrawTool.entries[o.getInt("t")], o.getInt("c"), (0 until p.length() / 2).map { Offset(p.getDouble(it * 2).toFloat(), p.getDouble(it * 2 + 1).toFloat()) })
    }
}.getOrDefault(emptyList())

/**
 * Drawing in a note (iOS "Markup"): draw with a finger or a stylus, pick a tool
 * and colour from the glass palette, undo/redo, "Fertig" puts the drawing into the note.
 */
@Composable
fun DrawingEditor(initial: String?, onCancel: () -> Unit, onDone: (String, Bitmap) -> Unit) {
    val c = LocalNotesColors.current
    val strokes = remember { mutableStateListOf<DrawStroke>().apply { addAll(strokesFromJson(initial)) } }
    val redo = remember { mutableStateListOf<DrawStroke>() }
    var current by remember { mutableStateOf<List<Offset>?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var tool by remember { mutableStateOf(DrawTool.PEN) }
    val palette = listOf(c.text, Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFFD60A), Color(0xFFFF453A), Color(0xFFBF5AF2))
    var color by remember { mutableStateOf(palette.first()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    BackHandler(onBack = onCancel)

    fun eraseAt(p: Offset) {
        val r = DrawTool.ERASER.width
        strokes.removeAll { s -> s.points.any { hypot(it.x - p.x, it.y - p.y) < r } }
    }

    fun render(): Pair<String, Bitmap> {
        val w = size.width.coerceAtLeast(1)
        val maxY = strokes.flatMap { it.points }.maxOfOrNull { it.y } ?: 0.3f
        val hRel = (maxY + 0.06f).coerceIn(0.25f, size.height.toFloat() / w)
        val h = (hRel * w).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE; strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
        }
        strokes.forEach { s ->
            paint.color = s.color; paint.alpha = (s.tool.alpha * 255).toInt(); paint.strokeWidth = s.tool.width * w
            val path = android.graphics.Path()
            s.points.forEachIndexed { i, pt -> if (i == 0) path.moveTo(pt.x * w, pt.y * w) else path.lineTo(pt.x * w, pt.y * w) }
            if (s.points.size == 1) path.lineTo(s.points[0].x * w + 0.5f, s.points[0].y * w)
            canvas.drawPath(path, paint)
        }
        return (if (strokes.isEmpty()) "" else strokesToJson(strokes, w / h.toFloat())) to bmp
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .pointerInput(tool, color) {
                    detectDragGestures(
                        onDragStart = { o -> val p = Offset(o.x / size.width, o.y / size.width); if (tool == DrawTool.ERASER) eraseAt(p) else current = listOf(p) },
                        onDrag = { change, _ ->
                            val p = Offset(change.position.x / size.width, change.position.y / size.width)
                            if (tool == DrawTool.ERASER) eraseAt(p) else { current = current.orEmpty() + p; tick++ }
                        },
                        onDragEnd = {
                            current?.let { pts -> strokes += DrawStroke(tool, color.toArgb(), pts); redo.clear() }
                            current = null
                        },
                        onDragCancel = { current = null }
                    )
                }
        ) {
            tick
            val w = size.width.toFloat()
            fun drawStroke(t: DrawTool, col: Color, pts: List<Offset>) {
                if (pts.isEmpty()) return
                val path = Path().apply { pts.forEachIndexed { i, pt -> if (i == 0) moveTo(pt.x * w, pt.y * w) else lineTo(pt.x * w, pt.y * w) } }
                drawPath(path, col.copy(alpha = t.alpha), style = Stroke(width = t.width * w, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            strokes.forEach { drawStroke(it.tool, Color(it.color), it.points) }
            current?.let { drawStroke(tool, color, it) }
        }

        // Top: undo / redo and "Fertig".
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            GlassCapsule {
                CapsuleIcon(Icons.AutoMirrored.Rounded.Undo, enabled = strokes.isNotEmpty()) { strokes.removeLastOrNull()?.let { redo += it } }
                CapsuleIcon(Icons.AutoMirrored.Rounded.Redo, enabled = redo.isNotEmpty()) { redo.removeLastOrNull()?.let { strokes += it } }
            }
            Spacer(Modifier.weight(1f))
            GlassTextButton("Fertig", bold = true, color = c.accent) { val (json, bmp) = render(); onDone(json, bmp) }
        }

        // Bottom: the tool palette.
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(12.dp).fillMaxWidth()
                .notesGlass(c, RoundedCornerShape(30.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                listOf(DrawTool.PEN to Icons.Rounded.Create, DrawTool.MARKER to Icons.Rounded.BorderColor, DrawTool.PENCIL to Icons.Rounded.Edit, DrawTool.ERASER to Icons.Rounded.AutoFixNormal).forEach { (t, icon) ->
                    val sel = t == tool
                    Box(
                        Modifier.size(52.dp).graphicsLayer { translationY = if (sel) -6.dp.toPx() else 0f }.clip(RoundedCornerShape(14.dp))
                            .background(if (sel) c.text.copy(alpha = 0.12f) else Color.Transparent).clickable { tool = t },
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = if (t == DrawTool.ERASER) c.text else color, modifier = Modifier.size(28.dp)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                palette.forEach { col ->
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(col)
                            .border(if (col == color) 3.dp else 1.dp, if (col == color) c.accent else c.separator, CircleShape)
                            .clickable { color = col; if (tool == DrawTool.ERASER) tool = DrawTool.PEN }
                    )
                }
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
