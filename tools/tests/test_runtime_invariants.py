"""Runtime safety invariant tests."""

import importlib.util
import sys
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
INVARIANTS_PATH = REPO_ROOT / "tools" / "check-invariants.py"
INSTALLER_PATH = (
    REPO_ROOT
    / "app"
    / "src"
    / "main"
    / "java"
    / "tv"
    / "withaibuild"
    / "customiuizer"
    / "installers"
    / "SampleInstaller.java"
)

_spec = importlib.util.spec_from_file_location("runtime_invariants", INVARIANTS_PATH)
runtime_invariants = importlib.util.module_from_spec(_spec)
sys.modules["runtime_invariants"] = runtime_invariants
_spec.loader.exec_module(runtime_invariants)


class InstallerOomBoundaryInvariant(unittest.TestCase):
    def test_throwable_catch_without_oom_rethrow_fails(self) -> None:
        text = "try { install(); } catch (Throwable ignored) { return; }"
        self.assertTrue(runtime_invariants.check_installer_oom_boundary(INSTALLER_PATH, text))

    def test_java_oom_rethrow_passes(self) -> None:
        text = (
            "try { install(); } catch (Throwable t) {"
            " if (t instanceof OutOfMemoryError) throw (OutOfMemoryError) t;"
            " return; }"
        )
        self.assertEqual(
            runtime_invariants.check_installer_oom_boundary(INSTALLER_PATH, text),
            [],
        )

    def test_kotlin_oom_rethrow_passes(self) -> None:
        path = INSTALLER_PATH.with_suffix(".kt")
        text = (
            "try { install() } catch (t: Throwable) {"
            " if (t is OutOfMemoryError) throw t"
            " return }"
        )
        self.assertEqual(runtime_invariants.check_installer_oom_boundary(path, text), [])


if __name__ == "__main__":
    unittest.main()
