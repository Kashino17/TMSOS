package com.tms.os.launcher.domain

import org.junit.Assert.*
import org.junit.Test

class HomeLayoutTest {
    @Test fun narrowCoverKeepsEveryIconInsideItsCell() {
        for (width in listOf(280f, 320f, 360f, 412f)) {
            for (columns in 4..6) {
                val layout = calculateHomeLayout(width, 780f, columns, 7, 1.25f, 12f, 1f)
                assertTrue(layout.iconSize + 14f <= (width - 32f) / layout.columns)
                assertTrue(layout.rows * (layout.iconSize + 30f) <= 780f - 226f)
            }
        }
    }

    @Test fun largeTextAndLandscapeReduceRowsInsteadOfOverlappingTheDock() {
        val normal = calculateHomeLayout(390f, 844f, 4, 6, 1f, 12f, 1f)
        val enlarged = calculateHomeLayout(390f, 640f, 4, 6, 1f, 16f, 1.5f)
        val landscape = calculateHomeLayout(720f, 480f, 6, 5, 1f, 12f, 1f)
        assertEquals(6, normal.rows)
        assertTrue(enlarged.rows < normal.rows)
        assertTrue(landscape.rows <= 3)
    }

    @Test fun paginationPreservesDraggedOrderAcrossWidgetCapacityAndFoldChanges() {
        val moved = (0 until 67).toMutableList().apply { add(22, removeAt(2)) }
        for ((first, rest) in listOf(16 to 24, 24 to 24, 18 to 30, 12 to 20)) {
            val pages = paginateHomeItems(moved, first, rest)
            assertEquals(moved, pages.flatten())
            assertEquals(first, pages.first().size)
            assertTrue(pages.drop(1).all { it.size <= rest })
        }
    }

    @Test fun emptyAndInvalidCapacityStillProduceReachablePages() {
        assertEquals(listOf(emptyList<Int>()), paginateHomeItems(emptyList<Int>(), 16, 24))
        assertEquals(listOf(listOf(1), listOf(2)), paginateHomeItems(listOf(1, 2), 0, 0))
    }
}
