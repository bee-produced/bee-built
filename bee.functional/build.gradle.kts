import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    java
    alias(libs.plugins.spring.boot).apply(false)
    alias(libs.plugins.spring.dependencymanagement)
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
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
    //         configurations["dgsTestImplementation"].extendsFrom(configurations.implementation.get())
    //         configurations["dgsTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
    //     }
    // }
    create("persistant") {
        java {
            srcDir("src/persistant/kotlin")
            compileClasspath += main.get().output
            runtimeClasspath += main.get().output
            configurations["persistantImplementation"].extendsFrom(configurations.implementation.get())
            configurations["persistantRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())
        }
    }
}

java {
    registerFeature("dgs") {
        usingSourceSet(sourceSets["dgs"])
    }
    registerFeature("persistant") {
        usingSourceSet(sourceSets["persistant"])
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
    "persistantImplementation"(libs.spring.boot.starter.data.jpa)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// See: https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html
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
