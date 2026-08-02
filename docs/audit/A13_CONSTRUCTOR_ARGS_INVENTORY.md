# Constructor/Hook Args Index Inventory

## java\tv\withaibuild\customiuizer\mods\SystemAudioAndVisualAndMoreHooks.kt

92: `if ("AudioFocus_For_Phone_Ring_And_Calls" == param.args[4] && audioFocusPkg != null &&`

99: `if (res != AudioManager.AUDIOFOCUS_REQUEST_FAILED && "AudioFocus_For_Phone_Ring_And_Calls" != param.args[4])`

100: `audioFocusPkg = param.args[5] as? String`

121: `val streamType = param.args[0] as? Int ?: return`

123: `val isMuteAdjust = param.args[2] as? Boolean ?: return`

134: `ModuleHelper.hookAllConstructors("com.android.server.wm.DisplayRotation", lpparam.classLoader, object : MethodHook() {`

160: `param.args[0] = false`

166: `ModuleHelper.hookAllConstructors(sblCls, object : MethodHook() {`

185: `ModuleHelper.hookAllConstructors(rrvCls, object : MethodHook() {`

214: `param.args[0] = true`

226: `param.args[0] = true`

247: `if (param.args[0] == 1) {`

322: `param.args[1] = Math.round(((param.args[1] as? Int ?: -1).let { if (it == -1) XposedHelpers.getIntField(param.thisObject, "mDefaultVibrationAmplitude") else it } * ratio))`

324: `param.args[0] = Math.max(3, Math.round((param.args[0] as? Long ?: 0L) * ratio))`

422: `if (param.args.size != 4) return`

423: `val contentValues = param.args[1] as? ContentValues ?: return`

436: `val imgUri = param.args[0] as? Uri ?: return`

437: `val contentValues = param.args[1] as? ContentValues ?: return`

469: `val mContext = param.args[0] as? Context ?: return`

473: `if (param.args.size < 7) return`

475: `param.args[4] = compress`

488: `val quality = param.args[1] as? Int ?: return`

489: `if (quality != 100 || param.args[2] is ByteArrayOutputStream) return`

494: `param.args[0] = compress`

495: `param.args[1] = newQuality`

524: `val lp = if (param.args.size == 1) param.args[0] else param.args[1]`

529: `val lp = if (param.args.size == 1) param.args[0] else param.args[1]`

581: `val event = param.args[0] as? MotionEvent ?: return`

630: `ModuleHelper.hookAllConstructors("com.android.server.wm.WindowSurfaceController", lpparam.classLoader, object : MethodHook() {`

632: `if (param.args.size <= 4) return`

633: `val windowType = param.args.getOrNull(4) as? Int ?: return`

638: `var flags = param.args.getOrNull(flagIndex) as? Int ?: return`

641: `param.args[flagIndex] = flags`

683: `ModuleHelper.hookAllConstructors("com.miui.applicationlock.widget.MiuiNumericInputView", lpparam.classLoader, object : MethodHook() {`

788: `val isKeyguardShowingNew = param.args[0] as? Boolean ?: false`


## java\tv\withaibuild\customiuizer\mods\SystemAudioAndVolumeHooks.kt

67: `val mStreamType = param.args[0] as? Int ?: return`


## java\tv\withaibuild\customiuizer\mods\SystemChargingAndWallpaperHooks.kt

31: `val charge = param.args[2] as? Int ?: return`

78: `ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.KeyguardIndicationTextView", lpparam.classLoader, object : MethodHook() {`

92: `if (param.throwable != null || param.result == null || param.args[5] == 1 || "com.android.thememanager" == param.args[1]) return`

99: `handleIncomingUser = XposedHelpers.callStaticMethod(ActivityManager::class.java, "handleIncomingUser", Binder.getCallingPid(), Binder.getCallingUid(), param.args[7], false, true, "changing wallpaper", null) as? Int ?: 0`

102: `val wallpaperData = XposedHelpers.callMethod(param.thisObject, "getWallpaperSafeLocked", handleIncomingUser, param.args[5])`


## java\tv\withaibuild\customiuizer\mods\SystemDisplayAndWindowHooks.kt

58: `XposedHelpers.callMethod(param.thisObject, "declineRequest", param.args[0])`

73: `val reason = param.args[3] as? String ?: return`

137: `ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader, object : MethodHook() {`

139: `val mView = param.args[0] as? android.view.View ?: return`

182: `val ratio = XposedHelpers.callMethod(blurUtils, "ratioOfBlurRadius", 1.0f * (param.args[1] as Int)) as? Float ?: return`

