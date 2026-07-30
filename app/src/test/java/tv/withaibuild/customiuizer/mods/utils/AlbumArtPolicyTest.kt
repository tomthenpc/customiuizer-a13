package tv.withaibuild.customiuizer.mods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumArtPolicyTest {

    @Test
    fun blurInputIsDownsampledToThePixelLimit() {
        val size = AlbumArtPolicy.downsampleSize(
            sourceWidth = 4_000,
            sourceHeight = 3_000,
            targetWidth = 1_080,
            targetHeight = 2_400,
            blur = 12,
            rescale = 3
        )

        assertTrue(size.width * size.height <= AlbumArtPolicy.BLUR_MAX_PIXELS)
        assertTrue(size.width < 4_000)
        assertTrue(size.height < 3_000)
    }

    @Test
    fun fitAndCoverKeepTheirAndroid13Geometry() {
        val fit = AlbumArtPolicy.downsampleSize(4_000, 3_000, 1_080, 2_400, 0, 2)
        val cover = AlbumArtPolicy.downsampleSize(4_000, 3_000, 1_080, 2_400, 0, 3)

        assertEquals(AlbumArtSize(1_080, 810), fit)
        assertEquals(AlbumArtSize(3_200, 2_400), cover)
    }

    @Test
    fun cacheBudgetIsMeasuredInBytesAndCappedSafely() {
        val oneFrame = 1_080 * 2_400 * 4

        assertEquals(oneFrame * 2, AlbumArtPolicy.cacheBudgetBytes(1_080, 2_400))
        assertEquals(0, AlbumArtPolicy.cacheBudgetBytes(0, 2_400))
        assertEquals(Int.MAX_VALUE, AlbumArtPolicy.cacheBudgetBytes(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun sizeAndProcessingParametersInvalidateTheCacheSignature() {
        val current = AlbumArtCacheSignature(1_080, 2_400, 10, 3, false)

        assertFalse(AlbumArtPolicy.shouldRebuildCache(current, current.copy()))
        assertTrue(
            AlbumArtPolicy.shouldRebuildCache(
                current,
                current.copy(targetHeight = 2_300)
            )
        )
        assertTrue(
            AlbumArtPolicy.shouldRebuildCache(
                current,
                current.copy(blur = 11, grayscale = true)
            )
        )
    }

    @Test
    fun cacheKeyIncludesSourceSizeAndEveryProcessingParameter() {
        val key = AlbumArtCacheKey(7, 600, 600, 1_080, 2_400, 8, 2, false)

        assertNotEquals(key, key.copy(sourceId = 8))
        assertNotEquals(key, key.copy(sourceWidth = 601))
        assertNotEquals(key, key.copy(targetHeight = 2_300))
        assertNotEquals(key, key.copy(blur = 9))
        assertNotEquals(key, key.copy(rescale = 3))
        assertNotEquals(key, key.copy(grayscale = true))
    }

    @Test
    fun onlyLatestGenerationMayPublish() {
        assertFalse(AlbumArtPolicy.shouldPublish(4L, 5L))
        assertTrue(AlbumArtPolicy.shouldPublish(5L, 5L))
    }
}
