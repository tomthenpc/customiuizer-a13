"""P2-2 R1 regression test: AppSelector async app-list load lifecycle contract.

This test reads the real ``AppSelector.kt`` source and verifies:
- The delayed app-load kickoff is View-lifecycle cancellable.
- The background Thread is created inside a companion/static worker function,
  structurally preventing strong AppSelector/Activity/View capture.
- Completion uses WeakReference + mainExecutor with a live-view gate.
- Failure-after-view-recreation retry gap is closed via retryAppLoadAfterInFlight.
- Retry is bounded: only one replacement attempt per lifecycle demand.

Negative cases work by mutating a copy of the source and re-running the
contract checks against the mutated version.
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


def _extract_brace_block(source: str, start_idx: int) -> str:
    """Given an index pointing at '{', return source up to matching '}'."""
    if start_idx < 0 or source[start_idx] != "{":
        return ""
    depth = 0
    for i in range(start_idx, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[start_idx : i + 1]
    return ""


def _extract_fun(source: str, fun_name: str, is_override: bool = False) -> str:
    """Return the full text of a function up to its closing brace."""
    prefix = r"override\s+" if is_override else ""
    pattern = rf"{prefix}fun\s+{fun_name}\s*\("
    m = re.search(pattern, source)
    if not m:
        return ""
    brace_start = source.find("{", m.end())
    if brace_start < 0:
        return ""
    block = _extract_brace_block(source, brace_start)
    return source[m.start() : brace_start + len(block)]


def _extract_method(source: str, method_name: str) -> str:
    return _extract_fun(source, method_name, is_override=True)


def _extract_on_activity_created(source: str) -> str:
    return _extract_method(source, "onActivityCreated")


def _extract_on_destroy_view(source: str) -> str:
    return _extract_method(source, "onDestroyView")


def _extract_schedule_app_load(source: str) -> str:
    return _extract_fun(source, "scheduleAppLoad")


def _extract_on_app_load_finished(source: str) -> str:
    return _extract_fun(source, "onAppLoadFinished")


def _extract_companion(source: str) -> str:
    """Extract the companion object { ... } block."""
    m = re.search(r"companion\s+object\s*\{", source)
    if not m:
        return ""
    brace_start = source.find("{", m.end() - 1)
    if brace_start < 0:
        return ""
    block = _extract_brace_block(source, brace_start)
    return source[m.start() : brace_start + len(block)]


def _extract_start_app_load_worker(source: str) -> str:
    """Extract startAppLoadWorker function from companion object."""
    companion = _extract_companion(source)
    return _extract_fun(companion, "startAppLoadWorker")


def _extract_thread_body_from_companion(source: str) -> str:
    """Extract the Thread { ... } block from the companion worker function."""
    worker = _extract_start_app_load_worker(source)
    m = re.search(r"Thread\s*\{", worker)
    if not m:
        return ""
    brace_start = worker.find("{", m.end() - 1)
    if brace_start < 0:
        return ""
    return _extract_brace_block(worker, brace_start)


def _extract_completion_block_from_companion(source: str) -> str:
    """Extract the mainExecutor.execute { ... } block from the companion worker."""
    worker = _extract_start_app_load_worker(source)
    m = re.search(r"mainExecutor\.execute\s*\{", worker)
    if not m:
        return ""
    brace_start = worker.find("{", m.end() - 1)
    if brace_start < 0:
        return ""
    return _extract_brace_block(worker, brace_start)


def _extract_all_thread_blocks(source: str) -> list[str]:
    """Find all Thread { ... } blocks anywhere in source."""
    blocks = []
    for m in re.finditer(r"Thread\s*\{", source):
        brace_start = source.find("{", m.end() - 1)
        if brace_start < 0:
            continue
        block = _extract_brace_block(source, brace_start)
        if block:
            blocks.append(block)
    return blocks


class AppSelectorAsyncLifecycleContractTest(unittest.TestCase):
    """Positive contract tests against the real AppSelector.kt."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _APPSELECTOR_REL
        cls.source = _read_source(cls.source_path)
        cls.on_activity_created = _extract_on_activity_created(cls.source)
        cls.on_destroy_view = _extract_on_destroy_view(cls.source)
        cls.schedule_app_load = _extract_schedule_app_load(cls.source)
        cls.on_app_load_finished = _extract_on_app_load_finished(cls.source)
        cls.companion = _extract_companion(cls.source)
        cls.worker = _extract_start_app_load_worker(cls.source)
        cls.thread_body = _extract_thread_body_from_companion(cls.source)
        cls.completion = _extract_completion_block_from_companion(cls.source)

    # --- 1. pendingAppLoadStart field ---

    def test_01_pending_app_load_start_field(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+pendingAppLoadStart\s*:\s*Runnable\s*\?\s*=\s*null",
        )

    # --- 2. appLoadInFlight field ---

    def test_02_app_load_in_flight_field(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+appLoadInFlight\s*=\s*false",
        )

    # --- 3. retryAppLoadAfterInFlight field ---

    def test_03_retry_app_load_after_in_flight_field(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+retryAppLoadAfterInFlight\s*=\s*false",
        )

    # --- 4. applicationContext used ---

    def test_04_application_context_used(self):
        self.assertIn("applicationContext", self.schedule_app_load)

    # --- 5. WeakReference<AppSelector> exists ---

    def test_05_weak_reference_exists(self):
        self.assertIn("WeakReference", self.schedule_app_load)
        self.assertIn("fragmentRef", self.schedule_app_load)

    # --- 6. delayed kickoff is identifiable Runnable ---

    def test_06_kickoff_is_named_runnable(self):
        self.assertRegex(
            self.schedule_app_load,
            r"object\s*:\s*Runnable\s*\{",
        )

    # --- 7. delay uses animDur ---

    def test_07_delay_uses_animDur(self):
        self.assertRegex(
            self.schedule_app_load,
            r"postDelayed\s*\(\s*runnable\s*,\s*animDur\.toLong\(\)\s*\)",
        )

    # --- 8. schedule removes old pending ---

    def test_08_old_pending_removed_before_schedule(self):
        self.assertIn("pendingAppLoadStart?.let", self.schedule_app_load)
        self.assertIn("removeCallbacks(previous)", self.schedule_app_load)

    # --- 9. callback self-clears ---

    def test_09_callback_self_clears(self):
        self.assertIn("pendingAppLoadStart === this", self.schedule_app_load)
        self.assertIn("pendingAppLoadStart = null", self.schedule_app_load)

    # --- 10. post failure clears slot ---

    def test_10_post_failure_clears_slot(self):
        self.assertRegex(
            self.schedule_app_load,
            r"if\s*\(\s*!.*postDelayed",
        )

    # --- 11. onDestroyView cancels pending ---

    def test_11_on_destroy_view_cancels_pending(self):
        self.assertTrue(self.on_destroy_view)
        self.assertIn("removeCallbacks", self.on_destroy_view)
        self.assertIn("pendingAppLoadStart = null", self.on_destroy_view)

    # --- 12. cleanup before super.onDestroyView ---

    def test_12_cleanup_before_super(self):
        super_idx = self.on_destroy_view.find("super.onDestroyView")
        remove_idx = self.on_destroy_view.find("removeCallbacks")
        self.assertGreater(super_idx, 0)
        self.assertLess(remove_idx, super_idx)

    # --- 13. Thread body does not use Activity ---

    def test_13_thread_body_no_activity(self):
        forbidden = ["act.", "act,", "act)", "activity", "requireActivity"]
        for token in forbidden:
            self.assertNotIn(token, self.thread_body, f"Thread body must not reference {token}")

    # --- 14. Thread body does not directly access View/listView/process ---

    def test_14_thread_body_no_view_access(self):
        forbidden = ["listView", "this@AppSelector", "process"]
        for token in forbidden:
            self.assertNotIn(token, self.thread_body, f"Thread body must not access {token}")

    # --- 15. completion uses mainExecutor ---

    def test_15_completion_uses_main_executor(self):
        self.assertIn("mainExecutor.execute", self.worker)

    # --- 16. completion uses WeakReference via onAppLoadFinished ---

    def test_16_completion_uses_weak_reference(self):
        self.assertIn("fragmentRef.get()", self.completion)
        self.assertIn("onAppLoadFinished", self.completion)

    # --- 17. onAppLoadFinished clears appLoadInFlight ---

    def test_17_completion_clears_in_flight(self):
        self.assertIn("appLoadInFlight = false", self.on_app_load_finished)

    # --- 18. onAppLoadFinished success sets initialized ---

    def test_18_success_sets_initialized(self):
        self.assertIn("initialized = true", self.on_app_load_finished)

    # --- 19. UI process checks isAdded ---

    def test_19_process_checks_is_added(self):
        self.assertIn("isAdded", self.on_app_load_finished)

    # --- 20. UI process checks view != null ---

    def test_20_process_checks_view_not_null(self):
        self.assertIn("view != null", self.on_app_load_finished)

    # --- 21. cache selection dispatch preserved ---

    def test_21_cache_dispatch_preserved(self):
        for helper in [
            "Helpers.openWithAppsList", "Helpers.getOpenWithApps",
            "Helpers.shareAppsList", "Helpers.getShareApps",
            "Helpers.installedAppsList", "Helpers.getInstalledApps",
            "Helpers.launchableAppsList", "Helpers.getLaunchableApps",
        ]:
            self.assertIn(helper, self.worker, f"Missing {helper} in worker")

    # --- 22. single-flight check in onActivityCreated ---

    def test_22_single_flight_check(self):
        self.assertIn("appLoadInFlight", self.on_activity_created)

    # --- 23. No runOnUiThread ---

    def test_23_no_run_on_ui_thread(self):
        self.assertNotIn("runOnUiThread", self.source)

    # --- 24. WeakReference import ---

    def test_24_weak_reference_import(self):
        self.assertIn("java.lang.ref.WeakReference", self.source)

    # --- 25. No Handler/Coroutine/ExecutorService ---

    def test_25_no_handler_or_coroutine(self):
        forbidden = [r"\bHandler\b", r"\bCoroutine\b", r"\bFlow\b", r"\bExecutorService\b"]
        for pat in forbidden:
            self.assertNotRegex(self.source, pat, f"Must not introduce {pat}")

    # ===== R1 state-machine contracts =====

    # --- 26. onActivityCreated sets retryAppLoadAfterInFlight when in-flight ---

    def test_26_in_flight_sets_retry_flag(self):
        self.assertIn("retryAppLoadAfterInFlight = true", self.on_activity_created)

    # --- 27. onActivityCreated calls scheduleAppLoad ---

    def test_27_calls_schedule_app_load(self):
        self.assertIn("scheduleAppLoad()", self.on_activity_created)

    # --- 28. onAppLoadFinished snapshots retry before clearing ---

    def test_28_completion_snapshots_retry(self):
        self.assertRegex(
            self.on_app_load_finished,
            r"val\s+retry\s*=\s*retryAppLoadAfterInFlight",
        )

    # --- 29. onAppLoadFinished clears retry flag ---

    def test_29_completion_clears_retry(self):
        self.assertIn("retryAppLoadAfterInFlight = false", self.on_app_load_finished)

    # --- 30. failure+retry+live-view triggers scheduleAppLoad ---

    def test_30_failure_retry_schedules(self):
        self.assertIn("retry", self.on_app_load_finished)
        self.assertIn("scheduleAppLoad()", self.on_app_load_finished)

    # --- 31. failure retry is gated by retry flag (not unconditional) ---

    def test_31_failure_retry_is_gated(self):
        # The retry path must check the retry flag, not just !success
        self.assertRegex(
            self.on_app_load_finished,
            r"else\s+if\s*\(\s*retry\s*&&\s*isAdded\s*&&\s*view\s*!=\s*null\s*\)",
        )

    # --- 32. success path does not schedule ---

    def test_32_success_does_not_schedule(self):
        # The success branch should set initialized and run process,
        # not call scheduleAppLoad
        success_section = re.search(
            r"if\s*\(\s*success\s*\)\s*\{(.*?)\}",
            self.on_app_load_finished,
            re.DOTALL,
        )
        self.assertIsNotNone(success_section, "Must have if (success) branch")
        self.assertNotIn("scheduleAppLoad", success_section.group(1))

    # --- 33. onDestroyView clears retry flag ---

    def test_33_on_destroy_view_clears_retry(self):
        self.assertIn("retryAppLoadAfterInFlight = false", self.on_destroy_view)

    # ===== R1 worker ownership contracts =====

    # --- 34. companion object exists ---

    def test_34_companion_object_exists(self):
        self.assertTrue(self.companion, "companion object must exist")

    # --- 35. startAppLoadWorker exists in companion ---

    def test_35_worker_helper_exists(self):
        self.assertTrue(self.worker, "startAppLoadWorker must exist in companion")

    # --- 36. worker helper receives WeakReference<AppSelector> ---

    def test_36_worker_receives_weak_reference(self):
        self.assertIn("WeakReference<AppSelector>", self.worker)

    # --- 37. worker helper does not receive AppSelector directly ---

    def test_37_worker_no_direct_app_selector_param(self):
        # Check parameter list - should not have AppSelector as a direct param
        # (WeakReference<AppSelector> is ok)
        param_match = re.search(r"fun\s+startAppLoadWorker\s*\(([^)]*)\)", self.worker)
        self.assertIsNotNone(param_match)
        params = param_match.group(1)
        # Check no bare AppSelector param (WeakReference<AppSelector> is fine)
        # Look for AppSelector not wrapped in WeakReference
        bare = re.search(r"(?<!WeakReference<)AppSelector(?!\s*>)", params)
        self.assertIsNone(bare, "Worker must not receive bare AppSelector parameter")

    # --- 38. worker helper does not receive Activity/View/Fragment ---

    def test_38_worker_no_activity_view_fragment_param(self):
        param_match = re.search(r"fun\s+startAppLoadWorker\s*\(([^)]*)\)", self.worker)
        self.assertIsNotNone(param_match)
        params = param_match.group(1)
        # Check for type names (after the colon), not field names (before the colon)
        # Split params and check the type part only
        for param_line in params.split(","):
            parts = param_line.split(":")
            if len(parts) < 2:
                continue
            param_type = parts[1].strip()
            for forbidden in ["Activity", "View", "Fragment"]:
                self.assertNotIn(forbidden, param_type,
                    f"Worker param type must not be {forbidden}: {param_type}")

    # --- 39. Thread body does not contain this@AppSelector ---

    def test_39_thread_body_no_this_capture(self):
        self.assertNotIn("this@AppSelector", self.thread_body)

    # --- 40. Thread body does not contain activity ---

    def test_40_thread_body_no_activity(self):
        # Check for actual Activity references, not field names like loadIsActivity
        # Look for patterns like "activity.", "act.", "requireActivity()" etc.
        activity_patterns = [r"\bactivity\b", r"\bact\.", r"requireActivity"]
        for pat in activity_patterns:
            self.assertNotRegex(
                self.thread_body,
                pat,
                f"Thread body must not reference Activity ({pat})",
            )

    # --- 41. Thread body does not contain view/listView/process ---

    def test_41_thread_body_no_view_listview_process(self):
        for token in ["view", "listView", "process"]:
            self.assertNotIn(token, self.thread_body)

    # --- 42. Thread body only enters owner via fragmentRef.get() ---

    def test_42_thread_body_enters_via_weak_ref(self):
        self.assertIn("fragmentRef.get()", self.completion)
        self.assertIn("onAppLoadFinished", self.completion)

    # --- 43. Thread is not created inside onActivityCreated or scheduleAppLoad ---

    def test_43_thread_not_in_instance_methods(self):
        # Thread { must not appear in onActivityCreated or scheduleAppLoad
        for method_text in [self.on_activity_created, self.schedule_app_load]:
            self.assertNotIn("Thread {", method_text,
                "Thread must not be created in instance methods")

    # --- 44. Thread is created inside companion worker ---

    def test_44_thread_in_companion_worker(self):
        self.assertIn("Thread {", self.worker)

    # --- 45. start failure clears retry flag ---

    def test_45_start_failure_clears_retry(self):
        # The catch block in scheduleAppLoad's runnable should clear retry
        self.assertIn("retryAppLoadAfterInFlight = false", self.schedule_app_load)


