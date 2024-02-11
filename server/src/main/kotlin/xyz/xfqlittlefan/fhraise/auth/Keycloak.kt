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
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.proxy.keycloakHost
import xyz.xfqlittlefan.fhraise.proxy.keycloakPort

private val keycloakAuthClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(Auth) {
        bearer {
            loadTokens(::loadTokens)
            refreshTokens(RefreshTokensParams::refreshTokens)
            sendWithoutRequest { it.url.host == keycloakHost && it.url.port == keycloakPort }
        }
    }
}

private suspend fun loadTokens(): BearerTokens? = runCatching {
    keycloakAuthClient.submitForm(
        url = "http://$keycloakHost:$keycloakPort/auth/realms/master/protocol/openid-connect/token",
        formParameters = parameters {
            append("grant_type", "password")
            append("client_id", "admin-cli")
            append("client_secret", adminClientSecret)
            append("username", adminUsername)
            append("password", adminPassword)
        },
    ).body<KeycloakTokens>()
}.getOrNull()?.let { BearerTokens(it.accessToken, it.refreshToken) }

private suspend fun RefreshTokensParams.refreshTokens(): BearerTokens? = runCatching {
    keycloakAuthClient.submitForm(
        url = "http://$keycloakHost:$keycloakPort/auth/realms/master/protocol/openid-connect/token",
        formParameters = parameters {
            append("grant_type", "refresh_token")
            append("client_id", "admin-cli")
            append("refresh_token", oldTokens?.refreshToken ?: "")
        },
    ).body<KeycloakTokens>()
}.getOrNull()?.let { BearerTokens(it.accessToken, it.refreshToken) }

@Serializable
private data class KeycloakTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Int,
)

private val adminUsername = appSecret.propertyOrNull("auth.keycloak.admin.username")?.getString() ?: "admin"
private val adminPassword = appSecret.propertyOrNull("auth.keycloak.admin.password")?.getString() ?: "admin"
private val adminClientSecret = appSecret.propertyOrNull("auth.keycloak.admin.clientSecret")?.getString() ?: "admin"
