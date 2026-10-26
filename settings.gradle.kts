pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.2.2"
        id("org.jetbrains.kotlin.android") version "1.9.20"
        id("com.google.devtools.ksp") version "1.9.20-1.0.14"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    }
}

plugins {
    id("org.gradle.toolchains.foojay") version "0.7.0"
}

// Configure Java toolchain auto-download
toolchainManagement {
    jvm {
        javaRepositories {
            repository("foojay") {
                resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Momentum"
include(":app")