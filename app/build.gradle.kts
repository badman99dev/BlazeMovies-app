plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("kapt")
    id("kotlin-parcelize")
    alias(libs.plugins.compose.compiler)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.movie.app.best"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blazemovies.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://blazemovies.badman993944.workers.dev/v1/\"")
            buildConfigField("String", "IMDB1_BASE_URL", "\"https://api.tiffara.com/\"")
            buildConfigField("String", "IMDB2_BASE_URL", "\"https://imdxapi.vercel.app/\"")
            buildConfigField("String", "MEILI_KEY_URL", "\"https://meilisearch.badman993944.workers.dev/\"")
            buildConfigField("String", "MEILI_SEARCH_URL", "\"https://meilisearch-rs25.onrender.com/indexes/movies/search\"")
            buildConfigField("String", "MEILI_PING_URL", "\"https://meilisearch-rs25.onrender.com/\"")
            buildConfigField("String", "DL_AGENT_BASE_URL", "\"https://dl-agent.badman993944.workers.dev/\"")
            buildConfigField("String", "GEMMA_BASE_URL", "\"https://slast430did.com/\"")
            buildConfigField("String", "GEMMA_PLAYLIST_HOST", "\"https://jemsa431ilk.com\"")
            buildConfigField("String", "SPARKLE_BASE_URL", "\"https://sparkling-breeze-1ad6.badman993944.workers.dev/\"")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"https://blazemovies.badman993944.workers.dev/v1/\"")
            buildConfigField("String", "IMDB1_BASE_URL", "\"https://api.tiffara.com/\"")
            buildConfigField("String", "IMDB2_BASE_URL", "\"https://imdxapi.vercel.app/\"")
            buildConfigField("String", "MEILI_KEY_URL", "\"https://meilisearch.badman993944.workers.dev/\"")
            buildConfigField("String", "MEILI_SEARCH_URL", "\"https://meilisearch-rs25.onrender.com/indexes/movies/search\"")
            buildConfigField("String", "MEILI_PING_URL", "\"https://meilisearch-rs25.onrender.com/\"")
            buildConfigField("String", "DL_AGENT_BASE_URL", "\"https://dl-agent.badman993944.workers.dev/\"")
            buildConfigField("String", "GEMMA_BASE_URL", "\"https://slast430did.com/\"")
            buildConfigField("String", "GEMMA_PLAYLIST_HOST", "\"https://jemsa431ilk.com\"")
            buildConfigField("String", "SPARKLE_BASE_URL", "\"https://sparkling-breeze-1ad6.badman993944.workers.dev/\"")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.accompanist.swiperefresh)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Cloudflare bypass solver (on-device cf_clearance acquisition)
    implementation(libs.cflib)
    
    // Image loading
    implementation(libs.coil.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // ExoPlayer (Media3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    
    // Ketch Download Manager (WorkManager-based)
    implementation("com.github.khushpanchal:Ketch:2.0.6")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Zip4j - ZIP extraction with progress monitoring
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    
    // DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Google Sign-In
    implementation(libs.play.services.auth)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    implementation(libs.lottie.compose)

    // ACRA Crash Reporting
    implementation(libs.acra.toast)
    implementation(libs.acra.dialog)
    implementation(libs.markdown)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}