package com.tms.os.launcher.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.FoldPostureMode
import com.tms.os.launcher.model.PageTransitionEffect
import com.tms.os.launcher.viewmodel.LauncherViewModel

@Composable
fun CommandCenterSettingsScreen(
    viewModel: LauncherViewModel,
    onBackToHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val context = LocalContext.current
    var activeSubPage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (activeSubPage != null) {
            SettingsSubPageContent(
                subPage = activeSubPage!!,
                viewModel = viewModel,
                onBack = { activeSubPage = null }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Header Card (exact match to screenshot with floating "OS" badge & "Explore >" pill)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(
                                    top = 48.dp,
                                    start = 22.dp,
                                    end = 22.dp,
                                    bottom = 22.dp
                                )
                        ) {
                            Text(
                                text = "Launcher Phone OS",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Willkommen in deinem persönlichen Command Center – kostenlos, werbefrei & optimiert für dein Samsung Galaxy Fold.",
                                color = Color(0xFF98989F),
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TMS OS Version 1.0.0-FoldEdition (Keine Abo-Kosten)",
                                color = Color(0xFF0A84FF),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Floating Circular Rainbow "OS" Logo (top-left overlapping the card like the screenshot)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset(x = 18.dp, y = (-26).dp)
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF050507))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFA6FF00),
                                                    Color(0xFFFF9500),
                                                    Color(0xFFFF2D55),
                                                    Color(0xFFAF52DE)
                                                )
                                            ),
                                            fontWeight = FontWeight.Light,
                                            fontSize = 34.sp
                                        )
                                    ) {
                                        append("OS")
                                    }
                                }
                            )
                        }

                        // Blue "Homescreen >" ("Explore >") Pill Button in top-right of card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-18).dp, y = 16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A84FF))
                                .clickable { onBackToHome() }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "Homescreen",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Status Banner if an action was triggered (e.g. 1-Click Smart Sort)
                if (statusMessage != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF30D158).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFF30D158).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF30D158),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusMessage!!,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section: "Intelligente Funktionen (Fold & Auto-Sortierung)"
                item {
                    Text(
                        text = "Intelligente Funktionen",
                        color = Color(0xFF8E8E93),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FeatureActionRow(
                            icon = Icons.Default.AutoAwesome,
                            iconBgColors = listOf(Color(0xFFFF9F0A), Color(0xFFFF375F)),
                            title = "Homescreen in Smart-Ordner sortieren",
                            subtitle = "Gruppiert alle Apps automatisch nach Kategorien",
                            onClick = {
                                viewModel.autoOrganizeHomeWithSmartFolders()
                                statusMessage = "Alle Apps wurden intelligent in iOS-Kategorie-Ordner sortiert!"
                            }
                        )

                        FeatureActionRow(
                            icon = Icons.Default.DevicesFold,
                            iconBgColors = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)),
                            title = "Galaxy Fold Display-Modus (${settings.foldMode.titleDe})",
                            subtitle = "Anpassen, wie sich der Innenscreen aufgeklappt verhält",
                            onClick = { activeSubPage = "fold" }
                        )
                    }
                }

                // Section: "Allgemein" (Exact menu rows from the user's screenshot!)
                item {
                    Text(
                        text = "Allgemein",
                        color = Color(0xFF8E8E93),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1C1C1E))
                    ) {
                        SettingsMenuRow(
                            icon = Icons.Default.Settings,
                            iconBg = Color(0xFF00C7BE),
                            title = "Allgemein & Suche",
                            subtitle = if ( settings.instantLaunchSingleSearchResult ) "Direkt-Start aktiv" else "Spotlight & Sortierung",
                            onClick = { activeSubPage = "general" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Palette,
                            iconBg = Color(0xFFCE52C9),
                            title = "Erscheinungsbild",
                            subtitle = "Icon-Größe ${(settings.iconSizeScale * 100).toInt()}%, Labels & Dock",
                            onClick = { activeSubPage = "appearance" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Cloud,
                            iconBg = Color(0xFF32ADE6),
                            title = "Wetter & Smart Widgets",
                            subtitle = "${settings.weatherCity}, ${settings.weatherTempCelsius}°C",
                            onClick = { activeSubPage = "weather" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.CompareArrows,
                            iconBg = Color(0xFFAF52DE),
                            title = "Seitenübergang",
                            subtitle = settings.pageTransition.titleDe,
                            onClick = { activeSubPage = "transition" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.GridView,
                            iconBg = Color(0xFF5856D6),
                            title = "Screen Grid & Fold-Layout",
                            subtitle = "Außen ${settings.coverColumns}x${settings.coverRows} • Innen ${settings.mainUnfoldedColumns}x${settings.mainUnfoldedRows}",
                            onClick = { activeSubPage = "grid" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.NotificationsNone,
                            iconBg = Color(0xFF5AC8FA),
                            title = "Badge-Benachrichtigungen",
                            subtitle = if (state.isBadgeServiceEnabled) "Aktiviert" else "Berechtigung prüfen",
                            onClick = { activeSubPage = "badges" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Visibility,
                            iconBg = Color(0xFF8E8E93),
                            title = "Ausgeblendete Apps & Kategorien",
                            subtitle = "${state.allApps.count { it.isHidden }} ausgeblendet • ${state.allApps.size} gesamt",
                            onClick = { activeSubPage = "hidden" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Language,
                            iconBg = Color(0xFF9B8BF4),
                            title = "Sprache",
                            subtitle = "Deutsch (Systemstandard)",
                            onClick = {
                                statusMessage = "Sprache ist auf Deutsch (DACH) optimiert."
                            }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Home,
                            iconBg = Color(0xFF00C7BE),
                            title = "Standard-Startbildschirm auswählen",
                            subtitle = if (state.isDefaultLauncher) "TMS OS ist als Standard aktiv" else "Tippen, um TMS OS als Standard festzulegen",
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureActionRow(
    icon: ImageVector,
    iconBgColors: List<Color>,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141416))
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(iconBgColors))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 12.5.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF8E8E93)
        )
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF636366)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.08f)
    )
}

@Composable
private fun SettingsSubPageContent(
    subPage: String,
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Subpage Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
                    .clickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (subPage) {
                    "general" -> "Allgemein & Intelligente Suche"
                    "appearance" -> "Erscheinungsbild & Icons"
                    "weather" -> "Wetter & Smart Widgets"
                    "transition" -> "Seitenübergang"
                    "grid" -> "Screen Grid & Fold-Raster"
                    "fold" -> "Samsung Galaxy Fold Modus"
                    "badges" -> "Badge-Benachrichtigungen"
                    "hidden" -> "Ausgeblendete Apps"
                    else -> "Einstellungen"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (subPage) {
                "general" -> {
                    item {
                        SettingsSwitchCard(
                            title = "Direkt-Start bei eindeutigem Suchtreffer",
                            subtitle = "Startet die App in Spotlight sofort, sobald nur noch 1 Treffer übrig ist",
                            checked = settings.instantLaunchSingleSearchResult,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(instantLaunchSingleSearchResult = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "Neue Apps automatisch kategorisieren",
                            subtitle = "Sortiert neu installierte Apps sofort in die passende Kategorie der App-Mediathek",
                            checked = settings.autoSortNewAppsIntoCategories,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(autoSortNewAppsIntoCategories = v) }
                            }
                        )
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "1-Klick Homescreen Aufräumen",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Wähle, wie dein Homescreen automatisch strukturiert werden soll:",
                                color = Color(0xFF8E8E93),
                                fontSize = 13.sp
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A84FF))
                                    .clickable {
                                        viewModel.autoOrganizeHomeWithSmartFolders()
                                        onBack()
                                    }
                                    .padding(vertical = 13.dp)
                            ) {
                                Text(
                                    text = "Wichtigste Apps + Smart-Kategorie-Ordner anlegen",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .clickable {
                                        viewModel.autoOrganizeHomeFlatSorted()
                                        onBack()
                                    }
                                    .padding(vertical = 13.dp)
                            ) {
                                Text(
                                    text = "Alle Ordner auflösen & nach Kategorie sortieren",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                "appearance" -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Icon-Größe: ${(settings.iconSizeScale * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = settings.iconSizeScale,
                                onValueChange = { scale ->
                                    viewModel.updateSettings { it.copy(iconSizeScale = scale) }
                                },
                                valueRange = 0.80f..1.20f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF0A84FF)
                                )
                            )
                        }
                    }
                    item {
                        SettingsSwitchCard(
                            title = "App-Namen auf dem Homescreen anzeigen",
                            subtitle = "Blendet die Beschriftung unter den Icons ein oder aus",
                            checked = settings.showAppLabels,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showAppLabels = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "App-Namen im Dock anzeigen",
                            subtitle = "Standardmäßig bei iOS ausgeblendet für einen cleanen Look",
                            checked = settings.showDockLabels,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showDockLabels = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "Zuletzt genutzte Apps im aufgeklappten Dock",
                            subtitle = "Zeigt auf dem großen Fold-Display rechts im Dock die 3 zuletzt geöffneten Apps (wie iPadOS)",
                            checked = settings.showRecentInUnfoldedDock,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showRecentInUnfoldedDock = v) }
                            }
                        )
                    }
                }

                "weather" -> {
                    item {
                        SettingsSwitchCard(
                            title = "iOS Uhr- & Wetter-Widgets auf Seite 1 anzeigen",
                            subtitle = "Zeigt oben auf dem ersten Homescreen das Kalender-/Uhr- und Wetter-Widget",
                            checked = settings.showTopWidgetsOnFirstPage,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showTopWidgetsOnFirstPage = v) }
                            }
                        )
                    }
                    item {
                        var cityInput by remember(settings.weatherCity) { mutableStateOf(settings.weatherCity) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Standort für Wetter-Widget",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = cityInput,
                                onValueChange = {
                                    cityInput = it
                                    viewModel.updateSettings { s -> s.copy(weatherCity = it) }
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                "transition" -> {
                    items(PageTransitionEffect.entries) { effect ->
                        val selected = settings.pageTransition == effect
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1C1C1E))
                                .clickable {
                                    viewModel.updateSettings { it.copy(pageTransition = effect) }
                                }
                                .padding(18.dp)
                        ) {
                            Text(
                                text = effect.titleDe,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0A84FF)
                                )
                            }
                        }
                    }
                }

                "fold", "grid" -> {
                    item {
                        Text(
                            text = "Verhalten im aufgeklappten Zustand (Galaxy Fold Innendisplay)",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(FoldPostureMode.entries) { mode ->
                        val selected = settings.foldMode == mode
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) Color(0xFF0A84FF) else Color.Transparent,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings { it.copy(foldMode = mode) }
                                }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.titleDe,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mode.descDe,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 12.5.sp
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0A84FF)
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Frontdisplay Raster (Cover Screen): ${settings.coverColumns} Spalten × ${settings.coverRows} Zeilen",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(4 to 5, 4 to 6, 5 to 6).forEach { (c, r) ->
                                    val active = settings.coverColumns == c && settings.coverRows == r
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (active) Color(0xFF0A84FF) else Color(0xFF2C2C2E))
                                            .clickable {
                                                viewModel.updateSettings {
                                                    it.copy(coverColumns = c, coverRows = r)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("${c}×${r}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Innendisplay Raster (iPadOS Modus): ${settings.mainUnfoldedColumns} Spalten × ${settings.mainUnfoldedRows} Zeilen",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(6 to 5, 6 to 6, 7 to 5, 8 to 5).forEach { (c, r) ->
                                    val active = settings.mainUnfoldedColumns == c && settings.mainUnfoldedRows == r
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (active) Color(0xFF0A84FF) else Color(0xFF2C2C2E))
                                            .clickable {
                                                viewModel.updateSettings {
                                                    it.copy(mainUnfoldedColumns = c, mainUnfoldedRows = r)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text("${c}×${r}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                "badges" -> {
                    item {
                        SettingsSwitchCard(
                            title = "Rote iOS Notification-Badges anzeigen",
                            subtitle = "Zeigt ungelesene Benachrichtigungs-Zähler direkt an den App-Icons und Ordnern",
                            checked = settings.enableBadges,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(enableBadges = v) }
                            }
                        )
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (state.isBadgeServiceEnabled) {
                                    "Status: Benachrichtigungszugriff ist aktiv"
                                } else {
                                    "Status: Benachrichtigungszugriff noch nicht erteilt"
                                },
                                color = if (state.isBadgeServiceEnabled) Color(0xFF30D158) else Color(0xFFFF9F0A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Damit TMS OS die Anzahl ungelesener Nachrichten (z. B. bei WhatsApp oder Mail) zählen kann, aktiviere „TMS OS“ in den Android-Benachrichtigungszugriffen.",
                                color = Color(0xFF8E8E93),
                                fontSize = 13.sp
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A84FF))
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    .padding(vertical = 13.dp)
                            ) {
                                Text(
                                    text = "Android Benachrichtigungszugriff öffnen",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                "hidden" -> {
                    val hiddenApps = state.allApps.filter { it.isHidden }
                    if (hiddenApps.isEmpty()) {
                        item {
                            Text(
                                text = "Aktuell sind keine Apps ausgeblendet. Du kannst jede App auf dem Homescreen gedrückt halten und auf „App ausblenden“ tippen.",
                                color = Color(0xFF8E8E93),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(hiddenApps, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = app.displayLabel,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0A84FF))
                                        .clickable { viewModel.hideOrUnhideApp(app.packageName, false) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Einblenden", color = Color.White, fontSize = 13.sp)
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
private fun SettingsSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 12.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF30D158)
            )
        )
    }
}
