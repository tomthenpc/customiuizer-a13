#!/usr/bin/env python3
"""Architecture invariants for the A13/A14 alignment."""
import re
import unittest
from pathlib import Path, PurePosixPath
REPO = Path(__file__).resolve().parent.parent.parent
SRC = REPO / "app" / "src" / "main" / "java"
MAIN = SRC / "tv" / "withaibuild" / "customiuizer" / "MainModule.java"
INPUT = SRC / "tv" / "withaibuild" / "customiuizer" / "installers" / "InputMethodInstaller.java"
UTILS = SRC / "tv" / "withaibuild" / "customiuizer" / "mods" / "utils"
CATALOG = SRC / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog"
def read(rel: str) -> str:
    return SRC.joinpath(*PurePosixPath(rel).parts).read_text(encoding="utf-8")
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
        self.assertIn("private val adaptedSpecsInternal", text)
        delimiter = "private val adaptedSpecsInternal by lazy(LazyThreadSafetyMode.NONE) { listOf("
        self.assertIn(delimiter, text, "registry and adapted spec lists must be split")
        registry_text, adapted_text = text.split(delimiter, 1)
        registry_ids = re.findall(r'id = "([^"]+)"', registry_text)
        adapted_ids = re.findall(r'id = "([^"]+)"', adapted_text)
        self.assertEqual(8, len(registry_ids), f"registry specs must contain exactly 8 ids: {registry_ids}")
        self.assertEqual(54, len(adapted_ids), f"adapted specs must contain exactly 54 ids: {adapted_ids}")
        self.assertEqual(62, len(registry_ids) + len(adapted_ids))
        self.assertEqual(set(), set(registry_ids) & set(adapted_ids), "registry and adapted ids must be disjoint")
    def test_feature_dispatcher_routing_no_duplicate_paths(self):
        dispatcher = (REPO / "app" / "src" / "main" / "java" / "tv" / "withaibuild" / "customiuizer" / "mods" / "catalog" / "FeatureDispatcher.kt").read_text(encoding="utf-8")
        # FeatureDispatcher must expose the minimal public surface.
        self.assertIn("fun createRuntime(", dispatcher)
        self.assertIn("fun install(", dispatcher)
        self.assertIn("fun installById(", dispatcher)
        # There must be no private install helper functions and no legacy routes.
        self.assertNotIn("installWithContract", dispatcher, "FeatureDispatcher must not use installWithContract")
        self.assertNotIn("installWithLegacyCheck", dispatcher, "FeatureDispatcher must not use installWithLegacyCheck")
        self.assertNotIn("recordRequested", dispatcher, "FeatureDispatcher must not contain recordRequested")
        # All production routing goes through FeatureInstallRegistry.installById("<canonical>", ...).
        install_by_id = re.findall(r'FeatureInstallRegistry\.installById\(\s*"([^"]+)"', dispatcher)
        self.assertEqual(
            0,
            len(install_by_id),
            "FeatureDispatcher must not hard-code any canonical id; all routing goes through FeatureInstallRegistry with runtime-derived spec",
        )
if __name__ == "__main__":
    unittest.main()
