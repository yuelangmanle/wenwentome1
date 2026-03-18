pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "wenwentome-reader"
include(
    ":app",
    ":core:model",
    ":core:database",
    ":data:localbooks",
    ":feature:library",
    ":feature:reader",
    ":feature:discover",
    ":feature:settings",
    ":sync:github",
    ":bridge:source",
)
