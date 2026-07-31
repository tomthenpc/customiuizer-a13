package tv.withaibuild.customiuizer.mods.utils;

import android.util.SparseArray;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class ResourceHooksTest {

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
    public void setResReplacementClearsActiveCache() throws Exception {
        // Put something in active by reflection; then a set call must invalidate it.
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
}
