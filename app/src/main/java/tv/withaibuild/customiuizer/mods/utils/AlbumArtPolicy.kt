package tv.withaibuild.customiuizer.mods.utils

import kotlin.math.sqrt

internal data class AlbumArtSize(val width: Int, val height: Int)

internal data class AlbumArtCacheKey(
    val sourceToken: Long,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val blur: Int,
    val rescale: Int,
    val grayscale: Boolean
)

internal data class AlbumArtCacheSignature(
    val targetWidth: Int,
    val targetHeight: Int,
    val blur: Int,
    val rescale: Int,
    val grayscale: Boolean
)

internal object AlbumArtPolicy {
    const val BLUR_MAX_PIXELS = 512 * 512
    const val CACHE_BUDGET_FRAMES = 1
    private const val BYTES_PER_PIXEL = 4

    fun downsampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        blur: Int,
        rescale: Int
    ): AlbumArtSize {
        if (sourceWidth <= 0 || sourceHeight <= 0) return AlbumArtSize(1, 1)

        var ratio = 1f
        if (targetWidth > 0 && targetHeight > 0) {
            val widthRatio = targetWidth.toFloat() / sourceWidth
            val heightRatio = targetHeight.toFloat() / sourceHeight
            val targetRatio = if (rescale == 3) {
                maxOf(widthRatio, heightRatio)
            } else {
                minOf(widthRatio, heightRatio)
            }
            ratio = minOf(ratio, targetRatio)
        }
        if (blur > 0) {
            val pixels = sourceWidth.toLong() * sourceHeight.toLong()
            if (pixels > BLUR_MAX_PIXELS) {
                ratio = minOf(ratio, sqrt(BLUR_MAX_PIXELS.toFloat() / pixels))
            }
        }

        return AlbumArtSize(
            (sourceWidth * ratio).toInt().coerceAtLeast(1),
            (sourceHeight * ratio).toInt().coerceAtLeast(1)
        )
    }

    fun cacheBudgetBytes(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 0
        val pixels = width.toLong() * height.toLong()
        val bytesPerBudget = BYTES_PER_PIXEL * CACHE_BUDGET_FRAMES
        if (pixels > Int.MAX_VALUE.toLong() / bytesPerBudget) return Int.MAX_VALUE
        return (pixels * bytesPerBudget).toInt()
    }

    fun shouldRebuildCache(
        current: AlbumArtCacheSignature?,
        requested: AlbumArtCacheSignature
    ): Boolean = current != requested

    fun shouldPublish(resultGeneration: Long, currentGeneration: Long): Boolean =
        resultGeneration == currentGeneration

    fun shouldContinue(
        resultGeneration: Long,
        currentGeneration: Long,
        interrupted: Boolean
    ): Boolean = !interrupted && shouldPublish(resultGeneration, currentGeneration)
}
