package tv.withaibuild.customiuizer.mods.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.withaibuild.customiuizer.utils.FakeXposedInterface;

/**
 * B2A-D3: ResourceHooks unified non-fatal boundary must reuse
 * {@link RuntimeFatality#throwIfFatal(Throwable)} and production catch paths
 * must go through that boundary.
 */
public class ResourceHooksFatalityTest {

    private Method logNonFatal;

    @Before
    public void setUp() throws Exception {
        FakeXposedInterface.reset();
        XposedHelpers.moduleInst = FakeXposedInterface.INSTANCE.create();
        logNonFatal = ResourceHooks.class.getDeclaredMethod("logNonFatal", Throwable.class);
        logNonFatal.setAccessible(true);
    }

    @After
    public void tearDown() {
        FakeXposedInterface.reset();
        XposedHelpers.moduleInst = null;
    }

    @Test
    public void logNonFatal_directOutOfMemoryError_propagates() {
        OutOfMemoryError failure = new OutOfMemoryError("resource oom");
        assertSame(failure, invokeFatal(failure));
    }

    @Test
    public void logNonFatal_directThreadDeath_propagates() {
        ThreadDeath failure = new ThreadDeath();
        assertSame(failure, invokeFatal(failure));
    }

    @Test
    public void logNonFatal_directInternalError_propagates() {
        InternalError failure = new InternalError("resource vm");
        assertSame(failure, invokeFatal(failure));
    }

    @Test
    public void logNonFatal_wrappedOutOfMemoryError_propagatesOriginal() {
        OutOfMemoryError failure = new OutOfMemoryError("wrapped resource oom");
        assertSame(failure, invokeFatal(new RuntimeException(failure)));
    }

    @Test
    public void logNonFatal_ordinaryRuntimeException_failOpen() throws Throwable {
        invokeLogNonFatal(new RuntimeException("ordinary resource failure"));
    }

    @Test
    public void addResource_ordinaryPath_failOpenDoesNotThrow() {
        ResourceHooks hooks = new ResourceHooks();
        hooks.addResource("b2a_d3_probe", 0x7f010001);
    }

    @Test
    public void productionCatchThrowableBlocks_useCanonicalFatalBoundary() throws IOException {
        String source = readResourceHooksSource();
        List<String> catches = extractCatchThrowableBodies(source);
        assertTrue(
            "ResourceHooks must contain the known catch(Throwable) sites",
            catches.size() >= 11
        );

        String logNonFatalSource = extractMethod(source, "private static void logNonFatal");
        assertTrue(
            "logNonFatal must call RuntimeFatality.throwIfFatal first",
            logNonFatalSource.contains("RuntimeFatality.throwIfFatal(t)")
        );
        assertTrue(
            "logNonFatal logging catch must also call RuntimeFatality.throwIfFatal",
            logNonFatalSource.contains("RuntimeFatality.throwIfFatal(ex)")
        );
        assertFalse(
            "ResourceHooks must not add a local isFatal helper",
            source.contains("boolean isFatal") || source.contains("private static boolean isFatal")
        );

        for (String body : catches) {
            boolean usesCanonical =
                body.contains("RuntimeFatality.throwIfFatal(") || body.contains("logNonFatal(");
            assertTrue(
                "catch(Throwable) must go through RuntimeFatality.throwIfFatal or logNonFatal: " + body,
                usesCanonical
            );
            assertFalse(
                "catch(Throwable) must not special-case only OutOfMemoryError",
                body.contains("instanceof OutOfMemoryError") && !body.contains("RuntimeFatality")
            );
        }
    }

    private Throwable invokeFatal(Throwable input) {
        try {
            invokeLogNonFatal(input);
            fail("expected fatal throwable");
            throw new AssertionError("unreachable");
        } catch (OutOfMemoryError oom) {
            return oom;
        } catch (ThreadDeath td) {
            return td;
        } catch (VirtualMachineError vm) {
            return vm;
        } catch (Throwable unexpected) {
            fail("unexpected throwable: " + unexpected);
            throw new AssertionError("unreachable");
        }
    }

    private void invokeLogNonFatal(Throwable input) throws Throwable {
        try {
            logNonFatal.invoke(null, input);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static String readResourceHooksSource() throws IOException {
        Path path = Paths.get("src/main/java/tv/withaibuild/customiuizer/mods/utils/ResourceHooks.java");
        if (!Files.isRegularFile(path)) {
            fail("missing source file: " + path.toAbsolutePath());
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static List<String> extractCatchThrowableBodies(String source) {
        Pattern pattern = Pattern.compile("catch \\(Throwable (t|ex)\\)");
        Matcher matcher = pattern.matcher(source);
        List<String> bodies = new ArrayList<>();
        while (matcher.find()) {
            int brace = source.indexOf('{', matcher.end());
            if (brace < 0) {
                fail("unterminated catch(Throwable) in ResourceHooks.java");
            }
            bodies.add(extractBrace(source, brace));
        }
        return bodies;
    }

    private static String extractBrace(String source, int brace) {
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(brace, i + 1);
                }
            }
        }
        fail("unterminated catch brace in ResourceHooks.java");
        return "";
    }

    private static String extractMethod(String source, String signaturePrefix) {
        int start = source.indexOf(signaturePrefix);
        if (start < 0) {
            fail("missing " + signaturePrefix);
        }
        int brace = source.indexOf('{', start);
        return extractBrace(source, brace);
    }
}
