package com.tms.os.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosText
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Horizontal position of a card relative to the screen centre, in card
 * spacings. depth 0 = front card; older cards (depth > 0) stack to the left and
 * tighten like on iOS, newer ones (depth < 0) leave quickly to the right.
 */
internal fun switcherX(depth: Float): Float = when {
    depth <= 0f -> depth * -1.75f
    depth <= 1f -> -depth
    else -> -(1f + (depth - 1f) * 0.72f)
}

/**
 * iOS-style app switcher inside the launcher: a horizontal stack of app
 * cards over the blurred wallpaper. Swipe sideways to browse, tap a card to
 * open the app (the card grows to full screen first), flick a card up to
 * close it. [progress] (0..1) is driven by the dock swipe-up gesture.
 */
@Composable
fun IosAppSwitcher(
    apps: List<AppInfo>,
    progress: () -> Float,
    onOpen: (AppInfo) -> Unit,
    onDismissApp: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val scroll = remember { Animatable(0f) }
    var opening by remember { mutableStateOf<String?>(null) }
    val openAnim = remember { Animatable(0f) }
    BackHandler(onBack = onClose)

    // Sideways browsing lives on the parent so it also works when the swipe
    // starts on a card (cards only claim vertical flicks and taps).
    val browse = Modifier.pointerInput(apps.size) {
        val spacingPx = size.width * 0.66f * 0.58f
        val lastIdx = (apps.size - 1).coerceAtLeast(0).toFloat()
        val tracker = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = { tracker.resetTracking() },
            onHorizontalDrag = { change, dx ->
                change.consume()
                tracker.addPosition(change.uptimeMillis, change.position)
                val raw = scroll.value + dx / spacingPx
                val v = when {
                    raw < 0f -> raw * 0.35f
                    raw > lastIdx -> lastIdx + (raw - lastIdx) * 0.35f
                    else -> raw
                }
                scope.launch { scroll.snapTo(v) }
            },
            onDragEnd = {
                val v = tracker.calculateVelocity().x / spacingPx
                val target = (scroll.value + v * 0.22f).roundToInt().toFloat().coerceIn(0f, lastIdx)
                scope.launch { scroll.animateTo(target, spring(dampingRatio = 0.86f, stiffness = 220f), v) }
            }
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().then(browse)) {
        val density = LocalDensity.current
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val cardW = screenW * 0.66f
        val cardH = cardW * (screenH / screenW)
        val spacing = cardW * 0.58f
        val last = (apps.size - 1).coerceAtLeast(0).toFloat()

        LaunchedEffect(apps.size) { if (scroll.value > last) scroll.animateTo(last, spring(1f, 300f)) }

        // Dim backdrop; tap empty space to return home.
        Box(
            Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.42f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) }
        )

        apps.forEachIndexed { i, app ->
            key(app.packageName) {
                SwitcherCard(
                    app = app,
                    index = i,
                    scroll = { scroll.value },
                    progress = progress,
                    cardW = cardW, cardH = cardH, spacing = spacing,
                    screenW = screenW, screenH = screenH,
                    openProgress = { if (opening == app.packageName) openAnim.value else 0f },
                    onTap = {
                        if (opening == null) {
                            opening = app.packageName
                            scope.launch {
                                openAnim.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 380f))
                                onOpen(app)
                            }
                        }
                    },
                    onFlungAway = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismissApp(app)
                    },
                    modifier = Modifier.zIndex(if (opening == app.packageName) 1000f else 500f - i)
                )
            }
        }
    }
}

