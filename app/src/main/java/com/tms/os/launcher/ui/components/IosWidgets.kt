package com.tms.os.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.LeftWidgetType
import com.tms.os.launcher.model.RightWidgetType
import com.tms.os.launcher.ui.theme.IosColor
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.SfDisplayFamily
import com.tms.os.launcher.ui.theme.SfLightFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* -------------------------------------------------------------------------- */
/*                                  Weather                                   */
/* -------------------------------------------------------------------------- */

private fun weatherGradient(code: Int, night: Boolean = false): List<Color> {
    return when (code) {
        0, 1 -> if (night) {
            listOf(Color(0xFF162A4A), Color(0xFF0A1324))
        } else {
            listOf(Color(0xFF3F8EF7), Color(0xFF1F5BC4))
        }
        2, 3 -> listOf(Color(0xFF5D87B8), Color(0xFF2A4666))
        45, 48 -> listOf(Color(0xFF7D8C9B), Color(0xFF3B4650))
        51, 53, 55, 56, 57 -> listOf(Color(0xFF4B7388), Color(0xFF172B38))
        61, 63, 65, 66, 67, 80, 81, 82 -> listOf(Color(0xFF3B5C77), Color(0xFF141F2C))
        71, 73, 75, 77, 85, 86 -> listOf(Color(0xFF6E91AC), Color(0xFF304A60))
        95, 96, 99 -> listOf(Color(0xFF343A55), Color(0xFF151827))
        else -> listOf(Color(0xFF3F8EF7), Color(0xFF1F5BC4))
    }
}

