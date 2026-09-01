package com.noki.vpn

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.BackendAndroidUpdate
import com.noki.vpn.data.BackendApiClient
import com.noki.vpn.data.SettingsRepository
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

internal data class AndroidUpdateLogEvent(
    val message: String,
    val details: String? = null,
    val errorType: String? = null,
)

internal fun interface AndroidUpdateStateLoader {
    suspend fun loadStateWithToken(
        token: String,
        fallbackState: AndroidUpdateUiState,
        language: AppLanguage,
    ): AndroidUpdateUiState
}

internal class AndroidUpdateCoordinator(
    private val app: Application,
    private val repository: SettingsRepository,
    private val backendApi: BackendApiClient,
    private val authRunner: AuthenticatedCallRunner,
    private val logEvent: (AndroidUpdateLogEvent) -> Unit,
) : AndroidUpdateStateLoader {
    fun unauthenticatedState(): AndroidUpdateUiState {
        repository.clearAndroidUpdateAvailable()
        return AndroidUpdateUiState(currentVersionName = repository.currentAppVersionName())
    }

    suspend fun loadState(
        fallbackState: AndroidUpdateUiState,
        language: AppLanguage,
    ): AndroidUpdateUiState {
        return try {
            authRunner.run { token -> loadStateWithToken(token, fallbackState, language) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            fallbackState.copy(
                isChecking = false,
                isDownloading = false,
                currentVersionName = repository.currentAppVersionName(),
                error = AppErrorMapper.readableNetworkError(language, error),
            )
        }
    }

    fun requestInstallPermissionIfNeeded(language: AppLanguage): String? {
        if (app.packageManager.canRequestPackageInstalls()) {
            return null
        }
        runCatching {
            app.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${app.packageName}".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return tr(
            language,
            "Разрешите установку из этого источника и нажмите обновление ещё раз",
            "Allow installs from this source and tap update again",
        )
    }

    suspend fun downloadAndLaunch(
        update: AndroidUpdateInfo,
        ensureInstallerLaunchAllowed: () -> Unit,
    ) {
        val targetFile = androidUpdateFile(update)
        clearSameOrOlderCachedApks()
        targetFile.delete()
        targetFile.parentFile?.mkdirs()

        logEvent(
            AndroidUpdateLogEvent(
                message = "download_start",
                details = "version=${update.versionName}, architecture=${update.architecture}, size=${update.apkSizeBytes ?: 0}",
            ),
        )

        try {
            authRunner.run { token ->
                backendApi.downloadAndroidUpdateApk(
                    token = token,
                    apkUrl = update.apkUrl,
                    expectedSha256 = update.apkSha256,
                    expectedSizeBytes = update.apkSizeBytes,
                    destination = targetFile,
                )
            }
            if (targetFile.length() <= 0L) {
                throw IllegalStateException("Downloaded APK is empty")
            }
            logEvent(
                AndroidUpdateLogEvent(
                    message = "download_complete",
                    details = "version=${update.versionName}, architecture=${update.architecture}, bytes=${targetFile.length()}",
                ),
            )
            currentCoroutineContext().ensureActive()
            ensureInstallerLaunchAllowed()
            launchInstaller(targetFile)
        } catch (cancelled: CancellationException) {
            targetFile.delete()
            clearSameOrOlderCachedApks()
            throw cancelled
        } catch (error: Throwable) {
            targetFile.delete()
            clearSameOrOlderCachedApks()
            logEvent(
                AndroidUpdateLogEvent(
                    message = "install_failed",
                    details = "version=${update.versionName}, architecture=${update.architecture}",
                    errorType = error::class.java.simpleName,
                ),
            )
            throw error
        }
    }

    fun readableInstallError(
        language: AppLanguage,
        error: Throwable,
    ): String {
        val causedByMissingInstaller = generateSequence(error as Throwable?) { it.cause }
            .any { it is ActivityNotFoundException }
        return when {
            causedByMissingInstaller ->
                tr(language, "Не удалось открыть установщик APK", "Failed to open APK installer")

            error is IllegalStateException && error.message == "Downloaded APK is empty" ->
                tr(language, "Скачанный APK пустой", "Downloaded APK is empty")

            error is IllegalStateException && error.message?.contains("APK installer", ignoreCase = true) == true ->
                tr(language, "Не удалось открыть установщик APK", "Failed to open APK installer")

            else -> AppErrorMapper.readableNetworkError(language, error)
        }
    }

    fun clearSameOrOlderCachedApks() {
        androidUpdateDirectory().listFiles()
            ?.filter { file -> AndroidUpdateCachePolicy.shouldDeleteCachedApk(file, repository.currentAppVersionName()) }
            ?.forEach { file -> file.delete() }
    }

    override suspend fun loadStateWithToken(
        token: String,
        fallbackState: AndroidUpdateUiState,
        language: AppLanguage,
    ): AndroidUpdateUiState {
        val currentVersionCode = repository.currentAppVersionCode()
        val currentVersionName = repository.currentAppVersionName()
        val update = try {
            backendApi.androidUpdate(
                token = token,
                versionCode = currentVersionCode,
                abis = Build.SUPPORTED_ABIS.toList(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            return fallbackState.copy(
                isChecking = false,
                isDownloading = false,
                currentVersionName = currentVersionName,
                error = if (fallbackState.update == null) {
                    AppErrorMapper.readableNetworkError(language, error)
                } else {
                    fallbackState.error
                },
            )
        }

        currentCoroutineContext().ensureActive()
        val uiUpdate = update.toAndroidUpdateInfoOrNull(currentVersionCode)
        if (uiUpdate == null) {
            repository.clearAndroidUpdateAvailable()
        } else {
            repository.markAndroidUpdateAvailable()
        }
        return AndroidUpdateUiState(
            currentVersionName = currentVersionName,
            update = uiUpdate,
        )
    }

    private fun BackendAndroidUpdate.toAndroidUpdateInfoOrNull(currentVersionCode: Long): AndroidUpdateInfo? {
        if (!updateAvailable) return null
        val code = versionCode ?: return null
        if (code <= currentVersionCode) return null
        val url = apkUrl?.takeIf { it.isNotBlank() } ?: return null
        return AndroidUpdateInfo(
            versionCode = code,
            versionName = versionName?.takeIf { it.isNotBlank() } ?: code.toString(),
            releaseNotes = releaseNotes,
            isForced = isForced,
            architecture = architecture?.takeIf { it.isNotBlank() } ?: "universal",
            apkUrl = url,
            apkSha256 = apkSha256,
            apkSizeBytes = apkSizeBytes,
        )
    }

    private fun androidUpdateFile(update: AndroidUpdateInfo): File {
        return File(androidUpdateDirectory(), AndroidUpdateCachePolicy.fileName(update))
    }

    private fun androidUpdateDirectory(): File {
        return File(app.cacheDir, "android_updates")
    }

    private fun launchInstaller(file: File) {
        val uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file,
        )
        val intent = buildInstallerIntent(uri)
        try {
            logEvent(
                AndroidUpdateLogEvent(
                    message = "installer_launch",
                    details = "bytes=${file.length()}",
                ),
            )
            app.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            throw IllegalStateException("APK installer is not available", error)
        }
    }

    private fun buildInstallerIntent(uri: Uri): Intent {
        val candidates = listOf(
            Intent(Intent.ACTION_INSTALL_PACKAGE),
            Intent(Intent.ACTION_VIEW),
        ).map { intent ->
            intent.setClipData(ClipData.newUri(app.contentResolver, "Noki Android update", uri))
            intent
                .setDataAndType(uri, APK_MIME_TYPE)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return candidates.firstOrNull { intent ->
            app.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        } ?: candidates.first()
    }

    private fun tr(
        language: AppLanguage,
        russian: String,
        english: String,
    ): String {
        return if (language == AppLanguage.RU) russian else english
    }
}
