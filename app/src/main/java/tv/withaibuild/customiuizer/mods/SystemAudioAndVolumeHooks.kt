package tv.withaibuild.customiuizer.mods

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.provider.Settings
import android.util.SparseIntArray
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import java.lang.reflect.Modifier

object SystemAudioAndVolumeHooks {

    private var callsResId = 0

    @JvmStatic
    fun NotificationVolumeSettingsRes() {
        callsResId = MainModule.resHooks.addResource("ring_volume_option_newtitle", R.string.calls)
    }

    @JvmStatic
    fun NotificationVolumeServiceHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.audio.AudioService", lpparam.classLoader, "updateStreamVolumeAlias", Boolean::class.javaPrimitiveType, String::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mStreamVolumeAlias = XposedHelpers.getObjectField(param.thisObject, "mStreamVolumeAlias") as? IntArray ?: return
                mStreamVolumeAlias[1] = 1
                mStreamVolumeAlias[5] = 5
                XposedHelpers.setObjectField(param.thisObject, "mStreamVolumeAlias", mStreamVolumeAlias)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.server.audio.AudioService\$VolumeStreamState", lpparam.classLoader, "readSettings", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mStreamType = XposedHelpers.getIntField(param.thisObject, "mStreamType")
                if (mStreamType != 1) return
                synchronized(param.member.declaringClass) {
                    val audioSystem = XposedHelpers.findClass("android.media.AudioSystem", lpparam.classLoader)
                    val DEVICE_OUT_ALL = XposedHelpers.getStaticObjectField(audioSystem, "DEVICE_OUT_ALL_SET") as? Set<Int> ?: return
                    val DEVICE_OUT_DEFAULT = XposedHelpers.getStaticIntField(audioSystem, "DEVICE_OUT_DEFAULT")
                    val DEFAULT_STREAM_VOLUME = XposedHelpers.getStaticObjectField(audioSystem, "DEFAULT_STREAM_VOLUME") as? IntArray ?: return
                    val mContentResolver = XposedHelpers.getObjectField(XposedHelpers.getSurroundingThis(param.thisObject), "mContentResolver")
                    val mIndexMap = XposedHelpers.getObjectField(param.thisObject, "mIndexMap") as? SparseIntArray ?: return
                    for (deviceType in DEVICE_OUT_ALL) {
                        val name = XposedHelpers.callMethod(param.thisObject, "getSettingNameForDevice", deviceType) as? String ?: continue
                        val defaultValue = if (deviceType == DEVICE_OUT_DEFAULT) DEFAULT_STREAM_VOLUME[mStreamType] else -1
                        val index = XposedHelpers.callStaticMethod(Settings.System::class.java, "getIntForUser", mContentResolver, name, defaultValue, -2) as? Int ?: continue
                        if (index != -1) {
                            val validIndex = XposedHelpers.callMethod(param.thisObject, "getValidIndex", 10 * index, true) as? Int ?: continue
                            mIndexMap.put(deviceType, validIndex)
                        }
                    }
                    XposedHelpers.setObjectField(param.thisObject, "mIndexMap", mIndexMap)
                }
                param.returnAndSkip(null)
            }
        })

        ModuleHelper.findAndHookMethodSilently("com.android.server.audio.AudioService", lpparam.classLoader, "shouldZenMuteStream", Int::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mStreamType = param.args[0] as? Int ?: return
                if (mStreamType == 5 && param.result != true) {
                    val mNm = XposedHelpers.getObjectField(param.thisObject, "mNm")
                    val mZenMode = XposedHelpers.callMethod(mNm, "getZenMode") as? Int ?: return
                    if (mZenMode == 1) param.setResult(true)
                }
            }
        })
    }

    @JvmStatic
    fun NotificationVolumeSettingsHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.settings.MiuiSoundSettings", lpparam.classLoader, "onCreate", Bundle::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val fragment = param.thisObject
                val context = XposedHelpers.callMethod(fragment, "getActivity") as? Context ?: return
                val modRes = ModuleHelper.getModuleRes(context)
                var order = 6

                var addPreference = "addPreference"
                try {
                    val vsbCls = XposedHelpers.findClassIfExists("com.android.settings.sound.VolumeSeekBarPreference", lpparam.classLoader) ?: return
                    val initSeekBar = XposedHelpers.findMethodsByExactParameters(fragment.javaClass, Void.TYPE, String::class.java, Int::class.java, Int::class.java)
                    if (initSeekBar.isEmpty()) {
                        XposedHelpers.log("NotificationVolumeSettingsHook", "Unable to find class/method in Settings to hook")
                        return
                    } else {
                        initSeekBar[0].isAccessible = true
                    }
                    val pgCls = XposedHelpers.findClassIfExists("androidx.preference.PreferenceGroup", lpparam.classLoader) ?: return
                    val pCls = XposedHelpers.findClassIfExists("androidx.preference.Preference", lpparam.classLoader) ?: return
                    val methods = XposedHelpers.findMethodsByExactParameters(pgCls, Void.TYPE, pCls)
                    for (method in methods) {
                        if (Modifier.isPublic(method.modifiers)) {
                            addPreference = method.name
                            break
                        }
                    }

                    val media = XposedHelpers.callMethod(fragment, "findPreference", "media_volume")
                    if (media != null) order = XposedHelpers.callMethod(media, "getOrder") as? Int ?: 6

                    val prefScreen = XposedHelpers.callMethod(fragment, "getPreferenceScreen")

                    var pref = XposedHelpers.newInstance(vsbCls, context)
                    XposedHelpers.callMethod(pref, "setKey", "notification_volume")
                    XposedHelpers.callMethod(pref, "setTitle", modRes.getString(R.string.system_mods_notifications))
                    XposedHelpers.callMethod(pref, "setPersistent", true)
                    XposedHelpers.callMethod(prefScreen, addPreference, pref)
                    initSeekBar[0].invoke(fragment, "notification_volume", 5, context.resources.getIdentifier("ic_audio_notification", "drawable", context.packageName))
                    XposedHelpers.callMethod(pref, "setOrder", order)

                    pref = XposedHelpers.newInstance(vsbCls, context)
                    XposedHelpers.callMethod(pref, "setKey", "system_volume")
                    XposedHelpers.callMethod(pref, "setTitle", modRes.getString(R.string.system_volume))
                    XposedHelpers.callMethod(pref, "setPersistent", true)
                    XposedHelpers.callMethod(prefScreen, addPreference, pref)
                    initSeekBar[0].invoke(fragment, "system_volume", 1, context.resources.getIdentifier("ic_audio_vol", "drawable", context.packageName))
                    XposedHelpers.callMethod(pref, "setOrder", order)

                    val mRingVolume = XposedHelpers.callMethod(param.thisObject, "findPreference", "ring_volume")
                    XposedHelpers.callMethod(mRingVolume, "setTitle", callsResId)
                } catch (t: Throwable) {
                    XposedHelpers.log("NotificationVolumeSettingsHook", "Unable to find class/method in Settings to hook")
                }
            }
        })
    }

    @JvmStatic
    fun VolumeStepsHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.android.server.audio.AudioService", lpparam.classLoader, "createStreamStates", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val audioCls = XposedHelpers.findClass("com.android.server.audio.AudioService", lpparam.classLoader)
                val maxStreamVolume = XposedHelpers.getStaticObjectField(audioCls, "MAX_STREAM_VOLUME") as? IntArray ?: return
                val mult = MainModule.mPrefs.getInt("system_volumesteps", 0)
                if (mult <= 0) return
                for (i in maxStreamVolume.indices) {
                    maxStreamVolume[i] = Math.round(maxStreamVolume[i] * mult / 100.0f)
                }
                XposedHelpers.setStaticObjectField(audioCls, "MAX_STREAM_VOLUME", maxStreamVolume)
            }
        })
    }
}
