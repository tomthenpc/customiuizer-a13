#!/usr/bin/env python3
"""Phase F-R4 owner-group discovery: direct ownership only; no auto PRESENT."""
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
        _owner_rank,
        reviewed_variant_fields_complete,
    )
except ImportError:
    from parity_phase_f import (
        JUNK_SYMBOLS,
        ProofManifest,
        RepoScan,
        SourceOwner,
        _basename,
        _owner_rank,
        reviewed_variant_fields_complete,
    )

OWNER_GROUP_SKIP_SYMBOLS = JUNK_SYMBOLS | frozenset({
    "installHook",
    "onPackageReady",
    "shouldLoad",
    "hasAnySystemUiStartupFeature",
    "install",
})

ALLOWED_EVIDENCE = frozenset({
    "LITERAL_READ",
    "INSTALLER_CALLEE",
    "FEATURE_SPEC",
    "SNAPSHOT_FIELD",
    "EXPLICIT_ALIAS",
})

FALSE_OWNER_ASSIGNMENTS_REMOVED = (
    ("NoFingerprintWakeHook", "controls_fingerprintfailure"),
    ("NoFingerprintWakeHook", "controls_fingerprintscreen"),
    ("NoFingerprintWakeHook", "controls_powerflash"),
    ("NoFingerprintWakeHook", "controls_volumedowndt_torch"),
    ("FingerprintHapticSuccessHook", "system_blocktoasts"),
    ("FingerprintHapticSuccessHook", "system_nolightuponcharges"),
    ("FingerprintHapticSuccessHook", "system_vibration"),
)


@dataclass(frozen=True)
class OwnershipRecord:
    owner: SourceOwner
    evidence_kind: str
    reference: str


@dataclass(frozen=True)
class TrueOwnerGroup:
    a14_path: str
    a14_symbol: str
    a13_path: str
    a13_symbol: str
    a14_owner: SourceOwner
    a13_owner: SourceOwner
    keys: tuple[str, ...]
    evidence_by_key: dict[str, tuple[OwnershipRecord, OwnershipRecord]]


@dataclass
class OwnerGroupStats:
    groups_discovered: int = 0
    groups_reviewed: int = 0
    present_equivalent: int = 0
    present_variant: int = 0
    true_partial: int = 0
    rom_hold: int = 0
    keys_assigned: int = 0
    keys_with_direct_owner_evidence: int = 0
    keys_with_explicit_alias_evidence: int = 0
    xml_only_unproven_keys: int = 0
    false_partials_reclassified: int = 0
    xml_family_groups: int = 0


@dataclass
class OwnerGroupIndex:
    by_key: dict[str, ProofManifest] = field(default_factory=dict)
    holds: dict[str, dict[str, str]] = field(default_factory=dict)
    discovered: list[TrueOwnerGroup] = field(default_factory=list)
    stats: OwnerGroupStats = field(default_factory=OwnerGroupStats)
    evidence_by_key: dict[str, tuple[OwnershipRecord, OwnershipRecord]] = field(default_factory=dict)


def forbidden_prefix_fallback_owners(key: str, scan: RepoScan) -> list[SourceOwner]:
    """Parent-prefix walk. Must never be used to assign semantic ownership."""
    parts = key.split("_")
    for i in range(len(parts) - 1, 0, -1):
        parent = "_".join(parts[:i])
        owned = scan.owners.get(parent) or []
        if owned:
            return owned
    return []


def _usable(owner: SourceOwner) -> bool:
    return owner.symbol not in OWNER_GROUP_SKIP_SYMBOLS


def literal_read_owners(key: str, scan: RepoScan) -> list[SourceOwner]:
    return [
        o for o in (scan.owners.get(key) or [])
        if _usable(o) and o.kind == "hook" and key in o.keys
    ]


def installer_callee_owners(key: str, scan: RepoScan) -> list[SourceOwner]:
    found: list[SourceOwner] = []
    seen: set[tuple[str, str]] = set()
    for callee in scan.callees.get(key) or []:
        for owner in scan.symbols.get(callee) or []:
            ident = (owner.path, owner.symbol)
            if ident in seen or not _usable(owner) or owner.kind != "hook":
                continue
            seen.add(ident)
            found.append(owner)
    return found


