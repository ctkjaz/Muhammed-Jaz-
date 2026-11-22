package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
            val decoded = decodeAvatarBitmap(context, sourceUri)
                ?: error("Could not read selected image.")

            // Center-crop square and resize cleanly to MAX_AVATAR_PX
            val (startX, startY, side) = computeSquareCropRect(decoded.width, decoded.height)
            if (side <= 0) {
                decoded.recycle()
                error("Selected image has invalid dimensions.")
            }

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

            // Write new timestamped file first so existing avatar is preserved if compression fails
            val outFile = File(context.filesDir, "${AVATAR_PREFIX}${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(outFile).use { fos ->
                    val ok = finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                    fos.flush()
                    if (!ok) error("Failed to encode profile photo.")
                }
            } finally {
                finalBitmap.recycle()
            }

            cleanupOldAvatars(context.filesDir, keepFileName = outFile.name)
            outFile.absolutePath
        }
    }

    internal fun computeSampleSize(rawWidth: Int, rawHeight: Int, maxAvatarPx: Int = MAX_AVATAR_PX): Int {
        if (rawWidth <= 0 || rawHeight <= 0 || maxAvatarPx <= 0) return 1
        var sampleSize = 1
        val limit = maxAvatarPx * 2
        while ((rawWidth / sampleSize) > limit || (rawHeight / sampleSize) > limit) {
            sampleSize *= 2
        }
        return sampleSize
    }

    internal fun computeSquareCropRect(width: Int, height: Int): Triple<Int, Int, Int> {
        val side = min(width, height)
        if (side <= 0) return Triple(0, 0, 0)
        val startX = (width - side) / 2
        val startY = (height - side) / 2
        return Triple(startX, startY, side)
    }

    private fun decodeAvatarBitmap(context: Context, sourceUri: Uri): Bitmap? {
        // On Android 9 (API 28)+, ImageDecoder natively handles HEIC/HEIF, WebP, PNG, JPEG,
        // and automatically applies EXIF camera orientation metadata.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val imageDecoderResult = runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                    val sampleSize = computeSampleSize(info.size.width, info.size.height)
                    if (sampleSize > 1) {
                        decoder.setTargetSampleSize(sampleSize)
                    }
                }
            }.getOrNull()

            if (imageDecoderResult != null) {
                return imageDecoderResult
            }
        }

        // Reliable BitmapFactory fallback (API < 28 or URI providers where ImageDecoder fails)
        return decodeViaBitmapFactory(context, sourceUri)
    }

    private fun decodeViaBitmapFactory(context: Context, sourceUri: Uri): Bitmap? {
        val resolver = context.contentResolver

        // 1. Inspect bounds first to prevent OOM on large camera photos.
        // Note: BitmapFactory.decodeStream ALWAYS returns null when inJustDecodeBounds = true,
        // so check stream nullability directly rather than the decodeStream return value.
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(sourceUri)
            ?: error("Could not open selected image.")
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }

        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) {
            // Some providers or test streams may not populate bounds on first pass; attempt direct decode.
            val directStream = resolver.openInputStream(sourceUri) ?: return null
            return directStream.use { stream -> BitmapFactory.decodeStream(stream) }
        }

        val sampleSize = computeSampleSize(rawW, rawH)

        // 2. Decode downsampled bitmap
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodeStream = resolver.openInputStream(sourceUri)
            ?: error("Could not read selected image.")
        return decodeStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    suspend fun removeAvatar(context: Context, currentPath: String) = withContext(Dispatchers.IO) {
        runCatching {
            if (currentPath.isNotBlank()) {
                File(currentPath).delete()
            }
            cleanupOldAvatars(context.filesDir, keepFileName = null)
        }
    }

    private fun cleanupOldAvatars(filesDir: File, keepFileName: String?) {
        filesDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(AVATAR_PREFIX) && it.name != keepFileName }
            ?.forEach { runCatching { it.delete() } }
    }
}
