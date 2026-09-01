package com.noki.vpn

import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidUpdateOperationOwnershipTest {
    @Test
    fun currentActiveJobAndSessionOwnOperation() {
        val owner = Job()

        assertTrue(
            isCurrentAndroidUpdateOperation(
                owner = owner,
                current = owner,
                isSessionCurrent = true,
            ),
        )
    }

    @Test
    fun replacementCancelledJobAndStaleSessionLoseOwnership() {
        val owner = Job()
        val replacement = Job()

        assertFalse(
            isCurrentAndroidUpdateOperation(
                owner = owner,
                current = replacement,
                isSessionCurrent = true,
            ),
        )

        owner.cancel()
        assertFalse(
            isCurrentAndroidUpdateOperation(
                owner = owner,
                current = owner,
                isSessionCurrent = true,
            ),
        )

        assertFalse(
            isCurrentAndroidUpdateOperation(
                owner = replacement,
                current = replacement,
                isSessionCurrent = false,
            ),
        )
    }

    @Test
    fun updateRevisionAdvancesAndWrapsWithoutReusingTheMaximumValue() {
        assertTrue(nextAndroidUpdateRevision(7L) == 8L)
        assertTrue(nextAndroidUpdateRevision(Long.MAX_VALUE) == 0L)
    }
}
