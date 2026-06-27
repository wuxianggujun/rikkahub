pluginManagement {
    val preferOfficialRepositories = System.getenv("CI").equals("true", ignoreCase = true)
    repositories {
        if (preferOfficialRepositories) {
            google {
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
            mavenCentral()
            gradlePluginPortal()
            maven("https://repo.itextsupport.com/android")
        }
        maven("https://maven.aliyun.com/repository/google") {
            name = "AliyunGoogleMirror"
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://maven.aliyun.com/repository/public") {
            name = "AliyunPublicMirror"
        }
        maven("https://maven.aliyun.com/repository/gradle-plugin") {
            name = "AliyunGradlePluginMirror"
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.itextsupport.com/android")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    val preferOfficialRepositories = System.getenv("CI").equals("true", ignoreCase = true)
    repositories {
        if (preferOfficialRepositories) {
            google()
            mavenCentral()
            maven("https://jitpack.io")
            mavenLocal()
        }
        maven("https://maven.aliyun.com/repository/google") {
            name = "AliyunGoogleMirror"
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://maven.aliyun.com/repository/public") {
            name = "AliyunPublicMirror"
        }
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }
}

rootProject.name = "rikkahub"

fun shouldIncludeStandaloneAppProjects(): Boolean {
    val explicitProperty = gradle.startParameter.projectProperties["rikkahub.includeStandaloneApp"]
        ?.toBooleanStrictOrNull() == true
    val explicitEnv = System.getenv("RIKKAHUB_INCLUDE_STANDALONE_APP")
        ?.toBooleanStrictOrNull() == true
    val explicitTask = gradle.startParameter.taskNames.any { taskName ->
        val normalized = taskName.trim().removePrefix(":")
        normalized == "app" ||
            normalized.startsWith("app:") ||
            normalized == "buildAll"
    }

    return gradle.parent == null || explicitProperty || explicitEnv || explicitTask
}

if (shouldIncludeStandaloneAppProjects()) {
    include(":app")
    include(":app:baselineprofile")
} else {
    logger.lifecycle(
        "Skipping RikkaHub standalone app projects for embedded build; " +
            "set -Prikkahub.includeStandaloneApp=true to include them."
    )
}

include(":highlight")
include(":ai")
include(":search")
include(":speech")
include(":common")
include(":document")
include(":material3")
include(":workspace")
include(":embedded")
