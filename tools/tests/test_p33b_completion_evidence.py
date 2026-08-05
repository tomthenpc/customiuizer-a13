from __future__ import annotations

import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).parents[2].resolve()


class _DocCountExtractor:
    """Extract declared focused and discover counts from completion evidence docs."""

    COMMANDS = {
        "tools.tests.test_legacy_exception_registry",
        "tools.tests.test_p33b_legacy_exception_routes",
        "tools.tests.test_legacy_exception_source_contract",
        "tools.tests.test_p33b_completion_evidence",
        "tools.tests.test_hook_ownership_inventory",
        "discover",
    }

    @classmethod
    def _load_doc(cls, rel: str) -> str:
        return (REPO_ROOT / rel).read_text(encoding="utf-8")

    @staticmethod
    def _find_last_section(text: str, heading_pattern: str) -> str | None:
        """Return the last section whose heading matches heading_pattern."""
        matches = list(re.finditer(rf"^({heading_pattern})(?:\s|$)", text, re.MULTILINE))
        if not matches:
            return None
        start = matches[-1].start()
        after = matches[-1].end()
        # Look for the next heading *after* the matched one.
        next_heading = re.search(r"^#{1,6}\s", text[after:], re.MULTILINE)
        if next_heading:
            return text[start : after + next_heading.start()]
        return text[start:]

    @classmethod
    def extract_task_state_counts(cls, rel: str | None = None) -> dict[str, int]:
        text = cls._load_doc("TASK_STATE.md")
        section = cls._find_last_section(text, r"#{2,6}\s*P3\.3B")
        if section is None:
            raise AssertionError("TASK_STATE.md: no active P3.3B section found")
        return cls._parse_verification_block(section, "TASK_STATE.md")

    @classmethod
    def extract_smart_counts(cls, rel: str | None = None) -> dict[str, int]:
        return cls.extract_smart_counts_from_text(cls._load_doc("SMART_OPERATION_STATE.md"))

    @classmethod
    def extract_smart_counts_from_text(cls, text: str) -> dict[str, int]:
        match = re.search(
            r"CurrentObjectiveStartEvidence:.*?\b(\d+)\s*/\s*\1\s+Python tests pass",
            text,
            re.DOTALL,
        )
        if not match:
            raise AssertionError(
                "SMART_OPERATION_STATE.md: no CurrentObjectiveStartEvidence Python pass count found"
            )
        return {"discover": int(match.group(1))}

    @classmethod
    def extract_handoff_counts(cls, rel: str) -> dict[str, int]:
        text = cls._load_doc(rel)
        section = cls._find_last_section(text, r"#{1,6}.*[Vv]erification")
        if section is None:
            raise AssertionError(f"{rel}: no Verification section found")
        return cls._parse_verification_block(section, rel)

    @classmethod
    def extract_task_slice_counts(cls, rel: str) -> dict[str, int]:
        return cls.extract_handoff_counts(rel)

    @classmethod
    def _parse_verification_block(cls, section: str, doc_name: str) -> dict[str, int]:
        """Parse a verification code block or list for N/N pass counts."""
        counts: dict[str, int] = {}
        command_to_key = {
            "python -m unittest tools.tests.test_legacy_exception_registry": "tools.tests.test_legacy_exception_registry",
            "python -m unittest tools.tests.test_p33b_legacy_exception_routes": "tools.tests.test_p33b_legacy_exception_routes",
            "python -m unittest tools.tests.test_legacy_exception_source_contract": "tools.tests.test_legacy_exception_source_contract",
            "python -m unittest tools.tests.test_p33b_completion_evidence": "tools.tests.test_p33b_completion_evidence",
            "python -m unittest tools.tests.test_hook_ownership_inventory": "tools.tests.test_hook_ownership_inventory",
            "python -m unittest discover -s tools/tests -p \"test_*.py\"": "discover",
        }
        for command, key in command_to_key.items():
            pattern = re.compile(
                r"(?:^|\n)\s*(?:-\s+)?"
                + re.escape(command)
                + r"\s+->\s*(\d+)\s*/\s*(\d+)\s+pass",
                re.MULTILINE,
            )
            matches = list(pattern.finditer(section))
            if not matches:
                raise AssertionError(f"{doc_name}: required metric {key!r} missing")
            values = set()
            for m in matches:
                left = int(m.group(1))
                right = int(m.group(2))
                if left != right:
                    raise AssertionError(
                        f"{doc_name}: asymmetric pass count for {key!r}: {left}/{right}"
                    )
                values.add(left)
            if len(values) > 1:
                raise AssertionError(
                    f"{doc_name}: conflicting duplicate metric {key!r}: {values}"
                )
            counts[key] = values.pop()
        return counts