@Composable
fun WeatherWidget(
    city: String,
    tempCelsius: Int,
    highCelsius: Int,
    lowCelsius: Int,
    condition: String,
    weatherCode: Int,
    useFahrenheit: Boolean,
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val temp = if (useFahrenheit) (tempCelsius * 9 / 5 + 32) else tempCelsius
    val high = if (useFahrenheit) (highCelsius * 9 / 5 + 32) else highCelsius
    val low = if (useFahrenheit) (lowCelsius * 9 / 5 + 32) else lowCelsius
    val gradient = weatherGradient(weatherCode)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = city,
                    color = Color.White,
                    style = IosText.footnote.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                WeatherGlyph(code = weatherCode, color = Color.White, size = 34.dp)
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "$temp°",
                color = Color.White,
                style = IosText.largeTitle.copy(
                    fontFamily = SfLightFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 52.sp,
                    letterSpacing = (-2).sp,
                    lineHeight = 56.sp
                )
            )
            Text(
                text = condition,
                color = Color.White.copy(alpha = 0.94f),
                style = IosText.footnote.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = strings.highLow.format(Locale.US, high, low),
                color = Color.White.copy(alpha = 0.72f),
                style = IosText.footnote.copy(fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun WeatherGlyph(code: Int, color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.065f, cap = StrokeCap.Round)

        fun sun(center: Offset, radius: Float) {
            drawCircle(color = color, radius = radius * 0.46f, center = center)
            for (i in 0 until 8) {
                val a = Math.toRadians((i * 45).toDouble())
                val sx = center.x + cos(a).toFloat() * radius * 0.62f
                val sy = center.y + sin(a).toFloat() * radius * 0.62f
                val ex = center.x + cos(a).toFloat() * radius * 0.92f
                val ey = center.y + sin(a).toFloat() * radius * 0.92f
                drawLine(color, Offset(sx, sy), Offset(ex, ey), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
            }
        }

        fun cloud(offsetX: Float = 0f, offsetY: Float = 0f, alpha: Float = 1f) {
            val c = color.copy(alpha = alpha)
            drawCircle(c, radius = w * 0.17f, center = Offset(w * 0.40f + offsetX, h * 0.56f + offsetY))
            drawCircle(c, radius = w * 0.22f, center = Offset(w * 0.58f + offsetX, h * 0.47f + offsetY))
            drawCircle(c, radius = w * 0.16f, center = Offset(w * 0.74f + offsetX, h * 0.56f + offsetY))
            drawRoundRect(
                color = c,
                topLeft = Offset(w * 0.24f + offsetX, h * 0.53f + offsetY),
                size = Size(w * 0.62f, h * 0.20f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f)
            )
        }

        when (code) {
            0, 1 -> sun(Offset(w * 0.5f, h * 0.5f), w * 0.42f)
            2 -> {
                sun(Offset(w * 0.66f, h * 0.34f), w * 0.26f)
                cloud(offsetX = -w * 0.04f, offsetY = h * 0.05f)
            }
            3 -> cloud()
            45, 48 -> {
                for (i in 0 until 3) {
                    drawLine(
                        color = color.copy(alpha = 0.9f),
                        start = Offset(w * 0.20f, h * (0.36f + i * 0.15f)),
                        end = Offset(w * 0.80f, h * (0.36f + i * 0.15f)),
                        strokeWidth = w * 0.07f,
                        cap = StrokeCap.Round
                    )
                }
            }
            in 51..67, in 80..82 -> {
                cloud(offsetY = -h * 0.08f)
                for (i in 0 until 3) {
                    drawLine(
                        color = color.copy(alpha = 0.9f),
                        start = Offset(w * (0.34f + i * 0.14f), h * 0.78f),
                        end = Offset(w * (0.31f + i * 0.14f), h * 0.92f),
                        strokeWidth = w * 0.045f,
                        cap = StrokeCap.Round
                    )
                }
            }
            in 71..77, in 85..86 -> {
                cloud(offsetY = -h * 0.08f)
                drawCircle(color.copy(alpha = 0.95f), w * 0.035f, Offset(w * 0.36f, h * 0.83f))
                drawCircle(color.copy(alpha = 0.95f), w * 0.035f, Offset(w * 0.52f, h * 0.86f))
                drawCircle(color.copy(alpha = 0.95f), w * 0.035f, Offset(w * 0.68f, h * 0.83f))
            }
            95, 96, 99 -> {
                cloud(offsetY = -h * 0.10f)
                val bolt = Path().apply {
                    moveTo(w * 0.50f, h * 0.66f)
                    lineTo(w * 0.42f, h * 0.82f)
                    lineTo(w * 0.54f, h * 0.82f)
                    lineTo(w * 0.48f, h * 0.96f)
                    lineTo(w * 0.66f, h * 0.76f)
                    lineTo(w * 0.54f, h * 0.76f)
                    close()
                }
                drawPath(bolt, Color(0xFFFFD60A))
            }
            else -> sun(Offset(w * 0.5f, h * 0.5f), w * 0.42f)
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                    Clock                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun ClockWidget(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000)
        }
    }
    val hour = now.get(Calendar.HOUR)
    val minute = now.get(Calendar.MINUTE)
    val second = now.get(Calendar.SECOND)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141416))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = min(size.width, size.height)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = d * 0.47f
            drawCircle(color = Color(0xFFF7F7F9), radius = radius, center = Offset(cx, cy))

            // Minute ticks
            for (i in 0 until 60) {
                val angle = Math.toRadians((i * 6 - 90).toDouble())
                val outer = radius * 0.965f
                val inner = if (i % 5 == 0) radius * 0.89f else radius * 0.935f
                drawLine(
                    color = Color.Black.copy(alpha = if (i % 5 == 0) 0.9f else 0.32f),
                    start = Offset(cx + cos(angle).toFloat() * inner, cy + sin(angle).toFloat() * inner),
                    end = Offset(cx + cos(angle).toFloat() * outer, cy + sin(angle).toFloat() * outer),
                    strokeWidth = if (i % 5 == 0) d * 0.012f else d * 0.004f,
                    cap = StrokeCap.Round
                )
            }

            // Hour numbers
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                textSize = d * 0.095f
            }
            for (i in 1..12) {
                val angle = Math.toRadians((i * 30 - 90).toDouble())
                val x = cx + cos(angle).toFloat() * radius * 0.77f
                val y = cy + sin(angle).toFloat() * radius * 0.77f
                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    x,
                    y - (textPaint.ascent() + textPaint.descent()) / 2f,
                    textPaint
                )
            }

            fun hand(degrees: Float, length: Float, width: Float, color: Color) {
                rotate(degrees, Offset(cx, cy)) {
                    drawLine(
                        color = color,
                        start = Offset(cx, cy),
                        end = Offset(cx, cy - length),
                        strokeWidth = width,
                        cap = StrokeCap.Round
                    )
                }
            }

            hand(hour * 30f + minute * 0.5f, radius * 0.50f, d * 0.028f, Color(0xFF141416))
            hand(minute * 6f + second * 0.1f, radius * 0.70f, d * 0.022f, Color(0xFF141416))
            hand(second * 6f, radius * 0.78f, d * 0.010f, Color(0xFFFF9F0A))
            drawCircle(Color(0xFFFF9F0A), d * 0.018f, Offset(cx, cy))
            drawCircle(Color(0xFF141416), d * 0.010f, Offset(cx, cy))
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                  Calendar                                  */
/* -------------------------------------------------------------------------- */

