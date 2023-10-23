import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    "processorImplementation"(sourceSets.main.get().output)
    "processorImplementation"(libs.dgs.spring.starter)
}

tasks.withType<Test> {
    useJUnitPlatform()
}