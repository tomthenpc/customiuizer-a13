#!/usr/bin/env python3
"""Extract a file (typically an APK) from an Android ROM.

Supports two input forms:

1. An ext4/f2fs logical partition image:
   python tools/rom_apk_extract.py system_ext_a.img \
       /priv-app/MiuiSystemUI/MiuiSystemUI.apk --out MiuiSystemUI.apk

2. A raw/sparse super image:
   python tools/rom_apk_extract.py super.img \
       /priv-app/MiuiSystemUI/MiuiSystemUI.apk \
       --partition system_ext_a --out MiuiSystemUI.apk

The default behaviour uses a temporary workdir, extracts only the needed
partition and file, writes the requested output, then cleans up the
intermediate partition. Use --keep-workdir only for debugging.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import tempfile
from pathlib import Path
from typing import Any

# Allow the script to run both as `python tools/rom_apk_extract.py` and as a
# module imported from the repository root (e.g. from tests).
_module_dir = Path(__file__).resolve().parent
_repo_root = _module_dir.parent
if str(_repo_root) not in sys.path:
    sys.path.insert(0, str(_repo_root))

from tools import rom_part_extractor


def try_import_ext4() -> Any:
    try:
        import ext4

        return ext4
    except ImportError:
        return None


def _looks_like_super(path: Path) -> bool:
    liblp = rom_part_extractor.try_import_liblp()
    if liblp is None:
        return False

    try:
        with tempfile.TemporaryDirectory(prefix="rom_apk_extract_probe_") as tmp:
            raw = Path(tmp) / "super_head.raw"
            if rom_part_extractor.is_sparse_image(path):
                rom_part_extractor.unsparse_head(path, raw, 8 * 1024 * 1024)
            else:
                with path.open("rb") as src, raw.open("wb") as dst:
                    dst.write(src.read(8 * 1024 * 1024))
            metadata = liblp.ReadMetadata(str(raw), 0)
            return bool(metadata.partitions)
    except Exception:
        return False


def _check_disk_space(path: Path, needed_bytes: int) -> None:
    parent = path.resolve()
    while parent and not parent.exists():
        parent = parent.parent
    if not parent:
        parent = Path(".")
    free = shutil.disk_usage(parent).free
    if needed_bytes > free:
        raise RuntimeError(
            f"insufficient disk space at {parent}: need {needed_bytes} bytes, "
            f"have {free} bytes"
        )


def extract_file_from_partition(
    partition_image: Path, internal_path: str, out_path: Path
) -> dict[str, Any]:
    ext4 = try_import_ext4()
    if ext4 is None:
        return {"ok": False, "error": "ext4 Python package not installed"}

    try:
        vol = ext4.Volume(open(partition_image, "rb"))
        inode = vol.inode_at(internal_path)
        data = inode.open().read()
        out_path.write_bytes(data)
        return {
            "ok": True,
            "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest(),
        }
    except Exception as e:
        return {"ok": False, "error": str(e)}


def extract_file_from_super(
    super_image: Path,
    partition_name: str,
    internal_path: str,
    out_path: Path,
    keep_workdir: bool = False,
) -> dict[str, Any]:
    liblp = rom_part_extractor.try_import_liblp()
    if liblp is None:
        return {"ok": False, "error": "liblp Python package not installed"}

    work_dir = Path(tempfile.mkdtemp(prefix="rom_apk_extract_"))
    partition_path = work_dir / f"{partition_name}.img"
    try:
        estimated = rom_part_extractor.estimate_partition_size(super_image, partition_name, liblp)
        if estimated is not None:
            print(f"estimated partition size: {estimated} bytes", file=sys.stderr)
            _check_disk_space(partition_path, estimated)

        part_result = rom_part_extractor.extract_partition(
            super_image, partition_name, partition_path, liblp
        )
        if not partition_path.exists():
            return {
                "ok": False,
                "error": f"partition extraction failed: {part_result}",
            }

        file_result = extract_file_from_partition(
            partition_path, internal_path, out_path
        )
        if file_result["ok"]:
            file_result["partition_extraction"] = part_result
        return file_result
    finally:
        if not keep_workdir:
            shutil.rmtree(work_dir, ignore_errors=True)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Extract a file from a partition or super image."
    )
    parser.add_argument("image", type=Path, help="ext4 partition image or super image.")
    parser.add_argument("path", help="Internal path (e.g. /app/Foo.apk).")
    parser.add_argument("--out", type=Path, required=True, help="Output file.")
    parser.add_argument("--partition", type=str, help="Partition name (only for super image input).")
    parser.add_argument(
        "--keep-workdir",
        action="store_true",
        help="Keep the temporary workdir (debug only).",
    )
    args = parser.parse_args(argv)

    _check_disk_space(args.out, 0)

    if _looks_like_super(args.image):
        if not args.partition:
            print(
                "ERROR: input looks like a super image; specify --partition",
                file=sys.stderr,
            )
            return 1
        result = extract_file_from_super(
            args.image,
            args.partition,
            args.path,
            args.out,
            keep_workdir=args.keep_workdir,
        )
    else:
        if args.partition:
            print(
                "WARNING: --partition is ignored for non-super input",
                file=sys.stderr,
            )
        result = extract_file_from_partition(args.image, args.path, args.out)

    if not result["ok"]:
        print(f"ERROR: {result['error']}", file=sys.stderr)
        return 1

    sha = result.get("sha256", "")
    print(
        json.dumps(
            {
                "ok": True,
                "out": str(args.out),
                "bytes": result["bytes"],
                "sha256": sha,
            },
            ensure_ascii=False,
        )
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
