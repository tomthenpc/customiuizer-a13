package tv.withaibuild.customiuizer.mods

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.FakeXposedInterface
import tv.withaibuild.customiuizer.utils.PrefMap
import java.lang.reflect.Proxy

/**
 * B3B: missing ROM class / field must fail-open and not abort later independent SystemUI hooks.
 */
class SystemUiB3BCompatibilityTest {

    private val parentClassLoader: ClassLoader
        get() = javaClass.classLoader!!

    @Before
    fun setUp() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = FakeXposedInterface.create()
        MainModule.mPrefs = PrefMap()
        MainModule.mPrefs["system_fivegtile"] = true
        MainModule.mPrefs["system_statusbar_batterytempandcurrent"] = true
        MainModule.mPrefs["system_disableanynotif"] = true
    }

    @After
    fun tearDown() {
        FakeXposedInterface.reset()
        XposedHelpers.moduleInst = null
        MainModule.mPrefs = PrefMap()
    }

    @Test
    fun addCustomTileMissingResourceIcon_stillInstallsIndependentTileHooks() {
        SystemUIMonitorAndTileHooks.AddCustomTileHook(lpparam(parentClassLoader))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == QS_FACTORY &&
                    it.executable.name == "createTileInternal"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == NFC_TILE &&
                    it.executable.name == "isAvailable"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == NFC_TILE &&
                    it.executable.name == "handleUpdateState"
            }
        )
        assertFalse(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == RESOURCE_ICON
            }
        )
    }

    @Test
    fun monitorDeviceInfoMissingDarkIconDispatcher_stillInstallsIndependentHooks() {
        SystemUIStatusBarHooks.MonitorDeviceInfoHook(lpparam(hidingClassLoader(DARK_ICON)))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == NETWORK_SPEED_VIEW &&
                    it.executable.name == "getSlot"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == NETWORK_SPEED_CONTROLLER
            }
        )
        assertFalse(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == DARK_ICON
            }
        )
    }

    @Test
    fun disableAnyNotificationMissingSettingsManager_stillInstallsFilterHelper() {
        SystemNotificationMoreHooks.DisableAnyNotificationHook(
            lpparam(hidingClassLoader(SETTINGS_MANAGER), "com.android.systemui")
        )

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == FILTER_HELPER &&
                    it.executable.name == "isNotificationForcedEnabled"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == FILTER_HELPER &&
                    it.executable.name == "canSystemNotificationBeBlocked"
            }
        )
        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == CLOUD_DATA &&
                    it.executable.name == "getFloatBlacklist"
            }
        )
        assertFalse(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == SETTINGS_MANAGER
            }
        )
    }

    @Test
    fun disableAnyNotificationMissingWhiteListsField_stillInstallsFilterHelper() {
        SystemNotificationMoreHooks.DisableAnyNotificationHook(lpparam(parentClassLoader))

        assertTrue(
            FakeXposedInterface.recordedHooks.any {
                it.executable.declaringClass.name == FILTER_HELPER &&
                    it.executable.name == "getNotificationForcedEnabledList"
            }
        )
    }

    @Test
    fun chargeAnimationWrappedOom_propagatesOriginal() {
        val oom = OutOfMemoryError("wrapped-charge")
        try {
            SystemDisplayAndWindowHooks.ChargeAnimationHook(
                lpparam(throwingClassLoader(WIRELESS_CHARGE, RuntimeException(oom)))
            )
            fail("expected wrapped OutOfMemoryError")
        } catch (t: OutOfMemoryError) {
            assertSame(oom, t)
        }
    }

    private fun lpparam(
        classLoader: ClassLoader,
        packageName: String = "com.android.systemui"
    ): PackageReadyParam {
        return Proxy.newProxyInstance(
            parentClassLoader,
            arrayOf(PackageReadyParam::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getPackageName" -> packageName
                "getProcessName" -> packageName
                "getClassLoader" -> classLoader
                else -> null
            }
        } as PackageReadyParam
    }

    private fun hidingClassLoader(hidden: String): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == hidden) {
                    throw ClassNotFoundException(name)
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    private fun throwingClassLoader(target: String, failure: Throwable): ClassLoader {
        return object : ClassLoader(parentClassLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == target) {
                    throw failure
                }
                return super.loadClass(name, resolve)
            }
        }
    }

    companion object {
        private const val QS_FACTORY = "com.android.systemui.qs.tileimpl.MiuiQSFactory"
        private const val NFC_TILE = "com.android.systemui.qs.tiles.MiuiNfcTile"
        private const val RESOURCE_ICON = "com.android.systemui.qs.tileimpl.QSTileImpl\$ResourceIcon"
        private const val DARK_ICON = "com.android.systemui.plugins.DarkIconDispatcher"
        private const val NETWORK_SPEED_VIEW = "com.android.systemui.statusbar.views.NetworkSpeedView"
        private const val NETWORK_SPEED_CONTROLLER =
            "com.android.systemui.statusbar.policy.NetworkSpeedController"
        private const val SETTINGS_MANAGER =
            "com.android.systemui.statusbar.notification.NotificationSettingsManager"
        private const val FILTER_HELPER = "miui.util.NotificationFilterHelper"
        private const val CLOUD_DATA = "com.miui.systemui.NotificationCloudData\$Companion"
        private const val WIRELESS_CHARGE =
            "com.android.keyguard.charge.MiuiWirelessChargeController"
    }
}
