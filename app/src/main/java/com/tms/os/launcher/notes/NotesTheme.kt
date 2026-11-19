package com.tms.os.launcher.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.ui.theme.SfDisplayFamily
import com.tms.os.launcher.ui.theme.SfFamily

/** iOS 26 Notes colours, dark and light. */
data class NotesColors(
    val dark: Boolean,
    val background: Color,
    val card: Color,
    val elevated: Color,
    val separator: Color,
    val text: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val glass: Color,
    val glassRim: Color,
    val folder: Color,
    val destructive: Color = Color(0xFFFF453A),
    val purple: Color = Color(0xFF7D5BE6),
    val orange: Color = Color(0xFFFF9F0A)
)

val DarkNotes = NotesColors(
    dark = true,
    background = Color.Black,
    card = Color(0xFF1C1C1E),
    elevated = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    text = Color.White,
    secondary = Color(0x99EBEBF5),
    tertiary = Color(0x4DEBEBF5),
    accent = Color(0xFFFFD60A),
    onAccent = Color.Black,
    glass = Color(0xDE333336),
    glassRim = Color(0x2EFFFFFF),
    folder = Color(0xFFFFD60A)
)

val LightNotes = NotesColors(
    dark = false,
    background = Color(0xFFF2F2F7),
    card = Color.White,
    elevated = Color.White,
    separator = Color(0xFFD1D1D6),
    text = Color.Black,
    secondary = Color(0x993C3C43),
    tertiary = Color(0x4D3C3C43),
    accent = Color(0xFFE3AA00),
    onAccent = Color.White,
    glass = Color(0xEBFFFFFF),
    glassRim = Color(0x1A000000),
    folder = Color(0xFFE3AA00),
    destructive = Color(0xFFFF3B30),
    purple = Color(0xFF5E5CE6),
    orange = Color(0xFFFF9500)
)

val LocalNotesColors = staticCompositionLocalOf { DarkNotes }

object NotesText {
    val largeTitle = TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.4).sp)
    val sectionTitle = TextStyle(fontFamily = SfDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp)
    val headline = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val body = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val subhead = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp)
    val footnote = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = SfFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
}

/** The Liquid Glass surface of iOS 26 buttons and bars. */
fun Modifier.notesGlass(c: NotesColors, shape: androidx.compose.ui.graphics.Shape = CircleShape): Modifier =
    this
        .shadow(if (c.dark) 0.dp else 10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
        .clip(shape)
        .background(c.glass)
        .border(0.7.dp, Brush.verticalGradient(listOf(c.glassRim, c.glassRim.copy(alpha = c.glassRim.alpha * 0.35f))), shape)

/** Shrinks slightly while pressed, like the iOS glass buttons. */
@Composable
fun Modifier.pressable(onClick: () -> Unit, enabled: Boolean = true): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return this
        .graphicsLayer { val s = if (pressed) 0.92f else 1f; scaleX = s; scaleY = s }
        .clickable(source, null, enabled = enabled, onClick = onClick)
}

@Composable
fun GlassCircle(icon: ImageVector, size: Dp = 44.dp, tint: Color? = null, onClick: () -> Unit) {
    val c = LocalNotesColors.current
    Box(Modifier.size(size).pressable(onClick).notesGlass(c), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint ?: c.text, modifier = Modifier.size(size * 0.5f))
    }
}

/** Several icon buttons in one glass capsule ("Teilen", "…"). */
@Composable
fun GlassCapsule(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val c = LocalNotesColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.height(44.dp).notesGlass(c).padding(horizontal = 4.dp),
        content = content
    )
}

@Composable
fun CapsuleIcon(icon: ImageVector, enabled: Boolean = true, tint: Color? = null, onClick: () -> Unit) {
    val c = LocalNotesColors.current
    Box(Modifier.size(40.dp).clip(CircleShape).pressable(onClick, enabled), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = (tint ?: c.text).copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(22.dp))
    }
}

/** A text button in a glass capsule ("Bearbeiten", "Fertig"). */
@Composable
fun GlassTextButton(text: String, bold: Boolean = false, color: Color? = null, onClick: () -> Unit) {
    val c = LocalNotesColors.current
    Box(Modifier.height(44.dp).widthIn(min = 44.dp).pressable(onClick).notesGlass(c).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, style = NotesText.body.copy(color = color ?: c.text, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium))
    }
}

/** The yellow "Fertig" circle with a check mark. */
@Composable
fun AccentCircle(icon: ImageVector, onClick: () -> Unit) {
    val c = LocalNotesColors.current
    Box(Modifier.size(44.dp).pressable(onClick).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = c.onAccent, modifier = Modifier.size(24.dp))
    }
}
