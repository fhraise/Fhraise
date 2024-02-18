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

package xyz.xfqlittlefan.fhraise.buildsrc

import org.gradle.kotlin.dsl.provideDelegate
import java.io.File
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class BuildProperties(private val file: File) : Properties(), ReadWriteProperty<Any?, String?> {
    init {
        with(file) {
            if (exists()) {
                load(inputStream())
            }
        }
    }

    private fun save() = with(file) { store(outputStream(), null) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = getProperty(property.name, null)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        setProperty(property.name, value)
        save()
    }

    operator fun invoke(defaultValue: String) = invoke(defaultValue, { it }, { it })

    operator fun invoke(defaultValue: Int) = invoke(defaultValue, String::toInt, Int::toString)

    inline operator fun <reified V> invoke(
        defaultValue: V, crossinline transform: (String) -> V, crossinline reverse: (V) -> String
    ) = object : ReadWriteProperty<Any?, V> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            this@BuildProperties.getValue(thisRef, property)?.let(transform) ?: defaultValue

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) =
            this@BuildProperties.setValue(thisRef, property, reverse(value))
    }
}

val versionProperties by lazy { BuildProperties(RootProject.project.file("version.properties")) }
val buildNumberProperties by lazy { BuildProperties(RootProject.project.file("build-number.properties")) }

private var devVer by versionProperties("0.1")
var projectDevVer
    get() = devVer
    set(value) {
        devVer = value
    }

private var commitSha by versionProperties("0000000")
var projectCommitSha
    get() = commitSha
    set(value) {
        commitSha = value
    }

private var reversion by versionProperties(0)
var projectReversion
    get() = reversion
    set(value) {
        reversion = value
    }

private var version by versionProperties("0.1.0")
var projectVersion
    get() = version
    set(value) {
        version = value
    }

private var buildNumber by buildNumberProperties(1)
var projectBuildNumber
    get() = buildNumber
    set(value) {
        buildNumber = value
    }
