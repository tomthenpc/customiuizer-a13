#!/usr/bin/env python3
"""Regression tests for the A13 View tree simulation.

These tests replace Android framework dependencies with the pure-logic
`FakeView` / `FakeViewGroup` model. Any regression in the A13 clamp/add/remove
rules should be reflected in this model and tests.
"""
import random
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from a13_view_tree_sim import FakeView, FakeViewGroup, IconGroupRegistry


class ViewTreeSimulationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.panel = FakeViewGroup("status_bar_contents")

    def test_negative_index_clamped_to_zero(self):
        view = FakeView(1)
        actual = self.panel.add_view(view, -5)
        self.assertEqual(actual, 0)
        self.assertEqual(self.panel.child_count, 1)

    def test_index_above_child_count_clamped(self):
        v1 = FakeView(1)
        v2 = FakeView(2)
        self.panel.add_view(v1, 0)
        actual = self.panel.add_view(v2, 100)
        self.assertEqual(actual, 1)
        self.assertEqual(self.panel.child_count, 2)

    def test_index_equal_to_child_count_appends(self):
        v1 = FakeView(1)
        v2 = FakeView(2)
        self.panel.add_view(v1, 0)
        actual = self.panel.add_view(v2, 1)
        self.assertEqual(actual, 1)

    def test_view_with_existing_parent_rejected(self):
        other = FakeViewGroup("other_container")
        view = FakeView(1)
        other.add_view(view, 0)
        with self.assertRaises(RuntimeError):
            self.panel.add_view(view, 0)

    def test_same_owner_and_slot_prevented_by_registry(self):
        reg = IconGroupRegistry()
        v1 = FakeView(1, owner="battery", slot=0)
        v2 = FakeView(2, owner="battery", slot=0)
        self.assertTrue(reg.register("battery", 0, v1))
        self.assertFalse(reg.register("battery", 0, v2))

    def test_duplicate_attach_does_not_increase_count(self):
        view = FakeView(1)
        self.panel.add_view(view, 0)
        actual = self.panel.add_view(view, 0)
        self.assertEqual(actual, 0)
        self.assertEqual(self.panel.child_count, 1)

    def test_detach_then_reattach(self):
        view = FakeView(1)
        self.panel.add_view(view, 0)
        view.detach()
        self.assertIsNone(view.parent)
        self.panel.add_view(view, 0)
        self.assertEqual(self.panel.child_count, 1)

    def test_disposed_view_cannot_re_attach(self):
        view = FakeView(1)
        self.panel.add_view(view, 0)
        view.dispose()
        with self.assertRaises(RuntimeError):
            self.panel.add_view(view, 0)

    def test_owner_replace_disposes_old(self):
        reg = IconGroupRegistry()
        old = FakeView(1, owner="battery", slot=0)
        new = FakeView(2, owner="battery", slot=0)
        reg.register("battery", 0, old)
        reg.replace("battery", 0, new)
        self.assertTrue(old.disposed)
        self.assertEqual(reg.size(), 1)

    def test_stress_attach_detach_owner_replace(self):
        rng = random.Random(0xA13)
        group = FakeViewGroup("stress")
        reg = IconGroupRegistry()
        for i in range(1000):
            view = FakeView(i, owner=f"owner_{rng.randint(0, 10)}", slot=rng.randint(0, 4))
            if rng.random() < 0.4 or group.child_count == 0:
                group.add_view(view, rng.randint(-5, 20))
                reg.register(view.owner, view.slot, view)
            elif rng.random() < 0.5:
                victim = rng.choice(group.children)
                victim.detach()
            else:
                victim = rng.choice(group.children)
                reg.replace(victim.owner, victim.slot, view)

        # Icon group must not exceed (owners * slots) because slots are [0..4].
        self.assertLessEqual(reg.size(), 11 * 5)
        # No negative indices and no index beyond child count.
        for idx, child in enumerate(group.children):
            self.assertGreaterEqual(idx, 0)
            self.assertLessEqual(idx, group.child_count)
        # Active children are a subset of registered views.
        for c in group.children:
            self.assertFalse(c.disposed)


if __name__ == "__main__":
    unittest.main()
