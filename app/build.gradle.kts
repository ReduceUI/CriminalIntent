import org.jetbrains.kotlin.gradle.dsl.JvmTarget
configurations.all {
    resolutionStrategy {
        // Force a specific version for the AndroidX annotation library
        force("androidx.annotation:annotation:1.9.1")
        // Force a specific version for the JetBrains annotation library
        force("org.jetbrains:annotations:23.0.0")
        // Force the newest Kotlin standard library version as well, just in case
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    }
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)

}

android {
    namespace = "com.bignerdranch.android.criminalintent"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bignerdranch.android.criminalintent"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx){
        exclude(group = "org.jetbrains", module = "annotations")
    }
    implementation("org.jetbrains:annotations:23.0.0")


    implementation(libs.androidx.appcompat){
        exclude(group = "org.jetbrains", module = "annotations")
    }
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation (libs.androidx.fragment.testing)
    testImplementation(kotlin("test"))
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
//    implementation (libs.androidx.recyclerview)

    kapt (libs.room.compiler)
    implementation (libs.androidx.room.ktx)

}