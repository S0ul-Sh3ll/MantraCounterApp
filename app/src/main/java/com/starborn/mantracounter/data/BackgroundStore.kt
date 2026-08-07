package com.starborn.mantracounter.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps background images inside app storage.
 *
 * The photo picker hands back a URI whose read permission is scoped to this process run, so the
 * image has to be copied in — otherwise the background disappears on next launch, or when the
 * original is deleted from the gallery. Images are downsampled on the way in: a phone photo is
 * ~4000px wide and there is no reason to hold that behind a 76dp card.
 */
object BackgroundStore {

    private const val DIR = "backgrounds"
    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 88

    /**
     * Copies [source] into app storage, downsampled and orientation-corrected.
     * Returns null if the image could not be read — callers must surface that rather than
     * silently saving a japa with no background.
     */
    suspend fun import(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = runCatching { decode(context, source) }.getOrNull() ?: return@withContext null
        runCatching {
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            val target = File(dir, "bg_${System.nanoTime()}.jpg")
            target.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            target.absolutePath
        }.getOrNull().also { bitmap.recycle() }
    }

    /** Deletes a previously imported image. Safe with null or an already-gone path. */
    suspend fun remove(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        runCatching { File(path).delete() }
        Unit
    }

    private fun decode(context: Context, uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(context, uri)
        else decodeLegacy(context, uri)

    /**
     * ImageDecoder applies EXIF rotation itself and handles HEIC, which is what most recent
     * phones actually shoot. BitmapFactory does neither, and would hand back sideways photos.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
            // A hardware bitmap cannot be compressed back out to a file.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeLegacy(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null

        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / sample > MAX_EDGE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}
