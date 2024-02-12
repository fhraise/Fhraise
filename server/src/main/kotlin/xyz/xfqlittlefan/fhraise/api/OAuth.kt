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
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.html.respondAutoClosePage
import xyz.xfqlittlefan.fhraise.link.AppUri
import xyz.xfqlittlefan.fhraise.routes.Api

@OptIn(ExperimentalSerializationApi::class)
private val oAuthApiClient = HttpClient {
    install(ContentNegotiation) { cbor() }
}

fun Route.apiOAuth() {
    apiOAuthRequest()
    apiOAuthVerify()
}

private fun Route.apiOAuthRequest() = get<Api.OAuth.Request> { req ->
    call.respond(ResponseBody(req))
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
