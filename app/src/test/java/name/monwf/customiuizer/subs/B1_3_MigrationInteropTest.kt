package name.monwf.customiuizer.subs

import android.content.Intent
import name.monwf.customiuizer.SubFragment
import org.junit.Assert.*
import org.junit.Test

class B1_3_MigrationInteropTest {

    @Test
    fun `MultiAction can be constructed and extends SubFragment`() {
        val clazz = MultiAction::class.java
        assertTrue(SubFragment::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        val instance = constructor.newInstance()
        assertNotNull(instance)

        val actionsClass = MultiAction.Actions::class.java
        assertTrue(actionsClass.isEnum)
        assertEquals(6, actionsClass.enumConstants?.size)
        assertNotNull(MultiAction.Actions.LAUNCHER)
    }

    @Test
    fun `MultiAction preserves lifecycle and action methods`() {
        val clazz = MultiAction::class.java

        val onActivityCreated = clazz.getDeclaredMethod(
            "onActivityCreated",
            android.os.Bundle::class.java
        )
        assertNotNull(onActivityCreated)

        val onActivityResult = clazz.getDeclaredMethod(
            "onActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Intent::class.java
        )
        assertNotNull(onActivityResult)

        val saveSharedPrefs = clazz.getDeclaredMethod("saveSharedPrefs")
        assertNotNull(saveSharedPrefs)

        val onDestroy = clazz.getDeclaredMethod("onDestroy")
        assertNotNull(onDestroy)
    }
}
