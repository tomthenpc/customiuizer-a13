package tv.withaibuild.customiuizer.mods.utils;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class XposedHelpersFatalityTest {
    public static class ThrowingTarget {
        static Throwable failure;

        public ThrowingTarget() throws Throwable {
            throw failure;
        }

        public ThrowingTarget(boolean safe) {}

        public void instanceCall() throws Throwable {
            throw failure;
        }

        public static void staticCall() throws Throwable {
            throw failure;
        }
    }

    @Test
    public void instanceCallRethrowsOriginalOutOfMemory() {
        OutOfMemoryError failure = new OutOfMemoryError("instance oom");
        ThrowingTarget.failure = failure;
        ThrowingTarget target = new ThrowingTarget(true);

        OutOfMemoryError thrown = assertThrows(
            OutOfMemoryError.class,
            () -> XposedHelpers.callMethod(target, "instanceCall")
        );

        assertSame(failure, thrown);
    }

    @Test
    public void staticCallRethrowsOriginalOutOfMemory() {
        OutOfMemoryError failure = new OutOfMemoryError("static oom");
        ThrowingTarget.failure = failure;

        OutOfMemoryError thrown = assertThrows(
            OutOfMemoryError.class,
            () -> XposedHelpers.callStaticMethod(ThrowingTarget.class, "staticCall")
        );

        assertSame(failure, thrown);
    }

    @Test
    public void constructorRethrowsOriginalOutOfMemory() {
        OutOfMemoryError failure = new OutOfMemoryError("constructor oom");
        ThrowingTarget.failure = failure;

        OutOfMemoryError thrown = assertThrows(
            OutOfMemoryError.class,
            () -> XposedHelpers.newInstance(ThrowingTarget.class)
        );

        assertSame(failure, thrown);
    }

    @Test
    public void instanceCallRethrowsOriginalThreadDeath() {
        ThreadDeath failure = new ThreadDeath();
        ThrowingTarget.failure = failure;
        ThrowingTarget target = new ThrowingTarget(true);

        ThreadDeath thrown = assertThrows(
            ThreadDeath.class,
            () -> XposedHelpers.callMethod(target, "instanceCall")
        );

        assertSame(failure, thrown);
    }

    @Test
    public void staticCallRethrowsOriginalThreadDeath() {
        ThreadDeath failure = new ThreadDeath();
        ThrowingTarget.failure = failure;

        ThreadDeath thrown = assertThrows(
            ThreadDeath.class,
            () -> XposedHelpers.callStaticMethod(ThrowingTarget.class, "staticCall")
        );

        assertSame(failure, thrown);
    }

    @Test
    public void constructorRethrowsOriginalThreadDeath() {
        ThreadDeath failure = new ThreadDeath();
        ThrowingTarget.failure = failure;

        ThreadDeath thrown = assertThrows(
            ThreadDeath.class,
            () -> XposedHelpers.newInstance(ThrowingTarget.class)
        );

        assertSame(failure, thrown);
    }

    @Test
    public void instanceCallRethrowsOriginalVirtualMachineError() {
        InternalError failure = new InternalError("instance vm error");
        ThrowingTarget.failure = failure;
        ThrowingTarget target = new ThrowingTarget(true);

        InternalError thrown = assertThrows(
            InternalError.class,
            () -> XposedHelpers.callMethod(target, "instanceCall")
        );

        assertSame(failure, thrown);
    }

    @Test
    public void staticCallRethrowsOriginalVirtualMachineError() {
        InternalError failure = new InternalError("static vm error");
        ThrowingTarget.failure = failure;

        InternalError thrown = assertThrows(
            InternalError.class,
            () -> XposedHelpers.callStaticMethod(ThrowingTarget.class, "staticCall")
        );

        assertSame(failure, thrown);
    }

    @Test
    public void constructorRethrowsOriginalVirtualMachineError() {
        InternalError failure = new InternalError("constructor vm error");
        ThrowingTarget.failure = failure;

        InternalError thrown = assertThrows(
            InternalError.class,
            () -> XposedHelpers.newInstance(ThrowingTarget.class)
        );

        assertSame(failure, thrown);
    }

    @Test
    public void instanceCallWrapsOrdinaryException() {
        IllegalStateException failure = new IllegalStateException("instance ordinary");
        ThrowingTarget.failure = failure;
        ThrowingTarget target = new ThrowingTarget(true);

        XposedHelpers.InvocationTargetError thrown = assertThrows(
            XposedHelpers.InvocationTargetError.class,
            () -> XposedHelpers.callMethod(target, "instanceCall")
        );

        assertSame(failure, thrown.getCause());
    }

    @Test
    public void staticCallWrapsOrdinaryException() {
        IllegalStateException failure = new IllegalStateException("static ordinary");
        ThrowingTarget.failure = failure;

        XposedHelpers.InvocationTargetError thrown = assertThrows(
            XposedHelpers.InvocationTargetError.class,
            () -> XposedHelpers.callStaticMethod(ThrowingTarget.class, "staticCall")
        );

        assertSame(failure, thrown.getCause());
    }

    @Test
    public void constructorWrapsOrdinaryException() {
        IllegalStateException failure = new IllegalStateException("constructor ordinary");
        ThrowingTarget.failure = failure;

        XposedHelpers.InvocationTargetError thrown = assertThrows(
            XposedHelpers.InvocationTargetError.class,
            () -> XposedHelpers.newInstance(ThrowingTarget.class)
        );

        assertSame(failure, thrown.getCause());
    }
}
