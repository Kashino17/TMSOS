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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem

@Composable
fun IosFrostedDock(
    dockApps: List<AppInfo>,
    recentApps: List<AppInfo>,
    isUnfolded: Boolean,
    showRecentInUnfolded: Boolean,
    showLabels: Boolean,
    showBadges: Boolean,
    iconSize: Dp,
    isJiggleMode: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(32.dp), clip = false)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC38383C),
                            Color(0xCC222226)
                        )
                    )
                )
                .border(
                    width = 0.85.dp,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            dockApps.forEach { app ->
                IosAppIconView(
                    app = app,
                    iconSize = iconSize,
                    showLabel = showLabels,
                    showBadges = showBadges,
                    isJiggleMode = isJiggleMode,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }

            // iPadOS-style Recent Apps section on the unfolded Galaxy Fold main screen!
            if (isUnfolded && showRecentInUnfolded && recentApps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(iconSize * 0.75f)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                recentApps.forEach { recentApp ->
                    IosAppIconView(
                        app = recentApp,
                        iconSize = iconSize,
                        showLabel = showLabels,
                        showBadges = showBadges,
                        isJiggleMode = false,
                        onClick = { onAppClick(recentApp) },
                        onLongClick = { onAppLongClick(recentApp) }
                    )
                }
            }
        }
    }
}