@Composable
fun CalendarWidget(
    modifier: Modifier = Modifier,
    language: com.tms.os.launcher.model.AppLanguage = com.tms.os.launcher.model.AppLanguage.DE,
    onClick: () -> Unit = {}
) {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) { while (true) { now = Calendar.getInstance(); delay(60_000) } }
    val locale = Locale.forLanguageTag(language.code)
    val weekday = SimpleDateFormat("EEEE", locale).format(now.time).uppercase(locale)
    val day = now.get(Calendar.DAY_OF_MONTH).toString()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF7F7F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = weekday,
                color = Color(0xFFFF3B30),
                style = IosText.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = day,
                color = Color(0xFF1C1C1E),
                style = IosText.largeTitle.copy(
                    fontFamily = SfDisplayFamily,
                    fontSize = 58.sp,
                    lineHeight = 64.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = SimpleDateFormat("MMMM yyyy", locale).format(now.time),
                color = Color(0xFF8A8A8E),
                style = IosText.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                  Battery                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun BatteryWidget(
    percent: Int,
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141416))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.battery,
                    color = Color(0xFF30D158),
                    style = IosText.footnote.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$percent %",
                    color = Color.White,
                    style = IosText.headline
                )
            }
            Spacer(Modifier.weight(1f))
            Canvas(modifier = Modifier.size(82.dp)) {
                val stroke = 11.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF30D158),
                    startAngle = -90f,
                    sweepAngle = 360f * percent / 100f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                              Siri Suggestions                              */
/* -------------------------------------------------------------------------- */

@Composable
fun SuggestionsWidget(
    apps: List<AppInfo>,
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    modifier: Modifier = Modifier,
    onClickApp: (AppInfo) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE62C2C2E))
            .padding(13.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = strings.siriSuggestions,
                color = Color.White.copy(alpha = 0.65f),
                style = IosText.caption.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (col in 0 until 2) {
                            val app = apps.getOrNull(row * 2 + col)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = app != null) { app?.let(onClickApp) }
                            ) {
                                if (app?.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = app?.displayLabel.orEmpty(),
                                    color = Color.White.copy(alpha = 0.92f),
                                    style = IosText.caption,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                    Music                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun MusicWidget(
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    var playing by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A2547), Color(0xFF161018))
                )
            )
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.music,
                    color = Color(0xFFFF375F),
                    style = IosText.caption.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFF8FAB), Color(0xFF7A2C5C), Color(0xFF1C1018))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161018))
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "TMS OS",
                        color = Color.White,
                        style = IosText.footnote.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Text(
                        text = "Apple Music",
                        color = Color.White.copy(alpha = 0.55f),
                        style = IosText.caption,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            playing = !playing
                            onPlayPause()
                        }
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onNext)
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                    Note                                    */
/* -------------------------------------------------------------------------- */

