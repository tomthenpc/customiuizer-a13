package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.content.FakeIntent
import android.content.Intent
import android.view.View
import com.android.systemui.Dependency
import com.android.systemui.controlcenter.policy.ControlCenterControllerImpl
import com.android.systemui.keyguard.KeyguardViewMediator
import com.android.systemui.qs.QSTileHost
import com.android.systemui.qs.tileimpl.MiuiQSFactory
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.qs.tiles.FakeNfcTile
import com.android.systemui.qs.tiles.FakeWifiTile
import com.android.systemui.statusbar.phone.CentralSurfaces
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.LinkedHashMap

class SecureQSTilesHookTest {

    private val parentClassLoader: ClassLoader
        get() = this.javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        Dependency.clear()
        resetSecuredTiles()
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
        Dependency.clear()
        resetSecuredTiles()
    }

    private fun resetSecuredTiles() {
        val field = SystemUILockScreenHooks::class.java.getDeclaredField("securedTiles")
        field.isAccessible = true
        (field.get(null) as? java.util.ArrayList<*>)?.clear()
    }

    private fun lpparam(): XposedModuleInterface.PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(XposedModuleInterface.PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> "com.android.systemui"
                "getProcessName" -> "com.android.systemui"
                "getClassLoader" -> parentClassLoader
                else -> null
            }
        } as XposedModuleInterface.PackageReadyParam
    }

    private fun installHook(prefs: PrefMap<String, Any>) {
        MainModule.mPrefs = prefs
        SystemUILockScreenHooks.SecureQSTilesHook(lpparam())
    }

    private fun fakeBeforeCallback(thisObject: Any?, args: List<Any?> = emptyList()): HookerClassHelper.BeforeHookCallback {
        val chain = FakeChain(null, thisObject, args, null, null)
        val constructor = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredConstructor(XposedInterface.Chain::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(chain)
    }

    private fun fakeAfterCallback(thisObject: Any?, args: List<Any?> = emptyList(), result: Any? = null): HookerClassHelper.AfterHookCallback {
        val before = fakeBeforeCallback(thisObject, args)
        val constructor = HookerClassHelper.AfterHookCallback::class.java.getDeclaredConstructor(
            HookerClassHelper.BeforeHookCallback::class.java,
            Any::class.java,
            Throwable::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(before, result, null)
    }

    private fun isSkipped(callback: HookerClassHelper.BeforeHookCallback): Boolean {
        val field = HookerClassHelper.BeforeHookCallback::class.java.getDeclaredField("skipped")
        field.isAccessible = true
        return field.get(callback) as Boolean
    }

    private fun matchesExecutable(it: FakeXposedInterface.RecordedHook, targetClassName: String, targetMethodName: String): Boolean {
        if (it.executable.declaringClass.name != targetClassName) return false
        if (targetMethodName == "<init>") return it.executable is java.lang.reflect.Constructor<*>
        return it.executable.name == targetMethodName
    }

    private fun getRecordedHook(targetClassName: String, targetMethodName: String): HookerClassHelper.MethodHook {
        val recorded = FakeXposedInterface.recordedHooks.filter { matchesExecutable(it, targetClassName, targetMethodName) }
        assertTrue("expected hook for $targetClassName#$targetMethodName; recorded:\n${allRecordedHooks()}", recorded.isNotEmpty())
        return recorded.first().hook
    }

    private fun recordedHookCount(targetClassName: String, targetMethodName: String): Int {
        return FakeXposedInterface.recordedHooks.count { matchesExecutable(it, targetClassName, targetMethodName) }
    }

    private fun allRecordedHooks(): String {
        return FakeXposedInterface.recordedHooks.joinToString("\n") { "${it.executable.declaringClass.name}#${it.executable.name}" }
    }

    private fun newFactory(): MiuiQSFactory = MiuiQSFactory()

    private fun newTileWithContext(tileName: String, context: Context): QSTileImpl {
        val tile = newFactory().createTileInternal(tileName)
        tile.mContext = context
        return tile
    }

    private fun newContext(): FakeContext = FakeContext()

    @Test
    fun createTileInternalHook_installsForMiuiQSFactory() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        assertTrue("createTileInternal should be hooked", recordedHookCount("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal") > 0)
    }

    @Test
    fun createTileInternal_noHookWhenNoSecureTileEnabled() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = false
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val tile = factory.createTileInternal("wifi")
        val callback = fakeAfterCallback(factory, listOf("wifi"), tile)
        after.afterHook(callback)

        assertEquals("handleClick should not be hooked for disabled tile", 0, recordedHookCount("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick"))
    }

    @Test
    fun createTileInternal_hooksWifiTileWhenEnabled() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val tile = factory.createTileInternal("wifi")
        val callback = fakeAfterCallback(factory, listOf("wifi"), tile)
        after.afterHook(callback)

        val handleClickCount = recordedHookCount("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick")
        assertTrue("handleClick should be hooked for enabled wifi tile; recorded:\n${allRecordedHooks()}", handleClickCount > 0)
    }

    @Test
    fun createTileInternal_normalizesCustomSpec() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val tile = factory.createTileInternal("custom(foo)")
        val callback = fakeAfterCallback(factory, listOf("custom(foo)"), tile)
        after.afterHook(callback)

        assertTrue("handleClick should be hooked for custom(...) tile", recordedHookCount("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick") > 0)
    }

    @Test
    fun createTileInternal_doesNotHookUnderscoreCustomTile() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val tile = factory.createTileInternal("custom_5G")
        val callback = fakeAfterCallback(factory, listOf("custom_5G"), tile)
        after.afterHook(callback)

        assertEquals("custom_5G should not be treated as secure custom tile", 0, recordedHookCount("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick"))
    }

    @Test
    fun createTileInternal_doesNotHookSameTileClassTwice() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tile1 = factory.createTileInternal("wifi")
        after.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile1))

        val tile2 = factory.createTileInternal("wifi")
        after.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile2))

        assertEquals("handleClick should be hooked exactly once per tile class", 1, recordedHookCount("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick"))
    }

    @Test
    fun handleClick_before_returnsWhenCalledAfterUnlock() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val tile = newTileWithContext("wifi", newContext())
        createAfter.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick")
        XposedHelpers.setAdditionalInstanceField(tile, "mCalledAfterUnlock", true)

        val view = View(tile.mContext)
        val before = fakeBeforeCallback(tile, listOf(view))
        clickHook.beforeHook(before)

        assertFalse("should not skip when mCalledAfterUnlock is set", isSkipped(before))
        assertFalse("mCalledAfterUnlock should be cleared after use", XposedHelpers.getAdditionalInstanceField(tile, "mCalledAfterUnlock") as? Boolean ?: true)
    }

    @Test
    fun handleClick_before_returnsWhenScreenLockDisabled() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val tile = newTileWithContext("wifi", newContext())
        createAfter.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick")
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", true)

        val view = View(tile.mContext)
        val before = fakeBeforeCallback(tile, listOf(view))
        clickHook.beforeHook(before)

        assertFalse("should not skip when screen lock is disabled", isSkipped(before))
    }

    @Test
    fun handleClick_before_skipsWhenKeyguardIsSecure() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        prefs["system_secureqs_keepopened"] = false
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val context = newContext()
        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true

        val centralSurfaces = CentralSurfaces()
        val controlCenter = ControlCenterControllerImpl()
        Dependency.setMock(CentralSurfaces::class.java, centralSurfaces)
        Dependency.setMock(ControlCenterControllerImpl::class.java, controlCenter)

        val tile = newTileWithContext("wifi", context)
        createAfter.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick")
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", false)

        val view = View(context)
        val before = fakeBeforeCallback(tile, listOf(view))
        clickHook.beforeHook(before)

        assertTrue("should skip when keyguard is secure", isSkipped(before))
    }

    @Test
    fun qSTileHostConstructor_registersReceiver() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val host = QSTileHost()
        host.mContext = newContext()

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        val callback = fakeAfterCallback(host)
        constructorAfter.afterHook(callback)

        val context = host.mContext as FakeContext
        assertEquals("one receiver should be registered", 1, context.registeredReceivers.size)
    }

    @Test
    fun qSTileHostReceiver_onReceive_looksUpTileAndSetsFlag() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val host = QSTileHost()
        val context = newContext()
        host.mContext = context

        val tile = FakeWifiTile()
        tile.mContext = context
        host.mTiles["wifi"] = tile

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val receiver = context.registeredReceivers.first()
        val intent = FakeIntent("tv.withaibuild.customiuizer.mods.action.HandleQSTileClick")
        intent.putExtra("tileName", "wifi")
        intent.putExtra("expandAfter", false)
        intent.putExtra("usingCenter", false)
        receiver.onReceive(context, intent)

        assertEquals("mCalledAfterUnlock should be set on tile", true, XposedHelpers.getAdditionalInstanceField(tile, "mCalledAfterUnlock"))
    }

    @Test
    fun qSTileHostReceiver_onReceive_doesNotCrashForUnknownTile() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val host = QSTileHost()
        val context = newContext()
        host.mContext = context

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val receiver = context.registeredReceivers.first()
        val intent = FakeIntent("tv.withaibuild.customiuizer.mods.action.HandleQSTileClick")
        intent.putExtra("tileName", "unknown")
        intent.putExtra("expandAfter", false)
        intent.putExtra("usingCenter", false)

        receiver.onReceive(context, intent)
        // Should return safely without throwing
    }

    @Test
    fun handleClick_before_toleratesMissingCentralSurfacesClass() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")
        val context = newContext()
        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true

        Dependency.setMock(ControlCenterControllerImpl::class.java, ControlCenterControllerImpl())
        // Do not register CentralSurfaces mock so cached class still resolves but behavior is present

        val tile = newTileWithContext("wifi", context)
        createAfter.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick")
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", false)

        val view = View(context)
        val before = fakeBeforeCallback(tile, listOf(view))

        // Should not throw even if CentralSurfaces post method is a stub; it will be invoked with the cached Method.
        clickHook.beforeHook(before)
        assertTrue("should still skip because keyguard is secure", isSkipped(before))
    }

    @Test
    fun createTileInternal_bindsExactSpecPerInstance() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tile1 = factory.createTileInternal("custom(foo)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(foo)"), tile1))

        val tile2 = factory.createTileInternal("custom(bar)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(bar)"), tile2))

        assertEquals("first tile must keep exact spec", "custom(foo)",
            XposedHelpers.getAdditionalInstanceField(tile1, "customiuizer.secure_qs_tile_spec"))
        assertEquals("second tile must keep exact spec", "custom(bar)",
            XposedHelpers.getAdditionalInstanceField(tile2, "customiuizer.secure_qs_tile_spec"))
    }

    @Test
    fun createTileInternal_bindsExactSpecPerInstance_reverseOrder() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tile1 = factory.createTileInternal("custom(bar)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(bar)"), tile1))

        val tile2 = factory.createTileInternal("custom(foo)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(foo)"), tile2))

        assertEquals("first tile must keep exact spec", "custom(bar)",
            XposedHelpers.getAdditionalInstanceField(tile1, "customiuizer.secure_qs_tile_spec"))
        assertEquals("second tile must keep exact spec", "custom(foo)",
            XposedHelpers.getAdditionalInstanceField(tile2, "customiuizer.secure_qs_tile_spec"))
    }

    @Test
    fun createTileInternal_sameClassDifferentSpecs_hooksOnce() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tile1 = factory.createTileInternal("custom(foo)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(foo)"), tile1))

        val tile2 = factory.createTileInternal("custom(bar)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(bar)"), tile2))

        assertEquals("handleClick should be hooked exactly once per class", 1,
            recordedHookCount("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick"))
    }

    @Test
    fun handleClick_before_usesInstanceSpecNotClosureAndFailsOpenWhenMissing() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tile = newTileWithContext("custom(bar)", newContext())
        createAfter.afterHook(fakeAfterCallback(factory, listOf("custom(bar)"), tile))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick")

        val context = tile.mContext as FakeContext
        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", false)

        val view = View(context)
        val before = fakeBeforeCallback(tile, listOf(view))
        clickHook.beforeHook(before)

        assertTrue("should skip when spec is present and keyguard is secure", isSkipped(before))

        XposedHelpers.removeAdditionalInstanceField(tile, "customiuizer.secure_qs_tile_spec")

        val beforeMissing = fakeBeforeCallback(tile, listOf(view))
        clickHook.beforeHook(beforeMissing)

        assertFalse("missing spec should fail-open and not skip", isSkipped(beforeMissing))
    }

    @Test
    fun mCalledAfterUnlock_isPerInstance() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tileFoo = newTileWithContext("custom(foo)", newContext())
        createAfter.afterHook(fakeAfterCallback(factory, listOf("custom(foo)"), tileFoo))

        val tileBar = newTileWithContext("custom(bar)", newContext())
        createAfter.afterHook(fakeAfterCallback(factory, listOf("custom(bar)"), tileBar))

        XposedHelpers.setAdditionalInstanceField(tileFoo, "mCalledAfterUnlock", true)

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick")

        val view = View(tileBar.mContext)
        val before = fakeBeforeCallback(tileBar, listOf(view))
        clickHook.beforeHook(before)

        assertFalse("bar should not skip when foo is marked", isSkipped(before))
        assertEquals("foo flag should remain", true,
            XposedHelpers.getAdditionalInstanceField(tileFoo, "mCalledAfterUnlock") as? Boolean)
    }

    @Test
    fun afterUnlockRoundTrip_usesCorrectExactSpecForSharedClass() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val host = QSTileHost()
        val context = newContext()
        host.mContext = context

        val tileFoo = FakeNfcTile()
        tileFoo.mContext = context
        XposedHelpers.setAdditionalInstanceField(tileFoo, "customiuizer.secure_qs_tile_spec", "custom(foo)")
        host.mTiles["custom(foo)"] = tileFoo

        val tileBar = FakeNfcTile()
        tileBar.mContext = context
        XposedHelpers.setAdditionalInstanceField(tileBar, "customiuizer.secure_qs_tile_spec", "custom(bar)")
        host.mTiles["custom(bar)"] = tileBar

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val receiver = context.registeredReceivers.first()
        val intent = FakeIntent("tv.withaibuild.customiuizer.mods.action.HandleQSTileClick")
        intent.putExtra("tileName", "custom(bar)")
        intent.putExtra("expandAfter", false)
        intent.putExtra("usingCenter", false)
        receiver.onReceive(context, intent)

        assertEquals("mCalledAfterUnlock should be set on bar tile", true,
            XposedHelpers.getAdditionalInstanceField(tileBar, "mCalledAfterUnlock"))
        assertEquals("foo tile should not be marked", true,
            XposedHelpers.getAdditionalInstanceField(tileFoo, "mCalledAfterUnlock") == null)
    }

    @Test
    fun hostReceiver_replacementKeepsLatestActive() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")

        val host1 = QSTileHost()
        val context1 = newContext()
        host1.mContext = context1
        constructorAfter.afterHook(fakeAfterCallback(host1))

        val host2 = QSTileHost()
        val context2 = newContext()
        host2.mContext = context2
        constructorAfter.afterHook(fakeAfterCallback(host2))

        val receivers = context2.registeredReceivers
        assertTrue("host2 should register a receiver", receivers.isNotEmpty())

        val field = ModuleHelper::class.java.getDeclaredField("moduleReceivers")
        field.isAccessible = true
        val moduleReceivers = field.get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val active = moduleReceivers["systemui.afterUnlockReceiver"]
        assertNotNull("active receiver must be present", active)
    }

    @Test
    fun qSTileHostReceiver_rethrowsWrappedOutOfMemoryFromClick() {
        runReceiverFatalTest(OutOfMemoryError("oom"), "expected OutOfMemoryError") { it is OutOfMemoryError }
    }

    @Test
    fun qSTileHostReceiver_rethrowsWrappedThreadDeathFromClick() {
        runReceiverFatalTest(ThreadDeath(), "expected ThreadDeath") { it is ThreadDeath }
    }

    @Test
    fun qSTileHostReceiver_rethrowsWrappedVirtualMachineErrorFromClick() {
        runReceiverFatalTest(StackOverflowError("so"), "expected VirtualMachineError") { it is VirtualMachineError }
    }

    @Test
    fun qSTileHostReceiver_logsOrdinaryRuntimeExceptionFromClick() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val host = QSTileHost()
        val context = newContext()
        host.mContext = context

        val tile = ThrowingWifiTile(RuntimeException("ordinary"))
        tile.mContext = context
        host.mTiles["wifi"] = tile

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val receiver = context.registeredReceivers.first()
        val intent = FakeIntent("tv.withaibuild.customiuizer.mods.action.HandleQSTileClick")
        intent.putExtra("tileName", "wifi")
        intent.putExtra("expandAfter", false)
        intent.putExtra("usingCenter", false)
        // Should not throw
        receiver.onReceive(context, intent)
    }

    private fun runReceiverFatalTest(cause: Throwable, message: String, check: (Throwable) -> Boolean) {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val host = QSTileHost()
        val context = newContext()
        host.mContext = context

        val tile = ThrowingWifiTile(cause)
        tile.mContext = context
        host.mTiles["wifi"] = tile

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val receiver = context.registeredReceivers.first()
        val intent = FakeIntent("tv.withaibuild.customiuizer.mods.action.HandleQSTileClick")
        intent.putExtra("tileName", "wifi")
        intent.putExtra("expandAfter", false)
        intent.putExtra("usingCenter", false)

        try {
            receiver.onReceive(context, intent)
            fail(message)
        } catch (t: Throwable) {
            assertTrue(message, check(t))
        }
    }

    open inner class ThrowingWifiTile(val cause: Throwable) : FakeWifiTile() {
        override fun handleClick(v: View?) {
            throw cause
        }
    }
}
