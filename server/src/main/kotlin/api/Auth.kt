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

import com.sanctionco.jmail.JMail
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import xyz.xfqlittlefan.fhraise.*
import xyz.xfqlittlefan.fhraise.auth.*
import xyz.xfqlittlefan.fhraise.http.port
import xyz.xfqlittlefan.fhraise.models.*
import xyz.xfqlittlefan.fhraise.pattern.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.pattern.usernameRegex
import xyz.xfqlittlefan.fhraise.proxy.keycloakScheme
import xyz.xfqlittlefan.fhraise.py.Message
import xyz.xfqlittlefan.fhraise.py.sendMessageToPy
import xyz.xfqlittlefan.fhraise.routes.Api
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.CredentialType.*
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.Request.VerificationType.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val authName = "app"
private val rateLimitName = RateLimitName("app-code-authentication")

val appAuthTimeout =
    appConfig.propertyOrNull("app.auth.timeout")?.getString()?.toLongOrNull()?.milliseconds ?: 5.minutes

private val keycloakClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

private const val authClientId = "fhraise"
private val authClientSecret =
    appSecret.propertyOrNull("auth.keycloak.app.client-secret")?.getString() ?: "client-secret"
private val authNonceSecret =
    (appSecret.propertyOrNull("auth.keycloak.app.nonce-secret")?.getString() ?: "nonce-secret").toByteArray()

private val appTokenUrl = url {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = "localhost"
    port = defaultServerPort
    path("/auth/realms/fhraise/protocol/openid-connect/token")
}

private val appAuthUrl = url {
    protocol = URLProtocol.createOrDefault(keycloakScheme)
    host = "localhost"
    port = defaultServerPort
    path("/auth/realms/fhraise/protocol/openid-connect/auth")
}

fun RateLimitConfig.registerAppCodeVerification() {
    register(rateLimitName) {
        rateLimiter(limit = 30, refillPeriod = 60.seconds)
    }
}

fun AuthenticationConfig.appAuth() {
    oauth(authName) {
        urlProvider = {
            url {
                path(Api.OAuth.PATH)
                parameters.clear()
                parameters.appendAll(request.queryParameters.filter { k, _ -> Api.OAuth.Query.run { k == PROVIDER } })
            }
        }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "fhraise",
                authorizeUrl = appAuthUrl,
                accessTokenUrl = appTokenUrl,
                requestMethod = HttpMethod.Post,
                clientId = authClientId,
                clientSecret = authClientSecret,
                nonceManager = StatelessHmacNonceManager(
                    key = authNonceSecret, timeoutMillis = appAuthTimeout.inWholeMilliseconds
                ),
                authorizeUrlInterceptor = {
                    request.headers.port?.let { port = it }
                    application.log.info("query: ${request.queryParameters}")
                    request.queryParameters[Api.OAuth.Query.PROVIDER]?.let { parameters.append("kc_idp_hint", it) }
                },
            )
        }
        client = keycloakClient
    }
}

@KtorDsl
fun Route.requireAppAuth(block: Route.() -> Unit) = authenticate(authName, build = block)

fun Route.apiAuth() {
    cborContentType {
        apiAuthRequest()
        apiAuthVerify()
    }
    apiAuthFace()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun Route.apiAuthRequest() = rateLimit(rateLimitName) {
    post<Api.Auth.Type.Request> { req ->
        val body = call.receive<Api.Auth.Type.Request.RequestBody>()

        val requestId = call.callId

        if (requestId == null) {
            call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Failure)
            return@post
        }

        if (!req.parent.validate(body.credential)) {
            call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.InvalidCredential)
            return@post
        }

        val user = getOrCreateUser(req.credentialType equals body.credential).getOrThrow()

        val token = jwt {
            withExpiresIn(appAuthTimeout)
            withPayload(
                AuthProcessToken(
                    requestId, user.id!!, body.credential, req.credentialType, req.verificationType
                )
            )
        }

        when (req.verificationType) {
            FhraiseToken, QrCode, SmsCode, EmailCode -> {
                val code = appDatabase.queryOrGenerateVerificationCode(application, token.hashCode())

                if (body.dry) {
                    logger.debug("Responding code dry request with success.")
                    call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Success(token))
                    return@post
                }

                if (sendVerificationCode(req, user, code.code)) {
                    logger.debug("Responding code request with success.")
                    call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Success(token))
                } else {
                    logger.debug("Responding code request with failure.")
                    call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Failure)
                }
            }

            Face -> {
                select {
                    application.launch {
                        if (sendMessageToPy(Message.Ping.Request) is Message.Ping.Response) {
                            logger.debug("Responding face request with success.")
                            call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Success(token))
                        } else {
                            logger.debug("Responding face request with failure.")
                            call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Failure)
                        }
                    }.onJoin {
                        logger.debug("Face request completed.")
                    }

                    onTimeout(5.seconds) {
                        logger.debug("Face request timed out.")
                        call.respondRequestResult(Api.Auth.Type.Request.ResponseBody.Failure)
                    }
                }
            }

            Password -> {
                logger.debug("Responding password request with success.")
                call.respondRequestResult(
                    Api.Auth.Type.Request.ResponseBody.Success(token, user.totp == true)
                )
            }
        }
    }
}

