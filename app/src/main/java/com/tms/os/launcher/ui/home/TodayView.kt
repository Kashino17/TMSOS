package com.tms.os.launcher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.widgets.IosWidget
import com.tms.os.launcher.ui.widgets.IosSmartStack
import com.tms.os.launcher.ui.widgets.WidgetEnv

/**
 * The iOS Today View: swipe right from the first home page. A quiet column of
 * widgets over the darkened wallpaper (small widgets pair up side by side).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayView(
    widgets: List<HomeGridItem.WidgetItem>,
    env: WidgetEnv,
    strings: HomeStrings,
    jiggle: Boolean,
    onWidgetTap: (HomeGridItem.WidgetItem) -> Unit,
    onWidgetSettings: (HomeGridItem.WidgetItem) -> Unit = {},
    onRemove: (HomeGridItem.WidgetItem) -> Unit,
    onEnterJiggle: () -> Unit,
    onExitJiggle: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val edge = 22.dp
        val gap = 30.dp
        // On the unfolded display the widgets keep phone width in a left column, like iPadOS.
        val full = minOf(maxWidth - edge * 2, 368.dp)
        val small = (full - gap) / 2
        Column(
            Modifier
                .fillMaxSize()
                .combinedClickable(remember { MutableInteractionSource() }, null, onClick = { if (jiggle) onExitJiggle() }, onLongClick = onEnterJiggle)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = edge)
        ) {
            Spacer(Modifier.height(9.dp))

            // Pair consecutive small widgets, full-width otherwise.
            val rows = mutableListOf<List<HomeGridItem.WidgetItem>>()
            var i = 0
            while (i < widgets.size) {
                val w = widgets[i]
                val next = widgets.getOrNull(i + 1)
                if (w.size == WidgetSize.SMALL && next?.size == WidgetSize.SMALL) { rows += listOf(w, next); i += 2 }
                else { rows += listOf(w); i++ }
            }
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.width(full)) {
                    row.forEach { w ->
                        val width: Dp = if (w.size == WidgetSize.SMALL) small else full
                        val height: Dp = when (w.size) { WidgetSize.LARGE -> small * 2 + gap; else -> small }
                        Box(Modifier.size(width, height).jiggle(jiggle, w.widgetId.hashCode(), 0.7f)) {
                            val surface = Modifier.fillMaxSize().combinedClickable(
                                remember { MutableInteractionSource() }, null,
                                // Settings only while editing; otherwise a tap does what the widget does.
                                onClick = { if (jiggle) { if (w.kind.configurable) onWidgetSettings(w) } else onWidgetTap(w) },
                                onLongClick = onEnterJiggle
                            )
                            if (w.isStack) IosSmartStack(w.kinds, w.size, env, surface)
                            else IosWidget(w.kind, w.size, env, surface, config = w.config)
                            RemoveBadge(jiggle, Modifier.offset((-8).dp, (-8).dp)) { onRemove(w) }
                        }
                    }
                }
                Spacer(Modifier.height(gap))
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}
