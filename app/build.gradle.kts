plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.scopacompanion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.scopacompanion"
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
    }
    composeOptions { // Added this block
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    // Core KTX and Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // Added for viewModel() composable

    // Declare Compose BOM as a platform dependency for consistent versioning
    // The version is managed by libs.versions.toml via 'androidx-compose-bom' entry
    implementation(libs.androidx.compose.foundation)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Apply BOM for androidTest as well

    // Compose UI, Material3, and Tooling dependencies
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3) // Use the specific Compose Material3 alias if available
    implementation(libs.androidx.compose.material.icons.core) // ADDED Material Icons Core
    implementation(libs.androidx.compose.material.icons.extended) // ADDED Material Icons Extended

    // Test dependencies
    testImplementation(libs.androidx.compose.ui.test.junit4) // CORRECTED ALIAS
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4) // Use the specific Compose Test alias
    debugImplementation(libs.androidx.compose.ui.tooling) // Use the specific Compose Tooling alias
    debugImplementation(libs.androidx.compose.ui.test.manifest) // Use the specific Compose Test Manifest alias
}
