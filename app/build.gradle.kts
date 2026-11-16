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

    defaultConfig {
        applicationId = "com.ricardo.streaminghub"
        minSdk = 28
        targetSdk = 28
        versionCode = 2
        versionName = "1.1"

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
