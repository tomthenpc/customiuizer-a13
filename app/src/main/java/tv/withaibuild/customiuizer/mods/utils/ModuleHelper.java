package tv.withaibuild.customiuizer.mods.utils;

import static tv.withaibuild.customiuizer.mods.utils.XposedHelpers.log;
import static tv.withaibuild.customiuizer.utils.HookUtils.getAppName;

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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Iterator;

import io.github.libxposed.api.XposedModuleInterface;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.R;
import tv.withaibuild.customiuizer.mods.GlobalActions;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.CustomMethodUnhooker;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;
import tv.withaibuild.customiuizer.utils.HookUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

// Reporting hook infrastructure used only by FeatureCatalog canary installers.
import tv.withaibuild.customiuizer.mods.utils.HookFailureReason;
import tv.withaibuild.customiuizer.mods.utils.HookInstaller;
import tv.withaibuild.customiuizer.mods.utils.HookOperation;


public class ModuleHelper {
    public static final String NOT_EXIST_SYMBOL = "ObjectFieldNotExist";

    public static final String prefsName = "customiuizer_prefs";

    @SuppressLint("StaticFieldLeak")
    public static Context mModuleContext = null;

    static CopyOnWriteArraySet<PreferenceObserver> prefObservers = new CopyOnWriteArraySet<PreferenceObserver>();
    private static final ConcurrentHashMap<String, PreferenceObserver> keyedPrefObservers =
        new ConcurrentHashMap<String, PreferenceObserver>();

    @FunctionalInterface
    public interface OwnedPreferenceCallback {
        void onChange(Object owner, String key);
    }

    private static class OwnedPreferenceObserver {
        final String key;
        final WeakReference<Object> ownerRef;
        final WeakReference<PreferenceObserver> observerRef;
        final OwnedPreferenceCallback callback;

        OwnedPreferenceObserver(String key, Object owner, PreferenceObserver observer) {
            this.key = key;
            this.ownerRef = new WeakReference<Object>(owner);
            this.observerRef = new WeakReference<PreferenceObserver>(observer);
            this.callback = null;
        }

        OwnedPreferenceObserver(String key, Object owner, OwnedPreferenceCallback callback) {
            this.key = key;
            this.ownerRef = new WeakReference<Object>(owner);
            this.observerRef = null;
            this.callback = callback;
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
        boolean recording = HookInstaller.isRecording();
        Class<?>[] paramTypes = extractParameterTypes(parameterTypesAndCallback);
        try {
            CustomMethodUnhooker unhooker;
            if (recording) {
                Class<?> hookClass = HookInstaller.resolveClassIfRecording(className, classLoader);
                if (hookClass != null) {
                    unhooker = XposedHelpers.findAndHookMethod(hookClass, methodName, parameterTypesAndCallback);
                } else {
                    // Class not in resolver cache; fall back to the legacy path.
                    unhooker = XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
                }
            } else {
                unhooker = XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            }
            if (unhooker != null && recording) {
                HookInstaller.recordInstall(className, methodName, HookOperation.EXACT_METHOD, Arrays.asList(paramTypes), 1);
            } else if (recording) {
                HookInstaller.recordFailure(className, methodName, HookOperation.EXACT_METHOD, Arrays.asList(paramTypes), HookFailureReason.MEMBER_NOT_FOUND);
            }
            return unhooker;
        } catch (Throwable t) {
            if (recording) {
                recordHookFailure(className, methodName, HookOperation.EXACT_METHOD, paramTypes, t);
            }
            log("Failed to hook " + methodName + " method in " + className + ": " + t);
            return null;
        }
    }

