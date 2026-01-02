import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.jorgen.posebaby"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.jorgen.posebaby"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
             localPropertiesFile.inputStream().use { properties.load(it) }
        }
        buildConfigField("String", "ZHIPU_API_KEY", "\"${properties.getProperty("ZHIPU_API_KEY")}\"")
        buildConfigField("String", "DOUBAO_API_KEY", "\"${properties.getProperty("DOUBAO_API_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystoreFile.exists()) {
                keystoreFile.inputStream().use { keystoreProperties.load(it) }
            }
            
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.viewfinder.compose)

    // ML Kit Pose Detection
    implementation(libs.google.mlkit.pose.detection)

    // ZAI SDK
    implementation("ai.z.openapi:zai-sdk:0.3.0")
    
    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}