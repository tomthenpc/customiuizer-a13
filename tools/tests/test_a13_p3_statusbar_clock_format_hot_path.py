"""Static contract tests for A13 P3-1 status bar clock format hot-path cache.

Uses a single contract_violations(source) checker that:
1. Passes (empty list) on the real production source.
2. Produces specific violation codes for each intentional mutation.

This avoids the previous fake self-fulfilling assertion pattern where
tests only proved the mutation text existed.
"""

import os
import re
import unittest


REPO_ROOT = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "..")
)
HOOKS_FILE = os.path.join(
    REPO_ROOT,
    "app",
    "src",
    "main",
    "java",
    "tv",
    "withaibuild",
    "customiuizer",
    "mods",
    "SystemStatusBarClockAndMoreHooks.kt",
)


VIOLATION_CODES = {
    "CACHE_CLASS_MISSING",
    "CACHE_RETAINS_CONTEXT",
    "CACHE_RETAINS_RESOURCES",
    "CACHE_RETAINS_VIEW",
    "CACHE_RETAINS_CALENDAR",
    "DEFAULT_PATH_CACHE_MISSING",
    "RESOURCE_ID_LAZY_CACHE_MISSING",
    "RESOLVED_FORMAT_CACHE_MISSING",
    "RAW_FORMAT_KEY_MISSING",
    "SHOW_SECONDS_KEY_MISSING",
    "IS24_KEY_MISSING",
    "HOUR_2D_KEY_MISSING",
    "PER_TICK_GET_STRING_MISSING",
    "FORMAT_BUILDER_NOT_LOCAL",
    "TEXT_BUILDER_NOT_LOCAL",
    "REUSABLE_BUILDER_FIELD",
    "CUSTOM_FORMAT_ROUTED_THROUGH_CACHE",
    "CC_CLOCK_DISPATCH_CHANGED",
    "CC_DATE_DISPATCH_CHANGED",
    "ROM_FORMAT_CALL_CHANGED",
}


def _read_hooks_file():
    with open(HOOKS_FILE, "r", encoding="utf-8") as f:
        return f.read()


def _extract_cache_class_block(source: str) -> str:
    """Extract the StatusBarClockFormatCache class body."""
    match = re.search(r"internal\s+class\s+StatusBarClockFormatCache\s*\{", source)
    if not match:
        return ""
    start = match.start()
    depth = 0
    i = match.end() - 1
    while i < len(source):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[start : i + 1]
        i += 1
    return ""


def _extract_update_time_block(source: str) -> str:
    """Extract the updateTime MethodHook object body."""
    match = re.search(r'"updateTime"\s*,\s*object\s*:\s*MethodHook', source)
    if not match:
        return ""
    start = match.start()
    before_match = re.search(r"override\s+fun\s+before\s*\(", source[start:])
    if not before_match:
        return ""
    before_start = start + before_match.start()
    # Search for the outer closing of the updateTime findAndHookMethod call.
    # The pattern is a line containing only whitespace and "})", then a blank line.
    end_pattern = re.compile(r"\n\s*\}\)\s*\n")
    end_match = end_pattern.search(source[before_start:])
    if end_match:
        return source[before_start : before_start + end_match.end()]
    # Fallback to end of file
    end_match2 = re.search(r"\n\s*\}\)\s*$", source[before_start:], re.MULTILINE)
    if end_match2:
        return source[before_start : before_start + end_match2.end()]
    return source[before_start : before_start + 3000]


def _extract_constructor_block(source: str) -> str:
    """Extract the MiuiClock constructor hook after() body."""
    match = re.search(
        r'hookAllConstructors\s*\(\s*"com\.android\.systemui\.statusbar\.views\.MiuiClock"',
        source,
    )
    if not match:
        return ""
    start = match.start()
    # Find the after() override inside this object
    after_match = re.search(r"override\s+fun\s+after\s*\(", source[start:])
    if not after_match:
        return ""
    after_start = start + after_match.start()
    # Heuristic: the constructor hook spans until the next findAndHookMethod
    next_hook = re.search(r"\n\s*ModuleHelper\.findAndHookMethod", source[after_start:])
    if next_hook:
        return source[after_start : after_start + next_hook.start()]
    return source[after_start : after_start + 4000]


