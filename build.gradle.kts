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

import org.gradle.internal.os.OperatingSystem
import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

val versionPropertiesFile = file("version.properties")
val versionProperties = Properties().apply {
    with(versionPropertiesFile) {
        if (exists()) {
            load(inputStream())
        }
    }
}

var devVer: String by versionProperties
var commitSha: String by versionProperties
var reversion: String by versionProperties
var version: String by versionProperties

val buildNumberPropertiesFile = file("build-number.properties")
val buildNumberProperties = Properties().apply {
    with(buildNumberPropertiesFile) {
        if (exists()) {
            load(inputStream())
        }
    }
}

var buildNumber: String by buildNumberProperties

val String.outputDirectory
    get() = rootProject.layout.buildDirectory.dir("outputs/binaries/$version.$buildNumber/$this")

val isLinux = OperatingSystem.current().isLinux
val isWindows = OperatingSystem.current().isWindows
val arch: String = System.getProperty("os.arch")

tasks.register("updateDevVer") {
    group = "versioning"
    description = "Update the dev version"

    doLast {
        val stream = ByteArrayOutputStream()
        val result = exec {
            commandLine("git", "describe", "--tags", "--abbrev=0", "--match=dev[0-9]*\\.[0-9]*")
            standardOutput = stream
            isIgnoreExitValue = true
        }
        devVer = if (result.exitValue == 0) stream.toString().trim().substringAfter("dev") else "0.0"
        versionProperties.store(versionPropertiesFile.outputStream(), null)
        logger.lifecycle("devVer: $devVer")
    }
}

tasks.register("updateCommitSha") {
    group = "versioning"
    description = "Update the commit sha of the main branch"

    doLast {
        val stream = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "main")
            standardOutput = stream
        }
        commitSha = stream.toString().trim()
        versionProperties.store(versionPropertiesFile.outputStream(), null)
        logger.lifecycle("commitSha: $commitSha")
    }
}

tasks.register("updateReversion") {
    group = "versioning"
    description = "Update the reversion"

    doLast {
        val stream = ByteArrayOutputStream()
        val result = exec {
            commandLine("git", "rev-list", "--count", "dev$devVer..main")
            standardOutput = stream
            isIgnoreExitValue = true
        }
        reversion = if (result.exitValue == 0) stream.toString().trim() else "0"
        versionProperties.store(versionPropertiesFile.outputStream(), null)
        logger.lifecycle("reversion: $reversion")
    }
}

tasks.register("updateVersion") {
    group = "versioning"
    description = "Update the version"

    dependsOn("updateDevVer", "updateCommitSha", "updateReversion")

    doLast {
        version = "$devVer.$reversion+$commitSha"
        versionProperties.store(versionPropertiesFile.outputStream(), null)
        logger.lifecycle("version: $version")
    }
}

