@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tms.os.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.IosStrings
import kotlinx.coroutines.launch
import java.util.Locale

/* -------------------------------------------------------------------------- */
/*                              App Library page                              */
/* -------------------------------------------------------------------------- */

private data class LibraryPod(
    val id: String,
    val title: String,
    val apps: List<AppInfo>,
    val category: AppCategory?
)

@Composable
fun IosAppLibraryPage(
    allApps: List<AppInfo>,
    categorizedApps: Map<AppCategory, List<AppInfo>>,
    settings: LauncherSettings,
    strings: IosStrings,
    isUnfolded: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onCategoryClick: (AppCategory) -> Unit,
    onOpenSpotlight: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showAlphabeticalList by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val language = settings.appLanguage

    val pods = remember(categorizedApps, allApps, language) {
        buildLibraryPods(categorizedApps, allApps, strings, language)
    }

    val visibleApps = remember(allApps) {
        allApps.filter { !it.isHidden }.sortedBy { it.displayLabel.lowercase(Locale.getDefault()) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // iOS renders the App Library as a heavily darkened, frosted take on
                // the wallpaper. Android can't blur the window wallpaper from here,
                // so the same effect is approximated with a deep vertical scrim.
                Brush.verticalGradient(
                    0f to Color(0x73000000),
                    0.55f to Color(0x59000000),
                    1f to Color(0x3D000000)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
        ) {
            LibraryHeaderPill(
                title = strings.appLibrary,
                searchHint = strings.search,
                showAlphabeticalList = showAlphabeticalList,
                showMenu = showMoreMenu,
                onOpenSpotlight = onOpenSpotlight,
                onMenuClick = { showMoreMenu = true },
                onDismissMenu = { showMoreMenu = false },
                onToggleList = {
                    showMoreMenu = false
                    showAlphabeticalList = !showAlphabeticalList
                },
                listLabel = if (showAlphabeticalList) strings.appLibraryCategories else "A–Z",
                settingsLabel = strings.settings,
                onOpenSettings = {
                    showMoreMenu = false
                    onOpenSettings()
                }
            )

            Spacer(Modifier.height(14.dp))

            if (showAlphabeticalList) {
                LibraryAlphabeticalList(
                    apps = visibleApps,
                    language = language,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isUnfolded) 3 else 2),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pods, key = { it.id }) { pod ->
                        LibraryCategoryPod(
                            title = pod.title,
                            apps = pod.apps,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick,
                            onPodClick = {
                                val cat = pod.category
                                if (cat != null) onCategoryClick(cat) else showAlphabeticalList = true
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun buildLibraryPods(
    categorizedApps: Map<AppCategory, List<AppInfo>>,
    allApps: List<AppInfo>,
    strings: IosStrings,
    language: com.tms.os.launcher.model.AppLanguage
): List<LibraryPod> {
    val pods = mutableListOf<LibraryPod>()
    val visible = allApps.filter { !it.isHidden }

    val suggestions = categorizedApps[AppCategory.SUGGESTIONS].orEmpty()
        .ifEmpty { visible.take(4) }
    if (suggestions.isNotEmpty()) {
        pods.add(
            LibraryPod(
                id = "pod_suggestions",
                title = strings.suggestions,
                apps = suggestions.take(4),
                category = AppCategory.SUGGESTIONS
            )
        )
    }

    val recentlyAdded = visible.sortedByDescending { it.firstInstallTime }.take(4)
    if (recentlyAdded.isNotEmpty()) {
        pods.add(
            LibraryPod(
                id = "pod_recent",
                title = strings.recentlyAdded,
                apps = recentlyAdded,
                category = null
            )
        )
    }

    for (category in AppCategory.entries) {
        if (category == AppCategory.SUGGESTIONS) continue
        val apps = categorizedApps[category].orEmpty()
        if (apps.isEmpty()) continue
        pods.add(
            LibraryPod(
                id = "pod_${category.id}",
                title = category.localizedTitle(language),
                apps = apps.take(7),
                category = category
            )
        )
    }
    return pods
}

/* -------------------------------------------------------------------------- */
/*                                Header pill                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun LibraryHeaderPill(
    title: String,
    searchHint: String,
    showAlphabeticalList: Boolean,
    showMenu: Boolean,
    onOpenSpotlight: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleList: () -> Unit,
    listLabel: String,
    settingsLabel: String,
    onOpenSettings: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(Color(0xB31C1C1E))
            .border(0.55.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenSpotlight
            )
            .padding(start = 15.dp, end = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = searchHint,
            tint = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            style = IosText.body.copy(fontSize = 16.5.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMenuClick
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "…",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(21.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu,
                modifier = Modifier.background(Color(0xF22C2C2E))
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (showAlphabeticalList) listLabel else "A–Z",
                            color = Color.White,
                            style = IosText.body.copy(fontSize = 16.sp)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = onToggleList
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                DropdownMenuItem(
                    text = {
                        Text(
                            text = settingsLabel,
                            color = Color.White,
                            style = IosText.body.copy(fontSize = 16.sp)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = onOpenSettings
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                               Category pod                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun LibraryCategoryPod(
    title: String,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onPodClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .background(Color(0xCC1C1C1E))
                .border(
                    0.55.dp,
                    Color.White.copy(alpha = 0.10f),
                    androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPodClick
                )
                .padding(13.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val gap = 9.dp
                val quadrant = (maxWidth - gap) / 2
                val largeIcon = quadrant * 0.86f
                val miniIcon = (quadrant - 5.dp) / 2 * 0.90f

                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LibraryAppIcon(apps.getOrNull(0), largeIcon, onAppClick, onAppLongClick)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LibraryAppIcon(apps.getOrNull(1), largeIcon, onAppClick, onAppLongClick)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LibraryAppIcon(apps.getOrNull(2), largeIcon, onAppClick, onAppLongClick)
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            val third = apps.getOrNull(3)
                            if (apps.size <= 4) {
                                LibraryAppIcon(third, largeIcon, onAppClick, onAppLongClick)
                            } else {
                                val extras = apps.drop(3).take(4)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onPodClick
                                        )
                                ) {
                                    for (row in 0..1) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            for (col in 0..1) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    LibraryAppIcon(
                                                        app = extras.getOrNull(row * 2 + col),
                                                        size = miniIcon,
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
                    }
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = title,
            color = Color.White.copy(alpha = 0.94f),
            style = IosText.footnote.copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LibraryAppIcon(
    app: AppInfo?,
    size: Dp,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    if (app == null) {
        Spacer(Modifier.size(size))
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(IosSquircle)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
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
            LibraryLetterIcon(app.displayLabel, size)
        }
    }
}

@Composable
private fun LibraryLetterIcon(label: String, size: Dp) {
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

/* -------------------------------------------------------------------------- */
/*                              Alphabetical list                             */
/* -------------------------------------------------------------------------- */

private data class LibraryLetterGroup(val letter: String, val apps: List<AppInfo>)

@Composable
private fun LibraryAlphabeticalList(
    apps: List<AppInfo>,
    language: com.tms.os.launcher.model.AppLanguage,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember(apps) {
        val map = LinkedHashMap<String, MutableList<AppInfo>>()
        for (app in apps) {
            val first = app.displayLabel.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (first in 'A'..'Z') first.toString() else "#"
            map.getOrPut(key) { mutableListOf() }.add(app)
        }
        map.map { (letter, list) -> LibraryLetterGroup(letter, list) }.sortedBy { it.letter }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
            .background(Color(0xCC1C1C1E))
            .border(
                0.55.dp,
                Color.White.copy(alpha = 0.10f),
                androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
            )
            .padding(start = 16.dp, top = 6.dp, bottom = 8.dp, end = 6.dp)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            groups.forEach { group ->
                item(key = "letter_${group.letter}") {
                    Column {
                        Text(
                            text = group.letter,
                            color = Color(0xFF0A84FF),
                            style = IosText.footnoteSemibold.copy(fontSize = 14.sp),
                            modifier = Modifier.padding(top = 12.dp, bottom = 5.dp)
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
                items(group.apps, key = { it.packageName }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                            .padding(vertical = 7.dp, horizontal = 2.dp)
                    ) {
                        LibraryAppIcon(
                            app = app,
                            size = 34.dp,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.displayLabel,
                                color = Color.White,
                                style = IosText.body.copy(fontSize = 16.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.category.localizedTitle(language),
                                color = Color.White.copy(alpha = 0.42f),
                                style = IosText.caption
                            )
                        }
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
        ) {
            groups.forEach { group ->
                Text(
                    text = group.letter,
                    color = Color(0xFF0A84FF),
                    style = IosText.caption.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                var index = 0
                                for (g in groups) {
                                    if (g.letter == group.letter) break
                                    index += 1 + g.apps.size
                                }
                                scope.launch { listState.scrollToItem(index) }
                            }
                        )
                        .padding(vertical = 1.dp)
                )
            }
        }
    }
}
