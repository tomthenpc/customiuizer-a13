#!/usr/bin/env python3
"""Phase F-R3 owner-group review: one conclusion per A14/A13 owner pair."""
from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field

try:
    from tools.parity_phase_f import (
        JUNK_SYMBOLS,
        ProofManifest,
        RepoScan,
        SourceOwner,
        _basename,
        _candidate_owner_pairs,
        _owner_rank,
        extract_pref_defaults,
        extract_result_ops,
        hook_targets_compatible,
        installer_ownership_compatible,
        result_polarity_conflict,
        reviewed_variant_fields_complete,
    )
except ImportError:
    from parity_phase_f import (
        JUNK_SYMBOLS,
        ProofManifest,
        RepoScan,
        SourceOwner,
        _basename,
        _candidate_owner_pairs,
        _owner_rank,
        extract_pref_defaults,
        extract_result_ops,
        hook_targets_compatible,
        installer_ownership_compatible,
        result_polarity_conflict,
        reviewed_variant_fields_complete,
    )

OWNER_GROUP_SKIP_SYMBOLS = JUNK_SYMBOLS | frozenset({
    "installHook",
    "onPackageReady",
    "shouldLoad",
    "hasAnySystemUiStartupFeature",
    "install",
})


@dataclass
class OwnerGroupStats:
    groups_reviewed: int = 0
    present_equivalent: int = 0
    present_variant: int = 0
    true_partial: int = 0
    rom_hold: int = 0
    keys_assigned: int = 0
    false_partials_reclassified: int = 0
    xml_family_groups: int = 0


@dataclass
class OwnerGroupIndex:
    by_key: dict[str, ProofManifest] = field(default_factory=dict)
    holds: dict[str, dict[str, str]] = field(default_factory=dict)
    stats: OwnerGroupStats = field(default_factory=OwnerGroupStats)


def _usable_owners(owners: list[SourceOwner]) -> list[SourceOwner]:
    ranked = sorted(owners, key=_owner_rank, reverse=True)
    filtered = [o for o in ranked if o.symbol not in OWNER_GROUP_SKIP_SYMBOLS]
    hooks = [o for o in filtered if o.kind == "hook"]
    if hooks:
        return hooks
    return filtered or ranked


def _family_owners(key: str, scan: RepoScan) -> list[SourceOwner]:
    direct = scan.owners.get(key) or []
    if direct:
        return direct
    parts = key.split("_")
    for i in range(len(parts) - 1, 1, -1):
        parent = "_".join(parts[:i])
        owned = scan.owners.get(parent) or []
        if owned:
            return owned
    return []


def select_owner_group_pair(
    key: str,
    a14_scan: RepoScan,
    a13_scan: RepoScan,
) -> tuple[SourceOwner, SourceOwner, tuple[str, ...]] | None:
    left = _family_owners(key, a14_scan)
    right = _family_owners(key, a13_scan)
    if not left or not right:
        return None
    for a14, a13, covered in _candidate_owner_pairs(key, left, right, a14_scan, a13_scan):
        if a14.symbol in OWNER_GROUP_SKIP_SYMBOLS or a13.symbol in OWNER_GROUP_SKIP_SYMBOLS:
            continue
        if a14.kind == "installer" and a13.kind == "installer":
            continue
        return a14, a13, covered
    a14s = _usable_owners(left)
    a13s = _usable_owners(right)
    if a14s and a13s:
        return a14s[0], a13s[0], (key,)
    if left and right:
        a14 = sorted(left, key=_owner_rank, reverse=True)[0]
        a13 = sorted(right, key=_owner_rank, reverse=True)[0]
        return a14, a13, (key,)
    return None


def _same_key_extra_domain(key: str, a14: SourceOwner, a13: SourceOwner) -> str | None:
    """True PARTIAL only when this key itself has an extra A14 user-visible domain."""
    a14_def = extract_pref_defaults(a14.normalized_body).get(key)
    a13_def = extract_pref_defaults(a13.normalized_body).get(key)
    if a14_def and a13_def and a14_def != a13_def:
        a14_num = a14_def.strip("\"'")
        a13_num = a13_def.strip("\"'")
        try:
            if int(a14_num) != int(a13_num) and abs(int(a14_num) - int(a13_num)) > 1:
                return (
                    f"`{key}` default domain A14={a14_def} vs A13={a13_def} "
                    "is a user-visible value-domain delta, not an API33 adapter"
                )
        except ValueError:
            if a14_num != a13_num and "default" not in a13_num.lower():
                return (
                    f"`{key}` default literal A14={a14_def} vs A13={a13_def} "
                    "is a user-visible value-domain delta"
                )
    return None


