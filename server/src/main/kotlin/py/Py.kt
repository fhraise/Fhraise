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

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import xyz.xfqlittlefan.fhraise.flow.IdMessageFlow
import java.util.*

val messageToPyFlow = IdMessageFlow<String, Message>(3, 3)
val messageFromPyFlow = IdMessageFlow<String, Message>(3, 3)

fun Route.py() {
    webSocket(pyWsPath) {
        messageToPyFlow.collect { (id, message) ->
            launch(start = CoroutineStart.UNDISPATCHED) {
                sendSerialized<Message>(message)
                messageFromPyFlow.emit(id to receiveDeserialized())
            }
        }
    }
}

suspend fun sendMessageToPy(
    message: Message, id: String = UUID.randomUUID().toString()
): Message {
    messageToPyFlow.emit(id to message)
    return messageFromPyFlow.take(id)
}
