#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


@dataclass
class UiPref:
    key: str
    title_ref: str
    title_cn: str
    title_en: str
    xml_file: str


def parse_strings(res_dir: Path) -> tuple[dict[str, str], dict[str, str]]:
    def read_values(values_dir: Path) -> dict[str, str]:
        out: dict[str, str] = {}
        if not values_dir.exists():
            return out
        for f in values_dir.glob("*.xml"):
            try:
                root = ET.parse(f).getroot()
            except ET.ParseError:
                continue
            for n in root.findall("string"):
                name = n.attrib.get("name")
                if not name:
                    continue
                out[name] = "".join(n.itertext()).strip()
        return out

    cn = read_values(res_dir / "values")
    en = read_values(res_dir / "values-en")
    return cn, en


def parse_prefs_xml(res_xml_dir: Path, cn: dict[str, str], en: dict[str, str]) -> dict[str, UiPref]:
    rows: dict[str, UiPref] = {}
    for f in sorted(res_xml_dir.glob("prefs_*.xml")):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        for elem in root.iter():
            key = elem.attrib.get(ANDROID_NS + "key")
            if not key:
                continue
            normalized_key = key[len("pref_key_") :] if key.startswith("pref_key_") else key
            title_ref = elem.attrib.get(ANDROID_NS + "title", "")
            title_cn = ""
            title_en = ""
            if title_ref.startswith("@string/"):
                sid = title_ref.split("/", 1)[1]
                title_cn = cn.get(sid, "")
                title_en = en.get(sid, "")
            rows[normalized_key] = UiPref(
                key=normalized_key,
                title_ref=title_ref,
                title_cn=title_cn,
                title_en=title_en,
                xml_file=f.name,
            )
    return rows


def parse_a14_feature_specs(a14_repo: Path) -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    feature_dir = a14_repo / "app/src/main/java/tv/withaibuild/customiuizer/mods/utils/feature"
    if not feature_dir.exists():
        return out
    text = ""
    for f in sorted(feature_dir.glob("*.kt")):
        text += "\n" + f.read_text(encoding="utf-8", errors="ignore")
    pattern = re.compile(
        r"LazyFeatureSpec\(\s*id\s*=\s*(?P<id>[A-Za-z0-9_]+)\s*,\s*name\s*=\s*\"(?P<name>[^\"]+)\"\s*,\s*preferenceKey\s*=\s*(?P<pref>null|\"[^\"]*\")\s*,\s*target\s*=\s*FeatureTarget\.(?P<target>[A-Z_]+)",
        re.S,
    )
    for m in pattern.finditer(text):
        pref = m.group("pref")
        if pref == "null":
            continue
        key = pref.strip('"')
        out[key] = {
            "a14_feature_id": m.group("id"),
            "a14_feature_name": m.group("name"),
            "host_package": m.group("target"),
        }
    return out


def parse_a13_pref_schema(a13_repo: Path) -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    schema = a13_repo / "app/src/main/java/tv/withaibuild/customiuizer/prefs/PreferenceSchema.kt"
    if not schema.exists():
        return out
    text = schema.read_text(encoding="utf-8", errors="ignore")
    entry_re = re.compile(
        r"PreferenceEntry\(\s*key\s*=\s*\"(?P<key>[^\"]+)\".*?ownerFeature\s*=\s*\"(?P<owner>[^\"]+)\".*?restartTarget\s*=\s*RestartTarget\.(?P<restart>[A-Z_]+)",
        re.S,
    )
    for m in entry_re.finditer(text):
        out[m.group("key")] = {
            "a13_feature_id": m.group("owner"),
            "restart_target": m.group("restart"),
        }
    return out


