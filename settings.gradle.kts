pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://mirrors.huaweicloud.com/repository/maven/") {
            name = "huaweicloud-maven"
            mavenContent {
                releasesOnly()
            }
        }
        maven("https://mirrors.aliyun.com/repository/google/") {
            name = "aliyun-google"
            mavenContent {
                releasesOnly()
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val useLocalLibxposed = providers.gradleProperty("useLocalLibxposed").orNull == "true"
        if (useLocalLibxposed) {
            mavenLocal {
                content {
                    includeGroup("io.github.libxposed")
                }
            }
        }
        google()
        mavenCentral()
        maven("https://mirrors.huaweicloud.com/repository/maven/") {
            name = "huaweicloud-maven"
            mavenContent {
                releasesOnly()
            }
        }
        maven("https://mirrors.aliyun.com/repository/google/") {
            name = "aliyun-google"
            mavenContent {
                releasesOnly()
            }
        }
    }
}

rootProject.name = "CustoMIUIzer-A13"
include(":app")
