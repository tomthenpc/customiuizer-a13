#!/usr/bin/env python3
"""A13 runtime baseline scenario orchestrator (R1 corrected).

Drives interactive scenarios on a real A13 device via ADB input commands,
and samples system_server / SystemUI metrics before and after each scenario.

R1 corrections:
- Artifact-based storage: raw dumps in separate files, not embedded in JSON.
- Notification capability probe: no assumption of `cmd notification remove`.
- Notification post verification: verify notification exists before proceeding.
- Menu trigger method recorded; coordinate-based UI automation confidence noted.
- Cleanup via swipe-dismiss, not shell remove.
- PID stability check between PRE and POST.
- CLK_TCK recorded in manifest.
- meminfo parse failure → null, not 0.
- module_state_source = OPERATOR_DECLARED.
- doctor checks 15 device capabilities.
- All adb command results checked (return code, stdout, stderr).

Usage:
    python tools/a13_runtime_baseline.py --adb <path> doctor
    python tools/a13_runtime_baseline.py --adb <path> run --scenario all --module-state enabled_features_off
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

# Status codes for scenario results
STATUS_OK = "OK"
STATUS_FAILED_PRECONDITION = "FAILED_PRECONDITION"
STATUS_FAILED = "FAILED"
STATUS_NOT_EXECUTED = "NOT_EXECUTED"
STATUS_DEVICE_NOT_CONNECTED = "DEVICE_NOT_CONNECTED"


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


# ---------------------------------------------------------------------------
# ADB command result
# ---------------------------------------------------------------------------

@dataclass
class AdbResult:
    """Result of an adb shell command with full error visibility."""
    command: str
    returncode: int
    stdout: str
    stderr: str

    @property
    def ok(self) -> bool:
        return self.returncode == 0


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
# Capability probe
# ---------------------------------------------------------------------------

@dataclass
class NotificationCapability:
    """Results of probing notification shell capabilities."""
    cmd_notification_available: bool = False
    post_available: bool = False
    list_available: bool = False
    remove_available: bool = False
    raw_help: str = ""

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


def _probe_notification_capability(adb: probe.Adb) -> NotificationCapability:
    """Probe what `cmd notification` subcommands are available on this device."""
    cap = NotificationCapability()
    r = _adb_shell(adb, "cmd notification")
    cap.raw_help = r.stdout + r.stderr
    if r.returncode != 0 and not r.stdout.strip():
        cap.cmd_notification_available = False
        return cap
    cap.cmd_notification_available = True
    help_text = (r.stdout + r.stderr).lower()
    cap.post_available = "post" in help_text
    cap.list_available = "list" in help_text
    cap.remove_available = "remove" in help_text
    return cap


@dataclass
class DisplayInfo:
    """Display calibration info for coordinate-based UI automation."""
    size: str = "unknown"
    density: str = "unknown"
    orientation: str = "unknown"
    raw_size: str = ""
    raw_density: str = ""
    raw_orientation: str = ""

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


def _probe_display(adb: probe.Adb) -> DisplayInfo:
    """Probe display size, density, orientation."""
    info = DisplayInfo()
    r = _adb_shell(adb, "wm size")
    info.raw_size = r.stdout
    info.size = r.stdout.strip().replace("Physical size: ", "") if r.ok else "unknown"
    r = _adb_shell(adb, "wm density")
    info.raw_density = r.stdout
    info.density = r.stdout.strip().replace("Physical density: ", "") if r.ok else "unknown"
    r = _adb_shell(adb, "dumpsys input | grep -i 'Orientation:'")
    info.raw_orientation = r.stdout
    info.orientation = r.stdout.strip() if r.ok else "unknown"
    return info


# ---------------------------------------------------------------------------
# Artifact storage
# ---------------------------------------------------------------------------

@dataclass
class ArtifactPaths:
    """Paths for one run's artifacts."""
    run_dir: Path
    raw_dir: Path
    manifest_path: Path
    samples_path: Path

    @classmethod
    def for_run(cls, base_dir: Path, run_id: str) -> "ArtifactPaths":
        run_dir = base_dir / run_id
        return cls(
            run_dir=run_dir,
            raw_dir=run_dir / "raw",
            manifest_path=run_dir / "manifest.json",
            samples_path=run_dir / "samples.json",
        )

    def mkdirs(self) -> None:
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self.raw_dir.mkdir(parents=True, exist_ok=True)


