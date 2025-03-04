rootProject.name = "bee.fetched.test"

dependencyResolutionManagement {
  versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

includeBuild("../bee.generative")

includeBuild("../bee.fetched")

includeBuild("../bee.persistent")

includeBuild("../bee.functional")

includeBuild("../bee.buzz")
