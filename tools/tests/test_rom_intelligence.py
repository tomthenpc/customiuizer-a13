#!/usr/bin/env python3
"""Tests for the ROM inventory and target diff workflow.

These tests skip gracefully if the scripts are not yet present, so the audit
branch can be verified before all tools are finalized.
"""
from __future__ import annotations

import json
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent


def run_py(script: str, *args: str) -> tuple[int, str, str]:
    env = os.environ.copy()
    env["PYTHONIOENCODING"] = "utf-8"
    proc = subprocess.run(
        [sys.executable, str(REPO / "tools" / script), *args],
        cwd=REPO,
        capture_output=True,
        text=True,
        env=env,
        encoding="utf-8",
        errors="replace",
    )
    return proc.returncode, proc.stdout, proc.stderr


def has_tool(name: str) -> str:
    return "found" if shutil.which(name) else "missing"


@unittest.skipUnless((REPO / "tools" / "rom_inventory.py").is_file(), "rom_inventory.py not present")
class RomInventoryTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_inventory.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_scan_empty_directory(self):
        """Scanning an empty directory must produce an empty JSON inventory."""
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out.json"
            rc, _, _ = run_py("rom_inventory.py", "-d", tmp, "-o", str(out), "-f", "json")
            self.assertEqual(rc, 0, f"non-zero exit for empty dir: {rc}")
            data = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(data, [])

    def test_scan_tools_directory_finds_no_roms(self):
        """Scanning `tools` (no APK/JAR) must not report any APK or JAR samples."""
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out.json"
            # Copy one .py file as a non-ROM sample.
            shutil.copy(REPO / "tools" / "rom_inventory.py", Path(tmp) / "rom_inventory.py")
            rc, _, _ = run_py("rom_inventory.py", "-d", tmp, "-o", str(out), "-f", "json")
            self.assertEqual(rc, 0, f"non-zero exit: {rc}")
            data = json.loads(out.read_text(encoding="utf-8"))
            types = {row["sample_type"] for row in data}
            self.assertNotIn("APK", types)
            self.assertNotIn("JAR", types)

    def test_scan_directory_with_json_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "out.json"
            (Path(tmp) / "readme.txt").write_text("not a rom", encoding="utf-8")
            rc, _, _ = run_py("rom_inventory.py", "-d", tmp, "-o", str(out), "-f", "json")
            self.assertIn(rc, (0, 2), f"unexpected exit {rc}")
            if rc == 0:
                data = json.loads(out.read_text(encoding="utf-8"))
                self.assertIsInstance(data, list)


@unittest.skipUnless((REPO / "tools" / "rom_target_diff.py").is_file(), "rom_target_diff.py not present")
class RomTargetDiffTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_target_diff.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_diff_two_text_files_reports_not_supported(self):
        a = REPO / ".gitignore"
        b = REPO / "README.md" if (REPO / "README.md").is_file() else a
        rc, out, err = run_py("rom_target_diff.py", str(a), str(b))
        self.assertIn(rc, (0, 1, 2), f"unexpected exit {rc}: {err}")


@unittest.skipUnless((REPO / "tools" / "rom_super_inspector.py").is_file(), "rom_super_inspector.py not present")
class RomSuperInspectorTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_super_inspector.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_missing_rom_directory_fails(self):
        missing = REPO / "nonexistent_rom_dir"
        rc, out, err = run_py("rom_super_inspector.py", "-r", str(missing))
        self.assertIn(rc, (1, 2), f"expected failure for missing dir: {rc}")

    def test_empty_directory_with_allow_missing_returns_zero(self):
        with tempfile.TemporaryDirectory() as tmp:
            rc, out, _ = run_py("rom_super_inspector.py", "-r", tmp, "--allow-missing")
            self.assertEqual(rc, 0, f"--allow-missing empty dir failed: {rc}")


