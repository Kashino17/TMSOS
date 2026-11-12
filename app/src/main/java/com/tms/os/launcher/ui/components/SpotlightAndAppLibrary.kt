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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SmartDisplay
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
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SpotlightSearchOverlay(
    query: String,
    results: List<AppInfo>,
    mathResult: String?,
    autoOpenKeyboard: Boolean = true,
    showWebSearch: Boolean = true,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onWebSearchClick: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val bgInteractionSource = remember { MutableInteractionSource() }

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

    // Outer full-screen backdrop: Tapping ANYWHERE on the background or swiping UP closes Spotlight!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x9908080C))
            .pointerInput(Unit) {
                var totalDy = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDy = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDy += dragAmount
                        if (totalDy < -42f) {
                            totalDy = 0f
                            dismissSpotlight()
                        }
                    }
                )
            }
            .clickable(
                interactionSource = bgInteractionSource,
                indication = null,
                onClick = dismissSpotlight
            )
            .padding(top = 46.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        // NOTE: This Column wraps only its content height so any tap below the cards hits the backdrop and closes Spotlight!
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top iOS 18 Frosted Spotlight Search Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume taps inside the search bar row
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), clip = false)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xEE343438), Color(0xEE26262A))
                            )
                        )
                        .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp)
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
                                text = "Apps, Kategorien oder Rechnung suchen…",
                                color = Color.White.copy(alpha = 0.48f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFF0A84FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    results.firstOrNull()?.let { onAppClick(it) }
                                }
                            ),
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
                        .clickable { dismissSpotlight() }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instant Math / Calculator Card if query is an arithmetic expression
            if (mathResult != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xEE2C2C2E), Color(0xEE1C1C1E))
                            )
                        )
                        .border(0.8.dp, Color(0xFFFF9F0A).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF9F0A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "$query =",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = mathResult,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Sofort-Rechner",
                        color = Color(0xFFFF9F0A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Frosted Siri Suggestions / App Results Card (Compact height so area below remains clickable to dismiss!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE62C2C2E), Color(0xE61C1C1E))
                        )
                    )
                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Only consume clicks inside the card itself
                    )
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                if (results.isEmpty()) {
                    Text(
                        text = "Keine installierte App für „$query“ gefunden.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                } else {
                    val displayedApps = results.take(8)
                    val rows = displayedApps.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                                iconSize = 58.dp,
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

                // Quick Web Search Shortcuts when user typed a query
                if (showWebSearch && query.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.14f))
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

            // Hint text on empty space below card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Text(
                    text = "Tippe auf den freien Hintergrund oder wische nach oben zum Schließen",
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 11.5.sp
                )
            }
        }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        // Authentic iOS App Library Frosted Top Search Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC2C2C2E))
                    .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (libraryFilter.isEmpty()) {
                        Text(
                            text = "App-Mediathek",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    BasicTextField(
                        value = libraryFilter,
                        onValueChange = {
                            libraryFilter = it
                            if (it.isNotEmpty()) showAlphabeticalList = true
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Color(0xFF0A84FF)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (libraryFilter.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Leeren",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { libraryFilter = "" }
                    )
                }
            }

            // Toggle between Smart Categories & A-Z List with Index Scroller
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (showAlphabeticalList) Color(0xFF0A84FF) else Color(0xCC2C2C2E))
                    .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAlphabeticalList = !showAlphabeticalList
                    }
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = if (showAlphabeticalList) "Kategorien" else "A–Z",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Direct shortcut to TMS OS Command Center Settings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC2C2C2E))
                    .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .clickable { onOpenSettingsClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "TMS OS Einstellungen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showAlphabeticalList || libraryFilter.isNotBlank()) {
            // Authentic iOS A-Z Vertical List + Right-Edge Fast Alphabet Index Scroller!
            val letters = remember(groupedByLetter) { groupedByLetter.keys.toList() }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xD8161618))
                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
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
                                Box(modifier = Modifier.size(44.dp)) {
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

                // Right-side iOS A-Z Quick Jump Index Bar
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
            // Authentic iOS Square Frosted Category Pods Grid
            val columnsCount = if (isUnfolded) 3 else 2
            val entries = categorizedApps.entries.toList()

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(bottom = 84.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(entries, key = { it.key.id }) { (category, appsInCat) ->
                    IosAppLibraryCategoryPod(
                        category = category,
                        apps = appsInCat,
                        glassOpacity = glassOpacity,
                        appLanguage = appLanguage,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        onExpandCategory = { onCategoryExpandClick(category) }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = "${allApps.count { !it.isHidden }} Apps automatisch in ${entries.size} iOS-Kategorien sortiert",
                            color = Color.White.copy(alpha = 0.50f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IosAppLibraryCategoryPod(
    category: AppCategory,
    apps: List<AppInfo>,
    glassOpacity: Float,
    appLanguage: AppLanguage,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onExpandCategory: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // True Square iOS Frosted Glass Pod
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(12.dp, RoundedCornerShape(30.dp), clip = false)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3A3A3C).copy(alpha = glassOpacity.coerceIn(0.45f, 0.92f)),
                            Color(0xFF222225).copy(alpha = (glassOpacity + 0.06f).coerceIn(0.50f, 0.95f))
                        )
                    )
                )
                .border(0.85.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(30.dp))
                .clickable { onExpandCategory() }
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Row 1: Quadrants 0 & 1
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PodDirectAppSlot(apps.getOrNull(0), onAppClick, onAppLongClick)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PodDirectAppSlot(apps.getOrNull(1), onAppClick, onAppLongClick)
                    }
                }

                // Row 2: Quadrant 2 & either Quadrant 3 (if <= 4 apps) OR 2x2 Mini Cluster
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PodDirectAppSlot(apps.getOrNull(2), onAppClick, onAppLongClick)
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (apps.size <= 4) {
                            PodDirectAppSlot(apps.getOrNull(3), onAppClick, onAppLongClick)
                        } else {
                            // 2x2 Mini-Icon Cluster that opens the full Category Modal
                            val extraApps = apps.drop(3).take(4)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable { onExpandCategory() }
                                    .padding(2.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    for (r in 0..1) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            for (c in 0..1) {
                                                val miniApp = extraApps.getOrNull(r * 2 + c)
                                                Box(
                                                    modifier = Modifier.size(25.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (miniApp?.iconBitmap != null) {
                                                        Image(
                                                            bitmap = miniApp.iconBitmap,
                                                            contentDescription = null,
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
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = category.localizedTitle(appLanguage),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PodDirectAppSlot(
    app: AppInfo?,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    if (app == null) {
        Spacer(modifier = Modifier.size(58.dp))
    } else {
        IosAppIconView(
            app = app,
            iconSize = 58.dp,
            showLabel = false,
            showBadges = true,
            isJiggleMode = false,
            onClick = { onAppClick(app) },
            onLongClick = { onAppLongClick(app) }
        )
    }
}
