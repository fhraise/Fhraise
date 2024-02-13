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

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.http.reverseProxy
import xyz.xfqlittlefan.fhraise.routes.Api

/**
 * 用于更精准地应用 ProGuard 规则
 */
class OAuthApplication(
    private val serverHost: String, private val serverPort: Int, private val callback: suspend (JwtTokenPair) -> Unit
) {
    private var appProxyClient = HttpClient {
        followRedirects = false
    }

    @OptIn(ExperimentalSerializationApi::class)
    val module: Application.() -> Unit = {
        install(Resources)
        install(ContentNegotiation) { cbor() }

        routing {
            post<Api.OAuth.Callback> {
                runCatching { call.receive<Api.OAuth.Callback.RequestBody>() }.getOrNull()?.let {
                    callback(it.tokenPair)
                    call.respond(HttpStatusCode.OK)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }

            reverseProxy(Regex(".*"), appProxyClient) {
                url {
                    host = serverHost
                    port = serverPort
                }
            }
        }
    }
}
