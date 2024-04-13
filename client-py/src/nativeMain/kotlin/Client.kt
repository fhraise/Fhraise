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
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi

class Client(private val host: String, private val port: UShort) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val messageChannel = Channel<Message?>(3)

    @OptIn(ExperimentalForeignApi::class)
    private val messageErrorChannel = Channel<CPointer<ThrowableVar>>(3)
    private val resultChannel = Channel<Message>(3)

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
    fun connect(throwable: CPointer<CPointerVar<ThrowableVar>>?) = runBlocking {
        logger.debug("Connecting to $host:$port.")
        suspendCancellableCoroutine { continuation ->
            logger.debug("Launching coroutine.")
            scope.launch(Dispatchers.IO) {
                runCatching(throwable) {
                    logger.debug("Starting connection.")
                    client.webSocket(host = host, port = port.toInt(), path = pyWsPath) {
                        continuation.resume(true)
                        logger.debug("Connected.")
                        while (true) {
                            logger.debug("Waiting for message...")
                            val receiveResult =
                                runCatching(null) { messageChannel.send(receiveDeserialized<Message>()) }.onFailure {
                                    logger.error("Failed to receive message.")
                                    it as ThrowableWrapper
                                    messageErrorChannel.send(it.throwable)
                                    messageChannel.send(null)
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
    fun receive(
        type: CPointer<CPointerVar<ByteVar>>,
        ref: CPointer<COpaquePointerVar>,
        throwable: CPointer<CPointerVar<ThrowableVar>>?,
        getResult: CPointer<CFunction<() -> CPointer<*>>>
    ): Boolean {
        val message = runBlocking { messageChannel.receive() }

        if (message == null) {
            return runBlocking {
                throwable?.pointed?.value = messageErrorChannel.receive()
                false
            }
        }

        type.pointed.value = message::class.qualifiedName!!.cstrPtr
        ref.pointed.value = StableRef.create(message).asCPointer()

        return runBlocking {
            runCatching(throwable) {
                resultChannel.send(getResult().asStableRef<Message>().get())
            }.isSuccess
        }
    }
}