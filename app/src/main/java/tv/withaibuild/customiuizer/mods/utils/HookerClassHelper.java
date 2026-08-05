package tv.withaibuild.customiuizer.mods.utils;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * API 101 adapter for the module's existing before/after callbacks.
 *
 * <p>The old MIUI 14 hooks rely on mutable argument arrays, early returns and
 * after-callback result replacement. Keeping that behavior in one adapter lets
 * each feature move to native API 101 {@link XposedInterface.Chain} hooks in
 * small, testable steps.</p>
 */
public final class HookerClassHelper {
    private HookerClassHelper() {
    }

    interface BeforeMethodCallback {
        void beforeHook(BeforeHookCallback callback);
    }

    interface AfterMethodCallback {
        void afterHook(AfterHookCallback callback);
    }

    public static final class BeforeHookCallback {
        private static final Object[] EMPTY_ARGS = new Object[0];

        private final Member member;
        private final Object thisObject;
        private final List<Object> argList;
        private Object[] args;
        private boolean skipped;
        private Object result;
        private Throwable throwable;

        BeforeHookCallback(XposedInterface.Chain chain) {
            member = chain.getExecutable();
            thisObject = chain.getThisObject();
            argList = chain.getArgs();
        }

        public Member getMember() {
            return member;
        }

        public Object getThisObject() {
            return thisObject;
        }

        /**
         * Materializes the full argument array. Prefer {@link #getArg(int)} for read-only hooks.
         */
        public Object[] getArgs() {
            if (args == null) {
                args = argList.isEmpty() ? EMPTY_ARGS : argList.toArray();
            }
            return args;
        }

        /**
         * Reads a single argument without copying the argument list.
         *
         * <p>Calling {@link #getArgs()} converts the argument list to an array, and
         * {@code intercept} must then pass that array to {@code chain.proceed(args)}, which
         * re-marshals every argument. Use this method in hooks that only read arguments.</p>
         *
         * @param index the zero-based argument index
         * @return the argument value
         */
        public Object getArg(int index) {
            return args != null ? args[index] : argList.get(index);
        }

        /**
         * Returns the number of arguments without materializing the argument array.
         */
        public int getArgsCount() {
            return args != null ? args.length : argList.size();
        }

        /**
         * Returns whether {@link #getArgs()} has been called and an argument array exists.
         */
        boolean hasMaterializedArgs() {
            return args != null;
        }

        public void returnAndSkip(Object returnValue) {
            skipped = true;
            result = returnValue;
            throwable = null;
        }

        public void throwAndSkip(Throwable throwable) {
            skipped = true;
            result = null;
            this.throwable = throwable;
        }
    }

    public static final class AfterHookCallback {
        private final Member member;
        private final Object thisObject;
        private final BeforeHookCallback before;
        private final boolean skipped;
        private Object result;
        private Throwable throwable;

        AfterHookCallback(BeforeHookCallback before, Object result, Throwable throwable) {
            member = before.member;
            thisObject = before.thisObject;
            this.before = before;
            skipped = before.skipped;
            this.result = result;
            this.throwable = throwable;
        }

        public Member getMember() {
            return member;
        }

        public Object getThisObject() {
            return thisObject;
        }

        public Object[] getArgs() {
            return before.getArgs();
        }

        /**
         * Reads a single argument without copying the argument list.
         */
        public Object getArg(int index) {
            return before.getArg(index);
        }

        /**
         * Returns the number of arguments without materializing the argument array.
         */
        public int getArgsCount() {
            return before.getArgsCount();
        }

        public Object getResult() {
            return result;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public void setResult(Object result) {
            this.result = result;
            throwable = null;
        }

        public void setThrowable(Throwable throwable) {
            result = null;
            this.throwable = throwable;
        }
    }

    /**
     * Compatibility hook implemented directly on API 101's interceptor model.
     */
    public static class MethodHook implements BeforeMethodCallback, AfterMethodCallback, XposedInterface.Hooker {
        public final int mPriority;
        boolean mIsReturnConstant;
        Object mReturnConstantValue;
        private final boolean hasAfter;

        public MethodHook() {
            this(XposedInterface.PRIORITY_DEFAULT);
        }

        public MethodHook(int priority) {
            mPriority = priority;
            hasAfter = declaresAfterCallback(getClass());
        }

        private static boolean declaresAfterCallback(Class<?> hookClass) {
            Class<?> current = hookClass;
            while (current != null && current != MethodHook.class) {
                for (Method method : current.getDeclaredMethods()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (method.getReturnType() == Void.TYPE
                        && parameterTypes.length == 1
                        && parameterTypes[0] == AfterHookCallback.class) {
                        return true;
                    }
                }
                current = current.getSuperclass();
            }
            return false;
        }

        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            if (mIsReturnConstant) {
                return mReturnConstantValue;
            }

            BeforeHookCallback before = new BeforeHookCallback(chain);
            beforeHook(before);

            Object result = before.result;
            Throwable throwable = before.throwable;
            if (!before.skipped) {
                try {
                    result = before.hasMaterializedArgs() ? chain.proceed(before.getArgs()) : chain.proceed();
                } catch (Throwable t) {
                    throwable = t;
                }
            }

            throwIfFatal(throwable);

            if (hasAfter) {
                AfterHookCallback after = new AfterHookCallback(before, result, throwable);
                afterHook(after);
                if (after.throwable != null) {
                    throw after.throwable;
                }
                return after.result;
            }

            if (throwable != null) {
                throw throwable;
            }
            return result;
        }

        public final void beforeHook(BeforeHookCallback callback) {
            try {
                before(callback);
            } catch (Throwable t) {
                throwIfFatal(t);
                XposedHelpers.log(t);
            }
        }

        public final void afterHook(AfterHookCallback callback) {
            try {
                after(callback);
            } catch (Throwable t) {
                throwIfFatal(t);
                XposedHelpers.log(t);
            }
        }

        protected void before(BeforeHookCallback callback) throws Throwable {
        }

        protected void after(AfterHookCallback callback) throws Throwable {
        }

        private static void throwIfFatal(Throwable throwable) {
            Throwable current = throwable;
            for (int depth = 0; current != null && depth < 8; depth++) {
                if (current instanceof OutOfMemoryError) {
                    throw (OutOfMemoryError) current;
                }
                if (current instanceof ThreadDeath) {
                    throw (ThreadDeath) current;
                }
                if (current instanceof VirtualMachineError) {
                    throw (VirtualMachineError) current;
                }

                Throwable next = current.getCause();
                if (next == current) return;
                current = next;
            }
        }
    }

    public interface CustomMethodUnhooker {
        void unhook();
    }

    /** Skips the hooked method and returns {@code null}. */
    public static final MethodHook DO_NOTHING = new MethodHook(XposedInterface.PRIORITY_HIGHEST) {{
        mIsReturnConstant = true;
        mReturnConstantValue = null;
    }};

    /** Creates a highest-priority callback which always returns the supplied value. */
    public static MethodHook returnConstant(final Object result) {
        return returnConstant(XposedInterface.PRIORITY_HIGHEST, result);
    }

    /** Creates a callback which always returns the supplied value at the requested priority. */
    public static MethodHook returnConstant(int priority, final Object result) {
        return new MethodHook(priority) {{
            mIsReturnConstant = true;
            mReturnConstantValue = result;
        }};
    }
}
