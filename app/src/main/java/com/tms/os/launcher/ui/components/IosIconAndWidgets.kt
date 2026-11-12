package com.tms.os.launcher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosAppIconView(
    app: AppInfo,
    iconSize: Dp = 60.dp,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteOrMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val rotation by if (isJiggleMode) {
        infiniteTransition.animateFloat(
            initialValue = -2.2f,
            targetValue = 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 135, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleRotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val cornerRadius = iconSize * 0.225f // Authentic Apple Squircle ratio

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize + 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        clip = false
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF1C1C1E))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.displayLabel,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = app.displayLabel.take(2).uppercase(Locale.ROOT),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Notification Badge (top-right red pill)
            if (showBadges && app.badgeCount > 0 && !isJiggleMode) {
                val badgeText = if (app.badgeCount > 99) "99+" else app.badgeCount.toString()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .height(21.dp)
                        .background(Color(0xFFFF3B30), shape = CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Jiggle Mode "-" remove button (top-left grey pill like iOS)
            if (isJiggleMode && onDeleteOrMenuClick != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD8E8E93))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable { onDeleteOrMenuClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bearbeiten",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = app.displayLabel,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier.width(iconSize + 18.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosFolderGridIconView(
    folder: HomeGridItem.FolderItem,
    appMap: Map<String, AppInfo>,
    iconSize: Dp = 60.dp,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "folderJiggle")
    val rotation by if (isJiggleMode) {
        infiniteTransition.animateFloat(
            initialValue = -2.0f,
            targetValue = 2.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "folderRotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val folderApps = remember(folder.appPackages, appMap) {
        folder.appPackages.mapNotNull { appMap[it] }
    }
    val totalBadges = remember(folderApps) {
        folderApps.sumOf { it.badgeCount }
    }
    val cornerRadius = iconSize * 0.23f
    val miniIconSize = (iconSize - 18.dp) / 3f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize + 8.dp)
        ) {
            // Frosted iOS Folder Container with 3x3 Mini Icon Grid
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .shadow(6.dp, RoundedCornerShape(cornerRadius), clip = false)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC3A3A3C),
                                Color(0xBB242426)
                            )
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .padding(6.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    for (row in 0 until 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val app = folderApps.getOrNull(index)
                                Box(
                                    modifier = Modifier
                                        .size(miniIconSize)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    if (app?.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
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

            if (showBadges && totalBadges > 0 && !isJiggleMode) {
                val badgeText = if (totalBadges > 99) "99+" else totalBadges.toString()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .height(21.dp)
                        .background(Color(0xFFFF3B30), shape = CircleShape)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = folder.title,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier.width(iconSize + 18.dp)
            )
        }
    }
}

@Composable
fun IosTopWidgetsHeader(
    weatherCity: String,
    weatherTempCelsius: Int,
    weatherCondition: String,
    onWeatherClick: () -> Unit,
    onSearchPillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(15_000L)
        }
    }

    val dayOfWeek = remember(currentTime) {
        SimpleDateFormat("EEEE", Locale.GERMAN).format(currentTime).uppercase(Locale.GERMAN)
    }
    val dayOfMonth = remember(currentTime) {
        SimpleDateFormat("d. MMMM", Locale.GERMAN).format(currentTime)
    }
    val timeStr = remember(currentTime) {
        SimpleDateFormat("HH:mm", Locale.GERMAN).format(currentTime)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Left Widget: iOS Calendar & Clock Smart Stack Card
        Box(
            modifier = Modifier
                .weight(1f)
                .height(132.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xDD1C1C1E),
                            Color(0xDD2C2C2E)
                        )
                    )
                )
                .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                .clickable { onSearchPillClick() }
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Column {
                    Text(
                        text = dayOfWeek,
                        color = Color(0xFFFF453A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = dayOfMonth,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "Tippen für Spotlight-Suche",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.5.sp
                )
            }
        }

        // Right Widget: iOS Weather Gradient Card
        Box(
            modifier = Modifier
                .weight(1f)
                .height(132.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE1A6DFF),
                            Color(0xEE489CFF)
                        )
                    )
                )
                .border(0.75.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .clickable { onWeatherClick() }
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = weatherCity,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${weatherTempCelsius}°",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column {
                    Text(
                        text = weatherCondition,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "H:${weatherTempCelsius + 3}°  T:${weatherTempCelsius - 6}°",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
