package com.tms.os.launcher.ui.theme

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import com.tms.os.launcher.R
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** One wallpaper and one blurred copy, shared by every glass surface. */
data class WallpaperBackdrop(
    val image: ImageBitmap? = null,
    val glass: ImageBitmap? = null,
    val viewport: IntSize = IntSize.Zero
)

val LocalWallpaperBackdrop = staticCompositionLocalOf { WallpaperBackdrop() }

@Composable
fun rememberWallpaperBackdrop(size: IntSize, useSystemWallpaper: Boolean, revision: Int): WallpaperBackdrop {
    val context = LocalContext.current
    var backdrop by remember(size, useSystemWallpaper) { mutableStateOf(WallpaperBackdrop(viewport = size)) }
    // The bundled wallpaper never changes: decoding + blurring it again on every
    // resume would steal frames from the return-to-home animation.
    // Returning from an app must not decode and blur the wallpaper again: only when the
    // user actually changed it (the system gives every wallpaper a new id).
    var loadedId by remember(size, useSystemWallpaper) { mutableStateOf<Int?>(null) }
    LaunchedEffect(size, useSystemWallpaper, if (useSystemWallpaper) revision else 0) {
        val id = if (useSystemWallpaper) withContext(Dispatchers.IO) {
            runCatching { WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)
        } else 0
        if (id == loadedId && id != -1 && backdrop.image != null) return@LaunchedEffect
        loadedId = id
        backdrop = withContext(Dispatchers.Default) {
            if (size.width <= 0 || size.height <= 0) return@withContext WallpaperBackdrop()
            val scale = minOf(1f, 1080f / max(size.width, size.height))
            val width = (size.width * scale).roundToInt().coerceAtLeast(1)
            val height = (size.height * scale).roundToInt().coerceAtLeast(1)
            val bitmap = if (useSystemWallpaper) readWallpaper(context, width, height)
                else readBundledWallpaper(context, width, height)
            // On Android 13+ the system can deny wallpaper pixel access. In that
            // case leave the window wallpaper visible, without asking for storage.
            if (bitmap == null) WallpaperBackdrop(viewport = size)
            else WallpaperBackdrop(bitmap.asImageBitmap(), frostedCopy(bitmap).asImageBitmap(), size)
        }
    }
    return backdrop
}

// Best-effort on older Android/OEMs. SecurityException is caught; no broad storage permission is requested.
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private fun readWallpaper(context: Context, width: Int, height: Int): Bitmap? = runCatching {
    val drawable = WallpaperManager.getInstance(context).drawable ?: return@runCatching null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val scale = max(width.toFloat() / drawable.intrinsicWidth.coerceAtLeast(1), height.toFloat() / drawable.intrinsicHeight.coerceAtLeast(1))
    val w = (drawable.intrinsicWidth * scale).roundToInt()
    val h = (drawable.intrinsicHeight * scale).roundToInt()
    drawable.setBounds((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
    drawable.draw(Canvas(bitmap))
    bitmap
}.getOrNull()

private fun readBundledWallpaper(context: Context, width: Int, height: Int): Bitmap {
    val source = BitmapFactory.decodeResource(context.resources, R.drawable.ios27_default_dark)
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
    val w = source.width * scale
    val h = source.height * scale
    Canvas(output).drawBitmap(source, null, RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    source.recycle()
    return output
}

/** Small separable box blur, calculated off the UI thread, also works on API 28. */
private fun frostedCopy(source: Bitmap): Bitmap {
    val w = (source.width / 8).coerceAtLeast(1)
    val h = (source.height / 8).coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(source, w, h, true)
    var pixels = IntArray(w * h)
    small.getPixels(pixels, 0, w, 0, 0, w, h)
    repeat(3) {
        for (horizontal in listOf(true, false)) {
            val output = IntArray(pixels.size)
            val radius = 4
            for (y in 0 until h) for (x in 0 until w) {
                var red = 0; var green = 0; var blue = 0
                for (d in -radius..radius) {
                    val sx = if (horizontal) (x + d).coerceIn(0, w - 1) else x
                    val sy = if (horizontal) y else (y + d).coerceIn(0, h - 1)
                    val color = pixels[sy * w + sx]
                    red += color shr 16 and 255; green += color shr 8 and 255; blue += color and 255
                }
                val count = radius * 2 + 1
                output[y * w + x] = (255 shl 24) or (red / count shl 16) or (green / count shl 8) or (blue / count)
            }
            pixels = output
        }
    }
    val output = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    if (small !== source) small.recycle()
    return output
}

/**
 * The neutral dark materials used across the launcher (dock, App Library pods,
 * Spotlight cards, pills …). One blurred wallpaper sample underneath, one of
 * these flat grey tints on top and a hairline border: quiet, never colourful.
 */
object IosMaterial {
    val dock = Color(0xA6262628)
    val pod = Color(0xA82E2E30)
    val card = Color(0xBF3A3A3C)
    val field = Color(0xE0484849)
    val pill = Color(0xD1242426)
    val menu = Color(0xE0262628)
    val widget = Color(0xD61C1C1E)
    val folder = Color(0xB3303032)
    const val hairline = 0.14f
}

/**
 * Frosted dark material: the blurred wallpaper under this surface, a flat grey
 * [tint] and a hairline border of [rim] alpha. Children stay sharp.
 */
fun Modifier.wallpaperGlass(
    shape: Shape,
    tint: Color = IosMaterial.card,
    rim: Float = IosMaterial.hairline
): Modifier = composed {
    val backdrop = LocalWallpaperBackdrop.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { origin = it.boundsInRoot().topLeft }
        .clip(shape)
        .drawWithCache {
            onDrawBehind {
                backdrop.glass?.let { bitmap ->
                    if (backdrop.viewport.width > 0 && backdrop.viewport.height > 0) {
                        drawImage(
                            bitmap,
                            dstOffset = IntOffset(-origin.x.roundToInt(), -origin.y.roundToInt()),
                            dstSize = backdrop.viewport
                        )
                    }
                }
                drawRect(tint)
            }
        }
        .then(if (rim > 0f) Modifier.border(0.7.dp, Color.White.copy(alpha = rim), shape) else Modifier)
}
