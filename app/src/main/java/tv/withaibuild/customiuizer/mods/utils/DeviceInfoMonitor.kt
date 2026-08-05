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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
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

    private val DF_1DEC = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue() = DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.getDefault()))
    }
    private val DF_2DEC = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue() = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.getDefault()))
    }

    private fun format1(value: Float): String = DF_1DEC.get()!!.format(value)
    private fun format2(value: Float): String = DF_2DEC.get()!!.format(value)

    private const val MONITOR_MESSAGE = 200021
    private const val UPDATE_MESSAGE = 100021
    internal const val BASE_DELAY_MS = 2_000L
    internal const val MAX_DELAY_MS = 60_000L
    private const val SCREEN_RECEIVER_KEY = "systemui.deviceInfoMonitorScreenReceiver"

    private const val BATTERY_UEVENT_PATH =
        "/sys/class/power_supply/battery/uevent"
    private const val CPU_TEMP_PATH =
        "/sys/devices/virtual/thermal/thermal_zone0/temp"

    private val sysfsReadLock = Any()
    private val batteryReadBuffer =
        ByteArray(DeviceInfoSysfsParser.BATTERY_BUFFER_BYTES)
    private val cpuReadBuffer =
        ByteArray(DeviceInfoSysfsParser.CPU_BUFFER_BYTES)
    private val batteryValues = DeviceInfoSysfsParser.BatteryValues()

    internal data class Snapshot(
        val showBatteryDetail: Boolean = true,
        val showDeviceTemp: Boolean = true,
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

    internal fun isCurrentTick(
        activeGeneration: Int,
        currentGeneration: Int,
        tickSnapshot: Snapshot,
        latestSnapshot: Snapshot?,
        canSchedule: Boolean
    ): Boolean {
        return activeGeneration == currentGeneration &&
            tickSnapshot === latestSnapshot &&
            canSchedule
    }

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
    private var fixedShowBatteryDetail = false
    private var fixedShowDeviceTemp = false

    @JvmStatic
    fun hook(
        lpparam: PackageReadyParam,
        showBatteryDetail: Boolean,
        showDeviceTemp: Boolean
    ) {
        classLoader = lpparam.classLoader
        fixedShowBatteryDetail = showBatteryDetail
        fixedShowDeviceTemp = showDeviceTemp
        snapshot = readSnapshot(
            MainModule.mPrefs,
            fixedShowBatteryDetail,
            fixedShowDeviceTemp
        )
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
                        val text = msg.obj as? String ?: return@guarded
                        val type = msg.arg1
                        val show = msg.arg2 != 0
                        SystemUIStatusBarHooks.updateDeviceInfoIcon(type, show, text)
                    }
                }
            }

            backgroundHandler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    ModuleHelper.guarded("DeviceInfoMonitor.monitorHandler") {
                        if (activeGeneration != generation || msg.what != MONITOR_MESSAGE) {
                            return@guarded
                        }
                        runTick(activeGeneration)
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
        val current = readSnapshot(
            MainModule.mPrefs,
            fixedShowBatteryDetail,
            fixedShowDeviceTemp
        )
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

    private fun runTick(activeGeneration: Int) {
        val current = snapshot ?: return
        if (!current.enabled) {
            synchronized(lock) { stopLocked() }
            return
        }

        synchronized(lock) {
            if (!isCurrentTick(
                    activeGeneration,
                    generation,
                    current,
                    snapshot,
                    lifecycle.canSchedule()
                )
            ) {
                return
            }
        }

        val complete = sampleAndPublish(activeGeneration, current)

        synchronized(lock) {
            if (!isCurrentTick(
                    activeGeneration,
                    generation,
                    current,
                    snapshot,
                    lifecycle.canSchedule()
                )
            ) {
                return
            }
            val delay = lifecycle.recordRead(complete)
            backgroundHandler?.removeMessages(MONITOR_MESSAGE)
            backgroundHandler?.sendEmptyMessageDelayed(MONITOR_MESSAGE, delay)
        }
    }

    private fun sampleAndPublish(activeGeneration: Int, current: Snapshot): Boolean {
        val showBattery = current.showBatteryDetail && shouldShowBatteryInfo(current)
        val tempMode =
            if (current.deviceTempContentOpt in 1..3) current.deviceTempContentOpt else 1
        val needBatteryTemp =
            current.showDeviceTemp && (tempMode == 1 || tempMode == 2)
        val needCpuTemp =
            current.showDeviceTemp && (tempMode == 1 || tempMode == 3)
        val needBatteryUevent = showBattery || needBatteryTemp

        var batteryTemperature = 0
        var batteryCurrentNow = 0
        var batteryVoltageNow = 0
        var cpuTemperature = 0

        var batteryRead = !needBatteryUevent
        var cpuRead = !needCpuTemp

        synchronized(sysfsReadLock) {
            if (needBatteryUevent) {
                val length = readSysfsFile(BATTERY_UEVENT_PATH, batteryReadBuffer)
                if (length >= 0) {
                    batteryRead = DeviceInfoSysfsParser.parseBatteryUevent(
                        batteryReadBuffer,
                        length,
                        batteryValues
                    )
                    if (batteryRead) {
                        batteryTemperature = batteryValues.temperature
                        batteryCurrentNow = batteryValues.currentNow
                        batteryVoltageNow = batteryValues.voltageNow
                    }
                }
            }
            if (needCpuTemp) {
                val length = readSysfsFile(CPU_TEMP_PATH, cpuReadBuffer)
                if (length >= 0) {
                    cpuTemperature =
                        DeviceInfoSysfsParser.parseCpuTemperature(cpuReadBuffer, length)
                    cpuRead = true
                }
            }
        }

        val batteryText = if (showBattery && batteryRead) {
            buildBatteryInfo(
                current,
                batteryTemperature,
                batteryCurrentNow,
                batteryVoltageNow
            )
        } else {
            ""
        }

        val tempText = if (current.showDeviceTemp) {
            buildDeviceInfo(current, batteryTemperature, cpuTemperature)
        } else {
            ""
        }

        publish(activeGeneration, current, 91, showBattery, batteryText)
        publish(activeGeneration, current, 92, current.showDeviceTemp, tempText)

        val batteryComplete = !needBatteryUevent || batteryRead
        val cpuComplete = !needCpuTemp || cpuRead
        return batteryComplete && cpuComplete
    }

    private fun publish(
        activeGeneration: Int,
        current: Snapshot,
        type: Int,
        show: Boolean,
        text: String
    ) {
        synchronized(lock) {
            if (!isCurrentTick(
                    activeGeneration,
                    generation,
                    current,
                    snapshot,
                    lifecycle.canSchedule()
                )
            ) {
                return
            }
            val handler = mainHandler ?: return
            val state = if (type == 91) batteryState else tempState
            val featureEnabled =
                if (type == 91) current.showBatteryDetail else current.showDeviceTemp
            if (!featureEnabled) return
            if (state.show != show || state.text != text) {
                state.show = show
                state.text = text
                handler.obtainMessage(
                    UPDATE_MESSAGE,
                    type,
                    if (show) 1 else 0,
                    text
                ).sendToTarget()
            }
        }
    }

    private fun readSysfsFile(path: String, buffer: ByteArray): Int {
        return try {
            FileInputStream(path).use { input ->
                var offset = 0
                val capacity = buffer.size
                while (offset < capacity) {
                    val read = input.read(buffer, offset, capacity - offset)
                    if (read < 0) return@use offset
                    if (read == 0) return@use -1
                    offset += read
                }
                if (input.read() >= 0) return@use -1
                offset
            }
        } catch (t: Throwable) {
            RuntimeFatality.throwIfFatal(t)
            -1
        }
    }

    private fun shouldShowBatteryInfo(current: Snapshot): Boolean {
        if (!current.batteryInCharge) return true
        val chargeUtils = chargeUtilsClass ?: return true
        val batteryStatus =
            ModuleHelper.getStaticObjectFieldSilently(chargeUtils, "sBatteryStatus")
        if (ModuleHelper.NOT_EXIST_SYMBOL == batteryStatus) return false
        return XposedHelpers.callMethod(batteryStatus, "isCharging") as? Boolean ?: false
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
    internal fun readSnapshot(
        prefs: PrefMap<String, Any>,
        showBatteryDetail: Boolean =
            prefs.getBoolean("system_statusbar_batterytempandcurrent"),
        showDeviceTemp: Boolean =
            prefs.getBoolean("system_statusbar_showdevicetemperature")
    ): Snapshot {
        return Snapshot(
            showBatteryDetail = showBatteryDetail,
            showDeviceTemp = showDeviceTemp,
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

    @JvmStatic
    internal fun buildBatteryInfo(
        current: Snapshot,
        batteryTemperature: Int,
        batteryCurrentNow: Int,
        batteryVoltageNow: Int
    ): String {
        val opt = current.batteryContentOpt
        var simpleTemp = ""
        if (opt == 1 || opt == 4) {
            val value = batteryTemperature
            simpleTemp = if (current.batteryTempDecimal) {
                (value / 10f).toString()
            } else if (value % 10 == 0) {
                (value / 10).toString()
            } else {
                (value / 10f).toString()
            }
        }

        val ratio = if (current.batteryFixCurrentRatio) 1f else 1_000f
        var rawCurrent = -1 * Math.round(batteryCurrentNow / ratio)
        var currentText = ""
        var currentUnit = "mA"
        if (opt == 1 || opt == 3 || opt == 5) {
            if (current.batteryPositive) rawCurrent = Math.abs(rawCurrent)
            if (Math.abs(rawCurrent) > 999) {
                currentText = format2(rawCurrent / 1_000f)
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
            val volts = batteryVoltageNow / 1_000f / 1_000f
            watts = format2(Math.abs(volts * rawCurrent) / 1_000f)
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
        batteryTemperature: Int,
        cpuTemperature: Int
    ): String {
        val opt =
            if (current.deviceTempContentOpt in 1..3) current.deviceTempContentOpt else 1
        val unit = if (current.deviceTempHideUnit) "" else "\u2103"
        val separator = if (current.deviceTempSingleRow) " " else "\n"
        return when (opt) {
            1 -> {
                val battery = format1(batteryTemperature / 10f)
                val cpu = format1(cpuTemperature / 1_000f)
                if (current.deviceTempReverseOrder) {
                    "$cpu$unit$separator$battery$unit"
                } else {
                    "$battery$unit$separator$cpu$unit"
                }
            }
            2 -> format1(batteryTemperature / 10f) + unit
            else -> format1(cpuTemperature / 1_000f) + unit
        }
    }
}
