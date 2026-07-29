package tv.withaibuild.customiuizer.subs

import android.os.Bundle
import tv.withaibuild.customiuizer.SubFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class B1MigrationInteropTest {

    private val loader: ClassLoader
        get() = javaClass.classLoader ?: error("ClassLoader unavailable")

    @Test
    fun categorySelectorCanBeInstantiatedAndExtendsSubFragment() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.CategorySelector")
        assertTrue(SubFragment::class.java.isAssignableFrom(cls))
        assertNotNull(cls.getDeclaredConstructor().newInstance())
    }

    @Test
    fun categorySelectorHasExpectedLifecycleMethods() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.CategorySelector")
        cls.getDeclaredMethod("onCreate", Bundle::class.java)
        cls.getDeclaredMethod("onActivityCreated", Bundle::class.java)
    }

    @Test
    fun controlsCanBeInstantiatedAndExtendsSubFragment() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.Controls")
        assertTrue(SubFragment::class.java.isAssignableFrom(cls))
        assertNotNull(cls.getDeclaredConstructor().newInstance())
    }

    @Test
    fun controlsHasExpectedLifecycleMethods() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.Controls")
        cls.getDeclaredMethod("onCreatePreferences", Bundle::class.java, String::class.java)
        cls.getDeclaredMethod("onActivityCreated", Bundle::class.java)
    }

    @Test
    fun launcherCanBeInstantiatedAndExtendsSubFragment() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.Launcher")
        assertTrue(SubFragment::class.java.isAssignableFrom(cls))
        assertNotNull(cls.getDeclaredConstructor().newInstance())
    }

    @Test
    fun launcherHasExpectedLifecycleMethods() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.Launcher")
        cls.getDeclaredMethod("onCreatePreferences", Bundle::class.java, String::class.java)
        cls.getDeclaredMethod("onActivityCreated", Bundle::class.java)
    }

    @Test
    fun colorSelectorCanBeInstantiatedAndExtendsSubFragment() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.ColorSelector")
        assertTrue(SubFragment::class.java.isAssignableFrom(cls))
        assertNotNull(cls.getDeclaredConstructor().newInstance())
    }

    @Test
    fun colorSelectorHasExpectedLifecycleMethods() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.ColorSelector")
        cls.getDeclaredMethod("onCreate", Bundle::class.java)
        cls.getDeclaredMethod("onActivityCreated", Bundle::class.java)
        cls.getDeclaredMethod("onSaveInstanceState", Bundle::class.java)
    }

    @Test
    fun colorSelectorPrivateHelpersRemainReflectable() {
        val cls = Class.forName("tv.withaibuild.customiuizer.subs.ColorSelector")
        cls.getDeclaredMethod("updateSelColor", Int::class.javaPrimitiveType).apply { isAccessible = true }
        cls.getDeclaredMethod("setSelected", Int::class.javaPrimitiveType).apply { isAccessible = true }
    }

    @Test
    fun mainFragmentB1FieldsRetainFqcnTypes() {
        val mainCls = Class.forName("tv.withaibuild.customiuizer.MainFragment", false, loader)
        assertEquals("tv.withaibuild.customiuizer.subs.CategorySelector", mainCls.getDeclaredField("catSelector").type.name)
        assertEquals("tv.withaibuild.customiuizer.subs.Controls", mainCls.getDeclaredField("prefControls").type.name)
        assertEquals("tv.withaibuild.customiuizer.subs.Launcher", mainCls.getDeclaredField("prefLauncher").type.name)
    }

    @Test
    fun subFragmentOpenColorSelectorMethodStillReferencesColorSelector() {
        val preferenceCls = Class.forName("androidx.preference.Preference", false, loader)
        val subFragmentCls = Class.forName("tv.withaibuild.customiuizer.SubFragment", false, loader)
        val method = subFragmentCls.getDeclaredMethod("doOpenColorSelector", preferenceCls)
        assertEquals("void", method.returnType.name)
    }
}
