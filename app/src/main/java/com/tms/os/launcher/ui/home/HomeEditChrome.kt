package com.tms.os.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.WidgetKind
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.IosMaterial
import com.tms.os.launcher.ui.theme.wallpaperGlass
import com.tms.os.launcher.ui.widgets.IosWidget
import com.tms.os.launcher.ui.widgets.IosSmartStack
import com.tms.os.launcher.ui.widgets.WeatherGlyph
import com.tms.os.launcher.ui.widgets.WidgetEnv
import com.tms.os.launcher.ui.widgets.canonicalHeight
import com.tms.os.launcher.ui.widgets.canonicalWidth

private val IosBlue = Color(0xFF0A84FF)
private val IosRed = Color(0xFFFF453A)

/* -------------------------------------------------------------------------- */
/*                          "Bearbeiten" / "Fertig" bar                       */
/* -------------------------------------------------------------------------- */

/**
 * iOS 18+ jiggle-mode chrome: a glass "Edit" pill that opens a small menu
 * (Add Widget, Customize) and a "Done" pill on the right.
 */
@Composable
fun IosEditTopBar(
    visible: Boolean,
    strings: HomeStrings,
    onAddWidget: () -> Unit,
    onCustomize: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (!visible && menuOpen) menuOpen = false
    Box(modifier.fillMaxWidth()) {
        // Tap-outside layer first, so it never changes where the pills sit.
        if (menuOpen) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2000.dp)
                    .clickable(remember { MutableInteractionSource() }, null) { menuOpen = false }
            )
        }
        Box(Modifier.fillMaxWidth().height(36.dp)) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(180)) + scaleIn(spring(0.7f, 500f), initialScale = 0.6f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.8f),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp)
            ) { GlassPill(strings.edit) { menuOpen = !menuOpen } }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(180)) + scaleIn(spring(0.7f, 500f), initialScale = 0.6f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.8f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp)
            ) { GlassPill(strings.done, bold = true, onClick = onDone) }
        }
        AnimatedVisibility(
            visible = menuOpen,
            enter = fadeIn(tween(140)) + scaleIn(spring(0.78f, 520f), initialScale = 0.3f, transformOrigin = TransformOrigin(0f, 0f)),
            exit = fadeOut(tween(110)) + scaleOut(tween(140), targetScale = 0.5f, transformOrigin = TransformOrigin(0f, 0f)),
            modifier = Modifier.padding(start = 18.dp, top = 44.dp)
        ) {
            Column(
                Modifier
                    .width(250.dp)
                    .shadow(24.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.2f))
                    .wallpaperGlass(RoundedCornerShape(22.dp), tint = IosMaterial.menu)
                    .padding(vertical = 6.dp)
            ) {
                MenuRow(strings.addWidget, Icons.Rounded.Widgets) { menuOpen = false; onAddWidget() }
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.15f)))
                MenuRow(strings.customize, Icons.Rounded.Palette) { menuOpen = false; onCustomize() }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = IosText.body.copy(color = Color.White))
    }
}

@Composable
fun GlassPill(text: String, bold: Boolean = false, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(34.dp)
            .wallpaperGlass(CircleShape, tint = IosMaterial.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Text(text, style = IosText.subheadline.copy(color = Color.White, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold))
    }
}

