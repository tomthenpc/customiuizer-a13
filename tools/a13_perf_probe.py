#!/usr/bin/env python3
"""Lightweight, standard-library-only ADB performance probe for A13.

Collects memory, thread, CPU and startup metrics from an Android device over
adb, without modifying the device, installing instrumentation, or adding
runtime dependencies in the Android app.

Examples:
    python tools/a13_perf_probe.py doctor
    python tools/a13_perf_probe.py process \
        --package tv.withaibuild.customiuizer.r13 \
        --scenario manager_idle \
        --module-state enabled_features_off \
        --repeat 5 \
        --output out/a13-perf/manager-idle.json
    python tools/a13_perf_probe.py startup \
        --package tv.withaibuild.customiuizer.r13 \
        --activity tv.withaibuild.customiuizer.MainActivity \
        --repeat 5 \
        --output out/a13-perf/manager-startup.json
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import statistics
import subprocess
import sys
import time
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT_DIR = REPO_ROOT / "out" / "a13-perf"

METRIC_UNITS: dict[str, str] = {
    "total_pss_kb": "KiB",
    "java_heap_kb": "KiB",
    "native_heap_kb": "KiB",
    "graphics_kb": "KiB",
    "private_dirty_kb": "KiB",
    "thread_count": "count",
    "cpu_time_ms": "ms",
    "startup_total_time_ms": "ms",
    "gc_count": "count",
    "gc_freed_objects": "count",
    "binder_proxy_count": "count",
    "loaded_class_count": "count",
    "process_restart_count": "count",
}

METRIC_SOURCES: dict[str, str] = {
    "total_pss_kb": "adb shell dumpsys meminfo <pid>",
    "java_heap_kb": "adb shell dumpsys meminfo <pid>",
    "native_heap_kb": "adb shell dumpsys meminfo <pid>",
    "graphics_kb": "adb shell dumpsys meminfo <pid>",
    "private_dirty_kb": "adb shell dumpsys meminfo <pid>",
    "thread_count": "/proc/<pid>/status",
    "cpu_time_ms": "/proc/<pid>/stat",
    "startup_total_time_ms": "adb shell am start -W",
    "gc_count": "dumpsys procstats --hours 0",
    "gc_freed_objects": "dumpsys procstats --hours 0",
    "binder_proxy_count": "/proc/<pid>/status",
    "loaded_class_count": "NOT_IMPLEMENTED_IN_PROBE",
    "process_restart_count": "dumpsys procstats --hours 0 <package>",
}


class ProbeError(Exception):
    """User-facing error with a stable reason code."""

    def __init__(self, message: str, reason: str = "probe_error") -> None:
        super().__init__(message)
        self.message = message
        self.reason = reason


@dataclass
class Sample:
    """One raw measurement sample."""

    taken_at: str
    metrics: dict[str, Any]
    errors: list[str] = field(default_factory=list)
    notes: str = ""


@dataclass
class Record:
    """Aggregated record ready to be merged into A13_PERF_BASELINE.json."""

    device: str
    rom: str
    android_version: str
    build_variant: str
    measurement_time: str
    measurement_method: str
    repetitions: int
    aggregation_method: str
    module_state: str
    process: dict[str, str]
    scenario: str
    metrics: dict[str, Any]
    raw_samples: list[dict[str, Any]]
    median: dict[str, Any]
    notes: str
    unavailable_reason: str = ""

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


def _safe_print(msg: str) -> None:
    """Write a message to stdout, replacing unsupported characters on Windows."""
    try:
        print(msg)
    except UnicodeEncodeError:
        if hasattr(sys.stdout, "buffer"):
            encoded = msg.encode(sys.stdout.encoding, errors="replace")
            sys.stdout.buffer.write(encoded + b"\n")


def _median(values: list[float | int | None]) -> float | None:
    """Return median of non-None numeric values, or None if all missing."""
    nums = [v for v in values if v is not None and isinstance(v, (int, float))]
    if not nums:
        return None
    return statistics.median(nums)


class Adb:
    """Minimal ADB wrapper with error handling and optional device override."""

    def __init__(self, adb_path: str | None = None, device: str | None = None) -> None:
        self.adb = adb_path or self._find_adb()
        self.device = device

    @staticmethod
    def _find_adb() -> str:
        exe = "adb.exe" if os.name == "nt" else "adb"
        found = shutil.which(exe) or shutil.which("adb")
        if not found:
            raise ProbeError(
                "adb not found in PATH. Install Android SDK platform-tools and add to PATH.",
                reason="adb_missing",
            )
        return found

    def build_cmd(self, subcmd: list[str]) -> list[str]:
        cmd = [self.adb]
        if self.device:
            cmd.extend(["-s", self.device])
        cmd.extend(subcmd)
        return cmd

    def run(
        self,
        subcmd: list[str],
        check: bool = True,
        timeout: int = 60,
        encoding: str = "utf-8",
    ) -> subprocess.CompletedProcess[str]:
        cmd = self.build_cmd(subcmd)
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                encoding=encoding,
                errors="replace",
                timeout=timeout,
            )
        except FileNotFoundError as exc:
            raise ProbeError(
                f"adb executable not found: {self.adb}",
                reason="adb_not_executable",
            ) from exc
        except subprocess.TimeoutExpired as exc:
            raise ProbeError(
                f"adb command timed out after {timeout}s: {' '.join(exc.cmd)}",
                reason="adb_timeout",
            ) from exc
        if check and result.returncode != 0:
            err = (result.stderr or "").strip()
            out = (result.stdout or "").strip()
            detail = err or out or f"exit code {result.returncode}"
            raise ProbeError(
                f"adb command failed: {detail}",
                reason="adb_error",
            )
        return result

    def version(self) -> str:
        return self.run(["version"]).stdout.strip()

    def devices(self) -> list[dict[str, str]]:
        result = self.run(["devices", "-l"])
        lines = [line.strip() for line in result.stdout.splitlines() if line.strip()]
        devices: list[dict[str, str]] = []
        for line in lines:
            if line.startswith("List of devices"):
                continue
            parts = line.split()
            if not parts:
                continue
            serial = parts[0]
            state = parts[1] if len(parts) > 1 else "unknown"
            props = {}
            for token in parts[2:]:
                if ":" in token:
                    k, v = token.split(":", 1)
                    props[k] = v
            devices.append({"serial": serial, "state": state, **props})
        return devices

    def shell(self, command: str, check: bool = True, timeout: int = 60) -> str:
        result = self.run(["shell", command], check=check, timeout=timeout)
        return result.stdout


def doctor(adb: Adb) -> dict[str, Any]:
    """Check adb, device connectivity, and shell access."""
    report: dict[str, Any] = {
        "adb_path": adb.adb,
        "version": "",
        "devices": [],
        "selected_device": adb.device,
        "shell_ok": False,
        "shell_user": "",
        "errors": [],
    }
    try:
        report["version"] = adb.version()
    except ProbeError as exc:
        report["errors"].append(exc.message)
        return report

    try:
        devices = adb.devices()
        report["devices"] = devices
    except ProbeError as exc:
        report["errors"].append(exc.message)
        return report

    online = [d for d in devices if d.get("state") == "device"]
    if not online:
        report["errors"].append("No device online. Connect a device and enable USB debugging.")
        return report

    if not adb.device:
        if len(online) == 1:
            adb.device = online[0]["serial"]
            report["selected_device"] = adb.device
        else:
            report["errors"].append(
                f"Multiple devices online ({len(online)}). Use --device <serial>."
            )
            return report

    try:
        shell_out = adb.shell("whoami; echo ---; id", check=False, timeout=15)
        report["shell_ok"] = True
        report["shell_user"] = shell_out.split("---")[0].strip() if "---" in shell_out else shell_out.strip()
    except ProbeError as exc:
        report["errors"].append(exc.message)

    return report


def _find_pid_for_package(adb: Adb, package: str) -> int:
    """Find a PID for the given package name."""
    out = adb.shell(f"pidof {package}", check=False, timeout=15).strip()
    if out:
        pids = [int(p) for p in re.split(r"\s+", out) if p.isdigit()]
        if pids:
            return min(pids)  # main process usually has the lowest pid

    # Fallback: ps -A and grep the package basename.
    ps_out = adb.shell("ps -A", check=False, timeout=15)
    for line in ps_out.splitlines():
        if package in line:
            parts = line.split()
            if parts and parts[1].isdigit():
                return int(parts[1])
    raise ProbeError(
        f"Process for package '{package}' not found. Is the app running?",
        reason="process_not_found",
    )


def _parse_int(text: str) -> int | None:
    text = text.strip().replace(",", "")
    if text and text.lstrip("-").isdigit():
        return int(text)
    return None


def _parse_first_number(line: str) -> int | None:
    tokens = line.replace(",", "").split()
    for t in tokens:
        t = t.strip(":")
        if t.lstrip("-").isdigit():
            return int(t)
    return None


def parse_meminfo(text: str) -> dict[str, Any]:
    """Parse `dumpsys meminfo <pid>` output.

    Tolerates both the detailed table and the compact summary forms.
    """
    result: dict[str, Any] = {
        "total_pss_kb": None,
        "java_heap_kb": None,
        "native_heap_kb": None,
        "graphics_kb": None,
        "private_dirty_kb": None,
    }
    if not text or not text.strip():
        return result

    # Look for category rows like "  Native Heap: 12345 12345 ..."
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        lower = stripped.lower()

        # Native heap
        if lower.startswith("native heap") and result["native_heap_kb"] is None:
            result["native_heap_kb"] = _parse_first_number(stripped)
        # Java / Dalvik heap
        elif (lower.startswith("dalvik heap") or lower.startswith("java heap")) and result["java_heap_kb"] is None:
            result["java_heap_kb"] = _parse_first_number(stripped)
        # Graphics variants
        elif (
            lower.startswith("graphics")
            or lower.startswith("gfx")
            or lower.startswith("egl")
            or lower.startswith("gl")
        ) and result["graphics_kb"] is None:
            result["graphics_kb"] = _parse_first_number(stripped)

    # Total PSS by OOM adjustment / summary forms
    for line in text.splitlines():
        lower = line.lower()
        if result["total_pss_kb"] is None and ("total pss" in lower or "total pss by category" in lower):
            val = _parse_first_number(line)
            if val is not None and val > 0:
                result["total_pss_kb"] = val
        if result["private_dirty_kb"] is None and ("total private dirty" in lower or "private dirty" in lower):
            val = _parse_first_number(line)
            if val is not None and val > 0:
                result["private_dirty_kb"] = val

    # Final fallback for total: the "TOTAL" row in the numeric table.
    if result["total_pss_kb"] is None:
        for line in text.splitlines():
            stripped = line.strip()
            if stripped.upper().startswith("TOTAL"):
                val = _parse_first_number(stripped)
                if val is not None and val > 0:
                    result["total_pss_kb"] = val
                    break

    return result


def parse_proc_status(text: str) -> dict[str, Any]:
    """Parse /proc/<pid>/status for thread count and other fields."""
    result: dict[str, Any] = {"thread_count": None, "binder_proxy_count": None}
    if not text:
        return result
    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("Threads:"):
            result["thread_count"] = _parse_int(line.split(":", 1)[1])
    # Binder proxy count is not in /proc/pid/status on stock Android.
    # Leave it as None so the caller can mark unavailable.
    return result


def parse_proc_stat(text: str, clock_tick: int | None = None) -> dict[str, Any]:
    """Parse /proc/<pid>/stat for CPU time and thread count."""
    result: dict[str, Any] = {"cpu_time_ms": None, "thread_count": None}
    text = text.strip()
    if not text:
        return result
    # Format: pid (comm) state ppid ... The comm may contain spaces and ')'.
    match = re.match(r"^(\d+) \((.*)\) (.*)$", text)
    if not match:
        return result
    tail = match.group(3)
    fields = tail.split()
    # fields layout (tail split, 0-indexed):
    # [0]=state [1]=ppid [2]=pgrp [3]=session [4]=tty [5]=tpgid
    # [6]=flags [7]=minflt [8]=cminflt [9]=majflt [10]=cmajflt
    # [11]=utime [12]=stime [13]=cutime [14]=cstime
    # [15]=priority [16]=nice [17]=num_threads
    if len(fields) < 18:
        return result
    try:
        utime = int(fields[11])
        stime = int(fields[12])
        num_threads = int(fields[17])
    except ValueError:
        return result
    tick = clock_tick or 100
    result["cpu_time_ms"] = int((utime + stime) * 1000 / tick)
    result["thread_count"] = num_threads
    return result


def parse_am_start_w(text: str) -> dict[str, Any]:
    """Parse `am start -W` output for startup time."""
    result: dict[str, Any] = {"startup_total_time_ms": None}
    if not text:
        return result
    for line in text.splitlines():
        m = re.search(r"(?:TotalTime|WaitTime):\s*(\d+)", line)
        if m:
            result["startup_total_time_ms"] = int(m.group(1))
            break
    return result


def _resolve_clock_tick(adb: Adb) -> int:
    """Get the device clock tick for /proc/<pid>/stat CPU time conversion."""
    try:
        out = adb.shell("getconf CLK_TCK", check=False, timeout=10).strip()
        if out.isdigit():
            return int(out)
    except ProbeError:
        pass
    return 100


def _measure_process_once(
    adb: Adb,
    package: str,
    clock_tick: int,
) -> dict[str, Any]:
    """Take one process sample and return raw metric values."""
    pid = _find_pid_for_package(adb, package)
    metrics: dict[str, Any] = {k: None for k in METRIC_UNITS}
    errors: list[str] = []

    meminfo = adb.shell(f"dumpsys meminfo {pid}", check=False, timeout=20)
    mem = parse_meminfo(meminfo)
    for k in ("total_pss_kb", "java_heap_kb", "native_heap_kb", "graphics_kb", "private_dirty_kb"):
        metrics[k] = mem.get(k)
    if all(v is None for v in mem.values()):
        errors.append("dumpsys meminfo did not return parseable output")

    status_text = adb.shell(f"cat /proc/{pid}/status", check=False, timeout=10)
    status = parse_proc_status(status_text)
    if metrics["thread_count"] is None:
        metrics["thread_count"] = status.get("thread_count")

    stat_text = adb.shell(f"cat /proc/{pid}/stat", check=False, timeout=10)
    stat = parse_proc_stat(stat_text, clock_tick)
    if metrics["cpu_time_ms"] is None:
        metrics["cpu_time_ms"] = stat.get("cpu_time_ms")
    if metrics["thread_count"] is None:
        metrics["thread_count"] = stat.get("thread_count")

    # GC and restart data: best effort, usually unavailable without extra permissions.
    procstats = adb.shell(f"dumpsys procstats --hours 0 {package}", check=False, timeout=20)
    if not procstats.strip():
        errors.append("dumpsys procstats returned empty output")

    # Binder proxy count is not available from a stable source here.
    # loaded_class_count is intentionally not implemented in this probe.
    return {
        "pid": pid,
        "metrics": metrics,
        "errors": errors,
    }


def _build_metric_value(metric: str, raw: dict[str, Any]) -> dict[str, Any]:
    value = raw.get(metric)
    unavailable_reason = ""
    if value is None:
        unavailable_reason = "NOT_REPORTED_BY_DEVICE"
        if metric in ("gc_count", "gc_freed_objects", "process_restart_count"):
            unavailable_reason = "PROCSTATS_UNAVAILABLE_OR_UNPRIVILEGED"
        if metric == "loaded_class_count":
            unavailable_reason = "NOT_IMPLEMENTED_IN_PROBE"
        if metric == "binder_proxy_count":
            unavailable_reason = "NO_STABLE_SOURCE_IN_PROC_STATUS"
    return {
        "unit": METRIC_UNITS[metric],
        "source": METRIC_SOURCES[metric],
        "value": value,
        "unavailable_reason": unavailable_reason,
    }


def _collect_device_info(adb: Adb) -> dict[str, str]:
    info: dict[str, str] = {
        "device": "unknown",
        "rom": "unknown",
        "android_version": "unknown",
        "build_variant": "unknown",
    }
    try:
        info["device"] = adb.shell("getprop ro.product.device", check=False, timeout=10).strip() or "unknown"
        info["rom"] = adb.shell("getprop ro.miui.ui.version.name", check=False, timeout=10).strip() or adb.shell("getprop ro.build.display.id", check=False, timeout=10).strip() or "unknown"
        info["android_version"] = adb.shell("getprop ro.build.version.release", check=False, timeout=10).strip() or "unknown"
    except ProbeError:
        pass
    return info


def cmd_doctor(adb: Adb, as_json: bool = False) -> int:
    report = doctor(adb)
    if as_json:
        _safe_print(json.dumps(report, indent=2, ensure_ascii=False))
    else:
        _safe_print(f"adb path: {report['adb_path']}")
        _safe_print(f"adb version: {report['version']}")
        _safe_print(f"selected device: {report['selected_device']}")
        _safe_print(f"online devices: {len([d for d in report['devices'] if d.get('state') == 'device'])}")
        _safe_print(f"shell ok: {report['shell_ok']} ({report['shell_user']})")
        if report["errors"]:
            _safe_print("errors:")
            for err in report["errors"]:
                _safe_print(f"  - {err}")
    return 0 if not report["errors"] else 1


def cmd_process(
    adb: Adb,
    package: str,
    scenario: str,
    module_state: str,
    repeat: int,
    output: Path,
    warmup: int,
) -> int:
    clock_tick = _resolve_clock_tick(adb)
    device_info = _collect_device_info(adb)
    samples: list[Sample] = []
    success_count = 0

    for i in range(-warmup, repeat):
        taken_at = datetime.now(timezone.utc).isoformat()
        try:
            measurement = _measure_process_once(adb, package, clock_tick)
            if i < 0:
                continue
            metrics = measurement["metrics"]
            if any(v is not None for v in metrics.values()):
                success_count += 1
            sample = Sample(
                taken_at=taken_at,
                metrics=metrics,
                errors=measurement["errors"],
                notes=f"pid={measurement['pid']}",
            )
            samples.append(sample)
            _safe_print(f"sample {i + 1}/{repeat}: total_pss={metrics.get('total_pss_kb')} pid={measurement['pid']}")
        except ProbeError as exc:
            _safe_print(f"sample {i + 1} failed: {exc.message}")
            if i >= 0:
                samples.append(Sample(taken_at=taken_at, metrics={k: None for k in METRIC_UNITS}, errors=[exc.message]))
            # Continue to next repeat rather than aborting, so we collect any usable data.
        time.sleep(0.5)

    # Build median and metric containers.
    raw_samples = [s.metrics for s in samples]
    metric_values: dict[str, Any] = {}
    median_values: dict[str, Any] = {}
    for metric in METRIC_UNITS:
        values = [s.metrics.get(metric) for s in samples]
        metric_values[metric] = _build_metric_value(metric, {metric: _median(values)})
        median_values[metric] = _median(values)

    record = Record(
        device=device_info["device"],
        rom=device_info["rom"],
        android_version=device_info["android_version"],
        build_variant=device_info["build_variant"],
        measurement_time=datetime.now(timezone.utc).isoformat(),
        measurement_method="adb_shell_dumpsys_meminfo_and_proc",
        repetitions=repeat,
        aggregation_method="median",
        module_state=module_state,
        process={"name": package, "package": package},
        scenario=scenario,
        metrics=metric_values,
        raw_samples=raw_samples,
        median=median_values,
        notes=f"warmup={warmup}; collected {len(samples)}/{repeat} samples",
    )

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps([record.as_dict()], indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    _safe_print(f"wrote {output}")
    return 0 if success_count > 0 else 1


def cmd_startup(
    adb: Adb,
    package: str,
    activity: str,
    repeat: int,
    output: Path,
) -> int:
    device_info = _collect_device_info(adb)
    samples: list[Sample] = []
    success_count = 0
    full_activity = activity if "." in activity else f"{package}.{activity}"

    for i in range(repeat):
        # Ensure cold start.
        adb.shell(f"am force-stop {package}", check=False, timeout=15)
        time.sleep(0.3)
        taken_at = datetime.now(timezone.utc).isoformat()
        try:
            out = adb.shell(f"am start -W -n {package}/{full_activity}", timeout=60)
            parsed = parse_am_start_w(out)
            metrics = {
                "startup_total_time_ms": parsed.get("startup_total_time_ms"),
            }
            if metrics["startup_total_time_ms"] is not None:
                success_count += 1
            samples.append(Sample(taken_at=taken_at, metrics=metrics, errors=[], notes=f"am start -W output length {len(out)}"))
            _safe_print(f"startup {i + 1}/{repeat}: total_time={metrics['startup_total_time_ms']} ms")
        except ProbeError as exc:
            _safe_print(f"startup {i + 1} failed: {exc.message}")
            samples.append(Sample(taken_at=taken_at, metrics={"startup_total_time_ms": None}, errors=[exc.message]))

    values = [s.metrics.get("startup_total_time_ms") for s in samples]
    record = Record(
        device=device_info["device"],
        rom=device_info["rom"],
        android_version=device_info["android_version"],
        build_variant=device_info["build_variant"],
        measurement_time=datetime.now(timezone.utc).isoformat(),
        measurement_method="adb_shell_am_start_w",
        repetitions=repeat,
        aggregation_method="median",
        module_state="module_enabled_typical_features",
        process={"name": package, "package": package},
        scenario="startup",
        metrics={"startup_total_time_ms": _build_metric_value("startup_total_time_ms", {"startup_total_time_ms": _median(values)})},
        raw_samples=[s.metrics for s in samples],
        median={"startup_total_time_ms": _median(values)},
        notes="cold start measured with am start -W; force-stop before each repetition",
    )

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps([record.as_dict()], indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    _safe_print(f"wrote {output}")
    return 0 if success_count > 0 else 1


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="A13 ADB performance probe")
    parser.add_argument("--adb", default=None, help="path to adb executable")
    parser.add_argument("--device", default=None, help="target device serial")
    subparsers = parser.add_subparsers(dest="command", required=True)

    p_doctor = subparsers.add_parser("doctor", help="check adb and device state")
    p_doctor.add_argument("--json", action="store_true", help="output JSON")

    p_process = subparsers.add_parser("process", help="sample a process repeatedly")
    p_process.add_argument("--package", required=True, help="target package name")
    p_process.add_argument("--scenario", required=True, help="scenario identifier")
    p_process.add_argument("--module-state", default="enabled_features_off", help="module state label")
    p_process.add_argument("--repeat", type=int, default=5, help="number of repetitions")
    p_process.add_argument("--warmup", type=int, default=0, help="discarded warmup samples")
    p_process.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_DIR / "process.json", help="output JSON file")

    p_startup = subparsers.add_parser("startup", help="measure cold startup repeatedly")
    p_startup.add_argument("--package", required=True, help="target package name")
    p_startup.add_argument("--activity", required=True, help="main activity class (short or fully qualified)")
    p_startup.add_argument("--repeat", type=int, default=5, help="number of repetitions")
    p_startup.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_DIR / "startup.json", help="output JSON file")

    args = parser.parse_args(argv)

    try:
        adb = Adb(adb_path=args.adb, device=args.device)
    except ProbeError as exc:
        _safe_print(exc.message)
        return 1

    if args.command == "doctor":
        return cmd_doctor(adb, as_json=args.json)
    if args.command == "process":
        return cmd_process(
            adb,
            package=args.package,
            scenario=args.scenario,
            module_state=args.module_state,
            repeat=args.repeat,
            output=args.output,
            warmup=args.warmup,
        )
    if args.command == "startup":
        return cmd_startup(
            adb,
            package=args.package,
            activity=args.activity,
            repeat=args.repeat,
            output=args.output,
        )
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
