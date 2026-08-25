import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.dmsapplication"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.dmsapplication"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.dmsapplication.TestRunner"

        // Đọc file local.properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        val serverBaseUrl = properties.getProperty(
            "SERVER_BASE_URL",
            "http://10.0.2.2:5078/"
        )
        buildConfigField("String", "SERVER_BASE_URL", "\"$serverBaseUrl\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
    }

    testOptions {
        // Tắt animations để tests chạy nhanh hơn
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        // Disable orchestrator for now to avoid issues
        // execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

// Fix Firebase Protobuf dependency conflict
configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-javalite:3.21.12")
        force("com.google.protobuf:protobuf-lite:3.0.1")
    }
}

dependencies {
    // ========================================
    // EXISTING DEPENDENCIES
    // ========================================

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // CAMERA & VISION
    val camerax_version = "1.3.4"
    implementation("androidx.camera:camera-view:${camerax_version}")
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // ROOM DATABASE
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // ANDROID UI & COMPOSE
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.compose.ui:ui-viewbinding:1.6.8")
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Google Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Khác
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ========================================
    // ANDROID TESTING DEPENDENCIES
    // ========================================

    // AndroidX Test - Core
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // AndroidX Test - JUnit
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")

    // Espresso
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")

    // Mockito
    androidTestImplementation("org.mockito:mockito-core:5.3.1")
    androidTestImplementation("org.mockito:mockito-android:5.3.1")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    // Coroutines Test
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Fragment Testing
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")

    // UI Automator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    // Truth
    androidTestImplementation("com.google.truth:truth:1.1.5")
}