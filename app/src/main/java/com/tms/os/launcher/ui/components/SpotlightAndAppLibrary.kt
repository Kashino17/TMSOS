package com.tms.os.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import java.util.Locale

@Composable
fun SpotlightSearchOverlay(
    query: String,
    results: List<AppInfo>,
    mathResult: String?,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE80D0D12))
            .clickable(onClick = onClose)
            .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
        ) {
            // Top iOS Spotlight Search Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF252529))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Apps, Kategorien oder Rechnung suchen…",
                                color = Color.White.copy(alpha = 0.45f),
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
                                .background(Color.White.copy(alpha = 0.2f))
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

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Abbrechen",
                    color = Color(0xFF0A84FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instant Math / Calculator Card if query is an arithmetic expression
            if (mathResult != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1F2937), Color(0xFF111827))
                            )
                        )
                        .border(1.dp, Color(0xFF0A84FF).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
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
                        text = "Rechner",
                        color = Color(0xFF0A84FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Section Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (query.isBlank()) "Siri-Vorschläge & Häufig genutzt" else "Treffer (${results.size})",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (query.isNotBlank() && results.isNotEmpty()) {
                    Text(
                        text = "Enter = ${results.first().displayLabel} öffnen",
                        color = Color(0xFF0A84FF),
                        fontSize = 12.sp
                    )
                }
            }

            // Results Container Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xCC1C1C1E))
                    .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    .padding(12.dp)
            ) {
                if (results.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp)
                    ) {
                        Text(
                            text = "Keine passende App für „$query“",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tipp: Du kannst auch nach Kategorien wie „Finanzen“, „Spiele“ oder Kürzeln wie „wa“ suchen.",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 78.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { it.packageName }) { app ->
                            IosAppIconView(
                                app = app,
                                iconSize = 58.dp,
                                showLabel = true,
                                showBadges = true,
                                isJiggleMode = false,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IosAppLibraryScreen(
    categorizedApps: Map<AppCategory, List<AppInfo>>,
    allApps: List<AppInfo>,
    isUnfolded: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onCategoryExpandClick: (AppCategory) -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var libraryFilter by remember { mutableStateOf("") }
    var showAlphabeticalList by remember { mutableStateOf(false) }

    val filteredAlphaApps = remember(allApps, libraryFilter) {
        val visible = allApps.filter { !it.isHidden }
        if (libraryFilter.isBlank()) visible
        else visible.filter {
            it.displayLabel.contains(libraryFilter, ignoreCase = true) ||
                it.category.titleDe.contains(libraryFilter, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 40.dp, start = 14.dp, end = 14.dp, bottom = 12.dp)
    ) {
        // App Library Top Search & Mode Switcher Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC252529))
                    .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp)
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
                            text = "App-Mediathek durchsuchen",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
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

            // Toggle between Smart Categories & A-Z List
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (showAlphabeticalList) Color(0xFF0A84FF) else Color(0xCC252529))
                    .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .clickable { showAlphabeticalList = !showAlphabeticalList }
                    .padding(horizontal = 12.dp)
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC252529))
                    .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
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
            // Alphabetical A-Z App List
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xBB161618))
                    .padding(12.dp)
            ) {
                items(filteredAlphaApps, key = { it.packageName }) { app ->
                    IosAppIconView(
                        app = app,
                        iconSize = 58.dp,
                        showLabel = true,
                        showBadges = true,
                        isJiggleMode = false,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
        } else {
            // Authentic iOS Smart Category Pods Grid (2 columns on Cover, 3 columns on Unfolded Fold!)
            val columnsCount = if (isUnfolded) 3 else 2
            val entries = categorizedApps.entries.toList()

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(entries, key = { it.key.id }) { (category, appsInCat) ->
                    IosAppLibraryCategoryPod(
                        category = category,
                        apps = appsInCat,
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
                            text = "${allApps.count { !it.isHidden }} Apps automatisch in ${entries.size} Kategorien sortiert",
                            color = Color.White.copy(alpha = 0.55f),
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
                .height(156.dp)
                .shadow(10.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD2C2C2E),
                            Color(0xDD1C1C1E)
                        )
                    )
                )
                .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                .clickable { onExpandCategory() }
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Row 1: items 0 and 1
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PodDirectAppSlot(apps.getOrNull(0), onAppClick, onAppLongClick)
                    PodDirectAppSlot(apps.getOrNull(1), onAppClick, onAppLongClick)
                }

                // Row 2: item 2 and either item 3 (if <= 4 apps) OR 2x2 mini cluster to expand category
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PodDirectAppSlot(apps.getOrNull(2), onAppClick, onAppLongClick)

                    if (apps.size <= 4) {
                        PodDirectAppSlot(apps.getOrNull(3), onAppClick, onAppLongClick)
                    } else {
                        // 2x2 Mini-Icon Cluster that opens the full Category Modal
                        val extraApps = apps.drop(3).take(4)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onExpandCategory() }
                                .padding(5.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                for (r in 0..1) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (c in 0..1) {
                                            val miniApp = extraApps.getOrNull(r * 2 + c)
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                            ) {
                                                if (miniApp?.iconBitmap != null) {
                                                    Image(
                                                        bitmap = miniApp.iconBitmap,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
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

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${category.titleDe} (${apps.size})",
            color = Color.White,
            fontSize = 12.5.sp,
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
        Spacer(modifier = Modifier.size(54.dp))
    } else {
        IosAppIconView(
            app = app,
            iconSize = 52.dp,
            showLabel = false,
            showBadges = true,
            isJiggleMode = false,
            onClick = { onAppClick(app) },
            onLongClick = { onAppLongClick(app) }
        )
    }
}
