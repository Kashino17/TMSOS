@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tms.os.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.AppShortcutItem
import com.tms.os.launcher.model.ContactItem
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.IconThemeMode
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.IosStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

/* ========================================================================== */
/*                                  Spotlight                                 */
/* ========================================================================== */

@Composable
fun IosSpotlightOverlay(
    visible: Boolean,
    query: String,
    results: List<AppInfo>,
    shortcuts: List<AppShortcutItem>,
    contacts: List<ContactItem>,
    mathResult: String?,
    allApps: List<AppInfo>,
    settings: LauncherSettings,
    strings: IosStrings,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onContactClick: (ContactItem) -> Unit,
    onWebSearch: (String, String) -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(170)),
        exit = fadeOut(tween(120)),
        modifier = Modifier.fillMaxSize()
    ) {
        SpotlightContent(
            query = query,
            results = results,
            shortcuts = shortcuts,
            contacts = contacts,
            mathResult = mathResult,
            allApps = allApps,
            settings = settings,
            strings = strings,
            onQueryChange = onQueryChange,
            onAppClick = onAppClick,
            onAppLongClick = onAppLongClick,
            onContactClick = onContactClick,
            onWebSearch = onWebSearch,
            onClose = onClose
        )
    }
}

@Composable
private fun SpotlightContent(
    query: String,
    results: List<AppInfo>,
    shortcuts: List<AppShortcutItem>,
    contacts: List<ContactItem>,
    mathResult: String?,
    allApps: List<AppInfo>,
    settings: LauncherSettings,
    strings: IosStrings,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onContactClick: (ContactItem) -> Unit,
    onWebSearch: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val bottomSearch = settings.searchBarAtBottomInSpotlight || settings.autoOpenKeyboardInSpotlight

    LaunchedEffect(Unit) {
        if (settings.autoOpenKeyboardInSpotlight) {
            delay(180)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2050507))
            .imePadding()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        total += dragAmount
                        if (abs(total) > 90f) {
                            total = 0f
                            onClose()
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 54.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            if (!bottomSearch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp)
                ) {
                    SpotlightSearchField(
                        query = query,
                        placeholder = strings.searchPlaceholder,
                        focusRequester = focusRequester,
                        onQueryChange = onQueryChange,
                        onSearch = { results.firstOrNull()?.let(onAppClick) },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = strings.cancel,
                        color = Color(0xFF0A84FF),
                        style = IosText.body.copy(fontSize = 16.5.sp),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                if (query.isBlank()) {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(strings.siriSuggestions)
                    Spacer(Modifier.height(10.dp))
                    if (results.isEmpty()) {
                        Text(
                            text = strings.loading,
                            color = Color.White.copy(alpha = 0.5f),
                            style = IosText.subheadline,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        SpotlightGrid(
                            apps = results.take(4),
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (allApps.isNotEmpty()) {
                        Text(
                            text = strings.apps,
                            color = Color.White.copy(alpha = 0.35f),
                            style = IosText.footnote,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        SpotlightGrid(
                            apps = allApps.take(8),
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick
                        )
                    }
                } else {
                    if (mathResult != null) {
                        Spacer(Modifier.height(10.dp))
                        MathResultCard(query = query, result = mathResult, strings = strings)
                        Spacer(Modifier.height(16.dp))
                    }

                    if (shortcuts.isNotEmpty()) {
                        SectionLabel(strings.suggestions)
                        Spacer(Modifier.height(8.dp))
                        ShortcutGroup(shortcuts = shortcuts, results = results, onAppClick = onAppClick)
                        Spacer(Modifier.height(16.dp))
                    }

                    if (contacts.isNotEmpty()) {
                        SectionLabel(strings.contacts)
                        Spacer(Modifier.height(8.dp))
                        ContactGroup(contacts = contacts, onContactClick = onContactClick)
                        Spacer(Modifier.height(16.dp))
                    }

                    SectionLabel(strings.apps)
                    Spacer(Modifier.height(10.dp))
                    if (results.isEmpty()) {
                        if (mathResult == null) {
                            EmptySpotlightResults(query = query, strings = strings)
                        }
                    } else {
                        SpotlightGrid(
                            apps = results.take(8),
                            onAppClick = { app ->
                                keyboard?.hide()
                                onAppClick(app)
                            },
                            onAppLongClick = { app ->
                                keyboard?.hide()
                                onAppLongClick(app)
                            }
                        )
                    }

                    if (settings.showWebSearchInSpotlight) {
                        Spacer(Modifier.height(20.dp))
                        SectionLabel(strings.webSearch)
                        Spacer(Modifier.height(10.dp))
                        WebSearchRow(
                            query = query,
                            onWebSearch = { engine ->
                                keyboard?.hide()
                                onWebSearch(query, engine)
                            }
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                }
            }

            if (bottomSearch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    SpotlightSearchField(
                        query = query,
                        placeholder = strings.searchPlaceholder,
                        focusRequester = focusRequester,
                        onQueryChange = onQueryChange,
                        onSearch = { results.firstOrNull()?.let(onAppClick) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotlightSearchField(
    query: String,
    placeholder: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(50.dp)
            .clip(CircleShape)
            .background(Color(0xCC343437))
            .border(0.55.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .padding(start = 14.dp, end = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.62f),
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.44f),
                    style = IosText.body.copy(fontSize = 17.sp),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = IosText.body.copy(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onQueryChange("") }
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.62f),
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(19.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.52f),
        style = IosText.footnoteSemibold.copy(fontSize = 13.sp)
    )
}

@Composable
private fun SpotlightGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        apps.chunked(4).forEach { rowApps ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0 until 4) {
                    val app = rowApps.getOrNull(i)
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (app != null) {
                            SpotlightAppTile(
                                app = app,
                                onAppClick = onAppClick,
                                onAppLongClick = onAppLongClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightAppTile(
    app: AppInfo,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(IosSquircle)
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.displayLabel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(IosSquircle)
                )
            } else {
                SpotlightLetterIcon(app.displayLabel, 58.dp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.displayLabel,
            color = Color.White,
            style = IosText.iconLabel.copy(fontSize = 11.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SpotlightLetterIcon(label: String, size: Dp) {
    val hue = remember(label) { ((label.hashCode() % 360) + 360) % 360 }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(IosSquircle)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.hsv(hue.toFloat(), 0.55f, 0.94f),
                        Color.hsv(((hue + 24) % 360).toFloat(), 0.72f, 0.62f)
                    )
                )
            )
    ) {
        Text(
            text = label.trim().take(1).uppercase(Locale.getDefault()),
            color = Color.White,
            style = IosText.title3.copy(fontSize = (size.value * 0.38f).sp)
        )
    }
}

@Composable
private fun MathResultCard(query: String, result: String, strings: IosStrings) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xCC1C1C1E))
            .border(0.55.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFFFF9F0A))
        ) {
            Icon(
                imageVector = Icons.Rounded.Calculate,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$query =",
                color = Color.White.copy(alpha = 0.55f),
                style = IosText.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = result,
                color = Color.White,
                style = IosText.title3.copy(fontSize = 21.sp)
            )
        }
        Text(
            text = strings.calculator,
            color = Color(0xFFFF9F0A),
            style = IosText.caption
        )
    }
}

@Composable
private fun ShortcutGroup(
    shortcuts: List<AppShortcutItem>,
    results: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC1C1C1E))
            .border(0.55.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(16.dp))
    ) {
        shortcuts.take(3).forEachIndexed { index, shortcut ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            results.firstOrNull { it.packageName == shortcut.packageName }
                                ?.let(onAppClick)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFFFD60A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shortcut.shortLabel,
                        color = Color.White,
                        style = IosText.subheadline.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = shortcut.appLabel,
                        color = Color.White.copy(alpha = 0.45f),
                        style = IosText.caption
                    )
                }
            }
            if (index != shortcuts.take(3).lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier.padding(start = 42.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactGroup(
    contacts: List<ContactItem>,
    onContactClick: (ContactItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC1C1C1E))
            .border(0.55.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(16.dp))
    ) {
        contacts.take(3).forEachIndexed { index, contact ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onContactClick(contact) }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        style = IosText.subheadline.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = contact.phoneNumber,
                        color = Color.White.copy(alpha = 0.45f),
                        style = IosText.caption
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            if (index != contacts.take(3).lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySpotlightResults(query: String, strings: IosStrings) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = strings.noResults,
            color = Color.White,
            style = IosText.headline
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "„$query“",
            color = Color.White.copy(alpha = 0.45f),
            style = IosText.footnote
        )
    }
}

@Composable
private fun WebSearchRow(
    query: String,
    onWebSearch: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        WebEnginePill(
            label = "Google",
            icon = Icons.Rounded.Public,
            tint = Color(0xFF0A84FF),
            modifier = Modifier.weight(1f),
            onClick = { onWebSearch("google") }
        )
        WebEnginePill(
            label = "YouTube",
            icon = Icons.Rounded.SmartDisplay,
            tint = Color(0xFFFF453A),
            modifier = Modifier.weight(1f),
            onClick = { onWebSearch("youtube") }
        )
        WebEnginePill(
            label = "Play Store",
            icon = Icons.Rounded.Shop,
            tint = Color(0xFF30D158),
            modifier = Modifier.weight(1f),
            onClick = { onWebSearch("play") }
        )
    }
}

