import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.jreleaser)
    `maven-publish`
    signing
    java
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
description = "Easier data handling for GraphQL + JPA"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    mavenCentral()
}

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
}

dependencies {
    implementation(libs.aspectjrt)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.jakarta.persistence.api)
    "dgsImplementation"(platform(libs.dgs.platform))
    "dgsImplementation"(libs.dgs.spring.starter)

    // TODO: Discuss strong coupling with Hibernate
    // * e.g. Kotlin-JDSL depdendency relies on Hibernate
    "jpaImplementation"(libs.spring.boot.starter.data.jpa)
    "jpaImplementation"(libs.spring.boot.starter.web)
    "jpaApi"(libs.jdsl)

    // runtimeOnly("org.postgresql:postgresql")

    testImplementation(sourceSets["jpa"].output)
    // testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.jdsl)
    testImplementation(libs.junit.api)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.engine)
    implementation(libs.h2)
    // implementation("org.postgresql:postgresql:42.5.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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


noArg {
    annotation("javax.persistence.Entity")
}

allOpen {
    annotation("javax.persistence.Entity")
}

// Based on https://www.tschuehly.de/posts/guide-kotlin-gradle-publish-to-maven-central/#51-generate-javadocs-and-sources-jars
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
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    project {
        copyright.set("bee produced")
    }
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