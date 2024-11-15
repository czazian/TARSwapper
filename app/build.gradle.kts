plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}

android {
    namespace = "com.example.tarswapper"
    compileSdk = 34


    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.example.tarswapper"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packagingOptions {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.5.0"))

    //Firebase Services
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    //Other Dependencies
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.github.bumptech.glide:glide:5.0.0-rc01")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("org.eclipse.angus:jakarta.mail:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("com.google.mlkit:language-id:17.0.0")   //Identify Language
    implementation("com.google.mlkit:translate:17.0.0")     //Translate Language
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.google.cloud:google-cloud-storage:2.15.0")
    implementation("com.itextpdf:itext7-core:7.2.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.4.0")
    implementation("com.google.maps.android:android-maps-utils:2.2.0")
    //implementation("io.kommunicate.sdk:kommunicateui:2.10.0")
    implementation ("me.relex:circleindicator:2.1.6") //slide indicator
    implementation ("com.google.android.exoplayer:exoplayer:2.18.1") //video player
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")      //pie chart
    implementation ("com.airbnb.android:lottie:3.4.0") //lottie animation


    //Compose Dependencies
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.0.0-alpha07")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.3")
    implementation("com.google.firebase:firebase-messaging:24.0.3")
    implementation("com.google.firebase:firebase-vertexai:16.0.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.tbuonomo:dotsindicator:4.2")

    //Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
