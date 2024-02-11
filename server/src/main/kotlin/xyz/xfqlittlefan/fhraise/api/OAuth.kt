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
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xfqlittlefan.fhraise.AppDatabase
import xyz.xfqlittlefan.fhraise.appClient
import xyz.xfqlittlefan.fhraise.appDatabase
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.auth.generateTokenPair
import xyz.xfqlittlefan.fhraise.flow.IdMessageFlow
import xyz.xfqlittlefan.fhraise.html.respondAutoClosePage
import xyz.xfqlittlefan.fhraise.link.AppUri
import xyz.xfqlittlefan.fhraise.models.Users
import xyz.xfqlittlefan.fhraise.models.getOrCreateUser
import xyz.xfqlittlefan.fhraise.routes.Api
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val oAuthFlow = IdMessageFlow<OAuthMessage>(MutableSharedFlow(replay = 3))

sealed class OAuthMessage {
    data object Received : OAuthMessage()

    data class Finished(val tokenPair: JwtTokenPair?) : OAuthMessage()

    data class Response(val block: suspend RoutingContext.(tokenPair: JwtTokenPair?) -> Unit) : OAuthMessage()
}

data class OAuthUserPrincipal(val id: String, val name: String? = null, val email: String? = null)

fun AuthenticationConfig.appOAuth() {
    Api.OAuth.Provider.entries.forEach { provider ->
        oauth(provider.name) {
            urlProvider = {
                url {
                    val requestId = request.queryParameters[Api.Auth.Query.REQUEST_ID] ?: ""
                    host = "localhost"
                    port = request.queryParameters[Api.Auth.Query.CALLBACK_PORT]?.toIntOrNull() ?: DEFAULT_PORT
                    path(provider.callback)
                    parameters.clear()
                    parameters[Api.Auth.Query.REQUEST_ID] = requestId
                    parameters[Api.Auth.Query.CALLBACK_PORT] = port.toString()
                }
            }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = provider.name,
                    authorizeUrl = provider.authorizeUrl,
                    accessTokenUrl = provider.accessTokenUrl,
                    requestMethod = HttpMethod.Post,
                    clientId = provider.clientId,
                    clientSecret = appSecret.propertyOrNull("auth.oauth.${provider.name.lowercase(Locale.US)}.client-secret")
                        ?.getString() ?: "client-secret",
                    nonceManager = StatelessHmacNonceManager(
                        appSecret.propertyOrNull("auth.oauth.${provider.name.lowercase(Locale.US)}.nonce-secret")
                            ?.getString()?.toByteArray() ?: "nonce-secret".toByteArray()
                    ),
                    authorizeUrlInterceptor = provider.authorizeUrlInterceptor,
                    defaultScopes = provider.defaultScopes
                )
            }
            client = appClient
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Route.apiOAuth() {
    apiOAuthWebsocket()
    Api.OAuth.Provider.entries.forEach { provider ->
        authenticate(provider.name) {
            get(provider.api) {}
        }

        authenticate(provider.name, optional = true) {
            get(provider.callback) {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                val requestId = call.queryParameters[Api.Auth.Query.REQUEST_ID]
                val response = async(start = CoroutineStart.UNDISPATCHED) {
                    select<suspend RoutingContext.(JwtTokenPair?) -> Unit> {
                        async {
                            (oAuthFlow.take { it.first == requestId && it.second is OAuthMessage.Response }.second as OAuthMessage.Response).block
                        }.onAwait { it }
                        onTimeout(5.seconds) {
                            {
                                call.respondAutoClosePage()
                            }
                        }
                    }
                }
                if (principal != null && requestId != null) {
                    oAuthFlow.emit(requestId to OAuthMessage.Received)
                    val oAuthPrincipal = appClient.getOAuthUserPrincipal(principal, provider)
                    val tokenPair = appDatabase.getOrCreateOAuthUser(oAuthPrincipal, provider).generateTokenPair()
                    tokenPair.let {
                        oAuthFlow.emit(requestId to OAuthMessage.Finished(it))
                        response.await()(it)
                    }
                } else {
                    response.await()(null)
                }
            }
        }
    }
}

private fun Route.apiOAuthWebsocket() {
    webSocket(Api.OAuth.Socket.PATH) {
        val requestId = call.callId
        if (requestId == null) {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "No call ID"))
            return@webSocket
        }

        val clientMessage = receiveDeserialized<Api.OAuth.Socket.ClientMessage>()

        // 此处使用 application 的 coroutineScope，确保 collector 至少在收到 Received 消息后可以发出 Response 消息
        // 而不会受到 WebSocket 连接意外关闭的影响
        // 这种用法是安全的，因为 WebSocket 连接关闭后，sendSerialized 会抛出异常，而导致协程取消
        val collect = application.launch(start = CoroutineStart.UNDISPATCHED) {
            oAuthFlow.collect(requestId) { message ->
                when (message) {
                    is OAuthMessage.Received -> {
                        oAuthFlow.emit(requestId to OAuthMessage.Response { tokenPair ->
                            if (clientMessage.sendDeepLink) {
                                call.respondRedirect(AppUri.OAuthCallback.fromTokenPair(tokenPair, requestId).uri)
                            } else {
                                call.respondAutoClosePage()
                            }
                        })
                        sendSerialized(Api.OAuth.Socket.ServerMessage.Received)
                    }

                    is OAuthMessage.Finished -> {
                        sendSerialized(Api.OAuth.Socket.ServerMessage.Result)
                        if (message.tokenPair != null) {
                            sendSerialized(Api.OAuth.Socket.ServerMessage.ResultMessage.Success)
                            sendSerialized(Api.OAuth.Socket.ServerMessage.ResultMessage.TokenPairMessage(message.tokenPair))
                        } else {
                            sendSerialized(Api.OAuth.Socket.ServerMessage.ResultMessage.Failure)
                        }
                        close()
                    }

                    else -> Unit
                }
            }
        }

        sendSerialized(Api.OAuth.Socket.ServerMessage.Ready)
        sendSerialized(Api.OAuth.Socket.ServerMessage.ReadyMessage {
            host = call.request.origin.localAddress
            port = call.request.origin.localPort
            path(Api.OAuth.Provider.Microsoft.api)
            parameters[Api.Auth.Query.REQUEST_ID] = requestId
            parameters[Api.Auth.Query.CALLBACK_PORT] = clientMessage.port.toString()
        })

        delay(5.minutes)
        collect.cancel()
        close(CloseReason(CloseReason.Codes.NORMAL, "Timeout"))
    }
}

