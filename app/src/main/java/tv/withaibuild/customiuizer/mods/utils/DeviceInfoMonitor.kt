package tv.withaibuild.customiuizer.mods.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.Properties
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.mods.SystemUIStatusBarHooks
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.utils.PrefMap

/**
 * Owns the status-bar device-info ticker. UI slot creation remains in the A13 status-bar hook;
 * this controller only owns scheduling, sysfs reads, and delivery of changed text.
 */
object DeviceInfoMonitor {

    private const val MONITOR_MESSAGE = 200021
    private const val UPDATE_MESSAGE = 100021
    internal const val BASE_DELAY_MS = 2_000L
    internal const val MAX_DELAY_MS = 60_000L
    private const val SCREEN_RECEIVER_KEY = "systemui.deviceInfoMonitorScreenReceiver"

    internal data class Snapshot(
        val showBatteryDetail: Boolean,
        val showDeviceTemp: Boolean,
        val batteryInCharge: Boolean,
        val batteryContentOpt: Int,
        val batteryTempDecimal: Boolean,
        val batteryFixCurrentRatio: Boolean,
        val batteryPositive: Boolean,
        val batterySingleRow: Boolean,
        val batteryReverseOrder: Boolean,
        val batteryHideUnit: Int,
        val deviceTempContentOpt: Int,
        val deviceTempHideUnit: Boolean,
        val deviceTempSingleRow: Boolean,
        val deviceTempReverseOrder: Boolean
    ) {
        val enabled: Boolean
            get() = showBatteryDetail || showDeviceTemp
    }

    internal class LifecycleState {
        var running: Boolean = false
            private set
        var screenOn: Boolean = false
            private set
        var consecutiveFailures: Int = 0
            private set

        fun start(enabled: Boolean, interactive: Boolean): Boolean {
            consecutiveFailures = 0
            running = enabled
            screenOn = enabled && interactive
            return running && screenOn
        }

        fun stop() {
            running = false
            screenOn = false
            consecutiveFailures = 0
        }

        fun onScreenOff() {
            screenOn = false
        }

        fun onScreenOn(): Boolean {
            if (!running) return false
            screenOn = true
            return true
        }

        fun recordRead(success: Boolean): Long {
            if (success) {
                consecutiveFailures = 0
                return BASE_DELAY_MS
            }
            consecutiveFailures++
            val multiplier = 1L shl consecutiveFailures.coerceAtMost(5)
            return (BASE_DELAY_MS * multiplier).coerceAtMost(MAX_DELAY_MS)
        }

        fun canSchedule(): Boolean = running && screenOn
    }

    private data class IconUpdate(
        val type: Int,
        val show: Boolean,
        val text: String
    )

    private data class ReadResult(
        val batteryShow: Boolean,
        val batteryText: String,
        val tempShow: Boolean,
        val tempText: String,
        val complete: Boolean
    )

    private data class TextState(
        var show: Boolean = false,
        var text: String = ""
    )

    private val lock = Any()
    private val lifecycle = LifecycleState()
    private val batteryState = TextState()
    private val tempState = TextState()

    @Volatile
    private var snapshot: Snapshot? = null

    private var generation = 0
    private var mainHandler: Handler? = null
    private var backgroundHandler: Handler? = null
    private var chargeUtilsClass: Class<*>? = null
    private var classLoader: ClassLoader? = null

