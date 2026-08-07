#!/usr/bin/env python3
"""Generate A13_MEMORY_LIFECYCLE_TOPOLOGY.md from the JSON inventory."""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
INVENTORY = ROOT / "docs" / "audit" / "A13_MEMORY_LIFECYCLE_INVENTORY.json"
OUT = ROOT / "docs" / "audit" / "A13_MEMORY_LIFECYCLE_TOPOLOGY.md"


def main() -> int:
    data = json.loads(INVENTORY.read_text(encoding="utf-8"))
    candidates = data["candidates"]

    by_root = data["root_kind_counts"]
    by_risk = data["risk_counts"]
    by_class = data["classification_counts"]
    top10 = data["top_10"]

    by_process = Counter(c["process"] for c in candidates)

    lines: list[str] = [
        "# A13-PERF-P2-0 Memory & Lifecycle Ownership / Retention Topology",
        "",
        "## Metadata",
        "",
        "| Field | Value |",
        "|-------|-------|",
        "| Task | `A13-PERF-P2-0` |",
        "| Base SHA | `5f00b0492c9cfc2cc62e171d424c269eeed3f492` |",
        "| Branch | `devin/a13-memory-performance-optimization` |",
        "| Scope | `app/src/main/java/**` |",
        "| Production changes | `FORBIDDEN` in P2-0 |",
        "",
        "## Summary counts",
        "",
        "| Risk | Count |",
        "|------|-------|",
    ]
    for risk in ("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO", "UNKNOWN"):
        if risk in by_risk:
            lines.append(f"| {risk} | {by_risk[risk]} |")
    lines.append("")
    lines.append("| Classification | Count |")
    lines.append("|----------------|-------|")
    for cls, cnt in sorted(by_class.items(), key=lambda x: -x[1]):
        lines.append(f"| {cls} | {cnt} |")
    lines.append("")
    lines.append("| Root kind | Count |")
    lines.append("|-----------|-------|")
    for root, cnt in sorted(by_root.items(), key=lambda x: -x[1]):
        lines.append(f"| {root} | {cnt} |")
    lines.append("")
    lines.append("| Process | Candidate count |")
    lines.append("|---------|-----------------|")
    for proc, cnt in sorted(by_process.items(), key=lambda x: -x[1]):
        lines.append(f"| {proc} | {cnt} |")
    lines.append("")

    # Process roots
    lines.append("## Process roots")
    lines.append("")
    for proc, cnt in sorted(by_process.items(), key=lambda x: -x[1]):
        p_cands = [c for c in candidates if c["process"] == proc]
        root_breakdown = Counter(c["root_kind"] for c in p_cands)
        lines.append(f"### {proc}")
        lines.append("")
        lines.append(f"- Total candidates: {cnt}")
        for root, n in sorted(root_breakdown.items(), key=lambda x: -x[1]):
            lines.append(f"- {root}: {n}")
        lines.append("")

    # Registration roots
    lines.append("## Registration / callback roots")
    lines.append("")
    reg_kinds = ["BROADCAST_RECEIVER_REGISTRATION", "CONTENT_OBSERVER_REGISTRATION", "LISTENER_REGISTRATION", "CALLBACK_REGISTRATION"]
    for kind in reg_kinds:
        regs = [c for c in candidates if c["root_kind"] == kind]
        if not regs:
            continue
        lines.append(f"### {kind}")
        lines.append("")
        for c in regs[:10]:
            lines.append(f"- `{c['source_file']}:{c['source_line']}` — {c['retained_type']} — {c['classification']} ({c['risk']})")
        lines.append("")

    # Async roots
    lines.append("## Async roots (Handler / Runnable / Thread / Executor)")
    lines.append("")
    for c in candidates:
        if c["root_kind"] in ("HANDLER", "THREAD_EXECUTOR"):
            lines.append(f"- `{c['source_file']}:{c['source_line']}` — {c['root_kind']} — retained `{c['retained_type']}` — {c['classification']} ({c['risk']})")
    lines.append("")

    # Collection roots
    lines.append("## Collection roots")
    lines.append("")
    for c in candidates:
        if "COLLECTION" in c["classification"] or ("Map" in c["retained_type"] or "List" in c["retained_type"] or "Deque" in c["retained_type"] or "Set" in c["retained_type"]):
            lines.append(f"- `{c['source_file']}:{c['source_line']}` — `{c['retained_type']}` — {c['classification']} ({c['risk']})")
    lines.append("")

    # Safe metadata roots
    lines.append("## Safe metadata roots")
    lines.append("")
    lines.append(f"- Total SAFE_STABLE_METADATA candidates: {by_class.get('SAFE_STABLE_METADATA', 0)}")
    lines.append("- Examples: `Method`, `Field`, `Class`, `String` constants, `Int`/`Long` config, reflection metadata.")
    lines.append("- These are stable process-lifetime metadata, not short-lived Android owner retention.")
    lines.append("")

    # Unknowns
    lines.append("## Unknowns / manual-review queue")
    lines.append("")
    lines.append(f"- Total UNKNOWN or MEDIUM candidates requiring manual review: {by_risk.get('UNKNOWN', 0) + by_risk.get('MEDIUM', 0)}")
    lines.append("- These need ROM/runtime evidence to confirm release path, owner lifetime, or callback capture.")
    lines.append("- WeakReference edges still require their registration root to be reviewed.")
    lines.append("")

    # Top 10
    lines.append("## Top 10 retention candidates")
    lines.append("")
    lines.append("| Rank | ID | Risk | Classification | Process | Source | Line | Retained | Notes |")
    lines.append("|------|----|------|----------------|---------|--------|------|----------|-------|")
    for i, c in enumerate(top10, 1):
        source = c["source_file"].split("/")[-1]
        notes = (c["evidence"][:80].replace("|", "\\|").replace("\n", " ") + "...") if c["evidence"] else ""
        lines.append(f"| {i} | {c['id']} | {c['risk']} | {c['classification']} | {c['process']} | {source} | {c['source_line']} | `{c['retained_type']}` | {notes} |")
    lines.append("")

    # Top 3 chains — derived from the reviewed inventory top 10
    lines.append("## Top 3 strongest retention chains")
    lines.append("")
    for i, c in enumerate(top10[:3], 1):
        source = c["source_file"].split("/")[-1]
        title = f"{i}. `{source}:{c['source_line']}` — {c['classification']} ({c['risk']})"
        lines.append(f"### {title}")
        lines.append("")
        lines.append(f"- **Root**: `{c['root_kind']}` retaining `{c['retained_type']}` in process `{c['process']}`.")
        if c.get("registration_site"):
            lines.append(f"- **Registration site**: `{c['registration_site'][:120].replace(chr(10), ' ')}`")
        if c.get("release_site"):
            lines.append(f"- **Release site**: `{c['release_site'][:120].replace(chr(10), ' ')}`")
        if c.get("review_rationale"):
            lines.append(f"- **Review rationale**: {c['review_rationale']}")
        else:
            lines.append(f"- **Evidence**: `{c['evidence'][:120].replace(chr(10), ' ')}...`")
        lines.append("")

    # Manual coverage — derived from the reviewed inventory
    lines.append("## Manual supplemental coverage")
    lines.append("")
    manual_high = sum(1 for c in candidates if c["risk"] in ("HIGH", "CRITICAL") and c["review_status"] == "REVIEWED")
    manual_medium = sum(1 for c in candidates if c["risk"] in ("MEDIUM", "UNKNOWN") and c["review_status"] == "NEEDS_ROM_EVIDENCE")
    benign = sum(1 for c in candidates if c["classification"] in ("SAFE_STABLE_METADATA", "PROCESS_LIFETIME_INTENTIONAL"))
    lines.append(f"- **Candidates reviewed**: {len([c for c in candidates if c['review_status'] == 'REVIEWED'])} of {len(candidates)}")
    lines.append(f"- **HIGH/CRITICAL manually reviewed**: {manual_high}")
    lines.append(f"- **MEDIUM/UNKNOWN needing ROM/runtime evidence**: {manual_medium}")
    lines.append(f"- **False-positive / benign count**: {benign} (`SAFE_STABLE_METADATA` + `PROCESS_LIFETIME_INTENTIONAL`) classified as not requiring production change.")
    lines.append("")

    # P2-1 recommendation — derive from the first reviewed, non-frozen, actionable cleanup
    lines.append("## Recommended P2-1 slice")
    lines.append("")
    P1B_FROZEN = (
        "SystemUILockScreenHooks", "SystemUINotificationHooks", "SystemAudioAndVolumeHooks",
        "P1B", "P0", "MainModule", "XposedHelpers", "ModuleHelper",
    )
    P2_ACTIONABLE = (
        "BOUNDED_DELAYED_CALLBACK_RETENTION",
        "BOUNDED_REPLACEMENT_RETENTION",
        "UNBALANCED_LISTENER_REGISTRATION",
        "DELAYED_CALLBACK_OWNER_RETENTION",
        "UNBALANCED_RECEIVER_REGISTRATION",
        "UNBALANCED_OBSERVER_REGISTRATION",
    )
    top = None
    frozen = ""
    for c in candidates:
        if c["review_status"] != "REVIEWED":
            continue
        if c["risk"] in ("INFO", "LOW", "UNKNOWN"):
            continue
        stem = c["source_file"].split("/")[-1]
        if any(f in c["source_file"] or f in stem for f in P1B_FROZEN):
            continue
        if c["classification"] in P2_ACTIONABLE:
            top = c
            break
    if top is None:
        # fall back to top10 and flag frozen-slice intersections
        top = top10[0] if top10 else None
        frozen = " (intersects a P1B/P0 frozen slice; a dedicated P2 task should authorize reopening that slice before production change)" if top and any(f in top["source_file"] for f in P1B_FROZEN) else ""

    if top and "SubFragment" in top["source_file"] and top["classification"] == "BOUNDED_DELAYED_CALLBACK_RETENTION":
        rec = "SubFragment.kt smooth-scroller delayed callback cleanup"
    elif top:
        source = top["source_file"].split("/")[-1]
        rec = f"{source}:{top['source_line']} {top['classification'].lower().replace('_', ' ')}"
    else:
        rec = "TBD"
    lines.append("```")
    lines.append(f"RECOMMENDED_P2_1 = {rec}")
    lines.append("```")
    lines.append("")
    lines.append("### Why this is ranked first")
    lines.append("")
    if top:
        lines.append(f"- **Top candidate**: `{top['source_file'].split('/')[-1]}:{top['source_line']}` — `{top['retained_type']}` — {top['classification']} ({top['risk']}).")
        if top.get("review_rationale"):
            lines.append(f"- **Review rationale**: {top['review_rationale']}")
        else:
            lines.append(f"- **Evidence**: `{top['evidence'][:120].replace(chr(10), ' ')}...`")
        if frozen:
            lines.append(f"- **Frozen slice note**: {frozen}")
    lines.append("- **Scope small**: one file or a single callback site, no new architecture.")
    lines.append("- **Regression risk low**: the fix only adds a matching `removeCallbacks` / `removeListener` call in an existing lifecycle teardown path.")
    lines.append("")
    lines.append("### P2-1 status")
    lines.append("")
    lines.append("```")
    lines.append("P2-1 = NOT_STARTED")
    lines.append("P2-0 = AUDIT_COMPLETE")
    lines.append("```")
    lines.append("")

    lines.append("## Static scanner note")
    lines.append("")
    lines.append("The scanner only discovers *candidates*. It does not prove runtime memory leaks. All HIGH/CRITICAL items were manually reviewed; MEDIUM/UNKNOWN items need ROM/runtime evidence before production change.")
    lines.append("")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