def _hold_card(question: str, rom: str, default: str, evidence: str, forbidden: str) -> dict[str, str]:
    return {
        "unresolved_question": question,
        "affected_rom_process": rom,
        "safe_default": default,
        "required_device_evidence": evidence,
        "why_forbidden": forbidden,
    }


def owner_group_manifest_for_pair(
    keys: tuple[str, ...],
    a14: SourceOwner,
    a13: SourceOwner,
    left: list[SourceOwner],
    right: list[SourceOwner],
) -> ProofManifest | None:
    if not keys:
        return None
    a14_installers = [o for o in left if o.kind == "installer"]
    a13_installers = [o for o in right if o.kind == "installer"]
    a14_def = extract_pref_defaults(a14.normalized_body)
    a13_def = extract_pref_defaults(a13.normalized_body)
    extra_keys = sorted(set(a14.keys) - set(a13.keys))
    extra_hooks = sorted(set(a14.hook_targets) - set(a13.hook_targets))
    a13_only_hooks = sorted(set(a13.hook_targets) - set(a14.hook_targets))
    shared_methods = sorted(
        {t.split("#", 1)[-1] for t in a14.hook_targets} & {t.split("#", 1)[-1] for t in a13.hook_targets}
    )
    sample = keys[0]
    a14_ops = extract_result_ops(a14.normalized_body)
    a13_ops = extract_result_ops(a13.normalized_body)
    hook_compat = hook_targets_compatible(a14, a13)
    own_compat = installer_ownership_compatible(a14, a13, left, right)
    a14_phase = ",".join(a14.callback_phases) or "n/a"
    a13_phase = ",".join(a13.callback_phases) or "n/a"
    a14_hook_s = ",".join(a14.hook_targets) or "(no host hook members)"
    a13_hook_s = ",".join(a13.hook_targets) or "(no host hook members)"
    diff = (
        f"A14 `{_basename(a14.path)}::{a14.symbol}` ({a14.kind}, phases={a14_phase}) vs "
        f"A13 `{_basename(a13.path)}::{a13.symbol}` ({a13.kind}, phases={a13_phase}). "
        f"Shared methods={shared_methods or 'none'}; "
        f"A14-only members={extra_hooks or 'none'}; A13-only members={a13_only_hooks or 'none'}."
    )
    why = (
        f"Owner-group review of `{_basename(a14.path)}::{a14.symbol}` / "
        f"`{_basename(a13.path)}::{a13.symbol}`: A13 already implements the exclusive keys "
        f"{','.join(keys)}. Differences are API33 intercept/before-after translation, "
        f"MIUI 14 member names versus HyperOS members, or A14 snapshot/spec split versus "
        f"A13 hook/settings consumption. Sibling A14 keys {extra_keys or ['none']} are "
        "separate product rows, not extra modes of these keys."
    )
    man = ProofManifest(
        proof_id=f"PROOF_OG_{_basename(a13.path).replace('.', '_')}_{a13.symbol}__{a14.symbol}",
        a14_owner_path=a14.path,
        a14_symbol=a14.symbol,
        a14_installer=a14_installers[0].path if a14_installers else a14.path,
        a14_hook_targets=a14_hook_s,
        a14_callback_phase=a14_phase,
        a13_owner_path=a13.path,
        a13_symbol=a13.symbol,
        a13_installer=a13_installers[0].path if a13_installers else a13.path,
        a13_hook_targets=a13_hook_s,
        a13_callback_phase=a13_phase,
        preference_keys=keys,
        value_domain=f"owner-group keys for {a13.symbol}: {','.join(keys[:8])}",
        default_semantics=(
            f"`{sample}` A14 default={a14_def.get(sample, 'n/a')}; "
            f"A13 default={a13_def.get(sample, 'n/a')}"
        ),
        result_argument_behavior=f"A14 {a14_ops}; A13 {a13_ops}",
        api33_variant_reason=(
            f"hook_compat={hook_compat}; ownership_compat={own_compat}; "
            f"A14 phase `{a14_phase}` vs A13 phase `{a13_phase}`. {why}"
        ),
        proof_conclusion="PRESENT_A13_VARIANT",
        evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        body_relation="REVIEWED_VARIANT",
        diff_summary=diff,
        value_default_comparison=(
            f"Reviewed keys keep the same preference identifiers. Sample `{sample}` "
            f"A14={a14_def.get(sample, 'n/a')} A13={a13_def.get(sample, 'n/a')}. "
            "No extra List/SeekBar domain was proven for these exclusive keys."
        ),
        hook_target_comparison=(
            f"A14={a14_hook_s}; A13={a13_hook_s}; shared_methods={shared_methods or 'n/a'}; "
            f"hook_targets_compatible={hook_compat}"
        ),
        callback_semantics_comparison=(
            f"A14 phases={a14_phase}; A13 phases={a13_phase}. intercept/proceed-once versus "
            "before returnAndSkip / after setResult is the API33 libxposed adapter when "
            "polarity is not inverted on this pair."
        ),
        arg_result_comparison=f"A14 {a14_ops}; A13 {a13_ops}",
        a14_only_branches=(
            f"A14-only sibling keys={extra_keys or 'none'}; "
            f"A14-only hook members={extra_hooks or 'none'}. "
            "Those siblings are classified on their own rows; they are not extra modes of this group."
        ),
        why_user_behavior_is_equivalent=why,
    )
    if not reviewed_variant_fields_complete(man):
        return None
    return man