184: `param.args[1] = Math.round((XposedHelpers.callMethod(blurUtils, "blurRadiusOfRatio", newRatio) as? Float ?: 0f))`

191: `param.args[0] = (param.args[0] as Float) * modifier / 100f`

322: `ModuleHelper.hookAllConstructors(abc, lpparam.classLoader, constructorHook)`

342: `ModuleHelper.hookAllConstructors(dpc, lpparam.classLoader, constructorHook)`


## java\tv\withaibuild\customiuizer\mods\SystemFreeformAndMultiWindowHooks.kt

170: `val request = param.args[0]`

199: `val intent = param.args[5] as? Intent ?: return`

203: `XposedHelpers.getObjectField(param.args[0], "mContext") as? Context`

205: `val mService = XposedHelpers.getObjectField(param.args[0], "mService")`

228: `val safeOptions = param.args[3]`

247: `val safeOptions = param.args[3]`

253: `val pkgName = getTaskPackageName(param.thisObject, param.args[2] as? Int ?: 0, options)`

262: `param.args[3] = safeOptions`

272: `val intent = param.args[1] as? Intent ?: return`

283: `if (param.args.size != 3) return`

284: `val pkgName = XposedHelpers.callMethod(param.args[1], "getStackPackageName") as? String ?: return`

305: `val taskId = XposedHelpers.getObjectField(param.args[0], "taskId")`

348: `val pkgName = getTaskPackageName(param.thisObject, param.args[0] as? Int ?: 0) ?: return`

359: `val miuiFreeFormActivityStack = XposedHelpers.callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", param.args[0])`

363: `fwApps[pkgName] = Pair(sScale, Rect(param.args[1] as? Rect ?: return))`

373: `ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.notification.NotificationMessagingTemplateViewWrapper", lpparam.classLoader, object : MethodHook() {`

437: `param.args[0] = blackList`

465: `val pkgName = param.args[0] as? String ?: return`

485: `val key = param.args[0] as? String ?: return`

487: `param.args[1] = "0"`


## java\tv\withaibuild\customiuizer\mods\SystemLockScreenMoreHooks.kt

35: `if ("*" != param.args[1]) return`

36: `val mode = XposedHelpers.callMethod(param.thisObject, "getAccessControlLockMode", param.args[0]) as? Int ?: 0`

77: `saveLastCheck(param.thisObject, param.args[0] as? String, param.args[1] as? Int ?: 0)`

81: `checkLastCheck(param.thisObject, param.args[1] as? Int ?: 0)`

88: `saveLastCheck(param.thisObject, param.args[0] as? String, param.args[2] as? Int ?: 0)`

92: `checkLastCheck(param.thisObject, param.args[2] as? Int ?: 0)`

107: `val intent = param.args[0] as? Intent ?: return`

235: `ModuleHelper.hookAllConstructors("com.android.systemui.keyguard.KeyguardSecurityContainerController", lpparam.classLoader, object : MethodHook() {`

373: `if (param.args.size == 0) return`

401: `ModuleHelper.hookAllConstructors("com.android.systemui.statusbar.policy.BluetoothControllerImpl", lpparam.classLoader, object : MethodHook() {`

404: `val mContext = param.args[0] as? Context ?: return`


## java\tv\withaibuild\customiuizer\mods\SystemNotificationMoreHooks.kt

254: `param.args[0] = true`

264: `param.args[0] = true`

295: `val pref = param.args[0]`

299: `param.args[1] = true`

497: `ModuleHelper.hookAllConstructors("com.android.server.wm.WallpaperController", lpparam.classLoader, object : MethodHook() {`


## java\tv\withaibuild\customiuizer\mods\SystemSecurityAndSystemHooks.kt

28: `ModuleHelper.hookAllConstructors("com.android.server.policy.BaseMiuiPhoneWindowManager", lpparam.classLoader, object : MethodHook() {`

57: `if (param.thisObject == signUnknown || param.args[0] == signUnknown) {`

61: `val flags = param.args[1] as? Int ?: return`

66: `ModuleHelper.hookAllConstructors("android.util.jar.StrictJarVerifier", lpparam.classLoader, object : MethodHook() {`

76: `val packageName = XposedHelpers.callMethod(param.args[1], "getPackageName") as? String ?: return`

77: `val sourcePackageName = param.args[0] as? String ?: return`

86: `val isSystem = XposedHelpers.callMethod(param.args[0], "isSystem") as? Boolean ?: return`

99: `param.args[0] = false`

102: `ModuleHelper.hookAllConstructors("com.android.server.wm.WindowSurfaceController", lpparam.classLoader, object : MethodHook() {`

104: `var flags = param.args[2] as? Int ?: return`

107: `param.args[2] = flags`


