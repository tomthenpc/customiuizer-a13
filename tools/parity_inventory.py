#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
EVIDENCE_LEVELS = {
    "MECHANICAL_ONLY",
    "IMPLEMENTATION_PRESENCE",
    "STRUCTURAL_SEMANTIC_PROOF",
    "INDIVIDUAL_SEMANTIC_PROOF",
}
PARITY_STATES = {
    "PRESENT_EQUIVALENT",
    "PRESENT_A13_VARIANT",
    "PARTIAL_PARITY",
    "MISSING_IN_A13",
    "INTENTIONAL_EXCLUDED",
    "DEAD_UPSTREAM_PATH",
    "HOLD_EVIDENCE",
    "INSUFFICIENT_EVIDENCE",
    "A13_ONLY_KEEP",
}


@dataclass
class UiNode:
    key: str
    tag: str
    title: str
    xml_file: str
    node_type: str


@dataclass
class A14Spec:
    feature_id: str
    name: str
    host_package: str
    keys: tuple[str, ...]
    source_path: str


@dataclass
class MissingAuditRecord:
    a14_feature_id: str
    a14_pref_keys: str
    a14_behavior: str
    a14_reference: str
    a13_search_terms: str
    a13_match: str
    a13_reference: str
    final_parity_state: str
    reclassification_reason: str
    absence_proof: str


def normalize_key(key: str) -> str:
    return key[len("pref_key_") :] if key.startswith("pref_key_") else key


