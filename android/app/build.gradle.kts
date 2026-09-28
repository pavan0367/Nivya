import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Apply Google Services plugin if google-services.json is present
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.nivya"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nivya"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            FileInputStream(localPropsFile).use { localProps.load(it) }
        }
        val mapsApiKey = System.getenv("MAPS_API_KEY")
            ?: (project.findProperty("MAPS_API_KEY") as? String)
            ?: localProps.getProperty("MAPS_API_KEY", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"${mapsApiKey}\"")
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                FileInputStream(localPropsFile).use { localProps.load(it) }
            }

            val keystorePath = System.getenv("NIVYA_RELEASE_STORE_FILE")
                ?: project.findProperty("NIVYA_RELEASE_STORE_FILE") as? String
                ?: localProps.getProperty("NIVYA_RELEASE_STORE_FILE")
            val storePass = System.getenv("NIVYA_RELEASE_STORE_PASSWORD")
                ?: project.findProperty("NIVYA_RELEASE_STORE_PASSWORD") as? String
                ?: localProps.getProperty("NIVYA_RELEASE_STORE_PASSWORD")
            val keyAl = System.getenv("NIVYA_RELEASE_KEY_ALIAS")
                ?: project.findProperty("NIVYA_RELEASE_KEY_ALIAS") as? String
                ?: localProps.getProperty("NIVYA_RELEASE_KEY_ALIAS")
            val keyPass = System.getenv("NIVYA_RELEASE_KEY_PASSWORD")
                ?: project.findProperty("NIVYA_RELEASE_KEY_PASSWORD") as? String
                ?: localProps.getProperty("NIVYA_RELEASE_KEY_PASSWORD")

            val isSigningRequired = project.hasProperty("requireSigning") ||
                System.getenv("REQUIRE_RELEASE_SIGNING") == "true"

            if (!keystorePath.isNullOrBlank()) {
                val ksFile = file(keystorePath)
                if (ksFile.exists()) {
                    storeFile = ksFile
                    storePassword = storePass
                    keyAlias = keyAl
                    keyPassword = keyPass
                } else if (isSigningRequired) {
                    throw org.gradle.api.GradleException("Release signing keystore file not found at: $keystorePath")
                }
            } else if (isSigningRequired) {
                throw org.gradle.api.GradleException(
                    "Release signing credentials missing. Set NIVYA_RELEASE_STORE_FILE, NIVYA_RELEASE_STORE_PASSWORD, NIVYA_RELEASE_KEY_ALIAS, NIVYA_RELEASE_KEY_PASSWORD environment variables or Gradle properties."
                )
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            buildConfigField("String", "API_BASE_URL", "\"https://nivya-blbf.onrender.com/api/v1/\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.forkEvery = 1
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "src/main/java",
                "../core",
                "../data",
                "../domain",
                "../permissions",
                "../services",
                "../ui"
            )
            res.srcDirs("src/main/res")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }
}

dependencies {
    // Core Android & Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Network: Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room Database + KSP
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore & Security Crypto
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Firebase Cloud Messaging (FCM)
    implementation("com.google.firebase:firebase-messaging:23.4.1")

    // Google Play Services Location (hardware GPS telemetry)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // OpenStreetMap Android Library (Free OSM visualization)
    implementation("org.osmdroid:osmdroid-android:6.1.18")


    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
