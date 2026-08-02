package tv.withaibuild.customiuizer.mods.catalog

import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks
import tv.withaibuild.customiuizer.mods.SystemDisplayAndWindowHooks.AutoBrightnessVariant
import tv.withaibuild.customiuizer.mods.diagnostics.DiagnosticRecorder
import tv.withaibuild.customiuizer.mods.diagnostics.InstallOutcome
import tv.withaibuild.customiuizer.mods.utils.HookInstaller
import tv.withaibuild.customiuizer.mods.utils.HookOperation
import tv.withaibuild.customiuizer.mods.utils.HookTargetResolver
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

class AutoBrightnessVariantExecutionTest {

    private data class Call(
        val type: String,
        val className: String,
        val memberName: String?
    )

    @After
    fun reset() {
        SystemDisplayAndWindowHooks.autoBrightnessInstallerForTest = null
        DiagnosticRecorder.reset()
        FeatureInstallRegistry.clear()
    }

    private fun runtimeFor(resolvedAbc: Boolean, resolvedDpc: Boolean): FeatureRuntime {
        val classLoader = this.javaClass.classLoader!!
        val resolver = object : HookTargetResolver(classLoader) {
            override fun resolveClass(className: String, diagnosticId: String): Class<*>? {
                return when {
                    resolvedAbc && className.endsWith("AutomaticBrightnessController") ->
                        com.android.server.display.AutomaticBrightnessController::class.java
                    resolvedDpc && className.endsWith("DisplayPowerController") ->
                        com.android.server.display.DisplayPowerController::class.java
                    else -> null
                }
            }

            override fun resolveMethod(
                className: String,
                methodName: String,
                vararg parameterTypes: Class<*>,
                diagnosticId: String
            ): java.lang.reflect.Method? {
                val clazz = resolveClass(className, diagnosticId) ?: return null
                return runCatching {
                    clazz.getDeclaredMethod(methodName, *parameterTypes)
                }.getOrNull()
            }

            override fun resolveAllConstructors(
                className: String,
                diagnosticId: String
            ): List<java.lang.reflect.Constructor<*>>? {
                val clazz = resolveClass(className, diagnosticId) ?: return null
                return clazz.declaredConstructors.toList()
            }
        }

        val lpparam = Proxy.newProxyInstance(
            classLoader,
            arrayOf(XposedModuleInterface.SystemServerStartingParam::class.java)
        ) { _, _, _ -> null } as XposedModuleInterface.SystemServerStartingParam

        val prefs = PrefMap<String, Any>().apply {
            put("system_autobrightness", true)
        }
        return FeatureRuntime("android", lpparam, classLoader, prefs).apply {
            resolverForTest = resolver
        }
    }

    private fun recordingSeam(): Pair<MutableList<Call>, SystemDisplayAndWindowHooks.AutoBrightnessInstaller> {
        val calls = mutableListOf<Call>()
        val seam = object : SystemDisplayAndWindowHooks.AutoBrightnessInstaller {
            override fun loadBacklightRange() {}
            override fun installExactMethod(
                className: String,
                methodName: String,
                parameterType: Class<*>,
                hook: tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
            ) {
                calls.add(Call("method", className, methodName))
                HookInstaller.recordInstall(className, methodName, HookOperation.EXACT_METHOD, listOf(parameterType), 1)
            }

            override fun installAllConstructors(
                className: String,
                hook: tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
            ) {
                calls.add(Call("constructors", className, null))
                HookInstaller.recordInstall(className, null, HookOperation.ALL_CONSTRUCTORS, emptyList(), 1)
            }
        }
        return calls to seam
    }

    @Test
    fun abcSelected_onlyAbcHooksInstalled() {
        val (calls, seam) = recordingSeam()
        SystemDisplayAndWindowHooks.autoBrightnessInstallerForTest = seam
        val runtime = runtimeFor(resolvedAbc = true, resolvedDpc = false)

        val result = FeatureDispatcher.installById("autoBrightnessRange", runtime)
        assertTrue(result)

        assertEquals(setOf(
            Call("method", "com.android.server.display.AutomaticBrightnessController", "clampScreenBrightness"),
            Call("constructors", "com.android.server.display.AutomaticBrightnessController", null)
        ), calls.toSet())
        assertFalse(calls.any { it.className.contains("DisplayPowerController") })
    }

