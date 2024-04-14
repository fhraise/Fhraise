/*
 * This file is part of Fhraise.
 * Copyright (c) 2024 HSAS Foodies. All Rights Reserved.
 *
 * Fhraise is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Fhraise is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Fhraise. If not, see <https://www.gnu.org/licenses/>.
 */

import xyz.xfqlittlefan.fhraise.buildsrc.*
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
}

buildscript {
    dependencies {
        classpath(libs.kotlinx.atomicfu)
    }
}

allprojects {
    apply(plugin = "kotlinx-atomicfu")
}

RootProject.project = rootProject

tasks.register("updateDevVer") {
    group = "versioning"
    description = "Update the dev version"

    doLast {
        val stream = ByteArrayOutputStream()
        val result = exec {
            commandLine = listOf("git", "describe", "--tags", "--abbrev=0", "--match=dev[0-9]*\\.[0-9]*")
            standardOutput = stream
            isIgnoreExitValue = true
        }
        projectDevVer = if (result.exitValue == 0) stream.toString().trim().substringAfter("dev") else "0.0"
        logger.lifecycle("devVer: $projectDevVer")
    }
}

tasks.register("updateCommitSha") {
    group = "versioning"
    description = "Update the commit sha of the main branch"

    doLast {
        val stream = ByteArrayOutputStream()
        exec {
            commandLine = listOf("git", "rev-parse", "--short", "HEAD")
            standardOutput = stream
        }
        projectCommitSha = stream.toString().trim()
        logger.lifecycle("commitSha: $projectCommitSha")
    }
}

tasks.register("updateReversion") {
    group = "versioning"
    description = "Update the reversion"

    doLast {
        val stream = ByteArrayOutputStream()
        val result = exec {
            commandLine = listOf("git", "rev-list", "--count", "dev$projectDevVer..main")
            standardOutput = stream
            isIgnoreExitValue = true
        }
        projectReversion = if (result.exitValue == 0) stream.toString().trim().toInt() else 0
        logger.lifecycle("reversion: $projectReversion")
    }
}

tasks.register("updateVersion") {
    group = "versioning"
    description = "Update the version"

    dependsOn("updateDevVer", "updateCommitSha", "updateReversion")

    doLast {
        projectVersion = "$projectDevVer.$projectReversion+$projectCommitSha"
        logger.lifecycle("version: $projectVersion")
    }
}

tasks.register("increaseBuildNumber") {
    group = "versioning"
    description = "Increase the build number"

    doLast {
        projectBuildNumber += 1
        logger.lifecycle("buildNumber: $projectBuildNumber")
    }
}

tasks.register("versioning") {
    group = "versioning"
    description = "Update the version and increase the build number"

    dependsOn("updateVersion", "increaseBuildNumber")
}

tasks.register("releaseAndroidApp") {
    group = "project build"
    description = "Build the Android release APK"

    dependsOn("compose-app:assembleRelease")

    doLast {
        val apkDir = file(project("compose-app").layout.buildDirectory.dir("outputs/apk/release"))
        val outputDir = file(outputDirectoryOf("android"))
        apkDir.copyRecursively(outputDir, overwrite = true)
        logger.lifecycle("output directory: ${outputDir.absolutePath}")
    }
}

tasks.register("releaseDesktopAppUberJar") {
    group = "project build"
    description = "Build the desktop release uber jar"

    dependsOn("compose-app:packageReleaseUberJarForCurrentOS")

    doLast {
        val outputDir = file(outputDirectoryOf("desktop-uber-jar"))
        file(project("compose-app").layout.buildDirectory.dir("compose/jars")).listFiles()
            ?.filter { it.name.contains("$projectDevVer.$projectReversion") && it.name.endsWith(".jar") }?.forEach {
                val arch = Regex(".+?-.+?-(.+?)-.+").find(it.name)?.groupValues?.get(1) ?: return@forEach
                it.copyTo(
                    outputDir.resolve("fhraise-${operatingSystem.familyName}-$projectVersion.$projectBuildNumber-${arch}.jar"),
                    overwrite = true
                )
                logger.lifecycle("output directory: ${outputDir.absolutePath}")
            }
    }
}

tasks.register<Tar>("releaseLinuxAppAndTar") {
    group = "project build"
    description = "Build the Linux release and create a tar archive"

    if (!operatingSystem.isLinux) {
        enabled = false
    }

    dependsOn("compose-app:packageReleaseAppImage")

    archiveBaseName = "fhraise"
    archiveAppendix = "linux"
    archiveVersion = "$projectVersion.$projectBuildNumber"
    archiveClassifier = SystemEnvironment.arch
    archiveExtension = "tar"
    compression = Compression.GZIP
    destinationDirectory = file(outputDirectoryOf("linux-tar"))
    from(file(outputDirectoryOf("desktop/main-release/app/Fhraise")))
}

