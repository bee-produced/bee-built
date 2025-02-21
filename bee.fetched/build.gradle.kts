import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.ktfmt)
  `maven-publish`
  signing
  java
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

description = "Automatically generate nested data fetchers for usage with data loaders."

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "21" } }

repositories { mavenCentral() }

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
  withSourcesJar()
  withJavadocJar()
  registerFeature("processor") {
    usingSourceSet(sourceSets["processor"])
    withSourcesJar()
    withJavadocJar()
  }
}

dependencies {
  implementation(libs.kotlin.stdlib)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.engine)
  "processorImplementation"("com.beeproduced:bee.generative:$version")
  "processorImplementation"(sourceSets.main.get().output)
  "processorImplementation"(libs.dgs.spring.starter)
}

tasks.withType<Test> { useJUnitPlatform() }

allprojects {
  // https://github.com/cortinico/ktfmt-gradle
  // https://github.com/facebook/ktfmt
  ktfmt { googleStyle() }
}

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
