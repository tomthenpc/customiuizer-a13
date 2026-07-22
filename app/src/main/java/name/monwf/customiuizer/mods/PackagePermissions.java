package name.monwf.customiuizer.mods;

import static name.monwf.customiuizer.mods.utils.XposedHelpers.findClass;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import name.monwf.customiuizer.mods.utils.HookerClassHelper;
import name.monwf.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import name.monwf.customiuizer.mods.utils.ModuleHelper;
import name.monwf.customiuizer.mods.utils.XposedHelpers;
import name.monwf.customiuizer.utils.Helpers;


public class PackagePermissions {

	private static final Set<String> systemPackages = ConcurrentHashMap.newKeySet();

//	@SuppressWarnings("unchecked")
//	private static void dobefore(final BeforeHookCallback param) {
//		ArrayList<String> requestedPermissions = (ArrayList<String>)getObjectField(param.getArgs()[0], "requestedPermissions");
//		param.setObjectExtra("orig_requested_permissions", requestedPermissions);
//		//ArrayList<Boolean> requestedPermissionsRequired = (ArrayList<Boolean>)getObjectField(param.getArgs()[0], "requestedPermissionsRequired");
//		//param.setObjectExtra("orig_requested_permissions_required", requestedPermissionsRequired);
//
//		String pkgName = (String)getObjectField(param.getArgs()[0], "packageName");
//		if (pkgName.equalsIgnoreCase(Helpers.modulePkg)) {
//			requestedPermissions.add("miui.permission.READ_LOGS");
//			requestedPermissions.add("miui.permission.DUMP_CACHED_LOG");
//		}
//
//		setObjectField(param.getArgs()[0], "requestedPermissions", requestedPermissions);
//		//setObjectField(param.getArgs()[0], "requestedPermissionsRequired", requestedPermissionsRequired);
//	}
//
//	@SuppressWarnings("unchecked")
//	private static void doafter(final AfterHookCallback param) {
//		ArrayList<String> origRequestedPermissions = (ArrayList<String>) param.getObjectExtra("orig_requested_permissions");
//		if (origRequestedPermissions != null) setObjectField(param.getArgs()[0], "requestedPermissions", origRequestedPermissions);
//		//ArrayList<Boolean> origRequestedPermissionsRequired = (ArrayList<Boolean>) param.getObjectExtra("orig_requested_permissions_required");
//		//if (origRequestedPermissionsRequired != null) setObjectField(param.getArgs()[0], "requestedPermissionsRequired", origRequestedPermissionsRequired);
//	}