private fun AppUri.OAuthCallback.Companion.fromTokenPair(tokenPair: JwtTokenPair?, requestId: String) =
    AppUri.OAuthCallback(tokenPair?.accessToken, tokenPair?.refreshToken, requestId)

private suspend fun HttpClient.getOAuthUserPrincipal(
    principal: OAuthAccessTokenResponse.OAuth2, provider: Api.OAuth.Provider
) = when (provider) {
    Api.OAuth.Provider.Google -> getOAuthUserPrincipalFromGoogle(principal.extraParameters["id_token"] as String)
    Api.OAuth.Provider.Microsoft -> getOAuthUserPrincipalFromMicrosoft(principal.accessToken)
}

private suspend fun AppDatabase.getOrCreateOAuthUser(oAuthPrincipal: OAuthUserPrincipal, provider: Api.OAuth.Provider) =
    dbQuery {
        getOrCreateUser(provider.oAuthColumn, oAuthPrincipal.id).apply {
            name ?: run { name = oAuthPrincipal.name }
            email ?: run { email = oAuthPrincipal.email }
        }
    }

private val Api.OAuth.Provider.oAuthColumn
    get() = when (this) {
        Api.OAuth.Provider.Google -> Users.google
        Api.OAuth.Provider.Microsoft -> Users.microsoft
    }

suspend fun HttpClient.getOAuthUserPrincipalFromGoogle(accessToken: String) = get {
    url("https://www.googleapis.com/userinfo/v2/me")
    headers {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
    }
}.body<GoogleUserInfo>().let {
    OAuthUserPrincipal(it.id, it.name, it.verifiedEmail?.let { verified -> if (verified) it.email else null })
}

@Serializable
data class GoogleUserInfo(
    val id: String, val name: String?, val email: String?, @SerialName("verified_email") val verifiedEmail: Boolean?
)

@Serializable
data class GoogleCerts(val keys: List<GoogleCert>) {
    @Serializable
    data class GoogleCert(val kid: String, val n: String, val e: String)
}

suspend fun HttpClient.getOAuthUserPrincipalFromMicrosoft(accessToken: String) = get {
    url("https://graph.microsoft.com/v1.0/me")
    headers {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
    }
}.body<MicrosoftUserInfo>().let {
    OAuthUserPrincipal(it.id, it.displayName, it.mail)
}

@Serializable
data class MicrosoftUserInfo(val id: String, val displayName: String?, val mail: String?)