@Composable
fun IosExpandedFolderModal(
    folder: HomeGridItem.FolderItem,
    appMap: Map<String, AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onRenameFolder: (String) -> Unit,
    onUnpackFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    var titleText by remember(folder.folderId, folder.title) { mutableStateOf(folder.title) }
    val folderApps = remember(folder.appPackages, appMap) {
        folder.appPackages.mapNotNull { appMap[it] }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC09090D))
            .clickable(onClick = onDismiss)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clickable(enabled = false) {}
        ) {
            // Editable Folder Title
            BasicTextField(
                value = titleText,
                onValueChange = {
                    titleText = it
                    onRenameFolder(it)
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Frosted Glass Folder Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp)
                    .shadow(20.dp, RoundedCornerShape(36.dp), clip = false)
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xEE2C2C2E), Color(0xEE1C1C1E))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(36.dp))
                    .padding(20.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folderApps, key = { it.packageName }) { app ->
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onUnpackFolder) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ordner auflösen",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Fertig",
                        color = Color(0xFF0A84FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun IosCategoryExpandedModal(
    category: AppCategory,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD809090D))
            .clickable(onClick = onDismiss)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clickable(enabled = false) {}
        ) {
            Text(
                text = category.titleDe,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${category.subtitleDe} • ${apps.size} Apps",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 480.dp)
                    .shadow(20.dp, RoundedCornerShape(36.dp), clip = false)
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xEE2C2C2E), Color(0xEE1C1C1E))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(36.dp))
                    .padding(20.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
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

@Composable
fun IosHapticTouchContextMenu(
    app: AppInfo,
    isPinnedInDock: Boolean,
    existingFolders: List<HomeGridItem.FolderItem>,
    onLaunchApp: () -> Unit,
    onToggleDockPin: () -> Unit,
    onMovePositionLeft: () -> Unit,
    onMovePositionRight: () -> Unit,
    onEnterJiggleMode: () -> Unit,
    onChangeCategory: (AppCategory) -> Unit,
    onMoveToFolder: (String) -> Unit,
    onCreateNewFolder: (String) -> Unit,
    onRenameApp: (String?) -> Unit,
    onHideApp: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstallApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var subMenuState by remember { mutableStateOf("main") } // "main", "category", "folder", "rename"
    var customNameInput by remember(app.displayLabel) { mutableStateOf(app.displayLabel) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC08080C))
            .clickable(onClick = onDismiss)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clickable(enabled = false) {}
        ) {
            // App Preview Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xEE252529))
                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                    .clickable { onLaunchApp() }
                    .padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                ) {
                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap,
                            contentDescription = app.displayLabel,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayLabel,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Kategorie: ${app.category.titleDe}",
                        color = Color(0xFF0A84FF),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Context Menu Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xF2252529))
                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            ) {
                when (subMenuState) {
                    "main" -> {
                        // Position Shift Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            TextButton(onClick = onMovePositionLeft) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Nach vorn", color = Color.White, fontSize = 13.sp)
                            }
                            TextButton(onClick = onMovePositionRight) {
                                Text("Nach hinten", color = Color.White, fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                        MenuDivider()

                        ContextMenuRow(
                            title = "Home-Bildschirm bearbeiten",
                            icon = Icons.Default.Edit,
                            onClick = onEnterJiggleMode
                        )
                        MenuDivider()

                        ContextMenuRow(
                            title = if (isPinnedInDock) "Aus dem Dock entfernen" else "Im Dock anheften",
                            icon = Icons.Default.PushPin,
                            onClick = onToggleDockPin
                        )
                        MenuDivider()

                        ContextMenuRow(
                            title = "Kategorie ändern (${app.category.titleDe})",
                            icon = Icons.Default.Category,
                            onClick = { subMenuState = "category" }
                        )
                        MenuDivider()

                        ContextMenuRow(
                            title = "In Ordner verschieben…",
                            icon = Icons.Default.CreateNewFolder,
                            onClick = { subMenuState = "folder" }
                        )
                        MenuDivider()

                        ContextMenuRow(
                            title = "App-Namen umbenennen",
                            icon = Icons.Default.Edit,
                            onClick = { subMenuState = "rename" }
                        )
                        MenuDivider()

                        ContextMenuRow(
                            title = "App ausblenden",
                            icon = Icons.Default.VisibilityOff,
                            onClick = onHideApp
                        )
                        MenuDivider()

                        ContextMenuRow(
                            title = "App-Details & System-Info",
                            icon = Icons.Default.Info,
                            onClick = onOpenAppInfo
                        )

                        if (!app.isSystemApp) {
                            MenuDivider()
                            ContextMenuRow(
                                title = "App deinstallieren",
                                icon = Icons.Default.DeleteOutline,
                                textColor = Color(0xFFFF453A),
                                onClick = onUninstallApp
                            )
                        }
                    }

                    "category" -> {
                        Text(
                            text = "Kategorie für „${app.displayLabel}“ wählen",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp)
                        )
                        MenuDivider()
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(AppCategory.entries.filter { it != AppCategory.SUGGESTIONS }) { cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onChangeCategory(cat)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = cat.titleDe,
                                        color = if (cat == app.category) Color(0xFF0A84FF) else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (cat == app.category) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = cat.subtitleDe,
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp
                                    )
                                }
                                MenuDivider()
                            }
                        }
                    }

                    "folder" -> {
                        Text(
                            text = "In Ordner verschieben",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp)
                        )
                        MenuDivider()
                        ContextMenuRow(
                            title = "+ Neuen Ordner (${app.category.titleDe}) erstellen",
                            icon = Icons.Default.CreateNewFolder,
                            textColor = Color(0xFF0A84FF),
                            onClick = { onCreateNewFolder(app.category.titleDe) }
                        )
                        MenuDivider()
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                            items(existingFolders, key = { it.folderId }) { folder ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onMoveToFolder(folder.folderId) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = folder.title,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${folder.appPackages.size} Apps",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                                MenuDivider()
                            }
                        }
                    }

                    "rename" -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Anzeigename anpassen",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            BasicTextField(
                                value = customNameInput,
                                onValueChange = { customNameInput = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161618))
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = {
                                    onRenameApp(null)
                                    onDismiss()
                                }) {
                                    Text("Zurücksetzen", color = Color.White.copy(alpha = 0.6f))
                                }
                                TextButton(onClick = {
                                    onRenameApp(customNameInput)
                                    onDismiss()
                                }) {
                                    Text("Speichern", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    title: String,
    icon: ImageVector,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.12f)
    )
}
