package com.tms.os.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.model.MinimalTemplate
import com.tms.os.launcher.ui.components.IosAppIcon
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.SfFamily
import com.tms.os.launcher.ui.theme.SfLightFamily
import com.tms.os.launcher.ui.widgets.MinimalShadow
import com.tms.os.launcher.ui.widgets.WidgetEnv
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ========================================================================== */
/*                         The minimalist home screen                         */
/* ========================================================================== */

/** Editing hooks of the minimalist page: wiggle mode, removing and dragging apps into a new order. */
class MinimalEdit(
    val jiggle: Boolean = false,
    /** The app hanging on the finger: its slot stays empty. */
    val hiddenPkg: String? = null,
    val onBounds: (String, Rect) -> Unit = { _, _ -> },
    val onRemove: (AppInfo) -> Unit = {},
    /** (package, top-left of what is picked up, finger-down position, current position) in root coordinates. */
    val onDragStart: (String, Offset, Offset, Offset) -> Unit = { _, _, _, _ -> },
    val onDragMove: (Offset) -> Unit = {},
    val onDragEnd: () -> Unit = {},
    val onEnterJiggle: () -> Unit = {},
    val onExitJiggle: () -> Unit = {}
)

/**
 * One calm page instead of the icon grid: the clock, and only the apps the
 * user picked. Plain layouts:
 *  - [MinimalTemplate.LIST]: the apps as a plain list of names;
 *  - [MinimalTemplate.ICONS]: icons without labels at the bottom;
 *  - [MinimalTemplate.FOCUS]: only the clock (the apps sit in the dock);
 * (The three iOS-style templates are not drawn here: they are a real,
 * freely editable home-screen layout.) Editing works as on the regular home screen: touch and hold, then drag an
 * app to a new place; the others make room.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalHomePage(
    template: MinimalTemplate,
    apps: List<AppInfo>,
    env: WidgetEnv,
    iconSize: Dp,
    bottomInset: Dp,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onClock: () -> Unit,
    onSetup: () -> Unit,
    labelFontSizeSp: Float = 12f,
    edit: MinimalEdit = MinimalEdit()
) {
    // Where each app on this page is, for picking it up.
    val rects = remember { mutableStateMapOf<String, Rect>() }
    val shown by rememberUpdatedState(apps.map { it.packageName })
    val editNow by rememberUpdatedState(edit)
    fun appAt(p: Offset): Pair<String, Rect>? =
        shown.firstNotNullOfOrNull { pkg -> rects[pkg]?.takeIf { it.inflate(it.height * 0.15f).contains(p) }?.let { pkg to it } }
    val report: (String, Rect) -> Unit = remember { { pkg, r -> rects[pkg] = r; editNow.onBounds(pkg, r) } }

    Box(
        Modifier
            .fillMaxSize()
            .combinedClickable(remember { MutableInteractionSource() }, null, onClick = { editNow.onExitJiggle() }, onLongClick = { editNow.onEnterJiggle() })
            // One gesture for the whole page (it never leaves the composition mid-drag).
            .homeDragGesture(
                key = "minimal",
                jiggle = edit.jiggle,
                enabled = true,
                canStart = { down -> appAt(down) != null },
                onStart = { down, at -> appAt(down)?.let { (pkg, r) -> editNow.onDragStart(pkg, r.topLeft, down, at) } },
                onMove = { editNow.onDragMove(it) },
                onEnd = { editNow.onDragEnd() }
            )
            .padding(bottom = bottomInset)
    ) {
        when (template) {
            MinimalTemplate.LIST -> Column(Modifier.fillMaxSize().padding(start = 30.dp, end = 24.dp, top = 30.dp)) {
                MinimalClock(env, centered = false, timeSize = 84, onClick = onClock)
                Spacer(Modifier.height(30.dp))
                if (apps.isEmpty()) EmptyHint(env.strings.minimalEmpty, TextAlign.Start, onSetup)
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(apps.take(template.maxApps), key = { it.packageName }) { app ->
                        MinimalAppName(app, edit, report, onAppClick, onAppLongClick, Modifier.animateItem(placementSpec = spring(dampingRatio = 0.8f, stiffness = 380f)))
                    }
                }
            }

            MinimalTemplate.ICONS -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(44.dp))
                MinimalClock(env, centered = true, timeSize = 96, onClick = onClock)
                if (apps.isEmpty()) { Spacer(Modifier.weight(1f)); EmptyHint(env.strings.minimalEmpty, TextAlign.Center, onSetup) }
                else LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.Bottom),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 20.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(apps.take(template.maxApps), key = { it.packageName }) { app ->
                        Box(Modifier.animateItem(placementSpec = spring(dampingRatio = 0.8f, stiffness = 380f)), contentAlignment = Alignment.TopCenter) {
                            AppCell(app, iconSize, labelFontSizeSp, false, edit, report, onAppClick, onAppLongClick)
                        }
                    }
                }
            }

            MinimalTemplate.FOCUS -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-70).dp)) {
                    MinimalClock(env, centered = true, timeSize = 108, onClick = onClock)
                    if (apps.isEmpty()) { Spacer(Modifier.height(26.dp)); EmptyHint(env.strings.minimalEmpty, TextAlign.Center, onSetup) }
                }
            }

            // The iOS-style templates are a real home-screen layout (grid, widgets, dock), not this page.
            else -> Unit
        }
    }
}

/** One app as an icon: reports where it is, wiggles in edit mode, hides while it is being carried. */
@Composable
private fun AppCell(
    app: AppInfo,
    iconSize: Dp,
    labelFontSizeSp: Float,
    showLabel: Boolean,
    edit: MinimalEdit,
    report: (String, Rect) -> Unit,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit
) {
    val iconPx = with(LocalDensity.current) { iconSize.toPx() }
    Box(
        Modifier
            .onGloballyPositioned { c ->
                val b = c.boundsInRoot()
                report(app.packageName, Rect(b.center.x - iconPx / 2f, b.top, b.center.x + iconPx / 2f, b.top + iconPx))
            }
            .graphicsLayer { alpha = if (app.packageName == edit.hiddenPkg) 0f else 1f }
    ) {
        IosAppIcon(
            app = app,
            iconSize = iconSize,
            labelFontSizeSp = labelFontSizeSp,
            showLabel = showLabel,
            showBadges = false,
            isJiggleMode = edit.jiggle,
            onClick = { onAppClick(app, Rect.Zero) },
            onClickWithBounds = { b -> onAppClick(app, b) },
            onLongClick = { onAppLongClick(app, Rect.Zero) },
            onLongClickWithBounds = { b -> if (!edit.jiggle) onAppLongClick(app, b) },
            onDeleteOrMenuClick = { edit.onRemove(app) }
        )
    }
}

