@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosText
import kotlin.math.abs
import java.util.Locale

private val LabelShadow = Shadow(
    color = Color.Black.copy(alpha = 0.62f),
    offset = Offset(0f, 1f),
    blurRadius = 4.5f
)

@Composable
fun IosAppIcon(
    app: AppInfo,
    iconSize: Dp,
    labelFontSizeSp: Float = 11.5f,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    isHighlightedTarget: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickWithBounds: ((Rect) -> Unit)? = null,
    onDeleteOrMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var iconBounds by remember { mutableStateOf(Rect.Zero) }

    val scale by animateFloatAsState(
        targetValue = when {
            isHighlightedTarget -> 1.10f
            isPressed -> 0.88f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 900f),
        label = "iconScale"
    )

    val jigglePhase = remember(app.packageName) { abs(app.packageName.hashCode()) % 37 }
    val transition = rememberInfiniteTransition(label = "jiggle")
    val rotation = if (isJiggleMode) {
        val angle by transition.animateFloat(
            initialValue = -1.15f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(160 + jigglePhase, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jiggleAngle"
        )
        angle
    } else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                alpha = if (isPressed) 0.82f else 1f
            }
            .semantics {
                contentDescription = app.displayLabel
                role = Role.Button
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isJiggleMode) onLongClick()
                    else if (onClickWithBounds != null && iconBounds != Rect.Zero) onClickWithBounds(iconBounds)
                    else onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize)
                .onGloballyPositioned { iconBounds = it.boundsInRoot() }
        ) {
            if (isHighlightedTarget) {
                Box(
                    modifier = Modifier
                        .size(iconSize + 10.dp)
                        .clip(IosSquircle)
                        .background(Color.White.copy(alpha = 0.20f))
                )
            }

            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(IosSquircle)
                )
            } else {
                LetterIcon(app.displayLabel, iconSize)
            }

            // Liquid-glass edge. Barely there, but it unifies third-party icons.
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(IosSquircle)
                    .border(0.6.dp, Color.White.copy(alpha = 0.13f), IosSquircle)
            )

            if (showBadges && app.badgeCount > 0 && !isJiggleMode) {
                val text = if (app.badgeCount > 99) "99+" else app.badgeCount.toString()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .height(20.dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .padding(horizontal = 5.5.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 12.sp,
                        style = IosText.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }

            if (isJiggleMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-7).dp, y = (-7).dp)
                        .size(23.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F7))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onDeleteOrMenuClick?.invoke() ?: onLongClick() },
                            onLongClick = { onDeleteOrMenuClick?.invoke() ?: onLongClick() }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.width(iconSize + 26.dp)
            ) {
                Text(
                    text = app.displayLabel,
                    color = Color.White,
                    fontSize = labelFontSizeSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = IosText.iconLabel.copy(
                        fontSize = labelFontSizeSp.sp,
                        shadow = LabelShadow
                    )
                )
            }
        }
    }
}

@Composable
private fun LetterIcon(label: String, iconSize: Dp) {
    val hue = remember(label) { ((label.hashCode() % 360) + 360) % 360 }
    val colors = remember(hue) {
        listOf(
            Color.hsv(hue.toFloat(), 0.58f, 0.95f),
            Color.hsv(((hue + 26) % 360).toFloat(), 0.72f, 0.66f)
        )
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(iconSize)
            .clip(IosSquircle)
            .background(Brush.verticalGradient(colors))
    ) {
        Text(
            text = label.trim().take(1).uppercase(Locale.getDefault()),
            color = Color.White,
            fontSize = (iconSize.value * 0.40f).sp,
            style = IosText.title2.copy(
                fontFamily = com.tms.os.launcher.ui.theme.SfDisplayFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun IosFolderIcon(
    folder: HomeGridItem.FolderItem,
    appMap: Map<String, AppInfo>,
    iconSize: Dp,
    labelFontSizeSp: Float = 11.5f,
    showLabel: Boolean = true,
    showBadges: Boolean = true,
    isJiggleMode: Boolean = false,
    isHighlightedTarget: Boolean = false,
    onClickWithBounds: (Rect) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val apps = remember(folder.appPackages, appMap) {
        folder.appPackages.mapNotNull { appMap[it] }
    }
    val totalBadges = remember(apps) { apps.sumOf { it.badgeCount } }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var bounds by remember { mutableStateOf(Rect.Zero) }

    val scale by animateFloatAsState(
        targetValue = if (isHighlightedTarget) 1.10f else if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 900f),
        label = "folderScale"
    )

    val jigglePhase = remember(folder.folderId) { abs(folder.folderId.hashCode()) % 37 }
    val transition = rememberInfiniteTransition(label = "folderJiggle")
    val rotation = if (isJiggleMode) {
        val a by transition.animateFloat(
            initialValue = -1.15f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(160 + jigglePhase, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "folderAngle"
        )
        a
    } else 0f

    val padding = iconSize * 0.085f
    val gap = iconSize * 0.028f
    val mini = (iconSize - padding * 2 - gap * 2) / 3f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = folder.title
                role = Role.Button
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (bounds != Rect.Zero) onClickWithBounds(bounds) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize)
                .onGloballyPositioned { bounds = it.boundsInRoot() }
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(IosSquircle)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.26f),
                                Color.White.copy(alpha = 0.13f)
                            )
                        )
                    )
                    .border(0.6.dp, Color.White.copy(alpha = 0.16f), IosSquircle)
                    .padding(padding)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.size(iconSize - padding * 2)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0 until 3) {
                                val app = apps.getOrNull(row * 3 + col)
                                Box(
                                    modifier = Modifier
                                        .size(mini)
                                        .clip(IosSquircle)
                                        .background(Color.White.copy(alpha = 0.10f))
                                ) {
                                    if (app?.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(mini)
                                                .clip(IosSquircle)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showBadges && totalBadges > 0 && !isJiggleMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .height(20.dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                        .padding(horizontal = 5.5.dp)
                ) {
                    Text(
                        text = if (totalBadges > 99) "99+" else totalBadges.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        style = IosText.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    )
                }
            }

            if (isJiggleMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-7).dp, y = (-7).dp)
                        .size(23.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F7))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLongClick,
                            onLongClick = onLongClick
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = folder.title,
                color = Color.White,
                fontSize = labelFontSizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = IosText.iconLabel.copy(
                    fontSize = labelFontSizeSp.sp,
                    shadow = LabelShadow
                ),
                modifier = Modifier.width(iconSize + 26.dp)
            )
        }
    }
}
