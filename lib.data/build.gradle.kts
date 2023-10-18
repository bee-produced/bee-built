import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    java
    // id("org.hibernate.orm") version "6.1.7.Final"
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
java.sourceCompatibility = JavaVersion.VERSION_17

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
    registerFeature("dgs") {
        usingSourceSet(sourceSets["dgs"])
    }
    registerFeature("jpa") {
        usingSourceSet(sourceSets["jpa"])
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

// hibernate {
//     enhancement {
//         enableLazyInitialization(true)
//         enableExtendedEnhancement(true)
//     }
// }
