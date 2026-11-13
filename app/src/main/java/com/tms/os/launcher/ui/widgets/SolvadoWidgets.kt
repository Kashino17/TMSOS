package com.tms.os.launcher.ui.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.model.AppLanguage
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.SolvadoFormat
import com.tms.os.launcher.model.SolvadoMetric
import com.tms.os.launcher.model.SolvadoState
import com.tms.os.launcher.model.SolvadoWidgetConfig
import com.tms.os.launcher.model.WidgetKind
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosText
import com.tms.os.launcher.ui.theme.SfDisplayFamily
import com.tms.os.launcher.ui.theme.SfFamily
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ========================================================================== */
/*                           TMS Solvado widgets                              */
/* ========================================================================== */

private val Plate = Color(0xF01C1C1E)
private val Ink = Color.White
private val Soft = Color(0x99EBEBF5)
private val Brand = Color(0xFF5E5CE6)
private val Good = Color(0xFF30D158)
private val Bad = Color(0xFFFF453A)

private fun text(size: Int, weight: FontWeight, color: Color) =
    TextStyle(fontFamily = SfFamily, fontWeight = weight, fontSize = size.sp, color = color)

private fun number(size: Int, color: Color = Ink) =
    TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = size.sp, color = color, letterSpacing = (-0.4).sp)

private fun rangeLabel(range: String, s: HomeStrings) = when (range) { "7d" -> s.last7; "30d" -> s.last30; else -> s.today }

/** "1.240 €", "3,4 %", "4,1×", "12.403" – or "–" when the figure was not measured. */
internal fun formatSolvado(value: Double?, format: SolvadoFormat, currency: String): String {
    if (value == null) return "–"
    val symbols = DecimalFormatSymbols(Locale.GERMANY)
    return when (format) {
        SolvadoFormat.MONEY -> {
            val pattern = if (kotlin.math.abs(value) >= 100 || value % 1.0 == 0.0) "#,##0" else "#,##0.00"
            val sign = when (currency.uppercase(Locale.ROOT)) { "EUR" -> "€"; "USD" -> "$"; "GBP" -> "£"; else -> currency.uppercase(Locale.ROOT) }
            "${DecimalFormat(pattern, symbols).format(value)} $sign"
        }
        SolvadoFormat.COUNT -> DecimalFormat("#,##0", symbols).format(value)
        SolvadoFormat.PERCENT -> "${DecimalFormat("0.#", symbols).format(value)} %"
        SolvadoFormat.RATIO -> "${DecimalFormat("0.0", symbols).format(value)}×"
    }
}

private class Figure(val label: String, val value: String, val tone: Color)

