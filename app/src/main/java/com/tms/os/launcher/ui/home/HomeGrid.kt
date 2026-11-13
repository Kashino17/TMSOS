package com.tms.os.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tms.os.launcher.domain.CellSpan
import com.tms.os.launcher.domain.PlacedCell
import com.tms.os.launcher.model.HomeGridItem
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

fun HomeGridItem.span(): CellSpan = when (this) {
    is HomeGridItem.WidgetItem -> CellSpan(size.cols, size.rows)
    else -> CellSpan(1, 1)
}

/** Pixel geometry of one home page, shared by layout, hit-testing and drop animation. */
@Stable
data class GridGeometry(
    val sidePx: Float,
    val cellW: Float,
    val cellH: Float,
    val iconPx: Float,
    val columns: Int,
    val rows: Int
) {
    /** Top-left of an item's visual box inside the page. */
    fun frameOffset(p: PlacedCell<*>): Offset =
        if (p.span.cols == 1 && p.span.rows == 1) Offset(sidePx + p.col * cellW, p.row * cellH)
        else Offset(sidePx + p.col * cellW + (cellW - iconPx) / 2f, p.row * cellH)

    fun frameWidth(p: PlacedCell<*>): Float =
        if (p.span.cols == 1 && p.span.rows == 1) cellW else (p.span.cols - 1) * cellW + iconPx

    fun frameHeight(p: PlacedCell<*>): Float =
        if (p.span.cols == 1 && p.span.rows == 1) cellH else (p.span.rows - 1) * cellH + iconPx

    /** Centre of the icon image of a 1x1 item. */
    fun iconCenter(p: PlacedCell<*>): Offset =
        Offset(sidePx + p.col * cellW + cellW / 2f, p.row * cellH + iconPx / 2f)

    fun cellAt(local: Offset): Pair<Int, Int> {
        val c = floor((local.x - sidePx) / cellW).toInt().coerceIn(0, columns - 1)
        val r = floor(local.y / cellH).toInt().coerceIn(0, rows - 1)
        return c to r
    }
}

/**
 * One home page. Every item sits at an absolute position and glides to a new
 * one with a spring whenever the order changes, which gives the iOS reflow
 * while another icon is dragged across the page.
 */
@Composable
fun HomeGridPage(
    placed: List<PlacedCell<HomeGridItem>>,
    geometry: GridGeometry,
    hiddenId: String?,
    modifier: Modifier = Modifier,
    content: @Composable (PlacedCell<HomeGridItem>, Dp, Dp) -> Unit
) {
    val density = LocalDensity.current
    Box(modifier) {
        for (p in placed) key(p.item.id) {
            val o = geometry.frameOffset(p)
            val target = IntOffset(o.x.toInt(), o.y.toInt())
            val anim = remember { Animatable(target, IntOffset.VectorConverter) }
            LaunchedEffect(target) {
                if (anim.value != target) anim.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 340f))
            }
            val w = with(density) { geometry.frameWidth(p).toDp() }
            val h = with(density) { geometry.frameHeight(p).toDp() }
            Box(
                Modifier
                    .offset { anim.value }
                    .size(w, h)
                    .graphicsLayer { alpha = if (p.item.id == hiddenId) 0f else 1f }
            ) { content(p, w, h) }
        }
    }
}

/**
 * SpringBoard jiggle: a small rotation plus a tiny bob, each item with its own
 * phase so the page never moves in lockstep.
 */
