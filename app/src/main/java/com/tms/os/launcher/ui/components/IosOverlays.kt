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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
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
import com.tms.os.launcher.ui.theme.wallpaperGlass
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.IosStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

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
                                    .fillMaxHeight(0.68f)
                            }
                        )
                        .wallpaperGlass(shape, tint = com.tms.os.launcher.ui.theme.IosMaterial.menu)
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
                                label = when (settings.appLanguage) {
                                    AppLanguage.DE -> "Hintergrund des Geräts"
                                    AppLanguage.EN -> "Device wallpaper"
                                    AppLanguage.TR -> "Cihaz duvar kâğıdı"
                                },
                                checked = settings.useSystemWallpaper,
                                onCheckedChange = { checked -> onSettingsChange { it.copy(useSystemWallpaper = checked) } }
                            )
                            MenuDivider(inset = 14.dp)
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

@Composable
private fun MenuDivider(inset: Dp = 0.dp) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.padding(start = inset)
    )
}
