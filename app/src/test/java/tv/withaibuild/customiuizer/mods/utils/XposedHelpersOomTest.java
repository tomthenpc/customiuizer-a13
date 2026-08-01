package tv.withaibuild.customiuizer.mods.utils;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class XposedHelpersOomTest {
    public static class ThrowingTarget {
        static OutOfMemoryError failure;

        public ThrowingTarget() {
            throw failure;
        }

        public ThrowingTarget(boolean safe) {}

        public void instanceCall() {
            throw failure;
        }

        public static void staticCall() {
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
}
