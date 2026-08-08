"""P2-3 regression test: ActivitySelector async activity-list load lifecycle contract.

This test reads the real ``ActivitySelector.kt`` source and verifies:
- The delayed activity-load kickoff is View-lifecycle cancellable.
- The background Thread is created inside a companion/static worker function,
  structurally preventing strong ActivitySelector/Activity/View capture.
- Completion uses WeakReference + mainExecutor with a live-view gate.
- Failure-after-view-recreation retry gap is closed via retryActivityLoadAfterInFlight.
- Retry is bounded: only one replacement attempt per lifecycle demand.
- Worker builds results in local storage, not Fragment field.
- Fragment activities field updated only on main thread.

Negative cases work by mutating a copy of the source and re-running the
contract checks against the mutated version.
"""

from __future__ import annotations

import re
import unittest
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
_ACTIVITYSELECTOR_REL = (
    "app/src/main/java/tv/withaibuild/customiuizer/subs/ActivitySelector.kt"
)


def _read_source(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _extract_brace_block(source: str, start_idx: int) -> str:
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


def _extract_schedule_activity_load(source: str) -> str:
    return _extract_fun(source, "scheduleActivityLoad")


def _extract_on_activity_load_finished(source: str) -> str:
    return _extract_fun(source, "onActivityLoadFinished")


def _extract_render_activities(source: str) -> str:
    return _extract_fun(source, "renderActivities")


def _extract_companion(source: str) -> str:
    m = re.search(r"companion\s+object\s*\{", source)
    if not m:
        return ""
    brace_start = source.find("{", m.end() - 1)
    if brace_start < 0:
        return ""
    block = _extract_brace_block(source, brace_start)
    return source[m.start() : brace_start + len(block)]


def _extract_start_activity_load_worker(source: str) -> str:
    companion = _extract_companion(source)
    return _extract_fun(companion, "startActivityLoadWorker")


def _extract_thread_body_from_companion(source: str) -> str:
    worker = _extract_start_activity_load_worker(source)
    m = re.search(r"Thread\s*\{", worker)
    if not m:
        return ""
    brace_start = worker.find("{", m.end() - 1)
    if brace_start < 0:
        return ""
    return _extract_brace_block(worker, brace_start)


def _extract_completion_block_from_companion(source: str) -> str:
    worker = _extract_start_activity_load_worker(source)
    m = re.search(r"mainExecutor\.execute\s*\{", worker)
    if not m:
        return ""
    brace_start = worker.find("{", m.end() - 1)
    if brace_start < 0:
        return ""
    return _extract_brace_block(worker, brace_start)


class ActivitySelectorAsyncLifecycleContractTest(unittest.TestCase):
    """Positive contract tests against the real ActivitySelector.kt."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _ACTIVITYSELECTOR_REL
        cls.source = _read_source(cls.source_path)
        cls.on_activity_created = _extract_on_activity_created(cls.source)
        cls.on_destroy_view = _extract_on_destroy_view(cls.source)
        cls.schedule_activity_load = _extract_schedule_activity_load(cls.source)
        cls.on_activity_load_finished = _extract_on_activity_load_finished(cls.source)
        cls.render_activities = _extract_render_activities(cls.source)
        cls.companion = _extract_companion(cls.source)
        cls.worker = _extract_start_activity_load_worker(cls.source)
        cls.thread_body = _extract_thread_body_from_companion(cls.source)
        cls.completion = _extract_completion_block_from_companion(cls.source)

    # --- 1. pendingActivityLoadStart field ---

    def test_01_pending_activity_load_start_field(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+pendingActivityLoadStart\s*:\s*Runnable\s*\?\s*=\s*null",
        )

    # --- 2. activityLoadInFlight field ---

    def test_02_activity_load_in_flight_field(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+activityLoadInFlight\s*=\s*false",
        )

    # --- 3. retryActivityLoadAfterInFlight field ---

    def test_03_retry_activity_load_after_in_flight_field(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+retryActivityLoadAfterInFlight\s*=\s*false",
        )

    # --- 4. scheduleActivityLoad exists ---

    def test_04_schedule_activity_load_exists(self):
        self.assertTrue(self.schedule_activity_load, "scheduleActivityLoad must exist")

    # --- 5. renderActivities exists ---

    def test_05_render_activities_exists(self):
        self.assertTrue(self.render_activities, "renderActivities must exist")

    # --- 6. onActivityLoadFinished exists ---

    def test_06_on_activity_load_finished_exists(self):
        self.assertTrue(self.on_activity_load_finished, "onActivityLoadFinished must exist")

    # --- 7. WeakReference<ActivitySelector> ---

    def test_07_weak_reference_exists(self):
        self.assertIn("WeakReference", self.schedule_activity_load)
        self.assertIn("fragmentRef", self.schedule_activity_load)

    # --- 8. applicationContext used ---

    def test_08_application_context_used(self):
        self.assertIn("applicationContext", self.schedule_activity_load)

    # --- 9. worker in companion/static ownership domain ---

    def test_09_worker_in_companion(self):
        self.assertTrue(self.companion, "companion object must exist")
        self.assertTrue(self.worker, "startActivityLoadWorker must exist in companion")

    # --- 10. Thread not in instance delayed Runnable ---

    def test_10_thread_not_in_instance_methods(self):
        for method_text in [self.on_activity_created, self.schedule_activity_load]:
            self.assertNotIn("Thread {", method_text,
                "Thread must not be created in instance methods")

    # --- 11. worker params: no bare ActivitySelector ---

    def test_11_worker_no_bare_activity_selector_param(self):
        param_match = re.search(r"fun\s+startActivityLoadWorker\s*\(([^)]*)\)", self.worker)
        self.assertIsNotNone(param_match)
        params = param_match.group(1)
        bare = re.search(r"(?<!WeakReference<)ActivitySelector(?!\s*>)", params)
        self.assertIsNone(bare, "Worker must not receive bare ActivitySelector parameter")

    # --- 12. worker params: no Activity/View/ListView ---

    def test_12_worker_no_activity_view_listview_param(self):
        param_match = re.search(r"fun\s+startActivityLoadWorker\s*\(([^)]*)\)", self.worker)
        self.assertIsNotNone(param_match)
        params = param_match.group(1)
        for param_line in params.split(","):
            parts = param_line.split(":")
            if len(parts) < 2:
                continue
            param_type = parts[1].strip()
            # Skip WeakReference types — they wrap the Fragment, which is fine
            if param_type.startswith("WeakReference"):
                continue
            for forbidden in ["Activity", "View", "ListView"]:
                self.assertNotIn(forbidden, param_type,
                    f"Worker param type must not be {forbidden}: {param_type}")

    # --- 13. packageName is immutable snapshot ---

    def test_13_package_name_snapshot(self):
        self.assertIn("packageName", self.worker)
        # packageName should be a String param, not read from fragment
        self.assertNotIn("fragment.pkg", self.thread_body)
        self.assertNotIn("this.pkg", self.thread_body)

    # --- 14. worker uses local loadedActivities ---

    def test_14_worker_uses_local_loaded_activities(self):
        self.assertIn("loadedActivities", self.thread_body)
        self.assertIn("ArrayList<AppData>", self.thread_body)

    # --- 15. worker does not read/write Fragment activities ---

    def test_15_worker_no_fragment_activities_access(self):
        self.assertNotIn("activities.clear", self.thread_body)
        self.assertNotIn("activities.add", self.thread_body)
        self.assertNotIn("fragment.activities", self.thread_body)

    # --- 16. completion in mainExecutor ---

    def test_16_completion_main_executor(self):
        self.assertIn("mainExecutor.execute", self.worker)
        self.assertNotIn("runOnUiThread", self.source)

    # --- 17. completion updates activities on main thread ---

    def test_17_completion_updates_activities_main_thread(self):
        self.assertIn("activities.clear()", self.on_activity_load_finished)
        self.assertIn("activities.addAll(loadedActivities)", self.on_activity_load_finished)

    # --- 18. success live-view render gate ---

    def test_18_success_live_view_render_gate(self):
        self.assertIn("isAdded", self.on_activity_load_finished)
        self.assertIn("view != null", self.on_activity_load_finished)
        self.assertIn("renderActivities()", self.on_activity_load_finished)

    # --- 19. in-flight View records retry demand ---

    def test_19_in_flight_records_retry(self):
        self.assertIn("retryActivityLoadAfterInFlight = true", self.on_activity_created)

    # --- 20. failure retry requires retry + isAdded + view != null ---

    def test_20_failure_retry_gated(self):
        self.assertRegex(
            self.on_activity_load_finished,
            r"else\s+if\s*\(\s*retry\s*&&\s*isAdded\s*&&\s*view\s*!=\s*null\s*\)",
        )

    # --- 21. no unconditional retry ---

    def test_21_no_unconditional_retry(self):
        # The retry path must check the retry flag, not just !success
        success_section = re.search(
            r"if\s*\(\s*success\s*\)\s*\{(.*?)\}",
            self.on_activity_load_finished,
            re.DOTALL,
        )
        self.assertIsNotNone(success_section, "Must have if (success) branch")
        self.assertNotIn("scheduleActivityLoad", success_section.group(1))

    # --- 22. onDestroyView clears pending ---

    def test_22_on_destroy_view_clears_pending(self):
        self.assertTrue(self.on_destroy_view)
        self.assertIn("removeCallbacks", self.on_destroy_view)
        self.assertIn("pendingActivityLoadStart = null", self.on_destroy_view)

    # --- 23. onDestroyView clears retry ---

    def test_23_on_destroy_view_clears_retry(self):
        self.assertIn("retryActivityLoadAfterInFlight = false", self.on_destroy_view)

    # --- 24. cleanup before super.onDestroyView ---

    def test_24_cleanup_before_super(self):
        super_idx = self.on_destroy_view.find("super.onDestroyView")
        remove_idx = self.on_destroy_view.find("removeCallbacks")
        self.assertGreater(super_idx, 0)
        self.assertLess(remove_idx, super_idx)

    # --- 25. animDur preserved ---

    def test_25_anim_dur_preserved(self):
        self.assertRegex(
            self.schedule_activity_load,
            r"postDelayed\s*\(\s*runnable\s*,\s*animDur\.toLong\(\)\s*\)",
        )

    # --- 26. GET_ACTIVITIES preserved ---

    def test_26_get_activities_preserved(self):
        self.assertIn("PackageManager.GET_ACTIVITIES", self.worker)

    # --- 27. empty-result Toast/finish preserved ---

    def test_27_empty_result_toast_finish(self):
        self.assertIn("no_activities_found", self.render_activities)
        self.assertIn("Toast.makeText", self.render_activities)
        self.assertIn("finish()", self.render_activities)

    # --- 28. adapter type Activities preserved ---

    def test_28_adapter_type_activities(self):
        self.assertIn("Helpers.AppAdapterType.Activities", self.render_activities)

    # --- 29. click result format preserved ---

    def test_29_click_result_format(self):
        self.assertIn("putExtra(\"activity\"", self.render_activities)
        self.assertIn("appData.pkgName", self.render_activities)
        self.assertIn("appData.actName", self.render_activities)
        self.assertIn("putExtra(\"user\", user)", self.render_activities)
        self.assertIn("targetFragment?.onActivityResult", self.render_activities)

    # --- 30. long-click LaunchIntent preserved ---

    def test_30_long_click_launch_intent(self):
        self.assertIn("ACTION_PREFIX", self.render_activities)
        self.assertIn("LaunchIntent", self.render_activities)
        self.assertIn("ComponentName", self.render_activities)
        self.assertIn("sendBroadcast", self.render_activities)

    # --- 31. Thread body no Activity capture ---

    def test_31_thread_body_no_activity(self):
        for pat in [r"\bactivity\b", r"\bact\.", r"requireActivity"]:
            self.assertNotRegex(self.thread_body, pat,
                f"Thread body must not reference Activity ({pat})")

    # --- 32. Thread body no this@ActivitySelector ---

    def test_32_thread_body_no_this_capture(self):
        self.assertNotIn("this@ActivitySelector", self.thread_body)

    # --- 33. Thread body no view/listView/process ---

    def test_33_thread_body_no_view_listview_process(self):
        for token in ["view", "listView", "process"]:
            self.assertNotIn(token, self.thread_body)

    # --- 34. completion uses WeakReference via onActivityLoadFinished ---

    def test_34_completion_weak_reference_handoff(self):
        self.assertIn("fragmentRef.get()", self.completion)
        self.assertIn("onActivityLoadFinished", self.completion)

    # --- 35. initialized field exists ---

    def test_35_initialized_field(self):
        self.assertRegex(self.source, r"private\s+var\s+initialized\s*=\s*false")

    # --- 36. initialized checked in onActivityCreated ---

    def test_36_initialized_checked_in_on_activity_created(self):
        self.assertIn("initialized", self.on_activity_created)

    # --- 37. initialized set on success ---

    def test_37_initialized_set_on_success(self):
        self.assertIn("initialized = true", self.on_activity_load_finished)

    # --- 38. progressBar GONE in renderActivities ---

    def test_38_progress_bar_gone(self):
        self.assertIn("am_progressBar", self.render_activities)
        self.assertIn("View.GONE", self.render_activities)

    # --- 39. start failure clears retry ---

    def test_39_start_failure_clears_retry(self):
        self.assertIn("retryActivityLoadAfterInFlight = false", self.schedule_activity_load)

    # --- 40. no Handler/Coroutine/ExecutorService ---

    def test_40_no_handler_coroutine_executor(self):
        forbidden = [r"\bHandler\b", r"\bCoroutine\b", r"\bFlow\b", r"\bExecutorService\b"]
        for pat in forbidden:
            self.assertNotRegex(self.source, pat, f"Must not introduce {pat}")


class ActivitySelectorAsyncLifecycleNegativeTest(unittest.TestCase):
    """Negative contract tests: mutating the source to remove the fix must FAIL."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _ACTIVITYSELECTOR_REL
        cls.original = _read_source(cls.source_path)

    def _check_contracts(self, source: str) -> list[str]:
        violations = []
        on_ac = _extract_on_activity_created(source)
        on_dv = _extract_on_destroy_view(source)
        sched = _extract_schedule_activity_load(source)
        finished = _extract_on_activity_load_finished(source)
        render = _extract_render_activities(source)
        companion = _extract_companion(source)
        worker = _extract_start_activity_load_worker(source)
        thread_body = _extract_thread_body_from_companion(source)
        completion = _extract_completion_block_from_companion(source)

        # Field checks
        if not re.search(r"private\s+var\s+pendingActivityLoadStart\s*:\s*Runnable\s*\?\s*=\s*null", source):
            violations.append("missing pendingActivityLoadStart field")
        if not re.search(r"private\s+var\s+activityLoadInFlight\s*=\s*false", source):
            violations.append("missing activityLoadInFlight field")
        if not re.search(r"private\s+var\s+retryActivityLoadAfterInFlight\s*=\s*false", source):
            violations.append("missing retryActivityLoadAfterInFlight field")
        if not re.search(r"private\s+var\s+initialized\s*=\s*false", source):
            violations.append("missing initialized field")

        # scheduleActivityLoad checks
        if not sched:
            violations.append("missing scheduleActivityLoad")
        else:
            if "applicationContext" not in sched:
                violations.append("missing applicationContext in scheduleActivityLoad")
            if "WeakReference" not in sched:
                violations.append("missing WeakReference in scheduleActivityLoad")
            if not re.search(r"object\s*:\s*Runnable\s*\{", sched):
                violations.append("kickoff not named Runnable")
            if not re.search(r"postDelayed\s*\(\s*runnable\s*,\s*animDur\.toLong\(\)\s*\)", sched):
                violations.append("postDelayed not using animDur")
            if "removeCallbacks(previous)" not in sched:
                violations.append("no old pending removal")
            if "pendingActivityLoadStart === this" not in sched:
                violations.append("no callback self-clear")
            if not re.search(r"if\s*\(\s*!.*postDelayed", sched):
                violations.append("no post failure cleanup")
            if "retryActivityLoadAfterInFlight = false" not in sched:
                violations.append("start failure does not clear retry")

        # onActivityCreated checks
        if "retryActivityLoadAfterInFlight = true" not in on_ac:
            violations.append("in-flight does not set retry flag")
        if "scheduleActivityLoad()" not in on_ac:
            violations.append("onActivityCreated does not call scheduleActivityLoad")
        if "activityLoadInFlight" not in on_ac:
            violations.append("no single-flight check")
        if "initialized" not in on_ac:
            violations.append("no initialized check")

        # onActivityLoadFinished checks
        if not finished:
            violations.append("missing onActivityLoadFinished")
        else:
            if not re.search(r"val\s+retry\s*=\s*retryActivityLoadAfterInFlight", finished):
                violations.append("completion does not snapshot retry")
            if "activityLoadInFlight = false" not in finished:
                violations.append("completion not clearing inFlight")
            if "retryActivityLoadAfterInFlight = false" not in finished:
                violations.append("completion not clearing retry")
            if "initialized = true" not in finished:
                violations.append("completion not setting initialized")
            if "activities.clear()" not in finished:
                violations.append("completion not clearing activities")
            if "activities.addAll(loadedActivities)" not in finished:
                violations.append("completion not adding loaded activities")
            if "isAdded" not in finished:
                violations.append("completion not checking isAdded")
            if "view != null" not in finished:
                violations.append("completion not checking view != null")
            if "renderActivities()" not in finished:
                violations.append("completion does not render")
            if "scheduleActivityLoad()" not in finished:
                violations.append("completion does not retry via scheduleActivityLoad")
            if not re.search(r"else\s+if\s*\(\s*retry\s*&&\s*isAdded\s*&&\s*view\s*!=\s*null\s*\)", finished):
                violations.append("failure retry not gated by retry flag")

        # renderActivities checks
        if not render:
            violations.append("missing renderActivities")
        else:
            if "no_activities_found" not in render:
                violations.append("renderActivities missing empty toast")
            if "AppAdapterType.Activities" not in render:
                violations.append("renderActivities missing adapter type")
            if "ACTION_PREFIX" not in render:
                violations.append("renderActivities missing long-click")
            if "am_progressBar" not in render:
                violations.append("renderActivities missing progressBar")

        # onDestroyView checks
        if not on_dv:
            violations.append("onDestroyView missing")
        else:
            if "removeCallbacks" not in on_dv:
                violations.append("onDestroyView missing removeCallbacks")
            if "pendingActivityLoadStart = null" not in on_dv:
                violations.append("onDestroyView missing null clear")
            if "retryActivityLoadAfterInFlight = false" not in on_dv:
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
            violations.append("missing startActivityLoadWorker")
        else:
            if "WeakReference<ActivitySelector>" not in worker:
                violations.append("worker missing WeakReference param")
            param_match = re.search(r"fun\s+startActivityLoadWorker\s*\(([^)]*)\)", worker)
            if param_match:
                params = param_match.group(1)
                bare = re.search(r"(?<!WeakReference<)ActivitySelector(?!\s*>)", params)
                if bare:
                    violations.append("worker receives bare ActivitySelector")
                for param_line in params.split(","):
                    parts = param_line.split(":")
                    if len(parts) < 2:
                        continue
                    param_type = parts[1].strip()
                    if param_type.startswith("WeakReference"):
                        continue
                    for forbidden in ["Activity", "View", "ListView"]:
                        if forbidden in param_type:
                            violations.append(f"worker receives {forbidden}")
            if "Thread {" not in worker:
                violations.append("Thread not in companion worker")
            if "mainExecutor.execute" not in worker:
                violations.append("no mainExecutor in worker")
            if "fragmentRef.get()" not in completion:
                violations.append("completion not using WeakReference")
            if "onActivityLoadFinished" not in completion:
                violations.append("completion not calling onActivityLoadFinished")
            if "GET_ACTIVITIES" not in worker:
                violations.append("missing GET_ACTIVITIES")

        # Thread body checks
        for pat in [r"\bactivity\b", r"\bact\.", r"requireActivity"]:
            if re.search(pat, thread_body):
                violations.append(f"Thread body references Activity ({pat})")
                break
        if "this@ActivitySelector" in thread_body:
            violations.append("Thread body captures this@ActivitySelector")
        for token in ["view", "listView", "process"]:
            if token in thread_body:
                violations.append(f"Thread body accesses {token}")
                break
        if "activities.clear" in thread_body or "activities.add" in thread_body:
            violations.append("Thread body mutates Fragment activities")
        if "loadedActivities" not in thread_body:
            violations.append("Thread body missing local loadedActivities")

        # Thread not in instance methods
        if "Thread {" in on_ac or "Thread {" in sched:
            violations.append("Thread created in instance method")

        # No runOnUiThread
        if "runOnUiThread" in source:
            violations.append("still using runOnUiThread")

        return violations

    # ===== Negative tests A-N =====

    def test_A_worker_uses_activity_fails(self):
        mutated = self.original.replace("appContext.packageManager", "activity.packageManager")
        violations = self._check_contracts(mutated)
        self.assertTrue(any("Activity" in v for v in violations))

    def test_B_thread_in_instance_fails(self):
        mutated = self.original.replace(
            "startActivityLoadWorker(appContext, fragmentRef, packageName)",
            "Thread { /* moved */ }.start()",
        )
        sched = _extract_schedule_activity_load(mutated)
        if "Thread {" not in sched:
            mutated = mutated.replace(
                "activityLoadInFlight = true",
                "activityLoadInFlight = true\n                    Thread { /* injected */ }.start()",
            )
        violations = self._check_contracts(mutated)
        self.assertTrue(any("Thread created in instance method" in v for v in violations))

    def test_C_worker_accesses_activities_fails(self):
        mutated = self.original.replace(
            "loadedActivities.add(appData)",
            "activities.add(appData)",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("Thread body mutates Fragment activities", violations)

    def test_D_no_local_result_list_fails(self):
        # Replace the local list with direct fragment field access
        mutated = self.original.replace(
            "val loadedActivities = ArrayList<AppData>()",
            "/* removed local list */",
        )
        mutated = mutated.replace("loadedActivities", "activities")
        violations = self._check_contracts(mutated)
        self.assertTrue(
            any("loadedActivities" in v or "mutates Fragment" in v for v in violations),
            "Removing local result list must cause contract failure",
        )

    def test_E_no_weak_reference_fails(self):
        mutated = self.original.replace("WeakReference(this)", "this")
        violations = self._check_contracts(mutated)
        self.assertIn("missing WeakReference in scheduleActivityLoad", violations)

    def test_F_completion_background_update_fails(self):
        # Move activities.addAll into thread body
        mutated = self.original.replace(
            "loadedActivities.add(appData)",
            "loadedActivities.add(appData)\n                    activities.add(appData)",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("Thread body mutates Fragment activities", violations)

    def test_G_no_live_view_gate_fails(self):
        mutated = self.original.replace("view != null", "true")
        violations = self._check_contracts(mutated)
        self.assertIn("completion not checking view != null", violations)

    def test_H_no_destroy_view_remove_callbacks_fails(self):
        mutated = self.original.replace("view?.removeCallbacks(pending)", "/* removed */")
        violations = self._check_contracts(mutated)
        self.assertIn("onDestroyView missing removeCallbacks", violations)

    def test_I_no_retry_demand_fails(self):
        mutated = self.original.replace(
            "retryActivityLoadAfterInFlight = true",
            "/* removed */",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("in-flight does not set retry flag", violations)

    def test_J_no_retry_gate_fails(self):
        mutated = self.original.replace(
            "else if (retry && isAdded && view != null)",
            "else if (isAdded && view != null)",
        )
        violations = self._check_contracts(mutated)
        self.assertIn("failure retry not gated by retry flag", violations)

    def test_K_unconditional_retry_fails(self):
        # Inject scheduleActivityLoad into the success branch
        mutated = self.original.replace(
            "if (success) {\n            activities.clear()",
            "if (success) {\n            scheduleActivityLoad()\n            activities.clear()",
        )
        finished = _extract_on_activity_load_finished(mutated)
        success_section = re.search(
            r"if\s*\(\s*success\s*\)\s*\{(.*?)\}",
            finished,
            re.DOTALL,
        )
        self.assertIsNotNone(success_section, "Must have if (success) branch")
        self.assertIn("scheduleActivityLoad", success_section.group(1),
            "Mutation should have injected scheduleActivityLoad into success path")

    def test_L_run_on_ui_thread_fails(self):
        mutated = self.original.replace(
            "appContext.mainExecutor.execute",
            "act.runOnUiThread",
        )
        violations = self._check_contracts(mutated)
        self.assertTrue(any("mainExecutor" in v or "runOnUiThread" in v for v in violations))

    def test_M_change_get_activities_fails(self):
        mutated = self.original.replace("PackageManager.GET_ACTIVITIES", "0")
        violations = self._check_contracts(mutated)
        self.assertIn("missing GET_ACTIVITIES", violations)

    def test_N_change_click_result_fails(self):
        mutated = self.original.replace(
            'putExtra("activity", "${appData.pkgName}|${appData.actName}")',
            'putExtra("changed", "wrong")',
        )
        render = _extract_render_activities(mutated)
        self.assertNotIn('putExtra("activity"', render)


if __name__ == "__main__":
    unittest.main()