@unittest.skipUnless(
    (REPO / "docs" / "rom-intelligence" / "A13_PROCESS_MATRIX.md").is_file(),
    "process matrix not present",
)
class ProcessMatrixDocTests(unittest.TestCase):
    def test_process_matrix_has_required_sections(self):
        text = (REPO / "docs" / "rom-intelligence" / "A13_PROCESS_MATRIX.md").read_text(
            encoding="utf-8", errors="replace"
        )
        for header in ("Feature ID", "Preference key(s)", "Business / user-visible name", "target package", "allowed process(es)", "denied process(es)"):
            self.assertIn(header, text, f"missing header: {header}")


@unittest.skipUnless(
    (REPO / "docs" / "rom-intelligence" / "A13_TARGET_MATRIX.md").is_file(),
    "target matrix not present",
)
class TargetMatrixDocTests(unittest.TestCase):
    def test_target_matrix_has_required_statuses(self):
        text = (REPO / "docs" / "rom-intelligence" / "A13_TARGET_MATRIX.md").read_text(
            encoding="utf-8", errors="replace"
        )
        for status in ("STATIC_RESOLVED", "CANDIDATE", "WAITING_FOR_SAMPLE"):
            self.assertIn(status, text, f"missing status: {status}")


@unittest.skipUnless(
    (REPO / "docs" / "audit" / "A13_FEATURE_RETIREMENT_AUDIT.md").is_file(),
    "retirement audit not present",
)
class FeatureRetirementDocTests(unittest.TestCase):
    def test_retirement_audit_has_categories(self):
        text = (REPO / "docs" / "audit" / "A13_FEATURE_RETIREMENT_AUDIT.md").read_text(
            encoding="utf-8", errors="replace"
        )
        for cat in ("KEEP", "KEEP_GUARDED", "EXPERIMENTAL", "FREEZE_LEGACY", "DELETE_DEAD"):
            self.assertIn(cat, text, f"missing category: {cat}")


