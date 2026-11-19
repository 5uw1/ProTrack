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

// Lowest safe versions of libraries that the Gradle plugins and Kotlin tooling pull in (AGP, lint,
// Kotlin swift export, ...). None of them ships in the apps – they run on the build machine – but
// GitHub's automatic dependency submission records every classpath Gradle resolves, and Dependabot
// raises an alert for any vulnerable version in it. A version requested below its floor is raised
// to the floor; higher versions are left alone. Drop an entry once the plugins that bring the library
// require a fixed version themselves.
val securityFloors = mapOf(
  "org.bouncycastle:bcprov-jdk18on" to "1.85", // GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26
  "org.bouncycastle:bcpkix-jdk18on" to "1.85", // GHSA-wg6q-6289-32hp
  "org.bouncycastle:bcutil-jdk18on" to "1.85", // kept in step with bcprov / bcpkix
  "org.freemarker:freemarker" to "2.3.35", // GHSA-27j2-h3m2-8237
  "org.jdom:jdom2" to "2.0.6.1", // GHSA-2363-cqg2-863c
  "com.google.guava:guava" to "33.7.2-jre", // GHSA-xxph-c9ww-hj94 and older advisories against 18.0
  "com.fasterxml.jackson.core:jackson-core" to "2.22.3", // GHSA-7hhh-6rmp-j9qf, GHSA-p6pp-m3f8-5c89
  "com.fasterxml.jackson.core:jackson-databind" to "2.22.3", // GHSA-cxp5-3px4-pw24, GHSA-wv8q-qhhj-9h54
  "org.bitbucket.b_c:jose4j" to "0.9.6", // GHSA-3677-xxcr-wjqv
  "org.apache.commons:commons-lang3" to "3.18.0", // GHSA-j288-q9x7-2f5v
  "org.apache.httpcomponents:httpclient" to "4.5.14", // GHSA-7r82-7xv7-xcpj (requested as 4.5.6)
  "io.opentelemetry:opentelemetry-api" to "1.67.0", // GHSA-ccrj-gw8r-854m, GHSA-rcgg-9c38-7xpx
  "io.opentelemetry:opentelemetry-context" to "1.67.0", // kept in step with opentelemetry-api
)

/** True when [version] is numerically below [floor], comparing the digit groups (33.7.1-jre < 33.7.2-jre). */
fun isBelow(version: String, floor: String): Boolean {
  fun parts(v: String) = Regex("\\d+").findAll(v).map { it.value.toLong() }.toList()
  val (a, b) = parts(version) to parts(floor)
  for (i in 0 until maxOf(a.size, b.size)) {
    val (x, y) = a.getOrElse(i) { 0L } to b.getOrElse(i) { 0L }
    if (x != y) return x < y
  }
  return false
}

fun ResolutionStrategy.applySecurityFloors() = eachDependency {
  val floor = securityFloors["${requested.group}:${requested.name}"] ?: return@eachDependency
  val version = requested.version
  // Ranges and dynamic versions ("[1.0,2.0)", "1.+") are left to Gradle; so is a version a BOM manages.
  if (version.isNullOrBlank() || version.any { it in "[]()+," }) return@eachDependency
  if (isBelow(version, floor)) {
    useVersion(floor)
    because("security floor, see settings.gradle.kts")
  }
}

// Every project's plugin classpath (buildscript) and every configuration, including the tool ones
// (lint, KSP, Kotlin compiler / swift export), before any of them is resolved.
gradle.beforeProject {
  buildscript.configurations.configureEach { resolutionStrategy.applySecurityFloors() }
  configurations.configureEach { resolutionStrategy.applySecurityFloors() }
}

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
