package com.noki.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AvatarBitmapDecoderTest {
    @Test
    fun normalAvatarNeedsNoSampling() {
        assertEquals(1, AvatarBitmapDecoder.inSampleSize(1_024, 768))
    }

    @Test
    fun hugeAndExtremeImagesStayInsideDecodedBudget() {
        val squareSample = AvatarBitmapDecoder.inSampleSize(40_000, 40_000)
        val wideSample = AvatarBitmapDecoder.inSampleSize(Int.MAX_VALUE, 1)

        assertTrue(40_000L / squareSample <= 2_048L)
        assertTrue(40_000L / squareSample * (40_000L / squareSample) <= 4_194_304L)
        assertTrue(Int.MAX_VALUE.toLong() / wideSample <= 2_048L)
    }

    @Test
    fun invalidBoundsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AvatarBitmapDecoder.inSampleSize(0, 100)
        }
    }
}
