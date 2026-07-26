pluginManagement {
    repositories {
        maven("https://mirrors.huaweicloud.com/repository/maven/")
        maven("https://mirrors.aliyun.com/repository/google/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
        maven("https://mirrors.huaweicloud.com/repository/maven/")
        maven("https://jitpack.io")
        maven("https://mirrors.aliyun.com/repository/google/")
        mavenCentral()
        google()
    }
}

rootProject.name = "CustoMIUIzer-A13"
include(":app")
