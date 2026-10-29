import com.android.build.api.artifact.SingleArtifact

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "im.carson.cclock"
  compileSdk = 37
  defaultConfig {
    applicationId = "im.carson.cclock"
    minSdk = 29
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      // Signed with the local debug key so release APKs install without a keystore.
      signingConfig = signingConfigs.getByName("debug")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

// Every release build also leaves a copy of its APK at build/outputs/apk/CClock.apk.
androidComponents {
  onVariants(selector().withBuildType("release")) { variant ->
    val apkDir = variant.artifacts.get(SingleArtifact.APK)
    val named = layout.buildDirectory.file("outputs/apk/CClock.apk")
    val copy = tasks.register("copyReleaseApk") {
      inputs.dir(apkDir)
      outputs.file(named)
      doLast {
        val apk = apkDir.get().asFile.listFiles { file -> file.extension == "apk" }!!.single()
        apk.copyTo(named.get().asFile, overwrite = true)
      }
    }
    tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(copy) }
  }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.kotlinx.serialization.json)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)

  testImplementation(libs.junit)
}