@Composable
private fun WebEnginePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xCC1C1C1E))
            .border(0.55.dp, Color.White.copy(alpha = 0.11f), RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = Color.White,
            style = IosText.caption.copy(fontSize = 12.sp),
            maxLines = 1
        )
    }
}

/* ========================================================================== */
/*                                   Folder                                   */
/* ========================================================================== */

@Composable
fun IosFolderOverlay(
    visible: Boolean,
    folder: HomeGridItem.FolderItem,
    appMap: Map<String, AppInfo>,
    originNormalized: Offset,
    strings: IosStrings,
    iconSize: Dp,
    labelFontSizeSp: Float,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDissolve: () -> Unit,
    onDismiss: () -> Unit
) {
    val progress = remember(folder.folderId) { Animatable(0.01f) }
    var title by remember(folder.folderId, folder.title) { mutableStateOf(folder.title) }
    val apps = remember(folder.appPackages, appMap) {
        folder.appPackages.mapNotNull { appMap[it] }
    }
    val columns = if (iconSize >= 62.dp) 4 else 3
    val rows = ceil(apps.size.toFloat() / columns.toFloat()).toInt().coerceAtLeast(1)
    val gridHeight = ((iconSize + 34.dp) * rows + 24.dp).coerceIn(150.dp, 470.dp)

    LaunchedEffect(folder.folderId) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f)
        )
    }

    val saveAndDismiss = {
        if (title.trim().isNotEmpty() && title != folder.title) onRenameFolder(title.trim())
        onDismiss()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3050507))
            .pointerInput(folder.folderId) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        total += dragAmount
                        if (abs(total) > 80f) {
                            total = 0f
                            saveAndDismiss()
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = saveAndDismiss
            )
            .padding(horizontal = 22.dp, vertical = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = if (columns >= 4) 580.dp else 410.dp)
                .graphicsLayer {
                    val p = progress.value
                    scaleX = 0.62f + 0.38f * p
                    scaleY = 0.62f + 0.38f * p
                    alpha = p
                    transformOrigin = TransformOrigin(originNormalized.x, originNormalized.y)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = IosText.title1.copy(
                    color = Color.White,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (title.trim().isNotEmpty() && title != folder.title) {
                            onRenameFolder(title.trim())
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .shadow(30.dp, RoundedCornerShape(38.dp), clip = false)
                    .clip(RoundedCornerShape(38.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xD93C3C3F), Color(0xD9222224))
                        )
                    )
                    .border(0.7.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(38.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
                            IosAppIcon(
                                app = app,
                                iconSize = iconSize,
                                labelFontSizeSp = labelFontSizeSp,
                                showLabel = true,
                                showBadges = true,
                                isJiggleMode = false,
                                onClick = {
                                    saveAndDismiss()
                                    onAppClick(app)
                                },
                                onLongClick = {
                                    saveAndDismiss()
                                    onAppLongClick(app)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FolderGlassPill(
                    text = strings.dissolveFolder,
                    onClick = onDissolve
                )
                FolderGlassPill(
                    text = strings.done,
                    onClick = saveAndDismiss,
                    emphasized = true
                )
            }
        }
    }
}

@Composable
private fun FolderGlassPill(
    text: String,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (emphasized) Color(0xCC0A84FF) else Color(0x992C2C2E))
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = IosText.subheadline.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/* ========================================================================== */
/*                            Expanded category                               */
/* ========================================================================== */

@Composable
fun IosCategoryExpandedView(
    visible: Boolean,
    category: AppCategory,
    apps: List<AppInfo>,
    strings: IosStrings,
    settings: LauncherSettings,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val progress = remember(category.id) { Animatable(0.01f) }
    LaunchedEffect(category.id) {
        progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 360f))
    }

    val language = settings.appLanguage

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3050507))
            .pointerInput(category.id) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        total += dragAmount
                        if (abs(total) > 80f) {
                            total = 0f
                            onDismiss()
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 22.dp, vertical = 32.dp)
    ) {
        val isWide = maxWidth >= 600.dp
        val columns = if (isWide) 6 else 4
        val iconSize = if (isWide) 62.dp else 58.dp
        val rows = ceil(apps.size.toFloat() / columns.toFloat()).toInt().coerceAtLeast(1)
        val gridHeight = ((iconSize + 34.dp) * rows + 28.dp).coerceIn(180.dp, 500.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = if (isWide) 660.dp else 440.dp)
                .graphicsLayer {
                    val p = progress.value
                    scaleX = 0.74f + 0.26f * p
                    scaleY = 0.74f + 0.26f * p
                    alpha = p
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Text(
                text = category.localizedTitle(language),
                color = Color.White,
                style = IosText.title1.copy(fontSize = 29.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = strings.appsCount.format(apps.size),
                color = Color.White.copy(alpha = 0.52f),
                style = IosText.footnote
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .shadow(30.dp, RoundedCornerShape(38.dp), clip = false)
                    .clip(RoundedCornerShape(38.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                category.accentColor.copy(alpha = 0.22f),
                                Color(0xE61C1C1E)
                            )
                        )
                    )
                    .border(0.7.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(38.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
                            IosAppIcon(
                                app = app,
                                iconSize = iconSize,
                                labelFontSizeSp = settings.labelFontSizeSp,
                                showLabel = true,
                                showBadges = settings.enableBadges,
                                isJiggleMode = false,
                                onClick = {
                                    onDismiss()
                                    onAppClick(app)
                                },
                                onLongClick = {
                                    onDismiss()
                                    onAppLongClick(app)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            FolderGlassPill(text = strings.done, onClick = onDismiss, emphasized = true)
        }
    }
}

/* ========================================================================== */
/*                               Context menu                                 */
/* ========================================================================== */

private const val PAGE_MAIN = "main"
private const val PAGE_RENAME = "rename"
private const val PAGE_CATEGORY = "category"
private const val PAGE_FOLDER = "folder"
private const val PAGE_REMOVE = "remove"

@Composable
fun IosContextMenu(
    visible: Boolean,
    app: AppInfo,
    isPinnedInDock: Boolean,
    existingFolders: List<HomeGridItem.FolderItem>,
    strings: IosStrings,
    appLanguage: AppLanguage = AppLanguage.DE,
    onLaunch: () -> Unit,
    onEditHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onAppInfo: () -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit,
    onRename: (String?) -> Unit,
    onChangeCategory: (AppCategory) -> Unit,
    onMoveToFolder: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var page by remember(app.packageName) { mutableStateOf(PAGE_MAIN) }
    var renameText by remember(app.packageName, app.displayLabel) { mutableStateOf(app.displayLabel) }
    val menuScale = remember(app.packageName) { Animatable(0.92f) }
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val renameFocus = remember { FocusRequester() }

    LaunchedEffect(app.packageName) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuScale.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 520f))
    }
    LaunchedEffect(page) {
        if (page == PAGE_RENAME) {
            delay(160)
            runCatching { renameFocus.requestFocus() }
            keyboard?.show()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    val close = {
        focusManager.clearFocus()
        keyboard?.hide()
        onDismiss()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99050507))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = close
            )
            .padding(horizontal = 22.dp, vertical = 26.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 336.dp)
                .graphicsLayer {
                    scaleX = menuScale.value
                    scaleY = menuScale.value
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            if (page == PAGE_REMOVE) {
                RemoveAppAlert(
                    app = app,
                    strings = strings,
                    onRemoveFromHome = {
                        onRemoveFromHome()
                        close()
                    },
                    onDelete = {
                        onUninstall()
                        close()
                    },
                    onCancel = { page = PAGE_MAIN }
                )
            } else {
                ContextMenuHeader(
                    app = app,
                    language = appLanguage,
                    onLaunch = onLaunch
                )

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(26.dp, RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xF21C1C1E))
                        .border(0.55.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
                ) {
                    Crossfade(
                        targetState = page,
                        animationSpec = tween(170),
                        label = "contextPage"
                    ) { currentPage ->
                        when (currentPage) {
                            PAGE_RENAME -> RenamePage(
                                text = renameText,
                                strings = strings,
                                focusRequester = renameFocus,
                                onTextChange = { renameText = it },
                                onReset = {
                                    renameText = app.label
                                    onRename(null)
                                    close()
                                },
                                onSave = {
                                    val cleaned = renameText.trim()
                                    onRename(cleaned.ifBlank { null })
                                    close()
                                },
                                onBack = {
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    page = PAGE_MAIN
                                }
                            )

                            PAGE_CATEGORY -> CategoryPage(
                                current = app.category,
                                strings = strings,
                                language = appLanguage,
                                onSelect = { category ->
                                    onChangeCategory(category)
                                    close()
                                },
                                onBack = { page = PAGE_MAIN }
                            )

                            PAGE_FOLDER -> FolderPage(
                                app = app,
                                folders = existingFolders,
                                strings = strings,
                                language = appLanguage,
                                onCreateFolder = {
                                    onCreateFolder(app.category.localizedTitle(appLanguage))
                                    close()
                                },
                                onSelectFolder = { folderId ->
                                    onMoveToFolder(folderId)
                                    close()
                                },
                                onBack = { page = PAGE_MAIN }
                            )

                            else -> MainMenuPage(
                                app = app,
                                strings = strings,
                                isPinnedInDock = isPinnedInDock,
                                onEditHome = onEditHome,
                                onRenameClick = { page = PAGE_RENAME },
                                onChangeCategoryClick = { page = PAGE_CATEGORY },
                                onMoveToFolderClick = { page = PAGE_FOLDER },
                                onToggleDock = onToggleDock,
                                onHide = onHide,
                                onAppInfo = onAppInfo,
                                onRemoveClick = { page = PAGE_REMOVE },
                                onDismiss = close
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuHeader(
    app: AppInfo,
    language: AppLanguage,
    onLaunch: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xEE2C2C2E))
            .border(0.6.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLaunch
            )
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(IosSquircle)
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.displayLabel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(IosSquircle)
                )
            } else {
                SpotlightLetterIcon(app.displayLabel, 54.dp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.displayLabel,
                color = Color.White,
                style = IosText.headline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.category.localizedTitle(language),
                color = Color.White.copy(alpha = 0.55f),
                style = IosText.caption,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MainMenuPage(
    app: AppInfo,
    strings: IosStrings,
    isPinnedInDock: Boolean,
    onEditHome: () -> Unit,
    onRenameClick: () -> Unit,
    onChangeCategoryClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onRemoveClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MenuRow(text = strings.editHomeScreen, onClick = onEditHome)
        MenuDivider()
        MenuRow(text = strings.rename, onClick = onRenameClick)
        MenuDivider()
        MenuRow(text = strings.changeCategory, onClick = onChangeCategoryClick, chevron = true)
        MenuDivider()
        MenuRow(text = strings.moveToFolder, onClick = onMoveToFolderClick, chevron = true)
        MenuDivider()
        MenuRow(
            text = if (isPinnedInDock) strings.removeFromDock else strings.moveToDock,
            onClick = onToggleDock
        )
        MenuDivider()
        MenuRow(text = strings.hideApp, onClick = onHide)
        MenuDivider()
        MenuRow(text = strings.appInfo, onClick = onAppInfo)
        MenuDivider()
        MenuRow(
            text = strings.removeApp,
            onClick = onRemoveClick,
            destructive = true,
            chevron = true
        )
    }
}

@Composable
private fun RenamePage(
    text: String,
    strings: IosStrings,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PageHeader(title = strings.rename, onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = IosText.body.copy(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x3D787880))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = strings.resetName,
                color = Color.White.copy(alpha = 0.62f),
                style = IosText.subheadline,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onReset
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = strings.save,
                color = Color(0xFF0A84FF),
                style = IosText.subheadline.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSave
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CategoryPage(
    current: AppCategory,
    strings: IosStrings,
    language: AppLanguage,
    onSelect: (AppCategory) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PageHeader(title = strings.changeCategory, onBack = onBack)
        LazyColumn(modifier = Modifier.heightIn(max = 330.dp)) {
            val categories = AppCategory.entries.filter { it != AppCategory.SUGGESTIONS }
            items(categories, key = { it.id }) { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(category) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = category.localizedTitle(language),
                        color = if (category == current) Color(0xFF0A84FF) else Color.White,
                        style = IosText.subheadline.copy(
                            fontWeight = if (category == current) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (category == current) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                MenuDivider(inset = 16.dp)
            }
        }
    }
}

@Composable
private fun FolderPage(
    app: AppInfo,
    folders: List<HomeGridItem.FolderItem>,
    strings: IosStrings,
    language: AppLanguage,
    onCreateFolder: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PageHeader(title = strings.moveToFolder, onBack = onBack)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCreateFolder
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CreateNewFolder,
                contentDescription = null,
                tint = Color(0xFF0A84FF),
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "${strings.newFolder} (${app.category.localizedTitle(language)})",
                color = Color(0xFF0A84FF),
                style = IosText.subheadline.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (folders.isNotEmpty()) {
            MenuDivider(inset = 16.dp)
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                items(folders, key = { it.folderId }) { folder ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectFolder(folder.folderId) }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = folder.title,
                            color = Color.White,
                            style = IosText.subheadline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.appsCount.format(folder.appPackages.size),
                            color = Color.White.copy(alpha = 0.42f),
                            style = IosText.caption
                        )
                    }
                    MenuDivider(inset = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = IosText.footnoteSemibold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    MenuDivider()
}

@Composable
private fun MenuRow(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    chevron: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.5.dp)
    ) {
        Text(
            text = text,
            color = if (destructive) Color(0xFFFF453A) else Color.White,
            style = IosText.body.copy(fontSize = 16.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (chevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MenuDivider(inset: Dp = 0.dp) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.padding(start = inset)
    )
}

@Composable
private fun RemoveAppAlert(
    app: AppInfo,
    strings: IosStrings,
    onRemoveFromHome: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 290.dp)
            .shadow(30.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF228282A))
            .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = strings.removeAppConfirm,
                color = Color.White,
                style = IosText.headline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = strings.removeAppMessage.format(app.displayLabel),
                color = Color.White.copy(alpha = 0.58f),
                style = IosText.footnote,
                textAlign = TextAlign.Center
            )
        }
        MenuDivider()
        AlertButton(
            text = strings.removeFromHome,
            color = Color(0xFF0A84FF),
            onClick = onRemoveFromHome
        )
        MenuDivider()
        if (!app.isSystemApp) {
            AlertButton(
                text = strings.deleteApp,
                color = Color(0xFFFF453A),
                onClick = onDelete
            )
            MenuDivider()
        }
        AlertButton(
            text = strings.cancel,
            color = Color(0xFF0A84FF),
            bold = true,
            onClick = onCancel
        )
    }
}

@Composable
private fun AlertButton(
    text: String,
    color: Color,
    bold: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.5.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = IosText.body.copy(
                fontSize = 16.sp,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
            ),
            textAlign = TextAlign.Center
        )
    }
}

