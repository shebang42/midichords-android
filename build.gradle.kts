buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.9.0")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
  }
}

tasks.register<Delete>("clean") {
  delete(layout.buildDirectory)
} 