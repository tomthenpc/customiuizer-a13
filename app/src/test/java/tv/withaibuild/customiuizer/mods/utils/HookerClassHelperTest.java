package tv.withaibuild.customiuizer.mods.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
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
    public void wrappedBeforeOutOfMemoryIsUnwrappedBeforeProceed() {
        FakeChain chain = new FakeChain();
        OutOfMemoryError failure = new OutOfMemoryError("wrapped before oom");
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback)
                throws Throwable {
                throw new InvocationTargetException(failure);
            }
        };

        OutOfMemoryError thrown = assertThrows(OutOfMemoryError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(chain.proceeded);
    }

    @Test
    public void beforeThreadDeathIsRethrownBeforeProceed() {
        FakeChain chain = new FakeChain();
        ThreadDeath failure = new ThreadDeath();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback) {
                throw failure;
            }
        };

        ThreadDeath thrown = assertThrows(ThreadDeath.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(chain.proceeded);
    }

    @Test
    public void wrappedBeforeThreadDeathIsUnwrappedBeforeProceed() {
        FakeChain chain = new FakeChain();
        ThreadDeath failure = new ThreadDeath();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback)
                throws Throwable {
                throw new InvocationTargetException(failure);
            }
        };

        ThreadDeath thrown = assertThrows(ThreadDeath.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(chain.proceeded);
    }

    @Test
    public void beforeInternalErrorIsRethrownBeforeProceed() {
        FakeChain chain = new FakeChain();
        InternalError failure = new InternalError("before internal");
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback) {
                throw failure;
            }
        };

        InternalError thrown = assertThrows(InternalError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(chain.proceeded);
    }

    @Test
    public void wrappedBeforeInternalErrorIsUnwrappedBeforeProceed() {
        FakeChain chain = new FakeChain();
        InternalError failure = new InternalError("wrapped before internal");
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback)
                throws Throwable {
                throw new InvocationTargetException(failure);
            }
        };

        InternalError thrown = assertThrows(InternalError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(chain.proceeded);
    }

    @Test
    public void wrappedOrdinaryBeforeFailureRemainsIsolated() throws Throwable {
        FakeChain chain = new FakeChain();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback)
                throws Throwable {
                throw new InvocationTargetException(new IllegalStateException("ordinary"));
            }
        };

        assertEquals("host-result", hook.intercept(chain));
        assertTrue(chain.proceeded);
    }

    @Test
    public void assertionErrorBeforeRemainsIsolated() throws Throwable {
        FakeChain chain = new FakeChain();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void before(HookerClassHelper.BeforeHookCallback callback) {
                throw new AssertionError("before assertion");
            }
        };

        assertEquals("host-result", hook.intercept(chain));
        assertTrue(chain.proceeded);
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
    public void afterThreadDeathIsRethrown() {
        FakeChain chain = new FakeChain();
        ThreadDeath failure = new ThreadDeath();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                throw failure;
            }
        };

        ThreadDeath thrown = assertThrows(ThreadDeath.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertTrue(chain.proceeded);
    }

    @Test
    public void wrappedAfterThreadDeathIsRethrown() {
        FakeChain chain = new FakeChain();
        ThreadDeath failure = new ThreadDeath();
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback)
                throws Throwable {
                throw new InvocationTargetException(failure);
            }
        };

        ThreadDeath thrown = assertThrows(ThreadDeath.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertTrue(chain.proceeded);
    }

    @Test
    public void afterInternalErrorIsRethrown() {
        FakeChain chain = new FakeChain();
        InternalError failure = new InternalError("after internal");
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                throw failure;
            }
        };

        InternalError thrown = assertThrows(InternalError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertTrue(chain.proceeded);
    }

    @Test
    public void wrappedAfterInternalErrorIsRethrown() {
        FakeChain chain = new FakeChain();
        InternalError failure = new InternalError("wrapped after internal");
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback)
                throws Throwable {
                throw new InvocationTargetException(failure);
            }
        };

        InternalError thrown = assertThrows(InternalError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
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

    @Test
    public void hostThreadDeathSkipsAfterAndIsRethrown() {
        FakeChain chain = new FakeChain();
        ThreadDeath failure = new ThreadDeath();
        chain.failure = failure;
        final boolean[] afterCalled = {false};
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                afterCalled[0] = true;
                callback.setResult("masked");
            }
        };

        ThreadDeath thrown = assertThrows(ThreadDeath.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(afterCalled[0]);
    }

    @Test
    public void wrappedHostThreadDeathSkipsAfterAndIsRethrown() {
        FakeChain chain = new FakeChain();
        ThreadDeath failure = new ThreadDeath();
        chain.failure = new InvocationTargetException(failure);
        final boolean[] afterCalled = {false};
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                afterCalled[0] = true;
                callback.setResult("masked");
            }
        };

        ThreadDeath thrown = assertThrows(ThreadDeath.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(afterCalled[0]);
    }

    @Test
    public void hostInternalErrorSkipsAfterAndIsRethrown() {
        FakeChain chain = new FakeChain();
        InternalError failure = new InternalError("host internal");
        chain.failure = failure;
        final boolean[] afterCalled = {false};
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                afterCalled[0] = true;
                callback.setResult("masked");
            }
        };

        InternalError thrown = assertThrows(InternalError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(afterCalled[0]);
    }

    @Test
    public void wrappedHostInternalErrorSkipsAfterAndIsRethrown() {
        FakeChain chain = new FakeChain();
        InternalError failure = new InternalError("wrapped host internal");
        chain.failure = new InvocationTargetException(failure);
        final boolean[] afterCalled = {false};
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                afterCalled[0] = true;
                callback.setResult("masked");
            }
        };

        InternalError thrown = assertThrows(InternalError.class, () -> hook.intercept(chain));
        assertSame(failure, thrown);
        assertFalse(afterCalled[0]);
    }

    @Test
    public void ordinaryHostThrowableStillRunsAfter() throws Throwable {
        FakeChain chain = new FakeChain();
        chain.failure = new IllegalStateException("host ordinary");
        final boolean[] afterCalled = {false};
        HookerClassHelper.MethodHook hook = new HookerClassHelper.MethodHook() {
            @Override protected void after(HookerClassHelper.AfterHookCallback callback) {
                afterCalled[0] = true;
                callback.setResult("replaced");
            }
        };

        assertEquals("replaced", hook.intercept(chain));
        assertTrue(afterCalled[0]);
    }
}
