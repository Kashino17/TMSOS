package com.tms.os.launcher.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.domain.WeatherForecast
import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.WidgetKind
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosSquircle
import com.tms.os.launcher.ui.theme.IosMaterial
import com.tms.os.launcher.ui.theme.SfDisplayFamily
import com.tms.os.launcher.ui.theme.SfFamily
import com.tms.os.launcher.ui.theme.SfLightFamily
import com.tms.os.launcher.ui.theme.wallpaperGlass
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* -------------------------------------------------------------------------- */
/*                                 Environment                                */
/* -------------------------------------------------------------------------- */

/** Everything a widget may show. One instance is shared by all widgets. */
data class WidgetEnv(
    val strings: HomeStrings,
    val language: AppLanguage,
    val dark: Boolean,
    val city: String,
    val tempC: Int,
    val highC: Int,
    val lowC: Int,
    val weatherCode: Int,
    val condition: String,
    val fahrenheit: Boolean,
    val forecast: WeatherForecast?,
    val batteryPercent: Int,
    val note: String,
    val suggestedApps: List<AppInfo>,
    val onAppClick: (AppInfo) -> Unit = {},
    /** Today's usage: total minutes and the most used apps; null until usage access is granted. */
    val screenMinutes: Int? = null,
    val screenTop: List<Pair<AppInfo, Int>> = emptyList(),
    /** TMS Solvado account and the last figures read from it. */
    val solvado: com.tms.os.launcher.model.SolvadoState = com.tms.os.launcher.model.SolvadoState(),
    /** The refresh arrow on a Solvado widget. */
    val onSolvadoRefresh: () -> Unit = {},
    /** Home-screen edit mode: a spacer shows its outline then. */
    val editing: Boolean = false
) {
    val locale: Locale get() = Locale.forLanguageTag(language.code)
    fun t(c: Int) = if (fahrenheit) c * 9 / 5 + 32 else c
}

/** Apple's point sizes for the three iPhone widget families. */
fun WidgetSize.canonicalWidth(): Dp = if (this == WidgetSize.SMALL) 158.dp else 338.dp
fun WidgetSize.canonicalHeight(): Dp = if (this == WidgetSize.LARGE) 354.dp else 158.dp

private val WidgetCorner = 22.dp
private val WidgetPadding = 16.dp

private fun bg(dark: Boolean) = if (dark) Color(0xF01C1C1E) else Color.White
private fun fg(dark: Boolean) = if (dark) Color.White else Color.Black
private fun fg2(dark: Boolean) = if (dark) Color(0x99EBEBF5) else Color(0x993C3C43)

private val IosRed = Color(0xFFFF3B30)
private val IosOrange = Color(0xFFFF9500)
private val IosGreen = Color(0xFF34C759)
private val IosYellow = Color(0xFFFFCC00)

private fun ts(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = SfFamily, fontWeight = weight, fontSize = size, color = color, letterSpacing = tracking
)

/* -------------------------------------------------------------------------- */
/*                                    Facade                                  */
/* -------------------------------------------------------------------------- */

/**
 * Renders a widget at Apple's canonical point size and scales the whole thing
 * to the space it gets, so typography and spacing keep iOS proportions on any
 * screen. The caller decides the outer size (from the home-screen grid).
 */
@Composable
fun IosWidget(kind: WidgetKind, size: WidgetSize, env: WidgetEnv, modifier: Modifier = Modifier, config: String = "") {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val base = LocalDensity.current
        val scale = min(maxWidth / size.canonicalWidth(), maxHeight / size.canonicalHeight()).coerceAtLeast(0.1f)
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, 1f)) {
            Box(
                Modifier
                    .requiredSize(size.canonicalWidth(), size.canonicalHeight())
                    .clip(RoundedCornerShape(WidgetCorner))
                    .then(
                        // The minimal widget is bare text on the wallpaper: no plate, no outline.
                        if (kind == WidgetKind.MINIMAL || kind == WidgetKind.SPACER) Modifier
                        else Modifier.border(0.6.dp, if (env.dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.06f), RoundedCornerShape(WidgetCorner))
                    )
            ) {
                when (kind) {
                    WidgetKind.WEATHER -> WeatherWidget(size, env)
                    WidgetKind.CALENDAR -> CalendarWidget(size, env, monthView = config == "view=month")
                    WidgetKind.CLOCK -> ClockWidget(size, env)
                    WidgetKind.BATTERY -> BatteryWidget(size, env)
                    WidgetKind.SUGGESTIONS -> SuggestionsWidget(size, env)
                    WidgetKind.NOTES -> NotesWidget(size, env)
                    WidgetKind.DIGITAL -> DigitalWidget(size, env)
                    WidgetKind.DATE -> DateWidget(env)
                    WidgetKind.YEAR -> YearWidget(size, env)
                    WidgetKind.SCREENTIME -> ScreenTimeWidget(size, env)
                    WidgetKind.MINIMAL -> MinimalWidget(size, env)
                    WidgetKind.SOLVADO_SHOP, WidgetKind.SOLVADO_ADMIN -> SolvadoWidget(kind, size, env, config)
                    WidgetKind.SPACER -> SpacerWidget(env)
                }
            }
        }
    }
}

