#!/usr/bin/env python3
"""Extract logical partitions from an Android sparse/raw super image.

Uses liblp metadata and the correct linear-extent offset formula:

    start_byte = target_data * LP_SECTOR_SIZE

`target_data` is the absolute physical sector on the target block device
and must not be mixed with `first_logical_sector`.

Typical usage:
    # list partitions
    python tools/rom_part_extractor.py super.img --list

    # extract a single partition
    python tools/rom_part_extractor.py super.img system_a --out system_a.img

    # extract all partitions to a directory
    python tools/rom_part_extractor.py super.img --all --out-dir out/
"""
from __future__ import annotations

import argparse
import shutil
import struct
import sys
import tempfile
from pathlib import Path
from typing import Any

# Keep in sync with rom_super_inspector.py
SPARSE_MAGIC = 0xED26FF3A
SPARSE_HEADER_FMT = "<IHHHHIII I"
SPARSE_CHUNK_FMT = "<HHII"
CHUNK_RAW = 0xCAC1
CHUNK_FILL = 0xCAC2
CHUNK_DONT_CARE = 0xCAC3
CHUNK_CRC = 0xCAC4
LP_SECTOR_SIZE = 512


def is_sparse_image(path: Path) -> bool:
    try:
        with path.open("rb") as f:
            magic = struct.unpack("<I", f.read(4))[0]
            return magic == SPARSE_MAGIC
    except (OSError, struct.error):
        return False