class P3_3B_CompletionEvidenceTest(unittest.TestCase):
    """Mechanical gate: completion evidence numbers in state docs must match
    the actual Python unittest counts.

    This is the only test in the module so that it adds exactly one test to the
    discover total and keeps the evidence equation simple.
    """

    def _count_module(self, module_name: str) -> int:
        loader = unittest.defaultTestLoader
        suite = loader.loadTestsFromName(module_name)
        return suite.countTestCases()

    def _discover_count(self) -> int:
        loader = unittest.defaultTestLoader
        tests_dir = str(REPO_ROOT / "tools" / "tests")
        suite = loader.discover(tests_dir, pattern="test_*.py")
        return suite.countTestCases()

    def _actual_counts(self) -> dict[str, int]:
        return {
            "tools.tests.test_legacy_exception_registry": self._count_module(
                "tools.tests.test_legacy_exception_registry"
            ),
            "tools.tests.test_p33b_legacy_exception_routes": self._count_module(
                "tools.tests.test_p33b_legacy_exception_routes"
            ),
            "tools.tests.test_legacy_exception_source_contract": self._count_module(
                "tools.tests.test_legacy_exception_source_contract"
            ),
            "tools.tests.test_p33b_completion_evidence": self._count_module(
                "tools.tests.test_p33b_completion_evidence"
            ),
            "tools.tests.test_hook_ownership_inventory": self._count_module(
                "tools.tests.test_hook_ownership_inventory"
            ),
            "discover": self._discover_count(),
        }

    def test_completion_evidence_numbers_match_unit_tests(self) -> None:
        """Compare focused and discover counts in docs to loader counts."""
        actual = self._actual_counts()

        docs: list[tuple[str, callable]] = [
            ("TASK_STATE.md", _DocCountExtractor.extract_task_state_counts),
            ("SMART_OPERATION_STATE.md", _DocCountExtractor.extract_smart_counts),
            (
                "docs/process/handoffs/A13-HANDOFF-2026-08-04-P3.3B-R3.md",
                _DocCountExtractor.extract_handoff_counts,
            ),
            (
                "docs/process/tasks/A13-P3.3B-R3-INDEPENDENT-TRUTH-EVIDENCE-REPAIR.md",
                _DocCountExtractor.extract_task_slice_counts,
            ),
            (
                "docs/process/tasks/A13-P3.3B-R4-GATE-COVERAGE-COMPLETION-EVIDENCE.md",
                _DocCountExtractor.extract_task_slice_counts,
            ),
            (
                "docs/process/handoffs/A13-HANDOFF-2026-08-05-P3.3B-R4.md",
                _DocCountExtractor.extract_handoff_counts,
            ),
        ]

        all_counts: dict[str, dict[str, int]] = {}
        for doc_name, extractor in docs:
            with self.subTest(doc=doc_name):
                path = REPO_ROOT / doc_name
                if not path.exists():
                    continue
                doc_counts = extractor(doc_name)
                all_counts[doc_name] = doc_counts
                for key in _DocCountExtractor.COMMANDS:
                    if key not in doc_counts:
                        continue
                    self.assertEqual(
                        actual[key],
                        doc_counts[key],
                        f"{doc_name}: {key} actual {actual[key]} != doc {doc_counts[key]}",
                    )

        self._run_doc_mutations(actual, all_counts)

    def _run_doc_mutations(
        self,
        actual: dict[str, int],
        all_counts: dict[str, dict[str, int]],
    ) -> None:
        """Verify that stale or missing metrics are rejected."""

        def assert_section_matches(
            expected: dict[str, int],
            section: str,
            doc_name: str,
        ) -> None:
            doc_counts = _DocCountExtractor._parse_verification_block(section, doc_name)
            for key in _DocCountExtractor.COMMANDS:
                if key not in doc_counts:
                    continue
                if doc_counts[key] != expected[key]:
                    raise AssertionError(
                        f"{doc_name}: {key} doc {doc_counts[key]} != actual {expected[key]}"
                    )

        def assert_smart_matches(expected: dict[str, int], text: str) -> None:
            doc_counts = _DocCountExtractor.extract_smart_counts_from_text(text)
            if doc_counts["discover"] != expected["discover"]:
                raise AssertionError(
                    f"SMART: discover doc {doc_counts['discover']} != actual {expected['discover']}"
                )

        # TASK_STATE focused and discover mutations
        task_text = (REPO_ROOT / "TASK_STATE.md").read_text(encoding="utf-8")
        section = _DocCountExtractor._find_last_section(task_text, r"#{2,6}\s*P3\.3B")
        self.assertIsNotNone(section)

        for key, label in [
            ("tools.tests.test_legacy_exception_registry", "stale_task_focused_count"),
            ("discover", "stale_task_discover_count"),
        ]:
            with self.subTest(mutation=label):
                bad = self._bump_count(section, key, actual[key] + 1)
                with self.assertRaises(AssertionError):
                    assert_section_matches(actual, bad, "TASK_STATE.md")

        with self.subTest(mutation="missing_task_metric"):
            missing = re.sub(
                r"- python -m unittest tools\.tests\.test_hook_ownership_inventory.*?pass\n",
                "",
                section,
                count=1,
            )
            with self.assertRaises(AssertionError):
                _DocCountExtractor._parse_verification_block(missing, "TASK_STATE.md")

        with self.subTest(mutation="conflicting_task_metric"):
            key = "tools.tests.test_hook_ownership_inventory"
            line = f"- python -m unittest {key}            -> {actual[key]}/{actual[key]} pass"
            conflict = section + f"\n- python -m unittest {key}            -> {actual[key] + 1}/{actual[key] + 1} pass"
            with self.assertRaises(AssertionError):
                _DocCountExtractor._parse_verification_block(conflict, "TASK_STATE.md")

        with self.subTest(mutation="asymmetric_task_count"):
            bad = self._asymmetric_count(section, "discover", actual["discover"], actual["discover"] + 1)
            with self.assertRaises(AssertionError):
                _DocCountExtractor._parse_verification_block(bad, "TASK_STATE.md")

        # SMART mutation
        smart_text = (REPO_ROOT / "SMART_OPERATION_STATE.md").read_text(encoding="utf-8")
        with self.subTest(mutation="stale_smart_discover_count"):
            bad = re.sub(
                r"\d+/\d+ Python tests pass",
                f"{actual['discover'] + 1}/{actual['discover'] + 1} Python tests pass",
                smart_text,
                count=1,
            )
            with self.assertRaises(AssertionError):
                assert_smart_matches(actual, bad)

        with self.subTest(mutation="asymmetric_smart_count"):
            bad = re.sub(
                r"\d+/\d+ Python tests pass",
                f"{actual['discover']}/{actual['discover'] + 1} Python tests pass",
                smart_text,
                count=1,
            )
            with self.assertRaises(AssertionError):
                _DocCountExtractor.extract_smart_counts_from_text(bad)

        # Handoff / task slice mutations
        for doc_name in [
            "docs/process/handoffs/A13-HANDOFF-2026-08-04-P3.3B-R3.md",
            "docs/process/tasks/A13-P3.3B-R3-INDEPENDENT-TRUTH-EVIDENCE-REPAIR.md",
            "docs/process/tasks/A13-P3.3B-R4-GATE-COVERAGE-COMPLETION-EVIDENCE.md",
            "docs/process/handoffs/A13-HANDOFF-2026-08-05-P3.3B-R4.md",
        ]:
            path = REPO_ROOT / doc_name
            if not path.exists():
                continue
            text = path.read_text(encoding="utf-8")
            section = _DocCountExtractor._find_last_section(text, r"#{1,6}.*[Vv]erification")
            if section is None:
                continue
            with self.subTest(doc=doc_name, mutation="stale_handoff_count"):
                bad = self._bump_count(section, "discover", actual["discover"] + 1)
                with self.assertRaises(AssertionError):
                    assert_section_matches(actual, bad, doc_name)

    def _asymmetric_count(self, section: str, key: str, left: int, right: int) -> str:
        command = {
            "discover": "python -m unittest discover -s tools/tests -p \"test_*.py\"",
        }.get(key, f"python -m unittest {key}")
        pattern = re.compile(
            rf"(\n\s*(?:-\s+)?{re.escape(command)}\s+->\s+)(\d+)(\s*/\s*)(\d+)(\s+pass)",
            re.MULTILINE,
        )
        return pattern.sub(rf"\g<1>{left}\3{right}\5", section, count=1)

    def _bump_count(
        self,
        section: str,
        key: str,
        value: int,
    ) -> str:
        return self._asymmetric_count(section, key, value, value)


if __name__ == "__main__":
    unittest.main()
