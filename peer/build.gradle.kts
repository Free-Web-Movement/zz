import org.gradle.internal.declarativedsl.parsing.main

plugins {
    id("java-library")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
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

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.core)
    implementation(libs.kmp.setting)

    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.ktx)

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.kotlin.test)
}
