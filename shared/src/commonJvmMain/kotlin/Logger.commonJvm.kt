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

import org.slf4j.LoggerFactory

actual class Logger actual constructor(actual val name: String) {
    private val logger = LoggerFactory.getLogger(name)

    actual fun trace(message: Any, throwable: Throwable?) = logger.trace(message.toString(), throwable)
    actual fun debug(message: Any, throwable: Throwable?) = logger.debug(message.toString(), throwable)
    actual fun info(message: Any, throwable: Throwable?) = logger.info(message.toString(), throwable)
    actual fun warn(message: Any, throwable: Throwable?) = logger.warn(message.toString(), throwable)
    actual fun error(message: Any, throwable: Throwable?) = logger.error(message.toString(), throwable)
}
