import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
}

kotlin {
  // --- Android (consumed by :androidApp) ---
  android {
    namespace = "com.suw1labs.worktracker.shared"
    compileSdk = 37
    minSdk = 31
    // Run commonTest on the Android host (JVM) as well.
    withHostTest {}
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
  }

  // --- Desktop: Windows / macOS / Linux ---
  jvm("desktop")

  // --- iOS (Compose Multiplatform 1.12 supports arm64 devices and Apple-silicon simulators) ---
  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
      implementation(libs.compose.material.icons.extended)
      implementation(libs.jetbrains.lifecycle.viewmodel.compose)
      implementation(libs.jetbrains.lifecycle.runtime.compose)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.datetime)
      // Backup file format (JSON) used to move data between devices.
      implementation(libs.kotlinx.serialization.json)
      // `api`: RoomDatabase.Builder is part of AppContainer's public constructor signature.
      api(libs.room.runtime)
      implementation(libs.sqlite.bundled)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
      implementation(libs.kotlinx.coroutines.android)
      // Android framework SQLite driver so Robolectric host tests can open the database too.
      implementation(libs.sqlite.framework)
    }
    getByName("desktopMain").dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutines.swing)
    }
    // Schema migration and backup/restore tests run against a real SQLite database on the JVM.
    getByName("desktopTest").dependencies {
      implementation(libs.room.testing)
    }
  }
}

// Every schema version is exported to composeApp/schemas/<db class>/<version>.json (committed to git).
// The JSON files are the source of truth for the migration test and must be updated whenever
// the @Database version is bumped.
room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  add("kspAndroid", libs.room.compiler)
  add("kspDesktop", libs.room.compiler)
  add("kspIosArm64", libs.room.compiler)
  add("kspIosSimulatorArm64", libs.room.compiler)
}

// Desktop installer version from CI: -PappVersion=1.2.3 (MSI needs a numeric MAJOR.MINOR.PATCH).
val desktopPackageVersion: String =
  (project.findProperty("appVersion") as String?)?.substringBefore('-')?.takeIf { Regex("\\d+\\.\\d+\\.\\d+").matches(it) } ?: "1.0.0"

compose.desktop {
  application {
    mainClass = "com.suw1labs.worktracker.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
      packageName = "WorkTracker"
      packageVersion = desktopPackageVersion
      description = "Work time tracking with project management, productivity reports and deadline reminders."
      vendor = "WorkTracker"

      windows {
        menuGroup = "WorkTracker"
        shortcut = true
        dirChooser = true
        perUserInstall = true
        upgradeUuid = "7C6C2C4E-3B8E-4F1D-9C1A-6E1B2F0A9D11"
        iconFile.set(project.file("icons/icon.ico"))
      }
      macOS {
        bundleID = "com.suw1labs.worktracker"
        iconFile.set(project.file("icons/icon.icns"))
      }
      linux {
        iconFile.set(project.file("icons/icon.png"))
      }
    }

    // Room + bundled SQLite do not survive ProGuard shrinking without extra rules.
    buildTypes.release.proguard { isEnabled.set(false) }
  }
}
