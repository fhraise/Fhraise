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

package xyz.xfqlittlefan.fhraise.py

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.logger
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi

class Client(private val host: String, private val port: UShort) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val messageChannel = Channel<Message>(3)

    private val resultChannel = Channel<Message>(3)

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
    fun connect(onError: OnError, onClose: OnClose) = runBlocking {
        logger.debug("Connecting to $host:$port.")
        suspendCancellableCoroutine { continuation ->
            logger.debug("Launching coroutine.")
            scope.launch(Dispatchers.IO) {
                runCatchingC(onError) {
                    logger.debug("Starting connection.")
                    client.webSocket(host = host, port = port.toInt(), path = pyWsPath) {
                        continuation.resume(true)
                        logger.debug("Connected.")
                        while (true) {
                            if (!isActive) {
                                logger.info("Connection closed.")
                                onClose()
                                break
                            }
                            logger.debug("Waiting for message...")
                            val receiveResult = runCatching {
                                val message = receiveDeserialized<Message>()
                                logger.debug("Received message.")
                                messageChannel.send(message)
                            }.onFailure { throwable ->
                                throwable.logger.debug("Caught throwable. Callback address: ${onError.rawValue}.")
                                throwable.cThrowable {
                                    onError(it)
                                }
                            }
                            if (receiveResult.isFailure) continue
                            logger.debug("Waiting for result...")
                            sendSerialized<Message>(resultChannel.receive())
                        }
                    }
                }.onFailure {
                    if (continuation.isActive) {
                        logger.error("Failed to connect.")
                        continuation.resume(false)
                    } else {
                        logger.error("Uncaught error in $this.")
                        throw it
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
    fun receive(onMessage: OnMessage, onError: OnError): Boolean {
        val message = runBlocking { messageChannel.receive() }

        logger.debug("Sending message to Python.")

        return runBlocking {
            val ref = StableRef.create(message)
            runCatchingC(onError) {
                memScoped {
                    val type = message::class.qualifiedName!!.cstr.ptr
                    val received = onMessage(type, ref.asCPointer()).asStableRef<Message>().get()
                    resultChannel.send(received)
                    logger.debug("Received result from Python: ${received::class.qualifiedName}.")
                }
            }.isSuccess.also { ref.dispose() }
        }
    }
}
