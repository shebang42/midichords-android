plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
}

android {
  namespace = "com.midichords"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.midichords"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

  // AndroidX Core
  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.11.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")

  // Lifecycle
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

  // Testing
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.10.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
} 