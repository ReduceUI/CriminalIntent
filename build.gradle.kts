// This is the top-level build file for your project.
// It defines plugin versions for all sub-projects and modules.

plugins {
    alias(libs.plugins.android.application) apply false
    //id("com.android.application") version "7.1.2" apply false
    alias(libs.plugins.android.library) apply false
    //id("com.android.library") version "8.11.2" apply false
    alias(libs.plugins.kotlin.android) apply false
    //id("org.jetbrains.kotlin.android") version "1.6.10" apply false
    alias(libs.plugins.kotlin.kapt) apply false
    //id("org.jetbrains.kotlin.kapt") version "1.6.10" apply false
}

// This task will clean the project's build directories.
//tasks.register("clean", Delete::class) {
//    delete(rootProject.layout.buildDirectory)
//}



////OLD Plugins
//plugins {
//    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.kotlin.android) apply false
//
//}