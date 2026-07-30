package tv.withaibuild.customiuizer.mods.utils;

import static tv.withaibuild.customiuizer.mods.utils.XposedHelpers.log;
import static tv.withaibuild.customiuizer.utils.Helpers.getAppName;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.api.XposedModuleInterface;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.R;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.CustomMethodUnhooker;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.utils.Helpers;


public class ModuleHelper {
    public static final String NOT_EXIST_SYMBOL = "ObjectFieldNotExist";

    public static final String prefsName = "customiuizer_prefs";

    @SuppressLint("StaticFieldLeak")
    public static Context mModuleContext = null;

    static CopyOnWriteArraySet<PreferenceObserver> prefObservers = new CopyOnWriteArraySet<PreferenceObserver>();
    private static final ConcurrentHashMap<String, PreferenceObserver> keyedPrefObservers =
        new ConcurrentHashMap<String, PreferenceObserver>();

    private static class OwnedPreferenceObserver {
        final String key;
        final WeakReference<Object> ownerRef;
        final WeakReference<PreferenceObserver> observerRef;

        OwnedPreferenceObserver(String key, Object owner, PreferenceObserver observer) {
            this.key = key;
            this.ownerRef = new WeakReference<Object>(owner);
            this.observerRef = new WeakReference<PreferenceObserver>(observer);
        }
    }

    private static final CopyOnWriteArrayList<OwnedPreferenceObserver> ownedPrefObservers =
        new CopyOnWriteArrayList<OwnedPreferenceObserver>();

    private static Class<?> sActivityThreadClass;
    private static Method sCurrentApplicationMethod;
    private static Method sCurrentActivityThreadMethod;
    private static Method sGetSystemContextMethod;

