/**
 * Genera el Baseline Profile de :app en un emulador (Gradle Managed Device). Se lanza a mano o
 * desde .github/workflows/baseline-profile.yml:
 *   ./gradlew :app:generateReleaseBaselineProfile
 * El resultado va a app/src/release/generated/baselineProfiles/ y se sube a git.
 */
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.ferryapps.vitals.baselineprofile"
    compileSdk = 36

    defaultConfig {
        // Baseline Profile sin root necesita Android 13+; el emulador usa 34.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    targetProjectPath = ":app"

    testOptions.managedDevices.localDevices {
        create("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