def unsparse(src: Path, dst: Path, progress: bool = True) -> int:
    """Unsparse a complete sparse image to `dst`. Returns bytes written."""
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
        for i in range(total_chunks):
            chunk_data = fsrc.read(12)
            if len(chunk_data) < 12:
                break
            chunk_type, reserved, chunk_sz, total_sz = struct.unpack(
                SPARSE_CHUNK_FMT, chunk_data
            )
            data_len = total_sz - 12

            if chunk_type == CHUNK_RAW:
                fill_len = chunk_sz * blk_sz
                fdst.write(fsrc.read(data_len))
                written += data_len
                if data_len != fill_len:
                    # Sparse spec allows data_len < fill_len? pad with zeros
                    fdst.write(b"\x00" * (fill_len - data_len))
                    written += fill_len - data_len
            elif chunk_type == CHUNK_FILL:
                fill_val = fsrc.read(data_len)
                if not fill_val:
                    fill_val = b"\x00"
                fill_len = chunk_sz * blk_sz
                fdst.write(fill_val * (fill_len // len(fill_val)))
                fdst.write(fill_val[: fill_len % len(fill_val)])
                written += fill_len
            elif chunk_type == CHUNK_DONT_CARE:
                fill_len = chunk_sz * blk_sz
                fdst.write(b"\x00" * fill_len)
                written += fill_len
            elif chunk_type == CHUNK_CRC:
                # CRC-32 per chunk; seek past it
                fsrc.seek(4, 1)
            else:
                fsrc.seek(data_len, 1)

            if progress and i % 50 == 0:
                print(f"  unsparse chunk {i + 1}/{total_chunks}", file=sys.stderr)

        return written


def unsparse_head(src: Path, dst: Path, max_bytes: int) -> int:
    """Unsparse just the first `max_bytes` of a sparse image."""
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


def read_metadata(super_path: Path, liblp: Any) -> Any:
    """Return liblp metadata for a sparse or raw super image."""
    if is_sparse_image(super_path):
        with tempfile.TemporaryDirectory(prefix="rom_part_extractor_") as tmp:
            raw = Path(tmp) / "super_head.raw"
            unsparse_head(super_path, raw, 8 * 1024 * 1024)
            return liblp.ReadMetadata(str(raw), 0)
    else:
        return liblp.ReadMetadata(str(super_path), 0)


def estimate_partition_size(super_path: Path, part_name: str, liblp: Any) -> int | None:
    """Return the extracted size in bytes for `part_name`, or None on error."""
    try:
        metadata = read_metadata(super_path, liblp)
    except Exception:
        return None
    for p in metadata.partitions:
        name = p.name.decode("utf-8", "replace").rstrip("\x00")
        if name != part_name:
            continue
        return sum(metadata.extents[ei].num_sectors * LP_SECTOR_SIZE for ei in range(
            p.first_extent_index, p.first_extent_index + p.num_extents
        ) if ei < len(metadata.extents))
    return None


def _check_disk_space(path: Path, needed_bytes: int) -> None:
    """Raise RuntimeError if the filesystem at `path` cannot fit `needed_bytes`."""
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


def extract_partition(
    super_path: Path,
    part_name: str,
    out_path: Path,
    liblp: Any,
    verify: bool = True,
) -> dict[str, Any]:
    """Extract `part_name` from `super_path` to `out_path`."""
    metadata = read_metadata(super_path, liblp)

    if not metadata.partitions:
        raise ValueError(f"{super_path}: no logical partitions")

    if len(metadata.block_devices) == 0:
        raise ValueError(f"{super_path}: no block devices")

    super_device = None
    for idx, bd in enumerate(metadata.block_devices):
        if bd.partition_name.decode("utf-8", "replace").rstrip("\x00") == "super":
            super_device = bd
            super_device_index = idx
            break

    if super_device is None:
        super_device = metadata.block_devices[0]
        super_device_index = 0

    for p in metadata.partitions:
        name = p.name.decode("utf-8", "replace").rstrip("\x00")
        if name != part_name:
            continue

        warnings: list[str] = []
        extent_count = 0

        with out_path.open("wb") as out:
            for ei in range(p.first_extent_index, p.first_extent_index + p.num_extents):
                if ei >= len(metadata.extents):
                    warnings.append(f"extent index {ei} out of range")
                    break
                ext = metadata.extents[ei]

                if ext.target_type != liblp.LP_TARGET_TYPE_LINEAR:
                    warnings.append(
                        f"extent {ei} has unsupported target_type {ext.target_type}; skipped"
                    )
                    continue

                if ext.target_source >= len(metadata.block_devices):
                    warnings.append(
                        f"extent {ei} target_source {ext.target_source} out of range; skipped"
                    )
                    continue

                bd = metadata.block_devices[ext.target_source]
                bd_name = bd.partition_name.decode("utf-8", "replace").rstrip("\x00")

                if ext.target_source != super_device_index:
                    warnings.append(
                        f"extent {ei} maps to block device {bd_name} (index {ext.target_source}), "
                        f"not the super image; cannot extract from super.img"
                    )
                    continue

                start_sector = int(ext.target_data)
                num_sectors = int(ext.num_sectors)
                end_sector = start_sector + num_sectors

                if verify:
                    if start_sector < bd.first_logical_sector:
                        warnings.append(
                            f"extent {ei} starts at sector {start_sector}, "
                            f"before block device {bd_name} first_logical_sector {bd.first_logical_sector}"
                        )
                    max_sectors = bd.size // LP_SECTOR_SIZE
                    if end_sector > max_sectors:
                        warnings.append(
                            f"extent {ei} ends at sector {end_sector}, "
                            f"past block device {bd_name} size {max_sectors} sectors"
                        )

                start_byte = start_sector * LP_SECTOR_SIZE
                length = num_sectors * LP_SECTOR_SIZE

                with super_path.open("rb") as sf:
                    if is_sparse_image(super_path):
                        # For sparse images, we must unsparse on the fly.
                        _copy_from_sparse(sf, out, start_byte, length)
                    else:
                        sf.seek(start_byte)
                        out.write(sf.read(length))

                extent_count += 1

        return {
            "partition_name": name,
            "extracted_bytes": out_path.stat().st_size if out_path.exists() else 0,
            "num_extents": p.num_extents,
            "extracted_extents": extent_count,
            "warnings": warnings,
        }

    raise ValueError(f"partition '{part_name}' not found in {super_path}")


def _copy_from_sparse(sparse_f, out, start_byte: int, length: int) -> None:
    """Copy `length` bytes starting at `start_byte` from a sparse image."""
    import struct

    # Re-read sparse header
    hdr = sparse_f.read(28)
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

    # Track current byte position in unsparse stream
    pos = 0
    end = start_byte + length
    chunks_read = 0

    while pos < end and chunks_read < total_chunks:
        chunk_data = sparse_f.read(12)
        if len(chunk_data) < 12:
            break
        chunk_type, reserved, chunk_sz, total_sz = struct.unpack(
            SPARSE_CHUNK_FMT, chunk_data
        )
        data_len = total_sz - 12
        chunk_len = chunk_sz * blk_sz

        if chunk_type == CHUNK_RAW:
            raw = sparse_f.read(data_len)
            # raw size may equal or be less than chunk_len; fill if needed
            if len(raw) < chunk_len:
                raw += b"\x00" * (chunk_len - len(raw))
            if pos + chunk_len > start_byte:
                out_start = max(0, start_byte - pos)
                out_end = min(chunk_len, end - pos)
                out.write(raw[out_start:out_end])
            pos += chunk_len
        elif chunk_type == CHUNK_FILL:
            fill_val = sparse_f.read(data_len)
            if not fill_val:
                fill_val = b"\x00"
            if pos + chunk_len > start_byte:
                out_start = max(0, start_byte - pos)
                out_end = min(chunk_len, end - pos)
                out.write(fill_val * ((out_end - out_start) // len(fill_val)))
                out.write(fill_val[: (out_end - out_start) % len(fill_val)])
            pos += chunk_len
        elif chunk_type == CHUNK_DONT_CARE:
            if pos + chunk_len > start_byte:
                out_start = max(0, start_byte - pos)
                out_end = min(chunk_len, end - pos)
                out.write(b"\x00" * (out_end - out_start))
            pos += chunk_len
        elif chunk_type == CHUNK_CRC:
            sparse_f.seek(4, 1)
        else:
            sparse_f.seek(data_len, 1)

        chunks_read += 1


def _cleanup_tree(path: Path, label: str = "temporary workdir") -> None:
    if not path.exists():
        return
    try:
        if path.is_dir():
            shutil.rmtree(path)
        else:
            path.unlink()
    except OSError as e:
        size = (
            sum(f.stat().st_size for f in path.rglob("*") if f.is_file())
            if path.is_dir()
            else path.stat().st_size
        )
        print(
            f"WARNING: failed to clean up {label}: {path} ({e}); "
            f"leftover size: {size} bytes",
            file=sys.stderr,
        )


def list_partitions(super_path: Path, liblp: Any) -> list[str]:
    metadata = read_metadata(super_path, liblp)
    return [
        p.name.decode("utf-8", "replace").rstrip("\x00")
        for p in metadata.partitions
    ]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Extract logical partitions from an Android super image."
    )
    parser.add_argument("super", type=Path, help="Sparse or raw super image.")
    parser.add_argument("partition", nargs="?", help="Partition name to extract.")
    parser.add_argument("--out", type=Path, help="Output image file. If omitted, a temporary workdir is used and cleaned unless --keep-workdir is set.")
    parser.add_argument("--out-dir", type=Path, help="Output directory for --all. If omitted with --all, a temporary workdir is used and cleaned unless --keep-workdir is set.")
    parser.add_argument("--list", action="store_true", help="List partitions.")
    parser.add_argument("--all", action="store_true", help="Extract all partitions.")
    parser.add_argument("--no-verify", action="store_true", help="Skip extent bounds verification.")
    parser.add_argument("--keep-workdir", action="store_true", help="Keep the temporary workdir used for extraction.")
    args = parser.parse_args(argv)

    liblp = try_import_liblp()
    if liblp is None:
        print("ERROR: liblp Python package not installed", file=sys.stderr)
        return 2

    if args.list:
        for name in list_partitions(args.super, liblp):
            print(name)
        return 0

    if args.all:
        if args.out_dir:
            work_dir = args.out_dir
            work_dir.mkdir(parents=True, exist_ok=True)
        else:
            work_dir = Path(tempfile.mkdtemp(prefix="rom_part_extractor_"))
        try:
            for name in list_partitions(args.super, liblp):
                out = work_dir / f"{name}.img"
                estimated = estimate_partition_size(args.super, name, liblp)
                if estimated is not None:
                    _check_disk_space(out, estimated)
                result = extract_partition(
                    args.super, name, out, liblp, verify=not args.no_verify
                )
                print(result)
        finally:
            if not args.keep_workdir and not args.out_dir:
                _cleanup_tree(work_dir, label="temporary workdir")
        return 0

    if not args.partition:
        print("ERROR: specify a partition name or --list", file=sys.stderr)
        return 1

    if args.out:
        out = args.out
        work_dir = None
    else:
        work_dir = Path(tempfile.mkdtemp(prefix="rom_part_extractor_"))
        out = work_dir / f"{args.partition}.img"

    try:
        estimated = estimate_partition_size(args.super, args.partition, liblp)
        if estimated is not None:
            print(f"estimated extracted size: {estimated} bytes")
            _check_disk_space(out, estimated)
        result = extract_partition(
            args.super, args.partition, out, liblp, verify=not args.no_verify
        )
        print(result)
    finally:
        if not args.keep_workdir and not args.out:
            if out.exists():
                _cleanup_tree(out, label="temporary partition")
            if work_dir is not None and work_dir.exists():
                _cleanup_tree(work_dir, label="temporary workdir")
    return 0


if __name__ == "__main__":
    sys.exit(main())
