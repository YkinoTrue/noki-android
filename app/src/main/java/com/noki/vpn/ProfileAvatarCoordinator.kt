package com.noki.vpn

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.AvatarCachePolicy
import com.noki.vpn.data.BackendApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal data class AvatarCropRequest(
    val sourceUri: String,
    val previewWidthPx: Float,
    val previewHeightPx: Float,
    val cropCircleSizePx: Float,
    val cropScale: Float,
    val cropOffsetX: Float,
    val cropOffsetY: Float,
)

internal fun interface ProfileAvatarLoader {
    suspend fun cachedBackendAvatarUri(
        token: String,
        backendAvatarUrl: String?,
        fallbackAvatarUri: String?,
    ): String?
}

internal object AvatarBitmapDecoder {
    private const val MAX_DECODED_SIDE = 2_048
    private const val MAX_DECODED_PIXELS = 4_194_304L

    fun inSampleSize(width: Int, height: Int): Int {
        require(width > 0 && height > 0) { "avatar_open_failed" }
        var sample = 1
        while (true) {
            val sampledWidth = (width.toLong() + sample - 1L) / sample
            val sampledHeight = (height.toLong() + sample - 1L) / sample
            if (
                sampledWidth <= MAX_DECODED_SIDE &&
                sampledHeight <= MAX_DECODED_SIDE &&
                sampledWidth * sampledHeight <= MAX_DECODED_PIXELS
            ) {
                return sample
            }
            check(sample <= Int.MAX_VALUE / 2) { "avatar_open_failed" }
            sample *= 2
        }
    }

    fun decode(openStream: () -> InputStream?): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IllegalStateException("avatar_open_failed")
        val sample = try {
            inSampleSize(bounds.outWidth, bounds.outHeight)
        } catch (_: IllegalArgumentException) {
            throw IllegalStateException("avatar_open_failed")
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openStream()?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("avatar_open_failed")
    }
}

internal class ProfileAvatarCoordinator(
    private val app: Application,
    private val backendApi: BackendApiClient,
) : ProfileAvatarLoader {
    private val cacheMutex = Mutex()

    suspend fun uploadCroppedAvatar(
        token: String,
        request: AvatarCropRequest,
    ): String {
        val bytes = withContext(Dispatchers.IO) {
            croppedAvatarJpegBytes(request)
        }
        val avatarUrl = backendApi.uploadAvatar(
            token = token,
            fileName = "avatar.jpg",
            mimeType = "image/jpeg",
            bytes = bytes,
        )
        return cacheUploadedAvatar(avatarUrl, bytes)
    }

    suspend fun deleteAvatar(token: String) {
        backendApi.deleteAvatar(token)
        cacheMutex.withLock {
            withContext(Dispatchers.IO) { clearAvatarCache() }
        }
    }

    override suspend fun cachedBackendAvatarUri(
        token: String,
        backendAvatarUrl: String?,
        fallbackAvatarUri: String?,
    ): String? {
        val avatarUrl = backendAvatarUrl?.takeIf { it.isNotBlank() } ?: return null
        return try {
            cacheMutex.withLock {
                val target = avatarCacheFile(avatarUrl)
                if (target.length() <= 0L) {
                    backendApi.downloadProfileAvatar(
                        token = token,
                        avatarUrl = avatarUrl,
                        destination = target,
                    )
                }
                target.takeIf { it.length() > 0L }?.toUri()?.toString()
                    ?: throw IllegalStateException("avatar_open_failed")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            fallbackAvatarUri?.takeIf { cached -> cached.startsWith("file:", ignoreCase = true) }
        }
    }

    fun readableAvatarError(
        language: AppLanguage,
        error: Throwable,
    ): String {
        return when (error.message) {
            "avatar_file_too_large" -> tr(language, "Файл больше 5 МБ", "File is larger than 5 MB")
            "avatar_open_failed" -> tr(language, "Не удалось открыть файл", "Could not open the file")
            else -> AppErrorMapper.readableNetworkError(language, error)
        }
    }

    private fun avatarCacheFile(avatarUrl: String): File {
        return File(app.cacheDir, "profile_avatar/${AvatarCachePolicy.cacheFileName(avatarUrl)}")
    }

    private fun clearAvatarCache() {
        File(app.cacheDir, "profile_avatar").deleteRecursively()
    }

    private suspend fun cacheUploadedAvatar(avatarUrl: String, bytes: ByteArray): String =
        cacheMutex.withLock {
            withContext(Dispatchers.IO) {
                clearAvatarCache()
                val target = avatarCacheFile(avatarUrl)
                val parent = checkNotNull(target.parentFile) { "avatar_cache_write_failed" }
                check(parent.isDirectory || parent.mkdirs()) { "avatar_cache_write_failed" }
                val temporary = File(parent, "${target.name}.part")
                try {
                    temporary.writeBytes(bytes)
                    Files.move(temporary.toPath(), target.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
                    target.toUri().toString()
                } finally {
                    temporary.delete()
                }
            }
        }

    private fun croppedAvatarJpegBytes(request: AvatarCropRequest): ByteArray {
        val sourceUri = request.sourceUri.toUri()
        val source = AvatarBitmapDecoder.decode { app.contentResolver.openInputStream(sourceUri) }
        val outputSize = 512
        val previewWidth = request.previewWidthPx.takeIf { it.isFinite() && it > 0f } ?: source.width.toFloat()
        val previewHeight = request.previewHeightPx.takeIf { it.isFinite() && it > 0f } ?: source.height.toFloat()
        val cropCircle = request.cropCircleSizePx
            .takeIf { it.isFinite() && it > 0f }
            ?: minOf(previewWidth, previewHeight)
        val baseScale = minOf(previewWidth / source.width.toFloat(), previewHeight / source.height.toFloat())
        val userScale = request.cropScale.takeIf(Float::isFinite)?.coerceIn(1f, 5f) ?: 1f
        val effectiveScale = baseScale * userScale
        val outputScale = outputSize / cropCircle
        val previewLeft = (previewWidth - source.width * effectiveScale) / 2f +
            request.cropOffsetX.takeIf(Float::isFinite).orZero()
        val previewTop = (previewHeight - source.height * effectiveScale) / 2f +
            request.cropOffsetY.takeIf(Float::isFinite).orZero()
        val cropLeft = (previewWidth - cropCircle) / 2f
        val cropTop = (previewHeight - cropCircle) / 2f
        val output = createBitmap(outputSize, outputSize)
        val matrix = Matrix().apply {
            postScale(effectiveScale * outputScale, effectiveScale * outputScale)
            postTranslate((previewLeft - cropLeft) * outputScale, (previewTop - cropTop) * outputScale)
        }
        return try {
            Canvas(output).drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            ByteArrayOutputStream().use { bytes ->
                check(output.compress(Bitmap.CompressFormat.JPEG, 92, bytes)) { "avatar_encode_failed" }
                bytes.toByteArray()
            }
        } finally {
            source.recycle()
            output.recycle()
        }
    }

    private fun Float?.orZero(): Float = this ?: 0f

    private fun tr(
        language: AppLanguage,
        russian: String,
        english: String,
    ): String {
        return if (language == AppLanguage.RU) russian else english
    }
}
