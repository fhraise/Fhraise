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

actual class Logger actual constructor(actual val name: String) : MyLogger {
    actual fun trace(message: Any, throwable: Throwable?) {
        getMessage("TRACE", message, throwable, true).forEach { println(it) }
    }

    actual fun debug(message: Any, throwable: Throwable?) {
        getMessage("DEBUG", message, throwable, true).forEach { println(it) }
    }

    actual fun info(message: Any, throwable: Throwable?) {
        getMessage("INFO", message, throwable).forEach { println(it) }
    }

    actual fun warn(message: Any, throwable: Throwable?) {
        getMessage("WARN", message, throwable).forEach { println(it) }
    }

    actual fun error(message: Any, throwable: Throwable?) {
        getMessage("ERROR", message, throwable).forEach { println(it) }
    }
}