    public static CustomMethodUnhooker findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        boolean recording = HookInstaller.isRecording();
        Class<?>[] paramTypes = extractParameterTypes(parameterTypesAndCallback);
        try {
            CustomMethodUnhooker unhooker = XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            if (unhooker != null && recording) {
                HookInstaller.recordInstall(clazz.getName(), methodName, HookOperation.EXACT_METHOD, Arrays.asList(paramTypes), 1);
            } else if (recording) {
                HookInstaller.recordFailure(clazz.getName(), methodName, HookOperation.EXACT_METHOD, Arrays.asList(paramTypes), HookFailureReason.MEMBER_NOT_FOUND);
            }
            return unhooker;
        } catch (Throwable t) {
            if (recording) {
                recordHookFailure(clazz.getName(), methodName, HookOperation.EXACT_METHOD, paramTypes, t);
            }
            log("Failed to hook " + methodName + " method in " + clazz.getCanonicalName() + ": " + t);
            return null;
        }
    }

    private static void recordHookFailure(String className, String memberName, HookOperation operation, Class<?>[] parameterTypes, Throwable t) {
        HookFailureReason reason;
        if (t instanceof NoSuchMethodError) {
            reason = HookFailureReason.MEMBER_NOT_FOUND;
        } else if (t instanceof XposedHelpers.ClassNotFoundError || t.getCause() instanceof ClassNotFoundException) {
            reason = HookFailureReason.CLASS_NOT_FOUND;
        } else {
            reason = HookFailureReason.HOOK_FAILED;
        }
        HookInstaller.recordFailure(className, memberName, operation, Arrays.asList(parameterTypes), reason);
    }

    private static Class<?>[] extractParameterTypes(Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            return new Class<?>[0];
        }
        int n = parameterTypesAndCallback.length - 1;
        Class<?>[] result = new Class<?>[n];
        for (int i = 0; i < n; i++) {
            if (parameterTypesAndCallback[i] instanceof Class<?>) {
                result[i] = (Class<?>) parameterTypesAndCallback[i];
            } else {
                return new Class<?>[0];
            }
        }
        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean findAndHookMethodSilently(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        boolean recording = HookInstaller.isRecording();
        Class<?>[] paramTypes = extractParameterTypes(parameterTypesAndCallback);
        try {
            if (recording) {
                Class<?> hookClass = HookInstaller.resolveClassIfRecording(className, classLoader);
                if (hookClass != null) {
                    XposedHelpers.findAndHookMethod(hookClass, methodName, parameterTypesAndCallback);
                } else {
                    XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
                }
            } else {
                XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            }
            if (recording) HookInstaller.recordInstall(className, methodName, HookOperation.EXACT_METHOD, Arrays.asList(paramTypes), 1);
            return true;
        } catch (Throwable t) {
            if (recording) recordHookFailure(className, methodName, HookOperation.EXACT_METHOD, paramTypes, t);
            return false;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean findAndHookMethodSilently(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        boolean recording = HookInstaller.isRecording();
        Class<?>[] paramTypes = extractParameterTypes(parameterTypesAndCallback);
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            if (recording) HookInstaller.recordInstall(clazz.getName(), methodName, HookOperation.EXACT_METHOD, Arrays.asList(paramTypes), 1);
            return true;
        } catch (Throwable t) {
            if (recording) recordHookFailure(clazz.getName(), methodName, HookOperation.EXACT_METHOD, paramTypes, t);
            return false;
        }
    }

    public static CustomMethodUnhooker findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        boolean recording = HookInstaller.isRecording();
        Class<?>[] paramTypes = extractParameterTypes(parameterTypesAndCallback);
        try {
            CustomMethodUnhooker unhooker;
            if (recording) {
                Class<?> hookClass = HookInstaller.resolveClassIfRecording(className, classLoader);
                if (hookClass != null) {
                    unhooker = XposedHelpers.findAndHookConstructor(hookClass, parameterTypesAndCallback);
                } else {
                    unhooker = XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
                }
            } else {
                unhooker = XposedHelpers.findAndHookConstructor(className, classLoader, parameterTypesAndCallback);
            }
            if (unhooker != null && recording) {
                HookInstaller.recordInstall(className, null, HookOperation.EXACT_CONSTRUCTOR, Arrays.asList(paramTypes), 1);
            } else if (recording) {
                HookInstaller.recordFailure(className, null, HookOperation.EXACT_CONSTRUCTOR, Arrays.asList(paramTypes), HookFailureReason.MEMBER_NOT_FOUND);
            }
            return unhooker;
        } catch (Throwable t) {
            if (recording) recordHookFailure(className, null, HookOperation.EXACT_CONSTRUCTOR, paramTypes, t);
            log("Failed to hook constructor in " + className + ": " + t);
            return null;
        }
    }