fun Modifier.jiggle(enabled: Boolean, seed: Int, amplitude: Float = 1.5f): Modifier = composed {
    if (!enabled) return@composed this
    val t = rememberInfiniteTransition(label = "jiggle")
    val period = 128 + abs(seed) % 26
    val angle by t.animateFloat(
        -amplitude, amplitude,
        infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(abs(seed) % period)),
        label = "a"
    )
    val bob by t.animateFloat(
        -0.6f, 0.6f,
        infiniteRepeatable(tween(period + 37, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )
    this.graphicsLayer {
        rotationZ = angle
        translationY = bob * density
    }
}

/**
 * iOS pick-up gesture, observed on the Initial pass so the icon's own click and
 * long-press menu keep working:
 *  - normal mode: long-press, then move without lifting → drag (menu closes);
 *  - jiggle mode: touch and move → drag immediately.
 * Movement before the long-press timeout is left to the pager (page swipe).
 */
fun Modifier.homeDragGesture(
    key: Any,
    jiggle: Boolean,
    enabled: Boolean,
    /** (finger-down position, current position), both in root coordinates. */
    onStart: (Offset, Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
    /** Finger-down position in root coordinates: false leaves the touch to everything else. */
    canStart: (Offset) -> Boolean = { true }
): Modifier = composed {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    // Mode flags are read live: starting a drag flips jiggle mode, and making
    // them pointerInput keys would cancel the very gesture that is running.
    val jiggleNow by rememberUpdatedState(jiggle)
    val enabledNow by rememberUpdatedState(enabled)
    val canStartNow by rememberUpdatedState(canStart)
    this
        .onGloballyPositioned { coords = it }
        .pointerInput(key) {
            val slop = viewConfiguration.touchSlop
            val holdMs = viewConfiguration.longPressTimeoutMillis
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!enabledNow) return@awaitEachGesture
                if (coords?.let { canStartNow(it.localToRoot(down.position)) } == false) return@awaitEachGesture
                if (!jiggleNow) {
                    val interrupted = withTimeoutOrNull(holdMs) {
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                            if (!c.pressed) return@withTimeoutOrNull true
                            if ((c.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
                        }
                        @Suppress("UNREACHABLE_CODE") true
                    }
                    if (interrupted != null) return@awaitEachGesture
                }
                // Held (or jiggling): wait for the finger to move.
                while (true) {
                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                    if (!c.pressed) return@awaitEachGesture
                    if ((c.position - down.position).getDistance() > slop) {
                        c.consume()
                        val lc = coords ?: return@awaitEachGesture
                        start(lc.localToRoot(down.position), lc.localToRoot(c.position))
                        break
                    }
                }
                // Always finish the drag, even if this gesture is cancelled (e.g. the
                // owning composable leaves the composition), so nothing stays lifted.
                var ended = false
                try {
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val c = ev.changes.firstOrNull { it.id == down.id }
                        if (c == null || !c.pressed) { ended = true; end(); break }
                        c.consume()
                        coords?.let { move(it.localToRoot(c.position)) }
                    }
                } finally {
                    if (!ended) end()
                }
            }
        }
}

/** Live state of an icon or widget being carried by the finger. */
@Stable
class HomeDragState {
    var item by mutableStateOf<HomeGridItem?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    var grab by mutableStateOf(Offset.Zero)
    /** Preview order (includes the dragged item at its would-be position). */
    var order by mutableStateOf<List<HomeGridItem>?>(null)
    var folderTarget by mutableStateOf<String?>(null)
    var hover by mutableStateOf<Hover?>(null)
    var edge by mutableStateOf(0)
    val dropAnim = Animatable(Offset.Zero, Offset.VectorConverter)
    var dropping by mutableStateOf(false)
    var dropScale by mutableStateOf(1f)
    /** Item did not start in the grid (dock or folder); it joins the grid only when hovered there. */
    var external by mutableStateOf(false)
    /** Preview dock order while dragging (null = unchanged). */
    var dockOrder by mutableStateOf<List<String>?>(null)
    var overDock by mutableStateOf(false)
    /** Free placement preview while dragging: every item's cell (null = unchanged). */
    var anchors by mutableStateOf<Map<String, com.tms.os.launcher.domain.GridAnchor>?>(null)
    /** The layout when the drag began; every preview is computed from it. */
    var basePages: List<List<PlacedCell<HomeGridItem>>>? = null

    val active get() = item != null

    fun clear() {
        item = null; order = null; folderTarget = null; hover = null; edge = 0; dropping = false; dropScale = 1f
        external = false; dockOrder = null; overDock = false; anchors = null; basePages = null
    }
}

/** Where the dragged item currently points: a cell on a page, maybe an icon centre. */
data class Hover(val page: Int, val col: Int, val row: Int, val overId: String?, val folder: Boolean)

/**
 * Computes the preview order for hovering a cell, following iOS: the carried
 * item takes the hovered item's slot and everything in between shifts by one;
 * an empty cell appends the item to the end of that page.
 */
