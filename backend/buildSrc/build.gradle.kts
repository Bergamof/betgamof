plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.serialization)
    implementation(libs.gradle.ktlint)
    implementation(libs.gradle.detekt)
}
