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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.logger
import kotlin.experimental.ExperimentalNativeApi

@ExperimentalForeignApi
typealias OnConnect = CPointer<CFunction<() -> Unit>>

@ExperimentalForeignApi
typealias OnMessage = CPointer<CFunction<(type: CPointer<ByteVar>, ref: COpaquePointer) -> CPointer<*>>>

@ExperimentalForeignApi
typealias OnClose = CPointer<CFunction<() -> Unit>>

@OptIn(ExperimentalForeignApi::class)
class Client(private val host: String, private val port: UShort) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val messageFlow = MutableSharedFlow<Message>(1)
    private val resultFlow = MutableSharedFlow<Message>(1)

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
    fun connect(onConnect: OnConnect, onError: OnError, onClose: OnClose) {
        runBlocking {
            logger.debug("Connecting to $host:$port.")
            runCatchingC(onError) {
                logger.debug("Starting connection.")
                client.webSocket(host = host, port = port.toInt(), path = pyWsPath) {
                    logger.debug("Connected.")

                    val jobList = mutableListOf<Job>()

                    val handler = CoroutineExceptionHandler { context, throwable ->
                        context.logger.trace("Caught exception: $throwable.")
                        throwable.cThrowable { onError(it) }
                    }

                    jobList += scope.launch(handler + Dispatchers.IO) {
                        logger.trace("Listening for close.")
                        val reason = closeReason.await()
                        logger.debug("Connection closed, reason: $reason.")
                        onClose()
                        jobList.forEach { it.cancel() }
                        scope.cancel()
                        this@runBlocking.cancel()
                    }

                    jobList += scope.launch(handler + Dispatchers.IO) {
                        onConnect()
                    }

                    jobList += scope.launch(handler + Dispatchers.IO) {
                        while (true) {
                            logger.debug("Waiting for message.")
                            messageFlow.emit(receiveDeserialized<Message>())
                            logger.trace("Received message.")
                        }
                    }

                    jobList += scope.launch(handler + Dispatchers.IO) {
                        while (true) {
                            resultFlow.collect {
                                logger.debug("Sending result.")
                                sendSerialized(it)
                            }
                        }
                    }

                    jobList.joinAll()
                }
                logger.trace("Code reached end of runCatchingC.")
            }.onFailure {
                logger.error("Connection error: $it.")
            }
            logger.trace("Code reached end of runBlocking.")
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
    fun receive(onMessage: OnMessage, onError: OnError) {
        runBlocking {
            messageFlow.collect { message ->
                logger.debug("Sending message to Python.")

                val ref = StableRef.create(message)

                runCatchingC(onError) {
                    memScoped {
                        val type = message::class.qualifiedName!!.cstr.ptr
                        val received = onMessage(type, ref.asCPointer()).asStableRef<Message>().get()
                        resultFlow.emit(received)
                        logger.debug("Received result from Python: ${received::class.qualifiedName}.")
                    }
                }

                ref.dispose()
            }
        }
    }
}
