package tv.withaibuild.customiuizer.mods.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;

/**
 * Hot-path resource replacement hooks. Resources getter callbacks are bound to a
 * fixed integer {@code kind} at install time; no Executable name lookup or string
 * comparison happens on the hot path.
 *
 * <p>Each kind has a dedicated MethodHook created once and reused for every call.
 * The per-kind bit in {@link #installedMask} records which Resources methods are
 * already hooked, so partial failures can be retried without double-hooking.
 */
public class ResourceHooks {

    private enum InstallState {
        UNINSTALLED,
        INSTALLING,
        INSTALLED,
        PARTIAL_FAILED
    }

    public enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    // Fixed method kinds. The index in the parallel arrays is also the bit offset
    // in installedMask (1 << index).
    static final int KIND_INTEGER = 0;
    static final int KIND_LAYOUT = 1;
    static final int KIND_FRACTION = 2;
    static final int KIND_BOOLEAN = 3;
    static final int KIND_DIMENSION = 4;
    static final int KIND_DIMENSION_PIXEL_OFFSET = 5;
    static final int KIND_DIMENSION_PIXEL_SIZE = 6;
    static final int KIND_TEXT = 7;
    static final int KIND_STRING = 8;
    static final int KIND_DRAWABLE_FOR_DENSITY = 9;
    static final int KIND_INT_ARRAY = 10;
    static final int KIND_STRING_ARRAY = 11;
    static final int KIND_TEXT_ARRAY = 12;
    static final int KIND_ANIMATION = 13;

    private static final int KIND_COUNT = 14;
    private static final int[] KINDS = {
        KIND_INTEGER, KIND_LAYOUT, KIND_FRACTION, KIND_BOOLEAN,
        KIND_DIMENSION, KIND_DIMENSION_PIXEL_OFFSET, KIND_DIMENSION_PIXEL_SIZE,
        KIND_TEXT, KIND_STRING, KIND_DRAWABLE_FOR_DENSITY,
        KIND_INT_ARRAY, KIND_STRING_ARRAY, KIND_TEXT_ARRAY, KIND_ANIMATION
    };

    private static final String[] NAMES = {
        "getInteger",
        "getLayout",
        "getFraction",
        "getBoolean",
        "getDimension",
        "getDimensionPixelOffset",
        "getDimensionPixelSize",
        "getText",
        "getString",
        "getDrawableForDensity",
        "getIntArray",
        "getStringArray",
        "getTextArray",
        "getAnimation"
    };

    private static final int[] EXTRA_ARGS = {
        0, 0, 2, 0,
        0, 0, 0,
        0, 0, 2,
        0, 0, 0, 0
    };

    private static final int ALL_METHODS_MASK = 0x00003fff; // (1 << 14) - 1
    private static final int MAX_ACTIVE = 256;

    private final AtomicReference<InstallState> installState =
        new AtomicReference<>(InstallState.UNINSTALLED);
    private final AtomicInteger installedMask = new AtomicInteger(0);

    private final SparseIntArray fakes = new SparseIntArray();
    private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> unresolved =
        new ConcurrentHashMap<>();
    private final AtomicReference<SparseArray<Pair<ReplacementType, Object>>> active =
        new AtomicReference<>(new SparseArray<Pair<ReplacementType, Object>>());

    public static int getFakeResId(String resourceName) {
        return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
    }

    public ResourceHooks() {}

    /**
     * Hot path entry. The kind is fixed at install time; no runtime Executable
     * or string lookup is performed here.
     */
    private Object interceptResource(int kind, XposedInterface.Chain chain) throws Throwable {
        // Fastest: no state at all.
        if (fakes.size() == 0 && unresolved.isEmpty() && active.get().size() == 0)
            return chain.proceed();

        int resId = (int) chain.getArg(0);

        int modResId = fakes.get(resId);
        if (modResId != 0) {
            Context mContext = ModuleHelper.findContext();
            if (mContext != null) {
                Object value = getFakeResource(mContext, kind, modResId, chain);
                if (value != null) return value;
            }
        }

        if (unresolved.isEmpty() && active.get().size() == 0) return chain.proceed();

        Object value = getResourceReplacement(resId, (Resources) chain.getThisObject(), kind, chain);
        if (value == null) return chain.proceed();

        if (kind == KIND_DIMENSION_PIXEL_OFFSET || kind == KIND_DIMENSION_PIXEL_SIZE) {
            if (value instanceof Float) value = ((Float) value).intValue();
        }
        return value;
    }

