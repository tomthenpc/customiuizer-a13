package tv.withaibuild.customiuizer.subs

import android.content.Intent
import tv.withaibuild.customiuizer.SubFragment
import tv.withaibuild.customiuizer.utils.SortableListView
import org.junit.Assert.*
import org.junit.Test

class B2_2_MigrationInteropTest {

    @Test
    fun `SortableList can be constructed and extends SubFragment`() {
        val clazz = SortableList::class.java
        assertTrue(SubFragment::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `SortableList uses SortableListView mSnapshotShadow reflection`() {
        val viewClass = SortableListView::class.java
        val field = viewClass.getDeclaredField("mSnapshotShadow")
        assertEquals("mSnapshotShadow", field.name)
        assertTrue(java.lang.reflect.Modifier.isPrivate(field.modifiers))
    }

    @Test
    fun `SortableList preserves lifecycle and action methods`() {
        val clazz = SortableList::class.java

        val onCreate = clazz.getDeclaredMethod("onCreate", android.os.Bundle::class.java)
        assertNotNull(onCreate)

        val onActivityCreated = clazz.getDeclaredMethod("onActivityCreated", android.os.Bundle::class.java)
        assertNotNull(onActivityCreated)

        val onOptionsItemSelected = clazz.getDeclaredMethod("onOptionsItemSelected", android.view.MenuItem::class.java)
        assertNotNull(onOptionsItemSelected)

        val onPrepareOptionsMenu = clazz.getDeclaredMethod("onPrepareOptionsMenu", android.view.Menu::class.java)
        assertNotNull(onPrepareOptionsMenu)

        val onActivityResult = clazz.getDeclaredMethod(
            "onActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Intent::class.java
        )
        assertNotNull(onActivityResult)
    }
}
