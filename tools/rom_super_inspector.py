#!/usr/bin/env python3
"""Offline Android sparse super partition inspector.

Scans a fastboot-style ROM directory, detects sparse `super.img` files,
unsparses just enough blocks to read the `liblp` dynamic partition
metadata, and writes a JSON/CSV inventory of partition names, sizes and
extents.  No ROM data is uploaded.

Typical usage:
    python tools/rom_super_inspector.py -r ROMS/xaga_cn -o out/xaga_cn.json

Exit codes:
    0  inspection completed (partition table found or --allow-missing)
    1  no super image or invalid input
    2  unexpected error
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import shutil
import struct
import sys
import tempfile
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent

# Android sparse image constants
SPARSE_MAGIC = 0xED26FF3A
SPARSE_HEADER_FMT = "<IHHHHIII I"  # 28 bytes; trailing 4 is checksum in v1.0
SPARSE_CHUNK_FMT = "<HHII"         # 12 bytes
CHUNK_RAW = 0xCAC1
CHUNK_FILL = 0xCAC2
CHUNK_DONT_CARE = 0xCAC3
CHUNK_CRC = 0xCAC4


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def is_sparse_image(path: Path) -> bool:
    try:
        with path.open("rb") as f:
            magic = struct.unpack("<I", f.read(4))[0]
            return magic == SPARSE_MAGIC
    except (OSError, struct.error):
        return False


def unsparse_head(src: Path, dst: Path, max_bytes: int) -> int:
    """Unsparse the first `max_bytes` of a sparse image to `dst`.

    Returns the number of bytes written.  Does not overwrite an existing file.
    """
    if dst.exists():
        raise FileExistsError(dst)

    with src.open("rb") as fsrc, dst.open("wb") as fdst:
        hdr = fsrc.read(28)
        if len(hdr) < 28:
            raise ValueError(f"{src}: not a sparse image (too short)")
        (
            magic,
            major,
            minor,
            file_hdr_sz,
            chunk_hdr_sz,
            blk_sz,
            total_blks,
            total_chunks,
            image_checksum,
        ) = struct.unpack(SPARSE_HEADER_FMT, hdr)
        if magic != SPARSE_MAGIC:
            raise ValueError(f"{src}: bad sparse magic {magic:#x}")

        written = 0
        for _ in range(total_chunks):
            if written >= max_bytes:
                break
            chunk_data = fsrc.read(12)
            if len(chunk_data) < 12:
                break
            chunk_type, reserved, chunk_sz, total_sz = struct.unpack(
                SPARSE_CHUNK_FMT, chunk_data
            )
            data_len = total_sz - 12

            if chunk_type == CHUNK_RAW:
                to_read = min(data_len, max_bytes - written)
                fdst.write(fsrc.read(to_read))
                if to_read < data_len:
                    fsrc.seek(data_len - to_read, 1)
                written += to_read
            elif chunk_type == CHUNK_FILL:
                fill_val = fsrc.read(data_len)
                if not fill_val:
                    fill_val = b"\x00"
                repeat_len = min(chunk_sz * blk_sz, max_bytes - written)
                fdst.write(fill_val * (repeat_len // len(fill_val)))
                fdst.write(fill_val[: repeat_len % len(fill_val)])
                written += repeat_len
            elif chunk_type == CHUNK_DONT_CARE:
                fill_len = min(chunk_sz * blk_sz, max_bytes - written)
                fdst.write(b"\x00" * fill_len)
                written += fill_len
            elif chunk_type == CHUNK_CRC:
                fsrc.seek(4, 1)
            else:
                fsrc.seek(data_len, 1)

        return written


def try_import_liblp() -> Any:
    try:
        import liblp

        return liblp
    except ImportError:
        return None


def inspect_super(src: Path, liblp: Any, max_metadata_bytes: int = 8 * 1024 * 1024) -> dict[str, Any]:
    """Return a metadata dict for `src` (sparse or raw super image)."""
    result: dict[str, Any] = {
        "source_path": str(src.resolve()),
        "source_sha256": sha256_file(src),
        "is_sparse": is_sparse_image(src),
        "size_bytes": src.stat().st_size,
        "logical_partitions": [],
        "warnings": [],
    }

    with tempfile.TemporaryDirectory(prefix="rom_super_inspector_") as tmp:
        raw = Path(tmp) / "super_head_raw.img"
        if result["is_sparse"]:
            try:
                unsparse_head(src, raw, max_metadata_bytes)
            except Exception as e:
                result["warnings"].append(f"unsparse failed: {e}")
                return result
        else:
            # Copy first chunk if non-sparse and we still want liblp metadata
            raw = Path(tmp) / "super_head_raw.img"
            with src.open("rb") as fsrc, raw.open("wb") as fdst:
                fdst.write(fsrc.read(max_metadata_bytes))

        try:
            metadata = liblp.ReadMetadata(str(raw), 0)
        except Exception as e:
            result["warnings"].append(f"liblp.ReadMetadata failed: {e}")
            return result

        if not metadata.partitions:
            result["warnings"].append("liblp returned no partitions")
            return result

        # For a dm-linear extent, target_data is already the absolute
        # physical sector on the target block device. It must NOT be
        # mixed with block_device.first_logical_sector, which only
        # describes where the first usable sector begins on that device.
        for part in metadata.partitions:
            name = part.name.decode("utf-8", "replace").rstrip("\x00")
            extents: list[dict[str, Any]] = []
            for ei in range(
                part.first_extent_index,
                part.first_extent_index + part.num_extents,
            ):
                if ei >= len(metadata.extents):
                    result["warnings"].append(
                        f"partition {name}: extent index {ei} out of range"
                    )
                    break
                ext = metadata.extents[ei]

                if ext.target_type not in (
                    liblp.LP_TARGET_TYPE_LINEAR,
                    liblp.LP_TARGET_TYPE_ZERO,
                ):
                    result["warnings"].append(
                        f"partition {name} extent {ei}: unsupported target_type={ext.target_type}; skipped"
                    )
                    continue

                if ext.target_type == liblp.LP_TARGET_TYPE_ZERO:
                    # zero extents have no physical backing
                    continue

                if ext.target_source >= len(metadata.block_devices):
                    result["warnings"].append(
                        f"partition {name} extent {ei}: target_source {ext.target_source} out of range"
                    )
                    continue

                target_device = metadata.block_devices[ext.target_source]
                target_device_name = (
                    target_device.partition_name.decode("utf-8", "replace")
                    .rstrip("\x00")
                )

                start_bytes = ext.target_data * 512

                if ext.target_source != 0:
                    result["warnings"].append(
                        f"partition {name} extent {ei}: target_source={ext.target_source} "
                        f"(block device {target_device_name}), start bytes computed for that device; "
                        "cannot locate within super.img"
                    )

                extents.append(
                    {
                        "target_source": ext.target_source,
                        "target_type": ext.target_type,
                        "target_data_sector": ext.target_data,
                        "num_sectors": ext.num_sectors,
                        "target_device_name": target_device_name,
                        "target_device_first_logical_sector": int(
                            target_device.first_logical_sector
                        ),
                        "start_bytes_on_target_device": start_bytes,
                        "size_bytes": ext.num_sectors * 512,
                    }
                )
            result["logical_partitions"].append(
                {
                    "name": name,
                    "first_extent_index": part.first_extent_index,
                    "num_extents": part.num_extents,
                    "attributes": part.attributes,
                    "size_bytes": sum(e["size_bytes"] for e in extents),
                    "extents": extents,
                }
            )

    return result


def find_super_images(root: Path) -> list[Path]:
    """Locate likely super partition images under `root`."""
    candidates: set[Path] = set()
    for p in root.rglob("*"):
        if not p.is_file():
            continue
        if p.suffix.lower() in (".img", ".bin", ".mbn"):
            name = p.name.lower()
            if "super" in name and name.startswith("super"):
                # Accept super*.img (sparse or raw). Avoid misclassifying
                # cust/rescue/userdata sparse images as super.
                candidates.add(p)
    return sorted(candidates)


def classify_fs_magic(first_bytes: bytes) -> str:
    if len(first_bytes) < 4:
        return "unknown"
    if first_bytes[:2] == b"\x53\xef":
        return "ext4_candidate"
    if first_bytes[:4] == b"\x10\x20\xf5\xf2" or first_bytes[:4] == b"\xf2\xf5\x20\x10":
        return "f2fs_candidate"
    if first_bytes[:4] == b"\xe0\xf5\xe0\xf5":
        return "erofs_candidate"
    if first_bytes[:4] == b"hsqs":
        return "squashfs_candidate"
    return "unknown"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Inspect fastboot ROM images and extract super partition metadata."
    )
    parser.add_argument(
        "-r", "--rom", required=True, type=Path, help="ROM directory to inspect."
    )
    parser.add_argument(
        "-o", "--out", type=Path, default=None, help="Output JSON/CSV file."
    )
    parser.add_argument(
        "-f", "--format", choices=("json", "csv"), default="json", help="Output format."
    )
    parser.add_argument(
        "--codename", default="", help="Device codename."
    )
    parser.add_argument(
        "--android", default="", help="Android version."
    )
    parser.add_argument(
        "--miui", default="", help="MIUI / HyperOS version."
    )
    parser.add_argument(
        "--fingerprint", default="", help="Build fingerprint."
    )
    parser.add_argument(
        "--metadata-bytes",
        type=int,
        default=8 * 1024 * 1024,
        help="Bytes to unsparse for liblp metadata (default 8 MiB).",
    )
    parser.add_argument(
        "--allow-missing",
        action="store_true",
        help="Exit 0 even if no super image is found.",
    )
    args = parser.parse_args(argv)

    if not args.rom.is_dir():
        print(f"ERROR: {args.rom} is not a directory", file=sys.stderr)
        return 1

    images = find_super_images(args.rom)
    if not images:
        print(f"No super image found under {args.rom}", file=sys.stderr)
        return 0 if args.allow_missing else 1

    liblp = try_import_liblp()
    if liblp is None:
        print("ERROR: liblp Python package not installed", file=sys.stderr)
        return 2

    rows: list[dict[str, Any]] = []
    for img in images:
        meta = inspect_super(img, liblp, args.metadata_bytes)
        row: dict[str, Any] = {
            "codename": args.codename,
            "android": args.android,
            "miui_hyperos": args.miui,
            "build_fingerprint": args.fingerprint,
            "source_filename": img.relative_to(args.rom).as_posix(),
            "source_sha256": meta["source_sha256"],
            "is_sparse": meta["is_sparse"],
            "size_bytes": meta["size_bytes"],
            "logical_partition_count": len(meta["logical_partitions"]),
            "logical_partitions": meta["logical_partitions"],
            "warnings": meta["warnings"],
        }
        rows.append(row)

    out = args.out or Path(f"{args.rom}.super.{args.format}")
    if args.format == "json":
        out.write_text(json.dumps(rows, indent=2, ensure_ascii=False), encoding="utf-8")
    else:
        if not rows:
            out.write_text("", encoding="utf-8")
        else:
            # CSV: one row per logical partition
            flat_rows: list[dict[str, Any]] = []
            for row in rows:
                base = {
                    k: v
                    for k, v in row.items()
                    if k not in ("logical_partitions", "warnings")
                }
                for part in row["logical_partitions"]:
                    flat = dict(base)
                    flat["partition_name"] = part["name"]
                    flat["partition_size_bytes"] = part["size_bytes"]
                    flat["partition_num_extents"] = part["num_extents"]
                    flat_rows.append(flat)
            if not flat_rows:
                out.write_text("", encoding="utf-8")
            else:
                keys = list(flat_rows[0].keys())
                with out.open("w", newline="", encoding="utf-8") as f:
                    writer = csv.DictWriter(f, fieldnames=keys)
                    writer.writeheader()
                    writer.writerows(flat_rows)

    print(f"Wrote {args.format.upper()}: {out}")
    for row in rows:
        if row["warnings"]:
            print(f"Warnings for {row['source_filename']}:")
            for w in row["warnings"]:
                print(f"  - {w}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
