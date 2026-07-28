package name.monwf.customiuizer.subs

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import name.monwf.customiuizer.SubFragment
import name.monwf.customiuizer.SubFragmentWithSearch
import name.monwf.customiuizer.utils.AppData
import name.monwf.customiuizer.utils.AppDataAdapter
import name.monwf.customiuizer.utils.Helpers
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class B2_8_MigrationInteropTest {

    @Test
    fun `AppDataAdapter extends BaseAdapter and Filterable`() {
        val clazz = AppDataAdapter::class.java
        assertTrue(android.widget.BaseAdapter::class.java.isAssignableFrom(clazz))
        assertTrue(android.widget.Filterable::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `AppDataAdapter preserves constructor signatures`() {
        val clazz = AppDataAdapter::class.java
        val ctx = Context::class.java
        val arr = ArrayList::class.java
        val type = Helpers.AppAdapterType::class.java

        assertNotNull(clazz.getDeclaredConstructor(ctx, arr))
        assertNotNull(clazz.getDeclaredConstructor(ctx, arr, type, String::class.java))
        assertNotNull(clazz.getDeclaredConstructor(ctx, arr, type, String::class.java, java.lang.Boolean.TYPE))
    }

    @Test
    fun `AppDataAdapter preserves public methods`() {
        val clazz = AppDataAdapter::class.java

        assertNotNull(clazz.getDeclaredMethod("getCount"))
        assertNotNull(clazz.getDeclaredMethod("getItem", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("getItemId", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("getView", Int::class.javaPrimitiveType, View::class.java, ViewGroup::class.java))
        assertNotNull(clazz.getDeclaredMethod("getFilter"))
        assertNotNull(clazz.getDeclaredMethod("updateSelectedApps"))
        assertNotNull(clazz.getDeclaredMethod("getItem", Int::class.javaPrimitiveType).returnType == AppData::class.java)
        assertNotNull(Filter::class.java.isAssignableFrom(clazz.getDeclaredMethod("getFilter").returnType))
    }

    @Test
    fun `WiFiList extends SubFragment and has public no-arg constructor`() {
        val clazz = WiFiList::class.java
        assertTrue(SubFragment::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `WiFiList preserves lifecycle and receiver methods`() {
        val clazz = WiFiList::class.java
        val bundle = Bundle::class.java

        assertNotNull(clazz.getDeclaredMethod("onCreate", bundle))
        assertNotNull(clazz.getDeclaredMethod("onActivityCreated", bundle))
        assertNotNull(clazz.getDeclaredMethod("onResume"))
        assertNotNull(clazz.getDeclaredMethod("onPause"))
        assertNotNull(clazz.getDeclaredMethod("onDestroy"))
        assertNotNull(clazz.getDeclaredMethod("registerReceivers"))
        assertNotNull(clazz.getDeclaredMethod("unregisterReceivers"))
        assertNotNull(clazz.getDeclaredMethod("isWiFiReady"))
        assertNotNull(clazz.getDeclaredMethod("isLocationServicesEnabled"))
        assertNotNull(clazz.getDeclaredMethod("updateProgressBar"))

        val innerNames = clazz.declaredClasses.map { it.simpleName }
        assertTrue(innerNames.contains("WiFiAdapter"))
    }

    @Test
    fun `AppSelector extends SubFragmentWithSearch and has public no-arg constructor`() {
        val clazz = AppSelector::class.java
        assertTrue(SubFragmentWithSearch::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `AppSelector preserves lifecycle and navigation methods`() {
        val clazz = AppSelector::class.java
        val bundle = Bundle::class.java

        assertNotNull(clazz.getDeclaredMethod("onCreate", bundle))
        assertNotNull(clazz.getDeclaredMethod("onActivityCreated", bundle))
        assertNotNull(clazz.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, android.content.Intent::class.java))
    }

    @Test
    fun `BTList extends SubFragment and has public no-arg constructor`() {
        val clazz = BTList::class.java
        assertTrue(SubFragment::class.java.isAssignableFrom(clazz))

        val constructor = clazz.getDeclaredConstructor()
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
        assertNotNull(constructor.newInstance())
    }

    @Test
    fun `BTList preserves lifecycle and device methods`() {
        val clazz = BTList::class.java
        val bundle = Bundle::class.java

        assertNotNull(clazz.getDeclaredMethod("onCreate", bundle))
        assertNotNull(clazz.getDeclaredMethod("onActivityCreated", bundle))
        assertNotNull(clazz.getDeclaredMethod("onResume"))
        assertNotNull(clazz.getDeclaredMethod("onPause"))
        assertNotNull(clazz.getDeclaredMethod("onDestroy"))
        assertNotNull(clazz.getDeclaredMethod("registerReceivers"))
        assertNotNull(clazz.getDeclaredMethod("unregisterReceivers"))
        assertNotNull(clazz.getDeclaredMethod("fetchCachedDevices"))
        assertNotNull(clazz.getDeclaredMethod("updateProgressBar"))

        val innerNames = clazz.declaredClasses.map { it.simpleName }
        assertTrue(innerNames.contains("BTAdapter"))
    }
}