    public static void hookAllConstructors(String className, ClassLoader classLoader, MethodHook callback) {
        boolean recording = HookInstaller.isRecording();
        try {
            Class<?> hookClass;
            if (recording) {
                hookClass = HookInstaller.resolveClassIfRecording(className, classLoader);
                if (hookClass == null) hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            } else {
                hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            }
            if (hookClass == null) {
                if (recording) HookInstaller.recordClassFailure(className, HookFailureReason.CLASS_NOT_FOUND);
                log("Failed to hook " + className + " constructor (no matching constructor found)");
                return;
            }
            Set<CustomMethodUnhooker> unhooks = XposedHelpers.hookAllConstructors(hookClass, callback);
            if (unhooks.isEmpty()) {
                if (recording) HookInstaller.recordFailure(className, null, HookOperation.ALL_CONSTRUCTORS, Arrays.asList(new Class<?>[0]), HookFailureReason.MEMBER_NOT_FOUND);
                log("Failed to hook " + className + " constructor (no matching constructor found)");
            } else if (recording) {
                HookInstaller.recordInstall(className, null, HookOperation.ALL_CONSTRUCTORS, Arrays.asList(new Class<?>[0]), unhooks.size());
            }
        } catch (Throwable t) {
            if (recording) recordHookFailure(className, null, HookOperation.ALL_CONSTRUCTORS, new Class<?>[0], t);
            log("Failed to hook " + className + " constructor: " + t);
        }
    }

