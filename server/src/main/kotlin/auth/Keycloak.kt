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
import io.ktor.server.application.*
import io.ktor.server.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.models.CredentialRepresentation
import xyz.xfqlittlefan.fhraise.models.UserQuery
import xyz.xfqlittlefan.fhraise.models.UserRepresentation
import xyz.xfqlittlefan.fhraise.proxy.keycloakHost
import xyz.xfqlittlefan.fhraise.proxy.keycloakPort
import xyz.xfqlittlefan.fhraise.proxy.keycloakScheme
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.days

private val authClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

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
            sendWithoutRequest { it.url.host == keycloakHost && it.url.port == keycloakPort }
        }
    }
}

private const val adminClientId = "admin-cli"
private val adminUsername = appSecret.propertyOrNull("auth.keycloak.admin.username")?.getString() ?: "admin"
private val adminPassword = appSecret.propertyOrNull("auth.keycloak.admin.password")?.getString() ?: "admin"
private val adminClientSecret = appSecret.propertyOrNull("auth.keycloak.admin.client-secret")?.getString() ?: "admin"

val keycloakTokenUrl = url {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = keycloakHost
    port = keycloakPort
    path("/auth/realms/fhraise/protocol/openid-connect/token")
}

private var currentTokens: BearerTokens? = null

private const val phoneNumberAttribute = "phoneNumber"

private suspend fun loadTokens() =
    currentTokens ?: getTokens()?.let { BearerTokens(it.accessToken, it.refreshToken) }?.also { currentTokens = it }

private suspend fun getTokens() = authClient.getTokensByPassword(
    adminClientId, adminClientSecret, adminUsername, Api.Auth.Type.Verify.RequestBody.Verification(adminPassword)
)

private suspend fun RefreshTokensParams.refreshTokens() =
    (oldTokens?.refreshToken ?: currentTokens?.refreshToken)?.let {
        authClient.refreshTokens(adminClientId, adminClientSecret, it) ?: getTokens()
    }?.let { BearerTokens(it.accessToken, it.refreshToken) }?.also { currentTokens = it }

suspend fun exchangeToken(userId: String) = currentTokens?.let {
    runCatching {
        adminClient.submitForm(
            url = keycloakTokenUrl,
            formParameters = parameters {
                append("client_id", adminClientId)
                append("client_secret", adminClientSecret)
                append("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                append("subject_token", it.accessToken)
                append("requested_token_type", "urn:ietf:params:oauth:token-type:refresh_token")
                append("audience", "fhraise")
                append("requested_subject", userId)
            },
        ).body<KeycloakTokens>()
    }.getOrNull()
}?.let { JwtTokenPair(it.accessToken, it.refreshToken) }

suspend fun getOrCreateUser(block: UserQuery.() -> Unit) = getUser(block)?.let { Result.success(it) } ?: run {
    createUser(UserQuery().apply(block)).fold(
        onSuccess = { getUser(block)?.let { Result.success(it) } ?: Result.failure(IllegalStateException()) },
        onFailure = { Result.failure(it) }, // TODO: 什么鬼玩意！！！
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
        it.status.isSuccess() -> it.body<List<UserRepresentation>>().firstOrNull()
        else -> null
    }
}?.let {
    adminClient.get(adminUrl {
        appendPathSegments("users", it.id!!)
    })
}?.let {
    when {
        it.status.isSuccess() -> it.body<UserRepresentation>()
        else -> null
    }
}

private suspend fun createUser(query: UserQuery) = adminClient.post {
    url(adminUrl { appendPathSegments("users") })
    contentType(ContentType.Application.Json)
    setBody(UserRepresentation().apply {
        username = query.generatedUsername
        email = query.email
        query.phoneNumber?.let { attributes = mapOf(phoneNumberAttribute to listOf(it)) }
    })
}.let { if (it.status.isSuccess()) Result.success(Unit) else Result.failure(Exception(it.bodyAsText())) }

suspend fun UserRepresentation.getCredentials() = adminClient.get(adminUrl {
    appendPathSegments("users", id!!, "credentials")
}).let {
    when {
        it.status.isSuccess() -> it.body<List<CredentialRepresentation>>()
        else -> null
    }
}

suspend fun UserRepresentation.update(block: UserRepresentation.() -> Unit = {}) =
    this@update.apply(block).let { newUser ->
        adminClient.put {
            url(adminUrl { appendPathSegments("users", id!!) })
            contentType(ContentType.Application.Json)
            setBody(newUser)
        }.let { if (it.status.isSuccess()) Result.success(newUser) else Result.failure(Exception(it.bodyAsText())) }
    }

suspend fun UserRepresentation.resetPassword(block: CredentialRepresentation.() -> Unit) = adminClient.put {
    url(adminUrl { appendPathSegments("users", id!!, "reset-password") })
    contentType(ContentType.Application.Json)
    setBody(CredentialRepresentation(type = CredentialRepresentation.CredentialType.Password).apply(block))
}.let { if (it.status.isSuccess()) Result.success(Unit) else Result.failure(Exception(it.bodyAsText())) }

private suspend fun UserRepresentation.delete() = adminClient.delete {
    url(adminUrl { appendPathSegments("users", id!!) })
}.let { if (it.status.isSuccess()) Result.success(Unit) else Result.failure(Exception(it.bodyAsText())) }

fun Application.cleanupUnverifiedUsersPreDay() {
    launch(Dispatchers.IO) {
        while (true) {
            log.trace("Cleaning up unverified users")
            getUnverifiedUsers()?.filter { it.createdAt!! + 7.days < Clock.System.now() }?.forEach {
                log.trace("Deleting unverified user ${it.username} (id: ${it.id})")
                it.delete()
            }
            log.trace(
                "Finished cleaning up unverified users, next time cleaning up: {}",
                (Clock.System.now() + 1.days).toLocalDateTime(TimeZone.currentSystemDefault())
            )
            delay(1.days)
        }
    }
}

private suspend fun getUnverifiedUsers(first: Int = 0): List<UserRepresentation>? = adminClient.get(adminUrl {
    appendPathSegments("users")
    parameters.apply {
        append("first", first.toString())
        append("max", "100")
        append("enabled", "false")
    }
}).let {
    when {
        it.status.isSuccess() -> it.body<List<UserRepresentation>>()
        else -> null
    }
}?.run {
    if (size == 100) getUnverifiedUsers(first + 100)?.plus(this) else this
}

infix fun Api.Auth.Type.CredentialType.equals(credential: String): UserQuery.() -> Unit = {
    when (this@equals) {
        Api.Auth.Type.CredentialType.Username -> username = credential
        Api.Auth.Type.CredentialType.PhoneNumber -> phoneNumber = credential
        Api.Auth.Type.CredentialType.Email -> email = credential
    }
}

private fun adminUrl(builder: URLBuilder.() -> Unit) = URLBuilder().apply {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = keycloakHost
    port = keycloakPort
    path("/auth/admin/realms/fhraise")
    builder()
}.buildString()
