#!/usr/bin/env python3
"""Probe a raw partition image for common Android filesystems.

This reads the on-disk superblock/magic at the authoritative offsets:

- squashfs: byte 0, magic b"hsqs"
- erofs:    1 KiB boot sector, little-endian magic 0xE0F5E1E2
- f2fs:     1 KiB boot sector, magic 0xF2F52010 (or backup at 5 KiB)
- ext4:     1 KiB boot sector, magic 0xEF53 at superblock+56

Output:
    SQUASHFS, EROFS, F2FS, EXT4, or UNKNOWN.
"""
from __future__ import annotations

import argparse
import struct
import sys
from pathlib import Path
from typing import Any

SQUASHFS_MAGIC = b"hsqs"
EROFS_MAGIC = 0xE0F5E1E2
F2FS_MAGIC = 0xF2F52010
EXT4_MAGIC = 0xEF53
EXT4_SUPERBLOCK_OFFSET = 1024
EXT4_MAGIC_OFFSET = 56


def detect_fs(path: Path) -> tuple[str, dict[str, Any]]:
    info: dict[str, Any] = {}

    try:
        with path.open("rb") as f:
            head = f.read(4)
            info["head_bytes"] = head.hex()
            if head == SQUASHFS_MAGIC:
                return "SQUASHFS", info

            # EROFS superblock at 1 KiB
            f.seek(1024)
            erofs_bytes = f.read(4)
            info["erofs_1024_bytes"] = erofs_bytes.hex()
            if erofs_bytes:
                val = struct.unpack("<I", erofs_bytes)[0]
                info["erofs_1024_magic"] = f"{val:#x}"
                if val == EROFS_MAGIC:
                    return "EROFS", info

            # F2FS primary superblock at 1 KiB, backup at 5 KiB
            f.seek(1024)
            f2fs_bytes = f.read(4)
            info["f2fs_1024_bytes"] = f2fs_bytes.hex()
            if f2fs_bytes:
                val = struct.unpack("<I", f2fs_bytes)[0]
                info["f2fs_1024_magic"] = f"{val:#x}"
                if val == F2FS_MAGIC:
                    return "F2FS", info

            f.seek(5120)
            f2fs_backup = f.read(4)
            info["f2fs_5120_bytes"] = f2fs_backup.hex()
            if f2fs_backup:
                val = struct.unpack("<I", f2fs_backup)[0]
                info["f2fs_5120_magic"] = f"{val:#x}"
                if val == F2FS_MAGIC:
                    return "F2FS", info

            # ext4: superblock at 1 KiB, magic at +56
            f.seek(EXT4_SUPERBLOCK_OFFSET + EXT4_MAGIC_OFFSET)
            ext4_bytes = f.read(2)
            info["ext4_1080_bytes"] = ext4_bytes.hex()
            if ext4_bytes:
                if len(ext4_bytes) == 2:
                    val = struct.unpack("<H", ext4_bytes)[0]
                    info["ext4_1080_magic"] = f"{val:#x}"
                    if val == EXT4_MAGIC:
                        return "EXT4", info

            info["offsets_checked"] = [0, 1024, 5120, 1080]
            return "UNKNOWN", info
    except (OSError, struct.error) as e:
        info["error"] = str(e)
        return "UNKNOWN", info


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Probe Android filesystem type.")
    parser.add_argument("image", type=Path, help="Raw partition image.")
    parser.add_argument("--json", action="store_true", help="Output JSON.")
    args = parser.parse_args(argv)

    fs, info = detect_fs(args.image)
    if args.json:
        import json
        print(json.dumps({"fs": fs, **info}, indent=2, ensure_ascii=False))
    else:
        print(fs)
        for k, v in info.items():
            print(f"  {k}: {v}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
