package com.noki.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapjackingProtectionPolicyTest {
    @Test
    fun filtersObscuredTouchesThroughAndroidEleven() {
        assertTrue(shouldFilterTouchesWhenObscured(apiLevel = 29))
        assertTrue(shouldFilterTouchesWhenObscured(apiLevel = 30))
    }

    @Test
    fun reliesOnPlatformProtectionFromAndroidTwelve() {
        assertFalse(shouldFilterTouchesWhenObscured(apiLevel = 31))
        assertFalse(shouldFilterTouchesWhenObscured(apiLevel = 36))
    }
}
