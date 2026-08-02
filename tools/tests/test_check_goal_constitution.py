#!/usr/bin/env python3
"""Tests for tools/check_goal_constitution.py."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import check_goal_constitution as checker


class GoalConstitutionTest(unittest.TestCase):

    def test_goal_and_constitution_files_exist(self) -> None:
        self.assertTrue(checker.GOAL.is_file(), "GOAL.md must exist")
        self.assertTrue(checker.CONSTITUTION.is_file(), "LONG_HORIZON_CONSTITUTION.md must exist")

    def test_checker_passes(self) -> None:
        self.assertEqual(0, checker.main(), "check_goal_constitution must pass")


if __name__ == "__main__":
    unittest.main()
