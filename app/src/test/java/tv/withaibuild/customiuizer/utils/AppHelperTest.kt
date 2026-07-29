package tv.withaibuild.customiuizer.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppHelperTest {

    @Before
    fun setUp() {
        AppHelper.resetMirrorState()
        AppHelper.mirrorIgnoreKeys = setOf(
            "pref_key_miuizer_locale",
            "pref_key_miuizer_launchericon",
            "pref_key_miuizer_synced_from_lsposed"
        )
    }

    @After
    fun tearDown() {
        AppHelper.resetMirrorState()
        AppHelper.appPrefs = null
    }

    @Test
    fun unboundEditsAreSyncedOnBindReconciliation() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_foo", true)
        appPrefs.put("pref_key_bar", 42)
        AppHelper.appPrefs = appPrefs

        AppHelper.onLocalPreferenceChanged(null, "pref_key_foo", true)
        AppHelper.onLocalPreferenceChanged(null, "pref_key_bar", 42)
        assertTrue("changes without remote should mark dirty", AppHelper.mirrorDirty)

        val remote = FakeSharedPreferences()
        AppHelper.reconcileRemotePreferences(remote)

        assertEquals(true, remote.getBoolean("pref_key_foo", false))
        assertEquals(42, remote.getInt("pref_key_bar", 0))
        assertFalse("reconcile success clears dirty", AppHelper.mirrorDirty)
    }

    @Test
    fun reconcileRecoversFromLocalAfterProcessRebuild() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_foo", "value")
        AppHelper.appPrefs = appPrefs

        // Simulate a new remote after process restart without any in-memory queue.
        val remote = FakeSharedPreferences()
        AppHelper.reconcileRemotePreferences(remote)

        assertEquals("value", remote.getString("pref_key_foo", null))
        assertFalse(AppHelper.mirrorDirty)
    }

    @Test
    fun commitFailurePreservesDirtyStateAndSucceedsOnNextBind() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_foo", true)
        AppHelper.appPrefs = appPrefs

        val failingRemote = FakeSharedPreferences()
        failingRemote.commitResult = false

        AppHelper.reconcileRemotePreferences(failingRemote)

        assertFalse("failed commit must not write", failingRemote.contains("pref_key_foo"))
        assertTrue("dirty state is preserved on failure", AppHelper.mirrorDirty)

        val succeedingRemote = FakeSharedPreferences()
        AppHelper.reconcileRemotePreferences(succeedingRemote)

        assertTrue(succeedingRemote.getBoolean("pref_key_foo", false))
        assertFalse(AppHelper.mirrorDirty)
    }

    @Test
    fun incrementalEditRetriesOnNextBindIfCommitFails() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_foo", true)
        AppHelper.appPrefs = appPrefs

        val failingRemote = FakeSharedPreferences()
        failingRemote.commitResult = false

        AppHelper.onLocalPreferenceChanged(failingRemote, "pref_key_foo", true)

        assertTrue(AppHelper.mirrorDirty)
        assertFalse(failingRemote.contains("pref_key_foo"))

        val succeedingRemote = FakeSharedPreferences()
        AppHelper.reconcileRemotePreferences(succeedingRemote)

        assertTrue(succeedingRemote.getBoolean("pref_key_foo", false))
        assertFalse(AppHelper.mirrorDirty)
    }

    @Test
    fun clearThenNewSettingsKeepCorrectOrderAndRemoteOnly() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_old", "x")
        appPrefs.put("pref_key_new", "y")
        AppHelper.appPrefs = appPrefs

        val remote = FakeSharedPreferences()
        remote.put("pref_key_old", "stale")
        remote.put("remote_only_key", "keep")

        // First reconcile establishes state.
        AppHelper.reconcileRemotePreferences(remote)
        assertEquals("y", remote.getString("pref_key_new", null))
        assertEquals("keep", remote.getString("remote_only_key", null))

        // Clear local and add a new key.
        appPrefs.put("pref_key_new", null)
        appPrefs.put("pref_key_old", null)
        appPrefs.put("pref_key_after_clear", "z")

        AppHelper.reconcileRemotePreferences(remote)

        assertNull("old key removed", remote.getString("pref_key_old", null))
        assertNull("removed key stays removed", remote.getString("pref_key_new", null))
        assertEquals("z", remote.getString("pref_key_after_clear", null))
        assertEquals("keep", remote.getString("remote_only_key", null))
    }

    @Test
    fun localOnlyAndRemoteOnlyKeysArePreserved() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_locale", "en")
        appPrefs.put("pref_key_foo", true)
        AppHelper.appPrefs = appPrefs

        AppHelper.setMirrorIgnoreKeys(setOf("pref_key_locale"))

        val remote = FakeSharedPreferences()
        remote.put("pref_key_locale", "zh")
        remote.put("remote_only_key", "keep")
        remote.put("pref_key_foo", false)

        AppHelper.reconcileRemotePreferences(remote)

        assertEquals("zh", remote.getString("pref_key_locale", null))
        assertEquals(true, remote.getBoolean("pref_key_foo", false))
        assertEquals("keep", remote.getString("remote_only_key", null))
    }

    @Test
    fun multipleEditsToSameKeyKeepLastValue() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_foo", "first")
        AppHelper.appPrefs = appPrefs

        val remote = FakeSharedPreferences()

        AppHelper.onLocalPreferenceChanged(remote, "pref_key_foo", "first")
        assertEquals("first", remote.getString("pref_key_foo", null))

        appPrefs.put("pref_key_foo", "last")
        AppHelper.onLocalPreferenceChanged(remote, "pref_key_foo", "last")
        assertEquals("last", remote.getString("pref_key_foo", null))
    }

    @Test
    fun fullClearReconcilesWithoutBlindClear() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_keep", "value")
        AppHelper.appPrefs = appPrefs

        val remote = FakeSharedPreferences()
        remote.put("pref_key_drop", "x")
        remote.put("remote_only_key", "keep")

        // onSharedPreferenceChanged with null key means a full clear.
        AppHelper.onLocalPreferenceChanged(remote, null, null)

        assertNull(remote.getString("pref_key_drop", null))
        assertEquals("value", remote.getString("pref_key_keep", null))
        assertEquals("keep", remote.getString("remote_only_key", null))
    }

    @Test
    fun resetMirrorStateClearsDirty() {
        val appPrefs = FakeSharedPreferences()
        appPrefs.put("pref_key_foo", true)
        AppHelper.appPrefs = appPrefs

        AppHelper.onLocalPreferenceChanged(null, "pref_key_foo", true)
        assertTrue(AppHelper.mirrorDirty)

        AppHelper.resetMirrorState()
        assertFalse(AppHelper.mirrorDirty)
    }
}
