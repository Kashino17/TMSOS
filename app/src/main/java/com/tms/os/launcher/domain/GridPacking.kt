package com.tms.os.launcher.domain

/** Cell footprint of one home-screen item. Icons and folders are 1x1. */
data class CellSpan(val cols: Int, val rows: Int)

/** An item placed on a page at a top-left cell. */
data class PlacedCell<T>(val item: T, val page: Int, val col: Int, val row: Int, val span: CellSpan)

/**
 * SpringBoard-style flow layout. Items keep their persisted order and are put
 * into the first free cell (row-major) that fits their footprint, so icons flow
 * around widgets. When an item no longer fits, a new page starts and the rest
 * of the order continues there, which is exactly what iOS does after a widget
 * is dropped onto a full page.
 *
 * @param rowsForPage rows available on a given page (page 0 can be shorter).
 */
fun <T> packIntoPages(
    items: List<T>,
    columns: Int,
    rowsForPage: (Int) -> Int,
    spanOf: (T) -> CellSpan
): List<List<PlacedCell<T>>> {
    val cols = columns.coerceAtLeast(1)
    val pages = mutableListOf<MutableList<PlacedCell<T>>>()
    var page = 0
    var rows = rowsForPage(0).coerceAtLeast(1)
    var occupied = Array(rows) { BooleanArray(cols) }
    pages.add(mutableListOf())
    // Once an item is placed, later items may not jump back above it: iOS keeps
    // reading order, so we only search from the last placed cell onward for 1x1
    // items, while multi-cell widgets search from the top to find a gap.
    var cursor = 0

    fun fits(r: Int, c: Int, span: CellSpan): Boolean {
        if (c + span.cols > cols || r + span.rows > rows) return false
        for (dr in 0 until span.rows) for (dc in 0 until span.cols) if (occupied[r + dr][c + dc]) return false
        return true
    }

    for (item in items) {
        val raw = spanOf(item)
        val span = CellSpan(raw.cols.coerceIn(1, cols), raw.rows.coerceIn(1, rows))
        var placed = false
        while (!placed) {
            val start = if (span.cols == 1 && span.rows == 1) cursor else cursor - cursor % cols
            var cell = start
            while (cell < rows * cols) {
                val r = cell / cols
                val c = cell % cols
                if (fits(r, c, span)) {
                    for (dr in 0 until span.rows) for (dc in 0 until span.cols) occupied[r + dr][c + dc] = true
                    pages[page].add(PlacedCell(item, page, c, r, span))
                    cursor = cell + if (span.cols == 1) 1 else span.cols
                    placed = true
                    break
                }
                cell++
            }
            if (!placed) {
                page++
                rows = rowsForPage(page).coerceAtLeast(1)
                occupied = Array(rows) { BooleanArray(cols) }
                pages.add(mutableListOf())
                cursor = 0
            }
        }
    }
    return pages
}

/* ------------------------------------------------------------------------ */
/*                    Free placement (iOS 18 home screen)                   */
/* ------------------------------------------------------------------------ */

/** A fixed top-left cell: the item stays exactly there, with free cells around it allowed. */
data class GridAnchor(val page: Int, val col: Int, val row: Int)

/**
 * Like [packIntoPages], but items with an [anchorOf] stay at their cell, leaving
 * gaps wherever the user left them. Items without one (new apps, a widget just
 * added) flow into the first free cells after the last anchored item, in their
 * list order. Without any anchors the result is exactly [packIntoPages].
 * An anchor that does not fit this grid (fewer columns, taken cell) is ignored
 * for that item, which then flows.
 */
fun <T> packAnchored(
    items: List<T>,
    columns: Int,
    rowsForPage: (Int) -> Int,
    spanOf: (T) -> CellSpan,
    anchorOf: (T) -> GridAnchor?
): List<List<PlacedCell<T>>> {
    val cols = columns.coerceAtLeast(1)
    val pages = mutableListOf<MutableList<PlacedCell<T>>>()
    val occupied = mutableListOf<Array<BooleanArray>>()
    fun ensure(page: Int) {
        while (pages.size <= page) {
            pages.add(mutableListOf())
            occupied.add(Array(rowsForPage(pages.size - 1).coerceAtLeast(1)) { BooleanArray(cols) })
        }
    }
    fun rowsOf(page: Int) = occupied[page].size
    fun fits(page: Int, r: Int, c: Int, span: CellSpan): Boolean {
        if (c < 0 || r < 0 || c + span.cols > cols || r + span.rows > rowsOf(page)) return false
        for (dr in 0 until span.rows) for (dc in 0 until span.cols) if (occupied[page][r + dr][c + dc]) return false
        return true
    }
    fun take(item: T, page: Int, r: Int, c: Int, span: CellSpan) {
        for (dr in 0 until span.rows) for (dc in 0 until span.cols) occupied[page][r + dr][c + dc] = true
        pages[page].add(PlacedCell(item, page, c, r, span))
    }
    fun spanFor(item: T, page: Int) = spanOf(item).let { CellSpan(it.cols.coerceIn(1, cols), it.rows.coerceIn(1, rowsForPage(page).coerceAtLeast(1))) }

    ensure(0)
    val flowing = mutableListOf<T>()
    var lastPage = 0
    var lastCell = -1
    for (item in items) {
        val a = anchorOf(item)
        if (a == null || a.page < 0 || a.page > 200) { flowing += item; continue }
        ensure(a.page)
        val span = spanFor(item, a.page)
        if (!fits(a.page, a.row, a.col, span)) { flowing += item; continue }
        take(item, a.page, a.row, a.col, span)
        val end = (a.row + span.rows - 1) * cols + a.col + span.cols - 1
        if (a.page > lastPage || (a.page == lastPage && end > lastCell)) { lastPage = a.page; lastCell = end }
    }

    // Unanchored items continue after the last anchored one, never into the user's gaps above it.
    var page = lastPage
    var cursor = lastCell + 1
    for (item in flowing) {
        while (true) {
            ensure(page)
            val span = spanFor(item, page)
            val rows = rowsOf(page)
            val start = if (span.cols == 1 && span.rows == 1) cursor else cursor - cursor % cols
            var placed = false
            var cell = start.coerceAtLeast(0)
            while (cell < rows * cols) {
                val r = cell / cols
                val c = cell % cols
                if (fits(page, r, c, span)) {
                    take(item, page, r, c, span)
                    cursor = cell + if (span.cols == 1) 1 else span.cols
                    placed = true
                    break
                }
                cell++
            }
            if (placed) break
            page++
            cursor = 0
        }
    }
    // Trailing pages left empty (an anchor that moved away) are not shown.
    while (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.size - 1)
    return pages
}

