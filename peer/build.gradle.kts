plugins {
    id("java-library")
    kotlin("plugin.serialization")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.bcpkix.jdk15on)
    implementation(libs.ktor.ktor.utils)
    implementation(libs.kotlinx.serialization.json)

}