	public static void hook(XposedModuleInterface.SystemServerStartingParam lpparam) {
		systemPackages.add(Helpers.modulePkg);
		//systemPackages.add("com.miui.packageinstaller");

		// Allow signature level permissions for module
		String PMSCls = "com.android.server.pm.permission.PermissionManagerServiceImpl";
		ModuleHelper.hookAllMethods(PMSCls, lpparam.getClassLoader(), "shouldGrantPermissionBySignature",
			new MethodHook() {
				@Override
				public Object intercept(XposedInterface.Chain chain) throws Throwable {
					String pkgName = (String)XposedHelpers.callMethod(chain.getArgs().get(0), "getPackageName");
					return systemPackages.contains(pkgName) ? true : chain.proceed();
				}
			}
		);

		ModuleHelper.hookAllMethodsSilently("com.android.server.pm.PackageManagerServiceUtils", lpparam.getClassLoader(), "verifySignatures",
			new MethodHook() {
				@Override
				public Object intercept(XposedInterface.Chain chain) throws Throwable {
					String pkgName = (String)XposedHelpers.callMethod(chain.getArgs().get(0), "getName");
					return systemPackages.contains(pkgName) ? true : chain.proceed();
				}
			}
		);

//		// Add custom permissions for module
//		if (!ModuleHelper.findAndHookMethodSilently("com.android.server.pm.permission.PermissionManagerService", lpparam.getClassLoader(), "grantRequestedRuntimePermissions",
//			"android.content.pm.PackageParser$Package", int[].class, String[].class, int.class, "com.android.server.pm.permission.PermissionManagerServiceInternal.PermissionCallback",
//			new MethodHook() {
//				@Override
//				protected void before(final BeforeHookCallback param) throws Throwable {
//					doBefore(param);
//				}
//				@Override
//				protected void after(final AfterHookCallback param) throws Throwable {
//					doAfter(param);
//				}
//			}
//		)) if (!ModuleHelper.findAndHookMethodSilently("com.android.server.pm.permission.PermissionManagerService", lpparam.getClassLoader(), "grantPermissions",
//			"android.content.pm.PackageParser$Package", boolean.class, String.class, "com.android.server.pm.permission.PermissionManagerInternal.PermissionCallback",
//			new MethodHook() {
//				@Override
//				protected void before(final BeforeHookCallback param) throws Throwable {
//					doBefore(param);
//				}
//				@Override
//				protected void after(final AfterHookCallback param) throws Throwable {
//					doAfter(param);
//				}
//			}
//		)) ModuleHelper.findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.getClassLoader(), "grantPermissionsLPw",
//			"android.content.pm.PackageParser$Package", boolean.class, String.class,
//			new MethodHook() {
//				@Override
//				protected void before(final BeforeHookCallback param) throws Throwable {
//					doBefore(param);
//				}
//				@Override
//				protected void after(final AfterHookCallback param) throws Throwable {
//					doAfter(param);
//				}
//			}
//		);

		// Make module appear as system app
		String ActQueryService = "com.android.server.pm.ComputerEngine";
		ModuleHelper.hookAllMethods(ActQueryService, lpparam.getClassLoader(), "queryIntentActivitiesInternal", new MethodHook() {
			@Override
			@SuppressWarnings("unchecked")
			public Object intercept(XposedInterface.Chain chain) throws Throwable {
				Object result = chain.proceed();
				if (chain.getArgs().size() < 6) return result;
				List<ResolveInfo> infos = (List<ResolveInfo>)result;
				if (infos != null) {
					for (ResolveInfo info: infos)
						if (info != null && info.activityInfo != null && systemPackages.contains(info.activityInfo.packageName))
							XposedHelpers.setObjectField(info, "system", true);
				}
				return result;
			}
		});

//		// Causes module removal by system on updates
//		ModuleHelper.hookAllMethods("com.android.server.pm.PackageManagerService", lpparam.getClassLoader(), "getApplicationInfoInternal", new MethodHook() {
//			@Override
//			protected void after(final AfterHookCallback param) throws Throwable {
//				ApplicationInfo info = (ApplicationInfo)param.getResult();
//				if (info != null && systemPackages.contains(info.packageName)) {
//					info.flags |= ApplicationInfo.FLAG_SYSTEM;
//					param.returnAndSkip(info);
//				}
//			}
//		});

		ModuleHelper.findAndHookMethod("android.content.pm.ApplicationInfo", lpparam.getClassLoader(), "isSystemApp", new MethodHook() {
			@Override
			public Object intercept(XposedInterface.Chain chain) throws Throwable {
				ApplicationInfo ai = (ApplicationInfo)chain.getThisObject();
				return ai != null && systemPackages.contains(ai.packageName) ? true : chain.proceed();
			}
		});

		ModuleHelper.findAndHookMethodSilently("android.content.pm.ApplicationInfo", lpparam.getClassLoader(), "isSignedWithPlatformKey", new MethodHook() {
			@Override
			public Object intercept(XposedInterface.Chain chain) throws Throwable {
				ApplicationInfo ai = (ApplicationInfo)chain.getThisObject();
				return ai != null && systemPackages.contains(ai.packageName) ? true : chain.proceed();
			}
		});

		ModuleHelper.hookAllMethodsSilently("com.android.server.wm.ActivityRecordInjector", lpparam.getClassLoader(),
			"canShowWhenLocked", HookerClassHelper.returnConstant(true));

		try {
			Class<?> dpgpiClass = findClass("com.android.server.pm.MiuiDefaultPermissionGrantPolicy", lpparam.getClassLoader());
			String[] MIUI_SYSTEM_APPS = (String[])XposedHelpers.getStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS");
			ArrayList<String> mySystemApps = new ArrayList<String>(Arrays.asList(MIUI_SYSTEM_APPS));
			mySystemApps.addAll(systemPackages);
			XposedHelpers.setStaticObjectField(dpgpiClass, "MIUI_SYSTEM_APPS", mySystemApps.toArray(new String[0]));
		} catch (Throwable t) {
			XposedHelpers.log(t);
		}
	}

}