/**
 * The anchors after dropping [dragged] at [target], as on iOS 18:
 *  - every item keeps the cell it is shown in now (the layout gets "frozen");
 *  - a free target just takes the item, nothing else moves;
 *  - an icon on the target is pushed one cell along – towards the place the
 *    dragged item came from when that is nearer, forward otherwise – and pushes
 *    the next one in turn until a free cell absorbs the chain (on to the next
 *    page if needed);
 *  - an icon cannot be dropped onto a widget, a widget not onto another widget:
 *    then null (nothing changes).
 */
fun <T> moveAnchored(
    pages: List<List<PlacedCell<T>>>,
    idOf: (T) -> String,
    dragged: T,
    target: GridAnchor,
    columns: Int,
    rowsForPage: (Int) -> Int,
    spanOf: (T) -> CellSpan
): Map<String, GridAnchor>? {
    val cols = columns.coerceAtLeast(1)
    val draggedId = idOf(dragged)
    val placed = pages.flatten().filter { idOf(it.item) != draggedId }
    val origin = pages.flatten().firstOrNull { idOf(it.item) == draggedId }
    val anchors = LinkedHashMap<String, GridAnchor>()
    placed.forEach { anchors[idOf(it.item)] = GridAnchor(it.page, it.col, it.row) }
    val spans = HashMap<String, CellSpan>()
    placed.forEach { spans[idOf(it.item)] = it.span }

    val rows = rowsForPage(target.page).coerceAtLeast(1)
    val raw = spanOf(dragged)
    val span = CellSpan(raw.cols.coerceIn(1, cols), raw.rows.coerceIn(1, rows))
    val col = target.col.coerceIn(0, cols - span.cols)
    val row = target.row.coerceIn(0, rows - span.rows)
    fun inRegion(c: Int, r: Int) = c in col until col + span.cols && r in row until row + span.rows

    // Who stands on the target region now.
    val covered = placed.filter { p ->
        p.page == target.page && (0 until p.span.rows).any { dr -> (0 until p.span.cols).any { dc -> inRegion(p.col + dc, p.row + dr) } }
    }
    val dragIsIcon = span.cols == 1 && span.rows == 1
    if (covered.any { it.span.cols > 1 || it.span.rows > 1 }) return null

    anchors[draggedId] = GridAnchor(target.page, col, row)
    if (covered.isEmpty()) return anchors

    // Cells taken by something that stays put (widgets, the dragged item) – a chain skips them.
    fun blocked(page: Int, c: Int, r: Int): Boolean {
        if (page == target.page && inRegion(c, r)) return true
        return anchors.any { (id, a) ->
            val s = if (id == draggedId) span else spans[id] ?: CellSpan(1, 1)
            (s.cols > 1 || s.rows > 1) && a.page == page && c in a.col until a.col + s.cols && r in a.row until a.row + s.rows
        }
    }
    fun iconAt(page: Int, c: Int, r: Int): String? = anchors.entries.firstOrNull { (id, a) ->
        id != draggedId && (spans[id] ?: CellSpan(1, 1)).let { it.cols == 1 && it.rows == 1 } && a.page == page && a.col == c && a.row == r
    }?.key

    val queue = ArrayDeque(covered.sortedBy { it.row * cols + it.col }.map { idOf(it.item) })
    queue.forEach { anchors.remove(it) }

    // Towards the vacated cell when it is on this page before the target.
    val start = row * cols + col
    val originCell = origin?.takeIf { it.page == target.page }?.let { it.row * cols + it.col }
    val backward = dragIsIcon && originCell != null && originCell < start

    fun walk(page: Int, cells: IntProgression): Boolean {
        for (cell in cells) {
            if (queue.isEmpty()) return true
            val c = cell % cols
            val r = cell / cols
            if (blocked(page, c, r)) continue
            val there = iconAt(page, c, r)
            if (there != null) { anchors.remove(there); queue.addLast(there) }
            anchors[queue.removeFirst()] = GridAnchor(page, c, r)
        }
        return queue.isEmpty()
    }
    if (backward) {
        // Backwards ends at the vacated cell, which is free: the chain always closes there.
        walk(target.page, (start - 1) downTo originCell!!)
    }
    var page = target.page
    var from = start + 1
    var guard = 0
    while (queue.isNotEmpty() && guard++ < 60) {
        val pageRows = rowsForPage(page).coerceAtLeast(1)
        walk(page, from until pageRows * cols)
        page++
        from = 0
    }
    return anchors
}
