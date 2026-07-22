# Changelog

## r13.0.0-api101

- Fork from upstream CustoMIUIzer v23.11.26 for MIUI 14 / Android 13.
- Add independent `name.monwf.customiuizer.a13` application identity and
  `CustoMIUIzer_forA13` / `米客_forA13` branding.
- Upgrade module metadata and dependencies to libxposed API 101.
- Migrate module lifecycle callbacks to API 101.
- Replace annotation/class hook registration with HookBuilder and interceptor
  instances using exception passthrough.
- Add an R8-safe compatibility adapter for remaining before/after callbacks.
- Migrate shared resource and package-permission hooks to native Chain
  interceptors.
- Limit hook activation to Android 13 and document the xaga target ROM matrix.
- Add unsigned-environment build fallback and GitHub Actions release builds.