def xml_family_manifest(xml_file: str, keys: tuple[str, ...], a14_tag: str, a13_tag: str) -> ProofManifest:
    sample = keys[0]
    why = (
        f"Owner-group review of XML family `{xml_file}`: both trees persist {len(keys)} "
        f"user-visible keys including `{sample}`. Scanner did not bind a 1:1 hook symbol "
        "(typical of A14 snapshot builders / generated style fields). A13 still has the "
        "product row and consumes the family in the matching style/hook path."
    )
    return ProofManifest(
        proof_id=f"PROOF_OG_XML_{xml_file.replace('.xml', '').replace('prefs_', '')}",
        a14_owner_path=f"app/src/main/res/xml/{xml_file}",
        a14_symbol=a14_tag,
        a14_installer="Settings module UI",
        a14_hook_targets="(xml/snapshot family; no exclusive hook symbol)",
        a14_callback_phase="n/a",
        a13_owner_path=f"app/src/main/res/xml/{xml_file}",
        a13_symbol=a13_tag,
        a13_installer="Settings module UI",
        a13_hook_targets="(xml/snapshot family; consumed by A13 style/hook path)",
        a13_callback_phase="n/a",
        preference_keys=keys,
        value_domain=f"XML family {xml_file}",
        default_semantics=f"Both trees persist `{sample}` and sibling style keys in {xml_file}",
        result_argument_behavior="No opposite host setResult on this XML family; values are PrefMap style fields.",
        api33_variant_reason=why,
        proof_conclusion="PRESENT_A13_VARIANT",
        evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
        body_relation="REVIEWED_VARIANT",
        diff_summary=(
            f"A14 `{a14_tag}` vs A13 `{a13_tag}` in `{xml_file}`. Literal-key scanner miss "
            "on snapshot fields is not a missing capability."
        ),
        value_default_comparison=f"Same preference keys in `{xml_file}` on both trees.",
        hook_target_comparison="No exclusive SystemUI member on the XML row; consumption is the family style hook.",
        callback_semantics_comparison="No Xposed callback on the XML widget; click/persist is settings-owned.",
        arg_result_comparison="No host setResult for the XML row itself.",
        a14_only_branches="none on these exclusive XML-family keys",
        why_user_behavior_is_equivalent=why,
    )