@Composable
private fun rememberNow(periodMs: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    com.tms.os.launcher.ui.theme.WhileResumed(periodMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(periodMs - System.currentTimeMillis() % periodMs)
        }
    }
    return now
}

/* -------------------------------------------------------------------------- */
/*                                   Weather                                  */
/* -------------------------------------------------------------------------- */

private fun isNight(millis: Long): Boolean {
    val h = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
    return h < 6 || h >= 20
}

private fun skyGradient(code: Int, night: Boolean): Brush {
    val colors = when {
        // Night: quiet slate instead of saturated navy.
        night && code <= 2 -> listOf(Color(0xFF14171D), Color(0xFF2A3342))
        night -> listOf(Color(0xFF1A1D23), Color(0xFF343C4A))
        code <= 1 -> listOf(Color(0xFF2B73D2), Color(0xFF5CA0EC))
        code == 2 -> listOf(Color(0xFF3B78C2), Color(0xFF7DA9DA))
        code == 3 || code == 45 || code == 48 -> listOf(Color(0xFF5B6F86), Color(0xFF8A9BAE))
        code in 51..67 || code in 80..82 -> listOf(Color(0xFF3F4E60), Color(0xFF66778A))
        code in 71..77 || code in 85..86 -> listOf(Color(0xFF6F8399), Color(0xFFA2B2C2))
        code >= 95 -> listOf(Color(0xFF2B2F42), Color(0xFF4C5168))
        else -> listOf(Color(0xFF2B73D2), Color(0xFF5CA0EC))
    }
    return Brush.verticalGradient(colors)
}

@Composable
private fun WeatherWidget(size: WidgetSize, env: WidgetEnv) {
    val now = rememberNow(60_000)
    val night = isNight(now)
    val white = Color.White
    Box(
        Modifier
            .fillMaxSize()
            .background(skyGradient(env.weatherCode, night))
            .padding(WidgetPadding)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(env.city, style = ts(15.sp, FontWeight.SemiBold, white), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${env.t(env.tempC)}°",
                        style = TextStyle(
                            fontFamily = SfLightFamily, fontWeight = FontWeight.Light, color = white, letterSpacing = (-1).sp,
                            fontSize = if (size == WidgetSize.SMALL) 44.sp else 40.sp,
                            lineHeight = if (size == WidgetSize.SMALL) 48.sp else 42.sp
                        )
                    )
                }
                if (size != WidgetSize.SMALL) {
                    Column(horizontalAlignment = Alignment.End) {
                        WeatherGlyph(env.weatherCode, night, Modifier.size(18.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(env.condition, style = ts(13.sp, FontWeight.SemiBold, white), maxLines = 1)
                        Text(highLow(env), style = ts(13.sp, FontWeight.SemiBold, white))
                    }
                }
            }
            if (size == WidgetSize.SMALL) {
                Spacer(Modifier.weight(1f))
                WeatherGlyph(env.weatherCode, night, Modifier.size(16.dp))
                Spacer(Modifier.height(3.dp))
                Text(env.condition, style = ts(13.sp, FontWeight.SemiBold, white), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(highLow(env), style = ts(13.sp, FontWeight.SemiBold, white))
            } else {
                Spacer(Modifier.weight(1f))
                HourlyRow(env)
                if (size == WidgetSize.LARGE) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(white.copy(alpha = 0.3f)))
                    Spacer(Modifier.height(6.dp))
                    DailyRows(env)
                }
            }
        }
    }
}

private fun highLow(env: WidgetEnv): String {
    val h = env.t(env.highC); val l = env.t(env.lowC)
    return when (env.language) {
        AppLanguage.DE -> "H:$h° T:$l°"
        AppLanguage.EN -> "H:$h° L:$l°"
        AppLanguage.TR -> "Y:$h° D:$l°"
    }
}

@Composable
private fun HourlyRow(env: WidgetEnv) {
    val hours = env.forecast?.hourly.orEmpty()
    val fmt = remember(env.language) { SimpleDateFormat(env.strings.hourFormat, env.locale) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (hours.isEmpty()) {
            // No network yet: show the current hour only, like iOS while loading.
            HourCell(env.strings.now, env.weatherCode, isNight(System.currentTimeMillis()), "${env.t(env.tempC)}°")
        } else hours.forEachIndexed { i, h ->
            HourCell(if (i == 0) env.strings.now else fmt.format(Date(h.epochMillis)), h.weatherCode, isNight(h.epochMillis), "${env.t(h.tempCelsius)}°")
        }
    }
}

@Composable
private fun HourCell(label: String, code: Int, night: Boolean, temp: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
        Text(label, style = ts(12.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.92f)), maxLines = 1)
        Spacer(Modifier.height(4.dp))
        WeatherGlyph(code, night, Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(temp, style = ts(15.sp, FontWeight.SemiBold, Color.White))
    }
}

