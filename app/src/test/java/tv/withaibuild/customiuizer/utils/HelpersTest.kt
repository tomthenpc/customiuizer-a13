package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpersTest {

    @Test
    fun constrainInt_clampsWithinRange() {
        assertEquals(0, Helpers.constrain(-5, 0, 10))
        assertEquals(10, Helpers.constrain(15, 0, 10))
        assertEquals(5, Helpers.constrain(5, 0, 10))
    }

    @Test
    fun constrainFloat_clampsWithinRange() {
        assertEquals(0.0f, Helpers.constrain(-1.0f, 0.0f, 1.0f), 0.0001f)
        assertEquals(1.0f, Helpers.constrain(2.0f, 0.0f, 1.0f), 0.0001f)
        assertEquals(0.5f, Helpers.constrain(0.5f, 0.0f, 1.0f), 0.0001f)
    }

    @Test
    fun lerpFloat_interpolates() {
        assertEquals(0.0f, Helpers.lerp(0.0f, 10.0f, 0.0f), 0.0001f)
        assertEquals(10.0f, Helpers.lerp(0.0f, 10.0f, 1.0f), 0.0001f)
        assertEquals(5.0f, Helpers.lerp(0.0f, 10.0f, 0.5f), 0.0001f)
    }

    @Test
    fun getVibrationPattern_parsesAndReturnsPattern() {
        val pattern = Helpers.getVibrationPattern("0,100,200,300")
        assertEquals(4, pattern.size.toLong())
        assertEquals(0L, pattern[0])
        assertEquals(100L, pattern[1])
        assertEquals(200L, pattern[2])
        assertEquals(300L, pattern[3])
    }

    @Test
    fun getVibrationPattern_emptyInputReturnsEmpty() {
        val pattern = Helpers.getVibrationPattern("")
        assertEquals(0, pattern.size.toLong())
    }

    @Test
    fun getVibrationPattern_invalidInputReturnsEmpty() {
        val pattern = Helpers.getVibrationPattern("not,a,number")
        assertEquals(0, pattern.size.toLong())
    }

    @Test
    fun containsStringPair_matchesFirstSegmentCaseInsensitive() {
        val hayStack = setOf("pkg1|act1", "pkg2|act2")
        assertTrue(Helpers.containsStringPair(hayStack, "pkg1"))
        assertTrue(Helpers.containsStringPair(hayStack, "PKG1"))
        assertFalse(Helpers.containsStringPair(hayStack, "pkg3"))
    }

    @Test
    fun containsStringPair_nullSetOrNeedleReturnsFalse() {
        assertFalse(Helpers.containsStringPair(null, "pkg1"))
        assertFalse(Helpers.containsStringPair(emptySet(), "pkg1"))
        assertFalse(Helpers.containsStringPair(setOf("pkg1|act1"), null))
    }
}
