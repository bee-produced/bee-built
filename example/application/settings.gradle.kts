rootProject.name = "application"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("../../gradle/libs.versions.toml")) }
    }
}

includeBuild("../../lib.data")
includeBuild("../../lib.result")
includeBuild("../../lib.events")

// Services
include(":service.media")
project(":service.media").projectDir = File("../service.media")
include(":service.organisation")
project(":service.organisation").projectDir = File("../service.organisation")

// Events
include(":service.media.events")
project(":service.media.events").projectDir = File("../service.media/events")
include(":service.organisation.events")
project(":service.organisation.events").projectDir = File("../service.organisation/events")

// Entities
include(":service.media.entities")
project(":service.media.entities").projectDir = File("../service.media/entities")
include(":service.organisation.entities")
project(":service.organisation.entities").projectDir = File("../service.organisation/entities")

// Other
include(":utils")
project(":utils").projectDir = File("../utils")