    private MethodHook createReplacementHook(final int kind) {
        return new MethodHook() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                return interceptResource(kind, chain);
            }
        };
    }

    /**
     * Install all Resources getter hooks. Each method is tracked by a bit in
     * {@link #installedMask}. Successful bits stay set; failed bits can be retried
     * by the next call. No method is ever registered twice.
     */
    private void applyHooks() {
        InstallState current;
        while (true) {
            current = installState.get();
            if (current == InstallState.INSTALLED || current == InstallState.INSTALLING) return;
            if (installState.compareAndSet(current, InstallState.INSTALLING)) break;
        }

        try {
            boolean anyFailed = false;
            for (int i = 0; i < KIND_COUNT; i++) {
                final int bit = 1 << i;
                // Try to claim this bit; if claimed, install and reset bit on failure.
                while (true) {
                    int mask = installedMask.get();
                    if ((mask & bit) != 0) break;
                    if (!installedMask.compareAndSet(mask, mask | bit)) continue;
                    try {
                        ModuleHelper.findAndHookMethod(
                            Resources.class,
                            NAMES[i],
                            buildHookArgs(i, createReplacementHook(KINDS[i]))
                        );
                    } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
                        logNonFatal(t);
                        installedMask.compareAndSet(mask | bit, mask);
                        anyFailed = true;
                    }
                    break;
                }
            }

            int finalMask = installedMask.get();
            installState.set(finalMask == ALL_METHODS_MASK ? InstallState.INSTALLED : InstallState.PARTIAL_FAILED);
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
            installState.set(InstallState.PARTIAL_FAILED);
        }
    }

    Class<?>[] getParamTypes(int index) {
        switch (KINDS[index]) {
            case KIND_FRACTION:
                return new Class<?>[] { int.class, int.class, int.class };
            case KIND_DRAWABLE_FOR_DENSITY:
                return new Class<?>[] { int.class, int.class, Resources.Theme.class };
            default:
                return new Class<?>[] { int.class };
        }
    }

    Object[] buildHookArgs(int index, MethodHook callback) {
        Class<?>[] paramTypes = getParamTypes(index);
        Object[] hookArgs = new Object[paramTypes.length + 1];
        System.arraycopy(paramTypes, 0, hookArgs, 0, paramTypes.length);
        hookArgs[paramTypes.length] = callback;
        return hookArgs;
    }

    public int addResource(String resName, int resId) {
        try {
            applyHooks();
            int fakeResId = getFakeResId(resName);
            fakes.put(fakeResId, resId);
            return fakeResId;
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
            return 0;
        }
    }

    private Object getFakeResource(
        Context context,
        int kind,
        int modResId,
        XposedInterface.Chain chain
    ) {
        try {
            if (context == null || fakes.size() == 0) return null;
            Resources modRes = ModuleHelper.getModuleRes(context);
            return callModuleResource(modRes, kind, modResId, chain);
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
            return null;
        }
    }

    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            unresolved.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.ID, replacementResId));
            active.set(new SparseArray<Pair<ReplacementType, Object>>());
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
        }
    }

    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            unresolved.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.DENSITY, replacementResValue));
            active.set(new SparseArray<Pair<ReplacementType, Object>>());
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
        }
    }

    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            unresolved.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.OBJECT, replacementResValue));
            active.set(new SparseArray<Pair<ReplacementType, Object>>());
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
        }
    }

    private Object getResourceReplacement(
        int resId,
        Resources res,
        int kind,
        XposedInterface.Chain chain
    ) {
        if (unresolved.isEmpty()) return null;

        SparseArray<Pair<ReplacementType, Object>> current = active.get();
        Pair<ReplacementType, Object> replacement = current.get(resId);

        if (replacement == null) {
            String pkgName = null;
            String resType = null;
            String resName = null;
            try {
                pkgName = res.getResourcePackageName(resId);
                resType = res.getResourceTypeName(resId);
                resName = res.getResourceEntryName(resId);
            } catch (OutOfMemoryError oom) {
                throw oom;
            } catch (Throwable t) {
                if (t instanceof VirtualMachineError || t instanceof ThreadDeath) throw t;
            }
            if (pkgName == null || resType == null || resName == null) return null;

            String resFullName = pkgName + ":" + resType + "/" + resName;
            replacement = unresolved.get(resFullName);
            if (replacement == null)
                replacement = unresolved.get("*:" + resType + "/" + resName);
            if (replacement == null) return null;

            // Copy-on-write to keep the hot path lock-free.
            while (true) {
                SparseArray<Pair<ReplacementType, Object>> copy = current.clone();
                if (copy.size() >= MAX_ACTIVE) copy.removeAt(0);
                copy.put(resId, replacement);
                if (active.compareAndSet(current, copy)) break;
                current = active.get();
            }
        }

        try {
            if (replacement.first == ReplacementType.OBJECT) return replacement.second;
            else if (replacement.first == ReplacementType.DENSITY) {
                return (Float) replacement.second * res.getDisplayMetrics().density;
            }

            int modResId = (Integer) replacement.second;
            if (modResId == 0) return null;

            // ID replacements are the only path that needs the module context.
            Context mContext = ModuleHelper.findContext();
            if (mContext == null) return null;
            Resources modRes = ModuleHelper.getModuleRes(mContext);
            return callModuleResource(modRes, kind, modResId, chain);
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
            return null;
        }
    }

    private static Object callModuleResource(
        Resources modRes,
        int kind,
        int modResId,
        XposedInterface.Chain chain
    ) {
        String method = NAMES[kind];
        int extra = EXTRA_ARGS[kind];
        try {
            switch (extra) {
                case 0:
                    return XposedHelpers.callMethod(modRes, method, modResId);
                case 1:
                    return XposedHelpers.callMethod(modRes, method, modResId, chain.getArg(1));
                case 2:
                    return XposedHelpers.callMethod(modRes, method, modResId, chain.getArg(1), chain.getArg(2));
                default:
                    return null;
            }
        } catch (OutOfMemoryError oom) { throw oom; } catch (Throwable t) {
            logNonFatal(t);
            return null;
        }
    }

    private static void logNonFatal(Throwable t) {
        try {
            XposedHelpers.log(t);
        } catch (OutOfMemoryError oom) {
            throw oom;
        } catch (Throwable ex) {
            if (ex instanceof VirtualMachineError || ex instanceof ThreadDeath) throw ex;
        }
    }
}
