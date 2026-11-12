package com.tms.os.launcher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.LeftWidgetType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosAppIconView(
    app: AppInfo,
    iconSize: Dp = 60.dp,
    labelFontSizeSp: Float = 11.5f,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteOrMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconPressScale"
    )

    // Organic per-icon jiggle duration so icons don't wobble in robotic lockstep
    val durationOffset = remember(app.packageName) { (abs(app.packageName.hashCode()) % 35) }
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val rotation by if (isJiggleMode) {
        infiniteTransition.animateFloat(
            initialValue = -2.3f,
            targetValue = 2.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 120 + durationOffset, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleRotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val cornerRadius = iconSize * 0.225f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (isPressed) 0.82f else 1f
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize + 8.dp)
        ) {
            // Render the full-bleed Apple Squircle bitmap directly without any dark wrapper box!
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .shadow(
                        elevation = 7.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.45f),
                        spotColor = Color.Black.copy(alpha = 0.45f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.displayLabel,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(Color(0xFF2C2C2E))
                    ) {
                        Text(
                            text = app.displayLabel.take(2).uppercase(Locale.ROOT),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Authentic iOS Notification Badge (top-right red pill)
            if (showBadges && app.badgeCount > 0 && !isJiggleMode) {
                val badgeText = if (app.badgeCount > 99) "99+" else app.badgeCount.toString()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-2).dp)
                        .shadow(4.dp, CircleShape, clip = false)
                        .height(22.dp)
                        .background(Color(0xFFFF3B30), shape = CircleShape)
                        .padding(horizontal = 6.5.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Jiggle Mode "-" remove/edit button (top-left frosted grey pill like iOS 18)
            if (isJiggleMode && onDeleteOrMenuClick != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(22.dp)
                        .shadow(3.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Color(0xEEAEAEB2))
                        .clickable { onDeleteOrMenuClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bearbeiten",
                        tint = Color(0xFF1C1C1E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = app.displayLabel,
                color = Color.White,
                fontSize = labelFontSizeSp.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.80f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f
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
    labelFontSizeSp: Float = 11.5f,
    glassOpacity: Float = 0.78f,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "folderPressScale"
    )

    val durationOffset = remember(folder.folderId) { (abs(folder.folderId.hashCode()) % 35) }
    val infiniteTransition = rememberInfiniteTransition(label = "folderJiggle")
    val rotation by if (isJiggleMode) {
        infiniteTransition.animateFloat(
            initialValue = -2.1f,
            targetValue = 2.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 125 + durationOffset, easing = FastOutSlowInEasing),
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
    val cornerRadius = iconSize * 0.225f
    val miniIconSize = (iconSize - 17.dp) / 3f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pressScale
                scaleY = pressScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize + 8.dp)
        ) {
            // Frosted iOS Folder Squircle Container with 3x3 Mini Icon Grid
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .shadow(7.dp, RoundedCornerShape(cornerRadius), clip = false)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF48484A).copy(alpha = glassOpacity.coerceIn(0.45f, 0.92f)),
                                Color(0xFF2C2C2E).copy(alpha = (glassOpacity + 0.06f).coerceIn(0.50f, 0.95f))
                            )
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.26f),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .padding(6.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    for (row in 0 until 3) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val app = folderApps.getOrNull(index)
                                Box(
                                    modifier = Modifier.size(miniIconSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app?.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
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

            if (showBadges && totalBadges > 0 && !isJiggleMode) {
                val badgeText = if (totalBadges > 99) "99+" else totalBadges.toString()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-2).dp)
                        .shadow(4.dp, CircleShape, clip = false)
                        .height(22.dp)
                        .background(Color(0xFFFF3B30), shape = CircleShape)
                        .padding(horizontal = 6.5.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = folder.title,
                color = Color.White,
                fontSize = labelFontSizeSp.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.80f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f
                    )
                ),
                modifier = Modifier.width(iconSize + 18.dp)
            )
        }
    }
}