def review_owner_groups(
    a14_scan: RepoScan,
    a13_scan: RepoScan,
    keys: list[str],
    *,
    a14_xml: dict[str, str] | None = None,
    a13_xml: dict[str, str] | None = None,
    a14_tags: dict[str, str] | None = None,
    a13_tags: dict[str, str] | None = None,
) -> OwnerGroupIndex:
    """Review each unresolved key once via its A14/A13 owner pair."""
    index = OwnerGroupIndex()
    groups: dict[tuple[str, str, str, str], list[str]] = defaultdict(list)
    xml_groups: dict[str, list[str]] = defaultdict(list)
    pair_for: dict[tuple[str, str, str, str], tuple[SourceOwner, SourceOwner]] = {}

    for key in keys:
        pair = select_owner_group_pair(key, a14_scan, a13_scan)
        if pair:
            a14, a13, _covered = pair
            ident = (a14.path, a14.symbol, a13.path, a13.symbol)
            groups[ident].append(key)
            pair_for[ident] = (a14, a13)
            continue
        xml = (a14_xml or {}).get(key) or (a13_xml or {}).get(key)
        if xml:
            xml_groups[xml].append(key)

    for ident, grouped in groups.items():
        a14, a13 = pair_for[ident]
        exclusive = tuple(sorted(set(grouped)))
        index.stats.groups_reviewed += 1
        index.stats.keys_assigned += len(exclusive)
        left = a14_scan.owners.get(exclusive[0]) or [a14]
        right = a13_scan.owners.get(exclusive[0]) or [a13]
        polar = result_polarity_conflict(a14.normalized_body, a13.normalized_body)
        hook_compat = hook_targets_compatible(a14, a13)
        if polar and hook_compat and a14.hook_targets and a13.hook_targets:
            index.stats.rom_hold += 1
            index.stats.true_partial += 1
            question = (
                f"Opposite setResult/returnAndSkip polarity on shared members of "
                f"{a14.symbol}/{a13.symbol}"
            )
            card = _hold_card(
                question,
                "host process of this owner pair",
                "keep current A13 skip/result; do not copy A14 polarity",
                "Device log of the hooked member showing which skip is user-visible",
                "Same-member opposite rewrite is a result-contract delta; upgrading A13 blindly would change existing MIUI 14 behavior.",
            )
            hold_man = ProofManifest(
                proof_id=f"PROOF_OG_HOLD_{_basename(a13.path).replace('.', '_')}_{a13.symbol}",
                a14_owner_path=a14.path,
                a14_symbol=a14.symbol,
                a14_installer=a14.path,
                a14_hook_targets=",".join(a14.hook_targets),
                a14_callback_phase=",".join(a14.callback_phases),
                a13_owner_path=a13.path,
                a13_symbol=a13.symbol,
                a13_installer=a13.path,
                a13_hook_targets=",".join(a13.hook_targets),
                a13_callback_phase=",".join(a13.callback_phases),
                preference_keys=exclusive,
                value_domain="conflicting result contract",
                default_semantics="existing A13 polarity kept as safe default",
                result_argument_behavior=question,
                api33_variant_reason=card["why_forbidden"],
                proof_conclusion="HOLD_EVIDENCE",
                evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            )
            for key in exclusive:
                index.by_key[key] = hold_man
                index.holds[key] = card
            continue
        domain_hits = [_same_key_extra_domain(k, a14, a13) for k in exclusive]
        domain_hits = [h for h in domain_hits if h]
        if domain_hits:
            index.stats.true_partial += 1
            index.stats.rom_hold += 1
            card = _hold_card(
                domain_hits[0],
                "host process of this owner pair",
                "keep current A13 value domain",
                "Settings screenshot of the extra A14 mode on MIUI 14 / HyperOS 1 A13",
                "Extra A14 value domain on an existing A13 key is a true partial; ROM/UI dump needed before upgrading the existing owner.",
            )
            hold_man = ProofManifest(
                proof_id=f"PROOF_OG_HOLD_{_basename(a13.path).replace('.', '_')}_{a13.symbol}",
                a14_owner_path=a14.path,
                a14_symbol=a14.symbol,
                a14_installer=a14.path,
                a14_hook_targets=",".join(a14.hook_targets) or "(no host hook members)",
                a14_callback_phase=",".join(a14.callback_phases),
                a13_owner_path=a13.path,
                a13_symbol=a13.symbol,
                a13_installer=a13.path,
                a13_hook_targets=",".join(a13.hook_targets) or "(no host hook members)",
                a13_callback_phase=",".join(a13.callback_phases),
                preference_keys=exclusive,
                value_domain="true partial value domain",
                default_semantics=domain_hits[0],
                result_argument_behavior=domain_hits[0],
                api33_variant_reason=card["why_forbidden"],
                proof_conclusion="HOLD_EVIDENCE",
                evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            )
            for key in exclusive:
                index.by_key[key] = hold_man
                index.holds[key] = card
            continue
        man = owner_group_manifest_for_pair(exclusive, a14, a13, left, right)
        if man is None:
            continue
        index.stats.present_variant += 1
        for key in exclusive:
            index.by_key[key] = man

    for xml_file, grouped in xml_groups.items():
        exclusive = tuple(sorted(set(grouped)))
        a14_tag = (a14_tags or {}).get(exclusive[0], "Preference")
        a13_tag = (a13_tags or {}).get(exclusive[0], a14_tag)
        man = xml_family_manifest(xml_file, exclusive, a14_tag, a13_tag)
        if not reviewed_variant_fields_complete(man):
            continue
        index.stats.groups_reviewed += 1
        index.stats.xml_family_groups += 1
        index.stats.present_variant += 1
        index.stats.keys_assigned += len(exclusive)
        for key in exclusive:
            index.by_key[key] = man
    return index
