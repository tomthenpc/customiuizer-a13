"""P2-1 regression test: SubFragment delayed highlight scroll lifecycle contract.

This test reads the real ``SubFragment.kt`` source and verifies that the
delayed highlight-scroll callback is properly bounded by the Fragment View
lifecycle.  It checks both positive contracts (the fix is present) and
negative contracts (removing the fix causes failures).

Negative cases work by mutating a temporary copy of the source and re-running
the contract checks against the mutated version.
"""

from __future__ import annotations

import re
import tempfile
import unittest
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
_SUBFRAGMENT_REL = (
    "app/src/main/java/tv/withaibuild/customiuizer/SubFragment.kt"
)


def _read_source(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def _extract_on_start(source: str) -> str:
    """Return the body of ``onStart()`` up to the closing brace."""
    m = re.search(r"override\s+fun\s+onStart\s*\(\s*\)\s*\{", source)
    if not m:
        return ""
    start = m.start()
    depth = 0
    for i in range(m.end() - 1, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[start : i + 1]
    return ""


def _extract_on_destroy_view(source: str) -> str:
    """Return the body of ``onDestroyView()`` up to the closing brace."""
    m = re.search(r"override\s+fun\s+onDestroyView\s*\(\s*\)\s*\{", source)
    if not m:
        return ""
    start = m.start()
    depth = 0
    for i in range(m.end() - 1, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[start : i + 1]
    return ""


class SubFragmentLifecycleContractTest(unittest.TestCase):
    """Positive contract tests against the real SubFragment.kt."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _SUBFRAGMENT_REL
        cls.source = _read_source(cls.source_path)
        cls.on_start = _extract_on_start(cls.source)
        cls.on_destroy_view = _extract_on_destroy_view(cls.source)

    # --- 1. pending Runnable field ---

    def test_pending_highlight_scroll_field_exists(self):
        self.assertRegex(
            self.source,
            r"private\s+var\s+pendingHighlightScroll\s*:\s*Runnable\s*\?\s*=\s*null",
            "SubFragment must declare a private var pendingHighlightScroll: Runnable? = null",
        )

    # --- 2. delayed callback is identifiable, not fully anonymous ---

    def test_callback_is_named_runnable(self):
        self.assertRegex(
            self.on_start,
            r"object\s*:\s*Runnable\s*\{",
            "Delayed callback must be an object : Runnable, not a bare lambda",
        )

    # --- 3. postDelayed 380 preserved ---

    def test_post_delayed_380_preserved(self):
        self.assertRegex(
            self.on_start,
            r"postDelayed\s*\(\s*runnable\s*,\s*380\s*\)",
            "postDelayed(runnable, 380) must be preserved",
        )

    # --- 4. onDestroyView exists ---

    def test_on_destroy_view_exists(self):
        self.assertTrue(
            self.on_destroy_view,
            "onDestroyView override must exist",
        )

    # --- 5. onDestroyView calls removeCallbacks ---

    def test_on_destroy_view_calls_remove_callbacks(self):
        self.assertIn("removeCallbacks", self.on_destroy_view)

    # --- 6. pending field set to null in onDestroyView ---

    def test_pending_field_nulled_in_on_destroy_view(self):
        self.assertRegex(
            self.on_destroy_view,
            r"pendingHighlightScroll\s*=\s*null",
            "onDestroyView must set pendingHighlightScroll = null",
        )

    # --- 7. cleanup before super.onDestroyView ---

    def test_cleanup_before_super_on_destroy_view(self):
        super_idx = self.on_destroy_view.find("super.onDestroyView")
        remove_idx = self.on_destroy_view.find("removeCallbacks")
        null_idx = self.on_destroy_view.find("pendingHighlightScroll = null")
        self.assertGreater(super_idx, 0, "super.onDestroyView must be called")
        self.assertGreater(
            remove_idx, 0, "removeCallbacks must be present"
        )
        self.assertGreater(
            null_idx, 0, "pendingHighlightScroll = null must be present"
        )
        self.assertLess(
            remove_idx,
            super_idx,
            "removeCallbacks must happen before super.onDestroyView()",
        )
        self.assertLess(
            null_idx,
            super_idx,
            "pendingHighlightScroll = null must happen before super.onDestroyView()",
        )

    # --- 8. Runnable self-clears pending slot after execution ---

    def test_runnable_self_clears_pending_slot(self):
        self.assertIn("finally", self.on_start)
        self.assertRegex(
            self.on_start,
            r"pendingHighlightScroll\s*===\s*this",
            "Runnable must check pendingHighlightScroll === this before clearing",
        )
        self.assertRegex(
            self.on_start,
            r"finally\s*\{[^}]*pendingHighlightScroll\s*=\s*null",
            "Runnable must clear pendingHighlightScroll in a finally block",
        )

    # --- 9. position threshold remains 9 ---

    def test_position_threshold_is_9(self):
        self.assertRegex(
            self.on_start,
            r"position\s*<\s*9",
            "position < 9 threshold must be preserved",
        )

    # --- 10. highlightKey = null before schedule ---

    def test_highlight_key_nulled_before_schedule(self):
        null_idx = self.on_start.find("highlightKey = null")
        post_idx = self.on_start.find("postDelayed")
        self.assertGreater(null_idx, 0, "highlightKey = null must be present")
        self.assertGreater(post_idx, 0, "postDelayed must be present")
        self.assertLess(
            null_idx,
            post_idx,
            "highlightKey = null must occur before postDelayed",
        )

    # --- 11. duplicate pending cancellation ---

    def test_duplicate_pending_cancellation(self):
        self.assertRegex(
            self.on_start,
            r"pendingHighlightScroll\s*\?\.",
            "onStart must check/cancel previous pendingHighlightScroll before scheduling",
        )
        self.assertRegex(
            self.on_start,
            r"removeCallbacks\s*\(\s*previous\s*\)",
            "onStart must removeCallbacks(previous) before scheduling new",
        )

    # --- 12. post failure cleanup ---

    def test_post_failure_cleanup(self):
        self.assertRegex(
            self.on_start,
            r"if\s*\(\s*!.*postDelayed",
            "onStart must check postDelayed return value",
        )

    # --- 13. SNAP_TO_START preserved ---

    def test_snap_to_start_preserved(self):
        self.assertIn("SNAP_TO_START", self.on_start)

    # --- 14. No Handler/Coroutine/Thread/Executor introduced ---

    def test_no_handler_or_coroutine_introduced(self):
        forbidden = [
            r"\bHandler\b",
            r"\bLooper\b",
            r"\bCoroutine\b",
            r"\bFlow\b",
            r"\bThread\b",
            r"\bExecutor\b",
            r"\bTimer\b",
        ]
        for pat in forbidden:
            self.assertNotRegex(
                self.on_start,
                pat,
                f"onStart must not introduce {pat}",
            )

    # --- 15. No onPause-based cleanup ---

    def test_no_on_pause_cleanup(self):
        self.assertNotRegex(
            self.source,
            r"override\s+fun\s+onPause\s*\(\s*\)\s*\{[^}]*removeCallbacks",
            "Cleanup must not be in onPause; it must be in onDestroyView",
        )


class SubFragmentLifecycleNegativeTest(unittest.TestCase):
    """Negative contract tests: mutating the source to remove the fix must FAIL."""

    @classmethod
    def setUpClass(cls):
        cls.source_path = _REPO_ROOT / _SUBFRAGMENT_REL
        cls.original = _read_source(cls.source_path)

    def _check_contracts(self, source: str) -> list[str]:
        """Return a list of contract violations (empty = all pass)."""
        violations = []
        on_start = _extract_on_start(source)
        on_destroy = _extract_on_destroy_view(source)

        if not re.search(r"private\s+var\s+pendingHighlightScroll\s*:\s*Runnable\s*\?\s*=\s*null", source):
            violations.append("missing pendingHighlightScroll field")
        if not re.search(r"object\s*:\s*Runnable\s*\{", on_start):
            violations.append("callback is not a named Runnable")
        if not re.search(r"postDelayed\s*\(\s*runnable\s*,\s*380\s*\)", on_start):
            violations.append("postDelayed(runnable, 380) missing")
        if not on_destroy:
            violations.append("onDestroyView missing")
        if "removeCallbacks" not in on_destroy:
            violations.append("onDestroyView missing removeCallbacks")
        if not re.search(r"pendingHighlightScroll\s*=\s*null", on_destroy):
            violations.append("onDestroyView missing pendingHighlightScroll = null")
        super_idx = on_destroy.find("super.onDestroyView")
        remove_idx = on_destroy.find("removeCallbacks")
        if super_idx > 0 and remove_idx > 0 and remove_idx > super_idx:
            violations.append("removeCallbacks after super.onDestroyView")
        if "finally" not in on_start or "pendingHighlightScroll = null" not in on_start:
            violations.append("Runnable does not self-clear pending slot")
        if not re.search(r"position\s*<\s*9", on_start):
            violations.append("position threshold changed from 9")
        null_idx = on_start.find("highlightKey = null")
        post_idx = on_start.find("postDelayed")
        if null_idx > 0 and post_idx > 0 and null_idx > post_idx:
            violations.append("highlightKey = null after schedule")
        if not re.search(r"pendingHighlightScroll\s*\?\.", on_start):
            violations.append("no duplicate pending cancellation")
        if not re.search(r"if\s*\(\s*!.*postDelayed", on_start):
            violations.append("no post failure cleanup")
        return violations

    def test_missing_cleanup_fails(self):
        """If removeCallbacks is removed from onDestroyView, contracts must fail."""
        mutated = self.original.replace("view?.removeCallbacks(pending)", "/* removed */")
        violations = self._check_contracts(mutated)
        self.assertIn(
            "onDestroyView missing removeCallbacks",
            violations,
            "Removing removeCallbacks must cause contract failure",
        )

    def test_wrong_lifecycle_fails(self):
        """If cleanup is only in onPause (not onDestroyView), contracts must fail."""
        # Remove onDestroyView entirely
        mutated = re.sub(
            r"override\s+fun\s+onDestroyView\s*\(\s*\)\s*\{.*?\n\s*\}",
            "",
            self.original,
            count=1,
            flags=re.DOTALL,
        )
        violations = self._check_contracts(mutated)
        self.assertIn(
            "onDestroyView missing",
            violations,
            "Removing onDestroyView must cause contract failure",
        )

    def test_no_self_clear_fails(self):
        """If the Runnable does not self-clear, contracts must fail."""
        # Remove the finally block self-clear
        mutated = self.original.replace(
            """try {
                    mList.layoutManager?.startSmoothScroll(smoothScroller)
                } finally {
                    if (pendingHighlightScroll === this) {
                        pendingHighlightScroll = null
                    }
                }""",
            """mList.layoutManager?.startSmoothScroll(smoothScroller)""",
        )
        violations = self._check_contracts(mutated)
        self.assertIn(
            "Runnable does not self-clear pending slot",
            violations,
            "Removing self-clear must cause contract failure",
        )

    def test_changed_delay_fails(self):
        """If 380 is changed to another value, contracts must fail."""
        mutated = self.original.replace("postDelayed(runnable, 380)", "postDelayed(runnable, 300)")
        violations = self._check_contracts(mutated)
        self.assertIn(
            "postDelayed(runnable, 380) missing",
            violations,
            "Changing 380 must cause contract failure",
        )

    def test_cleanup_after_super_fails(self):
        """If removeCallbacks happens after super.onDestroyView, contracts must fail."""
        # Reorder: super first, then cleanup
        mutated = self.original.replace(
            """pendingHighlightScroll?.let { pending ->
            view?.removeCallbacks(pending)
        }
        pendingHighlightScroll = null
        super.onDestroyView()""",
            """super.onDestroyView()
        pendingHighlightScroll?.let { pending ->
            view?.removeCallbacks(pending)
        }
        pendingHighlightScroll = null""",
        )
        violations = self._check_contracts(mutated)
        self.assertIn(
            "removeCallbacks after super.onDestroyView",
            violations,
            "Cleanup after super must cause contract failure",
        )


if __name__ == "__main__":
    unittest.main()
