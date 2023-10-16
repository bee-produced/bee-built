rootProject.name = "bee.fetched"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

includeBuild("../bee.generative")