/* ========================================================================== */
/*                              Customize sheet                               */
/* ========================================================================== */

@Composable
fun IosCustomizeSheet(
    visible: Boolean,
    settings: LauncherSettings,
    strings: IosStrings,
    onSettingsChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onSmartOrganize: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x73000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 340f)
            ) + fadeIn(tween(120)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(220)
            ) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BoxWithConstraints(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize()
            ) {
                val isWide = maxWidth >= 600.dp
                val shape = if (isWide) {
                    RoundedCornerShape(30.dp)
                } else {
                    RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
                }

                Column(
                    modifier = Modifier
                        .then(
                            if (isWide) {
                                Modifier
                                    .width(580.dp)
                                    .fillMaxHeight(0.87f)
                                    .padding(bottom = 26.dp)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.84f)
                            }
                        )
                        .shadow(34.dp, shape, clip = false)
                        .clip(shape)
                        .background(Color(0xF51C1C1E))
                        .border(0.6.dp, Color.White.copy(alpha = 0.12f), shape)
                        .navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 14.dp, top = 2.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = strings.customize,
                            color = Color.White,
                            style = IosText.title2,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.done,
                            color = Color(0xFF0A84FF),
                            style = IosText.body.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        SectionHeader(strings.appIcons)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconStyleCard(
                                label = strings.styleStandard,
                                mode = IconThemeMode.STANDARD,
                                selected = settings.iconThemeMode == IconThemeMode.STANDARD,
                                accent = Color(0xFF0A84FF),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSettingsChange { it.copy(iconThemeMode = IconThemeMode.STANDARD) }
                                }
                            )
                            IconStyleCard(
                                label = strings.styleDark,
                                mode = IconThemeMode.IOS_DARK,
                                selected = settings.iconThemeMode == IconThemeMode.IOS_DARK,
                                accent = Color(0xFF636366),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSettingsChange { it.copy(iconThemeMode = IconThemeMode.IOS_DARK) }
                                }
                            )
                            IconStyleCard(
                                label = strings.styleTinted,
                                mode = IconThemeMode.IOS_TINTED,
                                selected = settings.iconThemeMode == IconThemeMode.IOS_TINTED,
                                accent = Color(settings.tintAccentColor.colorHex),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSettingsChange { it.copy(iconThemeMode = IconThemeMode.IOS_TINTED) }
                                }
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        SectionHeader(strings.iconSize)
                        SegmentedControl(
                            options = listOf(
                                strings.small to 0.9f,
                                strings.medium to 1.0f,
                                strings.large to 1.1f
                            ),
                            selected = settings.iconSizeScale,
                            onSelect = { scale ->
                                onSettingsChange { it.copy(iconSizeScale = scale) }
                            }
                        )

                        Spacer(Modifier.height(18.dp))

                        SettingsGroup {
                            ToggleRow(
                                label = strings.appNames,
                                checked = settings.showAppLabels,
                                onCheckedChange = { checked ->
                                    onSettingsChange { it.copy(showAppLabels = checked) }
                                }
                            )
                            MenuDivider(inset = 14.dp)
                            ToggleRow(
                                label = strings.badges,
                                checked = settings.enableBadges,
                                onCheckedChange = { checked ->
                                    onSettingsChange { it.copy(enableBadges = checked) }
                                }
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        SectionHeader(strings.homeScreen)
                        SettingsGroup {
                            ToggleRow(
                                label = strings.showWidgets,
                                checked = settings.showTopWidgetsOnFirstPage,
                                onCheckedChange = { checked ->
                                    onSettingsChange { it.copy(showTopWidgetsOnFirstPage = checked) }
                                }
                            )
                            MenuDivider(inset = 14.dp)
                            ActionRow(
                                label = strings.smartOrganize,
                                icon = Icons.Rounded.AutoAwesome,
                                iconTint = Color.White,
                                iconBackground = Color(0xFF0A84FF),
                                onClick = {
                                    onSmartOrganize()
                                    onDismiss()
                                }
                            )
                            MenuDivider(inset = 14.dp)
                            ActionRow(
                                label = strings.settings,
                                icon = Icons.Rounded.Settings,
                                iconTint = Color.White,
                                iconBackground = Color(0xFF8E8E93),
                                onClick = {
                                    onDismiss()
                                    onOpenSettings()
                                }
                            )
                        }

                        Spacer(Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.48f),
        style = IosText.footnoteSemibold.copy(fontSize = 12.5.sp),
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 8.dp)
    )
}

