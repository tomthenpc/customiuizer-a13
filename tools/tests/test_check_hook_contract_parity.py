#!/usr/bin/env python3
"""Unit tests for check_hook_contract_parity."""
from __future__ import annotations

import re
import sys
import tempfile
import unittest
from pathlib import Path

# Make the tools directory importable.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import check_hook_contract_parity as parity


class TestParseHelpers(unittest.TestCase):
    def test_first_class_literal_prefers_dotted(self) -> None:
        args = ['"com.android.server.Foo"', 'lpparam.classLoader', '"bar"']
        self.assertEqual(parity.first_class_literal(args), "com.android.server.Foo")

    def test_first_class_literal_ignores_method_name(self) -> None:
        args = ['"bar"']
        self.assertIsNone(parity.first_class_literal(args))

    def test_first_method_literal_with_string_class(self) -> None:
        args = ['"com.android.server.Foo"', 'lpparam.classLoader', '"bar"', 'callback']
        self.assertEqual(parity.first_method_literal(args, False), "bar")

    def test_first_method_literal_with_class_variable(self) -> None:
        args = ['FooVar', '"bar"', 'callback']
        self.assertEqual(parity.first_method_literal(args, True), "bar")

    def test_unescape_dollar(self) -> None:
        self.assertEqual(parity.unescape_string(r"com.android.server.Foo\$Bar"), "com.android.server.Foo$Bar")


class TestContractParsing(unittest.TestCase):
    def test_contract_with_optional_criticality(self) -> None:
        text = '''import android.content.Context
        val noDarkForce: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "noDarkForce",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "UiModeManagerService.setForceDark",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.android.server.UiModeManagerService",
                        memberName = "setForceDark",
                        parameterTypes = listOf(Context::class.java)
                    ),
                    criticality = Criticality.OPTIONAL
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "noDarkForce")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        key = next(iter(targets))
        self.assertEqual(key.member_name, "setForceDark")
        self.assertEqual(key.parameter_types, ("android.content.Context",))
        self.assertEqual(targets[key][1], True)

    def test_contract_default_required(self) -> None:
        text = '''
        val appLock: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "appLock",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "SecurityManagerService.removeAccessControlPassLocked",
                        operation = HookOperation.ALL_METHODS_BY_NAME,
                        className = "com.miui.server.SecurityManagerService",
                        memberName = "removeAccessControlPassLocked"
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "appLock")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        key = next(iter(targets))
        self.assertEqual(targets[key][1], False)


class TestProductionParsing(unittest.TestCase):
    def test_parses_hook_all_methods_with_class_variable(self) -> None:
        source = '''
object SystemSecurityAndSystemHooks {
    @JvmStatic
    fun NoSignatureVerifyServiceHook(lpparam: SystemServerStartingParam) {
        val SignDetails = XposedHelpers.findClassIfExists("android.content.pm.SigningDetails", lpparam.classLoader)
        ModuleHelper.hookAllMethods(SignDetails, "checkCapability", object : MethodHook() {
            override fun before(param: BeforeHookCallback) {}
        })
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "SystemSecurityAndSystemHooks.kt"
            src.parent.mkdir(parents=True, exist_ok=True)
            src.write_text(source, encoding="utf-8")
            targets, errors = parity.extract_production_targets(root, "tv/withaibuild/customiuizer/mods/SystemSecurityAndSystemHooks.kt", "NoSignatureVerifyServiceHook")
            self.assertEqual(errors, [])
            self.assertEqual(len(targets), 1)
            self.assertEqual(targets[0].key.class_name, "android.content.pm.SigningDetails")
            self.assertEqual(targets[0].key.member_name, "checkCapability")
            self.assertEqual(targets[0].key.operation, "ALL_METHODS_BY_NAME")

    def test_parses_hook_all_constructors(self) -> None:
        source = '''
