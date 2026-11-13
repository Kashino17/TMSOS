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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tms.os.launcher.model.HomeGridItem
import com.tms.os.launcher.model.LauncherSettings
import com.tms.os.launcher.model.SolvadoMetric
import com.tms.os.launcher.model.SolvadoState
import com.tms.os.launcher.model.SolvadoWidgetConfig
import com.tms.os.launcher.model.WidgetKind
import com.tms.os.launcher.model.WidgetSize
import com.tms.os.launcher.ui.theme.HomeStrings
import com.tms.os.launcher.ui.theme.IosText
import java.util.Locale

private val Ink = Color.White
private val Soft = Color(0x99EBEBF5)
private val Blue = Color(0xFF0A84FF)
private val Red = Color(0xFFFF453A)
private val Group = Color(0xFF2C2C2E)

/** Everything the "Edit Widget" sheet can ask the launcher to do. */
class WidgetEditActions(
    val onConfig: (String) -> Unit,
    val onSize: (WidgetSize) -> Unit,
    val onSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
    val onSolvadoLogin: (email: String, password: String, server: String?) -> Unit,
    val onSolvadoLogout: () -> Unit,
    /** Weather for the phone's position; the launcher asks for the permission first. */
    val onUseLocation: () -> Unit,
    val onCity: (String) -> Unit,
    val onDismiss: () -> Unit
)

/**
 * "Edit Widget", as on iOS: opens when a configurable widget is tapped and
 * holds the settings of that one widget — its size, and per kind:
 *  - Solvado: shop (searchable), period, the figures and their order, how it refreshes;
 *  - Weather: the place (the phone's position on request, or typed in), the unit;
 *  - Calendar: today's view or the month.
 */
@Composable
fun WidgetEditSheet(
    widget: HomeGridItem.WidgetItem?,
    solvado: SolvadoState,
    settings: LauncherSettings,
    strings: HomeStrings,
    solvadoServer: String,
    locating: Boolean,
    locateFailed: Boolean,
    actions: WidgetEditActions
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(widget != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(140)), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0x80000000)).clickable(remember { MutableInteractionSource() }, null, onClick = actions.onDismiss))
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
            val isSolvado = item.kind == WidgetKind.SOLVADO_SHOP || item.kind == WidgetKind.SOLVADO_ADMIN
            // The shop list is a page of its own inside the sheet: with many shops it needs the room and a search field.
            var pickingShop by remember(item.widgetId) { mutableStateOf(false) }
            BackHandler { if (pickingShop) pickingShop = false else actions.onDismiss() }
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
                    if (pickingShop) Text(
                        "‹", style = IosText.title2.copy(color = Blue),
                        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { pickingShop = false }.padding(end = 12.dp)
                    )
                    Text(
                        when {
                            pickingShop -> strings.shop
                            isSolvado && !solvado.connected -> strings.solvadoConnect
                            else -> strings.widgetName(item.kind)
                        },
                        style = IosText.title2.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text(
                        strings.done, style = IosText.body.copy(color = Blue, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clip(CircleShape).clickable(onClick = actions.onDismiss).padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                if (pickingShop) {
                    val admin = false
                    val config = remember(item.config) { SolvadoWidgetConfig.decode(item.config, admin) }
                    ShopPicker(solvado, config.shopId, strings) { id ->
                        actions.onConfig(config.copy(shopId = id).encode())
                        pickingShop = false
                    }
                    return@Column
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (isSolvado && !solvado.connected) {
                        SignIn(solvado, strings, solvadoServer, actions.onSolvadoLogin)
                        return@Column
                    }
                    when (item.kind) {
                        WidgetKind.SOLVADO_SHOP, WidgetKind.SOLVADO_ADMIN -> SolvadoSettings(item, solvado, settings, strings, actions) { pickingShop = true }
                        WidgetKind.WEATHER -> WeatherSettings(item, settings, strings, locating, locateFailed, actions)
                        WidgetKind.CALENDAR -> CalendarSettings(item, strings, actions)
                        else -> SizeRow(item, strings, actions)
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Solvado */

@Composable
private fun SolvadoSettings(
    item: HomeGridItem.WidgetItem,
    state: SolvadoState,
    settings: LauncherSettings,
    strings: HomeStrings,
    actions: WidgetEditActions,
    onPickShop: () -> Unit
) {
    val admin = item.kind == WidgetKind.SOLVADO_ADMIN
    val config = remember(item.config, admin) { SolvadoWidgetConfig.decode(item.config, admin) }
    val language = settings.appLanguage
    state.error?.let { Text(it, style = IosText.footnote.copy(color = Red), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) }

    if (!admin) {
        // One row for the shop, however many there are; the list opens on its own page.
        val shop = state.shops.firstOrNull { it.id == config.shopId }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Group).clickable(onClick = onPickShop).padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            ShopTile(shop, shop?.name ?: "?", 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(strings.shop, style = IosText.footnote.copy(color = Soft))
                Text(shop?.name ?: strings.solvadoTapToChoose, style = IosText.body.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("›", style = IosText.title2.copy(color = Soft))
        }
    }

    SizeRow(item, strings, actions)

    Header(strings.period)
    Segments(SolvadoWidgetConfig.ranges.map { it to rangeLabel(it, strings) }, config.range) { actions.onConfig(config.copy(range = it).encode()) }

    Header(strings.figures)
    val capacity = item.size.solvadoCapacity()
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
                    if (next.isNotEmpty()) actions.onConfig(config.copy(metrics = next).encode())
                }.padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text(m.label(language), style = IosText.body.copy(color = Ink), modifier = Modifier.weight(1f))
                // A figure that does not fit this size any more stays chosen, but is marked as hidden.
                val shown = index in 0 until capacity
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(26.dp).clip(CircleShape).then(
                        when {
                            shown -> Modifier.background(Blue)
                            index >= 0 -> Modifier.background(Color(0xFF636366))
                            else -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        }
                    )
                ) { if (index >= 0) Text((index + 1).toString(), style = IosText.footnote.copy(color = Ink, fontWeight = FontWeight.Bold)) }
            }
        }
    }
    Text(
        String.format(Locale.ROOT, strings.figuresFit, capacity) + if (config.metrics.size > capacity) " " + strings.figuresTooMany else "",
        style = IosText.footnote.copy(color = Soft), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
    )

    Header(strings.refreshing)
    ToggleRow(strings.autoRefresh, settings.solvadoAutoRefresh) { actions.onSettings { it.copy(solvadoAutoRefresh = !it.solvadoAutoRefresh) } }
    Text(strings.autoRefreshHint, style = IosText.footnote.copy(color = Soft), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))

    Header(strings.account)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Group).padding(horizontal = 14.dp, vertical = 11.dp)) {
        SolvadoMark(34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("TMS Solvado", style = IosText.footnote.copy(color = Soft))
            Text(state.email.ifEmpty { "–" }, style = IosText.body.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(strings.signOut, style = IosText.body.copy(color = Red), modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = actions.onSolvadoLogout))
    }
}

