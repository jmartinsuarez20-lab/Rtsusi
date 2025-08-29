/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ritsuaichat"
    compileSdk = 34

    val jllamaLib = file("java-llama.cpp")

    // Execute "mvn compile" if folder target/ doesn't exist at ./java-llama.cpp/
    if (!file("$jllamaLib/target").exists()) {
        exec {
            commandLine = listOf("cmd", "/c", "mvn", "compile")
            workingDir = file("java-llama.cpp/")
        }
    }

    defaultConfig {
        applicationId = "com.example.ritsuaichat"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        externalNativeBuild {
            cmake {
                // Add an flags if needed
                cppFlags += ""
                arguments += ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // MODIFICADO
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Declare c++ sources
    externalNativeBuild {
        cmake {
            path = file("$jllamaLib/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Declare java sources
    sourceSets {
        named("main") {
            // Add source directory for java-llama.cpp
            java.srcDir("$jllamaLib/src/main/java")
        }
    }
}

dependencies {

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Llama.cpp (using local submodule)
    // Dependencies were:
    // implementation("de.kherud.llama-cpp-java:llama-cpp-java:1.8.0")
    // implementation("de.kherud.llama-cpp-java:backend-android-arm64-v8a:1.8.0")
    // implementation("de.kherud.llama-cpp-java:backend-android-armeabi-v7a:1.8.0")

    // PocketSphinx (free speech recognition)
    // implementation("edu.cmu.pocketsphinx.android:pocketsphinx-android:5prealpha@aar") // Reemplazado por Vosk
    implementation("com.alphacephei:vosk-android:0.3.47") // Alternativa a PocketSphinx


    // Other free speech recognition (alternative to PocketSphinx)
    // implementation("org.mozilla.deepspeech:android-inference:0.9.3") // Mozilla DeepSpeech (check availability)
    // implementation("com.alphacephei:vosk-android:0.3.32") // Vosk API

    // Free Text-to-Speech (TTS)
    // implementation("com.sun.speech.freetts:freetts:1.2.2") // FreeTTS (check Android compatibility)
    // Consider using Android's built-in TTS engine which is free

    // Hugging Face Transformers (for backup AI models, choose free models)
    // implementation("com.edenred.android.huggingface:huggingface-android-sdk:0.0.3") // May need specific model implementations

    // Room Database for local storage
    implementation("androidx.room:room-runtime:2.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")
    // To use Kotlin Symbol Processing (KSP)
    // ksp("androidx.room:room-compiler:2.5.0") // if you switch to KSP

    // OpenGL ES for 3D rendering (Android SDK includes this)
    // No specific Gradle dependency needed unless using a third-party engine

    // Retrofit for local communication (if Ritsu components are in separate processes/services)
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Or other converters

    // ExoPlayer for multimedia
    implementation("androidx.media3:media3-exoplayer:1.0.0")
    implementation("androidx.media3:media3-ui:1.0.0")

    // CameraX for camera functions
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
}