@unittest.skipUnless((REPO / "tools" / "rom_super_inspector.py").is_file(), "rom_super_inspector.py not present")
class LpOffsetRegressionTests(unittest.TestCase):
    """Regression tests for LP linear extent offset semantics.

    These intentionally fail under the old formula:
        start_bytes = (first_logical_sector + target_data) * 512
    and pass under the corrected formula:
        start_bytes = target_data * 512
    """

    def test_veux_like_product_a_offset_no_first_logical_sector_added(self):
        """first_logical_sector=2048, target_data=6144 -> 3145728, not 4194304."""
        import struct
        import tempfile

        from tools.rom_super_inspector import inspect_super

        class FakeExtent:
            target_source = 0
            target_type = 0
            target_data = 6144
            num_sectors = 1

        class FakePartition:
            name = b"product_a" + b"\x00" * (36 - len("product_a"))
            attributes = 0
            first_extent_index = 0
            num_extents = 1

        class FakeBlockDevice:
            partition_name = b"super" + b"\x00" * (36 - len("super"))
            first_logical_sector = 2048
            size = 10000000

        class FakeMetadata:
            partitions = [FakePartition()]
            extents = [FakeExtent()]
            block_devices = [FakeBlockDevice()]

        class FakeLiblp:
            LP_TARGET_TYPE_LINEAR = 0
            LP_TARGET_TYPE_ZERO = 1

            @staticmethod
            def ReadMetadata(path, slot):
                return FakeMetadata()

        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "super.img"
            src.write_bytes(struct.pack("<I", 0xED26FF3A) + b"\x00" * 24)
            meta = inspect_super(src, FakeLiblp)

        self.assertEqual(len(meta["logical_partitions"]), 1)
        part = meta["logical_partitions"][0]
        self.assertEqual(part["name"], "product_a")
        ext = part["extents"][0]
        # Correct formula gives target_data * 512 = 6144 * 512
        self.assertEqual(ext["start_bytes_on_target_device"], 6144 * 512)
        # Old formula would have given (2048 + 6144) * 512
        self.assertNotEqual(ext["start_bytes_on_target_device"], (2048 + 6144) * 512)
        self.assertEqual(
            ext["target_device_first_logical_sector"], 2048,
            "first_logical_sector must be preserved as metadata, not added",
        )

    def test_multi_extent_partition_preserves_order_and_source(self):
        """A partition with multiple linear extents keeps them in order."""
        import struct
        import tempfile

        from tools.rom_super_inspector import inspect_super

        class FakeExtent:
            def __init__(self, src, ttype, data, sectors):
                self.target_source = src
                self.target_type = ttype
                self.target_data = data
                self.num_sectors = sectors

        class FakePartition:
            name = b"vendor_a" + b"\x00" * (36 - len("vendor_a"))
            attributes = 0
            first_extent_index = 0
            num_extents = 3

        class FakeBlockDevice:
            partition_name = b"super" + b"\x00" * (36 - len("super"))
            first_logical_sector = 1024
            size = 10000000

        class FakeMetadata:
            partitions = [FakePartition()]
            extents = [
                FakeExtent(0, 0, 1000, 10),
                FakeExtent(0, 0, 2000, 20),
                FakeExtent(0, 0, 3000, 30),
            ]
            block_devices = [FakeBlockDevice()]

        class FakeLiblp:
            LP_TARGET_TYPE_LINEAR = 0
            LP_TARGET_TYPE_ZERO = 1

            @staticmethod
            def ReadMetadata(path, slot):
                return FakeMetadata()

        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "super.img"
            src.write_bytes(struct.pack("<I", 0xED26FF3A) + b"\x00" * 24)
            meta = inspect_super(src, FakeLiblp)

        part = meta["logical_partitions"][0]
        self.assertEqual(part["num_extents"], 3)
        self.assertEqual(part["extents"][0]["start_bytes_on_target_device"], 1000 * 512)
        self.assertEqual(part["extents"][1]["start_bytes_on_target_device"], 2000 * 512)
        self.assertEqual(part["extents"][2]["start_bytes_on_target_device"], 3000 * 512)

    def test_unsupported_target_type_is_warned_not_crash(self):
        """A non-linear extent must be skipped with a warning."""
        import struct
        import tempfile

        from tools.rom_super_inspector import inspect_super

        class FakeExtent:
            target_source = 0
            target_type = 99
            target_data = 100
            num_sectors = 10

        class FakePartition:
            name = b"odm_a" + b"\x00" * (36 - len("odm_a"))
            attributes = 0
            first_extent_index = 0
            num_extents = 1

        class FakeBlockDevice:
            partition_name = b"super" + b"\x00" * (36 - len("super"))
            first_logical_sector = 0
            size = 10000000

        class FakeMetadata:
            partitions = [FakePartition()]
            extents = [FakeExtent()]
            block_devices = [FakeBlockDevice()]

        class FakeLiblp:
            LP_TARGET_TYPE_LINEAR = 0
            LP_TARGET_TYPE_ZERO = 1

            @staticmethod
            def ReadMetadata(path, slot):
                return FakeMetadata()

        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "super.img"
            src.write_bytes(struct.pack("<I", 0xED26FF3A) + b"\x00" * 24)
            meta = inspect_super(src, FakeLiblp)

        part = meta["logical_partitions"][0]
        self.assertEqual(part["extents"], [])
        self.assertTrue(any("target_type" in w for w in meta["warnings"]))