@Composable
private fun DailyRows(env: WidgetEnv) {
    val days = env.forecast?.daily.orEmpty().take(5)
    if (days.isEmpty()) return
    val minAll = days.minOf { it.lowCelsius }
    val maxAll = days.maxOf { it.highCelsius }.coerceAtLeast(minAll + 1)
    val dayFmt = remember(env.language) { SimpleDateFormat("EE", env.locale) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        days.forEachIndexed { i, d ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(29.dp)) {
                Text(
                    if (i == 0) env.strings.today else dayFmt.format(Date(d.epochMillis)).trimEnd('.'),
                    style = ts(15.sp, FontWeight.SemiBold, Color.White), modifier = Modifier.width(64.dp), maxLines = 1
                )
                WeatherGlyph(d.weatherCode, false, Modifier.size(20.dp))
                Spacer(Modifier.width(18.dp))
                Text("${env.t(d.lowCelsius)}°", style = ts(15.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.6f)), modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                Spacer(Modifier.width(8.dp))
                Canvas(Modifier.weight(1f).height(4.dp)) {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(Color.Black.copy(alpha = 0.18f), cornerRadius = r)
                    val s = (d.lowCelsius - minAll).toFloat() / (maxAll - minAll)
                    val e = (d.highCelsius - minAll).toFloat() / (maxAll - minAll)
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color(0xFF8FD8C8), Color(0xFFFFD25A), Color(0xFFFF9A3C))),
                        topLeft = Offset(size.width * s, 0f), size = Size(size.width * (e - s).coerceAtLeast(0.06f), size.height), cornerRadius = r
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${env.t(d.highCelsius)}°", style = ts(15.sp, FontWeight.SemiBold, Color.White), modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
            }
        }
    }
}