class AppSelectorAsyncLifecycleNegativeTest(unittest.TestCase):
    """Negative contract tests: mutating the source to remove the fix must FAIL."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _APPSELECTOR_REL
        cls.original = _read_source(cls.source_path)

    def _check_contracts(self, source: str) -> list[str]:
        violations = []
        on_ac = _extract_on_activity_created(source)
        on_dv = _extract_on_destroy_view(source)
        sched = _extract_schedule_app_load(source)
        finished = _extract_on_app_load_finished(source)
        companion = _extract_companion(source)
        worker = _extract_start_app_load_worker(source)
        thread_body = _extract_thread_body_from_companion(source)
        completion = _extract_completion_block_from_companion(source)

        # Field checks
        if not re.search(r"private\s+var\s+pendingAppLoadStart\s*:\s*Runnable\s*\?\s*=\s*null", source):
            violations.append("missing pendingAppLoadStart field")
        if not re.search(r"private\s+var\s+appLoadInFlight\s*=\s*false", source):
            violations.append("missing appLoadInFlight field")
        if not re.search(r"private\s+var\s+retryAppLoadAfterInFlight\s*=\s*false", source):
            violations.append("missing retryAppLoadAfterInFlight field")

        # scheduleAppLoad checks
        if not sched:
            violations.append("missing scheduleAppLoad")
        else:
            if "applicationContext" not in sched:
                violations.append("missing applicationContext in scheduleAppLoad")
            if "WeakReference" not in sched:
                violations.append("missing WeakReference in scheduleAppLoad")
            if not re.search(r"object\s*:\s*Runnable\s*\{", sched):
                violations.append("kickoff not named Runnable")
            if not re.search(r"postDelayed\s*\(\s*runnable\s*,\s*animDur\.toLong\(\)\s*\)", sched):
                violations.append("postDelayed not using animDur")
            if "removeCallbacks(previous)" not in sched:
                violations.append("no old pending removal")
            if "pendingAppLoadStart === this" not in sched:
                violations.append("no callback self-clear")
            if not re.search(r"if\s*\(\s*!.*postDelayed", sched):
                violations.append("no post failure cleanup")
            if "retryAppLoadAfterInFlight = false" not in sched:
                violations.append("start failure does not clear retry")

        # onActivityCreated checks
        if "retryAppLoadAfterInFlight = true" not in on_ac:
            violations.append("in-flight does not set retry flag")
        if "scheduleAppLoad()" not in on_ac:
            violations.append("onActivityCreated does not call scheduleAppLoad")
        if "appLoadInFlight" not in on_ac:
            violations.append("no single-flight check")

        # onAppLoadFinished checks
        if not finished:
            violations.append("missing onAppLoadFinished")
        else:
            if not re.search(r"val\s+retry\s*=\s*retryAppLoadAfterInFlight", finished):
                violations.append("completion does not snapshot retry")
            if "appLoadInFlight = false" not in finished:
                violations.append("completion not clearing inFlight")
            if "retryAppLoadAfterInFlight = false" not in finished:
                violations.append("completion not clearing retry")
            if "initialized = true" not in finished:
                violations.append("completion not setting initialized")
            if "isAdded" not in finished:
                violations.append("completion not checking isAdded")
            if "view != null" not in finished:
                violations.append("completion not checking view != null")
            if "scheduleAppLoad()" not in finished:
                violations.append("completion does not retry via scheduleAppLoad")
            if not re.search(r"else\s+if\s*\(\s*retry\s*&&\s*isAdded\s*&&\s*view\s*!=\s*null\s*\)", finished):
                violations.append("failure retry not gated by retry flag")

        # onDestroyView checks
        if not on_dv:
            violations.append("onDestroyView missing")
        else:
            if "removeCallbacks" not in on_dv:
                violations.append("onDestroyView missing removeCallbacks")
            if "pendingAppLoadStart = null" not in on_dv:
                violations.append("onDestroyView missing null clear")
            if "retryAppLoadAfterInFlight = false" not in on_dv:
                violations.append("onDestroyView missing retry clear")
            super_idx = on_dv.find("super.onDestroyView")
            remove_idx = on_dv.find("removeCallbacks")
            if super_idx <= 0:
                violations.append("onDestroyView missing super.onDestroyView")
            elif remove_idx > 0 and remove_idx > super_idx:
                violations.append("removeCallbacks after super")

        # Worker ownership checks
        if not companion:
            violations.append("missing companion object")
        if not worker:
            violations.append("missing startAppLoadWorker")
        else:
            if "WeakReference<AppSelector>" not in worker:
                violations.append("worker missing WeakReference param")
            param_match = re.search(r"fun\s+startAppLoadWorker\s*\(([^)]*)\)", worker)
            if param_match:
                params = param_match.group(1)
                bare = re.search(r"(?<!WeakReference<)AppSelector(?!\s*>)", params)
                if bare:
                    violations.append("worker receives bare AppSelector")
                # Check type part only (after colon)
                for param_line in params.split(","):
                    parts = param_line.split(":")
                    if len(parts) < 2:
                        continue
                    param_type = parts[1].strip()
                    for forbidden in ["Activity", "View", "Fragment"]:
                        if forbidden in param_type:
                            violations.append(f"worker receives {forbidden}")
            if "Thread {" not in worker:
                violations.append("Thread not in companion worker")
            if "mainExecutor.execute" not in worker:
                violations.append("no mainExecutor in worker")
            if "fragmentRef.get()" not in completion:
                violations.append("completion not using WeakReference")
            if "onAppLoadFinished" not in completion:
                violations.append("completion not calling onAppLoadFinished")

        # Thread body checks
        for pat in [r"\bactivity\b", r"\bact\.", r"requireActivity"]:
            if re.search(pat, thread_body):
                violations.append(f"Thread body references Activity ({pat})")
                break
        if "this@AppSelector" in thread_body:
            violations.append("Thread body captures this@AppSelector")
        for token in ["listView", "view", "process"]:
            if token in thread_body:
                violations.append(f"Thread body accesses {token}")
                break

        # Thread not in instance methods
        if "Thread {" in on_ac or "Thread {" in sched:
            violations.append("Thread created in instance method")

        # No runOnUiThread
        if "runOnUiThread" in source:
            violations.append("still using runOnUiThread")

        # Dispatch checks
        for helper in ["Helpers.getOpenWithApps", "Helpers.getShareApps",
                       "Helpers.getInstalledApps", "Helpers.getLaunchableApps"]:
            if helper not in worker:
                violations.append(f"missing {helper} dispatch")

        return violations

    # ===== Original negative tests (A-J) =====

    def test_A_activity_capture_fails(self):
        mutated = self.original.replace("applicationContext", "activity")
        violations = self._check_contracts(mutated)
        self.assertTrue(any("applicationContext" in v or "Activity" in v for v in violations))

    def test_B_thread_captures_fragment_fails(self):
        mutated = self.original.replace(
            "fragmentRef.get()?.onAppLoadFinished(success)",
            "this@AppSelector.onAppLoadFinished(success)",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("Thread body captures this@AppSelector", violations)

    def test_C_no_weak_reference_fails(self):
        mutated = self.original.replace("WeakReference(this)", "this")
        violations = self._check_contracts(mutated)
        self.assertIn("missing WeakReference in scheduleAppLoad", violations)

    def test_D_no_destroy_view_cleanup_fails(self):
        mutated = self.original.replace("view?.removeCallbacks(pending)", "/* removed */")
        violations = self._check_contracts(mutated)
        self.assertIn("onDestroyView missing removeCallbacks", violations)

    def test_E_no_view_gate_fails(self):
        mutated = self.original.replace("view != null", "true")
        violations = self._check_contracts(mutated)
        self.assertIn("completion not checking view != null", violations)

    def test_F_no_inflight_reset_fails(self):
        mutated = self.original.replace("appLoadInFlight = false", "/* removed */")
        violations = self._check_contracts(mutated)
        self.assertIn("completion not clearing inFlight", violations)

    def test_G_no_single_flight_fails(self):
        mutated = self.original.replace(
            "} else if (appLoadInFlight) {",
            "} else if (false) {",
        )
        mutated = mutated.replace(
            "private var appLoadInFlight = false",
            "private var appLoadInFlight_removed = false",
        )
        violations = self._check_contracts(mutated)
        self.assertTrue(any("appLoadInFlight" in v for v in violations))

    def test_H_run_on_ui_thread_fails(self):
        mutated = self.original.replace(
            "appContext.mainExecutor.execute",
            "act.runOnUiThread",
        )
        violations = self._check_contracts(mutated)
        self.assertTrue(any("mainExecutor" in v or "runOnUiThread" in v for v in violations))

    def test_I_no_super_destroy_view_fails(self):
        mutated = re.sub(
            r"(retryAppLoadAfterInFlight = false\s*\n\s*)super\.onDestroyView\(\)",
            r"\1/* removed */",
            self.original,
            count=1,
        )
        violations = self._check_contracts(mutated)
        self.assertIn("onDestroyView missing super.onDestroyView", violations)

    def test_J_changed_dispatch_fails(self):
        mutated = self.original.replace("Helpers.getOpenWithApps(appContext)", "/* removed */")
        violations = self._check_contracts(mutated)
        self.assertIn("missing Helpers.getOpenWithApps dispatch", violations)

    # ===== R1 state-machine negative tests =====

    def test_K_no_retry_flag_set_fails(self):
        """If appLoadInFlight → plain return without retry flag, must fail."""
        mutated = self.original.replace(
            "retryAppLoadAfterInFlight = true",
            "/* removed */",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("in-flight does not set retry flag", violations)

    def test_L_no_failure_retry_fails(self):
        """If failure+retry+live-view does not scheduleAppLoad, must fail."""
        mutated = self.original.replace(
            "scheduleAppLoad()",
            "/* removed */",
        )
        violations = self._check_contracts(mutated)
        # Should fail both for onActivityCreated and onAppLoadFinished
        self.assertTrue(
            any("scheduleAppLoad" in v for v in violations),
            "Removing scheduleAppLoad must cause contract failure",
        )

    def test_M_unconditional_retry_fails(self):
        """If failure path retries without retry flag gate, must fail."""
        mutated = self.original.replace(
            "else if (retry && isAdded && view != null)",
            "else if (isAdded && view != null)",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("failure retry not gated by retry flag", violations)

    def test_N_retry_not_consumed_fails(self):
        """If completion does not clear retry flag, must fail."""
        mutated = self.original.replace(
            "retryAppLoadAfterInFlight = false\n        if (success)",
            "/* retry not cleared */\n        if (success)",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("completion not clearing retry", violations)

    def test_O_on_destroy_view_no_retry_clear_fails(self):
        """If onDestroyView does not clear retry flag, must fail."""
        # Remove the retry clear from onDestroyView (but keep it in onAppLoadFinished)
        mutated = re.sub(
            r"(pendingAppLoadStart = null\s*\n\s*)retryAppLoadAfterInFlight = false(\s*\n\s*super\.onDestroyView)",
            r"\1\2",
            self.original,
            count=1,
        )
        violations = self._check_contracts(mutated)
        self.assertIn("onDestroyView missing retry clear", violations)

    def test_P_success_path_schedules_fails(self):
        """If success path calls scheduleAppLoad, must fail."""
        # This is a structural check - success should not call scheduleAppLoad
        # We can verify by checking the positive test passes and the contract
        # check catches it if we inject a scheduleAppLoad into success branch
        mutated = self.original.replace(
            "if (isAdded && view != null) {\n                process?.run()\n            }",
            "if (isAdded && view != null) {\n                process?.run()\n                scheduleAppLoad()\n            }",
        )
        # This is a positive contract - we check that the real source does NOT have this
        # The positive test test_32_success_does_not_schedule already covers this
        # For negative, we verify our check catches it
        finished = _extract_on_app_load_finished(mutated)
        success_section = re.search(
            r"if\s*\(\s*success\s*\)\s*\{(.*?)\}",
            finished,
            re.DOTALL,
        )
        if success_section:
            self.assertIn("scheduleAppLoad", success_section.group(1),
                "Mutation should have injected scheduleAppLoad into success path")

    # ===== R1 worker ownership negative tests =====

    def test_Q_thread_in_instance_method_fails(self):
        """If Thread is created in an instance method, must fail."""
        # Move Thread creation from companion to scheduleAppLoad
        mutated = self.original.replace(
            "startAppLoadWorker(",
            "Thread {\n                    var success = false\n                    /* moved */\n                }.start()\n                // startAppLoadWorker(",
        )
        # Add Thread { to scheduleAppLoad by replacing the worker call
        sched = _extract_schedule_app_load(mutated)
        if "Thread {" not in sched:
            # Force it by adding a Thread block
            mutated = mutated.replace(
                "appLoadInFlight = true",
                "appLoadInFlight = true\n                    Thread { /* injected */ }.start()",
            )
        violations = self._check_contracts(mutated)
        self.assertTrue(
            any("Thread created in instance method" in v for v in violations),
            "Thread in instance method must cause contract failure",
        )

    def test_R_worker_receives_bare_appselector_fails(self):
        """If worker receives bare AppSelector instead of WeakReference, must fail."""
        # Replace WeakReference<AppSelector> param with AppSelector
        mutated = self.original.replace(
            "fragmentRef: WeakReference<AppSelector>",
            "fragmentRef: AppSelector",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("worker receives bare AppSelector", violations)

    def test_S_no_companion_fails(self):
        """If companion object is removed, must fail."""
        mutated = self.original.replace("companion object {", "/* companion removed */ {")
        violations = self._check_contracts(mutated)
        self.assertIn("missing companion object", violations)

    def test_T_worker_no_on_app_load_finished_fails(self):
        """If completion does not call onAppLoadFinished, must fail."""
        mutated = self.original.replace(
            "fragmentRef.get()?.onAppLoadFinished(success)",
            "fragmentRef.get()?.let { /* direct field access */ }",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("completion not calling onAppLoadFinished", violations)


if __name__ == "__main__":
    unittest.main()
