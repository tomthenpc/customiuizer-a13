#!/usr/bin/env python3
"""ROM sample inventory generator.

Scans a local directory for APK/JAR/unknown ROM sample files, computes
SHA-256, optionally queries installed Android SDK / Java tooling for
metadata, and produces a JSON or CSV inventory without uploading files.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import shutil
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def sample_type(path: Path) -> str:
    ext = path.suffix.lower()
    if ext == ".apk":
        return "APK"
    if ext == ".jar":
        return "JAR"
    return "unknown"


def detect_zip(path: Path) -> bool:
    try:
        with path.open("rb") as f:
            return f.read(4) == b"PK\x03\x04"
    except OSError:
        return False


def tool_available(name: str) -> bool:
    return shutil.which(name) is not None


def try_apkanalyzer(path: Path) -> dict[str, str]:
    if not tool_available("apkanalyzer"):
        return {}
    meta: dict[str, str] = {}
    try:
        # apkanalyzer manifest print <apk>
        out = subprocess.check_output(
            ["apkanalyzer", "manifest", "print", str(path)],
            text=True,
            stderr=subprocess.STDOUT,
            timeout=60,
        )
        # Very rough XML tag extraction
        pkg = re.search(r'package="([^"]+)"', out)
        if pkg:
            meta["package"] = pkg.group(1)
        ver_name = re.search(r'android:versionName="([^"]+)"', out)
        if ver_name:
            meta["versionName"] = ver_name.group(1)
        ver_code = re.search(r'android:versionCode="([0-9]+)"', out)
        if ver_code:
            meta["versionCode"] = ver_code.group(1)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError):
        pass
    return meta


def try_aapt(path: Path) -> dict[str, str]:
    for tool in ("aapt2", "aapt"):
        if not tool_available(tool):
            continue
        try:
            out = subprocess.check_output(
                [tool, "dump", "badging", str(path)],
                text=True,
                stderr=subprocess.DEVNULL,
                timeout=60,
            )
            meta: dict[str, str] = {}
            pkg = re.search(r"package:\s*name='([^']+)'\s*versionCode='([^']+)'\s*versionName='([^']+)'", out)
            if pkg:
                meta["package"] = pkg.group(1)
                meta["versionCode"] = pkg.group(2)
                meta["versionName"] = pkg.group(3)
            return meta
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError):
            continue
    return {}


def try_javap_classes(path: Path) -> list[str]:
    if not tool_available("javap"):
        return []
    try:
        with zipfile.ZipFile(path) as zf:
            classes = [
                n[:-6].replace("/", ".")
                for n in zf.namelist()
                if n.endswith(".class") and not n.startswith("META-INF/")
            ]
        return classes
    except (zipfile.BadZipFile, OSError):
        return []


def try_jadx_summary(path: Path) -> dict[str, Any]:
    if not tool_available("jadx"):
        return {}
    try:
        out = subprocess.check_output(
            ["jadx", "-d", "-", "--deobf", str(path)],
            text=True,
            stderr=subprocess.DEVNULL,
            timeout=120,
        )
        return {"decompile_output_lines": len(out.splitlines())}
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError):
        return {}


def extract_apk_metadata(path: Path, warnings: list[str]) -> dict[str, Any]:
    meta: dict[str, Any] = {}
    if tool_available("apkanalyzer"):
        meta.update(try_apkanalyzer(path))
    elif tool_available("aapt") or tool_available("aapt2"):
        meta.update(try_aapt(path))
    else:
        warnings.append("apkanalyzer/aapt/aapt2 not available; APK metadata not extracted")
    return meta


def extract_jar_metadata(path: Path, warnings: list[str]) -> dict[str, Any]:
    meta: dict[str, Any] = {}
    if tool_available("javap"):
        classes = try_javap_classes(path)
        meta["class_count"] = len(classes)
        meta["classes_sample"] = classes[:20]
    else:
        warnings.append("javap not available; JAR class listing not performed")
    if tool_available("jadx"):
        meta.update(try_jadx_summary(path))
    return meta


def inventory_row(path: Path, base_dir: Path, args: argparse.Namespace, warnings: list[str]) -> dict[str, Any]:
    ftype = sample_type(path)
    if ftype == "unknown" and detect_zip(path):
        ftype = "ZIP"

    row: dict[str, Any] = {
        "sampleId": sha256(path),
        "device": args.device or "",
        "codename": args.codename or "",
        "Android": args.android or "",
        "SDK": args.sdk or "",
        "MIUI_HyperOS": args.miui or "",
        "build_fingerprint": args.fingerprint or "",
        "package": "",
        "process": args.process or "",
        "versionName": "",
        "versionCode": "",
        "source_filename": str(path.relative_to(base_dir)),
        "SHA256": sha256(path),
        "sample_type": ftype,
        "collection_date": args.collection_date or datetime.now(timezone.utc).isoformat(),
        "verification_status": args.status or "UNVERIFIED",
    }

    if ftype == "APK":
        row.update(extract_apk_metadata(path, warnings))
    elif ftype == "JAR" or ftype == "ZIP":
        row.update(extract_jar_metadata(path, warnings))

    return row


def collect_files(root: Path) -> list[Path]:
    files = []
    for p in root.rglob("*"):
        if p.is_file():
            files.append(p)
    return sorted(files)


def write_json(rows: list[dict[str, Any]], out: Path) -> None:
    out.write_text(json.dumps(rows, indent=2, ensure_ascii=False), encoding="utf-8")


def write_csv(rows: list[dict[str, Any]], out: Path) -> None:
    if not rows:
        out.write_text("", encoding="utf-8")
        return
    keys = list(rows[0].keys())
    with out.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=keys)
        writer.writeheader()
        writer.writerows(rows)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Scan a directory and build a local ROM sample inventory.")
    parser.add_argument("-d", "--dir", required=True, type=Path, help="Directory to scan.")
    parser.add_argument("-o", "--out", type=Path, default=None, help="Output file (default: <dir>.json).")
    parser.add_argument("-f", "--format", choices=("json", "csv"), default="json", help="Output format.")
    parser.add_argument("--device", default="", help="Device name.")
    parser.add_argument("--codename", default="", help="Device codename.")
    parser.add_argument("--android", default="", help="Android version.")
    parser.add_argument("--sdk", default="", help="Android SDK level.")
    parser.add_argument("--miui", default="", help="MIUI / HyperOS version.")
    parser.add_argument("--fingerprint", default="", help="Build fingerprint.")
    parser.add_argument("--process", default="", help="Target process.")
    parser.add_argument("--status", default="UNVERIFIED", choices=(
        "COMPILE_STUB", "LOCAL_ROM_SAMPLE", "DEVICE_EXTRACTED", "UPSTREAM_REFERENCE", "UNVERIFIED"
    ), help="Verification status for all samples.")
    parser.add_argument("--collection-date", default="", help="ISO-8601 collection date.")
    parser.add_argument("--with-tools", action="store_true", help="Attempt apkanalyzer/javap/jadx if installed.")
    args = parser.parse_args(argv)

    if not args.dir.is_dir():
        print(f"ERROR: {args.dir} is not a directory", file=sys.stderr)
        return 1

    warnings: list[str] = []
    rows: list[dict[str, Any]] = []

    for path in collect_files(args.dir):
        if not args.with_tools:
            # basic inventory: sha, name, type
            ftype = sample_type(path)
            if ftype == "unknown" and detect_zip(path):
                ftype = "ZIP"
            rows.append({
                "sampleId": sha256(path),
                "device": args.device or "",
                "codename": args.codename or "",
                "Android": args.android or "",
                "SDK": args.sdk or "",
                "MIUI_HyperOS": args.miui or "",
                "build_fingerprint": args.fingerprint or "",
                "package": "",
                "process": args.process or "",
                "versionName": "",
                "versionCode": "",
                "source_filename": str(path.relative_to(args.dir)),
                "SHA256": sha256(path),
                "sample_type": ftype,
                "collection_date": args.collection_date or datetime.now(timezone.utc).isoformat(),
                "verification_status": args.status,
            })
        else:
            rows.append(inventory_row(path, args.dir, args, warnings))

    if not args.with_tools:
        warnings.append("Tool analysis skipped. Re-run with --with-tools to use apkanalyzer/javap/jadx.")
    else:
        for tool in ("apkanalyzer", "aapt", "aapt2", "javap", "jadx"):
            if not tool_available(tool):
                warnings.append(f"{tool} not found in PATH; using degraded mode for files that need it")

    out = args.out or Path(f"{args.dir}.json" if args.format == "json" else f"{args.dir}.csv")
    if args.format == "json":
        write_json(rows, out)
    else:
        write_csv(rows, out)

    print(f"Wrote inventory: {out}")
    if warnings:
        print("Warnings:")
        for w in warnings:
            print(f"  - {w}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