    @Test
    fun dpcSelected_onlyDpcHooksInstalled() {
        val (calls, seam) = recordingSeam()
        SystemDisplayAndWindowHooks.autoBrightnessInstallerForTest = seam
        val runtime = runtimeFor(resolvedAbc = false, resolvedDpc = true)

        val result = FeatureDispatcher.installById("autoBrightnessRange", runtime)
        assertTrue(result)

        assertEquals(setOf(
            Call("method", "com.android.server.display.DisplayPowerController", "clampScreenBrightness"),
            Call("constructors", "com.android.server.display.DisplayPowerController", null)
        ), calls.toSet())
        assertFalse(calls.any { it.className.contains("AutomaticBrightnessController") })
    }

    @Test
    fun partialBundle_noHooksInstalled() {
        val (calls, seam) = recordingSeam()
        SystemDisplayAndWindowHooks.autoBrightnessInstallerForTest = seam
        val runtime = runtimeFor(resolvedAbc = true, resolvedDpc = false)
        // Resolve only the clamp method by making the constructor target unresolvable.
        val resolver = object : HookTargetResolver(runtime.classLoader) {
            override fun resolveMethod(
                className: String,
                methodName: String,
                vararg parameterTypes: Class<*>,
                diagnosticId: String
            ): java.lang.reflect.Method? {
                return if (methodName == "clampScreenBrightness")
                    com.android.server.display.AutomaticBrightnessController::class.java
                        .getDeclaredMethod("clampScreenBrightness", Float::class.javaPrimitiveType)
                else null
            }

            override fun resolveAllConstructors(
                className: String,
                diagnosticId: String
            ): List<java.lang.reflect.Constructor<*>>? = null

            override fun resolveClass(className: String, diagnosticId: String): Class<*>? =
                if (className.endsWith("AutomaticBrightnessController"))
                    com.android.server.display.AutomaticBrightnessController::class.java
                else null
        }
        runtime.resolverForTest = resolver

        val result = FeatureDispatcher.installById("autoBrightnessRange", runtime)
        assertFalse(result)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun abcInstallFailure_doesNotSwitchToDpc() {
        val (calls, seam) = recordingSeam()
        val failingSeam = object : SystemDisplayAndWindowHooks.AutoBrightnessInstaller by seam {
            override fun installExactMethod(
                className: String,
                methodName: String,
                parameterType: Class<*>,
                hook: tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
            ) {
                calls.add(Call("method", className, methodName))
                if (className.endsWith("AutomaticBrightnessController")) {
                    throw IllegalStateException("simulated ABC failure")
                }
                HookInstaller.recordInstall(className, methodName, HookOperation.EXACT_METHOD, listOf(parameterType), 1)
            }
        }
        SystemDisplayAndWindowHooks.autoBrightnessInstallerForTest = failingSeam
        val runtime = runtimeFor(resolvedAbc = true, resolvedDpc = true)

        val result = FeatureDispatcher.installById("autoBrightnessRange", runtime)
        assertFalse(result)
        assertTrue(calls.any { it.className.endsWith("AutomaticBrightnessController") })
        assertFalse(calls.any { it.className.endsWith("DisplayPowerController") })
    }

    @Test
    fun businessInstallerOom_propagatesAndCleansSession() {
        val (_, seam) = recordingSeam()
        val oomSeam = object : SystemDisplayAndWindowHooks.AutoBrightnessInstaller by seam {
            override fun installExactMethod(
                className: String,
                methodName: String,
                parameterType: Class<*>,
                hook: tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
            ) {
                throw OutOfMemoryError("seam oom")
            }
        }
        SystemDisplayAndWindowHooks.autoBrightnessInstallerForTest = oomSeam
        val runtime = runtimeFor(resolvedAbc = true, resolvedDpc = true)

        try {
            FeatureDispatcher.installById("autoBrightnessRange", runtime)
            fail("expected OOM to propagate")
        } catch (oom: OutOfMemoryError) {
            assertEquals("seam oom", oom.message)
        }
        assertFalse(tv.withaibuild.customiuizer.mods.utils.HookInstaller.isRecording())
    }


}
