package tv.withaibuild.customiuizer.mods.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;

/**
 * Hot-path resource replacement hooks. All per-call reflection and string
 * lookups are avoided on the resolved cache hit path.
 *
 * <p>Each hooked {@link Resources} method gets a dedicated {@link TypedMethodHook}
 * that already knows its method name and extra-argument count. The method name
 * is no longer read from {@code chain.getExecutable().getName()} on every call.
 *
 * <p>{@link #applyHooks()} uses an explicit install state machine and a per-method
 * installed set so failures can be retried without double-hooking the same method.
 */
public class ResourceHooks {

    /** Install state for the whole hook family. */
    private enum InstallState {
        UNINSTALLED,
        INSTALLING,
        INSTALLED,
        FAILED_PARTIAL
    }

    /** Method types we replace. Each instance is created exactly once. */
    private enum ResourceMethod {
        INTEGER("getInteger", 0),
        LAYOUT("getLayout", 0),
        FRACTION("getFraction", 2),
        BOOLEAN("getBoolean", 0),
        DIMENSION("getDimension", 0),
        DIMENSION_PIXEL_OFFSET("getDimensionPixelOffset", 0),
        DIMENSION_PIXEL_SIZE("getDimensionPixelSize", 0),
        TEXT("getText", 0),
        STRING("getString", 0),
        DRAWABLE_FOR_DENSITY("getDrawableForDensity", 2),
        INT_ARRAY("getIntArray", 0),
        STRING_ARRAY("getStringArray", 0),
        TEXT_ARRAY("getTextArray", 0),
        ANIMATION("getAnimation", 0);

        final String name;
        final int extraArgs;

        ResourceMethod(String name, int extraArgs) {
            this.name = name;
            this.extraArgs = extraArgs;
        }
    }

    public enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    /** Set once the hook family is live; retries are allowed from FAILED_PARTIAL. */
    private final AtomicReference<InstallState> installState =
        new AtomicReference<>(InstallState.UNINSTALLED);

    /** Tracks which specific Resources method has already been hooked successfully. */
    private final Set<String> installedMethods = ConcurrentHashMap.newKeySet();

    private final SparseIntArray fakes = new SparseIntArray();
    private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> unresolved =
        new ConcurrentHashMap<>();
    private final AtomicReference<SparseArray<Pair<ReplacementType, Object>>> active =
        new AtomicReference<>(new SparseArray<Pair<ReplacementType, Object>>());

    private static final int MAX_ACTIVE = 256;

    public static int getFakeResId(String resourceName) {
        return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
    }

    public ResourceHooks() {}

    /** Hook with fixed method metadata; no Executable lookup on the hot path. */
    private final class TypedMethodHook extends MethodHook {
        final ResourceMethod method;

        TypedMethodHook(ResourceMethod method) {
            this.method = method;
        }

        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            // Skip the common case entirely: no fakes, active cache or replacements.
            SparseArray<Pair<ReplacementType, Object>> currentActive = active.get();
            if (fakes.size() == 0 && unresolved.isEmpty() && currentActive.size() == 0)
                return chain.proceed();

            int resId = (int) chain.getArg(0);

            // Fakes table is keyed by the fake resource id.
            int modResId = fakes.get(resId);
            if (modResId != 0) {
                Context mContext = ModuleHelper.findContext();
                if (mContext != null) {
                    Object value = getFakeResource(mContext, method, modResId, chain);
                    if (value != null) return value;
                }
            }

            // Active replacements are checked before Context lookup.
            if (unresolved.isEmpty() && currentActive.size() == 0) return chain.proceed();

            Object value = getResourceReplacement(resId, (Resources) chain.getThisObject(), method, chain);
            if (value == null) return chain.proceed();

