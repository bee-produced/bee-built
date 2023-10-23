
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.kotlin.allopen)
}

group = "com.beeproduced"
version = libs.versions.bee.built.get()
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jakarta.persistence.api)
    api(libs.hibernate.core)
    api("com.beeproduced:bee.persistent")
    api("com.beeproduced:bee.persistent") {
        capabilities { requireCapability("com.beeproduced:bee.persistent-jpa") }
    }
}

noArg {
    annotations("jakarta.persistence.Entity", "jakarta.persistence.Embeddable")
}

allOpen {
    annotations("jakarta.persistence.Entity")
}
