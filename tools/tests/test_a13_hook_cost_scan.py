import json
import shutil
import sys
import tempfile
import unittest
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import a13_hook_cost_scan as scan


class HookCostScanTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="a13_hook_cost_scan_test_"))

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _write(self, rel: str, content: str, linesep: str = "\n") -> Path:
        path = self.tmp / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content.replace("\n", linesep).encode("utf-8"))
        return path

    def _scan(self) -> list[scan.HookCostRecord]:
        return scan.HookCostScanner(self.tmp).scan()

    def test_java_hook_call_is_recognized(self):
        self._write(
            "JavaHook.java",
            '''
package test;
import io.github.libxposed.api.XposedHelpers;
public class JavaHook {
    public static void install(ClassLoader cl) {
        XposedHelpers.findAndHookMethod("com.android.Foo", cl, "bar", new MethodHook() { });
        XposedHelpers.findAndHookConstructor("com.android.Foo", cl, String.class, new MethodHook() { });
    }
}
''',
        )
        records = self._scan()
        types = Counter(r.hook_type for r in records)
        self.assertEqual(types["METHOD_HOOK"], 1)
        self.assertEqual(types["CONSTRUCTOR_HOOK"], 1)
        rec = next(r for r in records if r.hook_type == "METHOD_HOOK")
        self.assertEqual(rec.target_class, "com.android.Foo")
        self.assertEqual(rec.target_method, "bar")

    def test_kotlin_hook_call_and_all_methods(self):
        self._write(
            "KotlinHook.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object KotlinHook {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.Foo", cl, "bar", object : MethodHook() { })
        ModuleHelper.hookAllMethods("com.android.Foo", "allMethods", object : MethodHook() { })
    }
}
''',
        )
        records = self._scan()
        types = Counter(r.hook_type for r in records)
        self.assertEqual(types["METHOD_HOOK"], 1)
        self.assertEqual(types["ALL_METHODS"], 1)

    def test_all_constructors(self):
        self._write(
            "KotlinConstructor.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object KotlinConstructor {
    fun install(cl: ClassLoader) {
        ModuleHelper.hookAllConstructors("com.android.Foo", object : MethodHook() { })
    }
}
''',
        )
        records = self._scan()
        self.assertEqual(len(records), 1)
        self.assertEqual(records[0].hook_type, "ALL_CONSTRUCTORS")

    def test_multi_target_loop_is_counted(self):
        self._write(
            "MultiTargetLoop.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object MultiTargetLoop {
    fun install(cl: ClassLoader) {
        for (name in listOf("alpha", "beta", "gamma")) {
            ModuleHelper.findAndHookMethod("com.android.Shared", cl, name, object : MethodHook() { })
        }
    }
}
''',
        )
        records = self._scan()
        # Scanner counts source call sites, not loop iterations.
        self.assertEqual(len(records), 1)
        self.assertEqual(records[0].target_class, "com.android.Shared")
        # The method name is a loop variable and cannot be resolved statically.
        self.assertEqual(records[0].target_method, "unknown")
        self.assertIn("name", records[0].notes)

    def test_comments_and_strings_do_not_count(self):
        self._write(
            "ConditionalAndComments.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object ConditionalAndComments {
    fun install(cl: ClassLoader, enabled: Boolean) {
        // ModuleHelper.findAndHookMethod("com.android.NotUsed", cl, "notUsed", object : MethodHook() { })
        val pseudo = "ModuleHelper.findAndHookMethod(\"com.android.NotUsed\", cl, \"notUsed\", object : MethodHook() { })"
        if (enabled) {
            ModuleHelper.findAndHookMethod("com.android.Conditional", cl, "run", object : MethodHook() { })
        }
    }
}
''',
        )
        records = self._scan()
        self.assertEqual(len(records), 1)
        self.assertEqual(records[0].target_class, "com.android.Conditional")
        self.assertEqual(records[0].target_method, "run")

    def test_dynamic_class_name_is_marked(self):
        self._write(
            "DynamicClassName.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object DynamicClassName {
    fun install(cl: ClassLoader, className: String) {
        ModuleHelper.findAndHookMethod(className, cl, "run", object : MethodHook() { })
    }
}
''',
        )
        records = self._scan()
        self.assertEqual(len(records), 1)
        self.assertEqual(records[0].target_method, "run")
        self.assertIn("className", records[0].target_class)

    def test_listeners_and_delayed_callbacks(self):
        self._write(
            "ListenerAndObserver.kt",
            '''
package test
import android.content.Context
import android.os.Handler
import android.os.Looper

