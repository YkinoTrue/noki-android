package com.noki.vpn.data

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMarkersStoreConcurrencyTest {
    @Test
    fun `concurrent callers retain both notification markers`() {
        val preferences = coordinatedPreferences()
        val firstStore = SettingsMarkersStore(preferences.preferences, appVersionCode = { 1L })
        val secondStore = SettingsMarkersStore(preferences.preferences, appVersionCode = { 1L })
        val firstFailure = AtomicReference<Throwable?>()
        val secondFailure = AtomicReference<Throwable?>()

        val first = Thread {
            runCatching { firstStore.markAppNotificationsSeen(listOf("first")) }
                .exceptionOrNull()
                ?.let(firstFailure::set)
        }
        first.start()
        assertTrue("first caller must read the empty marker set", preferences.awaitFirstRead())

        val second = Thread {
            runCatching { secondStore.markAppNotificationsSeen(listOf("second")) }
                .exceptionOrNull()
                ?.let(secondFailure::set)
        }
        second.start()

        try {
            assertTrue(
                "both callers must read before either writes unless the store serializes them",
                preferences.awaitSecondReadOrSecondBlocked(second),
            )
        } finally {
            preferences.releaseReads()
            first.join(TimeUnit.SECONDS.toMillis(10))
            second.join(TimeUnit.SECONDS.toMillis(10))
        }

        assertFalse("first caller must finish", first.isAlive)
        assertFalse("second caller must finish", second.isAlive)
        assertNull(firstFailure.get())
        assertNull(secondFailure.get())
        assertEquals(setOf("first", "second"), firstStore.loadSeenAppNotificationIds())
    }

    private fun coordinatedPreferences(): CoordinatedPreferences {
        val values = mutableMapOf<String, Set<String>>()
        val lock = Any()
        val firstRead = CountDownLatch(1)
        val bothRead = CountDownLatch(1)
        val releaseReads = CountDownLatch(1)
        var readCount = 0
        val preferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getStringSet" -> {
                    val value = synchronized(lock) {
                        readCount += 1
                        if (readCount == 1) firstRead.countDown()
                        if (readCount == 2) bothRead.countDown()
                        values[args!![0] as String]?.toSet()
                    }
                    check(releaseReads.await(10, TimeUnit.SECONDS)) {
                        "timed out waiting for both notification-marker reads"
                    }
                    value ?: args!![1]
                }

                "edit" -> editor(values, lock)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as SharedPreferences
        return CoordinatedPreferences(
            preferences = preferences,
            awaitFirstRead = { firstRead.await(10, TimeUnit.SECONDS) },
            awaitSecondReadOrSecondBlocked = { second ->
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
                while (bothRead.count > 0 && second.state != Thread.State.BLOCKED) {
                    if (System.nanoTime() >= deadline) return@CoordinatedPreferences false
                    Thread.yield()
                }
                true
            },
            releaseReads = releaseReads::countDown,
        )
    }

    private fun editor(
        values: MutableMap<String, Set<String>>,
        lock: Any,
    ): SharedPreferences.Editor {
        val pending = mutableMapOf<String, Set<String>>()
        return Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java),
        ) { proxy, method, args ->
            when (method.name) {
                "putStringSet" -> {
                    pending[args!![0] as String] = (args[1] as Set<*>).filterIsInstance<String>().toSet()
                    proxy
                }

                "apply" -> synchronized(lock) { values.putAll(pending) }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as SharedPreferences.Editor
    }

    private data class CoordinatedPreferences(
        val preferences: SharedPreferences,
        val awaitFirstRead: () -> Boolean,
        val awaitSecondReadOrSecondBlocked: (Thread) -> Boolean,
        val releaseReads: () -> Unit,
    )
}
