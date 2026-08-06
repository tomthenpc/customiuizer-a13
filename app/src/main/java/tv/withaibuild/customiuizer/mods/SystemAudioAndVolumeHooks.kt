package tv.withaibuild.customiuizer.mods

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
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
        callsResId = MainModule.getResHooks().addResource("ring_volume_option_newtitle", R.string.calls)
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

        installNotificationVolumeReadSettingsHook(lpparam)

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
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    XposedHelpers.log("NotificationVolumeSettingsHook", "Unable to find class/method in Settings to hook")
                }
            }
        })
    }

    @JvmStatic
    fun VolumeStepsHook(lpparam: SystemServerStartingParam) {
        val audioClass = XposedHelpers.findClassIfExists("com.android.server.audio.AudioService", lpparam.classLoader) ?: return
        val maxStreamVolumeField = XposedHelpers.findFieldIfExists(audioClass, "MAX_STREAM_VOLUME") ?: return
        val mult = MainModule.mPrefs.getInt("system_volumesteps", 0)
        if (mult <= 0) return

        ModuleHelper.findAndHookMethod(audioClass, "createStreamStates", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val maxStreamVolume = maxStreamVolumeField.get(null) as? IntArray ?: return
                for (i in maxStreamVolume.indices) {
                    maxStreamVolume[i] = Math.round(maxStreamVolume[i] * mult / 100.0f)
                }
                maxStreamVolumeField.set(null, maxStreamVolume)
            }
        })
    }

    private fun installNotificationVolumeReadSettingsHook(lpparam: SystemServerStartingParam) {
        val volumeStreamStateClass = XposedHelpers.findClassIfExists(
            "com.android.server.audio.AudioService\$VolumeStreamState",
            lpparam.classLoader
        ) ?: return
        val audioServiceClass = volumeStreamStateClass.enclosingClass ?: return

        val audioSystemClass = XposedHelpers.findClassIfExists("android.media.AudioSystem", lpparam.classLoader)
        val settingsSystemClass = XposedHelpers.findClassIfExists("android.provider.Settings\$System", lpparam.classLoader)
        if (audioSystemClass == null || settingsSystemClass == null) {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: framework class not found")
            return
        }

        val mStreamTypeField = XposedHelpers.findFieldIfExists(volumeStreamStateClass, "mStreamType") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: mStreamType not found")
            return
        }
        val mIndexMapField = XposedHelpers.findFieldIfExists(volumeStreamStateClass, "mIndexMap") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: mIndexMap not found")
            return
        }
        val outerThisField = XposedHelpers.findFieldIfExists(volumeStreamStateClass, "this\$0") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: enclosing instance not found")
            return
        }
        val mContentResolverField = XposedHelpers.findFieldIfExists(audioServiceClass, "mContentResolver") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: mContentResolver not found")
            return
        }

        val deviceOutAllSetField = XposedHelpers.findFieldIfExists(audioSystemClass, "DEVICE_OUT_ALL_SET") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: DEVICE_OUT_ALL_SET not found")
            return
        }
        val deviceOutDefaultField = XposedHelpers.findFieldIfExists(audioSystemClass, "DEVICE_OUT_DEFAULT") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: DEVICE_OUT_DEFAULT not found")
            return
        }
        val defaultStreamVolumeField = XposedHelpers.findFieldIfExists(audioSystemClass, "DEFAULT_STREAM_VOLUME") ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: DEFAULT_STREAM_VOLUME not found")
            return
        }

        val getSettingNameForDeviceMethod = XposedHelpers.findMethodExactIfExists(
            volumeStreamStateClass, "getSettingNameForDevice", Int::class.javaPrimitiveType
        ) ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: getSettingNameForDevice not found")
            return
        }
        val getValidIndexMethod = XposedHelpers.findMethodExactIfExists(
            volumeStreamStateClass, "getValidIndex", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
        ) ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: getValidIndex not found")
            return
        }
        val getIntForUserMethod = XposedHelpers.findMethodExactIfExists(
            settingsSystemClass, "getIntForUser",
            ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        ) ?: run {
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: Settings.System.getIntForUser not found")
            return
        }

        val deviceOutDefault = try {
            deviceOutDefaultField.getInt(null)
        } catch (t: Throwable) {
            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
            XposedHelpers.log("NotificationVolumeServiceHook", "AudioService readSettings hook skipped: cannot read DEVICE_OUT_DEFAULT")
            return
        }

        ModuleHelper.findAndHookMethod(volumeStreamStateClass, "readSettings", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val thisObject = param.thisObject
                val streamType = try {
                    mStreamTypeField.getInt(thisObject)
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    return
                }
                if (streamType != 1) return

                val outerThis = try {
                    outerThisField.get(thisObject)
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    return
                }
                val contentResolver = try {
                    mContentResolverField.get(outerThis)
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    return
                }
                val indexMap = try {
                    mIndexMapField.get(thisObject) as? SparseIntArray
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    return
                } ?: return

                val deviceOutAll = try {
                    deviceOutAllSetField.get(null)
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    return
                } as? Set<*> ?: return

                val defaultStreamVolume = try {
                    defaultStreamVolumeField.get(null)
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                    return
                } as? IntArray ?: return

                synchronized(volumeStreamStateClass) {
                    for (rawDeviceType in deviceOutAll) {
                        val deviceType = rawDeviceType as? Int ?: continue
                        val name = try {
                            getSettingNameForDeviceMethod.invoke(thisObject, deviceType) as? String
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                            continue
                        } ?: continue

                        val defaultValue = if (deviceType == deviceOutDefault) defaultStreamVolume[streamType] else -1
                        val index = try {
                            getIntForUserMethod.invoke(null, contentResolver, name, defaultValue, -2) as? Int
                        } catch (t: Throwable) {
                            if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                            continue
                        } ?: continue

                        if (index != -1) {
                            val validIndex = try {
                                getValidIndexMethod.invoke(thisObject, 10 * index, true) as? Int
                            } catch (t: Throwable) {
                                if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                                continue
                            } ?: continue

                            indexMap.put(deviceType, validIndex)
                        }
                    }
                }

                try {
                    mIndexMapField.set(thisObject, indexMap)
                } catch (t: Throwable) {
                    if (t is OutOfMemoryError || t is ThreadDeath || t is VirtualMachineError) throw t
                }
                param.returnAndSkip(null)
            }
        })
    }
}
