package tv.withaibuild.customiuizer.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppHelperTest {

    @After
    fun tearDown() {
        // Ensure the queue is empty after each test so they do not interfere.
        AppHelper.flushRemotePreferenceEdits(null)
    }

    @Test
    fun queueRemotePreferenceEditIsFlushedInOrder() {
        val remote = FakeSharedPreferences()

        AppHelper.queueRemotePreferenceEdit("pref_key_foo", true)
        AppHelper.queueRemotePreferenceEdit("pref_key_bar", 42)
        AppHelper.queueRemotePreferenceEdit("pref_key_baz", "text")

        AppHelper.flushRemotePreferenceEdits(remote)

        assertEquals(true, remote.getBoolean("pref_key_foo", false))
        assertEquals(42, remote.getInt("pref_key_bar", 0))
        assertEquals("text", remote.getString("pref_key_baz", null))
    }

    @Test
    fun queueRemotePreferenceClearDropsPreClearValues() {
        val remote = FakeSharedPreferences()
        remote.put("pref_key_old", "remove")

        AppHelper.queueRemotePreferenceEdit("pref_key_foo", "x")
        AppHelper.queueRemotePreferenceClear()
        AppHelper.queueRemotePreferenceEdit("pref_key_bar", "y")

        AppHelper.flushRemotePreferenceEdits(remote)

        assertNull(remote.getString("pref_key_foo", null))
        assertEquals("y", remote.getString("pref_key_bar", null))
        assertNull(remote.getString("pref_key_old", null))
    }

    @Test
    fun nullRemoteDoesNotFlush() {
        AppHelper.queueRemotePreferenceEdit("pref_key_foo", true)
        AppHelper.flushRemotePreferenceEdits(null)

        val remote = FakeSharedPreferences()
        AppHelper.flushRemotePreferenceEdits(remote)

        assertEquals(true, remote.getBoolean("pref_key_foo", false))
    }

    @Test
    fun removeIsFlushedAsRemove() {
        val remote = FakeSharedPreferences()
        remote.put("pref_key_foo", "value")

        AppHelper.queueRemotePreferenceEdit("pref_key_foo", null)
        AppHelper.flushRemotePreferenceEdits(remote)

        assertFalse(remote.contains("pref_key_foo"))
        assertNull(remote.getString("pref_key_foo", null))
    }

    @Test
    fun stringSetIsFlushed() {
        val remote = FakeSharedPreferences()
        val set = setOf("a", "b")

        AppHelper.queueRemotePreferenceEdit("pref_key_set", set)
        AppHelper.flushRemotePreferenceEdits(remote)

        @Suppress("UNCHECKED_CAST")
        val stored = remote.getStringSet("pref_key_set", null) as Set<String>
        assertEquals(set, stored)
    }
}