private fun Route.apiAuthVerify() = post<Api.Auth.Type.Verify> { req ->
    val body = call.receive<Api.Auth.Type.Verify.RequestBody>()

    val token = verifiedJwtOrNull(req.token)?.decodedPayloadOrNull<AuthProcessToken>() ?: run {
        call.respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure)
        return@post
    }

    when (token.verificationType) {
        FhraiseToken, QrCode, SmsCode, EmailCode -> call.respondCodeVerificationResult(token, req, body)
        Face -> call.respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure) // 人脸验证使用 WebSocket
        Password -> call.respondPasswordVerificationResult(token, req, body)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun Route.apiAuthFace() = webSocket(Api.Auth.Face.PATH) {
    suspend fun close(message: String, reason: CloseReason.Codes = CloseReason.Codes.VIOLATED_POLICY) =
        close(CloseReason(reason, message))

    val handshakeRequest = select {
        async {
            runCatching { receiveDeserialized<Api.Auth.Face.Handshake.Request>() }.getOrElse {
                sendSerialized<Api.Auth.Face.Handshake.Response>(Api.Auth.Face.Handshake.Response.Failure)
                close("Invalid handshake request.")
                logger.warn("Client sent invalid handshake request.", it)
                error("Client sent invalid handshake request.")
            }
        }.onAwait { it }
        onTimeout(5.seconds) {
            sendSerialized<Api.Auth.Face.Handshake.Response>(Api.Auth.Face.Handshake.Response.Failure)
            close("Handshake timed out.")
            error("Client did not send handshake request.")
        }
    }

    logger.debug("Received handshake request.")

    val token = verifiedJwtOrNull(handshakeRequest.token)?.decodedPayloadOrNull<AuthProcessToken>() ?: run {
        sendSerialized<Api.Auth.Face.Handshake.Response>(Api.Auth.Face.Handshake.Response.Failure)
        logger.warn("Client sent invalid token.")
        return@webSocket close("Invalid token.")
    }

    if (token.verificationType != Face) {
        sendSerialized<Api.Auth.Face.Handshake.Response>(Api.Auth.Face.Handshake.Response.Failure)
        return@webSocket close("Invalid token type.")
    }

    val pyHandshakeResult = sendMessageToPy(Message.Handshake.Request(token.userId))

    if (pyHandshakeResult !is Message.Handshake.Response.Success) {
        sendSerialized<Api.Auth.Face.Handshake.Response>(Api.Auth.Face.Handshake.Response.Failure)
        return@webSocket close("Handshake failed.")
    }

    while (true) {
        val clientMessage = runCatching { receiveDeserialized<Message.Client>() }.getOrElse {
            sendMessageToPy(Message.Client.Cancel)
            logger.warn("Client sent invalid message.", it)
            return@webSocket close("Invalid message.")
        }

        if (clientMessage is Message.Client.Cancel) {
            close("Client cancelled.", CloseReason.Codes.NORMAL)
            sendMessageToPy(clientMessage)
            logger.debug("Client cancelled face verification.")
            return@webSocket
        }

        logger.debug("Sending client message to Python.")

        val pyResult = sendMessageToPy(clientMessage)

        if (pyResult !is Message.Result) {
            close("Internal error.", CloseReason.Codes.INTERNAL_ERROR)
            sendMessageToPy(Message.Client.Cancel)
            logger.error("Python returned invalid result.")
            return@webSocket
        }

        logger.debug("Sending Python result to client.")

        sendSerialized<Message.Result>(pyResult)

        when (pyResult) {
            is Message.Result.InternalError, is Message.Result.Cancelled -> {
                return@webSocket close("Internal error.", CloseReason.Codes.INTERNAL_ERROR)
            }

            is Message.Result.Success -> {
                exchangeToken(token.userId)?.let {
                    sendSerialized<Api.Auth.Type.Verify.ResponseBody>(Api.Auth.Type.Verify.ResponseBody.Success(it))
                } ?: sendSerialized<Api.Auth.Type.Verify.ResponseBody>(Api.Auth.Type.Verify.ResponseBody.Failure)
                return@webSocket close("Success.", CloseReason.Codes.NORMAL)
            }

            else -> {}
        }
    }
}

