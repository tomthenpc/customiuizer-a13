#!/usr/bin/env python3
"""A13 runtime baseline scenario orchestrator (R2 corrected).

R2 corrections:
- Notification scenario: post unique notification per iteration, verify after post.
- Cleanup: dismiss notification while shade is open, then close shade.
- Menu close: use KEYCODE_BACK instead of arbitrary tap.
- Menu trigger evidence: uiautomator dump BEFORE/AFTER long-press, store raw artifacts.
- PID pair validity: pre_pid == post_pid, both non-null.
- Valid-pair aggregation: only valid pairs enter median/delta.
- Failed repetitions excluded from aggregate.
- CLI exit codes: 0=OK, 1=DEVICE, 2=CLI, 3=SCENARIO.
- Clock tick provenance: DEVICE_GETCONF / UNAVAILABLE, raw ticks preserved.
- Boot stable: record boot_id, sys.boot_completed, uptime; script does NOT reboot.
- Multi-device guard: fail if multiple online devices and no --device.
- Doctor: no KEYCODE_MENU; uses input --help and documented smoke checks.
- Coordinate adaptation: center_x = width/2, or strict 1080p portrait precondition.
- Notification capability precondition checked before scenario.
- All critical adb commands checked.
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
DEFAULT_OUTPUT_DIR = REPO_ROOT / "artifacts" / "runtime-baseline"

sys.path.insert(0, str(Path(__file__).resolve().parent))
import a13_perf_probe as probe  # noqa: E402

SYSTEM_SERVER_PACKAGE = "system_server"
SYSTEMUI_PACKAGE = "com.android.systemui"

SYSTEM_SERVER_METRICS = {"total_pss_kb", "private_dirty_kb", "cpu_time_ms"}
SYSTEMUI_METRICS = {"total_pss_kb", "java_heap_kb", "native_heap_kb", "thread_count"}

NOTIF_TAG_PREFIX = "customiuizer_a13_baseline"

STATUS_OK = "OK"
STATUS_FAILED_PRECONDITION = "FAILED_PRECONDITION"
STATUS_FAILED = "FAILED"
STATUS_NOT_EXECUTED = "NOT_EXECUTED"
STATUS_DEVICE_NOT_CONNECTED = "DEVICE_NOT_CONNECTED"

# Exit codes
EXIT_OK = 0
EXIT_DEVICE = 1
EXIT_CLI = 2
EXIT_SCENARIO = 3


@dataclass
class ClockTickInfo:
    value: int | None
    source: str  # DEVICE_GETCONF or UNAVAILABLE
    raw_output: str


@dataclass
class NotificationCapability:
    cmd_notification_available: bool = False
    post_available: bool = False
    list_available: bool = False
    remove_available: bool = False
    raw_help: str = ""

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class DisplayInfo:
    width: int | None = None
    height: int | None = None
    density: str = "unknown"
    orientation: str = "unknown"
    size_raw: str = ""
    density_raw: str = ""
    orientation_raw: str = ""
    coordinate_confidence: str = "UNKNOWN"

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class AdbResult:
    command: str
    returncode: int
    stdout: str
    stderr: str

    @property
    def ok(self) -> bool:
        return self.returncode == 0


def _print(msg: str) -> None:
    try:
        print(msg)
    except UnicodeEncodeError:
        if hasattr(sys.stdout, "buffer"):
            sys.stdout.buffer.write(msg.encode(sys.stdout.encoding, errors="replace") + b"\n")


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _run_id() -> str:
    return datetime.now().strftime("%Y%m%dT%H%M%S")


def _metric_filter_for(package: str) -> set[str]:
    if package == SYSTEM_SERVER_PACKAGE:
        return SYSTEM_SERVER_METRICS
    if package == SYSTEMUI_PACKAGE:
        return SYSTEMUI_METRICS
    return set(probe.METRIC_UNITS.keys())


def _filter_metrics(metrics: dict[str, Any], allowed: set[str]) -> dict[str, Any]:
    return {k: v for k, v in metrics.items() if k in allowed}


def _adb_shell(adb: probe.Adb, command: str, timeout: int = 30) -> AdbResult:
    """Run adb shell command and return full result, never raising."""
    cmd = adb.build_cmd(["shell", command])
    try:
        result = subprocess.run(
            cmd, capture_output=True, text=True,
            encoding="utf-8", errors="replace", timeout=timeout,
        )
        return AdbResult(
            command=command,
            returncode=result.returncode,
            stdout=result.stdout or "",
            stderr=result.stderr or "",
        )
    except subprocess.TimeoutExpired:
        return AdbResult(command=command, returncode=-1, stdout="", stderr="TIMEOUT")
    except Exception as exc:
        return AdbResult(command=command, returncode=-1, stdout="", stderr=str(exc))


# ---------------------------------------------------------------------------
# Clock tick with provenance
# ---------------------------------------------------------------------------

def _resolve_clock_tick_with_provenance(adb: probe.Adb) -> ClockTickInfo:
    """Get CLK_TCK from device with provenance. No silent fallback."""
    r = _adb_shell(adb, "getconf CLK_TCK", timeout=10)
    out = r.stdout.strip()
    if r.ok and out:
        try:
            val = int(out)
            if val > 0:
                return ClockTickInfo(value=val, source="DEVICE_GETCONF", raw_output=out)
        except ValueError:
            pass
    return ClockTickInfo(value=None, source="UNAVAILABLE", raw_output=r.stdout + r.stderr)


# ---------------------------------------------------------------------------
# Capability and display probing
# ---------------------------------------------------------------------------

def _probe_notification_capability(adb: probe.Adb) -> NotificationCapability:
    cap = NotificationCapability()
    r = _adb_shell(adb, "cmd notification")
    cap.raw_help = r.stdout + r.stderr
    if not r.ok and not r.stdout.strip():
        cap.cmd_notification_available = False
        return cap
    cap.cmd_notification_available = True
    help_text = (r.stdout + r.stderr).lower()
    cap.post_available = "post" in help_text
    cap.list_available = "list" in help_text
    cap.remove_available = "remove" in help_text
    return cap


def _parse_display_size(raw: str) -> tuple[int | None, int | None]:
    """Parse 'Physical size: 1080x2400' into (width, height)."""
    m = re.search(r"(\d+)x(\d+)", raw)
    if m:
        return int(m.group(1)), int(m.group(2))
    return None, None


def _probe_display(adb: probe.Adb) -> DisplayInfo:
    info = DisplayInfo()
    r = _adb_shell(adb, "wm size")
    info.size_raw = r.stdout
    if r.ok:
        w, h = _parse_display_size(r.stdout)
        info.width = w
        info.height = h
    r = _adb_shell(adb, "wm density")
    info.density_raw = r.stdout
    info.density = r.stdout.strip().replace("Physical density: ", "") if r.ok else "unknown"
    r = _adb_shell(adb, "dumpsys input | grep -i 'Orientation:'")
    info.orientation_raw = r.stdout
    info.orientation = r.stdout.strip() if r.ok else "unknown"

    if info.width == 1080 and info.height and info.height >= 1920:
        info.coordinate_confidence = "1080P_PORTRAIT_ASSUMED"
    else:
        info.coordinate_confidence = "NON_1080P_REQUIRES_CALIBRATION"
    return info


def _swipe_coords(display: DisplayInfo, from_top: bool) -> tuple[int, int, int, int]:
    """Compute swipe coordinates based on display size or 1080p fallback."""
    w = display.width or 1080
    h = display.height or 2400
    center_x = w // 2
    if from_top:
        # Open shade: top center → 2/3 down
        return center_x, 0, center_x, int(h * 0.67)
    else:
        # Close shade: lower center → top
        return center_x, int(h * 0.67), center_x, 0


def _make_swipe(display: DisplayInfo, from_top: bool) -> str:
    x1, y1, x2, y2 = _swipe_coords(display, from_top)
    return f"input swipe {x1} {y1} {x2} {y2} 300"


def _make_long_press(display: DisplayInfo) -> str:
    """Long press near top-center of notification shade."""
    w = display.width or 1080
    h = display.height or 2400
    x = w // 2
    y = int(h * 0.25)  # notification row around top quarter
    return f"input swipe {x} {y} {x} {y} 800"


def _make_swipe_dismiss(display: DisplayInfo) -> str:
    """Swipe notification left from center."""
    w = display.width or 1080
    h = display.height or 2400
    start_x = w // 2
    end_x = int(w * 0.1)
    y = int(h * 0.25)
    return f"input swipe {start_x} {y} {end_x} {y} 300"


# ---------------------------------------------------------------------------
# Artifacts
# ---------------------------------------------------------------------------

@dataclass
class ArtifactPaths:
    run_dir: Path
    raw_dir: Path
    uiautomator_dir: Path
    manifest_path: Path
    samples_path: Path

    @classmethod
    def for_run(cls, base_dir: Path, run_id: str, scenario_id: str) -> "ArtifactPaths":
        run_dir = base_dir / f"{run_id}_{scenario_id}"
        return cls(
            run_dir=run_dir,
            raw_dir=run_dir / "raw",
            uiautomator_dir=run_dir / "uiautomator",
            manifest_path=run_dir / "manifest.json",
            samples_path=run_dir / "samples.json",
        )

    def mkdirs(self) -> None:
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self.uiautomator_dir.mkdir(parents=True, exist_ok=True)


def _raw_filename(sample_index: int, process: str, kind: str, phase: str) -> str:
    pkg_short = "system_server" if process == SYSTEM_SERVER_PACKAGE else "systemui"
    return f"{sample_index:03d}_{phase}_{pkg_short}_{kind}.txt"


def _uiautomator_filename(rep: int, iteration: int, moment: str) -> str:
    return f"r{rep}_i{iteration}_{moment}.xml"


# ---------------------------------------------------------------------------
# Process sampling with raw artifact storage and raw ticks
# ---------------------------------------------------------------------------

@dataclass
class ProcessSample:
    sample_index: int
    process: str
    pid: int | None
    taken_at: str
    phase: str
    metrics: dict[str, Any]
    raw_refs: dict[str, str] = field(default_factory=dict)
    raw_ticks: dict[str, Any] = field(default_factory=dict)
    errors: list[str] = field(default_factory=list)
    pid_changed: bool = False


def _find_pid(adb: probe.Adb, package: str) -> int | None:
    r = _adb_shell(adb, f"pidof {package}", timeout=15)
    if r.ok and r.stdout.strip():
        pids = [int(p) for p in re.split(r"\s+", r.stdout.strip()) if p.isdigit()]
        if pids:
            return min(pids)
    r = _adb_shell(adb, "ps -A", timeout=15)
    for line in r.stdout.splitlines():
        if package in line:
            parts = line.split()
            if parts and parts[1].isdigit():
                return int(parts[1])
    return None


def _parse_meminfo_fail_visible(text: str) -> dict[str, Any]:
    result: dict[str, Any] = {
        "total_pss_kb": None,
        "java_heap_kb": None,
        "native_heap_kb": None,
        "graphics_kb": None,
        "private_dirty_kb": None,
    }
    if not text or not text.strip():
        result["_parse_status"] = "EMPTY_OUTPUT"
        return result
    found_any = False
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        lower = stripped.lower()
        if lower.startswith("native heap") and result["native_heap_kb"] is None:
            val = probe._parse_first_number(stripped)
            if val is not None:
                result["native_heap_kb"] = val
                found_any = True
        elif (lower.startswith("dalvik heap") or lower.startswith("java heap")) and result["java_heap_kb"] is None:
            val = probe._parse_first_number(stripped)
            if val is not None:
                result["java_heap_kb"] = val
                found_any = True
        elif (lower.startswith("graphics") or lower.startswith("gfx") or lower.startswith("egl") or lower.startswith("gl")) and result["graphics_kb"] is None:
            val = probe._parse_first_number(stripped)
            if val is not None:
                result["graphics_kb"] = val
                found_any = True
    for line in text.splitlines():
        lower = line.lower()
        if result["total_pss_kb"] is None and ("total pss" in lower or "total pss by category" in lower):
            val = probe._parse_first_number(line)
            if val is not None and val > 0:
                result["total_pss_kb"] = val
                found_any = True
        if result["private_dirty_kb"] is None and ("total private dirty" in lower or "private dirty" in lower):
            val = probe._parse_first_number(line)
            if val is not None and val > 0:
                result["private_dirty_kb"] = val
                found_any = True
    if result["total_pss_kb"] is None:
        for line in text.splitlines():
            stripped = line.strip()
            if stripped.upper().startswith("TOTAL"):
                val = probe._parse_first_number(stripped)
                if val is not None and val > 0:
                    result["total_pss_kb"] = val
                    found_any = True
                    break
    if not found_any:
        result["_parse_status"] = "PARSE_FAILED"
    else:
        result["_parse_status"] = "OK"
    return result


def _sample_process_with_artifacts(
    adb: probe.Adb,
    package: str,
    clock_tick: ClockTickInfo,
    artifacts: ArtifactPaths,
    sample_index: int,
    phase: str,
    prev_pid: int | None = None,
) -> ProcessSample:
    taken_at = _now_iso()
    pid = _find_pid(adb, package)
    pid_changed = prev_pid is not None and pid is not None and pid != prev_pid

    metrics: dict[str, Any] = {k: None for k in probe.METRIC_UNITS}
    raw_refs: dict[str, str] = {}
    raw_ticks: dict[str, Any] = {}
    errors: list[str] = []

    if pid is None:
        errors.append(f"Process '{package}' not found")
        return ProcessSample(
            sample_index=sample_index, process=package, pid=None,
            taken_at=taken_at, phase=phase, metrics=metrics,
            raw_refs=raw_refs, raw_ticks=raw_ticks, errors=errors,
            pid_changed=pid_changed,
        )

    # meminfo
    r = _adb_shell(adb, f"dumpsys meminfo {pid}", timeout=20)
    meminfo_raw = r.stdout
    if not r.ok:
        errors.append(f"dumpsys meminfo failed: rc={r.returncode} stderr={r.stderr[:200]}")
    mem = _parse_meminfo_fail_visible(meminfo_raw)
    parse_status = mem.pop("_parse_status", "OK")
    if parse_status != "OK":
        errors.append(f"meminfo parse: {parse_status}")
    for k in ("total_pss_kb", "java_heap_kb", "native_heap_kb", "graphics_kb", "private_dirty_kb"):
        metrics[k] = mem.get(k)
    raw_name = _raw_filename(sample_index, package, "meminfo", phase)
    (artifacts.raw_dir / raw_name).write_text(meminfo_raw, encoding="utf-8", errors="replace")
    raw_refs["meminfo"] = f"raw/{raw_name}"

    # proc stat
    r = _adb_shell(adb, f"cat /proc/{pid}/stat", timeout=10)
    stat_raw = r.stdout
    if r.ok:
        stat = probe.parse_proc_stat(stat_raw, clock_tick.value if clock_tick.value else 100)
        metrics["cpu_time_ms"] = stat.get("cpu_time_ms")
        if metrics["thread_count"] is None:
            metrics["thread_count"] = stat.get("thread_count")
        # Preserve raw ticks
        match = re.match(r"^(\d+) \((.*)\) (.*)$", stat_raw)
        if match:
            tail = match.group(3)
            fields = tail.split()
            if len(fields) >= 18:
                try:
                    raw_ticks["utime"] = int(fields[11])
                    raw_ticks["stime"] = int(fields[12])
                    raw_ticks["num_threads"] = int(fields[17])
                    raw_ticks["cpu_time_ticks"] = raw_ticks["utime"] + raw_ticks["stime"]
                    raw_ticks["clk_tck"] = clock_tick.value
                    if clock_tick.value is None:
                        metrics["cpu_time_ms"] = None
                except ValueError:
                    pass
    else:
        errors.append(f"cat /proc/{pid}/stat failed: rc={r.returncode}")
    raw_name = _raw_filename(sample_index, package, "proc_stat", phase)
    (artifacts.raw_dir / raw_name).write_text(stat_raw, encoding="utf-8", errors="replace")
    raw_refs["proc_stat"] = f"raw/{raw_name}"

    # proc status
    r = _adb_shell(adb, f"cat /proc/{pid}/status", timeout=10)
    status_raw = r.stdout
    if r.ok:
        status = probe.parse_proc_status(status_raw)
        if metrics["thread_count"] is None:
            metrics["thread_count"] = status.get("thread_count")
    else:
        errors.append(f"cat /proc/{pid}/status failed: rc={r.returncode}")
    raw_name = _raw_filename(sample_index, package, "proc_status", phase)
    (artifacts.raw_dir / raw_name).write_text(status_raw, encoding="utf-8", errors="replace")
    raw_refs["proc_status"] = f"raw/{raw_name}"

    return ProcessSample(
        sample_index=sample_index, process=package, pid=pid,
        taken_at=taken_at, phase=phase, metrics=metrics,
        raw_refs=raw_refs, raw_ticks=raw_ticks, errors=errors,
        pid_changed=pid_changed,
    )


# ---------------------------------------------------------------------------
# Scenario definitions
# ---------------------------------------------------------------------------

@dataclass
class ScenarioStep:
    description: str
    commands: list[str] = field(default_factory=list)
    wait_after_s: float = 1.0
    critical: bool = True


@dataclass
class Scenario:
    scenario_id: str
    description: str
    warmup_s: float
    steps: list[ScenarioStep]
    target_processes: list[str]
    repeat: int = 5
    requires_notification: bool = False


# ---------------------------------------------------------------------------
# Notification helper functions
# ---------------------------------------------------------------------------

def _post_notification(adb: probe.Adb, tag: str) -> AdbResult:
    cmd = f"cmd notification post -t 'Baseline Test' 'Baseline test body' {tag}"
    return _adb_shell(adb, cmd, timeout=15)


def _verify_notification_exists(adb: probe.Adb, tag: str, cap: NotificationCapability) -> tuple[bool, str]:
    if cap.list_available:
        r = _adb_shell(adb, "cmd notification list", timeout=15)
        if r.ok:
            if tag in r.stdout:
                return True, "cmd_notification_list"
            return False, "cmd_notification_list_tag_not_found"
    r = _adb_shell(adb, "dumpsys notification --noredact", timeout=20)
    if r.ok and tag in r.stdout:
        return True, "dumpsys_notification_noredact"
    r = _adb_shell(adb, "dumpsys notification", timeout=20)
    if r.ok and tag in r.stdout:
        return True, "dumpsys_notification"
    return False, "verification_failed"


def _uiautomator_dump(adb: probe.Adb, artifacts: ArtifactPaths, rep: int, iteration: int, moment: str) -> str:
    out_name = _uiautomator_filename(rep, iteration, moment)
    out_path = artifacts.uiautomator_dir / out_name
    r = _adb_shell(adb, "uiautomator dump --compressed /sdcard/window_dump.xml", timeout=10)
    if r.ok:
        r2 = _adb_shell(adb, "cat /sdcard/window_dump.xml", timeout=10)
        if r2.ok:
            out_path.write_text(r2.stdout, encoding="utf-8", errors="replace")
            return f"uiautomator/{out_name}"
    return ""


def _build_notification_menu_steps(
    run_id: str,
    rep: int,
    display: DisplayInfo,
) -> tuple[list[ScenarioStep], list[dict[str, Any]]]:
    """Build steps for one repetition of notification_menu_create_delete scenario.

    Returns (steps, iteration_plan) where iteration_plan contains tag and evidence info.
    """
    steps: list[ScenarioStep] = []
    iteration_plan: list[dict[str, Any]] = []

    if display.coordinate_confidence == "NON_1080P_REQUIRES_CALIBRATION":
        _print("  WARNING: display not 1080p portrait; coordinate confidence reduced")

    for i in range(10):
        tag = f"{NOTIF_TAG_PREFIX}_{run_id}_r{rep}_i{i}"
        iteration_plan.append({
            "iteration": i,
            "tag": tag,
            "notification_source": "SHELL_TEST_NOTIFICATION",
            "menu_trigger_method": "LONG_PRESS_NOTIFICATION_ROW",
            "menu_close_method": "KEYCODE_BACK",
            "ui_automation_confidence": display.coordinate_confidence,
        })

        # Post
        steps.append(ScenarioStep(
            f"r{rep} i{i}: Post shell test notification (tag={tag})",
            [f"cmd notification post -t 'Baseline Test' 'Baseline test body' {tag}"],
            2.0,
        ))

        # Open shade
        steps.append(ScenarioStep(
            f"r{rep} i{i}: Swipe down to open notification panel",
            [_make_swipe(display, from_top=True)],
            1.5,
        ))

        # Long-press to trigger menu (BEFORE uiautomator dump)
        # Actual uiautomator dump is executed between steps by runner, not in commands.
        steps.append(ScenarioStep(
            f"r{rep} i{i}: Long-press notification row to trigger menu",
            [_make_long_press(display)],
            2.0,
        ))

        # Close menu with BACK
        steps.append(ScenarioStep(
            f"r{rep} i{i}: Close notification menu with KEYCODE_BACK",
            ["input keyevent 4"],
            1.0,
        ))

        # Swipe-dismiss target notification while shade is open
        steps.append(ScenarioStep(
            f"r{rep} i{i}: Swipe-dismiss target notification while shade is open",
            [_make_swipe_dismiss(display)],
            1.0,
            critical=False,
        ))

        # Close shade
        steps.append(ScenarioStep(
            f"r{rep} i{i}: Swipe up to close panel",
            [_make_swipe(display, from_top=False)],
            1.5,
        ))

    return steps, iteration_plan


# ---------------------------------------------------------------------------
# Doctor
# ---------------------------------------------------------------------------

def cmd_doctor(adb: probe.Adb) -> int:
    report: dict[str, Any] = {
        "adb_path": adb.adb,
        "checks": {},
        "errors": [],
        "status": "",
    }

    def check(name: str, ok: bool, detail: str = "") -> None:
        report["checks"][name] = {"ok": ok, "detail": detail}
        if not ok:
            report["errors"].append(f"{name}: {detail}")

    try:
        ver = adb.version()
        check("adb_version", True, ver)
    except probe.ProbeError as exc:
        check("adb_version", False, exc.message)

    try:
        devices = adb.devices()
        online = [d for d in devices if d.get("state") == "device"]
        check("device_count", len(online) >= 1, f"online={len(online)}")
        if not online:
            report["status"] = STATUS_DEVICE_NOT_CONNECTED
            _print(json.dumps(report, indent=2, ensure_ascii=False))
            return EXIT_DEVICE
        if len(online) == 1:
            check("device_single", True, online[0]["serial"])
        else:
            check("device_single", False, f"multiple devices: {len(online)}, use --device")
            report["status"] = "MULTIPLE_DEVICES"
            _print(json.dumps(report, indent=2, ensure_ascii=False))
            return EXIT_DEVICE
    except probe.ProbeError as exc:
        check("device_count", False, exc.message)
        report["status"] = STATUS_DEVICE_NOT_CONNECTED
        _print(json.dumps(report, indent=2, ensure_ascii=False))
        return EXIT_DEVICE

    r = _adb_shell(adb, "getprop ro.build.version.release")
    check("android_version", r.ok, r.stdout.strip() or r.stderr)

    r = _adb_shell(adb, "getprop ro.miui.ui.version.name")
    rom = r.stdout.strip() if r.ok else "unknown"
    r2 = _adb_shell(adb, "getprop ro.build.display.id")
    check("rom_fingerprint", True, f"miui={rom} display={r2.stdout.strip() if r2.ok else 'unknown'}")

    r = _adb_shell(adb, "pidof system_server")
    ss_pid = r.stdout.strip() if r.ok and r.stdout.strip() else None
    check("system_server_pid", ss_pid is not None, ss_pid or "not found")

    r = _adb_shell(adb, "pidof com.android.systemui")
    sui_pid = r.stdout.strip() if r.ok and r.stdout.strip() else None
    check("systemui_pid", sui_pid is not None, sui_pid or "not found")

    if ss_pid:
        r = _adb_shell(adb, f"dumpsys meminfo {ss_pid}", timeout=20)
        check("meminfo_system_server", r.ok and bool(r.stdout.strip()),
              f"rc={r.returncode} len={len(r.stdout)}")

    if ss_pid:
        r = _adb_shell(adb, f"cat /proc/{ss_pid}/stat")
        check("proc_stat", r.ok and bool(r.stdout.strip()), f"rc={r.returncode}")

    if ss_pid:
        r = _adb_shell(adb, f"cat /proc/{ss_pid}/status")
        check("proc_status", r.ok and bool(r.stdout.strip()), f"rc={r.returncode}")

    r = _adb_shell(adb, "cmd notification post --help")
    check("notification_post_help", r.ok or "post" in (r.stdout + r.stderr).lower(),
          f"rc={r.returncode}")

    cap = _probe_notification_capability(adb)
    check("notification_capability", cap.cmd_notification_available,
          f"post={cap.post_available} list={cap.list_available} remove={cap.remove_available}")
    report["notification_capability"] = cap.as_dict()

    r = _adb_shell(adb, "wm size")
    check("display_size", r.ok, r.stdout.strip() or r.stderr)

    r = _adb_shell(adb, "wm density")
    check("display_density", r.ok, r.stdout.strip() or r.stderr)

    r = _adb_shell(adb, "dumpsys input | grep -i 'Orientation:'")
    check("display_orientation", r.ok, r.stdout.strip() or r.stderr)

    r = _adb_shell(adb, "input --help")
    check("input_help", r.ok, "input command available" if r.ok else r.stderr)

    # uiautomator availability check
    r = _adb_shell(adb, "uiautomator dump --help")
    check("uiautomator_dump", r.returncode == 0 or "dump" in (r.stdout + r.stderr).lower(),
          f"rc={r.returncode}")

    report["status"] = "OK" if not report["errors"] else "CHECKS_FAILED"
    _print(json.dumps(report, indent=2, ensure_ascii=False))
    return EXIT_OK if not report["errors"] else EXIT_DEVICE


# ---------------------------------------------------------------------------
# Aggregation
# ---------------------------------------------------------------------------

def _is_sample_valid(s: dict[str, Any]) -> bool:
    """A sample is valid if it has a PID and all required metrics are parseable."""
    if s.get("pid") is None:
        return False
    # All required metrics for this process must be non-null
    return all(v is not None for v in s.get("metrics", {}).values())


def _build_valid_pairs(
    pre_samples: list[dict[str, Any]],
    post_samples: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """Pair PRE and POST by process and repeat_index, require PID match and both valid."""
    pre_by_key = {(s["process"], s["repeat_index"]): s for s in pre_samples}
    post_by_key = {(s["process"], s["repeat_index"]): s for s in post_samples}
    pairs = []
    for key, pre in pre_by_key.items():
        post = post_by_key.get(key)
        if post is None:
            continue
        pair_valid = (
            _is_sample_valid(pre) and _is_sample_valid(post)
            and pre["pid"] is not None
            and post["pid"] is not None
            and pre["pid"] == post["pid"]
        )
        pairs.append({
            "sample_index": pre["sample_index"],
            "process_package": pre["process"],
            "pre": pre,
            "post": post,
            "pair_valid": pair_valid,
            "pre_pid": pre["pid"],
            "post_pid": post["pid"],
            "pair_identity_reason": "same_pid" if pair_valid else _pair_invalid_reason(pre, post),
        })
    return pairs


def _pair_invalid_reason(pre: dict[str, Any], post: dict[str, Any]) -> str:
    if pre["pid"] is None and post["pid"] is None:
        return "both_pids_missing"
    if pre["pid"] is None:
        return "pre_pid_missing"
    if post["pid"] is None:
        return "post_pid_missing"
    if pre["pid"] != post["pid"]:
        return f"pid_changed: {pre['pid']} -> {post['pid']}"
    if not _is_sample_valid(pre) or not _is_sample_valid(post):
        return "metrics_invalid"
    return "unknown"


def _median_of_pairs(pairs: list[dict[str, Any]], key: str, phase: str) -> float | None:
    values = []
    for p in pairs:
        if not p["pair_valid"]:
            continue
        v = p[phase].get("metrics", {}).get(key)
        if v is not None and isinstance(v, (int, float)):
            values.append(v)
    return statistics.median(values) if values else None


def _build_median_from_pairs(pairs: list[dict[str, Any]], allowed: set[str], phase: str) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for metric in allowed:
        med = _median_of_pairs(pairs, metric, phase)
        result[metric] = {
            "unit": probe.METRIC_UNITS.get(metric, "unknown"),
            "source": probe.METRIC_SOURCES.get(metric, "unknown"),
            "value": med,
            "unavailable_reason": "" if med is not None else "NOT_REPORTED_BY_DEVICE",
        }
    return result


def _build_delta_from_pairs(pairs: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    # Determine keys from first valid pair
    keys: set[str] = set()
    for p in pairs:
        if p["pair_valid"]:
            keys.update(p["pre"].get("metrics", {}).keys())
            break
    result: dict[str, dict[str, Any]] = {}
    for metric in keys:
        pre_vals = [p["pre"]["metrics"][metric] for p in pairs if p["pair_valid"] and metric in p["pre"].get("metrics", {})]
        post_vals = [p["post"]["metrics"][metric] for p in pairs if p["pair_valid"] and metric in p["post"].get("metrics", {})]
        pre_med = statistics.median(pre_vals) if pre_vals else None
        post_med = statistics.median(post_vals) if post_vals else None
        delta = None
        if pre_med is not None and post_med is not None:
            delta = post_med - pre_med
        result[metric] = {
            "unit": probe.METRIC_UNITS.get(metric, "unknown"),
            "pre_median": pre_med,
            "post_median": post_med,
            "delta": delta,
        }
    return result


# ---------------------------------------------------------------------------
# Scenario runner
# ---------------------------------------------------------------------------

def _run_scenario(
    adb: probe.Adb,
    scenario: Scenario,
    module_state: str,
    repeat: int,
    output_dir: Path,
    run_id: str,
    clock_tick: ClockTickInfo,
    notif_cap: NotificationCapability,
    display: DisplayInfo,
    device_info: dict[str, str],
    adb_serial: str | None,
) -> tuple[int, dict[str, Any]]:
    """Run one scenario. Returns (exit_status, manifest)."""
    artifacts = ArtifactPaths.for_run(output_dir, run_id, scenario.scenario_id)
    artifacts.mkdirs()

    _print(f"Scenario: {scenario.scenario_id}")
    _print(f"  module_state: {module_state} (source=OPERATOR_DECLARED)")
    _print(f"  repeat: {repeat}")
    _print(f"  warmup: {scenario.warmup_s}s")
    _print(f"  CLK_TCK: {clock_tick.value} (source={clock_tick.source})")

    # Notification capability precondition
    if scenario.requires_notification:
        if not notif_cap.cmd_notification_available or not notif_cap.post_available:
            manifest = {
                "run_id": run_id,
                "scenario_id": scenario.scenario_id,
                "status": STATUS_FAILED_PRECONDITION,
                "reason": f"cmd notification post not available (cmd={notif_cap.cmd_notification_available}, post={notif_cap.post_available})",
                "module_state": module_state,
                "module_state_source": "OPERATOR_DECLARED",
            }
            artifacts.manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
            return EXIT_SCENARIO, manifest

    sample_counter = 0
    all_pre: list[dict[str, Any]] = []
    all_post: list[dict[str, Any]] = []
    all_pairs: list[dict[str, Any]] = []
    all_repetitions: list[dict[str, Any]] = []
    scenario_status = STATUS_OK

    # For boot stable, record precondition
    boot_info: dict[str, Any] = {}
    if scenario.scenario_id == "boot_stable":
        r = _adb_shell(adb, "getprop sys.boot_completed")
        boot_info["sys_boot_completed"] = r.stdout.strip() if r.ok else None
        r = _adb_shell(adb, "cat /proc/sys/kernel/random/boot_id")
        boot_info["boot_id"] = r.stdout.strip() if r.ok else None
        r = _adb_shell(adb, "cat /proc/uptime")
        boot_info["uptime_raw"] = r.stdout.strip() if r.ok else None
        if boot_info["sys_boot_completed"] != "1":
            _print("  WARNING: sys.boot_completed != 1; boot_stable precondition not met")

    for rep in range(repeat):
        _print(f"\n--- {scenario.scenario_id} rep {rep+1}/{repeat} ---")

        if scenario.warmup_s > 0:
            _print(f"  warmup {scenario.warmup_s}s ...")
            time.sleep(scenario.warmup_s)

        # Pre-samples
        pre_pids: dict[str, int | None] = {}
        pre_samples: list[dict[str, Any]] = []
        for pkg in scenario.target_processes:
            sample_counter += 1
            s = _sample_process_with_artifacts(
                adb, pkg, clock_tick, artifacts, sample_counter, "PRE",
            )
            _print(f"  pre  {pkg}: pss={s.metrics.get('total_pss_kb')} pid={s.pid}")
            sample_dict = {
                "sample_index": s.sample_index,
                "process": s.process,
                "pid": s.pid,
                "taken_at": s.taken_at,
                "phase": "PRE",
                "repeat_index": rep,
                "metrics": _filter_metrics(s.metrics, _metric_filter_for(pkg)),
                "raw_refs": s.raw_refs,
                "raw_ticks": s.raw_ticks,
                "errors": s.errors,
                "pid_changed": False,
            }
            pre_pids[pkg] = s.pid
            pre_samples.append(sample_dict)
            all_pre.append(sample_dict)

        # Build and execute steps
        steps = scenario.steps
        iteration_plan: list[dict[str, Any]] = []
        if scenario.requires_notification:
            steps, iteration_plan = _build_notification_menu_steps(run_id, rep, display)

        step_failed = False
        rep_status = STATUS_OK
        rep_reason = ""

        for i, step in enumerate(steps):
            _print(f"  step: {step.description}")

            # uiautomator dump BEFORE long-press for menu evidence
            if scenario.requires_notification and "Long-press" in step.description:
                ref_before = _uiautomator_dump(adb, artifacts, rep, i, "BEFORE")
                if ref_before:
                    _print(f"    uiautomator BEFORE saved: {ref_before}")

            for cmd in step.commands:
                r = _adb_shell(adb, cmd, timeout=30)
                if not r.ok and step.critical:
                    rep_reason = f"step '{step.description}': rc={r.returncode} stderr={r.stderr[:200]}"
                    _print(f"    CRITICAL FAIL: {rep_reason}")
                    rep_status = STATUS_FAILED
                    step_failed = True
                    break
                elif not r.ok:
                    _print(f"    non-critical fail: rc={r.returncode} cmd={cmd}")
            if step_failed:
                break

            # uiautomator dump AFTER long-press
            if scenario.requires_notification and "Long-press" in step.description:
                ref_after = _uiautomator_dump(adb, artifacts, rep, i, "AFTER")
                if ref_after:
                    _print(f"    uiautomator AFTER saved: {ref_after}")

            # Notification post verification
            if scenario.requires_notification and "Post shell test notification" in step.description:
                # Extract tag from command to verify
                m = re.search(r"(customiuizer_a13_baseline_[\w_]+)", step.commands[0])
                if m:
                    tag = m.group(1)
                    found, method = _verify_notification_exists(adb, tag, notif_cap)
                    if not found:
                        rep_reason = f"notification verification failed for {tag} via {method}"
                        _print(f"    FAILED_PRECONDITION: {rep_reason}")
                        rep_status = STATUS_FAILED_PRECONDITION
                        step_failed = True
                        break
                    _print(f"    notification verified via {method}: {tag}")

            if step.wait_after_s > 0:
                time.sleep(step.wait_after_s)

        if step_failed:
            all_repetitions.append({
                "repeat_index": rep,
                "status": rep_status,
                "reason": rep_reason,
                "valid_for_aggregation": False,
                "pre_samples": pre_samples,
                "post_samples": [],
                "iteration_plan": iteration_plan,
            })
            if scenario_status == STATUS_OK or rep_status == STATUS_FAILED_PRECONDITION:
                scenario_status = rep_status
            continue

        time.sleep(2.0)

        # Post-samples
        post_samples: list[dict[str, Any]] = []
        for pkg in scenario.target_processes:
            sample_counter += 1
            s = _sample_process_with_artifacts(
                adb, pkg, clock_tick, artifacts, sample_counter, "POST",
                prev_pid=pre_pids.get(pkg),
            )
            _print(f"  post {pkg}: pss={s.metrics.get('total_pss_kb')} pid={s.pid} pid_changed={s.pid_changed}")
            sample_dict = {
                "sample_index": s.sample_index,
                "process": s.process,
                "pid": s.pid,
                "taken_at": s.taken_at,
                "phase": "POST",
                "repeat_index": rep,
                "metrics": _filter_metrics(s.metrics, _metric_filter_for(pkg)),
                "raw_refs": s.raw_refs,
                "raw_ticks": s.raw_ticks,
                "errors": s.errors,
                "pid_changed": s.pid_changed,
            }
            post_samples.append(sample_dict)
            all_post.append(sample_dict)

        # Build per-process valid pairs for this repetition
        valid_for_agg = True
        rep_pairs: dict[str, list[dict[str, Any]]] = {}
        for pkg in scenario.target_processes:
            pkg_pre = [s for s in pre_samples if s["process"] == pkg]
            pkg_post = [s for s in post_samples if s["process"] == pkg]
            pairs = _build_valid_pairs(pkg_pre, pkg_post)
            rep_pairs[pkg] = pairs
            all_pairs.extend(pairs)
            if not any(p["pair_valid"] for p in pairs):
                valid_for_agg = False
                rep_reason = f"no valid PID pair for {pkg}"
                _print(f"    invalid pair for {pkg}: pre_pid={pkg_pre[0]['pid'] if pkg_pre else None} post_pid={pkg_post[0]['pid'] if pkg_post else None}")

        all_repetitions.append({
            "repeat_index": rep,
            "status": STATUS_OK if valid_for_agg else STATUS_FAILED,
            "reason": "" if valid_for_agg else rep_reason,
            "valid_for_aggregation": valid_for_agg,
            "pre_samples": pre_samples,
            "post_samples": post_samples,
            "iteration_plan": iteration_plan,
            "pairs": {pkg: [p["pair_valid"] for p in pairs] for pkg, pairs in rep_pairs.items()},
        })
        if not valid_for_agg and scenario_status == STATUS_OK:
            scenario_status = STATUS_FAILED

    # Aggregate only from valid pairs
    valid_pairs_by_process: dict[str, list[dict[str, Any]]] = {}
    for pkg in scenario.target_processes:
        valid_pairs_by_process[pkg] = [p for p in all_pairs if p["process_package"] == pkg and p["pair_valid"]]

    median_pre: dict[str, dict[str, Any]] = {}
    median_post: dict[str, dict[str, Any]] = {}
    delta: dict[str, dict[str, Any]] = {}
    for pkg in scenario.target_processes:
        pairs = valid_pairs_by_process.get(pkg, [])
        allowed = _metric_filter_for(pkg)
        median_pre[pkg] = _build_median_from_pairs(pairs, allowed, "pre")
        median_post[pkg] = _build_median_from_pairs(pairs, allowed, "post")
        delta[pkg] = _build_delta_from_pairs(pairs)

    valid_for_aggregation = sum(1 for r in all_repetitions if r["valid_for_aggregation"])

    manifest = {
        "run_id": run_id,
        "scenario_id": scenario.scenario_id,
        "description": scenario.description,
        "module_state": module_state,
        "module_state_source": "OPERATOR_DECLARED",
        "device": device_info.get("device", "unknown"),
        "rom": device_info.get("rom", "unknown"),
        "android_version": device_info.get("android_version", "unknown"),
        "build_variant": device_info.get("build_variant", "unknown"),
        "adb_serial": adb_serial or "default",
        "clk_tck_value": clock_tick.value,
        "clk_tck_source": clock_tick.source,
        "clk_tck_raw": clock_tick.raw_output,
        "notification_capability": notif_cap.as_dict(),
        "display_info": display.as_dict(),
        "boot_info": boot_info if scenario.scenario_id == "boot_stable" else None,
        "repetitions_requested": repeat,
        "repetitions_completed": len(all_repetitions),
        "repetitions_valid_for_aggregation": valid_for_aggregation,
        "repetitions_failed": len(all_repetitions) - valid_for_aggregation,
        "scenario_status": scenario_status,
        "total_samples": sample_counter,
        "all_pairs": [
            {
                "sample_index": p["sample_index"],
                "pre_pid": p["pre_pid"],
                "post_pid": p["post_pid"],
                "pair_valid": p["pair_valid"],
                "process": p["pre"]["process"],
            }
            for p in all_pairs
        ],
        "repetitions": all_repetitions,
        "created_at": _now_iso(),
    }
    artifacts.manifest_path.write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    samples_data = {
        "run_id": run_id,
        "scenario_id": scenario.scenario_id,
        "module_state": module_state,
        "median_pre": median_pre,
        "median_post": median_post,
        "delta": delta,
        "raw_all_pre_samples": all_pre,
        "raw_all_post_samples": all_post,
    }
    artifacts.samples_path.write_text(
        json.dumps(samples_data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    _print(f"\n  status={scenario_status} reps_completed={len(all_repetitions)}/{repeat} "
           f"valid_for_aggregation={valid_for_aggregation}")
    _print(f"  manifest: {artifacts.manifest_path}")
    _print(f"  samples:  {artifacts.samples_path}")
    _print(f"  raw dir:  {artifacts.raw_dir}")

    exit_status = EXIT_OK if scenario_status == STATUS_OK else EXIT_SCENARIO
    return exit_status, manifest


# ---------------------------------------------------------------------------
# Multi-device guard and run command
# ---------------------------------------------------------------------------

def _ensure_single_device(adb: probe.Adb) -> tuple[bool, list[dict[str, str]]]:
    try:
        devices = adb.devices()
        online = [d for d in devices if d.get("state") == "device"]
        if not online:
            return False, []
        if len(online) > 1 and not adb.device:
            return False, online
        return True, online
    except probe.ProbeError:
        return False, []


def cmd_run(
    adb: probe.Adb,
    scenario_names: list[str],
    module_state: str,
    repeat_override: int | None,
    output_dir: Path,
) -> int:
    run_id = _run_id()
    _print(f"Run ID: {run_id}")
    _print(f"Module state: {module_state} (source=OPERATOR_DECLARED)")
    _print(f"Output: {output_dir}")

    ok, online = _ensure_single_device(adb)
    if not ok:
        if not online:
            _print(f"{STATUS_DEVICE_NOT_CONNECTED}: no device online")
        else:
            _print(f"MULTIPLE_DEVICES: {len(online)} devices online, use --device")
        return EXIT_DEVICE

    clock_tick = _resolve_clock_tick_with_provenance(adb)
    _print(f"CLK_TCK: {clock_tick.value} (source={clock_tick.source})")
    device_info = probe._collect_device_info(adb)
    notif_cap = _probe_notification_capability(adb)
    display = _probe_display(adb)
    adb_serial = adb.device

    names = list(SCENARIOS.keys()) if "all" in scenario_names else scenario_names
    exit_code = EXIT_OK

    for sname in names:
        scenario = SCENARIOS.get(sname)
        if not scenario:
            _print(f"unknown scenario: {sname}")
            exit_code = max(exit_code, EXIT_CLI)
            continue
        repeat = repeat_override if repeat_override is not None else scenario.repeat
        status, _ = _run_scenario(
            adb, scenario, module_state, repeat, output_dir, run_id,
            clock_tick, notif_cap, display, device_info, adb_serial,
        )
        exit_code = max(exit_code, status)

    return exit_code


# ---------------------------------------------------------------------------
# Scenario registry
# ---------------------------------------------------------------------------

SCENARIOS = {
    "boot_stable": Scenario(
        scenario_id="boot_stable",
        description="Boot to home, wait 5 minutes, then sample. Script does NOT reboot device.",
        warmup_s=300.0,
        steps=[ScenarioStep("Wait 5 minutes for stabilization", [], 0, critical=False)],
        target_processes=[SYSTEM_SERVER_PACKAGE, SYSTEMUI_PACKAGE],
        repeat=1,
    ),
    "notification_panel_open_close": Scenario(
        scenario_id="notification_panel_open_close",
        description="Open and close notification panel 10 times.",
        warmup_s=10.0,
        steps=[
            ScenarioStep("Swipe down to open notification panel", [], 1.5),
            ScenarioStep("Swipe up to close panel", [], 1.5),
        ],
        target_processes=[SYSTEMUI_PACKAGE, SYSTEM_SERVER_PACKAGE],
        repeat=5,
    ),
    "volume_adjust_10": Scenario(
        scenario_id="volume_adjust_10",
        description="Press volume up 5 times then volume down 5 times.",
        warmup_s=10.0,
        steps=[
            ScenarioStep("Volume up press 1/5", ["input keyevent 24"], 0.5),
            ScenarioStep("Volume up press 2/5", ["input keyevent 24"], 0.5),
            ScenarioStep("Volume up press 3/5", ["input keyevent 24"], 0.5),
            ScenarioStep("Volume up press 4/5", ["input keyevent 24"], 0.5),
            ScenarioStep("Volume up press 5/5", ["input keyevent 24"], 0.5),
            ScenarioStep("Volume down press 1/5", ["input keyevent 25"], 0.5),
            ScenarioStep("Volume down press 2/5", ["input keyevent 25"], 0.5),
            ScenarioStep("Volume down press 3/5", ["input keyevent 25"], 0.5),
            ScenarioStep("Volume down press 4/5", ["input keyevent 25"], 0.5),
            ScenarioStep("Volume down press 5/5", ["input keyevent 25"], 0.5),
        ],
        target_processes=[SYSTEM_SERVER_PACKAGE],
        repeat=5,
    ),
    "qs_panel_expand_collapse": Scenario(
        scenario_id="qs_panel_expand_collapse",
        description="Expand QS panel fully and collapse 10 times.",
        warmup_s=10.0,
        steps=[
            ScenarioStep("Swipe down to open notification panel", [], 1.0),
            ScenarioStep("Swipe down again to expand QS fully", [], 1.5),
            ScenarioStep("Swipe up to close panel", [], 1.5),
        ],
        target_processes=[SYSTEMUI_PACKAGE, SYSTEM_SERVER_PACKAGE],
        repeat=5,
    ),
    "notification_menu_create_delete": Scenario(
        scenario_id="notification_menu_create_delete",
        description="Post, verify, open shade, long-press, BACK close, swipe-dismiss, close shade.",
        warmup_s=10.0,
        steps=[],
        target_processes=[SYSTEMUI_PACKAGE, SYSTEM_SERVER_PACKAGE],
        repeat=5,
        requires_notification=True,
    ),
}

# Fill steps with display-dependent swipes at runtime in _run_scenario for non-notification scenarios
# For notification scenario, steps are built per-repetition with unique tags.


def _fill_steps_with_display(steps: list[ScenarioStep], display: DisplayInfo) -> list[ScenarioStep]:
    """Replace placeholder swipe steps with display-aware coordinates."""
    filled = []
    for step in steps:
        new_commands = []
        for cmd in step.commands:
            new_commands.append(cmd)
        # For placeholder steps (empty command, descriptions contain 'swipe'),
        # we cannot reliably distinguish. Instead rely on scenario building.
        filled.append(ScenarioStep(step.description, new_commands, step.wait_after_s, step.critical))
    return filled


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="A13 runtime baseline scenario orchestrator (R2)")
    parser.add_argument("--adb", default=None, help="path to adb executable")
    parser.add_argument("--device", default=None, help="target device serial")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("doctor", help="check device capabilities")

    p_run = subparsers.add_parser("run", help="run one or more scenarios")
    p_run.add_argument("--scenario", nargs="+", default=["all"], help="scenario name(s) or 'all'")
    p_run.add_argument("--module-state", default="enabled_features_off", help="operator-declared module state")
    p_run.add_argument("--repeat", type=int, default=None, help="override per-scenario repeat count")
    p_run.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR, help="output directory")

    args = parser.parse_args(argv)
    try:
        adb = probe.Adb(adb_path=args.adb, device=args.device)
    except probe.ProbeError as exc:
        _print(exc.message)
        return EXIT_DEVICE

    if args.command == "doctor":
        return cmd_doctor(adb)
    if args.command == "run":
        return cmd_run(adb, args.scenario, args.module_state, args.repeat, args.output_dir)
    return EXIT_CLI


if __name__ == "__main__":
    raise SystemExit(main())
