// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    if(libs.versions.kotlin.get().contains("2.")) {
        alias(libs.plugins.compose.compiler) apply false
    }


    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dependency.guard) apply false
    alias(libs.plugins.module.graph) apply true
    alias(libs.plugins.baselineprofile) apply false
}