tasks.register("increaseBuildNumber") {
    group = "versioning"
    description = "Increase the build number"

    doLast {
        buildNumber = try {
            (buildNumber.toInt() + 1).toString()
        } catch (e: Throwable) {
            "1"
        }
        buildNumberProperties.store(buildNumberPropertiesFile.outputStream(), null)
        logger.lifecycle("buildNumber: $buildNumber")
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

    dependsOn("composeApp:assembleRelease")

    doLast {
        val apkDir = file(project(":composeApp").layout.buildDirectory.dir("outputs/apk/release"))
        val outputDir = file("android".outputDirectory)
        apkDir.copyRecursively(outputDir, overwrite = true)
        apkDir.deleteRecursively()
        logger.lifecycle("output directory: ${outputDir.absolutePath}")
    }
}

tasks.register("releaseLinuxApp") {
    group = "project build"
    description = "Build the Linux release executable"

    if (!isLinux) {
        enabled = false
    }

    dependsOn("composeApp:packageReleaseAppImage", "composeApp:packageReleaseDeb", "composeApp:packageReleaseRpm")
}

tasks.register<Tar>("releaseTarLinuxApp") {
    group = "project build"
    description = "Build the Linux release tar"

    if (!isLinux) {
        enabled = false
    }

    dependsOn("releaseLinuxApp")

    archiveBaseName = "fhraise"
    archiveAppendix = "linux"
    archiveVersion = "$version.$buildNumber"
    archiveClassifier = arch
    archiveExtension = "tar"
    compression = Compression.GZIP
    destinationDirectory = file("linux-tar".outputDirectory)
    from(file("desktop/main-release/app/Fhraise".outputDirectory))
}

tasks.register("releaseWindowsApp") {
    group = "project build"
    description = "Build the Windows release executable"

    if (!isWindows) {
        enabled = false
    }

    dependsOn("composeApp:packageReleaseAppImage", "composeApp:packageReleaseMsi")
}

tasks.register<Zip>("releaseZipWindowsApp") {
    group = "project build"
    description = "Build the Windows release zip"

    if (!isWindows) {
        enabled = false
    }

    dependsOn("releaseWindowsApp")

    archiveBaseName = "fhraise"
    archiveAppendix = "windows"
    archiveVersion = "$version.$buildNumber"
    archiveClassifier = arch
    archiveExtension = "zip"
    destinationDirectory = file("windows-zip".outputDirectory)
    from(file("desktop/main-release/app/Fhraise".outputDirectory))
}

tasks.register("releaseArchiveDesktopApp") {
    group = "project build"
    description = "Build the desktop release archive"

    if (isLinux) {
        dependsOn("releaseTarLinuxApp")
    } else if (isWindows) {
        dependsOn("releaseZipWindowsApp")
    }
}

tasks.register("releaseWebApp") {
    group = "project build"
    description = "Build the Web release"

    dependsOn("composeApp:wasmJsBrowserProductionWebpack")

    doLast {
        logger.lifecycle("output directory: ${file("web".outputDirectory).absolutePath}")
    }
}

tasks.register<Tar>("releaseTarWebApp") {
    group = "project build"
    description = "Build the Web release tar"

    dependsOn("releaseWebApp")

    archiveBaseName = "fhraise"
    archiveAppendix = "web"
    archiveVersion = "$version.$buildNumber"
    archiveExtension = "tar"
    compression = Compression.GZIP
    destinationDirectory = file("web-tar".outputDirectory)
    from(file("web".outputDirectory))
}

tasks.register("ciVersioning") {
    group = "ci"
    description = "Update the version and output it"

    dependsOn("updateVersion")

    doLast {
        val outputDir = file(project.property("output").toString())
        outputDir.writeText("version=${version.substringBefore('+')}")
        logger.lifecycle("Wrote version to ${outputDir.absolutePath}")
    }
}

tasks.register("ciReleaseLinuxApp") {
    group = "ci"
    description = "Build on the linux platform"

    dependsOn("releaseAndroidApp", "releaseTarLinuxApp", "releaseTarWebApp")

    doLast {
        val assetsDir = file(layout.buildDirectory.dir("assets"))
        file("android".outputDirectory).listFiles()?.first { it.name.endsWith(".apk") }?.copyTo(
            assetsDir.resolve("fhraise-android-$version.$buildNumber.apk"), overwrite = true
        )
        file("desktop/main-release/deb".outputDirectory).listFiles()?.first()?.copyTo(
            assetsDir.resolve("fhraise-linux-$version.$buildNumber-$arch.deb"), overwrite = true
        )
        file("desktop/main-release/rpm".outputDirectory).listFiles()?.first()?.copyTo(
            assetsDir.resolve("fhraise-linux-$version.$buildNumber-$arch.rpm"), overwrite = true
        )
        file("linux-tar".outputDirectory).copyRecursively(assetsDir, overwrite = true)
        file("web-tar".outputDirectory).copyRecursively(assetsDir, overwrite = true)
    }
}

tasks.register("ciReleaseWindowsApp") {
    group = "ci"
    description = "Build on the windows platform"

    dependsOn("releaseZipWindowsApp")

    doLast {
        val assetsDir = file(layout.buildDirectory.dir("assets"))
        file("windows-zip".outputDirectory).copyRecursively(assetsDir, overwrite = true)
        file("desktop/main-release/msi".outputDirectory).listFiles()?.first()?.copyTo(
            assetsDir.resolve("fhraise-windows-$version.$buildNumber-$arch.msi"), overwrite = true
        )
    }
}

tasks.register("ciReleaseApp") {
    group = "ci"
    description = "Build the release app"

    if (isLinux) {
        dependsOn("ciReleaseLinuxApp")
    } else if (isWindows) {
        dependsOn("ciReleaseWindowsApp")
    }
}

tasks.register("release") {
    group = "project build"
    description = "Create a new release"

    dependsOn("versioning", "releaseAndroidApp", "releaseLinuxApp", "releaseWindowsApp", "releaseWebApp")
}

project(":composeApp").tasks.configureEach {
    if (name == "assembleDebug") {
        dependsOn(":versioning")
    }
}

tasks.register("cleanReleases") {
    group = "project build"
    description = "Clean the releases"

    doLast {
        file("android".outputDirectory).deleteRecursively()
        file("desktop".outputDirectory).deleteRecursively()
        file("linux-tar".outputDirectory).deleteRecursively()
        file("windows-zip".outputDirectory).deleteRecursively()
        file("web-tar".outputDirectory).deleteRecursively()
        file(layout.buildDirectory.dir("assets")).deleteRecursively()
    }
}

tasks.register("runDesktopApp") {
    group = "project build"
    description = "Run the desktop app"

    dependsOn("versioning", "composeApp:run")
}

tasks.register("runWebApp") {
    group = "project build"
    description = "Run the web app"

    dependsOn("versioning", "composeAppwasmJsBrowserDevelopmentRun")
}

tasks.register("installReleaseAndroidApp") {
    group = "project build"
    description = "Install the Android release APK"

    dependsOn("releaseAndroidApp")

    doLast {
        val apk = file("android".outputDirectory).listFiles()!!.first { it.name.endsWith(".apk") }!!
        val cmd = mutableListOf("adb")
        if (hasProperty("device")) {
            cmd.add("-s")
            cmd.add(property("device").toString())
        }
        cmd.addAll(listOf("install", "-r", apk.absolutePath))

        exec {
            commandLine(cmd)
        }
    }
}
