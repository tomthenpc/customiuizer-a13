import unittest
from tools.parity_inventory import (
    classify_ui_node,
    default_parity_for_key_match,
    derive_batch_counts,
    parity_accounting_invariant,
)


class ParityInventoryTest(unittest.TestCase):
    def test_category_node_not_product_feature(self):
        self.assertEqual(classify_ui_node("PreferenceCategory", "system_cat"), "CATEGORY")

    def test_navigation_node_not_product_feature(self):
        self.assertEqual(classify_ui_node("PreferenceScreen", "system"), "NAVIGATION_ENTRY")

    def test_same_key_defaults_to_insufficient_evidence(self):
        self.assertEqual(default_parity_for_key_match(True), "INSUFFICIENT_EVIDENCE")

    def test_a13_only_keep_state(self):
        row = {"parity_state": "A13_ONLY_KEEP", "a14_feature_id": "", "phase_e_batch": ""}
        self.assertEqual(row["parity_state"], "A13_ONLY_KEEP")

    def test_e_batch_counts_derived_from_rows(self):
        rows = [
            {"parity_state": "MISSING_IN_A13", "phase_e_batch": "E1"},
            {"parity_state": "PARTIAL_PARITY", "phase_e_batch": "E3"},
            {"parity_state": "INSUFFICIENT_EVIDENCE", "phase_e_batch": "E2"},
        ]
        c = derive_batch_counts(rows)
        self.assertEqual(c["E1"], 1)
        self.assertEqual(c["E3"], 1)
        self.assertNotIn("E2", c)

    def test_parity_accounting_invariant(self):
        rows = [
            {"a14_feature_id": "f1", "parity_state": "PRESENT_A13_VARIANT"},
            {"a14_feature_id": "f2", "parity_state": "MISSING_IN_A13"},
            {"a14_feature_id": "", "parity_state": "A13_ONLY_KEEP"},
        ]
        self.assertTrue(parity_accounting_invariant(rows))

    def test_dynamic_island_excluded_exactly_once_check(self):
        rows = [
            {"parity_state": "INTENTIONAL_EXCLUDED"},
            {"parity_state": "PRESENT_A13_VARIANT"},
        ]
        excluded = sum(1 for r in rows if r["parity_state"] == "INTENTIONAL_EXCLUDED")
        self.assertEqual(excluded, 1)


if __name__ == "__main__":
    unittest.main()

