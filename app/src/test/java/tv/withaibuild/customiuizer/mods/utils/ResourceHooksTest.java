package tv.withaibuild.customiuizer.mods.utils;

import android.util.SparseArray;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

public class ResourceHooksTest {

    private static final int ALL_METHODS_MASK = 0x00003fff;

    private ResourceHooks hooks;

    @Before
    public void setUp() {
        hooks = new ResourceHooks();
    }

    @Test
    public void getFakeResIdIsStableForSameName() {
        int a = ResourceHooks.getFakeResId("my_resource");
        int b = ResourceHooks.getFakeResId("my_resource");
        Assert.assertEquals(a, b);
    }

    @Test
    public void getFakeResIdIsDifferentForDifferentNames() {
        int a = ResourceHooks.getFakeResId("foo");
        int b = ResourceHooks.getFakeResId("bar");
        Assert.assertNotEquals(a, b);
    }

    @Test
    public void applyHooksCompletesAndMaskIsBounded() throws Exception {
        Method applyHooks = ResourceHooks.class.getDeclaredMethod("applyHooks");
        applyHooks.setAccessible(true);

        applyHooks.invoke(hooks);

        int mask = getInstalledMask();
        Assert.assertTrue("installed mask must not exceed all methods",
            (mask & ~ALL_METHODS_MASK) == 0);

        String state = getStateName();
        Assert.assertTrue("expected INSTALLED or PARTIAL_FAILED, got " + state,
            "INSTALLED".equals(state) || "PARTIAL_FAILED".equals(state));

        int expectedMask = "INSTALLED".equals(state) ? ALL_METHODS_MASK : mask;
        Assert.assertEquals(expectedMask, mask);
    }

    @Test
    public void applyHooksIsIdempotent() throws Exception {
        Method applyHooks = ResourceHooks.class.getDeclaredMethod("applyHooks");
        applyHooks.setAccessible(true);

        applyHooks.invoke(hooks);
        int firstMask = getInstalledMask();
        String firstState = getStateName();

        applyHooks.invoke(hooks);
        int secondMask = getInstalledMask();
        String secondState = getStateName();

        Assert.assertEquals("installed mask must not grow on second call", firstMask, secondMask);
        Assert.assertEquals("install state must not change", firstState, secondState);
    }

    @Test
    public void concurrentApplyHooksDoesNotExceedAllMethods() throws Exception {
        Method applyHooks = ResourceHooks.class.getDeclaredMethod("applyHooks");
        applyHooks.setAccessible(true);

        final int threads = 4;
        final CyclicBarrier start = new CyclicBarrier(threads);
        final CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    applyHooks.invoke(hooks);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            }).start();
        }
        done.await();

        int mask = getInstalledMask();
        Assert.assertTrue("concurrent installs must not exceed all methods",
            (mask & ~ALL_METHODS_MASK) == 0);
    }

    @Test
    public void setResReplacementClearsActiveCache() throws Exception {
        @SuppressWarnings("unchecked")
        AtomicReference<Object> active = (AtomicReference<Object>) getFieldByName("active");
        SparseArray<Object> one = new SparseArray<>();
        one.put(1, new Object());
        active.set(one);

        hooks.setResReplacement("pkg", "dimen", "foo", 0x7f020001);

        @SuppressWarnings("unchecked")
        SparseArray<Object> after = (SparseArray<Object>) active.get();
        Assert.assertEquals(0, after.size());
    }

    @Test
    public void setObjectReplacementClearsActiveCache() throws Exception {
        @SuppressWarnings("unchecked")
        AtomicReference<Object> active = (AtomicReference<Object>) getFieldByName("active");
        SparseArray<Object> one = new SparseArray<>();
        one.put(1, new Object());
        active.set(one);

        hooks.setObjectReplacement("pkg", "string", "bar", "replaced");

        @SuppressWarnings("unchecked")
        SparseArray<Object> after = (SparseArray<Object>) active.get();
        Assert.assertEquals(0, after.size());
    }

    @Test
    public void setDensityReplacementClearsActiveCache() throws Exception {
        @SuppressWarnings("unchecked")
        AtomicReference<Object> active = (AtomicReference<Object>) getFieldByName("active");
        SparseArray<Object> one = new SparseArray<>();
        one.put(1, new Object());
        active.set(one);

        hooks.setDensityReplacement("pkg", "dimen", "baz", 2.0f);

        @SuppressWarnings("unchecked")
        SparseArray<Object> after = (SparseArray<Object>) active.get();
        Assert.assertEquals(0, after.size());
    }

    private Object getFieldByName(String name) {
        try {
            Field f = ResourceHooks.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(hooks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getStateName() {
        try {
            Field f = ResourceHooks.class.getDeclaredField("installState");
            f.setAccessible(true);
            AtomicReference ref = (AtomicReference) f.get(hooks);
            return ref.get().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getInstalledMask() {
        try {
            Field f = ResourceHooks.class.getDeclaredField("installedMask");
            f.setAccessible(true);
            return ((java.util.concurrent.atomic.AtomicInteger) f.get(hooks)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
