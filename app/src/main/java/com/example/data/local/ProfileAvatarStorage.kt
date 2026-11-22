package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object ProfileAvatarStorage {
    private const val MAX_AVATAR_PX = 512
    private const val AVATAR_PREFIX = "profile_avatar_"

    suspend fun savePickedAvatar(context: Context, sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 1. Inspect bounds first to prevent OOM on large camera photos
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            } ?: error("Could not read selected image.")

            val rawW = boundsOpts.outWidth
            val rawH = boundsOpts.outHeight
            if (rawW <= 0 || rawH <= 0) {
                error("Unsupported image format.")
            }

            var sampleSize = 1
            while ((rawW / sampleSize) > MAX_AVATAR_PX * 2 || (rawH / sampleSize) > MAX_AVATAR_PX * 2) {
                sampleSize *= 2
            }

            // 2. Decode downsampled bitmap
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: error("Could not decode selected image.")

            // 3. Center-crop square and resize cleanly to MAX_AVATAR_PX
            val side = min(decoded.width, decoded.height)
            val startX = (decoded.width - side) / 2
            val startY = (decoded.height - side) / 2
            val square = Bitmap.createBitmap(decoded, startX, startY, side, side)
            if (square !== decoded) decoded.recycle()

            val targetSize = min(side, MAX_AVATAR_PX)
            val finalBitmap = if (side > targetSize) {
                val scaled = Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
                square.recycle()
                scaled
            } else {
                square
            }

            // 4. Clean up previous avatar files and write a fresh timestamped file for cache-busting
            cleanupOldAvatars(context.filesDir)
            val outFile = File(context.filesDir, "${AVATAR_PREFIX}${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                fos.flush()
            }
            finalBitmap.recycle()
            outFile.absolutePath
        }
    }

    suspend fun removeAvatar(context: Context, currentPath: String) = withContext(Dispatchers.IO) {
        runCatching {
            if (currentPath.isNotBlank()) {
                File(currentPath).delete()
            }
            cleanupOldAvatars(context.filesDir)
        }
    }

    private fun cleanupOldAvatars(filesDir: File) {
        filesDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(AVATAR_PREFIX) }
            ?.forEach { runCatching { it.delete() } }
    }
}
