package tv.withaibuild.customiuizer;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class MainModuleResourceHooksTest {
    @Test
    public void resourceHooksHolderReturnsOneProcessInstance() {
        assertSame(MainModule.getResHooks(), MainModule.getResHooks());
    }

    @Test
    public void eagerResourceHooksFieldIsRemoved() {
        assertThrows(
            NoSuchFieldException.class,
            () -> MainModule.class.getDeclaredField("resHooks")
        );
    }
}
