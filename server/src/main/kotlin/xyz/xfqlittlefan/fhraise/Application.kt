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

package xyz.xfqlittlefan.fhraise

import com.sanctionco.jmail.JMail
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.models.cleanupVerificationCodes
import xyz.xfqlittlefan.fhraise.models.queryVerificationCode
import xyz.xfqlittlefan.fhraise.models.respondEmailVerificationCode
import xyz.xfqlittlefan.fhraise.models.verifyCode
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = DefaultServerPort, host = "0.0.0.0", module = Application::module).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    val database = AppDatabase.current

    install(Resources)

    install(RateLimit) {
        register(RateLimitName("codeVerification")) {
            rateLimiter(limit = 30, refillPeriod = 60.seconds)
        }
    }

    authentication {
        digest {
            realm = "fhraise-user"
        }
    }

    install(ContentNegotiation) { cbor() }

    cleanupVerificationCodes()

    routing {
        rateLimit(RateLimitName("codeVerification")) {
            post<Api.Auth.Email.Request> {
                val req = call.receive<Api.Auth.Email.Request.RequestBody>()
                if (!JMail.strictValidator().isValid(req.email)) {
                    call.respond(Api.Auth.Email.Request.ResponseBody.InvalidEmailAddress)
                    return@post
                }

                val code = database.queryVerificationCode(this) { emailOwner(req.email) }

                if (req.dry) {
                    call.respond(Api.Auth.Email.Request.ResponseBody.Success)
                    return@post
                }

                call.respondEmailVerificationCode {
                    email = req.email
                    this.code = code.code
                }
            }

            post<Api.Auth.Email.Verify> {
                val req = call.receive<Api.Auth.Email.Verify.RequestBody>()

                if (database.verifyCode(req.code) { emailOwner(req.email) }) {
                    call.respond(Api.Auth.Email.Verify.ResponseBody.Success)
                } else {
                    call.respond(Api.Auth.Email.Verify.ResponseBody.Failure)
                }
            }
        }
    }
}

val applicationConfig = ApplicationConfig("application.yaml")
val applicationSecret = ApplicationConfig("secret.yaml")
