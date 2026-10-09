import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
}

android {
    namespace = "ru.netology.nework"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.netology.nework"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        if (rootProject.file("maps.properties").exists()) {
            properties.load(rootProject.file("maps.properties").inputStream())
        }
        val mapkitApiKey = properties.getProperty("MAPS_API_KEY", "")

        if (rootProject.file("api.properties").exists()) {
            properties.load(rootProject.file("api.properties").inputStream())
        }
        val apiKey = properties.getProperty("API_KEY", "")

        buildConfigField("String", "MAPS_API_KEY", "\"$mapkitApiKey\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        buildConfigField("String", "BASE_URL", "\"http://94.228.125.136:8080\"")

    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.retrofit)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.room.paging)
    implementation(libs.converter.gson)
    implementation(libs.androidx.room)
    ksp(libs.androidx.room.compiler)
    implementation(libs.vbpd)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.imagepicker)
    implementation(libs.glide)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.swiperefreshlayout)
}