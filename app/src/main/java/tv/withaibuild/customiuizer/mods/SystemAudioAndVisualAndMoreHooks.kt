package tv.withaibuild.customiuizer.mods

import android.app.Notification
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.text.SpannableString
import android.view.Gravity
import android.widget.FrameLayout
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.TypefaceSpan
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.utils.AudioVisualizer
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.utils.Helpers
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.text.SimpleDateFormat
import java.util.*

object SystemAudioAndVisualAndMoreHooks {

    private const val StatusBarCls = "com.android.systemui.statusbar.phone.CentralSurfacesImpl"
    private var audioFocusPkg: String? = null

    @Suppress("UNCHECKED_CAST")
    private fun removeListener(thisObject: Any) {
        val mRecords = XposedHelpers.getObjectField(thisObject, "mRecords") as? ArrayList<Any> ?: return
        for (record in mRecords) {
            val callingPackage = XposedHelpers.getObjectField(record, "callingPackage") as? String ?: continue
            var events = XposedHelpers.getIntField(record, "events")
            if ((events and PhoneStateListener.LISTEN_CALL_STATE) == PhoneStateListener.LISTEN_CALL_STATE &&
                MainModule.mPrefs.getStringSet("system_ignorecalls_apps").contains(callingPackage)) {
                events = events and PhoneStateListener.LISTEN_CALL_STATE.inv()
                XposedHelpers.setIntField(record, "events", events)
            }
        }
    }

