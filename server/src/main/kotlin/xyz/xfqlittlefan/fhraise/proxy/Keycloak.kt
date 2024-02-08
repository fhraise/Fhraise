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

package xyz.xfqlittlefan.fhraise.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import xyz.xfqlittlefan.fhraise.appConfig
import xyz.xfqlittlefan.fhraise.safeExplicitness

private val keycloakProxyClient = HttpClient {
    followRedirects = false
}

val keycloakHost = appConfig.propertyOrNull("keycloak.host")?.getString() ?: "localhost"
val keycloakPort = appConfig.propertyOrNull("keycloak.port")?.getString()?.toInt() ?: 8080

@OptIn(InternalAPI::class)
fun Route.proxyKeycloak() {
    route(Regex("/auth/((js|realms|resources)/.*|robots.txt|favicon.ico)")) {
        handle {
            keycloakProxyClient.request {
                url("http://$keycloakHost:$keycloakPort${call.request.uri}")
                method = call.request.httpMethod
                headers.appendAll(call.request.headers)
                headers.remove(HttpHeaders.TransferEncoding)
                headers[HttpHeaders.Host] = "$keycloakHost:$keycloakPort"
                headers[HttpHeaders.Forwarded] =
                    "for=${call.request.local.remoteHost};host=${call.request.headers[HttpHeaders.Host] ?: ""};proto=${call.request.local.scheme}"
                application.log.trace("Headers: {}", headers.entries())
                body = call.receive()
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