    public static void hookAllConstructors(Class<?> hookClass, MethodHook callback) {
        boolean recording = HookInstaller.isRecording();
        try {
            Set<CustomMethodUnhooker> unhooks = XposedHelpers.hookAllConstructors(hookClass, callback);
            if (unhooks.isEmpty()) {
                if (recording) HookInstaller.recordFailure(hookClass.getName(), null, HookOperation.ALL_CONSTRUCTORS, Arrays.asList(new Class<?>[0]), HookFailureReason.MEMBER_NOT_FOUND);
                log("Failed to hook " + hookClass.getCanonicalName() + " constructor");
            } else if (recording) {
                HookInstaller.recordInstall(hookClass.getName(), null, HookOperation.ALL_CONSTRUCTORS, Arrays.asList(new Class<?>[0]), unhooks.size());
            }
        } catch (Throwable t) {
            if (recording) recordHookFailure(hookClass.getName(), null, HookOperation.ALL_CONSTRUCTORS, new Class<?>[0], t);
            log(t);
        }
    }

    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, MethodHook callback) {
        boolean recording = HookInstaller.isRecording();
        try {
            Class<?> hookClass;
            if (recording) {
                hookClass = HookInstaller.resolveClassIfRecording(className, classLoader);
                if (hookClass == null) hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            } else {
                hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            }
            if (hookClass == null) {
                if (recording) HookInstaller.recordClassFailure(className, HookFailureReason.CLASS_NOT_FOUND);
                log("Failed to hook " + methodName + " method in " + className);
                return;
            }
            Set<CustomMethodUnhooker> unhooks = XposedHelpers.hookAllMethods(hookClass, methodName, callback);
            if (unhooks.isEmpty()) {
                if (recording) HookInstaller.recordFailure(className, methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), HookFailureReason.MEMBER_NOT_FOUND);
                log("Failed to hook " + methodName + " method in " + className);
            } else if (recording) {
                HookInstaller.recordInstall(className, methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), unhooks.size());
            }
        } catch (Throwable t) {
            if (recording) recordHookFailure(className, methodName, HookOperation.ALL_METHODS_BY_NAME, new Class<?>[0], t);
            log(t);
        }
    }

    public static void hookAllMethods(Class<?> hookClass, String methodName, MethodHook callback) {
        boolean recording = HookInstaller.isRecording();
        try {
            Set<CustomMethodUnhooker> unhooks = XposedHelpers.hookAllMethods(hookClass, methodName, callback);
            if (unhooks.isEmpty()) {
                if (recording) HookInstaller.recordFailure(hookClass.getName(), methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), HookFailureReason.MEMBER_NOT_FOUND);
                log("Failed to hook " + methodName + " method in " + hookClass.getCanonicalName());
            } else if (recording) {
                HookInstaller.recordInstall(hookClass.getName(), methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), unhooks.size());
            }
        } catch (Throwable t) {
            if (recording) recordHookFailure(hookClass.getName(), methodName, HookOperation.ALL_METHODS_BY_NAME, new Class<?>[0], t);
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
        boolean recording = HookInstaller.isRecording();
        try {
            Class<?> hookClass;
            if (recording) {
                hookClass = HookInstaller.resolveClassIfRecording(className, classLoader);
                if (hookClass == null) hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            } else {
                hookClass = XposedHelpers.findClassIfExists(className, classLoader);
            }
            if (hookClass == null) {
                if (recording) HookInstaller.recordClassFailure(className, HookFailureReason.CLASS_NOT_FOUND);
                return false;
            }
            Set<CustomMethodUnhooker> unhooks = XposedHelpers.hookAllMethods(hookClass, methodName, callback);
            boolean hooked = !unhooks.isEmpty();
            if (recording) {
                if (hooked) HookInstaller.recordInstall(className, methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), unhooks.size());
                else HookInstaller.recordFailure(className, methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), HookFailureReason.MEMBER_NOT_FOUND);
            }
            return hooked;
        } catch (Throwable t) {
            if (recording) recordHookFailure(className, methodName, HookOperation.ALL_METHODS_BY_NAME, new Class<?>[0], t);
            return false;
        }
    }

    public static boolean hookAllMethodsSilently(Class<?> hookClass, String methodName, MethodHook callback) {
        boolean recording = HookInstaller.isRecording();
        try {
            if (hookClass == null) {
                return false;
            }
            Set<CustomMethodUnhooker> unhooks = XposedHelpers.hookAllMethods(hookClass, methodName, callback);
            boolean hooked = !unhooks.isEmpty();
            if (recording) {
                if (hooked) HookInstaller.recordInstall(hookClass.getName(), methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), unhooks.size());
                else HookInstaller.recordFailure(hookClass.getName(), methodName, HookOperation.ALL_METHODS_BY_NAME, Arrays.asList(new Class<?>[0]), HookFailureReason.MEMBER_NOT_FOUND);
            }
            return hooked;
        } catch (Throwable t) {
            if (recording) recordHookFailure(hookClass.getName(), methodName, HookOperation.ALL_METHODS_BY_NAME, new Class<?>[0], t);
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

    /**
     * Registers an owner-aware callback without requiring the owner to retain a separate
     * observer object. The callback is retained strongly, while the owner is passed in only
     * after dereferencing its weak reference. Callers must use the supplied owner argument
     * instead of capturing the owner in the callback closure.
     */
    public static void observeOwnedPreferenceChange(
        String key,
        Object owner,
        OwnedPreferenceCallback callback
    ) {
        if (owner == null || callback == null) return;
        dropOwnedObserver(key, owner);
        ownedPrefObservers.add(new OwnedPreferenceObserver(key, owner, callback));
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
            PreferenceObserver observer =
                registration.observerRef == null ? null : registration.observerRef.get();
            if (
                registrationOwner == null ||
                (registration.callback == null && observer == null)
            ) return true;
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
            Object owner = registration.ownerRef.get();
            PreferenceObserver prefObserver =
                registration.observerRef == null ? null : registration.observerRef.get();
            if (
                owner == null ||
                (registration.callback == null && prefObserver == null)
            ) {
                sawCleared = true;
                continue;
            }
            try {
                if (registration.callback != null) {
                    registration.callback.onChange(owner, key);
                } else {
                    prefObserver.onChange(key);
                }
            } catch (Throwable t) {
                log(t);
            }
        }
        if (sawCleared) dropOwnedObserver(null, null);
    }

    private static final int MAX_STALE_RECEIVERS = 3;

    private enum RegistrationState {
        PENDING_REGISTER,
        ACTIVE,
        STALE,
        RELEASED,
        REGISTER_FAILED
    }

    private static class ReceiverRegistration {
        final Context context;
        final BroadcastReceiver receiver;
        final String key;
        volatile RegistrationState state;

        ReceiverRegistration(Context context, BroadcastReceiver receiver, String key, RegistrationState state) {
            Context applicationContext = context.getApplicationContext();
            this.context = applicationContext != null ? applicationContext : context;
            this.receiver = receiver;
            this.key = key;
            this.state = state;
        }
    }

    private static final ConcurrentHashMap<String, ReceiverRegistration> moduleReceivers =
        new ConcurrentHashMap<String, ReceiverRegistration>();
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<ReceiverRegistration>> staleModuleReceivers =
        new ConcurrentHashMap<String, ConcurrentLinkedDeque<ReceiverRegistration>>();

    public static boolean registerModuleReceiver(
        Context context,
        String key,
        BroadcastReceiver receiver,
        IntentFilter filter,
        int flags
    ) {
        final Context registrationContext = context.getApplicationContext() != null
            ? context.getApplicationContext() : context;

        final ReceiverRegistration newRegistration =
            new ReceiverRegistration(registrationContext, receiver, key, RegistrationState.PENDING_REGISTER);

        synchronized (moduleReceivers) {
            // 1. Try to release any stale receivers to free slots before registering.
            if (!drainModuleStale(key)) return false;

            // 2. Register the new receiver with the framework first.
            //    The old active receiver stays in the map and keeps working if this fails.
            try {
                registrationContext.registerReceiver(receiver, filter, flags);
            } catch (Throwable t) {
                log(key, t);
                newRegistration.state = RegistrationState.REGISTER_FAILED;
                return false;
            }

            // 3. The new receiver is live; make it active and release the previous one.
            newRegistration.state = RegistrationState.ACTIVE;
            ReceiverRegistration previous = moduleReceivers.put(key, newRegistration);
            if (previous != null) {
                releaseReceiver(previous);
            }

            // 4. Identity check: another thread may have replaced us.
            ReceiverRegistration current = moduleReceivers.get(key);
            if (current == newRegistration) {
                return true;
            }

            // We lost the race; clean up our own registration.
            releaseReceiver(newRegistration);
            return false;
        }
    }

    public static void unregisterModuleReceiver(String key) {
        synchronized (moduleReceivers) {
            ReceiverRegistration previous = moduleReceivers.remove(key);
            if (previous != null) releaseReceiver(previous);
            drainModuleStale(key);
        }
    }

    private static class OwnedReceiverRegistration extends ReceiverRegistration {
        final WeakReference<Object> ownerRef;

        OwnedReceiverRegistration(Context context, Object owner, BroadcastReceiver receiver, String key) {
            super(context, receiver, key, RegistrationState.PENDING_REGISTER);
            this.ownerRef = new WeakReference<Object>(owner);
        }
    }

    /** Per-key bucket for owned receivers. The bucket is the unit of locking. */
    private static class OwnedReceiverBucket {
        final ArrayList<OwnedReceiverRegistration> registrations = new ArrayList<>();
    }

    private static final ConcurrentHashMap<String, OwnedReceiverBucket> ownedReceivers =
        new ConcurrentHashMap<String, OwnedReceiverBucket>();
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<OwnedReceiverRegistration>> staleOwnedReceivers =
        new ConcurrentHashMap<String, ConcurrentLinkedDeque<OwnedReceiverRegistration>>();

    public static boolean registerOwnedReceiver(
        Context context,
        Object owner,
        String key,
        BroadcastReceiver receiver,
        IntentFilter filter,
        int flags
    ) {
        final Context registrationContext = context.getApplicationContext() != null
            ? context.getApplicationContext() : context;

        final OwnedReceiverRegistration newRegistration =
            new OwnedReceiverRegistration(registrationContext, owner, receiver, key);

        while (true) {
            final OwnedReceiverBucket bucket =
                ownedReceivers.computeIfAbsent(key, k -> new OwnedReceiverBucket());

            synchronized (bucket) {
                // If the bucket we locked is no longer in the map, another thread
                // removed it; retry with the current (or a new) bucket.
                if (ownedReceivers.get(key) != bucket) continue;

                // Try to release any stale receivers to free slots before registering.
                if (!drainOwnedStale(key)) return false;

                try {
                    registrationContext.registerReceiver(receiver, filter, flags);
                } catch (Throwable t) {
                    log(key, t);
                    newRegistration.state = RegistrationState.REGISTER_FAILED;
                    return false;
                }

                newRegistration.state = RegistrationState.ACTIVE;

                // Replace stale, dead-owner or same-owner registrations. Keep different owners.
                for (int i = bucket.registrations.size() - 1; i >= 0; i--) {
                    OwnedReceiverRegistration r = bucket.registrations.get(i);
                    Object existingOwner = r.ownerRef.get();
                    if (existingOwner != null && existingOwner != owner) continue;
                    releaseReceiver(r);
                    bucket.registrations.remove(i);
                }
                bucket.registrations.add(newRegistration);

                if (bucket.registrations.contains(newRegistration)) {
                    return true;
                }

                releaseReceiver(newRegistration);
                return false;
            }
        }
    }

    public static void unregisterOwnedReceiver(String key, Object owner) {
        OwnedReceiverBucket bucket = ownedReceivers.get(key);
        if (bucket == null) return;
        synchronized (bucket) {
            // If the bucket is no longer tracked, another thread replaced/removed it.
            if (ownedReceivers.get(key) != bucket) return;

            for (int i = bucket.registrations.size() - 1; i >= 0; i--) {
                OwnedReceiverRegistration r = bucket.registrations.get(i);
                Object registrationOwner = r.ownerRef.get();
                if (registrationOwner != null && registrationOwner != owner) continue;
                releaseReceiver(r);
                bucket.registrations.remove(i);
            }
            if (bucket.registrations.isEmpty()) {
                ownedReceivers.remove(key, bucket);
            }
            drainOwnedStale(key);
        }
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

    private static boolean tryRelease(ReceiverRegistration registration) {
        if (registration == null) return true;
        synchronized (registration) {
            switch (registration.state) {
                case RELEASED:
                    return true;
                case PENDING_REGISTER:
                case REGISTER_FAILED:
                    registration.state = RegistrationState.RELEASED;
                    return true;
                case ACTIVE:
                case STALE:
                    try {
                        registration.context.unregisterReceiver(registration.receiver);
                        registration.state = RegistrationState.RELEASED;
                        return true;
                    } catch (Throwable t) {
                        registration.state = RegistrationState.STALE;
                        return false;
                    }
            }
        }
        return false;
    }

    private static void releaseReceiver(ReceiverRegistration registration) {
        if (registration == null) return;
        synchronized (registration) {
            switch (registration.state) {
                case RELEASED:
                case REGISTER_FAILED:
                case STALE:
                    return;
                case PENDING_REGISTER:
                    registration.state = RegistrationState.RELEASED;
                    return;
                case ACTIVE:
                    try {
                        registration.context.unregisterReceiver(registration.receiver);
                        registration.state = RegistrationState.RELEASED;
                    } catch (Throwable t) {
                        registration.state = RegistrationState.STALE;
                        if (registration instanceof OwnedReceiverRegistration) {
                            addToStale(staleOwnedReceivers, registration.key, (OwnedReceiverRegistration) registration);
                        } else {
                            addToStale(staleModuleReceivers, registration.key, registration);
                        }
                    }
                    return;
            }
        }
    }

    /**
     * Atomically add a stale registration to the deque for {@code key}. The deque
     * is the synchronization object; if the deque we locked is no longer in the
     * map, we retry with the current (or newly created) deque. This prevents
     * adding to a detached deque that is about to be removed.
     */
    private static <T extends ReceiverRegistration> void addToStale(
        ConcurrentHashMap<String, ConcurrentLinkedDeque<T>> map,
        String key,
        T reg
    ) {
        while (true) {
            ConcurrentLinkedDeque<T> deque = map.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<T>());
            synchronized (deque) {
                if (map.get(key) == deque) {
                    deque.add(reg);
                    return;
                }
                // Deque was removed while we were acquiring the lock; retry.
            }
        }
    }

    private static boolean drainModuleStale(String key) {
        return drainStale(staleModuleReceivers, key, MAX_STALE_RECEIVERS);
    }

    private static boolean drainOwnedStale(String key) {
        return drainStale(staleOwnedReceivers, key, MAX_STALE_RECEIVERS);
    }

    /**
     * Drain a stale deque for {@code key} without losing concurrent re-adds.
     * The drain and the {@link #addToStale} path synchronize on the same deque
     * and re-check that the deque is still the one mapped to {@code key}.
     */
    private static <T extends ReceiverRegistration> boolean drainStale(
        ConcurrentHashMap<String, ConcurrentLinkedDeque<T>> map,
        String key,
        int max
    ) {
        ConcurrentLinkedDeque<T> deque = map.get(key);
        while (true) {
            if (deque == null) return true;
            synchronized (deque) {
                if (map.get(key) == deque) {
                    if (!deque.isEmpty()) {
                        Iterator<T> it = deque.iterator();
                        while (it.hasNext()) {
                            T reg = it.next();
                            if (tryRelease(reg)) {
                                it.remove();
                            }
                        }
                    }
                    if (deque.isEmpty()) {
                        map.remove(key, deque);
                    }
                    break;
                }
            }
            // The deque was detached by another drain; get the current one and retry.
            deque = map.get(key);
        }
        ConcurrentLinkedDeque<T> after = map.get(key);
        return after == null || after.isEmpty() || after.size() < max;
    }

    public static synchronized Context getModuleContext(Context context) throws Throwable {
        return getModuleContext(context, null);
    }

    public static synchronized Context getModuleContext(Context context, Configuration config) throws Throwable {
        if (mModuleContext == null) {
            mModuleContext = context.createPackageContext(HookUtils.modulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
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
                return HookUtils.getAppIcon(modCtx, MainModule.mPrefs.getString(key + "_app", ""));
            else if (action == 20)
                return HookUtils.getAppIcon(modCtx, MainModule.mPrefs.getString(key + "_activity", ""), true);
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
            if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;
            log(t);
        }
    }

    @FunctionalInterface
    interface CallbackFailureLogger {
        void log(String callbackName, Throwable failure);
    }

    private static final CallbackFailureLogger CALLBACK_FAILURE_LOGGER = XposedHelpers::log;
    private static final ConcurrentHashMap<String, Boolean> loggedCallbackFailures =
        new ConcurrentHashMap<String, Boolean>();

    private static void logGuardedFailure(
        String callbackName,
        Throwable failure,
        CallbackFailureLogger failureLogger
    ) {
        if (loggedCallbackFailures.putIfAbsent(callbackName, Boolean.TRUE) == null) {
            failureLogger.log(callbackName, failure);
        }
    }

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
            if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;
            logGuardedFailure(callbackName, t, failureLogger);
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
            if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;
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
            if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;
            logGuardedFailure(callbackName, t, failureLogger);
            return fallback;
        }
    }
}
