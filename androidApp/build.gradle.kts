plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
}

// Version stamping from CI: -PappVersion=1.2.3 -PappVersionCode=42 (defaults for local builds).
val appVersion: String = (project.findProperty("appVersion") as String?)?.takeIf { it.isNotBlank() } ?: "1.0.0"
val appVersionCode: Int = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1

android {
  namespace = "com.suw1labs.worktracker"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "com.suw1labs.worktracker"
    minSdk = 31
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersion

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    // Blank env values (as set by CI without secrets) count as "not configured".
    val releaseKeystorePath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() } ?: "${rootDir}/my-upload-key.jks"
    if (file(releaseKeystorePath).isFile) {
      create("release") {
        storeFile = file(releaseKeystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
    val debugKeystore = file("${rootDir}/debug.keystore")
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfigs.findByName("release")?.let {
        signingConfig = it
      }
    }
    debug {
      signingConfigs.findByName("debugConfig")?.let {
        signingConfig = it
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  lint {
    // False positive: the check fires on registerForActivityResult even though the app uses no Fragments.
    disable += "InvalidFragmentVersionForActivityResult"
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  // All UI, view models and the Room data layer live in the shared multiplatform module.
  implementation(project(":composeApp"))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.compose.ui.test.junit4)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(libs.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.compose.ui.tooling)
}
