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

import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import xyz.xfqlittlefan.fhraise.buildsrc.outputDirectoryOf
import xyz.xfqlittlefan.fhraise.buildsrc.projectVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

group = "xyz.xfqlittlefan.fhraise"
project.version = projectVersion

kotlin {
    val defaultSharedLibConfigure: SharedLibrary.() -> Unit = {
        export(project(":py-common"))
    }

    linuxArm64 {
        binaries {
            sharedLib {
                baseName = "fhraisepy"
                outputDirectoryProperty.set(outputDirectoryOf("clientPy/linuxArm64"))
                defaultSharedLibConfigure()
            }
        }
    }

    linuxX64 {
        binaries {
            sharedLib {
                baseName = "fhraisepy"
                outputDirectoryProperty.set(outputDirectoryOf("clientPy/linuxX64"))
                defaultSharedLibConfigure()
            }
        }
    }

    mingwX64 {
        binaries {
            sharedLib {
                baseName = "libfhraisepy"
                outputDirectoryProperty.set(outputDirectoryOf("clientPy/mingwX64"))
                defaultSharedLibConfigure()
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(projects.shared)
                api(projects.pyCommon)
                api(projects.pyInternal)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.cbor)
            }
        }

        val nonMingwMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        val mingwMain by getting {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }

        val linuxMain by getting {
            dependsOn(nonMingwMain)
        }
    }
}
