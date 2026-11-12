package com.tms.os.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
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
import com.tms.os.launcher.model.AppCategory
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.FoldPostureMode
import com.tms.os.launcher.model.IconThemeMode
import com.tms.os.launcher.model.LeftWidgetType
import com.tms.os.launcher.model.PageTransitionEffect
import com.tms.os.launcher.model.RightWidgetType
import com.tms.os.launcher.model.TintAccentColor
import com.tms.os.launcher.ui.components.IosAppIconView
import com.tms.os.launcher.viewmodel.LauncherViewModel
import kotlin.math.roundToInt

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
                // Hero Header Card
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
                                .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
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
                                text = "Dein maßgeschneidertes iOS 18 Command Center – mit randlosen Squircle-Icons, echtem Live-Wetter, Hardware-Blur & Galaxy Fold Optimierung.",
                                color = Color(0xFF98989F),
                                fontSize = 13.5.sp,
                                lineHeight = 18.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TMS OS v4.0.0 Reference • ${state.allApps.size} Apps indexiert",
                                color = Color(0xFF0A84FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Floating Circular Rainbow "OS" Logo
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset(x = 18.dp, y = (-26).dp)
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF050507))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
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

                        // Blue "Homescreen >" Pill Button
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

                // Section: "Intelligente Funktionen"
                item {
                    Text(
                        text = "Intelligente Funktionen",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
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
                            subtitle = "Gruppiert alle Apps automatisch & präzise nach iOS-Kategorien",
                            onClick = {
                                viewModel.autoOrganizeHomeWithSmartFolders()
                                statusMessage = "Alle Apps wurden sauber in iOS-Kategorie-Ordner sortiert!"
                            }
                        )

                        FeatureActionRow(
                            icon = Icons.Default.DevicesFold,
                            iconBgColors = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6)),
                            title = "Galaxy Fold Display-Modus",
                            subtitle = "${settings.foldMode.titleDe} • Außen ${settings.coverColumns}×${settings.coverRows} • Innen ${settings.mainUnfoldedColumns}×${settings.mainUnfoldedRows}",
                            onClick = { activeSubPage = "grid" }
                        )
                    }
                }

                // Section: "Allgemein"
                item {
                    Text(
                        text = "Allgemein & Design",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
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
                            title = "Allgemein & Spotlight-Suche",
                            subtitle = "Swipe-Down, Direkt-Start, Web-Suche & Sortierung",
                            onClick = { activeSubPage = "general" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Palette,
                            iconBg = Color(0xFFCE52C9),
                            title = "Erscheinungsbild & iOS-Icons",
                            subtitle = "iOS 18 System-Icons, Icon-Größe ${(settings.iconSizeScale * 100).roundToInt()}%, Glas & Blur",
                            onClick = { activeSubPage = "appearance" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Cloud,
                            iconBg = Color(0xFF32ADE6),
                            title = "Live-Wetter & Smart Widgets",
                            subtitle = "${settings.weatherCity}: ${settings.weatherTempCelsius}°C (${settings.weatherCondition}) • ${settings.leftWidgetType.titleDe}",
                            onClick = { activeSubPage = "weather" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            iconBg = Color(0xFFAF52DE),
                            title = "Seitenübergang",
                            subtitle = settings.pageTransition.titleDe,
                            onClick = { activeSubPage = "transition" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.GridView,
                            iconBg = Color(0xFF5856D6),
                            title = "Screen Grid & Dock-Konfiguration",
                            subtitle = "Cover ${settings.coverColumns}×${settings.coverRows} • Main ${settings.mainUnfoldedColumns}×${settings.mainUnfoldedRows} • ${settings.dockIconCount} Dock-Apps",
                            onClick = { activeSubPage = "grid" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.NotificationsNone,
                            iconBg = Color(0xFF5AC8FA),
                            title = "Badge-Benachrichtigungen",
                            subtitle = if (state.isBadgeServiceEnabled) "Live-Zähler aktiv" else "Tippen zum Aktivieren",
                            onClick = { activeSubPage = "badges" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Visibility,
                            iconBg = Color(0xFFFF9F0A),
                            title = "Kategorien-Manager & Apps ausblenden",
                            subtitle = "${state.allApps.size} Apps verwalten • ${state.allApps.count { it.isHidden }} ausgeblendet",
                            onClick = { activeSubPage = "hidden" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Language,
                            iconBg = Color(0xFF9B8BF4),
                            title = "Sprache & Region",
                            subtitle = settings.appLanguage.displayName,
                            onClick = { activeSubPage = "language" }
                        )
                        RowDivider()

                        SettingsMenuRow(
                            icon = Icons.Default.Home,
                            iconBg = Color(0xFF30D158),
                            title = "Standard-Startbildschirm auswählen",
                            subtitle = if (state.isDefaultLauncher) "TMS OS ist als Standard-Launcher aktiv" else "Tippen, um TMS OS als Standard festzulegen",
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
                .padding(bottom = 16.dp)
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (subPage) {
                    "general" -> "Allgemein & Spotlight-Suche"
                    "appearance" -> "Erscheinungsbild & iOS-Icons"
                    "weather" -> "Live-Wetter & Smart Widgets"
                    "transition" -> "Seitenübergang"
                    "grid" -> "Screen Grid & Fold-Layout"
                    "badges" -> "Badge-Benachrichtigungen"
                    "hidden" -> "Kategorien & Apps verwalten"
                    "language" -> "Sprache & Region"
                    else -> "Einstellungen"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (subPage) {
                "general" -> {
                    item {
                        SettingsSwitchCard(
                            title = "Spotlight-Suchleiste unten (iOS 18 Einhand-Modus)",
                            subtitle = "Positioniert das Suchfeld direkt über der Tastatur für bequeme Einhand-Bedienung auf dem Galaxy Fold",
                            checked = settings.searchBarAtBottomInSpotlight,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(searchBarAtBottomInSpotlight = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "Swipe-Down für Spotlight-Suche",
                            subtitle = "Wische auf dem Homescreen nach unten (oberer Rand öffnet die Android-Benachrichtigungsleiste)",
                            checked = settings.enableSwipeDownSpotlight,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(enableSwipeDownSpotlight = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "Tastatur in Spotlight sofort öffnen",
                            subtitle = "Blendet die Tastatur direkt beim Herunterziehen ein, sodass du sofort tippen kannst",
                            checked = settings.autoOpenKeyboardInSpotlight,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(autoOpenKeyboardInSpotlight = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "Direkt-Start bei eindeutigem Suchtreffer",
                            subtitle = "Startet die App in Spotlight automatisch, sobald nur noch 1 passender Treffer übrig ist",
                            checked = settings.instantLaunchSingleSearchResult,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(instantLaunchSingleSearchResult = v) }
                            }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            title = "Web-Suche (Google, YouTube, Play Store) in Spotlight",
                            subtitle = "Zeigt unter den App-Treffern Direkt-Buttons für Web-, Video- und Store-Suche",
                            checked = settings.showWebSearchInSpotlight,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showWebSearchInSpotlight = v) }
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (state.isUsageStatsGranted) {
                                    "✓ 30-Tage Nutzungsanalyse (UsageStats) aktiv"
                                } else {
                                    "Nutzungsanalyse für intelligente Seite-1-Auswahl"
                                },
                                color = if (state.isUsageStatsGranted) Color(0xFF30D158) else Color(0xFFFF9F0A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Erlaubt TMS OS, deine am häufigsten genutzten Apps der letzten 30 Tage automatisch als Einzel-Apps auf Seite 1 zu platzieren.",
                                color = Color(0xFF8E8E93),
                                fontSize = 12.5.sp
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Nutzungszugriff in Android öffnen",
                                    color = Color(0xFF0A84FF),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
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
                                text = "Seite 1 = Top-Einzel-Apps • Seite 2 = Alle Smart-Kategorie-Ordner:",
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
                                    text = "Seite 1 Einzel-Apps + Seite 2 Smart-Ordner anlegen",
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
                                    text = "Alle Ordner auflösen & flach nach Kategorien sortieren",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    item {
                        var backupStatus by remember { mutableStateOf<String?>(null) }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Backup & Wiederherstellung (JSON)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sichere dein komplettes Layout, Kategorien, Ordner & Einstellungen in die Zwischenablage oder spiele ein Backup wieder ein.",
                                color = Color(0xFF8E8E93),
                                fontSize = 12.5.sp
                            )
                            if (backupStatus != null) {
                                Text(
                                    text = backupStatus!!,
                                    color = Color(0xFF30D158),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2C2C2E))
                                        .clickable {
                                            val json = viewModel.exportBackupJson()
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("TMS_OS_Backup", json))
                                            backupStatus = "✓ Backup-JSON in Zwischenablage kopiert!"
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text("Backup kopieren", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2C2C2E))
                                        .clickable {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val text = cm?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                            if (viewModel.importBackupJson(text)) {
                                                backupStatus = "✓ Backup erfolgreich wiederhergestellt!"
                                            } else {
                                                backupStatus = "Kein gültiges Backup in der Zwischenablage."
                                            }
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text("Backup importieren", color = Color(0xFF0A84FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                "appearance" -> {
                    // Live Icon Preview Card
                    item {
                        val sampleApps = state.allApps.take(4)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1F2430), Color(0xFF12141C))
                                    )
                                )
                                .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "LIVE-VORSCHAU (${settings.iconThemeMode.titleDe.uppercase()})",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                sampleApps.forEach { app ->
                                    IosAppIconView(
                                        app = app,
                                        iconSize = 59.dp * settings.iconSizeScale,
                                        labelFontSizeSp = settings.labelFontSizeSp,
                                        showLabel = settings.showAppLabels,
                                        showBadges = settings.enableBadges,
                                        isJiggleMode = false,
                                        onClick = {},
                                        onLongClick = {}
                                    )
                                }
                            }
                        }
                    }

                    // iOS 18 Icon Theme Mode Selector (Standard / iOS 18 Dark / iOS 18 Tinted)
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
                                text = "iOS 18 Icon-Farbmodus",
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IosSegmentedRow(
                                options = IconThemeMode.entries,
                                selected = settings.iconThemeMode,
                                labelFor = { it.titleDe },
                                onSelect = { mode ->
                                    viewModel.updateSettings { it.copy(iconThemeMode = mode) }
                                }
                            )

                            if (settings.iconThemeMode == IconThemeMode.IOS_TINTED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "iOS 18 Tinting-Akzentfarbe:",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 12.5.sp
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(TintAccentColor.entries) { accent ->
                                        val isSelected = settings.tintAccentColor == accent
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF2C2C2E) else Color(0xFF141416))
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.6.dp,
                                                    color = if (isSelected) Color(accent.colorHex) else Color.White.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    viewModel.updateSettings { it.copy(tintAccentColor = accent) }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(accent.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = accent.titleDe,
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SettingsSwitchCard(
                            title = "Echte iOS 18 System-Icons verwenden",
                            subtitle = "Ersetzt Kamera, Fotos, Live-Kalender (mit aktuellem Datum!), Uhr, Telefon & Einstellungen durch native Apple iOS 18 Icons",
                            checked = settings.useIosSystemIcons,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(useIosSystemIcons = v) }
                            }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            title = "Randlose Vollbild-Squircle-Icons (Full-Bleed)",
                            subtitle = "Entfernt automatisch dunkle Kästchen/Ränder um Samsung- & Android-Icons für 100% echte Apple-Squircles",
                            checked = settings.fullBleedAdaptiveIcons,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(fullBleedAdaptiveIcons = v) }
                            }
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Icon-Größe",
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.iconSizeScale * 100).roundToInt()}%",
                                    color = Color(0xFF0A84FF),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Schriftgröße der App-Namen",
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${settings.labelFontSizeSp.roundToInt()} sp",
                                    color = Color(0xFF0A84FF),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.labelFontSizeSp,
                                onValueChange = { sz ->
                                    viewModel.updateSettings { it.copy(labelFontSizeSp = sz) }
                                },
                                valueRange = 9.5f..14.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF0A84FF)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Frosted-Glass Deckkraft (Dock & Ordner)",
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.glassOpacity * 100).roundToInt()}%",
                                    color = Color(0xFF0A84FF),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.glassOpacity,
                                onValueChange = { op ->
                                    viewModel.updateSettings { it.copy(glassOpacity = op) }
                                },
                                valueRange = 0.45f..0.95f,
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
                            subtitle = "Blendet die Beschriftung unter den Icons auf dem Homescreen ein oder aus",
                            checked = settings.showAppLabels,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showAppLabels = v) }
                            }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            title = "App-Namen im Dock anzeigen",
                            subtitle = "Bei iOS standardmäßig ausgeblendet für ein cleanes Dock",
                            checked = settings.showDockLabels,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showDockLabels = v) }
                            }
                        )
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0A84FF))
                                .clickable { viewModel.openWallpaperPicker() }
                                .padding(15.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "System-Hintergrundbild (Wallpaper) ändern",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                "weather" -> {
                    item {
                        var cityInput by remember(settings.weatherCity) { mutableStateOf(settings.weatherCity) }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Live-Wetter Standort",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Aktuell: ${settings.weatherCity} • ${settings.weatherTempCelsius}°C (${settings.weatherCondition})",
                                        color = Color(0xFF30D158),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            BasicTextField(
                                value = cityInput,
                                onValueChange = { cityInput = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .padding(13.dp)
                            )

                            // Quick City Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Berlin", "Dubai", "München", "Frankfurt", "Hamburg", "Wien", "Zürich", "Istanbul")) { presetCity ->
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (cityInput.equals(presetCity, ignoreCase = true)) Color(0xFF0A84FF)
                                                else Color(0xFF2C2C2E)
                                            )
                                            .clickable {
                                                cityInput = presetCity
                                                viewModel.refreshLiveWeatherForCity(presetCity)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = presetCity,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A84FF))
                                    .clickable {
                                        viewModel.refreshLiveWeatherForCity(cityInput)
                                    }
                                    .padding(vertical = 13.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.isWeatherRefreshing) "Lade Live-Wetterdaten…" else "Jetzt Live-Wetter für „$cityInput“ abrufen",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SettingsSwitchCard(
                            title = "Top-Widgets auf Seite 1 anzeigen",
                            subtitle = "Zeigt die zwei iOS Smart-Widgets oben auf der ersten Homescreen-Seite",
                            checked = settings.showTopWidgetsOnFirstPage,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showTopWidgetsOnFirstPage = v) }
                            }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            title = "Temperatur in Fahrenheit (°F)",
                            subtitle = "Standardmäßig in Celsius (°C)",
                            checked = settings.useFahrenheit,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(useFahrenheit = v) }
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Linkes Smart-Widget auswählen (oder direkt auf dem Homescreen antippen)",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(LeftWidgetType.entries) { wType ->
                        val selected = settings.leftWidgetType == wType
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) Color(0xFF0A84FF) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings { it.copy(leftWidgetType = wType) }
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = wType.titleDe,
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold
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

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rechtes Smart-Stack Widget auswählen (oder direkt auf dem Homescreen vertikal wischen)",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(RightWidgetType.entries) { rType ->
                        val selected = settings.rightWidgetType == rType
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) Color(0xFF0A84FF) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings { it.copy(rightWidgetType = rType) }
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = rType.titleDe,
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold
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
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) Color(0xFF0A84FF) else Color.Transparent,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings { it.copy(pageTransition = effect) }
                                }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = effect.titleDe,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = effect.descDe,
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
                }

                "grid" -> {
                    item {
                        Text(
                            text = "Samsung Galaxy Fold Innendisplay-Modus (Aufgeklappt)",
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
                                text = "Frontdisplay (Cover Screen): ${settings.coverColumns} Spalten × ${settings.coverRows} Zeilen",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Spalten (Außen):", color = Color(0xFF8E8E93), fontSize = 12.5.sp)
                            IosSegmentedRow(
                                options = listOf(3, 4, 5),
                                selected = settings.coverColumns,
                                labelFor = { "$it Spalten" },
                                onSelect = { c -> viewModel.updateSettings { it.copy(coverColumns = c) } }
                            )
                            Text("Zeilen (Außen):", color = Color(0xFF8E8E93), fontSize = 12.5.sp)
                            IosSegmentedRow(
                                options = listOf(4, 5, 6, 7),
                                selected = settings.coverRows,
                                labelFor = { "$it Zeilen" },
                                onSelect = { r -> viewModel.updateSettings { it.copy(coverRows = r) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))

                            Text(
                                text = "Innendisplay (Main Unfolded): ${settings.mainUnfoldedColumns} Spalten × ${settings.mainUnfoldedRows} Zeilen",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Spalten (Innen):", color = Color(0xFF8E8E93), fontSize = 12.5.sp)
                            IosSegmentedRow(
                                options = listOf(5, 6, 7, 8),
                                selected = settings.mainUnfoldedColumns,
                                labelFor = { "$it Spalten" },
                                onSelect = { c -> viewModel.updateSettings { it.copy(mainUnfoldedColumns = c) } }
                            )
                            Text("Zeilen (Innen):", color = Color(0xFF8E8E93), fontSize = 12.5.sp)
                            IosSegmentedRow(
                                options = listOf(4, 5, 6),
                                selected = settings.mainUnfoldedRows,
                                labelFor = { "$it Zeilen" },
                                onSelect = { r -> viewModel.updateSettings { it.copy(mainUnfoldedRows = r) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))

                            Text(
                                text = "Anzahl der gepinnten Apps im Dock: ${settings.dockIconCount}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IosSegmentedRow(
                                options = listOf(4, 5, 6),
                                selected = settings.dockIconCount,
                                labelFor = { "$it Dock-Apps" },
                                onSelect = { d -> viewModel.updateSettings { it.copy(dockIconCount = d) } }
                            )
                        }
                    }

                    item {
                        SettingsSwitchCard(
                            title = "Zuletzt genutzte Apps im aufgeklappten Dock",
                            subtitle = "Zeigt auf dem Innendisplay rechts im Dock automatisch die 3 zuletzt geöffneten Apps (wie bei iPadOS)",
                            checked = settings.showRecentInUnfoldedDock,
                            onCheckedChange = { v ->
                                viewModel.updateSettings { it.copy(showRecentInUnfoldedDock = v) }
                            }
                        )
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
                                    "✓ Benachrichtigungszugriff ist aktiv"
                                } else {
                                    "Benachrichtigungszugriff noch nicht erteilt"
                                },
                                color = if (state.isBadgeServiceEnabled) Color(0xFF30D158) else Color(0xFFFF9F0A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Damit TMS OS ungelesene Nachrichten (z. B. bei WhatsApp, Telegram oder Mail) in Echtzeit zählen kann, aktiviere „TMS OS“ in den Android-Benachrichtigungszugriffen.",
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
                    item {
                        var filterQuery by remember { mutableStateOf("") }
                        var onlyHidden by remember { mutableStateOf(false) }
                        var editingCategoryForPkg by remember { mutableStateOf<String?>(null) }

                        val filteredApps = remember(state.allApps, filterQuery, onlyHidden) {
                            state.allApps.filter { app ->
                                (!onlyHidden || app.isHidden) &&
                                    (filterQuery.isBlank() ||
                                        app.displayLabel.contains(filterQuery, ignoreCase = true) ||
                                        app.category.titleDe.contains(filterQuery, ignoreCase = true))
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BasicTextField(
                                    value = filterQuery,
                                    onValueChange = { filterQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                    cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .padding(12.dp)
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (onlyHidden) Color(0xFF0A84FF) else Color(0xFF1C1C1E))
                                        .clickable { onlyHidden = !onlyHidden }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = if (onlyHidden) "Nur Ausgeblendete" else "Alle (${state.allApps.size})",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            filteredApps.forEach { app ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1C1C1E))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(modifier = Modifier.size(42.dp)) {
                                                if (app.iconBitmap != null) {
                                                    Image(
                                                        bitmap = app.iconBitmap,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = app.displayLabel,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Kategorie: ${app.category.titleDe} (Tippen zum Ändern)",
                                                    color = Color(0xFF0A84FF),
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.clickable {
                                                        editingCategoryForPkg =
                                                            if (editingCategoryForPkg == app.packageName) null else app.packageName
                                                    }
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (app.isHidden) Color(0xFF30D158)
                                                    else Color(0xFF2C2C2E)
                                                )
                                                .clickable {
                                                    viewModel.hideOrUnhideApp(app.packageName, !app.isHidden)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = if (app.isHidden) "Einblenden" else "Ausblenden",
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (editingCategoryForPkg == app.packageName) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(AppCategory.entries.filter { it != AppCategory.SUGGESTIONS }) { cat ->
                                                val isCurrent = cat == app.category
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isCurrent) Color(0xFF0A84FF) else Color(0xFF2C2C2E)
                                                        )
                                                        .clickable {
                                                            viewModel.changeAppCategory(app.packageName, cat)
                                                            editingCategoryForPkg = null
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = cat.titleDe,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
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

                "language" -> {
                    items(AppLanguage.entries) { lang ->
                        val selected = settings.appLanguage == lang
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
                                    viewModel.updateSettings { it.copy(appLanguage = lang) }
                                }
                                .padding(18.dp)
                        ) {
                            Text(
                                text = lang.displayName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
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
            }
        }
    }
}

@Composable
private fun <T> IosSegmentedRow(
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C2C2E))
            .padding(4.dp)
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) Color(0xFF0A84FF) else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = labelFor(opt),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
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
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
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
                checkedTrackColor = Color(0xFF30D158),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF39393D)
            )
        )
    }
}
