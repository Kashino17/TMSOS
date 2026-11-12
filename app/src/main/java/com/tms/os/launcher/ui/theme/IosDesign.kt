package com.tms.os.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tms.os.launcher.R
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/* -------------------------------------------------------------------------- */
/*                                  Typography                                */
/* -------------------------------------------------------------------------- */

/**
 * Inter is the spiritually closest freely licensed relative of SF Pro.
 * Inter Display is used for the large optical sizes (large titles, widgets).
 */
val LocalLauncherSettings = staticCompositionLocalOf { com.tms.os.launcher.model.LauncherSettings() }

val SfFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val SfDisplayFamily = FontFamily(
    Font(R.font.inter_display_regular, FontWeight.Normal),
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold, FontWeight.Bold)
)

val SfLightFamily = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

object IosText {
    val largeTitle = TextStyle(
        fontFamily = SfDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 41.sp
    )
    val title1 = TextStyle(
        fontFamily = SfDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.35).sp,
        lineHeight = 34.sp
    )
    val title2 = TextStyle(
        fontFamily = SfDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 28.sp
    )
    val title3 = TextStyle(
        fontFamily = SfDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 25.sp
    )
    val headline = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 22.sp
    )
    val body = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 22.sp
    )
    val callout = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = (-0.15).sp,
        lineHeight = 21.sp
    )
    val subheadline = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 20.sp
    )
    val footnote = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = (-0.05).sp,
        lineHeight = 18.sp
    )
    val footnoteSemibold = footnote.copy(fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )
    val captionMedium = caption.copy(fontWeight = FontWeight.Medium)
    val iconLabel = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = (-0.18).sp,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center
    )
    val search = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp
    )
    val button = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp
    )
    val tabLabel = TextStyle(
        fontFamily = SfFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.1.sp
    )
}

object IosFontWeight {
    val regular = FontWeight.Normal
    val medium = FontWeight.Medium
    val semibold = FontWeight.SemiBold
    val bold = FontWeight.Bold
}

/* -------------------------------------------------------------------------- */
/*                                   Colours                                  */
/* -------------------------------------------------------------------------- */

@Immutable
data class IosPalette(
    val dark: Boolean,
    val background: Color,
    val secondaryBackground: Color,
    val tertiaryBackground: Color,
    val groupedBackground: Color,
    val cardBackground: Color,
    val cardBackgroundElevated: Color,
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val fill: Color,
    val secondaryFill: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val purple: Color,
    val pink: Color,
    val teal: Color,
    val indigo: Color,
    val gray: Color,
    val switchOff: Color,
    val chromeGlass: Color,
    val chromeGlassBorder: Color,
    val chromeGlassHighlight: Color
)

val IosDarkPalette = IosPalette(
    dark = true,
    background = Color(0xFF000000),
    secondaryBackground = Color(0xFF1C1C1E),
    tertiaryBackground = Color(0xFF2C2C2E),
    groupedBackground = Color(0xFF000000),
    cardBackground = Color(0xFF1C1C1E),
    cardBackgroundElevated = Color(0xFF2C2C2E),
    separator = Color(0x33FFFFFF),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x9EFFFFFF),
    tertiaryLabel = Color(0x61FFFFFF),
    quaternaryLabel = Color(0x33FFFFFF),
    fill = Color(0x3D787880),
    secondaryFill = Color(0x29787880),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    yellow = Color(0xFFFFD60A),
    purple = Color(0xFFBF5AF2),
    pink = Color(0xFFFF375F),
    teal = Color(0xFF64D2FF),
    indigo = Color(0xFF5E5CE6),
    gray = Color(0xFF8E8E93),
    switchOff = Color(0xFF39393D),
    chromeGlass = Color(0x8C1C1C1E),
    chromeGlassBorder = Color(0x1FFFFFFF),
    chromeGlassHighlight = Color(0x14FFFFFF)
)

