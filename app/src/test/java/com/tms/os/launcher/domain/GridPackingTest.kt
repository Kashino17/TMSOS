package com.tms.os.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GridPackingTest {
    private val icon = CellSpan(1, 1)
    private fun span(s: String) = when (s[0]) { 'S' -> CellSpan(2, 2); 'M' -> CellSpan(4, 2); 'L' -> CellSpan(4, 4); else -> icon }

    @Test fun iconsFlowAroundSmallWidget() {
        // S at top-left, then 6 icons: two fill the right half of rows 0-1, the rest continue on row 2.
        val items = listOf("S") + (1..6).map { "a$it" }
        val page = packIntoPages(items, 4, { 6 }, ::span).single()
        val pos = page.associate { it.item to (it.col to it.row) }
        assertEquals(0 to 0, pos["S"])
        assertEquals(2 to 0, pos["a1"]); assertEquals(3 to 0, pos["a2"])
        assertEquals(2 to 1, pos["a3"]); assertEquals(3 to 1, pos["a4"])
        assertEquals(0 to 2, pos["a5"]); assertEquals(1 to 2, pos["a6"])
    }

    @Test fun widgetThatDoesNotFitStartsNextPageAndKeepsOrder() {
        val items = (1..22).map { "a$it" } + listOf("M", "b")
        val pages = packIntoPages(items, 4, { 6 }, ::span)
        assertEquals(2, pages.size)
        assertEquals(listOf("M", "b"), pages[1].map { it.item })
        assertEquals(0, pages[1][0].row)
        assertEquals(items, pages.flatten().map { it.item })
    }

    @Test fun shorterFirstPageIsRespected() {
        val pages = packIntoPages((1..10).map { "a$it" }, 4, { if (it == 0) 2 else 6 }, ::span)
        assertEquals(8, pages[0].size)
        assertEquals(2, pages[1].size)
    }

    @Test fun emptyListYieldsOneEmptyPage() {
        assertEquals(listOf(emptyList<PlacedCell<String>>()), packIntoPages(emptyList<String>(), 4, { 6 }, ::span))
    }
}
