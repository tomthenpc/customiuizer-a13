package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class B2_7_MigrationInteropTest {

    @Test
    fun `AudioVisualizer can be constructed and extends View`() {
        val clazz = AudioVisualizer::class.java
        assertTrue(View::class.java.isAssignableFrom(clazz))

        assertNotNull(clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java, Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredConstructor(Context::class.java, AttributeSet::class.java))

        val constructor = clazz.getDeclaredConstructor(Context::class.java)
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.modifiers))
    }

    @Test
    fun `AudioVisualizer exposes public enums`() {
        val clazz = AudioVisualizer::class.java

        assertNotNull(clazz.getDeclaredMethod("allZeros", ByteArray::class.java))

        val innerNames = clazz.declaredClasses.map { it.simpleName }
        assertTrue("BarStyle" in innerNames)
        assertTrue("ColorMode" in innerNames)
        assertTrue("RenderType" in innerNames)
    }

    @Test
    fun `AudioVisualizer exposes public state fields`() {
        val clazz = AudioVisualizer::class.java

        val fields = arrayOf(
            "isScreenOn", "showOnCustom", "colorMode", "barStyle", "renderType",
            "glowLevel", "customColor", "showInDrawer", "showWithControllerOnly"
        )
        for (name in fields) {
            val field = clazz.getField(name)
            assertNotNull(field)
            assertTrue(java.lang.reflect.Modifier.isPublic(field.modifiers))
        }
    }

    @Test
    fun `AudioVisualizer preserves rendering and update methods`() {
        val clazz = AudioVisualizer::class.java

        assertNotNull(clazz.getDeclaredMethod("onDraw", android.graphics.Canvas::class.java))
        assertNotNull(clazz.getDeclaredMethod("onSizeChanged", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("onDetachedFromWindow"))
        assertNotNull(clazz.getDeclaredMethod("hasOverlappingRendering"))
        assertNotNull(clazz.getDeclaredMethod("setPlaying", Boolean::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("updateViewState", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("updateScreenOn", Boolean::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("updateMusicArt", Bitmap::class.java))
        assertNotNull(clazz.getDeclaredMethod("setColor", Int::class.javaPrimitiveType))
        assertNotNull(clazz.getDeclaredMethod("setBitmap"))
    }
}