@Composable
fun NoteWidget(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFD60A))
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Notiz",
                color = Color.Black.copy(alpha = 0.55f),
                style = IosText.caption.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                color = Color.Black.copy(alpha = 0.86f),
                style = IosText.subheadline,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                             System / Storage                               */
/* -------------------------------------------------------------------------- */

@Composable
fun SystemWidget(
    usedRamGb: Float,
    totalRamGb: Float,
    usedStorageGb: Int,
    totalStorageGb: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1C1C1E))
            .padding(15.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            SystemMetric("Arbeitsspeicher", usedRamGb, totalRamGb.toFloat())
            SystemMetric("Speicher", usedStorageGb.toFloat(), totalStorageGb.toFloat())
        }
    }
}

@Composable
private fun SystemMetric(label: String, used: Float, total: Float) {
    val pct = if (total > 0f) (used / total).coerceIn(0f, 1f) else 0f
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                style = IosText.caption
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(pct * 100).toInt()} %",
                color = Color.White,
                style = IosText.caption.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(IosColor.blue)
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                   Facade                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun IosHomeWidget(
    leftType: LeftWidgetType,
    language: com.tms.os.launcher.model.AppLanguage = com.tms.os.launcher.model.AppLanguage.DE,
    rightType: RightWidgetType,
    isLeft: Boolean,
    strings: com.tms.os.launcher.ui.theme.IosStrings,
    weatherCity: String,
    weatherTemp: Int,
    weatherHigh: Int,
    weatherLow: Int,
    weatherCondition: String,
    weatherCode: Int,
    useFahrenheit: Boolean,
    batteryPercent: Int,
    quickNoteText: String,
    suggestedApps: List<AppInfo>,
    usedRamGb: Float,
    totalRamGb: Float,
    usedStorageGb: Int,
    totalStorageGb: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClickApp: (AppInfo) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit
) {
    if (isLeft) {
        when (leftType) {
            LeftWidgetType.CALENDAR_CLOCK -> CalendarWidget(modifier = modifier.aspectRatio(1f), language = language, onClick = onClick)
            LeftWidgetType.BATTERY_STATUS -> BatteryWidget(percent = batteryPercent, strings = strings, modifier = modifier.aspectRatio(1f), onClick = onClick)
            LeftWidgetType.SIRI_SUGGESTIONS -> SuggestionsWidget(apps = suggestedApps.take(4), strings = strings, modifier = modifier.aspectRatio(1f), onClickApp = onClickApp)
            LeftWidgetType.QUICK_NOTE -> NoteWidget(text = quickNoteText, modifier = modifier.aspectRatio(1f), onClick = onClick)
        }
    } else {
        when (rightType) {
            RightWidgetType.LIVE_WEATHER -> WeatherWidget(
                city = weatherCity,
                tempCelsius = weatherTemp,
                highCelsius = weatherHigh,
                lowCelsius = weatherLow,
                condition = weatherCondition,
                weatherCode = weatherCode,
                useFahrenheit = useFahrenheit,
                strings = strings,
                modifier = modifier.aspectRatio(1f),
                onClick = onClick
            )
            RightWidgetType.MEDIA_CONTROLLER -> MusicWidget(strings = strings, modifier = modifier.aspectRatio(1f), onPlayPause = onMediaPlayPause, onNext = onMediaNext)
            RightWidgetType.STORAGE_RAM -> SystemWidget(
                usedRamGb = usedRamGb,
                totalRamGb = totalRamGb,
                usedStorageGb = usedStorageGb,
                totalStorageGb = totalStorageGb,
                modifier = modifier.aspectRatio(1f)
            )
        }
    }
}