    private static void ensureActivityThread(ClassLoader classLoader) throws Throwable {
        if (sActivityThreadClass != null) return;
        sActivityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader);
        sCurrentApplicationMethod = sActivityThreadClass.getDeclaredMethod("currentApplication");
        sCurrentApplicationMethod.setAccessible(true);
        sCurrentActivityThreadMethod = sActivityThreadClass.getDeclaredMethod("currentActivityThread");
        sCurrentActivityThreadMethod.setAccessible(true);
        sGetSystemContextMethod = sActivityThreadClass.getDeclaredMethod("getSystemContext");
        sGetSystemContextMethod.setAccessible(true);
    }

    public static CustomMethodUnhooker hookMethod(Method method, MethodHook callback) {
        try {
            CustomMethodUnhooker unhooker = XposedHelpers.doHookMethod(method, callback);
            return unhooker;
        } catch (Throwable t) {
            log("Failed to hook " + method.getName() + " method: " + t);
            return null;
        }
    }

    public static CustomMethodUnhooker findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            CustomMethodUnhooker unhooker = XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            return unhooker;
        } catch (Throwable t) {
            log("Failed to hook " + methodName + " method in " + className + ": " + t);
            return null;
        }
    }

    public static CustomMethodUnhooker findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            CustomMethodUnhooker unhooker = XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            return unhooker;
        } catch (Throwable t) {
            log("Failed to hook " + methodName + " method in " + clazz.getCanonicalName() + ": " + t);
            return null;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static CustomMethodUnhooker findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        try {
            CustomMethodUnhooker unhooker = XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
            return unhooker;
        } catch (Throwable t) {
            log("Failed to hook constructor in " + className + ": " + t);
            return null;
        }
    }

    public static void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedHelpers.hookAllConstructors(hookClass, callback).isEmpty()) {
                log("Failed to hook " + className + " constructor (no matching constructor found)");
            }
        } catch (Throwable t) {
            log("Failed to hook " + className + " constructor: " + t);
        }
    }

    public static void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        try {
            if (XposedHelpers.hookAllConstructors(hookClass, callback).isEmpty()) {
                log("Failed to hook " + hookClass.getCanonicalName() + " constructor");
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (hookClass == null || XposedHelpers.hookAllMethods(hookClass, methodName, callback).isEmpty()) {
                log("Failed to hook " + methodName + " method in " + className);
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            if (XposedHelpers.hookAllMethods(hookClass, methodName, callback).isEmpty()) {
                log("Failed to hook " + methodName + " method in " + hookClass.getCanonicalName());
            }
        } catch (Throwable t) {
            log(t);
        }
    }

    public static Object proxySystemProperties(String method, String prop, String val, ClassLoader classLoader) {
        return XposedHelpers.callStaticMethod(XposedHelpers.findClassIfExists("android.os.SystemProperties", classLoader),
            method, prop, val);
    }

    public static Object proxySystemProperties(String method, String prop, int val, ClassLoader classLoader) {
        return XposedHelpers.callStaticMethod(XposedHelpers.findClassIfExists("android.os.SystemProperties", classLoader),
            method, prop, val);
    }

    public static boolean hookAllMethodsSilently(String className, ClassLoader classLoader, String methodName, MethodHook callback) {
        try {
            Class<?> hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            boolean hooked = hookClass != null && XposedHelpers.hookAllMethods(hookClass, methodName, callback).size() > 0;
            return hooked;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback) {
        try {
            boolean hooked = hookClass != null && XposedHelpers.hookAllMethods(hookClass, methodName, callback).size() > 0;
            return hooked;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Object getStaticObjectFieldSilently(Class <?> clazz, String fieldName) {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            return NOT_EXIST_SYMBOL;
        }
    }

    public static Object getObjectFieldSilently(Object obj, String fieldName) {
        try {
            return XposedHelpers.getObjectField(obj, fieldName);
        } catch (Throwable t) {
            return NOT_EXIST_SYMBOL;
        }
    }

    public static Context findContext() {
        Context context = null;
        try {
            ensureActivityThread(MainModule.class.getClassLoader());
            context = (Context) sCurrentApplicationMethod.invoke(null);
            if (context == null) {
                Object currentActivityThread = sCurrentActivityThreadMethod.invoke(null);
                if (currentActivityThread != null) context = (Context) sGetSystemContextMethod.invoke(currentActivityThread);
            }
        } catch (Throwable ignore) {}
        return context;
    }

    public static Context findContext(XposedModuleInterface.PackageReadyParam lpparam) {
        Context context = null;
        try {
            ensureActivityThread(lpparam.getClassLoader());
            context = (Context) sCurrentApplicationMethod.invoke(null);
            if (context == null) {
                Object currentActivityThread = sCurrentActivityThreadMethod.invoke(null);
                if (currentActivityThread != null) context = (Context) sGetSystemContextMethod.invoke(currentActivityThread);
            }
        } catch (Throwable ignore) {}
        return context;
    }

    public static String stringifyBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        String string = "Bundle{";
        for (String key : bundle.keySet()) {
            string = string + " " + key + " -> " + bundle.get(key) + ";";
        }
        string += " }Bundle";
        return string;
    }

    public static long getNextMIUIAlarmTime(Context context) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), "next_alarm_clock_formatted");
        long nextTime = 0;
        if (!TextUtils.isEmpty(nextAlarm)) try {
            TimeZone timeZone = TimeZone.getTimeZone("UTC");
            SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context) ? "EHm" : "Ehma"), Locale.getDefault());
            dateFormat.setTimeZone(timeZone);
            long nextTimePart = dateFormat.parse(nextAlarm).getTime();

            Calendar cal = Calendar.getInstance(timeZone);
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTimeInMillis(nextTimePart);
            int targetDay = cal.get(Calendar.DAY_OF_WEEK);
            int targetHour = cal.get(Calendar.HOUR_OF_DAY);
            int targetMinute = cal.get(Calendar.MINUTE);

            cal = Calendar.getInstance();
            int diff = targetDay - cal.get(Calendar.DAY_OF_WEEK);
            if (diff < 0) diff += 7;

            cal.add(Calendar.DAY_OF_MONTH, diff);
            cal.set(Calendar.HOUR_OF_DAY, targetHour);
            cal.set(Calendar.MINUTE, targetMinute);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            nextTime = cal.getTimeInMillis();
        } catch (Throwable t) {
            log(t);
        }
        return nextTime;
    }
    public static void openAppInfo(Context context, String pkg, int user) {
        try {
            Intent intent = new Intent("miui.intent.action.APP_MANAGER_APPLICATION_DETAIL");
            intent.setPackage("com.miui.securitycenter");
            intent.putExtra("package_name", pkg);
            if (user != 0) intent.putExtra("miui.intent.extra.USER_ID", user);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setData(Uri.parse("package:" + pkg));
                if (user != 0)
                    XposedHelpers.callMethod(context, "startActivityAsUser", intent, XposedHelpers.newInstance(UserHandle.class, user));
                else
                    context.startActivity(intent);
            } catch (Throwable t2) {
                log(t2);
            }
        }
    }

    public interface PreferenceObserver {
        void onChange(String key);
    }

    public static void observePreferenceChange(PreferenceObserver prefObserver) {
        if (prefObserver != null) prefObservers.add(prefObserver);
    }

    public static void observePreferenceChange(String key, PreferenceObserver prefObserver) {
        if (prefObserver == null) {
            keyedPrefObservers.remove(key);
        } else {
            keyedPrefObservers.put(key, prefObserver);
        }
    }

    public static void observePreferenceChange(String key, Object owner, PreferenceObserver prefObserver) {
        if (owner == null) {
            observePreferenceChange(key, prefObserver);
            return;
        }
        dropOwnedObserver(key, owner);
        if (prefObserver != null)
            ownedPrefObservers.add(new OwnedPreferenceObserver(key, owner, prefObserver));
    }

    public static void removePreferenceObserver(String key, Object owner) {
        if (owner == null) {
            keyedPrefObservers.remove(key);
            return;
        }
        dropOwnedObserver(key, owner);
    }

    private static void dropOwnedObserver(@Nullable String key, @Nullable Object owner) {
        ownedPrefObservers.removeIf(registration -> {
            Object registrationOwner = registration.ownerRef.get();
            PreferenceObserver observer = registration.observerRef.get();
            if (registrationOwner == null || observer == null) return true;
            return registrationOwner == owner && registration.key.equals(key);
        });
    }

    public static void handlePreferenceChanged(@Nullable String key) {
        for (PreferenceObserver prefObserver : prefObservers) {
            try {
                prefObserver.onChange(key);
            } catch (Throwable t) {
                log(t);
            }
        }
        for (PreferenceObserver prefObserver : keyedPrefObservers.values()) {
            try {
                prefObserver.onChange(key);
            } catch (Throwable t) {
                log(t);
            }
        }
        boolean sawCleared = false;
        for (OwnedPreferenceObserver registration : ownedPrefObservers) {
            PreferenceObserver prefObserver = registration.observerRef.get();
            if (registration.ownerRef.get() == null || prefObserver == null) {
                sawCleared = true;
                continue;
            }
            try {
                prefObserver.onChange(key);
            } catch (Throwable t) {
                log(t);
            }
        }
        if (sawCleared) dropOwnedObserver(null, null);
    }

    private static class ReceiverRegistration {
        final Context context;
        final BroadcastReceiver receiver;

        ReceiverRegistration(Context context, BroadcastReceiver receiver) {
            Context applicationContext = context.getApplicationContext();
            this.context = applicationContext != null ? applicationContext : context;
            this.receiver = receiver;
        }
    }

    private static final ConcurrentHashMap<String, ReceiverRegistration> moduleReceivers =
        new ConcurrentHashMap<String, ReceiverRegistration>();

    public static boolean registerModuleReceiver(
        Context context,
        String key,
        BroadcastReceiver receiver,
        IntentFilter filter,
        int flags
    ) {
        unregisterModuleReceiver(key);
        Context registrationContext = context.getApplicationContext();
        if (registrationContext == null) registrationContext = context;
        try {
            registrationContext.registerReceiver(receiver, filter, flags);
            moduleReceivers.put(key, new ReceiverRegistration(registrationContext, receiver));
            return true;
        } catch (Throwable t) {
            log(key, t);
            return false;
        }
    }

    public static void unregisterModuleReceiver(String key) {
        ReceiverRegistration previous = moduleReceivers.remove(key);
        if (previous != null) releaseReceiver(previous);
    }

    private static class OwnedReceiverRegistration extends ReceiverRegistration {
        final WeakReference<Object> ownerRef;

        OwnedReceiverRegistration(Context context, Object owner, BroadcastReceiver receiver) {
            super(context, receiver);
            this.ownerRef = new WeakReference<Object>(owner);
        }
    }

    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<OwnedReceiverRegistration>> ownedReceivers =
        new ConcurrentHashMap<String, CopyOnWriteArrayList<OwnedReceiverRegistration>>();

    public static boolean registerOwnedReceiver(
        Context context,
        Object owner,
        String key,
        BroadcastReceiver receiver,
        IntentFilter filter,
        int flags
    ) {
        CopyOnWriteArrayList<OwnedReceiverRegistration> registrations =
            ownedReceivers.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<OwnedReceiverRegistration>());
        registrations.removeIf(registration -> {
            Object registrationOwner = registration.ownerRef.get();
            if (registrationOwner != null && registrationOwner != owner) return false;
            releaseReceiver(registration);
            return true;
        });
        Context registrationContext = context.getApplicationContext();
        if (registrationContext == null) registrationContext = context;
        try {
            registrationContext.registerReceiver(receiver, filter, flags);
            registrations.add(new OwnedReceiverRegistration(registrationContext, owner, receiver));
            return true;
        } catch (Throwable t) {
            log(key, t);
            return false;
        }
    }

    public static void unregisterOwnedReceiver(String key, Object owner) {
        CopyOnWriteArrayList<OwnedReceiverRegistration> registrations = ownedReceivers.get(key);
        if (registrations == null) return;
        registrations.removeIf(registration -> {
            Object registrationOwner = registration.ownerRef.get();
            if (registrationOwner != null && registrationOwner != owner) return false;
            releaseReceiver(registration);
            return true;
        });
        if (registrations.isEmpty()) ownedReceivers.remove(key, registrations);
    }

    private static final ConcurrentHashMap<String, Runnable> moduleRegistrations =
        new ConcurrentHashMap<String, Runnable>();

    public static void replaceModuleRegistration(String key, Runnable cleanup) {
        Runnable previous = moduleRegistrations.put(key, cleanup);
        if (previous != null) guarded("ModuleHelper.replaceRegistration:" + key, previous);
    }

    public static void clearModuleRegistration(String key) {
        Runnable previous = moduleRegistrations.remove(key);
        if (previous != null) guarded("ModuleHelper.clearRegistration:" + key, previous);
    }

    private static void releaseReceiver(ReceiverRegistration registration) {
        try {
            registration.context.unregisterReceiver(registration.receiver);
        } catch (Throwable ignored) {
            // The old Context may already have torn down the registration.
        }
    }

    public static synchronized Context getModuleContext(Context context) throws Throwable {
        return getModuleContext(context, null);
    }

    public static synchronized Context getModuleContext(Context context, Configuration config) throws Throwable {
        if (mModuleContext == null) {
            mModuleContext = context.createPackageContext(Helpers.modulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        }
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static synchronized Resources getModuleRes(Context context) throws Throwable {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }

    public static Drawable getActionImage(Context context, String key) {
        try {
            int action = MainModule.mPrefs.getInt(key + "_action", 1);
            Context modCtx = getModuleContext(context);
            if (action == 8)
                return Helpers.getAppIcon(modCtx, MainModule.mPrefs.getString(key + "_app", ""));
            else if (action == 20)
                return Helpers.getAppIcon(modCtx, MainModule.mPrefs.getString(key + "_activity", ""), true);
            else
                return null;
        } catch (Throwable t) {
            return null;
        }
    }
    public static String getActionName(Context context, String key) {
        try {
            int action = MainModule.mPrefs.getInt(key + "_action", 1);
            Resources modRes = getModuleRes(context);
            int resId = GlobalActions.getActionResId(action);
            if (resId != 0)
                return modRes.getString(resId);
            else if (action == 8)
                return (String)getAppName(getModuleContext(context), MainModule.mPrefs.getString(key + "_app", ""), true);
            else if (action == 9)
                return MainModule.mPrefs.getString(key + "_shortcut_name", "");
            else if (action == 10) {
                int what = MainModule.mPrefs.getInt(key + "_toggle", 0);
                switch (what) {
                    case 1: return modRes.getString(R.string.array_global_toggle_wifi);
                    case 2: return modRes.getString(R.string.array_global_toggle_bt);
                    case 3: return modRes.getString(R.string.array_global_toggle_gps);
                    case 4: return modRes.getString(R.string.array_global_toggle_nfc);
                    case 5: return modRes.getString(R.string.array_global_toggle_sound);
                    case 6: return modRes.getString(R.string.array_global_toggle_brightness);
                    case 7: return modRes.getString(R.string.array_global_toggle_rotation);
                    case 8: return modRes.getString(R.string.array_global_toggle_torch);
                    case 9: return modRes.getString(R.string.array_global_toggle_mobiledata);
                    default: return null;
                }
            } else if (action == 20) {
                Context ctx = getModuleContext(context);
                String pref = MainModule.mPrefs.getString(key + "_activity", "");
                String name = (String)getAppName(ctx, pref);
                if (name == null || name.isEmpty()) name = (String)getAppName(ctx, pref, true);
                return name;
            } else
                return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Runs a framework/deferred callback, logging instead of propagating any failure.
     *
     * Framework callbacks (BroadcastReceiver.onReceive, Handler.handleMessage,
     * Runnable.run, View listeners, etc.) run outside the MethodHook try/catch.
     * An uncaught exception inside them can crash system_server, SystemUI or Launcher.
     */
    public static void guarded(Runnable block) {
        try {
            block.run();
        } catch (Throwable t) {
            log(t);
        }
    }

    @FunctionalInterface
    interface CallbackFailureLogger {
        void log(String callbackName, Throwable failure);
    }

    private static final CallbackFailureLogger CALLBACK_FAILURE_LOGGER = XposedHelpers::log;

    /**
     * Named guarded variant for framework callbacks where release logs must identify
     * the failing registration without relying on an obfuscated callback class name.
     */
    public static void guarded(String callbackName, Runnable block) {
        guarded(callbackName, block, CALLBACK_FAILURE_LOGGER);
    }

    static void guarded(String callbackName, Runnable block, CallbackFailureLogger failureLogger) {
        try {
            block.run();
        } catch (Throwable t) {
            failureLogger.log(callbackName, t);
        }
    }

    /**
     * Guarded variant that returns a value. The fallback is returned when the body fails
     * so the framework sees a safe, "not consumed" result.
     */
    public static <T> T guarded(T fallback, Callable<T> block) {
        try {
            return block.call();
        } catch (Throwable t) {
            log(t);
            return fallback;
        }
    }

    /**
     * Named guarded variant for callbacks with an explicit, call-site-specific fallback.
     */
    public static <T> T guarded(String callbackName, T fallback, Callable<T> block) {
        return guarded(callbackName, fallback, block, CALLBACK_FAILURE_LOGGER);
    }

    static <T> T guarded(String callbackName, T fallback, Callable<T> block, CallbackFailureLogger failureLogger) {
        try {
            return block.call();
        } catch (Throwable t) {
            failureLogger.log(callbackName, t);
            return fallback;
        }
    }
}