def classify(a14_key: str, a13_key: str | None, a14_name: str) -> tuple[str, str, str, str]:
    low = (a14_key + " " + a14_name).lower()
    if "dynamic" in low and "island" in low:
        return "INTENTIONAL_EXCLUDED", "A14_NEW_FEATURE", "NO", "Dynamic Island excluded by product policy"
    if a13_key is None:
        return "MISSING_IN_A13", "A14_NEW_FEATURE", "NO", "No A13 key match in UI/schema"
    if a14_key == a13_key:
        return "PRESENT_A13_VARIANT", "UPSTREAM_INTENT_EQUIVALENT", "NO", "Key match; semantic verification required"
    return "PARTIAL_PARITY", "SEMANTIC_DRIFT", "NO", "Mapped via heuristic; verify semantics"


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

    a14_cn, a14_en = parse_strings(a14 / "app/src/main/res")
    a13_cn, a13_en = parse_strings(a13 / "app/src/main/res")
    a14_ui = parse_prefs_xml(a14 / "app/src/main/res/xml", a14_cn, a14_en)
    a13_ui = parse_prefs_xml(a13 / "app/src/main/res/xml", a13_cn, a13_en)
    a14_specs = parse_a14_feature_specs(a14)
    a13_schema = parse_a13_pref_schema(a13)
    a14_java_text = ""
    for src in a14.glob("app/src/main/java/**/*.kt"):
        a14_java_text += "\n" + src.read_text(encoding="utf-8", errors="ignore")

    rows: list[dict[str, str]] = []
    for k, pref in sorted(a14_ui.items()):
        a13_match = k if k in a13_ui or k in a13_schema else ""
        spec = a14_specs.get(k, {})
        parity, src_rel, dyn_excl, ev = classify(k, a13_match or None, spec.get("a14_feature_name", pref.title_en or pref.title_cn))
        rows.append(
            {
                "domain": pref.xml_file.replace("prefs_", "").replace(".xml", ""),
                "a14_feature_id": spec.get("a14_feature_id", f"UI_{k}"),
                "a14_name": spec.get("a14_feature_name", pref.title_en or pref.title_cn or k),
                "a14_pref_keys": k,
                "a13_feature_id": a13_schema.get(k, {}).get("a13_feature_id", f"UI_{a13_match}" if a13_match else ""),
                "a13_pref_keys": a13_match,
                "parity_state": parity,
                "source_relationship": src_rel,
                "risk": "MEDIUM" if parity in {"MISSING_IN_A13", "PARTIAL_PARITY"} else "LOW",
                "priority": "P1" if parity in {"MISSING_IN_A13", "PARTIAL_PARITY"} else "P2",
                "phase_e_batch": "E3" if "launcher" in pref.xml_file or "statusbar" in pref.xml_file else "E2",
                "dynamic_island_excluded": dyn_excl,
                "evidence": ev,
            }
        )

    a14_keys = set(a14_ui.keys())
    for k, pref in sorted(a13_ui.items()):
        if k in a14_keys:
            continue
        rows.append(
            {
                "domain": pref.xml_file.replace("prefs_", "").replace(".xml", ""),
                "a14_feature_id": "",
                "a14_name": "",
                "a14_pref_keys": "",
                "a13_feature_id": a13_schema.get(k, {}).get("a13_feature_id", f"UI_{k}"),
                "a13_pref_keys": k,
                "parity_state": "PRESENT_A13_VARIANT",
                "source_relationship": "A13_COMPAT_VARIANT",
                "risk": "LOW",
                "priority": "P3",
                "phase_e_batch": "",
                "dynamic_island_excluded": "NO",
                "evidence": "A13-only visible preference",
            }
        )

    csv_path = out_dir / "A13_A14_FEATURE_MATRIX.csv"
    fieldnames = [
        "domain",
        "a14_feature_id",
        "a14_name",
        "a14_pref_keys",
        "a13_feature_id",
        "a13_pref_keys",
        "parity_state",
        "source_relationship",
        "risk",
        "priority",
        "phase_e_batch",
        "dynamic_island_excluded",
        "evidence",
    ]
    with csv_path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(rows)

    ui_without_impl = sorted([k for k in a14_ui.keys() if k not in a14_java_text])
    impl_keys = set(re.findall(r'get(?:Boolean|Int|String|StringAsInt)\("([a-z0-9_]+)"', a14_java_text))
    impl_without_ui = sorted([k for k in impl_keys if k not in a14_ui])

    print(f"A14_UI_FEATURES={len(a14_ui)}")
    print(f"A13_UI_FEATURES={len(a13_ui)}")
    print(f"MATRIX_ROWS={len(rows)}")
    print(f"UI_WITHOUT_IMPLEMENTATION={len(ui_without_impl)}")
    print(f"IMPLEMENTATION_WITHOUT_UI={len(impl_without_ui)}")
    print(f"CSV={csv_path}")
    if ui_without_impl:
        print(f"UI_WITHOUT_IMPLEMENTATION_SAMPLE={','.join(ui_without_impl[:20])}")
    if impl_without_ui:
        print(f"IMPLEMENTATION_WITHOUT_UI_SAMPLE={','.join(impl_without_ui[:20])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