object Foo {
    @JvmStatic
    fun Bar(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllConstructors("com.android.server.wm.WindowSurfaceController", lpparam.classLoader, object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "Foo.kt"
            src.write_text(source, encoding="utf-8")
            targets, errors = parity.extract_production_targets(root, "Foo.kt", "Bar")
            self.assertEqual(errors, [])
            self.assertEqual(len(targets), 1)
            self.assertEqual(targets[0].key.member_name, "<constructors>")
            self.assertEqual(targets[0].key.operation, "ALL_CONSTRUCTORS")


class TestJvmTypeNormalization(unittest.TestCase):
    """F1 — Fully qualified JVM type normalization."""

    def test_fq_top_level_class(self) -> None:
        self.assertEqual(parity.resolve_type_expr("android.content.Context", {}), "android.content.Context")

    def test_fq_java_lang_class(self) -> None:
        self.assertEqual(parity.resolve_type_expr("java.lang.String", {}), "java.lang.String")

    def test_imported_nested_class(self) -> None:
        imports = {"Settings": "android.provider.Settings"}
        self.assertEqual(parity.resolve_type_expr("Settings.System", imports), "android.provider.Settings$System")

    def test_fq_nested_class(self) -> None:
        self.assertEqual(
            parity.resolve_type_expr("android.provider.Settings.System", {}),
            "android.provider.Settings$System",
        )

    def test_explicit_dollar_nested_name(self) -> None:
        self.assertEqual(
            parity.resolve_type_expr("android.provider.Settings$System", {}),
            "android.provider.Settings$System",
        )

    def test_import_shorthand_equals_fq_form(self) -> None:
        imports = {"Settings": "android.provider.Settings"}
        shorthand = parity.resolve_type_expr("Settings.System::class.java", imports)
        fq = parity.resolve_type_expr("android.provider.Settings.System::class.java", {})
        self.assertEqual(shorthand, fq)
        self.assertEqual(shorthand, "android.provider.Settings$System")

    def test_array_of_fq_nested(self) -> None:
        imports = {"Settings": "android.provider.Settings"}
        self.assertEqual(
            parity.resolve_type_expr("Array<Settings.System>::class.java", imports),
            "android.provider.Settings$System[]",
        )

    def test_two_overloads_remain_distinct(self) -> None:
        """Two overloads of the same member with different parameter types are both matched."""
        contracts = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                ),
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(STRING)
                    )
                )
            )
        )
        }
        '''
        production = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.example.Foo", lpparam.classLoader, "bar", Int::class.javaPrimitiveType, object : MethodHook() {})
        ModuleHelper.findAndHookMethod("com.example.Foo", lpparam.classLoader, "bar", String::class.java, object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            prod.parent.mkdir(parents=True, exist_ok=True)
            prod.write_text(production, encoding="utf-8")
            contracts_path = root / "CatalogContracts.kt"
            contracts_path.write_text(contracts, encoding="utf-8")
            source_root = root
            batch = {"testContract": ("tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")}
            issues = parity.check_batch(batch, contracts, source_root)
            self.assertEqual(issues, [])

    def test_ambiguous_nested_class_raises(self) -> None:
        with self.assertRaises(parity.TypeResolutionError):
            parity.resolve_type_expr("Settings.System", {})


class TestDisjointDiagnostics(unittest.TestCase):
    """F2 — Disjoint diagnostics for parameter type parity."""

    def test_one_mismatch_emits_exactly_one_parameter_types_mismatch(self) -> None:
        contracts = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                )
            )
        )
        }
        '''
        production = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.example.Foo", lpparam.classLoader, "bar", Int::class.javaPrimitiveType, String::class.java, object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            prod.parent.mkdir(parents=True, exist_ok=True)
            prod.write_text(production, encoding="utf-8")
            contracts_path = root / "CatalogContracts.kt"
            contracts_path.write_text(contracts, encoding="utf-8")
            source_root = root
            batch = {"testContract": ("tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")}
            issues = parity.check_batch(batch, contracts, source_root)
            self.assertEqual(len([i for i in issues if "PARAMETER_TYPES_MISMATCH" in i]), 1)
            self.assertEqual(len([i for i in issues if "ORPHAN" in i]), 0)

    def test_real_base_level_orphan(self) -> None:
        contracts = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                )
            )
        )
        }
        '''
        production = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.example.Other", lpparam.classLoader, "bar", Int::class.javaPrimitiveType, object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            prod.parent.mkdir(parents=True, exist_ok=True)
            prod.write_text(production, encoding="utf-8")
            contracts_path = root / "CatalogContracts.kt"
            contracts_path.write_text(contracts, encoding="utf-8")
            source_root = root
            batch = {"testContract": ("tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")}
            issues = parity.check_batch(batch, contracts, source_root)
            self.assertEqual(len([i for i in issues if "ORPHAN_CONTRACT_TARGET" in i]), 1)
            self.assertEqual(len([i for i in issues if "ORPHAN_PARAMETER_TYPES" in i]), 0)


class TestParameterTypesParity(unittest.TestCase):
    def test_contract_parameter_types_parsed(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT, BOOLEAN, STRING)
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        key = next(iter(targets))
        self.assertEqual(key.parameter_types, ("int", "boolean", "java.lang.String"))

    def test_contract_empty_list(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = emptyList()
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        key = next(iter(targets))
        self.assertEqual(key.parameter_types, ())

    def test_contract_no_parameter_types_defaults_empty(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar"
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        key = next(iter(targets))
        self.assertEqual(key.parameter_types, ())

    def test_production_extracts_exact_method_types(self) -> None:
        source = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.example.Foo", lpparam.classLoader, "bar", Int::class.javaPrimitiveType, String::class.java, object : MethodHook() {
            override fun before(param: BeforeHookCallback) {}
        })
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            src.parent.mkdir(parents=True, exist_ok=True)
            src.write_text(source, encoding="utf-8")
            targets, errors = parity.extract_production_targets(root, "tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")
            self.assertEqual(errors, [])
            self.assertEqual(len(targets), 1)
            self.assertEqual(targets[0].key.parameter_types, ("int", "java.lang.String"))

    def test_production_extracts_constructor_types(self) -> None:
        source = '''import android.content.Context

