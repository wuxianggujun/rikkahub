plugins {
    alias(libs.plugins.android.library)
}

val webUiDir = rootProject.layout.projectDirectory.dir("web-ui")
val webUiNodeModulesDir = webUiDir.dir("node_modules")
val webStaticResourcesDir = layout.projectDirectory.dir("src/main/resources/static")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val pnpmCommand = if (isWindows) {
    providers.environmentVariable("PNPM_HOME")
        .map { pnpmHome -> file("$pnpmHome/pnpm.CMD").absolutePath }
        .getOrElse("pnpm")
} else {
    "pnpm"
}

fun Exec.configurePnpmCommand(vararg arguments: String) {
    if (isWindows) {
        commandLine("cmd", "/c", pnpmCommand, *arguments)
    } else {
        commandLine("zsh", "-ic", "pnpm ${arguments.joinToString(" ")}")
    }
}

val installWebUiDependencies = tasks.register<Exec>("installWebUiDependencies") {
    group = "build"
    description = "Install web-ui dependencies from the lockfile before building static resources."

    workingDir = webUiDir.asFile
    configurePnpmCommand("install", "--frozen-lockfile")

    inputs.files(
        webUiDir.file(".npmrc"),
        webUiDir.file("package.json"),
        webUiDir.file("pnpm-lock.yaml"),
        webUiDir.file("pnpm-workspace.yaml")
    )
    outputs.dir(webUiNodeModulesDir)
}

val buildWebUi = tasks.register<Exec>("buildWebUi") {
    group = "build"
    description = "Build web-ui and copy its static output into the web module resources."

    dependsOn(installWebUiDependencies)
    workingDir = webUiDir.asFile
    configurePnpmCommand("run", "build")

    inputs.files(
        webUiDir.file("package.json"),
        webUiDir.file("pnpm-lock.yaml"),
        webUiDir.file("components.json"),
        webUiDir.file("copy.ts"),
        webUiDir.file("react-router.config.ts"),
        webUiDir.file("tsconfig.json"),
        webUiDir.file("vite.config.ts"),
        webUiDir.file("vite-env.d.ts")
    )
    inputs.dir(webUiDir.dir("app"))
    inputs.dir(webUiDir.dir("public"))
    outputs.dir(webStaticResourcesDir)
}

android {
    namespace = "me.rerere.rikkahub.web"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

tasks.named("preBuild") {
    dependsOn(buildWebUi)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ktor server
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.conditional.headers)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.cors)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.sse)
    api(libs.ktor.server.cio)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
