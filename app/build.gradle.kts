plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dagger.hilt.android.plugin")
}

android {
        namespace = "com.momentummm.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.momentummm.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Estas propiedades deben estar en gradle.properties (no en Git)
            storeFile = file(project.findProperty("MOMENTUM_STORE_FILE") as String? ?: "momentum-release-key.jks")
            storePassword = project.findProperty("MOMENTUM_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("MOMENTUM_KEY_ALIAS") as String?
            keyPassword = project.findProperty("MOMENTUM_KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ndkVersion = "29.0.13599879 rc2"
    buildToolsVersion = "36.0.0"
}

kotlin {
    jvmToolchain(21)
}

configurations.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:2.0.21",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21"
    )
}

dependencies {
    // Forzar alineamiento de todas las libs Kotlin a 2.0.21
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:2.0.21"))
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
    }

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0") // Para AutoSyncManager lifecycle observer
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Material Components (Views) for XML themes
    implementation("com.google.android.material:material:1.12.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room database (keeping for migration)
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Appwrite SDK (actualizado)
    implementation("io.appwrite:sdk-for-android:8.1.0")


    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Widget support
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // In-app billing for purchases
    implementation("com.android.billingclient:billing:6.1.0")
    implementation("com.android.billingclient:billing-ktx:6.1.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Export capabilities - CSV/PDF
    implementation("com.opencsv:opencsv:5.8")
    implementation("com.itextpdf:itext7-core:7.2.5")


    // Enhanced UI components
    implementation("androidx.compose.animation:animation:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")


    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}