tasks.register<Zip>("releaseWindowsAppAndZip") {
    group = "project build"
    description = "Build the Windows release and create a zip archive"

    if (!operatingSystem.isWindows) {
        enabled = false
    }

    dependsOn("compose-app:packageReleaseAppImage")

    archiveBaseName = "fhraise"
    archiveAppendix = "windows"
    archiveVersion = "$projectVersion.$projectBuildNumber"
    archiveClassifier = SystemEnvironment.arch
    archiveExtension = "zip"
    destinationDirectory = file(outputDirectoryOf("windows-zip"))
    from(file(outputDirectoryOf("desktop/main-release/app/Fhraise")))
}

tasks.register("releaseDesktopAppAndArchive") {
    group = "project build"
    description = "Build the desktop release and create an archive"

    if (operatingSystem.isLinux) {
        dependsOn("releaseLinuxAppAndTar")
    } else if (operatingSystem.isWindows) {
        dependsOn("releaseWindowsAppAndZip")
    }
}

tasks.register("releaseDesktopApp") {
    group = "project build"
    description = "Build the desktop release"

    dependsOn("releaseDesktopAppAndArchive", "releaseDesktopAppUberJar")

    if (operatingSystem.isLinux) {
        dependsOn("compose-app:packageReleaseDeb", "compose-app:packageReleaseRpm")
    } else if (operatingSystem.isWindows) {
        dependsOn("compose-app:packageReleaseMsi")
    }
}

tasks.register("releaseWebApp") {
    group = "project build"
    description = "Build the Web release"

    dependsOn("compose-app:wasmJsBrowserDistribution")

    doLast {
        logger.lifecycle("output directory: ${file(outputDirectoryOf("web")).absolutePath}")
    }
}

tasks.register<Tar>("releaseWebAppAndTar") {
    group = "project build"
    description = "Build the Web release and create a tar archive"

    dependsOn("releaseWebApp")

    archiveBaseName = "fhraise"
    archiveAppendix = "web"
    archiveVersion = "$projectVersion.$projectBuildNumber"
    archiveExtension = "tar"
    compression = Compression.GZIP
    destinationDirectory = file(outputDirectoryOf("web-tar"))
    from(file(outputDirectoryOf("web")))
}

tasks.register("releaseServer") {
    group = "project build"
    description = "Build the server release"

    dependsOn("server:shadowJar")

    doLast {
        val jar = file(project("server").layout.buildDirectory.file("libs/server-all.jar"))
        val outputDir = file(outputDirectoryOf("server"))
        jar.copyTo(outputDir.resolve("fhraise-server-$projectVersion.$projectBuildNumber.jar"), overwrite = true)
        logger.lifecycle("output directory: ${outputDir.absolutePath}")
    }
}

tasks.register("releaseClientPySharedLib") {
    group = "project build"
    description = "Build the client Py shared library"

    if (SystemEnvironment.isLinux) {
        dependsOn("client-py:linkReleaseSharedLinuxArm64", "client-py:linkReleaseSharedLinuxX64")
    } else if (SystemEnvironment.isWindows) {
        dependsOn("client-py:linkReleaseSharedMingwX64")
    }

    doLast {
        logger.lifecycle("output directory: ${file(outputDirectoryOf("clientPy")).absolutePath}")
    }
}

tasks.register("ciVersioning") {
    group = "ci"
    description = "Update the version and output it"

    dependsOn("updateVersion")

    doLast {
        val outputVersion = "v${projectVersion.substringBefore('+')}"
        file(System.getenv("GITHUB_OUTPUT")).writeText("version=$outputVersion")
        logger.lifecycle("Wrote version to GitHub output: $outputVersion")
    }
}

tasks.register("ciReleaseLinuxApp") {
    group = "ci"
    description = "Build on the Linux platform"

    if (!operatingSystem.isLinux) {
        enabled = false
    }

    dependsOn("releaseAndroidApp", "releaseDesktopApp", "releaseWebAppAndTar", "releaseServer")

    doLast {
        val assetsDir = file(layout.buildDirectory.dir("assets"))
        file(outputDirectoryOf("android")).listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { file ->
            file.name.substringAfter("compose-app-").substringBefore("-release").let {
                file.copyTo(
                    assetsDir.resolve("fhraise-android-$projectVersion.$projectBuildNumber-$it.apk"), overwrite = true
                )
            }
        }
        file(outputDirectoryOf("desktop/main-release/deb")).listFiles()?.first()?.copyTo(
            assetsDir.resolve("fhraise-linux-$projectVersion.$projectBuildNumber-${SystemEnvironment.arch}.deb"),
            overwrite = true
        )
        file(outputDirectoryOf("desktop/main-release/rpm")).listFiles()?.first()?.copyTo(
            assetsDir.resolve("fhraise-linux-$projectVersion.$projectBuildNumber-${SystemEnvironment.arch}.rpm"),
            overwrite = true
        )
        file(outputDirectoryOf("linux-tar")).copyRecursively(assetsDir, overwrite = true)
        file(outputDirectoryOf("web-tar")).copyRecursively(assetsDir, overwrite = true)
        file(outputDirectoryOf("server")).copyRecursively(assetsDir, overwrite = true)
    }
}

