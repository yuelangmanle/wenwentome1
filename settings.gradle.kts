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
        maven("https://raw.githubusercontent.com/psiegman/mvn-repo/master/releases") {
            content {
                includeGroup("nl.siegmann.epublib")
            }
        }
    }
}

rootProject.name = "wenwentome-reader"
include(
    ":app",
    ":core:model",
    ":core:database",
    ":data:apihub",
    ":data:localbooks",
    ":feature:apihub",
    ":feature:library",
    ":feature:reader",
    ":feature:discover",
    ":feature:settings",
    ":sync:github",
    ":bridge:source",
)
