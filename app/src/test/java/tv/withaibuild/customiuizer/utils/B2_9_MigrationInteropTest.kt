package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class B2_9_MigrationInteropTest {

    @Test
    fun `LockedAppAdapter extends BaseAdapter and Filterable`() {
        val clazz = LockedAppAdapter::class.java
        assertTrue(BaseAdapter::class.java.isAssignableFrom(clazz))
        assertTrue(Filterable::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `LockedAppAdapter preserves constructor and public methods`() {
        val clazz = LockedAppAdapter::class.java

        assertNotNull(clazz.getDeclaredConstructor(Context::class.java, ArrayList::class.java))
        assertNotNull(clazz.getDeclaredMethod("getCount"))
        assertNotNull(clazz.getDeclaredMethod("getItem", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("getItemId", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("getView", Int::class.javaPrimitiveType, View::class.java, ViewGroup::class.java))
        assertNotNull(clazz.getDeclaredMethod("getFilter"))
        assertNotNull(clazz.getDeclaredMethod("isEnabled", Int::class.javaPrimitiveType))
        assertNotNull(Filter::class.java.isAssignableFrom(clazz.getDeclaredMethod("getFilter").returnType))
    }

    @Test
    fun `PrivacyAppAdapter extends BaseAdapter and Filterable`() {
        val clazz = PrivacyAppAdapter::class.java
        assertTrue(BaseAdapter::class.java.isAssignableFrom(clazz))
        assertTrue(Filterable::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `PrivacyAppAdapter preserves constructor and public methods`() {
        val clazz = PrivacyAppAdapter::class.java

        assertNotNull(clazz.getDeclaredConstructor(Context::class.java, ArrayList::class.java))
        assertNotNull(clazz.getDeclaredMethod("getCount"))
        assertNotNull(clazz.getDeclaredMethod("getItem", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("getItemId", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("getView", Int::class.javaPrimitiveType, View::class.java, ViewGroup::class.java))
        assertNotNull(clazz.getDeclaredMethod("getFilter"))
        assertNotNull(Filter::class.java.isAssignableFrom(clazz.getDeclaredMethod("getFilter").returnType))
    }
}
