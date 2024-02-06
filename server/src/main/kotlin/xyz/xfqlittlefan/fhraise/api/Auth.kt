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

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sanctionco.jmail.JMail
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.html.stream.appendHTML
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import xyz.xfqlittlefan.fhraise.AppDatabase
import xyz.xfqlittlefan.fhraise.auth.jwtAudience
import xyz.xfqlittlefan.fhraise.auth.jwtIssuer
import xyz.xfqlittlefan.fhraise.auth.jwtRealm
import xyz.xfqlittlefan.fhraise.auth.jwtSecret
import xyz.xfqlittlefan.fhraise.models.*
import xyz.xfqlittlefan.fhraise.pattern.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.pattern.usernameRegex
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.seconds

private const val authName = "app"
private val rateLimitName = RateLimitName("app-code-verification")

fun RateLimitConfig.registerAppCodeVerification() {
    register(rateLimitName) {
        rateLimiter(limit = 30, refillPeriod = 60.seconds)
    }
}

fun AuthenticationConfig.appAuth() {
    jwt(authName) {
        realm = jwtRealm
        verifier(JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer(jwtIssuer).withAudience(jwtAudience).build())
        validate { credential ->
            if (credential.payload.getClaim("id").asString() != "") {
                JWTPrincipal(credential.payload)
            } else {
                null
            }
        }
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

private fun Route.apiAuthRequest() = post<Api.Auth.Type.Request> {
    val req = call.receive<Api.Auth.Type.Request.RequestBody>()

    if (!it.validate(req.credential)) {
        call.respond(Api.Auth.Type.Request.ResponseBody.InvalidCredential)
        return@post
    }

    val code = AppDatabase.current.queryOrGenerateVerificationCode(this, it.parent, req.credential)

    if (req.dry) {
        call.respond(Api.Auth.Type.Request.ResponseBody.Success)
        return@post
    }

    call.respondVerificationCode(it, req.credential, code.code)
}

private fun Route.apiAuthVerify() = post<Api.Auth.Type.Verify> {
    val req = call.receive<Api.Auth.Type.Verify.RequestBody>()

    if (AppDatabase.current.verifyCode(req.code, it.parent, req.credential)) {
        call.respond(Api.Auth.Type.Verify.ResponseBody.Success)
    } else {
        call.respond(Api.Auth.Type.Verify.ResponseBody.Failure)
    }
}

private fun Api.Auth.Type.Request.validate(credential: String) = when (parent.type) {
    Api.Auth.Type.Enum.Username -> credential.matches(usernameRegex)
    Api.Auth.Type.Enum.PhoneNumber -> credential.matches(phoneNumberRegex)
    Api.Auth.Type.Enum.Email -> JMail.strictValidator().isValid(credential)
}

private suspend fun RoutingCall.respondVerificationCode(
    request: Api.Auth.Type.Request, credential: String, code: String
) = when (request.parent.type) {
    Api.Auth.Type.Enum.Username -> respond(Api.Auth.Type.Request.ResponseBody.Failure)
    Api.Auth.Type.Enum.PhoneNumber -> respond(Api.Auth.Type.Request.ResponseBody.Failure)
    Api.Auth.Type.Enum.Email -> respondEmailVerificationCode(credential, code)
}

suspend fun RoutingCall.respondEmailVerificationCode(emailAddress: String, code: String) {
    if (!smtpReady) {
        respond(Api.Auth.Type.Request.ResponseBody.Failure)
        return
    }

    val email = EmailBuilder.startingBlank().apply {
        from("Fhraise", "noreply@auth.fhraise.com")
        to(emailAddress)
        withSubject("Fhraise 邮件地址验证")
        withHTMLText(buildString {
            appendText("<!DOCTYPE html>")
            appendHTML().emailVerificationCode(code)
            appendLine()
        })
    }.buildEmail()

    MailerBuilder.withTransportStrategy(TransportStrategy.SMTPS)
        .withSMTPServer(smtpServer!!, smtpPort!!, smtpUsername!!, smtpPassword!!).buildMailer().sendMail(email, true)

    respond(Api.Auth.Type.Request.ResponseBody.Success)
}
