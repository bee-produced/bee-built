/*
The Library Loader plugin currently has an IDEA bug that causes it to not recognize the "libs" variable.
Until https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed the suppress annotation is required.
 */
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation("com.beeproduced:events")
    api(project(":service.media.entities"))
}
