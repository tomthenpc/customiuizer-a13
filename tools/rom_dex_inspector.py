#!/usr/bin/env python3
"""Inspect a SystemUI APK/DEX for target classes/methods/fields.

This is a read-only, offline evidence tool. It uses androguard when
available, otherwise returns a best-effort scan result.

Example:
    python tools/rom_dex_inspector.py systemui.apk \
        --class Lcom/android/systemui/statusbar/notification/row/MiuiNotificationMenuRow;
"""
from __future__ import annotations

import argparse
import contextlib
import io
import json
import logging
import sys
from pathlib import Path
from typing import Any


def try_androguard() -> Any:
    try:
        from androguard.core.apk import APK
        from androguard.core.dex import DEX

        return APK, DEX
    except ImportError:
        return None


def dex_type_to_java(desc: str) -> str:
    """Convert a DEX descriptor to a Java-style type string."""
    if not desc:
        return desc
    if desc.startswith("["):
        return dex_type_to_java(desc[1:]) + "[]"
    if desc.startswith("L") and desc.endswith(";"):
        return desc[1:-1].replace("/", ".")
    primitives = {
        "V": "void",
        "Z": "boolean",
        "B": "byte",
        "S": "short",
        "C": "char",
        "I": "int",
        "J": "long",
        "F": "float",
        "D": "double",
    }
    return primitives.get(desc, desc)


def parse_descriptor(desc: str) -> tuple[list[str], str]:
    """Parse a method descriptor into (parameter types, return type)."""
    if not desc.startswith("(") or ")" not in desc:
        return [], desc
    params, ret = desc[1:].split(")", 1)
    types: list[str] = []
    i = 0
    while i < len(params):
        c = params[i]
        if c == "[":
            j = i
            while j < len(params) and params[j] == "[":
                j += 1
            if params[j] == "L":
                end = params.index(";", j) + 1
                types.append(dex_type_to_java(params[i:end]))
                i = end
            else:
                types.append(dex_type_to_java(params[i : j + 1]))
                i = j + 1
        elif c == "L":
            end = params.index(";", i) + 1
            types.append(dex_type_to_java(params[i:end]))
            i = end
        else:
            types.append(dex_type_to_java(c))
            i += 1
    return types, dex_type_to_java(ret)


def inspect_apk(apk_path: Path, target_classes: list[str]) -> dict[str, Any]:
    result: dict[str, Any] = {
        "apk_path": str(apk_path),
        "target_classes": target_classes,
        "androguard_available": False,
        "classes": {},
        "warnings": [],
    }
    andro = try_androguard()
    if andro is None:
        result["warnings"].append("androguard not installed; cannot inspect DEX")
        return result

    APK, DEX = andro
    result["androguard_available"] = True

    # androguard is extremely verbose on stdout/stderr. The only reliable
    # short-term fix is to redirect both streams to an in-memory buffer while
    # we parse. Any log lines are discarded; our own result is built in memory.
    _log_buffer = io.StringIO()

    def _capture():
        return contextlib.redirect_stdout(_log_buffer), contextlib.redirect_stderr(_log_buffer)

    with contextlib.ExitStack() as stack:
        for ctx in _capture():
            stack.enter_context(ctx)

        # Also raise the module log level as a second line of defense.
        for logger_name in list(logging.root.manager.loggerDict):
            if logger_name.startswith("androguard"):
                logging.getLogger(logger_name).setLevel(logging.CRITICAL)

        try:
            apk = APK(str(apk_path))
        except Exception as e:
            result["warnings"].append(f"APK parse failed: {e}")
            return result

        package = apk.get_package()
        result["package"] = package

        dex_idx = 0
        for raw in apk.get_all_dex():
            try:
                d = DEX(raw)
            except Exception as e:
                result["warnings"].append(f"DEX {dex_idx} parse failed: {e}")
                dex_idx += 1
                continue

            for cls in d.get_classes():
                name = cls.get_name()
                if name not in target_classes:
                    continue

                methods = []
                for m in cls.get_methods():
                    methods.append(
                        {
                            "name": m.get_name(),
                            "descriptor": m.get_descriptor(),
                            "params": parse_descriptor(m.get_descriptor())[0],
                            "return": parse_descriptor(m.get_descriptor())[1],
                        }
                    )

                fields = []
                for f in cls.get_fields():
                    fields.append(
                        {
                            "name": f.get_name(),
                            "type": dex_type_to_java(f.get_descriptor()),
                            "descriptor": f.get_descriptor(),
                        }
                    )

                result["classes"][name] = {
                    "dex_index": dex_idx,
                    "super": cls.get_superclassname(),
                    "methods": methods,
                    "fields": fields,
                }

            dex_idx += 1

        for target in target_classes:
            if target not in result["classes"]:
                result["warnings"].append(f"class {target} not found in any DEX")

    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Inspect a SystemUI APK/DEX.")
    parser.add_argument("apk", type=Path, help="APK file to inspect.")
    parser.add_argument(
        "--class",
        dest="classes",
        action="append",
        default=[],
        help="Target class (DEX format, e.g. Lcom/foo/Bar;).",
    )
    parser.add_argument(
        "--json", action="store_true", help="Output machine-readable JSON."
    )
    parser.add_argument(
        "--out", type=Path, default=None, help="Output file for JSON."
    )
    args = parser.parse_args(argv)

    if not args.classes:
        print("ERROR: specify at least one --class", file=sys.stderr)
        return 1

    result = inspect_apk(args.apk, args.classes)
    output = json.dumps(result, indent=2, ensure_ascii=False)
    if args.out:
        args.out.write_text(output, encoding="utf-8")
        return 0
    if args.json:
        print(output)
    else:
        for name, info in result["classes"].items():
            print(f"class {name}")
            print(f"  super: {info['super']}")
            for m in info["methods"]:
                print(f"  method {m['name']} : {m['descriptor']}")
            for f in info["fields"]:
                print(f"  field {f['name']} : {f['type']}")
        if result["warnings"]:
            for w in result["warnings"]:
                print(f"warning: {w}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
