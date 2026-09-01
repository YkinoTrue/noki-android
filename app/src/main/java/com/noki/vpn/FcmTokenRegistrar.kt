package com.noki.vpn

import android.content.Context
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.noki.vpn.data.BackendApiClient
import com.noki.vpn.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import java.security.MessageDigest

internal fun isCurrentFcmRegistration(owner: Job, current: Job?): Boolean =
    current === owner && owner.isActive

internal class FcmRegistrationOwner {
    private val lock = Any()
    private var current: Job? = null

    fun replace(next: Job): Job? = synchronized(lock) {
        current.also { current = next }
    }

    fun commitIfCurrent(owner: Job, commit: () -> Unit): Boolean = synchronized(lock) {
        if (!isCurrentFcmRegistration(owner, current)) return@synchronized false
        commit()
        true
    }

    fun cancelAndClear(clear: () -> Unit): Job? = synchronized(lock) {
        val previous = current
        current = null
        clear()
        previous
    }

    fun clearIfCurrent(owner: Job) = synchronized(lock) {
        if (current === owner) current = null
    }
}

object FcmTokenRegistrar {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registrationOwner = FcmRegistrationOwner()

    fun syncCurrentTokenIfAvailable(
        context: Context,
        repository: SettingsRepository,
        backendApi: BackendApiClient,
    ) {
        if (!isFirebaseConfigured(context)) return

        val task = runCatching { FirebaseMessaging.getInstance().token }
            .getOrElse { error ->
                repository.recordAppLog(
                    category = "notification",
                    level = "error",
                    message = "fcm_token_fetch_failed",
                    errorType = error::class.java.simpleName,
                )
                return
            }
        task.addOnSuccessListener { fcmToken ->
            registerKnownToken(
                repository = repository,
                backendApi = backendApi,
                fcmToken = fcmToken,
            )
        }
        task.addOnFailureListener { error ->
            repository.recordAppLog(
                category = "notification",
                level = "error",
                message = "fcm_token_fetch_failed",
                errorType = error::class.java.simpleName,
            )
        }
    }

    fun registerKnownToken(
        repository: SettingsRepository,
        backendApi: BackendApiClient,
        fcmToken: String?,
    ) {
        if (fcmToken.isNullOrBlank()) {
            cancelPendingRegistration(repository)
            return
        }
        val cleanToken = fcmToken
        val tokenHash = sha256(cleanToken)

        val job = scope.launch(start = CoroutineStart.LAZY) {
            val ownerJob = currentCoroutineContext()[Job]
                ?: error("FCM registration coroutine has no Job")
            try {
                val settings = repository.load()
                val authToken = settings.backendAccessToken
                val cleanDeviceId = settings.backendDeviceId
                if (!FcmRegistrationStatePolicy.shouldSyncCurrentToken(
                        accessToken = authToken,
                        deviceId = cleanDeviceId,
                        fcmToken = cleanToken,
                    )
                ) {
                    if (cleanDeviceId.isBlank()) {
                        registrationOwner.commitIfCurrent(ownerJob) {
                            repository.clearLastRegisteredFcmTokenHash()
                        }
                    }
                    return@launch
                }
                backendApi.registerFcmToken(
                    token = authToken.orEmpty(),
                    fcmToken = cleanToken,
                    deviceId = cleanDeviceId,
                )
                registrationOwner.commitIfCurrent(ownerJob) {
                    repository.saveLastRegisteredFcmTokenHash(tokenHash, cleanDeviceId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                registrationOwner.commitIfCurrent(ownerJob) {
                    repository.clearLastRegisteredFcmTokenHash()
                    repository.recordAppLog(
                        category = "notification",
                        level = "error",
                        message = "fcm_token_registration_failed",
                        errorType = error::class.java.simpleName,
                    )
                }
            } finally {
                registrationOwner.clearIfCurrent(ownerJob)
            }
        }
        val staleJob = registrationOwner.replace(job)
        staleJob?.cancel()
        job.start()
    }

    internal fun cancelPendingRegistration(repository: SettingsRepository) {
        val staleJob = registrationOwner.cancelAndClear {
            repository.clearLastRegisteredFcmTokenHash()
        }
        staleJob?.cancel()
    }

    private fun isFirebaseConfigured(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
