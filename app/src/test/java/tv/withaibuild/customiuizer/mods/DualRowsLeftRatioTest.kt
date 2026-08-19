package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Test

class DualRowsLeftRatioTest {

    @Test
    fun defaultPreservesLegacyFourSixSplit() {
        val (left, right) = SystemUIStatusBarHooks.resolveDualRowsCutoutWeights(4)
        assertEquals(4f, left, 0f)
        assertEquals(6f, right, 0f)
    }

    @Test
    fun clampsToSeekbarBounds() {
        val low = SystemUIStatusBarHooks.resolveDualRowsCutoutWeights(1)
        assertEquals(3f, low.first, 0f)
        assertEquals(7f, low.second, 0f)
        val high = SystemUIStatusBarHooks.resolveDualRowsCutoutWeights(9)
        assertEquals(7f, high.first, 0f)
        assertEquals(3f, high.second, 0f)
    }
}