def contract_violations(source: str) -> list[str]:
    """Return list of violation codes for the given source.

    The real production source must return [].
    Mutated sources should return one or more specific violation codes.
    """
    violations: list[str] = []
    cache_block = _extract_cache_class_block(source)
    update_time_block = _extract_update_time_block(source)
    constructor_block = _extract_constructor_block(source)

    # 1. Cache class existence
    if not cache_block:
        violations.append("CACHE_CLASS_MISSING")
        return violations

    # 2. Cache must not retain Android owner / heavy types
    forbidden_field_re = re.compile(
        r"(private\s+)?(val|var)\s+\w+\s*:\s*(Context|Resources|View|TextView|Calendar|MiuiClock)"
    )
    forbidden_matches = forbidden_field_re.findall(cache_block)
    for match in forbidden_matches:
        # match[2] is the type
        if match[2] == "Context":
            violations.append("CACHE_RETAINS_CONTEXT")
        elif match[2] == "Resources":
            violations.append("CACHE_RETAINS_RESOURCES")
        elif match[2] in ("View", "TextView"):
            violations.append("CACHE_RETAINS_VIEW")
        elif match[2] == "Calendar":
            violations.append("CACHE_RETAINS_CALENDAR")
        elif match[2] == "MiuiClock":
            violations.append("CACHE_RETAINS_VIEW")

    # 3. Cache must not use map-of-clock topology
    if re.search(r"(HashMap|WeakHashMap|ConcurrentHashMap)\s*<\s*\w*\s*Clock", cache_block):
        violations.append("CACHE_RETAINS_VIEW")

    # 4. Resolved format cache must exist
    if "cachedResolvedFormat" not in cache_block:
        violations.append("RESOLVED_FORMAT_CACHE_MISSING")

    # 5. Resource ID lazy cache must exist
    if "resIdNoAmpm" not in cache_block or "resIdWithAmpm" not in cache_block:
        violations.append("RESOURCE_ID_LAZY_CACHE_MISSING")

    # 6. Default path must use the cache
    if (
        "statusBarClockFormatCache" not in source
        or "StatusBarClockFormatCache" not in source
    ):
        violations.append("DEFAULT_PATH_CACHE_MISSING")

    # 7. updateTime default path must use resolveResourceId / resolveFormat
    if update_time_block:
        default_path_match = re.search(r'"clock"\s*==\s*clockName\s*&&\s*statusbarClockTweak', update_time_block)
        if default_path_match:
            default_rest = update_time_block[default_path_match.end():]
            # Primary cached path: the "if (formatCache != null) { ... }" block
            primary_match = re.search(
                r"if\s*\(\s*formatCache\s*!=\s*null\s*\)\s*\{(.*?)\}\s*else\s*\{",
                default_rest,
                re.DOTALL,
            )
            if primary_match:
                primary_path = primary_match.group(1)
                fallback_match = re.search(
                    r"else\s*\{(.*?)\}\s*\}\s*\}\s*\}\)",
                    default_rest,
                    re.DOTALL,
                )
                fallback_path = fallback_match.group(1) if fallback_match else ""
            else:
                primary_path = ""
                fallback_path = default_rest

            if "formatCache" not in default_rest or "resolveFormat" not in default_rest:
                violations.append("DEFAULT_PATH_CACHE_MISSING")
            if primary_match and "mContext.getString(fmtResId)" not in primary_path:
                violations.append("PER_TICK_GET_STRING_MISSING")
            if primary_match and "formatCache.resolveResourceId" not in primary_path:
                violations.append("RESOURCE_ID_LAZY_CACHE_MISSING")

    # 8. Cache keys: rawFormat, showSeconds, is24, hourIn2d
    if cache_block:
        if "cachedRawFormat" not in cache_block or "rawFormat == cachedRawFormat" not in cache_block:
            violations.append("RAW_FORMAT_KEY_MISSING")
        if "cachedShowSeconds" not in cache_block or "showSeconds == cachedShowSeconds" not in cache_block:
            violations.append("SHOW_SECONDS_KEY_MISSING")
        if "cachedIs24" not in cache_block or "is24 == cachedIs24" not in cache_block:
            violations.append("IS24_KEY_MISSING")
        if "cachedHourIn2d" not in cache_block or "hourIn2d == cachedHourIn2d" not in cache_block:
            violations.append("HOUR_2D_KEY_MISSING")

    # 9. Callback-local StringBuilders
    if update_time_block:
        if "val formatSb = StringBuilder" not in update_time_block:
            violations.append("FORMAT_BUILDER_NOT_LOCAL")
        if "val textSb = StringBuilder" not in update_time_block:
            violations.append("TEXT_BUILDER_NOT_LOCAL")

    # 10. No reusable StringBuilder fields in cache or object
    sb_field_re = re.compile(
        r"(?:private\s+|val\s+|var\s+)\w+\s*[:=]\s*(?:StringBuilder)"
    )
    object_field_match = re.search(
        r"object\s+SystemStatusBarClockAndMoreHooks\s*\{(.*?)\n\s*@JvmStatic",
        source,
        re.DOTALL,
    )
    if object_field_match:
        object_header = object_field_match.group(1)
        for line in object_header.split("\n"):
            if sb_field_re.search(line):
                violations.append("REUSABLE_BUILDER_FIELD")
                break

    # 11. ROM format call preserved
    if update_time_block and "callMethod(mCalendar" not in update_time_block:
        violations.append("ROM_FORMAT_CALL_CHANGED")

    # 12. Custom format does not route through default cache
    if update_time_block:
        custom_match = re.search(
            r"system_statusbar_clock_customformat.*?\n.*?timeFmt\s*=\s*customFormat",
            update_time_block,
            re.DOTALL,
        )
        if not custom_match:
            # If custom format is used with resolveFormat, that's a violation
            if "resolveFormat(customFormat" in update_time_block:
                violations.append("CUSTOM_FORMAT_ROUTED_THROUGH_CACHE")

    # 13. ccClock / ccDate paths preserved
    if update_time_block:
        if '"ccClock" == clockName' not in update_time_block:
            violations.append("CC_CLOCK_DISPATCH_CHANGED")
        if '"ccDate" == clockName' not in update_time_block:
            violations.append("CC_DATE_DISPATCH_CHANGED")

    # 14. Cache set in constructor for "clock" branch
    if constructor_block:
        clock_branch = re.search(
            r'setAdditionalInstanceField\s*\(\s*clock\s*,\s*"clockName"\s*,\s*"clock"\s*\)',
            constructor_block,
        )
        if clock_branch and "statusBarClockFormatCache" not in constructor_block:
            violations.append("DEFAULT_PATH_CACHE_MISSING")

    # Deduplicate while preserving order
    seen = set()
    result = []
    for v in violations:
        if v not in seen:
            seen.add(v)
            result.append(v)
    return result


