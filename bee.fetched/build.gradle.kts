import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
The Library Loader plugin currently has an IDEA bug that causes it to not recognize the "libs" variable.
Until https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed the suppress annotation is required.
 */
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    java
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

sourceSets {
    create("processor") {
        java {
            srcDir("src/processor/kotlin")
            compileClasspath += main.get().output
            runtimeClasspath += main.get().output
            configurations["processorImplementation"].extendsFrom(configurations.implementation.get())
            configurations["processorRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
        }
    }
}

java {
    registerFeature("processor") {
        usingSourceSet(sourceSets["processor"])
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)

    "processorImplementation"("com.beeproduced:bee.generative")
    "processorImplementation"(libs.kotlin.poet)
}

tasks.withType<Test> {
    useJUnitPlatform()
}