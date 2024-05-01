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

import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import xyz.xfqlittlefan.fhraise.buildsrc.outputDirectoryOf
import xyz.xfqlittlefan.fhraise.buildsrc.projectBuildNumber
import xyz.xfqlittlefan.fhraise.buildsrc.projectVersion
import java.time.Year
import java.util.*

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
}

val androidCompileSdk: String by project
val androidMinSdk: String by project
val androidTargetSdk: String by project

kotlin {
    @OptIn(ExperimentalWasmDsl::class) wasmJs {
        moduleName = "fhraise"
        browser {
            commonWebpackConfig {
                outputFileName = "fhraise.js"
            }
            @OptIn(ExperimentalDistributionDsl::class) distribution {
                outputDirectory = outputDirectoryOf("web")
            }
        }
        binaries.executable()
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class) compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.resources)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.cbor)
                implementation(libs.bcrypt)
            }
        }

        val commonJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.androidx.datastore.core)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(compose.preview)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.resources)
                implementation(libs.ktor.server.contentNegotiation)
                implementation(libs.ktor.client.cio)
                implementation(libs.slf4j.api)
            }
        }

        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.core.splashscreen)
                implementation(libs.androidx.window)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.browser)
                implementation(libs.logback.android)
            }
        }

        val desktopMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
                implementation(libs.javaCv)
                implementation(libs.logback)
            }

            tasks.withType<Jar> {
                exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

android {
    namespace = "xyz.xfqlittlefan.fhraise"
    compileSdk = androidCompileSdk.toInt()

    defaultConfig {
        applicationId = "xyz.xfqlittlefan.fhraise"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = projectBuildNumber
        versionName = projectVersion
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        val store = file("key.jks")
        if (store.exists()) {
            val properties = Properties()
            file("key.properties").run {
                if (exists()) {
                    properties.load(inputStream())

                    create("release") {
                        storeFile = store
                        storePassword = properties.getProperty("storePassword")
                        keyAlias = properties.getProperty("keyAlias")
                        keyPassword = properties.getProperty("keyPassword")
                    }
                }
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "compose-android.pro"
            )

            try {
                signingConfig = signingConfigs.getByName("release")
            } catch (e: UnknownDomainObjectException) {
                logger.error("${e.message} Maybe a key.jks or key.properties file is missing?")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
}

compose.desktop {
    application {
        mainClass = "xyz.xfqlittlefan.fhraise.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage, TargetFormat.Msi)
            modules("java.instrument", "java.management", "jdk.unsupported")
            packageName = "Fhraise"
            packageVersion = projectVersion
            description = "Fhraise"
            copyright = "© 2024${
                Year.now().value.let {
                    if (it > 2024) "-$it" else ""
                }
            } HSAS Foodies. All Rights Reserved."
            vendor = "HSAS Foodies"
            licenseFile = rootProject.file("LICENSE")
            outputBaseDir = outputDirectoryOf("desktop")

            linux {
                debMaintainer = "xfqwdsj@qq.com"
                menuGroup = "Utility"
                rpmLicenseType = "GPL-3.0-or-later"
            }

            windows {
                packageVersion = projectVersion.split("+").first()
                dirChooser = true
                menuGroup = "HSAS Foodies"
                upgradeUuid = "e72b5bab-6eb1-41a6-b9c4-d755d92103ae"
            }
        }

        buildTypes {
            release {
                proguard {
                    version = "7.4.2"
                    configurationFiles.from("compose-desktop.pro")
                    optimize = false
                }
            }
        }
    }
}

compose.experimental {
    web.application {}
}
