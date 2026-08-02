package tv.withaibuild.customiuizer.installers

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.utils.PrefMap
import java.util.AbstractMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests for [SystemServerInstaller] safety boundaries.
 */
class SystemServerInstallerTest {

    private val originalPrefs = PrefMap<String, Any>()

    @Before
    fun setUp() {
        MainModule.mPrefs = originalPrefs
    }

    @After
    fun tearDown() {
        MainModule.mPrefs = PrefMap()
    }

    private fun setThrowingEntrySet(error: Throwable) {
        val prefs = PrefMap<String, Any>()
        val throwingMap = object : AbstractMap<String, Any>() {
            @Suppress("UNCHECKED_CAST")
            override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
                get() = throw error
        }
        val stateField = PrefMap::class.java.getDeclaredField("state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val ref = stateField.get(prefs) as AtomicReference<Map<String, Any>>
        ref.set(throwingMap)
        MainModule.mPrefs = prefs
    }

    @Test
    fun needGlobalActions_rethrowsOutOfMemoryError() {
        setThrowingEntrySet(OutOfMemoryError("test"))
        try {
            SystemServerInstaller.needGlobalActions()
            fail("expected OutOfMemoryError to be rethrown")
        } catch (e: OutOfMemoryError) {
            assertTrue("OutOfMemoryError rethrown", e.message == "test")
        }
    }

    @Test
    fun needGlobalActions_rethrowsStackOverflowError() {
        setThrowingEntrySet(StackOverflowError("test"))
        try {
            SystemServerInstaller.needGlobalActions()
            fail("expected StackOverflowError to be rethrown")
        } catch (e: StackOverflowError) {
            assertTrue("StackOverflowError rethrown", e.message == "test")
        }
    }

    @Test
    fun needGlobalActions_rethrowsInternalError() {
        setThrowingEntrySet(InternalError("test"))
        try {
            SystemServerInstaller.needGlobalActions()
            fail("expected InternalError to be rethrown")
        } catch (e: InternalError) {
            assertTrue("InternalError rethrown", e.message == "test")
        }
    }

    @Test
    fun needGlobalActions_rethrowsThreadDeath() {
        setThrowingEntrySet(ThreadDeath())
        try {
            SystemServerInstaller.needGlobalActions()
            fail("expected ThreadDeath to be rethrown")
        } catch (e: ThreadDeath) {
            // expected
        }
    }

    @Test
    fun needGlobalActions_swallowsRuntimeException() {
        setThrowingEntrySet(RuntimeException("test"))
        assertFalse("runtime exception is logged and path returns false", SystemServerInstaller.needGlobalActions())
    }

    @Test
    fun needGlobalActions_returnsTrueForActionPreference() {
        val prefs = PrefMap<String, Any>()
        prefs["controls_backlong_action"] = 2
        MainModule.mPrefs = prefs
        assertTrue("action preference greater than 1 triggers global actions", SystemServerInstaller.needGlobalActions())
    }

    @Test
    fun needGlobalActions_mediaFallbackTrueWithPlayerApps() {
        val prefs = PrefMap<String, Any>()
        prefs["controls_volumemedia_up"] = 1
        prefs["controls_mediaplayer_apps"] = setOf("com.spotify.music")
        MainModule.mPrefs = prefs
        assertTrue("media key enabled with player apps triggers global actions", SystemServerInstaller.needGlobalActions())
    }

    @Test
    fun needGlobalActions_mediaFallbackFalseWithoutPlayerApps() {
        val prefs = PrefMap<String, Any>()
        prefs["controls_volumemedia_up"] = 1
        prefs["controls_mediaplayer_apps"] = setOf<String>()
        MainModule.mPrefs = prefs
        assertFalse("media key enabled without player apps does not trigger global actions", SystemServerInstaller.needGlobalActions())
    }

    @Test
    fun needGlobalActions_mediaFallbackPreservedWhenPreferencesThrow() {
        val prefs = PrefMap<String, Any>()
        val mediaPlayers = setOf("com.spotify.music")
        val backing = mutableMapOf(
            "pref_key_controls_volumemedia_up" to 1,
            "pref_key_controls_mediaplayer_apps" to mediaPlayers
        )
        val throwingMap = object : MutableMap<String, Any> by backing {
            @Suppress("UNCHECKED_CAST")
            override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
                get() = throw RuntimeException("entry iteration failed")
        }
        val stateField = PrefMap::class.java.getDeclaredField("state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val ref = stateField.get(prefs) as AtomicReference<Map<String, Any>>
        ref.set(throwingMap)
        MainModule.mPrefs = prefs
        assertTrue("media fallback runs even when entrySet threw", SystemServerInstaller.needGlobalActions())
    }
}
