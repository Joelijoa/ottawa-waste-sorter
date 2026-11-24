package com.ottawa.wastesorter.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Redimensionne et compresse les photos avant envoi (réduit le coût de l'IA et le temps d'envoi).
 * Les photos ne sont jamais conservées en taille réelle.
 */
class ImageProcessor(private val context: Context) {

    /** Image JPEG prête à l'envoi, côté le plus long ≤ [UPLOAD_MAX_SIDE] px. */
    suspend fun prepareUpload(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(uri, UPLOAD_MAX_SIDE)
        try {
            bitmap.toJpeg(UPLOAD_QUALITY)
        } finally {
            bitmap.recycle()
        }
    }

    /** Miniature locale pour l'historique ; renvoie le chemin du fichier. */
    suspend fun saveThumbnail(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(uri, THUMB_MAX_SIDE)
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.writeBytes(bitmap.toJpeg(THUMB_QUALITY))
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    /** Image réduite pour l'aperçu à l'écran. */
    suspend fun loadPreview(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching { decodeScaled(uri, PREVIEW_MAX_SIDE) }.getOrNull()
    }

    /** Supprime une photo temporaire prise avec la caméra (dans le cache de l'application). */
    fun deleteIfTemporary(uri: Uri) {
        val path = uri.path ?: return
        if (uri.scheme == "file" && path.startsWith(context.cacheDir.absolutePath)) File(path).delete()
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable image" }

        val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)

        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Unreadable image")

        val rotation = resolver.openInputStream(uri).use { stream ->
            if (stream == null) 0 else when (
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }

        val scale = minOf(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        if (rotation == 0 && scale >= 1f) return decoded

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result !== decoded) decoded.recycle()
        return result
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    companion object {
        const val UPLOAD_MAX_SIDE = 1024
        const val UPLOAD_QUALITY = 80
        const val THUMB_MAX_SIDE = 256
        const val THUMB_QUALITY = 75
        const val PREVIEW_MAX_SIDE = 1600

        /** Facteur d'échantillonnage (puissance de 2) — logique isolée pour les tests unitaires. */
        fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
            var sample = 1
            while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
            return sample
        }
    }
}
