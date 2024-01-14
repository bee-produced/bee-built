rootProject.name = "bee.persistent.test"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

includeBuild("../bee.generative")
includeBuild("../bee.fetched")
includeBuild("../bee.persistent")
includeBuild("../bee.functional")
includeBuild("../bee.buzz")

// Datasources
include(":datasource.a")
project(":datasource.a").projectDir = File("./datasource.a")
include(":datasource.b")
project(":datasource.b").projectDir = File("./datasource.b")
include(":datasource.test")
project(":datasource.test").projectDir = File("./datasource.test")


