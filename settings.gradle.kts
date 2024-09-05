pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven {setUrl("https://mirrors.huaweicloud.com/repository/maven")}

        maven {setUrl("https://maven.scijava.org/content/repositories/public")}
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {setUrl("https://mirrors.huaweicloud.com/repository/maven")}
        maven {setUrl("https://maven.scijava.org/content/repositories/public")}
        google()
        mavenCentral()
    }
}

rootProject.name = "TestK2"
include(":app")
include(":mylibrary")
include(":mylibrary2")
