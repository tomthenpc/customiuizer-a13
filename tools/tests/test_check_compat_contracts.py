import importlib.util
import sys
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
MODULE_PATH = REPO_ROOT / "tools" / "check-compat-contracts.py"

_spec = importlib.util.spec_from_file_location("check_compat_contracts", MODULE_PATH)
check_compat_contracts = importlib.util.module_from_spec(_spec)
sys.modules["check_compat_contracts"] = check_compat_contracts
_spec.loader.exec_module(check_compat_contracts)


class CheckCompatContractsTest(unittest.TestCase):
    def test_main_runs_cleanly(self):
        self.assertEqual(check_compat_contracts.main(), 0)
