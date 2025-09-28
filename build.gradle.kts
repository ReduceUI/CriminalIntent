// This is the top-level build file for your project.
// It defines plugin versions for all sub-projects and modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.safeargs.kotlin) apply false

}

// This task will clean the project's build directories.
//tasks.register("clean", Delete::class) {
//    delete(rootProject.layout.buildDirectory)
//}