/** All shops with their logos, filtered as you type. */
@Composable
private fun ShopPicker(state: SolvadoState, selected: String, strings: HomeStrings, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val shops = remember(state.shops, query) { state.shops.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) } }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0x3D767680)).padding(horizontal = 12.dp)
    ) {
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(strings.searchShops, style = IosText.body.copy(color = Soft))
            BasicTextField(query, { query = it }, singleLine = true, textStyle = IosText.body.copy(color = Ink), cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxWidth())
        }
        if (query.isNotEmpty()) Text("✕", style = IosText.footnote.copy(color = Soft), modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { query = "" }.padding(6.dp))
    }
    if (state.shops.isEmpty()) Text(strings.noShops, style = IosText.body.copy(color = Soft), modifier = Modifier.padding(24.dp))
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp)) {
        items(shops, key = { it.id }) { shop ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) { onPick(shop.id) }.padding(vertical = 8.dp)
            ) {
                ShopTile(shop, shop.name, 40.dp)
                Spacer(Modifier.width(13.dp))
                Text(shop.name, style = IosText.body.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (shop.id == selected) Text("✓", style = IosText.body.copy(color = Blue, fontWeight = FontWeight.Bold))
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
        SolvadoMark(40.dp)
        Spacer(Modifier.width(12.dp))
        Text(strings.solvadoConnectHint, style = IosText.footnote.copy(color = Soft))
    }
    Spacer(Modifier.height(8.dp))
    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Group)) {
        Field(strings.email, email, { email = it }, KeyboardType.Email)
        Divider()
        Field(strings.password, password, { password = it }, KeyboardType.Password, secret = true)
        if (showServer) { Divider(); Field(strings.server, host, { host = it }, KeyboardType.Uri) }
    }
    state.error?.let { Text(it, style = IosText.footnote.copy(color = Red), modifier = Modifier.padding(start = 16.dp, top = 8.dp)) }
    Spacer(Modifier.height(16.dp))
    val ready = email.isNotBlank() && password.isNotEmpty() && !state.busy
    Button(if (state.busy) "…" else strings.signIn, ready) { onLogin(email, password, host.takeIf { showServer }) }
    Spacer(Modifier.height(14.dp))
    if (!showServer) Text(
        strings.server, style = IosText.footnote.copy(color = Soft),
        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { showServer = true }.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

/* ------------------------------------------------------------------ Weather */

@Composable
private fun WeatherSettings(
    item: HomeGridItem.WidgetItem,
    settings: LauncherSettings,
    strings: HomeStrings,
    locating: Boolean,
    locateFailed: Boolean,
    actions: WidgetEditActions
) {
    SizeRow(item, strings, actions)

    Header(strings.place)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Group).padding(horizontal = 16.dp, vertical = 13.dp)) {
        Text(strings.currentPlace, style = IosText.body.copy(color = Soft), modifier = Modifier.weight(1f))
        Text(settings.weatherCity, style = IosText.body.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(10.dp))
    Button(if (locating) strings.locating else strings.useMyLocation, !locating, onClick = actions.onUseLocation)
    Text(
        if (locateFailed) strings.locateFailed else strings.locationHint,
        style = IosText.footnote.copy(color = if (locateFailed) Red else Soft), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
    )

    Spacer(Modifier.height(14.dp))
    var city by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Group).padding(start = 16.dp, end = 8.dp)) {
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            if (city.isEmpty()) Text(strings.typePlace, style = IosText.body.copy(color = Soft))
            BasicTextField(
                city, { city = it }, singleLine = true, textStyle = IosText.body.copy(color = Ink), cursorBrush = SolidColor(Blue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (city.isNotBlank()) { actions.onCity(city); city = "" } }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            strings.apply, style = IosText.body.copy(color = if (city.isBlank()) Soft else Blue, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { if (city.isNotBlank()) { actions.onCity(city); city = "" } }.padding(10.dp)
        )
    }

    Header(strings.unit)
    Segments(listOf("c" to "°C", "f" to "°F"), if (settings.useFahrenheit) "f" else "c") { u -> actions.onSettings { it.copy(useFahrenheit = u == "f") } }
}

