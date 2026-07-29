package tv.withaibuild.customiuizer

import tv.withaibuild.customiuizer.subs.ActivitySelector
import org.junit.Assert.*
import org.junit.Test

class B2_3_MigrationInteropTest {

    @Test
    fun `SubFragmentWithSearch can be constructed and extends SubFragment`() {
        val clazz = SubFragmentWithSearch::class.java
        assertTrue(SubFragment::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `SubFragmentWithSearch exposes search API methods`() {
        val clazz = SubFragmentWithSearch::class.java

        val setActionModeStyle = clazz.getDeclaredMethod(
            "setActionModeStyle",
            android.view.View::class.java
        )
        assertNotNull(setActionModeStyle)

        val applyFilter = clazz.getDeclaredMethod("applyFilter", String::class.java)
        assertNotNull(applyFilter)

        val onActivityCreated = clazz.getDeclaredMethod(
            "onActivityCreated",
            android.os.Bundle::class.java
        )
        assertNotNull(onActivityCreated)

        val listViewField = clazz.getField("listView")
        assertTrue(java.lang.reflect.Modifier.isPublic(listViewField.modifiers))
    }

    @Test
    fun `ActivitySelector can be constructed and extends SubFragmentWithSearch`() {
        val clazz = ActivitySelector::class.java
        assertTrue(SubFragmentWithSearch::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `ActivitySelector preserves lifecycle methods`() {
        val clazz = ActivitySelector::class.java

        val onCreate = clazz.getDeclaredMethod("onCreate", android.os.Bundle::class.java)
        assertNotNull(onCreate)

        val onActivityCreated = clazz.getDeclaredMethod(
            "onActivityCreated",
            android.os.Bundle::class.java
        )
        assertNotNull(onActivityCreated)
    }
}