fun reorderFor(
    order: List<HomeGridItem>,
    dragged: HomeGridItem,
    hover: Hover,
    pages: List<List<PlacedCell<HomeGridItem>>>
): List<HomeGridItem> {
    val base = order.filterNot { it.id == dragged.id }
    val from = order.indexOfFirst { it.id == dragged.id }
    val over = hover.overId?.takeIf { it != dragged.id }
    val insert = if (over != null) {
        val overIdx = order.indexOfFirst { it.id == over }
        val baseIdx = base.indexOfFirst { it.id == over }
        if (from in 0 until overIdx) baseIdx + 1 else baseIdx
    } else {
        val onPage = pages.getOrNull(hover.page).orEmpty().filter { it.item.id != dragged.id }
        val last = onPage.lastOrNull()?.item
        if (last != null) base.indexOfFirst { it.id == last.id } + 1
        else {
            val prev = pages.take(hover.page).flatten().lastOrNull { it.item.id != dragged.id }?.item
            if (prev != null) base.indexOfFirst { it.id == prev.id } + 1 else 0
        }
    }
    return base.toMutableList().apply { add(insert.coerceIn(0, size), dragged) }
}

/** Animated "folder will be created here" plate behind a hovered icon. */
@Composable
fun folderHoverScale(active: Boolean): Float {
    val s by animateFloatAsState(if (active) 1f else 0f, spring(0.7f, 500f), label = "folderHover")
    return s
}

fun distance(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)


/** Removes [pkg] from every folder (folders left empty disappear, except [keep]). */
fun List<HomeGridItem>.withoutFolderApp(pkg: String, keep: String? = null): List<HomeGridItem> = mapNotNull { item ->
    if (item is HomeGridItem.FolderItem && item.appPackages.contains(pkg)) {
        val rest = item.appPackages - pkg
        if (rest.isEmpty() && item.folderId != keep) null else item.copy(appPackages = rest)
    } else item
}

/** Where the 3×3 app slots of an open folder panel are, in root coordinates. */
class FolderSlots(val panel: Rect, private val padH: Float, private val padV: Float, val iconPx: Float, private val rowH: Float) {
    private val left = panel.left + padH
    private val top = panel.top + padV
    val cellW = (panel.width - 2 * padH) / 3f
    val pitch = (panel.height - 2 * padV - rowH) / 2f

    /** Slot (0..8) nearest to [p]. */
    fun slotAt(p: Offset): Int {
        val col = ((p.x - left) / cellW).toInt().coerceIn(0, 2)
        val row = ((p.y - top + (pitch - rowH) / 2f) / pitch).toInt().coerceIn(0, 2)
        return row * 3 + col
    }

    fun iconTopLeft(slot: Int) = Offset(left + (slot % 3) * cellW + (cellW - iconPx) / 2f, top + (slot / 3) * pitch)
    fun iconRect(slot: Int) = Rect(iconTopLeft(slot), androidx.compose.ui.geometry.Size(iconPx, iconPx))
}

/**
 * A folder that is open underneath a running drag, as on iOS: either the drag
 * started inside it, or the app was held over its icon until it sprang open.
 * While it is open the app is arranged inside the folder; moving out of the
 * panel closes it and the app is carried on the home screen again.
 */
class FolderDragSession {
    var folderId by mutableStateOf<String?>(null)
    /** Preview order of the folder's apps, including the carried one. */
    var order by mutableStateOf<List<String>>(emptyList())
    var hoverIndex by mutableStateOf(-1)
    var outside by mutableStateOf(false)
    /** The finger has been inside the panel (or moved on) since the folder opened. */
    var armed = false
    /** The finger has actually been inside the panel. */
    var entered = false
    var openedAt = Offset.Zero
    /** -1 / 1 while the finger rests at the panel's left / right edge. */
    var edge by mutableStateOf(0)

    val active get() = folderId != null

    fun open(id: String, apps: List<String>, at: Offset, armed: Boolean) {
        order = apps; hoverIndex = -1; outside = false; this.armed = armed; entered = armed; openedAt = at; edge = 0
        folderId = id
    }

    fun clear() {
        folderId = null; hoverIndex = -1; outside = false; armed = false; entered = false; edge = 0
    }
}


/**
 * Dragging an app into a new place on the minimalist home screen. Page and dock
 * are one ordered list (the first apps sit in the dock), so every move – within
 * the page, within the dock or between the two – is a change of position in it.
 */
class MinimalDragState {
    var pkg by mutableStateOf<String?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    var grab by mutableStateOf(Offset.Zero)
    /** Preview order while dragging. */
    var order by mutableStateOf<List<String>?>(null)
    /** Centre of every position, taken when the drag starts (positions do not move, apps do). */
    var slots: List<Offset?> = emptyList()
    var hover by mutableStateOf(-1)
    var dropping by mutableStateOf(false)
    val dropAnim = Animatable(Offset.Zero, Offset.VectorConverter)

    val active get() = pkg != null

    fun clear() { pkg = null; order = null; slots = emptyList(); hover = -1; dropping = false }
}
