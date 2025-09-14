// app/build.gradle.kts
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

/** Pročitaj local.properties ako postoji */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
/** Fallback na 10.0.2.2 (host iz Android emulatora) */
val apiBaseUrl: String = localProps.getProperty("API_BASE_URL") ?: "http://10.0.2.2:8000/"

android {
    namespace = "com.example.secureimagemessenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.secureimagemessenger"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Dostupno u kodu kao BuildConfig.API_URL
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    // (opcionalno) razdvojiš URL po build tipovima
    /*
    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"http://10.0.2.2:8000/\"")
        }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "API_URL", "\"https://api.tvoj-domen.com/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    */
}

dependencies {
    // Compose BOM (1.7.x)
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")
    // Coil (slike)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Retrofit + Moshi + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    implementation("com.google.firebase:firebase-firestore-ktx")
    // ako koristiš await():
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
