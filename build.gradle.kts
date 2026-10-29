// Top-level build file for Kadan.
// Versions live in gradle/libs.versions.toml. Check the official releases pages
// (Android Gradle Plugin, Kotlin, Compose BOM, Room) at build time and bump
// the catalog if a version is no longer available — do not copy a version
// number from a blog post without checking it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