/** The grey "−" badge in the top-left corner of jiggling icons and widgets. */
@Composable
fun RemoveBadge(visible: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(0.55f, 600f)) + fadeIn(tween(90)),
        exit = scaleOut(tween(120)) + fadeOut(tween(120)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFE3E3E8))
                .clickable(onClick = onClick)
        ) {
            Box(Modifier.width(10.dp).height(2.dp).clip(CircleShape).background(Color(0xFF1C1C1E)))
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                  iOS alert                                 */
/* -------------------------------------------------------------------------- */

data class AlertAction(val title: String, val destructive: Boolean = false, val cancel: Boolean = false, val onClick: () -> Unit)

/** iOS 26 alert: glass card, centred text, capsule buttons. */
@Composable
fun IosAlert(visible: Boolean, title: String, message: String, actions: List<AlertAction>, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        BackHandler(onBack = onDismiss)
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f))
                .clickable(remember { MutableInteractionSource() }, null) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .animateEnterExit(enter = scaleIn(spring(0.75f, 420f), initialScale = 1.15f), exit = scaleOut(targetScale = 0.95f))
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.78f)
                    .shadow(30.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                    .wallpaperGlass(RoundedCornerShape(30.dp), tint = IosMaterial.menu)
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp)
            ) {
                Text(title, style = IosText.headline.copy(color = Color.White, textAlign = TextAlign.Center))
                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(message, style = IosText.footnote.copy(color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center))
                }
                Spacer(Modifier.height(18.dp))
                val horizontal = actions.size == 2
                if (horizontal) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    actions.forEach { AlertButton(it, Modifier.weight(1f)) }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { AlertButton(it, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun AlertButton(action: AlertAction, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(if (!action.cancel && !action.destructive) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = action.onClick)
    ) {
        Text(
            action.title,
            style = IosText.body.copy(
                color = if (action.destructive) IosRed else Color.White,
                fontWeight = if (action.cancel) FontWeight.Normal else FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

/* -------------------------------------------------------------------------- */
/*                                Widget gallery                              */
/* -------------------------------------------------------------------------- */

/**
 * iOS widget gallery: a tall sheet with a search field, featured widgets that
 * are added with one tap, and the list of widget apps. Choosing an app shows
 * its sizes in a horizontal pager with the blue "Add Widget" button.
 */
@Composable
fun IosWidgetGallery(
    visible: Boolean,
    env: WidgetEnv,
    strings: HomeStrings,
    onAdd: (WidgetKind, WidgetSize) -> Unit,
    onAddStack: (List<WidgetKind>, WidgetSize) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
        var detail by remember { mutableStateOf<WidgetKind?>(null) }
        var query by remember { mutableStateOf("") }
        BackHandler { if (detail != null) detail = null else onDismiss() }
        val haptic = LocalHapticFeedback.current
        val add: (WidgetKind, WidgetSize) -> Unit = { k, s -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAdd(k, s) }

        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)
        ) {
            Column(
                Modifier
                    .animateEnterExit(
                        enter = slideInVertically(spring(0.88f, 300f)) { it },
                        exit = slideOutVertically(tween(260)) { it }
                    )
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(top = 64.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(remember { MutableInteractionSource() }, null) {}
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                }
                AnimatedContent(
                    targetState = detail,
                    transitionSpec = {
                        if (targetState != null) slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                        else slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it }
                    },
                    label = "gallery"
                ) { kind ->
                    if (kind == null) GalleryHome(env, strings, query, { query = it }, add, { k, s -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAddStack(k, s) }, { detail = it }, onDismiss)
                    else GalleryDetail(kind, env, strings, { detail = null }, add)
                }
            }
        }
    }
}

@Composable
private fun GalleryHome(
    env: WidgetEnv,
    strings: HomeStrings,
    query: String,
    onQuery: (String) -> Unit,
    onAdd: (WidgetKind, WidgetSize) -> Unit,
    onAddStack: (List<WidgetKind>, WidgetSize) -> Unit,
    onOpen: (WidgetKind) -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x3D767680)).padding(horizontal = 10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color(0x99EBEBF5), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(strings.searchWidgets, style = IosText.body.copy(color = Color(0x99EBEBF5)))
                    BasicTextField(query, onQuery, singleLine = true, textStyle = IosText.body.copy(color = Color.White), cursorBrush = SolidColor(IosBlue), modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x3D767680)).clickable(onClick = onClose)
            ) { Icon(Icons.Rounded.Close, null, tint = Color(0xFFAEAEB2), modifier = Modifier.size(18.dp)) }
        }
        val kinds = WidgetKind.entries.filter { query.isBlank() || strings.widgetName(it).contains(query, ignoreCase = true) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (query.isBlank()) {
                Spacer(Modifier.height(10.dp))
                // Smart Stack first, as in the iOS gallery.
                val stackKinds = listOf(WidgetKind.WEATHER, WidgetKind.CALENDAR, WidgetKind.SUGGESTIONS, WidgetKind.CLOCK)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    IosSmartStack(
                        stackKinds, WidgetSize.MEDIUM, env,
                        Modifier.fillMaxWidth().height(150.dp).clickable { onAddStack(stackKinds, WidgetSize.MEDIUM) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Smart Stack", style = IosText.footnote.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold))
                }
                Spacer(Modifier.height(22.dp))
                FeaturedWidget(WidgetKind.WEATHER, WidgetSize.MEDIUM, env, strings, Modifier.fillMaxWidth(), onAdd)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    FeaturedWidget(WidgetKind.CALENDAR, WidgetSize.SMALL, env, strings, Modifier.weight(1f), onAdd)
                    FeaturedWidget(WidgetKind.CLOCK, WidgetSize.SMALL, env, strings, Modifier.weight(1f), onAdd)
                }
                Spacer(Modifier.height(22.dp))
                FeaturedWidget(WidgetKind.SUGGESTIONS, WidgetSize.MEDIUM, env, strings, Modifier.fillMaxWidth(), onAdd)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    FeaturedWidget(WidgetKind.BATTERY, WidgetSize.SMALL, env, strings, Modifier.weight(1f), onAdd)
                    FeaturedWidget(WidgetKind.NOTES, WidgetSize.SMALL, env, strings, Modifier.weight(1f), onAdd)
                }
                Spacer(Modifier.height(26.dp))
            }
            Column(Modifier.clip(RoundedCornerShape(26.dp)).background(Color(0xFF2C2C2E))) {
                kinds.forEachIndexed { i, kind ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(kind) }.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        KindTile(kind, Modifier.size(36.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(strings.widgetName(kind), style = IosText.body.copy(color = Color.White), modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0x4DEBEBF5), modifier = Modifier.size(20.dp))
                    }
                    if (i != kinds.lastIndex) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.12f)))
                }
            }
            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FeaturedWidget(kind: WidgetKind, size: WidgetSize, env: WidgetEnv, strings: HomeStrings, modifier: Modifier, onAdd: (WidgetKind, WidgetSize) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IosWidget(
            kind, size, env,
            Modifier
                .fillMaxWidth()
                .height(if (size == WidgetSize.SMALL) 150.dp else 150.dp)
                .clickable { pressed = true; onAdd(kind, size) }
        )
        Spacer(Modifier.height(8.dp))
        Text(strings.widgetName(kind), style = IosText.footnote.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun GalleryDetail(kind: WidgetKind, env: WidgetEnv, strings: HomeStrings, onBack: () -> Unit, onAdd: (WidgetKind, WidgetSize) -> Unit) {
    val sizes = kind.sizes
    val pager = rememberPagerState { sizes.size }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x3D767680)).clickable(onClick = onBack)
            ) { Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        }
        Text(strings.widgetName(kind), style = IosText.title1.copy(color = Color.White))
        Spacer(Modifier.height(8.dp))
        Text(
            strings.widgetDescription(kind),
            style = IosText.subheadline.copy(color = Color(0x99EBEBF5), textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        HorizontalPager(pager, modifier = Modifier.fillMaxWidth().weight(1f)) { i ->
            val s = sizes[i]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val w = if (s == WidgetSize.SMALL) 170.dp else 340.dp
                IosWidget(kind, s, env, Modifier.width(w).height(w * (s.canonicalHeight() / s.canonicalWidth())))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(sizes.size) { i ->
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (pager.currentPage == i) 1f else 0.3f)))
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(50.dp)
                .clip(CircleShape)
                .background(IosBlue)
                .clickable { onAdd(kind, sizes[pager.currentPage]) }
                .padding(horizontal = 28.dp)
        ) {
            Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.addWidget, style = IosText.headline.copy(color = Color.White))
        }
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** Small app-style tile for the widget list. */
@Composable
fun KindTile(kind: WidgetKind, modifier: Modifier) {
    val shape = RoundedCornerShape(9.dp)
    when (kind) {
        WidgetKind.WEATHER -> Box(modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(0xFF2B73D2), Color(0xFF5CA0EC)))), contentAlignment = Alignment.Center) {
            WeatherGlyph(2, false, Modifier.size(24.dp))
        }
        WidgetKind.CALENDAR -> Box(modifier.clip(shape).background(Color.White), contentAlignment = Alignment.Center) {
            Text(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH).toString(), style = IosText.title3.copy(color = Color.Black, fontWeight = FontWeight.Normal))
        }
        WidgetKind.CLOCK -> Box(modifier.clip(shape).background(Color.Black), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(26.dp)) {
                drawCircle(Color.White)
                drawLine(Color.Black, center, Offset(center.x, size.height * 0.2f), strokeWidth = size.width * 0.08f)
                drawLine(Color.Black, center, Offset(size.width * 0.72f, center.y), strokeWidth = size.width * 0.08f)
            }
        }
        WidgetKind.BATTERY -> Box(modifier.clip(shape).background(Color(0xFF34C759)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(22.dp, 12.dp)) {
                drawRoundRect(Color.White, size = size.copy(width = size.width * 0.9f), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                drawRoundRect(Color.White, topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()), size = size.copy(width = size.width * 0.55f, height = size.height - 5.dp.toPx()), cornerRadius = CornerRadius(1.5.dp.toPx()))
            }
        }
        WidgetKind.SUGGESTIONS -> Box(
            modifier.clip(shape).background(Brush.linearGradient(listOf(Color(0xFF5E5CE6), Color(0xFFBF5AF2), Color(0xFFFF375F))))
        )
        WidgetKind.NOTES -> Box(modifier.clip(shape).background(Color.White)) {
            Box(Modifier.fillMaxWidth().height(9.dp).background(Color(0xFFFFCC00)))
            Column(Modifier.offset(y = 13.dp).padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x33000000))) }
            }
        }
    }
}
