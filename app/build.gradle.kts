plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)


    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id ("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ravi.busmanagementt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ravi.busmanagementt"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

}

dependencies {

    // Navigation
    implementation(libs.androidx.navigation.compose)
// ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Coroutines (using BOM)
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
// Material Icons
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

// Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation(libs.firebase.firestore)
    kapt("com.google.dagger:hilt-android-compiler:2.57.2")

    // Lottie for Animations
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Splash API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Google Maps Compose library
    // Google Maps Compose utility library
//    Google Services & Maps
    implementation ("com.google.android.gms:play-services-maps:19.2.0")
    val mapsComposeVersion = "6.12.1"
    implementation ("com.google.android.gms:play-services-places:17.1.0")
    implementation("com.google.android.libraries.places:places:5.0.0")
    implementation("com.google.maps.android:maps-compose:$mapsComposeVersion")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose-utils:$mapsComposeVersion")
    implementation("com.google.maps:google-maps-services:2.2.0")


    //firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions")

    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


    // Retrofit for network calls
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}