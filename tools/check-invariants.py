#!/usr/bin/env python3
"""A13 runtime invariant static gate.

Ported from the A14 tool and adapted for A13's Java/Kotlin mix. It checks the
contracts that keep the module from taking down system_server, SystemUI or
Launcher: framework callbacks and deferred lambdas must not let reflection
or framework failures escape, receivers must have a managed lifetime, and
legacy or wasteful patterns must be avoided.

Usage:
    python tools/check-invariants.py            # check the whole module source
    python tools/check-invariants.py --staged   # check only files staged in git

Exit code 0 means every invariant holds. Any other exit code means at least one
rule in AGENTS.md was violated and the change must not be committed.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SOURCE_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"
SCOPE_FILE = REPO_ROOT / "app" / "src" / "main" / "resources" / "META-INF" / "xposed" / "scope.list"
MAIN_MODULE = SOURCE_ROOT / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"
REQUIRED_SCOPES = ("system", "android", "com.android.systemui", "com.miui.home", "com.mi.android.globallauncher")

# Files that are allowed to break a rule, with the reason. Keep this list short;
# every entry is a place where the invariant is enforced rather than consumed.
ALLOWED = {
    "no-raw-register-receiver": {
        "tv/withaibuild/customiuizer/mods/utils/ModuleHelper.java",
        "tv/withaibuild/customiuizer/mods/utils/ModuleHelper.kt",
    },
    "guard-framework-callbacks": {
        # The settings app is the module's own process. A throw there shows a
        # normal app crash dialog; it cannot take a system process down.
        "tv/withaibuild/customiuizer/MainApplication.kt",
        "tv/withaibuild/customiuizer/MainApplication.java",
        "tv/withaibuild/customiuizer/tasker/UnlockReceiver.kt",
        "tv/withaibuild/customiuizer/tasker/UnlockReceiver.java",
    },
}

# Reflection helpers that must not throw out of an unguarded framework callback.
REFLECTION = re.compile(
    r"XposedHelpers\.|\bcallMethod\(|\bgetObjectField\(|\bsetObjectField\(|"
    r"\bgetStaticObjectField\(|\bsetStaticObjectField\(|\bcallStaticMethod\("
)

LINE_COMMENT = re.compile(r"//[^\n]*")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)


def strip_comments(text: str) -> str:
    """Blanks out comments, preserving every newline so line numbers stay correct."""

    def blank(match: re.Match[str]) -> str:
        return re.sub(r"[^\n]", " ", match.group(0))

    return LINE_COMMENT.sub(blank, BLOCK_COMMENT.sub(blank, text))


class Finding:
    def __init__(self, rule: str, path: Path, line: int, detail: str) -> None:
        self.rule = rule
        self.path = path
        self.line = line
        self.detail = detail

    def __str__(self) -> str:
        rel = self.path.relative_to(REPO_ROOT).as_posix()
        return f"{rel}:{self.line}: [{self.rule}] {self.detail}"


def rel_posix(path: Path) -> str:
    return path.relative_to(SOURCE_ROOT).as_posix()


def is_allowed(rule: str, path: Path) -> bool:
    return rel_posix(path) in ALLOWED.get(rule, set())


def block_at(text: str, search_from: int) -> tuple[str, int]:
    """Returns the brace-balanced block starting at the first '{' at or after search_from."""
    start = text.index("{", search_from)
    depth = 0
    index = start
    while index < len(text):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                break
        index += 1
    return text[start : index + 1], start


def line_of(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def has_outer_exception_boundary(body: str) -> bool:
    """Whether the callback body immediately enters the shared guard or an explicit try."""
    content = body[1:-1].lstrip()
    content = re.sub(r"^[^{}\n]*->\s*", "", content)
    return content.startswith("ModuleHelper.guarded") or content.startswith("try")


# --- rules -----------------------------------------------------------------

CALLBACK_SIGNATURES = (
    r"override fun handleMessage\(",
    r"override fun onReceive\(",
    r"override fun onChange\(",
    r"override fun run\s*\(",
    r"override fun afterTextChanged\(",
    r"@Override\s+(?:public\s+)?(?:void|boolean|int\b)\s+handleMessage\s*\(",
    r"@Override\s+(?:public\s+)?(?:void|boolean)\s+onReceive\s*\(",
    r"@Override\s+(?:public\s+)?(?:void|boolean)\s+onChange\s*\(",
    r"@Override\s+(?:public\s+)?(?:void|boolean|int\b)\s+run\s*\(",
)


def check_guard_framework_callbacks(path: Path, text: str) -> list[Finding]:
    """Framework-invoked callbacks run outside the MethodHook try/catch.

    A reflective miss on a ROM that renamed a field then propagates out of the
    module and kills system_server, SystemUI or Launcher. Wrap the body in
    ModuleHelper.guarded, or catch inside it.

    PreferenceObserver.onChange is exempt: ModuleHelper.observePreferenceChange
    already isolates every observer it dispatches to.
    """
    if is_allowed("guard-framework-callbacks", path):
        return []
    normalized_path = path.as_posix()
    if "customiuizer/mods/" not in normalized_path and not normalized_path.endswith(
        "/customiuizer/utils/BatteryIndicator.kt"
    ):
        return []
    findings = []
    for signature in CALLBACK_SIGNATURES:
        for match in re.finditer(signature, text):
            body, start = block_at(text, match.end() - 1)
            header = text[match.start() : start]
            if "guarded" in header or has_outer_exception_boundary(body):
                continue
            always_guard = "onReceive" in signature or "handleMessage" in signature or "afterTextChanged" in signature
            if not always_guard and not REFLECTION.search(body):
                continue
            if ": ModuleHelper.PreferenceObserver" in text[max(0, match.start() - 400) : match.start()]:
                continue
            if re.search(r"\bPreferenceObserver\b", text[max(0, match.start() - 200) : match.start()]):
                continue
            findings.append(
                Finding(
                    "guard-framework-callbacks",
                    path,
                    line_of(text, match.start()),
                    "framework callback is not wrapped in an outer ModuleHelper.guarded/try boundary",
                )
            )
    return findings


DEFERRED_CALLBACKS = (
    r"\bRunnable\s*\(?\s*\{",
    r"\b(?:post|postDelayed|postAtTime|postOnAnimation|runOnUiThread)\s*\(\s*\{",
    r"\bThread\s*\(\s*\{",
    r"\bHandler\s*\([^\n]*\)\s*\{",
    r"\bset(?:On\w+Listener)\s*\{",
    r"\b(?:withEndAction|doOnLayout|addUpdateListener|postFrameCallback)\s*\(?\s*\{",
)


def check_guard_deferred_callbacks(path: Path, text: str) -> list[Finding]:
    """Lambdas that run later are outside the hook's try/catch, exactly like named callbacks.

    The round-one rule only matched override callbacks, so postDelayed(Runnable { ... })
    and setOnXxxListener { ... } slip through — including bodies posted to handlers
    inside system_server where an uncaught throw reboots the device.

    Anything deferred from mods/ must be wrapped in ModuleHelper.guarded or a try.
    """
    if "customiuizer/mods/" not in path.as_posix():
        return []
    findings = []
    for pattern in DEFERRED_CALLBACKS:
        for match in re.finditer(pattern, text):
            body, start = block_at(text, match.end() - 1)
            # object callbacks are checked at their actual override entry by
            # check_guard_framework_callbacks, not at the enclosing object body.
            if "override fun" in body:
                continue
            if has_outer_exception_boundary(body) or "runCatching" in body:
                continue
            # An empty lambda cannot throw; it is a deliberate no-op replacement.
            if not body.strip("{} \n\t"):
                continue
            findings.append(
                Finding(
                    "guard-deferred-callbacks",
                    path,
                    line_of(text, match.start()),
                    "deferred body runs outside the hook try/catch; wrap it in ModuleHelper.guarded",
                )
            )
    return findings


def check_coroutine_scopes_handle_failure(path: Path, text: str) -> list[Finding]:
    """A SupervisorJob does not swallow failures, it only stops them cascading.

    A13 does not use coroutines in the module source, but the rule is kept to
    catch any future introduction.
    """
    if "customiuizer/mods/" not in path.as_posix():
        return []
    findings = []
    for match in re.finditer(r"CoroutineScope\(", text):
        end = text.find("\n", match.start())
        statement = text[match.start() : end if end != -1 else len(text)]
        if "coroutineFailureHandler" in statement:
            continue
        findings.append(
            Finding(
                "coroutine-scopes-handle-failure",
                path,
                line_of(text, match.start()),
                "add + ModuleHelper.coroutineFailureHandler to this scope",
            )
        )
    return findings


def check_no_raw_register_receiver(path: Path, text: str) -> list[Finding]:
    """Receivers registered straight on a Context outlive their hook target.

    Cleanup keyed on the hooked instance cannot see the registration a previous
    instance made, so every recreation of the target leaves another live
    receiver behind. Use ModuleHelper.registerModuleReceiver (one per key) or
    registerOwnedReceiver (one per live owner).

    A raw registration is accepted only when the same file unregisters that
    exact receiver, which is how the screen-state, weather and step-counter
    controllers manage their own paired lifetime.
    """
    if is_allowed("no-raw-register-receiver", path):
        return []
    if "customiuizer/mods/" not in path.as_posix():
        return []
    findings = []
    for match in re.finditer(r"\.registerReceiver\(\s*([^,\n]*)", text):
        receiver = match.group(1).strip()
        # A null receiver is a synchronous sticky-broadcast read, not a registration.
        if receiver.startswith("null"):
            continue
        # Anonymous receivers can never be unregistered; they always need the registry.
        if not re.fullmatch(r"[\w.]+", receiver):
            findings.append(
                Finding(
                    "no-raw-register-receiver",
                    path,
                    line_of(text, match.start()),
                    "anonymous receiver cannot be unregistered; "
                    "use ModuleHelper.registerModuleReceiver / registerOwnedReceiver",
                )
            )
            continue
        if f"unregisterReceiver({receiver}" in text:
            continue
        # A declared field plus an unregister path in the same file is a managed
        # lifetime, even when the unregister call goes through a local alias.
        declared_field = re.search(rf"^\s*(?:private )?(?:var|val) {re.escape(receiver)}\b", text, re.MULTILINE)
        if declared_field and "unregisterReceiver(" in text:
            continue
        findings.append(
            Finding(
                "no-raw-register-receiver",
                path,
                line_of(text, match.start()),
                "use ModuleHelper.registerModuleReceiver / registerOwnedReceiver, "
                "or unregister this exact receiver in the same file",
            )
        )
    return findings


def check_no_looperless_handler(path: Path, text: str) -> list[Finding]:
    """Handler() with no Looper binds to whichever thread ran the hook.

    In a hook that is not guaranteed to run on a Looper thread it throws
    outright. Always pass an explicit Looper.
    """
    findings = []
    for match in re.finditer(r"\bnew\s+Handler\s*\(\s*\)", text):
        findings.append(
            Finding(
                "no-looperless-handler",
                path,
                line_of(text, match.start()),
                "pass an explicit Looper, e.g. new Handler(Looper.getMainLooper())",
            )
        )
    return findings


def check_no_redundant_arg_marshalling(path: Path, text: str) -> list[Finding]:
    """getArgsArray + proceed(args) is only for hooks that rewrite arguments.

    It allocates the argument list and a copy of it on every invocation, and
    makes the framework re-marshal every argument on proceed. Hooks that only
    read arguments must use Chain.getArg(i) / Chain.getArgs() and Chain.proceed().
    """
    findings = []
    for match in re.finditer(r"(?:override fun|public)\s+intercept\s*\(", text):
        body, start = block_at(text, match.end() - 1)
        if "getArgsArray" not in body:
            continue
        if re.search(r"\bargs\w*\[\s*[^\]]+\]\s*=[^=]", body):
            continue
        findings.append(
            Finding(
                "no-redundant-arg-marshalling",
                path,
                line_of(text, match.start()),
                "hook does not rewrite arguments; use Chain.getArg(i) and Chain.proceed()",
            )
        )
    return findings


def check_no_legacy_xposed(path: Path, text: str) -> list[Finding]:
    """The module runs on libxposed API 101/102 only."""
    findings = []
    for match in re.finditer(r"de\.robv\.android\.xposed", text):
        findings.append(
            Finding(
                "no-legacy-xposed",
                path,
                line_of(text, match.start()),
                "legacy Xposed API is not available at runtime",
            )
        )
    return findings


def check_no_regex_split_on_literal(path: Path, text: str) -> list[Finding]:
    """split("x".toRegex()) compiles a Pattern on every call.

    Java's String.split takes a single-character fast path that does not touch
    the regex engine; the mechanical Kotlin translation loses it.

    Only single-character delimiters are flagged; a genuine pattern such as
    "\\s+" has to stay a Regex.
    """
    findings = []
    for match in re.finditer(r'split\(\s*"(?:\\\\)?[^"\\+*?\[\]{}()^$]"\.toRegex\(\)', text):
        findings.append(
            Finding(
                "no-regex-split-on-literal",
                path,
                line_of(text, match.start()),
                "split on a literal delimiter, not a compiled Regex",
            )
        )
    return findings


def check_launcher_rename_loop_exit(path: Path, text: str) -> list[Finding]:
    """The migrated shortcut rename loop must stop after its unique key matches.

    Launcher.java used continue for non-app entries and break after updating the
    matching shortcut. A forEach migration cannot express that non-local break
    and silently scans and may update later entries.
    """
    if rel_posix(path) != "tv/withaibuild/customiuizer/mods/LauncherIconHooks.kt":
        return []
    match = re.search(r"\bfun\s+RenameShortcutsHook\s*\(", text)
    if match is None:
        return [
            Finding(
                "launcher-rename-loop-exit",
                path,
                1,
                "RenameShortcutsHook is missing",
            )
        ]
    body, _ = block_at(text, match.end() - 1)
    if re.search(r"\bfor\s*\(\s*shortcut\s+in\s+mAllLoadedApps\s*\)", body) and re.search(
        r"\bbreak\b", body
    ):
        return []
    return [
        Finding(
            "launcher-rename-loop-exit",
            path,
            line_of(text, match.start()),
            "preserve Launcher.java continue/break semantics with an explicit for loop",
        )
    ]


def check_xposed_scope() -> list[Finding]:
    """The packaged Xposed scope must contain the LSPosed system scope and must
    not confuse the process name system_server with the scope name system.
    """
    findings: list[Finding] = []
    if not SCOPE_FILE.is_file():
        return [Finding("xposed-scope", SCOPE_FILE, 1, "scope.list is missing")]

    lines = [line.strip() for line in SCOPE_FILE.read_text(encoding="utf-8").splitlines()]
    non_empty = [line for line in lines if line]

    for scope in REQUIRED_SCOPES:
        if scope not in non_empty:
            findings.append(
                Finding("xposed-scope", SCOPE_FILE, 1, f"scope.list is missing required scope '{scope}'")
            )

    if "system_server" in non_empty:
        findings.append(
            Finding("xposed-scope", SCOPE_FILE, 1, "scope.list must use 'system' (scope), not 'system_server' (process)")
        )

    seen = set()
    duplicates = set()
    for line in non_empty:
        if line in seen:
            duplicates.add(line)
        seen.add(line)
    for dup in sorted(duplicates):
        findings.append(Finding("xposed-scope", SCOPE_FILE, 1, f"scope.list contains duplicate scope '{dup}'"))

    if MAIN_MODULE.is_file() and "onSystemServerStarting" in MAIN_MODULE.read_text(encoding="utf-8"):
        if "system" not in non_empty:
            findings.append(
                Finding(
                    "xposed-scope",
                    SCOPE_FILE,
                    1,
                    "MainModule has onSystemServerStarting but scope.list does not include 'system'",
                )
            )

    return findings


EXPECTED_DEFSTYLE = {
    "CheckBoxPreferenceEx.kt":
        "androidx.preference.R.attr.switchPreferenceStyle",
    "DropDownPreferenceEx.kt":
        "androidx.preference.R.attr.dropdownPreferenceStyle",
    "EditTextPreferenceEx.kt":
        "androidx.preference.R.attr.editTextPreferenceStyle",
    "ListPreferenceEx.kt":
        "androidx.preference.R.attr.dialogPreferenceStyle",
    "PreferenceCategoryEx.kt":
        "androidx.preference.R.attr.preferenceCategoryStyle",
    "PreferenceEx.kt":
        "androidx.preference.R.attr.preferenceStyle",
    "SeekBarPreference.kt":
        "androidx.preference.R.attr.preferenceStyle",
}


def check_preference_style_attr(
    path: Path,
    text: str
) -> list[Finding]:
    expected = EXPECTED_DEFSTYLE.get(path.name)
    if expected is None:
        return []

    pattern = re.compile(
        r"defStyleAttr\s*:\s*Int\s*=\s*"
        r"([A-Za-z0-9_.$]+)"
    )
    matches = list(pattern.finditer(text))

    if not matches:
        return [
            Finding(
                "preference-style-attr",
                path,
                1,
                "missing defStyleAttr default; "
                f"expected {expected}",
            )
        ]

    if len(matches) != 1:
        return [
            Finding(
                "preference-style-attr",
                path,
                line_of(text, matches[0].start()),
                "expected exactly one defStyleAttr default, "
                f"found {len(matches)}",
            )
        ]

    match = matches[0]
    actual = match.group(1)

    if actual != expected:
        return [
            Finding(
                "preference-style-attr",
                path,
                line_of(text, match.start()),
                f"expected defStyleAttr {expected}, "
                f"got {actual}",
            )
        ]

    return []


RULES = (
    check_guard_framework_callbacks,
    check_guard_deferred_callbacks,
    check_coroutine_scopes_handle_failure,
    check_no_raw_register_receiver,
    check_no_looperless_handler,
    check_no_redundant_arg_marshalling,
    check_no_legacy_xposed,
    check_no_regex_split_on_literal,
    check_launcher_rename_loop_exit,
    check_preference_style_attr,
)


def staged_files() -> list[Path]:
    result = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        check=True,
    )
    files = []
    for name in result.stdout.splitlines():
        path = REPO_ROOT / name
        if path.suffix in (".kt", ".java") and path.is_file() and SOURCE_ROOT in path.parents:
            files.append(path)
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--staged", action="store_true", help="check only files staged in git")
    args = parser.parse_args()

    files = staged_files() if args.staged else sorted(SOURCE_ROOT.rglob("*.kt")) + sorted(SOURCE_ROOT.rglob("*.java"))
    findings: list[Finding] = []
    for path in files:
        text = strip_comments(path.read_text(encoding="utf-8"))
        for rule in RULES:
            findings.extend(rule(path, text))
    findings.extend(check_xposed_scope())

    if not findings:
        print(f"check-invariants: {len(files)} files, no violations")
        return 0

    by_rule: dict[str, list[Finding]] = {}
    for finding in findings:
        by_rule.setdefault(finding.rule, []).append(finding)

    for rule, items in sorted(by_rule.items()):
        try:
            doc = next(r for r in RULES if r.__name__.replace("check_", "").replace("_", "-") == rule).__doc__
        except StopIteration:
            doc = ""
        print(f"\n=== {rule} ({len(items)}) ===")
        print((doc or "").strip().split("\n\n")[0])
        print()
        for finding in items:
            print(f"  {finding}")

    print(f"\ncheck-invariants: {len(findings)} violation(s) across {len(files)} files")
    return 1


if __name__ == "__main__":
    sys.exit(main())