class TestStatusBarClockFormatCacheContract(unittest.TestCase):
    """Positive contract tests for the format cache implementation."""

    def setUp(self):
        self.source = _read_hooks_file()

    def test_full_contract_has_no_violations(self):
        violations = contract_violations(self.source)
        self.assertEqual([], violations, f"Expected no violations, got: {violations}")

    def test_cache_class_exists(self):
        self.assertIn("StatusBarClockFormatCache", self.source)

    def test_cache_no_context_field(self):
        cache_block = _extract_cache_class_block(self.source)
        self.assertNotIn("Context", cache_block)

    def test_cache_no_resources_field(self):
        cache_block = _extract_cache_class_block(self.source)
        field_pattern = re.compile(r"(private\s+)?(val|var)\s+\w+\s*:\s*Resources")
        matches = field_pattern.findall(cache_block)
        self.assertEqual([], matches, f"Cache retains Resources field: {matches}")

    def test_update_time_still_calls_getString(self):
        update_time_block = _extract_update_time_block(self.source)
        self.assertIn("getString", update_time_block)

    def test_raw_format_in_cache_key(self):
        cache_block = _extract_cache_class_block(self.source)
        self.assertIn("rawFormat", cache_block)
        self.assertIn("cachedRawFormat", cache_block)

    def test_cache_set_in_constructor_for_clock(self):
        self.assertIn(
            'setAdditionalInstanceField(clock, "statusBarClockFormatCache"',
            self.source,
        )

    def test_format_builders_local(self):
        update_time_block = _extract_update_time_block(self.source)
        self.assertIn("val formatSb = StringBuilder", update_time_block)
        self.assertIn("val textSb = StringBuilder", update_time_block)