def feature_spec_owners(key: str, scan: RepoScan) -> list[SourceOwner]:
    return [
        o for o in (scan.owners.get(key) or [])
        if _usable(o) and o.kind == "spec" and key in o.keys
    ]


def true_owner_candidates(key: str, scan: RepoScan) -> list[OwnershipRecord]:
    records: list[OwnershipRecord] = []
    seen: set[tuple[str, str, str]] = set()

    def add(owner: SourceOwner, kind: str) -> None:
        ident = (owner.path, owner.symbol, kind)
        if ident in seen:
            return
        seen.add(ident)
        records.append(OwnershipRecord(
            owner=owner,
            evidence_kind=kind,
            reference=f"{owner.path}::{owner.symbol} ({kind})",
        ))

    for owner in installer_callee_owners(key, scan):
        add(owner, "INSTALLER_CALLEE")
    for owner in literal_read_owners(key, scan):
        add(owner, "LITERAL_READ")
    for owner in feature_spec_owners(key, scan):
        add(owner, "FEATURE_SPEC")
    return records


def pair_true_owners(
    key: str,
    a14_scan: RepoScan,
    a13_scan: RepoScan,
) -> tuple[OwnershipRecord, OwnershipRecord] | None:
    """Same-symbol or same-file literal consumers only. No ranked-first fallback."""
    left = true_owner_candidates(key, a14_scan)
    right = true_owner_candidates(key, a13_scan)
    if not left or not right:
        return None
    right_by_symbol = {rec.owner.symbol: rec for rec in right}
    for rec in left:
        hit = right_by_symbol.get(rec.owner.symbol)
        if hit:
            return rec, hit
    for a14 in left:
        for a13 in right:
            if _basename(a14.owner.path) != _basename(a13.owner.path):
                continue
            if key in a14.owner.keys and key in a13.owner.keys:
                return a14, a13
    return None


def ranked_first_owner(owners: list[SourceOwner]) -> SourceOwner | None:
    """Highest _owner_rank. Must never prove semantic ownership."""
    if not owners:
        return None
    return sorted(owners, key=_owner_rank, reverse=True)[0]


def discover_true_owner_groups(
    keys: list[str],
    a14_scan: RepoScan,
    a13_scan: RepoScan,
) -> tuple[list[TrueOwnerGroup], dict[str, tuple[OwnershipRecord, OwnershipRecord]], list[str]]:
    grouped: dict[tuple[str, str, str, str], list[str]] = defaultdict(list)
    evidence: dict[str, tuple[OwnershipRecord, OwnershipRecord]] = {}
    xml_only: list[str] = []
    for key in keys:
        pair = pair_true_owners(key, a14_scan, a13_scan)
        if not pair:
            xml_only.append(key)
            continue
        a14_rec, a13_rec = pair
        ident = (a14_rec.owner.path, a14_rec.owner.symbol, a13_rec.owner.path, a13_rec.owner.symbol)
        grouped[ident].append(key)
        evidence[key] = (a14_rec, a13_rec)

    groups: list[TrueOwnerGroup] = []
    for ident, grouped_keys in grouped.items():
        exclusive = tuple(sorted(set(grouped_keys)))
        a14_rec, a13_rec = evidence[exclusive[0]]
        groups.append(TrueOwnerGroup(
            a14_path=ident[0],
            a14_symbol=ident[1],
            a13_path=ident[2],
            a13_symbol=ident[3],
            a14_owner=a14_rec.owner,
            a13_owner=a13_rec.owner,
            keys=exclusive,
            evidence_by_key={k: evidence[k] for k in exclusive},
        ))
    groups.sort(key=lambda g: (g.a13_path, g.a13_symbol, g.keys))
    return groups, evidence, xml_only


def group_keys_for_symbol(groups: list[TrueOwnerGroup], symbol: str) -> tuple[str, ...]:
    keys: list[str] = []
    for group in groups:
        if group.a13_symbol == symbol or group.a14_symbol == symbol:
            keys.extend(group.keys)
    return tuple(sorted(set(keys)))


