package tv.withaibuild.customiuizer.mods.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface;
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook;

public class ResourceHooks {
	private boolean hooksApplied = false;

	public enum ReplacementType {
		ID,
		DENSITY,
		OBJECT
	}

	private final SparseIntArray fakes = new SparseIntArray();
	private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> unresolved = new ConcurrentHashMap<>();
	private final AtomicReference<SparseArray<Pair<ReplacementType, Object>>> active =
		new AtomicReference<>(new SparseArray<Pair<ReplacementType, Object>>());

	private static final int MAX_ACTIVE = 256;

	public static int getFakeResId(String resourceName) {
		return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
	}

	@SuppressWarnings("FieldCanBeLocal")
	private final MethodHook mReplaceHook = new MethodHook() {
		@Override
		public Object intercept(XposedInterface.Chain chain) throws Throwable {
			// Skip the common case entirely: no fakes or replacements means we do not
			// need to read arguments, resolve context, or look up resource names.
			if (fakes.size() == 0 && unresolved.isEmpty()) return chain.proceed();

			int resId = (int) chain.getArg(0);

			// Fakes table is keyed by the fake resource id, so we can test for a hit
			// without invoking findContext() or the costly executable name JNI call.
			int modResId = fakes.get(resId);
			if (modResId != 0) {
				Context mContext = ModuleHelper.findContext();
				if (mContext != null) {
					String method = chain.getExecutable().getName();
					Object value = getFakeResource(mContext, method, chain);
					if (value != null) return value;
				}
			}

			// Avoid all findContext/name-resolution work when no replacements are registered.
			if (unresolved.isEmpty()) return chain.proceed();

			Context mContext = ModuleHelper.findContext();
			if (mContext == null) return chain.proceed();

			String method = chain.getExecutable().getName();
			Object value = getResourceReplacement(mContext, (Resources) chain.getThisObject(), method, chain);
			if (value == null) return chain.proceed();
			if ("getDimensionPixelOffset".equals(method) || "getDimensionPixelSize".equals(method)) {
				if (value instanceof Float) value = ((Float) value).intValue();
			}
			return value;
		}
	};

	public ResourceHooks() {}

	private void applyHooks() {
		if (hooksApplied) return;
		hooksApplied = true;
		ModuleHelper.findAndHookMethod(Resources.class, "getInteger", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getLayout", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getFraction", int.class, int.class, int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getBoolean", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getDimension", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getDimensionPixelOffset", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getDimensionPixelSize", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getText", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getString", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getDrawableForDensity", int.class, int.class, Resources.Theme.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getIntArray", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getStringArray", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getTextArray", int.class, mReplaceHook);
		ModuleHelper.findAndHookMethod(Resources.class, "getAnimation", int.class, mReplaceHook);
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

	private Object getFakeResource(Context context, String method, XposedInterface.Chain chain) {
		try {
			if (context == null || fakes.size() == 0) return null;
			int resId = (int) chain.getArg(0);
			int modResId = fakes.get(resId);
			if (modResId == 0) return null;

			Resources modRes = ModuleHelper.getModuleRes(context);
			if ("getDrawable".equals(method))
				return XposedHelpers.callMethod(modRes, method, modResId, chain.getArg(1));
			else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
				return XposedHelpers.callMethod(modRes, method, modResId, chain.getArg(1), chain.getArg(2));
			else
				return XposedHelpers.callMethod(modRes, method, modResId);
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

	private Object getResourceReplacement(Context context, Resources res, String method, XposedInterface.Chain chain) {
		if (context == null || unresolved.isEmpty()) return null;

		int resId = (int) chain.getArg(0);
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

			Resources modRes = ModuleHelper.getModuleRes(context);
			if ("getDrawable".equals(method))
				return XposedHelpers.callMethod(modRes, method, modResId, chain.getArg(1));
			else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
				return XposedHelpers.callMethod(modRes, method, modResId, chain.getArg(1), chain.getArg(2));
			else
				return XposedHelpers.callMethod(modRes, method, modResId);
		} catch (Throwable t) {
			XposedHelpers.log(t);
			return null;
		}
	}

}
