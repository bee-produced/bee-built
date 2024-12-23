plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ktfmt)
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

java.sourceCompatibility = JavaVersion.VERSION_17

java.targetCompatibility = JavaVersion.VERSION_17

repositories { mavenCentral() }

dependencies {
  implementation(libs.kotlin.stdlib)
  implementation("com.beeproduced:bee.buzz")
  api(project(":service.media.entities"))
}
