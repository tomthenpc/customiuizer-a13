import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from tools import apk_semantic_diff
from tools import catalog_contract_probe
from tools import ci_contract_scan
from tools import source_hazard_scan


class SourceHazardTest(unittest.TestCase):
    def test_finds_swallowed_throwable(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            path = root / "app/src/main/java/x/Bad.kt"
            path.parent.mkdir(parents=True)
            path.write_text(
                "package x\nobject Bad { fun x() { try {} catch (t: Throwable) { } } }\n",
                encoding="utf-8",
            )
            findings = source_hazard_scan.collect(root, ["app/src/main/java"])
            rules = {f.rule for f in findings}
            self.assertIn("EMPTY_CATCH", rules)
            self.assertIn("CATCH_THROWABLE_NO_FATAL", rules)

    def test_allow_marker_is_narrow(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            path = root / "app/src/main/java/x/Bad.kt"
            path.parent.mkdir(parents=True)
            path.write_text(
                "package x\nfun x() { try {} catch (t: Throwable) { } } "
                "// BRUTAL_ALLOW:EMPTY_CATCH\n",
                encoding="utf-8",
            )
            rules = {f.rule for f in source_hazard_scan.collect(root, ["app/src/main/java"])}
            self.assertNotIn("EMPTY_CATCH", rules)
            self.assertIn("CATCH_THROWABLE_NO_FATAL", rules)


class CIContractTest(unittest.TestCase):
    def test_catches_shallow_signing_and_brittle_sdk(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "bad.yml"
            path.write_text(
                """name: bad
on:
  push:
    branches:
      - main
jobs:
  x:
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v4
      - run: sdkmanager "platforms;android-37"
      - run: ./gradlew -PofficialRelease=true assembleDevelop
""",
                encoding="utf-8",
            )
            errors = ci_contract_scan.scan_workflow(path, "devin/audit", "main")
            text = "\n".join(errors)
            self.assertIn("CI_FULL_HISTORY", text)
            self.assertIn("CI_SIGNING", text)
            self.assertIn("CI_API37_RESOLUTION", text)
            self.assertIn("CI_EXACT_BRANCH", text)

    def test_catches_windows_only_tool_path(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            path = root / "tools" / "bad.py"
            path.parent.mkdir(parents=True)
            drive = f"{chr(67)}{chr(58)}"
            sep = chr(92) * 2
            path.write_text(
                f'x = "a/b".replace("/", "{sep}")\n'
                + fr'y = "{drive}\tmp\x"' + "\n",
                encoding="utf-8",
            )
            errors = ci_contract_scan.scan_repo_scripts(root)
            joined = "\n".join(errors)
            self.assertIn("CI_WINDOWS_PATH_REPLACE", joined)
            self.assertIn("CI_HARDCODED_DRIVE", joined)


class APKDiffTest(unittest.TestCase):
    def _apk(self, path: Path, entries: dict[str, bytes]):
        with zipfile.ZipFile(path, "w") as zf:
            for name, data in entries.items():
                zf.writestr(name, data)

    def test_ignores_signature_but_detects_dex(self):
        with tempfile.TemporaryDirectory() as td:
            old = Path(td) / "old.apk"
            new = Path(td) / "new.apk"
            self._apk(old, {"classes.dex": b"a", "META-INF/X.SF": b"old"})
            self._apk(new, {"classes.dex": b"b", "META-INF/X.SF": b"new"})
            result = apk_semantic_diff.compare(
                apk_semantic_diff.inspect(old), apk_semantic_diff.inspect(new)
            )
            self.assertEqual(["classes.dex"], result["changed"])
            self.assertFalse(result["normalizedEqual"])


class CatalogParserTest(unittest.TestCase):
    def test_balanced_feature_spec_blocks(self):
        text = 'FeatureSpec(id = "a", condition = { x(1) }), FeatureSpec(id = "b")'
        blocks = catalog_contract_probe.balanced_blocks(text, "FeatureSpec")
        self.assertEqual(2, len(blocks))
        self.assertEqual("a", catalog_contract_probe.field(blocks[0], "id"))


class BrutalConfigContractTest(unittest.TestCase):
    """Fail fast if brutal_test_config.json is missing required fields."""

    CONFIG = Path(__file__).resolve().parents[1] / "brutal_test_config.json"

    def test_determinism_fields_present(self) -> None:
        cfg = json.loads(self.CONFIG.read_text(encoding="utf-8"))
        self.assertIn("determinism_command", cfg, "brutal_test_config.json must define determinism_command")
        self.assertIsInstance(cfg["determinism_command"], str)
        self.assertTrue(cfg["determinism_command"].strip(), "determinism_command must not be empty")
        self.assertIn("determinism_outputs", cfg, "brutal_test_config.json must define determinism_outputs")
        self.assertIsInstance(cfg["determinism_outputs"], list)
        self.assertGreater(len(cfg["determinism_outputs"]), 0, "determinism_outputs must not be empty")
        for p in cfg["determinism_outputs"]:
            self.assertIsInstance(p, str)
            self.assertTrue(p.strip(), "determinism_outputs entries must not be empty")

    def test_hermetic_commands_present(self) -> None:
        cfg = json.loads(self.CONFIG.read_text(encoding="utf-8"))
        self.assertIn("hermetic_commands", cfg)
        self.assertIsInstance(cfg["hermetic_commands"], list)
        self.assertGreater(len(cfg["hermetic_commands"]), 0)


class StaticOwnerHazardTest(unittest.TestCase):
    """Tests for STATIC_STRONG_ANDROID_OWNER detection of Context fields."""

    def _scan(self, code: str) -> set[str]:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            p = root / "app" / "src" / "main" / "java" / "x" / "Test.kt"
            p.parent.mkdir(parents=True)
            p.write_text(code, encoding="utf-8")
            return {f.rule for f in source_hazard_scan.collect(root, ["app/src/main/java"])}

    def test_object_context_field_detected(self):
        rules = self._scan(
            "package x\nobject Bad {\n    @JvmField\n    var ctx: Context? = null\n}\n"
        )
        self.assertIn("STATIC_STRONG_ANDROID_OWNER", rules)

    def test_object_fq_context_field_detected(self):
        rules = self._scan(
            "package x\nobject Bad {\n    @JvmField\n    var ctx: android.content.Context? = null\n}\n"
        )
        self.assertIn("STATIC_STRONG_ANDROID_OWNER", rules)

    def test_companion_context_field_detected(self):
        rules = self._scan(
            "package x\nclass Foo {\n    companion object {\n        @JvmField\n        var ctx: Context? = null\n    }\n}\n"
        )
        self.assertIn("STATIC_STRONG_ANDROID_OWNER", rules)

    def test_local_context_variable_ignored(self):
        rules = self._scan(
            "package x\nclass Foo {\n    fun doWork() {\n        val ctx: Context? = null\n    }\n}\n"
        )
        self.assertNotIn("STATIC_STRONG_ANDROID_OWNER", rules)

    def test_instance_context_field_ignored(self):
        rules = self._scan(
            "package x\nclass Foo {\n    var ctx: Context? = null\n}\n"
        )
        self.assertNotIn("STATIC_STRONG_ANDROID_OWNER", rules)

    def test_weak_reference_context_ignored(self):
        rules = self._scan(
            "package x\nobject Bad {\n    var ctx: WeakReference<Context>? = null\n}\n"
        )
        self.assertNotIn("STATIC_STRONG_ANDROID_OWNER", rules)


class EagerHandlerThreadHazardTest(unittest.TestCase):
    """Tests for EAGER_HANDLER_THREAD detection."""

    def _scan(self, code: str) -> set[str]:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            p = root / "app" / "src" / "main" / "java" / "x" / "Test.kt"
            p.parent.mkdir(parents=True)
            p.write_text(code, encoding="utf-8")
            return {f.rule for f in source_hazard_scan.collect(root, ["app/src/main/java"])}

    def test_apply_start_detected(self):
        rules = self._scan(
            'package x\nobject Bad {\n    val w = android.os.HandlerThread("bad").apply { start() }\n}\n'
        )
        self.assertIn("EAGER_HANDLER_THREAD", rules)

    def test_chained_start_detected(self):
        rules = self._scan(
            'package x\nobject Bad {\n    val w = android.os.HandlerThread("bad")\n    init { w.start() }\n}\n'
        )
        self.assertIn("EAGER_HANDLER_THREAD", rules)

    def test_no_start_ignored(self):
        rules = self._scan(
            'package x\nclass Foo {\n    val w = android.os.HandlerThread("worker")\n}\n'
        )
        self.assertNotIn("EAGER_HANDLER_THREAD", rules)

    def test_lazy_lifecycle_start_ignored(self):
        # start() is far from construction (in a separate method, >160 chars apart)
        padding = "    " + "// lifecycle logic\n" * 12
        rules = self._scan(
            'package x\nclass Foo {\n    val w = android.os.HandlerThread("worker")\n'
            + padding
            + '    fun onStart() {\n        w.start()\n    }\n}\n'
        )
        self.assertNotIn("EAGER_HANDLER_THREAD", rules)


class WrongBranchMutatorTest(unittest.TestCase):
    """Tests for the wrong CI branch mutator."""

    def test_mutator_replaces_yaml_branch(self):
        from tools.brutal_test_runner import mutate_wrong_branch
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            wf = root / ".github" / "workflows" / "ci.yml"
            wf.parent.mkdir(parents=True)
            wf.write_text(
                "on:\n  push:\n    branches:\n      - main\njobs:\n  x:\n    runs-on: ubuntu-latest\n",
                encoding="utf-8",
            )
            cfg = {"fast_workflows": [".github/workflows/ci.yml"], "expected_branch": "main"}
            mutate_wrong_branch(root, cfg)
            text = wf.read_text(encoding="utf-8")
            self.assertIn("devin/stale-ci-branch", text)
            self.assertNotIn("- main", text)


if __name__ == "__main__":
    unittest.main()
