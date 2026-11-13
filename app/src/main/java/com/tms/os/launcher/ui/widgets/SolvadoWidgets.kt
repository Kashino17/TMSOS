package com.tms.os.launcher.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.R
import com.tms.os.launcher.model.SolvadoFormat
import com.tms.os.launcher.model.SolvadoMetric
import com.tms.os.launcher.model.SolvadoShop
import com.tms.os.launcher.model.SolvadoWidgetConfig
import com.tms.os.launcher.model.WidgetKind
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.SfDisplayFamily
import com.tms.os.launcher.ui.theme.SfFamily
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ========================================================================== */
/*       TMS Solvado widgets, in the colours of the Solvado dashboard         */
/* ========================================================================== */

/** The dark theme of the Solvado web app (zinc surfaces, violet primary, the orange of the mark). */
internal object SolvadoColors {
    val surfaceTop = Color(0xFF1B1B20)
    val surfaceBottom = Color(0xFF0C0C0F)
    val ink = Color(0xFFFAFAFA)
    val muted = Color(0xFFA1A1AA)
    val line = Color(0xFF27272A)
    val violet = Color(0xFF8B5CF6)
    val orange = Color(0xFFF26B21)
    val good = Color(0xFF34D399)
    val bad = Color(0xFFF87171)
}

private fun label(size: Float = 11.5f) =
    TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.Medium, fontSize = size.sp, color = SolvadoColors.muted)

private fun value(size: Int, color: Color = SolvadoColors.ink) =
    TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = size.sp, color = color, letterSpacing = (-0.4).sp)

internal fun rangeLabel(range: String, s: HomeStrings) = when (range) { "7d" -> s.last7; "30d" -> s.last30; else -> s.today }

/** How many figures each size holds. */
internal fun WidgetSize.solvadoCapacity(): Int = when (this) { WidgetSize.SMALL -> 3; WidgetSize.MEDIUM -> 6; WidgetSize.LARGE -> 10 }

/** "1.240 €", "1,2 Mio. €", "3,4 %", "4,1×", "12.403" – or "–" when the figure was not measured. */
internal fun formatSolvado(value: Double?, format: SolvadoFormat, currency: String): String {
    if (value == null) return "–"
    val symbols = DecimalFormatSymbols(Locale.GERMANY)
    return when (format) {
        SolvadoFormat.MONEY -> {
            val sign = when (currency.uppercase(Locale.ROOT)) { "EUR" -> "€"; "USD" -> "$"; "GBP" -> "£"; else -> currency.uppercase(Locale.ROOT) }
            val magnitude = kotlin.math.abs(value)
            val number = when {
                magnitude >= 1_000_000 -> DecimalFormat("0.##", symbols).format(value / 1_000_000) + " Mio."
                magnitude >= 100 || value % 1.0 == 0.0 -> DecimalFormat("#,##0", symbols).format(value)
                else -> DecimalFormat("#,##0.00", symbols).format(value)
            }
            "$number $sign"
        }
        SolvadoFormat.COUNT -> DecimalFormat("#,##0", symbols).format(value)
        SolvadoFormat.PERCENT -> "${DecimalFormat("0.#", symbols).format(value)} %"
        SolvadoFormat.RATIO -> "${DecimalFormat("0.0", symbols).format(value)}×"
    }
}

private class Figure(val label: String, val value: String, val tone: Color)

/** The Solvado mark as a small app-style tile. */
@Composable
internal fun SolvadoMark(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.solvado_mark), null, contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.24f))
    )
}

/** A shop's logo tile; until it is loaded (or if it has none) its initial on violet. */
@Composable
internal fun ShopTile(shop: SolvadoShop?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.24f)
    val icon = shop?.icon
    if (icon != null) Image(icon, null, contentScale = ContentScale.Crop, modifier = modifier.size(size).clip(shape))
    else Box(modifier.size(size).clip(shape).background(SolvadoColors.violet), contentAlignment = Alignment.Center) {
        Text(
            name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "S" },
            style = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.5f).sp, color = Color.White)
        )
    }
}

/** Circular arrow; turns while figures are being fetched. Tapping it fetches, nothing else does it on its own. */
@Composable
private fun RefreshButton(busy: Boolean, onClick: () -> Unit) {
    val angle = if (busy) {
        val spin by rememberInfiniteTransition(label = "refresh").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle")
        spin
    } else 0f
    Box(
        Modifier
            .size(30.dp)
            // Its own tap target: the rest of the widget opens the settings.
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(15.dp).graphicsLayer { rotationZ = angle }) {
            val stroke = size.minDimension * 0.13f
            val inset = stroke / 2f
            drawArc(
                SolvadoColors.muted, startAngle = -50f, sweepAngle = 290f, useCenter = false,
                topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // Arrow head at the open end of the arc.
            rotate(-50f) {
                val tip = Offset(size.width - inset, size.height / 2f)
                val head = size.minDimension * 0.30f
                drawPath(Path().apply {
                    moveTo(tip.x - head * 0.75f, tip.y - head * 0.05f)
                    lineTo(tip.x + head * 0.75f, tip.y - head * 0.05f)
                    lineTo(tip.x, tip.y + head * 0.95f)
                    close()
                }, SolvadoColors.muted)
            }
        }
    }
}

