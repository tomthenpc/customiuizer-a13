package tv.withaibuild.customiuizer.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.StatusBarClockFormatCache
import tv.withaibuild.customiuizer.mods.SystemStatusBarClockAndMoreHooks.replaceClockHourToken

class StatusBarClockFormatCacheTest {

    private fun createCache(): StatusBarClockFormatCache {
        return StatusBarClockFormatCache()
    }

    @Test
    fun resolveFormat_12h_noLeadingZero_noSeconds() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", false, false, false)
        assertEquals("h:mm", result)
    }

    @Test
    fun resolveFormat_24h_noLeadingZero_noSeconds() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", false, true, false)
        assertEquals("H:mm", result)
    }

    @Test
    fun resolveFormat_12h_leadingZero_noSeconds() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", false, false, true)
        assertEquals("hh:mm", result)
    }

    @Test
    fun resolveFormat_24h_leadingZero_noSeconds() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", false, true, true)
        assertEquals("HH:mm", result)
    }

    @Test
    fun resolveFormat_showSeconds_false_noChange() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", false, false, false)
        assertEquals("h:mm", result)
    }

    @Test
    fun resolveFormat_showSeconds_true_appendsSeconds() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", true, false, false)
        assertEquals("h:mm:ss", result)
    }

    @Test
    fun resolveFormat_showSeconds_true_24h_leadingZero() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm", true, true, true)
        assertEquals("HH:mm:ss", result)
    }

    @Test
    fun resolveFormat_sameInput_returnsCachedResult() {
        val cache = createCache()
        val first = cache.resolveFormat("h:mm", false, false, false)
        val second = cache.resolveFormat("h:mm", false, false, false)
        assertEquals(first, second)
        assertSame(first, second)
    }

    @Test
    fun resolveFormat_rawFormatChanged_recomputes() {
        val cache = createCache()
        val first = cache.resolveFormat("h:mm", false, false, false)
        assertEquals("h:mm", first)
        val second = cache.resolveFormat("h:mm a", false, false, false)
        assertEquals("h:mm a", second)
        assertNotSame(first, second)
    }

    @Test
    fun resolveFormat_showSecondsChanged_recomputes() {
        val cache = createCache()
        val first = cache.resolveFormat("h:mm", false, false, false)
        assertEquals("h:mm", first)
        val second = cache.resolveFormat("h:mm", true, false, false)
        assertEquals("h:mm:ss", second)
    }

    @Test
    fun resolveFormat_is24Changed_recomputes() {
        val cache = createCache()
        val first = cache.resolveFormat("h:mm", false, false, false)
        assertEquals("h:mm", first)
        val second = cache.resolveFormat("h:mm", false, true, false)
        assertEquals("H:mm", second)
    }

    @Test
    fun resolveFormat_hourIn2dChanged_recomputes() {
        val cache = createCache()
        val first = cache.resolveFormat("h:mm", false, false, false)
        assertEquals("h:mm", first)
        val second = cache.resolveFormat("h:mm", false, false, true)
        assertEquals("hh:mm", second)
    }

    @Test
    fun resolveFormat_noHourToken_returnsInputUnchanged() {
        val cache = createCache()
        val result = cache.resolveFormat("mm:ss", false, false, false)
        assertEquals("mm:ss", result)
    }

    @Test
    fun resolveFormat_noHourToken_cachedCorrectly() {
        val cache = createCache()
        val first = cache.resolveFormat("mm:ss", false, false, false)
        val second = cache.resolveFormat("mm:ss", false, false, false)
        assertSame(first, second)
    }

    @Test
    fun resolveFormat_amPmFormat_preservesAmPmToken() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm a", false, false, false)
        assertEquals("h:mm a", result)
    }

    @Test
    fun resolveFormat_amPmFormat_24h_replacesHourToken() {
        val cache = createCache()
        val result = cache.resolveFormat("h:mm a", false, true, false)
        assertEquals("H:mm a", result)
    }

    @Test
    fun replaceClockHourToken_existingBehavior_preserved() {
        assertEquals("HH:mm", replaceClockHourToken("h:mm", "HH"))
        assertEquals("H:mm a", replaceClockHourToken("hh:mm a", "H"))
        assertEquals("HH:mm h:ss", replaceClockHourToken("h:mm h:ss", "HH"))
        assertEquals("h mm HH:ss", replaceClockHourToken("h mm hh:ss", "HH"))
        val format = "H:mm a"
        assertSame(format, replaceClockHourToken(format, "HH"))
    }

    @Test
    fun resolveFormat_cacheMissThenHit_provesCaching() {
        val cache = createCache()
        val first = cache.resolveFormat("h:mm", true, true, true)
        val second = cache.resolveFormat("h:mm", true, true, true)
        assertEquals("HH:mm:ss", first)
        assertSame(first, second)
    }

    @Test
    fun resolveFormat_allFourKeysChange_recomputesEachTime() {
        val cache = createCache()
        val r1 = cache.resolveFormat("h:mm", false, false, false)
        val r2 = cache.resolveFormat("h:mm", true, false, false)
        val r3 = cache.resolveFormat("h:mm", true, true, false)
        val r4 = cache.resolveFormat("h:mm", true, true, true)
        assertEquals("h:mm", r1)
        assertEquals("h:mm:ss", r2)
        assertEquals("H:mm:ss", r3)
        assertEquals("HH:mm:ss", r4)
    }
}