    @JvmStatic
    fun hook(lpparam: PackageReadyParam) {
        classLoader = lpparam.classLoader
        snapshot = readSnapshot(MainModule.mPrefs)
        if (snapshot?.enabled != true) return

        resolveChargeUtilsIfNeeded(snapshot!!)
        ModuleHelper.observePreferenceChange("systemui.deviceInfoMonitor") { key ->
            if (key == null ||
                key.startsWith("system_statusbar_batterytempandcurrent") ||
                key.startsWith("system_statusbar_showdevicetemperature")
            ) {
                refreshConfiguration()
            }
        }

        ModuleHelper.hookAllConstructors(
            "com.android.systemui.statusbar.policy.NetworkSpeedController",
            lpparam.classLoader,
            object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val context = param.getArg(0) as? Context ?: return
                    val looper = param.getArg(1) as? Looper ?: Looper.myLooper() ?: return
                    start(context, looper)
                }
            }
        )
    }

    private fun start(context: Context, looper: Looper) {
        val applicationContext = context.applicationContext ?: context
        val currentSnapshot = snapshot ?: return
        synchronized(lock) {
            stopLocked()
            generation++
            val activeGeneration = generation

            mainHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    ModuleHelper.guarded("DeviceInfoMonitor.updateHandler") {
                        if (activeGeneration != generation || msg.what != UPDATE_MESSAGE) {
                            return@guarded
                        }
                        val update = msg.obj as? IconUpdate ?: return@guarded
                        SystemUIStatusBarHooks.updateDeviceInfoIcon(
                            update.type,
                            update.show,
                            update.text
                        )
                    }
                }
            }

            backgroundHandler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    ModuleHelper.guarded("DeviceInfoMonitor.monitorHandler") {
                        if (activeGeneration != generation || msg.what != MONITOR_MESSAGE) {
                            return@guarded
                        }
                        runTick(applicationContext, activeGeneration)
                    }
                }
            }

            val powerManager =
                applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val runImmediately = lifecycle.start(
                currentSnapshot.enabled,
                powerManager?.isInteractive ?: true
            )
            registerScreenReceiverLocked(applicationContext, activeGeneration)
            if (runImmediately) backgroundHandler?.sendEmptyMessage(MONITOR_MESSAGE)
        }
    }

    private fun registerScreenReceiverLocked(context: Context, activeGeneration: Int) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ModuleHelper.guarded("DeviceInfoMonitor.screenReceiver") {
                    synchronized(lock) {
                        if (activeGeneration != generation) return@guarded
                        when (intent.action) {
                            Intent.ACTION_SCREEN_OFF -> {
                                lifecycle.onScreenOff()
                                backgroundHandler?.removeMessages(MONITOR_MESSAGE)
                                mainHandler?.removeMessages(UPDATE_MESSAGE)
                            }
                            Intent.ACTION_SCREEN_ON -> {
                                if (lifecycle.onScreenOn()) {
                                    backgroundHandler?.removeMessages(MONITOR_MESSAGE)
                                    backgroundHandler?.sendEmptyMessage(MONITOR_MESSAGE)
                                }
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ModuleHelper.registerModuleReceiver(
            context,
            SCREEN_RECEIVER_KEY,
            receiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun refreshConfiguration() {
        val current = readSnapshot(MainModule.mPrefs)
        snapshot = current
        resolveChargeUtilsIfNeeded(current)
        synchronized(lock) {
            if (!current.enabled) {
                stopLocked()
                return
            }
            if (lifecycle.canSchedule()) {
                backgroundHandler?.removeMessages(MONITOR_MESSAGE)
                backgroundHandler?.sendEmptyMessage(MONITOR_MESSAGE)
            }
        }
    }

    private fun resolveChargeUtilsIfNeeded(current: Snapshot) {
        if (!current.showBatteryDetail || !current.batteryInCharge || chargeUtilsClass != null) {
            return
        }
        chargeUtilsClass = XposedHelpers.findClassIfExists(
            "com.android.keyguard.charge.ChargeUtils",
            classLoader
        )
    }

    private fun runTick(context: Context, activeGeneration: Int) {
        val current = snapshot ?: return
        if (!current.enabled) {
            synchronized(lock) { stopLocked() }
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isInteractive == false) {
            synchronized(lock) {
                if (activeGeneration == generation) {
                    lifecycle.onScreenOff()
                    backgroundHandler?.removeMessages(MONITOR_MESSAGE)
                }
            }
            return
        }

        val result = readDeviceData(current)
        publish(current, result)

        synchronized(lock) {
            if (activeGeneration != generation || !lifecycle.canSchedule()) return
            val delay = lifecycle.recordRead(result.complete)
            backgroundHandler?.sendEmptyMessageDelayed(MONITOR_MESSAGE, delay)
        }
    }

    private fun publish(current: Snapshot, result: ReadResult) {
        if (current.showBatteryDetail &&
            (batteryState.show != result.batteryShow || batteryState.text != result.batteryText)
        ) {
            batteryState.show = result.batteryShow
            batteryState.text = result.batteryText
            mainHandler?.obtainMessage(
                UPDATE_MESSAGE,
                IconUpdate(91, result.batteryShow, result.batteryText)
            )?.sendToTarget()
        }
        if (current.showDeviceTemp &&
            (tempState.show != result.tempShow || tempState.text != result.tempText)
        ) {
            tempState.show = result.tempShow
            tempState.text = result.tempText
            mainHandler?.obtainMessage(
                UPDATE_MESSAGE,
                IconUpdate(92, result.tempShow, result.tempText)
            )?.sendToTarget()
        }
    }

    private fun readDeviceData(current: Snapshot): ReadResult {
        val showBattery = current.showBatteryDetail && shouldShowBatteryInfo(current)
        val tempMode =
            if (current.deviceTempContentOpt in 1..3) current.deviceTempContentOpt else 1
        val needBatteryTemp =
            current.showDeviceTemp && (tempMode == 1 || tempMode == 2)
        val needCpuTemp =
            current.showDeviceTemp && (tempMode == 1 || tempMode == 3)
        val needBatteryUevent = showBattery || needBatteryTemp

        val props = if (needBatteryUevent) readBatteryProperties() else null
        val cpuTemp = if (needCpuTemp) readCpuTemperature() else null
        val batteryComplete = !needBatteryUevent || props != null
        val cpuComplete = !needCpuTemp || cpuTemp != null

        return ReadResult(
            batteryShow = showBattery,
            batteryText =
                if (showBattery && props != null) buildBatteryInfo(current, props) else "",
            tempShow = current.showDeviceTemp,
            tempText = if (current.showDeviceTemp) {
                buildDeviceInfo(
                    current,
                    props?.getProperty("POWER_SUPPLY_TEMP"),
                    cpuTemp
                )
            } else {
                ""
            },
            complete = batteryComplete && cpuComplete
        )
    }

    private fun shouldShowBatteryInfo(current: Snapshot): Boolean {
        if (!current.batteryInCharge) return true
        val chargeUtils = chargeUtilsClass ?: return true
        val batteryStatus =
            ModuleHelper.getStaticObjectFieldSilently(chargeUtils, "sBatteryStatus")
        if (ModuleHelper.NOT_EXIST_SYMBOL == batteryStatus) return false
        return XposedHelpers.callMethod(batteryStatus, "isCharging") as? Boolean ?: false
    }

    private fun readBatteryProperties(): Properties? {
        return try {
            FileInputStream("/sys/class/power_supply/battery/uevent").use { input ->
                Properties().apply { load(input) }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readCpuTemperature(): String? {
        return try {
            RandomAccessFile(
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "r"
            ).use { it.readLine() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun stopLocked() {
        generation++
        backgroundHandler?.removeMessages(MONITOR_MESSAGE)
        mainHandler?.removeMessages(UPDATE_MESSAGE)
        ModuleHelper.unregisterModuleReceiver(SCREEN_RECEIVER_KEY)
        backgroundHandler = null
        mainHandler = null
        lifecycle.stop()
        batteryState.show = false
        batteryState.text = ""
        tempState.show = false
        tempState.text = ""
    }

    @JvmStatic
    internal fun readSnapshot(prefs: PrefMap<String, Any>): Snapshot {
        return Snapshot(
            showBatteryDetail =
                prefs.getBoolean("system_statusbar_batterytempandcurrent"),
            showDeviceTemp =
                prefs.getBoolean("system_statusbar_showdevicetemperature"),
            batteryInCharge =
                prefs.getBoolean("system_statusbar_batterytempandcurrent_incharge"),
            batteryContentOpt =
                prefs.getStringAsInt("system_statusbar_batterytempandcurrent_content", 1),
            batteryTempDecimal =
                prefs.getBoolean("system_statusbar_batterytempandcurrent_temp_decimal"),
            batteryFixCurrentRatio =
                prefs.getBoolean("system_statusbar_batterytempandcurrent_fixcurrentratio"),
            batteryPositive =
                prefs.getBoolean("system_statusbar_batterytempandcurrent_positive"),
            batterySingleRow =
                prefs.getBoolean("system_statusbar_batterytempandcurrent_singlerow"),
            batteryReverseOrder =
                prefs.getBoolean("system_statusbar_batterytempandcurrent_reverseorder"),
            batteryHideUnit =
                prefs.getStringAsInt("system_statusbar_batterytempandcurrent_hideunit", 0),
            deviceTempContentOpt =
                prefs.getStringAsInt("system_statusbar_showdevicetemperature_content", 1),
            deviceTempHideUnit =
                prefs.getBoolean("system_statusbar_showdevicetemperature_hideunit"),
            deviceTempSingleRow =
                prefs.getBoolean("system_statusbar_showdevicetemperature_singlerow"),
            deviceTempReverseOrder =
                prefs.getBoolean("system_statusbar_showdevicetemperature_reverseorder")
        )
    }

    private fun parseSysfsInt(raw: String?, fallback: Int = 0): Int {
        return raw?.trim()?.toIntOrNull() ?: fallback
    }

    @JvmStatic
    internal fun buildBatteryInfo(current: Snapshot, props: Properties): String {
        val opt = current.batteryContentOpt
        var simpleTemp = ""
        if (opt == 1 || opt == 4) {
            val value = parseSysfsInt(props.getProperty("POWER_SUPPLY_TEMP"))
            simpleTemp = if (current.batteryTempDecimal) {
                (value / 10f).toString()
            } else if (value % 10 == 0) {
                (value / 10).toString()
            } else {
                (value / 10f).toString()
            }
        }

        val ratio = if (current.batteryFixCurrentRatio) 1f else 1_000f
        var rawCurrent =
            -1 * Math.round(parseSysfsInt(props.getProperty("POWER_SUPPLY_CURRENT_NOW")) / ratio)
        var currentText = ""
        var currentUnit = "mA"
        if (opt == 1 || opt == 3 || opt == 5) {
            if (current.batteryPositive) rawCurrent = Math.abs(rawCurrent)
            if (Math.abs(rawCurrent) > 999) {
                currentText = String.format(Locale.getDefault(), "%.2f", rawCurrent / 1_000f)
                currentUnit = "A"
            } else {
                currentText = rawCurrent.toString()
            }
        }

        val hideUnit = current.batteryHideUnit
        val tempUnit = if (hideUnit == 1 || hideUnit == 2) "" else "\u2103"
        val powerUnit = if (hideUnit == 1 || hideUnit == 3) "" else "W"
        val finalCurrentUnit = if (hideUnit == 1 || hideUnit == 3) "" else currentUnit
        var watts = ""
        if (opt == 2 || opt == 4 || opt == 5) {
            val volts =
                parseSysfsInt(props.getProperty("POWER_SUPPLY_VOLTAGE_NOW")) / 1_000f / 1_000f
            watts = String.format(
                Locale.getDefault(),
                "%.2f",
                Math.abs(volts * rawCurrent) / 1_000
            )
        }

        val separator = if (current.batterySingleRow) " " else "\n"
        return when (opt) {
            1 -> if (current.batteryReverseOrder) {
                "$currentText$finalCurrentUnit$separator$simpleTemp$tempUnit"
            } else {
                "$simpleTemp$tempUnit$separator$currentText$finalCurrentUnit"
            }
            4 -> if (current.batteryReverseOrder) {
                "$watts$powerUnit$separator$simpleTemp$tempUnit"
            } else {
                "$simpleTemp$tempUnit$separator$watts$powerUnit"
            }
            2 -> "$watts$powerUnit"
            5 -> if (current.batteryReverseOrder) {
                "$watts$powerUnit$separator$currentText$finalCurrentUnit"
            } else {
                "$currentText$finalCurrentUnit$separator$watts$powerUnit"
            }
            else -> "$currentText$finalCurrentUnit"
        }
    }

    @JvmStatic
    internal fun buildDeviceInfo(
        current: Snapshot,
        batteryTemp: String?,
        cpuTemp: String?
    ): String {
        val opt =
            if (current.deviceTempContentOpt in 1..3) current.deviceTempContentOpt else 1
        val unit = if (current.deviceTempHideUnit) "" else "\u2103"
        val separator = if (current.deviceTempSingleRow) " " else "\n"
        return when (opt) {
            1 -> {
                val battery = String.format(
                    Locale.getDefault(),
                    "%.1f",
                    parseSysfsInt(batteryTemp) / 10f
                )
                val cpu = String.format(
                    Locale.getDefault(),
                    "%.1f",
                    parseSysfsInt(cpuTemp) / 1_000f
                )
                if (current.deviceTempReverseOrder) {
                    "$cpu$unit$separator$battery$unit"
                } else {
                    "$battery$unit$separator$cpu$unit"
                }
            }
            2 -> String.format(
                Locale.getDefault(),
                "%.1f$unit",
                parseSysfsInt(batteryTemp) / 10f
            )
            else -> String.format(
                Locale.getDefault(),
                "%.1f$unit",
                parseSysfsInt(cpuTemp) / 1_000f
            )
        }
    }
}
