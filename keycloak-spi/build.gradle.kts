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

import java.util.*

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-library`
}

val versionProperties = Properties().apply {
    with(rootProject.file("version.properties")) {
        if (exists()) {
            load(inputStream())
        }
    }
}
val version: String = versionProperties.getProperty("version", "0.1.0")

group = "xyz.xfqlittlefan.fhraise"
project.version = version

dependencies {
    implementation(libs.keycloak.core)
    implementation(libs.keycloak.server.spi)
    implementation(libs.keycloak.server.spi.private)
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().filter { it.name.contains("kotlin") }
             .map { if (it.isDirectory) it else zipTree(it) })
}