def _raw_filename(
    sample_index: int,
    process: str,
    kind: str,
    phase: str,
) -> str:
    """Generate deterministic raw artifact filename."""
    pkg_short = "system_server" if process == SYSTEM_SERVER_PACKAGE else "systemui"
    return f"{sample_index:03d}_{phase}_{pkg_short}_{kind}.txt"


# ---------------------------------------------------------------------------
# Process sampling with raw artifact storage
# ---------------------------------------------------------------------------

@dataclass
class ProcessSample:
    """One process sample with parsed metrics and raw file references."""
    sample_index: int
    process: str
    pid: int | None
    taken_at: str
    phase: str  # PRE or POST
    metrics: dict[str, Any]
    raw_refs: dict[str, str] = field(default_factory=dict)
    errors: list[str] = field(default_factory=list)
    pid_changed: bool = False


def _find_pid(adb: probe.Adb, package: str) -> int | None:
    """Find PID for package, return None if not found (fail-visible)."""
    r = _adb_shell(adb, f"pidof {package}", timeout=15)
    if r.ok and r.stdout.strip():
        pids = [int(p) for p in re.split(r"\s+", r.stdout.strip()) if p.isdigit()]
        if pids:
            return min(pids)
    # Fallback: ps -A
    r = _adb_shell(adb, "ps -A", timeout=15)
    for line in r.stdout.splitlines():
        if package in line:
            parts = line.split()
            if parts and parts[1].isdigit():
                return int(parts[1])
    return None


def _parse_meminfo_fail_visible(text: str) -> dict[str, Any]:
    """Parse meminfo. Missing fields → None, never 0."""
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
    clock_tick: int,
    artifacts: ArtifactPaths,
    sample_index: int,
    phase: str,
    prev_pid: int | None = None,
) -> ProcessSample:
    """Sample a process, save raw output to artifacts, return structured sample."""
    taken_at = _now_iso()
    pid = _find_pid(adb, package)
    pid_changed = prev_pid is not None and pid is not None and pid != prev_pid

    metrics: dict[str, Any] = {k: None for k in probe.METRIC_UNITS}
    raw_refs: dict[str, str] = {}
    errors: list[str] = []

    if pid is None:
        errors.append(f"Process '{package}' not found")
        return ProcessSample(
            sample_index=sample_index, process=package, pid=None,
            taken_at=taken_at, phase=phase, metrics=metrics,
            raw_refs=raw_refs, errors=errors, pid_changed=pid_changed,
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
    # Save raw meminfo
    raw_name = _raw_filename(sample_index, package, "meminfo", phase)
    (artifacts.raw_dir / raw_name).write_text(meminfo_raw, encoding="utf-8", errors="replace")
    raw_refs["meminfo"] = f"raw/{raw_name}"

    # proc stat
    r = _adb_shell(adb, f"cat /proc/{pid}/stat", timeout=10)
    stat_raw = r.stdout
    if r.ok:
        stat = probe.parse_proc_stat(stat_raw, clock_tick)
        metrics["cpu_time_ms"] = stat.get("cpu_time_ms")
        if metrics["thread_count"] is None:
            metrics["thread_count"] = stat.get("thread_count")
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
        raw_refs=raw_refs, errors=errors, pid_changed=pid_changed,
    )


# ---------------------------------------------------------------------------
# Scenario definitions
# ---------------------------------------------------------------------------

@dataclass
class ScenarioStep:
    description: str
    commands: list[str] = field(default_factory=list)
    wait_after_s: float = 1.0
    critical: bool = True  # if False, failure is logged but not fatal


