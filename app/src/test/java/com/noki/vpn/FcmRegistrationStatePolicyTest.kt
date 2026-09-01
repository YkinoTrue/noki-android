package com.noki.vpn

import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class FcmRegistrationStatePolicyTest {
    @Test
    fun authenticatedDeviceWithCurrentTokenAlwaysSyncs() {
        assertTrue(
            FcmRegistrationStatePolicy.shouldSyncCurrentToken(
                accessToken = "access",
                deviceId = "device",
                fcmToken = "fcm",
            ),
        )
        assertFalse(FcmRegistrationStatePolicy.shouldSyncCurrentToken(null, "device", "fcm"))
        assertFalse(FcmRegistrationStatePolicy.shouldSyncCurrentToken("access", "", "fcm"))
        assertFalse(FcmRegistrationStatePolicy.shouldSyncCurrentToken("access", "device", " "))
    }

    @Test
    fun onlyActiveCurrentRegistrationMayCommit() {
        val owner = Job()
        val replacement = Job()

        assertTrue(isCurrentFcmRegistration(owner, owner))
        assertFalse(isCurrentFcmRegistration(owner, replacement))
        owner.cancel()
        assertFalse(isCurrentFcmRegistration(owner, owner))
    }

    @Test
    fun logoutClearCannotRaceWithCurrentRegistrationMarkerCommit() {
        val registrationOwner = FcmRegistrationOwner()
        val job = Job()
        val marker = AtomicReference<String?>()
        val commitEntered = CountDownLatch(1)
        val releaseCommit = CountDownLatch(1)
        registrationOwner.replace(job)

        val commitThread = thread {
            registrationOwner.commitIfCurrent(job) {
                marker.set("stale-marker")
                commitEntered.countDown()
                check(releaseCommit.await(1, TimeUnit.SECONDS))
            }
        }
        assertTrue(commitEntered.await(1, TimeUnit.SECONDS))
        val cancelStarted = CountDownLatch(1)
        val cancelThread = thread {
            cancelStarted.countDown()
            registrationOwner.cancelAndClear { marker.set(null) }?.cancel()
        }
        assertTrue(cancelStarted.await(1, TimeUnit.SECONDS))

        releaseCommit.countDown()
        commitThread.join(1_000L)
        cancelThread.join(1_000L)

        assertFalse(commitThread.isAlive)
        assertFalse(cancelThread.isAlive)
        assertNull(marker.get())
        assertFalse(registrationOwner.commitIfCurrent(job) { marker.set("late-marker") })
    }

    @Test
    fun failedRegistrationClearsMarkerAndAuthenticatedPollingRetries() {
        val registrar = File("src/main/java/com/noki/vpn/FcmTokenRegistrar.kt").readText()
        val polling = File("src/main/java/com/noki/vpn/LogNotificationUiActions.kt").readText()

        assertTrue(
            Regex(
                """catch \([^)]*: Exception\) \{[\s\S]*?repository\.clearLastRegisteredFcmTokenHash\(\)[\s\S]*?fcm_token_registration_failed""",
            ).containsMatchIn(registrar),
        )
        assertTrue(registrar.contains("addOnFailureListener"))
        assertTrue(registrar.contains("fcm_token_fetch_failed"))
        assertTrue(
            polling.contains(
                """if (!repository.isFcmPushRegistered()) {
                syncFcmTokenIfAvailable()
            }""",
            ),
        )
    }
}
