plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.ferryapps.vitals"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ferryapps.vitals"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "2.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Solo la build de release manda estadísticas y fallos a Firebase (ver buildTypes).
        manifestPlaceholders["firebaseCollectionOff"] = "false"
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            isDebuggable = true
            manifestPlaceholders["firebaseCollectionOff"] = "true"
            optimization {
                enable = false
            }
        }
        release {
            isDebuggable = false
            proguardFiles("proguard-rules.pro")
            // R8: APK más pequeño y arranque más rápido. Importa en los móviles de gama
            // baja que usa la mayoría de usuarios (Moto E14, Redmi 12C…).
            optimization {
                enable = true
            }
        }
        // Build types del plugin de Baseline Profile. Se declaran solo para firmarlos con la clave
        // de debug (release no tiene firma en Gradle) y para que el emulador de la CI no mande
        // nada a Analytics ni a Crashlytics.
        create("nonMinifiedRelease") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            manifestPlaceholders["firebaseCollectionOff"] = "true"
        }
        create("benchmarkRelease") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            manifestPlaceholders["firebaseCollectionOff"] = "true"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric necesita los recursos para las capturas de Play Store.
            isIncludeAndroidResources = true
            all { test ->
                // Las capturas (src/test/.../storescreenshots) solo se generan a propósito:
                // ./gradlew :app:testDebugUnitTest -PstoreScreenshots
                if (project.hasProperty("storeScreenshots")) {
                    test.systemProperty("roborazzi.test.record", "true")
                    test.systemProperty("storeScreenshots.outDir", rootProject.file("docs/store-assets/screenshots/raw").absolutePath)
                    test.maxHeapSize = "3g"
                } else {
                    test.exclude("**/storescreenshots/**")
                }
            }
        }
    }
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    implementation(project(":core"))
    implementation(project(":designsystem"))
    implementation(project(":feature:monitor"))
    implementation(project(":feature:battery"))
    implementation(project(":feature:hardware"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:widget"))

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.kotlinMetadataJvm)

    // Firebase (google-services plugin active here)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // Compose + Navigation
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Baseline Profile: lo genera :baselineprofile y ProfileInstaller lo aplica al instalar
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Valoración en Google Play sin salir de la app
    implementation(libs.play.review.ktx)

    // WorkManager (Configuration.Provider en VitalsApplication)
    implementation(libs.workmanager.ktx)

    // Startup — explícita para que Lint resuelva InitializationProvider
    // (WorkManager ya la trae transitivamente, pero AGP 9 lo exige directo)
    implementation(libs.startup.runtime)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.leakcanary.android)
}
