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

package xyz.xfqlittlefan.fhraise.py

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class Logger internal constructor(private val tag: Any) {
    @Deprecated("This constructor is for calling from C code only.", level = DeprecationLevel.HIDDEN)
    constructor(tag: String) : this("<From C> $tag" as Any)

    fun debug(message: String) {
        println("Debug", message)
    }

    fun info(message: String) {
        println("Info", message)
    }

    fun warn(message: String) {
        println("Warn", message)
    }

    fun error(message: String) {
        println("Error", message)
    }

    private fun println(level: String, message: String) {
        message.split("\n").forEach {
            println("${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())} [$tag] $level: $it")
        }
    }
}

internal val Any.logger: Logger get() = Logger(this::class.let { it.qualifiedName ?: it })
