package com.tms.os.launcher.notes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Pictures in notes: copied (downscaled) into the app's own storage, never referenced from outside. */
class NoteMedia(private val context: Context, private val store: NoteStore) {

    private val maxSide = 2048

    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            var bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                ?: return@runCatching null
            // Camera pictures carry their rotation in EXIF.
            val rotation = context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            if (rotation != 0f) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
            bmp = NoteImages.onWhite(bmp)
            val name = "${UUID.randomUUID()}.jpg"
            store.mediaFile(name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            name
        }.getOrNull()
    }

    suspend fun importFile(f: File): String? = importImage(Uri.fromFile(f)).also { withContext(Dispatchers.IO) { f.delete() } }

    /** Target for "Foto aufnehmen"; shared with the camera app through the FileProvider. */
    fun newCameraFile(): File = File(store.mediaDir, "camera_${UUID.randomUUID()}.jpg")

    suspend fun saveDrawing(png: Bitmap, oldFile: String?): String = withContext(Dispatchers.IO) {
        val name = oldFile ?: "${UUID.randomUUID()}.png"
        store.mediaFile(name).outputStream().use { png.compress(Bitmap.CompressFormat.PNG, 100, it) }
        name
    }
}

/** "Dokumente scannen": Google's document scanner (edge detection, cropping, filters), from Play services. */
object NoteScanner {
    fun start(context: Context, launch: (IntentSender) -> Unit) {
        val activity = context.findActivity() ?: return
        // ML Kit sets itself up through a content provider that only runs in the app's
        // main process; Notes has its own process (":notes"), so it is set up here.
        val ready = runCatching { com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context.applicationContext) }.isSuccess
        if (!ready) { unavailable(context); return }
        runCatching { startScan(activity, context, launch) }.onFailure { unavailable(context) }
    }

    private fun unavailable(context: Context) =
        Toast.makeText(context, "Der Scanner ist auf diesem Gerät nicht verfügbar.", Toast.LENGTH_SHORT).show()

    private fun startScan(activity: Activity, context: Context, launch: (IntentSender) -> Unit) {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(24)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener { runCatching { launch(it) }.onFailure { unavailable(context) } }
            .addOnFailureListener { unavailable(context) }
    }

    fun pages(resultCode: Int, data: Intent?): List<Uri> =
        if (resultCode != Activity.RESULT_OK) emptyList()
        else runCatching { GmsDocumentScanningResult.fromActivityResultIntent(data)?.pages?.map { it.imageUri } }.getOrNull().orEmpty()
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