@dataclass
class Scenario:
    scenario_id: str
    description: str
    warmup_s: float
    steps: list[ScenarioStep]
    target_processes: list[str]
    repeat: int = 5
    requires_notification: bool = False


def _swipe_down() -> list[ScenarioStep]:
    return [ScenarioStep("Swipe down to open notification panel", ["input swipe 540 0 540 800 300"], 1.5)]


def _swipe_up_close() -> list[ScenarioStep]:
    return [ScenarioStep("Swipe up to close panel", ["input swipe 540 800 540 0 300"], 1.5)]


def _volume_press(direction: str, count: int) -> list[ScenarioStep]:
    keycode = "24" if direction == "up" else "25"
    return [
        ScenarioStep(f"Volume {direction} press {i+1}/{count}", [f"input keyevent {keycode}"], 0.5)
        for i in range(count)
    ]


def _qs_expand_full() -> list[ScenarioStep]:
    return [
        ScenarioStep("Swipe down to open notification panel", ["input swipe 540 0 540 800 300"], 1.0),
        ScenarioStep("Swipe down again to expand QS fully", ["input swipe 540 200 540 800 300"], 1.5),
    ]


SCENARIOS: dict[str, Scenario] = {
    "boot_stable": Scenario(
        scenario_id="boot_stable",
        description="Boot to home, wait 5 minutes, then sample.",
        warmup_s=300.0,
        steps=[ScenarioStep("Wait 5 minutes after boot for stabilization", [], 0, critical=False)],
        target_processes=[SYSTEM_SERVER_PACKAGE, SYSTEMUI_PACKAGE],
        repeat=1,
    ),
    "notification_panel_open_close": Scenario(
        scenario_id="notification_panel_open_close",
        description="Open and close notification panel 10 times.",
        warmup_s=10.0,
        steps=[*_swipe_down(), *_swipe_up_close()] * 10,
        target_processes=[SYSTEMUI_PACKAGE, SYSTEM_SERVER_PACKAGE],
        repeat=5,
    ),
    "volume_adjust_10": Scenario(
        scenario_id="volume_adjust_10",
        description="Press volume up 5 times then volume down 5 times.",
        warmup_s=10.0,
        steps=[*_volume_press("up", 5), *_volume_press("down", 5)],
        target_processes=[SYSTEM_SERVER_PACKAGE],
        repeat=5,
    ),
    "qs_panel_expand_collapse": Scenario(
        scenario_id="qs_panel_expand_collapse",
        description="Expand QS panel fully and collapse 10 times.",
        warmup_s=10.0,
        steps=[*_qs_expand_full(), *_swipe_up_close()] * 10,
        target_processes=[SYSTEMUI_PACKAGE, SYSTEM_SERVER_PACKAGE],
        repeat=5,
    ),
    "notification_menu_create_delete": Scenario(
        scenario_id="notification_menu_create_delete",
        description="Post notification, verify, open shade, long-press to trigger "
                    "MiuiNotificationMenuRow#createMenuViews, dismiss, cleanup. "
                    "Repeated 10 times. NOTIFICATION_SOURCE = SHELL_TEST_NOTIFICATION.",
        warmup_s=10.0,
        steps=[],  # Built dynamically in runner
        target_processes=[SYSTEMUI_PACKAGE, SYSTEM_SERVER_PACKAGE],
        repeat=5,
        requires_notification=True,
    ),
}


# ---------------------------------------------------------------------------
# Notification operations
# ---------------------------------------------------------------------------

def _post_notification(adb: probe.Adb, tag: str) -> AdbResult:
    """Post a shell test notification via `cmd notification post`."""
    cmd = f"cmd notification post -t 'Baseline Test' 'Baseline test body' {tag}"
    return _adb_shell(adb, cmd, timeout=15)


