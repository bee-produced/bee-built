/*
The Library Loader plugin currently has an IDEA bug that causes it to not recognize the "libs" variable.
Until https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed the suppress annotation is required.
 */
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.boot).apply(false)
    alias(libs.plugins.spring.dependencymanagement)
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    api("com.beeproduced:result")
    api("com.beeproduced:result") {
        capabilities { requireCapability("com.beeproduced:result-jpa") }
    }
    api(libs.michael.result)
    implementation(libs.kotlin.stdlib)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.mapstruct)
}