## java\tv\withaibuild\customiuizer\mods\SystemSettingsAndConnectivityHooks.kt

40: `param.args[0] = dlgTitleId`

49: `val str = (param.args[0] as? CharSequence ?: return).toString() + "\n" + title + ": " + wifiSharedKey[0]`

50: `param.args[0] = str`

66: `val wifiEntry = param.args[0]`

84: `val wifiEntry = param.args[0]`

122: `val mCurrentCarrier = param.args[0] as? String ?: return`

123: `param.args[0] = mCurrentCarrier.replace(" | ", "")`


## java\tv\withaibuild\customiuizer\mods\SystemSettingsMoreHooks.kt

71: `val msg = param.args[0] as? Message ?: return`

79: `param.args[0] = msg`


## java\tv\withaibuild\customiuizer\mods\SystemShareAndOpenWithHooks.kt

53: `if (param.args[0] == null) return`

54: `if (param.args.size < 6) return`

55: `val origIntent = param.args[0] as? Intent ?: return`

174: `if (param.args[0] == null) return`

175: `if (param.args.size < 6) return`

176: `val origIntent = param.args[0] as? Intent ?: return`


## java\tv\withaibuild\customiuizer\mods\SystemStatusBarAndClockHooks.kt

109: `if (actionBarColor != NOCOLOR) param.args[0] = actionBarColor`

110: `else if (Color.alpha(param.args[0] as? Int ?: 0) < 255) param.args[0] = Color.TRANSPARENT`

116: `hookToolbar(param.thisObject, param.args[0] as? Drawable ?: return)`

122: `hookWindowDecor(param.thisObject, param.args[0] as? Drawable ?: return)`

137: `hookToolbar(param.thisObject, param.args[0] as? Drawable ?: return)`

148: `hookWindowDecor(param.thisObject, param.args[0] as? Drawable ?: return)`

159: `hookToolbar(param.thisObject, param.args[0] as? Drawable ?: return)`

169: `hookWindowDecor(param.thisObject, param.args[0] as? Drawable ?: return)`

192: `val pkgName = XposedHelpers.getObjectField(param.args[0], "pkg") as? String ?: return`


## java\tv\withaibuild\customiuizer\mods\Various.kt

99: `ModuleHelper.findAndHookConstructor("androidx.fragment.app.Fragment", lpparam.classLoader, object : MethodHook() {`

154: `val key = XposedHelpers.callMethod(param.args[0], "getKey") as? String ?: return`

155: `val title = XposedHelpers.callMethod(param.args[0], "getTitle") as? CharSequence`

234: `param.args[0] = checkBundle(param.thisObject as? Context, param.args[0] as? Bundle)`

253: `param.args[0] = checkBundle(`

255: `param.args[0] as? Bundle`

309: `val menu = param.args[0] as? Menu ?: return`

337: `val item = param.args[0] as? MenuItem ?: return`

388: `if ((param.args[3] as? Int ?: -1) == 128 && (param.args[4] as? Int ?: -1) == 0) {`

431: `val addWhiteList = param.args[1] as? Boolean ?: false`

482: `ModuleHelper.hookAllConstructors(regionSamplingHelper, object : MethodHook() {`

487: `val view = param.args[0] as? View ?: return`

521: `val me = param.args[1] as? MotionEvent ?: return`

552: `val view = param.args[0] as? View ?: return`

589: `ModuleHelper.hookAllConstructors(handlerClass, object : MethodHook() {`

591: `if (param.args.size == 2) {`

592: `param.args[1] = 0`

600: `param.args[0] = 0`

629: `val key = param.args[1] as? String ?: return`

653: `val view = param.args[0] as? View ?: return`

677: `param.args[0] = 100`

682: `if ("callPreference" == param.args[1] as? String && "GET" == param.args[2] as? String) {`

683: `val extras = param.args[3] as? Bundle ?: return`

706: `val permissionRequest = param.args[0] ?: return`

714: `val permissionRequest = param.args[0] ?: return`

752: `val msg = param.args[0] as? Message ?: return`

820: `val key = param.args[1] as? String ?: return`

822: `param.args[1] = "next_alarm_clock_formatted"`

832: `if ((param.args[0] as? Int ?: -1) != 500) return`

869: `val showUi = param.args[3] as? Boolean ?: false`

875: `param.args[3] = false`

887: `if ((param.result as? Boolean != true) || param.args[0]?.toString() != "INCOMING") return`

1007: `val viewHolder = param.args[0] ?: return`

1106: `if ("persist.sys.allow_sys_app_update" == param.args[0] as? String) {`

1123: `val key = param.args[0] as? String ?: return`