/* ----------------------------------------------------------------- Calendar */

@Composable
private fun CalendarSettings(item: HomeGridItem.WidgetItem, strings: HomeStrings, actions: WidgetEditActions) {
    SizeRow(item, strings, actions)
    Header(strings.display)
    Segments(listOf("" to strings.viewDay, "view=month" to strings.viewMonth), if (item.config == "view=month") "view=month" else "") { actions.onConfig(it) }
    Text(strings.calendarHint, style = IosText.footnote.copy(color = Soft), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
}

/* ------------------------------------------------------------------- pieces */

@Composable
private fun SizeRow(item: HomeGridItem.WidgetItem, strings: HomeStrings, actions: WidgetEditActions) {
    if (item.kind.sizes.size < 2) return
    Header(strings.size)
    Segments(
        item.kind.sizes.map { it.id to when (it) { WidgetSize.SMALL -> strings.sizeSmall; WidgetSize.MEDIUM -> strings.sizeMedium; WidgetSize.LARGE -> strings.sizeLarge } },
        item.size.id
    ) { id -> actions.onSize(WidgetSize.fromId(id)) }
}

@Composable
private fun Segments(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x3D767680)).padding(2.dp)) {
        options.forEach { (id, text) ->
            val on = id == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (on) Color(0xFF636366) else Color.Transparent)
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(id) }
            ) { Text(text, style = IosText.subheadline.copy(color = Ink, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal), maxLines = 1) }
        }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Group).clickable(remember { MutableInteractionSource() }, null, onClick = onToggle).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, style = IosText.body.copy(color = Ink), modifier = Modifier.weight(1f))
        val thumb by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.75f, stiffness = 500f), label = "thumb")
        val on by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "track")
        Box(Modifier.size(51.dp, 31.dp).clip(CircleShape).background(Color(0xFF39393D)).background(Color(0xFF30D158).copy(alpha = on))) {
            Box(Modifier.offset(x = thumb, y = 2.dp).size(27.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun Button(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (enabled) Blue else Blue.copy(alpha = 0.4f)).clickable(enabled = enabled, onClick = onClick)
    ) { Text(text, style = IosText.headline.copy(color = Ink)) }
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
