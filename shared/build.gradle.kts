/*
 * This file is part of Fhraise.
 * Copyright (c) 2024-2025 HSAS Foodies. All Rights Reserved.
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

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class) compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class) wasmJs {
        browser()
    }

    linuxArm64()
    linuxX64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.slf4j.api)
            }
        }

        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.logback.android)
            }
        }

        val jvmMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.logback)
            }
        }

        val notJvmMain by creating {
            dependsOn(commonMain)
        }

        val wasmJsMain by getting {
            dependsOn(notJvmMain)
        }

        val nativeMain by getting {
            dependsOn(notJvmMain)
        }
    }
}

val androidCompileSdk: String by project
val androidMinSdk: String by project

android {
    namespace = "xyz.xfqlittlefan.fhraise.shared"
    compileSdk = androidCompileSdk.toInt()

    defaultConfig {
        minSdk = androidMinSdk.toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
        }
    }
}
