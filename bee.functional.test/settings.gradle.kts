rootProject.name = "bee.functional.test"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

includeBuild("../bee.generative")
includeBuild("../bee.functional")


