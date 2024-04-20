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

package xyz.xfqlittlefan.fhraise.oauth

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair

actual suspend inline fun startOAuthApplication(
    host: String, port: Int, crossinline callback: suspend (JwtTokenPair) -> Unit
) = AndroidStartOAuthApplicationImpl.start(host, port) { callback(it) }

object AndroidStartOAuthApplicationImpl {
    lateinit var start: suspend (host: String, port: Int, callback: suspend (JwtTokenPair) -> Unit) -> EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
}
