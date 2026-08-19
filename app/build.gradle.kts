import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ai.helply.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ai.helply.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }

        val envFile = rootProject.file(".env")
        val envProperties = Properties()
        if (envFile.exists()) {
            envFile.forEachLine { line ->
                if (line.contains("=") && !line.startsWith("#")) {
                    val parts = line.split("=", limit = 2)
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"")
                    envProperties.setProperty(key, value)
                }
            }
        }
        val hfToken = envProperties.getProperty("HF_TOKEN") ?: System.getenv("HF_TOKEN") ?: ""
        buildConfigField("String", "DEFAULT_HF_TOKEN", "\"$hfToken\"")

        val googleOAuthClientId = envProperties.getProperty("GOOGLE_OAUTH_CLIENT_ID")
            ?: System.getenv("GOOGLE_OAUTH_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_OAUTH_CLIENT_ID", "\"$googleOAuthClientId\"")
        manifestPlaceholders["appAuthRedirectScheme"] = "ai.helply.oauth"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core & Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room Database + SQLCipher Encryption
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    // Security & Encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("net.openid:appauth:0.11.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // LiteRT / TFLite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-api:2.16.1")

    // MediaPipe GenAI - On-Device LLM Inference Engine
    implementation("com.google.mediapipe:tasks-genai:0.10.22")
    implementation("com.google.mediapipe:solution-core:0.10.20")

    // Kotlinx Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ML Kit OCR
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Chrome Custom Tabs (for OAuth redirect handling)
    implementation("androidx.browser:browser:1.8.0")

    // ─── Email Intelligence (Gmail OAuth via AppAuth + Gmail REST API) ──────────
    // AppAuth already listed above under Security & Encryption

    // WorkManager with Hilt support (background email polling)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
}
