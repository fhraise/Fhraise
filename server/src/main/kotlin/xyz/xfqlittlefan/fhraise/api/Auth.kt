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
import io.ktor.server.auth.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.future.await
import kotlinx.html.stream.appendHTML
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import xyz.xfqlittlefan.fhraise.appConfig
import xyz.xfqlittlefan.fhraise.appDatabase
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.auth.*
import xyz.xfqlittlefan.fhraise.cborContentType
import xyz.xfqlittlefan.fhraise.models.*
import xyz.xfqlittlefan.fhraise.pattern.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.pattern.usernameRegex
import xyz.xfqlittlefan.fhraise.routes.Api
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.CredentialType.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val authName = "app"
private val rateLimitName = RateLimitName("app-code-authentication")

val appAuthTimeout =
    appConfig.propertyOrNull("app.auth.timeout")?.getString()?.toLongOrNull()?.milliseconds ?: 5.minutes

val oAuthApiClient = HttpClient {
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

fun RateLimitConfig.registerAppCodeVerification() {
    register(rateLimitName) {
        rateLimiter(limit = 30, refillPeriod = 60.seconds)
    }
}

fun AuthenticationConfig.appAuth() {
    oauth(authName) {
        urlProvider = {
            url {
                host = "localhost"
                port = request.queryParameters[Api.Auth.Query.CALLBACK_PORT]?.toIntOrNull() ?: DEFAULT_PORT
                path(Api.Auth.CALLBACK)
                parameters.clear()
                parameters[Api.Auth.Query.REQUEST_ID] = request.queryParameters[Api.Auth.Query.REQUEST_ID] ?: ""
                parameters[Api.Auth.Query.CALLBACK_PORT] = port.toString()
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
            )
        }
        client = oAuthApiClient
    }
}

@KtorDsl
fun Route.requireAppAuth(block: Route.() -> Unit) = authenticate(authName, build = block)

fun Route.apiAuth() {
    apiAuthRequest()
    apiAuthVerify()
}

private fun Route.apiAuthRequest() = rateLimit(rateLimitName) {
    cborContentType {
        post<Api.Auth.Type.Request> { req ->
            val body = call.receive<Api.Auth.Type.Request.RequestBody>()

            val requestId = call.callId

            if (requestId == null) {
                call.respond(Api.Auth.Type.Request.ResponseBody.Failure)
                return@post
            }

            if (!req.parent.validate(body.credential)) {
                call.respond(Api.Auth.Type.Request.ResponseBody.InvalidCredential)
                return@post
            }

            val token = jwt {
                withExpiresIn(appAuthTimeout)
                withPayload(AuthProcessToken(requestId, req.type, body.credential.hashCode()))
            }

            when (req.type) {
                Api.Auth.Type.Request.VerificationType.VerificationCode -> {
                    val code = appDatabase.queryOrGenerateVerificationCode(this, token.hashCode())

                    if (body.dry) {
                        call.respond(Api.Auth.Type.Request.ResponseBody.Success(token))
                        return@post
                    }

                    if (sendVerificationCode(req, body.credential, code.code)) {
                        call.respond(Api.Auth.Type.Request.ResponseBody.Success(token))
                    } else {
                        call.respond(Api.Auth.Type.Request.ResponseBody.Failure)
                    }
                }

                Api.Auth.Type.Request.VerificationType.Password -> call.respond(
                    Api.Auth.Type.Request.ResponseBody.Success(token)
                )
            }
        }
    }
}

private fun Route.apiAuthVerify() = post<Api.Auth.Type.Verify> { req ->
    val body = call.receive<Api.Auth.Type.Verify.RequestBody>()

    val token = verifiedJwtOrNull(req.token) ?: run {
        call.respond(Api.Auth.Type.Verify.ResponseBody.Failure)
        return@post
    }

    when (token.decodedPayload<AuthProcessToken>().type) {
        Api.Auth.Type.Request.VerificationType.VerificationCode -> call.respondCodeVerificationResult(req, body)
        Api.Auth.Type.Request.VerificationType.Password -> call.respondPasswordVerificationResult(req, body)
    }
}

private suspend fun RoutingCall.respondCodeVerificationResult(
    request: Api.Auth.Type.Verify, body: Api.Auth.Type.Verify.RequestBody
) {
    val verificationValid = appDatabase.verifyCode(request.token.hashCode(), body.verification)

    if (verificationValid) {
        getOrCreateUser {
            when (request.parent.credentialType) {
                Username -> username = body.verification
                Email -> email = body.verification
                PhoneNumber -> phoneNumber = body.verification
            }
        }.fold(
            onSuccess = { user ->
                exchangeToken(user.id!!)?.let {
                    respond(Api.Auth.Type.Verify.ResponseBody.Success(it))
                } ?: respond(Api.Auth.Type.Verify.ResponseBody.Failure)
            },
            onFailure = { respond(Api.Auth.Type.Verify.ResponseBody.Failure) },
        )
    } else {
        respond(Api.Auth.Type.Verify.ResponseBody.Failure)
    }
}

private suspend fun RoutingCall.respondPasswordVerificationResult(
    request: Api.Auth.Type.Verify, body: Api.Auth.Type.Verify.RequestBody
) {
    val user = getUser {
        when (request.parent.credentialType) {
            Username -> username = body.verification
            Email -> email = body.verification
            PhoneNumber -> phoneNumber = body.verification
        }
    } ?: run {
        respond(Api.Auth.Type.Verify.ResponseBody.Failure)
        return
    }

    oAuthApiClient.getTokensByPassword(authClientId, authClientSecret, user.username!!, body.verification)?.let {
        respond(Api.Auth.Type.Verify.ResponseBody.Success(JwtTokenPair(it.accessToken, it.refreshToken)))
    } ?: respond(Api.Auth.Type.Verify.ResponseBody.Failure)
}

private fun Api.Auth.Type.validate(credential: String) = when (credentialType) {
    Username -> credential.matches(usernameRegex)
    PhoneNumber -> credential.matches(phoneNumberRegex)
    Email -> JMail.strictValidator().isValid(credential)
}

private suspend fun sendVerificationCode(request: Api.Auth.Type.Request, credential: String, code: String) =
    when (request.parent.credentialType) {
        Username -> false
        PhoneNumber -> false
        Email -> sendEmailVerificationCode(credential, code)
    }

private suspend fun sendEmailVerificationCode(emailAddress: String, code: String): Boolean {
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
            .sendMail(email, true).await()
    }.onFailure { return false }

    return true
}

@Serializable
private data class AuthProcessToken(
    @SerialName("rid") val requestId: String,
    @SerialName("typ") val type: Api.Auth.Type.Request.VerificationType,
    @SerialName("crd") val credentialHash: Int
)
