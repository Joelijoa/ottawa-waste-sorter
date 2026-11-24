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

rootProject.name = "OttawaWasteSorter"

// Moteur de règles partagé (Kotlin pur) : utilisé par l'application ET par le backend.
include(":rules")
include(":backend")

// L'application Android n'est pas incluse dans l'image Docker du backend
// (pas de SDK Android dans le conteneur) : SKIP_ANDROID=true l'exclut.
if (System.getenv("SKIP_ANDROID") != "true") {
    include(":app")
}
