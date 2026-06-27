plugins {
    alias(libs.plugins.android.library)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
}

fun resolveTinaNativeAbis(): List<String> {
    val allAbiProperty = providers.gradleProperty("tina.allAbi").orNull?.trim()
    val allAbiRequested = when {
        allAbiProperty != null -> allAbiProperty.equals("true", ignoreCase = true)
        System.getenv("CI")?.equals("true", ignoreCase = true) == true -> true
        else -> gradle.startParameter.taskNames.any { it.contains("AllAbi", ignoreCase = true) }
    }
    if (allAbiRequested) return listOf("arm64-v8a", "x86_64")

    providers.gradleProperty("android.injected.build.abi").orNull
        ?.split(',')
        ?.asSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("arm64") || it.startsWith("aarch64") || it.startsWith("x86_64") }
        ?.let { abi ->
            return if (abi.startsWith("x86_64")) listOf("x86_64") else listOf("arm64-v8a")
        }

    return when (providers.gradleProperty("tina.devAbi").orNull?.trim()) {
        "x86_64" -> listOf("x86_64")
        else -> listOf("arm64-v8a")
    }
}

val tinaNativeAbis = resolveTinaNativeAbis()

android {
    namespace = "me.rerere.workspace"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += tinaNativeAbis
        }
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.xz)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
