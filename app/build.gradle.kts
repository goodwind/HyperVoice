import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The Groq key for the proof of concept: read from the owner's machine at build time and put in the APK,
// never in the repository. A build without it still works; the service then answers "no key".
val groqKey: String = File(System.getProperty("user.home"), ".cache/hypervoice/groq.key")
    .takeIf { it.isFile }?.readText()?.trim().orEmpty()

android {
    namespace = "io.github.goodwind.hypervoice"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.goodwind.hypervoice"
        minSdk = 24
        targetSdk = 30
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GROQ_KEY", "\"$groqKey\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

dependencies {
    implementation(project(":shared"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
