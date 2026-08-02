#!/usr/bin/env python3
"""Unit tests for check_hook_contract_parity."""
from __future__ import annotations

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
        text = '''
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
        targets = parity.parse_contract_targets(text, "noDarkForce")
        self.assertEqual(len(targets), 1)
        key = next(iter(targets))
        self.assertEqual(key.member_name, "setForceDark")
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
        targets = parity.parse_contract_targets(text, "appLock")
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
        targets = parity.parse_contract_targets(text, "testContract")
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
        targets = parity.parse_contract_targets(text, "testContract")
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
        targets = parity.parse_contract_targets(text, "testContract")
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
            self.assertTrue(any("PARAMETER_TYPES_MISMATCH" in i for i in issues), issues)

    def test_orphan_parameter_types_detected(self) -> None:
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
                        parameterTypes = listOf(INT, STRING)
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
            self.assertTrue(any("ORPHAN_PARAMETER_TYPES" in i for i in issues), issues)


if __name__ == "__main__":
    unittest.main()
