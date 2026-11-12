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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.wallpaperGlass
import com.tms.os.launcher.ui.widgets.IosWidget
import com.tms.os.launcher.ui.widgets.IosSmartStack
import com.tms.os.launcher.ui.widgets.WidgetEnv

/**
 * The iOS Today View: swipe right from the first home page. A glass search
 * field on top, then the widget column (small widgets pair up side by side).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayView(
    widgets: List<HomeGridItem.WidgetItem>,
    env: WidgetEnv,
    strings: HomeStrings,
    jiggle: Boolean,
    onSearch: () -> Unit,
    onWidgetTap: (HomeGridItem.WidgetItem) -> Unit,
    onRemove: (HomeGridItem.WidgetItem) -> Unit,
    onEnterJiggle: () -> Unit,
    onExitJiggle: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val edge = maxWidth * (27f / 393f)
        val gap = 20.dp
        val full = maxWidth - edge * 2
        val small = (full - gap) / 2
        Column(
            Modifier
                .fillMaxSize()
                .combinedClickable(remember { MutableInteractionSource() }, null, onClick = { if (jiggle) onExitJiggle() }, onLongClick = onEnterJiggle)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = edge)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .wallpaperGlass(RoundedCornerShape(19.dp), tint = Color.White.copy(alpha = 0.08f), rim = 0.45f)
                    .combinedClickable(onClick = onSearch)
                    .padding(horizontal = 12.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.search, style = IosText.body.copy(color = Color.White.copy(alpha = 0.75f)), modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Mic, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(gap))

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
                Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { w ->
                        val width: Dp = if (w.size == WidgetSize.SMALL) small else full
                        val height: Dp = when (w.size) { WidgetSize.LARGE -> small * 2 + gap; else -> small }
                        Box(Modifier.size(width, height).jiggle(jiggle, w.widgetId.hashCode(), 0.7f)) {
                            val surface = Modifier.fillMaxSize().combinedClickable(
                                remember { MutableInteractionSource() }, null,
                                onClick = { if (!jiggle) onWidgetTap(w) },
                                onLongClick = onEnterJiggle
                            )
                            if (w.isStack) IosSmartStack(w.kinds, w.size, env, surface)
                            else IosWidget(w.kind, w.size, env, surface)
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
