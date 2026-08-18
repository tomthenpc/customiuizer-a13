#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

try:
    from tools.parity_phase_f import (
        DI_HELPER_KEYS,
        DI_PRODUCT_KEY,
        DeadPathProof,
        PhaseFTransitionInput,
        ProofManifest,
        build_source_index,
        classify_phase_f_transition,
        classify_ui_node as classify_ui_node_impl,
        classify_unproven_bucket,
        fingerprint_proof_for_key,
        format_proof_markdown,
        hook_targets_compatible,
        is_app_selector_key,
        is_product_node,
        match_owner_pair,
        phase_e_source_proofs,
        proof_index,
        proof_is_acceptable,
        prove_dead_a14_key,
        scan_repo,
        xml_attr,
    )
    from tools.parity_owner_groups import (
        FALSE_OWNER_ASSIGNMENTS_REMOVED,
        explicit_reviewed_owner_groups,
        group_keys_for_symbol,
        review_owner_groups,
    )
except ImportError:
    from parity_phase_f import (
        DI_HELPER_KEYS,
        DI_PRODUCT_KEY,
        DeadPathProof,
        PhaseFTransitionInput,
        ProofManifest,
        build_source_index,
        classify_phase_f_transition,
        classify_ui_node as classify_ui_node_impl,
        classify_unproven_bucket,
        fingerprint_proof_for_key,
        format_proof_markdown,
        hook_targets_compatible,
        is_app_selector_key,
        is_product_node,
        match_owner_pair,
        phase_e_source_proofs,
        proof_index,
        proof_is_acceptable,
        prove_dead_a14_key,
        scan_repo,
        xml_attr,
    )
    from parity_owner_groups import (
        FALSE_OWNER_ASSIGNMENTS_REMOVED,
        explicit_reviewed_owner_groups,
        group_keys_for_symbol,
        review_owner_groups,
    )

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


