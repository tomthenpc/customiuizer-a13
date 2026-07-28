package name.monwf.customiuizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import name.monwf.customiuizer.subs.MultiAction
import name.monwf.customiuizer.utils.Helpers
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class B2_5_6_MigrationInteropTest {

    @Test
    fun `PreferenceFragmentBase can be constructed and extends PreferenceFragmentCompat`() {
        val clazz = PreferenceFragmentBase::class.java
        assertTrue(PreferenceFragmentCompat::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `PreferenceFragmentBase exposes public constants and methods`() {
        val clazz = PreferenceFragmentBase::class.java

        assertNotNull(clazz.getField("PICK_BACKFILE"))
        assertNotNull(clazz.getField("SAVE_BACKFILE"))
        assertNotNull(clazz.getDeclaredMethod("getActionBar"))
        assertNotNull(clazz.getDeclaredMethod("confirmEdit"))
        assertNotNull(clazz.getDeclaredMethod("onOptionsItemSelected", MenuItem::class.java))
        assertNotNull(clazz.getDeclaredMethod("openSubFragment", Fragment::class.java, Bundle::class.java, Helpers.SettingsType::class.java, Helpers.ActionBarType::class.java, String::class.java, Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("openSubFragment", Fragment::class.java, Bundle::class.java, Helpers.SettingsType::class.java, Helpers.ActionBarType::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("showXposedDialog", androidx.appcompat.app.AppCompatActivity::class.java))
        assertNotNull(clazz.getDeclaredMethod("showBackupRestoreDialog"))
    }

    @Test
    fun `SubFragment can be constructed and extends PreferenceFragmentBase`() {
        val clazz = SubFragment::class.java
        assertTrue(PreferenceFragmentBase::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `SubFragment exposes public listener and preference fields`() {
        val clazz = SubFragment::class.java

        val listenerFields = arrayOf(
            "openAppsEdit", "openAppsBWEdit", "openShareEdit", "openOpenWithEdit",
            "openLauncherActions", "openControlsActions", "openNavbarActions",
            "openStatusbarActions", "openLockScreenActions", "openLaunchActions",
            "openActivitiesList", "openColorSelector"
        )
        for (name in listenerFields) {
            val field = clazz.getField(name)
            assertNotNull(field)
            assertTrue(java.lang.reflect.Modifier.isPublic(field.modifiers))
        }

        assertNotNull(clazz.getField("settingTitle"))
        assertNotNull(clazz.getField("padded"))
        assertNotNull(clazz.getField("settingsType"))
        assertNotNull(clazz.getField("abType"))
    }

    @Test
    fun `SubFragment preserves lifecycle and sub navigation methods`() {
        val clazz = SubFragment::class.java

        assertNotNull(clazz.getDeclaredMethod("onCreate", Bundle::class.java))
        assertNotNull(clazz.getDeclaredMethod("onCreatePreferences", Bundle::class.java, String::class.java))
        assertNotNull(clazz.getDeclaredMethod("onCreateView", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java))
        assertNotNull(clazz.getDeclaredMethod("onViewCreated", View::class.java, Bundle::class.java))
        assertNotNull(clazz.getDeclaredMethod("onActivityCreated", Bundle::class.java))
        assertNotNull(clazz.getDeclaredMethod("onStart"))
        assertNotNull(clazz.getDeclaredMethod("saveSharedPrefs"))
        assertNotNull(clazz.getDeclaredMethod("loadSharedPrefs"))
        assertNotNull(clazz.getDeclaredMethod("openApps", String::class.java))
        assertNotNull(clazz.getDeclaredMethod("openAppsBW", String::class.java))
        assertNotNull(clazz.getDeclaredMethod("doOpenColorSelector", Preference::class.java))
        assertNotNull(clazz.getDeclaredMethod("openMultiAction", Preference::class.java, MultiAction.Actions::class.java))
        assertNotNull(clazz.getDeclaredMethod("selectSub"))
        assertNotNull(clazz.getDeclaredMethod("finish"))
        assertNotNull(clazz.getDeclaredMethod("confirmEdit"))
    }
}
