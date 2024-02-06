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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.platform.bringWindowToFront
import xyz.xfqlittlefan.fhraise.platform.openUrl
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.minutes

internal expect val sendDeepLink: Boolean

@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
actual suspend fun CoroutineScope.microsoftSignIn(host: String, port: Int) = coroutineScope {
    withContext(Dispatchers.IO) {
        val channel = Channel<JwtTokenPair?>()

        val server = with(MicrosoftApplicationModule(host, port)) {
            embeddedServer(CIO, port = 0, module = module).start()
        }

        val client = HttpClient {
            install(ContentNegotiation) { cbor() }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
            }
        }

        launch(start = CoroutineStart.UNDISPATCHED) {
            client.webSocket(host = host, port = port, path = Api.OAuth.Socket.PATH) {
                sendSerialized(
                    Api.OAuth.Socket.ClientMessage(
                        Api.OAuth.Provider.Microsoft,
                        server.engine.resolvedConnectors().first().port.toUShort(),
                        sendDeepLink
                    )
                )
                var error =
                    runCatching { receiveDeserialized<Api.OAuth.Socket.ServerMessage>() }.getOrNull() != Api.OAuth.Socket.ServerMessage.Ready

                var readyMessage: Api.OAuth.Socket.ServerMessage.ReadyMessage? = null

                if (!error) {
                    readyMessage =
                        runCatching { receiveDeserialized<Api.OAuth.Socket.ServerMessage.ReadyMessage>() }.getOrNull()
                    error = readyMessage == null
                }

                if (!error) {
                    openUrl(readyMessage!!.url)
                    error =
                        runCatching { receiveDeserialized<Api.OAuth.Socket.ServerMessage>() }.getOrNull() != Api.OAuth.Socket.ServerMessage.Received
                    bringWindowToFront()
                }

                if (!error) {
                    error =
                        runCatching { receiveDeserialized<Api.OAuth.Socket.ServerMessage>() }.getOrNull() != Api.OAuth.Socket.ServerMessage.Result
                }

                var result: Api.OAuth.Socket.ServerMessage.ResultMessage? = null

                if (!error) {
                    result =
                        runCatching { receiveDeserialized<Api.OAuth.Socket.ServerMessage.ResultMessage>() }.getOrNull()
                    error = result == null
                }

                if (!error && result == Api.OAuth.Socket.ServerMessage.ResultMessage.Success) {
                    runCatching { channel.send(receiveDeserialized<Api.OAuth.Socket.ServerMessage.ResultMessage.TokenPairMessage>().tokenPair) }
                }

                close(CloseReason(CloseReason.Codes.NORMAL, "End of authentication"))
                runCatching { channel.send(null) }
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
private class MicrosoftApplicationModule(
    private val host: String,
    private val port: Int,
) {
    val module: Application.() -> Unit = {
        routing {
            get(Api.OAuth.Provider.Microsoft.callback) {
                call.respondRedirect {
                    this.host = this@MicrosoftApplicationModule.host
                    this.port = this@MicrosoftApplicationModule.port
                }
            }
        }
    }
}
