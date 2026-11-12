package com.tms.os.launcher.ui.components

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.data.SystemStorageRamInfo
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.LeftWidgetType
import com.tms.os.launcher.model.RightWidgetType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosAppIconView(
    app: AppInfo,
    iconSize: Dp = 66.dp,
    labelFontSizeSp: Float = 11.5f,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    isHighlightedTarget: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteOrMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = when {
            isHighlightedTarget -> 1.12f
            isPressed -> 0.90f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconPressScale"
    )

    val durationOffset = remember(app.packageName) { (abs(app.packageName.hashCode()) % 30) }
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val rotation by if (isJiggleMode) {
        infiniteTransition.animateFloat(
            initialValue = -2.0f,
            targetValue = 2.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 125 + durationOffset, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleRotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val cornerRadius = iconSize * 0.225f
    val isRecentlyInstalled = remember(app.firstInstallTime) {
        val fiveDaysMs = 5L * 24L * 3600L * 1000L
        (System.currentTimeMillis() - app.firstInstallTime) in 1..fiveDaysMs
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (isPressed) 0.84f else 1f
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
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize)
        ) {
            if (isHighlightedTarget) {
                Box(
                    modifier = Modifier
                        .size(iconSize + 8.dp)
                        .clip(RoundedCornerShape(cornerRadius + 4.dp))
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }

            // Full-bleed Squircle Icon
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

            // Red Notification Badge (Top-Right)
            if (showBadges && app.badgeCount > 0 && !isJiggleMode) {
                val badgeText = if (app.badgeCount > 99) "99+" else app.badgeCount.toString()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-4).dp)
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

            // Authentic iOS Jiggle Mode Minus Badge "—" (Top-Left, exact match to Screenshot 3!)
            if (isJiggleMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(24.dp)
                        .shadow(2.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA))
                        .clickable {
                            onDeleteOrMenuClick?.invoke() ?: onLongClick()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.width(iconSize + 18.dp)
            ) {
                if (isRecentlyInstalled && !isJiggleMode) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A84FF))
                    )
                    Spacer(modifier = Modifier.width(3.5.dp))
                }
                Text(
                    text = app.displayLabel,
                    color = Color.White,
                    fontSize = labelFontSizeSp.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.75f),
                            offset = Offset(0f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosFolderGridIconView(
    folder: HomeGridItem.FolderItem,
    appMap: Map<String, AppInfo>,
    iconSize: Dp = 66.dp,
    labelFontSizeSp: Float = 11.5f,
    glassOpacity: Float = 0.78f,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    isHighlightedTarget: Boolean = false,
    onClickWithOrigin: (Offset) -> Unit,
    onLongClickWithOrigin: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx().coerceAtLeast(1f) }
    val screenH = with(density) { config.screenHeightDp.dp.toPx().coerceAtLeast(1f) }

    var centerInRoot by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = when {
            isHighlightedTarget -> 1.14f
            isPressed -> 0.90f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "folderPressScale"
    )

    val durationOffset = remember(folder.folderId) { (abs(folder.folderId.hashCode()) % 30) }
    val infiniteTransition = rememberInfiniteTransition(label = "folderJiggle")
    val rotation by if (isJiggleMode) {
        infiniteTransition.animateFloat(
            initialValue = -2.0f,
            targetValue = 2.0f,
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
    val miniIconSize = (iconSize - 16.dp) / 3f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val cx = ((pos.x + coords.size.width / 2f) / screenW).coerceIn(0.1f, 0.9f)
                val cy = ((pos.y + coords.size.height / 2f) / screenH).coerceIn(0.1f, 0.9f)
                centerInRoot = Offset(cx, cy)
            }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pressScale
                scaleY = pressScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClickWithOrigin(centerInRoot) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClickWithOrigin(centerInRoot)
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xB82C2C2E))
                    .border(
                        width = if (isHighlightedTarget) 2.dp else 0.5.dp,
                        color = if (isHighlightedTarget) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.14f),
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
                        .offset(x = 5.dp, y = (-4).dp)
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

            if (isJiggleMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(24.dp)
                        .shadow(2.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA))
                        .clickable { onLongClickWithOrigin(centerInRoot) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folder.title,
                color = Color.White,
                fontSize = labelFontSizeSp.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f
                    )
                ),
                modifier = Modifier.width(iconSize + 18.dp)
            )
        }
    }
}

