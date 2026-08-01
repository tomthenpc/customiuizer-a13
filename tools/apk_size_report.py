#!/usr/bin/env python3
"""Deterministic, APK-only size attribution tool.

Does not require ADB, sign or any runtime. Reads the ZIP central directory
and produces JSON/Markdown reports. Can compare two APKs to produce a
delta report suitable for a CI budget gate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Optional


@dataclass(frozen=True)
class ApkEntry:
    name: str
    compressed: int
    uncompressed: int
    sha256: str


@dataclass
class ApkSizeReport:
    apk_path: str
    total_compressed: int
    total_uncompressed: int
    entry_count: int
    entries: list[ApkEntry]
    by_extension: dict[str, dict[str, int]]
    by_prefix: dict[str, dict[str, int]]
    sha256: str

    def to_json(self, indent: int = 2) -> str:
        return json.dumps(asdict(self), indent=indent, ensure_ascii=False, sort_keys=True)

    def to_markdown(self) -> str:
        lines = [
            "# APK Size Report",
            "",
            f"- APK: `{self.apk_path}`",
            f"- SHA-256: `{self.sha256}`",
            f"- Total compressed: {self.total_compressed:,} bytes",
            f"- Total uncompressed: {self.total_uncompressed:,} bytes",
            f"- Entries: {self.entry_count}",
            "",
            "## Top 20 entries",
            "| File | Compressed | Uncompressed |",
            "|---|---|---|",
        ]
        for e in sorted(self.entries, key=lambda x: x.compressed, reverse=True)[:20]:
            lines.append(f"| `{e.name}` | {e.compressed:,} | {e.uncompressed:,} |")

        lines += ["", "## By extension", "| Extension | Compressed | Count |", "|---|---|---|"]
        for ext, data in sorted(
            self.by_extension.items(), key=lambda x: x[1]["compressed"], reverse=True
        ):
            lines.append(f"| {ext or '(none)'} | {data['compressed']:,} | {data['count']} |")

        lines += ["", "## By prefix", "| Prefix | Compressed | Count |", "|---|---|---|"]
        for prefix, data in sorted(
            self.by_prefix.items(), key=lambda x: x[1]["compressed"], reverse=True
        ):
            lines.append(f"| {prefix} | {data['compressed']:,} | {data['count']} |")

        return "\n".join(lines)


def _entry_prefix(name: str) -> str:
    if "/" in name:
        return name.split("/", 1)[0] + "/"
    return "(root)"


def _entry_extension(name: str) -> str:
    parts = name.rsplit(".", 1)
    if len(parts) == 2 and "/" not in parts[1]:
        return parts[1].lower()
    return "(none)"


def _sha256_of_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _sha256_of_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        while True:
            block = f.read(8192)
            if not block:
                break
            h.update(block)
    return h.hexdigest()


def report(apk_path: Path, verify_sha: bool = False) -> ApkSizeReport:
    entries: list[ApkEntry] = []
    total_compressed = 0
    total_uncompressed = 0

    with zipfile.ZipFile(apk_path, "r") as zf:
        for info in zf.infolist():
            raw = zf.read(info.filename)
            entry = ApkEntry(
                name=info.filename,
                compressed=info.compress_size,
                uncompressed=info.file_size,
                sha256=_sha256_of_bytes(raw) if verify_sha else "",
            )
            entries.append(entry)
            total_compressed += info.compress_size
            total_uncompressed += info.file_size

    by_extension: dict[str, Counter] = defaultdict(Counter)
    by_prefix: dict[str, Counter] = defaultdict(Counter)

    for e in entries:
        by_extension[_entry_extension(e.name)]["compressed"] += e.compressed
        by_extension[_entry_extension(e.name)]["count"] += 1
        by_extension[_entry_extension(e.name)]["uncompressed"] += e.uncompressed
        by_prefix[_entry_prefix(e.name)]["compressed"] += e.compressed
        by_prefix[_entry_prefix(e.name)]["count"] += 1
        by_prefix[_entry_prefix(e.name)]["uncompressed"] += e.uncompressed

    def to_sorted_dict(counter: dict[str, Counter]) -> dict[str, dict[str, int]]:
        return {k: dict(v) for k, v in sorted(counter.items())}

    return ApkSizeReport(
        apk_path=str(apk_path),
        total_compressed=total_compressed,
        total_uncompressed=total_uncompressed,
        entry_count=len(entries),
        entries=sorted(entries, key=lambda x: x.name),
        by_extension=to_sorted_dict(by_extension),
        by_prefix=to_sorted_dict(by_prefix),
        sha256=_sha256_of_file(apk_path),
    )


def compare(before: Path, after: Path) -> dict[str, object]:
    r1 = report(before)
    r2 = report(after)

    old = {e.name: e for e in r1.entries}
    new = {e.name: e for e in r2.entries}

    added = [name for name in sorted(new) if name not in old]
    removed = [name for name in sorted(old) if name not in new]
    changed = [
        {
            "name": name,
            "before_compressed": old[name].compressed,
            "after_compressed": new[name].compressed,
            "delta": new[name].compressed - old[name].compressed,
        }
        for name in sorted(new)
        if name in old and old[name].compressed != new[name].compressed
    ]

    return {
        "before": r1.apk_path,
        "after": r2.apk_path,
        "before_total": r1.total_compressed,
        "after_total": r2.total_compressed,
        "total_delta": r2.total_compressed - r1.total_compressed,
        "added_count": len(added),
        "removed_count": len(removed),
        "changed_count": len(changed),
        "added_files": added,
        "removed_files": removed,
        "changed_files": sorted(changed, key=lambda x: x["delta"], reverse=True),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="APK size attribution")
    parser.add_argument("apk", type=Path)
    parser.add_argument("--compare-with", type=Path, default=None)
    parser.add_argument("--json", type=Path, default=None)
    parser.add_argument("--md", type=Path, default=None)
    args = parser.parse_args()

    if args.compare_with:
        result = compare(args.apk, args.compare_with)
        output = json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True)
    else:
        result = report(args.apk)
        output = result.to_json()

    if args.json:
        args.json.write_text(output, encoding="utf-8")
    else:
        print(output)

    if args.md and not args.compare_with:
        Path(args.md).write_text(report(args.apk).to_markdown(), encoding="utf-8")


if __name__ == "__main__":
    main()