@Composable
private fun SwitcherCard(
    app: AppInfo,
    index: Int,
    scroll: () -> Float,
    progress: () -> Float,
    cardW: Float, cardH: Float, spacing: Float,
    screenW: Float, screenH: Float,
    openProgress: () -> Float,
    onTap: () -> Unit,
    onFlungAway: () -> Unit,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lift = remember { Animatable(0f) }
    // Smoothly close the gap when a neighbour is flicked away.
    val slot by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.85f, stiffness = 260f), label = "slot")
    val tint = remember(app.packageName) { dominantColor(app.iconBitmap) }
    val wDp = with(density) { cardW.toDp() }
    val hDp = with(density) { cardH.toDp() }

    Box(
        modifier
            .requiredSize(wDp, hDp)
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                val o = openProgress()
                val depth = slot - scroll()
                val cx = screenW / 2f + switcherX(depth) * spacing
                val cy = screenH / 2f + lift.value + (1f - p) * screenH * 0.25f
                // The card is laid out at the top-left corner; everything else is draw-time translation.
                val baseX = cx - cardW / 2f
                val baseY = cy - cardH / 2f
                translationX = baseX + (0f - baseX) * o
                translationY = baseY + (0f - baseY) * o
                val s = (0.92f + 0.08f * p) * (1f + (screenW / cardW - 1f) * o)
                scaleX = s; scaleY = s
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f).takeIf { o > 0f }
                    ?: androidx.compose.ui.graphics.TransformOrigin.Center
                alpha = p * (1f - (-lift.value / screenH).coerceIn(0f, 1f) * 0.8f) *
                    (if (depth > 4f) (5f - depth).coerceIn(0f, 1f) else 1f)
                shadowElevation = 24.dp.toPx()
                shape = RoundedCornerShape((38f * (1f - o) + 0f).dp)
                clip = true
            }
            .pointerInput(app.packageName) { detectTapGestures(onTap = { onTap() }) }
            .pointerInput(app.packageName) {
                val tracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val next = lift.value + if (lift.value + dy > 0f) dy * 0.25f else dy
                        scope.launch { lift.snapTo(next) }
                    },
                    onDragEnd = {
                        val vy = tracker.calculateVelocity().y
                        scope.launch {
                            if (lift.value < -cardH * 0.28f || vy < -1400f) {
                                lift.animateTo(-screenH * 1.1f, tween(220))
                                onFlungAway()
                            } else lift.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 420f))
                        }
                    }
                )
            }
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 1f), tint.darker(0.72f))))
    ) {
        Box(Modifier.align(Alignment.Center).size(96.dp).clip(IosSquircle)) {
            app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
    }

    // Icon + name above the card (fades with the card, hidden while opening).
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .zIndex(500f - index)
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                val depth = slot - scroll()
                val cx = screenW / 2f + switcherX(depth) * spacing
                val cy = screenH / 2f + lift.value + (1f - p) * screenH * 0.25f
                translationX = cx - cardW / 2f
                translationY = cy - cardH / 2f - 44.dp.toPx()
                alpha = p * (1f - openProgress()) * (1f - (-lift.value / (cardH * 0.5f)).coerceIn(0f, 1f)) *
                    (if (depth > 1.5f) 0f else if (depth > 0.5f) (1.5f - depth) else 1f)
            }
    ) {
        Box(Modifier.size(32.dp).clip(IosSquircle)) {
            app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(8.dp))
        Text(app.displayLabel, style = IosText.subheadline.copy(color = Color.White, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Average colour of the icon, used as the card's "screenshot" surface. */
private fun dominantColor(bitmap: ImageBitmap?): Color {
    if (bitmap == null) return Color(0xFF2C2C2E)
    return runCatching {
        val small = android.graphics.Bitmap.createScaledBitmap(bitmap.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, false), 12, 12, true)
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        var white = 0; var opaque = 0
        for (y in 0 until 12) for (x in 0 until 12) {
            val c = small.getPixel(x, y)
            if ((c ushr 24) < 128) continue
            opaque++
            val cr = c shr 16 and 255; val cg = c shr 8 and 255; val cb = c and 255
            if (cr > 232 && cg > 232 && cb > 232) { white++; continue }
            r += cr; g += cg; b += cb; n++
        }
        // A logo on a white plate (Chrome, Gmail, Google …) opens on a light canvas;
        // averaging a multicolour logo would only give mud.
        if (n == 0L || (opaque > 0 && white.toFloat() / opaque > 0.28f)) return Color(0xFFF2F2F7)
        val avg = Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt()), hsv)
        if (hsv[1] < 0.32f) Color(0xFFF2F2F7) else avg
    }.getOrDefault(Color(0xFF2C2C2E))
}

private fun Color.darker(f: Float) = Color(red * f, green * f, blue * f, alpha)
