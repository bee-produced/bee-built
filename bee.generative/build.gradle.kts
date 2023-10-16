import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
The Library Loader plugin currently has an IDEA bug that causes it to not recognize the "libs" variable.
Until https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed the suppress annotation is required.
 */
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    java
    `java-gradle-plugin`
    `kotlin-dsl`
    // TODO: Fix versioning?
    // https://youtrack.jetbrains.com/issue/KT-54238
    id("org.jetbrains.kotlin.plugin.sam.with.receiver") version("1.8.0")
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