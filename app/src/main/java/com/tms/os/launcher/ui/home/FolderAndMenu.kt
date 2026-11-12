package com.tms.os.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.ui.components.IosAppIcon
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.wallpaperGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/* ========================================================================== */
/*                                 Open folder                                */
/* ========================================================================== */

/**
 * iOS folder: the folder icon grows into a large frosted panel (3×3 apps per
 * page), the title floats above it, nothing else. Tapping outside shrinks the
 * panel back into the icon. In edit mode the title becomes an editable field
 * and the apps jiggle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosFolderView(
    folder: HomeGridItem.FolderItem,
    appMap: Map<String, AppInfo>,
    iconBounds: Rect?,
    iconSize: Dp,
    labelFontSizeSp: Float,
    jiggle: Boolean,
    onAppClick: (AppInfo, Rect) -> Unit,
    onAppLongClick: (AppInfo, Rect) -> Unit,
    onRename: (String) -> Unit,
    onEnterJiggle: () -> Unit,
    onDismiss: () -> Unit,
    /** True while one of its apps is being carried on the home screen: stay alive, invisible. */
    hidden: Boolean = false,
    onDragOutStart: (AppInfo, Offset, Offset, Offset) -> Unit = { _, _, _, _ -> },
    onDragMove: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val progress = remember(folder.folderId) { Animatable(0f) }
    val iconRects = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }
    val scope = rememberCoroutineScope()
    val apps = remember(folder.appPackages, appMap) { folder.appPackages.mapNotNull { appMap[it] } }
    val pagesOfApps = remember(apps) { apps.chunked(9).ifEmpty { listOf(emptyList()) } }
    val pager = rememberPagerState { pagesOfApps.size }
    var title by remember(folder.folderId) { mutableStateOf(folder.title) }
    var closing by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(folder.folderId) { progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 300f)) }

    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        focus.clearFocus(); keyboard?.hide()
        if (title.isNotBlank() && title.trim() != folder.title) onRename(title.trim())
        scope.launch {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f))
            onDismiss(); then()
        }
    }
    BackHandler { close() }

    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { alpha = if (hidden) 0f else 1f }) {
        val density = LocalDensity.current
        val panelW = min(maxWidth.value - 2 * 26f, 340f).dp
        val panelH = panelW + 6.dp
        var panelBounds by remember { mutableStateOf(Rect.Zero) }

        // Dim + tap outside
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(remember { MutableInteractionSource() }, null) { close() }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).offset(y = (-18).dp)
        ) {
            // Title: big, bold, above the panel. Editable while jiggling.
            Box(
                Modifier
                    .width(panelW)
                    .graphicsLayer {
                        val p = progress.value.coerceIn(0f, 1f)
                        alpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationY = (1f - p) * 30.dp.toPx()
                    }
                    .padding(bottom = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (jiggle) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .wallpaperGlass(RoundedCornerShape(16.dp), tint = Color.White.copy(alpha = 0.16f), rim = 0.35f)
                            .padding(horizontal = 14.dp)
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            singleLine = true,
                            textStyle = IosText.title1.copy(color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                            cursorBrush = SolidColor(Color(0xFF0A84FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }),
                            modifier = Modifier.weight(1f)
                        )
                        if (title.isNotEmpty()) Icon(
                            Icons.Rounded.Cancel, null, tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(22.dp).clickable { title = "" }
                        )
                    }
                } else {
                    Text(
                        title,
                        style = IosText.title1.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.combinedClickable(
                            remember { MutableInteractionSource() }, null,
                            onClick = {}, onLongClick = onEnterJiggle
                        )
                    )
                }
            }

            // Panel: grows out of the folder icon.
            Box(
                Modifier
                    .onGloballyPositioned { panelBounds = it.boundsInRoot() }
                    .graphicsLayer {
                        val p = progress.value
                        val b = iconBounds
                        if (b != null && b.width > 0f && panelBounds.width > 0f) {
                            val s = lerp(b.width / panelBounds.width, 1f, p)
                            scaleX = s; scaleY = s
                            translationX = lerp(b.center.x - panelBounds.center.x, 0f, p)
                            translationY = lerp(b.top + b.width / 2f - panelBounds.center.y, 0f, p)
                        } else {
                            val s = lerp(0.6f, 1f, p); scaleX = s; scaleY = s
                        }
                        alpha = (p * 3f).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin.Center
                    }
                    .size(panelW, panelH)
                    .shadow(30.dp, RoundedCornerShape(40.dp), ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.25f))
                    .wallpaperGlass(RoundedCornerShape(40.dp), tint = Color.White.copy(alpha = 0.20f), rim = 0.5f)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .combinedClickable(remember { MutableInteractionSource() }, null, onClick = {}, onLongClick = onEnterJiggle)
                    // iOS: long-press an app and drag it out of the folder onto the home screen.
                    .homeDragGesture(
                        key = folder.folderId,
                        jiggle = jiggle,
                        enabled = true,
                        onStart = { downAt, at ->
                            iconRects.entries.firstOrNull { it.value.contains(downAt) }?.let { (pkg, r) ->
                                apps.firstOrNull { it.packageName == pkg }?.let { app -> onDragOutStart(app, r.topLeft, downAt, at) }
                            }
                        },
                        onMove = onDragMove,
                        onEnd = onDragEnd
                    )
            ) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 22.dp)
                            .graphicsLayer { alpha = ((progress.value - 0.45f) / 0.55f).coerceIn(0f, 1f) },
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(3) { row ->
                            Row(Modifier.fillMaxWidth()) {
                                repeat(3) { col ->
                                    val app = pagesOfApps[page].getOrNull(row * 3 + col)
                                    Box(
                                        Modifier.weight(1f).then(
                                            if (app != null) Modifier.onGloballyPositioned { c ->
                                                val b = c.boundsInRoot()
                                                val px = with(density) { iconSize.toPx() }
                                                iconRects[app.packageName] = Rect(b.center.x - px / 2, b.top, b.center.x + px / 2, b.top + px)
                                            } else Modifier
                                        ),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        if (app != null) IosAppIcon(
                                            app = app,
                                            iconSize = iconSize,
                                            labelFontSizeSp = labelFontSizeSp,
                                            isJiggleMode = jiggle,
                                            onClick = { close { onAppClick(app, Rect.Zero) } },
                                            onClickWithBounds = { b -> close { onAppClick(app, b) } },
                                            onLongClick = { onAppLongClick(app, Rect.Zero) },
                                            onLongClickWithBounds = { b -> onAppLongClick(app, b) }
                                        ) else Spacer(Modifier.size(iconSize))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.height(10.dp).graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
            ) {
                if (pagesOfApps.size > 1) repeat(pagesOfApps.size) { i ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.35f)))
                }
            }
        }
    }
}