object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookConstructor("com.example.Foo", lpparam.classLoader, Context::class.java, String::class.java, object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            src.parent.mkdir(parents=True, exist_ok=True)
            src.write_text(source, encoding="utf-8")
            targets, errors = parity.extract_production_targets(root, "tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")
            self.assertEqual(errors, [])
            self.assertEqual(len(targets), 1)
            self.assertEqual(targets[0].key.parameter_types, ("android.content.Context", "java.lang.String"))
            self.assertEqual(targets[0].key.member_name, "<constructors>")

    def test_all_methods_have_empty_types(self) -> None:
        source = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.hookAllMethods("com.example.Foo", lpparam.classLoader, "bar", object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            src.parent.mkdir(parents=True, exist_ok=True)
            src.write_text(source, encoding="utf-8")
            targets, errors = parity.extract_production_targets(root, "tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")
            self.assertEqual(errors, [])
            self.assertEqual(len(targets), 1)
            self.assertEqual(targets[0].key.parameter_types, ())

    def test_parameter_types_mismatch_detected(self) -> None:
        contracts = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                )
            )
        )
        }
        '''
        production = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.example.Foo", lpparam.classLoader, "bar", Int::class.javaPrimitiveType, String::class.java, object : MethodHook() {})
    }
}
'''
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            prod.parent.mkdir(parents=True, exist_ok=True)
            prod.write_text(production, encoding="utf-8")
            contracts_path = root / "CatalogContracts.kt"
            contracts_path.write_text(contracts, encoding="utf-8")
            source_root = root
            batch = {"testContract": ("tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")}
            issues = parity.check_batch(batch, contracts, source_root)
            self.assertEqual(len([i for i in issues if "PARAMETER_TYPES_MISMATCH" in i]), 1)
            self.assertEqual(len([i for i in issues if "ORPHAN" in i]), 0)


