pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "WorkTracker"

// Ensure local.properties or ANDROID_HOME exists for Xcode & CI build environments
val androidSdkPath = System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: "${System.getProperty("user.home")}/Library/Android/sdk"

val localProps = file("local.properties")
if (!localProps.exists() && file(androidSdkPath).exists()) {
    localProps.writeText("sdk.dir=${androidSdkPath.replace("\\", "/")}\n")
}

// Shared Kotlin Multiplatform module: common UI + data layer, plus the iOS framework
// and the desktop (Windows / macOS / Linux) application entry point.
include(":composeApp")
// Thin Android application shell that hosts the shared UI.
include(":androidApp")
