/*
The Library Loader plugin currently has an IDEA bug that causes it to not recognize the "libs" variable.
Until https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed the suppress annotation is required.
 */
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot).apply(false)
    alias(libs.plugins.spring.dependencymanagement)
    java
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()

}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

sourceSets {
    create("simple") {
        java {
            srcDir("src/simple/kotlin")
            compileClasspath += main.get().output
            runtimeClasspath += main.get().output
            configurations["simpleImplementation"].extendsFrom(configurations.implementation.get())
            configurations["simpleRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
        }
    }
}

java {
    registerFeature("simple") {
        usingSourceSet(sourceSets["simple"])
    }
}

dependencies {
    implementation("com.beeproduced:result")
    implementation(libs.kotlin.stdlib)
    implementation(libs.spring.boot.starter.web)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
