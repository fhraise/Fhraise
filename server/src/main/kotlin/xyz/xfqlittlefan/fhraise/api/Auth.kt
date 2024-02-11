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
import io.ktor.http.*
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
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import xyz.xfqlittlefan.fhraise.appConfig
import xyz.xfqlittlefan.fhraise.appDatabase
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.auth.jwt
import xyz.xfqlittlefan.fhraise.auth.withExpiresIn
import xyz.xfqlittlefan.fhraise.models.*
import xyz.xfqlittlefan.fhraise.pattern.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.pattern.usernameRegex
import xyz.xfqlittlefan.fhraise.proxy.keycloakHost
import xyz.xfqlittlefan.fhraise.proxy.keycloakPort
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val authName = "app"
private val rateLimitName = RateLimitName("app-code-authentication")

val appAuthTimeout =
    appConfig.propertyOrNull("app.auth.timeout")?.getString()?.toLongOrNull()?.milliseconds ?: 5.minutes

val oAuthApiClient = HttpClient()

fun RateLimitConfig.registerAppCodeVerification() {
    register(rateLimitName) {
        rateLimiter(limit = 30, refillPeriod = 60.seconds)
    }
}

fun AuthenticationConfig.appAuth() {
    oauth(authName) {
        urlProvider = {
            url {
                val requestId = request.queryParameters[Api.Auth.Query.REQUEST_ID] ?: ""
                host = "localhost"
                port = request.queryParameters[Api.Auth.Query.CALLBACK_PORT]?.toIntOrNull() ?: DEFAULT_PORT
                path(Api.Auth.CALLBACK)
                parameters.clear()
                parameters[Api.Auth.Query.REQUEST_ID] = requestId
                parameters[Api.Auth.Query.CALLBACK_PORT] = port.toString()
            }
        }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "fhraise",
                authorizeUrl = "http://$keycloakHost:$keycloakPort/auth/realms/fhraise/protocol/openid-connect/auth",
                accessTokenUrl = "http://$keycloakHost:$keycloakPort/auth/realms/fhraise/protocol/openid-connect/token",
                requestMethod = HttpMethod.Post,
                clientId = "fhraise",
                clientSecret = appSecret.propertyOrNull("auth.keycloak.app.client-secret")?.getString()
                    ?: "client-secret",
                nonceManager = StatelessHmacNonceManager(
                    key = (appSecret.propertyOrNull("auth.keycloak.app.nonce-secret")?.getString()
                        ?: "nonce-secret").toByteArray(),
                    timeoutMillis = appAuthTimeout.inWholeMilliseconds,
                ),
            )
        }
        client = oAuthApiClient
    }
}

@KtorDsl
fun Route.requireAppAuth(block: Route.() -> Unit) = authenticate(authName, build = block)

fun Route.apiAuth() {
    rateLimit(rateLimitName) {
        apiAuthRequest()
    }
    apiAuthVerify()
}

private fun Route.apiAuthRequest() = post<Api.Auth.Type.Request> { req ->
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
        withClaim(jwtClaimRequestId, requestId)
        withClaim(jwtClaimType, "${req.parent.credentialType}:${req.type}".hashCode())
        withClaim(jwtClaimCredential, body.credential.hashCode())
    }

    when (req.type) {
        Api.Auth.Type.Request.VerificationType.VerificationCode -> {

            val code = appDatabase.queryOrGenerateVerificationCode(this, token.hashCode())

            if (body.dry) {
                call.respondSuccess(token)
                return@post
            }

            if (sendVerificationCode(req, body.credential, code.code)) {
                call.respondSuccess(token)
            } else {
                call.respond(Api.Auth.Type.Request.ResponseBody.Failure)
            }
        }

        Api.Auth.Type.Request.VerificationType.Password -> call.respondSuccess(token)
    }
}

private fun Route.apiAuthVerify() = post<Api.Auth.Type.Verify> { req ->
    val body = call.receive<Api.Auth.Type.Verify.RequestBody>()

    if (req.parent.validate(body.credential) && appDatabase.verifyCode(req.token.hashCode(), body.credential)) {
        call.respond(Api.Auth.Type.Verify.ResponseBody.Success)
    } else {
        call.respond(Api.Auth.Type.Verify.ResponseBody.Failure)
    }
}

private fun Api.Auth.Type.validate(credential: String) = when (credentialType) {
    Api.Auth.Type.CredentialType.Username -> credential.matches(usernameRegex)
    Api.Auth.Type.CredentialType.PhoneNumber -> credential.matches(phoneNumberRegex)
    Api.Auth.Type.CredentialType.Email -> JMail.strictValidator().isValid(credential)
}

private suspend fun sendVerificationCode(request: Api.Auth.Type.Request, credential: String, code: String) =
    when (request.parent.credentialType) {
        Api.Auth.Type.CredentialType.Username -> false
        Api.Auth.Type.CredentialType.PhoneNumber -> false
        Api.Auth.Type.CredentialType.Email -> sendEmailVerificationCode(credential, code)
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

private const val jwtClaimRequestId = "rid"
private const val jwtClaimType = "typ"
private const val jwtClaimCredential = "crd"

private suspend fun RoutingCall.respondSuccess(token: String) =
    respond(Api.Auth.Type.Request.ResponseBody.Success(token))