private suspend fun RoutingCall.respondCodeVerificationResult(
    token: AuthProcessToken, request: Api.Auth.Type.Verify, body: Api.Auth.Type.Verify.RequestBody
) {
    val verificationValid = appDatabase.verifyCode(request.token.hashCode(), body.verification.value)

    if (verificationValid) {
        // TODO: 别再创建了
        getOrCreateUser(request.credentialType equals token.credential).fold(
            onSuccess = { user ->
                var userNeedsUpdate = false

                if (user.enabled != true) {
                    user.enabled = true
                    userNeedsUpdate = true
                }
                if (request.credentialType == Email && user.emailVerified != true) {
                    user.emailVerified = true
                    userNeedsUpdate = true
                }

                if (userNeedsUpdate) user.update()

                exchangeToken(user.id!!)?.let {
                    respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Success(it))
                } ?: respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure)
            },
            onFailure = { respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure) },
        )
    } else {
        respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure)
    }
}

private suspend fun RoutingCall.respondPasswordVerificationResult(
    token: AuthProcessToken, request: Api.Auth.Type.Verify, body: Api.Auth.Type.Verify.RequestBody
) {
    val user = getUser(request.credentialType equals token.credential) ?: run {
        respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure)
        return
    }

    if (user.getCredentials()?.any { it.type == CredentialRepresentation.CredentialType.Password } != true) {
        user.resetPassword { value = body.verification.value }.onSuccess { user.update { enabled = true } }
    }

    keycloakClient.getTokensByPassword(authClientId, authClientSecret, user.username!!, body.verification)?.let {
        respondVerificationResult(
            Api.Auth.Type.Verify.ResponseBody.Success(
                JwtTokenPair(
                    it.accessToken, it.refreshToken
                )
            )
        )
    } ?: respondVerificationResult(Api.Auth.Type.Verify.ResponseBody.Failure)
}

private fun Api.Auth.Type.validate(credential: String) = when (credentialType) {
    Username -> credential.matches(usernameRegex)
    PhoneNumber -> credential.matches(phoneNumberRegex)
    Email -> JMail.strictValidator().isValid(credential)
}

private fun sendVerificationCode(request: Api.Auth.Type.Request, user: UserRepresentation, code: String) =
    when (request.verificationType) {
        FhraiseToken -> false
        QrCode -> false
        SmsCode -> false
        EmailCode -> user.email?.let {
            sendEmailVerificationCode(it, code)
        } ?: false

        else -> false
    }

private fun sendEmailVerificationCode(emailAddress: String, code: String): Boolean {
    if (!smtpReady) {
        return false
    }

    val email = EmailBuilder.startingBlank().apply {
        from("Fhraise", smtpUsername!!)
        to(emailAddress)
        withSubject("Fhraise 邮件地址验证")
        withHTMLText(buildString {
            append("<!DOCTYPE html>")
            appendHTML().emailVerificationCode(code)
        })
    }.buildEmail()

    runCatching {
        MailerBuilder.withTransportStrategy(TransportStrategy.SMTPS)
            .withSMTPServer(smtpServer!!, smtpPort!!, smtpUsername!!, smtpPassword!!).buildMailer()
            .sendMail(email, true)
    }

    return true
}

@Serializable
private data class AuthProcessToken(
    @SerialName("rid") val requestId: String,
    @SerialName("uid") val userId: String,
    @SerialName("crd") val credential: String,
    @SerialName("crt") val credentialType: Api.Auth.Type.CredentialType,
    @SerialName("vrt") val verificationType: Api.Auth.Type.Request.VerificationType
)

private suspend fun RoutingCall.respondRequestResult(result: Api.Auth.Type.Request.ResponseBody) {
    respond<Api.Auth.Type.Request.ResponseBody>(result)
}

private suspend fun RoutingCall.respondVerificationResult(result: Api.Auth.Type.Verify.ResponseBody) {
    respond<Api.Auth.Type.Verify.ResponseBody>(result)
}
