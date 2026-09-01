package com.noki.vpn.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthButtonClickGateTest {
    @Test
    fun `second click is rejected until animation finishes`() {
        val gate = AuthButtonClickGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())

        gate.finish()

        assertTrue(gate.tryStart())
    }
}
