import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependencymanagement)
  alias(libs.plugins.kotlin.jpa)
  // ksp plugin must be placed before kapt
  // https://github.com/google/ksp/issues/1445#issuecomment-1763422067
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.dgs.codegen)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.kotlin.noarg)
  alias(libs.plugins.ktfmt)
  java
  id("bee.generative")
}

allprojects {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "21"
    }
  }

  tasks.register<Task>(name = "resolveDependencies") {
    group = "Build Setup"
    description = "Resolve and prefetch dependencies"
    doLast {
      rootProject.allprojects.forEach {
        it.buildscript.configurations.filter(Configuration::isCanBeResolved).forEach {
          it.resolve()
        }
        it.configurations.filter(Configuration::isCanBeResolved).forEach { it.resolve() }
      }
    }
  }
}

group = "com.beeproduced"

version = libs.versions.bee.built.get()

java.sourceCompatibility = JavaVersion.VERSION_21

java.targetCompatibility = JavaVersion.VERSION_21

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

repositories { mavenCentral() }

dependencyManagement { imports { mavenBom(libs.dgs.platform.get().toString()) } }

dependencies {
  // service modules & more
  implementation(project(":service.media"))
  implementation(project(":service.media.events"))
  implementation(project(":service.organisation"))
  implementation(project(":service.organisation.events"))
  implementation(project(":utils"))
  // in-house libraries
  implementation("com.beeproduced:bee.buzz")
  implementation("com.beeproduced:bee.buzz") {
    capabilities { requireCapability("com.beeproduced:bee.buzz-simple") }
  }
  implementation("com.beeproduced:bee.functional") {
    capabilities { requireCapability("com.beeproduced:bee.functional-dgs") }
  }
  implementation("com.beeproduced:bee.persistent")
  implementation("com.beeproduced:bee.persistent") {
    capabilities { requireCapability("com.beeproduced:bee.persistent-dgs") }
  }
  beeGenerative("com.beeproduced:bee.fetched")
  // external dependencies
  implementation(libs.kotlin.stdlib)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.boot.starter.websocket)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.spring.boot.starter.cache)
  implementation(libs.spring.boot.starter.actuator)
  implementation(libs.spring.boot.starter.aop)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.oauth2.resource.server)
  implementation(libs.spring.boot.starter.oauth2.client)
  implementation(libs.spring.security.config)
  implementation(libs.spring.security.messaging)
  implementation(libs.spring.security.web)
  implementation(libs.jackson.module.kotlin)
  implementation(platform(libs.dgs.platform))
  implementation(libs.dgs.starter)
  implementation(libs.dgs.pagination)
  implementation(libs.dgs.extended.scalars)
  implementation(libs.multipart.spring.graphql)
  implementation(libs.konform)
  implementation(libs.mapstruct)
  implementation(libs.datafaker)
  implementation("org.apache.tika:tika-core:2.4.1")
  kapt(libs.mapstruct.processor)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.spring.boot.starter.data.jpa)
  testImplementation(libs.spring.security.test)
  testImplementation(libs.kotlin.test)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
  implementation(libs.h2)
  testImplementation(libs.springmockk)

  if (System.getProperty("os.arch") == "aarch64" && System.getProperty("os.name") == "Mac OS X") {
    runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.76.Final:osx-aarch_64")
  }

  implementation("net.bytebuddy:byte-buddy:1.14.9")
  implementation("net.bytebuddy:byte-buddy-agent:1.14.9")
}

tasks.withType<Test> { useJUnitPlatform() }

ktfmt { googleStyle() }

subprojects { afterEvaluate { ktfmt { googleStyle() } } }

tasks.register("ktfmtFormatAll") {
  group = "formatting"
  description = "Runs ktfmtFormat for all projects"

  // Add the root project's ktfmtFormat task as a dependency
  dependsOn(tasks.named("ktfmtFormat"))

  // Iterate over all subprojects and add their ktfmtFormat tasks as dependencies, excluding the
  // specified project
  subprojects.forEach { subproject -> dependsOn(subproject.tasks.named("ktfmtFormat")) }
}

// DGS Codegen
// See: https://stackoverflow.com/a/70954759/12347616
tasks.withType<GenerateJavaTask> {
  notCompatibleWithConfigurationCache("Remove later")
  packageName = "com.beeproduced.example.application.graphql"
  subPackageNameTypes = "dto"
  generateCustomAnnotations = true
  typeMapping =
    mutableMapOf(
      "DateTime" to "java.time.Instant",
      "Upload" to "org.springframework.web.multipart.MultipartFile",
    )
}

beeGenerative {
  arg("fetchedScanPackage", "com.beeproduced.example.application.graphql.dto")
  arg("fetchedPackageName", "com.beeproduced.example.application.graphql.fetcher")
}

kapt {
  // Fix error: incompatible types: NonExistentClass cannot be converted to Annotation
  // @error.NonExistentClass
  // https://stackoverflow.com/a/55646891/12347616
  correctErrorTypes = true
  arguments {
    // Set Mapstruct Configuration options here
    // https://kotlinlang.org/docs/reference/kapt.html#annotation-processor-arguments
    // https://mapstruct.org/documentation/stable/reference/html/#configuration-options
    arg("mapstruct.defaultComponentModel", "spring")
  }
}

tasks.getByName<Jar>("jar") { enabled = false }

tasks.bootRun { jvmArgs = listOf("-Dspring.output.ansi.enabled=ALWAYS") }
