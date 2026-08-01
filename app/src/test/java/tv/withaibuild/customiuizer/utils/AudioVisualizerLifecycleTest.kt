package tv.withaibuild.customiuizer.utils

import android.view.Choreographer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AudioVisualizerLifecycleTest {

    @Test
    fun bandLimitsArePrecomputedWithinTheAvailableFftBins() {
        val bands = floatArrayOf(60f, 250f, 500f, 2_000f, 8_000f, 22_050f)

        val limits = audioVisualizerBandBinLimits(bands, 1_024)

        assertArrayEquals(intArrayOf(2, 6, 12, 47, 186, 512), limits)
        for (index in 1 until limits.size) {
            assertTrue(limits[index - 1] <= limits[index])
        }
        assertTrue(limits.all { it in 1..512 })
    }

    @Test
    fun displayRequiresPlaybackAndEveryViewLifecycleGate() {
        assertTrue(shouldDisplayAudioVisualizer(true, true, true, true))
        assertFalse(shouldDisplayAudioVisualizer(false, true, true, true))
        assertFalse(shouldDisplayAudioVisualizer(true, false, true, true))
        assertFalse(shouldDisplayAudioVisualizer(true, true, false, true))
        assertFalse(shouldDisplayAudioVisualizer(true, true, true, false))
    }

    @Test
    fun onlyTheLatestGenerationCanPublish() {
        val first = 41L
        val latest = first + 1

        assertFalse(isCurrentAudioVisualizerGeneration(first, latest))
        assertTrue(isCurrentAudioVisualizerGeneration(latest, latest))
    }

    @Test
    fun visualizerUsesOneFrameCallbackAndNoPerBandAnimatorArray() {
        val clazz = AudioVisualizer::class.java
        val frameCallbacks = clazz.declaredFields.count {
            Choreographer.FrameCallback::class.java.isAssignableFrom(it.type)
        }

        assertEquals(1, frameCallbacks)
        assertEquals(Runnable::class.java, clazz.getDeclaredField("mFrameRequest").type)
        assertEquals(AtomicBoolean::class.java, clazz.getDeclaredField("mFrameRequestPosted").type)
        assertFalse(clazz.declaredFields.any { it.name == "mValueAnimators" })
        assertFalse(clazz.declaredClasses.any { it.simpleName == "PaletteTask" })
    }
}
