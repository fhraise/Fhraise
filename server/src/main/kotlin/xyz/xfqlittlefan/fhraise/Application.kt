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
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.api.Auth
import xyz.xfqlittlefan.fhraise.models.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = DefaultServerPort, host = "0.0.0.0", module = Application::module).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    val database = AppDatabase.current

    install(Resources)

    install(RateLimit) {
        register(RateLimitName("verifyCode")) {
            rateLimiter(limit = 30, refillPeriod = 60.seconds)
        }
    }

    authentication {
        digest {
            realm = "fhraise-user"
        }
    }

    install(ContentNegotiation) { cbor() }

    cleanupVerifyCodes()

    routing {
        rateLimit(RateLimitName("verifyCode")) {
            post<Auth.Email.Request> { req ->
                if (!JMail.strictValidator().isValid(req.email)) {
                    call.respond(Auth.Email.Request.Response.InvalidEmailAddress)
                    return@post
                }

                val verifyCode = database.dbQuery {
                    VerifyCode.find { VerifyCodes.email eq req.email }.firstOrNull()
                } ?: run {
                    val newCode = database.dbQuery {
                        VerifyCode.new {
                            email = req.email
                            code = (0 until 6).map { (0..9).random() }.joinToString("")
                            createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        }
                    }

                    launch {
                        delay(verifyCodeTtl)

                        database.dbQuery {
                            newCode.delete()
                        }
                    }

                    newCode
                }

                call.respondEmailVerifyCode {
                    email = req.email
                    code = verifyCode.code
                }
            }
        }
    }
}

val applicationConfig = ApplicationConfig("application.yaml")
val applicationSecret = ApplicationConfig("secret.yaml")
