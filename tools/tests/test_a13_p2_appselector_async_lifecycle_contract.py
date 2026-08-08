"""P2-2 regression test: AppSelector async app-list load lifecycle contract.

This test reads the real ``AppSelector.kt`` source and verifies that the
delayed app-load kickoff is properly bounded by the Fragment View lifecycle,
the background Thread does not capture Activity or Fragment, and completion
uses WeakReference + mainExecutor with a live-view gate.

Negative cases work by mutating a temporary copy of the source and re-running
the contract checks against the mutated version.
"""

from __future__ import annotations

import re
import unittest
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
_APPSELECTOR_REL = (
    "app/src/main/java/tv/withaibuild/customiuizer/subs/AppSelector.kt"
)


def _read_source(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _extract_method(source: str, method_name: str) -> str:
    """Return the full text of a method override up to its closing brace."""
    pattern = rf"override\s+fun\s+{method_name}\s*\("
    m = re.search(pattern, source)
    if not m:
        return ""
    # Find the opening brace of the method body
    brace_start = source.find("{", m.end())
    if brace_start < 0:
        return ""
    depth = 0
    for i in range(brace_start, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[m.start() : i + 1]
    return ""


def _extract_on_activity_created(source: str) -> str:
    return _extract_method(source, "onActivityCreated")


def _extract_on_destroy_view(source: str) -> str:
    return _extract_method(source, "onDestroyView")


def _extract_thread_body(source: str) -> str:
    """Extract the Thread { ... } block from onActivityCreated."""
    on_ac = _extract_on_activity_created(source)
    m = re.search(r"Thread\s*\{", on_ac)
    if not m:
        return ""
    depth = 0
    for i in range(m.end() - 1, len(on_ac)):
        if on_ac[i] == "{":
            depth += 1
        elif on_ac[i] == "}":
            depth -= 1
            if depth == 0:
                return on_ac[m.start() : i + 1]
    return ""


def _extract_completion_block(source: str) -> str:
    """Extract the mainExecutor.execute { ... } block."""
    on_ac = _extract_on_activity_created(source)
    m = re.search(r"mainExecutor\.execute\s*\{", on_ac)
    if not m:
        return ""
    depth = 0
    for i in range(m.end() - 1, len(on_ac)):
        if on_ac[i] == "{":
            depth += 1
        elif on_ac[i] == "}":
            depth -= 1
            if depth == 0:
                return on_ac[m.start() : i + 1]
    return ""


class AppSelectorAsyncLifecycleContractTest(unittest.TestCase):
    """Positive contract tests against the real AppSelector.kt."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _APPSELECTOR_REL
        cls.source = _read_source(cls.source_path)
        cls.on_activity_created = _extract_on_activity_created(cls.source)
        cls.on_destroy_view = _extract_on_destroy_view(cls.source)
        cls.thread_body = _extract_thread_body(cls.source)
        cls.completion = _extract_completion_block(cls.source)

    # --- 1. pendingAppLoadStart field ---

    def test_pending_app_load_start_field_exists(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+pendingAppLoadStart\s*:\s*Runnable\s*\?\s*=\s*null",
            "AppSelector must declare pendingAppLoadStart: Runnable? = null",
        )

    # --- 2. appLoadInFlight field ---

    def test_app_load_in_flight_field_exists(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+appLoadInFlight\s*=\s*false",
            "AppSelector must declare appLoadInFlight = false",
        )

    # --- 3. applicationContext used ---

    def test_application_context_used(self):
        self.assertIn(
            "applicationContext",
            self.on_activity_created,
            "onActivityCreated must use applicationContext",
        )

    # --- 4. WeakReference<AppSelector> exists ---

    def test_weak_reference_exists(self):
        self.assertIn("WeakReference", self.on_activity_created)
        self.assertIn("fragmentRef", self.on_activity_created)

    # --- 5. delayed kickoff is identifiable Runnable ---

    def test_kickoff_is_named_runnable(self):
        self.assertRegex(
            self.on_activity_created,
            r"object\s*:\s*Runnable\s*\{",
            "Delayed kickoff must be an object : Runnable, not a bare lambda",
        )

    # --- 6. delay uses animDur ---

    def test_delay_uses_animDur(self):
        self.assertRegex(
            self.on_activity_created,
            r"postDelayed\s*\(\s*runnable\s*,\s*animDur\.toLong\(\)\s*\)",
            "postDelayed must use animDur.toLong()",
        )

    # --- 7. schedule removes old pending ---

    def test_old_pending_removed_before_schedule(self):
        self.assertRegex(
            self.on_activity_created,
            r"pendingAppLoadStart\s*\?\.",
            "onActivityCreated must check previous pendingAppLoadStart",
        )
        self.assertIn("removeCallbacks(previous)", self.on_activity_created)

    # --- 8. callback self-clears ---

    def test_callback_self_clears(self):
        self.assertIn("pendingAppLoadStart === this", self.on_activity_created)
        self.assertIn("pendingAppLoadStart = null", self.on_activity_created)

    # --- 9. post failure clears slot ---

    def test_post_failure_clears_slot(self):
        self.assertRegex(
            self.on_activity_created,
            r"if\s*\(\s*!.*postDelayed",
            "onActivityCreated must check postDelayed return value",
        )

    # --- 10. onDestroyView cancels pending ---

    def test_on_destroy_view_cancels_pending(self):
        self.assertTrue(self.on_destroy_view, "onDestroyView must exist")
        self.assertIn("removeCallbacks", self.on_destroy_view)
        self.assertIn("pendingAppLoadStart = null", self.on_destroy_view)

    # --- 11. cleanup before super.onDestroyView ---

    def test_cleanup_before_super(self):
        super_idx = self.on_destroy_view.find("super.onDestroyView")
        remove_idx = self.on_destroy_view.find("removeCallbacks")
        null_idx = self.on_destroy_view.find("pendingAppLoadStart = null")
        self.assertGreater(super_idx, 0, "super.onDestroyView must be called")
        self.assertLess(remove_idx, super_idx, "removeCallbacks must be before super")
        self.assertLess(null_idx, super_idx, "pendingAppLoadStart = null must be before super")

    # --- 12. Thread body does not use Activity ---

    def test_thread_body_no_activity(self):
        forbidden = ["act.", "act,", "act)", "activity", "requireActivity"]
        for token in forbidden:
            self.assertNotIn(
                token,
                self.thread_body,
                f"Thread body must not reference Activity ({token})",
            )

    # --- 13. Thread body does not directly access View/listView ---

    def test_thread_body_no_view_access(self):
        # The Thread body must not directly access Fragment fields like listView,
        # view, or this@AppSelector. Access through WeakReference (fragment.xxx) is allowed.
        forbidden = ["listView", "this@AppSelector"]
        for token in forbidden:
            self.assertNotIn(
                token,
                self.thread_body,
                f"Thread body must not directly access {token}",
            )
        # Check that 'process' is only accessed through the WeakReference, not directly
        # Direct access would be "process?.run()" without "fragment." prefix
        direct_process = re.search(r"(?<!fragment\.)process\?\.run\(\)", self.thread_body)
        self.assertIsNone(
            direct_process,
            "Thread body must not directly access process without WeakReference",
        )

    # --- 14. completion uses mainExecutor ---

    def test_completion_uses_main_executor(self):
        self.assertIn("mainExecutor.execute", self.on_activity_created)

    # --- 15. completion uses WeakReference ---

    def test_completion_uses_weak_reference(self):
        self.assertIn("fragmentRef.get()", self.completion)

    # --- 16. completion clears appLoadInFlight ---

    def test_completion_clears_in_flight(self):
        self.assertIn("appLoadInFlight = false", self.completion)

    # --- 17. success sets initialized ---

    def test_success_sets_initialized(self):
        self.assertIn("initialized = true", self.completion)

    # --- 18. UI process checks isAdded ---

    def test_process_checks_is_added(self):
        self.assertIn("isAdded", self.completion)

    # --- 19. UI process checks view != null ---

    def test_process_checks_view_not_null(self):
        self.assertIn("view != null", self.completion)

    # --- 20. cache selection dispatch preserved ---

    def test_cache_dispatch_preserved(self):
        self.assertIn("Helpers.openWithAppsList", self.on_activity_created)
        self.assertIn("Helpers.getOpenWithApps", self.on_activity_created)
        self.assertIn("Helpers.shareAppsList", self.on_activity_created)
        self.assertIn("Helpers.getShareApps", self.on_activity_created)
        self.assertIn("Helpers.installedAppsList", self.on_activity_created)
        self.assertIn("Helpers.getInstalledApps", self.on_activity_created)
        self.assertIn("Helpers.launchableAppsList", self.on_activity_created)
        self.assertIn("Helpers.getLaunchableApps", self.on_activity_created)

    # --- 21. single-flight check ---

    def test_single_flight_check(self):
        self.assertIn("appLoadInFlight", self.on_activity_created)

    # --- 22. No runOnUiThread ---

    def test_no_run_on_ui_thread(self):
        self.assertNotIn("runOnUiThread", self.on_activity_created)

    # --- 23. WeakReference import ---

    def test_weak_reference_import(self):
        self.assertIn("java.lang.ref.WeakReference", self.source)

    # --- 24. No Handler/Coroutine/Executor pool ---

    def test_no_handler_or_coroutine(self):
        forbidden = [r"\bHandler\b", r"\bCoroutine\b", r"\bFlow\b", r"\bExecutorService\b"]
        for pat in forbidden:
            self.assertNotRegex(self.on_activity_created, pat, f"Must not introduce {pat}")


class AppSelectorAsyncLifecycleNegativeTest(unittest.TestCase):
    """Negative contract tests: mutating the source to remove the fix must FAIL."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _APPSELECTOR_REL
        cls.original = _read_source(cls.source_path)

    def _check_contracts(self, source: str) -> list[str]:
        """Return a list of contract violations (empty = all pass)."""
        violations = []
        on_ac = _extract_on_activity_created(source)
        on_dv = _extract_on_destroy_view(source)
        thread_body = _extract_thread_body(source)
        completion = _extract_completion_block(source)

        if not re.search(r"private\s+var\s+pendingAppLoadStart\s*:\s*Runnable\s*\?\s*=\s*null", source):
            violations.append("missing pendingAppLoadStart field")
        if not re.search(r"private\s+var\s+appLoadInFlight\s*=\s*false", source):
            violations.append("missing appLoadInFlight field")
        if "applicationContext" not in on_ac:
            violations.append("missing applicationContext")
        if "WeakReference" not in on_ac:
            violations.append("missing WeakReference")
        if not re.search(r"object\s*:\s*Runnable\s*\{", on_ac):
            violations.append("kickoff not named Runnable")
        if not re.search(r"postDelayed\s*\(\s*runnable\s*,\s*animDur\.toLong\(\)\s*\)", on_ac):
            violations.append("postDelayed not using animDur")
        if "removeCallbacks(previous)" not in on_ac:
            violations.append("no old pending removal")
        if "pendingAppLoadStart === this" not in on_ac:
            violations.append("no callback self-clear")
        if not re.search(r"if\s*\(\s*!.*postDelayed", on_ac):
            violations.append("no post failure cleanup")
        if not on_dv:
            violations.append("onDestroyView missing")
        if "removeCallbacks" not in on_dv:
            violations.append("onDestroyView missing removeCallbacks")
        if "pendingAppLoadStart = null" not in on_dv:
            violations.append("onDestroyView missing null clear")
        super_idx = on_dv.find("super.onDestroyView")
        remove_idx = on_dv.find("removeCallbacks")
        if super_idx <= 0:
            violations.append("onDestroyView missing super.onDestroyView")
        elif remove_idx > 0 and remove_idx > super_idx:
            violations.append("removeCallbacks after super")
        # Thread body Activity check
        for token in ["act.", "activity", "requireActivity"]:
            if token in thread_body:
                violations.append(f"Thread body references Activity ({token})")
                break
        if "this@AppSelector" in thread_body:
            violations.append("Thread body captures this@AppSelector")
        if "listView" in thread_body:
            violations.append("Thread body directly accesses listView")
        # Check for direct process access (without fragment. prefix)
        if re.search(r"(?<!fragment\.)process\?\.run\(\)", thread_body):
            violations.append("Thread body directly accesses process")
        if "mainExecutor.execute" not in on_ac:
            violations.append("no mainExecutor")
        if "fragmentRef.get()" not in completion:
            violations.append("completion not using WeakReference")
        if "appLoadInFlight = false" not in completion:
            violations.append("completion not clearing inFlight")
        if "initialized = true" not in completion:
            violations.append("completion not setting initialized")
        if "isAdded" not in completion:
            violations.append("completion not checking isAdded")
        if "view != null" not in completion:
            violations.append("completion not checking view != null")
        if "appLoadInFlight" not in on_ac:
            violations.append("no single-flight check")
        if "runOnUiThread" in on_ac:
            violations.append("still using runOnUiThread")
        # Dispatch checks
        for helper in ["Helpers.getOpenWithApps", "Helpers.getShareApps",
                       "Helpers.getInstalledApps", "Helpers.getLaunchableApps"]:
            if helper not in on_ac:
                violations.append(f"missing {helper} dispatch")
        return violations

    def test_A_activity_capture_fails(self):
        """If applicationContext is replaced with Activity, contracts must fail."""
        mutated = self.original.replace("applicationContext", "activity")
        violations = self._check_contracts(mutated)
        self.assertTrue(
            any("applicationContext" in v or "Activity" in v for v in violations),
            "Activity capture must cause contract failure",
        )

    def test_B_thread_captures_fragment_fails(self):
        """If Thread body captures this@AppSelector, contracts must fail."""
        mutated = self.original.replace(
            "val fragment = fragmentRef.get()",
            "val fragment = this@AppSelector",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("Thread body captures this@AppSelector", violations)

    def test_C_no_weak_reference_fails(self):
        """If WeakReference is removed, contracts must fail."""
        mutated = self.original.replace("WeakReference(this)", "this")
        violations = self._check_contracts(mutated)
        self.assertIn("missing WeakReference", violations)

    def test_D_no_destroy_view_cleanup_fails(self):
        """If onDestroyView removeCallbacks is removed, contracts must fail."""
        mutated = self.original.replace(
            "view?.removeCallbacks(pending)",
            "/* removed */",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("onDestroyView missing removeCallbacks", violations)

    def test_E_no_view_gate_fails(self):
        """If completion has no view != null gate, contracts must fail."""
        mutated = self.original.replace("fragment.view != null", "true")
        violations = self._check_contracts(mutated)
        self.assertIn("completion not checking view != null", violations)

    def test_F_no_inflight_reset_fails(self):
        """If appLoadInFlight reset is removed, contracts must fail."""
        mutated = self.original.replace(
            "fragment.appLoadInFlight = false",
            "/* removed */",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("completion not clearing inFlight", violations)

    def test_G_no_single_flight_fails(self):
        """If single-flight check is removed, contracts must fail."""
        # Remove the appLoadInFlight check from onActivityCreated
        mutated = self.original.replace(
            "} else if (appLoadInFlight) {",
            "} else if (false) {",
        )
        # Also remove the field declaration so the check fails
        mutated = mutated.replace(
            "private var appLoadInFlight = false",
            "private var appLoadInFlight_removed = false",
        )
        violations = self._check_contracts(mutated)
        self.assertTrue(
            any("appLoadInFlight" in v for v in violations),
            "Removing single-flight must cause contract failure",
        )

    def test_H_run_on_ui_thread_fails(self):
        """If mainExecutor is replaced with runOnUiThread, contracts must fail."""
        mutated = self.original.replace(
            "appContext.mainExecutor.execute",
            "act.runOnUiThread",
        )
        violations = self._check_contracts(mutated)
        self.assertTrue(
            any("mainExecutor" in v or "runOnUiThread" in v for v in violations),
            "runOnUiThread must cause contract failure",
        )

    def test_I_no_super_destroy_view_fails(self):
        """If super.onDestroyView is removed, contracts must fail."""
        # Replace super.onDestroyView() that comes after pendingAppLoadStart = null
        # in AppSelector's onDestroyView (not in parent class)
        mutated = re.sub(
            r"(pendingAppLoadStart = null\s*\n\s*)super\.onDestroyView\(\)",
            r"\1/* removed */",
            self.original,
            count=1,
        )
        violations = self._check_contracts(mutated)
        self.assertIn("onDestroyView missing super.onDestroyView", violations)

    def test_J_changed_dispatch_fails(self):
        """If openwith/share/installed/launchable dispatch is changed, contracts must fail."""
        # Remove getOpenWithApps entirely
        mutated = self.original.replace("Helpers.getOpenWithApps(appContext)", "/* removed */")
        violations = self._check_contracts(mutated)
        self.assertIn("missing Helpers.getOpenWithApps dispatch", violations)


if __name__ == "__main__":
    unittest.main()
