package com.tms.os.launcher.domain

import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.ui.home.Hover
import com.tms.os.launcher.ui.home.reorderFor
import com.tms.os.launcher.ui.home.span
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    private fun app(n: String) = HomeGridItem.AppItem(n)
    private val items = listOf("a", "b", "c", "d", "e").map(::app)
    private fun pages(order: List<HomeGridItem>) = packIntoPages(order, 4, { 6 }) { it.span() }
    private fun names(l: List<HomeGridItem>) = l.map { (it as HomeGridItem.AppItem).packageName }

    @Test fun draggingForwardTakesTheHoveredSlot() {
        // A over C: iOS gives B C A D E (A lands where C was).
        val r = reorderFor(items, items[0], Hover(0, 2, 0, "app:c", false), pages(items))
        assertEquals(listOf("b", "c", "a", "d", "e"), names(r))
    }

    @Test fun draggingBackwardTakesTheHoveredSlot() {
        val r = reorderFor(items, items[3], Hover(0, 1, 0, "app:b", false), pages(items))
        assertEquals(listOf("a", "d", "b", "c", "e"), names(r))
    }

    @Test fun emptyCellAppendsToEndOfPage() {
        val r = reorderFor(items, items[0], Hover(0, 3, 4, null, false), pages(items))
        assertEquals(listOf("b", "c", "d", "e", "a"), names(r))
    }
}