class TestDuplicateContractTarget(unittest.TestCase):
    """F3 — Duplicate TargetKey must not be silently overwritten."""

    def test_duplicate_identical_target(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                ),
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(len([e for e in errors if "DUPLICATE_CONTRACT_TARGET" in e]), 1)
        self.assertEqual(len(targets), 1)

    def test_duplicate_required_optional_target(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                ),
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    ),
                    criticality = Criticality.OPTIONAL
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(len([e for e in errors if "DUPLICATE_CONTRACT_TARGET" in e]), 1)
        self.assertEqual(len(targets), 1)


class TestUnresolvedParameterTypes(unittest.TestCase):
    """F4 — Unsupported parameterTypes expressions must not silently degrade to empty."""

    def test_unsupported_list_expression_fails(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = buildList { add(INT) }
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertTrue(any("UNRESOLVED_PARAMETER_TYPES" in e for e in errors), errors)
        self.assertEqual(len(targets), 0)

    def test_unresolved_type_in_list_fails(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(UNKNOWN_TYPE)
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertTrue(any("UNRESOLVED_PARAMETER_TYPES" in e for e in errors), errors)

    def test_empty_list_valid(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = emptyList()
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        self.assertEqual(next(iter(targets)).parameter_types, ())

    def test_empty_list_of_valid(self) -> None:
        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf()
                    )
                )
            )
        )
        }
        '''
        targets, errors = parity.parse_contract_targets(text, "testContract")
        self.assertEqual(errors, [])
        self.assertEqual(len(targets), 1)
        self.assertEqual(next(iter(targets)).parameter_types, ())


class TestMutations(unittest.TestCase):
    """Verify that reintroducing the repaired bugs makes the relevant tests fail."""

    def _patch_and_assert_fails(self, attr: str, buggy, correct_callable) -> None:
        original = getattr(parity, attr)
        setattr(parity, attr, buggy)
        try:
            with self.assertRaises(AssertionError):
                correct_callable()
        finally:
            setattr(parity, attr, original)

    def test_mutation_package_dot_to_dollar_fails(self) -> None:
        original = parity.resolve_type_expr

        def buggy(expr: str, imports: dict[str, str]) -> str:
            # Reintroduce naive package-dot-to-$ conversion for FQ names without $.
            if "." in expr and "$" not in expr:
                return expr.replace(".", "$").replace("$$", "$")
            return original(expr, imports)

        def correct() -> None:
            self.assertEqual(parity.resolve_type_expr("android.content.Context", {}), "android.content.Context")

        self._patch_and_assert_fails("resolve_type_expr", buggy, correct)

    def test_mutation_remove_duplicate_detection_fails(self) -> None:
        def buggy_record(
            results: dict,
            key: parity.TargetKey,
            value: tuple[str, bool],
            feature_id: str,
            errors: list[str],
        ) -> None:
            # Reintroduce silent overwrite.
            results[key] = value

        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                ),
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                )
            )
        )
        }
        '''

        def correct() -> None:
            targets, errors = parity.parse_contract_targets(text, "testContract")
            self.assertEqual(len([e for e in errors if "DUPLICATE_CONTRACT_TARGET" in e]), 1)
            self.assertEqual(len(targets), 1)

        self._patch_and_assert_fails("_record_contract_target", buggy_record, correct)

    def test_mutation_unsupported_expression_to_empty_fails(self) -> None:
        original = parity.resolve_parameter_list

        def buggy(text: str, imports: dict[str, str]) -> tuple[tuple[str, ...], list[str]]:
            # Reintroduce silent fallback to empty for unknown list expressions.
            text = text.strip()
            if text == "emptyList()" or text == "listOf()":
                return (), []
            m = re.match(r"^listOf\s*\((.*)\)\s*$", text, re.DOTALL)
            if not m:
                return (), []
            inner = m.group(1).strip()
            if not inner:
                return (), []
            # ignore errors and unresolved types
            types = []
            for p in inner.split(","):
                p = p.strip()
                if not p:
                    continue
                try:
                    types.append(parity.resolve_type_expr(p, imports))
                except parity.TypeResolutionError:
                    pass
            return tuple(types), []

        text = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = buildList { add(INT) }
                    )
                )
            )
        )
        }
        '''

        def correct() -> None:
            targets, errors = parity.parse_contract_targets(text, "testContract")
            self.assertTrue(any("UNRESOLVED_PARAMETER_TYPES" in e for e in errors), errors)

        self._patch_and_assert_fails("resolve_parameter_list", buggy, correct)

    def test_mutation_orphan_reporting_after_same_base_mismatch_fails(self) -> None:
        original = parity.check_batch

        def buggy(batch, contracts_text, source_root):
            # Restore the old buggy orphan reporting after same-base mismatch.
            issues = original(batch, contracts_text, source_root)
            for feature_id, (rel_path, function_name) in batch.items():
                prod_targets, _ = parity.extract_production_targets(source_root, rel_path, function_name)
                contract_targets, _ = parity.parse_contract_targets(contracts_text, feature_id)
                prod_keys = {pt.key for pt in prod_targets}
                contract_keys = set(contract_targets.keys())

                def base(k):
                    return (k.class_name, k.member_name, k.operation)

                prod_by_base = {}
                for k in prod_keys:
                    prod_by_base.setdefault(base(k), []).append(k)
                orphan = contract_keys - prod_keys
                for k in orphan:
                    b = base(k)
                    if b in prod_by_base:
                        prod_versions = [p.parameter_types for p in prod_by_base[b]]
                        issues.append(
                            f"ORPHAN_PARAMETER_TYPES: {feature_id} contract {k} not found in production; production has {prod_versions}"
                        )
            return issues

        contracts = '''
        val testContract: HookTargetContract by lazy(kotlin.LazyThreadSafetyMode.NONE) { HookTargetContract(
            featureId = "testContract",
            requirements = listOf(
                SingleTargetRequirement(
                    target = HookTargetSpec(
                        id = "Foo.bar",
                        operation = HookOperation.EXACT_METHOD,
                        className = "com.example.Foo",
                        memberName = "bar",
                        parameterTypes = listOf(INT)
                    )
                )
            )
        )
        }
        '''
        production = '''
object ExampleHooks {
    @JvmStatic
    fun ExampleHook(lpparam: SystemServerStartingParam) {
        ModuleHelper.findAndHookMethod("com.example.Foo", lpparam.classLoader, "bar", Int::class.javaPrimitiveType, String::class.java, object : MethodHook() {})
    }
}
'''

        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            prod = root / "tv" / "withaibuild" / "customiuizer" / "mods" / "ExampleHooks.kt"
            prod.parent.mkdir(parents=True, exist_ok=True)
            prod.write_text(production, encoding="utf-8")
            contracts_path = root / "CatalogContracts.kt"
            contracts_path.write_text(contracts, encoding="utf-8")
            source_root = root
            batch = {"testContract": ("tv/withaibuild/customiuizer/mods/ExampleHooks.kt", "ExampleHook")}

            def correct() -> None:
                issues = parity.check_batch(batch, contracts, source_root)
                self.assertEqual(len([i for i in issues if "PARAMETER_TYPES_MISMATCH" in i]), 1)
                self.assertEqual(len([i for i in issues if "ORPHAN" in i]), 0)

            self._patch_and_assert_fails("check_batch", buggy, correct)


if __name__ == "__main__":
    unittest.main()