tasks.register("ciReleaseWindowsApp") {
    group = "ci"
    description = "Build on the Windows platform"

    if (!operatingSystem.isWindows) {
        enabled = false
    }

    dependsOn("releaseDesktopApp")

    doLast {
        val assetsDir = file(layout.buildDirectory.dir("assets"))
        file(outputDirectoryOf("windows-zip")).copyRecursively(assetsDir, overwrite = true)
        file(outputDirectoryOf("desktop/main-release/msi")).listFiles()?.first()?.copyTo(
            assetsDir.resolve("fhraise-windows-$projectVersion.$projectBuildNumber-${SystemEnvironment.arch}.msi"),
            overwrite = true
        )
    }
}

tasks.register<Tar>("ciReleaseLinuxClientPySharedLib") {
    group = "ci"
    description = "Build the client Py shared library"

    dependsOn("releaseClientPySharedLib")

    archiveBaseName = "libfhraisepy"
    archiveAppendix = "linux"
    archiveVersion = "$projectVersion.$projectBuildNumber"
    archiveClassifier = "multi-arch"
    archiveExtension = "tar"
    compression = Compression.GZIP
    destinationDirectory = file(layout.buildDirectory.dir("assets"))
    from(file(outputDirectoryOf("clientPy")))
}

tasks.register<Zip>("ciReleaseWindowsClientPySharedLib") {
    group = "ci"
    description = "Build the client Py shared library"

    dependsOn("releaseClientPySharedLib")

    archiveBaseName = "libfhraisepy"
    archiveAppendix = "windows"
    archiveVersion = "$projectVersion.$projectBuildNumber"
    archiveClassifier = "multi-arch"
    archiveExtension = "zip"
    destinationDirectory = file(layout.buildDirectory.dir("assets"))
    from(file(outputDirectoryOf("clientPy")))
}

tasks.register("ciReleaseApp") {
    group = "ci"
    description = "Build the release app"

    if (operatingSystem.isLinux) {
        dependsOn("ciReleaseLinuxApp")
        dependsOn("ciReleaseLinuxClientPySharedLib")
    } else if (operatingSystem.isWindows) {
        dependsOn("ciReleaseWindowsApp")
        dependsOn("ciReleaseWindowsClientPySharedLib")
    }

    doLast {
        val assetsDir = file(layout.buildDirectory.dir("assets"))
        file(outputDirectoryOf("desktop-uber-jar")).copyRecursively(assetsDir, overwrite = true)
    }
}

tasks.register("release") {
    group = "project build"
    description = "Create a new release"

    dependsOn("versioning", "releaseAndroidApp", "releaseDesktopApp", "releaseWebApp", "releaseServer")
}

tasks.register("cleanReleases") {
    group = "project build"
    description = "Clean the releases"

    doLast {
        file(layout.buildDirectory.dir("outputs/binaries")).deleteRecursively()
        file(layout.buildDirectory.dir("assets")).deleteRecursively()
    }
}

tasks.configureEach {
    if (name == "clean") {
        dependsOn("cleanReleases")
    }
}

tasks.register("runDesktopApp") {
    group = "project build"
    description = "Run the desktop app"

    dependsOn("compose-app:run")
}

tasks.register("runWebApp") {
    group = "project build"
    description = "Run the web app"

    dependsOn("compose-app:wasmJsBrowserDevelopmentRun")
}

tasks.register("runServer") {
    group = "project build"
    description = "Run the server"

    dependsOn("server:run")
}

tasks.register("installReleaseAndroidApp") {
    group = "project build"
    description = "Install the Android release APK"

    dependsOn("releaseAndroidApp")

    doLast {
        val apk = file(outputDirectoryOf("android")).listFiles()!!.first { it.name.endsWith(".apk") }!!
        val cmd = mutableListOf("adb")
        if (hasProperty("device")) {
            cmd.add("-s")
            cmd.add(property("device").toString())
        }
        cmd.addAll(listOf("install", "-r", apk.absolutePath))

        exec {
            commandLine = cmd
        }
    }
}
