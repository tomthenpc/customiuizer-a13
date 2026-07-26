-verbose

# Xposed
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keepattributes RuntimeVisibleAnnotations
-keep public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onModuleLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}

# Hook callbacks are loaded only inside target processes. Keep them away from
# ordinary app startup classes because libxposed is compileOnly in the APK.
-keep,allowobfuscation class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }

-keepnames class name.monwf.customiuizer.GateWayLauncher

# Obfuscation
-repackageclasses
-allowaccessmodification

-dontwarn kotlin.jvm.internal.SourceDebugExtension
-dontwarn android.**
-dontwarn miui.**
# -dontnote **
