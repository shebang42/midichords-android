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
    minSdk = 23
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

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
      all {
        it.useJUnitPlatform()
      }
    }
  }
}

// Version constants
object Versions {
  const val kotlin = "1.9.22"
  const val coroutines = "1.7.3"
  const val lifecycle = "2.7.0"
  const val mockito = "5.10.0"
  const val mockitoKotlin = "5.2.1"
  const val robolectric = "4.11.1"
  const val androidxTest = "1.5.0"
  const val espresso = "3.5.1"
  const val androidxJUnit = "1.1.5"
  const val junitJupiter = "5.10.1"
}

dependencies {
  // Kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}")

  // AndroidX Core
  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.11.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")

  // Lifecycle
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}")

  // Testing
  testImplementation("junit:junit:4.13.2")
  // JUnit 5 (Jupiter)
  testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitJupiter}")
  testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.junitJupiter}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junitJupiter}")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${Versions.junitJupiter}") // For JUnit 4 backward compatibility
  
  testImplementation("org.mockito:mockito-core:${Versions.mockito}")
  testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")
  testImplementation("org.robolectric:robolectric:${Versions.robolectric}")
  testImplementation("androidx.test:core:${Versions.androidxTest}")
  testImplementation("androidx.test:runner:${Versions.androidxTest}")
  testImplementation("androidx.test:rules:${Versions.androidxTest}")
  testImplementation("androidx.test.ext:junit:${Versions.androidxJUnit}")
  testImplementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
  
  androidTestImplementation("androidx.test.ext:junit:${Versions.androidxJUnit}")
  androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
} 