def _verify_notification_exists(adb: probe.Adb, tag: str, cap: NotificationCapability) -> tuple[bool, str]:
    """Verify notification with given tag exists. Returns (found, method)."""
    if cap.list_available:
        r = _adb_shell(adb, "cmd notification list", timeout=15)
        if r.ok and tag in r.stdout:
            return True, "cmd_notification_list"
        # list ran but tag not found
        if r.ok:
            return False, "cmd_notification_list_tag_not_found"
    # Fallback: dumpsys notification
    r = _adb_shell(adb, "dumpsys notification --noredact", timeout=20)
    if r.ok and tag in r.stdout:
        return True, "dumpsys_notification"
    # Try without --noredact
    r = _adb_shell(adb, "dumpsys notification", timeout=20)
    if r.ok and tag in r.stdout:
        return True, "dumpsys_notification_redacted"
    return False, "verification_failed"


def _swipe_dismiss_notification(adb: probe.Adb) -> AdbResult:
    """Dismiss notification via swipe-left on notification area."""
    return _adb_shell(adb, "input swipe 540 400 0 400 300", timeout=10)


def _build_notification_menu_steps(run_id: str, cap: NotificationCapability) -> list[ScenarioStep]:
    """Build steps for notification_menu_create_delete scenario."""
    tag = f"{NOTIF_TAG_PREFIX}_{run_id}"
    steps: list[ScenarioStep] = []

    for i in range(10):
        iter_tag = f"{tag}_{i}"
        # Post notification
        steps.append(ScenarioStep(
            f"[{i+1}/10] Post shell test notification (tag={iter_tag})",
            [f"cmd notification post -t 'Baseline Test' 'Baseline test body' {iter_tag}"],
            2.0,
        ))
        # Open shade
        steps.append(ScenarioStep(
            f"[{i+1}/10] Swipe down to open notification panel",
            ["input swipe 540 0 540 800 300"],
            1.5,
        ))
        # Long-press notification row to trigger MiuiNotificationMenuRow#createMenuViews
        steps.append(ScenarioStep(
            f"[{i+1}/10] Long-press notification row to trigger menu (LONG_PRESS_NOTIFICATION_ROW)",
            ["input swipe 540 400 540 400 800"],
            2.0,
        ))
        # Dismiss menu
        steps.append(ScenarioStep(
            f"[{i+1}/10] Tap to dismiss menu",
            ["input tap 540 400"],
            1.0,
        ))
        # Close panel
        steps.append(ScenarioStep(
            f"[{i+1}/10] Swipe up to close panel",
            ["input swipe 540 800 540 0 300"],
            1.5,
        ))
        # Cleanup: swipe-dismiss the notification
        steps.append(ScenarioStep(
            f"[{i+1}/10] Swipe-dismiss notification (cleanup)",
            ["input swipe 540 400 0 400 300"],
            1.0,
            critical=False,  # cleanup failure is not fatal for measurement
        ))

    return steps


# ---------------------------------------------------------------------------
# Doctor
# ---------------------------------------------------------------------------

