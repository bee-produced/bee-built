import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.jpa)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.ktfmt)
  `maven-publish`
  signing
  java
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

description = "Easier data handling for GraphQL + JPA"

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "21" } }

repositories { mavenCentral() }

sourceSets {
  create("dgs") {
    java {
      srcDir("src/dgs/kotlin")
      compileClasspath += main.get().output
      runtimeClasspath += main.get().output
      configurations["dgsImplementation"].extendsFrom(configurations.implementation.get())
      configurations["dgsRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
    }
  }
  create("jpa") {
    java {
      srcDir("src/jpa/kotlin")
      compileClasspath += main.get().output
      runtimeClasspath += main.get().output
      configurations["jpaImplementation"].extendsFrom(configurations.implementation.get())
      configurations["jpaRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
    }
  }
  create("blaze") {
    java {
      srcDir("src/blaze/kotlin")
      compileClasspath += main.get().output
      runtimeClasspath += main.get().output
      configurations["blazeImplementation"].extendsFrom(configurations.implementation.get())
      configurations["blazeRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
    }
  }
  create("blaze-processor") {
    java {
      srcDir("src/blaze-processor/kotlin")
      compileClasspath += main.get().output
      runtimeClasspath += main.get().output
      configurations["blazeProcessorImplementation"].extendsFrom(
        configurations.implementation.get()
      )
      configurations["blazeProcessorRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
    }
  }
}

java {
  withSourcesJar()
  withJavadocJar()
  registerFeature("dgs") {
    usingSourceSet(sourceSets["dgs"])
    withSourcesJar()
    withJavadocJar()
  }
  registerFeature("jpa") {
    usingSourceSet(sourceSets["jpa"])
    withSourcesJar()
    withJavadocJar()
  }
  registerFeature("blaze") {
    usingSourceSet(sourceSets["blaze"])
    withSourcesJar()
    withJavadocJar()
  }
  registerFeature("blazeProcessor") {
    usingSourceSet(sourceSets["blaze-processor"])
    withSourcesJar()
    withJavadocJar()
  }
}

// Set agent as defined by JEP 451
// https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#0.3
// https://github.com/raphw/byte-buddy/discussions/1535
val byteBuddyAgent = configurations.create("byteBuddyAgent")

dependencies {
  implementation(libs.aspectjrt)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.reflect)
  implementation(libs.jakarta.persistence.api)
  "dgsImplementation"(platform(libs.dgs.platform))
  "dgsImplementation"(libs.dgs.starter)

  // TODO: Discuss strong coupling with Hibernate
  // * e.g. Kotlin-JDSL dependency relies on Hibernate
  "jpaImplementation"(libs.spring.boot.starter.data.jpa)
  "jpaImplementation"(libs.spring.boot.starter.web)
  "jpaApi"(libs.jdsl)

  "blazeRuntimeOnly"(libs.blaze.core.impl.jakarta)
  "blazeApi"(libs.blaze.integration.hibernate)
  "blazeApi"(libs.blaze.entity.view.api.jakarta)
  "blazeImplementation"(libs.blaze.entity.view.impl.jakarta)
  "blazeApi"(libs.blaze.integration.spring)
  "blazeImplementation"(libs.spring.boot.starter.web)
  "blazeImplementation"(libs.bytebuddy)
  "blazeImplementation"(libs.bytebuddy.agent)
  "blazeProcessorImplementation"("com.beeproduced:bee.generative:$version")
  "blazeProcessorImplementation"(sourceSets["blaze"].output)

  testImplementation(sourceSets["jpa"].output)
  testImplementation(libs.spring.boot.starter.test) { exclude("org.mockito", "mockito-core") }
  testImplementation(libs.spring.boot.starter.data.jpa)
  testImplementation(libs.jdsl)
  testImplementation(libs.kotlin.test)
  implementation(libs.h2)

  @Suppress("UnstableApiUsage") byteBuddyAgent(libs.bytebuddy.agent) { isTransitive = false }
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "21"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
  jvmArgs("-javaagent:${byteBuddyAgent.asPath}")
}

/* // See: https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html
val springTestTask = tasks.register<Test>("jpaTest") {
    description = "Runs spring support tests."
    group = "verification"
    useJUnitPlatform()

    val springSupportTestSourceSet = sourceSets["springSupportTest"]
    testClassesDirs = springSupportTestSourceSet.output.classesDirs
    classpath = springSupportTestSourceSet.runtimeClasspath

    shouldRunAfter(tasks.test)
} */

allprojects {
  // https://github.com/cortinico/ktfmt-gradle
  // https://github.com/facebook/ktfmt
  ktfmt { googleStyle() }
}

noArg { annotation("javax.persistence.Entity") }

allOpen { annotation("javax.persistence.Entity") }

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
