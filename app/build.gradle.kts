import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystorePropertiesPath =
    providers.gradleProperty("customiuizerA13KeystoreProperties").orNull
        ?: providers.environmentVariable("CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES").orNull

val keystorePropertiesFile = keystorePropertiesPath?.let(::file)
val keystoreProperties = Properties()
val hasReleaseSigning = if (keystorePropertiesFile != null && keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { key ->
        keystoreProperties.getProperty(key).isNullOrBlank().not()
    }
} else {
    false
}

val lastVersion = 134
val lastVersionName = "r13.10.0"
val supportedLocales = setOf(
    "ru-rRU",
    "zh-rCN",
    "zh-rTW",
    "ja-rJP",
    "vi-rVN",
    "cs-rCZ",
    "pt-rBR",
    "tr-rTR",
    "es-rES"
)

android {
    namespace = "tv.withaibuild.customiuizer"
    compileSdk = 36

    signingConfigs {
        create("v2") {
            if (hasReleaseSigning) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
            enableV1Signing = false
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "tv.withaibuild.customiuizer.r13"
        minSdk = 33
        //noinspection OldTargetApi,ExpiredTargetSdkVersion
        targetSdk = 34
        versionCode = lastVersion
        versionName = lastVersionName
        val buildTimeProp = (findProperty("buildTime") as? String)?.toLongOrNull() ?: 0L
        buildConfigField("long", "BUILD_TIME", "${buildTimeProp}L")
        resConfigs(*supportedLocales.toTypedArray())
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        val releaseSigning = signingConfigs.getByName("v2")

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            if (hasReleaseSigning) signingConfig = releaseSigning
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("develop") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            if (hasReleaseSigning) signingConfig = releaseSigning
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            versionNameSuffix = "-debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/androidx.*.version",
                "**.kotlin_builtins",
                "**.kotlin_metadata"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.api.ApkVariantOutput).outputFileName =
                "CustoMIUIzer-A13-${versionName}.apk"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        // Supported translations intentionally fall back to the base strings when incomplete.
        warning += "MissingTranslation"
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

val signingRequiredTasks = setOf(
    "assembleRelease",
    "bundleRelease",
    "packageRelease",
    "packageReleaseBundle",
    "packageReleaseUniversalApk",
    "signReleaseBundle",
    "assembleDevelop",
    "bundleDevelop",
    "packageDevelop",
    "packageDevelopBundle",
    "packageDevelopUniversalApk",
    "signDevelopBundle"
)
val signingConfigurationPath = keystorePropertiesPath ?: "(not configured)"

tasks.configureEach {
    if (!hasReleaseSigning && name in signingRequiredTasks) {
        val packagingKind = if (name.contains("Develop")) "develop" else "release"
        val signingFailureMessage =
            "Formal $packagingKind packaging requires the A13 signing configuration. " +
                "Set the Gradle property 'customiuizerA13KeystoreProperties' or the " +
                "environment variable 'CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES' to a " +
                "properties file containing storeFile, storePassword, keyAlias, and keyPassword. " +
                "Configured path: $signingConfigurationPath. " +
                "Unsigned CI verification may run tests, lint, and R8 only."
        doFirst {
            throw GradleException(signingFailureMessage)
        }
    }
}

dependencies {
    compileOnly(files("lib/miuisystem.jar"))
    compileOnly(files("lib/framework.jar"))
    compileOnly(libs.libxposed.api)

    implementation(libs.libxposed.service)
    implementation(enforcedPlatform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(libs.commons.lang3)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    testImplementation(libs.libxposed.api)
}