val IosLightPalette = IosPalette(
    dark = false,
    background = Color(0xFFF2F2F7),
    secondaryBackground = Color(0xFFFFFFFF),
    tertiaryBackground = Color(0xFFF2F2F7),
    groupedBackground = Color(0xFFF2F2F7),
    cardBackground = Color(0xFFFFFFFF),
    cardBackgroundElevated = Color(0xFFFFFFFF),
    separator = Color(0x2E3C3C43),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    quaternaryLabel = Color(0x2E3C3C43),
    fill = Color(0x1F767680),
    secondaryFill = Color(0x14767680),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    yellow = Color(0xFFFFCC00),
    purple = Color(0xFFAF52DE),
    pink = Color(0xFFFF2D55),
    teal = Color(0xFF5AC8FA),
    indigo = Color(0xFF5856D6),
    gray = Color(0xFF8E8E93),
    switchOff = Color(0xFFE5E5EA),
    chromeGlass = Color(0xB3FFFFFF),
    chromeGlassBorder = Color(0x1F000000),
    chromeGlassHighlight = Color(0x66FFFFFF)
)

val LocalIosPalette = staticCompositionLocalOf { IosDarkPalette }

object IosColor {
    val blue = Color(0xFF0A84FF)
    val green = Color(0xFF30D158)
    val red = Color(0xFFFF453A)
    val orange = Color(0xFFFF9F0A)
    val yellow = Color(0xFFFFD60A)
    val purple = Color(0xFFBF5AF2)
    val pink = Color(0xFFFF375F)
    val teal = Color(0xFF64D2FF)
    val indigo = Color(0xFF5E5CE6)
    val gray = Color(0xFF8E8E93)
    val weatherBlue = Color(0xFF2E7CF6)
}

/* -------------------------------------------------------------------------- */
/*                                   Motion                                   */
/* -------------------------------------------------------------------------- */

object IosMotion {
    /** Icon press: quick, tight, almost no bounce. */
    fun press() = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 900f
    )

    /** Sheet / modal spring used across iOS. */
    fun modal() = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = 360f
    )

    fun grabber() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val easeOut = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val easeInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val launch = tween<Float>(durationMillis = 300, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f))
}

/* -------------------------------------------------------------------------- */
/*                                   Shapes                                   */
/* -------------------------------------------------------------------------- */

/**
 * Continuous superelliptic icon silhouette. The exponent is tuned for the
 * optical radius at launcher icon sizes; this is not an Apple asset.
 */
class IosSquircleShape(
    private val n: Float = 4.2f,
    private val steps: Int = 160
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Generic(Path())
        }
        val key = (size.width.toRawBits().toLong() shl 32) or
            (size.height.toRawBits().toLong() and 0xFFFFFFFFL)
        val path = pathCache.getOrPut(key) { buildPath(size.width, size.height, n, steps) }
        return Outline.Generic(path)
    }

    private fun buildPath(w: Float, h: Float, n: Float, steps: Int): Path {
        val path = Path()
        val exponent = 2f / n
        val cx = w / 2f
        val cy = h / 2f
        for (i in 0..steps) {
            val t = (i.toDouble() / steps) * 2.0 * PI
            val c = cos(t)
            val s = sin(t)
            val x = cx + (sign(c) * abs(c).pow(exponent.toDouble())).toFloat() * cx
            val y = cy + (sign(s) * abs(s).pow(exponent.toDouble())).toFloat() * cy
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private val pathCache = ConcurrentHashMap<Long, Path>(32)
}

val IosSquircle = IosSquircleShape()

/** Standard container radii used by iOS. */
object IosShape {
    val card = RoundedCornerShape(22.dp)
    val cardSmall = RoundedCornerShape(14.dp)
    val cardLarge = RoundedCornerShape(28.dp)
    val sheet = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
    val search = RoundedCornerShape(12.dp)
    val control = RoundedCornerShape(10.dp)
    val menu = RoundedCornerShape(22.dp)
    val tile = RoundedCornerShape(7.dp)
}

/* -------------------------------------------------------------------------- */
/*                                   Metrics                                  */
/* -------------------------------------------------------------------------- */

object IosMetrics {
    val statusBarHeight = 54.dp
    val screenHorizontalMargin = 23.dp
    val gridRowGapMin = 6.dp
    val dockHeight = 82.dp
    val dockCorner = 39.dp
    val pageDotsHeight = 26.dp
    val searchButtonHeight = 34.dp
    val homeIndicatorWidth = 134.dp
    val homeIndicatorHeight = 5.dp
}
