pluginManagement {
    repositories {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyBooksLibrary"
include(":app")

// Tự động cài git hooks khi bất kỳ lệnh Gradle nào được chạy.
// Chạy ngay sau clone — không cần ./gradlew preBuild thủ công.
gradle.settingsEvaluated {
    val hooksSource = File(rootDir, ".githooks")
    val hooksTarget = File(rootDir, ".git/hooks")
    if (hooksSource.isDirectory && hooksTarget.isDirectory) {
        hooksSource.listFiles()?.forEach { hook ->
            val target = File(hooksTarget, hook.name)
            if (!target.exists() || hook.readText() != target.readText()) {
                hook.copyTo(target, overwrite = true)
                target.setExecutable(true)
            }
        }
    }
}
 