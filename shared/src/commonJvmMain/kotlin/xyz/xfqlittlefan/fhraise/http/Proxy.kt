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

package xyz.xfqlittlefan.fhraise.http

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*

/**
 * 创建一个反向代理。
 *
 * @param path
 */
@OptIn(InternalAPI::class)
@KtorDsl
fun Route.reverseProxy(path: Regex, client: HttpClient, block: HttpRequestBuilder.() -> Unit) {
    route(path) {
        handle {
            client.request {
                url(call.request.uri)
                method = call.request.httpMethod
                headers.appendAll(call.request.headers)
                headers.remove(HttpHeaders.TransferEncoding)
                headers.append(
                    HttpHeaders.Forwarded,
                    "for=${call.request.origin.remoteHost};host=${call.request.headers[HttpHeaders.Host] ?: ""};proto=${call.request.origin.scheme}"
                )
                call.receiveChannel().let {
                    body = object : OutgoingContent.ReadChannelContent() {
                        override fun readFrom() = it
                    }
                }
                block()
            }.let { response ->
                call.respond(object : OutgoingContent.WriteChannelContent() {
                    override val contentLength: Long? = response.contentLength()
                    override val contentType: ContentType? = response.contentType()
                    override val status: HttpStatusCode = response.status
                    override val headers: Headers = Headers.build {
                        appendAll(response.headers.safeExplicitness)
                    }

                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        response.content.copyAndClose(channel)
                    }
                })
            }
        }
    }
}
