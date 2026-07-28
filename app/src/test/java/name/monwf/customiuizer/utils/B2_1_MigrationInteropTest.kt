package name.monwf.customiuizer.utils

import android.widget.ListView
import org.junit.Assert.*
import org.junit.Test

class B2_1_MigrationInteropTest {

    @Test
    fun `SortableListView can be constructed and extends ListView`() {
        val clazz = SortableListView::class.java
        assertTrue(ListView::class.java.isAssignableFrom(clazz))

        val constructor1 = clazz.getConstructor(android.content.Context::class.java)
        assertNotNull(constructor1)

        val constructor2 = clazz.getConstructor(
            android.content.Context::class.java,
            android.util.AttributeSet::class.java
        )
        assertNotNull(constructor2)

        val constructor3 = clazz.getConstructor(
            android.content.Context::class.java,
            android.util.AttributeSet::class.java,
            Int::class.javaPrimitiveType
        )
        assertNotNull(constructor3)
    }

    @Test
    fun `SortableListView keeps mSnapshotShadow field for SortableList reflection`() {
        val clazz = SortableListView::class.java
        val field = clazz.getDeclaredField("mSnapshotShadow")
        assertEquals("mSnapshotShadow", field.name)
        assertTrue(java.lang.reflect.Modifier.isPrivate(field.modifiers))
    }

    @Test
    fun `SortableListView exposes listener interface and expected methods`() {
        val clazz = SortableListView::class.java
        val listener = clazz.getDeclaredMethod(
            "setOnOrderChangedListener",
            SortableListView.OnOrderChangedListener::class.java
        )
        assertNotNull(listener)

        val createAnimation = clazz.getDeclaredMethod(
            "createAnimation",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        assertNotNull(createAnimation)

        val getHitten = clazz.getDeclaredMethod("getHittenItemPosition", android.view.MotionEvent::class.java)
        assertNotNull(getHitten)

        val setItemUpperBound = clazz.getDeclaredMethod(
            "setItemUpperBound",
            Int::class.javaPrimitiveType,
            android.graphics.drawable.Drawable::class.java
        )
        assertNotNull(setItemUpperBound)

        val getListener = clazz.getDeclaredMethod("getListenerForStartingSort")
        assertNotNull(getListener)

        val dispatchDraw = clazz.getDeclaredMethod("dispatchDraw", android.graphics.Canvas::class.java)
        assertNotNull(dispatchDraw)
    }
}
