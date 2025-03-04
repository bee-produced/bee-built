rootProject.name = "bee.buzz"

dependencyResolutionManagement {
  versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

includeBuild("../bee.functional")
