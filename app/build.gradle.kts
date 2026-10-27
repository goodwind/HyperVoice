import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The Groq key for the proof of concept: read from the owner's machine at build time and put in the APK,
// never in the repository. A build without it still works; the service then answers "no key".
// -PnoKey builds without it, to see the app as a person without a key does.
val groqKey: String = File(System.getProperty("user.home"), ".cache/hypervoice/groq.key")
    .takeIf { it.isFile && !project.hasProperty("noKey") }?.readText()?.trim().orEmpty()

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

    buildTypes {
        // The key of the owner's machine is for debug builds only; a release never carries one.
        release { buildConfigField("String", "GROQ_KEY", "\"\"") }
    }

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
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
