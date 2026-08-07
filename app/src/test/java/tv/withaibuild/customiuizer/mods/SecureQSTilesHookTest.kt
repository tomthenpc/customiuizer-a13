package tv.withaibuild.customiuizer.mods

import android.content.BroadcastReceiver
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
import org.junit.Assert.assertNotEquals
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

    @Test
    fun afterUnlockRoundTrip_fullSharedClassLifecycle() {
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

        val host = QSTileHost()
        val context = tileBar.mContext as FakeContext
        host.mContext = context
        host.mTiles["custom(foo)"] = tileFoo
        host.mTiles["custom(bar)"] = tileBar

        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true

        val centralSurfaces = CentralSurfaces()
        val controlCenter = ControlCenterControllerImpl()
        Dependency.setMock(CentralSurfaces::class.java, centralSurfaces)
        Dependency.setMock(ControlCenterControllerImpl::class.java, controlCenter)

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick")
        val view = View(context)
        val before = fakeBeforeCallback(tileBar, listOf(view))

        clickHook.beforeHook(before)

        assertTrue("bar before hook should skip", isSkipped(before))
        assertEquals("one runnable should be posted", 1, centralSurfaces.postedRunnables.size)

        val posted = centralSurfaces.postedRunnables.first()
        assertTrue("posted dismissing-runnable should be true by default (keepOpened false)", posted.first)
        posted.second.run()

        assertEquals("one broadcast should be sent", 1, context.sentBroadcasts.size)

        val actualBroadcast = context.sentBroadcasts.single()
        assertEquals("action", "tv.withaibuild.customiuizer.mods.action.HandleQSTileClick", actualBroadcast.action)
        val actualTileName = actualBroadcast.getStringExtra("tileName")
        assertEquals("production broadcast must carry bar exact spec", "custom(bar)", actualTileName)
        assertNotEquals("production broadcast must not carry foo spec", "custom(foo)", actualTileName)
        assertEquals("expandAfter", false, actualBroadcast.getBooleanExtra("expandAfter", true))
        assertEquals("usingCenter", true, actualBroadcast.getBooleanExtra("usingCenter", false))

        val receiver = context.registeredReceivers.first()
        receiver.onReceive(context, actualBroadcast)

        assertEquals("bar should execute original handleClick once", 1, (tileBar as FakeNfcTile).clickCount)
        assertEquals("foo should not execute original handleClick", 0, (tileFoo as FakeNfcTile).clickCount)
        assertEquals("foo flag should not be set", true,
            XposedHelpers.getAdditionalInstanceField(tileFoo, "mCalledAfterUnlock") == null)

        // Simulate the before-hook entry that the JVM fake may not trigger via Method.invoke,
        // proving the after-unlock flag is consumed per-instance on bar and not shared with foo.
        val secondBefore = fakeBeforeCallback(tileBar, listOf(view))
        clickHook.beforeHook(secondBefore)

        assertFalse("second before hook should not skip when mCalledAfterUnlock is set", isSkipped(secondBefore))
        assertEquals("bar mCalledAfterUnlock should be cleared after consumption", false,
            XposedHelpers.getAdditionalInstanceField(tileBar, "mCalledAfterUnlock") as? Boolean)
        assertEquals("foo mCalledAfterUnlock should remain untouched", true,
            XposedHelpers.getAdditionalInstanceField(tileFoo, "mCalledAfterUnlock") == null)
    }

    @Test
    fun mCalledAfterUnlock_isPerInstance_strong() {
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

        val context = tileBar.mContext as FakeContext
        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", false)

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick")
        val view = View(context)
        val before = fakeBeforeCallback(tileBar, listOf(view))
        clickHook.beforeHook(before)

        assertTrue("bar with its own spec and keyguard secure should skip", isSkipped(before))
        assertEquals("foo flag should not be touched", true,
            XposedHelpers.getAdditionalInstanceField(tileFoo, "mCalledAfterUnlock") as? Boolean)
    }

    @Test
    fun createTileInternal_rebindsSpecOnSameInstance() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_custom"] = true
        installHook(prefs)

        val factory = newFactory()
        val after = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val tile = factory.createTileInternal("custom(foo)")
        after.afterHook(fakeAfterCallback(factory, listOf("custom(foo)"), tile))

        after.afterHook(fakeAfterCallback(factory, listOf("custom(bar)"), tile))

        assertEquals("spec should be rebound to custom(bar)", "custom(bar)",
            XposedHelpers.getAdditionalInstanceField(tile, "customiuizer.secure_qs_tile_spec"))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeNfcTile", "handleClick")
        val context = newContext()
        tile.mContext = context
        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", false)

        val host = QSTileHost()
        host.mContext = context
        host.mTiles["custom(bar)"] = tile

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")
        constructorAfter.afterHook(fakeAfterCallback(host))

        val centralSurfaces = CentralSurfaces()
        val controlCenter = ControlCenterControllerImpl()
        Dependency.setMock(CentralSurfaces::class.java, centralSurfaces)
        Dependency.setMock(ControlCenterControllerImpl::class.java, controlCenter)

        val view = View(context)
        val before = fakeBeforeCallback(tile, listOf(view))

        clickHook.beforeHook(before)

        assertTrue("should skip with rebound spec and keyguard secure", isSkipped(before))
        assertEquals("one runnable should be posted", 1, centralSurfaces.postedRunnables.size)

        centralSurfaces.postedRunnables.first().second.run()

        assertEquals("one broadcast should be sent", 1, context.sentBroadcasts.size)

        val receiver = context.registeredReceivers.first()
        val broadcast = FakeIntent("tv.withaibuild.customiuizer.mods.action.HandleQSTileClick")
        broadcast.putExtra("tileName", "custom(bar)")
        broadcast.putExtra("expandAfter", false)
        broadcast.putExtra("usingCenter", false)
        receiver.onReceive(context, broadcast)

        assertEquals("rebound tile should execute original handleClick once", 1, (tile as FakeNfcTile).clickCount)
    }

    @Test
    fun hostReceiver_successfulReplacement_unregistersOldReceiver() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")

        val host1 = QSTileHost()
        val context1 = newContext()
        host1.mContext = context1
        constructorAfter.afterHook(fakeAfterCallback(host1))
        val receiver1 = context1.registeredReceivers.first()

        val host2 = QSTileHost()
        val context2 = newContext()
        host2.mContext = context2
        constructorAfter.afterHook(fakeAfterCallback(host2))
        val receiver2 = context2.registeredReceivers.first()

        assertEquals("context2 should register one receiver", 1, context2.registeredReceivers.size)
        assertFalse("context1 registered list should no longer contain receiver1", context1.registeredReceivers.contains(receiver1))
        assertTrue("context1 should have unregistered receiver1", context1.unregisteredReceivers.contains(receiver1))

        val field = ModuleHelper::class.java.getDeclaredField("moduleReceivers")
        field.isAccessible = true
        val moduleReceivers = field.get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val active = moduleReceivers["systemui.afterUnlockReceiver"]
        assertNotNull(active)
        val activeEntry = active!!
        val activeReceiver = activeEntry.javaClass.getDeclaredField("receiver").apply { isAccessible = true }.get(activeEntry) as BroadcastReceiver
        assertEquals("active receiver should be receiver2", receiver2, activeReceiver)

        val staleField = ModuleHelper::class.java.getDeclaredField("staleModuleReceivers")
        staleField.isAccessible = true
        val staleMap = staleField.get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val staleDeque = staleMap["systemui.afterUnlockReceiver"] as? java.util.Deque<*>
        assertTrue("stale registry should not contain receiver1 on success path", staleDeque?.none { reg ->
            reg?.javaClass?.getDeclaredField("receiver")?.apply { isAccessible = true }?.get(reg) == receiver1
        } ?: true)
    }

    @Test
    fun hostReceiver_registerFailure_keepsOldReceiverActive() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")

        val host1 = QSTileHost()
        val context1 = newContext()
        host1.mContext = context1
        constructorAfter.afterHook(fakeAfterCallback(host1))
        val receiver1 = context1.registeredReceivers.first()

        val host2 = QSTileHost()
        val context2 = newContext()
        context2.failRegisterReceiver = true
        host2.mContext = context2
        constructorAfter.afterHook(fakeAfterCallback(host2))

        assertTrue("context2 should not register a new receiver", context2.registeredReceivers.isEmpty())
        assertFalse("receiver1 should not be unregistered", context1.unregisteredReceivers.contains(receiver1))

        val field = ModuleHelper::class.java.getDeclaredField("moduleReceivers")
        field.isAccessible = true
        val moduleReceivers = field.get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val active = moduleReceivers["systemui.afterUnlockReceiver"]
        assertNotNull(active)
        val activeEntry = active!!
        val activeReceiver = activeEntry.javaClass.getDeclaredField("receiver").apply { isAccessible = true }.get(activeEntry) as BroadcastReceiver
        assertEquals("active receiver should still be receiver1", receiver1, activeReceiver)
    }

    @Test
    fun hostReceiver_unregisterFailure_movesOldReceiverToStale() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        installHook(prefs)

        val constructorAfter = getRecordedHook("com.android.systemui.qs.QSTileHost", "<init>")

        val host1 = QSTileHost()
        val context1 = newContext()
        context1.failUnregisterReceiver = true
        host1.mContext = context1
        constructorAfter.afterHook(fakeAfterCallback(host1))
        val receiver1 = context1.registeredReceivers.first()

        val host2 = QSTileHost()
        val context2 = newContext()
        host2.mContext = context2
        constructorAfter.afterHook(fakeAfterCallback(host2))
        val receiver2 = context2.registeredReceivers.first()

        assertFalse("receiver1 should not be unregistered", context1.unregisteredReceivers.contains(receiver1))

        val field = ModuleHelper::class.java.getDeclaredField("moduleReceivers")
        field.isAccessible = true
        val moduleReceivers = field.get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val active = moduleReceivers["systemui.afterUnlockReceiver"]
        assertNotNull(active)
        val activeEntry = active!!
        val activeReceiver = activeEntry.javaClass.getDeclaredField("receiver").apply { isAccessible = true }.get(activeEntry) as BroadcastReceiver
        assertEquals("active should be receiver2", receiver2, activeReceiver)

        val staleField = ModuleHelper::class.java.getDeclaredField("staleModuleReceivers")
        staleField.isAccessible = true
        val staleMap = staleField.get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val staleDeque = staleMap["systemui.afterUnlockReceiver"] as? java.util.Deque<*>
        assertTrue("stale registry should contain receiver1", staleDeque?.any { reg ->
            reg?.javaClass?.getDeclaredField("receiver")?.apply { isAccessible = true }?.get(reg) == receiver1
        } ?: false)
    }

    @Test
    fun tileHook_before_rethrowsWrappedFatalFromPost() {
        val prefs = PrefMap<String, Any>()
        prefs["system_secureqs"] = true
        prefs["system_secureqs_wifi"] = true
        installHook(prefs)

        val factory = newFactory()
        val createAfter = getRecordedHook("com.android.systemui.qs.tileimpl.MiuiQSFactory", "createTileInternal")

        val context = newContext()
        context.keyguardManager?.locked = true
        context.keyguardManager?.secure = true
        val keyguardClass = XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", parentClassLoader)
        XposedHelpers.setAdditionalStaticField(keyguardClass, "isScreenLockDisabled", false)

        Dependency.setMock(CentralSurfaces::class.java, CentralSurfaces())
        Dependency.setMock(ControlCenterControllerImpl::class.java, ThrowingControlCenterControllerImpl(OutOfMemoryError("oom")))

        val tile = newTileWithContext("wifi", context)
        createAfter.afterHook(fakeAfterCallback(factory, listOf("wifi"), tile))

        val clickHook = getRecordedHook("com.android.systemui.qs.tiles.FakeWifiTile", "handleClick")
        val view = View(context)
        val before = fakeBeforeCallback(tile, listOf(view))

        try {
            clickHook.beforeHook(before)
            fail("expected OutOfMemoryError")
        } catch (t: Throwable) {
            assertTrue("expected wrapped OOM to escape through Handler post", t is OutOfMemoryError)
        }
    }

    open inner class ThrowingWifiTile(val cause: Throwable) : FakeWifiTile() {
        override fun handleClick(v: View?) {
            throw cause
        }
    }

    class ThrowingControlCenterControllerImpl(val cause: Throwable) : ControlCenterControllerImpl() {
        override fun isUseControlCenter(): Boolean {
            throw cause
        }
    }
}
