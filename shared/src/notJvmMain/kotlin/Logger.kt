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

package xyz.xfqlittlefan.fhraise

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface MyLogger {
    fun Logger.getMessage(
        level: String, message: Any, throwable: Throwable?, stackTrace: Boolean = false
    ): List<String> {
        var result = message.toString()
        if (throwable != null) {
            result += "\n$throwable"
            if (stackTrace) {
                result += "\n" + throwable.stackTraceToString()
            }
        }

        return result.split("\n").map {
            "${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())} [$name] $level: $it"
        }
    }
}
