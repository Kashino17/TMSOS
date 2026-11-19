package com.tms.os.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreePlacementTest {
    private val icon = CellSpan(1, 1)
    private val widget = CellSpan(2, 2)
    private fun span(id: String) = if (id.startsWith("w")) widget else icon
    private fun pack(items: List<String>, anchors: Map<String, GridAnchor>) =
        packAnchored(items, 4, { 6 }, ::span) { anchors[it] }
    private fun where(pages: List<List<PlacedCell<String>>>) = pages.flatten().associate { it.item to GridAnchor(it.page, it.col, it.row) }

    @Test
    fun `without anchors it is the plain flow layout`() {
        val items = listOf("a", "w1", "b", "c", "d")
        assertEquals(where(packIntoPages(items, 4, { 6 }, ::span)), where(pack(items, emptyMap())))
    }

    @Test
    fun `anchored items keep their cell and leave gaps`() {
        val pages = pack(listOf("a", "b"), mapOf("a" to GridAnchor(0, 0, 5), "b" to GridAnchor(0, 3, 2)))
        assertEquals(GridAnchor(0, 0, 5), where(pages)["a"])
        assertEquals(GridAnchor(0, 3, 2), where(pages)["b"])
    }

    @Test
    fun `new items flow after the last anchored one, not into the gaps above`() {
        val pages = pack(listOf("a", "new"), mapOf("a" to GridAnchor(0, 1, 4)))
        assertEquals(GridAnchor(0, 2, 4), where(pages)["new"])
    }

    @Test
    fun `an anchor that does not fit flows instead`() {
        val pages = pack(listOf("a", "b"), mapOf("a" to GridAnchor(0, 0, 0), "b" to GridAnchor(0, 0, 0)))
        assertEquals(GridAnchor(0, 1, 0), where(pages)["b"])
    }

    @Test
    fun `dropping on a free cell moves nothing else`() {
        val pages = pack(listOf("a", "b", "c"), emptyMap())
        val anchors = moveAnchored(pages, { it }, "a", GridAnchor(0, 2, 5), 4, { 6 }, ::span)!!
        assertEquals(GridAnchor(0, 2, 5), anchors["a"])
        assertEquals(GridAnchor(0, 1, 0), anchors["b"])
        assertEquals(GridAnchor(0, 2, 0), anchors["c"])
    }

    @Test
    fun `dropping on an icon pushes it towards the vacated cell`() {
        val pages = pack(listOf("a", "b", "c", "d"), emptyMap())
        val anchors = moveAnchored(pages, { it }, "a", GridAnchor(0, 2, 0), 4, { 6 }, ::span)!!
        assertEquals(GridAnchor(0, 2, 0), anchors["a"])
        assertEquals(GridAnchor(0, 1, 0), anchors["c"])
        assertEquals(GridAnchor(0, 0, 0), anchors["b"])
        assertEquals(GridAnchor(0, 3, 0), anchors["d"])
    }

    @Test
    fun `dropping on an icon pushes the chain forward into the next gap`() {
        val pages = pack(listOf("a", "b", "c", "d"), mapOf("a" to GridAnchor(0, 0, 0), "b" to GridAnchor(0, 1, 0), "c" to GridAnchor(0, 2, 0), "d" to GridAnchor(0, 0, 3)))
        val anchors = moveAnchored(pages, { it }, "d", GridAnchor(0, 0, 0), 4, { 6 }, ::span)!!
        assertEquals(GridAnchor(0, 0, 0), anchors["d"])
        assertEquals(GridAnchor(0, 1, 0), anchors["a"])
        assertEquals(GridAnchor(0, 2, 0), anchors["b"])
        assertEquals(GridAnchor(0, 3, 0), anchors["c"])
    }

    @Test
    fun `an icon cannot land on a widget`() {
        val pages = pack(listOf("w1", "a"), emptyMap())
        assertNull(moveAnchored(pages, { it }, "a", GridAnchor(0, 1, 1), 4, { 6 }, ::span))
    }

    @Test
    fun `a widget pushes the icons it covers out of its way`() {
        val pages = pack(listOf("a", "b", "w1"), mapOf("a" to GridAnchor(0, 0, 3), "b" to GridAnchor(0, 1, 3), "w1" to GridAnchor(0, 0, 0)))
        val anchors = moveAnchored(pages, { it }, "w1", GridAnchor(0, 0, 3), 4, { 6 }, ::span)!!
        assertEquals(GridAnchor(0, 0, 3), anchors["w1"])
        val icons = setOf(anchors["a"], anchors["b"])
        assertEquals(setOf(GridAnchor(0, 2, 3), GridAnchor(0, 3, 3)), icons)
    }
}
