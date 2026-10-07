plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.whatscheck.engines"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.whatscheck.engines"
        minSdk = 29
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
        // El .aar de whatsmeow solo trae arm64-v8a (los dos teléfonos de prueba son arm64)
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // app de pruebas: no se distribuye
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":engine-api"))
    // F4: whatsmeow compilado con gomobile (engine-whatsmeow/build-aar.sh)
    implementation(files("libs/wacheck.aar"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    testImplementation(libs.junit)
}