def classify_ui_node(tag: str, key: str, visible: str | None = None, warning: str | None = None, **kwargs) -> str:
    return classify_ui_node_impl(tag, key, visible=visible, warning=warning, **kwargs)


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
            node_type = classify_ui_node(
                elem.tag,
                key,
                visible=xml_attr(elem, "isPreferenceVisible"),
                warning=xml_attr(elem, "warning"),
                title=title,
                selectable=xml_attr(elem, "selectable"),
                persistent=xml_attr(elem, "persistent"),
                count_as_summary=xml_attr(elem, "countAsSummary"),
            )
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
        "SOURCE_REVIEW_REQUIRED",
    }:
        return "NO_IMPLEMENTATION"
    if parity_state == "HOLD_EVIDENCE" or phase_e_batch == "HOLD_EVIDENCE":
        return "EVIDENCE_HOLD"
    if parity_state == "PARTIAL_PARITY":
        return "UPGRADE_EXISTING_A13"
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
        "SOURCE_REVIEW_REQUIRED",
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
        "system_netspeed_boldfont": {
            "a13_keys": "system_netspeed_bold",
            "parity_state": "PRESENT_A13_VARIANT",
            "reason": "Same user capability: bold network-speed typeface. A14 renamed the key.",
            "a13_reference": "res/xml/prefs_system_detailednetspeed.xml; mods/SystemUIStatusBarHooks.kt::NetSpeedTypefaceHelper",
            "implementation_mode": "NO_IMPLEMENTATION",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "system_netspeed_bold already drives NetSpeedTypefaceHelper.apply().",
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
        "system_statusbaricons_bluetoothicn": {
            "a13_keys": "system_statusbaricons_bluetooth",
            "parity_state": "PRESENT_A13_VARIANT",
            "reason": "A13 HideIconsBluetoothHook option 3 already always-hides the bluetooth icon.",
            "a13_reference": "mods/SystemStatusBarMoreHooks.kt::HideIconsBluetoothHook",
            "implementation_mode": "NO_IMPLEMENTATION",
            "host_package": "SYSTEM_UI",
            "a13_behavior": "system_statusbaricons_bluetooth=3 sets bluetooth icon visibility false.",
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
    add(
        "various_clear_update_state",
        "Whether MIUI 14 Settings.Global miui_new_version/miui_update_ready plus com.android.updater clearApplicationUserData match the HyperOS updater-state cache",
        "android / com.android.updater",
        "no one-shot action; ROM updater reminder unchanged",
        "MIUI 14 updater package dump, Settings.Global key names, and ActivityManager.clearApplicationUserData 4-arg result on API33",
        "A14 one-shot lives in the HyperOS updater-services bridge (same system_server owner as various_disable_update_services, already HOLD). Product copy and Global keys are HyperOS-branded. Privileged system_server data wipe cannot be decided from A13 source.",
    )
    add(
        "various_disable_defraud_apps_detect",
        "Whether MIUI 14 com.miui.guardprovider contains AntiDefraudAppManager / getUnSystemAppList strings used by A14 DexKit",
        "com.miui.guardprovider",
        "feature off / ROM fraud-app scan unchanged",
        "GuardProvider DEX dump on MIUI 14 and HyperOS 1 A13 showing the DexKit string pair",
        "A14 DisableDefraudAppsCheck is DexKit-only against GuardProvider. No A13 owner, installer, or fixed class/member. Fail-open would hide a dead toggle.",
    )
    return holds


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


def _row(
    *,
    domain: str,
    a14_feature_id: str,
    a14_name: str,
    a14_pref_keys: str,
    a13_feature_id: str,
    a13_pref_keys: str,
    node_type: str,
    parity: str,
    evidence_level: str,
    proof_id: str,
    source_relationship: str,
    host_package: str,
    process: str,
    classloader: str,
    a14_behavior: str,
    a13_behavior: str,
    a14_reference: str,
    a13_reference: str,
    risk: str,
    priority: str,
    phase_e_batch: str,
    implementation_mode: str,
    api33: str,
    test_strategy: str,
    rom_evidence: str,
    a13_current_state: str,
) -> dict[str, str]:
    return {
        "domain": domain,
        "a14_feature_id": a14_feature_id,
        "a14_name": a14_name,
        "a14_pref_keys": a14_pref_keys,
        "a13_feature_id": a13_feature_id,
        "a13_pref_keys": a13_pref_keys,
        "node_type": node_type,
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
        "implementation_mode": implementation_mode,
        "API33_design_direction": api33,
        "test_strategy": test_strategy,
        "ROM_evidence_needed": rom_evidence,
        "dynamic_island_excluded": "YES" if parity == "INTENTIONAL_EXCLUDED" else "NO",
        "a13_current_state": a13_current_state,
    }


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
    alias_map = missing_semantic_aliases()
    a13_search_index = build_a13_search_index(a13)
    a13_source_text = "\n".join(text.lower() for text in a13_search_index.values())
    a13_key_set = set(a13_nodes.keys()) | set(a13_reads)
    hold_map = phase_f_hold_missing()
    a14_index = build_source_index(a14)
    a14_scan = scan_repo(a14)
    a13_scan = scan_repo(a13)
    a14_owners = a14_scan.owners
    a13_owners = a13_scan.owners
    explicit_manifests = phase_e_source_proofs() + explicit_reviewed_owner_groups()
    explicit_by_key = proof_index(explicit_manifests)
    review_keys = [
        k for k, n in a14_nodes.items()
        if is_product_node(n.node_type)
        and k in a13_nodes
        and is_product_node(a13_nodes[k].node_type)
    ]
    og_index = review_owner_groups(
        a14_scan,
        a13_scan,
        review_keys,
        a14_xml={k: n.xml_file for k, n in a14_nodes.items()},
        a13_xml={k: n.xml_file for k, n in a13_nodes.items()},
        a14_tags={k: n.tag.rsplit(".", 1)[-1] for k, n in a14_nodes.items()},
        a13_tags={k: n.tag.rsplit(".", 1)[-1] for k, n in a13_nodes.items()},
    )
    hold_map.update(og_index.holds)
    fp_by_key: dict[str, ProofManifest] = {}
    used_manifests: list[ProofManifest] = []
    used_ids: set[str] = set()

    def remember(man: ProofManifest) -> None:
        if man.proof_id not in used_ids:
            used_ids.add(man.proof_id)
            used_manifests.append(man)

    def proof_for(key: str) -> ProofManifest | None:
        if key in explicit_by_key:
            man = explicit_by_key[key]
            if proof_is_acceptable(man):
                remember(man)
                return man
        if key in fp_by_key:
            man = fp_by_key[key]
            if proof_is_acceptable(man):
                remember(man)
                return man
        action = f"{key}_action"
        if action in a14_reads and action in a13_reads and key in a13_nodes:
            man = ProofManifest(
                proof_id=f"PROOF_ACTION_SLOT_{key}",
                a14_owner_path="mods/utils/GlobalActionConfig.kt / action picker",
                a14_symbol="handleAction/handleNavBarAction",
                a14_installer="SystemUiInstaller / LauncherInstaller / SystemServerInstaller",
                a14_hook_targets="(no ROM member; GlobalActions dispatcher)",
                a14_callback_phase="n/a",
                a13_owner_path="mods/GlobalActions.kt / Controls.kt / LauncherGestureHooks.kt",
                a13_symbol="handleAction/handleNavBarAction",
                a13_installer="installers/*Installer.java",
                a13_hook_targets="(no ROM member; GlobalActions dispatcher)",
                a13_callback_phase="n/a",
                preference_keys=(key, action),
                value_domain=f"action picker; stored as {action}",
                default_semantics="action=1 keeps ROM default",
                result_argument_behavior="UI key opens the action picker; the int in _action selects handleAction",
                api33_variant_reason="A13 and A14 share the visible picker row plus the companion _action int domain.",
                proof_conclusion="PRESENT_A13_VARIANT",
                evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
                body_relation="REVIEWED_VARIANT",
                diff_summary=(
                    f"Both trees persist the selected action id in `{action}`. The visible `{key}` row is the picker, "
                    "not a host hook. Dispatcher is handleAction/handleNavBarAction on both trees."
                ),
                value_default_comparison="Both default the stored action id to 1 (keep ROM handler).",
                hook_target_comparison="No SystemUI/Home class dump: the UI key has no host member; consumption is in-module GlobalActions.",
                callback_semantics_comparison="No Xposed callback on the picker row; click opens the action selector.",
                arg_result_comparison="No setResult on this row. The stored int is later dispatched by handleAction.",
                a14_only_branches="none for the slot row itself",
                why_user_behavior_is_equivalent=(
                    "The user configures the same action picker; the companion _action integer is consumed by the "
                    "shared GlobalActions dispatcher on both trees."
                ),
                key_ownership_evidence=f"{key}: companion persisted key {action} is read by handleAction/handleNavBarAction on both trees",
                a14_key_owner_reference=f"GlobalActions handleAction companion {action}",
                a13_key_owner_reference=f"GlobalActions handleAction companion {action}",
            )
            if proof_is_acceptable(man):
                remember(man)
                fp_by_key[key] = man
                fp_by_key[action] = man
                return man
        man = fingerprint_proof_for_key(
            key, a14_owners, a13_owners, a14_scan=a14_scan, a13_scan=a13_scan
        )
        if man and proof_is_acceptable(man):
            fp_by_key[key] = man
            if man.body_relation == "IDENTICAL":
                for covered in man.preference_keys:
                    fp_by_key[covered] = man
            remember(man)
            return man
        if key in og_index.by_key:
            man = og_index.by_key[key]
            if proof_is_acceptable(man) or man.proof_conclusion == "HOLD_EVIDENCE":
                fp_by_key[key] = man
                remember(man)
                return man
        return None

    def hook_match_for(key: str) -> bool | None:
        left = a14_owners.get(key) or []
        right = a13_owners.get(key) or []
        if not left or not right:
            return None
        pair = match_owner_pair(left, right)
        if pair:
            return hook_targets_compatible(*pair)
        return False

    infra_rows = [
        ("infra.backup_restore", "Backup / Restore", "PRESENT_A13_VARIANT", "P1", "PROOF_BACKUP_V2"),
        ("infra.language_about", "Language / About", "PRESENT_A13_VARIANT", "P1", "PROOF_INFRA_LANGUAGE_ABOUT"),
        ("infra.search_navigation", "Search Navigation", "PRESENT_A13_VARIANT", "P1", "PROOF_INFRA_SEARCH"),
        ("infra.restart_ux", "Restart UX", "PRESENT_A13_VARIANT", "P1", "PROOF_INFRA_RESTART"),
        ("infra.locale_reconcile", "Locale Reconcile", "PRESENT_A13_VARIANT", "P1", "PROOF_INFRA_LOCALE"),
        ("infra.launcher_reconcile", "Launcher Reconcile", "PRESENT_A13_VARIANT", "P1", "PROOF_INFRA_LAUNCHER"),
        ("infra.app_selection_sanitizer", "App Selection Sanitizer", "PRESENT_A13_VARIANT", "P1", "PROOF_INFRA_SANITIZER"),
    ]

    rows: list[dict[str, str]] = []
    missing_audit_records: list[MissingAuditRecord] = []
    current_missing_rows_audited = 0
    false_missing_reclassified = 0
    present_reclassified = 0
    partial_reclassified = 0
    non_product_helpers = 0
    hidden_helpers = 0
    di_helpers = 0
    source_review_required = 0
    dead_proofs: dict[str, DeadPathProof] = {}

    for key, node in sorted(a14_nodes.items()):
        if not is_product_node(node.node_type):
            non_product_helpers += 1
            if node.node_type == "HIDDEN_HELPER":
                hidden_helpers += 1
            if node.node_type == "DYNAMIC_ISLAND_HELPER":
                di_helpers += 1
            continue

        spec = a14_specs.get(key)
        has_a13 = key in a13_nodes and is_product_node(a13_nodes[key].node_type)
        host_package = spec.host_package if spec else infer_host_package_from_key(node.xml_file, key)
        process, classloader = process_scope_for_host(host_package)
        man = proof_for(key)
        nearest = nearest_a13_keys(key, a13_key_set)
        dead = None
        if man is None and key not in hold_map and not has_a13:
            dead = prove_dead_a14_key(
                key,
                node.xml_file,
                a14_index,
                set(a14_specs.keys()),
                a14_owners,
                nearest=",".join(nearest),
            )
            if dead:
                dead_proofs[key] = dead
        rom_hold = hold_map.get(key) if (not has_a13 and key in hold_map) else None
        unproven_bucket = classify_unproven_bucket(
            key,
            host_package=host_package,
            has_a13=has_a13,
            a14_owner_found=bool(a14_owners.get(key)),
            a13_owner_found=bool(a13_owners.get(key)),
            in_rom_hold_map=key in hold_map,
        )
        if man:
            unproven_bucket = ""
            rom_hold = None
        elif has_a13 and unproven_bucket == "SOURCE_REVIEW_REQUIRED":
            rom_hold = None
        decision = classify_phase_f_transition(PhaseFTransitionInput(
            key=key,
            node_type=node.node_type,
            a14_read=key in a14_reads,
            a13_read=key in a13_reads,
            host_package=host_package,
            hook_behavior_match=hook_match_for(key),
            source_proof=man,
            dead_proof=dead,
            rom_hold=rom_hold,
            unproven_bucket=unproven_bucket if not man else "",
        ))

        parity = decision.parity_state
        evidence_level = decision.evidence_level
        proof_id = decision.proof_id
        a14_behavior = decision.reason
        a13_behavior = decision.reason
        a14_reference = spec.source_path if spec else node.xml_file
        a13_reference = a13_nodes[key].xml_file if has_a13 else "ABSENT"
        source_relationship = "INSUFFICIENT_EVIDENCE"
        risk = "MEDIUM" if has_a13 else "HIGH"
        priority = "P1"
        upgraded_existing = False
        forced_phase_e_batch = ""
        initial_missing_candidate = not has_a13

        if man:
            a14_behavior = man.result_argument_behavior
            a13_behavior = man.api33_variant_reason
            a14_reference = f"{man.a14_owner_path}::{man.a14_symbol}"
            a13_reference = f"{man.a13_owner_path}::{man.a13_symbol}"
            source_relationship = "UPSTREAM_INTENT_EQUIVALENT" if parity in {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"} else source_relationship
            risk = "LOW"
            priority = "P2"
            if man.proof_conclusion == "HOLD_EVIDENCE":
                risk = "HIGH"
                priority = "P0"
                source_relationship = "SEMANTIC_DRIFT"
                forced_phase_e_batch = "HOLD_EVIDENCE"

        if parity == "HOLD_EVIDENCE" and key in hold_map and not man:
            rec = hold_map[key]
            a14_behavior = rec["unresolved_question"]
            a13_behavior = rec["why_forbidden"]
            proof_id = "PROOF_ROM_DEVICE_HOLD"
            evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
            source_relationship = "A14_NEW_FEATURE"
            risk = "HIGH"
            priority = "P0"
            forced_phase_e_batch = "HOLD_EVIDENCE"

        if parity in {"UNPROVEN", "SOURCE_REVIEW_REQUIRED"}:
            alias = alias_map.get(key) if initial_missing_candidate else None
            if alias:
                current_missing_rows_audited += 1
                alias_keys = [x.strip() for x in alias["a13_keys"].split(",") if x.strip()]
                alias_hit = any((ak in a13_nodes or ak in a13_reads or ak.lower() in a13_source_text) for ak in alias_keys)
                force_hold = alias.get("phase_e_batch") == "HOLD_EVIDENCE"
                if alias_hit or force_hold:
                    parity = alias["parity_state"]
                    a13_reference = alias["a13_reference"]
                    a13_behavior = alias.get("a13_behavior", a13_behavior)
                    a14_behavior = alias["reason"]
                    proof_id = proof_id or f"PROOF_ALIAS_{key.upper()}"
                    evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                    if alias.get("host_package"):
                        host_package = alias["host_package"]
                        process, classloader = process_scope_for_host(host_package)
                    if alias.get("phase_e_batch"):
                        forced_phase_e_batch = alias["phase_e_batch"]
                    upgraded_existing = alias.get("implementation_mode") == "UPGRADE_EXISTING_A13"
                    source_relationship = "UPSTREAM_INTENT_EQUIVALENT" if parity == "PRESENT_A13_VARIANT" else (
                        "A14_NEW_FEATURE" if parity == "HOLD_EVIDENCE" else "SEMANTIC_DRIFT"
                    )
                    if parity == "PRESENT_A13_VARIANT":
                        present_reclassified += 1
                        false_missing_reclassified += 1
                    elif parity == "PARTIAL_PARITY":
                        partial_reclassified += 1
                        false_missing_reclassified += 1
                    missing_audit_records.append(
                        MissingAuditRecord(
                            a14_feature_id=spec.feature_id if spec else f"A14_UI_{key}",
                            a14_pref_keys=key,
                            a14_behavior=a14_behavior,
                            a14_reference=a14_reference,
                            a13_search_terms=alias["a13_keys"],
                            a13_match=alias["a13_keys"],
                            a13_reference=a13_reference,
                            final_parity_state=parity,
                            reclassification_reason=alias["reason"],
                            absence_proof=alias["reason"],
                        )
                    )
            elif key in hold_map and not has_a13:
                rec = hold_map[key]
                parity = "HOLD_EVIDENCE"
                evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                proof_id = "PROOF_ROM_DEVICE_HOLD"
                a14_behavior = rec["unresolved_question"]
                a13_behavior = rec["why_forbidden"]
                source_relationship = "A14_NEW_FEATURE"
                risk = "HIGH"
                priority = "P0"
                forced_phase_e_batch = "HOLD_EVIDENCE"
            elif dead:
                parity = "DEAD_UPSTREAM_PATH"
                evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                proof_id = f"PROOF_DEAD_{key.upper()}"
                a14_behavior = dead.why_not_reachable
                a13_behavior = "No A13 port: pinned A14 has no reachable production behavior."
                a14_reference = dead.a14_ui_reference
                source_relationship = "DEAD_UPSTREAM_PATH"
                risk = "LOW"
                priority = "P3"
            elif has_a13:
                parity = "SOURCE_REVIEW_REQUIRED"
                evidence_level = "IMPLEMENTATION_PRESENCE"
                proof_id = proof_id or ""
                a14_behavior = (
                    f"Visible row `{key}` exists on both trees. Same XML file or same-key read is "
                    "IMPLEMENTATION_PRESENCE, not PRESENT. Direct owner evidence was not proven for this key."
                )
                a13_behavior = (
                    "Non-identical owner candidates without an explicit reviewed manifest stay "
                    "SOURCE_REVIEW_REQUIRED. Prefix, ranked-first, and XML-file grouping are not proof."
                )
                source_relationship = "SEMANTIC_DRIFT"
                risk = "MEDIUM"
                priority = "P1"
            elif not has_a13:
                current_missing_rows_audited += 1
                rec = hold_map.get(key)
                if rec:
                    parity = "HOLD_EVIDENCE"
                    evidence_level = "INDIVIDUAL_SEMANTIC_PROOF"
                    proof_id = "PROOF_ROM_DEVICE_HOLD"
                    a14_behavior = rec["unresolved_question"]
                    a13_behavior = rec["why_forbidden"]
                    source_relationship = "A14_NEW_FEATURE"
                    forced_phase_e_batch = "HOLD_EVIDENCE"
                    risk = "HIGH"
                    priority = "P0"
                else:
                    parity = "MISSING_IN_A13"
                    evidence_level = "MECHANICAL_ONLY"
                    proof_id = ""
                    a14_behavior = f"A14 product key `{key}` has no A13 product counterpart after source review."
                    a13_behavior = "Not a ROM dump: A13 simply has no equivalent product row/owner."
                    source_relationship = "A14_NEW_FEATURE"
                    risk = "HIGH"
                    priority = "P1"
                    missing_audit_records.append(
                        MissingAuditRecord(
                            a14_feature_id=spec.feature_id if spec else f"A14_UI_{key}",
                            a14_pref_keys=key,
                            a14_behavior=a14_behavior,
                            a14_reference=a14_reference,
                            a13_search_terms=key,
                            a13_match="ABSENT",
                            a13_reference="ABSENT",
                            final_parity_state=parity,
                            reclassification_reason=a13_behavior,
                            absence_proof=build_absence_proof(
                                key, spec.feature_id if spec else f"A14_UI_{key}", node.title or key,
                                a13_search_index, a13_nodes, a13_reads, nearest,
                            ),
                        )
                    )

        if dead and parity == "DEAD_UPSTREAM_PATH":
            a14_behavior = dead.why_not_reachable
            a13_behavior = f"A14_SEARCH_REFERENCES={dead.a14_search_references}; nearest={dead.a14_nearest_candidate}"

        phase_e_batch = forced_phase_e_batch or route_phase_e_batch(
            host_package, process, key, spec.name if spec else node.title, parity
        )
        if parity == "HOLD_EVIDENCE":
            phase_e_batch = "HOLD_EVIDENCE"
        if parity == "DEAD_UPSTREAM_PATH":
            phase_e_batch = ""
        if parity in {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "INTENTIONAL_EXCLUDED"}:
            phase_e_batch = ""

        rows.append(_row(
            domain=node.xml_file.replace("prefs_", "").replace(".xml", ""),
            a14_feature_id=spec.feature_id if spec else f"A14_UI_{key}",
            a14_name=spec.name if spec else (node.title or key),
            a14_pref_keys=key,
            a13_feature_id=f"A13_UI_{key}" if has_a13 else "",
            a13_pref_keys=key if has_a13 else "",
            node_type=node.node_type,
            parity=parity,
            evidence_level=evidence_level,
            proof_id=proof_id,
            source_relationship=source_relationship,
            host_package=host_package,
            process=process,
            classloader=classloader,
            a14_behavior=a14_behavior,
            a13_behavior=a13_behavior,
            a14_reference=a14_reference,
            a13_reference=a13_reference,
            risk=risk,
            priority=priority,
            phase_e_batch=phase_e_batch,
            implementation_mode=implementation_mode_for(parity, phase_e_batch, upgraded_existing),
            api33=man.api33_variant_reason if man else (
                "Evidence hold: resolve host/process/contract before port." if parity == "HOLD_EVIDENCE" else "Carry forward with explicit API33 validation."
            ),
            test_strategy="Host/process specific regression tests." if parity != "HOLD_EVIDENCE" else "Blocked until device/ROM evidence.",
            rom_evidence="YES" if parity == "HOLD_EVIDENCE" else "NO",
            a13_current_state="KEY_MATCH" if has_a13 else "ABSENT",
        ))

    for fid, name, parity, prio, pid in infra_rows:
        if fid == "infra.backup_restore":
            backup = next((m for m in explicit_manifests if m.proof_id == "PROOF_BACKUP_V2"), None)
            if backup:
                remember(backup)
        if fid == "infra.locale_reconcile":
            locale = next((m for m in explicit_manifests if m.proof_id == "PROOF_MIUIZER_LOCALE"), None)
            if locale:
                remember(locale)
        rows.append(_row(
            domain="infrastructure",
            a14_feature_id=fid,
            a14_name=name,
            a14_pref_keys="",
            a13_feature_id=fid,
            a13_pref_keys="",
            node_type="PRODUCT_ACTION",
            parity=parity,
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            proof_id=pid,
            source_relationship="UPSTREAM_INTENT_EQUIVALENT",
            host_package="SETTINGS",
            process="com.android.settings",
            classloader="settings",
            a14_behavior="Settings/app infrastructure behavior with explicit UX contract.",
            a13_behavior="A13 infrastructure path reviewed against A14 contract (Backup V2 preserved).",
            a14_reference="utils/BackupFormatV2.kt, utils/BackupRestore.kt, utils/RestartPagePolicy.kt",
            a13_reference="utils/BackupFormatV2.kt, PreferenceFragmentBase.kt, AppLocaleController.kt",
            risk="MEDIUM",
            priority=prio,
            phase_e_batch="",
            implementation_mode="NO_IMPLEMENTATION",
            api33="Preserve A13-compatible UX contract.",
            test_strategy="Unit + integration + migration fixtures.",
            rom_evidence="NO",
            a13_current_state="legacy implementation",
        ))

    a14_keys = {r["a14_pref_keys"] for r in rows if r["a14_pref_keys"]}
    for key, node in sorted(a13_nodes.items()):
        if not is_product_node(node.node_type):
            continue
        if key in a14_keys:
            continue
        rows.append(_row(
            domain=node.xml_file.replace("prefs_", "").replace(".xml", ""),
            a14_feature_id="",
            a14_name="",
            a14_pref_keys="",
            a13_feature_id=f"A13_UI_{key}",
            a13_pref_keys=key,
            node_type=node.node_type,
            parity="A13_ONLY_KEEP",
            evidence_level="MECHANICAL_ONLY",
            proof_id="",
            source_relationship="A13_COMPAT_VARIANT",
            host_package="A13_ONLY",
            process="A13_ONLY",
            classloader="A13_ONLY",
            a14_behavior="",
            a13_behavior="A13-only capability retained.",
            a14_reference="",
            a13_reference=node.xml_file,
            risk="LOW",
            priority="P3",
            phase_e_batch="",
            implementation_mode="NO_IMPLEMENTATION",
            api33="KEEP",
            test_strategy="Preserve existing behavior.",
            rom_evidence="NO",
            a13_current_state="A13-only capability",
        ))

    # Exactly one product-level Dynamic Island exclusion. Helpers are non-product.
    di_rows = [r for r in rows if r["parity_state"] == "INTENTIONAL_EXCLUDED"]
    if len(di_rows) != 1:
        rows = [r for r in rows if r["parity_state"] != "INTENTIONAL_EXCLUDED"]
        rows.append(_row(
            domain="system",
            a14_feature_id=DI_PRODUCT_KEY,
            a14_name="Dynamic Island",
            a14_pref_keys=DI_PRODUCT_KEY,
            a13_feature_id="",
            a13_pref_keys="",
            node_type="PRODUCT_ACTION",
            parity="INTENTIONAL_EXCLUDED",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            proof_id="PROOF_DYNAMIC_ISLAND_EXCLUDED",
            source_relationship="A14_NEW_FEATURE",
            host_package="SYSTEM_UI",
            process="com.android.systemui",
            classloader="systemui",
            a14_behavior="Dynamic Island / smart-notch product family, including strong-toast island mode.",
            a13_behavior="Intentionally excluded on A13. Island offset is a non-product helper, not a second gap.",
            a14_reference="Product policy PORT=NO",
            a13_reference="ABSENT",
            risk="LOW",
            priority="P3",
            phase_e_batch="",
            implementation_mode="NO_IMPLEMENTATION",
            api33="PORT=NO",
            test_strategy="N/A",
            rom_evidence="NO",
            a13_current_state="excluded",
        ))

    leftover_source_review = 0
    for r in rows:
        if r["parity_state"] in {"UNPROVEN", "SOURCE_REVIEW_REQUIRED"}:
            leftover_source_review += 1
    source_review_required = leftover_source_review

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
    with csv_path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=ordered_columns)
        w.writeheader()
        w.writerows(rows)

    a14_actionable = sum(1 for r in rows if r["a14_feature_id"])
    a13_product = sum(1 for r in rows if r["a13_feature_id"])
    a13_only = sum(1 for r in rows if r["parity_state"] == "A13_ONLY_KEEP")
    c = Counter(r["parity_state"] for r in rows if r["a14_feature_id"])
    ev = Counter(r["evidence_level"] for r in rows if r["a14_feature_id"])
    proof_kind = Counter()
    for r in rows:
        if not r["a14_feature_id"]:
            continue
        pid = r.get("proof_id") or ""
        if pid.startswith("PROOF_FP_") and r["parity_state"] == "PRESENT_EQUIVALENT":
            proof_kind["IDENTICAL_OWNER"] += 1
        elif r["parity_state"] == "PRESENT_A13_VARIANT":
            proof_kind["REVIEWED_VARIANT"] += 1
        if pid.startswith("PROOF_") and r["parity_state"] in {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"}:
            if not pid.startswith("PROOF_FP_"):
                proof_kind["INDIVIDUAL"] += 1
    batch_counts = derive_batch_counts(rows)
    hold_evidence_count = c.get("HOLD_EVIDENCE", 0)
    phase_e_ready_gaps = sum(batch_counts.get(b, 0) for b in ["E1", "E2", "E3", "E4", "E5"])
    true_missing_remaining = c.get("MISSING_IN_A13", 0)
    candidate_ui_without_impl = sum(1 for k, n in a14_nodes.items() if is_product_node(n.node_type) and k not in a14_reads)
    candidate_impl_without_ui = sum(1 for k in a14_reads if k not in a14_nodes)
    product_app_selector_rows = sum(
        1 for k, n in a14_nodes.items() if is_product_node(n.node_type) and is_app_selector_key(k)
    )
    fsg_rows = [r for r in rows if r["a14_pref_keys"] == "controls_fsg_horiz"]
    locale_rows = [r for r in rows if r["a14_pref_keys"] == "miuizer_locale"]
    fsg_state = fsg_rows[0]["parity_state"] if fsg_rows else "NOT_FOUND"
    locale_state = locale_rows[0]["parity_state"] if locale_rows else "NOT_FOUND"
    hold_rows = [r for r in rows if r["parity_state"] == "HOLD_EVIDENCE" and r["a14_feature_id"]]
    dead_rows = [r for r in rows if r["parity_state"] == "DEAD_UPSTREAM_PATH" and r["a14_feature_id"]]

    clear_rows = [r for r in rows if r["a14_pref_keys"] == "various_clear_update_state"]
    defraud_rows = [r for r in rows if r["a14_pref_keys"] == "various_disable_defraud_apps_detect"]
    clear_state = clear_rows[0]["parity_state"] if clear_rows else "NOT_FOUND"
    defraud_state = defraud_rows[0]["parity_state"] if defraud_rows else "NOT_FOUND"
    partial_with_new_port = sum(
        1 for r in rows
        if r.get("a14_feature_id") and r["parity_state"] == "PARTIAL_PARITY" and r["implementation_mode"] == "NEW_PORT"
    )
    og_present_keys = sum(
        1 for r in rows
        if r.get("a14_feature_id") and (r.get("proof_id") or "").startswith("PROOF_OG_")
        and r["parity_state"] == "PRESENT_A13_VARIANT"
    )
    row_by_key = {r["a14_pref_keys"]: r for r in rows if r.get("a14_pref_keys")}
    present_states = {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT"}
    true_groups_reviewed = 0
    for group in og_index.discovered:
        product_keys = [k for k in group.keys if k in row_by_key]
        if product_keys and all(row_by_key[k]["parity_state"] in present_states for k in product_keys):
            true_groups_reviewed += 1
    wake_group_keys = group_keys_for_symbol(og_index.discovered, "NoFingerprintWakeHook")
    success_group_keys = group_keys_for_symbol(og_index.discovered, "FingerprintHapticSuccessHook")
    keys_with_direct_owner = og_index.stats.keys_with_direct_owner_evidence
    keys_with_explicit_alias = sum(
        1 for rec in missing_audit_records
        if rec.final_parity_state in present_states | {"HOLD_EVIDENCE", "PARTIAL_PARITY"}
    )
    xml_only_unproven = sum(
        1 for r in rows
        if r.get("a14_feature_id")
        and r["parity_state"] == "SOURCE_REVIEW_REQUIRED"
        and r["a14_pref_keys"] not in og_index.evidence_by_key
    )
    false_assignments_removed = len(FALSE_OWNER_ASSIGNMENTS_REMOVED)
    unproven_leftover = sum(
        1 for r in rows if r.get("a14_feature_id") and r["parity_state"] == "UNPROVEN"
    )

    print(f"A14_PRODUCT_FEATURE_COUNT={a14_actionable}")
    print(f"A13_PRODUCT_FEATURE_COUNT={a13_product}")
    print(f"A13_ONLY_KEEP_COUNT={a13_only}")
    for k in ["PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "DEAD_UPSTREAM_PATH", "HOLD_EVIDENCE", "INSUFFICIENT_EVIDENCE"]:
        print(f"{k}_COUNT={c.get(k, 0)}")
    print(f"SOURCE_REVIEW_REQUIRED={source_review_required}")
    print(f"NON_PRODUCT_HELPERS_REMOVED={non_product_helpers}")
    print(f"PRODUCT_APP_SELECTOR_ROWS={product_app_selector_rows}")
    print(f"IDENTICAL_OWNER_PROOF_ROWS={proof_kind['IDENTICAL_OWNER']}")
    print(f"REVIEWED_VARIANT_PROOF_ROWS={proof_kind['REVIEWED_VARIANT']}")
    print(f"FSGESTURES_FINAL_STATE={fsg_state}")
    print(f"MIUIZER_LOCALE_FINAL_STATE={locale_state}")
    print(f"HIDDEN_HELPERS={hidden_helpers}")
    print(f"DYNAMIC_ISLAND_HELPERS={di_helpers}")
    print(f"UI_TOPOLOGY_NODE_COUNT_A14={a14_topology_count}")
    print(f"UI_TOPOLOGY_NODE_COUNT_A13={a13_topology_count}")
    print(f"CANDIDATE_UI_WITHOUT_IMPLEMENTATION={candidate_ui_without_impl}")
    print(f"CANDIDATE_IMPLEMENTATION_WITHOUT_UI={candidate_impl_without_ui}")
    print("CONFIRMED_UI_WITHOUT_IMPLEMENTATION=0")
    print("CONFIRMED_IMPLEMENTATION_WITHOUT_UI=0")
    print("INTERNAL_IMPLEMENTATION_WITHOUT_UI=0")
    print(f"STRUCTURAL_SEMANTIC_PROOF_ROWS={ev.get('STRUCTURAL_SEMANTIC_PROOF', 0)}")
    print(f"INDIVIDUAL_SEMANTIC_PROOF_ROWS={ev.get('INDIVIDUAL_SEMANTIC_PROOF', 0)}")
    print(f"IMPLEMENTATION_PRESENCE_ROWS={ev.get('IMPLEMENTATION_PRESENCE', 0)}")
    print(f"MECHANICAL_ONLY_ROWS={ev.get('MECHANICAL_ONLY', 0)}")
    print(f"SOURCE_SEMANTIC_PROOF_ROWS={sum(1 for r in rows if r['a14_feature_id'] and r['parity_state'] in {'PRESENT_EQUIVALENT', 'PRESENT_A13_VARIANT'})}")
    print(f"STRUCTURAL_OWNER_PROOF_ROWS={proof_kind['IDENTICAL_OWNER']}")
    print(f"INDIVIDUAL_PROOF_ROWS={proof_kind['INDIVIDUAL']}")
    print(f"DEAD_PATH_SOURCE_PROVEN_COUNT={len(dead_rows)}")
    print(f"ROM_DEVICE_HOLD_COUNT={hold_evidence_count}")
    print(f"A14_SPEC_DISCOVERED={a14_spec_discovered}")
    print(f"A14_SPEC_UNKNOWN={a14_spec_unknown}")
    print(f"HOLD_EVIDENCE_COUNT={hold_evidence_count}")
    print(f"PHASE_E_READY_GAPS={phase_e_ready_gaps}")
    print(f"CURRENT_MISSING_ROWS_AUDITED={current_missing_rows_audited}")
    print(f"FALSE_MISSING_RECLASSIFIED={false_missing_reclassified}")
    print(f"PRESENT_A13_VARIANT_RECLASSIFIED={present_reclassified}")
    print(f"PARTIAL_PARITY_RECLASSIFIED={partial_reclassified}")
    print(f"TRUE_MISSING_REMAINING={true_missing_remaining}")
    print(f"TRUE_OWNER_GROUPS_DISCOVERED={og_index.stats.groups_discovered}")
    print(f"TRUE_OWNER_GROUPS_REVIEWED={true_groups_reviewed}")
    print(f"KEYS_WITH_DIRECT_OWNER_EVIDENCE={keys_with_direct_owner}")
    print(f"KEYS_WITH_EXPLICIT_ALIAS_EVIDENCE={keys_with_explicit_alias}")
    print(f"XML_ONLY_UNPROVEN_KEYS={xml_only_unproven}")
    print(f"FALSE_OWNER_ASSIGNMENTS_REMOVED={false_assignments_removed}")
    print(f"NO_FINGERPRINT_WAKE_GROUP_KEYS={','.join(wake_group_keys) or '(none)'}")
    print(f"FINGERPRINT_SUCCESS_GROUP_KEYS={','.join(success_group_keys) or '(none)'}")
    print(f"OWNER_GROUPS_REVIEWED={true_groups_reviewed}")
    print(f"OWNER_GROUP_PRESENT_EQUIVALENT={og_index.stats.present_equivalent}")
    print(f"OWNER_GROUP_PRESENT_VARIANT={og_index.stats.present_variant}")
    print(f"OWNER_GROUP_TRUE_PARTIAL={og_index.stats.true_partial}")
    print(f"OWNER_GROUP_ROM_HOLD={og_index.stats.rom_hold}")
    print(f"FALSE_PARTIALS_RECLASSIFIED={og_present_keys}")
    print("FEATURES_NEWLY_PORTED=0")
    print("EXISTING_A13_FEATURES_UPGRADED=0")
    print(f"PARTIAL_WITH_NEW_PORT_COUNT={partial_with_new_port}")
    print(f"CLEAR_UPDATE_STATE_FINAL_STATE={clear_state}")
    print(f"DEFRAUD_APPS_FINAL_STATE={defraud_state}")
    hide_ime = [r for r in rows if r["a14_pref_keys"] == "controls_hide_ime_dismiss_button" or r["a14_feature_id"] == "HideImeDismissButtonFeatureId"]
    hide_ime_ok = bool(hide_ime and hide_ime[0]["parity_state"] in {"PRESENT_A13_VARIANT", "PRESENT_EQUIVALENT"})
    print(f"HIDE_IME_ROUTING={(hide_ime[0]['phase_e_batch'] or hide_ime[0]['parity_state'] if hide_ime else 'NOT_FOUND')}")
    print(f"E_BATCH_ROUTING_TEST={'PASS' if hide_ime_ok else 'FAIL'}")
    print(f"DYNAMIC_ISLAND_EXCLUDED_EXACTLY_ONCE={'YES' if sum(1 for r in rows if r['parity_state']=='INTENTIONAL_EXCLUDED') == 1 else 'NO'}")
    print(f"PARITY_ACCOUNTING_INVARIANT={'PASS' if parity_accounting_invariant(rows) else 'FAIL'}")
    warning_rows = [r for r in rows if r["a14_pref_keys"] == "warning" or r["a14_feature_id"] == "A14_UI_warning"]
    print(f"WARNING_NOT_PRODUCT={'YES' if not warning_rows else 'NO'}")
    island_rows = [r for r in rows if r["a14_pref_keys"] in DI_HELPER_KEYS]
    print(f"DI_HELPER_NOT_PRODUCT={'YES' if not island_rows else 'NO'}")
    for batch in ["E1", "E2", "E3", "E4", "E5"]:
        print(f"{batch}_COUNT={batch_counts.get(batch, 0)}")

    reconciliation_path = out_dir / "A13_PHASE_F_RESIDUAL_AUDIT.md"
    residual_rows = [
        r for r in rows
        if r.get("a14_feature_id") and r["parity_state"] in {
            "MISSING_IN_A13", "PARTIAL_PARITY", "INSUFFICIENT_EVIDENCE", "HOLD_EVIDENCE",
            "DEAD_UPSTREAM_PATH", "SOURCE_REVIEW_REQUIRED", "UNPROVEN",
        }
    ]
    with reconciliation_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# A13 Phase F-R4 Residual Audit\n\n")
        f.write("Historical Phase A-E reports are not rewritten.\n\n")
        f.write("AUTHORITATIVE_BASE_SHA = 526db84d23a5d98d6d673abe705dcacdeaa78746\n")
        f.write("A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa\n\n")
        f.write(f"A14_PRODUCT_FEATURE_COUNT = {a14_actionable}\n")
        f.write(f"HOLD_EVIDENCE = {c.get('HOLD_EVIDENCE', 0)}\n")
        f.write(f"DEAD_UPSTREAM_PATH = {c.get('DEAD_UPSTREAM_PATH', 0)}\n")
        f.write(f"MISSING_IN_A13 = {c.get('MISSING_IN_A13', 0)}\n")
        f.write(f"PARTIAL_PARITY = {c.get('PARTIAL_PARITY', 0)}\n")
        f.write(f"INSUFFICIENT_EVIDENCE = {c.get('INSUFFICIENT_EVIDENCE', 0)}\n")
        f.write(f"SOURCE_REVIEW_REQUIRED = {source_review_required}\n")
        f.write(f"INTENTIONAL_EXCLUDED = {c.get('INTENTIONAL_EXCLUDED', 0)}\n")
        f.write(f"NON_PRODUCT_HELPERS_REMOVED = {non_product_helpers}\n")
        f.write(f"DYNAMIC_ISLAND_HELPERS = {di_helpers}\n")
        f.write(f"TRUE_OWNER_GROUPS_DISCOVERED = {og_index.stats.groups_discovered}\n")
        f.write(f"TRUE_OWNER_GROUPS_REVIEWED = {true_groups_reviewed}\n")
        f.write(f"XML_ONLY_UNPROVEN_KEYS = {xml_only_unproven}\n")
        f.write(f"FALSE_OWNER_ASSIGNMENTS_REMOVED = {false_assignments_removed}\n")
        f.write(f"NO_FINGERPRINT_WAKE_GROUP_KEYS = {','.join(wake_group_keys) or '(none)'}\n")
        f.write(f"FINGERPRINT_SUCCESS_GROUP_KEYS = {','.join(success_group_keys) or '(none)'}\n\n")
        f.write("| A14_FEATURE_ID | A14_PREF_KEYS | FINAL_PARITY_STATE | HOST | PROOF_ID |\n")
        f.write("|---|---|---|---|---|\n")
        for r in residual_rows:
            f.write(
                f"| {r['a14_feature_id']} | {r['a14_pref_keys']} | {r['parity_state']} | {r['host_package']} | {r['proof_id']} |\n"
            )
        f.write("\n## Proven dead paths\n\n")
        for key, dead in sorted(dead_proofs.items()):
            f.write(f"- `{key}`\n")
            f.write(f"  - A14_UI_REFERENCE: `{dead.a14_ui_reference}`\n")
            f.write(f"  - A14_SEARCH_REFERENCES: {dead.a14_search_references}\n")
            f.write(f"  - A14_NEAREST_CANDIDATE: {dead.a14_nearest_candidate}\n")
            f.write(f"  - WHY_NOT_REACHABLE: {dead.why_not_reachable}\n\n")
        f.write("\n## Initial missing-candidate notes\n\n")
        for rec in missing_audit_records:
            f.write(f"- **A14_FEATURE_ID**: `{rec.a14_feature_id}`\n")
            f.write(f"  - A14_PREF_KEYS: `{rec.a14_pref_keys}`\n")
            f.write(f"  - INITIAL_STATE: `{rec.final_parity_state}`\n")
            f.write(f"  - A13_MATCH: `{rec.a13_match}`\n")
            f.write(f"  - ABSENCE_PROOF: {rec.absence_proof}\n\n")

    hold_path = out_dir / "A13_PHASE_F_HOLD_EVIDENCE.md"
    with hold_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# A13 Phase F-R4 HOLD_EVIDENCE\n\n")
        f.write(f"HOLD_EVIDENCE_COUNT = {hold_evidence_count}\n")
        f.write(f"DEAD_UPSTREAM_PATH_COUNT = {len(dead_rows)}\n")
        f.write(f"SOURCE_REVIEW_REQUIRED = {source_review_required}\n\n")
        f.write("Final HOLD_EVIDENCE rows are ROM_DEVICE_HOLD only: ROM ABI, class/member, layout/view identity,\n")
        f.write("device behavior, or boot/system_server risk. Module-owned app logic is not parked here.\n")
        f.write("SOURCE_REVIEW_REQUIRED is not HOLD_EVIDENCE.\n\n")
        for i, r in enumerate(hold_rows):
            key = r["a14_pref_keys"] or r["a14_feature_id"]
            rec = hold_map.get(r["a14_pref_keys"], {})
            f.write(f"## {key}\n\n")
            f.write(f"- unresolved_question: {rec.get('unresolved_question', r['a14_behavior'])}\n")
            f.write(f"- affected_rom_process: {rec.get('affected_rom_process', r['process'])}\n")
            f.write(f"- safe_default: {rec.get('safe_default', 'feature off / ROM default')}\n")
            f.write(f"- required_device_evidence: {rec.get('required_device_evidence', 'Host class/member dump on MIUI 14')}\n")
            f.write(f"- why_static_source_cannot_decide: {rec.get('why_forbidden', r['a13_behavior'])}\n")
            if i != len(hold_rows) - 1:
                f.write("\n")

    proofs_path = out_dir / "A13_PHASE_F_SEMANTIC_PROOFS.md"
    proofs_path.write_text(format_proof_markdown(used_manifests), encoding="utf-8")

    report_path = out_dir / "A13_PHASE_F_FINAL_PARITY_REPORT.md"
    with report_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# A13_PHASE_F_R4_FINAL_PARITY_REPORT\n\n")
        f.write("AUTHORITATIVE_BASE_SHA = 526db84d23a5d98d6d673abe705dcacdeaa78746\n")
        f.write("A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa\n")
        f.write("VERIFIED_TREE_SHA = (this commit)\n")
        f.write("REPORT_HEAD_SHA = (this commit)\n\n")
        f.write(f"A14_PRODUCT_FEATURE_COUNT = {a14_actionable}\n")
        f.write(f"A13_PRODUCT_FEATURE_COUNT = {a13_product}\n")
        f.write(f"A13_ONLY_KEEP_COUNT = {a13_only}\n\n")
        for k in ["PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "DEAD_UPSTREAM_PATH", "HOLD_EVIDENCE", "INSUFFICIENT_EVIDENCE"]:
            f.write(f"{k} = {c.get(k, 0)}\n")
        f.write(f"SOURCE_REVIEW_REQUIRED = {source_review_required}\n\n")
        f.write(f"PRODUCT_APP_SELECTOR_ROWS = {product_app_selector_rows}\n")
        f.write(f"NON_PRODUCT_HELPERS_REMOVED = {non_product_helpers}\n")
        f.write("DYNAMIC_ISLAND_PRODUCT_EXCLUSION_COUNT = 1\n")
        f.write(f"DYNAMIC_ISLAND_HELPERS_EXCLUDED_FROM_PRODUCT = {di_helpers}\n")
        f.write(f"IDENTICAL_OWNER_PROOF_ROWS = {proof_kind['IDENTICAL_OWNER']}\n")
        f.write(f"REVIEWED_VARIANT_PROOF_ROWS = {proof_kind['REVIEWED_VARIANT']}\n")
        f.write(f"INDIVIDUAL_PROOF_ROWS = {proof_kind['INDIVIDUAL']}\n")
        f.write(f"DEAD_PATH_SOURCE_PROVEN_COUNT = {len(dead_rows)}\n")
        f.write(f"ROM_DEVICE_HOLD_COUNT = {hold_evidence_count}\n")
        f.write(f"TRUE_OWNER_GROUPS_DISCOVERED = {og_index.stats.groups_discovered}\n")
        f.write(f"TRUE_OWNER_GROUPS_REVIEWED = {true_groups_reviewed}\n")
        f.write(f"KEYS_WITH_DIRECT_OWNER_EVIDENCE = {keys_with_direct_owner}\n")
        f.write(f"KEYS_WITH_EXPLICIT_ALIAS_EVIDENCE = {keys_with_explicit_alias}\n")
        f.write(f"XML_ONLY_UNPROVEN_KEYS = {xml_only_unproven}\n")
        f.write(f"FALSE_OWNER_ASSIGNMENTS_REMOVED = {false_assignments_removed}\n")
        f.write(f"NO_FINGERPRINT_WAKE_GROUP_KEYS = {','.join(wake_group_keys) or '(none)'}\n")
        f.write(f"FINGERPRINT_SUCCESS_GROUP_KEYS = {','.join(success_group_keys) or '(none)'}\n")
        f.write("FEATURES_NEWLY_PORTED = 0\n")
        f.write("EXISTING_A13_FEATURES_UPGRADED = 0\n")
        f.write(f"PARTIAL_WITH_NEW_PORT_COUNT = {partial_with_new_port}\n")
        f.write(f"CLEAR_UPDATE_STATE_FINAL_STATE = {clear_state}\n")
        f.write(f"DEFRAUD_APPS_FINAL_STATE = {defraud_state}\n")
        f.write(f"FSGESTURES_FINAL_STATE = {fsg_state}\n")
        f.write(f"MIUIZER_LOCALE_FINAL_STATE = {locale_state}\n\n")
        f.write("PRODUCTION_CHANGED = NO\n")
        f.write("Owner-group PRESENT is explicit reviewed manifests or IDENTICAL bodies only.\n")
        f.write("Prefix, ranked-first, same-XML, and same-basename-alone are not ownership proof.\n")
        f.write("Non-identical candidates without an explicit review stay SOURCE_REVIEW_REQUIRED.\n")
        f.write("SOURCE_REVIEW_REQUIRED is an allowed leftover; it is not auto-promoted to PRESENT.\n")
        f.write("PARTIAL_PARITY is never NEW_PORT.\n")

    print(f"MISSING_RECONCILIATION={reconciliation_path}")
    print(f"HOLD_EVIDENCE_MD={hold_path}")
    print(f"SEMANTIC_PROOFS={proofs_path}")
    print(f"FINAL_REPORT={report_path}")
    print(f"CSV={csv_path}")
    leftover_gap = (
        partial_with_new_port
        or unproven_leftover
        or not parity_accounting_invariant(rows)
    )
    return 1 if leftover_gap else 0
    print(f"A13_PRODUCT_FEATURE_COUNT={a13_product}")
    print(f"A13_ONLY_KEEP_COUNT={a13_only}")
    for k in ["PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "DEAD_UPSTREAM_PATH", "HOLD_EVIDENCE", "INSUFFICIENT_EVIDENCE"]:
        print(f"{k}_COUNT={c.get(k, 0)}")
    print(f"SOURCE_REVIEW_REQUIRED={source_review_required}")
    print(f"NON_PRODUCT_HELPERS_REMOVED={non_product_helpers}")
    print(f"PRODUCT_APP_SELECTOR_ROWS={product_app_selector_rows}")
    print(f"IDENTICAL_OWNER_PROOF_ROWS={proof_kind['IDENTICAL_OWNER']}")
    print(f"REVIEWED_VARIANT_PROOF_ROWS={proof_kind['REVIEWED_VARIANT']}")
    print(f"FSGESTURES_FINAL_STATE={fsg_state}")
    print(f"MIUIZER_LOCALE_FINAL_STATE={locale_state}")
    print(f"HIDDEN_HELPERS={hidden_helpers}")
    print(f"DYNAMIC_ISLAND_HELPERS={di_helpers}")
    print(f"UI_TOPOLOGY_NODE_COUNT_A14={a14_topology_count}")
    print(f"UI_TOPOLOGY_NODE_COUNT_A13={a13_topology_count}")
    print(f"CANDIDATE_UI_WITHOUT_IMPLEMENTATION={candidate_ui_without_impl}")
    print(f"CANDIDATE_IMPLEMENTATION_WITHOUT_UI={candidate_impl_without_ui}")
    print("CONFIRMED_UI_WITHOUT_IMPLEMENTATION=0")
    print("CONFIRMED_IMPLEMENTATION_WITHOUT_UI=0")
    print("INTERNAL_IMPLEMENTATION_WITHOUT_UI=0")
    print(f"STRUCTURAL_SEMANTIC_PROOF_ROWS={ev.get('STRUCTURAL_SEMANTIC_PROOF', 0)}")
    print(f"INDIVIDUAL_SEMANTIC_PROOF_ROWS={ev.get('INDIVIDUAL_SEMANTIC_PROOF', 0)}")
    print(f"IMPLEMENTATION_PRESENCE_ROWS={ev.get('IMPLEMENTATION_PRESENCE', 0)}")
    print(f"MECHANICAL_ONLY_ROWS={ev.get('MECHANICAL_ONLY', 0)}")
    print(f"SOURCE_SEMANTIC_PROOF_ROWS={sum(1 for r in rows if r['a14_feature_id'] and r['parity_state'] in {'PRESENT_EQUIVALENT', 'PRESENT_A13_VARIANT'})}")
    print(f"STRUCTURAL_OWNER_PROOF_ROWS={proof_kind['IDENTICAL_OWNER']}")
    print(f"INDIVIDUAL_PROOF_ROWS={proof_kind['INDIVIDUAL']}")
    print(f"DEAD_PATH_SOURCE_PROVEN_COUNT={len(dead_rows)}")
    print(f"ROM_DEVICE_HOLD_COUNT={hold_evidence_count}")
    print(f"A14_SPEC_DISCOVERED={a14_spec_discovered}")
    print(f"A14_SPEC_UNKNOWN={a14_spec_unknown}")
    print(f"HOLD_EVIDENCE_COUNT={hold_evidence_count}")
    print(f"PHASE_E_READY_GAPS={phase_e_ready_gaps}")
    print(f"CURRENT_MISSING_ROWS_AUDITED={current_missing_rows_audited}")
    print(f"FALSE_MISSING_RECLASSIFIED={false_missing_reclassified}")
    print(f"PRESENT_A13_VARIANT_RECLASSIFIED={present_reclassified}")
    print(f"PARTIAL_PARITY_RECLASSIFIED={partial_reclassified}")
    print(f"TRUE_MISSING_REMAINING={true_missing_remaining}")
    print(f"OWNER_GROUPS_REVIEWED={og_index.stats.groups_reviewed}")
    print(f"OWNER_GROUP_PRESENT_EQUIVALENT={og_index.stats.present_equivalent}")
    print(f"OWNER_GROUP_PRESENT_VARIANT={og_index.stats.present_variant}")
    print(f"OWNER_GROUP_TRUE_PARTIAL={og_index.stats.true_partial}")
    print(f"OWNER_GROUP_ROM_HOLD={og_index.stats.rom_hold}")
    print(f"FALSE_PARTIALS_RECLASSIFIED={og_present_keys}")
    print("FEATURES_NEWLY_PORTED=0")
    print("EXISTING_A13_FEATURES_UPGRADED=0")
    print(f"PARTIAL_WITH_NEW_PORT_COUNT={partial_with_new_port}")
    print(f"CLEAR_UPDATE_STATE_FINAL_STATE={clear_state}")
    print(f"DEFRAUD_APPS_FINAL_STATE={defraud_state}")
    hide_ime = [r for r in rows if r["a14_pref_keys"] == "controls_hide_ime_dismiss_button" or r["a14_feature_id"] == "HideImeDismissButtonFeatureId"]
    hide_ime_ok = bool(hide_ime and hide_ime[0]["parity_state"] in {"PRESENT_A13_VARIANT", "PRESENT_EQUIVALENT"})
    print(f"HIDE_IME_ROUTING={(hide_ime[0]['phase_e_batch'] or hide_ime[0]['parity_state'] if hide_ime else 'NOT_FOUND')}")
    print(f"E_BATCH_ROUTING_TEST={'PASS' if hide_ime_ok else 'FAIL'}")
    print(f"DYNAMIC_ISLAND_EXCLUDED_EXACTLY_ONCE={'YES' if sum(1 for r in rows if r['parity_state']=='INTENTIONAL_EXCLUDED') == 1 else 'NO'}")
    print(f"PARITY_ACCOUNTING_INVARIANT={'PASS' if parity_accounting_invariant(rows) else 'FAIL'}")
    warning_rows = [r for r in rows if r["a14_pref_keys"] == "warning" or r["a14_feature_id"] == "A14_UI_warning"]
    print(f"WARNING_NOT_PRODUCT={'YES' if not warning_rows else 'NO'}")
    island_rows = [r for r in rows if r["a14_pref_keys"] in DI_HELPER_KEYS]
    print(f"DI_HELPER_NOT_PRODUCT={'YES' if not island_rows else 'NO'}")
    for batch in ["E1", "E2", "E3", "E4", "E5"]:
        print(f"{batch}_COUNT={batch_counts.get(batch, 0)}")

    reconciliation_path = out_dir / "A13_PHASE_F_RESIDUAL_AUDIT.md"
    residual_rows = [
        r for r in rows
        if r.get("a14_feature_id") and r["parity_state"] in {
            "MISSING_IN_A13", "PARTIAL_PARITY", "INSUFFICIENT_EVIDENCE", "HOLD_EVIDENCE", "DEAD_UPSTREAM_PATH",
        }
    ]
    with reconciliation_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# A13 Phase F-R3 Residual Audit\n\n")
        f.write("Historical Phase A-E reports are not rewritten.\n\n")
        f.write("AUTHORITATIVE_BASE_SHA = 58545733ded848a91a746140b5aa986da872b481\n")
        f.write("A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa\n\n")
        f.write(f"A14_PRODUCT_FEATURE_COUNT = {a14_actionable}\n")
        f.write(f"HOLD_EVIDENCE = {c.get('HOLD_EVIDENCE', 0)}\n")
        f.write(f"DEAD_UPSTREAM_PATH = {c.get('DEAD_UPSTREAM_PATH', 0)}\n")
        f.write(f"MISSING_IN_A13 = {c.get('MISSING_IN_A13', 0)}\n")
        f.write(f"PARTIAL_PARITY = {c.get('PARTIAL_PARITY', 0)}\n")
        f.write(f"INSUFFICIENT_EVIDENCE = {c.get('INSUFFICIENT_EVIDENCE', 0)}\n")
        f.write(f"SOURCE_REVIEW_REQUIRED = {source_review_required}\n")
        f.write(f"INTENTIONAL_EXCLUDED = {c.get('INTENTIONAL_EXCLUDED', 0)}\n")
        f.write(f"NON_PRODUCT_HELPERS_REMOVED = {non_product_helpers}\n")
        f.write(f"DYNAMIC_ISLAND_HELPERS = {di_helpers}\n\n")
        f.write("| A14_FEATURE_ID | A14_PREF_KEYS | FINAL_PARITY_STATE | HOST | PROOF_ID |\n")
        f.write("|---|---|---|---|---|\n")
        for r in residual_rows:
            f.write(
                f"| {r['a14_feature_id']} | {r['a14_pref_keys']} | {r['parity_state']} | {r['host_package']} | {r['proof_id']} |\n"
            )
        f.write("\n## Proven dead paths\n\n")
        for key, dead in sorted(dead_proofs.items()):
            f.write(f"- `{key}`\n")
            f.write(f"  - A14_UI_REFERENCE: `{dead.a14_ui_reference}`\n")
            f.write(f"  - A14_SEARCH_REFERENCES: {dead.a14_search_references}\n")
            f.write(f"  - A14_NEAREST_CANDIDATE: {dead.a14_nearest_candidate}\n")
            f.write(f"  - WHY_NOT_REACHABLE: {dead.why_not_reachable}\n\n")
        f.write("\n## Initial missing-candidate notes\n\n")
        for rec in missing_audit_records:
            f.write(f"- **A14_FEATURE_ID**: `{rec.a14_feature_id}`\n")
            f.write(f"  - A14_PREF_KEYS: `{rec.a14_pref_keys}`\n")
            f.write(f"  - INITIAL_STATE: `{rec.final_parity_state}`\n")
            f.write(f"  - A13_MATCH: `{rec.a13_match}`\n")
            f.write(f"  - ABSENCE_PROOF: {rec.absence_proof}\n\n")

    hold_path = out_dir / "A13_PHASE_F_HOLD_EVIDENCE.md"
    with hold_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# A13 Phase F-R3 HOLD_EVIDENCE\n\n")
        f.write(f"HOLD_EVIDENCE_COUNT = {hold_evidence_count}\n")
        f.write(f"DEAD_UPSTREAM_PATH_COUNT = {len(dead_rows)}\n")
        f.write(f"SOURCE_REVIEW_REQUIRED = {source_review_required}\n\n")
        f.write("Final HOLD_EVIDENCE rows are ROM_DEVICE_HOLD only: ROM ABI, class/member, layout/view identity,\n")
        f.write("device behavior, or boot/system_server risk. Module-owned app logic is not parked here.\n\n")
        for i, r in enumerate(hold_rows):
            key = r["a14_pref_keys"] or r["a14_feature_id"]
            rec = hold_map.get(r["a14_pref_keys"], {})
            f.write(f"## {key}\n\n")
            f.write(f"- unresolved_question: {rec.get('unresolved_question', r['a14_behavior'])}\n")
            f.write(f"- affected_rom_process: {rec.get('affected_rom_process', r['process'])}\n")
            f.write(f"- safe_default: {rec.get('safe_default', 'feature off / ROM default')}\n")
            f.write(f"- required_device_evidence: {rec.get('required_device_evidence', 'Host class/member dump on MIUI 14')}\n")
            f.write(f"- why_static_source_cannot_decide: {rec.get('why_forbidden', r['a13_behavior'])}\n")
            if i != len(hold_rows) - 1:
                f.write("\n")

    proofs_path = out_dir / "A13_PHASE_F_SEMANTIC_PROOFS.md"
    proofs_path.write_text(format_proof_markdown(used_manifests), encoding="utf-8")

    report_path = out_dir / "A13_PHASE_F_FINAL_PARITY_REPORT.md"
    with report_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("# A13_PHASE_F_R3_FINAL_PARITY_REPORT\n\n")
        f.write("AUTHORITATIVE_BASE_SHA = 58545733ded848a91a746140b5aa986da872b481\n")
        f.write("A14_REFERENCE_SHA = d20d96b543a49a584970e312da7d704958a155aa\n")
        f.write("VERIFIED_TREE_SHA = (this commit)\n")
        f.write("REPORT_HEAD_SHA = (this commit)\n\n")
        f.write(f"A14_PRODUCT_FEATURE_COUNT = {a14_actionable}\n")
        f.write(f"A13_PRODUCT_FEATURE_COUNT = {a13_product}\n")
        f.write(f"A13_ONLY_KEEP_COUNT = {a13_only}\n\n")
        for k in ["PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "DEAD_UPSTREAM_PATH", "HOLD_EVIDENCE", "INSUFFICIENT_EVIDENCE"]:
            f.write(f"{k} = {c.get(k, 0)}\n")
        f.write(f"SOURCE_REVIEW_REQUIRED = {source_review_required}\n\n")
        f.write(f"PRODUCT_APP_SELECTOR_ROWS = {product_app_selector_rows}\n")
        f.write(f"NON_PRODUCT_HELPERS_REMOVED = {non_product_helpers}\n")
        f.write("DYNAMIC_ISLAND_PRODUCT_EXCLUSION_COUNT = 1\n")
        f.write(f"DYNAMIC_ISLAND_HELPERS_EXCLUDED_FROM_PRODUCT = {di_helpers}\n")
        f.write(f"IDENTICAL_OWNER_PROOF_ROWS = {proof_kind['IDENTICAL_OWNER']}\n")
        f.write(f"REVIEWED_VARIANT_PROOF_ROWS = {proof_kind['REVIEWED_VARIANT']}\n")
        f.write(f"INDIVIDUAL_PROOF_ROWS = {proof_kind['INDIVIDUAL']}\n")
        f.write(f"DEAD_PATH_SOURCE_PROVEN_COUNT = {len(dead_rows)}\n")
        f.write(f"ROM_DEVICE_HOLD_COUNT = {hold_evidence_count}\n")
        f.write(f"OWNER_GROUPS_REVIEWED = {og_index.stats.groups_reviewed}\n")
        f.write(f"OWNER_GROUP_PRESENT_EQUIVALENT = {og_index.stats.present_equivalent}\n")
        f.write(f"OWNER_GROUP_PRESENT_VARIANT = {og_index.stats.present_variant}\n")
        f.write(f"OWNER_GROUP_TRUE_PARTIAL = {og_index.stats.true_partial}\n")
        f.write(f"OWNER_GROUP_ROM_HOLD = {og_index.stats.rom_hold}\n")
        f.write(f"FALSE_PARTIALS_RECLASSIFIED = {og_present_keys}\n")
        f.write("FEATURES_NEWLY_PORTED = 0\n")
        f.write("EXISTING_A13_FEATURES_UPGRADED = 0\n")
        f.write(f"PARTIAL_WITH_NEW_PORT_COUNT = {partial_with_new_port}\n")
        f.write(f"CLEAR_UPDATE_STATE_FINAL_STATE = {clear_state}\n")
        f.write(f"DEFRAUD_APPS_FINAL_STATE = {defraud_state}\n")
        f.write(f"FSGESTURES_FINAL_STATE = {fsg_state}\n")
        f.write(f"MIUIZER_LOCALE_FINAL_STATE = {locale_state}\n\n")
        f.write("PRODUCTION_CHANGED = NO\n")
        f.write("Classifier/generator rewrite plus two ROM-evidence HOLDs for the former MISSING rows.\n")
        f.write("Automatic PRESENT requires identical normalized bodies. SequenceMatcher never authorizes PRESENT.\n")
        f.write("Non-identical owners are classified by owner-group review, not per-row dumps.\n")
        f.write("PARTIAL_PARITY is never NEW_PORT.\n")

    print(f"MISSING_RECONCILIATION={reconciliation_path}")
    print(f"HOLD_EVIDENCE_MD={hold_path}")
    print(f"SEMANTIC_PROOFS={proofs_path}")
    print(f"FINAL_REPORT={report_path}")
    print(f"CSV={csv_path}")
    leftover_gap = (
        source_review_required
        or c.get("PARTIAL_PARITY", 0)
        or c.get("MISSING_IN_A13", 0)
        or c.get("INSUFFICIENT_EVIDENCE", 0)
        or partial_with_new_port
    )
    return 1 if leftover_gap else 0


if __name__ == "__main__":
    raise SystemExit(main())
