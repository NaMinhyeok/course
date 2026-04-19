plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "course"

include(
    "core:core-api",
    "core:core-enum",
    "storage:db-core",
)
