package com.tms.os.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.AppShortcutItem
import com.tms.os.launcher.model.ContactItem
import kotlinx.coroutines.launch

private data class LibraryPodData(
    val id: String,
    val title: String,
    val apps: List<AppInfo>,
    val category: AppCategory? = null
)

@Composable
fun SpotlightSearchOverlay(
    query: String,
    results: List<AppInfo>,
    shortcuts: List<AppShortcutItem> = emptyList(),
    contacts: List<ContactItem> = emptyList(),
    mathResult: String?,
    autoOpenKeyboard: Boolean = true,
    showWebSearch: Boolean = true,
    searchBarAtBottom: Boolean = false,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onContactClick: (ContactItem) -> Unit = {},
    onWebSearchClick: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dismissSpotlight = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onClose()
    }

    LaunchedEffect(autoOpenKeyboard) {
        if (autoOpenKeyboard) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Outer full-screen backdrop: Tapping ANYWHERE on the background or swiping UP/DOWN closes Spotlight!
    Box(
        contentAlignment = if (searchBarAtBottom) Alignment.BottomCenter else Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA0A0A0E))
            .pointerInput(Unit) {
                var totalDy = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDy = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDy += dragAmount
                        if (totalDy < -36f || totalDy > 65f) {
                            totalDy = 0f
                            dismissSpotlight()
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismissSpotlight
            )
            .imePadding()
            .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!searchBarAtBottom) {
                SpotlightSearchPillRow(
                    query = query,
                    focusRequester = focusRequester,
                    onQueryChange = onQueryChange,
                    onGo = { results.firstOrNull()?.let { onAppClick(it) } },
                    onDismiss = dismissSpotlight
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Instant Math / Currency / Unit Conversion Card
            if (mathResult != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xEE242428))
                        .border(0.8.dp, Color(0xFFFF9F0A).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF9F0A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$query =",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = mathResult,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Rechner",
                        color = Color(0xFFFF9F0A),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick App Shortcuts Card
            if (shortcuts.isNotEmpty() && query.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    shortcuts.take(2).forEach { sc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xE628282C))
                                .border(0.75.dp, Color(0xFF0A84FF).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .clickable {
                                    results.firstOrNull { it.packageName == sc.packageName }?.let { onAppClick(it) }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFD60A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = sc.shortLabel,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sc.appLabel,
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 10.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Contacts Direct-Call Card
            if (contacts.isNotEmpty() && query.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE6252529))
                        .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "KONTAKTE DIREKTWAHL",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    contacts.take(3).forEach { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onContactClick(contact) }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF30D158))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Anrufen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Frosted Siri Suggestions / App Results Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xD9242428))
                    .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (query.isBlank()) "SIRI-VORSCHLÄGE" else "TOP-TREFFER (${results.size})",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    if (query.isNotBlank() && results.isNotEmpty()) {
                        Text(
                            text = "Öffnen: ${results.first().displayLabel}",
                            color = Color(0xFF0A84FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onAppClick(results.first()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (results.isEmpty()) {
                    Text(
                        text = "Keine installierte App für „$query“ gefunden.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                    )
                } else {
                    val displayedApps = results.take(8)
                    val rows = displayedApps.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (row in rows) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (i in 0 until 4) {
                                    val app = row.getOrNull(i)
                                    Box(
                                        contentAlignment = Alignment.TopCenter,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (app != null) {
                                            IosAppIconView(
                                                app = app,
                                                iconSize = 64.dp,
                                                showLabel = true,
                                                showBadges = true,
                                                isJiggleMode = false,
                                                onClick = {
                                                    keyboardController?.hide()
                                                    onAppClick(app)
                                                },
                                                onLongClick = {
                                                    keyboardController?.hide()
                                                    onAppLongClick(app)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showWebSearch && query.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpotlightActionPill(
                            icon = Icons.Default.Public,
                            label = "Google",
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.weight(1f),
                            onClick = { onWebSearchClick(query, "google") }
                        )
                        SpotlightActionPill(
                            icon = Icons.Default.SmartDisplay,
                            label = "YouTube",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.weight(1f),
                            onClick = { onWebSearchClick(query, "youtube") }
                        )
                        SpotlightActionPill(
                            icon = Icons.Default.Shop,
                            label = "Play Store",
                            tint = Color(0xFF30D158),
                            modifier = Modifier.weight(1f),
                            onClick = { onWebSearchClick(query, "play") }
                        )
                    }
                }
            }

            if (searchBarAtBottom) {
                Spacer(modifier = Modifier.height(12.dp))
                SpotlightSearchPillRow(
                    query = query,
                    focusRequester = focusRequester,
                    onQueryChange = onQueryChange,
                    onGo = { results.firstOrNull()?.let { onAppClick(it) } },
                    onDismiss = dismissSpotlight
                )
            }
        }
    }
}

@Composable
private fun SpotlightSearchPillRow(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onGo: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xE636363A))
                .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(23.dp))
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Suchen",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Color(0xFF0A84FF)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onGo() }),
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
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable { onQueryChange("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Löschen",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Abbrechen",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SpotlightActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * 1:1 Authentic iOS App-Bibliothek Screen (Exact match to Screenshot 2!)
 * - Top full-width Frosted Pill with centered "🔍 App-Bibliothek" and "⋮" menu icon on the right.
 * - 2-column square pods where the 3 primary icons + 2x2 mini-cluster fill the pod edge-to-edge.
 * - Includes "Vorschläge" + "Kürzlich hinzugefügt" + all smart categories.
 */
@Composable
fun IosAppLibraryScreen(
    categorizedApps: Map<AppCategory, List<AppInfo>>,
    allApps: List<AppInfo>,
    isUnfolded: Boolean,
    glassOpacity: Float = 0.78f,
    appLanguage: AppLanguage = AppLanguage.DE,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onCategoryExpandClick: (AppCategory) -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var libraryFilter by remember { mutableStateOf("") }
    var showAlphabeticalList by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val alphaListState = rememberLazyListState()

    val filteredAlphaApps = remember(allApps, libraryFilter) {
        val visible = allApps.filter { !it.isHidden }
        if (libraryFilter.isBlank()) visible
        else visible.filter {
            it.displayLabel.contains(libraryFilter, ignoreCase = true) ||
                it.category.localizedTitle(appLanguage).contains(libraryFilter, ignoreCase = true)
        }
    }

    val groupedByLetter = remember(filteredAlphaApps) {
        filteredAlphaApps.groupBy { app ->
            val first = app.displayLabel.firstOrNull()?.uppercaseChar() ?: '#'
            if (first in 'A'..'Z') first.toString() else "#"
        }.toSortedMap()
    }

    // Build pods matching Screenshot 2: "Vorschläge", "Kürzlich hinzugefügt", then Smart Categories
    val pods = remember(categorizedApps, allApps, appLanguage) {
        val list = mutableListOf<LibraryPodData>()
        val visible = allApps.filter { !it.isHidden }

        val suggestions = categorizedApps[AppCategory.SUGGESTIONS].orEmpty().ifEmpty {
            visible.take(4)
        }
        if (suggestions.isNotEmpty()) {
            list.add(
                LibraryPodData(
                    id = "pod_suggestions",
                    title = "Vorschläge",
                    apps = suggestions.take(4),
                    category = AppCategory.SUGGESTIONS
                )
            )
        }

        val recentlyAdded = visible.sortedByDescending { it.firstInstallTime }.take(8)
        if (recentlyAdded.isNotEmpty()) {
            list.add(
                LibraryPodData(
                    id = "pod_recent",
                    title = "Kürzlich hinzugefügt",
                    apps = recentlyAdded,
                    category = null
                )
            )
        }

        for ((cat, appsInCat) in categorizedApps) {
            if (cat == AppCategory.SUGGESTIONS || appsInCat.isEmpty()) continue
            list.add(
                LibraryPodData(
                    id = "pod_${cat.id}",
                    title = cat.localizedTitle(appLanguage),
                    apps = appsInCat,
                    category = cat
                )
            )
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 18.dp, end = 18.dp, bottom = 4.dp)
    ) {
        // 1:1 Authentic iOS App-Bibliothek Top Pill ("🔍 App-Bibliothek" centered + "⋮" on right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xA62C2C2E))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp)
        ) {
            // Centered Search Icon + Input / Placeholder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.82f)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    if (libraryFilter.isEmpty()) {
                        Text(
                            text = "App-Bibliothek",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = libraryFilter,
                        onValueChange = {
                            libraryFilter = it
                            if (it.isNotEmpty()) showAlphabeticalList = true
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color(0xFF0A84FF))
                    )
                }
                if (libraryFilter.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Leeren",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                libraryFilter = ""
                                showAlphabeticalList = false
                            }
                    )
                }
            }

            // Right-aligned Three-Dots Menu ("⋮") inside the pill
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menü",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showMoreMenu = true }
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (showAlphabeticalList) "Kategorie-Kacheln anzeigen" else "Alphabetische A–Z Liste",
                                color = Color.White
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            showAlphabeticalList = !showAlphabeticalList
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("TMS OS Einstellungen", color = Color(0xFF0A84FF)) },
                        onClick = {
                            showMoreMenu = false
                            onOpenSettingsClick()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showAlphabeticalList || libraryFilter.isNotBlank()) {
            val letters = remember(groupedByLetter) { groupedByLetter.keys.toList() }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xD8161618))
                    .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 6.dp)
            ) {
                LazyColumn(
                    state = alphaListState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    groupedByLetter.entries.forEach { (letter, appsForLetter) ->
                        item(key = "header_$letter") {
                            Text(
                                text = letter,
                                color = Color(0xFF0A84FF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = Color.White.copy(alpha = 0.12f)
                            )
                        }
                        items(appsForLetter, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppClick(app) }
                                    .padding(vertical = 7.dp)
                            ) {
                                Box(modifier = Modifier.size(48.dp)) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.displayLabel,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.displayLabel,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = app.category.localizedTitle(appLanguage),
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 12.sp
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
                        .width(24.dp)
                        .fillMaxHeight()
                ) {
                    letters.forEach { letter ->
                        Text(
                            text = letter,
                            color = Color(0xFF0A84FF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    var targetIdx = 0
                                    for ((k, v) in groupedByLetter) {
                                        if (k == letter) break
                                        targetIdx += 1 + v.size
                                    }
                                    scope.launch {
                                        alphaListState.scrollToItem(targetIdx)
                                    }
                                }
                                .padding(vertical = 1.5.dp)
                        )
                    }
                }
            }
        } else {
            // 1:1 Authentic iOS App-Bibliothek Square Frosted Pods Grid (Exact match to Screenshot 2!)
            val columnsCount = if (isUnfolded) 3 else 2

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(bottom = 32.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pods, key = { it.id }) { pod ->
                    IosAppLibraryCategoryPod(
                        title = pod.title,
                        apps = pod.apps,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        onExpandCategory = {
                            if (pod.category != null && pod.category != AppCategory.SUGGESTIONS) {
                                onCategoryExpandClick(pod.category)
                            } else {
                                showAlphabeticalList = true
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 1:1 Authentic iOS App-Bibliothek Category Pod (Screenshot 2):
 * - Rounded square (`RoundedCornerShape(30.dp)`), dark translucent glass (`Color(0xA6242426)`).
 * - Inside padding `12.dp`, row/column spacing `10.dp`.
 * - Each of the 3 primary app icons uses `Modifier.fillMaxSize()` so it fills its quadrant completely!
 * - The 4th quadrant (if > 4 apps) renders a 2x2 sub-grid (`Arrangement.spacedBy(5.dp)`) where each
 *   mini-icon fills its sub-cell edge-to-edge!
 */
@Composable
private fun IosAppLibraryCategoryPod(
    title: String,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onExpandCategory: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xA6242426))
                .border(0.5.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(30.dp))
                .clickable { onExpandCategory() }
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Row 1: Quadrant 0 & Quadrant 1
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PodFullBleedAppIcon(apps.getOrNull(0), onAppClick, onAppLongClick)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PodFullBleedAppIcon(apps.getOrNull(1), onAppClick, onAppLongClick)
                    }
                }

                // Row 2: Quadrant 2 & Quadrant 3 (Direct icon if <= 4 apps, else 2x2 cluster filling Quadrant 3!)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PodFullBleedAppIcon(apps.getOrNull(2), onAppClick, onAppLongClick)
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (apps.size <= 4) {
                            PodFullBleedAppIcon(apps.getOrNull(3), onAppClick, onAppLongClick)
                        } else {
                            val extraApps = apps.drop(3).take(4)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onExpandCategory() }
                                    .padding(1.dp)
                            ) {
                                for (r in 0..1) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        for (c in 0..1) {
                                            val miniApp = extraApps.getOrNull(r * 2 + c)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (miniApp?.iconBitmap != null) {
                                                    Image(
                                                        bitmap = miniApp.iconBitmap,
                                                        contentDescription = miniApp.displayLabel,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize()
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

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PodFullBleedAppIcon(
    app: AppInfo?,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    if (app?.iconBitmap != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onAppClick(app) }
        ) {
            Image(
                bitmap = app.iconBitmap,
                contentDescription = app.displayLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Spacer(modifier = Modifier.fillMaxSize())
    }
}
