import java.util.Properties
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

    lint {
        // Not for Google Play: it is installed on a TV's own Android 11, where targetSdk 30 is what was tested.
        disable += "ExpiredTargetSdkVersion"
    }

    // Releases are signed with a key that lives outside version control, in local/signing.properties
    // (storeFile, storePassword, keyAlias, keyPassword). Android only installs an update signed with the
    // key of the copy it replaces, so that key must be kept for good. Without it (a fresh checkout) the
    // build falls back to the machine's debug key: it installs, but cannot update a release.
    val signingProps = rootProject.file("local/signing.properties")
    signingConfigs {
        if (signingProps.exists()) {
            val p = Properties().apply { signingProps.inputStream().use { load(it) } }
            create("release") {
                storeFile = file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // The key of the owner's machine is for debug builds only; a release never carries one.
            buildConfigField("String", "GROQ_KEY", "\"\"")
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
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