class TestStatusBarClockFormatCacheMutations(unittest.TestCase):
    """Real mutation tests — contract_violations(mutated) must report expected codes."""

    def setUp(self):
        self.original = _read_hooks_file()

    def _assert_mutation_produces(self, source: str, expected_code: str):
        violations = contract_violations(source)
        self.assertIn(
            expected_code,
            violations,
            f"Expected mutation to trigger {expected_code}; got {violations}",
        )

    def test_A_resolved_format_cache_removed_fails(self):
        mutated = self.original.replace("cachedResolvedFormat", "REMOVED_FIELD")
        self._assert_mutation_produces(mutated, "RESOLVED_FORMAT_CACHE_MISSING")

    def test_B_get_identifier_back_in_default_path_fails(self):
        # Remove the cache call in primary default path; leaving a direct getIdentifier there
        mutated = self.original.replace("formatCache.resolveResourceId", "REMOVED")
        self._assert_mutation_produces(mutated, "RESOURCE_ID_LAZY_CACHE_MISSING")

    def test_C_cache_class_has_context_field_fails(self):
        mutated = self.original.replace(
            "private var cachedShowSeconds = false",
            "private var context: Context? = null\n        private var cachedShowSeconds = false",
        )
        self._assert_mutation_produces(mutated, "CACHE_RETAINS_CONTEXT")

    def test_D_cache_class_has_resources_field_fails(self):
        mutated = self.original.replace(
            "private var cachedShowSeconds = false",
            "private var resources: Resources? = null\n        private var cachedShowSeconds = false",
        )
        self._assert_mutation_produces(mutated, "CACHE_RETAINS_RESOURCES")

    def test_E_raw_format_key_removed_fails(self):
        mutated = self.original.replace("rawFormat == cachedRawFormat", "REMOVED_KEY")
        self._assert_mutation_produces(mutated, "RAW_FORMAT_KEY_MISSING")

    def test_F_showSeconds_key_removed_fails(self):
        mutated = self.original.replace("showSeconds == cachedShowSeconds", "REMOVED_KEY")
        self._assert_mutation_produces(mutated, "SHOW_SECONDS_KEY_MISSING")

    def test_G_is24_key_removed_fails(self):
        mutated = self.original.replace("is24 == cachedIs24", "REMOVED_KEY")
        self._assert_mutation_produces(mutated, "IS24_KEY_MISSING")

    def test_H_hourIn2d_key_removed_fails(self):
        mutated = self.original.replace("hourIn2d == cachedHourIn2d", "REMOVED_KEY")
        self._assert_mutation_produces(mutated, "HOUR_2D_KEY_MISSING")

    def test_I_formatSb_fielded_fails(self):
        mutated = self.original.replace(
            "private var cachedResolvedFormat: String? = null",
            'private var cachedResolvedFormat: String? = null\n        private val formatSb = StringBuilder()',
        )
        self._assert_mutation_produces(mutated, "REUSABLE_BUILDER_FIELD")

    def test_J_textSb_fielded_fails(self):
        mutated = self.original.replace(
            "private var cachedResolvedFormat: String? = null",
            'private var cachedResolvedFormat: String? = null\n        private val textSb = StringBuilder()',
        )
        self._assert_mutation_produces(mutated, "REUSABLE_BUILDER_FIELD")

    def test_K_custom_format_routed_through_cache_fails(self):
        mutated = self.original.replace(
            "timeFmt = customFormat",
            "timeFmt = formatCache.resolveFormat(customFormat, false, false, false)",
        )
        self._assert_mutation_produces(mutated, "CUSTOM_FORMAT_ROUTED_THROUGH_CACHE")

    def test_L_getString_removed_fails(self):
        # Remove the per-tick getString call in the primary cached path
        mutated = self.original.replace(
            "val rawFormat = mContext.getString(fmtResId)",
            "val rawFormat = \"h:mm\"",
        )
        self._assert_mutation_produces(mutated, "PER_TICK_GET_STRING_MISSING")

    def test_M_ccClock_dispatch_changed_fails(self):
        mutated = self.original.replace('"ccClock" == clockName', 'false')
        self._assert_mutation_produces(mutated, "CC_CLOCK_DISPATCH_CHANGED")

    def test_N_ccDate_dispatch_changed_fails(self):
        mutated = self.original.replace('"ccDate" == clockName', 'false')
        self._assert_mutation_produces(mutated, "CC_DATE_DISPATCH_CHANGED")

    def test_O_rom_format_call_changed_fails(self):
        mutated = self.original.replace("callMethod(mCalendar", "callMethod(REMOVED")
        self._assert_mutation_produces(mutated, "ROM_FORMAT_CALL_CHANGED")

    def test_P_default_path_missing_cache_fails(self):
        mutated = self.original.replace(
            'setAdditionalInstanceField(clock, "statusBarClockFormatCache", StatusBarClockFormatCache())',
            "",
        )
        self._assert_mutation_produces(mutated, "DEFAULT_PATH_CACHE_MISSING")

    def test_Q_cache_class_removed_fails(self):
        mutated = self.original.replace(
            "internal class StatusBarClockFormatCache {",
            "internal class REMOVED_CACHE {",
        )
        # Replace resolveResourceId / resolveFormat usages so checker reports class missing
        mutated = mutated.replace("StatusBarClockFormatCache", "REMOVED")
        self._assert_mutation_produces(mutated, "CACHE_CLASS_MISSING")


class TestViolationCodesSet(unittest.TestCase):
    """Sanity: all emitted codes are in the VIOLATION_CODES catalog."""

    def test_all_emitted_codes_are_known(self):
        source = _read_hooks_file()
        # Collect all known mutations and verify their violations are in the catalog
        mutations = [
            (source.replace("cachedResolvedFormat", "X"), "RESOLVED_FORMAT_CACHE_MISSING"),
            (source.replace("formatCache.resolveResourceId", "X"), "RESOURCE_ID_LAZY_CACHE_MISSING"),
        ]
        for mutated, _ in mutations:
            for v in contract_violations(mutated):
                self.assertIn(v, VIOLATION_CODES, f"Unknown violation code: {v}")


if __name__ == "__main__":
    unittest.main()
