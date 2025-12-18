plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    // Plugin Compose SUDAH DIHAPUS karena tidak dipakai
}

android {
    namespace = "com.example.myproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myproject"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17 // GANTI JADI 17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17" // GANTI JADI 17
    }

    // --- BAGIAN PENTING YANG DIPERBAIKI ---
    buildFeatures {
        viewBinding = true  // Wajib True agar ActivityMainBinding muncul
        compose = false     // Dimatikan karena kita pakai XML
    }

    // Blok composeOptions DIHAPUS karena tidak dipakai

    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    // Core Android (Wajib)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    implementation("com.google.firebase:firebase-auth-ktx:22.1.2")

    // TensorFlow Lite (Sesuai kode Anda)
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    // ... dependencies lainnya
    // AI TensorFlow Lite (Wajib)
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    // Testing (Standar)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}