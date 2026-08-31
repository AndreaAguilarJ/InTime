import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    // `expect object` sigue marcado como Beta en Kotlin 2.0. Se usa a propósito
    // para las fronteras de plataforma, así que se acepta explícitamente en vez
    // de arrastrar un aviso en cada compilación.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    // Los tres targets que necesita una app de App Store: dispositivo real,
    // simulador en Mac con Apple Silicon y simulador en Mac Intel.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "MomentumShared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                // Iconos vectoriales reales, los mismos que usa la app Android. Sin
                // ellos la barra de navegación dibujaba una cápsula vacía donde va
                // el icono, que se ve como un defecto.
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                // `api` y no `implementation`: los tipos de fecha aparecen en la
                // superficie pública del módulo, así que quien lo consume --la app
                // Android incluida-- necesita verlos.
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.momentummm.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