    @JvmStatic
    fun NoCallInterruptionHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.audio.AudioService", lpparam.classLoader, "requestAudioFocus", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if ("AudioFocus_For_Phone_Ring_And_Calls" == param.args[4] && audioFocusPkg != null &&
                    MainModule.mPrefs.getStringSet("system_ignorecalls_apps").contains(audioFocusPkg))
                    param.returnAndSkip(1)
            }

            override fun after(param: AfterHookCallback) {
                val res = param.result as? Int ?: return
                if (res != AudioManager.AUDIOFOCUS_REQUEST_FAILED && "AudioFocus_For_Phone_Ring_And_Calls" != param.args[4])
                    audioFocusPkg = param.args[5] as? String
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.TelephonyRegistry", lpparam.classLoader, "notifyCallState", Int::class.javaPrimitiveType, String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                removeListener(param.thisObject)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.TelephonyRegistry", lpparam.classLoader, "notifyCallStateForPhoneId", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                removeListener(param.thisObject)
            }
        })
    }

    @JvmStatic
    fun FirstVolumePressHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.audio.AudioService\$VolumeController", lpparam.classLoader, "suppressAdjustment", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val streamType = param.args[0] as? Int ?: return
                if (streamType != AudioManager.STREAM_MUSIC) return
                val isMuteAdjust = param.args[2] as? Boolean ?: return
                if (isMuteAdjust) return
                val mController = XposedHelpers.getObjectField(param.thisObject, "mController")
                if (mController == null) return
                param.setResult(false)
            }
        })
    }

    @JvmStatic
    fun AllRotationsHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllConstructors("com.android.server.wm.DisplayRotation", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setIntField(param.thisObject, "mAllowAllRotations", if (MainModule.mPrefs.getStringAsInt("system_allrotations2", 1) == 2) 1 else 0)
            }
        })
    }

    @JvmStatic
    fun ScreenDimTimeHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.power.PowerManagerService", lpparam.classLoader, "readConfigurationLocked", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.setIntField(param.thisObject, "mScreenOffTimeoutSetting", MainModule.mPrefs.getInt("system_screendimtime", 15000))
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.power.PowerManagerService", lpparam.classLoader, "setStayOnSettingInternal", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (MainModule.mPrefs.getInt("system_screendimtime", 15000) == 0) param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun NoOverscrollAppHook(lpparam: PackageReadyParam) {
        val hookParam = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = false
            }
        }

        val sblCls = XposedHelpers.findClassIfExists("miuix.springback.view.SpringBackLayout", lpparam.classLoader)
        if (sblCls != null) {
            ModuleHelper.hookAllConstructors(sblCls, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    try {
                        XposedHelpers.callMethod(param.thisObject, "setSpringBackEnable", false)
                    } catch (t: Throwable) {
                        try { XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false) } catch (ignore: Throwable) {}
                    }
                }
            })
            ModuleHelper.findAndHookMethodSilently(sblCls, "setSpringBackEnable", Boolean::class.javaPrimitiveType, hookParam)
        }

        val rrvCls = XposedHelpers.findClassIfExists("androidx.recyclerview.widget.RemixRecyclerView", lpparam.classLoader)
        if (rrvCls != null) {
            ModuleHelper.hookAllConstructors(rrvCls, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    (param.thisObject as? View)?.overScrollMode = View.OVER_SCROLL_NEVER
                    try {
                        XposedHelpers.callMethod(param.thisObject, "setSpringEnabled", false)
                    } catch (t: Throwable) {
                        try { XposedHelpers.setBooleanField(param.thisObject, "mSpringEnabled", false) } catch (ignore: Throwable) {}
                    }
                }
            })
            ModuleHelper.findAndHookMethodSilently(rrvCls, "setSpringEnabled", Boolean::class.javaPrimitiveType, hookParam)
        }

        ModuleHelper.findAndHookMethod("android.widget.AbsListView", lpparam.classLoader, "initAbsListView", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                (param.thisObject as? AbsListView)?.overScrollMode = View.OVER_SCROLL_NEVER
            }
        })
    }

    @JvmStatic
    fun AllowAllKeyguardHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod(Notification::class.java, "setEnableKeyguard", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = true
            }
        })

        ModuleHelper.findAndHookMethod(Notification::class.java, "isEnableKeyguard", HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.notification.MiuiNotificationCompat", lpparam.classLoader, "isEnableKeyguard", Notification::class.java, HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun AllowAllFloatHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod(Notification::class.java, "setEnableFloat", Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                param.args[0] = true
            }
        })

        ModuleHelper.findAndHookMethod(Notification::class.java, "isEnableFloat", HookerClassHelper.returnConstant(true))
        ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.notification.MiuiNotificationCompat", lpparam.classLoader, "isEnableFloat", Notification::class.java, HookerClassHelper.returnConstant(true))
    }

    @JvmStatic
    fun AllowDirectReplyHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl", lpparam.classLoader, "shouldAllowLockscreenRemoteInput", HookerClassHelper.returnConstant(true))
        if (!ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl", lpparam.classLoader, "setLockscreenAllowRemoteInput", Boolean::class.javaPrimitiveType, HookerClassHelper.returnConstant(true))) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl", lpparam.classLoader, "setLockScreenAllowRemoteInput", Boolean::class.javaPrimitiveType, HookerClassHelper.returnConstant(true))
        }
    }

    @JvmStatic
    fun HideQSHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod(StatusBarCls, lpparam.classLoader, "onStateChanged", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mNotificationPanel = XposedHelpers.getObjectField(param.thisObject, "mQSContainer")
                if (param.args[0] == 1) {
                    XposedHelpers.callMethod(mNotificationPanel, "setShowQSPanel", false)
                } else {
                    val mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler") as? Handler ?: return
                    mHandler.postDelayed({
                        XposedHelpers.callMethod(mNotificationPanel, "setShowQSPanel", true)
                    }, 300)
                }
            }
        })
    }

    @JvmStatic
    fun LockScreenTimeoutHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.NotificationShadeWindowControllerImpl", lpparam.classLoader, "applyUserActivityTimeout", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mLpChanged = XposedHelpers.getObjectField(param.thisObject, "mLpChanged") ?: return
                val userActivityTimeout = XposedHelpers.getLongField(mLpChanged, "userActivityTimeout")
                if (userActivityTimeout > 0)
                    XposedHelpers.setLongField(mLpChanged, "userActivityTimeout", MainModule.mPrefs.getInt("system_lstimeout", 3) * 1000L)
            }
        })
    }

    private val formatter = SimpleDateFormat("H:m", Locale.ENGLISH)

    @JvmStatic
    fun MuffledVibrationHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.android.server.VibratorService", lpparam.classLoader, "doVibratorOn", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val ratioRinger = MainModule.mPrefs.getInt("system_vibration_amp_ringer", 100) / 100f
                val ratioNotif = MainModule.mPrefs.getInt("system_vibration_amp_notif", 100) / 100f
                val ratioOther = MainModule.mPrefs.getInt("system_vibration_amp_other", 100) / 100f

                var isRingtone = false
                var isNotification = false
                val mCurrentVibration = XposedHelpers.getObjectField(param.thisObject, "mCurrentVibration")
                if (mCurrentVibration != null) try {
                    isRingtone = XposedHelpers.callMethod(mCurrentVibration, "isRingtone") as? Boolean ?: false
                    isNotification = XposedHelpers.callMethod(mCurrentVibration, "isNotification") as? Boolean ?: false
                } catch (t: Throwable) {
                    val mUsageHint = XposedHelpers.getIntField(mCurrentVibration, "mUsageHint")
                    isRingtone = mUsageHint == 6
                    isNotification = mUsageHint == 5 || mUsageHint == 7 || mUsageHint == 8 || mUsageHint == 9
                }

                val ratio: Float
                if (isRingtone) ratio = ratioRinger
                else if (isNotification) ratio = ratioNotif
                else ratio = ratioOther
                if (ratio == 1.0f) return

                val key = "system_vibration_amp_period"
                val startHour = MainModule.mPrefs.getInt(key + "_start_hour", 0)
                val startMinute = MainModule.mPrefs.getInt(key + "_start_minute", 0)
                val endHour = MainModule.mPrefs.getInt(key + "_end_hour", 0)
                val endMinute = MainModule.mPrefs.getInt(key + "_end_minute", 0)

                formatter.timeZone = TimeZone.getDefault()
                val start = formatter.parse("$startHour:$startMinute")
                val end = formatter.parse("$endHour:$endMinute")
                val now = formatter.parse(formatter.format(Date()))

                val insidePeriod = if (start.before(end)) now.after(start) && now.before(end) else now.before(end) || now.after(start)
                if (!insidePeriod) return

                val mSupportsAmplitudeControl = try {
                    XposedHelpers.getBooleanField(param.thisObject, "mSupportsAmplitudeControl")
                } catch (ignored: Throwable) {
                    false
                }

                if (mSupportsAmplitudeControl) {
                    param.args[1] = Math.round(((param.args[1] as? Int ?: -1).let { if (it == -1) XposedHelpers.getIntField(param.thisObject, "mDefaultVibrationAmplitude") else it } * ratio))
                } else {
                    param.args[0] = Math.max(3, Math.round((param.args[0] as? Long ?: 0L) * ratio))
                }
            }
        })
    }

    private val formatterAlarm = SimpleDateFormat("H:m", Locale.ENGLISH)

    private fun hookUpdateTime(thisObject: Any, isSingle: Boolean) {
        try {
            var mCurrentDate: TextView? = null
            var mCurrentDateLarge: TextView? = null
            if (isSingle) {
                try { mCurrentDate = XposedHelpers.getObjectField(thisObject, "mCurrentDate") as? TextView } catch (ignore: Throwable) {}
                try { mCurrentDateLarge = XposedHelpers.getObjectField(thisObject, "mCurrentDateLarge") as? TextView } catch (ignore: Throwable) {}
            } else {
                try { mCurrentDate = XposedHelpers.getObjectField(thisObject, "mLocalDate") as? TextView } catch (ignore: Throwable) {}
            }
            if (mCurrentDate == null && mCurrentDateLarge == null) return

            val mContext = mCurrentDate?.context ?: mCurrentDateLarge?.context ?: return

            var timestamp = ModuleHelper.getNextMIUIAlarmTime(mContext)
            if (timestamp == 0L && MainModule.mPrefs.getBoolean("system_lsalarm_all"))
                timestamp = Helpers.getNextStockAlarmTime(mContext)
            if (timestamp == 0L) return

            val alarmStr = StringBuilder()
            alarmStr.append("\n").append(ModuleHelper.getModuleRes(mContext).getString(R.string.system_statusbaricons_alarm_title)).append(" ")
            val format = MainModule.mPrefs.getStringAsInt("system_lsalarm_format", 1)
            if (format == 1 || format == 3) {
                val dateFormat = SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), if (DateFormat.is24HourFormat(mContext)) "EHmm" else "EHmma"), Locale.getDefault())
                dateFormat.timeZone = TimeZone.getDefault()
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = timestamp
                alarmStr.append(dateFormat.format(calendar.time))
            }
            if (format == 2 || format == 3) {
                val timeStr = StringBuilder(DateUtils.getRelativeTimeSpanString(timestamp, java.lang.System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE))
                timeStr.setCharAt(0, Character.toLowerCase(timeStr[0]))
                alarmStr.append(if (format == 3) " ($timeStr)" else timeStr)
            }
            if (mCurrentDate != null) {
                mCurrentDate.textAlignment = View.TEXT_ALIGNMENT_INHERIT
                mCurrentDate.setLineSpacing(0f, 1.5f)
                mCurrentDate.append(alarmStr)
                if (isSingle) {
                    val pos = Settings.System.getInt(mContext.contentResolver, "selected_keyguard_clock_position", 0)
                    if (pos != 2 && pos != 4) mCurrentDate.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
            }
            if (mCurrentDateLarge != null) {
                val resId = mCurrentDateLarge.resources.getIdentifier("miui_clock_date_text_size", "dimen", "com.android.systemui")
                val fontSize = if (resId == 0) Math.round(mCurrentDateLarge.resources.displayMetrics.density * 14.0f) else mCurrentDateLarge.resources.getDimensionPixelSize(resId)
                alarmStr.insert(1, "\n\n ")
                val span = SpannableString(alarmStr)
                span.setSpan(AbsoluteSizeSpan(fontSize, false), 0, alarmStr.length, 0)
                span.setSpan(TypefaceSpan("sans-serif"), 0, alarmStr.length, 0)
                mCurrentDateLarge.append(span)
            }
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }

    @JvmStatic
    fun LockScreenAlarmHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.keyguard.clock.MiuiKeyguardSingleClock", lpparam.classLoader, "updateTime", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mMiuiBaseClock = XposedHelpers.getObjectField(param.thisObject, "mMiuiBaseClock")
                if (mMiuiBaseClock != null) hookUpdateTime(mMiuiBaseClock, true)
            }
        })
        ModuleHelper.findAndHookMethod("com.android.keyguard.clock.MiuiKeyguardDualClock", lpparam.classLoader, "updateTime", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mMiuiDualClock = XposedHelpers.getObjectField(param.thisObject, "mMiuiDualClock")
                if (mMiuiDualClock != null) hookUpdateTime(mMiuiDualClock, false)
            }
        })
    }

    @JvmStatic
    fun ScreenshotConfigHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("android.content.ContentResolver", lpparam.classLoader, "update", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (param.args.size != 4) return
                val contentValues = param.args[1] as? ContentValues ?: return
                var displayName = contentValues.getAsString("_display_name")
                if (displayName != null && displayName.contains("Screenshot")) {
                    val format = MainModule.mPrefs.getStringAsInt("system_screenshot_format", 2)
                    val ext = if (format <= 2) ".jpg" else if (format == 3) ".png" else ".webp"
                    displayName = displayName.replace(".png", "").replace(".jpg", "").replace(".webp", "") + ext
                    contentValues.put("_display_name", displayName)
                }
            }
        })

        ModuleHelper.findAndHookMethod("android.content.ContentResolver", lpparam.classLoader, "insert", Uri::class.java, ContentValues::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val imgUri = param.args[0] as? Uri ?: return
                val contentValues = param.args[1] as? ContentValues ?: return
                val displayName = contentValues.getAsString("_display_name")
                if (MediaStore.Images.Media.EXTERNAL_CONTENT_URI == imgUri && displayName != null && displayName.contains("Screenshot")) {
                    val folder = MainModule.mPrefs.getStringAsInt("system_screenshot_path", 1)
                    val dir = MainModule.mPrefs.getString("system_screenshot_mypath", "")
                    val format = MainModule.mPrefs.getStringAsInt("system_screenshot_format", 2)
                    val ext = if (format <= 2) ".jpg" else if (format == 3) ".png" else ".webp"

                    var mScreenshotDir: File?
                    var newDisplayName = displayName.replace(".png", "").replace(".jpg", "").replace(".webp", "") + ext
                    if (folder > 1) {
                        mScreenshotDir = if (folder == 4 && !TextUtils.isEmpty(dir)) {
                            File(dir)
                        } else {
                            File(Environment.getExternalStoragePublicDirectory(if (folder == 2) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DCIM), "Screenshots")
                        }
                        if (mScreenshotDir.exists() == false) mScreenshotDir.mkdirs()
                        val relativePath = mScreenshotDir.path.replace(Environment.getExternalStorageDirectory().path + File.separator, "")
                        contentValues.put("relative_path", relativePath)
                        if (contentValues.getAsString("_data") != null) {
                            contentValues.put("_data", mScreenshotDir.path + "/" + newDisplayName)
                        }
                    }
                    contentValues.put("_display_name", newDisplayName)
                }
            }
        })

        val format = MainModule.mPrefs.getStringAsInt("system_screenshot_format", 2)
        if (format > 2) {
            ModuleHelper.findAndHookMethod("com.miui.screenshot.MiuiScreenshotApplication", lpparam.classLoader, "attachBaseContext", Context::class.java, object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mContext = param.args[0] as? Context ?: return
                    val versionCode = mContext.packageManager.getPackageInfo(mContext.packageName, 0).longVersionCode
                    val changeFormatHook = object : MethodHook() {
                        override fun before(param: BeforeHookCallback) {
                            if (param.args.size < 7) return
                            val compress = if (format <= 2) Bitmap.CompressFormat.JPEG else if (format == 3) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.WEBP
                            param.args[4] = compress
                        }
                    }
                    when {
                        versionCode >= 10400056 -> ModuleHelper.hookAllMethods("com.miui.screenshot.u0.f\$a", lpparam.classLoader, "a", changeFormatHook)
                        versionCode >= 10400034 -> ModuleHelper.hookAllMethods("com.miui.screenshot.x0.e\$a", lpparam.classLoader, "a", changeFormatHook)
                    }
                }
            })
        }

        ModuleHelper.hookAllMethods("android.graphics.Bitmap", lpparam.classLoader, "compress", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val quality = param.args[1] as? Int ?: return
                if (quality != 100 || param.args[2] is ByteArrayOutputStream) return
                val format = MainModule.mPrefs.getStringAsInt("system_screenshot_format", 2)
                var newQuality = MainModule.mPrefs.getInt("system_screenshot_quality", 100)
                if (format == 3) newQuality = 100
                val compress = if (format <= 2) Bitmap.CompressFormat.JPEG else if (format == 3) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.WEBP
                param.args[0] = compress
                param.args[1] = newQuality
            }
        })
    }

    @JvmStatic
    fun ToastTimeHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.notification.NotificationManagerService", lpparam.classLoader, "showNextToastLocked", object : MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.callMethod(param.thisObject, "getContext") as? Context ?: return
                val mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler") as? Handler ?: return
                val mToastQueue = XposedHelpers.getObjectField(param.thisObject, "mToastQueue") as? ArrayList<Any> ?: return
                if (mToastQueue.size == 0) return
                val mod = (MainModule.mPrefs.getInt("system_toasttime", 0) - 4) * 1000
                for (record in mToastQueue) {
                    if (record != null && mHandler.hasMessages(2, record)) {
                        mHandler.removeCallbacksAndMessages(record)
                        val duration = XposedHelpers.getIntField(record, "duration")
                        val delay = Math.max(1000, (if (duration == 1) 3500 else 2000) + mod)
                        mHandler.sendMessageDelayed(Message.obtain(mHandler, 2, record), delay.toLong())
                    }
                }
            }
        })

        val windowClass = "com.android.server.wm.DisplayPolicy"
        ModuleHelper.hookAllMethods(windowClass, lpparam.classLoader, "adjustWindowParamsLw", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val lp = if (param.args.size == 1) param.args[0] else param.args[1]
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mPrevHideTimeout", XposedHelpers.getLongField(lp, "hideTimeoutMilliseconds"))
            }

            override fun after(param: AfterHookCallback) {
                val lp = if (param.args.size == 1) param.args[0] else param.args[1]
                val mPrevHideTimeout = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mPrevHideTimeout") as? Long ?: return
                val mHideTimeout = XposedHelpers.getLongField(lp, "hideTimeoutMilliseconds")
                if (mPrevHideTimeout == -1L || mHideTimeout == -1L) return

                var dur = 0L
                if (mPrevHideTimeout == 1000L || mPrevHideTimeout == 4000L || mPrevHideTimeout == 5000L || mPrevHideTimeout == 7000L || mPrevHideTimeout != mHideTimeout)
                    dur = Math.max(1000, 3500 + (MainModule.mPrefs.getInt("system_toasttime", 0) - 4) * 1000).toLong()
                if (dur != 0L) XposedHelpers.setLongField(lp, "hideTimeoutMilliseconds", dur)
            }
        })
    }

    @JvmStatic
    fun ClearAllTasksHook(lpparam: SystemServerStartingParam) {
        val wpuClass = "com.android.server.wm.WindowProcessUtils"
        ModuleHelper.hookAllMethods(wpuClass, lpparam.classLoader, "getPerceptibleRecentAppList", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                param.setResult(null)
            }
        })
    }

    @JvmStatic
    fun InactiveBrightnessSliderHook(lpparam: PackageReadyParam) {
        val hook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getStringAsInt("system_inactivebrightness", 1)
                if (opt == 2) {
                    val mSlider = XposedHelpers.getObjectField(param.thisObject, "mSlider") as? SeekBar ?: return
                    mSlider.setOnTouchListener { _, _ -> true }
                } else if (opt == 3) {
                    try {
                        val sliderView = param.thisObject as? View ?: return
                        val lp = sliderView.layoutParams as? ViewGroup.MarginLayoutParams ?: return
                        lp.height = 0
                        lp.topMargin = Math.round(2 * sliderView.resources.displayMetrics.density)
                        lp.bottomMargin = 0
                        sliderView.layoutParams = lp
                    } catch (t: Throwable) {
                        XposedHelpers.log(t)
                    }
                }
            }
        }
        ModuleHelper.findAndHookMethod("com.android.systemui.settings.brightness.BrightnessSliderView", lpparam.classLoader, "onFinishInflate", hook)
    }

    private val startPos = FloatArray(2)

    private fun processLSEvent(param: AfterHookCallback) {
        val event = param.args[0] as? MotionEvent ?: return
        if (event.pointerCount > 1) return
        val action = event.actionMasked
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP) return

        val mKeyguardBottomArea = XposedHelpers.getObjectField(param.thisObject, "mBottomAreaView") as? ViewGroup ?: return
        val mIndicationArea = XposedHelpers.getObjectField(mKeyguardBottomArea, "mIndicationArea") as? ViewGroup ?: return
        if (!Helpers.isReallyVisible(mIndicationArea)) return

        val coord = IntArray(2)
        mIndicationArea.getLocationOnScreen(coord)
        val rect = Rect(coord[0], coord[1], coord[0] + mIndicationArea.width, coord[1] + mIndicationArea.height)
        if (!rect.contains(event.x.toInt(), event.y.toInt())) return

        if (action == MotionEvent.ACTION_DOWN) {
            startPos[0] = event.x
            startPos[1] = event.y
        } else if (action == MotionEvent.ACTION_UP) try {
            val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
            val slop = ViewConfiguration.get(mContext).scaledTouchSlop
            if (Math.abs(event.x - startPos[0]) > slop || Math.abs(event.y - startPos[1]) > slop) return
            val mPanelViewController = XposedHelpers.getObjectField(param.thisObject, "mPanelViewController")
            val statusBarKeyguardViewManager = XposedHelpers.getObjectField(mPanelViewController, "statusBarKeyguardViewManager")

            XposedHelpers.callMethod(statusBarKeyguardViewManager, "showGenericBouncer", true)
        } catch (t: Throwable) {
            XposedHelpers.log(t)
        }
    }

    @JvmStatic
    fun TapToUnlockHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods("com.android.keyguard.injector.KeyguardPanelViewInjector", lpparam.classLoader, "onTouchEvent", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                processLSEvent(param)
            }
        })

        ModuleHelper.hookAllMethods("com.android.keyguard.injector.KeyguardPanelViewInjector", lpparam.classLoader, "onInterceptTouchEvent", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                processLSEvent(param)
            }
        })
    }

    @JvmStatic
    fun TempHideOverlayAppHook(lpparam: SystemServerStartingParam) {
        val flagIndex = 2
        ModuleHelper.hookAllConstructors("com.android.server.wm.WindowSurfaceController", lpparam.classLoader, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val windowType = param.args[4] as? Int ?: return
                if (windowType != WindowManager.LayoutParams.TYPE_PHONE
                    && windowType != WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                    && windowType != WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    && windowType != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) return
                var flags = param.args[flagIndex] as? Int ?: return
                val skipFlag = 64
                flags = flags or skipFlag
                param.args[flagIndex] = flags
            }
        })
    }

    @JvmStatic
    fun GalleryScreenshotPathHook(lpparam: PackageReadyParam) {
        val MIUIStorageConstants = XposedHelpers.findClass("com.miui.gallery.storage.constants.MIUIStorageConstants", lpparam.classLoader)
        val folder = MainModule.mPrefs.getStringAsInt("system_gallery_screenshots_path", 1)
        val ssPath = when (folder) {
            2 -> Environment.DIRECTORY_PICTURES + File.separator + "Screenshots"
            3 -> Environment.DIRECTORY_DCIM + File.separator + "Screenshots"
            else -> ""
        }
        if (folder > 1) {
            XposedHelpers.setStaticObjectField(MIUIStorageConstants, "DIRECTORY_SCREENSHOT_PATH", ssPath)
        }
    }

    @JvmStatic
    fun ScreenshotFloatTimeHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.miui.screenshot.GlobalScreenshot", lpparam.classLoader, "startGotoThumbnailAnimation", Runnable::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mIsShowLongScreenShotGuide = try {
                    XposedHelpers.getBooleanField(param.thisObject, "mIsShowLongScreenShotGuide")
                } catch (ignore: Throwable) {
                    false
                }
                if (mIsShowLongScreenShotGuide) return
                val opt = MainModule.mPrefs.getInt("system_screenshot_floattime", 0)
                if (opt <= 0) return
                val mHandler = XposedHelpers.getObjectField(param.thisObject, "mHandler") as? Handler ?: return
                val mQuitThumbnailRunnable = XposedHelpers.getObjectField(param.thisObject, "mQuitThumbnailRunnable") as? Runnable ?: return
                mHandler.removeCallbacks(mQuitThumbnailRunnable)
                mHandler.postDelayed(mQuitThumbnailRunnable, opt * 1000L)
            }
        })
    }

    @JvmStatic
    fun ScrambleAppLockPINHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllConstructors("com.miui.applicationlock.widget.MiuiNumericInputView", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val keys = param.thisObject as? LinearLayout ?: return
                val mRandomViews = ArrayList<View>()
                var bottom0: View? = null
                var bottom2: View? = null
                for (row in 0..3) {
                    val cols = keys.getChildAt(row) as? ViewGroup ?: continue
                    for (col in 0..2) {
                        if (row == 3) {
                            if (col == 0) {
                                bottom0 = cols.getChildAt(col)
                                continue
                            } else if (col == 2) {
                                bottom2 = cols.getChildAt(col)
                                continue
                            }
                        }
                        mRandomViews.add(cols.getChildAt(col))
                    }
                    cols.removeAllViews()
                }

                Collections.shuffle(mRandomViews)

                var cnt = 0
                for (row in 0..3) {
                    val cols = keys.getChildAt(row) as? ViewGroup ?: continue
                    for (col in 0..2) {
                        if (row == 3) {
                            if (col == 0) {
                                bottom0?.let { cols.addView(it) }
                                continue
                            } else if (col == 2) {
                                bottom2?.let { cols.addView(it) }
                                continue
                            }
                        }
                        cols.addView(mRandomViews[cnt])
                        cnt++
                    }
                }
            }
        })
    }

    private var audioViz: AudioVisualizer? = null
    private var isKeyguardShowing = false
    private var isNotificationPanelExpanded = false
    private var mMediaController: MediaController? = null

    private fun updateAudioVisualizerState(context: Context) {
        if (audioViz == null) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val isMusicPlaying = am.isMusicActive
        var isPlaying = false
        if (mMediaController == null || mMediaController?.playbackState == null || mMediaController?.playbackState?.state != PlaybackState.STATE_PLAYING) {
            if (!audioViz!!.showWithControllerOnly) isPlaying = isMusicPlaying
        } else {
            isPlaying = isMusicPlaying && mMediaController?.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        audioViz?.updateViewState(isPlaying, isKeyguardShowing, isNotificationPanelExpanded)
    }

    @JvmStatic
    fun AudioVisualizerHook(lpparam: PackageReadyParam) {
        ModuleHelper.hookAllMethods(StatusBarCls, lpparam.classLoader, "makeStatusBarView", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val viewController = XposedHelpers.getObjectField(param.thisObject, "mNotificationPanelViewController")
                val mNotificationPanel = XposedHelpers.getObjectField(viewController, "mView") as? FrameLayout ?: return

                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val visFrame = FrameLayout(mContext)
                visFrame.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                audioViz = AudioVisualizer(mContext)
                audioViz!!.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM)
                audioViz!!.isClickable = false
                visFrame.addView(audioViz)
                visFrame.isClickable = false
                val wallpaper = mNotificationPanel.findViewById<View>(mContext.resources.getIdentifier("wallpaper", "id", lpparam.packageName))
                val themebkg = mNotificationPanel.findViewById<View>(mContext.resources.getIdentifier("theme_background", "id", lpparam.packageName))
                val awesome = mNotificationPanel.findViewById<View>(mContext.resources.getIdentifier("awesome_lock_screen_container", "id", lpparam.packageName))

                var order = 0
                if (awesome != null) order = Math.max(order, mNotificationPanel.indexOfChild(awesome))
                if (themebkg != null) order = Math.max(order, mNotificationPanel.indexOfChild(themebkg))
                if (wallpaper != null) order = Math.max(order, mNotificationPanel.indexOfChild(wallpaper))
                mNotificationPanel.addView(visFrame, order + 1)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader, "onScreenTurnedOff", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                audioViz?.updateScreenOn(false)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader, "onScreenTurnedOn", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                audioViz?.updateScreenOn(true)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl", lpparam.classLoader, "notifyKeyguardState", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val isKeyguardShowingNew = param.args[0] as? Boolean ?: false
                if (isKeyguardShowing != isKeyguardShowingNew) {
                    isKeyguardShowing = isKeyguardShowingNew
                    isNotificationPanelExpanded = false
                    updateAudioVisualizerState(XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return)
                }
            }
        })

        ModuleHelper.hookAllMethods(StatusBarCls, lpparam.classLoader, "setPanelExpanded", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val isNotificationPanelExpandedNew = XposedHelpers.getBooleanField(param.thisObject, "mPanelExpanded")
                if (isNotificationPanelExpanded != isNotificationPanelExpandedNew) {
                    isNotificationPanelExpanded = isNotificationPanelExpandedNew
                    updateAudioVisualizerState(XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.NotificationMediaManager", lpparam.classLoader, "updateMediaMetaData", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                if (audioViz == null) return
                val mContext = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context ?: return
                val powerMgr = mContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
                val isScreenOn = powerMgr.isInteractive
                if (!isScreenOn) {
                    audioViz!!.updateScreenOn(false)
                    return
                } else audioViz!!.isScreenOn = true

                val mMediaMetadata = XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata") as? MediaMetadata
                var art: Bitmap? = null
                if (mMediaMetadata != null) {
                    art = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    if (art == null) art = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    if (art == null) art = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                }
                if (art == null) {
                    val wallpaperMgr = WallpaperManager.getInstance(mContext)
                    @Suppress("MissingPermission")
                    val wallpaperDrawable: Drawable? = try { wallpaperMgr.drawable } catch (ignore: Throwable) { null }
                    if (wallpaperDrawable is BitmapDrawable) {
                        art = wallpaperDrawable.bitmap
                    }
                }

                mMediaController = XposedHelpers.getObjectField(param.thisObject, "mMediaController") as? MediaController
                updateAudioVisualizerState(mContext)
                audioViz!!.updateMusicArt(art)
            }
        })
    }
}
