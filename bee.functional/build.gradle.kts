import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.spring.boot).apply(false)
  alias(libs.plugins.spring.dependencymanagement)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.ktfmt)
  `maven-publish`
  signing
  java
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

description = "Functional kotlin bindings, integration with DGS, `bee.persistent` & more."

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "21" } }

repositories { mavenCentral() }

dependencyManagement {
  imports { mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) }
}

// See: https://docs.gradle.org/5.3.1/userguide/feature_variants.html#sec::declare_feature_variants
// And: https://stackoverflow.com/a/62529917/12347616
// Also: https://github.com/gradle/gradle/issues/25609
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
  // create("dgsTest"){
  //     java {
  //         srcDir("src/dgs/kotlin")
  //         compileClasspath += sourceSets.getByName("dgs").output + test.get().output
  //         runtimeClasspath += sourceSets.getByName("dgs").output + test.get().output
  //
  // configurations["dgsTestImplementation"].extendsFrom(configurations.implementation.get())
  //         configurations["dgsTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
  //     }
  // }
  create("persistent") {
    java {
      srcDir("src/persistent/kotlin")
      compileClasspath += main.get().output
      runtimeClasspath += main.get().output
      configurations["persistentImplementation"].extendsFrom(configurations.implementation.get())
      configurations["persistentRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
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
  registerFeature("persistent") {
    usingSourceSet(sourceSets["persistent"])
    withSourcesJar()
    withJavadocJar()
  }
}

dependencies {
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.reflect)
  api(libs.michael.result)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.engine)
  "dgsImplementation"(platform(libs.dgs.platform))
  "dgsImplementation"(libs.dgs.spring.starter)
  "dgsImplementation"(libs.spring.boot.starter.aop)
  "dgsImplementation"(libs.bytebuddy)
  "dgsImplementation"(libs.bytebuddy.agent)
  "persistentImplementation"(libs.spring.boot.starter.data.jpa)
}

tasks.withType<Test> { useJUnitPlatform() }

allprojects {
  // https://github.com/cortinico/ktfmt-gradle
  // https://github.com/facebook/ktfmt
  ktfmt { googleStyle() }
}

// See:
// https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html
// val dgsTestTask = tasks.register<Test>("dgsTest") {
//     description = "Runs dgs support tests."
//     group = "verification"
//     useJUnitPlatform()
//
//     val dgsTestSourceSet = sourceSets["dgsTest"]
//     testClassesDirs = dgsTestSourceSet.output.classesDirs
//     classpath = dgsTestSourceSet.runtimeClasspath
//
//     shouldRunAfter(tasks.test)
// }
//
// tasks.check { dependsOn(dgsTestTask) }

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

// Invalid pom is produced when using both the dependency management plugin and Gradle's bom support
// See:
// https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/257#issuecomment-895790557
tasks.withType<GenerateMavenPom>().all {
  doLast {
    val file =
      file("${layout.buildDirectory.get().asFile.toURI()}/publications/Maven/pom-default.xml")
    var text = file.readText()
    val regex =
      "(?s)(<dependencyManagement>.+?<dependencies>)(.+?)(</dependencies>.+?</dependencyManagement>)"
        .toRegex()
    val matcher = regex.find(text)
    if (matcher != null) {
      text = regex.replaceFirst(text, "")
      val firstDeps = matcher.groups[2]!!.value
      text = regex.replaceFirst(text, "$1$2$firstDeps$3")
    }
    file.writeText(text)
  }
}
