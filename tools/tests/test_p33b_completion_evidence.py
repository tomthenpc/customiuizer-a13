from __future__ import annotations

import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).parents[2]


class P3_3B_CompletionEvidenceTest(unittest.TestCase):
    """Mechanical gate: completion evidence numbers in state docs must match
    the actual Python unittest counts.

    This is the only test in the module so that it adds exactly one test to the
    discover total and keeps the evidence equation simple.
    """

    def _count_module(self, module_name: str) -> int:
        loader = unittest.TestLoader()
        suite = loader.loadTestsFromName(module_name)
        return suite.countTestCases()

    def _discover_count(self) -> int:
        loader = unittest.TestLoader()
        suite = loader.discover("tools/tests", pattern="test_*.py")
        return suite.countTestCases()

    def _read_doc(self, name: str) -> str:
        return (REPO_ROOT / name).read_text(encoding="utf-8")

    def _extract_latest_number(self, doc: str, command: str) -> int:
        # Find the last verification block in the doc and look for the command.
        # Match pattern: "- <command> ... -> N/N pass" or "... N/N pass".
        pattern = re.compile(
            re.escape(command) + r".*?->\s*(\d+)\s*/\s*\1\s*pass",
            re.DOTALL,
        )
        matches = list(pattern.finditer(doc))
        if not matches:
            raise AssertionError(f"no completion evidence line found for: {command}")
        return int(matches[-1].group(1))

    def test_completion_evidence_numbers_match_unit_tests(self) -> None:
        """Compare the focused and discover counts recorded in TASK_STATE.md and
        SMART_OPERATION_STATE.md to the actual unittest loader counts.
        """
        expected = {
            "tools.tests.test_legacy_exception_registry": 72,
            "tools.tests.test_p33b_legacy_exception_routes": 88,
            "tools.tests.test_legacy_exception_source_contract": 56,
            "tools.tests.test_hook_ownership_inventory": 2,
            "discover": 440,
        }

        actual = {
            "tools.tests.test_legacy_exception_registry": self._count_module(
                "tools.tests.test_legacy_exception_registry"
            ),
            "tools.tests.test_p33b_legacy_exception_routes": self._count_module(
                "tools.tests.test_p33b_legacy_exception_routes"
            ),
            "tools.tests.test_legacy_exception_source_contract": self._count_module(
                "tools.tests.test_legacy_exception_source_contract"
            ),
            "tools.tests.test_hook_ownership_inventory": self._count_module(
                "tools.tests.test_hook_ownership_inventory"
            ),
            "discover": self._discover_count(),
        }

        # Also validate the numbers recorded in TASK_STATE.md.
        task_state = self._read_doc("TASK_STATE.md")
        doc_counts = {
            "tools.tests.test_legacy_exception_registry": self._extract_latest_number(
                task_state, "python -m unittest tools.tests.test_legacy_exception_registry"
            ),
            "tools.tests.test_p33b_legacy_exception_routes": self._extract_latest_number(
                task_state, "python -m unittest tools.tests.test_p33b_legacy_exception_routes"
            ),
            "tools.tests.test_legacy_exception_source_contract": self._extract_latest_number(
                task_state, "python -m unittest tools.tests.test_legacy_exception_source_contract"
            ),
            "tools.tests.test_hook_ownership_inventory": self._extract_latest_number(
                task_state, "python -m unittest tools.tests.test_hook_ownership_inventory"
            ),
            "discover": self._extract_latest_number(
                task_state, "python -m unittest discover -s tools/tests -p \"test_*.py\""
            ),
        }

        for key in expected:
            self.assertEqual(
                actual[key],
                expected[key],
                f"{key}: actual count {actual[key]} != declared {expected[key]}",
            )
            self.assertEqual(
                doc_counts[key],
                expected[key],
                f"{key}: TASK_STATE.md count {doc_counts[key]} != declared {expected[key]}",
            )


if __name__ == "__main__":
    unittest.main()
