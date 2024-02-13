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
import io.ktor.client.plugins.resources.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.platform.openUrl
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.minutes
import io.ktor.client.plugins.resources.Resources as ClientResources
import io.ktor.server.resources.Resources as ServerResources

internal expect val sendDeepLink: Boolean

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
actual suspend fun CoroutineScope.microsoftSignIn(host: String, port: Int) = coroutineScope {
    withContext(Dispatchers.IO) {
        val channel = Channel<JwtTokenPair?>()

        val server = with(MicrosoftApplicationModule(host, port)) {
            embeddedServer(CIO, port = 0, module = module).start()
        }

        val client = HttpClient {
//            install(ContentNegotiation) { cbor() }
//            install(WebSockets) {
//                contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
//            }
            install(ClientResources)
        }

        client.href(
            Api.OAuth.Request(
                provider = Api.OAuth.Provider.Microsoft,
                callbackPort = server.engine.resolvedConnectors().first().port,
                sendDeepLink = sendDeepLink
            )
        ).let {
            openUrl {
                this.host = host
                this.port = port
                encodedPath = it
            }
        }

        val clean = {
            server.stop()
            client.close()
        }

        select {
            channel.onReceiveCatching {
                clean()
                it.getOrNull()
            }
            onTimeout(5.minutes) {
                clean()
                null
            }
        }
    }
}

/**
 * 用于更精准地应用 ProGuard 规则
 */
private class MicrosoftApplicationModule(private val host: String, private val port: Int) {
    private lateinit var authSessionId: String

    @OptIn(ExperimentalSerializationApi::class)
    val module: Application.() -> Unit = {
        install(ServerResources)
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

            get("/auth/realms/fhraise/broker/${Api.OAuth.Provider.Microsoft.brokerName}/endpoint") {
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
                    this.host = this@MicrosoftApplicationModule.host
                    this.port = this@MicrosoftApplicationModule.port
                    path(Api.OAuth.Endpoint.PATH)
                    parameters.append(Api.OAuth.AUTH_SESSION_ID, authSessionId)
                    parameters.append(Api.OAuth.Endpoint.Query.BROKER_NAME, Api.OAuth.Provider.Microsoft.brokerName)
                    parameters.append(Api.OAuth.Query.CALLBACK_PORT, this@MicrosoftApplicationModule.port.toString())
//                    parameters.appendAll(call.request.queryParameters)
//                    parameters.append("cc", call.request.headers[HttpHeaders.Cookie] ?: "")
                }
            }
        }
    }
}
