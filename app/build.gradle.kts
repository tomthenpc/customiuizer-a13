import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystorePropertiesFile = rootProject.file("../keystore.properties")
val keystoreProperties = Properties()
val hasReleaseSigning = if (keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { key ->
        keystoreProperties.getProperty(key).isNullOrBlank().not()
    }
} else {
    false
}

val lastVersion = 122
val lastVersionName = "r13.2.3-devin"
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
    namespace = "name.monwf.customiuizer"
    compileSdk = 36

    signingConfigs {
        create("v2") {
            if (hasReleaseSigning) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else {
                // Missing signing configuration: point to a path that will not be a valid keystore,
                // so packageRelease / packageDevelop fail at execution time instead of silently
                // falling back to the Android debug key.
                storeFile = rootProject.file("../keystore.properties")
                storePassword = ""
                keyAlias = ""
                keyPassword = ""
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
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        resConfigs(*supportedLocales.toTypedArray())
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        val releaseSigning = signingConfigs.getByName("v2")

        getByName("release") {
            check(hasReleaseSigning) {
                "Release signing configuration is missing or incomplete. " +
                "Ensure ${rootProject.file("../keystore.properties")} exists and defines " +
                "storeFile, storePassword, keyAlias, and keyPassword."
            }
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = releaseSigning
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("develop") {
            check(hasReleaseSigning) {
                "Develop signing configuration is missing or incomplete. " +
                "Ensure ${rootProject.file("../keystore.properties")} exists and defines " +
                "storeFile, storePassword, keyAlias, and keyPassword."
            }
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = releaseSigning
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
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
}