/* ========================================================================== */
/*                          Long-press app menu (iOS 26)                      */
/* ========================================================================== */

private enum class MenuPage { Main, More, Folder, Category, Rename }

data class MenuRowSpec(val title: String, val icon: ImageVector, val destructive: Boolean = false, val chevron: Boolean = false, val image: androidx.compose.ui.graphics.ImageBitmap? = null, val onClick: () -> Unit)

/**
 * The iOS 26 Haptic Touch menu: the icon lifts in place over a blurred home
 * screen and a glass menu unfolds from the icon's corner, left- or right-
 * aligned to the icon and below it when there is room (above otherwise).
 */
@Composable
fun IosAppMenu(
    app: AppInfo,
    anchor: Rect?,
    iconSize: Dp,
    strings: IosStrings,
    hs: HomeStrings,
    language: AppLanguage,
    isPinnedInDock: Boolean,
    folders: List<HomeGridItem.FolderItem>,
    quickActions: List<com.tms.os.launcher.model.AppQuickAction>,
    onQuickAction: (com.tms.os.launcher.model.AppQuickAction) -> Unit,
    onEditHome: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    onToggleDock: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onRename: (String?) -> Unit,
    onChangeCategory: (AppCategory) -> Unit,
    onMoveToFolder: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onLaunch: () -> Unit,
    onDismiss: () -> Unit
) {
    val progress = remember(app.packageName) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var page by remember(app.packageName) { mutableStateOf(MenuPage.Main) }
    var closing by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(app.packageName) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        progress.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 420f))
    }
    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        focus.clearFocus(); keyboard?.hide()
        scope.launch {
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 650f))
            onDismiss(); then()
        }
    }
    BackHandler { if (page != MenuPage.Main) page = MenuPage.Main else close() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val a = anchor?.takeIf { it.width > 0f } ?: Rect(screenW / 2 - 90f, screenH / 3, screenW / 2 + 90f, screenH / 3 + 180f)
        val leftSide = a.center.x < screenW / 2

        // Tap anywhere else to dismiss.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.12f))
                .clickable(remember { MutableInteractionSource() }, null) { close() }
        )

        // The lifted icon, exactly over the original one.
        val iconPx = with(density) { iconSize.toPx() }
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset((a.center.x - iconPx / 2).roundToInt(), a.top.roundToInt()) }
                .size(iconSize)
                .graphicsLayer {
                    val s = 1f + 0.08f * progress.value
                    scaleX = s; scaleY = s
                    shadowElevation = 18.dp.toPx() * progress.value
                    shape = IosSquircle
                    clip = true
                }
                .clickable { close(onLaunch) }
        ) {
            app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }

        // Menu placed next to the icon with a custom layout (needs its own height).
        val gap = with(density) { 14.dp.toPx() }
        val margin = with(density) { 16.dp.toPx() }
        val iconBottom = a.top + iconPx
        Layout(
            content = {
                Column(
                    Modifier
                        .width(292.dp)
                        .heightIn(max = 460.dp)
                        .shadow(28.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.3f))
                        .wallpaperGlass(RoundedCornerShape(28.dp), tint = Color(0xB8202024), rim = 0.4f)
                        .clickable(remember { MutableInteractionSource() }, null) {}
                ) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            if (targetState != MenuPage.Main) slideInHorizontally { it / 2 } + fadeIn() togetherWith slideOutHorizontally { -it / 2 } + fadeOut()
                            else slideInHorizontally { -it / 2 } + fadeIn() togetherWith slideOutHorizontally { it / 2 } + fadeOut()
                        },
                        label = "menuPage"
                    ) { p ->
                        when (p) {
                            MenuPage.Main -> {
                                // App shortcuts first, then a thick separator, like iOS.
                                val actions = quickActions.map { a ->
                                    MenuRowSpec(a.label, Icons.Rounded.Apps, image = a.icon) { close { onQuickAction(a) } }
                                }
                                val system = listOf(
                                    MenuRowSpec(strings.editHomeScreen, Icons.Rounded.Apps) { close(onEditHome) },
                                    MenuRowSpec(hs.shareApp, Icons.Rounded.IosShare) { close(onShare) },
                                    MenuRowSpec(strings.more, Icons.Rounded.MoreHoriz, chevron = true) { page = MenuPage.More },
                                    MenuRowSpec(strings.removeApp, Icons.Rounded.RemoveCircleOutline, destructive = true) { close(onRemove) }
                                )
                                val seps = buildSet { if (actions.isNotEmpty()) add(actions.size); add(actions.size + 3) }
                                MenuList(actions + system, separatorBefore = seps)
                            }
                            MenuPage.More -> Column {
                                SubHeader(strings.more) { page = MenuPage.Main }
                                MenuList(listOf(
                                    MenuRowSpec(if (isPinnedInDock) strings.removeFromDock else strings.moveToDock, Icons.Rounded.Dock) { close(onToggleDock) },
                                    MenuRowSpec(strings.moveToFolder, Icons.Rounded.Folder, chevron = true) { page = MenuPage.Folder },
                                    MenuRowSpec(strings.rename, Icons.Rounded.Edit) { page = MenuPage.Rename },
                                    MenuRowSpec(strings.changeCategory, Icons.Rounded.Label, chevron = true) { page = MenuPage.Category },
                                    MenuRowSpec(strings.appInfo, Icons.Rounded.Info) { close(onAppInfo) },
                                    MenuRowSpec(strings.hideApp, Icons.Rounded.VisibilityOff) { close(onHide) }
                                ))
                            }
                            MenuPage.Folder -> Column {
                                SubHeader(strings.moveToFolder) { page = MenuPage.More }
                                MenuList(
                                    listOf(MenuRowSpec(strings.newFolder, Icons.Rounded.CreateNewFolder) { close { onCreateFolder(app.category.localizedTitle(language)) } }) +
                                        folders.map { f -> MenuRowSpec(f.title, Icons.Rounded.Folder) { close { onMoveToFolder(f.folderId) } } },
                                    scroll = true
                                )
                            }
                            MenuPage.Category -> Column {
                                SubHeader(strings.changeCategory) { page = MenuPage.More }
                                MenuList(
                                    AppCategory.entries.filter { it != AppCategory.SUGGESTIONS }.map { c ->
                                        MenuRowSpec(c.localizedTitle(language), if (c == app.category) Icons.Rounded.Check else Icons.Rounded.Label) { close { onChangeCategory(c) } }
                                    },
                                    scroll = true
                                )
                            }
                            MenuPage.Rename -> RenameBox(app, strings, onBack = { page = MenuPage.More }) { name -> close { onRename(name) } }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val p = progress.value
                alpha = (p * 2f).coerceIn(0f, 1f)
            }
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
            val below = iconBottom + gap + placeable.height <= screenH - margin
            val x = if (leftSide) (a.center.x - iconPx / 2).coerceIn(margin, screenW - placeable.width - margin)
                else (a.center.x + iconPx / 2 - placeable.width).coerceIn(margin, screenW - placeable.width - margin)
            val y = if (below) iconBottom + gap else max(margin, a.top - gap - placeable.height)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeWithLayer(x.roundToInt(), y.roundToInt()) {
                    val p = progress.value
                    val s = lerp(0.25f, 1f, p)
                    scaleX = s; scaleY = s
                    transformOrigin = TransformOrigin(if (leftSide) 0f else 1f, if (below) 0f else 1f)
                }
            }
        }
    }
}

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = IosText.headline.copy(color = Color.White), maxLines = 1)
    }
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.14f)))
}

