package com.noki.vpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager

internal class VpnScreenStateMonitor(
    private val context: Context,
    private val onScreenStateChanged: (Boolean) -> Unit,
) {
    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> onScreenStateChanged(true)
                Intent.ACTION_SCREEN_OFF -> onScreenStateChanged(false)
            }
        }
    }

    fun isInteractive(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive

    fun start() {
        if (registered) return
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }
}
