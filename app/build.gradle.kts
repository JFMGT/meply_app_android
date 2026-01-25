plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.meply.meply"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.meply.meply"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE", "\"https://admin.meeplemates.de/api/\"")
            buildConfigField("String", "IMAGE_BASE", "\"https://admin.meeplemates.de\"")
            buildConfigField("String", "WEB_BASE", "\"https://dev.meply.de\"")
            buildConfigField("String", "APP_JWT", "\"7390af640e9802bca102d00294a68093bac023d80b545a2ce0dfa527af366e47c033f3a6c99fc4a5334a9ad7582f8f433415f0ada1d9bfa65e52104e1b3e347f88b2b7e960f6bbdcce27b1e163b9601de2dbb4ba0c71acf14326527f0b733bda507b96b23108a94961ddf3e3f5b92308ea5627f2557d5aded1ef44715cde8bda\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE", "\"https://api.meeplemates.de/api/\"")
            buildConfigField("String", "IMAGE_BASE", "\"https://api.meeplemates.de\"")
            buildConfigField("String", "WEB_BASE", "\"https://meply.de\"")
            buildConfigField("String", "APP_JWT", "\"HIER_LIVE_TOKEN_EINTRAGEN\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // Die Version 4.12.0 ist bereits in deinen Abhängigkeiten
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.github.yalantis:ucrop:2.2.8")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}