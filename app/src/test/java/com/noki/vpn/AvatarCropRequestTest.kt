package com.noki.vpn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AvatarCropRequestTest {
    private val request = AvatarCropRequest("content://photo", 300f, 200f, 100f, 1f, 0f, 0f)

    @Test
    fun clockwiseRotationMapsLandscapeCornersAroundPreviewCenter() {
        val points = floatArrayOf(0f, 0f, 200f, 100f)
        request.copy(rotationQuarterTurns = 1).imageMatrix(200, 100).mapPoints(points)
        assertArrayEquals(floatArrayOf(200f, 0f, 100f, 200f), points, 0.001f)
    }

    @Test
    fun counterclockwiseRotationIsOppositeAndFourTurnsRestoreOriginal() {
        val points = floatArrayOf(0f, 0f, 200f, 100f)
        request.copy(rotationQuarterTurns = -1).imageMatrix(200, 100).mapPoints(points)
        assertArrayEquals(floatArrayOf(100f, 200f, 200f, 0f), points, 0.001f)
        val original = floatArrayOf(0f, 0f, 200f, 100f)
        val restored = original.clone()
        request.imageMatrix(200, 100).mapPoints(original)
        request.copy(rotationQuarterTurns = 4).imageMatrix(200, 100).mapPoints(restored)
        assertArrayEquals(original, restored, 0.001f)
    }

    @Test
    fun offsetsAreClampedUsingRotatedDimensions() {
        val clamped = request.copy(rotationQuarterTurns = 1, cropOffsetX = 1_000f, cropOffsetY = -1_000f)
            .clampOffsets(200, 100)
        assertEquals(0f, clamped.cropOffsetX, 0.001f)
        assertEquals(-50f, clamped.cropOffsetY, 0.001f)
    }

    @Test
    fun panoramaAlwaysCoversCropAndInvalidGestureValuesAreNormalized() {
        val panorama = request.copy(previewWidthPx = 100f, previewHeightPx = 100f, cropCircleSizePx = 80f,
            cropScale = Float.NaN, cropOffsetX = Float.NaN, cropOffsetY = Float.POSITIVE_INFINITY)
        val points = floatArrayOf(200f, 0f, 200f, 20f)
        panorama.imageMatrix(400, 20).mapPoints(points)
        assertArrayEquals(floatArrayOf(50f, 10f, 50f, 90f), points, 0.001f)
    }
}