/** Multicolour weather symbol in the style of SF Symbols "fill" variants. */
@Composable
fun WeatherGlyph(code: Int, night: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sunC = Color(0xFFFFD60A)
        val cloudC = Color.White
        fun sun(c: Offset, r: Float) {
            drawCircle(sunC, r * 0.5f, c)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                drawLine(
                    sunC,
                    Offset(c.x + cos(a).toFloat() * r * 0.72f, c.y + sin(a).toFloat() * r * 0.72f),
                    Offset(c.x + cos(a).toFloat() * r * 0.98f, c.y + sin(a).toFloat() * r * 0.98f),
                    strokeWidth = r * 0.16f, cap = StrokeCap.Round
                )
            }
        }
        fun moon(c: Offset, r: Float) {
            val p = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r))
            }
            val cut = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(c.x - r * 0.35f, c.y - r * 1.25f, c.x + r * 1.65f, c.y + r * 0.75f))
            }
            drawPath(Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, p, cut), Color.White)
        }
        fun cloud(dy: Float = 0f, color: Color = cloudC) {
            drawCircle(color, w * 0.18f, Offset(w * 0.36f, h * 0.60f + dy))
            drawCircle(color, w * 0.24f, Offset(w * 0.56f, h * 0.50f + dy))
            drawCircle(color, w * 0.16f, Offset(w * 0.76f, h * 0.62f + dy))
            drawRoundRect(color, Offset(w * 0.18f, h * 0.58f + dy), Size(w * 0.74f, h * 0.20f), CornerRadius(w * 0.1f))
        }
        fun drops(color: Color) {
            for (i in 0 until 3) drawLine(color, Offset(w * (0.36f + i * 0.16f), h * 0.86f), Offset(w * (0.32f + i * 0.16f), h * 0.98f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        }
        when {
            code <= 1 -> if (night) moon(Offset(w * 0.5f, h * 0.5f), w * 0.4f) else sun(Offset(w * 0.5f, h * 0.5f), w * 0.48f)
            code == 2 -> {
                if (night) moon(Offset(w * 0.64f, h * 0.32f), w * 0.24f) else sun(Offset(w * 0.66f, h * 0.32f), w * 0.3f)
                cloud(dy = h * 0.06f)
            }
            code == 3 -> cloud()
            code == 45 || code == 48 -> for (i in 0 until 3) drawLine(cloudC, Offset(w * 0.14f, h * (0.34f + i * 0.17f)), Offset(w * 0.86f, h * (0.34f + i * 0.17f)), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            code in 51..67 || code in 80..82 -> { cloud(dy = -h * 0.12f); drops(Color(0xFF64D2FF)) }
            code in 71..77 || code in 85..86 -> {
                cloud(dy = -h * 0.12f)
                for (i in 0 until 3) drawCircle(Color.White, w * 0.05f, Offset(w * (0.36f + i * 0.16f), h * 0.9f))
            }
            code >= 95 -> {
                cloud(dy = -h * 0.14f)
                val bolt = Path().apply {
                    moveTo(w * 0.52f, h * 0.62f); lineTo(w * 0.40f, h * 0.84f); lineTo(w * 0.52f, h * 0.84f)
                    lineTo(w * 0.46f, h * 1.0f); lineTo(w * 0.66f, h * 0.76f); lineTo(w * 0.54f, h * 0.76f); close()
                }
                drawPath(bolt, sunC)
            }
            else -> sun(Offset(w * 0.5f, h * 0.5f), w * 0.48f)
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                  Calendar                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun CalendarWidget(size: WidgetSize, env: WidgetEnv, monthView: Boolean = false) {
    val now = rememberNow(60_000)
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val weekday = SimpleDateFormat("EEEE", env.locale).format(Date(now)).uppercase(env.locale)
    val day = cal.get(Calendar.DAY_OF_MONTH).toString()
    val dark = env.dark
    // Dark appearance keeps everything white and calm; light keeps Apple's red accent.
    val accent = if (dark) Color.White else IosRed
    val weekdayStyle = ts(12.sp, FontWeight.SemiBold, accent, 0.2.sp)
    val dayStyle = TextStyle(fontFamily = SfLightFamily, fontWeight = FontWeight.Light, fontSize = 30.sp, lineHeight = 34.sp, color = fg(dark), letterSpacing = (-0.3).sp)
    val noteStyle = ts(14.sp, FontWeight.Normal, fg(dark).copy(alpha = 0.92f)).copy(lineHeight = 18.sp)
    Box(Modifier.fillMaxSize().background(bg(dark)).padding(WidgetPadding)) {
        when (size) {
            // "Month" setting: the small widget is the month calendar, as on iOS.
            WidgetSize.SMALL -> if (monthView) MonthGrid(now, env, Modifier.fillMaxSize(), compact = true) else Column(Modifier.fillMaxSize()) {
                Text(weekday, style = weekdayStyle, maxLines = 1)
                Text(day, style = dayStyle)
                Spacer(Modifier.weight(1f))
                Text(env.strings.noMoreEvents, style = noteStyle, maxLines = 2)
            }
            WidgetSize.MEDIUM -> Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxSize()) {
                    Text(weekday, style = weekdayStyle, maxLines = 1)
                    Text(day, style = dayStyle)
                    Spacer(Modifier.weight(1f))
                    Text(env.strings.noMoreEvents, style = noteStyle, maxLines = 2)
                }
                Spacer(Modifier.width(10.dp))
                MonthGrid(now, env, Modifier.weight(1f), compact = true)
            }
            WidgetSize.LARGE -> Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(day, style = dayStyle)
                    Spacer(Modifier.width(8.dp))
                    Text(weekday, style = weekdayStyle, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(10.dp))
                MonthGrid(now, env, Modifier.fillMaxWidth().weight(1f), compact = false)
                Text(env.strings.noMoreEvents, style = noteStyle)
            }
        }
    }
}

@Composable
private fun MonthGrid(now: Long, env: WidgetEnv, modifier: Modifier, compact: Boolean) {
    val dark = env.dark
    val cal = Calendar.getInstance(env.locale).apply { timeInMillis = now }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("LLLL", env.locale).format(Date(now)).uppercase(env.locale)
    val first = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val lead = (first.get(Calendar.DAY_OF_WEEK) - cal.firstDayOfWeek + 7) % 7
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val initials = (0 until 7).map { i ->
        val c = Calendar.getInstance(env.locale).apply { set(Calendar.DAY_OF_WEEK, (cal.firstDayOfWeek - 1 + i) % 7 + 1) }
        SimpleDateFormat("EEEEE", env.locale).format(c.time).uppercase(env.locale)
    }
    val fs = if (compact) 10.5.sp else 14.sp
    Column(modifier) {
        Text(month, style = ts(if (compact) 11.sp else 12.sp, FontWeight.SemiBold, if (dark) Color.White else IosRed, 0.2.sp), maxLines = 1)
        Spacer(Modifier.height(if (compact) 3.dp else 8.dp))
        Row(Modifier.fillMaxWidth()) {
            initials.forEach { Text(it, style = ts(fs, FontWeight.SemiBold, fg2(dark)), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        val cells = lead + daysInMonth
        val weeks = (cells + 6) / 7
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            for (w in 0 until weeks) Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val n = w * 7 + d - lead + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (n in 1..daysInMonth) {
                            if (n == today) Box(Modifier.size(if (compact) 15.dp else 26.dp).clip(CircleShape).background(IosRed))
                            Text(n.toString(), style = ts(fs, FontWeight.SemiBold, if (n == today) Color.White else fg(dark)), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                    Clock                                   */
/* -------------------------------------------------------------------------- */

private data class ClockCity(val name: (AppLanguage) -> String, val zone: TimeZone)

private val WorldCities = listOf(
    ClockCity({ "Cupertino" }, TimeZone.getTimeZone("America/Los_Angeles")),
    ClockCity({ if (it == AppLanguage.EN) "Tokyo" else if (it == AppLanguage.TR) "Tokyo" else "Tokio" }, TimeZone.getTimeZone("Asia/Tokyo")),
    ClockCity({ "Sydney" }, TimeZone.getTimeZone("Australia/Sydney"))
)

@Composable
private fun ClockWidget(size: WidgetSize, env: WidgetEnv) {
    val now = rememberNow(1_000)
    val dark = env.dark
    val local = TimeZone.getDefault()
    val localName = local.id.substringAfterLast('/').replace('_', ' ')
    Box(Modifier.fillMaxSize().background(if (dark) Color.Black else Color.White)) {
        if (size == WidgetSize.SMALL) {
            AnalogDial(now, local, localName.take(3).uppercase(env.locale), dark, Modifier.fillMaxSize().padding(8.dp))
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val all = listOf(ClockCity({ localName }, local)) + WorldCities
                all.forEach { c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp)) {
                        AnalogDial(now, c.zone, null, dark, Modifier.size(66.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(c.name(env.language), style = ts(13.sp, FontWeight.SemiBold, fg(dark)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val diff = (c.zone.getOffset(now) - local.getOffset(now)) / 3_600_000
                        Text(
                            if (diff == 0) env.strings.today else (if (diff > 0) "+$diff" else "$diff") + " h",
                            style = ts(11.sp, FontWeight.Medium, fg2(dark)), maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalogDial(now: Long, zone: TimeZone, label: String?, dark: Boolean, modifier: Modifier) {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
    val h = cal.get(Calendar.HOUR); val m = cal.get(Calendar.MINUTE); val s = cal.get(Calendar.SECOND)
    val faceDark = dark || cal.get(Calendar.HOUR_OF_DAY) !in 6..17
    val face = if (faceDark) Color(0xFF1C1C1E) else Color.White
    val ink = if (faceDark) Color.White else Color.Black
    Canvas(modifier) {
        val d = min(size.width, size.height)
        val c = center
        val r = d / 2f
        drawCircle(face, r, c)
        if (!faceDark) drawCircle(Color.Black.copy(alpha = 0.08f), r, c, style = Stroke(d * 0.006f))
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90)
            val big = i % 5 == 0
            val inner = r * if (big) 0.86f else 0.9f
            drawLine(
                ink.copy(alpha = if (big) 0.9f else 0.35f),
                Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner),
                Offset(c.x + cos(a).toFloat() * r * 0.95f, c.y + sin(a).toFloat() * r * 0.95f),
                strokeWidth = d * if (big) 0.014f else 0.006f
            )
        }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.toArgb(); textAlign = android.graphics.Paint.Align.CENTER
            textSize = d * 0.12f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        for (i in 1..12) {
            val a = Math.toRadians(i * 30.0 - 90)
            drawContext.canvas.nativeCanvas.drawText(
                i.toString(), c.x + cos(a).toFloat() * r * 0.68f,
                c.y + sin(a).toFloat() * r * 0.68f - (paint.ascent() + paint.descent()) / 2f, paint
            )
        }
        if (label != null) {
            paint.textSize = d * 0.085f; paint.color = ink.copy(alpha = 0.55f).toArgb()
            drawContext.canvas.nativeCanvas.drawText(label, c.x, c.y - r * 0.28f, paint)
        }
        fun hand(deg: Float, len: Float, width: Float, color: Color, tail: Float = 0f) = rotate(deg, c) {
            drawLine(color, Offset(c.x, c.y + tail), Offset(c.x, c.y - len), strokeWidth = width, cap = StrokeCap.Round)
        }
        hand(h * 30f + m * 0.5f, r * 0.5f, d * 0.045f, ink)
        hand(m * 6f + s * 0.1f, r * 0.78f, d * 0.035f, ink)
        drawCircle(ink, d * 0.035f, c)
        hand(s * 6f, r * 0.86f, d * 0.012f, IosOrange, tail = r * 0.18f)
        drawCircle(IosOrange, d * 0.022f, c)
        drawCircle(face, d * 0.009f, c)
    }
}

/* -------------------------------------------------------------------------- */
/*                                   Battery                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun BatteryWidget(size: WidgetSize, env: WidgetEnv) {
    val dark = env.dark
    val pct = env.batteryPercent.coerceIn(0, 100)
    val ring = if (pct <= 20) IosRed else IosGreen
    Box(Modifier.fillMaxSize().background(bg(dark)).padding(WidgetPadding)) {
        if (size == WidgetSize.SMALL) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                BatteryRing(pct, ring, dark, Modifier.size(82.dp))
                Spacer(Modifier.weight(1f))
                Text("$pct %", style = TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = fg(dark)))
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until 4) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BatteryRing(if (i == 0) pct else -1, ring, dark, Modifier.size(58.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(if (i == 0) "$pct %" else "", style = ts(15.sp, FontWeight.SemiBold, fg(dark)))
                }
            }
        }
    }
}

@Composable
private fun BatteryRing(pct: Int, color: Color, dark: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val track = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
        drawCircle(track, size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        if (pct >= 0) {
            drawArc(color, -90f, 360f * pct / 100f, false, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            phoneGlyph(if (dark) Color.White else Color.Black)
        }
    }
}

private fun DrawScope.phoneGlyph(color: Color) {
    val w = size.width * 0.2f
    val h = size.height * 0.36f
    val tl = Offset(center.x - w / 2, center.y - h / 2)
    drawRoundRect(color, tl, Size(w, h), CornerRadius(w * 0.28f), style = Stroke(size.width * 0.035f))
    drawRoundRect(color, Offset(center.x - w * 0.18f, tl.y + h * 0.08f), Size(w * 0.36f, h * 0.05f), CornerRadius(h * 0.03f))
}

/* -------------------------------------------------------------------------- */
/*                               Siri Suggestions                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun SuggestionsWidget(size: WidgetSize, env: WidgetEnv) {
    val rows = if (size == WidgetSize.LARGE) 4 else 2
    val apps = env.suggestedApps.take(rows * 4)
    Box(
        Modifier
            .fillMaxSize()
            .wallpaperGlass(RoundedCornerShape(WidgetCorner), tint = IosMaterial.widget, rim = 0f)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        // 2 rows in 158 pt need breathing room between a label and the next icon.
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            for (r in 0 until rows) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (c in 0 until 4) {
                    val app = apps.getOrNull(r * 4 + c)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(70.dp).then(
                            if (app != null) Modifier.pointerTap { env.onAppClick(app) } else Modifier
                        )
                    ) {
                        Box(Modifier.size(44.dp)) {
                            app?.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(app?.displayLabel.orEmpty(), style = ts(10.sp, FontWeight.Normal, Color.White, (-0.1).sp).copy(lineHeight = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                 Digital clock · Date · Year · Screen Time · Minimal        */
/* -------------------------------------------------------------------------- */

private fun WidgetEnv.format(pattern: String, millis: Long): String =
    java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(millis))

/** "13. November" / "November 13" / "13 Kasım". */
private fun WidgetEnv.dayMonth(millis: Long): String = format(
    when (language) { AppLanguage.EN -> "MMMM d"; AppLanguage.TR -> "d MMMM"; else -> "d. MMMM" }, millis
)

private fun WidgetEnv.duration(minutes: Int): String =
    if (minutes >= 60) String.format(Locale.ROOT, strings.hoursMinutes, minutes / 60, minutes % 60) else String.format(Locale.ROOT, strings.minutesOnly, minutes)

private fun digits(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Light) = TextStyle(
    fontFamily = com.tms.os.launcher.ui.theme.SfLightFamily, fontWeight = weight, fontSize = size, color = color, letterSpacing = (-1).sp
)

@Composable
private fun DigitalWidget(size: WidgetSize, env: WidgetEnv) {
    val now = rememberNow(60_000)
    val dark = env.dark
    val time = env.format("HH:mm", now)
    val weekday = env.format("EEEE", now).uppercase(env.locale)
    Box(Modifier.fillMaxSize().background(bg(dark)).padding(WidgetPadding)) {
        if (size == WidgetSize.SMALL) {
            Column(Modifier.fillMaxSize()) {
                Text(weekday, style = ts(12.sp, FontWeight.SemiBold, IosRed, 0.3.sp), maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text(time, style = digits(47.sp, fg(dark)), maxLines = 1, softWrap = false)
                Text(env.dayMonth(now), style = ts(14.sp, FontWeight.Medium, fg2(dark)), maxLines = 1)
            }
        } else {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(time, style = digits(78.sp, fg(dark)).copy(letterSpacing = (-2).sp), maxLines = 1, softWrap = false)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(weekday, style = ts(12.sp, FontWeight.SemiBold, IosRed, 0.3.sp), maxLines = 1)
                    Text(env.dayMonth(now), style = ts(17.sp, FontWeight.SemiBold, fg(dark)), maxLines = 1)
                    Spacer(Modifier.height(10.dp))
                    Text("${env.t(env.tempC)}° ${env.city}", style = ts(13.sp, FontWeight.Medium, fg2(dark)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DateWidget(env: WidgetEnv) {
    val now = rememberNow(60_000)
    val dark = env.dark
    val cal = Calendar.getInstance(env.locale).apply { timeInMillis = now }
    Column(Modifier.fillMaxSize().background(bg(dark)).padding(WidgetPadding)) {
        Text(env.format("EEEE", now).uppercase(env.locale), style = ts(12.sp, FontWeight.SemiBold, IosRed, 0.3.sp), maxLines = 1)
        Text(cal.get(Calendar.DAY_OF_MONTH).toString(), style = digits(64.sp, fg(dark)).copy(lineHeight = 68.sp), maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(env.format("LLLL yyyy", now), style = ts(14.sp, FontWeight.Medium, fg(dark)), maxLines = 1)
        Text(String.format(Locale.ROOT, env.strings.week, cal.get(Calendar.WEEK_OF_YEAR)), style = ts(13.sp, FontWeight.Medium, fg2(dark)), maxLines = 1)
    }
}

@Composable
private fun YearWidget(size: WidgetSize, env: WidgetEnv) {
    val now = rememberNow(60_000)
    val dark = env.dark
    val cal = Calendar.getInstance(env.locale).apply { timeInMillis = now }
    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
    val fraction = dayOfYear.toFloat() / daysInYear
    val track = fg(dark).copy(alpha = 0.14f)

    @Composable
    fun Summary(modifier: Modifier) = Column(modifier) {
        Text(cal.get(Calendar.YEAR).toString(), style = ts(13.sp, FontWeight.SemiBold, fg2(dark)), maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text("${(fraction * 100).toInt()} %", style = TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, color = fg(dark)), maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(track)) {
            Box(Modifier.fillMaxWidth(fraction).height(7.dp).clip(CircleShape).background(fg(dark)))
        }
        Spacer(Modifier.height(8.dp))
        Text(String.format(Locale.ROOT, env.strings.daysLeft, daysInYear - dayOfYear), style = ts(13.sp, FontWeight.Medium, fg2(dark)), maxLines = 1)
    }

    Box(Modifier.fillMaxSize().background(bg(dark)).padding(WidgetPadding)) {
        if (size == WidgetSize.SMALL) Summary(Modifier.fillMaxSize())
        else Row(Modifier.fillMaxSize()) {
            Summary(Modifier.width(118.dp).fillMaxSize())
            Spacer(Modifier.width(18.dp))
            // One bar per month, filled as far as the year has come.
            val month = cal.get(Calendar.MONTH)
            val monthFraction = cal.get(Calendar.DAY_OF_MONTH).toFloat() / cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            Row(Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                for (m in 0..11) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val fill = when { m < month -> 1f; m == month -> monthFraction; else -> 0f }
                    Box(Modifier.width(9.dp).height(96.dp).clip(RoundedCornerShape(4.dp)).background(track), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height(96.dp * fill).background(if (m == month) IosOrange else fg(dark)))
                    }
                    Spacer(Modifier.height(5.dp))
                    val initial = Calendar.getInstance(env.locale).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.MONTH, m) }
                        .let { env.format("LLLLL", it.timeInMillis) }
                    Text(initial, style = ts(10.sp, FontWeight.Medium, if (m == month) fg(dark) else fg2(dark)), maxLines = 1)
                }
            }
        }
    }
}

private val UsageColors = listOf(Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF9F0A), Color(0xFFBF5AF2))

@Composable
private fun ScreenTimeWidget(size: WidgetSize, env: WidgetEnv) {
    val dark = env.dark
    val total = env.screenMinutes
    val top = env.screenTop
    Box(Modifier.fillMaxSize().background(bg(dark)).padding(WidgetPadding)) {
        if (total == null) {
            Column(Modifier.fillMaxSize()) {
                Text(env.strings.screenTime, style = ts(13.sp, FontWeight.SemiBold, fg2(dark)), maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text(env.strings.allowAccess, style = ts(15.sp, FontWeight.Medium, fg(dark)))
            }
            return@Box
        }
        val sum = top.sumOf { it.second }.coerceAtLeast(1)

        @Composable
        fun Summary(modifier: Modifier) = Column(modifier) {
            Text(env.strings.screenTime, style = ts(13.sp, FontWeight.SemiBold, fg2(dark)), maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(
                env.duration(total),
                style = TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = if (total >= 60) 24.sp else 30.sp, color = fg(dark)),
                maxLines = 1
            )
            Text(env.strings.today, style = ts(13.sp, FontWeight.Medium, fg2(dark)), maxLines = 1)
            Spacer(Modifier.weight(1f))
            // What the time went into, as one segmented bar.
            Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(fg(dark).copy(alpha = 0.14f))) {
                top.forEachIndexed { i, (_, minutes) ->
                    Box(Modifier.weight(minutes.coerceAtLeast(1).toFloat()).height(8.dp).background(UsageColors[i % UsageColors.size]))
                }
            }
            if (size == WidgetSize.SMALL) {
                Spacer(Modifier.height(7.dp))
                Text(top.firstOrNull()?.first?.displayLabel.orEmpty(), style = ts(13.sp, FontWeight.Medium, fg2(dark)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (size == WidgetSize.SMALL) Summary(Modifier.fillMaxSize())
        else Row(Modifier.fillMaxSize()) {
            Summary(Modifier.width(128.dp).fillMaxSize())
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                top.take(3).forEachIndexed { i, (app, minutes) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp)) {
                            app.iconBitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(app.displayLabel, style = ts(13.sp, FontWeight.SemiBold, fg(dark)), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(env.duration(minutes), style = ts(12.sp, FontWeight.Medium, fg2(dark)), maxLines = 1)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(fg(dark).copy(alpha = 0.12f))) {
                                Box(Modifier.fillMaxWidth((minutes.toFloat() / top.first().second.coerceAtLeast(1)).coerceIn(0.04f, 1f)).height(4.dp).clip(CircleShape).background(UsageColors[i % UsageColors.size]))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shared by the Minimal widget and the minimalist home screen: white type that stays legible on any wallpaper. */
val MinimalShadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.38f), Offset(0f, 1f), 10f)

@Composable
private fun MinimalWidget(size: WidgetSize, env: WidgetEnv) {
    val now = rememberNow(60_000)
    val time = env.format("HH:mm", now)
    val date = "${env.format("EEEE", now)}, ${env.dayMonth(now)}"
    val info = "${env.t(env.tempC)}° ${env.city}  ·  ${env.strings.battery} ${env.batteryPercent.coerceIn(0, 100)} %"
    val white = Color.White
    val soft = Color.White.copy(alpha = 0.78f)
    when (size) {
        WidgetSize.SMALL -> Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.Center) {
            Text(time, style = digits(50.sp, white).copy(shadow = MinimalShadow), maxLines = 1, softWrap = false)
            Text(env.dayMonth(now), style = ts(15.sp, FontWeight.Medium, soft).copy(shadow = MinimalShadow), maxLines = 1)
        }
        WidgetSize.MEDIUM -> Column(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalArrangement = Arrangement.Center) {
            Text(time, style = digits(84.sp, white).copy(shadow = MinimalShadow, letterSpacing = (-2).sp, lineHeight = 88.sp), maxLines = 1, softWrap = false)
            Text(date, style = ts(18.sp, FontWeight.Medium, white).copy(shadow = MinimalShadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(info, style = ts(13.sp, FontWeight.Medium, soft).copy(shadow = MinimalShadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        WidgetSize.LARGE -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date, style = ts(20.sp, FontWeight.Medium, white).copy(shadow = MinimalShadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(time, style = digits(124.sp, white).copy(shadow = MinimalShadow, letterSpacing = (-3).sp, lineHeight = 130.sp), maxLines = 1, softWrap = false)
            Text(info, style = ts(15.sp, FontWeight.Medium, soft).copy(shadow = MinimalShadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Tap that does not steal long-press/drag from the home-screen gesture layer. */
private fun Modifier.pointerTap(onTap: () -> Unit): Modifier =
    this.pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }

/* -------------------------------------------------------------------------- */
/*                                    Notes                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun NotesWidget(family: WidgetSize, env: WidgetEnv) {
    val dark = env.dark
    val text = env.note.trim()
    val title = text.lineSequence().firstOrNull().orEmpty().ifBlank { env.strings.notesEmpty }
    val body = text.lineSequence().drop(1).joinToString(" ").trim()
    val date = remember(env.language) { SimpleDateFormat("dd.MM.yy", env.locale).format(Date()) }
    Box(Modifier.fillMaxSize().background(bg(dark))) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp)) {
                Canvas(Modifier.size(14.dp)) {
                    drawRoundRect(IosYellow, cornerRadius = CornerRadius(size.width * 0.22f))
                    for (i in 1..3) drawLine(Color.White, Offset(size.width * 0.22f, size.height * (0.22f * i + 0.06f)), Offset(size.width * 0.78f, size.height * (0.22f * i + 0.06f)), strokeWidth = size.height * 0.08f, cap = StrokeCap.Round)
                }
                Spacer(Modifier.width(5.dp))
                Text(env.strings.notesTitle, style = ts(13.sp, FontWeight.SemiBold, if (dark) IosYellow else Color(0xFFE0A800)))
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(fg2(dark).copy(alpha = 0.25f)))
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(title, style = ts(15.sp, FontWeight.SemiBold, fg(dark)), maxLines = if (family == WidgetSize.SMALL) 2 else 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(date, style = ts(13.sp, FontWeight.Normal, fg2(dark)))
                if (body.isNotEmpty()) Text(body, style = ts(13.sp, FontWeight.Normal, fg2(dark)), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                 Smart Stack                                */
/* -------------------------------------------------------------------------- */

/**
 * iOS Smart Stack: several same-size widgets on top of each other. Swipe up or
 * down to flip through them; small page dots appear at the right edge while
 * scrolling and fade out shortly after. [onCurrent] reports the visible kind
 * (the home screen shows its name under the stack).
 */
@Composable
fun IosSmartStack(
    kinds: List<WidgetKind>,
    size: WidgetSize,
    env: WidgetEnv,
    modifier: Modifier = Modifier,
    onCurrent: (WidgetKind) -> Unit = {}
) {
    val pager = androidx.compose.foundation.pager.rememberPagerState { kinds.size }
    var dotsVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(pager.currentPage) { kinds.getOrNull(pager.currentPage)?.let(onCurrent) }
    LaunchedEffect(pager.isScrollInProgress) {
        if (pager.isScrollInProgress) dotsVisible = true
        else { delay(1400); dotsVisible = false }
    }
    val dotsAlpha by androidx.compose.animation.core.animateFloatAsState(if (dotsVisible) 1f else 0f, androidx.compose.animation.core.tween(220), label = "dots")
    Box(modifier) {
        androidx.compose.foundation.pager.VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)),
            pageSpacing = 12.dp
        ) { i ->
            IosWidget(kinds[i], size, env, Modifier.fillMaxSize())
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 11.dp)
                .alpha(dotsAlpha)
        ) {
            repeat(kinds.size) { i ->
                Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.35f)))
            }
        }
    }
}