@unittest.skipUnless((REPO / "tools" / "rom_fs_probe.py").is_file(), "rom_fs_probe.py not present")
class RomFsProbeTests(unittest.TestCase):
    def test_authoritative_fs_detection_ext4(self):
        """ext4 superblock magic is at 1024+56, not byte 0."""
        import tempfile

        from tools.rom_fs_probe import detect_fs

        with tempfile.TemporaryDirectory() as tmp:
            img = Path(tmp) / "fake_ext4.img"
            data = bytearray(4096)
            data[1024 + 56] = 0x53
            data[1024 + 57] = 0xEF
            img.write_bytes(bytes(data))
            fs, _ = detect_fs(img)
            self.assertEqual(fs, "EXT4")

    def test_authoritative_fs_detection_erofs(self):
        """EROFS superblock magic is at 1024."""
        import tempfile

        from tools.rom_fs_probe import detect_fs

        with tempfile.TemporaryDirectory() as tmp:
            img = Path(tmp) / "fake_erofs.img"
            data = bytearray(4096)
            struct.pack_into("<I", data, 1024, 0xE0F5E1E2)
            img.write_bytes(bytes(data))
            fs, _ = detect_fs(img)
            self.assertEqual(fs, "EROFS")

    def test_authoritative_fs_detection_f2fs(self):
        """F2FS primary superblock magic is at 1024."""
        import tempfile

        from tools.rom_fs_probe import detect_fs

        with tempfile.TemporaryDirectory() as tmp:
            img = Path(tmp) / "fake_f2fs.img"
            data = bytearray(8192)
            struct.pack_into("<I", data, 1024, 0xF2F52010)
            img.write_bytes(bytes(data))
            fs, _ = detect_fs(img)
            self.assertEqual(fs, "F2FS")

    def test_authoritative_fs_detection_squashfs(self):
        """squashfs magic is at byte 0."""
        import tempfile

        from tools.rom_fs_probe import detect_fs

        with tempfile.TemporaryDirectory() as tmp:
            img = Path(tmp) / "fake_squashfs.img"
            data = bytearray(4096)
            data[0:4] = b"hsqs"
            img.write_bytes(bytes(data))
            fs, _ = detect_fs(img)
            self.assertEqual(fs, "SQUASHFS")

    def test_unknown_when_no_magic(self):
        import tempfile

        from tools.rom_fs_probe import detect_fs

        with tempfile.TemporaryDirectory() as tmp:
            img = Path(tmp) / "empty.img"
            img.write_bytes(b"\x00" * 8192)
            fs, _ = detect_fs(img)
            self.assertEqual(fs, "UNKNOWN")


@unittest.skipUnless((REPO / "tools" / "rom_part_extractor.py").is_file(), "rom_part_extractor.py not present")
class RomPartExtractorTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_part_extractor.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_default_run_cleans_temp_partition(self):
        """Extracting a partition to a temporary workdir and not passing --out
        must not leave a file in the current directory."""
        super_img = Path(r"C:\Home\xiaomi\rom\A13\veux_id_global_images_OS1.0.10.0.TKCIDXM_13.0\images\super.img")
        if not super_img.is_file():
            self.skipTest("veux super.img not available")

        leftover = REPO / "system_ext_a.img"
        leftover.unlink(missing_ok=True)
        rc, _, _ = run_py(
            "rom_part_extractor.py", str(super_img), "system_ext_a"
        )
        self.assertEqual(rc, 0)
        self.assertFalse(leftover.exists(), "default extraction left system_ext_a.img in repo root")


@unittest.skipUnless((REPO / "tools" / "rom_apk_extract.py").is_file(), "rom_apk_extract.py not present")
class RomApkExtractTests(unittest.TestCase):
    def test_help_returns_zero(self):
        rc, out, _ = run_py("rom_apk_extract.py", "--help")
        self.assertEqual(rc, 0, f"--help failed: {out}")

    def test_default_pipeline_cleans_temp_partition(self):
        """rom_apk_extract must extract MiuiSystemUI.apk without leaving a
        1.5GB partition dump in the output directory."""
        super_img = Path(r"C:\Home\xiaomi\rom\A13\veux_id_global_images_OS1.0.10.0.TKCIDXM_13.0\images\super.img")
        if not super_img.is_file():
            self.skipTest("veux super.img not available")

        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "MiuiSystemUI.apk"
            rc, out_text, _ = run_py(
                "rom_apk_extract.py",
                str(super_img),
                "/priv-app/MiuiSystemUI/MiuiSystemUI.apk",
                "--partition", "system_ext_a",
                "--out", str(out),
            )
            self.assertEqual(rc, 0, f"apk extraction failed: {out_text}")
            self.assertTrue(out.is_file(), "MiuiSystemUI.apk was not written")
            data = json.loads(out_text.splitlines()[-1])
            self.assertEqual(data["sha256"], "dd2271dfcd6975c0d8997d4a00a7ee975b0b45f7da6737487f4bf7dfed867b94")


if __name__ == "__main__":
    unittest.main()
