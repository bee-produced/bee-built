
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot).apply(false)
    alias(libs.plugins.spring.dependencymanagement)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    id("bee.generative")
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

dependencies {
    implementation("com.beeproduced:bee.buzz")
    implementation("com.beeproduced:bee.persistent")
    implementation("com.beeproduced:bee.persistent") {
        capabilities { requireCapability("com.beeproduced:bee.persistent-dgs") }
    }
    beeGenerative("com.beeproduced:bee.persistent", "blaze")
    implementation(libs.kotlin.stdlib)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.konform)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.security.core)
    implementation(libs.spring.boot.starter.oauth2.client)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.api)
    testImplementation(libs.datafaker)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.spring.boot.starter.test) {
        exclude("org.mockito", "mockito-core")
    }
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.jdsl)
    testImplementation(libs.junit.api)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.springmockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

beeGenerative {
    arg("persistentPackageName", "com.beeproduced.datasource.b")
    arg("persistentSubPackageRepository", "repositories")
    arg("persistentSubPackageView", "views")
    arg("persistentSubPackageDSL", "dsl")
}

noArg {
    annotations("jakarta.persistence.Entity", "jakarta.persistence.Embeddable")
}

allOpen {
    annotations("jakarta.persistence.Entity")
}
