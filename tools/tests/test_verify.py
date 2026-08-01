import importlib.util
import sys
import unittest
from pathlib import Path
from unittest.mock import patch


REPO_ROOT = Path(__file__).resolve().parent.parent.parent
MODULE_PATH = REPO_ROOT / "tools" / "verify.py"

_spec = importlib.util.spec_from_file_location("verify", MODULE_PATH)
verify = importlib.util.module_from_spec(_spec)
sys.modules["verify"] = verify
_spec.loader.exec_module(verify)


class VerifyCompatGateTest(unittest.TestCase):
    def test_fast_mode_stops_before_compile_when_compat_gate_fails(self):
        with patch.object(verify, "check_invariants", return_value=0), \
                patch.object(verify, "check_compat_contracts", return_value=1), \
                patch.object(verify, "compile_debug") as compile_debug:
            self.assertEqual(1, verify.fast_mode(None, False))
            compile_debug.assert_not_called()

    def test_full_mode_stops_before_compile_when_compat_gate_fails(self):
        with patch.object(verify, "check_invariants", return_value=0), \
                patch.object(verify, "check_compat_contracts", return_value=1), \
                patch.object(verify, "compile_debug") as compile_debug:
            self.assertEqual(1, verify.full_mode())
            compile_debug.assert_not_called()


if __name__ == "__main__":
    unittest.main()
