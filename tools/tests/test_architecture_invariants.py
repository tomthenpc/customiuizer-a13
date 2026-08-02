#!/usr/bin/env python3
"""Architecture invariants for the A13/A14 alignment."""
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"
MAIN = SRC / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"
INPUT = SRC / "tv" / "withaibuild" / "customiuizer" / "installers" / "InputMethodInstaller.java"
UTILS = SRC / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils"
CATALOG = SRC / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog"


def read(rel: str) -> str:
    return (SRC / rel.replace("/", "\\")).read_text(encoding="utf-8")


class ArchitectureInvariantTest(unittest.TestCase):
    def test_main_module_delegates_input_method_to_installer(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertIn("InputMethodInstaller.install(lpparam, pkg);", text)
        self.assertNotIn("VolumeCursorHook(lpparam);", text)
        self.assertNotIn("GboardPaddingHook(lpparam);", text)

    def test_input_method_installer_contains_real_hooks(self):
        text = INPUT.read_text(encoding="utf-8")
        self.assertIn("VolumeCursorHook", text)
        self.assertIn("GboardPaddingHook", text)
        self.assertIn("FixInputMethodBottomMarginHook", text)

    def test_feature_state_model_exists(self):
        text = (UTILS / "FeatureState.kt").read_text(encoding="utf-8")
        for state in ("NOT_INSTALLED", "INSTALLING", "INSTALLED", "FAILED_TRANSIENT", "FAILED_PERMANENT"):
            self.assertIn(state, text)

    def test_feature_install_result_exists(self):
        text = (UTILS / "FeatureInstallResult.kt").read_text(encoding="utf-8")
        for result in ("Installed", "AlreadyInstalled", "Disabled", "FailedTransient", "FailedPermanent"):
            self.assertIn(result, text)
        self.assertIn("isActive", text)

    def test_feature_install_registry_exists(self):
        text = (CATALOG / "FeatureInstallRegistry.kt").read_text(encoding="utf-8")
        self.assertIn("installById", text)
        self.assertIn("canonicalSpecs", text)
        self.assertIn("@JvmStatic", text)

    def test_main_module_no_direct_launcher_hooks(self):
        text = MAIN.read_text(encoding="utf-8")
        for cls in ("LauncherLayoutHooks", "LauncherSystemHooks", "LauncherAnimationHooks", "LauncherFolderHooks", "LauncherIconHooks"):
            self.assertNotIn(cls, text, f"MainModule still directly references {cls}")

    def test_launcher_installer_contains_package_ready_hooks(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "LauncherInstaller.java").read_text(encoding="utf-8")
        self.assertIn("installPackageReady", text)
        self.assertIn("HorizontalSpacingRes", text)
        self.assertIn("DisableLauncherLogHook", text)

    def test_main_module_delegates_settings_security_power_to_installers(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertIn("SettingsInstaller.install(lpparam);", text)
        self.assertIn("SecurityCenterInstaller.install(lpparam);", text)
        self.assertIn("PowerKeeperInstaller.install(lpparam);", text)
        # The old central router must not be called for those packages anymore.
        # The body of MainModule should not contain Settings/Security/Power hook class calls directly.
        for cls in ("SystemSettingsMoreHooks", "SystemNotificationMoreHooks"):
            self.assertNotIn(cls, text, f"MainModule still directly references {cls}")

    def test_settings_installer_contains_real_hooks(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "SettingsInstaller.java").read_text(encoding="utf-8")
        self.assertIn("miuizerSettingsHook", text)
        self.assertIn("USBConfigSettingsHook", text)

    def test_security_center_installer_contains_real_hooks(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "SecurityCenterInstaller.java").read_text(encoding="utf-8")
        self.assertIn("AppInfoHook", text)
        self.assertIn("SkipSecurityScanHook", text)

    def test_wallpaper_media_phone_installers_contain_real_hooks(self):
        for name in ("WallpaperInstaller", "MediaInstaller", "PhoneInstaller"):
            text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / f"{name}.java").read_text(encoding="utf-8")
            self.assertIn("install", text)

    def test_package_installer_router_only_routes_remaining_packages(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "PackageInstallerRouter.java").read_text(encoding="utf-8")
        for pkg in ("com.miui.miwallpaper", "com.android.incallui", "com.miui.screenshot", "com.miui.gallery"):
            self.assertNotIn(pkg, text, f"PackageInstallerRouter still routes {pkg}")

    def test_main_module_delegates_wallpaper_media_phone_to_installers(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertIn("WallpaperInstaller.install", text)
        self.assertIn("MediaInstaller.install", text)
        self.assertIn("PhoneInstaller.install", text)

    def test_main_module_delegates_generic_and_package_installer(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertIn("GenericAppInstaller.install(lpparam, pkg);", text)
        self.assertIn("PackageInstallerRouter.install(lpparam);", text)
        self.assertNotIn("Various.AlarmCompatHook();", text)

    def test_main_module_no_direct_application_attach_hook_decisions(self):
        text = MAIN.read_text(encoding="utf-8")
        self.assertNotIn("mPrefs.get", text)
        self.assertNotIn("ModuleHelper.findAndHookMethod", text)

    def test_generic_app_installer_contains_lbe_and_alarm_compat(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "GenericAppInstaller.java").read_text(encoding="utf-8")
        self.assertIn("SmartClipboardActionHook", text)
        self.assertIn("AlarmCompatHook", text)
        self.assertIn("StatusBarBackgroundHook", text)
        self.assertIn("VolumeMediaPlayerHook", text)

    def test_package_installer_router_only_package_installer(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "installers" / "PackageInstallerRouter.java").read_text(encoding="utf-8")
        self.assertIn("com.miui.packageinstaller", text)
        self.assertNotIn("com.lbe.security.miui", text)

    def test_process_scope_is_single_source(self):
        text = read("tv/withaibuild/customiuizer/mods/utils/ProcessScope.kt")
        self.assertIn("enum class ProcessScope", text)

    def test_feature_catalog_registry_and_legacy_spec_counts(self):
        text = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureCatalog.kt").read_text(encoding="utf-8")
        self.assertIn("private val registrySpecsInternal", text)
        self.assertIn("private val legacySpecsInternal", text)

        delimiter = "private val legacySpecsInternal by lazy(LazyThreadSafetyMode.NONE) { listOf("
        self.assertIn(delimiter, text, "registry and legacy spec lists must be split")
        registry_text, legacy_text = text.split(delimiter, 1)

        registry_ids = re.findall(r'id = "([^"]+)"', registry_text)
        legacy_ids = re.findall(r'id = "([^"]+)"', legacy_text)

        self.assertEqual(8, len(registry_ids), f"registry specs must contain exactly 8 ids: {registry_ids}")
        self.assertEqual(19, len(legacy_ids), f"legacy specs must contain exactly 19 ids: {legacy_ids}")
        self.assertEqual(27, len(registry_ids) + len(legacy_ids))
        self.assertEqual(set(), set(registry_ids) & set(legacy_ids), "registry and legacy ids must be disjoint")

    def test_feature_dispatcher_routing_no_duplicate_paths(self):
        dispatcher = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureDispatcher.kt").read_text(encoding="utf-8")
        catalog = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureCatalog.kt").read_text(encoding="utf-8")

        # Build canonical id -> diagnostic id map from FeatureCatalog.kt.
        catalog_ids = dict(re.findall(r'id = "([^"]+)".*?diagnosticId = DiagnosticIds\.(\w+)', catalog, re.DOTALL))
        diagnostic_to_canonical = {v: k for k, v in catalog_ids.items()}

        # Registry route uses FeatureInstallRegistry.installById("<canonical>").
        install_by_id = re.findall(r'FeatureInstallRegistry\.installById\(\s*"([^"]+)"', dispatcher)
        # Legacy route calls installWithContract(DiagnosticIds.XXX, ...).
        legacy_records = re.findall(r'recordRequested\(DiagnosticIds\.(\w+)\)', dispatcher)

        # Ensure each migrated feature is routed only through the registry.
        self.assertEqual(8, len(install_by_id), "FeatureDispatcher must route exactly 8 registry features through installById")
        self.assertEqual(17, len(legacy_records), "FeatureDispatcher must route exactly 17 legacy features through installWithContract")

        registry_ids_from_dispatcher = set(install_by_id)
        legacy_diagnostics_from_dispatcher = set(legacy_records)

        # Any legacy diagnostic id that maps back to a registry canonical id is a duplicate path.
        duplicate_paths = [
            diagnostic_to_canonical[did]
            for did in legacy_diagnostics_from_dispatcher
            if did in diagnostic_to_canonical and diagnostic_to_canonical[did] in registry_ids_from_dispatcher
        ]

        self.assertEqual(
            0,
            len(duplicate_paths),
            f"features must not be routed through both registry and legacy dispatcher paths: {duplicate_paths}",
        )


if __name__ == "__main__":
    unittest.main()
