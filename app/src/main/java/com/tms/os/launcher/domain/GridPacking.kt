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
