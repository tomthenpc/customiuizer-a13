#!/usr/bin/env python3
"""Tests for the APK size attribution tool."""
import shutil
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from apk_size_report import ApkSizeReport, compare, report


class ApkSizeReportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = Path(tempfile.mkdtemp(prefix="a13_apk_test_"))
        cls.apk1 = cls.tmp / "app1.apk"
        with zipfile.ZipFile(cls.apk1, "w") as zf:
            zf.writestr("classes.dex", b"X" * 2000)
            zf.writestr("classes2.dex", b"Y" * 1000)
            zf.writestr("res/values/strings.xml", b"<xml/>")

        cls.apk2 = cls.tmp / "app2.apk"
        with zipfile.ZipFile(cls.apk2, "w") as zf:
            zf.writestr("classes.dex", b"X" * 2000)
            zf.writestr("classes2.dex", b"Y" * 2000)
            zf.writestr("res/values/strings.xml", b"<xml/>")
            zf.writestr("res/drawable/new.png", b"PNG" * 100)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.tmp, ignore_errors=True)

    def test_total_and_entry_count(self):
        r = report(self.apk1)
        self.assertEqual(r.entry_count, 3)
        self.assertEqual(len(r.entries), 3)
        self.assertGreater(r.total_uncompressed, 0)

    def test_deterministic_json(self):
        a = report(self.apk1).to_json()
        b = report(self.apk1).to_json()
        self.assertEqual(a, b)

    def test_by_extension_aggregation(self):
        r = report(self.apk1)
        self.assertIn("dex", r.by_extension)
        self.assertIn("xml", r.by_extension)
        self.assertEqual(r.by_extension["dex"]["count"], 2)

    def test_by_prefix_aggregation(self):
        r = report(self.apk1)
        self.assertIn("res/", r.by_prefix)
        self.assertEqual(r.by_prefix["res/"]["count"], 1)

    def test_compare_detects_added_and_changed(self):
        delta = compare(self.apk1, self.apk2)
        self.assertIn("res/drawable/new.png", delta["added_files"])
        self.assertEqual(delta["added_count"], 1)
        self.assertGreater(delta["total_delta"], 0)

    def test_markdown_contains_sha(self):
        md = report(self.apk1).to_markdown()
        self.assertIn("SHA-256", md)
        self.assertIn("Total compressed", md)

    def test_sha256_is_hex(self):
        r = report(self.apk1)
        self.assertEqual(len(r.sha256), 64)
        int(r.sha256, 16)


if __name__ == "__main__":
    unittest.main()
