import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    java
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("bee.generative") {
            id = "bee.generative"
            implementationClass = "com.beeproduced.bee.generative.BeeGenerativePlugin"
        }
    }
}

dependencies {
    // implementation(kotlin("gradle-plugin"))
    api(libs.ksp.api)
    api(libs.kotlin.poet)
    implementation(libs.kotlin.stdlib)
    implementation(libs.ksp.plugin)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}