@Composable
private fun IconStyleCard(
    label: String,
    mode: IconThemeMode,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = when (mode) {
        IconThemeMode.STANDARD -> listOf(Color(0xFF64B5FF), Color(0xFF0A5BFF))
        IconThemeMode.IOS_DARK -> listOf(Color(0xFF6E6E73), Color(0xFF2C2C2E))
        IconThemeMode.IOS_TINTED -> listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.42f))
    }

    Box(
        modifier = modifier
            .height(102.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF323236) else Color(0xFF242426))
            .border(
                width = if (selected) 1.8.dp else 0.6.dp,
                color = if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(IosSquircle)
                    .background(Brush.verticalGradient(gradient))
            ) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.92f),
                style = IosText.caption.copy(fontSize = 12.sp),
                maxLines = 1
            )
        }

        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    options: List<Pair<String, Float>>,
    selected: Float,
    onSelect: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0x3D787880))
            .padding(2.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = abs(selected - value) < 0.03f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) Color(0xFF636366) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(value) }
                    )
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                    style = IosText.footnote.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF242426))
            .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = IosText.body.copy(fontSize = 16.sp),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IosSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconBackground)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = Color.White,
            style = IosText.body.copy(fontSize = 16.sp),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.26f),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val offset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
        label = "switchThumb"
    )
    val track by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(160),
        label = "switchTrack"
    )

    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(lerp(Color(0xFF39393D), Color(0xFF34C759), track))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
