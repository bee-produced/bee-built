plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.spring.boot).apply(false)
  alias(libs.plugins.spring.dependencymanagement)
  alias(libs.plugins.ktfmt)
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

repositories { mavenCentral() }

dependencyManagement {
  imports { mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) }
}

dependencies {
  implementation(project(":service.organisation.events"))
  implementation(project(":utils"))
  implementation("com.beeproduced:bee.buzz")
  implementation("com.beeproduced:bee.persistent")
  implementation("com.beeproduced:bee.persistent") {
    capabilities { requireCapability("com.beeproduced:bee.persistent-dgs") }
  }
  implementation(libs.kotlin.stdlib)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.konform)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.security.core)
  implementation(libs.spring.boot.starter.oauth2.client)
  testImplementation(libs.spring.security.test)

  testImplementation(libs.datafaker)
  testRuntimeOnly(libs.junit.engine)
  testImplementation(libs.spring.boot.starter.test) { exclude("org.mockito", "mockito-core") }
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.spring.boot.starter.data.jpa)
  testImplementation(libs.jdsl)
  testImplementation(libs.kotlin.test)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.springmockk)
}

tasks.withType<Test> { useJUnitPlatform() }