@Composable
fun IosTopWidgetsHeader(
    leftWidgetType: LeftWidgetType,
    batteryPercent: Int,
    suggestedApps: List<AppInfo>,
    weatherCity: String,
    weatherTempCelsius: Int,
    weatherHighCelsius: Int,
    weatherLowCelsius: Int,
    weatherCondition: String,
    useFahrenheit: Boolean,
    isWeatherRefreshing: Boolean,
    onCycleLeftWidget: () -> Unit,
    onSuggestedAppClick: (AppInfo) -> Unit,
    onWeatherClick: () -> Unit,
    onRefreshWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(10_000L)
        }
    }

    val dayOfWeek = remember(currentTime) {
        SimpleDateFormat("EEEE", Locale.GERMAN).format(currentTime).uppercase(Locale.GERMAN)
    }
    val dayNumber = remember(currentTime) {
        SimpleDateFormat("d", Locale.GERMAN).format(currentTime)
    }
    val monthName = remember(currentTime) {
        SimpleDateFormat("MMMM", Locale.GERMAN).format(currentTime)
    }
    val timeStr = remember(currentTime) {
        SimpleDateFormat("HH:mm", Locale.GERMAN).format(currentTime)
    }

    val displayTemp = if (useFahrenheit) (weatherTempCelsius * 9f / 5f + 32f).roundToInt() else weatherTempCelsius
    val displayHigh = if (useFahrenheit) (weatherHighCelsius * 9f / 5f + 32f).roundToInt() else weatherHighCelsius
    val displayLow = if (useFahrenheit) (weatherLowCelsius * 9f / 5f + 32f).roundToInt() else weatherLowCelsius

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // LEFT WIDGET (Interactive Smart Stack: Calendar/Clock, Battery Ring, or Siri Top-4 Apps)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(144.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xEA1C1C1E),
                            Color(0xEA28282C)
                        )
                    )
                )
                .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .clickable { onCycleLeftWidget() }
                .padding(14.dp)
        ) {
            when (leftWidgetType) {
                LeftWidgetType.CALENDAR_CLOCK -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = dayOfWeek,
                                    color = Color(0xFFFF453A),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "$dayNumber. $monthName",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Widget wechseln",
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = timeStr,
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-1.2).sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF30D158))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Keine Termine heute",
                                    color = Color.White.copy(alpha = 0.60f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                LeftWidgetType.BATTERY_STATUS -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // iOS Circular Battery Ring Canvas
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 7.dp.toPx()
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.14f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    val ringColor = when {
                                        batteryPercent <= 20 -> Color(0xFFFF453A)
                                        batteryPercent <= 40 -> Color(0xFFFFD60A)
                                        else -> Color(0xFF30D158)
                                    }
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = (batteryPercent / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "${batteryPercent}%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Widget wechseln",
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Galaxy Fold",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Batterie • $timeStr Uhr",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                LeftWidgetType.SIRI_SUGGESTIONS -> {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SIRI-VORSCHLÄGE",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Widget wechseln",
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestedApps.take(2).forEach { app ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { onSuggestedAppClick(app) }
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.displayLabel,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestedApps.drop(2).take(2).forEach { app ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { onSuggestedAppClick(app) }
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.displayLabel,
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

        // RIGHT WIDGET: Authentic iOS 18 Live Weather Widget
        Box(
            modifier = Modifier
                .weight(1f)
                .height(144.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF145DA0),
                            Color(0xFF2E8BC0),
                            Color(0xFF56B4E9)
                        )
                    )
                )
                .border(0.8.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                .clickable { onWeatherClick() }
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = weatherCity,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${displayTemp}°",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-1).sp
                        )
                    }

                    // Multi-layered iOS Sun & Cloud Canvas Graphic + Refresh tap
                    Box(
                        contentAlignment = Alignment.TopEnd,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onRefreshWeatherClick() }
                    ) {
                        Canvas(modifier = Modifier.size(38.dp)) {
                            // Sun glow
                            drawCircle(
                                color = Color(0x55FFD60A),
                                radius = size.minDimension * 0.45f,
                                center = Offset(size.width * 0.42f, size.height * 0.42f)
                            )
                            // Sun core
                            drawCircle(
                                color = Color(0xFFFFD60A),
                                radius = size.minDimension * 0.30f,
                                center = Offset(size.width * 0.42f, size.height * 0.42f)
                            )
                            // Soft iOS translucent white cloud in foreground
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.92f),
                                topLeft = Offset(size.width * 0.22f, size.height * 0.48f),
                                size = Size(size.width * 0.68f, size.height * 0.32f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f),
                                radius = size.minDimension * 0.20f,
                                center = Offset(size.width * 0.48f, size.height * 0.50f)
                            )
                        }
                        if (isWeatherRefreshing) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = weatherCondition,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "H:${displayHigh}°  T:${displayLow}°",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