/**
 * Top Home Screen Widgets Header:
 * Displays two authentic 2x2 iOS Widgets side-by-side (or vertically swipeable Smart-Stacks):
 * - Left: Analog Clock ("Uhr I"), Month Calendar Grid ("Kalender - Monat"), Battery Ring ("Batterie - Status"), or Quick Note
 * - Right: Live Weather Forecast, Month Calendar Grid, or Battery Ring
 */
@Composable
fun IosTopWidgetsHeader(
    leftWidgetType: LeftWidgetType,
    rightWidgetType: RightWidgetType,
    batteryPercent: Int,
    storageRamInfo: SystemStorageRamInfo,
    quickNoteText: String,
    suggestedApps: List<AppInfo>,
    weatherCity: String,
    weatherTempCelsius: Int,
    weatherHighCelsius: Int,
    weatherLowCelsius: Int,
    weatherCondition: String,
    useFahrenheit: Boolean,
    isWeatherRefreshing: Boolean,
    onCycleLeftWidget: (Int) -> Unit,
    onCycleRightWidget: (Int) -> Unit,
    onQuickNoteChange: (String) -> Unit,
    onSuggestedAppClick: (AppInfo) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onWeatherClick: () -> Unit,
    onRefreshWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // LEFT 2x2 SQUARE WIDGET
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .pointerInput(leftWidgetType) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dy = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dy += dragAmount
                            if (dy < -36f) {
                                dy = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCycleLeftWidget(+1)
                            } else if (dy > 36f) {
                                dy = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCycleLeftWidget(-1)
                            }
                        }
                    )
                }
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCycleLeftWidget(+1)
                }
        ) {
            when (leftWidgetType) {
                LeftWidgetType.CALENDAR_CLOCK -> IosAnalogClockWidgetCard(modifier = Modifier.fillMaxSize())
                LeftWidgetType.BATTERY_STATUS -> IosBatteryRingWidgetCard(
                    batteryPercent = batteryPercent,
                    modifier = Modifier.fillMaxSize()
                )
                LeftWidgetType.SIRI_SUGGESTIONS -> IosMonthCalendarWidgetCard(modifier = Modifier.fillMaxSize())
                LeftWidgetType.QUICK_NOTE -> IosPhotoRecommendWidgetCard(modifier = Modifier.fillMaxSize())
            }
        }

        // RIGHT 2x2 SQUARE WIDGET
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .pointerInput(rightWidgetType) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dy = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dy += dragAmount
                            if (dy < -36f) {
                                dy = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCycleRightWidget(+1)
                            } else if (dy > 36f) {
                                dy = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCycleRightWidget(-1)
                            }
                        }
                    )
                }
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCycleRightWidget(+1)
                }
        ) {
            when (rightWidgetType) {
                RightWidgetType.LIVE_WEATHER -> IosMonthCalendarWidgetCard(modifier = Modifier.fillMaxSize())
                RightWidgetType.MEDIA_CONTROLLER -> IosBatteryRingWidgetCard(
                    batteryPercent = batteryPercent,
                    modifier = Modifier.fillMaxSize()
                )
                RightWidgetType.STORAGE_RAM -> IosCompactWeatherWidgetCard(
                    city = weatherCity,
                    tempC = weatherTempCelsius,
                    highC = weatherHighCelsius,
                    lowC = weatherLowCelsius,
                    condition = weatherCondition,
                    useFahrenheit = useFahrenheit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 1:1 Authentic iOS Analog Clock Widget ("Uhr I" from Screenshot 1!)
 * White circular dial inside a dark squircle, 60 minute ticks, bold 1..12 numerals,
 * black hour/minute hands, and live sweeping orange second hand.
 */
@Composable
fun IosAnalogClockWidgetCard(
    modifier: Modifier = Modifier
) {
    var timeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            timeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // White clock face circle
            drawCircle(
                color = Color(0xFFF5F5F7),
                radius = radius,
                center = center
            )

            // 60 perimeter tick marks
            for (i in 0 until 60) {
                val angleRad = Math.toRadians((i * 6 - 90).toDouble())
                val isHourTick = i % 5 == 0
                val outerR = radius * 0.95f
                val innerR = if (isHourTick) radius * 0.86f else radius * 0.90f
                val start = Offset(
                    center.x + cos(angleRad).toFloat() * innerR,
                    center.y + sin(angleRad).toFloat() * innerR
                )
                val end = Offset(
                    center.x + cos(angleRad).toFloat() * outerR,
                    center.y + sin(angleRad).toFloat() * outerR
                )
                drawLine(
                    color = if (isHourTick) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
                    start = start,
                    end = end,
                    strokeWidth = if (isHourTick) radius * 0.032f else radius * 0.015f,
                    cap = StrokeCap.Round
                )
            }

            // Bold 1..12 numerals
            val textPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(20, 20, 22)
                textAlign = NativePaint.Align.CENTER
                textSize = radius * 0.23f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val fontMetrics = textPaint.fontMetrics
            val textBaselineOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f
            val numRadius = radius * 0.70f

            for (hour in 1..12) {
                val angleRad = Math.toRadians((hour * 30 - 90).toDouble())
                val nx = center.x + cos(angleRad).toFloat() * numRadius
                val ny = center.y + sin(angleRad).toFloat() * numRadius + textBaselineOffset
                drawContext.canvas.nativeCanvas.drawText(hour.toString(), nx, ny, textPaint)
            }

            val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
            val sec = cal.get(Calendar.SECOND)
            val min = cal.get(Calendar.MINUTE) + sec / 60f
            val hr = (cal.get(Calendar.HOUR) % 12) + min / 60f

            // Hour hand
            val hrAngle = Math.toRadians((hr * 30f - 90f).toDouble())
            drawLine(
                color = Color(0xFF1C1C1E),
                start = center,
                end = Offset(
                    center.x + cos(hrAngle).toFloat() * (radius * 0.48f),
                    center.y + sin(hrAngle).toFloat() * (radius * 0.48f)
                ),
                strokeWidth = radius * 0.065f,
                cap = StrokeCap.Round
            )

            // Minute hand
            val minAngle = Math.toRadians((min * 6f - 90f).toDouble())
            drawLine(
                color = Color(0xFF1C1C1E),
                start = center,
                end = Offset(
                    center.x + cos(minAngle).toFloat() * (radius * 0.76f),
                    center.y + sin(minAngle).toFloat() * (radius * 0.76f)
                ),
                strokeWidth = radius * 0.048f,
                cap = StrokeCap.Round
            )

            // Orange Second hand
            val secAngle = Math.toRadians((sec * 6f - 90f).toDouble())
            val tailStart = Offset(
                center.x - cos(secAngle).toFloat() * (radius * 0.16f),
                center.y - sin(secAngle).toFloat() * (radius * 0.16f)
            )
            val secEnd = Offset(
                center.x + cos(secAngle).toFloat() * (radius * 0.85f),
                center.y + sin(secAngle).toFloat() * (radius * 0.85f)
            )
            drawLine(
                color = Color(0xFFFF9F0A),
                start = tailStart,
                end = secEnd,
                strokeWidth = radius * 0.022f,
                cap = StrokeCap.Round
            )

            // Center cap
            drawCircle(color = Color(0xFFFF9F0A), radius = radius * 0.055f, center = center)
            drawCircle(color = Color.White, radius = radius * 0.022f, center = center)
        }
    }
}

/**
 * 1:1 Authentic iOS Month Calendar Widget ("Kalender - Monat" from Screenshot 1!)
 */
@Composable
fun IosMonthCalendarWidgetCard(
    modifier: Modifier = Modifier
) {
    val calendarData = remember {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = SimpleDateFormat("MMMM", Locale.GERMAN).format(cal.time).uppercase(Locale.GERMAN)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1 .. Sat=7
        val mondayOffset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        Triple(monthName, today, Pair(mondayOffset, daysInMonth))
    }

    val monthName = calendarData.first
    val todayDay = calendarData.second
    val startOffset = calendarData.third.first
    val daysInMonth = calendarData.third.second

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = monthName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // Weekday Header Row: M T W T F S S
        val headers = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            headers.forEach { h ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = h,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5-6 Weeks Grid
        val totalSlots = startOffset + daysInMonth
        val rows = ((totalSlots + 6) / 7).coerceIn(5, 6)
        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - startOffset + 1
                    val isValidDay = dayNum in 1..daysInMonth
                    val isToday = dayNum == todayDay

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(17.dp)
                    ) {
                        if (isValidDay) {
                            if (isToday) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(17.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30))
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = dayNum.toString(),
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1:1 Authentic iOS Battery Ring Widget ("Batterie - Status" from Screenshot 1!)
 * Green circular progress ring with phone icon inside at top-left, huge "37%" at bottom-left.
 */
@Composable
fun IosBatteryRingWidgetCard(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(62.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.5.dp.toPx()
                drawArc(
                    color = Color(0xFF48484A),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val ringColor = when {
                    batteryPercent <= 20 -> Color(0xFFFF453A)
                    else -> Color(0xFF34C759)
                }
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = (batteryPercent.coerceIn(0, 100) / 100f) * 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = "${batteryPercent}%",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp
        )
    }
}

/**
 * 1:1 Authentic iOS Photo Widget ("Foto - Empfohlen" from Screenshot 1!)
 */
@Composable
fun IosPhotoRecommendWidgetCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Spring blue sky background with soft depth bokeh
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E6BC6), Color(0xFF5D9CE6), Color(0xFF9AC5F4))
                )
            )
            // Soft background bokeh blossoms
            val bokehSpots = listOf(
                Triple(0.18f, 0.22f, 28f),
                Triple(0.82f, 0.18f, 34f),
                Triple(0.15f, 0.78f, 32f),
                Triple(0.75f, 0.85f, 36f),
                Triple(0.50f, 0.14f, 24f)
            )
            bokehSpots.forEach { (bx, by, r) ->
                drawCircle(
                    color = Color(0xFFFFD1DC).copy(alpha = 0.32f),
                    radius = r * (w / 200f),
                    center = Offset(w * bx, h * by)
                )
            }
            // Dark woody cherry tree branches
            val branchColor = Color(0xFF3B2322)
            drawLine(
                color = branchColor,
                start = Offset(0f, h * 0.88f),
                end = Offset(w * 0.82f, h * 0.22f),
                strokeWidth = 7f * (w / 200f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = branchColor,
                start = Offset(w * 0.34f, h * 0.60f),
                end = Offset(w * 0.92f, h * 0.58f),
                strokeWidth = 5f * (w / 200f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = branchColor,
                start = Offset(w * 0.48f, h * 0.48f),
                end = Offset(w * 0.25f, h * 0.18f),
                strokeWidth = 4.5f * (w / 200f),
                cap = StrokeCap.Round
            )
            // 5-petal Cherry Blossoms along the branches
            val blossoms = listOf(
                Triple(0.26f, 0.66f, 22f),
                Triple(0.45f, 0.52f, 26f),
                Triple(0.68f, 0.34f, 24f),
                Triple(0.80f, 0.24f, 20f),
                Triple(0.62f, 0.58f, 23f),
                Triple(0.84f, 0.56f, 21f),
                Triple(0.32f, 0.28f, 22f),
                Triple(0.52f, 0.74f, 20f)
            )
            val petalShades = listOf(
                Color(0xFFFFE4EC),
                Color(0xFFFFC1D3),
                Color(0xFFFFB2C9),
                Color(0xFFFFF0F5)
            )
            blossoms.forEachIndexed { idx, (fx, fy, scale) ->
                val cx = w * fx
                val cy = h * fy
                val petalDist = scale * 0.52f * (w / 200f)
                val petalRadius = scale * 0.46f * (w / 200f)
                val petalColor = petalShades[idx % petalShades.size]
                for (p in 0 until 5) {
                    val angle = Math.toRadians((p * 72.0) + (idx * 15.0))
                    val px = cx + (cos(angle) * petalDist).toFloat()
                    val py = cy + (sin(angle) * petalDist).toFloat()
                    drawCircle(
                        color = petalColor,
                        radius = petalRadius,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = petalRadius * 0.55f,
                        center = Offset(px, py)
                    )
                }
                // Crimson/Golden flower stamen center
                drawCircle(
                    color = Color(0xFFD8436B),
                    radius = petalRadius * 0.42f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = petalRadius * 0.22f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * Compact 2x2 Live Weather Widget Card
 */
@Composable
fun IosCompactWeatherWidgetCard(
    city: String,
    tempC: Int,
    highC: Int,
    lowC: Int,
    condition: String,
    useFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    val displayTemp = if (useFahrenheit) (tempC * 9f / 5f + 32f).roundToInt() else tempC
    val displayHigh = if (useFahrenheit) (highC * 9f / 5f + 32f).roundToInt() else highC
    val displayLow = if (useFahrenheit) (lowC * 9f / 5f + 32f).roundToInt() else lowC

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF334155))
                )
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
            .padding(15.dp)
    ) {
        Column {
            Text(
                text = city,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${displayTemp}°",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.5).sp
            )
        }

        Column {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = condition,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "H:${displayHigh}° T:${displayLow}°",
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 1:1 Authentic Full-Width 4x2 Weather Forecast Widget Card ("Wetter - Vorhersage" from Screenshot 1!)
 * Shows City, 26°C, Nieseln, H:31°C T:25°C, and 6-hour forecast row (22, 23, 00, 01, 02, 03).
 */
@Composable
fun IosFullWeatherForecastWidgetCard(
    city: String,
    tempC: Int,
    highC: Int,
    lowC: Int,
    condition: String,
    useFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    val unit = if (useFahrenheit) "°F" else "°C"
    val displayTemp = if (useFahrenheit) (tempC * 9f / 5f + 32f).roundToInt() else tempC
    val displayHigh = if (useFahrenheit) (highC * 9f / 5f + 32f).roundToInt() else highC
    val displayLow = if (useFahrenheit) (lowC * 9f / 5f + 32f).roundToInt() else lowC
    val curHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1F2633), Color(0xFF2B3548))
                )
            )
            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Top Header: City + Current Temp + Condition + High/Low
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = city,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${displayTemp}$unit",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = condition,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "H:${displayHigh}$unit T:${displayLow}$unit",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom 6-Hour Hourly Forecast Row (22  23  00  01  02  03)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (offset in 0 until 6) {
                val hour = (curHour + offset) % 24
                val hourStr = String.format(Locale.ROOT, "%02d", hour)
                val stepTemp = displayTemp - (offset / 2)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hourStr,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = if (offset % 3 == 0) Icons.Default.Grain else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stepTemp}$unit",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 1:1 Authentic iOS Widget Gallery Bottom Sheet (Exact match to Screenshot 1!)
 * Slides up over the Home Screen with grab handle, "Widgets suchen" search bar,
 * Full-width Weather Forecast Widget, and 2x2 grid of Analog Clock, Month Calendar, Photo & Battery!
 */
