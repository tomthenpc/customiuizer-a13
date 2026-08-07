import re
import subprocess
import unittest
from pathlib import Path


class P1BPhaseProvenanceTest(unittest.TestCase):
    """Verify all authoritative full commit SHAs in A13_P1B_PHASE_QA.md exist."""

    def test_all_phase_ledger_shas_exist(self):
        repo_root = Path(__file__).resolve().parents[2]
        ledger_path = repo_root / "docs" / "audit" / "A13_P1B_PHASE_QA.md"
        self.assertTrue(ledger_path.is_file(), f"Ledger not found: {ledger_path}")

        text = ledger_path.read_text(encoding="utf-8")
        # Extract full 40-hex SHAs inside backticks (authoritative full SHAs).
        shas = set(re.findall(r"`([0-9a-f]{40})`", text))
        self.assertTrue(shas, "No full SHAs found in phase ledger")

        missing = []
        for sha in sorted(shas):
            try:
                subprocess.run(
                    ["git", "cat-file", "-e", f"{sha}^{{commit}}"],
                    cwd=repo_root,
                    check=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                )
            except subprocess.CalledProcessError:
                missing.append(sha)

        self.assertEqual(
            missing,
            [],
            f"Phase ledger references non-existent commit(s): {missing}",
        )


if __name__ == "__main__":
    unittest.main()
