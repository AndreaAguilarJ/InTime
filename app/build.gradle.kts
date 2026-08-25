import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dagger.hilt.android.plugin")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use { load(it) }
}

// CI puede inyectar la variable de entorno; en desarrollo se usa local.properties.
// El valor nunca se imprime. Una cadena vacía desactiva Gemini de forma segura.
val geminiApiKey = providers.environmentVariable("GEMINI_API_KEY").orNull
    ?: localProperties.getProperty("GEMINI_API_KEY")
    ?: ""
val geminiBuildConfigValue = "\"${geminiApiKey
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")}\""

val releaseSigningPropertyNames = listOf(
    "MOMENTUM_STORE_FILE",
    "MOMENTUM_STORE_PASSWORD",
    "MOMENTUM_KEY_ALIAS",
    "MOMENTUM_KEY_PASSWORD"
)
val releaseSigningValues = releaseSigningPropertyNames.associateWith { propertyName ->
    providers.gradleProperty(propertyName).orNull
}

android {
    namespace = "com.momentummm.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.momentummm.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.1.0"

        buildConfigField("String", "GEMINI_API_KEY", geminiBuildConfigValue)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = releaseSigningValues["MOMENTUM_STORE_FILE"]
                ?.takeIf { it.isNotBlank() }
                ?.let { rootProject.file(it) }
            storePassword = releaseSigningValues["MOMENTUM_STORE_PASSWORD"]
            keyAlias = releaseSigningValues["MOMENTUM_KEY_ALIAS"]
            keyPassword = releaseSigningValues["MOMENTUM_KEY_PASSWORD"]
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
    lint {
        // Dos detectores de Compose crashean sobre MinimalAppListScreen.kt y abortan TODO
        // el analisis. La causa de fondo es una combinacion de versiones incompatible:
        // el proyecto usa Kotlin 2.0.21 con Compose BOM 2023.10.01, que es de octubre de
        // 2023 y viene pensado para Kotlin 1.9; sus artefactos de lint no entienden el
        // UAST de Kotlin 2.0. Son fallos de la herramienta, no del codigo de la app, y el
        // remedio lo sugiere lint en su propio mensaje de error. Sin esto el analisis
        // completo NO se puede ejecutar: es preferible perder DOS comprobaciones antes
        // que quedarse sin ninguna. La solucion de fondo es subir el Compose BOM.
        disable += "AutoboxingStateCreation"
        disable += "MutableCollectionMutableState"
    }
    testOptions {
        unitTests {
            // Las pruebas unitarias corren en la JVM, sin framework de Android: sin esto,
            // cualquier llamada a android.util.Log lanza "Method e in android.util.Log not
            // mocked" y tumba la prueba aunque la logica bajo prueba sea correcta.
            // OJO: esto hace que las APIs de Android devuelvan valores por defecto, asi que
            // la logica verificable (por ejemplo el hash de contrasena) debe usar APIs de la
            // JVM y no del framework, o la prueba quedaria verde midiendo nada.
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ndkVersion = "29.0.13599879 rc2"
    buildToolsVersion = "36.0.0"
}

// Se ejecuta antes de la validación opaca de AGP, pero solo cuando se pide release.
tasks.configureEach {
    if (name == "validateSigningRelease") {
        doFirst {
            val missingProperties = releaseSigningValues
                .filterValues { it.isNullOrBlank() }
                .keys
                .sorted()
            if (missingProperties.isNotEmpty()) {
                throw GradleException(
                    "No se puede generar release: faltan ${missingProperties.joinToString()}. " +
                        "Defínelas en ~/.gradle/gradle.properties (recomendado) o con -P; " +
                        "no guardes el keystore ni las contraseñas en el repositorio."
                )
            }

            val configuredStoreFile = rootProject.file(
                checkNotNull(releaseSigningValues["MOMENTUM_STORE_FILE"])
            )
            if (!configuredStoreFile.isFile) {
                throw GradleException(
                    "No se puede generar release: MOMENTUM_STORE_FILE no apunta a un fichero " +
                        "de keystore existente (${configuredStoreFile.absolutePath})."
                )
            }
        }
    }
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

    // Google Generative AI (Gemini)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Widget support
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // In-app billing for purchases
    implementation("com.android.billingclient:billing:7.1.1")
    implementation("com.android.billingclient:billing-ktx:7.1.1")

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