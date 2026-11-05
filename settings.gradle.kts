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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // Required for NewPipeExtractor (online music search/streaming) — see brain.md Section 17.
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "Viaura"

// Ensure bundled archives are extracted if missing on clean build environments
val csDir = file("app/src/cloudstream-lib")
val csZip = file("app/src/cloudstream-lib.zip")
if (!csDir.exists() && csZip.exists()) {
  java.util.zip.ZipFile(csZip).use { zip ->
    for (entry in zip.entries().asSequence()) {
      val target = file("app/src/${entry.name}")
      if (entry.isDirectory) {
        target.mkdirs()
      } else {
        target.parentFile?.mkdirs()
        zip.getInputStream(entry).use { input ->
          target.outputStream().use { output -> input.copyTo(output) }
        }
      }
    }
  }
}

val iconsCheck = file("app/src/main/res/drawable/viaura_video_setting.xml")
val iconsZip = file("app/src/viaura-icons.zip")
if (!iconsCheck.exists() && iconsZip.exists()) {
  java.util.zip.ZipFile(iconsZip).use { zip ->
    for (entry in zip.entries().asSequence()) {
      val target = file("app/src/main/res/${entry.name}")
      if (entry.isDirectory) {
        target.mkdirs()
      } else {
        target.parentFile?.mkdirs()
        zip.getInputStream(entry).use { input ->
          target.outputStream().use { output -> input.copyTo(output) }
        }
      }
    }
  }
}

// Packed source folders (many small files kept as one zip so the repo file list stays short). Entries are relative to app/src.
// The .github/workflows/build-apk.yml step "Unpack packed source folders" does the same in CI; this block is for local / AI Studio builds.
val packedSources = listOf(
  "app/src/lyrics-providers-src.zip" to "app/src/main/java/com/example/lyrics/providers",
  "app/src/widgets-src.zip" to "app/src/main/java/com/example/widgets",
  "app/src/unit-tests-src.zip" to "app/src/test/java"
)
for ((zipPath, checkDir) in packedSources) {
  val z = file(zipPath)
  if (!file(checkDir).exists() && z.exists()) {
    java.util.zip.ZipFile(z).use { zip ->
      for (entry in zip.entries().asSequence()) {
        val target = file("app/src/${entry.name}")
        if (entry.isDirectory) {
          target.mkdirs()
        } else {
          target.parentFile?.mkdirs()
          zip.getInputStream(entry).use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
      }
    }
  }
}

include(":app")
include(":cloudstream-lib")
// The CloudStream runtime module lives inside the app folder (app/src/cloudstream-lib) to keep the repo root clean.
project(":cloudstream-lib").projectDir = file("app/src/cloudstream-lib")
