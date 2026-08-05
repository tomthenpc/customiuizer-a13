import json
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from tools import source_hazard_scan


class FindNewFindingsTest(unittest.TestCase):
    def _make(self, rule: str, path: str, line: int, snippet: str):
        return source_hazard_scan.Finding(rule, path, line, snippet)

    def test_baseline_one_current_two_yields_one_new(self):
        finding = self._make(
            "PRINT_STACK_TRACE",
            "x/Bad.kt",
            10,
            "t.printStackTrace()",
        )
        findings = [finding, finding]
        baseline = Counter({finding.fingerprint: 1})
        new = source_hazard_scan.find_new_findings(findings, baseline)
        self.assertEqual(1, len(new))
        self.assertEqual(finding.fingerprint, new[0].fingerprint)

    def test_baseline_and_current_equal_yields_zero_new(self):
        finding = self._make(
            "PRINT_STACK_TRACE",
            "x/Bad.kt",
            10,
            "t.printStackTrace()",
        )
        findings = [finding, finding]
        baseline = Counter({finding.fingerprint: 2})
        new = source_hazard_scan.find_new_findings(findings, baseline)
        self.assertEqual(0, len(new))

    def test_fingerprint_is_independent_of_line_number(self):
        f1 = self._make("PRINT_STACK_TRACE", "x/Bad.kt", 10, "t.printStackTrace()")
        f2 = self._make("PRINT_STACK_TRACE", "x/Bad.kt", 99, "t.printStackTrace()")
        self.assertEqual(f1.fingerprint, f2.fingerprint)

    def test_baseline_counter_preserves_duplicates(self):
        with tempfile.TemporaryDirectory() as td:
            baseline_path = Path(td) / "baseline.json"
            baseline_path.write_text(
                json.dumps({
                    "schema": 1,
                    "fingerprints": ["abc", "abc", "def"],
                }),
                encoding="utf-8",
            )
            baseline = source_hazard_scan.load_baseline(baseline_path)
            self.assertEqual(3, sum(baseline.values()))
            self.assertEqual(2, baseline["abc"])
            self.assertEqual(1, baseline["def"])

    def test_fewer_current_than_baseline_does_not_fail(self):
        finding = self._make(
            "PRINT_STACK_TRACE",
            "x/Bad.kt",
            10,
            "t.printStackTrace()",
        )
        findings = [finding]
        baseline = Counter({finding.fingerprint: 5})
        new = source_hazard_scan.find_new_findings(findings, baseline)
        self.assertEqual(0, len(new))

    def test_strict_all_returns_all_findings(self):
        f1 = self._make("PRINT_STACK_TRACE", "x/A.kt", 1, "a.printStackTrace()")
        f2 = self._make("PRINT_STACK_TRACE", "x/B.kt", 2, "b.printStackTrace()")
        findings = [f1, f2]
        baseline = Counter()
        new = source_hazard_scan.find_new_findings(findings, baseline)
        self.assertEqual(findings, new)


class SourceHazardIntegrationTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
