package com.tms.os.launcher.domain

import kotlin.math.floor

/**
 * @param sidePadding horizontal padding of the grid so that, with icons centred in
 * equal cells, the outer icon edge lands where SpringBoard puts it (~7 % of width).
 */
data class HomeLayout(val columns: Int, val rows: Int, val iconSize: Float, val sidePadding: Float = 16f)

/** iOS: 64 pt icons on a 393 pt wide iPhone, outer icon edge 27 pt from the screen edge. */
private const val IOS_ICON = 64f
private const val IOS_EDGE_RATIO = 27f / 393f

/** Keep labels and icons inside their cells even on a narrow Fold cover. */
fun calculateHomeLayout(
    width: Float,
    height: Float,
    columns: Int,
    requestedRows: Int,
    iconScale: Float,
    labelSize: Float,
    fontScale: Float
): HomeLayout {
    val safeColumns = minOf(columns.coerceIn(4, 8), floor((width - 32f) / 44f).toInt().coerceAtLeast(4))
    val cellWidth = (width - 32f).coerceAtLeast(1f) / safeColumns
    val icon = minOf(IOS_ICON * iconScale.coerceIn(0.75f, 1.25f), cellWidth - 14f).coerceAtLeast(28f)
    val cellHeight = icon + labelSize.coerceIn(10f, 18f) * fontScale.coerceAtLeast(1f) + 18f
    val rowsThatFit = floor((height - 226f).coerceAtLeast(cellHeight) / cellHeight).toInt()

    // Solve  edge = p + ((width - 2p) / c - icon) / 2  for p.
    val c = safeColumns.toFloat()
    val edge = width * IOS_EDGE_RATIO
    val side = ((edge - width / (2f * c) + icon / 2f) / (1f - 1f / c)).coerceIn(4f, 16f)

    return HomeLayout(safeColumns, minOf(requestedRows.coerceIn(3, 7), rowsThatFit.coerceAtLeast(2)), icon, side)
}

/** The list is the persisted order; sorting stale page/position fields undoes a drag. */
fun <T> paginateHomeItems(items: List<T>, firstPageCapacity: Int, pageCapacity: Int): List<List<T>> {
    if (items.isEmpty()) return listOf(emptyList())
    val first = firstPageCapacity.coerceAtLeast(1)
    return listOf(items.take(first)) + items.drop(first).chunked(pageCapacity.coerceAtLeast(1))
}
