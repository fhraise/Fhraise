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
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xfqlittlefan.fhraise.logger

suspend fun HttpClient.getTokensByPassword(clientId: String, clientSecret: String, username: String, password: String) =
    runCatching {
        submitForm(
            url = keycloakTokenUrl,
            formParameters = parameters {
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("grant_type", "password")
                append("username", username)
                append("password", password)
            },
        ).body<KeycloakTokens>()
    }.getOrElse {
        logger.error("Failed to get tokens by password", it)
        null
    }

suspend fun HttpClient.refreshTokens(clientId: String, clientSecret: String, refreshToken: String) = runCatching {
    submitForm(
        url = keycloakTokenUrl,
        formParameters = parameters {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        },
    ).body<KeycloakTokens>()
}.getOrElse {
    logger.error("Failed to refresh tokens", it)
    null
}

@Serializable
data class KeycloakTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