/** One widget for both kinds: header (what, which period), then as many figures as the size holds. */
@Composable
fun SolvadoWidget(kind: WidgetKind, size: WidgetSize, env: WidgetEnv, configRaw: String) {
    val admin = kind == WidgetKind.SOLVADO_ADMIN
    val state = env.solvado
    val config = remember(configRaw, admin) { SolvadoWidgetConfig.decode(configRaw, admin) }
    val s = env.strings
    val stats = if (admin) null else state.shopStats["${config.shopId}|${config.range}"]
    val shopName = state.shops.firstOrNull { it.id == config.shopId }?.name ?: stats?.name
    val title = if (admin) "Solvado" else shopName ?: "Solvado"

    Column(Modifier.fillMaxSize().background(Plate).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(Brand), contentAlignment = Alignment.Center) {
                Text("S", style = text(11, FontWeight.Bold, Ink))
            }
            Spacer(Modifier.width(7.dp))
            Text(title, style = text(13, FontWeight.SemiBold, Ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (state.connected && size != WidgetSize.SMALL) Text(rangeLabel(config.range, s), style = text(12, FontWeight.Medium, Soft), maxLines = 1)
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
            Text(hint, style = text(15, FontWeight.Medium, Ink))
            return@Column
        }

        val currency = if (admin) "EUR" else stats?.currency ?: state.shops.firstOrNull { it.id == config.shopId }?.currency ?: "EUR"
        val incomplete = !admin && (stats?.adSpendDaysMissing ?: 0) > 0
        val figures = config.metrics.map { m ->
            val value = if (admin) state.admin?.get(if (m.ranged) "${m.id}:${config.range}" else m.id) else stats?.values?.get(m.id)
            val starred = incomplete && (m == SolvadoMetric.AD_SPEND || m == SolvadoMetric.PROFIT || m == SolvadoMetric.ROAS || m == SolvadoMetric.MARGIN)
            val tone = if (m == SolvadoMetric.PROFIT && value != null) (if (value < 0) Bad else Good) else Ink
            Figure(m.label(env.language), formatSolvado(value, m.format, currency) + if (starred && value != null) " *" else "", tone)
        }

        when (size) {
            WidgetSize.SMALL -> {
                Spacer(Modifier.weight(1f))
                figures.getOrNull(0)?.let {
                    Text(it.label, style = text(12, FontWeight.Medium, Soft), maxLines = 1)
                    Text(it.value, style = number(27, it.tone), maxLines = 1, softWrap = false)
                }
                figures.getOrNull(1)?.let {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(it.label, style = text(12, FontWeight.Medium, Soft), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(it.value, style = text(13, FontWeight.SemiBold, it.tone), maxLines = 1)
                    }
                }
            }
            WidgetSize.MEDIUM -> {
                Spacer(Modifier.height(10.dp))
                FigureGrid(figures.take(4), valueSize = 22, spread = true, Modifier.weight(1f))
            }
            WidgetSize.LARGE -> {
                Spacer(Modifier.height(16.dp))
                // Few figures stay together at the top instead of drifting to the corners.
                FigureGrid(figures.take(8), valueSize = 24, spread = figures.size > 6, Modifier.weight(1f))
                Row {
                    Text(
                        if (state.updatedAt > 0) String.format(Locale.ROOT, s.updatedAt, SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(state.updatedAt))) else "",
                        style = text(11, FontWeight.Medium, Soft), modifier = Modifier.weight(1f)
                    )
                    if (incomplete) Text(s.incomplete, style = text(11, FontWeight.Medium, Soft))
                }
            }
        }
    }
}

