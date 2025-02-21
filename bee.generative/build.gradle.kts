import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.ktfmt)
  `maven-publish`
  signing
  java
  `java-gradle-plugin`
  `kotlin-dsl`
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

description = "Plugin for the `bee-built` platform."

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "21" } }

repositories { mavenCentral() }

gradlePlugin {
  plugins {
    register("bee.generative") {
      id = "bee.generative"
      implementationClass = "com.beeproduced.bee.generative.BeeGenerativePlugin"
      displayName = project.name
      description = project.description
    }
  }
}

java {
  withSourcesJar()
  withJavadocJar()
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

tasks.withType<Test> { useJUnitPlatform() }

ktfmt { googleStyle() }

val generateResources by
  tasks.registering {
    val propFile =
      file("${layout.buildDirectory.get().asFile.toURI()}/generated/bee.generative.properties")
    outputs.file(propFile)
    doLast {
      propFile.parentFile.mkdirs()
      propFile.writeText("version=${project.version}")
    }
  }

tasks.named<Copy>("processResources") { from(generateResources) }

// Based on
// https://www.tschuehly.de/posts/guide-kotlin-gradle-publish-to-maven-central/#51-generate-javadocs-and-sources-jars
publishing {
  publications {
    create<MavenPublication>("Maven") {
      from(components["java"])
      description = project.description
    }
    withType<MavenPublication> {
      pom {
        packaging = "jar"
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/bee-produced/bee-built")
        licenses {
          license {
            name.set("MIT license")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("kurbaniec")
            name.set("Kacper Urbaniec")
            email.set("kacper.urbaniec@beeproduced.com")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:bee-produced/bee-built.git")
          developerConnection.set("scm:git:ssh:git@github.com:bee-produced/bee-built.git")
          url.set("https://github.com/bee-produced/bee-built")
        }
      }
    }
  }
  repositories { maven { url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI() } }
}

jreleaser {
  project { copyright.set("bee produced") }
  gitRootSearch.set(true)
  signing {
    active.set(Active.ALWAYS)
    armored.set(true)
  }
  deploy {
    maven {
      nexus2 {
        create("maven-central") {
          active.set(Active.ALWAYS)
          url.set("https://s01.oss.sonatype.org/service/local")
          closeRepository.set(true)
          releaseRepository.set(true)
          stagingRepositories.add("build/staging-deploy")
        }
      }
    }
  }
}

// Required as plugin markers are only relevant for gradle plugin portal
// https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_markers
tasks.register("removePluginMetadata") {
  doLast {
    val dirToRemove = file("${layout.buildDirectory.get().asFile.toURI()}/staging-deploy/bee")
    dirToRemove.deleteRecursively()
  }
}

tasks.named("publish") { finalizedBy("removePluginMetadata") }