/** One widget for both kinds: who and when on top, then as many figures as the size holds. */
@Composable
fun SolvadoWidget(kind: WidgetKind, size: WidgetSize, env: WidgetEnv, configRaw: String) {
    val admin = kind == WidgetKind.SOLVADO_ADMIN
    val state = env.solvado
    val config = remember(configRaw, admin) { SolvadoWidgetConfig.decode(configRaw, admin) }
    val s = env.strings
    val stats = if (admin) null else state.shopStats["${config.shopId}|${config.range}"]
    val shop = state.shops.firstOrNull { it.id == config.shopId }
    val small = size == WidgetSize.SMALL
    val title = if (admin) (if (small) "Solvado" else "TMS Solvado") else shop?.name ?: stats?.name ?: "TMS Solvado"
    val time = if (state.updatedAt > 0) SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(state.updatedAt)) else null
    val subtitle = listOfNotNull(if (admin && !small) "Admin" else null, rangeLabel(config.range, s), time?.takeIf { !small }).joinToString(" · ")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SolvadoColors.surfaceTop, SolvadoColors.surfaceBottom)))
            // The violet of the dashboard, as a soft light from the top corner.
            .background(Brush.radialGradient(listOf(SolvadoColors.violet.copy(alpha = 0.20f), Color.Transparent), center = Offset(Float.POSITIVE_INFINITY, 0f), radius = 520f))
    ) {
        Column(Modifier.fillMaxSize().padding(start = 14.dp, end = if (small) 8.dp else 10.dp, top = 12.dp, bottom = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    if (admin || (shop == null && stats == null)) SolvadoMark(if (small) 26.dp else 30.dp)
                    else {
                        ShopTile(shop, title, if (small) 26.dp else 30.dp)
                        // Whose numbers these are: the Solvado mark rides on the shop's logo.
                        SolvadoMark(13.dp, Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp))
                    }
                }
                Spacer(Modifier.width(if (small) 9.dp else 11.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.SemiBold, fontSize = (if (small) 13 else 14).sp, color = SolvadoColors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = label(11f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (state.connected) RefreshButton(state.busy, env.onSolvadoRefresh)
            }

            // Nothing to show yet: say what a tap does instead of drawing empty figures.
            val hint = when {
                !state.connected -> s.solvadoTapToConnect
                admin && !state.isAdmin -> s.solvadoTapToConnect
                !admin && config.shopId.isEmpty() -> if (state.shops.isEmpty()) s.noShops else s.solvadoTapToChoose
                else -> null
            }
            if (hint != null) {
                Spacer(Modifier.weight(1f))
                Text(hint, style = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = SolvadoColors.ink), modifier = Modifier.padding(end = 6.dp))
                return@Column
            }

            val currency = if (admin) "EUR" else stats?.currency ?: shop?.currency ?: "EUR"
            val incomplete = !admin && (stats?.adSpendDaysMissing ?: 0) > 0
            val figures = config.metrics.take(size.solvadoCapacity()).map { m ->
                val v = if (admin) state.admin?.get(if (m.ranged) "${m.id}:${config.range}" else m.id) else stats?.values?.get(m.id)
                val starred = incomplete && (m == SolvadoMetric.AD_SPEND || m == SolvadoMetric.PROFIT || m == SolvadoMetric.ROAS || m == SolvadoMetric.MARGIN)
                val tone = if (m == SolvadoMetric.PROFIT && v != null) (if (v < 0) SolvadoColors.bad else SolvadoColors.good) else SolvadoColors.ink
                Figure(m.label(env.language), formatSolvado(v, m.format, currency) + if (starred && v != null) " *" else "", tone)
            }

            when (size) {
                WidgetSize.SMALL -> {
                    Spacer(Modifier.weight(1f))
                    figures.getOrNull(0)?.let {
                        Text(it.label, style = label(), maxLines = 1)
                        Text(it.value, style = value(if (figures.size > 2) 23 else 26, it.tone), maxLines = 1, softWrap = false)
                    }
                    figures.drop(1).forEach {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                            Text(it.label, style = label(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(it.value, style = value(13, it.tone), maxLines = 1)
                        }
                    }
                }
                WidgetSize.MEDIUM -> {
                    Spacer(Modifier.height(9.dp))
                    // Up to four figures stand in two columns, five and six in three.
                    val columns = if (figures.size > 4) 3 else 2
                    FigureGrid(figures, columns, valueSize = if (columns == 3) 18 else 21, spread = true, Modifier.weight(1f))
                }
                WidgetSize.LARGE -> {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(SolvadoColors.line))
                    Spacer(Modifier.height(14.dp))
                    FigureGrid(figures, 2, valueSize = 24, spread = figures.size > 6, Modifier.weight(1f))
                    if (incomplete) Text(s.incomplete, style = label(10.5f))
                }
            }
        }
    }
}

/** Label over value in [columns] columns; rows spread over the height or stay together at the top. */
@Composable
private fun FigureGrid(figures: List<Figure>, columns: Int, valueSize: Int, spread: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = if (spread) Arrangement.SpaceBetween else Arrangement.spacedBy(20.dp)) {
        figures.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { f ->
                    Column(Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(f.label, style = label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(f.value, style = value(valueSize, f.tone), maxLines = 1, softWrap = false)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Marks free cells in edit mode; on the normal home screen a spacer shows nothing at all. */
@Composable
internal fun SpacerWidget(env: WidgetEnv) {
    if (!env.editing) return
    Box(
        Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(19.dp)).background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                Color.White.copy(alpha = 0.45f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(19.dp.toPx()),
                style = Stroke(1.4.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx())))
            )
        }
        Text(env.strings.widgetName(WidgetKind.SPACER), style = label(13f).copy(color = Color.White.copy(alpha = 0.8f)))
    }
}