/** Two columns of label-over-value, rows spread over the height. */
@Composable
private fun FigureGrid(figures: List<Figure>, valueSize: Int, spread: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = if (spread) Arrangement.SpaceBetween else Arrangement.spacedBy(22.dp)) {
        figures.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { f ->
                    Column(Modifier.weight(1f)) {
                        Text(f.label, style = text(12, FontWeight.Medium, Soft), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(f.value, style = number(valueSize, f.tone), maxLines = 1, softWrap = false)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* ========================================================================== */
/*                    Sign-in and "Edit Widget" in one sheet                  */
/* ========================================================================== */

private val Blue = Color(0xFF0A84FF)
private val Group = Color(0xFF2C2C2E)

/**
 * Opens when a Solvado widget is tapped. Signed out it is the sign-in form;
 * signed in it is what iOS calls "Edit Widget": the shop, the period and the
 * figures this one widget shows.
 */
@Composable
fun SolvadoWidgetSheet(
    widget: HomeGridItem.WidgetItem?,
    state: SolvadoState,
    strings: HomeStrings,
    language: AppLanguage,
    server: String,
    onLogin: (email: String, password: String, server: String?) -> Unit,
    onLogout: () -> Unit,
    onConfig: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(widget != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(140)), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0x80000000)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        // Keep the last widget while the sheet slides out.
        var last by remember { mutableStateOf(widget) }
        if (widget != null) last = widget
        AnimatedVisibility(
            widget != null,
            enter = slideInVertically(spring(dampingRatio = 0.88f, stiffness = 340f)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val item = last ?: return@AnimatedVisibility
            BackHandler(onBack = onDismiss)
            val admin = item.kind == WidgetKind.SOLVADO_ADMIN
            val config = remember(item.config, admin) { SolvadoWidgetConfig.decode(item.config, admin) }
            Column(
                Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.24f)))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 6.dp)) {
                    Text(if (state.connected) strings.editWidget else strings.solvadoConnect, style = IosText.title2.copy(color = Ink), modifier = Modifier.weight(1f))
                    Text(
                        strings.done, style = IosText.body.copy(color = Blue, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (!state.connected) SignIn(state, strings, server, onLogin)
                    else {
                        // Account
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Group).padding(horizontal = 16.dp, vertical = 13.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("TMS Solvado", style = IosText.footnote.copy(color = Soft))
                                Text(state.email.ifEmpty { "–" }, style = IosText.body.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(strings.signOut, style = IosText.body.copy(color = Bad), modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onLogout))
                        }
                        state.error?.let { Text(it, style = IosText.footnote.copy(color = Bad), modifier = Modifier.padding(start = 16.dp, top = 8.dp)) }

                        if (!admin) {
                            Header(strings.shop)
                            if (state.shops.isEmpty()) Text(strings.noShops, style = IosText.body.copy(color = Soft), modifier = Modifier.padding(horizontal = 4.dp))
                            Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Group)) {
                                state.shops.forEachIndexed { i, shop ->
                                    if (i > 0) Divider()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { onConfig(config.copy(shopId = shop.id).encode()) }.padding(horizontal = 16.dp, vertical = 13.dp)
                                    ) {
                                        Text(shop.name, style = IosText.body.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        if (shop.id == config.shopId) Text("✓", style = IosText.body.copy(color = Blue, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }

                        Header(strings.period)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x3D767680)).padding(2.dp)) {
                            SolvadoWidgetConfig.ranges.forEach { r ->
                                val selected = r == config.range
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFF636366) else Color.Transparent)
                                        .clickable(remember { MutableInteractionSource() }, null) { onConfig(config.copy(range = r).encode()) }
                                ) { Text(rangeLabel(r, strings), style = IosText.subheadline.copy(color = Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)) }
                            }
                        }

                        Header(strings.figures)
                        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Group)) {
                            // Chosen figures first, in the order they appear on the widget.
                            val all = config.metrics + SolvadoMetric.of(admin).filterNot { it in config.metrics }
                            all.forEachIndexed { i, m ->
                                if (i > 0) Divider()
                                val index = config.metrics.indexOf(m)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) {
                                        val next = if (index >= 0) config.metrics - m else config.metrics + m
                                        if (next.isNotEmpty()) onConfig(config.copy(metrics = next).encode())
                                    }.padding(horizontal = 16.dp, vertical = 11.dp)
                                ) {
                                    Text(m.label(language), style = IosText.body.copy(color = Ink), modifier = Modifier.weight(1f))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(26.dp).clip(CircleShape)
                                            .then(if (index >= 0) Modifier.background(Blue) else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                                    ) { if (index >= 0) Text((index + 1).toString(), style = IosText.footnote.copy(color = Ink, fontWeight = FontWeight.Bold)) }
                                }
                            }
                        }
                        Text(strings.figuresHint, style = IosText.footnote.copy(color = Soft), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignIn(state: SolvadoState, strings: HomeStrings, server: String, onLogin: (String, String, String?) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var host by remember { mutableStateOf(server) }
    var showServer by remember { mutableStateOf(false) }
    Text(strings.solvadoConnectHint, style = IosText.footnote.copy(color = Soft), modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
    Spacer(Modifier.height(8.dp))
    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Group)) {
        Field(strings.email, email, { email = it }, KeyboardType.Email)
        Divider()
        Field(strings.password, password, { password = it }, KeyboardType.Password, secret = true)
        if (showServer) { Divider(); Field(strings.server, host, { host = it }, KeyboardType.Uri) }
    }
    state.error?.let { Text(it, style = IosText.footnote.copy(color = Bad), modifier = Modifier.padding(start = 16.dp, top = 8.dp)) }
    Spacer(Modifier.height(16.dp))
    val ready = email.isNotBlank() && password.isNotEmpty() && !state.busy
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(if (ready) Blue else Blue.copy(alpha = 0.4f))
            .clickable(enabled = ready) { onLogin(email, password, host.takeIf { showServer }) }
    ) { Text(if (state.busy) "…" else strings.signIn, style = IosText.headline.copy(color = Ink)) }
    Spacer(Modifier.height(14.dp))
    if (!showServer) Text(
        strings.server, style = IosText.footnote.copy(color = Soft),
        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { showServer = true }.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, type: KeyboardType, secret: Boolean = false) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        if (value.isEmpty()) Text(label, style = IosText.body.copy(color = Soft))
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = IosText.body.copy(color = Ink), cursorBrush = SolidColor(Blue),
            keyboardOptions = KeyboardOptions(keyboardType = type, autoCorrectEnabled = false),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Header(title: String) {
    Text(title.uppercase(Locale.getDefault()), style = IosText.footnote.copy(color = Soft, fontWeight = FontWeight.Medium), modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 9.dp))
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.12f)))
}
