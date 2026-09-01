package com.noki.vpn.vpn

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class VpnDestroyCoordinator(
    private val lifecycleMutex: Mutex,
    private val cancelAndJoinActiveTransition: suspend () -> Unit,
    private val releaseResources: () -> Unit,
    private val cancelBackgroundWork: () -> Unit,
) {
    suspend fun destroy() {
        try {
            cancelAndJoinActiveTransition()
            lifecycleMutex.withLock { releaseResources() }
        } finally {
            cancelBackgroundWork()
        }
    }
}
