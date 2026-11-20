plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val tmdbToken = System.getenv("TMDB_READ_ACCESS_TOKEN") ?: ""

android {
    namespace = "com.ricardo.streaminghub"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    defaultConfig {
        applicationId = "com.ricardo.streaminghub"
        minSdk = 28
        targetSdk = 28
        versionCode = 4
        versionName = "1.3"

        buildConfigField(
            "String",
            "TMDB_READ_ACCESS_TOKEN",
            "\"$tmdbToken\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
}
