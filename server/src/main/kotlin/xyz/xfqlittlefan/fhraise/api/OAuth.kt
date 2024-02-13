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

package xyz.xfqlittlefan.fhraise.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.defaultServerPort
import xyz.xfqlittlefan.fhraise.html.respondAutoClosePage
import xyz.xfqlittlefan.fhraise.link.AppUri
import xyz.xfqlittlefan.fhraise.routes.Api

@OptIn(ExperimentalSerializationApi::class)
private val oAuthApiClient = HttpClient {
//    install(ContentNegotiation) { cbor() }
//    install(HttpCookies)
    followRedirects = false
}

fun Route.apiOAuth() {
    apiOAuthRequest()
    apiOAuthVerify()
    apiOAuthEndpoint()
}

private fun Route.apiOAuthRequest() = get<Api.OAuth.Request> { req ->
    oAuthApiClient.get {
        url {
            host = "localhost"
            port = defaultServerPort
            path(Api.OAuth.PATH)
            parameters.apply {
                append(Api.OAuth.Query.PROVIDER, req.provider.brokerName)
                append(Api.OAuth.Query.REQUEST_ID, call.callId!!)
                append(Api.OAuth.Query.CALLBACK_PORT, req.callbackPort.toString())
                append(Api.OAuth.Query.SEND_DEEP_LINK, req.sendDeepLink.toString())
            }
        }
    }.let { parseResponse(it, req.provider, req.callbackPort) }
}

private suspend fun RoutingContext.parseResponse(
    response: HttpResponse, provider: Api.OAuth.Provider, callbackPort: Int, setCookie: String? = null
) {
    response.headers[HttpHeaders.Location]?.let { location ->
        val redirection = URLBuilder(location)
        if (redirection.host.endsWith(provider.domain)) {
            setCookie?.let { call.response.headers.append(HttpHeaders.SetCookie, it) }
            response.headers[HttpHeaders.SetCookie]?.let { call.response.headers.append(HttpHeaders.SetCookie, it) }
            call.respondRedirect(redirection.apply {
                parameters["redirect_uri"] = url {
                    this@apply.parameters["redirect_uri"]?.let { takeFrom(it) }
                    port = callbackPort
                    call.response.headers.values(HttpHeaders.SetCookie).map { parseServerSetCookieHeader(it) }
                        .distinct().forEach {
                            val (name, value) = it
                            parameters["Cookie$name"] = value
                        }
                }
            }.buildString())
        } else parseResponse(
            oAuthApiClient.get(location) {
                setCookie?.let { headers.append(HttpHeaders.Cookie, it) }
                response.headers[HttpHeaders.SetCookie]?.let { headers.append(HttpHeaders.Cookie, it) }
            }, provider, callbackPort, response.headers[HttpHeaders.SetCookie]
        )
    } ?: call.respondBytes(response.bodyAsChannel().toByteArray(), response.contentType(), response.status)
}

private fun Route.apiOAuthEndpoint() = get(Api.OAuth.Endpoint.PATH) {
    val brokerName = call.parameters[Api.OAuth.Endpoint.Query.BROKER_NAME]

    if (brokerName == null) {
        call.respond(HttpStatusCode.BadRequest)
        return@get
    }

    oAuthApiClient.get {
        call.request.queryParameters.filter { k, _ -> k.startsWith("Cookie") }.forEach { k, v ->
            cookie(k.removePrefix("Cookie"), v.first())
        }

        url {
            host = "localhost"
            port = defaultServerPort
            path("/auth/realms/fhraise/broker/$brokerName/endpoint")
            parameters.appendAll(call.request.queryParameters)
        }
    }.let { call.respondBytes(it.bodyAsChannel().toByteArray(), it.contentType(), it.status) }
}

private fun Route.apiOAuthVerify() = requireAppAuth {
    get(Api.OAuth.PATH) {
        val principal = call.principal<OAuthAccessTokenResponse.OAuth2>()
        val requestId = call.queryParameters[Api.OAuth.Query.REQUEST_ID]
        val callbackPort = call.queryParameters[Api.OAuth.Query.CALLBACK_PORT]?.toIntOrNull()

        if (principal == null || requestId == null || callbackPort == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        if (call.queryParameters[Api.OAuth.Query.SEND_DEEP_LINK] == "true") {
            call.respondRedirect(
                AppUri.OAuthCallback(principal.accessToken, principal.refreshToken, requestId).uri
            )
        } else {
            oAuthApiClient.sendToken(
                call.request.origin.remoteHost, callbackPort, requestId, principal
            )
            call.respondAutoClosePage()
        }
    }
}

private suspend fun HttpClient.sendToken(
    host: String, port: Int, requestId: String, principal: OAuthAccessTokenResponse.OAuth2
) = post {
    url {
        this.host = host
        this.port = port
        path(Api.OAuth.Callback.PATH)
    }

    contentType(ContentType.Application.Cbor)

    setBody(Api.OAuth.Callback.RequestBody(requestId, JwtTokenPair(principal.accessToken, principal.refreshToken!!)))
}

private fun RoutingContext.ResponseBody(request: Api.OAuth.Request): Api.OAuth.Request.ResponseBody {
    val requestId = call.callId!!
    return Api.OAuth.Request.ResponseBody(requestId, url {
        host = call.request.host()
        port = call.request.port()
        path(Api.OAuth.PATH)
        parameters.apply {
            append(Api.OAuth.Query.PROVIDER, request.provider.brokerName)
            append(Api.OAuth.Query.REQUEST_ID, requestId)
            append(Api.OAuth.Query.CALLBACK_PORT, request.callbackPort.toString())
            append(Api.OAuth.Query.SEND_DEEP_LINK, request.sendDeepLink.toString())
        }
    })
}
