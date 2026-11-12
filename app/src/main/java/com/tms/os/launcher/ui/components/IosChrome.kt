package com.tms.os.launcher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.ui.theme.wallpaperGlass
import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.ui.theme.IosText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* -------------------------------------------------------------------------- */
/*                                 Status Bar                                 */
/* -------------------------------------------------------------------------- */

private data class BatteryState(val level: Int, val charging: Boolean)

@Composable
fun IosStatusBar(
    lightContent: Boolean = true,
    language: AppLanguage = AppLanguage.DE,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    var battery by remember { mutableStateOf(readBattery(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                battery = readBattery(context)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val formatter = remember(is24Hour, language) {
        SimpleDateFormat(if (is24Hour) "H:mm" else "h:mm", Locale.forLanguageTag(language.code))
    }
    val connectivity = remember { context.getSystemService(ConnectivityManager::class.java) }
    var wifiConnected by remember { mutableStateOf(false) }
    var cellularConnected by remember { mutableStateOf(false) }
    DisposableEffect(connectivity) {
        fun updateNetwork() {
            val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            cellularConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = updateNetwork()
            override fun onLost(network: Network) = updateNetwork()
        }
        updateNetwork()
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    val timeText = formatter.format(now)
    val tint = if (lightContent) Color.White else Color(0xFF151515)
    val shadow = if (lightContent) {
        Shadow(Color.Black.copy(alpha = 0.18f), Offset(0f, 1f), 2f)
    } else {
        Shadow(Color.White.copy(alpha = 0.25f), Offset(0f, 1f), 3f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(start = 30.dp, end = 27.dp, top = 6.dp)
            .semantics { contentDescription = "Statusleiste" }
    ) {
        Text(
            text = timeText,
            color = tint,
            style = IosText.headline.copy(
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp,
                shadow = shadow
            )
        )

        Spacer(Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (cellularConnected) Text(if (language == AppLanguage.DE) "Mobil" else "Cell", color = tint, style = IosText.caption)
            if (wifiConnected) WifiGlyph(color = tint)
            BatteryGlyph(level = battery.level, charging = battery.charging, color = tint)
        }
    }
}

private fun readBattery(context: Context): BatteryState {
    return try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100f / scale).toInt() else 85
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        BatteryState(pct.coerceIn(0, 100), charging)
    } catch (_: Exception) {
        BatteryState(85, false)
    }
}

@Composable
private fun CellularBars(color: Color) {
    Canvas(modifier = Modifier.size(width = 18.dp, height = 12.dp)) {
        val barW = 3.1.dp.toPx()
        val gap = 1.7.dp.toPx()
        val heights = listOf(4.2f, 6.8f, 9.2f, 11.6f)
        heights.forEachIndexed { i, h ->
            val x = i * (barW + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h.dp.toPx()),
                size = Size(barW, h.dp.toPx()),
                cornerRadius = CornerRadius(0.9.dp.toPx(), 0.9.dp.toPx())
            )
        }
    }
}

@Composable
private fun WifiGlyph(color: Color) {
    Canvas(modifier = Modifier.size(17.dp)) {
        val stroke = 1.9.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height * 0.80f
        val paint = Stroke(width = stroke, cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(cx - size.width * 0.48f, cy - size.height * 0.74f),
            size = Size(size.width * 0.96f, size.height * 0.96f),
            style = paint
        )
        drawArc(
            color = color,
            startAngle = 215f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(cx - size.width * 0.32f, cy - size.height * 0.52f),
            size = Size(size.width * 0.64f, size.height * 0.64f),
            style = paint
        )
        drawCircle(
            color = color,
            radius = 1.8.dp.toPx(),
            center = Offset(cx, cy - 1.1.dp.toPx())
        )
    }
}

@Composable
private fun BatteryGlyph(level: Int, charging: Boolean, color: Color) {
    val fillColor = when {
        charging -> Color(0xFF30D158)
        level <= 20 -> Color(0xFFFF453A)
        else -> color
    }
    Canvas(modifier = Modifier.size(width = 27.dp, height = 13.dp)) {
        val stroke = 1.15.dp.toPx()
        val bodyW = size.width - 3.4.dp.toPx()
        val bodyH = size.height - 2.dp.toPx()
        val top = 1.dp.toPx()
        val corner = 3.4.dp.toPx()
        drawRoundRect(
            color = color.copy(alpha = 0.42f),
            topLeft = Offset(0f, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = color.copy(alpha = 0.42f),
            topLeft = Offset(size.width - 2.4.dp.toPx(), size.height * 0.35f),
            size = Size(2.4.dp.toPx(), size.height * 0.30f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        val pad = 2.1.dp.toPx()
        val innerW = (bodyW - pad * 2) * (level.coerceIn(4, 100) / 100f)
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(pad, top + pad),
            size = Size(innerW, bodyH - pad * 2),
            cornerRadius = CornerRadius(1.6.dp.toPx(), 1.6.dp.toPx())
        )
        if (charging) {
            rotate(-90f) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.55f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 0f),
                    strokeWidth = 0f
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                               Home Chrome                                  */
/* -------------------------------------------------------------------------- */

@Composable
fun IosPageDots(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 1) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.5.dp),
        modifier = modifier
            .height(24.dp)
            .clip(CircleShape)
            .wallpaperGlass(CircleShape, tint = Color.White.copy(alpha = 0.05f), rim = 0.45f)
            .padding(horizontal = 10.dp)
            .semantics { contentDescription = "Seite ${selected + 1} von $count" }
    ) {
        repeat(count) { i ->
            val active = i == selected
            val size by animateDpAsState(
                targetValue = if (active) 6.5.dp else 6.dp,
                animationSpec = spring(stiffness = 700f),
                label = "dotSize"
            )
            val alpha by animateFloatAsState(
                targetValue = if (active) 1f else 0.34f,
                animationSpec = spring(stiffness = 700f),
                label = "dotAlpha"
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun IosSearchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .height(30.dp)
            .wallpaperGlass(CircleShape, tint = Color.White.copy(alpha = 0.06f), rim = 0.5f)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = text }
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.92f),
            style = IosText.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
        )
    }
}

@Composable
fun IosTopEditBar(
    editLabel: String,
    doneLabel: String,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 6.dp)
    ) {
        GlassPill(text = editLabel, onClick = onEdit)
        GlassPill(text = doneLabel, onClick = onDone)
    }
}

@Composable
private fun GlassPill(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .wallpaperGlass(CircleShape, tint = Color.Black.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = IosText.subheadline.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * The iOS "search" pill / widget-gallery style material. Exposed so overlays
 * can share exactly the same optical recipe.
 */
fun Modifier.iosGlass(
    color: Color = Color(0x8C1C1C1E),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    shape: androidx.compose.ui.graphics.Shape = CircleShape
): Modifier = this
    .clip(shape)
    .background(color)
    .border(0.5.dp, borderColor, shape)
