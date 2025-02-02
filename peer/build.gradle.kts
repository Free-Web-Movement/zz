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

    val room_version = "2.7.0-alpha12"
    
//
    implementation("androidx.room:room-runtime:$room_version")
//
//    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
//    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")
//
//    // If this project only uses Java source, use the Java annotationProcessor
//    // No additional plugins are necessary
//    annotationProcessor("androidx.room:room-compiler:$room_version")
//
//    // optional - Kotlin Extensions and Coroutines support for Room
//    implementation("androidx.room:room-ktx:$room_version")

    testImplementation(libs.kotlin.test)
}