@Composable
private fun MenuList(rows: List<MenuRowSpec>, separatorBefore: Set<Int> = emptySet(), scroll: Boolean = false) {
    Column(Modifier.then(if (scroll) Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()) else Modifier).padding(vertical = 6.dp)) {
        rows.forEachIndexed { i, r ->
            if (i in separatorBefore) Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(6.dp).background(Color.Black.copy(alpha = 0.12f)))
            else if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.10f)))
            val color = if (r.destructive) Color(0xFFFF453A) else Color.White
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = r.onClick).padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(r.title, style = IosText.body.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                if (r.image != null) Image(r.image, null, modifier = Modifier.size(22.dp))
                else Icon(if (r.chevron) Icons.Rounded.ChevronRight else r.icon, null, tint = color.copy(alpha = if (r.chevron) 0.5f else 1f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RenameBox(app: AppInfo, strings: IosStrings, onBack: () -> Unit, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(app.displayLabel) }
    val fr = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { delay(180); runCatching { fr.requestFocus() }; keyboard?.show() }
    Column {
        SubHeader(strings.rename, onBack)
        Box(
            Modifier.padding(14.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                text, { text = it }, singleLine = true,
                textStyle = IosText.body.copy(color = Color.White),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone(text.trim().ifBlank { null }) }),
                modifier = Modifier.fillMaxWidth().focusRequester(fr)
            )
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable { onDone(null) }, contentAlignment = Alignment.Center) {
                Text(strings.resetName, style = IosText.subheadline.copy(color = Color.White))
            }
            Box(Modifier.weight(1f).height(42.dp).clip(CircleShape).background(Color(0xFF0A84FF)).clickable { onDone(text.trim().ifBlank { null }) }, contentAlignment = Alignment.Center) {
                Text(strings.save, style = IosText.subheadline.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}
