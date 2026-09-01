package com.noki.vpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.noki.vpn.MainActivity
import com.noki.vpn.data.VpnConnectionState
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VpnForegroundResyncInstrumentedTest {
    @Test
    fun notificationStopThenForegroundQueryReportsDisconnected() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val states = LinkedBlockingQueue<String>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                states.offer(intent?.getStringExtra(AppVpnService.EXTRA_STATE).orEmpty())
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AppVpnService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                assertEquals(VpnConnectionState.DISCONNECTED.name, states.awaitState())

                scenario.onActivity { activity ->
                    activity.startService(AppVpnService.stopIntent(activity))
                }
                assertEquals(VpnConnectionState.DISCONNECTED.name, states.awaitState())

                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                assertEquals(VpnConnectionState.DISCONNECTED.name, states.awaitState())
            }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    private fun LinkedBlockingQueue<String>.awaitState(): String? =
        poll(10, TimeUnit.SECONDS)
}
