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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.routes.Api

/**
 * 用于更精准地应用 ProGuard 规则
 */
class OAuthApplication(
    private val provider: Api.OAuth.Provider, private val serverHost: String, private val serverPort: Int
) {
    private var appProxyClient = HttpClient {
        followRedirects = false
    }

    private lateinit var authSessionId: String

    @OptIn(ExperimentalSerializationApi::class)
    val module: Application.() -> Unit = {
        install(Resources)
        install(ContentNegotiation) { cbor() }

        routing {
            contentType(ContentType.Application.Cbor) {
                post<Api.OAuth.Message> {
                    authSessionId = runCatching { call.receive<Api.OAuth.Message.RequestBody>() }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }.authSessionId
                    call.respond(HttpStatusCode.OK)
                }
            }

            get("/auth/realms/fhraise/broker/${provider.brokerName}/endpoint") {
//                call.request.headers[HttpHeaders.SetCookie]?.let {
//                    call.response.headers.append(HttpHeaders.SetCookie, it)
//                }
//                call.request.headers[HttpHeaders.Cookie]?.let {
//                    call.response.headers.append(HttpHeaders.SetCookie, it)
//                }
                if (!::authSessionId.isInitialized) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                call.respondRedirect {
                    host = serverHost
                    port = serverPort
                    path(Api.OAuth.Endpoint.PATH)
                    parameters.append(Api.OAuth.AUTH_SESSION_ID, authSessionId)
                    parameters.append(Api.OAuth.Endpoint.Query.BROKER_NAME, provider.brokerName)
                    parameters.append(Api.OAuth.Query.CALLBACK_PORT, this@OAuthApplication.serverPort.toString())
//                    parameters.appendAll(call.request.queryParameters)
//                    parameters.append("cc", call.request.headers[HttpHeaders.Cookie] ?: "")
                }
            }
        }
    }
}
