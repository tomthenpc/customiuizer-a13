package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefPairTest {

    @Test
    fun `first returns first segment`() {
        assertEquals("a", PrefPair.first("a|b"))
    }

    @Test
    fun `first returns whole string when no delimiter`() {
        assertEquals("abc", PrefPair.first("abc"))
    }

    @Test
    fun `first returns empty string for leading delimiter`() {
        assertEquals("", PrefPair.first("|b"))
    }

    @Test
    fun `first stops at first delimiter`() {
        assertEquals("a", PrefPair.first("a|b|c"))
    }

    @Test
    fun `second returns second segment`() {
        assertEquals("b", PrefPair.second("a|b"))
    }

    @Test
    fun `second returns empty when no delimiter`() {
        assertEquals("", PrefPair.second("abc"))
    }

    @Test
    fun `second returns empty for trailing delimiter`() {
        assertEquals("", PrefPair.second("a|"))
    }

    @Test
    fun `second stops after first delimiter`() {
        assertEquals("b|c", PrefPair.second("a|b|c"))
    }

    @Test
    fun `firstEquals matches case-insensitively`() {
        assertTrue(PrefPair.firstEquals("A|b", "a"))
        assertTrue(PrefPair.firstEquals("a|b", "A"))
    }

    @Test
    fun `firstEquals mismatches different length`() {
        assertFalse(PrefPair.firstEquals("abc|b", "a"))
    }

    @Test
    fun `firstEquals handles no delimiter`() {
        assertTrue(PrefPair.firstEquals("abc", "ABC"))
    }

    @Test
    fun `firstEquals handles leading delimiter`() {
        assertTrue(PrefPair.firstEquals("|b", ""))
    }

    @Test
    fun `firstEquals handles multiple delimiters`() {
        assertTrue(PrefPair.firstEquals("a|b|c", "A"))
    }

    @Test
    fun `containsFirst finds matching first segment`() {
        val set = setOf("a|1", "b|2", "c|3")
        assertTrue(PrefPair.containsFirst(set, "b"))
        assertTrue(PrefPair.containsFirst(set, "B"))
    }

    @Test
    fun `containsFirst returns false for missing first segment`() {
        val set = setOf("a|1", "b|2")
        assertFalse(PrefPair.containsFirst(set, "z"))
    }

    @Test
    fun `containsFirst handles null and empty sets`() {
        assertFalse(PrefPair.containsFirst(null, "a"))
        assertFalse(PrefPair.containsFirst(emptySet(), "a"))
    }
}