def cmd_doctor(adb: probe.Adb) -> int:
    """Check device capabilities for runtime baseline collection."""
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

    # 1. ADB version
    try:
        ver = adb.version()
        check("adb_version", True, ver)
    except probe.ProbeError as exc:
        check("adb_version", False, exc.message)

    # 2. Devices
    try:
        devices = adb.devices()
        online = [d for d in devices if d.get("state") == "device"]
        check("device_count", len(online) >= 1, f"online={len(online)}")
        if not online:
            report["status"] = STATUS_DEVICE_NOT_CONNECTED
            _print(json.dumps(report, indent=2, ensure_ascii=False))
            return 1
        if len(online) == 1:
            check("device_single", True, online[0]["serial"])
        else:
            check("device_single", False, f"multiple devices: {len(online)}, use --device")
            report["status"] = "MULTIPLE_DEVICES"
            _print(json.dumps(report, indent=2, ensure_ascii=False))
            return 1
    except probe.ProbeError as exc:
        check("device_count", False, exc.message)
        report["status"] = STATUS_DEVICE_NOT_CONNECTED
        _print(json.dumps(report, indent=2, ensure_ascii=False))
        return 1

    # 3. Android version
    r = _adb_shell(adb, "getprop ro.build.version.release")
    check("android_version", r.ok, r.stdout.strip() or r.stderr)

    # 4. ROM fingerprint
    r = _adb_shell(adb, "getprop ro.miui.ui.version.name")
    rom = r.stdout.strip() if r.ok else "unknown"
    r2 = _adb_shell(adb, "getprop ro.build.display.id")
    check("rom_fingerprint", True, f"miui={rom} display={r2.stdout.strip() if r2.ok else 'unknown'}")

    # 5. pidof system_server
    r = _adb_shell(adb, "pidof system_server")
    ss_pid = r.stdout.strip() if r.ok and r.stdout.strip() else None
    check("system_server_pid", ss_pid is not None, ss_pid or "not found")

    # 6. pidof com.android.systemui
    r = _adb_shell(adb, "pidof com.android.systemui")
    sui_pid = r.stdout.strip() if r.ok and r.stdout.strip() else None
    check("systemui_pid", sui_pid is not None, sui_pid or "not found")

    # 7. dumpsys meminfo
    if ss_pid:
        r = _adb_shell(adb, f"dumpsys meminfo {ss_pid}", timeout=20)
        check("meminfo_system_server", r.ok and bool(r.stdout.strip()), f"rc={r.returncode} len={len(r.stdout)}")

    # 8. /proc/<pid>/stat
    if ss_pid:
        r = _adb_shell(adb, f"cat /proc/{ss_pid}/stat")
        check("proc_stat", r.ok and bool(r.stdout.strip()), f"rc={r.returncode}")

    # 9. /proc/<pid>/status
    if ss_pid:
        r = _adb_shell(adb, f"cat /proc/{ss_pid}/status")
        check("proc_status", r.ok and bool(r.stdout.strip()), f"rc={r.returncode}")

    # 10. cmd notification post --help
    r = _adb_shell(adb, "cmd notification post --help")
    check("notification_post_help", r.ok or "post" in (r.stdout + r.stderr).lower(),
          f"rc={r.returncode}")

    # 11. notification capability
    cap = _probe_notification_capability(adb)
    check("notification_capability", cap.cmd_notification_available,
          f"post={cap.post_available} list={cap.list_available} remove={cap.remove_available}")
    report["notification_capability"] = cap.as_dict()

    # 12. display size
    r = _adb_shell(adb, "wm size")
    check("display_size", r.ok, r.stdout.strip() or r.stderr)

    # 13. density
    r = _adb_shell(adb, "wm density")
    check("display_density", r.ok, r.stdout.strip() or r.stderr)

    # 14. orientation
    r = _adb_shell(adb, "dumpsys input | grep -i 'Orientation:'")
    check("display_orientation", r.ok, r.stdout.strip() or r.stderr)

    # 15. input swipe/keyevent executability (test with harmless keyevent)
    r = _adb_shell(adb, "input keyevent 82")  # KEYCODE_MENU, harmless
    check("input_keyevent", r.returncode == 0, f"rc={r.returncode}")

    report["status"] = "OK" if not report["errors"] else "CHECKS_FAILED"
    _print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not report["errors"] else 1


# ---------------------------------------------------------------------------
# Scenario runner
# ---------------------------------------------------------------------------

def _median_of(samples: list[dict[str, Any]], key: str) -> float | None:
    values = []
    for s in samples:
        v = s.get("metrics", {}).get(key)
        if v is not None and isinstance(v, (int, float)):
            values.append(v)
    return statistics.median(values) if values else None


