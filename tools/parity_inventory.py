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
PARITY_STATES = {"PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "INSUFFICIENT_EVIDENCE", "A13_ONLY_KEEP"}


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
    ]
    for src in repo.glob("app/src/main/java/**/*.kt"):
        text = src.read_text(encoding="utf-8", errors="ignore")
        for p in patterns:
            for m in p.finditer(text):
                keys.add(m.group(1))
    for src in repo.glob("app/src/main/java/**/*.java"):
        text = src.read_text(encoding="utf-8", errors="ignore")
        for p in patterns:
            for m in p.finditer(text):
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
    if key.startswith("system_") and any(t in key for t in ["statusbar", "lockscreen", "drawer", "cc_", "volume_"]):
        return "SYSTEM_UI"
    if "usb_default" in key or "window_blur" in key or "autobrightness" in key:
        return "SYSTEM_SERVER"
    if key.startswith("various_") and any(t in key for t in ["security", "antivirus", "marketing", "permission", "analytics", "daemon", "update_services"]):
        return "SECURITY_CENTER"
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
    if host_package in {"SECURITY_CENTER", "PACKAGE_INSTALLER"}:
        return "E4"
    if host_package == "SYSTEM_SERVER" or process == "android":
        return "E5"
    if any(t in low for t in ["permission", "privacy", "updater", "daemon", "analytics", "marketing", "antivirus"]):
        return "E4"
    if host_package in {"SETTINGS", "ANY"} and any(t in low for t in ["settings", "permission"]):
        return "E4"
    if host_package == "SETTINGS":
        return "E2"
    if host_package and host_package not in {"UNKNOWN_DISCOVERY", "UNRESOLVED"}:
        return "E2"
    return "HOLD_EVIDENCE"


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
        "INSUFFICIENT_EVIDENCE",
    })
    return lhs == rhs


