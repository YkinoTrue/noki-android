package com.noki.vpn.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnServiceStartCommandPolicyTest {
    @Test
    fun ownedDelayedCallbackCoalescesSchedulesAndCancelsByStableIdentity() {
        val scheduled = mutableListOf<Runnable>()
        val callback = OwnedDelayedCallback {}
        val remove: (Runnable) -> Unit = { scheduled.remove(it) }
        val post: (Runnable) -> Unit = { scheduled.add(it) }

        callback.schedule(remove, post)
        callback.schedule(remove, post)

        assertEquals(1, scheduled.size)
        assertSame(callback.runnable, scheduled.single())

        callback.cancel(remove)
        assertTrue(scheduled.isEmpty())
    }

    @Test
    fun startDuringStopIsQueuedUntilCleanupCompletes() {
        assertEquals(
            VpnServiceStartCommandPolicy.ActiveStartDecision.QueueAfterCleanup,
            VpnServiceStartCommandPolicy.activeStartDecision(
                VpnServiceStartCommandPolicy.ActiveOperation.Stop,
            ),
        )
    }

    @Test
    fun startDuringRestartCoalescesWithRestartStart() {
        assertEquals(
            VpnServiceStartCommandPolicy.ActiveStartDecision.CoalesceWithRestart,
            VpnServiceStartCommandPolicy.activeStartDecision(
                VpnServiceStartCommandPolicy.ActiveOperation.Restart,
            ),
        )
    }

    @Test
    fun systemAlwaysOnIsFreshFirstWithCachedFallback() {
        val options = VpnServiceStartCommandPolicy.startOptions(
            isNullIntent = false,
            action = "android.net.VpnService",
            refreshSessionExtra = false,
        )

        assertTrue(options.forceRefreshSession)
        assertTrue(options.allowCachedFallback)
        assertEquals(VpnRuntimeMode.ACCOUNT, options.runtimeMode)
    }

    @Test
    fun stickyRestartIsFreshFirstWithCachedFallback() {
        val options = VpnServiceStartCommandPolicy.startOptions(
            isNullIntent = true,
            action = null,
            refreshSessionExtra = false,
        )

        assertTrue(options.forceRefreshSession)
        assertTrue(options.allowCachedFallback)
        assertEquals(VpnRuntimeMode.ACCOUNT, options.runtimeMode)
    }

    @Test
    fun manualRefreshUsesCachedProfileWhenFreshBackendIsTransientlyUnavailable() {
        val options = VpnServiceStartCommandPolicy.startOptions(
            isNullIntent = false,
            action = "com.noki.vpn.START",
            refreshSessionExtra = true,
        )

        assertTrue(options.forceRefreshSession)
        assertTrue(options.allowCachedFallback)
        assertEquals(VpnRuntimeMode.ACCOUNT, options.runtimeMode)
    }

    @Test
    fun temporaryStartUsesIsolatedAuthTempRuntime() {
        val options = VpnServiceStartCommandPolicy.startOptions(
            isNullIntent = false,
            action = "com.noki.vpn.START_TEMPORARY",
            refreshSessionExtra = false,
        )

        assertEquals(VpnRuntimeMode.AUTH_TEMP, options.runtimeMode)
        assertTrue(options.forceRefreshSession)
        assertTrue(!options.allowCachedFallback)
    }

    @Test
    fun stopAndRevokeTemporaryStopsOnlyAuthTempRuntime() {
        assertEquals(
            VpnServiceStartCommandPolicy.TemporaryStopDecision.StopAndRevoke,
            VpnServiceStartCommandPolicy.temporaryStopDecision(
                action = "com.noki.vpn.STOP_AND_REVOKE_AUTH_TEMP",
                runtimeMode = VpnRuntimeMode.AUTH_TEMP,
            ),
        )
        assertEquals(
            VpnServiceStartCommandPolicy.TemporaryStopDecision.RevokeOnly,
            VpnServiceStartCommandPolicy.temporaryStopDecision(
                action = "com.noki.vpn.STOP_AND_REVOKE_AUTH_TEMP",
                runtimeMode = VpnRuntimeMode.ACCOUNT,
            ),
        )
        assertEquals(
            VpnServiceStartCommandPolicy.TemporaryStopDecision.NotRequested,
            VpnServiceStartCommandPolicy.temporaryStopDecision(
                action = "com.noki.vpn.STOP",
                runtimeMode = VpnRuntimeMode.AUTH_TEMP,
            ),
        )
    }
}