@Composable
fun IosWidgetGalleryBottomSheet(
    weatherCity: String,
    weatherTempCelsius: Int,
    weatherHighCelsius: Int,
    weatherLowCelsius: Int,
    weatherCondition: String,
    useFahrenheit: Boolean,
    batteryPercent: Int,
    showWidgetsOnHome: Boolean,
    onSelectWidgetsForHome: (LeftWidgetType, RightWidgetType) -> Unit,
    onToggleWidgetsOnHome: (Boolean) -> Unit,
    onAutoSortSmartFolders: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Color(0xF51C1C1E))
                .border(
                    width = 0.6.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Top Grab Handle
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5A5A5E))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // "Widgets suchen" Frosted Pill Search Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color(0xFF3A3A3C))
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF98989F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Widgets suchen",
                            color = Color(0xFF98989F),
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color(0xFF0A84FF))
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Full-Width Weather Forecast Widget ("Wetter / Vorhersage")
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectWidgetsForHome(LeftWidgetType.CALENDAR_CLOCK, RightWidgetType.STORAGE_RAM)
                                onDismiss()
                            }
                    ) {
                        IosFullWeatherForecastWidgetCard(
                            city = weatherCity,
                            tempC = weatherTempCelsius,
                            highC = weatherHighCelsius,
                            lowC = weatherLowCelsius,
                            condition = weatherCondition,
                            useFahrenheit = useFahrenheit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Wetter",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Vorhersage",
                            color = Color(0xFF98989F),
                            fontSize = 13.sp
                        )
                    }
                }

                // 2. Row 1 of 2x2 Widgets: "Uhr / Uhr I" & "Kalender / Monat"
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectWidgetsForHome(LeftWidgetType.CALENDAR_CLOCK, RightWidgetType.LIVE_WEATHER)
                                    onDismiss()
                                }
                        ) {
                            IosAnalogClockWidgetCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Uhr",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Uhr I",
                                color = Color(0xFF98989F),
                                fontSize = 13.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectWidgetsForHome(LeftWidgetType.SIRI_SUGGESTIONS, RightWidgetType.LIVE_WEATHER)
                                    onDismiss()
                                }
                        ) {
                            IosMonthCalendarWidgetCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kalender",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Monat",
                                color = Color(0xFF98989F),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 3. Row 2 of 2x2 Widgets: "Foto / Empfohlen" & "Batterie / Status"
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectWidgetsForHome(LeftWidgetType.QUICK_NOTE, RightWidgetType.LIVE_WEATHER)
                                    onDismiss()
                                }
                        ) {
                            IosPhotoRecommendWidgetCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Foto",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Empfohlen",
                                color = Color(0xFF98989F),
                                fontSize = 13.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectWidgetsForHome(LeftWidgetType.BATTERY_STATUS, RightWidgetType.MEDIA_CONTROLLER)
                                    onDismiss()
                                }
                        ) {
                            IosBatteryRingWidgetCard(
                                batteryPercent = batteryPercent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Batterie",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Status",
                                color = Color(0xFF98989F),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Quick actions at bottom of Widget Gallery
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2C2C2E))
                                .clickable {
                                    onToggleWidgetsOnHome(!showWidgetsOnHome)
                                    onDismiss()
                                }
                                .padding(vertical = 13.dp)
                        ) {
                            Text(
                                text = if (showWidgetsOnHome) "Widgets ausblenden" else "Widgets einblenden",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0A84FF))
                                .clickable {
                                    onDismiss()
                                    onOpenSettings()
                                }
                                .padding(vertical = 13.dp)
                        ) {
                            Text(
                                text = "TMS OS Einstellungen",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
