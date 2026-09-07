plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.smartkiosk.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.smartkiosk.tv"
        minSdk = 23
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.glide)

    // Media3 ExoPlayer for Digital Signage
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Embedded Ktor HTTP Server for Remote Management
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
}
