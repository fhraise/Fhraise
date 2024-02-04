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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.platform.openUrl
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalSerializationApi::class)
actual suspend fun CoroutineScope.microsoftSignIn(host: String, port: Int): String? = coroutineScope {
    var continuation: Continuation<String?>? = null
    var callbackPort: String? = null
    var callId: String? = null

    val callbackServer = embeddedServer(Netty, port = 0) {
        routing {
            get(Api.Auth.OAuth.Provider.Microsoft.callback) {
                println(call.request.uri)
                call.respondRedirect(url {
                    this.host = host
                    this.port = port
                    this.encodedPath = call.request.path()
                    parameters.appendAll(call.request.queryParameters)
                    parameters.append("port", callbackPort!!)
                    parameters.append("id", callId!!)
                })
                engine.stop()
            }
        }
    }.start()

    callbackPort = callbackServer.engine.resolvedConnectors().first().port.toString()

    val client = HttpClient {
        install(ContentNegotiation) { cbor() }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        }
    }

    val webSocketClient: suspend CoroutineScope.() -> Unit = {
        client.webSocket(host = host, port = port, path = Api.Auth.OAuth.Socket.PATH) {
            sendSerialized(Api.Auth.OAuth.Socket.ClientMessage(callbackPort.toInt()))
            var error =
                runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage>() }.getOrNull() != Api.Auth.OAuth.Socket.ServerMessage.Ready

            var readyMessage: Api.Auth.OAuth.Socket.ServerMessage.ReadyMessage? = null

            if (!error) {
                readyMessage =
                    runCatching { receiveDeserialized<Api.Auth.OAuth.Socket.ServerMessage.ReadyMessage>() }.getOrNull()
                error = readyMessage == null
            }

            if (!error) {
                openUrl(URLBuilder(readyMessage!!.url).apply {
                    parameters.append("port", callbackPort)
                    parameters.append("id", readyMessage.callId)
                }.buildString())
                callId = readyMessage.callId
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
