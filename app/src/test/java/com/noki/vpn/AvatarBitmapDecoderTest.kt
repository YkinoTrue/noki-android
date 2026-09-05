package com.noki.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)

class AvatarBitmapDecoderTest {
    @Test
    fun validImageSurvivesBoundsPassAndBothStreamsAreClosed() {
        val fixture = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use {
            fixture.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.toByteArray()
        }
        fixture.recycle()
        var opened = 0
        var closed = 0
        val decoded = AvatarBitmapDecoder.decode {
            opened++
            object : java.io.ByteArrayInputStream(bytes) {
                override fun close() { closed++; super.close() }
            }
        }
        try {
            assertEquals(8, decoded.width)
            assertEquals(4, decoded.height)
            assertEquals(2, opened)
            assertEquals(2, closed)
        } finally {
            decoded.recycle()
        }
    }

    @Test
    fun unreadableImageIsRejected() {
        assertThrows(IllegalStateException::class.java) {
            AvatarBitmapDecoder.decode { byteArrayOf(1, 2, 3).inputStream() }
        }
    }

    @Test
    fun missingStreamIsRejected() {
        assertThrows(IllegalStateException::class.java) { AvatarBitmapDecoder.decode { null } }
    }

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