            if (method == ResourceMethod.DIMENSION_PIXEL_OFFSET
                || method == ResourceMethod.DIMENSION_PIXEL_SIZE) {
                if (value instanceof Float) value = ((Float) value).intValue();
            }
            return value;
        }
    }

    /**
     * Install all method hooks. The state machine guarantees:
     * <ul>
     *   <li>no method is double-hooked;</li>
     *   <li>a partial failure can be retried on the next call;</li>
     *   <li>parallel callers block cleanly on the state machine.</li>
     * </ul>
     */
    private void applyHooks() {
        InstallState current;
        while (true) {
            current = installState.get();
            if (current == InstallState.INSTALLED || current == InstallState.INSTALLING) return;
            if (installState.compareAndSet(current, InstallState.INSTALLING)) break;
        }

        boolean allOk = true;
        try {
            for (ResourceMethod method : ResourceMethod.values()) {
                if (!installedMethods.add(method.name)) continue;
                Class<?>[] paramTypes = getParamTypes(method);
                try {
                    ModuleHelper.findAndHookMethod(Resources.class, method.name, paramTypes, new TypedMethodHook(method));
                } catch (Throwable t) {
                    try { XposedHelpers.log(t); } catch (Throwable ignored) {}
                    installedMethods.remove(method.name);
                    allOk = false;
                }
            }
        } catch (Throwable t) {
            try { XposedHelpers.log(t); } catch (Throwable ignored) {}
            allOk = false;
        } finally {
            installState.set(allOk ? InstallState.INSTALLED : InstallState.FAILED_PARTIAL);
        }
    }

    private static Class<?>[] getParamTypes(ResourceMethod method) {
        switch (method) {
            case FRACTION:
                return new Class<?>[] { int.class, int.class, int.class };
            case DRAWABLE_FOR_DENSITY:
                return new Class<?>[] { int.class, int.class, Resources.Theme.class };
            default:
                return new Class<?>[] { int.class };
        }
    }

    public int addResource(String resName, int resId) {
        try {
            applyHooks();
            int fakeResId = getFakeResId(resName);
            fakes.put(fakeResId, resId);
            return fakeResId;
        } catch (Throwable t) {
            XposedHelpers.log(t);
            return 0;
        }
    }

    private Object getFakeResource(
        Context context,
        ResourceMethod method,
        int modResId,
        XposedInterface.Chain chain
    ) {
        try {
            if (context == null || fakes.size() == 0) return null;
            Resources modRes = ModuleHelper.getModuleRes(context);
            return callModuleResource(modRes, method, modResId, chain);
        } catch (Throwable t) {
            XposedHelpers.log(t);
            return null;
        }
    }

    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            unresolved.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.ID, replacementResId));
            active.set(new SparseArray<Pair<ReplacementType, Object>>());
        } catch (Throwable t) {
            XposedHelpers.log(t);
        }
    }

    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            unresolved.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.DENSITY, replacementResValue));
            active.set(new SparseArray<Pair<ReplacementType, Object>>());
        } catch (Throwable t) {
            XposedHelpers.log(t);
        }
    }

    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            unresolved.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.OBJECT, replacementResValue));
            active.set(new SparseArray<Pair<ReplacementType, Object>>());
        } catch (Throwable t) {
            XposedHelpers.log(t);
        }
    }

    private Object getResourceReplacement(
        int resId,
        Resources res,
        ResourceMethod method,
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
            } catch (Throwable ignore) {}
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
            return callModuleResource(modRes, method, modResId, chain);
        } catch (Throwable t) {
            XposedHelpers.log(t);
            return null;
        }
    }

    private static Object callModuleResource(
        Resources modRes,
        ResourceMethod method,
        int modResId,
        XposedInterface.Chain chain
    ) {
        try {
            switch (method.extraArgs) {
                case 0:
                    return XposedHelpers.callMethod(modRes, method.name, modResId);
                case 1:
                    return XposedHelpers.callMethod(modRes, method.name, modResId, chain.getArg(1));
                case 2:
                    return XposedHelpers.callMethod(modRes, method.name, modResId, chain.getArg(1), chain.getArg(2));
                default:
                    return null;
            }
        } catch (Throwable t) {
            XposedHelpers.log(t);
            return null;
        }
    }
}