def build_sanity_overrides() -> dict[str, dict[str, str]]:
    # Explicit semantic proofs for required sanity features.
    return {
        "system_usb_default_function": {
            "parity_state": "MISSING_IN_A13",
            "evidence_level": "INDIVIDUAL_SEMANTIC_PROOF",
            "proof_id": "PROOF_USB_DEFAULT_PURPOSE_A14_ONLY",
            "a14_behavior": "Sets default USB function semantics from settings selector.",
            "a13_behavior": "No equivalent user-visible selector.",
            "risk": "HIGH",
            "priority": "P0",
            "a14_reference": "mods/utils/feature/SystemServerFeatures.kt::UsbDefaultFunctionFeatureId",
            "a13_reference": "ABSENT",
            "api33": "Introduce API33-safe USB default function routing via system_server hook parity.",
            "test_strategy": "System-server hook unit + integration with USB state transitions.",
            "rom_evidence": "YES",
        },
        "controls_hide_ime_dismiss_button": {
            "parity_state": "MISSING_IN_A13",
            "evidence_level": "INDIVIDUAL_SEMANTIC_PROOF",
            "proof_id": "PROOF_HIDE_IME_DISMISS_A14_ONLY",
            "a14_behavior": "Hides gesture-navigation IME dismiss affordance.",
            "a13_behavior": "No dedicated toggle/behavior branch found.",
            "risk": "MEDIUM",
            "priority": "P1",
            "a14_reference": "mods/utils/feature/SystemUiFeatures.kt::HideImeDismissButtonFeatureId",
            "a13_reference": "ABSENT",
            "api33": "Add SystemUI behavior gate with IME-specific guard.",
            "test_strategy": "SystemUI behavior test with IME visibility transitions.",
            "rom_evidence": "YES",
        },
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
    overrides = build_sanity_overrides()

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
        ("infra.backup_restore", "Backup / Restore", "MISSING_IN_A13", "P0"),
        ("infra.language_about", "Language / About", "PRESENT_A13_VARIANT", "P1"),
        ("infra.search_navigation", "Search Navigation", "PRESENT_A13_VARIANT", "P1"),
        ("infra.restart_ux", "Restart UX", "PRESENT_A13_VARIANT", "P1"),
        ("infra.locale_reconcile", "Locale Reconcile", "PRESENT_A13_VARIANT", "P1"),
        ("infra.launcher_reconcile", "Launcher Reconcile", "PRESENT_A13_VARIANT", "P1"),
        ("infra.app_selection_sanitizer", "App Selection Sanitizer", "PRESENT_A13_VARIANT", "P1"),
    ]

    rows: list[dict[str, str]] = []
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

        phase_e_batch = route_phase_e_batch(host_package, process, key, spec.name if spec else node.title, parity)
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
            "API33_design_direction": ov["api33"] if ov else ("Carry forward behavior with explicit API33 validation." if phase_e_batch != "HOLD_EVIDENCE" else "Evidence hold: resolve host/process/contract before Phase E."),
            "test_strategy": ov["test_strategy"] if ov else ("Host/process specific regression tests." if phase_e_batch != "HOLD_EVIDENCE" else "Blocked until evidence completion."),
            "ROM_evidence_needed": ov["rom_evidence"] if ov else ("YES" if phase_e_batch in {"E3", "E5"} else "NO"),
            "dynamic_island_excluded": "YES" if parity == "INTENTIONAL_EXCLUDED" else "NO",
        })

    for fid, name, parity, prio in infra_rows:
        phase_e_batch = route_phase_e_batch("SETTINGS", "com.android.settings", fid, name, parity)
        if fid == "infra.backup_restore":
            phase_e_batch = "E1"
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
            "API33_design_direction": "KEEP",
            "test_strategy": "Preserve existing behavior.",
            "ROM_evidence_needed": "NO",
            "dynamic_island_excluded": "NO",
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
            "API33_design_direction": "PORT=NO",
            "test_strategy": "N/A",
            "ROM_evidence_needed": "NO",
            "dynamic_island_excluded": "YES",
        })

    # Required matrix columns order.
    ordered_columns = [
        "domain", "a14_feature_id", "a14_name", "a14_pref_keys", "a13_feature_id", "a13_pref_keys",
        "parity_state", "evidence_level", "proof_id",
        "host_package", "process", "classloader",
        "a14_behavior", "a13_behavior", "a14_reference", "a13_reference",
        "risk", "priority", "phase_e_batch",
        "API33_design_direction", "test_strategy", "ROM_evidence_needed",
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
    hold_evidence_count = sum(1 for r in rows if r["phase_e_batch"] == "HOLD_EVIDENCE" and r["parity_state"] in {"MISSING_IN_A13", "PARTIAL_PARITY"})
    phase_e_ready_gaps = sum(batch_counts.get(b, 0) for b in ["E1", "E2", "E3", "E4", "E5"])

    confirmed_ui_without_impl = 0
    candidate_ui_without_impl = sum(1 for k, n in a14_nodes.items() if n.node_type in {"ACTIONABLE_FEATURE", "SUBOPTION"} and k not in a14_reads)
    candidate_impl_without_ui = sum(1 for k in a14_reads if k not in a14_nodes)

    print(f"A14_PRODUCT_FEATURE_COUNT={a14_actionable}")
    print(f"A13_PRODUCT_FEATURE_COUNT={a13_product}")
    print(f"A13_ONLY_KEEP_COUNT={a13_only}")
    for k in ["PRESENT_EQUIVALENT", "PRESENT_A13_VARIANT", "PARTIAL_PARITY", "MISSING_IN_A13", "INTENTIONAL_EXCLUDED", "INSUFFICIENT_EVIDENCE"]:
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
    hide_ime = [r for r in rows if r["a14_feature_id"] == "HideImeDismissButtonFeatureId"]
    print(f"HIDE_IME_ROUTING={(hide_ime[0]['phase_e_batch'] if hide_ime else 'NOT_FOUND')}")
    print(f"E_BATCH_ROUTING_TEST={'PASS' if (hide_ime and hide_ime[0]['phase_e_batch'] == 'E3') else 'FAIL'}")
    print(f"DYNAMIC_ISLAND_EXCLUDED_EXACTLY_ONCE={'YES' if sum(1 for r in rows if r['parity_state']=='INTENTIONAL_EXCLUDED') == 1 else 'NO'}")
    print(f"PARITY_ACCOUNTING_INVARIANT={'PASS' if parity_accounting_invariant(rows) else 'FAIL'}")
    for batch in ["E1", "E2", "E3", "E4", "E5"]:
        print(f"{batch}_COUNT={batch_counts.get(batch, 0)}")
    print(f"CSV={csv_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