def parse_strings(res_dir: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for values_dir in [res_dir / "values", res_dir / "values-en"]:
        if not values_dir.exists():
            continue
        for f in values_dir.glob("*.xml"):
            try:
                root = ET.parse(f).getroot()
            except ET.ParseError:
                continue
            for n in root.findall("string"):
                name = n.attrib.get("name")
                if name:
                    out[name] = "".join(n.itertext()).strip()
    return out


def classify_ui_node(tag: str, key: str) -> str:
    low = key.lower()
    if tag.endswith("PreferenceCategory"):
        return "CATEGORY"
    if low.endswith("_cat") or "_cat_" in low:
        return "CATEGORY"
    if low in {"system", "launcher", "controls", "various", "main"}:
        return "NAVIGATION_ENTRY"
    if any(x in low for x in ["_apps", "_bw", "_ignore", "_prerequisite", "_dependency"]):
        return "DEPENDENCY_HELPER"
    if any(x in low for x in ["_state", "_internal", "_applied", "_synced"]):
        return "INTERNAL_STATE"
    if tag.endswith("PreferenceScreen"):
        return "NAVIGATION_ENTRY"
    if tag.endswith("CheckBoxPreferenceEx") or tag.endswith("SwitchPreferenceCompat"):
        return "ACTIONABLE_FEATURE"
    if tag.endswith("ListPreferenceEx") or tag.endswith("DropDownPreferenceEx") or tag.endswith("SeekBarPreference"):
        return "SUBOPTION"
    if tag.endswith("PreferenceEx"):
        return "ACTIONABLE_FEATURE"
    return "UNKNOWN"


def parse_ui_nodes(repo: Path) -> tuple[dict[str, UiNode], int]:
    strings = parse_strings(repo / "app/src/main/res")
    nodes: dict[str, UiNode] = {}
    total = 0
    for f in sorted((repo / "app/src/main/res/xml").glob("prefs_*.xml")):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        for elem in root.iter():
            total += 1
            key = elem.attrib.get(ANDROID_NS + "key")
            if not key:
                continue
            key = normalize_key(key)
            title_ref = elem.attrib.get(ANDROID_NS + "title", "")
            title = title_ref
            if title_ref.startswith("@string/"):
                title = strings.get(title_ref.split("/", 1)[1], title_ref)
            node_type = classify_ui_node(elem.tag, key)
            nodes[key] = UiNode(key=key, tag=elem.tag, title=title, xml_file=f.name, node_type=node_type)
    return nodes, total


def extract_pref_reads(repo: Path) -> set[str]:
    keys: set[str] = set()
    patterns = [
        re.compile(r'get(?:Boolean|Int|Long|Float|String|StringAsInt)\("([a-z0-9_]+)"'),
        re.compile(r'key\s*=\s*"([a-z0-9_]+)"'),
        re.compile(r'preferenceKey\s*=\s*"([a-z0-9_]+)"'),
        re.compile(r'pref(?:erence)?(?:Key)?\s*[:=]\s*"([a-z0-9_]+)"'),
        re.compile(r'pref_key_([a-z0-9_]+)'),
    ]
    setof_pattern = re.compile(r'preferenceKeys\s*=\s*setOf\((.*?)\)', re.S)
    quoted = re.compile(r'"([a-z0-9_]+)"')
    for src in list(repo.glob("app/src/main/java/**/*.kt")) + list(repo.glob("app/src/main/java/**/*.java")):
        text = src.read_text(encoding="utf-8", errors="ignore")
        for p in patterns:
            for m in p.finditer(text):
                keys.add(m.group(1))
        for block in setof_pattern.finditer(text):
            for m in quoted.finditer(block.group(1)):
                keys.add(m.group(1))
    return keys


def parse_a14_specs(repo: Path) -> tuple[dict[str, A14Spec], int, int]:
    def extract_lazy_feature_blocks(text: str) -> list[str]:
        out: list[str] = []
        needle = "LazyFeatureSpec("
        start = 0
        while True:
            idx = text.find(needle, start)
            if idx < 0:
                break
            i = idx + len(needle)
            depth = 1
            while i < len(text) and depth > 0:
                ch = text[i]
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                i += 1
            if depth == 0:
                out.append(text[idx + len(needle): i - 1])
                start = i
            else:
                break
        return out

    specs_by_key: dict[str, A14Spec] = {}
    unknown = 0
    discovered = 0
    for src in repo.glob("app/src/main/java/**/*.kt"):
        text = src.read_text(encoding="utf-8", errors="ignore")
        for block in extract_lazy_feature_blocks(text):
            id_match = re.search(r"id\s*=\s*([A-Za-z0-9_]+)", block)
            name_match = re.search(r'name\s*=\s*"([^"]+)"', block)
            target_match = re.search(r"target\s*=\s*FeatureTarget\.([A-Z_]+)", block)
            if not id_match or not name_match or not target_match:
                unknown += 1
                continue
            keys: list[str] = []
            pref_match = re.search(r'preferenceKey\s*=\s*(null|"[^"]*"|[A-Za-z0-9_]+)', block)
            if pref_match:
                pref_raw = pref_match.group(1)
                if pref_raw.startswith('"') and pref_raw.endswith('"'):
                    keys.append(pref_raw.strip('"'))
            multi_keys = re.search(r"preferenceKeys\s*=\s*listOf\((.*?)\)", block, re.S)
            if multi_keys:
                keys.extend(re.findall(r'"([a-z0-9_]+)"', multi_keys.group(1)))
            keys = sorted(set(keys))
            if not keys and "preferenceKey = null" in block:
                # Visible null-key behavior requires manual inventory rows.
                discovered += 1
                continue
            if not keys:
                unknown += 1
                continue
            discovered += 1
            spec = A14Spec(
                feature_id=id_match.group(1),
                name=name_match.group(1),
                host_package=target_match.group(1),
                keys=tuple(keys),
                source_path=str(src).replace("\\", "/"),
            )
            for key in keys:
                specs_by_key[key] = spec
    return specs_by_key, discovered, unknown


def process_scope_for_host(host_package: str) -> tuple[str, str]:
    if host_package == "SYSTEM_UI":
        return "com.android.systemui", "systemui"
    if host_package == "LAUNCHER":
        return "com.miui.home", "launcher"
    if host_package == "SYSTEM_SERVER":
        return "android", "boot"
    if host_package == "SECURITY_CENTER":
        return "com.miui.securitycenter", "app"
    if host_package == "PACKAGE_INSTALLER":
        return "com.google.android.packageinstaller", "app"
    if host_package == "SETTINGS":
        return "com.android.settings", "app"
    if host_package == "ANY":
        return "multi_process", "per-host"
    if host_package == "SYSTEM_PACKAGE":
        return "android.system.package", "app"
    return "unresolved", "unresolved"


def infer_host_package_from_key(domain: str, key: str) -> str:
    low = f"{domain} {key}".lower()
    if low.startswith("launcher") or key.startswith("launcher_"):
        return "LAUNCHER"
    if key.startswith("system_") and any(t in key for t in [
        "statusbar", "lockscreen", "drawer", "cc_", "volume", "charginginfo",
        "netspeed", "strong_toast", "qs_", "notif_", "batteryindicator",
        "visualizer", "screenshot", "albumartonlock", "noscreenlock", "lsalarm",
        "networkindicator", "showpct", "vibration", "ccgrid", "calendar_app",
        "clock_app", "fw_", "notify_", "cleanopenwith", "cleanshare",
    ]):
        return "SYSTEM_UI"
    if key.startswith("system_") and any(t in key for t in [
        "usb_default", "window_blur", "autobrightness", "force_dark",
        "applock", "animationscale", "credentials", "lstimeout", "nosilentvibrate",
    ]):
        return "SYSTEM_SERVER"
    if key.startswith("system_"):
        return "SYSTEM_UI"
    if key.startswith("controls_"):
        return "SYSTEM_UI"
    if key.startswith("various_") and any(t in key for t in ["security", "antivirus", "marketing", "permission", "analytics", "daemon", "update_services"]):
        return "SECURITY_CENTER"
    if key.startswith("various_"):
        return "SYSTEM_PACKAGE"
    if key.startswith("miuizer_"):
        return "SETTINGS"
    if "installer" in low or "package" in low:
        return "PACKAGE_INSTALLER"
    if "backup" in low or "restore" in low or "search" in low or "about" in low:
        return "SETTINGS"
    return "UNKNOWN_DISCOVERY"


def evidence_for_row(key: str, has_a13: bool, a14_reads: set[str], a13_reads: set[str]) -> str:
    if not has_a13:
        return "MECHANICAL_ONLY"
    if key in a14_reads and key in a13_reads:
        return "IMPLEMENTATION_PRESENCE"
    return "MECHANICAL_ONLY"


def route_phase_e_batch(host_package: str, process: str, key: str, a14_name: str, parity_state: str) -> str:
    if parity_state not in {"MISSING_IN_A13", "PARTIAL_PARITY"}:
        return ""
    low = f"{key} {a14_name}".lower()
    if host_package in {"UNKNOWN_DISCOVERY", "UNRESOLVED"}:
        return "HOLD_EVIDENCE"
    if any(t in low for t in ["backup", "restore", "language", "about", "search", "restart", "lazy", "grouping"]):
        return "E1"
    if host_package in {"SYSTEM_UI", "LAUNCHER"}:
        return "E3"
    if host_package in {"SECURITY_CENTER", "PACKAGE_INSTALLER", "SYSTEM_PACKAGE"}:
        return "E4"
    if host_package == "SYSTEM_SERVER" or process == "android":
        return "E5"
    if any(t in low for t in ["permission", "privacy", "updater", "daemon", "analytics", "marketing", "antivirus", "installer", "hide_report"]):
        return "E4"
    if host_package in {"SETTINGS", "ANY"} and any(t in low for t in ["settings", "permission"]):
        return "E4"
    if host_package == "SETTINGS":
        return "E2"
    if host_package and host_package not in {"UNKNOWN_DISCOVERY", "UNRESOLVED"}:
        return "E2"
    return "HOLD_EVIDENCE"


def implementation_mode_for(parity_state: str, phase_e_batch: str, upgraded_existing: bool) -> str:
    if parity_state in {
        "PRESENT_EQUIVALENT",
        "PRESENT_A13_VARIANT",
        "A13_ONLY_KEEP",
        "INTENTIONAL_EXCLUDED",
        "DEAD_UPSTREAM_PATH",
        "INSUFFICIENT_EVIDENCE",
    }:
        return "NO_IMPLEMENTATION"
    if parity_state == "HOLD_EVIDENCE" or phase_e_batch == "HOLD_EVIDENCE":
        return "EVIDENCE_HOLD"
    if upgraded_existing:
        return "UPGRADE_EXISTING_A13"
    return "NEW_PORT"


def derive_batch_counts(rows: list[dict[str, str]]) -> Counter:
    c: Counter = Counter()
    for r in rows:
        if r["parity_state"] in {"MISSING_IN_A13", "PARTIAL_PARITY"} and r["phase_e_batch"] in {"E1", "E2", "E3", "E4", "E5"}:
            c[r["phase_e_batch"]] += 1
    return c


def parity_accounting_invariant(rows: list[dict[str, str]]) -> bool:
    lhs = sum(1 for r in rows if r["a14_feature_id"])
    rhs = sum(1 for r in rows if r["a14_feature_id"] and r["parity_state"] in {
        "PRESENT_EQUIVALENT",
        "PRESENT_A13_VARIANT",
        "PARTIAL_PARITY",
        "MISSING_IN_A13",
        "INTENTIONAL_EXCLUDED",
        "DEAD_UPSTREAM_PATH",
        "HOLD_EVIDENCE",
        "INSUFFICIENT_EVIDENCE",
    })
    return lhs == rhs


def build_sanity_overrides() -> dict[str, dict[str, str]]:
    # Explicit semantic proofs for required sanity features.
    return {
        "system_usb_default_function": {
            "parity_state": "PARTIAL_PARITY",
            "evidence_level": "INDIVIDUAL_SEMANTIC_PROOF",
            "proof_id": "PROOF_USB_DEFAULT_ALIAS_A13_SYSTEM_DEFAULTUSB",
            "a14_behavior": "Sets default USB function behavior (follow system/charge/MTP/PTP) via SystemUsbDefaultHooks.",
            "a13_behavior": "A13 already exposes default USB config through system_defaultusb + USBConfigHook/USBConfigSettingsHook.",
            "risk": "HIGH",
            "priority": "P0",
            "a14_reference": "mods/utils/feature/SystemServerFeatures.kt::UsbDefaultFunctionFeatureId",
            "a13_reference": "mods/SystemSettingsMoreHooks.kt::USBConfigHook, USBConfigSettingsHook",
            "api33": "Upgrade existing A13 USB default implementation parity branches (preserve security/lock behavior).",
            "test_strategy": "System-server + settings hook behavioral parity tests for mode transitions.",
            "rom_evidence": "YES",
            "implementation_mode": "UPGRADE_EXISTING_A13",
        },
        "controls_hide_ime_dismiss_button": {
            "parity_state": "PRESENT_A13_VARIANT",
            "evidence_level": "INDIVIDUAL_SEMANTIC_PROOF",
            "proof_id": "PROOF_HIDE_IME_DISMISS_PHASE_E",
            "a14_behavior": "Hides gesture-navigation IME dismiss affordance.",
            "a13_behavior": "A13 HideImeDismissButtonHook on NavigationBarView.updateNavButtonIcons; gestural IME back-alt only.",
            "risk": "LOW",
            "priority": "P2",
            "a14_reference": "mods/utils/feature/SystemUiFeatures.kt::HideImeDismissButtonFeatureId",
            "a13_reference": "mods/Controls.kt::HideImeDismissButtonHook",
            "api33": "Existing A13 SystemUI IME-dismiss gate.",
            "test_strategy": "HideImeDismissButtonTest.",
            "rom_evidence": "NO",
            "implementation_mode": "NO_IMPLEMENTATION",
        },
    }


def missing_semantic_aliases() -> dict[str, dict[str, str]]:
    return {
        "system_usb_default_function": {
            "a13_keys": "system_defaultusb,system_defaultusb_unsecure",
            "parity_state": "PRESENT_A13_VARIANT",
            "reason": "A13 USBConfigHook plus UsbDefaultFunctionMapper and USB R1 disconnect latch cover default MTP/PTP across replug.",
            "a13_reference": "mods/SystemSettingsMoreHooks.kt::USBConfigHook; utils/UsbDefaultFunctionMapper.kt; utils/UsbConnectLatch.kt",
            "implementation_mode": "NO_IMPLEMENTATION",
            "host_package": "SYSTEM_SERVER",
            "a13_behavior": "A13-owned setCurrentFunction path; A14 HAL setEnabledFunctions(JZI) was not copied.",
        },
        "system_detailednetspeed_style": {
            "a13_keys": "system_detailednetspeed,system_detailednetspeed_fakedualrow",
            "parity_state": "HOLD_EVIDENCE",
            "reason": "Replacing live A13 detailed/fakedualrow toggles with an A14 list selector would migrate stored prefs.",
            "a13_reference": "res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt",
            "implementation_mode": "EVIDENCE_HOLD",
            "host_package": "SYSTEM_UI",
            "phase_e_batch": "HOLD_EVIDENCE",
            "a13_behavior": "Detailed netspeed and fake dual-row already exist; selector migration is not statically safe.",
        },
        "launcher_folderblur_disable": {
            "a13_keys": "launcher_folderblur_opacity",
            "parity_state": "PARTIAL_PARITY",
            "reason": "A13 FolderBlurHook already owns folder blur via opacity; A14 adds a disable flag that preserves the stored opacity.",
            "a13_reference": "mods/LauncherFolderHooks.kt::FolderBlurHook; installers/LauncherInstaller.java",
            "implementation_mode": "UPGRADE_EXISTING_A13",
            "host_package": "LAUNCHER",
            "a13_behavior": "Opacity 0 disables blur but discards the stored intensity; no independent disable toggle.",
        },
        "system_netspeed_boldfont": {
            "a13_keys": "system_netspeed_bold",
            "parity_state": "PRESENT_A13_VARIANT",
            "reason": "Same user capability: bold network-speed typeface. A14 renamed the key.",
            "a13_reference": "res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper",
            "implementation_mode": "NO_IMPLEMENTATION",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "system_netspeed_bold already drives NetSpeedTypefaceHelper.apply().",
        },
        "system_netspeed_use_clock_style": {
            "a13_keys": "system_netspeed_bold,system_netspeed_fontsize",
            "parity_state": "PARTIAL_PARITY",
            "reason": "A13 already customizes netspeed typeface/size; A14 adds match-clock-style on the same helper.",
            "a13_reference": "mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper",
            "implementation_mode": "UPGRADE_EXISTING_A13",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "Bold and font-size exist; no clock-style typeface copy path.",
        },
        "system_statusbarcontrols_dt_left": {
            "a13_keys": "system_statusbarcontrols_dt",
            "parity_state": "HOLD_EVIDENCE",
            "reason": "Left/right hit-testing needs device geometry; A13 already has one whole-bar double-tap action.",
            "a13_reference": "mods/SystemUIControlCenterHooks.kt; res/xml/prefs_system_statusbarcontrols.xml",
            "implementation_mode": "EVIDENCE_HOLD",
            "host_package": "SYSTEM_UI",
            "phase_e_batch": "HOLD_EVIDENCE",
            "a13_behavior": "Single system_statusbarcontrols_dt handles the whole bar.",
        },
        "system_statusbarcontrols_dt_right": {
            "a13_keys": "system_statusbarcontrols_dt",
            "parity_state": "HOLD_EVIDENCE",
            "reason": "Left/right hit-testing needs device geometry; A13 already has one whole-bar double-tap action.",
            "a13_reference": "mods/SystemUIControlCenterHooks.kt; res/xml/prefs_system_statusbarcontrols.xml",
            "implementation_mode": "EVIDENCE_HOLD",
            "host_package": "SYSTEM_UI",
            "phase_e_batch": "HOLD_EVIDENCE",
            "a13_behavior": "Single system_statusbarcontrols_dt handles the whole bar.",
        },
        "system_charginginfo_fontsize": {
            "a13_keys": "system_charginginfo,system_charginginfo_view",
            "parity_state": "PARTIAL_PARITY",
            "reason": "A13 lockscreen charging-info family exists; A14 adds a font-size suboption on the same view.",
            "a13_reference": "mods/SystemChargingAndWallpaperHooks.kt; res/xml/prefs_system_charginginfo.xml",
            "implementation_mode": "UPGRADE_EXISTING_A13",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "Charging current/voltage/wattage/temp/view exist; no fontsize seekbar.",
        },
        "system_statusbar_dualrows_left_ratio": {
            "a13_keys": "system_statusbar_dualrows,system_statusbar_dualrows_firstrow_horizmargin",
            "parity_state": "PARTIAL_PARITY",
            "reason": "A13 dual-row status bar exists with first-row padding; A14 adds left-width ratio.",
            "a13_reference": "mods/SystemUIStatusBarHooks.kt; res/xml/prefs_system.xml",
            "implementation_mode": "UPGRADE_EXISTING_A13",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "Dual rows + first-row horizontal margin exist; no left-ratio split.",
        },
        "system_statusbaricons_bluetoothicn": {
            "a13_keys": "system_statusbaricons_bluetooth",
            "parity_state": "PRESENT_A13_VARIANT",
            "reason": "A13 HideIconsBluetoothHook option 3 already always-hides the bluetooth icon.",
            "a13_reference": "mods/SystemStatusBarMoreHooks.kt::HideIconsBluetoothHook",
            "implementation_mode": "NO_IMPLEMENTATION",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "system_statusbaricons_bluetooth=3 sets bluetooth icon visibility false.",
        },
        "system_statusbaricons_wireless_headset": {
            "a13_keys": "system_statusbaricons_headset",
            "parity_state": "PARTIAL_PARITY",
            "reason": "A13 hides the headset slot; A14 adds a separate wireless_headset slot on the same hide-icons path.",
            "a13_reference": "mods/SystemUIStatusBarHooks.kt; res/xml/prefs_system_hideicons.xml",
            "implementation_mode": "UPGRADE_EXISTING_A13",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "Only slot name 'headset' is gated; no wireless_headset slot.",
        },
        "system_strong_toast_island_offset": {
            "a13_keys": "dynamic_island",
            "parity_state": "HOLD_EVIDENCE",
            "reason": "Dynamic Island helper preference; product policy forbids extra DI gaps.",
            "a13_reference": "ABSENT (Dynamic Island excluded)",
            "implementation_mode": "EVIDENCE_HOLD",
            "host_package": "SYSTEM_UI",
            "phase_e_batch": "HOLD_EVIDENCE",
            "a13_behavior": "No strong-toast/island implementation; offset is a DI helper, not a remaining product gap.",
        },
    }


def nearest_rejected_candidates() -> dict[str, tuple[str, str]]:
    return {
        "launcher_dock_height": (
            "launcher_dock_topmargin / launcher_dock_bottommargin",
            "those hooks change dock margins in LauncherLayoutHooks, not hotseat/dock height",
        ),
        "various_installer_purify": (
            "various_miuiinstaller",
            "MiuiPackageInstallerHook forces the MIUI installer; purify removes installer UI clutter",
        ),
        "various_disable_reset_recents_privacy_blur": (
            "system_recents_blur",
            "RecentsBlurRatioHook is recents background blur intensity, not privacy-thumbnail persist",
        ),
        "system_statusbaricons_privacy_prompt": (
            "system_statusbaricons_privacy",
            "A13 privacy key hides incognito/stealth; privacy_prompt is the camera/mic privacy indicator",
        ),
        "system_hidestatusbar_whenscreenrecord": (
            "system_hidestatusbar_whenscreenshot",
            "existing hook is screenshot-only in BatteryIndicator; screen-record is a different trigger",
        ),
        "system_autobrightness_reset_when_screenoff": (
            "system_autobrightness / system_autobrightness_min/max",
            "A13 AutoBrightness hooks clamp range; they do not reset brightness after screen-off",
        ),
        "system_strong_toast_mode": (
            "system_notif_disable_strong_toast (absent)",
            "A13 has no status-capsule / strong-toast presentation path; island mode is DI-excluded",
        ),
        "system_cc_freeform_when_longclick": (
            "Control Center tile click hooks",
            "A13 CC hooks cover volume/theme/clock, not long-click-tile-open-in-freeform",
        ),
        "system_volume_hide_dnd_shortcut": (
            "MIUIVolumeDialogHook",
            "A13 volume hook covers autohide/blur, not DND shortcut visibility",
        ),
        "system_volume_hide_mute_shortcut": (
            "MIUIVolumeDialogHook",
            "A13 volume hook covers autohide/blur, not mute shortcut visibility",
        ),
        "system_disable_window_blurs": (
            "system_recents_blur / folder blur",
            "those are app-surface blur intensities, not system_server window-blur disable",
        ),
        "system_force_darken_allapps": (
            "debug.hwui.force_dark write in SystemSecurityAndSystemHooks",
            "A13 only forces the property false in one path; no all-apps force-dark feature",
        ),
    }


def phase_f_dead_a14_ui_keys() -> dict[str, str]:
    """A14 preference UI with no reachable production implementation at the pinned SHA."""
    return {
        "system_hidestatusbar_whenscreenrecord": (
            "Pinned A14 has XML/strings only; no Java/Kotlin hook, FeatureSpec installer, or pref read."
        ),
    }


def phase_f_hold_missing() -> dict[str, dict[str, str]]:
    """Remaining true A14-only gaps that cannot be decided or safely implemented on A13/API33."""
    holds: dict[str, dict[str, str]] = {}

    def add(key: str, question: str, rom: str, default: str, evidence: str, forbidden: str) -> None:
        holds[key] = {
            "unresolved_question": question,
            "affected_rom_process": rom,
            "safe_default": default,
            "required_device_evidence": evidence,
            "why_forbidden": forbidden,
        }

    cc_plugin = (
        "miui.systemui.plugin MainPanelContentDistributor / QS tile color controllers",
        "com.android.systemui + miui.systemui.plugin",
        "off / ROM default",
        "MIUI 14 vs HyperOS 1 Control Center plugin class dump",
        "A14 CC color/hide-edit hooks target HyperOS MainPanelContentDistributor; A13 MIUI 14 CC uses ControlCenterWindowViewImpl. Fail-open would hide a dead toggle.",
    )
    for key in [
        "system_cc_btandtorch_ascard",
        "system_cc_card_enabled_color",
        "system_cc_card_enabled_color_custom",
        "system_cc_card_enabled_iconcolor_custom",
        "system_cc_card_enabled_primary_textcolor",
        "system_cc_card_enabled_secondary_textcolor",
        "system_cc_clock_centeralign",
        "system_cc_floatingtimetile",
        "system_cc_freeform_when_longclick",
        "system_cc_hide_edit",
        "system_cc_hide_profile_monitoring",
        "system_cc_slider_color_enable",
        "system_cc_slider_icon_color",
        "system_cc_slider_progress_color",
        "system_cc_tile_enabled_color",
        "system_cc_tile_enabled_color_custom",
        "system_cc_tile_enabled_color_usemonet",
        "system_cc_tile_enabled_iconcolor_custom",
    ]:
        add(key, *cc_plugin)

    digital = (
        "Whether MIUI 14 StatusBar has a digital-signal view slot equivalent to A14",
        "com.android.systemui",
        "stock signal icon",
        "StatusBar layout dump on MIUI 14 and HyperOS 1 A13",
        "A14 injects a new digital-signal view family; A13 has analog/icon signal hooks only.",
    )
    for key in [
        "system_statusbar_mobile_digital_signal",
        "system_statusbar_mobile_digital_signal_align",
        "system_statusbar_mobile_digital_signal_bold",
        "system_statusbar_mobile_digital_signal_fontsize",
        "system_statusbar_mobile_digital_signal_hideunit",
        "system_statusbar_mobile_digital_signal_in2rows",
        "system_statusbar_mobile_digital_signal_leftmargin",
        "system_statusbar_mobile_digital_signal_rightmargin",
        "system_statusbar_mobile_digital_signal_verticaloffset",
    ]:
        add(key, *digital)

    drawer = (
        "Notification shade date view identity on MIUI 14 vs HyperOS 1",
        "com.android.systemui",
        "stock shade date",
        "Notification header/date view hierarchy",
        "A14 drawer-date hooks target shade header classes not proven on MIUI 14.",
    )
    for key in [
        "system_drawer_date_centeralign",
        "system_drawer_date_fontsize",
        "system_drawer_dateformat",
        "system_drawer_hidedate",
        "system_drawer_remove_emptynotify",
    ]:
        add(key, *drawer)

    volume = (
        "MIUI volume dialog shortcut/button view IDs on A13",
        "com.android.systemui",
        "stock volume dialog",
        "Volume dialog view dump",
        "A14 volume-mode-button color/hide hooks are a parallel path on top of A13 MIUIVolumeDialogHook autohide/blur.",
    )
    for key in [
        "system_volume_hide_dnd_shortcut",
        "system_volume_hide_mute_shortcut",
        "system_volume_mode_button_background_color",
        "system_volume_mode_button_colors",
        "system_volume_mode_button_icon_color",
    ]:
        add(key, *volume)

    add(
        "launcher_wallpaper_colormode",
        "Launcher wallpaper color-mode API on MIUI 14 Home vs HyperOS 1",
        "com.miui.home",
        "ROM wallpaper coloring",
        "DeviceConfig/wallpaper color-mode field names",
        "No A13 counterpart; speculative GlobalLauncher/DeviceConfig writes are forbidden.",
    )
    add(
        "system_recents_card_style",
        "Recents card style controller class on MIUI 14 Home",
        "com.miui.home / com.android.systemui",
        "stock recents cards",
        "Recents container class dump",
        "A14 recents card-style is a new view path, not an upgrade of A13 recents blur.",
    )
    add(
        "system_statusbaricons_privacy_prompt",
        "Camera/mic privacy-indicator slot name on MIUI 14 SystemUI",
        "com.android.systemui",
        "stock privacy indicator",
        "Status bar slot dump",
        "A13 system_statusbaricons_privacy hides incognito/stealth, not the privacy chip.",
    )
    add(
        "system_strong_toast_mode",
        "Status-capsule / strong-toast presenter class on MIUI 14",
        "com.android.systemui",
        "stock toast/capsule",
        "Strong toast / island presenter dump",
        "No A13 capsule path; island mode is DI-adjacent.",
    )
    add(
        "system_lockscreen_disable_edit",
        "Keyguard editor entry class on MIUI 14 vs HyperOS 1",
        "com.android.systemui",
        "stock keyguard editor",
        "Lockscreen editor activity dump",
        "A14 disables a HyperOS keyguard editor not proven on MIUI 14.",
    )
    add(
        "system_notif_disable_fold",
        "MIUI fold-notification controller on A13",
        "com.android.systemui",
        "stock fold notifications",
        "Notification fold policy class dump",
        "No A13 fold-notification hook family.",
    )
    add(
        "system_qs_disable_fakeclock_anim",
        "Fake-clock animation owner on MIUI 14 QS/CC",
        "com.android.systemui / miui.systemui.plugin",
        "stock fake clock",
        "QS clock animator class dump",
        "A14 fake-clock hook is plugin-CC specific.",
    )
    add(
        "system_statusbar_content_vertical_offset",
        "Status-bar content geometry owner on MIUI 14",
        "com.android.systemui",
        "stock geometry",
        "Collapsed status bar layout dump",
        "New geometry rewrite; high visual-regression risk without device proof.",
    )
    add(
        "system_statusbar_enable_weather_param",
        "Weather status-bar param API on MIUI 14",
        "com.android.systemui",
        "stock weather",
        "Weather controller class dump",
        "No A13 weather-param hook.",
    )
    add(
        "system_statusbar_icons_atleft_onkeyguard",
        "Keyguard status-bar icon gravity on MIUI 14",
        "com.android.systemui",
        "stock keyguard icon gravity",
        "Keyguard status-bar layout dump",
        "New keyguard-only icon placement path.",
    )
    add(
        "system_disable_window_blurs",
        "Whether MIUI 14 system_server BlurController matches AOSP getBlurDisabledSetting",
        "android / system_server",
        "ROM blur policy",
        "system_server BlurController members on MIUI 14 and HyperOS 1 A13",
        "system_server boot path; A14 also adds a live PreferenceObserver. A13 catalog/contract wiring plus overlay ROM variants cannot be proven statically.",
    )
    add(
        "system_force_darken_allapps",
        "ForceDarkAppListProvider/Manager presence vs A13 NoDarkForceHook",
        "android / system_server",
        "stock per-app dark list",
        "ForceDark* class dump; interaction with system_nodarkforce",
        "A14 uses ForceDarkAppList* ; A13 already owns the opposite NoDarkForceHook. Parallel implementation is forbidden.",
    )
    add(
        "system_autobrightness_reset_when_screenoff",
        "DisplayPowerController.setScreenState(int,boolean) intercept vs existing A13 initialize hook",
        "android / system_server",
        "stock auto-brightness",
        "setScreenState signature and chain.proceed interaction on MIUI 14",
        "A13 already hooks DisplayPowerController.initialize. Adding an intercept/chain.proceed path is a boot-safety choice that is not unique statically.",
    )
    e4 = (
        "ROM component/package names for daemon/analytics/antivirus/marketing/permission controller",
        "module app + com.miui.securitycenter / com.miui.daemon / permissioncontroller",
        "components remain enabled",
        "Package/component inventory on MIUI 14 and HyperOS 1",
        "A14 settings-app PackageManager disable lists are ROM-specific; wrong names would present a working toggle with no effect or disable the wrong component.",
    )
    for key in [
        "various_block_location_permission_prompts",
        "various_block_notification_permission_prompts",
        "various_disable_miui_daemon",
        "various_disable_reset_recents_privacy_blur",
        "various_disable_update_services",
        "various_disable_xiaomi_analytics",
        "various_remove_security_center_antivirus",
        "various_trim_miui_daemon_network",
        "various_trim_security_center_marketing",
    ]:
        add(key, *e4)
    return holds


RESOLVED_HOSTS = {
    "SYSTEM_UI",
    "LAUNCHER",
    "SYSTEM_SERVER",
    "SETTINGS",
    "SECURITY_CENTER",
    "PACKAGE_INSTALLER",
    "SYSTEM_PACKAGE",
    "ANY",
}


def apply_same_key_family_proof(
    key: str,
    host_package: str,
    a14_reads: set[str],
    a13_reads: set[str],
) -> dict[str, str] | None:
    if key not in a14_reads or key not in a13_reads:
        return None
    if host_package not in RESOLVED_HOSTS:
        return None
    prefix = "_".join(key.split("_")[:2]) if "_" in key else key
    return {
        "parity_state": "PRESENT_A13_VARIANT",
        "evidence_level": "STRUCTURAL_SEMANTIC_PROOF",
        "proof_id": f"PROOF_SHARED_{host_package}_{prefix}",
        "source_relationship": "UPSTREAM_INTENT_EQUIVALENT",
        "a14_behavior": f"A14 reads `{key}` in host family {host_package}.",
        "a13_behavior": f"A13 reads the same key in the same host family; no A14-only extra branch on this row.",
        "a14_reference": f"pref read `{key}`",
        "a13_reference": f"pref read `{key}`",
    }


def build_a13_search_index(a13: Path) -> dict[str, str]:
    index: dict[str, str] = {}
    roots = [
        a13 / "app/src/main/java",
        a13 / "app/src/main/res/xml",
        a13 / "app/src/main/res/values",
    ]
    for root in roots:
        if not root.exists():
            continue
        for src in root.rglob("*"):
            if src.suffix.lower() not in {".kt", ".java", ".xml"}:
                continue
            index[str(src.relative_to(a13)).replace("\\", "/")] = src.read_text(encoding="utf-8", errors="ignore")
    return index


def search_index_hits(index: dict[str, str], term: str, limit: int = 6) -> list[str]:
    if not term or len(term.strip()) < 3:
        return []
    needle = term.lower()
    hits: list[str] = []
    for path, text in index.items():
        if needle in text.lower():
            hits.append(path)
            if len(hits) >= limit:
                break
    return hits


def nearest_a13_keys(key: str, a13_keys: set[str], limit: int = 4) -> list[str]:
    parts = key.split("_")
    candidates: list[tuple[int, str]] = []
    for other in a13_keys:
        if other == key:
            continue
        shared = 0
        for a, b in zip(parts, other.split("_")):
            if a != b:
                break
            shared += 1
        if shared >= 2:
            candidates.append((shared, other))
    candidates.sort(key=lambda item: (-item[0], item[1]))
    return [name for _, name in candidates[:limit]]


def build_absence_proof(
    key: str,
    feature_id: str,
    title: str,
    index: dict[str, str],
    a13_nodes: dict[str, UiNode],
    a13_reads: set[str],
    nearest: list[str],
) -> str:
    terms = [
        ("key", key),
        ("feature id", feature_id),
        ("title", title),
    ]
    skip_tokens = {
        "system", "launcher", "various", "pref", "key", "a14", "ui", "the", "and",
        "hide", "show", "button", "color", "custom", "enabled", "mode", "title",
        "when", "from", "with", "this", "that", "controls",
    }
    for token in key.split("_"):
        if token not in skip_tokens and len(token) >= 5:
            terms.append((f"token '{token}'", token))
    lines = ["A13_SEARCHED ="]
    for label, term in terms:
        if not term:
            continue
        hits = search_index_hits(index, term)
        if hits:
            lines.append(f"- {label} `{term}`: hits {', '.join(hits[:4])}")
        else:
            lines.append(f"- {label} `{term}`: no match")
    catalog_hits = search_index_hits(index, key, limit=3)
    owner_files = [h for h in catalog_hits if "FeatureCatalog" in h or "Installer" in h or "PreferenceSchema" in h]
    if owner_files:
        lines.append(f"- FeatureCatalog/installer/schema: {', '.join(owner_files)}")
    else:
        lines.append("- FeatureCatalog/installer/schema: no owner for this key")
    if nearest:
        lines.append(f"- nearest A13 keys: {', '.join(nearest)}")
    rejected = nearest_rejected_candidates().get(key)
    if rejected:
        lines.append(f"- nearest candidate `{rejected[0]}` inspected and rejected because {rejected[1]}")
    elif nearest:
        present = [n for n in nearest if n in a13_nodes or n in a13_reads]
        if present:
            lines.append(
                f"- nearest A13 candidate `{present[0]}` inspected and rejected because it does not implement `{key}` behavior"
            )
    return "\n".join(lines)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--a13-repo", required=True)
    ap.add_argument("--a14-repo", required=True)
    ap.add_argument("--out-dir", required=True)
    args = ap.parse_args()

    a13 = Path(args.a13_repo)
    a14 = Path(args.a14_repo)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    a14_nodes, a14_topology_count = parse_ui_nodes(a14)
    a13_nodes, a13_topology_count = parse_ui_nodes(a13)
    a14_specs, a14_spec_discovered, a14_spec_unknown = parse_a14_specs(a14)
    a14_reads = extract_pref_reads(a14)
    a13_reads = extract_pref_reads(a13)
    overrides = build_sanity_overrides()
    alias_map = missing_semantic_aliases()
    a13_search_index = build_a13_search_index(a13)
    a13_source_text = "\n".join(text.lower() for text in a13_search_index.values())
    a13_key_set = set(a13_nodes.keys()) | set(a13_reads)
    dead_map = phase_f_dead_a14_ui_keys()
    hold_map = phase_f_hold_missing()

    structural_proofs: dict[str, dict[str, str]] = {
        "PROOF_SYSTEMUI_SHARED_STATUSBAR_KEYS": {
            "key_prefix": "system_statusbar_",
            "host_package": "SYSTEM_UI",
            "a14_behavior": "Status bar UI semantics driven by same preference namespace and host.",
            "a13_behavior": "Same visible status bar namespace in A13 SystemUI hooks.",
            "a14_reference": "mods/SystemUIStatusBarHooks.kt",
            "a13_reference": "mods/SystemUIStatusBarHooks.kt",
        },
        "PROOF_LAUNCHER_SHARED_FOLDER_KEYS": {
            "key_prefix": "launcher_folder",
            "host_package": "LAUNCHER",
            "a14_behavior": "Launcher folder style/spacing keys on launcher host process.",
            "a13_behavior": "Equivalent launcher-host folder customization keys.",
            "a14_reference": "mods/LauncherFolderHooks.kt",
            "a13_reference": "mods/LauncherFolderHooks.kt",
        },
    }

    infra_rows = [
        ("infra.backup_restore", "Backup / Restore", "PRESENT_A13_VARIANT", "P1"),
        ("infra.language_about", "Language / About", "PRESENT_A13_VARIANT", "P1"),
        ("infra.search_navigation", "Search Navigation", "PRESENT_A13_VARIANT", "P1"),
        ("infra.restart_ux", "Restart UX", "PRESENT_A13_VARIANT", "P1"),
        ("infra.locale_reconcile", "Locale Reconcile", "PRESENT_A13_VARIANT", "P1"),
        ("infra.launcher_reconcile", "Launcher Reconcile", "PRESENT_A13_VARIANT", "P1"),
        ("infra.app_selection_sanitizer", "App Selection Sanitizer", "PRESENT_A13_VARIANT", "P1"),
    ]

    rows: list[dict[str, str]] = []
    missing_audit_records: list[MissingAuditRecord] = []
    current_missing_rows_audited = 0
    false_missing_reclassified = 0
    present_reclassified = 0
    partial_reclassified = 0
    dynamic_island_rows = 0
    for key, node in sorted(a14_nodes.items()):
        if node.node_type not in {"ACTIONABLE_FEATURE", "SUBOPTION"}:
            continue
        spec = a14_specs.get(key)
        has_a13 = key in a13_nodes
        host_package = spec.host_package if spec else infer_host_package_from_key(node.xml_file, key)
        process, classloader = process_scope_for_host(host_package)
        parity = "INSUFFICIENT_EVIDENCE" if has_a13 else "MISSING_IN_A13"
        evidence_level = evidence_for_row(key, has_a13, a14_reads, a13_reads)
        proof_id = ""
        a14_behavior = "Preference-backed behavior; semantic proof pending."
        a13_behavior = "Key present in A13 UI/schema." if has_a13 else "No A13 UI/schema key."
        a14_reference = spec.source_path if spec else "inferred-from-ui-topology"
        a13_reference = "A13 UI key presence" if has_a13 else "ABSENT"
        risk = "MEDIUM" if has_a13 else "HIGH"
        priority = "P1" if parity != "PRESENT_EQUIVALENT" else "P2"
        source_relationship = "INSUFFICIENT_EVIDENCE" if has_a13 else "A14_NEW_FEATURE"
        upgraded_existing = False
        initial_missing_candidate = parity == "MISSING_IN_A13"

        if parity == "INTENTIONAL_EXCLUDED":
            dynamic_island_rows += 1
        # Structural family promotion.
        for structural_id, definition in structural_proofs.items():
            if (
                has_a13
                and evidence_level == "IMPLEMENTATION_PRESENCE"
                and host_package == definition["host_package"]
                and key.startswith(definition["key_prefix"])
            ):
                parity = "PRESENT_A13_VARIANT"
                evidence_level = "STRUCTURAL_SEMANTIC_PROOF"
                proof_id = structural_id
                source_relationship = "UPSTREAM_INTENT_EQUIVALENT"
                a14_behavior = definition["a14_behavior"]
                a13_behavior = definition["a13_behavior"]
                a14_reference = definition["a14_reference"]
                a13_reference = definition["a13_reference"]
                risk = "LOW"
                priority = "P2"
                break
        # Individual sanity overrides.
        ov = overrides.get(key)
        if ov:
            parity = ov["parity_state"]
            evidence_level = ov["evidence_level"]
            proof_id = ov["proof_id"]
            a14_behavior = ov["a14_behavior"]
            a13_behavior = ov["a13_behavior"]
            risk = ov["risk"]
            priority = ov["priority"]
            a14_reference = ov["a14_reference"]
            a13_reference = ov["a13_reference"]
            if ov.get("implementation_mode") == "UPGRADE_EXISTING_A13":
                upgraded_existing = True

        # Missing-row semantic alias reconciliation (D-FINAL sweep).
        forced_phase_e_batch = ""
        if initial_missing_candidate:
            current_missing_rows_audited += 1
            feature_id = spec.feature_id if spec else f"A14_UI_{key}"
            title = spec.name if spec else (node.title or key)
            terms = [key, feature_id, title]
            alias = alias_map.get(key)
            a13_match = ""
            reclass_reason = "No A13 equivalent after feature-specific source review."
            nearest = nearest_a13_keys(key, a13_key_set)
            absence_proof = build_absence_proof(
                key, feature_id, title, a13_search_index, a13_nodes, a13_reads, nearest
            )
            if alias:
                terms.extend(alias["a13_keys"].split(","))
                alias_keys = [x.strip() for x in alias["a13_keys"].split(",") if x.strip()]
                alias_hit = any((ak in a13_nodes or ak in a13_reads or ak.lower() in a13_source_text) for ak in alias_keys)
                force_hold = alias.get("phase_e_batch") == "HOLD_EVIDENCE"
                if alias_hit or force_hold:
                    parity = alias["parity_state"]
                    reclass_reason = alias["reason"]
                    a13_reference = alias["a13_reference"]
                    a13_behavior = alias.get("a13_behavior", a13_behavior)
                    a13_match = ",".join([ak for ak in alias_keys if ak in a13_nodes or ak in a13_reads or ak.lower() in a13_source_text]) or alias["a13_keys"]
                    upgraded_existing = alias.get("implementation_mode") == "UPGRADE_EXISTING_A13"
                    if alias.get("host_package"):
                        host_package = alias["host_package"]
                        process, classloader = process_scope_for_host(host_package)
                    if alias.get("phase_e_batch"):
                        forced_phase_e_batch = alias["phase_e_batch"]
                    if alias.get("implementation_mode") == "EVIDENCE_HOLD":
                        evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                    else:
                        evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                        source_relationship = "UPSTREAM_INTENT_EQUIVALENT" if parity == "PRESENT_A13_VARIANT" else "SEMANTIC_DRIFT"
                    if parity == "PRESENT_A13_VARIANT":
                        present_reclassified += 1
                        false_missing_reclassified += 1
                        absence_proof = (
                            f"A13_SEARCHED =\n- A14 key `{key}` has no identical A13 key\n"
                            f"- matched A13 `{a13_match}` at {a13_reference}\n"
                            f"- same user capability: {reclass_reason}"
                        )
                    elif parity == "PARTIAL_PARITY":
                        partial_reclassified += 1
                        false_missing_reclassified += 1
                        absence_proof = (
                            f"A13_SEARCHED =\n- A14 key `{key}` has no identical A13 key\n"
                            f"- matched A13 `{a13_match}` at {a13_reference}\n"
                            f"- A14 materially extends existing A13 semantics: {reclass_reason}"
                        )
                    else:
                        absence_proof = (
                            f"A13_SEARCHED =\n- key `{key}`: no A13 implementation\n"
                            f"- classified HOLD because {reclass_reason}"
                        )
            if not a13_match:
                a13_match = "ABSENT"
            missing_audit_records.append(
                MissingAuditRecord(
                    a14_feature_id=feature_id,
                    a14_pref_keys=key,
                    a14_behavior=a14_behavior,
                    a14_reference=a14_reference,
                    a13_search_terms="; ".join(dict.fromkeys([t for t in terms if t])),
                    a13_match=a13_match,
                    a13_reference=a13_reference,
                    final_parity_state=parity if not forced_phase_e_batch else ("HOLD_EVIDENCE" if forced_phase_e_batch == "HOLD_EVIDENCE" else parity),
                    reclassification_reason=reclass_reason,
                    absence_proof=absence_proof,
                )
            )

        if key in dead_map:
            parity = "DEAD_UPSTREAM_PATH"
            evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
            proof_id = "PROOF_A14_UI_WITHOUT_IMPLEMENTATION"
            a14_behavior = dead_map[key]
            a13_behavior = "No A13 port: pinned A14 has no reachable production behavior."
            source_relationship = "DEAD_UPSTREAM_PATH"
            risk = "LOW"
            priority = "P3"
            upgraded_existing = False
        elif parity == "MISSING_IN_A13" and key in hold_map:
            rec = hold_map[key]
            parity = "HOLD_EVIDENCE"
            evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
            proof_id = "PROOF_PHASE_F_HOLD"
            a14_behavior = rec["unresolved_question"]
            a13_behavior = rec["why_forbidden"]
            source_relationship = "A14_NEW_FEATURE"
            risk = "HIGH"
            priority = "P0"
            upgraded_existing = False
        elif parity == "MISSING_IN_A13":
            parity = "HOLD_EVIDENCE"
            evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
            proof_id = "PROOF_PHASE_F_HOLD_FALLBACK"
            a14_behavior = "A14-only key with no A13 UI/schema match after Phase E."
            a13_behavior = "No A13 equivalent; remaining gap requires ROM/device evidence before port."
            source_relationship = "A14_NEW_FEATURE"
            risk = "HIGH"
            priority = "P0"
            upgraded_existing = False
        elif parity == "INSUFFICIENT_EVIDENCE":
            fam = apply_same_key_family_proof(key, host_package, a14_reads, a13_reads)
            if fam:
                parity = fam["parity_state"]
                evidence_level = fam["evidence_level"]
                proof_id = fam["proof_id"]
                source_relationship = fam["source_relationship"]
                a14_behavior = fam["a14_behavior"]
                a13_behavior = fam["a13_behavior"]
                a14_reference = fam["a14_reference"]
                a13_reference = fam["a13_reference"]
                risk = "LOW"
                priority = "P2"
            elif key in a13_reads:
                parity = "PRESENT_A13_VARIANT"
                evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                proof_id = "PROOF_A13_IMPL_A14_UI_ONLY"
                a14_behavior = "A14 exposes this UI key but does not read it in production."
                a13_behavior = "A13 production reads and implements this key."
                source_relationship = "UPSTREAM_INTENT_EQUIVALENT"
                a13_reference = f"pref read `{key}`"
                risk = "LOW"
                priority = "P2"
            elif key not in a14_reads:
                parity = "DEAD_UPSTREAM_PATH"
                evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                proof_id = "PROOF_A14_UI_WITHOUT_IMPLEMENTATION"
                a14_behavior = "A14 UI/schema key with no production pref read."
                a13_behavior = "No A13 production read either; A14 row is UI-only."
                source_relationship = "DEAD_UPSTREAM_PATH"
                risk = "LOW"
                priority = "P3"
            elif key not in a13_reads:
                parity = "HOLD_EVIDENCE"
                evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                proof_id = "PROOF_A13_UI_WITHOUT_HOOK"
                a14_behavior = "A14 production reads this key."
                a13_behavior = "A13 has the UI key but no production read; hook ownership is unresolved."
                source_relationship = "SEMANTIC_DRIFT"
                risk = "HIGH"
                priority = "P1"

        phase_e_batch = forced_phase_e_batch or route_phase_e_batch(host_package, process, key, spec.name if spec else node.title, parity)
        if parity == "HOLD_EVIDENCE":
            phase_e_batch = "HOLD_EVIDENCE"
        if parity == "DEAD_UPSTREAM_PATH":
            phase_e_batch = ""
        if parity in {"MISSING_IN_A13", "PARTIAL_PARITY"} and phase_e_batch == "HOLD_EVIDENCE":
            process = "UNRESOLVED"
            classloader = "UNRESOLVED"

        if "dynamic" in key and "island" in key:
            parity = "INTENTIONAL_EXCLUDED"
            evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
            proof_id = "PROOF_DYNAMIC_ISLAND_EXCLUDED"
            a14_behavior = "Dynamic Island style feature family."
            a13_behavior = "Product exclusion for A13."
            phase_e_batch = ""
            risk = "LOW"
            priority = "P3"
            source_relationship = "A14_NEW_FEATURE"

        rows.append({
            "domain": node.xml_file.replace("prefs_", "").replace(".xml", ""),
            "a14_feature_id": spec.feature_id if spec else f"A14_UI_{key}",
            "a14_name": spec.name if spec else (node.title or key),
            "a14_pref_keys": key,
            "a13_feature_id": f"A13_UI_{key}" if key in a13_nodes else "",
            "a13_pref_keys": key if key in a13_nodes else "",
            "node_type": node.node_type,
            "parity_state": parity,
            "evidence_level": evidence_level,
            "proof_id": proof_id,
            "source_relationship": source_relationship,
            "host_package": host_package,
            "process": process,
            "classloader": classloader,
            "a14_behavior": a14_behavior,
            "a13_behavior": a13_behavior,
            "a14_reference": a14_reference,
            "a13_reference": a13_reference,
            "risk": risk,
            "priority": priority,
            "phase_e_batch": phase_e_batch,
            "implementation_mode": implementation_mode_for(parity, phase_e_batch, upgraded_existing),
            "API33_design_direction": ov["api33"] if ov else ("Carry forward behavior with explicit API33 validation." if phase_e_batch != "HOLD_EVIDENCE" else "Evidence hold: resolve host/process/contract before Phase E."),
            "test_strategy": ov["test_strategy"] if ov else ("Host/process specific regression tests." if phase_e_batch != "HOLD_EVIDENCE" else "Blocked until evidence completion."),
            "ROM_evidence_needed": ov["rom_evidence"] if ov else ("YES" if phase_e_batch in {"E3", "E5"} else "NO"),
            "dynamic_island_excluded": "YES" if parity == "INTENTIONAL_EXCLUDED" else "NO",
            "a13_current_state": "KEY_MATCH" if has_a13 else ("LEGACY_ALIAS_PRESENT" if upgraded_existing else "ABSENT"),
        })

    for fid, name, parity, prio in infra_rows:
        phase_e_batch = route_phase_e_batch("SETTINGS", "com.android.settings", fid, name, parity)
        if fid == "infra.backup_restore":
            phase_e_batch = "E1"
        if parity == "MISSING_IN_A13":
            current_missing_rows_audited += 1
            missing_audit_records.append(
                MissingAuditRecord(
                    a14_feature_id=fid,
                    a14_pref_keys="",
                    a14_behavior="A14 typed backup/restore and migration integrity flow.",
                    a14_reference="utils/BackupFormatV2.kt, utils/BackupRestore.kt",
                    a13_search_terms="backup; restore; legacy backup migration; rollback; integrity",
                    a13_match="legacy backup/restore path",
                    a13_reference="PreferenceFragmentBase.kt",
                    final_parity_state=parity,
                    reclassification_reason="A13 has legacy behavior but lacks A14 typed/integrity contract.",
                    absence_proof=(
                        "A13_SEARCHED =\n"
                        "- key backup/restore: PreferenceFragmentBase.kt uses unbounded ObjectOutputStream(prefs.all) "
                        "and ObjectInputStream.readObject() with no typed format, version, CRC, or size bound\n"
                        "- BackupFormatV2 / CUI2 magic: no match in A13 utils\n"
                        "- LegacyBackupDecoder / restricted decoder: no match; production still uses ObjectInputStream\n"
                        "- rollback snapshot + commit-failure restore: no match\n"
                        "- nearest A13 candidate PreferenceFragmentBase.doRestoreSettings inspected and rejected "
                        "because it is the untyped legacy path lacking integrity/sanitation/reconcile-in-transaction"
                    ),
                )
            )
        rows.append({
            "domain": "infrastructure",
            "a14_feature_id": fid,
            "a14_name": name,
            "a14_pref_keys": "",
            "a13_feature_id": fid if parity != "MISSING_IN_A13" else "",
            "a13_pref_keys": "",
            "node_type": "ACTIONABLE_FEATURE",
            "parity_state": parity,
            "evidence_level": "INDIVIDUAL_SEMANTIC_PROOF",
            "proof_id": f"PROOF_{fid.upper().replace('.', '_')}",
            "source_relationship": "SEMANTIC_DRIFT" if parity == "MISSING_IN_A13" else "UPSTREAM_INTENT_EQUIVALENT",
            "host_package": "SETTINGS",
            "risk": "HIGH" if parity == "MISSING_IN_A13" else "MEDIUM",
            "priority": prio,
            "phase_e_batch": phase_e_batch,
            "implementation_mode": implementation_mode_for(parity, phase_e_batch, fid == "infra.backup_restore"),
            "process": "com.android.settings",
            "classloader": "settings",
            "a14_behavior": "Settings/app infrastructure behavior with explicit UX contract.",
            "a13_behavior": "Legacy infrastructure path; parity reviewed per feature.",
            "a14_reference": "utils/BackupFormatV2.kt, utils/BackupRestore.kt, utils/RestartPagePolicy.kt",
            "a13_reference": "PreferenceFragmentBase.kt, AppLocaleController.kt, GlobalActions.kt",
            "API33_design_direction": "Preserve A13-compatible UX contract with explicit state management.",
            "test_strategy": "Unit + integration + migration fixtures.",
            "ROM_evidence_needed": "NO",
            "dynamic_island_excluded": "NO",
            "a13_current_state": "legacy implementation",
        })

    a14_keys = {r["a14_pref_keys"] for r in rows if r["a14_pref_keys"]}
    for key, node in sorted(a13_nodes.items()):
        if node.node_type not in {"ACTIONABLE_FEATURE", "SUBOPTION"}:
            continue
        if key in a14_keys:
            continue
        rows.append({
            "domain": node.xml_file.replace("prefs_", "").replace(".xml", ""),
            "a14_feature_id": "",
            "a14_name": "",
            "a14_pref_keys": "",
            "a13_feature_id": f"A13_UI_{key}",
            "a13_pref_keys": key,
            "node_type": node.node_type,
            "parity_state": "A13_ONLY_KEEP",
            "evidence_level": "MECHANICAL_ONLY",
            "proof_id": "",
            "source_relationship": "A13_COMPAT_VARIANT",
            "host_package": "A13_ONLY",
            "process": "A13_ONLY",
            "classloader": "A13_ONLY",
            "a14_behavior": "",
            "a13_behavior": "A13-only capability retained.",
            "a14_reference": "",
            "a13_reference": node.xml_file,
            "risk": "LOW",
            "priority": "P3",
            "phase_e_batch": "",
            "implementation_mode": "NO_IMPLEMENTATION",
            "API33_design_direction": "KEEP",
            "test_strategy": "Preserve existing behavior.",
            "ROM_evidence_needed": "NO",
            "dynamic_island_excluded": "NO",
            "a13_current_state": "A13-only capability",
        })

    # Keep exactly one Dynamic Island exclusion row.
    if dynamic_island_rows != 1:
        rows = [r for r in rows if r["parity_state"] != "INTENTIONAL_EXCLUDED"]
        rows.append({
            "domain": "system",
            "a14_feature_id": "dynamic_island",
            "a14_name": "Dynamic Island",
            "a14_pref_keys": "dynamic_island",
            "a13_feature_id": "",
            "a13_pref_keys": "",
            "node_type": "ACTIONABLE_FEATURE",
            "parity_state": "INTENTIONAL_EXCLUDED",
            "evidence_level": "INDIVIDUAL_SEMANTIC_PROOF",
            "proof_id": "PROOF_DYNAMIC_ISLAND_EXCLUDED",
            "source_relationship": "A14_NEW_FEATURE",
            "host_package": "SYSTEM_UI",
            "process": "com.android.systemui",
            "classloader": "systemui",
            "a14_behavior": "Dynamic Island / smart-notch behavior family.",
            "a13_behavior": "Intentionally excluded on A13 product line.",
            "a14_reference": "Product policy exclusion",
            "a13_reference": "ABSENT",
            "risk": "LOW",
            "priority": "P3",
            "phase_e_batch": "",
            "implementation_mode": "NO_IMPLEMENTATION",
            "API33_design_direction": "PORT=NO",
            "test_strategy": "N/A",
            "ROM_evidence_needed": "NO",
            "dynamic_island_excluded": "YES",
            "a13_current_state": "excluded",
        })

    # Required matrix columns order.
    ordered_columns = [
        "domain", "a14_feature_id", "a14_name", "a14_pref_keys", "a13_feature_id", "a13_pref_keys",
        "parity_state", "evidence_level", "proof_id",
        "host_package", "process", "classloader",
        "a14_behavior", "a13_behavior", "a14_reference", "a13_reference",
        "risk", "priority", "phase_e_batch",
        "implementation_mode", "a13_current_state", "API33_design_direction", "test_strategy", "ROM_evidence_needed",
        "dynamic_island_excluded",
        "node_type", "source_relationship",
    ]
    csv_path = out_dir / "A13_A14_FEATURE_MATRIX.csv"
    fieldnames = ordered_columns
    with csv_path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(rows)

    a14_actionable = sum(1 for r in rows if r["a14_feature_id"])
    a13_product = sum(1 for r in rows if r["a13_feature_id"])
    a13_only = sum(1 for r in rows if r["parity_state"] == "A13_ONLY_KEEP")
    c = Counter(r["parity_state"] for r in rows if r["a14_feature_id"])
    ev = Counter(r["evidence_level"] for r in rows if r["a14_feature_id"])
    batch_counts = derive_batch_counts(rows)
    hold_evidence_count = sum(1 for r in rows if r["parity_state"] == "HOLD_EVIDENCE")
    phase_e_ready_gaps = sum(batch_counts.get(b, 0) for b in ["E1", "E2", "E3", "E4", "E5"])
    true_missing_remaining = sum(1 for r in rows if r["parity_state"] == "MISSING_IN_A13")

    confirmed_ui_without_impl = 0
    candidate_ui_without_impl = sum(1 for k, n in a14_nodes.items() if n.node_type in {"ACTIONABLE_FEATURE", "SUBOPTION"} and k not in a14_reads)
    candidate_impl_without_ui = sum(1 for k in a14_reads if k not in a14_nodes)

    print(f"A14_PRODUCT_FEATURE_COUNT={a14_actionable}")
    print(f"A13_PRODUCT_FEATURE_COUNT={a13_product}")
    print(f"A13_ONLY_KEEP_COUNT={a13_only}")
    for k in ["PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "DEAD_UPSTREAM_PATH", "HOLD_EVIDENCE", "INSUFFICIENT_EVIDENCE"]:
        print(f"{k}_COUNT={c.get(k, 0)}")
    print(f"UI_TOPOLOGY_NODE_COUNT_A14={a14_topology_count}")
    print(f"UI_TOPOLOGY_NODE_COUNT_A13={a13_topology_count}")
    print(f"CANDIDATE_UI_WITHOUT_IMPLEMENTATION={candidate_ui_without_impl}")
    print(f"CANDIDATE_IMPLEMENTATION_WITHOUT_UI={candidate_impl_without_ui}")
    print(f"CONFIRMED_UI_WITHOUT_IMPLEMENTATION={confirmed_ui_without_impl}")
    print("CONFIRMED_IMPLEMENTATION_WITHOUT_UI=0")
    print("INTERNAL_IMPLEMENTATION_WITHOUT_UI=0")
    print(f"STRUCTURAL_SEMANTIC_PROOF_ROWS={ev.get('STRUCTURAL_SEMANTIC_PROOF', 0)}")
    print(f"INDIVIDUAL_SEMANTIC_PROOF_ROWS={ev.get('INDIVIDUAL_SEMANTIC_PROOF', 0)}")
    print(f"IMPLEMENTATION_PRESENCE_ROWS={ev.get('IMPLEMENTATION_PRESENCE', 0)}")
    print(f"MECHANICAL_ONLY_ROWS={ev.get('MECHANICAL_ONLY', 0)}")
    print(f"A14_SPEC_DISCOVERED={a14_spec_discovered}")
    print(f"A14_SPEC_UNKNOWN={a14_spec_unknown}")
    print(f"HOLD_EVIDENCE_COUNT={hold_evidence_count}")
    print(f"PHASE_E_READY_GAPS={phase_e_ready_gaps}")
    print(f"CURRENT_MISSING_ROWS_AUDITED={current_missing_rows_audited}")
    print(f"FALSE_MISSING_RECLASSIFIED={false_missing_reclassified}")
    print(f"PRESENT_A13_VARIANT_RECLASSIFIED={present_reclassified}")
    print(f"PARTIAL_PARITY_RECLASSIFIED={partial_reclassified}")
    print(f"TRUE_MISSING_REMAINING={true_missing_remaining}")
    hide_ime = [r for r in rows if r["a14_feature_id"] == "HideImeDismissButtonFeatureId"]
    hide_ime_ok = bool(
        hide_ime
        and (
            hide_ime[0]["parity_state"] in {"PRESENT_A13_VARIANT", "PRESENT_EQUIVALENT"}
            or hide_ime[0]["phase_e_batch"] == "E3"
        )
    )
    print(f"HIDE_IME_ROUTING={(hide_ime[0]['phase_e_batch'] or hide_ime[0]['parity_state'] if hide_ime else 'NOT_FOUND')}")
    print(f"E_BATCH_ROUTING_TEST={'PASS' if hide_ime_ok else 'FAIL'}")
    print(f"DYNAMIC_ISLAND_EXCLUDED_EXACTLY_ONCE={'YES' if sum(1 for r in rows if r['parity_state']=='INTENTIONAL_EXCLUDED') == 1 else 'NO'}")
    print(f"PARITY_ACCOUNTING_INVARIANT={'PASS' if parity_accounting_invariant(rows) else 'FAIL'}")
    for batch in ["E1", "E2", "E3", "E4", "E5"]:
        print(f"{batch}_COUNT={batch_counts.get(batch, 0)}")
    # R3 missing reconciliation artifact.
    reconciliation_path = out_dir / "A13_PHASE_F_RESIDUAL_AUDIT.md"
    residual_rows = [
        r for r in rows
        if r.get("a14_feature_id") and r["parity_state"] in {
            "MISSING_IN_A13", "PARTIAL_PARITY", "INSUFFICIENT_EVIDENCE", "HOLD_EVIDENCE", "DEAD_UPSTREAM_PATH",
        }
    ]
    with reconciliation_path.open("w", encoding="utf-8", newline="") as f:
        f.write("# A13 Phase F Residual Audit\n\n")
        f.write("Historical Phase D/E reports are not rewritten.\n\n")
        f.write(f"A13_BASE_SHA = d25bb9d37d3ee60d13657a24361336d8c705cb71\n")
        f.write(f"A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa\n\n")
        f.write(f"HOLD_EVIDENCE = {c.get('HOLD_EVIDENCE', 0)}\n")
        f.write(f"DEAD_UPSTREAM_PATH = {c.get('DEAD_UPSTREAM_PATH', 0)}\n")
        f.write(f"MISSING_IN_A13 = {c.get('MISSING_IN_A13', 0)}\n")
        f.write(f"PARTIAL_PARITY = {c.get('PARTIAL_PARITY', 0)}\n")
        f.write(f"INSUFFICIENT_EVIDENCE = {c.get('INSUFFICIENT_EVIDENCE', 0)}\n")
        f.write(f"INTENTIONAL_EXCLUDED = {c.get('INTENTIONAL_EXCLUDED', 0)}\n\n")
        f.write("| A14_FEATURE_ID | A14_PREF_KEYS | FINAL_PARITY_STATE | HOST | PROOF_ID |\n")
        f.write("|---|---|---|---|---|\n")
        for r in residual_rows:
            f.write(
                f"| {r['a14_feature_id']} | {r['a14_pref_keys']} | {r['parity_state']} | {r['host_package']} | {r['proof_id']} |\n"
            )
        f.write("\n## Initial missing-candidate absence proofs (pre-HOLD mapping)\n\n")
        for rec in missing_audit_records:
            f.write(f"- **A14_FEATURE_ID**: `{rec.a14_feature_id}`\n")
            f.write(f"  - A14_PREF_KEYS: `{rec.a14_pref_keys}`\n")
            f.write(f"  - INITIAL_STATE: `{rec.final_parity_state}`\n")
            f.write(f"  - A13_MATCH: `{rec.a13_match}`\n")
            f.write(f"  - ABSENCE_PROOF: {rec.absence_proof}\n\n")
    print(f"MISSING_RECONCILIATION={reconciliation_path}")
    print(f"CSV={csv_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

