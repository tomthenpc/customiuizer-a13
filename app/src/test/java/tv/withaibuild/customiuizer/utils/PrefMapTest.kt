package tv.withaibuild.customiuizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrefMapTest {

    private lateinit var map: PrefMap<String, Any>

    @Before
    fun setUp() {
        map = PrefMap()
    }

    @Test
    fun getBooleanReturnsTrueAndFalse() {
        map["pref_key_true"] = true
        map["pref_key_false"] = false
        assertTrue(map.getBoolean("true"))
        assertFalse(map.getBoolean("false"))
    }

    @Test
    fun getBooleanParsesStringTrueFalse() {
        map["pref_key_t"] = "true"
        map["pref_key_one"] = "1"
        map["pref_key_f"] = "false"
        map["pref_key_zero"] = "0"
        assertTrue(map.getBoolean("t"))
        assertTrue(map.getBoolean("one"))
        assertFalse(map.getBoolean("f"))
        assertFalse(map.getBoolean("zero"))
    }

    @Test
    fun getBooleanUnknownValueReturnsDefault() {
        map["pref_key_unknown"] = "notABoolean"
        map["pref_key_number"] = 42
        assertFalse(map.getBoolean("unknown"))
        assertFalse(map.getBoolean("number"))
        assertTrue(map.getBoolean("unknown", true))
        assertTrue(map.getBoolean("number", true))
    }

    @Test
    fun getBooleanMissingKeyReturnsDefault() {
        assertFalse(map.getBoolean("missing"))
        assertTrue(map.getBoolean("missing", true))
    }

    @Test
    fun getStringAsIntParsesNumericString() {
        map["pref_key_foo"] = "123"
        assertEquals(123, map.getStringAsInt("foo", 0))
        assertEquals(123, map.getStringAsInt("pref_key_foo", 0))
    }

    @Test
    fun getStringAsIntReturnsDefaultForDamagedString() {
        map["pref_key_foo"] = "not-a-number"
        assertEquals(7, map.getStringAsInt("foo", 7))
    }

    @Test
    fun getStringAsIntReturnsNumberAsInt() {
        map["pref_key_foo"] = 42
        assertEquals(42, map.getStringAsInt("foo", 0))
    }

    @Test
    fun getStringAsIntReturnsDefaultForUnknownType() {
        map["pref_key_foo"] = listOf("x")
        assertEquals(5, map.getStringAsInt("foo", 5))
    }

    @Test
    fun getStringAsIntCachesParseResult() {
        map["pref_key_foo"] = "123"
        val first = map.getStringAsInt("foo", 0)
        val second = map.getStringAsInt("foo", 0)
        assertEquals(first, second)
        // Key is still cached after the first successful parse.
        assertEquals(123, map.getStringAsInt("foo", 0))
    }

    @Test
    fun putInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map["pref_key_foo"] = "456"
        assertEquals(456, map.getStringAsInt("foo", 0))
    }

    @Test
    fun removeInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.remove("pref_key_foo")
        assertEquals(9, map.getStringAsInt("foo", 9))
    }

    @Test
    fun clearInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.clear()
        assertEquals(9, map.getStringAsInt("foo", 9))
    }

    @Test
    fun putAllInvalidatesCache() {
        map["pref_key_foo"] = "123"
        map.getStringAsInt("foo", 0)
        map.putAll(mapOf("pref_key_foo" to "456"))
        assertEquals(456, map.getStringAsInt("foo", 0))
    }
}
