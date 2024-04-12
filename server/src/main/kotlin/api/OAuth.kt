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
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.html.respondAutoClosePage
import xyz.xfqlittlefan.fhraise.http.port
import xyz.xfqlittlefan.fhraise.routes.Api

@OptIn(ExperimentalSerializationApi::class)
private val oAuthApiClient = HttpClient {
    install(Resources)
    install(ContentNegotiation) { cbor() }
    followRedirects = false
}

fun Route.apiOAuth() = requireAppAuth {
    get(Api.OAuth.PATH) {
        val principal = call.principal<OAuthAccessTokenResponse.OAuth2>()

        if (principal == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        oAuthApiClient.sendToken(call.request.origin.remoteHost, call.request.headers.port ?: DEFAULT_PORT, principal)
        call.respondAutoClosePage()
    }
}

private suspend fun HttpClient.sendToken(
    host: String, port: Int, principal: OAuthAccessTokenResponse.OAuth2
) = post(Api.OAuth.Callback()) {
    url {
        this.host = host
        this.port = port
    }

    contentType(ContentType.Application.Cbor)
    setBody(Api.OAuth.Callback.RequestBody(JwtTokenPair(principal.accessToken, principal.refreshToken!!)))
}
