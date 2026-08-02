#!/usr/bin/env python3
"""Tests for tools/check_document_contracts.py."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import check_document_contracts as checker


class DocumentContractCheckerTest(unittest.TestCase):
    """Mechanical gate for v4 documentation contract."""

    def test_required_v4_docs_exist(self) -> None:
        repo = checker.REPO_ROOT
        required = {
            "A13_FULL_REVIEW_V4.md",
            "A13_ALGORITHM_OPTIMIZATION_PLAN_V4.md",
            "A13_DOCUMENT_UPDATE_PLAN_V4.md",
            "DOCUMENTATION_CONTRACT_V4.md",
            "CHECKPOINT_AND_CI_TRANSACTION_V4.md",
            "ALGORITHM_OPTIMIZATION_GOVERNANCE_V4.md",
        }
        audit_dir = repo / "docs" / "audit"
        for name in required:
            self.assertTrue(
                (audit_dir / name).exists(),
                f"Required v4 audit doc missing: {name}",
            )

    def test_checker_passes(self) -> None:
        self.assertEqual(0, checker.main(), "check_document_contracts must pass")


if __name__ == "__main__":
    unittest.main()
