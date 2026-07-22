# API 101 port notes

## Baseline and scope

The source baseline is upstream tag `v23.11.26` (`108fa205`). The primary test
matrix is Redmi Note 11T Pro / Pro+ (`xaga`) running MIUI 14 Android 13 builds
`V14.0.10.0.TLOINXM` and `V14.0.7.0.TLOCNXM`.

This repository is separate from `customiuizer-a14` because the two branches
target different Android framework and MIUI implementations. Keeping distinct
application IDs and release histories prevents a working A13 build from being
upgraded in place to an incompatible A14 build.

## Runtime contract

The reference LSPosed v2.0.4 (7741) archive and the Vector v2.0 (3046) Actions
artifact both contain the `API_101` implementation marker in their framework
DEX. The module targets API 101 even when a newer framework also implements API
102, preserving the requested compatibility floor.

API 101 changes applied here:

1. The module has a no-argument construction path and initializes framework
   state from `onModuleLoaded`.
2. Package and system-server entry points use `PackageReadyParam` and
   `SystemServerStartingParam`.
3. Hooks are registered as interceptor instances through `HookBuilder`.
4. Existing before/after hooks are represented by `MethodHook`, which directly
   implements `XposedInterface.Hooker` and calls `Chain.proceed` exactly once.
5. Hook priority and original exception behavior are preserved.

## Incremental refactor boundary

`ResourceHooks` and `PackagePermissions` are native Chain implementations.
Large feature files still use the adapter so ROM-specific behavior remains
reviewable. Future migrations should be made by feature group, with each hook
preserving these cases explicitly:

- before-only skip versus proceed;
- mutable arguments passed to `Chain.proceed`;
- after callbacks that run when the original throws;
- after callbacks that replace a result or throwable;
- constructor return behavior and hook priority.

## Validation limits

The Gradle build, R8 release build, APK metadata, API references, and framework
API markers can be verified off-device. MIUI method availability and behavioral
correctness require on-device regression on both target ROMs; a successful APK
build alone is not proof that every optional MIUI hook exists on both packages.