object ListenerAndObserver {
    private val handler = Handler(Looper.getMainLooper())
    fun start(ctx: Context) {
        ctx.registerReceiver(null, null)
        ctx.contentResolver.registerContentObserver(null, true, null)
        handler.postDelayed({ }, 1000L)
    }
}
''',
        )
        records = self._scan()
        types = Counter(r.hook_type for r in records)
        self.assertEqual(types["BROADCAST_RECEIVER"], 1)
        self.assertEqual(types["CONTENT_OBSERVER"], 1)
        self.assertEqual(types["HANDLER_DELAYED"], 1)

    def test_windows_newline(self):
        self._write(
            "WindowsNewline.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object WindowsNewline {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.Win", cl, "run", object : MethodHook() { })
    }
}
''',
            linesep="\r\n",
        )
        records = self._scan()
        self.assertEqual(len(records), 1)
        self.assertEqual(records[0].target_method, "run")

    def test_duplicate_target_grouping(self):
        self._write(
            "DuplicateA.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object DuplicateA {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.Dup", cl, "same", object : MethodHook() { })
    }
}
''',
        )
        self._write(
            "DuplicateB.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object DuplicateB {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.Dup", cl, "same", object : MethodHook() { })
    }
}
''',
        )
        records = self._scan()
        for r in records:
            self.assertIn("2 sites", r.duplicate_target_group)

    def test_stable_order(self):
        self._write(
            "Zeta.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object Zeta {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.Z", cl, "z", object : MethodHook() { })
    }
}
''',
        )
        self._write(
            "Alpha.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object Alpha {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.A", cl, "a", object : MethodHook() { })
    }
}
''',
        )
        first = [r.source_file for r in self._scan()]
        second = [r.source_file for r in self._scan()]
        self.assertEqual(first, second)
        # File names should be sorted alphabetically.
        self.assertTrue(first[0].endswith("Alpha.kt"))

    def test_json_schema_written(self):
        self._write(
            "Schema.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object Schema {
    fun install(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.S", cl, "s", object : MethodHook() { })
    }
}
''',
        )
        out = self.tmp / "out.json"
        records = self._scan()
        scan._write_cost_map(records, out)
        data = json.loads(out.read_text(encoding="utf-8"))
        self.assertEqual(data["schema_version"], 1)
        self.assertIn("records", data)
        self.assertEqual(data["total_records"], 1)

    def test_unresolved_recorded(self):
        self._write(
            "Unresolved.kt",
            '''
package test
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object Unresolved {
    fun install(cl: ClassLoader, cls: String) {
        ModuleHelper.hookAllMethods(cls, "method", object : MethodHook() { })
    }
}
''',
        )
        records = self._scan()
        self.assertEqual(len(records), 1)
        # Class name is a runtime variable -> unresolved target class.
        self.assertEqual(records[0].target_class, "cls")
        self.assertEqual(records[0].target_method, "method")
        self.assertNotEqual(records[0].confidence, "high")


    def test_method_level_process_mapping(self):
        """A mod class with methods called from different installers must get per-method process scope."""
        self._write(
            "mods/FeatureClass.kt",
            '''
package tv.withaibuild.customiuizer.mods
import tv.withaibuild.customiuizer.mods.utils.ModuleHelper
import tv.withaibuild.customiuizer.mods.utils.HookerClassHelper.MethodHook

object FeatureClass {
    fun serverHook(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.Server", cl, "run", object : MethodHook() { })
    }
    fun uiHook(cl: ClassLoader) {
        ModuleHelper.findAndHookMethod("com.android.UI", cl, "run", object : MethodHook() { })
    }
}
''',
        )
        self._write(
            "installers/SystemServerInstaller.java",
            '''
package tv.withaibuild.customiuizer.installers;
import io.github.libxposed.api.XposedModuleInterface;
import tv.withaibuild.customiuizer.mods.FeatureClass;
public class SystemServerInstaller {
    public static void install(XposedModuleInterface.SystemServerStartingParam lpparam) {
        FeatureClass.serverHook(lpparam.getClassLoader());
    }
}
''',
        )
        self._write(
            "installers/SystemUiInstaller.java",
            '''
package tv.withaibuild.customiuizer.installers;
import io.github.libxposed.api.XposedModuleInterface;
import tv.withaibuild.customiuizer.mods.FeatureClass;
public class SystemUiInstaller {
    public static void install(XposedModuleInterface.PackageReadyParam lpparam) {
        FeatureClass.uiHook(lpparam.getClassLoader());
    }
}
''',
        )
        records = self._scan()
        self.assertEqual(len(records), 2)
        for r in records:
            if r.registration_function == "serverHook":
                self.assertEqual(r.target_process, "SYSTEM_SERVER")
            elif r.registration_function == "uiHook":
                self.assertEqual(r.target_process, "SYSTEM_UI")
            else:
                self.fail(f"unexpected registration_function {r.registration_function}")

    def test_regression_checks_android_package(self):
        self._write(
            "installers/AndroidPackageInstaller.java",
            '''
package tv.withaibuild.customiuizer.installers;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import tv.withaibuild.customiuizer.MainModule;
import tv.withaibuild.customiuizer.mods.catalog.FeatureDispatcher;
import tv.withaibuild.customiuizer.mods.catalog.FeatureRuntime;

public final class AndroidPackageInstaller {
    static boolean isAnyFeatureEnabled(PrefMap prefs) {
        return prefs.getBoolean("system_cleanshare") || prefs.getBoolean("system_cleanopenwith");
    }
    public static void install(PackageReadyParam lpparam, Runnable watchPreferences) {
        String pkg = lpparam.getPackageName();
        if (!"android".equals(pkg)) return;
        if (!isAnyFeatureEnabled(MainModule.mPrefs)) return;

        FeatureRuntime androidRuntime = null;
        boolean listenerNeeded = false;
        if (MainModule.mPrefs.getBoolean("system_cleanshare") || MainModule.mPrefs.getBoolean("system_cleanopenwith")) {
            androidRuntime = FeatureDispatcher.createRuntime(pkg, lpparam, lpparam.getClassLoader(), MainModule.mPrefs);
            if (MainModule.mPrefs.getBoolean("system_cleanshare")) {
                if (FeatureDispatcher.installById("cleanShareMenu", androidRuntime)) listenerNeeded = true;
            }
        }
        if (listenerNeeded) watchPreferences.run();
    }
}
''',
        )
        findings = scan._regression_checks(self.tmp)
        self.assertTrue(any(f["id"] == "EARLY_FEATURE_GATE_ANDROID" and f["status"] == "pass" for f in findings))
        self.assertTrue(any(f["id"] == "GUARDED_WATCH_PREFERENCES_ANDROID" and f["status"] == "pass" for f in findings))
        self.assertTrue(any(f["id"] == "LAZY_FEATURE_RUNTIME_ANDROID" and f["status"] == "pass" for f in findings))


if __name__ == "__main__":
    unittest.main()
