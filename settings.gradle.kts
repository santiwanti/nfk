pluginManagement {
    includeBuild("convention-plugins")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nfk"
include(":library")
include(":sample:composeApp")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
