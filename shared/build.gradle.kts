import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
       namespace = "com.ferbotz.momentic.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    sourceSets {
        all {
            // kotlin.time.Instant / Clock carry the timestamps on creations, stats and RSVPs.
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            // Data layer — Android HTTP engine
            implementation(libs.ktor.client.okhttp)
            // Presentation layer — previews and legal pages open in Custom Tabs
            implementation(libs.androidx.browser)
            // Presentation layer — upright, cropped photos before upload
            implementation(libs.androidx.exifinterface)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation.compose)

            // Data layer — networking (Ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            // Data layer — serialization, coroutines, dates
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // Data layer — preferences
            implementation(libs.multiplatform.settings.noArg)
            // Data layer — DI (exposed so the app module can call initKoin)
            api(libs.koin.core)
            // Data layer — logging
            implementation(libs.kermit)
            // Presentation layer — async image loading (template art, uploaded photos)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            // Presentation layer — drag to reorder photos and a group's entries
            implementation(libs.reorderable)
        }
        iosMain.dependencies {
            // Data layer — iOS HTTP engine
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Fonts (and their licences) live in commonMain/composeResources; the generated accessors sit beside the app's code.
compose.resources {
    packageOfResClass = "com.ferbotz.momentic.resources"
}
