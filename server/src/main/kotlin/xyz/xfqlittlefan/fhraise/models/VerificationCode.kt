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

package xyz.xfqlittlefan.fhraise.models

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import xyz.xfqlittlefan.fhraise.AppDatabase
import xyz.xfqlittlefan.fhraise.applicationConfig
import xyz.xfqlittlefan.fhraise.applicationSecret
import xyz.xfqlittlefan.fhraise.routes.Api
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

val verificationCodeTtl = applicationConfig.propertyOrNull("app.verify-code.ttl")?.getString()?.toLongOrNull()
    ?: 5.minutes.inWholeMilliseconds

val smtpServer = applicationSecret.propertyOrNull("auth.email.smtp.server")?.getString()
val smtpPort = smtpServer?.let { applicationSecret.propertyOrNull("auth.email.smtp.port")?.getString()?.toIntOrNull() }
val smtpUsername = smtpPort?.let { applicationSecret.propertyOrNull("auth.email.smtp.username")?.getString() }
val smtpPassword = smtpUsername?.let { applicationSecret.propertyOrNull("auth.email.smtp.password")?.getString() }
val smtpReady = smtpPassword != null

object VerificationCodes : IdTable<String>() {
    val owner = text("owner")
    val code = char("code", 6)
    val createdAt = datetime("created_at")
    override val id = owner.entityId()
    override val primaryKey = PrimaryKey(owner)
}

class VerificationCode(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, VerificationCode>(VerificationCodes)

    var owner by VerificationCodes.owner
        internal set
    var code by VerificationCodes.code
        internal set
    var createdAt by VerificationCodes.createdAt
        internal set
}

object VerificationCodeOwnerBuilder {
    fun userOwner(uuid: UUID) = "user:$uuid"
    fun phoneNumberOwner(phoneNumber: String) = "phone:$phoneNumber"
    fun emailOwner(email: String) = "email:$email"
}

suspend fun AppDatabase.queryVerificationCode(scope: CoroutineScope, build: VerificationCodeOwnerBuilder.() -> String) =
    VerificationCodeOwnerBuilder.build().let {
        dbQuery {
            VerificationCode.find { VerificationCodes.owner eq it }.firstOrNull()
        } ?: run {
            val newCode = dbQuery {
                VerificationCode.new {
                    owner = it
                    code = (0 until 6).map { (0..9).random() }.joinToString("")
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            scope.launch {
                delay(verificationCodeTtl)

                dbQuery {
                    newCode.delete()
                }
            }

            newCode
        }
    }

suspend fun AppDatabase.verifyCode(code: String, build: VerificationCodeOwnerBuilder.() -> String) = dbQuery {
    VerificationCode.find { VerificationCodes.owner eq VerificationCodeOwnerBuilder.build() }.firstOrNull()?.let {
        if (it.code == code) {
            it.delete()
            true
        } else {
            false
        }
    } ?: false
}

suspend fun RoutingCall.respondEmailVerificationCode(block: EmailVerificationCode.() -> Unit) {
    if (!smtpReady) {
        respond(Api.Auth.Email.Request.ResponseBody.Failure)
        return
    }

    val config = EmailVerificationCode().apply(block)

    val email = EmailBuilder.startingBlank().apply {
        from("Fhraise", "noreply@auth.fhraise.com")
        to(config.email)
        withSubject("Fhraise 邮件地址验证")
        withHTMLText(buildString {
            appendText("<!DOCTYPE html>")
            appendHTML().emailVerificationCode(config.code)
            appendLine()
        })
    }.buildEmail()

    MailerBuilder.withTransportStrategy(TransportStrategy.SMTPS)
        .withSMTPServer(smtpServer!!, smtpPort!!, smtpUsername!!, smtpPassword!!).buildMailer().sendMail(email, true)

    respond(Api.Auth.Email.Request.ResponseBody.Success)
}

class EmailVerificationCode internal constructor() {
    lateinit var email: String
    lateinit var code: String
}

fun TagConsumer<StringBuilder>.emailVerificationCode(code: String) = html {
    head {
        style {
            unsafe {
                raw(
                    """
                        body {
                            font-family: Arial, sans-serif;
                            margin: 0;
                            padding: 0;
                            background-color: #f0f0f0;
                        }
                
                        .email-container {
                            width: 100%;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            background-color: #ffffff;
                            box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.1);
                        }
                
                        .email-header {
                            text-align: center;
                            padding: 20px 0;
                        }
                
                        .email-header img {
                            max-width: 200px;
                        }
                
                        .email-content {
                            padding: 20px;
                        }
                
                        .email-content h1 {
                            color: #333333;
                            font-size: 24px;
                            margin-bottom: 20px;
                        }
                
                        .email-content p {
                            color: #666666;
                            font-size: 16px;
                            line-height: 1.5;
                        }
                
                        .verification-code {
                            display: inline-block;
                            padding: 10px 20px;
                            margin: 20px 0;
                            font-size: 24px;
                            color: #ffffff;
                            background-color: #007BFF;
                            border-radius: 5px;
                        }
                    """.trimIndent()
                )
            }
        }
    }

    body {
        div("email-container") {
            div("email-header") {
                p {
                    +"Fhraise"
                }
            }
            div("email-content") {
                h1 {
                    +"Fhraise 邮件地址验证"
                }
                p {
                    +"您的验证码是："
                }
                div("verification-code") {
                    +code
                }
                p {
                    +"请在 5 分钟内完成验证。"
                }
            }
        }
    }
}

fun Application.cleanupVerificationCodes() {
    launch(Dispatchers.IO) {
        log.trace("Cleaning up expired verification codes, ttl: $verificationCodeTtl")
        AppDatabase.current.dbQuery {
            VerificationCode.all().forEach {
                log.trace("Checking verification code {} created at {}", it.id, it.createdAt)
                if (it.createdAt.toInstant(TimeZone.currentSystemDefault()) + verificationCodeTtl.milliseconds < Clock.System.now()) {
                    log.trace("Verification code {} is expired, deleting", it.id)
                    it.delete()
                } else {
                    log.trace("Verification code {} is still valid", it.id)
                }
            }
        }
        log.trace("Verification code cleanup done")
    }
}
