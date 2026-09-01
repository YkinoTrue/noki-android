package com.noki.vpn.vpn

import com.noki.vpn.data.StoredSettings

internal interface VpnConnectedSidecars {
    fun start(owner: RuntimeOwner, snapshot: StoredSettings)

    fun stop(owner: RuntimeOwner?)
}

internal class OwnedVpnConnectedSidecars(
    private val onStart: (RuntimeOwner, StoredSettings) -> Unit,
    private val onStop: (RuntimeOwner?) -> Unit,
) : VpnConnectedSidecars {
    private var activeOwner: RuntimeOwner? = null

    @Synchronized
    override fun start(owner: RuntimeOwner, snapshot: StoredSettings) {
        if (activeOwner == owner) return
        activeOwner?.let(onStop)
        activeOwner = owner
        onStart(owner, snapshot)
    }

    @Synchronized
    override fun stop(owner: RuntimeOwner?) {
        val current = activeOwner ?: return
        if (owner != null && owner != current) return
        activeOwner = null
        onStop(current)
    }
}