def _build_median(samples: list[dict[str, Any]], allowed: set[str]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for metric in allowed:
        med = _median_of(samples, metric)
        result[metric] = {
            "unit": probe.METRIC_UNITS.get(metric, "unknown"),
            "source": probe.METRIC_SOURCES.get(metric, "unknown"),
            "value": med,
            "unavailable_reason": "" if med is not None else "NOT_REPORTED_BY_DEVICE",
        }
    return result


def _build_delta(pre: dict[str, dict[str, Any]], post: dict[str, dict[str, Any]]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for metric in post:
        pre_val = pre.get(metric, {}).get("value")
        post_val = post.get(metric, {}).get("value")
        delta_val = None
        if pre_val is not None and post_val is not None:
            delta_val = post_val - pre_val
        result[metric] = {
            "unit": probe.METRIC_UNITS.get(metric, "unknown"),
            "pre": pre_val,
            "post": post_val,
            "delta": delta_val,
        }
    return result


def _run_scenario(
    adb: probe.Adb,
    scenario: Scenario,
    module_state: str,
    repeat: int,
    output_dir: Path,
    run_id: str,
    clock_tick: int,
    notif_cap: NotificationCapability,
    display_info: DisplayInfo,
    device_info: dict[str, str],
    adb_serial: str | None,
) -> dict[str, Any]:
    """Run one scenario for all repetitions, return aggregated record."""
    artifacts = ArtifactPaths.for_run(output_dir, f"{run_id}_{scenario.scenario_id}")
    artifacts.mkdirs()

    # Build steps for notification scenario
    steps = scenario.steps
    if scenario.requires_notification:
        steps = _build_notification_menu_steps(run_id, notif_cap)

    all_pre: dict[str, list[dict[str, Any]]] = {}
    all_post: dict[str, list[dict[str, Any]]] = {}
    all_errors: list[str] = []
    all_pid_changes: list[dict[str, Any]] = []
    sample_counter = 0
    reps_completed = 0
    scenario_status = STATUS_OK

    for rep in range(repeat):
        _print(f"\n--- {scenario.scenario_id} rep {rep+1}/{repeat} ---")

        # Warmup
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
            pre_pids[pkg] = s.pid
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
                "errors": s.errors,
                "pid_changed": False,
            }
            pre_samples.append(sample_dict)
            all_pre.setdefault(pkg, []).append(sample_dict)

        # Notification verification for notification scenarios
        if scenario.requires_notification:
            # Verify first notification tag exists
            first_tag = f"{NOTIF_TAG_PREFIX}_{run_id}_0"
            found, method = _verify_notification_exists(adb, first_tag, notif_cap)
            if not found:
                _print(f"  FAILED_PRECONDITION: notification {first_tag} not verified via {method}")
                all_errors.append(f"notification verification failed: {first_tag} via {method}")
                scenario_status = STATUS_FAILED_PRECONDITION
                continue

        # Execute steps
        steps_executed = 0
        step_failed = False
        for step in steps:
            _print(f"  step: {step.description}")
            for cmd in step.commands:
                r = _adb_shell(adb, cmd, timeout=30)
                if not r.ok and step.critical:
                    all_errors.append(f"step '{step.description}': rc={r.returncode} stderr={r.stderr[:200]}")
                    _print(f"    CRITICAL FAIL: rc={r.returncode}")
                    step_failed = True
                    break
                elif not r.ok:
                    all_errors.append(f"step '{step.description}' (non-critical): rc={r.returncode}")
                    _print(f"    non-critical fail: rc={r.returncode}")
            if step_failed:
                break
            if step.wait_after_s > 0:
                time.sleep(step.wait_after_s)
            steps_executed += 1

        if step_failed:
            scenario_status = STATUS_FAILED
            continue

        # Settle
        time.sleep(2.0)

        # Post-samples with PID stability check
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
                "errors": s.errors,
                "pid_changed": s.pid_changed,
            }
            post_samples.append(sample_dict)
            all_post.setdefault(pkg, []).append(sample_dict)
            if s.pid_changed:
                all_pid_changes.append({
                    "process": pkg,
                    "repeat": rep,
                    "pre_pid": pre_pids.get(pkg),
                    "post_pid": s.pid,
                })

        reps_completed += 1

    # Aggregate
    median_pre: dict[str, dict[str, Any]] = {}
    median_post: dict[str, dict[str, Any]] = {}
    delta: dict[str, dict[str, Any]] = {}
    for pkg in scenario.target_processes:
        median_pre[pkg] = _build_median(all_pre.get(pkg, []), _metric_filter_for(pkg))
        median_post[pkg] = _build_median(all_post.get(pkg, []), _metric_filter_for(pkg))
        delta[pkg] = _build_delta(median_pre.get(pkg, {}), median_post.get(pkg, {}))

    # Write manifest
    manifest = {
        "run_id": run_id,
        "scenario_id": scenario.scenario_id,
        "module_state": module_state,
        "module_state_source": "OPERATOR_DECLARED",
        "device": device_info.get("device", "unknown"),
        "rom": device_info.get("rom", "unknown"),
        "android_version": device_info.get("android_version", "unknown"),
        "build_variant": device_info.get("build_variant", "unknown"),
        "adb_serial": adb_serial or "default",
        "clk_tck": clock_tick,
        "notification_capability": notif_cap.as_dict(),
        "display_info": display_info.as_dict(),
        "notification_source": "SHELL_TEST_NOTIFICATION" if scenario.requires_notification else "N/A",
        "menu_trigger_method": "LONG_PRESS_NOTIFICATION_ROW" if scenario.requires_notification else "N/A",
        "ui_automation_confidence": "COORDINATE_BASED" if scenario.requires_notification else "N/A",
        "repetitions_requested": repeat,
        "repetitions_completed": reps_completed,
        "scenario_status": scenario_status,
        "total_samples": sample_counter,
        "pid_changes": all_pid_changes,
        "errors": all_errors,
        "created_at": _now_iso(),
    }
    artifacts.manifest_path.write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    # Write samples
    samples_data = {
        "run_id": run_id,
        "scenario_id": scenario.scenario_id,
        "module_state": module_state,
        "median_pre": median_pre,
        "median_post": median_post,
        "delta": delta,
        "all_pre_samples": {k: v for k, v in all_pre.items()},
        "all_post_samples": {k: v for k, v in all_post.items()},
    }
    artifacts.samples_path.write_text(
        json.dumps(samples_data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    _print(f"\n  status={scenario_status} reps_completed={reps_completed}/{repeat}")
    _print(f"  manifest: {artifacts.manifest_path}")
    _print(f"  samples:  {artifacts.samples_path}")
    _print(f"  raw dir:  {artifacts.raw_dir}")

    return manifest


# ---------------------------------------------------------------------------
# Run command
# ---------------------------------------------------------------------------

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

    # Device check
    try:
        devices = adb.devices()
        online = [d for d in devices if d.get("state") == "device"]
        if not online:
            _print(f"{STATUS_DEVICE_NOT_CONNECTED}: no device online")
            return 1
    except probe.ProbeError as exc:
        _print(f"{STATUS_DEVICE_NOT_CONNECTED}: {exc.message}")
        return 1

    clock_tick = probe._resolve_clock_tick(adb)
    _print(f"CLK_TCK: {clock_tick}")
    device_info = probe._collect_device_info(adb)
    notif_cap = _probe_notification_capability(adb)
    display_info = _probe_display(adb)
    adb_serial = adb.device

    names = list(SCENARIOS.keys()) if "all" in scenario_names else scenario_names
    exit_code = 0

    for sname in names:
        scenario = SCENARIOS.get(sname)
        if not scenario:
            _print(f"unknown scenario: {sname}")
            exit_code = 2
            continue
        repeat = repeat_override if repeat_override is not None else scenario.repeat
        _run_scenario(
            adb, scenario, module_state, repeat, output_dir, run_id,
            clock_tick, notif_cap, display_info, device_info, adb_serial,
        )

    return exit_code


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="A13 runtime baseline scenario orchestrator")
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
        return 1

    if args.command == "doctor":
        return cmd_doctor(adb)
    if args.command == "run":
        return cmd_run(adb, args.scenario, args.module_state, args.repeat, args.output_dir)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
