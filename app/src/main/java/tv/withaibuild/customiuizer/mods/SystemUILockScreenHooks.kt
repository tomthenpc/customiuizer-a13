package tv.withaibuild.customiuizer.mods

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import tv.withaibuild.customiuizer.MainModule
import tv.withaibuild.customiuizer.R
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.AfterHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.BeforeHookCallback
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook
import tv.withaibuild.customiuizer.mods.utils.LockScreenAlbumArtController
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.XposedHelpers
import tv.withaibuild.customiuizer.mods.utils.ResourceHooks
import java.lang.reflect.Method
import java.util.LinkedHashMap

@Suppress("UNUSED_PARAMETER")
object SystemUILockScreenHooks {

    private val cameraResetTag = ResourceHooks.getFakeResId("camera_reset_tag")
    private val securedTiles = ArrayList<String>()

    @JvmStatic
    fun LockScreenTopMarginHook(lpparam: PackageReadyParam) {
        val statusBarPaddingTop = IntArray(1)
        ModuleHelper.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.callMethod(param.getThisObject(), "getApplicationContext") as? Context ?: return
                val dimenResId = mContext.resources.getIdentifier("status_bar_padding_top", "dimen", lpparam.packageName)
                statusBarPaddingTop[0] = mContext.resources.getDimensionPixelSize(dimenResId)
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView", lpparam.classLoader, "updateViewStatusBarPaddingTop", View::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val view = param.getArg(0) as? View ?: return
                view.setPadding(view.paddingLeft, statusBarPaddingTop[0], view.paddingRight, view.paddingBottom)
                param.returnAndSkip(null)
            }
        })
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                XposedHelpers.callMethod(param.getThisObject(), "onDensityOrFontScaleChanged")
            }
        })
    }

    @JvmStatic
    fun LockScreenAlbumArtHook(lpparam: PackageReadyParam) {
        val MiuiThemeUtilsClass = XposedHelpers.findClassIfExists("com.android.keyguard.utils.MiuiKeyguardUtils", lpparam.classLoader)
        LockScreenAlbumArtController.setMiuiThemeUtilsClass(MiuiThemeUtilsClass)

        ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val panel = param.getThisObject() ?: return
                val isDefaultLockScreenTheme = XposedHelpers.callStaticMethod(MiuiThemeUtilsClass, "isDefaultLockScreenTheme") as? Boolean ?: return
                if (isDefaultLockScreenTheme) {
                    val mBlurRatioChangedListener = XposedHelpers.getObjectField(panel, "mBlurRatioChangedListener")
                    val notificationShadeDepthController = XposedHelpers.getObjectField(panel, "notificationShadeDepthController")
                    XposedHelpers.callMethod(notificationShadeDepthController, "removeListener", mBlurRatioChangedListener)
                    val view = XposedHelpers.getObjectField(panel, "mThemeBackgroundView") as? View ?: return
                    view.alpha = 1.0f

                    val mContext = view.context
                    val intentFilter = IntentFilter(GlobalActions.EVENT_PREFIX + "UPDATE_LS_ALBUM_ART")
                    val albumArtReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            ModuleHelper.guarded("SystemUILockScreenHooks.albumArtReceiver") {
                                if (intent.action == GlobalActions.EVENT_PREFIX + "UPDATE_LS_ALBUM_ART") {
                                    try {
                                        XposedHelpers.callMethod(panel, "updateThemeBackground")
                                    } catch (e: Throwable) {
                                        XposedHelpers.callMethod(panel, "updateThemeBackgroundVisibility")
                                    }
                                }
                            }
                        }
                    }
                    ModuleHelper.registerModuleReceiver(
                        mContext,
                        "systemui.albumArtReceiver",
                        albumArtReceiver,
                        intentFilter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                }
            }
        })

        val updateLockscreenHook = object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val isDefaultLockScreenTheme = XposedHelpers.callStaticMethod(MiuiThemeUtilsClass, "isDefaultLockScreenTheme") as? Boolean ?: return
                if (!isDefaultLockScreenTheme) return
                val view = XposedHelpers.getObjectField(param.getThisObject(), "mThemeBackgroundView") as? View ?: return
                val isOnShade = XposedHelpers.callMethod(param.getThisObject(), "isOnShade") as? Boolean ?: return
                if (isOnShade) {
                    view.visibility = View.GONE
                } else {
                    view.visibility =
                        if (LockScreenAlbumArtController.applyTo(view)) View.VISIBLE else View.GONE
                }
                param.returnAndSkip(null)
            }
        }
        ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader, "updateThemeBackground", updateLockscreenHook)
        ModuleHelper.findAndHookMethodSilently("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader, "updateThemeBackgroundVisibility", updateLockscreenHook)

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.NotificationMediaManager", lpparam.classLoader, "updateMediaMetaData", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val isDefaultLockScreenTheme = XposedHelpers.callStaticMethod(MiuiThemeUtilsClass, "isDefaultLockScreenTheme") as? Boolean ?: return
                if (!isDefaultLockScreenTheme) {
                    LockScreenAlbumArtController.clear(null, false)
                    return
                }
                val mMediaMetadata = XposedHelpers.getObjectField(param.getThisObject(), "mMediaMetadata") as? MediaMetadata
                var art: Bitmap? = null
                if (mMediaMetadata != null) {
                    art = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    if (art == null) art = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    if (art == null) art = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                }
                val blur = MainModule.mPrefs.getInt("system_albumartonlock_blur", 0)
                val rescale = MainModule.mPrefs.getStringAsInt("system_albumartonlock_scale", 1)
                val grayscale = MainModule.mPrefs.getBoolean("system_albumartonlock_gray")
                LockScreenAlbumArtController.update(mContext, art, blur, rescale, grayscale)
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.NotificationMediaManager", lpparam.classLoader, "clearCurrentMediaNotification", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val isDefaultLockScreenTheme = XposedHelpers.callStaticMethod(MiuiThemeUtilsClass, "isDefaultLockScreenTheme") as? Boolean ?: return
                LockScreenAlbumArtController.clear(mContext, isDefaultLockScreenTheme)
            }
        })
    }

    private fun modifyCameraImage(mContext: Context, mKeyguardRightView: View, mDarkMode: Boolean): Boolean {
        if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_off")) {
            restoreCameraImage(mKeyguardRightView)
            return false
        }

        val key = "system_lockscreenshortcuts_right"
        val action = MainModule.mPrefs.getInt(key + "_action", 1)
        if (action <= 1) {
            restoreCameraImage(mKeyguardRightView)
            return false
        }

        val str = ModuleHelper.getActionName(mContext, key)
        if (str == null) {
            restoreCameraImage(mKeyguardRightView)
            return false
        }

        val icon: Drawable? = ModuleHelper.getActionImage(mContext, key)
        mKeyguardRightView.setBackgroundColor(Color.TRANSPARENT)
        mKeyguardRightView.foreground = icon
        mKeyguardRightView.foregroundGravity = Gravity.CENTER

        val density = mContext.resources.displayMetrics.density
        val size = Math.round(mContext.resources.configuration.smallestScreenWidthDp * density)

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.setShadowLayer(2 * density, 0f, 0f, if (mDarkMode) Color.argb(90, 255, 255, 255) else Color.argb(90, 0, 0, 0))
        paint.textSize = 20 * density
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = if (mDarkMode) Color.WHITE else Color.BLACK
        paint.alpha = 90

        val bounds = Rect()
        paint.getTextBounds(str, 0, str.length, bounds)
        val x = size / 2f - bounds.width() / 2f
        val iconHeight = if (icon == null) 0f else icon.intrinsicHeight / 2f + 30 * density
        val y = size / 2f + bounds.height() / 2f + iconHeight
        canvas.drawText(str, x, y, paint)
        paint.style = Paint.Style.FILL
        paint.clearShadowLayer()
        paint.color = if (mDarkMode) Color.BLACK else Color.WHITE
        paint.alpha = if (mDarkMode) 160 else 230
        canvas.drawText(str, x, y, paint)

        val bmpDrawable = BitmapDrawable(mContext.resources, bmp)
        if (mKeyguardRightView is ImageView) {
            mKeyguardRightView.scaleType = ImageView.ScaleType.CENTER
            mKeyguardRightView.setImageDrawable(bmpDrawable)
        } else {
            bmpDrawable.gravity = Gravity.CENTER
            mKeyguardRightView.background = bmpDrawable
        }

        return true
    }

    private fun restoreCameraImage(mKeyguardRightView: View) {
        mKeyguardRightView.setBackgroundColor(Color.BLACK)
        mKeyguardRightView.foreground = null
        if (mKeyguardRightView is ImageView)
            mKeyguardRightView.scaleType = ImageView.ScaleType.FIT_END
    }

    @JvmStatic
    fun LockScreenShortcutHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultLeftButton", lpparam.classLoader, "getIcon", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val img = param.getResult() ?: return
                if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_left_tapaction")) {
                    val thisObject = XposedHelpers.getSurroundingThis(param.getThisObject())
                    val mContext = XposedHelpers.getObjectField(thisObject, "mContext") as? Context ?: return
                    val mDarkMode = XposedHelpers.getBooleanField(thisObject, "mDarkStyle")
                    val flashlightDrawable = ModuleHelper.getModuleRes(mContext).getDrawable(
                        if (mDarkMode) R.drawable.keyguard_bottom_flashlight_img_dark else R.drawable.keyguard_bottom_flashlight_img_light,
                        mContext.theme
                    )
                    XposedHelpers.setObjectField(img, "drawable", flashlightDrawable)
                } else if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_left_off")) {
                    XposedHelpers.setObjectField(img, "isVisible", false)
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView\$MiuiDefaultRightButton", lpparam.classLoader, "getIcon", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val img = param.getResult() ?: return
                if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_off")) {
                    XposedHelpers.setObjectField(img, "isVisible", false)
                    return
                }

                val opt = MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_image")
                if (!opt) return
                val thisObject = XposedHelpers.getSurroundingThis(param.getThisObject())
                val mContext = XposedHelpers.getObjectField(thisObject, "mContext") as? Context ?: return
                val mDarkMode = XposedHelpers.getBooleanField(thisObject, "mDarkStyle")
                XposedHelpers.setObjectField(img, "drawable", ModuleHelper.getModuleRes(mContext).getDrawable(
                    if (mDarkMode) R.drawable.keyguard_bottom_miuizer_img_dark else R.drawable.keyguard_bottom_miuizer_img_light,
                    mContext.theme
                ))
            }
        })

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader, "initTipsView", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val opt = MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_image")
                if (!opt) return
                val isLeft = param.getArg(0) as? Boolean ?: return
                if (!isLeft) {
                    val mRightAffordanceViewTips = XposedHelpers.getObjectField(param.getThisObject(), "mRightAffordanceViewTips") as? TextView
                    mRightAffordanceViewTips?.text = ModuleHelper.getModuleRes(mRightAffordanceViewTips.context).getString(R.string.system_lockscreenshortcuts_right_image_hint)
                }
            }
        })

        if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_left_tapaction")) {
            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader, "onFinishInflate", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mLeftAffordanceView = XposedHelpers.getObjectField(param.getThisObject(), "mLeftAffordanceView") as? View ?: return
                    mLeftAffordanceView.setOnLongClickListener {
                        ModuleHelper.guarded(false) {
                            val flashlightController = XposedHelpers.getObjectField(param.getThisObject(), "mFlashlightController")
                            val z = !(XposedHelpers.callMethod(flashlightController, "isEnabled") as? Boolean ?: false)
                            XposedHelpers.callMethod(flashlightController, "setFlashlight", z)
                            XposedHelpers.callMethod(param.getThisObject(), "updateLeftAffordanceIcon")
                            true
                        }
                    }
                }
            })

            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader, "updateLeftAffordanceIcon", object : MethodHook() {
                override fun after(param: AfterHookCallback) {
                    val mLeftAffordanceView = XposedHelpers.getObjectField(param.getThisObject(), "mLeftAffordanceView")
                    val flashlightController = XposedHelpers.getObjectField(param.getThisObject(), "mFlashlightController")
                    val isOn = XposedHelpers.callMethod(flashlightController, "isEnabled") as? Boolean ?: false
                    XposedHelpers.callMethod(mLeftAffordanceView, "setCircleRadiusWithoutAnimation", if (isOn) 66f else 0f)
                }
            })

            ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader, "onClick", View::class.java, object : MethodHook() {
                override fun before(param: BeforeHookCallback) {
                    val view = param.getArg(0) as? View ?: return
                    val mLeftAffordanceView = XposedHelpers.getObjectField(param.getThisObject(), "mLeftAffordanceView") as? View ?: return
                    if (view == mLeftAffordanceView) {
                        param.returnAndSkip(null)
                    }
                }
            })
        }

        ModuleHelper.hookAllMethods("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader, "launchCamera", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                if (GlobalActions.handleAction(mContext, "system_lockscreenshortcuts_right", true)) {
                    param.returnAndSkip(null)
                    val PanelInjector = XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader),
                        "get",
                        XposedHelpers.findClass("com.android.keyguard.injector.KeyguardPanelViewInjector", lpparam.classLoader)
                    )
                    val panelController = XposedHelpers.getObjectField(PanelInjector, "mPanelViewController")
                    val mNotificationPanelView = XposedHelpers.getObjectField(PanelInjector, "mPanelView") as? View ?: return
                    val oldResetRunnable = mNotificationPanelView.getTag(cameraResetTag) as? Runnable
                    if (oldResetRunnable != null) mNotificationPanelView.removeCallbacks(oldResetRunnable)
                    val resetRunnable = Runnable {
                        ModuleHelper.guarded {
                            XposedHelpers.callMethod(panelController, "resetViews", false)
                            mNotificationPanelView.setTag(cameraResetTag, null)
                        }
                    }
                    mNotificationPanelView.postDelayed(resetRunnable, 500)
                    mNotificationPanelView.setTag(cameraResetTag, resetRunnable)
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.keyguard.MiuiKeyguardCameraView", lpparam.classLoader, "setDarkStyle", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_image")) {
                    val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                    val mDarkMode = XposedHelpers.getBooleanField(param.getThisObject(), "mDarkStyle")
                    XposedHelpers.callMethod(param.getThisObject(), "setPreviewImageDrawable", ModuleHelper.getModuleRes(mContext).getDrawable(
                        if (mDarkMode) R.drawable.keyguard_bottom_miuizer_img_dark else R.drawable.keyguard_bottom_miuizer_img_light,
                        mContext.theme
                    ))
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.MiuiKeyguardCameraView", lpparam.classLoader, "updatePreView", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mPreViewContainer = XposedHelpers.getObjectField(param.getThisObject(), "mPreViewContainer") as? View ?: return
                if ("active" == mPreViewContainer.tag) {
                    XposedHelpers.setFloatField(param.getThisObject(), "mIconCircleAlpha", 0.0f)
                    (param.getThisObject() as? View)?.invalidate()
                }
            }
        })

        ModuleHelper.hookAllMethods("com.android.keyguard.MiuiKeyguardCameraView", lpparam.classLoader, "setPreviewImageDrawable", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mDarkMode = XposedHelpers.getBooleanField(param.getThisObject(), "mDarkStyle")
                val mIconView = XposedHelpers.getObjectField(param.getThisObject(), "mIconView") as? ImageView
                if (mIconView != null) {
                    if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_image")) {
                        mIconView.setImageDrawable(ModuleHelper.getModuleRes(mContext).getDrawable(
                            if (mDarkMode) R.drawable.keyguard_bottom_miuizer_img_dark else R.drawable.keyguard_bottom_miuizer_img_light,
                            mContext.theme
                        ))
                    } else {
                        val resId = if (mDarkMode) {
                            mContext.resources.getIdentifier("keyguard_bottom_camera_img_dark", "drawable", lpparam.packageName)
                        } else {
                            mContext.resources.getIdentifier("keyguard_bottom_camera_img", "drawable", lpparam.packageName)
                        }
                        mIconView.setImageDrawable(mContext.getDrawable(resId))
                    }
                }

                val mPreView = XposedHelpers.getObjectField(param.getThisObject(), "mPreView") as? View
                val mPreViewContainer = XposedHelpers.getObjectField(param.getThisObject(), "mPreViewContainer") as? View
                val mBackgroundView = XposedHelpers.getObjectField(param.getThisObject(), "mBackgroundView") as? View
                val mIconCircleStrokePaint = XposedHelpers.getObjectField(param.getThisObject(), "mIconCircleStrokePaint") as? Paint
                val mPreViewOutlineProvider = XposedHelpers.getObjectField(param.getThisObject(), "mPreViewOutlineProvider") as? ViewOutlineProvider
                val result = modifyCameraImage(mContext, mPreView ?: return, mDarkMode)
                if (result) param.returnAndSkip(null)
                if (mPreViewContainer != null) {
                    mPreViewContainer.setBackgroundColor(if (result) Color.TRANSPARENT else Color.BLACK)
                    mPreViewContainer.outlineProvider = if (result) null else mPreViewOutlineProvider
                    mPreViewContainer.tag = if (result) "active" else "inactive"
                }
                if (mBackgroundView != null)
                    mBackgroundView.setBackgroundColor(if (result) Color.TRANSPARENT else Color.BLACK)
                if (mIconCircleStrokePaint != null)
                    mIconCircleStrokePaint.color = if (result) Color.TRANSPARENT else Color.WHITE
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.MiuiKeyguardCameraView", lpparam.classLoader, "handleMoveDistanceChanged", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mIconView = XposedHelpers.getObjectField(param.getThisObject(), "mIconView") as? View
                if (MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_off")) {
                    mIconView?.visibility = View.GONE
                    param.returnAndSkip(null)
                } else {
                    mIconView?.visibility = View.VISIBLE
                }
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.MiuiKeyguardCameraView", lpparam.classLoader, "startFullScreenAnim", object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val action = MainModule.mPrefs.getInt("system_lockscreenshortcuts_right_action", 1)
                if (action <= 1) return
                val mAnimatorSet = XposedHelpers.getObjectField(param.getThisObject(), "mAnimatorSet") as? AnimatorSet ?: return
                param.setResult(null)
                mAnimatorSet.pause()
                mAnimatorSet.removeAllListeners()
                mAnimatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                        GlobalActions.handleAction(mContext, "system_lockscreenshortcuts_right", true)
                        val mCallBack = XposedHelpers.getObjectField(param.getThisObject(), "mCallBack")
                        if (mCallBack != null)
                            XposedHelpers.callMethod(mCallBack, "onCompletedAnimationEnd")
                        XposedHelpers.setBooleanField(param.getThisObject(), "mIsPendingStartCamera", false)
                        XposedHelpers.callMethod(param.getThisObject(), "dismiss")
                        val mBackgroundView = XposedHelpers.getObjectField(param.getThisObject(), "mBackgroundView") as? View
                        mBackgroundView?.alpha = 1.0f
                    }
                })
                mAnimatorSet.resume()
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.KeyguardMoveHelper", lpparam.classLoader, "setTranslation", Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mCurrentScreen = XposedHelpers.getIntField(param.getThisObject(), "mCurrentScreen")
                if (mCurrentScreen != 1) return
                val arg = param.getArg(0) as? Float ?: return
                if (arg < 0 && MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_off"))
                    param.getArgs()[0] = 0.0f
                else if (arg > 0 && MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_left_off"))
                    param.getArgs()[0] = 0.0f
            }
        })

        ModuleHelper.findAndHookMethod("com.android.keyguard.KeyguardMoveHelper", lpparam.classLoader, "fling", Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {
                val mCurrentScreen = XposedHelpers.getIntField(param.getThisObject(), "mCurrentScreen")
                if (mCurrentScreen != 1) return
                val arg = param.getArg(0) as? Float ?: return
                if (arg < 0 && MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_right_off"))
                    param.returnAndSkip(null)
                else if (arg > 0 && MainModule.mPrefs.getBoolean("system_lockscreenshortcuts_left_off"))
                    param.returnAndSkip(null)
            }
        })
    }

    @JvmStatic
    fun LockScreenSecureLaunchHook() {
        ModuleHelper.findAndHookMethod(Activity::class.java, "onCreate", Bundle::class.java, object : MethodHook() {
            @Suppress("ConstantConditionIf")
            override fun after(param: AfterHookCallback) {
                val act = param.getThisObject() as? Activity ?: return
                val intent = act.intent ?: return
                val mFromSecureKeyguard = intent.getBooleanExtra("StartActivityWhenLocked", false)
                var mStartedFromLockScreen = false
                try {
                    mStartedFromLockScreen = XposedHelpers.getAdditionalInstanceField(act.application, "wasStartedFromLockScreen") as? Boolean ?: false
                } catch (_: Throwable) {}
                if (mFromSecureKeyguard || mStartedFromLockScreen) {
                    XposedHelpers.setAdditionalInstanceField(act.application, "wasStartedFromLockScreen", true)
                    act.setShowWhenLocked(true)
                    act.setInheritShowWhenLocked(true)
                }
            }
        })
    }

    @JvmStatic
    fun SecureQSTilesHook(lpparam: PackageReadyParam) {
        val tileHostCls = XposedHelpers.findClassIfExists("com.android.systemui.qs.QSTileHost", lpparam.classLoader)

        val hook = object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val mContext = XposedHelpers.getObjectField(param.getThisObject(), "mContext") as? Context ?: return
                val mAfterUnlockReceiver = object : BroadcastReceiver() {
                    @Suppress("UNCHECKED_CAST")
                    override fun onReceive(context: Context, intent: Intent) {
                        try {
                            val tileName = intent.getStringExtra("tileName") ?: return
                            val expandAfter = intent.getBooleanExtra("expandAfter", false)
                            val usingCenter = intent.getBooleanExtra("usingCenter", false)
                            if ("edit" == tileName || expandAfter) {
                                val expandIntent = Intent(GlobalActions.ACTION_PREFIX + "ExpandSettings")
                                expandIntent.putExtra("forceExpand", true)
                                context.sendBroadcast(expandIntent)
                            }
                            val mTiles = XposedHelpers.getObjectField(param.getThisObject(), "mTiles") as? LinkedHashMap<String, Any> ?: return
                            var tile = mTiles[tileName]
                            if (tile == null) {
                                if (usingCenter) {
                                    val mController = XposedHelpers.callStaticMethod(
                                        XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader),
                                        "get",
                                        XposedHelpers.findClassIfExists("com.android.systemui.miui.statusbar.policy.ControlPanelController", lpparam.classLoader)
                                    )
                                    val mControlCenter = XposedHelpers.getObjectField(mController, "mControlCenter")
                                    val mControlPanelContentView = XposedHelpers.getObjectField(mControlCenter, "mControlPanelContentView")
                                    val mControlCenterPanel = XposedHelpers.callMethod(mControlPanelContentView, "getControlCenterPanel")
                                    var mBigTile: Any? = null
                                    when (tileName) {
                                        "bt" -> mBigTile = XposedHelpers.getObjectField(mControlCenterPanel, "mBigTile1")
                                        "cell" -> mBigTile = XposedHelpers.getObjectField(mControlCenterPanel, "mBigTile2")
                                        "wifi" -> mBigTile = XposedHelpers.getObjectField(mControlCenterPanel, "mBigTile3")
                                    }
                                    if (mBigTile != null) tile = XposedHelpers.getObjectField(mBigTile, "mQSTile")
                                    if (tile == null) return
                                } else {
                                    return
                                }
                            }
                            XposedHelpers.setAdditionalInstanceField(tile, "mCalledAfterUnlock", true)
                            val clickHandler = XposedHelpers.findMethodExact(tile.javaClass, "handleClick", View::class.java)
                            clickHandler.invoke(tile, arrayOfNulls<Any>(1))
                        } catch (t: Throwable) {
                            XposedHelpers.log(t)
                        }
                    }
                }
                ModuleHelper.registerModuleReceiver(
                    mContext,
                    "systemui.afterUnlockReceiver",
                    mAfterUnlockReceiver,
                    IntentFilter(GlobalActions.ACTION_PREFIX + "HandleQSTileClick"),
                    Context.RECEIVER_EXPORTED
                )
            }
        }

        ModuleHelper.hookAllConstructors(tileHostCls, hook)

        val FactoryImpl = "com.android.systemui.qs.tileimpl.MiuiQSFactory"
        ModuleHelper.findAndHookMethod(FactoryImpl, lpparam.classLoader, "createTileInternal", String::class.java, object : MethodHook() {
            override fun after(param: AfterHookCallback) {
                val tile = param.getResult() ?: return
                val tileClass = tile.javaClass.canonicalName ?: return
                val tileName = param.getArg(0) as? String ?: return
                var name = tileName
                if (name.startsWith("intent(")) name = "intent"
                else if (name.startsWith("custom(")) name = "custom"
                val secureTitles = HashSet<String>()
                if (MainModule.mPrefs.getBoolean("system_secureqs_wifi")) secureTitles.add("wifi")
                if (MainModule.mPrefs.getBoolean("system_secureqs_bt")) secureTitles.add("bt")
                if (MainModule.mPrefs.getBoolean("system_secureqs_mobiledata")) secureTitles.add("cell")
                if (MainModule.mPrefs.getBoolean("system_secureqs_airplane")) secureTitles.add("airplane")
                if (MainModule.mPrefs.getBoolean("system_secureqs_location")) secureTitles.add("gps")
                if (MainModule.mPrefs.getBoolean("system_secureqs_hotspot")) secureTitles.add("hotspot")
                if (MainModule.mPrefs.getBoolean("system_secureqs_nfc")) secureTitles.add("nfc")
                if (MainModule.mPrefs.getBoolean("system_secureqs_sync")) secureTitles.add("sync")
                if (MainModule.mPrefs.getBoolean("system_secureqs_edit")) secureTitles.add("edit")
                if (MainModule.mPrefs.getBoolean("system_secureqs_custom")) {
                    secureTitles.add("intent")
                    secureTitles.add("custom")
                }
                if (secureTitles.contains(name) && !securedTiles.contains(tileClass)) {
                    val tileHook = object : MethodHook() {
                        override fun before(param2: BeforeHookCallback) {
                            val mCalledAfterUnlock = XposedHelpers.getAdditionalInstanceField(param2.getThisObject(), "mCalledAfterUnlock") as? Boolean
                            if (mCalledAfterUnlock == true) {
                                XposedHelpers.setAdditionalInstanceField(param2.getThisObject(), "mCalledAfterUnlock", false)
                                return
                            }
                            var isScreenLockDisabled = XposedHelpers.getAdditionalStaticField(
                                XposedHelpers.findClass("com.android.systemui.keyguard.KeyguardViewMediator", lpparam.classLoader),
                                "isScreenLockDisabled"
                            ) as? Boolean ?: false
                            if (isScreenLockDisabled) return
                            val mContext = XposedHelpers.getObjectField(param2.getThisObject(), "mContext") as? Context ?: return
                            val kgMgr = mContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
                            if (!kgMgr.isKeyguardLocked || !kgMgr.isKeyguardSecure) return
                            Handler(mContext.mainLooper).post {
                                try {
                                    val DependencyClass = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
                                    val StatusbarClsForDep = "com.android.systemui.statusbar.phone.CentralSurfaces"
                                    val mStatusBar = XposedHelpers.callStaticMethod(DependencyClass, "get", XposedHelpers.findClassIfExists(StatusbarClsForDep, lpparam.classLoader))
                                    val usingControlCenter: Boolean
                                    val mController = XposedHelpers.callStaticMethod(DependencyClass, "get", XposedHelpers.findClassIfExists("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl", lpparam.classLoader))
                                    usingControlCenter = XposedHelpers.callMethod(mController, "isUseControlCenter") as? Boolean ?: false
                                    if (usingControlCenter) XposedHelpers.callMethod(mController, "collapseControlCenter", true)
                                    val keepOpened = MainModule.mPrefs.getBoolean("system_secureqs_keepopened")
                                    val expandAfter = usingControlCenter && keepOpened
                                    XposedHelpers.callMethod(mStatusBar, "postQSRunnableDismissingKeyguard", !keepOpened, Runnable {
                                        ModuleHelper.guarded {
                                            val intent = Intent(GlobalActions.ACTION_PREFIX + "HandleQSTileClick")
                                            intent.putExtra("tileName", tileName)
                                            intent.putExtra("expandAfter", expandAfter)
                                            intent.putExtra("usingCenter", usingControlCenter)
                                            mContext.sendBroadcast(intent)
                                        }
                                    })
                                } catch (t: Throwable) {
                                    XposedHelpers.log(t)
                                }
                            }
                            param2.returnAndSkip(null)
                        }
                    }
                    ModuleHelper.findAndHookMethod(tileClass, lpparam.classLoader, "handleClick", View::class.java, tileHook)
                    ModuleHelper.hookAllMethodsSilently(tileClass, lpparam.classLoader, "handleSecondaryClick", tileHook)
                    securedTiles.add(tileClass)
                }
            }
        })
    }

    @JvmStatic
    fun HideLockscreenZenModeHook(lpparam: PackageReadyParam) {
        ModuleHelper.findAndHookMethod("com.android.systemui.statusbar.notification.zen.ZenModeViewController", lpparam.classLoader, "shouldBeVisible", HookerClassHelper.returnConstant(false))
    }
}
