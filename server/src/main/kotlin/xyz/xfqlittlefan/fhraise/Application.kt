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

import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.auth.appJwt
import xyz.xfqlittlefan.fhraise.html.respondAutoClosePage
import xyz.xfqlittlefan.fhraise.link.AppUri
import xyz.xfqlittlefan.fhraise.models.Users
import xyz.xfqlittlefan.fhraise.models.cleanupVerificationCodes
import xyz.xfqlittlefan.fhraise.models.getOrCreateUserBy
import xyz.xfqlittlefan.fhraise.oauth.OAuthMessage
import xyz.xfqlittlefan.fhraise.oauth.getOAuthUserPrincipalFromMicrosoft
import xyz.xfqlittlefan.fhraise.oauth.oAuthFlow
import xyz.xfqlittlefan.fhraise.routes.Api
import xyz.xfqlittlefan.fhraise.routes.apiAuthEmailRequest
import xyz.xfqlittlefan.fhraise.routes.apiAuthEmailVerify
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(CIO, port = DefaultServerPort, host = "0.0.0.0", module = Application::module).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
fun Application.module() {
    val database = AppDatabase.current

    install(CallId) { generate() }

    install(Resources)

    install(RateLimit) {
        register(RateLimitName("codeVerification")) {
            rateLimiter(limit = 30, refillPeriod = 60.seconds)
        }
    }

    authentication {
        appJwt()

        oauth("microsoft") {
            urlProvider = {
                url {
                    val requestId = request.queryParameters[Api.Auth.OAuth.Socket.Query.REQUEST_ID] ?: ""
                    host = "localhost"
                    port = request.queryParameters[Api.Auth.OAuth.Socket.Query.CALLBACK_PORT]?.toIntOrNull()
                        ?: DEFAULT_PORT
                    path(Api.Auth.OAuth.Provider.Microsoft.callback)
                    parameters.clear()
                    parameters[Api.Auth.OAuth.Socket.Query.REQUEST_ID] = requestId
                    parameters[Api.Auth.OAuth.Socket.Query.CALLBACK_PORT] = port.toString()
                }
            }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "microsoft",
                    authorizeUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize",
                    accessTokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                    requestMethod = HttpMethod.Post,
                    clientId = MicrosoftOAuthClientId,
                    clientSecret = applicationSecret.propertyOrNull("auth.oauth.microsoft.client-secret")?.getString()
                        ?: "client-secret",
                    defaultScopes = listOf("https://graph.microsoft.com/.default"),
                )
            }
            client = appClient
        }
    }

    install(ContentNegotiation) { cbor() }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
    }

    cleanupVerificationCodes()

    routing {
        contentType(ContentType.Application.Cbor) {
            rateLimit(RateLimitName("codeVerification")) {
                apiAuthEmailRequest(database)
                apiAuthEmailVerify(database)
            }
        }

        webSocket(Api.Auth.OAuth.Socket.PATH) {
            val requestId = call.callId
            if (requestId == null) {
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "No call ID"))
                return@webSocket
            }

            val clientMessage = receiveDeserialized<Api.Auth.OAuth.Socket.ClientMessage>()

            // 此处使用 module 的 coroutineScope，确保 collector 至少在收到 Received 消息后可以发出 Response 消息
            // 而不会受到 WebSocket 连接意外关闭的影响
            // 这种用法是安全的，因为 WebSocket 连接关闭后，sendSerialized 会抛出异常，而导致协程取消
            val collect = this@module.launch(start = CoroutineStart.UNDISPATCHED) {
                oAuthFlow.collect(requestId) { message ->
                    when (message) {
                        is OAuthMessage.Received -> {
                            oAuthFlow.emit(requestId to OAuthMessage.Response {
                                if (clientMessage.sendDeepLink) {
                                    call.respondRedirect(AppUri.OAuthCallback())
                                } else {
                                    call.respondAutoClosePage()
                                }
                            })
                            sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.Received)
                        }

                        is OAuthMessage.Finished -> {
                            sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.Result)
                            if (message.id != null) {
                                sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.ResultMessage.Success)
                                sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.ResultMessage.UserIdMessage(message.id))
                            } else {
                                sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.ResultMessage.Failure)
                            }
                            close()
                        }

                        else -> Unit
                    }
                }
            }

            sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.Ready)
            sendSerialized(Api.Auth.OAuth.Socket.ServerMessage.ReadyMessage {
                host = call.request.origin.localAddress
                port = call.request.origin.localPort
                path(Api.Auth.OAuth.Provider.Microsoft.api)
                parameters[Api.Auth.OAuth.Socket.Query.REQUEST_ID] = requestId
                parameters[Api.Auth.OAuth.Socket.Query.CALLBACK_PORT] = clientMessage.port.toString()
            })

            delay(5.minutes)
            collect.cancel()
            close(CloseReason(CloseReason.Codes.NORMAL, "Timeout"))
        }

        authenticate("microsoft") {
            get(Api.Auth.OAuth.Provider.Microsoft.api) {}

            get(Api.Auth.OAuth.Provider.Microsoft.callback) {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                val requestId = call.queryParameters[Api.Auth.OAuth.Socket.Query.REQUEST_ID]
                if (principal != null && requestId != null) {
                    val response = async(start = CoroutineStart.UNDISPATCHED) {
                        select<suspend RoutingContext.(String?) -> Unit> {
                            async {
                                (oAuthFlow.take { it.first == requestId && it.second is OAuthMessage.Response }.second as OAuthMessage.Response).block
                            }.onAwait { it }
                            onTimeout(5.seconds) {
                                {
                                    call.respondAutoClosePage()
                                }
                            }
                        }
                    }
                    oAuthFlow.emit(requestId to OAuthMessage.Received)
                    val oAuthPrincipal = appClient.getOAuthUserPrincipalFromMicrosoft(principal.accessToken)
                    database.dbQuery {
                        database.getOrCreateUserBy(Users.microsoft, oAuthPrincipal.id).apply {
                            name ?: run { name = oAuthPrincipal.name }
                            email ?: run { email = oAuthPrincipal.email }
                        }
                    }
                    oAuthPrincipal.id.let {
                        oAuthFlow.emit(requestId to OAuthMessage.Finished(it))
                        response.await()(it)
                    }
                }
            }
        }
    }
}

val applicationConfig = ApplicationConfig("application.yaml")
val applicationSecret = ApplicationConfig("secret.yaml")
