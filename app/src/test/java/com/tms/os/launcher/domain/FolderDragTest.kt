package com.tms.os.launcher.domain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.ui.home.FolderSlots
import com.tms.os.launcher.ui.home.withoutFolderApp
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderDragTest {
    private val a = HomeGridItem.AppItem("a")
    private fun folder(id: String, vararg apps: String) = HomeGridItem.FolderItem(id, id, apps.toList())

    @Test
    fun `an app carried out of a folder is no longer listed in it`() {
        val list = listOf(a, folder("f", "x", "y"), folder("g", "z"))
        assertEquals(listOf(a, folder("f", "y"), folder("g", "z")), list.withoutFolderApp("x"))
    }

    @Test
    fun `a folder left empty disappears unless the app is being arranged inside it`() {
        val list = listOf(a, folder("f", "x"))
        assertEquals(listOf<HomeGridItem>(a), list.withoutFolderApp("x"))
        assertEquals(listOf(a, folder("f")), list.withoutFolderApp("x", keep = "f"))
    }

    @Test
    fun `folder slots map a finger position to the 3x3 grid and back`() {
        // Panel 340x346 at (100, 500); 10/22 padding, 60 px icons, 80 px rows.
        val slots = FolderSlots(Rect(100f, 500f, 440f, 846f), 10f, 22f, 60f, 80f)
        for (slot in 0..8) assertEquals(slot, slots.slotAt(slots.iconRect(slot).center))
        assertEquals(0, slots.slotAt(Offset(0f, 0f)))
        assertEquals(8, slots.slotAt(Offset(2000f, 2000f)))
    }
}