def _ownership_fields(keys: tuple[str, ...], evidence: dict[str, tuple[OwnershipRecord, OwnershipRecord]]) -> tuple[str, str, str]:
    lines = []
    a14_refs = []
    a13_refs = []
    for key in keys:
        pair = evidence.get(key)
        if not pair:
            continue
        a14_rec, a13_rec = pair
        lines.append(
            f"{key}: A14={a14_rec.reference}; A13={a13_rec.reference}"
        )
        a14_refs.append(f"{key}={a14_rec.reference}")
        a13_refs.append(f"{key}={a13_rec.reference}")
    return "; ".join(lines), "; ".join(a14_refs), "; ".join(a13_refs)


def explicit_reviewed_owner_groups() -> list[ProofManifest]:
    """Human-reviewed non-identical owner pairs. One pair closes only keys it consumes."""
    return [
        ProofManifest(
            proof_id="PROOF_OG_NO_FINGERPRINT_WAKE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a14_symbol="NoFingerprintWakeHook",
            a14_installer="A14 SystemServer feature / installer condition controls_fingerprintwake",
            a14_hook_targets="com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent",
            a14_callback_phase="intercept",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a13_symbol="NoFingerprintWakeHook",
            a13_installer="installers/SystemServerInstaller.java if controls_fingerprintwake",
            a13_hook_targets="com.android.server.policy.MiuiPhoneWindowManager#processBackFingerprintDpcenterEvent",
            a13_callback_phase="before",
            preference_keys=("controls_fingerprintwake",),
            value_domain="boolean; default false; installer gate only (hook body has no pref read)",
            default_semantics="off keeps ROM back-fingerprint wake-when-screen-off",
            result_argument_behavior=(
                "When the hooked method's screen-on boolean is false, skip the original "
                "processBackFingerprintDpcenterEvent so a back-fingerprint tap does not wake the device. "
                "When screen-on is true, the original method runs."
            ),
            api33_variant_reason=(
                "A14 intercept: if !isScreenOn skip with null and do not chain.proceed(); else one proceed. "
                "A13 before: if !isScreenOn returnAndSkip(null). Same member, same screen-on test, same skip."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary=(
                "Shared: MiuiPhoneWindowManager.processBackFingerprintDpcenterEvent(KeyEvent, boolean); "
                "skip original when arg1 is false. Differ: A14 intercept/proceed-once vs A13 before skip. "
                "No extra A14 user-visible branch on controls_fingerprintwake."
            ),
            value_default_comparison="Both use a boolean installer gate; neither introduces extra modes.",
            hook_target_comparison="Same class and member on both trees.",
            callback_semantics_comparison=(
                "A14 intercept skip-or-proceed-once maps to A13 before returnAndSkip; the original is not "
                "invoked on the skipped path on either tree."
            ),
            arg_result_comparison="Skipped result is null; the KeyEvent is not rewritten.",
            a14_only_branches="none for this key; intercept scaffolding only",
            why_user_behavior_is_equivalent=(
                "User contract is: with the toggle on, a back-fingerprint press while the screen is off "
                "does not wake the device. That is the same skip on the same ROM method."
            ),
            key_ownership_evidence="controls_fingerprintwake: INSTALLER_CALLEE on both trees (SystemServerInstaller / A14 installer condition → NoFingerprintWakeHook)",
            a14_key_owner_reference="Controls.kt::NoFingerprintWakeHook INSTALLER_CALLEE controls_fingerprintwake",
            a13_key_owner_reference="Controls.kt::NoFingerprintWakeHook INSTALLER_CALLEE via SystemServerInstaller.java",
        ),
        ProofManifest(
            proof_id="PROOF_OG_FINGERPRINT_HAPTIC_SUCCESS",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a14_symbol="FingerprintHapticSuccessHook",
            a14_installer="installer condition controls_fingerprintsuccess > 1",
            a14_hook_targets="com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated",
            a14_callback_phase="intercept",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a13_symbol="FingerprintHapticSuccessHook",
            a13_installer="installers/SystemServerInstaller.java if controls_fingerprintsuccess > 1",
            a13_hook_targets="com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated",
            a13_callback_phase="after",
            preference_keys=("controls_fingerprintsuccess", "controls_fingerprintsuccess_ignore"),
            value_domain="string-int 1=ROM default, 2=light haptic, 3=strong haptic; controls_fingerprintsuccess_ignore is a boolean helper consumed by this same hook",
            default_semantics="default `1` keeps ROM success haptic; 2/3 replace it",
            result_argument_behavior=(
                "After AuthenticationClient.onAuthenticated, if mAuthSuccess: opt 2 light vibration, "
                "opt 3 strong vibration; ignoreSystem comes from controls_fingerprintsuccess_ignore. "
                "The authentication result is not rewritten."
            ),
            api33_variant_reason=(
                "A14 intercept always chain.proceed() once then runs the haptic side-effect. "
                "A13 after runs the same haptic side-effect. No skip of onAuthenticated."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary=(
                "Shared: AuthenticationClient.onAuthenticated; mAuthSuccess gate; getString(controls_fingerprintsuccess) "
                "2/3 haptic; ignore boolean. Differ: A14 intercept+proceed then haptic vs A13 after haptic. "
                "A14 uses toInt(); A13 uses toIntOrNull()?:1. Unknown values keep default 1 on A13; A14 toInt() can throw and is caught."
            ),
            value_default_comparison="Both default the visible list to 1 (keep ROM). The ignore helper is consumed by this same hook, not by system_vibration/toast owners.",
            hook_target_comparison="Same AuthenticationClient.onAuthenticated member.",
            callback_semantics_comparison="A14 proceed-once then side-effect equals A13 after side-effect; neither returnAndSkip.",
            arg_result_comparison="Host return value unchanged; only vibrator side-effect after success.",
            a14_only_branches="none that add a fourth haptic mode; intercept wrapper only",
            why_user_behavior_is_equivalent=(
                "The user-visible control is success-haptic strength. Both trees apply light/strong vibration "
                "only on authenticated success and leave ROM behavior at value 1."
            ),
            key_ownership_evidence=(
                "controls_fingerprintsuccess: LITERAL_READ + INSTALLER_CALLEE in FingerprintHapticSuccessHook; "
                "controls_fingerprintsuccess_ignore: LITERAL_READ in the same hook (ignoreSystem). "
                "system_blocktoasts / system_nolightuponcharges / system_vibration are not consumed here."
            ),
            a14_key_owner_reference="Controls.kt::FingerprintHapticSuccessHook LITERAL_READ controls_fingerprintsuccess,controls_fingerprintsuccess_ignore",
            a13_key_owner_reference="Controls.kt::FingerprintHapticSuccessHook LITERAL_READ + SystemServerInstaller INSTALLER_CALLEE",
        ),
        ProofManifest(
            proof_id="PROOF_OG_FINGERPRINT_HAPTIC_FAILURE",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a14_symbol="FingerprintHapticFailureHook",
            a14_installer="installer condition controls_fingerprintfailure",
            a14_hook_targets="com.android.server.biometrics.sensors.AcquisitionClient#vibrateError",
            a14_callback_phase="intercept",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a13_symbol="FingerprintHapticFailureHook",
            a13_installer="installers/SystemServerInstaller.java if controls_fingerprintfailure",
            a13_hook_targets="com.android.server.biometrics.sensors.AcquisitionClient#vibrateError",
            a13_callback_phase="before",
            preference_keys=("controls_fingerprintfailure",),
            value_domain="boolean; default false",
            default_semantics="off keeps ROM error vibration",
            result_argument_behavior="Skip AcquisitionClient.vibrateError entirely so fingerprint failure does not vibrate.",
            api33_variant_reason=(
                "A14 intercept sets skipped=true result=null and never proceeds. "
                "A13 before returnAndSkip(null). Same skip of vibrateError."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary="Same member vibrateError skipped unconditionally once the installer gate is on. Callback adapter only.",
            value_default_comparison="Boolean gate on both trees; no extra failure-haptic modes.",
            hook_target_comparison="Same AcquisitionClient.vibrateError.",
            callback_semantics_comparison="A14 intercept skip vs A13 before returnAndSkip; original not called.",
            arg_result_comparison="Skipped result null; no argument rewrite.",
            a14_only_branches="none for this key",
            why_user_behavior_is_equivalent="Toggle on means no error haptic on fingerprint failure. Same skipped ROM method.",
            key_ownership_evidence="controls_fingerprintfailure: INSTALLER_CALLEE → FingerprintHapticFailureHook on both trees",
            a14_key_owner_reference="Controls.kt::FingerprintHapticFailureHook INSTALLER_CALLEE",
            a13_key_owner_reference="Controls.kt::FingerprintHapticFailureHook INSTALLER_CALLEE via SystemServerInstaller.java",
        ),
        ProofManifest(
            proof_id="PROOF_OG_FINGERPRINT_SCREEN_ON",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a14_symbol="FingerprintScreenOnHook",
            a14_installer="installer condition controls_fingerprintscreen",
            a14_hook_targets="com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated",
            a14_callback_phase="intercept",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a13_symbol="FingerprintScreenOnHook",
            a13_installer="installers/SystemServerInstaller.java if controls_fingerprintscreen",
            a13_hook_targets="com.android.server.biometrics.sensors.AuthenticationClient#onAuthenticated",
            a13_callback_phase="after",
            preference_keys=("controls_fingerprintscreen",),
            value_domain="boolean; default false",
            default_semantics="off keeps ROM screen-off fingerprint failure behavior",
            result_argument_behavior=(
                "After onAuthenticated, if authentication failed and PowerManager is not interactive, "
                "send WakeUp. Success path does not wake. Authentication result is unchanged."
            ),
            api33_variant_reason=(
                "A14 intercept proceeds once then applies the wake side-effect. A13 after applies the same "
                "mAuthSuccess / isInteractive tests and WakeUp. No extra A14 wake condition."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary="Shared: onAuthenticated; skip wake on success or if already interactive; WakeUp on failed auth while screen off. Adapter: intercept vs after.",
            value_default_comparison="Boolean installer gate on both trees.",
            hook_target_comparison="Same AuthenticationClient.onAuthenticated.",
            callback_semantics_comparison="A14 proceed-once then side-effect equals A13 after side-effect.",
            arg_result_comparison="Host result unchanged; WakeUp is a GlobalActions side-effect.",
            a14_only_branches="none for this key",
            why_user_behavior_is_equivalent="Failed fingerprint while the screen is off wakes the device. Same tests and action.",
            key_ownership_evidence="controls_fingerprintscreen: INSTALLER_CALLEE → FingerprintScreenOnHook on both trees",
            a14_key_owner_reference="Controls.kt::FingerprintScreenOnHook INSTALLER_CALLEE",
            a13_key_owner_reference="Controls.kt::FingerprintScreenOnHook INSTALLER_CALLEE via SystemServerInstaller.java",
        ),
        ProofManifest(
            proof_id="PROOF_OG_POWER_KEY_FLASH",
            a14_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a14_symbol="PowerKeyHook",
            a14_installer="installer condition controls_powerflash",
            a14_hook_targets="com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing",
            a14_callback_phase="intercept,after",
            a13_owner_path="app/src/main/java/tv/withaibuild/customiuizer/mods/Controls.kt",
            a13_symbol="PowerKeyHook",
            a13_installer="installers/SystemServerInstaller.java if controls_powerflash",
            a13_hook_targets="com.android.server.policy.PhoneWindowManager#init,com.android.server.policy.MiuiPhoneWindowManager#interceptKeyBeforeQueueing",
            a13_callback_phase="before,after",
            preference_keys=("controls_powerflash", "controls_powerflash_delay"),
            value_domain="boolean; default false",
            default_semantics="off keeps ROM power-key behavior",
            result_argument_behavior=(
                "When the screen is off, KEYCODE_POWER down starts a long-press timer; long-press toggles torch "
                "and holds a wake lock. Short press wakes and turns torch off. "
                "controls_powerflash_delay triples ViewConfiguration.getLongPressTimeout when true. "
                "Volume-down torch is a different installer key."
            ),
            api33_variant_reason=(
                "A14 intercept: ACTION_DOWN skip with result 0 and never proceeds; ACTION_UP skip 0 after wake/torch-off. "
                "A13 before: returnAndSkip(0) on the same paths. A14 registers the SCREEN_ON receiver through "
                "registerModuleReceiver; A13 uses Context.registerReceiver with explicit unregister of the previous owner."
            ),
            proof_conclusion="PRESENT_A13_VARIANT",
            evidence_level="INDIVIDUAL_SEMANTIC_PROOF",
            body_relation="REVIEWED_VARIANT",
            diff_summary=(
                "Shared: PhoneWindowManager.init SCREEN_ON receiver; MiuiPhoneWindowManager.interceptKeyBeforeQueueing "
                "KEYCODE_POWER; FLAG_VIRTUAL_HARD_KEY / FLAG_FROM_SYSTEM filters; isInteractive early return; "
                "controls_powerflash_delay long-press timeout. Differ: A14 intercept skip-0 vs A13 before returnAndSkip(0); "
                "A14 guarded inline long-press runnable vs A13 mPowerLongPressRunnable; A14 registerModuleReceiver vs A13 registerReceiver."
            ),
            value_default_comparison="controls_powerflash boolean installer gate; controls_powerflash_delay boolean default false on both trees.",
            hook_target_comparison="PhoneWindowManager.init and MiuiPhoneWindowManager.interceptKeyBeforeQueueing on both trees.",
            callback_semantics_comparison="A14 intercept skip-or-proceed vs A13 before returnAndSkip; init side-effect is after-equivalent (A14 proceeds once then registers).",
            arg_result_comparison="Skipped queue result is 0; KeyEvent is not rewritten. Torch/wake are side-effects.",
            a14_only_branches="receiver helper name torchScreenOnReceiver; no extra user-visible flashlight mode; does not consume fingerprint/toast/vibration keys",
            why_user_behavior_is_equivalent=(
                "User contract is power-key flashlight while the screen is off, with an optional longer delay. "
                "Same filters, same torch/wake side-effects, same delay key."
            ),
            key_ownership_evidence=(
                "controls_powerflash: INSTALLER_CALLEE → PowerKeyHook; "
                "controls_powerflash_delay: LITERAL_READ inside PowerKeyHook. "
                "controls_volumedowndt_torch is not consumed here."
            ),
            a14_key_owner_reference="Controls.kt::PowerKeyHook INSTALLER_CALLEE controls_powerflash; LITERAL_READ controls_powerflash_delay",
            a13_key_owner_reference="Controls.kt::PowerKeyHook INSTALLER_CALLEE via SystemServerInstaller.java; LITERAL_READ controls_powerflash_delay",
        ),
    ]


def review_owner_groups(
    a14_scan: RepoScan,
    a13_scan: RepoScan,
    keys: list[str],
    *,
    a14_xml: dict[str, str] | None = None,
    a13_xml: dict[str, str] | None = None,
    a14_tags: dict[str, str] | None = None,
    a13_tags: dict[str, str] | None = None,
    explicit: list[ProofManifest] | None = None,
) -> OwnerGroupIndex:
    """Discover true owner groups. PRESENT only from explicit reviewed manifests."""
    del a14_xml, a13_xml, a14_tags, a13_tags
    index = OwnerGroupIndex()
    groups, evidence, unpaired = discover_true_owner_groups(keys, a14_scan, a13_scan)
    index.discovered = groups
    index.evidence_by_key = evidence
    index.stats.groups_discovered = len(groups)
    index.stats.keys_with_direct_owner_evidence = len(evidence)
    index.stats.xml_only_unproven_keys = len(unpaired)
    index.stats.keys_with_explicit_alias_evidence = 0

    explicit = explicit_reviewed_owner_groups() if explicit is None else list(explicit)
    explicit_by_key = {}
    for man in explicit:
        if man.proof_conclusion == "PRESENT_A13_VARIANT":
            if not reviewed_variant_fields_complete(man):
                continue
            if not (man.key_ownership_evidence and man.a14_key_owner_reference and man.a13_key_owner_reference):
                continue
        for key in man.preference_keys:
            explicit_by_key[key] = man

    reviewed_idents: set[tuple[str, str, str, str]] = set()
    for group in groups:
        for key in group.keys:
            man = explicit_by_key.get(key)
            if not man:
                continue
            if key not in man.preference_keys:
                continue
            index.by_key[key] = man
            index.stats.keys_assigned += 1
            reviewed_idents.add((group.a14_path, group.a14_symbol, group.a13_path, group.a13_symbol))
            if man.proof_conclusion == "PRESENT_A13_VARIANT":
                index.stats.present_variant += 1
            elif man.proof_conclusion == "PRESENT_EQUIVALENT":
                index.stats.present_equivalent += 1
    index.stats.groups_reviewed = len(reviewed_idents)
    return index
