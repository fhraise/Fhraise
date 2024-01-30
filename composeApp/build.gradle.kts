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

import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
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

val versionProperties = Properties().apply {
    with(rootProject.file("version.properties")) {
        if (exists()) {
            load(inputStream())
        }
    }
}
val version: String = versionProperties.getProperty("version", "0.1.0")

val buildNumberProperties = Properties().apply {
    with(rootProject.file("build-number.properties")) {
        if (exists()) {
            load(inputStream())
        }
    }
}
val buildNumber: String = buildNumberProperties.getProperty("buildNumber", "1")

val String.outputDirectory
    get() = rootProject.layout.buildDirectory.dir("outputs/binaries/$version.$buildNumber/$this")

kotlin {
    @OptIn(ExperimentalWasmDsl::class) wasmJs {
        moduleName = "fhraise"
        browser {
            commonWebpackConfig {
                outputFileName = "fhraise.js"
            }
            @OptIn(ExperimentalDistributionDsl::class) distribution {
                outputDirectory = "web".outputDirectory
            }
        }
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.resources)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.cbor)
                implementation(projects.shared)
            }
        }

        val commonJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.androidx.datastore.core)
                implementation(libs.androidx.datastore.preferences.core)
                implementation(compose.preview)
                implementation(libs.ktor.client.okhttp)
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
            }
        }

        val desktopMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
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

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "xyz.xfqlittlefan.fhraise"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = buildNumber.toInt()
        versionName = version
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage, TargetFormat.Msi)
            modules("jdk.unsupported")
            packageName = "Fhraise"
            packageVersion = version
            description = "Fhraise"
            copyright = "© 2024${
                Year.now().value.let {
                    if (it > 2024) "-$it" else ""
                }
            } HSAS Foodies. All Rights Reserved."
            vendor = "HSAS Foodies"
            licenseFile = rootProject.file("LICENSE")
            outputBaseDir = "desktop".outputDirectory

            linux {
                debMaintainer = "xfqwdsj@qq.com"
                menuGroup = "Utility"
                rpmLicenseType = "GPL-3.0-or-later"
            }

            windows {
                packageVersion = version.split("+").first()
                dirChooser = true
                menuGroup = "HSAS Foodies"
                upgradeUuid = "e72b5bab-6eb1-41a6-b9c4-d755d92103ae"
            }
        }

        buildTypes {
            release {
                proguard {
                    version = "7.4.1"
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

// TODO: Workaround for https://github.com/JetBrains/compose-multiplatform/issues/4085, remove when fixed
tasks.withType<LintModelWriterTask> {
    dependsOn("copyFontsToAndroidAssets")
}

tasks.withType<AndroidLintAnalysisTask> {
    dependsOn("copyFontsToAndroidAssets")
}
