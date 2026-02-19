import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeHotReload)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.buildkonfig)
  alias(libs.plugins.mokkery)
  alias(libs.plugins.sqldelight)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
  localPropertiesFile.inputStream().use { localProperties.load(it) }
}

buildkonfig {
  packageName = "com.hologrampacific.flyergoblin"

  // Default values for when properties are not set
  defaultConfigs {
    buildConfigField(
      STRING,
      "SOUNDCLOUD_CLIENT_ID",
      localProperties.getProperty("soundcloud.client.id", ""),
    )
    buildConfigField(
      STRING,
      "SOUNDCLOUD_CLIENT_SECRET",
      localProperties.getProperty("soundcloud.client.secret", ""),
    )
  }
}

kotlin {
  androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  jvm()

  sourceSets {
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.activity.compose)
    }
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(compose.materialIconsExtended)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.viewmodel.navigation3)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.androidx.navigation3.ui)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.datetime)
      implementation(libs.filekit.core)
      implementation(libs.filekit.compose)
      implementation(libs.koin.core)
      implementation(libs.koin.compose)
      implementation(libs.koin.compose.viewmodel)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.serialization.kotlinx.json)
      implementation(libs.ktor.client.logging)
      implementation(libs.kermit)
      implementation(libs.sqldelight.coroutines.extensions)
      implementation(libs.coil.compose)
      implementation(libs.coil.network.ktor)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.ktor.client.mock)
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.mokkery.runtime)
    }
    androidMain.dependencies {
      implementation(libs.ktor.client.okhttp)
      implementation(libs.sqldelight.android.driver)
    }
    androidUnitTest.dependencies { implementation(libs.sqldelight.sqlite.driver) }
    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
      implementation(libs.sqldelight.native.driver)
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
      implementation(libs.ktor.client.java)
      implementation(libs.slf4j.simple)
      implementation(libs.compose.webview.multiplatform)
      implementation(libs.sqldelight.sqlite.driver)
    }
  }
}

android {
  namespace = "com.hologrampacific.flyergoblin"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.hologrampacific.flyergoblin"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  buildTypes { getByName("release") { isMinifyEnabled = false } }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

dependencies { debugImplementation(libs.compose.uiTooling) }

compose.desktop {
  application {
    mainClass = "com.hologrampacific.flyergoblin.MainKt"

    // JVM args required for JCEF (Chromium WebView) on Java 17+
    jvmArgs("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED")
    jvmArgs("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED")
    jvmArgs("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED")

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "com.hologrampacific.flyergoblin"
      packageVersion = "1.0.0"
    }
  }
}

val appPackageName = "com.hologrampacific.flyergoblin"
val dbName = "flyergoblin.db"

tasks.register<Exec>("resetDatabase") {
  group = "flyergoblin"
  description = "Deletes $dbName from desktop, connected Android device, and booted iOS simulator"
  commandLine(
    "bash",
    "-c",
    """
    (rm -f "${'$'}HOME/.flyergoblin/$dbName" && echo "Desktop: deleted") || echo "Desktop: skipping"
    (adb=${'$'}{ANDROID_HOME:-${'$'}HOME/Library/Android/sdk}/platform-tools/adb && ${'$'}adb shell run-as $appPackageName rm -f "databases/$dbName" 2>/dev/null && echo "Android: deleted") || echo "Android: skipping"
    (container=${'$'}(xcrun simctl get_app_container booted $appPackageName data 2>/dev/null) && rm -f "${'$'}container/Library/Application Support/databases/$dbName" && echo "iOS Simulator: deleted") || echo "iOS Simulator: skipping"
    exit 0
    """
      .trimIndent(),
  )
}

sqldelight {
  databases {
    create("AppDatabase") {
      packageName.set("com.hologrampacific.flyergoblin.db")
      schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
      verifyMigrations.set(true)
    }
  }
}
