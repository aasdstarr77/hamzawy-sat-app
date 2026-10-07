plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hamzawy.sat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hamzawy.sat"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("com.google.code.gson:gson:2.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
