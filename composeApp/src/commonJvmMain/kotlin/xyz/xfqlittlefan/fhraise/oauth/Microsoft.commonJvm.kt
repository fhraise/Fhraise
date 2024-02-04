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
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.platform.openUrl
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.time.Duration.Companion.minutes

fun Function<*>.methodName(): String {
    val method = (this as? KFunction<*>)?.javaMethod ?: return "${javaClass.name}.invoke"

    val clazz = method.declaringClass
    val name = method.name
    return "${clazz.name}.$name"
}

@OptIn(ExperimentalSerializationApi::class)
actual suspend fun CoroutineScope.microsoftSignIn(host: String, port: Int): String? = coroutineScope {
    withContext(Dispatchers.IO) {
        var continuation: Continuation<String?>? = null
        var callbackPort: UShort? = null
        var requestId: String? = null

        val callbackServer = with(MicrosoftApplicationModule(host, port, { callbackPort!! }, { requestId!! })) {
            embeddedServer(CIO, port = 0, module = module).start()
        }

        callbackPort = callbackServer.engine.resolvedConnectors().first().port.toUShort()

        val client = HttpClient {
            install(ContentNegotiation) { cbor() }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
            }
        }

        val webSocketClient: suspend CoroutineScope.() -> Unit = {
            client.webSocket(host = host, port = port, path = Api.Auth.OAuth.Socket.PATH) {
                sendSerialized(Api.Auth.OAuth.Socket.ClientMessage(callbackPort))
                var error =
                    runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage>() }.getOrNull() != Api.Auth.OAuth.Socket.ServerMessage.Ready

                var readyMessage: Api.Auth.OAuth.Socket.ServerMessage.ReadyMessage? = null

                if (!error) {
                    readyMessage =
                        runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage.ReadyMessage>() }.getOrNull()
                    error = readyMessage == null
                }

                if (!error) {
                    openUrl(readyMessage!!.url)
                    error =
                        runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage>() }.getOrNull() != Api.Auth.OAuth.Socket.ServerMessage.Result
                }

                var result: Api.Auth.OAuth.Socket.ServerMessage.ResultMessage? = null

                if (!error) {
                    result =
                        runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage.ResultMessage>() }.getOrNull()
                    error = result == null
                }

                if (!error && result == Api.Auth.OAuth.Socket.ServerMessage.ResultMessage.Success) {
                    val userId =
                        runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage.ResultMessage.UserIdMessage>() }.getOrNull()?.id
                    runCatching { continuation?.resume(userId) }
                }

                close(CloseReason(CloseReason.Codes.NORMAL, "End of authentication"))
                runCatching { continuation?.resume(null) }
            }
        }

        suspendCoroutine {
            continuation = it
            launch(block = webSocketClient)
            launch {
                delay(5.minutes)
                client.close()
                runCatching { continuation?.resume(null) }
            }
        }
    }
}

/**
 * 用于更精准地应用 ProGuard 规则
 */
private class MicrosoftApplicationModule(
    private val host: String,
    private val port: Int,
    private val getCallbackPort: () -> UShort,
    private val getRequestId: () -> String
) {
    val module: Application.() -> Unit = {
        routing {
            get(Api.Auth.OAuth.Provider.Microsoft.callback) {
                call.respondRedirect {
                    this.host = this@MicrosoftApplicationModule.host
                    this.port = this@MicrosoftApplicationModule.port
                }
                engine.stop()
            }
        }
    }
}
