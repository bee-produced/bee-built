import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot).apply(false)
    alias(libs.plugins.spring.dependencymanagement)
    alias(libs.plugins.jreleaser)
    `maven-publish`
    signing
    java
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
description = "Simple event manager based on the mediator pattern."
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

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

sourceSets {
    create("simple") {
        java {
            srcDir("src/simple/kotlin")
            compileClasspath += main.get().output
            runtimeClasspath += main.get().output
            configurations["simpleImplementation"].extendsFrom(configurations.implementation.get())
            configurations["simpleRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
    registerFeature("simple") {
        usingSourceSet(sourceSets["simple"])
        withSourcesJar()
        withJavadocJar()
    }
}

dependencies {
    implementation("com.beeproduced:bee.functional:$version")
    implementation(libs.kotlin.stdlib)
    implementation(libs.spring.boot.starter.web)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
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

// Invalid pom is produced when using both the dependency management plugin and Gradle's bom support
// See: https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/257#issuecomment-895790557
tasks.withType<GenerateMavenPom>().all {
    doLast {
        val file = File("$buildDir/publications/maven/pom-default.xml")
        var text = file.readText()
        val regex = "(?s)(<dependencyManagement>.+?<dependencies>)(.+?)(</dependencies>.+?</dependencyManagement>)".toRegex()
        val matcher = regex.find(text)
        if (matcher != null) {
            text = regex.replaceFirst(text, "")
            val firstDeps = matcher.groups[2]!!.value
            text = regex.replaceFirst(text, "$1$2$firstDeps$3")
        }
        file.writeText(text)
    }
}