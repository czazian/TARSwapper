buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
        repositories {
            mavenCentral()
        }
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("androidx.navigation.safeargs") version "2.7.7" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
}