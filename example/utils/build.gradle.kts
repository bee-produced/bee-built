plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.spring.boot).apply(false)
  alias(libs.plugins.spring.dependencymanagement)
  alias(libs.plugins.ktfmt)
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

repositories { mavenCentral() }

dependencies {
  api("com.beeproduced:bee.functional")
  api("com.beeproduced:bee.functional") {
    capabilities { requireCapability("com.beeproduced:bee.functional-persistent") }
  }
  api(libs.michael.result)
  // api(files("../etc.libs/kotlin-result-jvm-1.1.22-SNAPSHOT.jar"))
  // api("com.github.bee-produced.kotlin-result:kotlin-result:v1.1.22-SNAPSHOT")
  implementation(libs.kotlin.stdlib)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.mapstruct)
}