@Composable
private fun MinimalClock(env: WidgetEnv, centered: Boolean, timeSize: Int, onClick: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    com.tms.os.launcher.ui.theme.WhileResumed {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    val locale = env.locale
    val time = SimpleDateFormat("HH:mm", locale).format(Date(now))
    val dayMonth = SimpleDateFormat(
        when (env.language) { AppLanguage.EN -> "EEEE, MMMM d"; AppLanguage.TR -> "EEEE, d MMMM"; else -> "EEEE, d. MMMM" }, locale
    ).format(Date(now))
    val info = "${env.t(env.tempC)}° ${env.city}  ·  ${env.strings.battery} ${env.batteryPercent.coerceIn(0, 100)} %"
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    ) {
        Text(
            time,
            style = TextStyle(
                fontFamily = SfLightFamily, fontWeight = FontWeight.Light, fontSize = timeSize.sp, lineHeight = (timeSize + 6).sp,
                letterSpacing = (-2.5).sp, color = Color.White, shadow = MinimalShadow
            ),
            maxLines = 1, softWrap = false
        )
        Text(dayMonth, style = minimalText(20, FontWeight.Medium, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(info, style = minimalText(14, FontWeight.Medium, Color.White.copy(alpha = 0.76f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun minimalText(size: Int, weight: FontWeight, color: Color) =
    TextStyle(fontFamily = SfFamily, fontWeight = weight, fontSize = size.sp, color = color, shadow = MinimalShadow)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalAppName(
    app: AppInfo,
    edit: MinimalEdit,
    report: (String, Rect) -> Unit,
    onClick: (AppInfo, Rect) -> Unit,
    onLongClick: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (pressed && !edit.jiggle) 0.45f else 1f, tween(if (pressed) 40 else 220), label = "press")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { this.alpha = if (app.packageName == edit.hiddenPkg) 0f else alpha }
    ) {
        if (edit.jiggle) {
            RemoveBadge(true, Modifier) { edit.onRemove(app) }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            app.displayLabel,
            style = minimalText(27, FontWeight.Normal, Color.White).copy(letterSpacing = (-0.3).sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .onGloballyPositioned { bounds = it.boundsInRoot(); report(app.packageName, bounds) }
                .jiggle(edit.jiggle, app.packageName.hashCode(), amplitude = 0.6f)
                .combinedClickable(
                    source, null,
                    onClick = { if (!edit.jiggle) onClick(app, bounds) },
                    onLongClick = { if (!edit.jiggle) onLongClick(app, bounds) }
                )
        )
    }
}

@Composable
private fun EmptyHint(text: String, align: TextAlign, onClick: () -> Unit) {
    Text(
        text,
        style = minimalText(16, FontWeight.Medium, Color.White.copy(alpha = 0.7f)).copy(textAlign = align),
        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(vertical = 12.dp)
    )
}

/* ========================================================================== */
/*                               Setup sheet                                  */
/* ========================================================================== */

private val Blue = Color(0xFF0A84FF)
private val Secondary = Color(0x99EBEBF5)
private const val MaxMinimalApps = 12

/**
 * Switches the minimalist home screen on and off, picks its layout and the
 * apps on it: a searchable list of every app, the chosen ones numbered in
 * the order they will appear.
 */
@Composable
fun MinimalSetupSheet(
    visible: Boolean,
    settings: LauncherSettings,
    allApps: List<AppInfo>,
    strings: HomeStrings,
    onSettingsChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onDismiss: () -> Unit,
    /** The apps on the iOS-style minimalist layout while it is shown; null = the plain templates' own list. */
    layoutApps: List<String>? = null,
    onToggleLayoutApp: (String) -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(140)), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0x80000000)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(
            visible,
            enter = slideInVertically(spring(dampingRatio = 0.88f, stiffness = 340f)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BackHandler(onBack = onDismiss)
            var query by remember { mutableStateOf("") }
            // iOS-style templates: the list shows (and changes) what is on that home screen, without a limit.
            val live = layoutApps != null
            val selected = layoutApps ?: settings.minimalApps
            val candidates = remember(allApps) { allApps.filter { !it.isHidden }.sortedBy { it.displayLabel.lowercase(Locale.getDefault()) } }
            val rows = remember(candidates, selected, query) {
                val q = query.trim()
                val byPkg = candidates.associateBy { it.packageName }
                val chosen = selected.mapNotNull { byPkg[it] }
                (chosen + candidates.filterNot { it.packageName in selected }).filter { q.isEmpty() || it.displayLabel.contains(q, ignoreCase = true) }
            }
            // Switching it on for the first time starts with the apps used most.
            fun withSuggestions(s: LauncherSettings): LauncherSettings =
                if (s.minimalApps.isNotEmpty()) s else s.copy(
                    minimalApps = allApps.filter { !it.isHidden && !it.packageName.contains("launcher") }
                        .sortedWith(compareByDescending<AppInfo> { it.effectivePriorityScore }.thenByDescending { it.lastLaunchedTime })
                        .take(6).map { it.packageName }
                )

            Column(
                Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.24f)))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 4.dp)) {
                    Text(strings.minimal, style = IosText.title2.copy(color = Color.White), modifier = Modifier.weight(1f))
                    Text(
                        strings.done,
                        style = IosText.body.copy(color = Blue, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), modifier = Modifier.weight(1f)) {
                    item(key = "#toggle") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2C2C2E))
                                .clickable { onSettingsChange { withSuggestions(it).copy(minimalMode = !it.minimalMode) } }
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Text(strings.minimalToggle, style = IosText.body.copy(color = Color.White), modifier = Modifier.weight(1f))
                            Switch(settings.minimalMode)
                        }
                        Text(strings.minimalHint, style = IosText.footnote.copy(color = Secondary), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                        Spacer(Modifier.height(22.dp))
                        listOf(
                            strings.templatesPlain to listOf(
                                MinimalTemplate.LIST to strings.templateList,
                                MinimalTemplate.ICONS to strings.templateIcons,
                                MinimalTemplate.FOCUS to strings.templateFocus
                            ),
                            strings.templatesIos to listOf(
                                MinimalTemplate.IOS_WIDGETS to strings.templateIosWidgets,
                                MinimalTemplate.IOS_LARGE to strings.templateIosLarge,
                                MinimalTemplate.IOS_BOTTOM to strings.templateIosBottom
                            )
                        ).forEachIndexed { g, (title, templates) ->
                            if (g > 0) Spacer(Modifier.height(18.dp))
                            SheetHeader("${strings.template} · $title", null)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                templates.forEach { (template, label) ->
                                    TemplateCard(template, label, settings.minimalTemplate == template, Modifier.weight(1f)) {
                                        onSettingsChange { it.copy(minimalTemplate = template) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        if (settings.minimalTemplate.ios) Text(strings.minimalIosHint, style = IosText.footnote.copy(color = Secondary), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp))
                        SheetHeader(strings.yourApps, if (live || settings.minimalTemplate.ios) selected.size.toString() else String.format(Locale.ROOT, strings.selectedOf, selected.size, MaxMinimalApps))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0x3D767680)).padding(horizontal = 9.dp)
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = Secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text(strings.searchApps, style = IosText.body.copy(color = Secondary))
                                BasicTextField(
                                    query, { query = it }, singleLine = true,
                                    textStyle = IosText.body.copy(color = Color.White), cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (query.isNotEmpty()) Icon(Icons.Rounded.Cancel, null, tint = Secondary, modifier = Modifier.size(18.dp).clickable { query = "" })
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    items(rows, key = { it.packageName }) { app ->
                        val index = selected.indexOf(app.packageName)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    if (live) onToggleLayoutApp(app.packageName)
                                    else onSettingsChange { s ->
                                        when {
                                            app.packageName in s.minimalApps -> s.copy(minimalApps = s.minimalApps - app.packageName)
                                            // Only the plain templates have a fixed number of places.
                                            !s.minimalTemplate.ios && s.minimalApps.size >= MaxMinimalApps -> s
                                            else -> s.copy(minimalApps = s.minimalApps + app.packageName)
                                        }
                                    }
                                }
                                .padding(vertical = 7.dp)
                        ) {
                            Box(Modifier.size(40.dp)) {
                                app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                            }
                            Spacer(Modifier.width(13.dp))
                            Text(app.displayLabel, style = IosText.body.copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .then(if (index >= 0) Modifier.background(Blue) else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                            ) {
                                if (index >= 0) Text((index + 1).toString(), style = IosText.footnote.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 9.dp), verticalAlignment = Alignment.Bottom) {
        Text(title.uppercase(Locale.getDefault()), style = IosText.footnote.copy(color = Secondary, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = IosText.footnote.copy(color = Secondary))
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val thumb by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.75f, stiffness = 500f), label = "thumb")
    val on by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "track")
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(CircleShape)
            .background(Color(0xFF39393D))
            .background(Color(0xFF30D158).copy(alpha = on))
    ) {
        Box(Modifier.offset(x = thumb, y = 2.dp).size(27.dp).clip(CircleShape).background(Color.White))
    }
}

/** Schematic preview of a layout: a bar for the clock, lines or dots for the apps. */
@Composable
private fun TemplateCard(template: MinimalTemplate, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ink = Color.White.copy(alpha = 0.9f)
    val soft = Color.White.copy(alpha = 0.45f)
    Column(modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.56f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0E0E10))
                .border(if (selected) 2.5.dp else 1.dp, if (selected) Blue else Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            when (template) {
                MinimalTemplate.LIST -> Column(Modifier.fillMaxSize()) {
                    Box(Modifier.width(44.dp).height(12.dp).clip(RoundedCornerShape(3.dp)).background(ink))
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(30.dp).height(4.dp).clip(CircleShape).background(soft))
                    Spacer(Modifier.height(16.dp))
                    listOf(46, 34, 52, 28, 40).forEach { w ->
                        Box(Modifier.width(w.dp).height(6.dp).clip(CircleShape).background(ink.copy(alpha = 0.75f)))
                        Spacer(Modifier.height(9.dp))
                    }
                }
                MinimalTemplate.ICONS -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(48.dp).height(13.dp).clip(RoundedCornerShape(3.dp)).background(ink))
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(soft))
                    Spacer(Modifier.weight(1f))
                    repeat(2) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            repeat(4) { Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.8f))) }
                        }
                    }
                }
                MinimalTemplate.IOS_WIDGETS, MinimalTemplate.IOS_LARGE, MinimalTemplate.IOS_BOTTOM -> Column(Modifier.fillMaxSize()) {
                    @Composable
                    fun Icons(n: Int) = Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(4) { i -> Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(if (i < n) ink.copy(alpha = 0.8f) else Color.Transparent)) }
                    }
                    when (template) {
                        MinimalTemplate.IOS_WIDGETS -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.22f)))
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF3B82D9)))
                            }
                            Spacer(Modifier.height(6.dp))
                            Icons(4); Icons(4)
                            Spacer(Modifier.weight(1f))
                        }
                        MinimalTemplate.IOS_LARGE -> {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(9.dp)).background(Color(0xFF3B82D9)))
                            Spacer(Modifier.height(6.dp))
                            Icons(4)
                            Spacer(Modifier.weight(1f))
                        }
                        else -> {
                            Spacer(Modifier.weight(1f))
                            Icons(4); Icons(4)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.12f)).padding(5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) { repeat(4) { Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.8f))) } }
                }
                MinimalTemplate.FOCUS -> Box(Modifier.fillMaxSize()) {
                    Column(Modifier.align(Alignment.Center).offset(y = (-14).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(54.dp).height(15.dp).clip(RoundedCornerShape(3.dp)).background(ink))
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(soft))
                    }
                    Row(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.12f)).padding(5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) { repeat(4) { Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.8f))) } }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(label, style = IosText.footnote.copy(color = if (selected) Color.White else Secondary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
    }
}
