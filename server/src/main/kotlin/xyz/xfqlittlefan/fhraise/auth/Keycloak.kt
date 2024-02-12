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

package xyz.xfqlittlefan.fhraise.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.util.*
import kotlinx.serialization.json.Json
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.defaultServerPort
import xyz.xfqlittlefan.fhraise.models.UserQuery
import xyz.xfqlittlefan.fhraise.models.UserRepresentation
import xyz.xfqlittlefan.fhraise.proxy.keycloakHost
import xyz.xfqlittlefan.fhraise.proxy.keycloakPort
import xyz.xfqlittlefan.fhraise.proxy.keycloakScheme

private val adminClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(Auth) {
        bearer {
            loadTokens(::loadTokens)
            refreshTokens(RefreshTokensParams::refreshTokens)
            sendWithoutRequest { it.url.host == "localhost" && it.url.port == defaultServerPort }
        }
    }
}

private const val adminClientId = "admin-cli"
private val adminUsername = appSecret.propertyOrNull("auth.keycloak.admin.username")?.getString() ?: "admin"
private val adminPassword = appSecret.propertyOrNull("auth.keycloak.admin.password")?.getString() ?: "admin"
private val adminClientSecret = appSecret.propertyOrNull("auth.keycloak.admin.clientSecret")?.getString() ?: "admin"

val appAuthUrl = url {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = "localhost"
    port = defaultServerPort
    path("/auth/realms/fhraise/protocol/openid-connect/auth")
}

val appTokenUrl = url {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = "localhost"
    port = defaultServerPort
    path("/auth/realms/fhraise/protocol/openid-connect/token")
}

private var currentTokens = BearerTokens("", "")

private const val phoneNumberAttribute = "phoneNumber"

private suspend fun loadTokens(): BearerTokens? =
    adminClient.getTokensByPassword(adminClientId, adminClientSecret, adminUsername, adminPassword)
        ?.let { BearerTokens(it.accessToken, it.refreshToken) }?.also { currentTokens = it }

private suspend fun RefreshTokensParams.refreshTokens(): BearerTokens? =
    adminClient.refreshTokens(adminClientId, adminClientSecret, oldTokens?.refreshToken ?: currentTokens.refreshToken)
        ?.let { BearerTokens(it.accessToken, it.refreshToken) }?.also { currentTokens = it }

suspend fun exchangeToken(userId: String) = runCatching {
    adminClient.submitForm(
        url = appTokenUrl,
        formParameters = parameters {
            append("client_id", adminClientId)
            append("client_secret", adminClientSecret)
            append("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
            append("subject_token", currentTokens.accessToken)
            append("requested_token_type", "urn:ietf:params:oauth:token-type:refresh_token")
            append("audience", "fhraise")
            append("requested_subject", userId)
        },
    ).body<KeycloakTokens>()
}.getOrNull()?.let { JwtTokenPair(it.accessToken, it.refreshToken) }

suspend fun getOrCreateUser(block: UserQuery.() -> Unit) = getUser(block)?.let { Result.success(it) } ?: run {
    createUser(UserQuery().apply(block)).fold(
        onSuccess = { getUser(block)?.let { Result.success(it) } ?: Result.failure(IllegalStateException()) },
        onFailure = { Result.failure(it) },
    )
}

suspend fun getUser(block: UserQuery.() -> Unit) = adminClient.get(adminUrl {
    appendPathSegments("users")
    val query = UserQuery().apply(block)
    parameters.apply {
        query.username?.let { append("username", it) }
        query.email?.let { append("email", it) }
        query.phoneNumber?.let { append("q", "$phoneNumberAttribute:$it") }
        append("max", "1")
    }
}).let {
    when {
        it.status.isSuccess() -> it.body<List<UserRepresentation>>().first()
        else -> null
    }
}

private suspend fun createUser(query: UserQuery) = adminClient.post {
    url(adminUrl { appendPathSegments("users") })
    setBody(UserRepresentation().apply {
        username = query.generatedUsername
        email = query.email
        query.phoneNumber?.let { attributes = mapOf(phoneNumberAttribute to listOf(it)) }
    })
}.let { if (it.status.isSuccess()) Result.success(Unit) else Result.failure(Exception(it.bodyAsText())) }

fun adminUrl(builder: URLBuilder.() -> Unit) = URLBuilder().apply {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = keycloakHost
    port = keycloakPort
    path("/auth/admin/realms/fhraise")
    builder()
}.buildString()
