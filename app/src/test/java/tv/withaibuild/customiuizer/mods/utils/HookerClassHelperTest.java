package tv.withaibuild.customiuizer.mods.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.github.libxposed.api.XposedInterface;

public class HookerClassHelperTest {
    private static final Method EXECUTABLE;

    static {
        try {
            EXECUTABLE = HookerClassHelperTest.class.getDeclaredMethod("target");
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static void target() {}

    private static final class FakeChain implements XposedInterface.Chain {
        Object result = "host-result";
        Throwable failure;
        boolean proceeded;

        @Override public Executable getExecutable() { return EXECUTABLE; }
        @Override public Object getThisObject() { return this; }
        @Override public List<Object> getArgs() { return Collections.emptyList(); }
        @Override public Object getArg(int index) { return getArgs().get(index); }

        @Override public Object proceed() throws Throwable {
            proceeded = true;
            if (failure != null) throw failure;
            return result;
        }

        @Override public Object proceed(Object[] args) throws Throwable { return proceed(); }
        @Override public Object proceedWith(Object thisObject) throws Throwable { return proceed(); }
        @Override public Object proceedWith(Object thisObject, Object[] args) throws Throwable { return proceed(); }
    }

    @Test
    public void ordinaryBeforeFailureIsIsolated() throws Throwable {
        FakeChain chain = new FakeChain();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback) {
                throw new IllegalStateException("before");
            }
        };

        assertEquals("host-result", hook.intercept(chain));
        assertTrue(chain.proceeded);
    }

    @Test
    public void beforeOutOfMemoryIsRethrownBeforeProceed() {
        FakeChain chain = new FakeChain();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback) {
                throw new OutOfMemoryError("before oom");
            }
        };

        assertThrows(OutOfMemoryError.class, () -> hook.intercept(chain));
        assertFalse(chain.proceeded);
    }

    @Test
    public void ordinaryAfterFailureKeepsHostResult() throws Throwable {
        FakeChain chain = new FakeChain();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                throw new IllegalStateException("after");
            }
        };

        assertEquals("host-result", hook.intercept(chain));
        assertTrue(chain.proceeded);
    }

    @Test
    public void afterOutOfMemoryIsRethrown() {
        FakeChain chain = new FakeChain();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                throw new OutOfMemoryError("after oom");
            }
        };

        assertThrows(OutOfMemoryError.class, () -> hook.intercept(chain));
        assertTrue(chain.proceeded);
    }

    @Test
    public void hostOutOfMemorySkipsAfterAndIsRethrown() {
        FakeChain chain = new FakeChain();
        chain.failure = new OutOfMemoryError("host oom");
        final boolean[] afterCalled = {false};
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                afterCalled[0] = true;
                callback.setResult("masked");
            }
        };

        assertThrows(OutOfMemoryError.class, () -> hook.intercept(chain));
        assertFalse(afterCalled[0]);
    }
}
