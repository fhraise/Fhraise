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

import xyz.xfqlittlefan.fhraise.buildsrc.projectVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-library`
}

group = "xyz.xfqlittlefan.fhraise"
project.version = projectVersion

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
