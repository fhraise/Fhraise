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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.html.stream.appendHTML
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import xyz.xfqlittlefan.fhraise.AppDatabase
import xyz.xfqlittlefan.fhraise.api.Auth
import xyz.xfqlittlefan.fhraise.applicationConfig
import xyz.xfqlittlefan.fhraise.applicationSecret
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

val verifyCodeTtl = applicationConfig.propertyOrNull("app.verify-code.ttl")?.getString()?.toLongOrNull()
    ?: 5.minutes.inWholeMilliseconds

val smtpServer = applicationSecret.propertyOrNull("auth.email.smtp.server")?.getString()
val smtpPort = smtpServer?.let { applicationSecret.propertyOrNull("auth.email.smtp.port")?.getString()?.toIntOrNull() }
val smtpUsername = smtpPort?.let { applicationSecret.propertyOrNull("auth.email.smtp.username")?.getString() }
val smtpPassword = smtpUsername?.let { applicationSecret.propertyOrNull("auth.email.smtp.password")?.getString() }
val smtpReady = smtpPassword != null

object VerifyCodes : IdTable<String>() {
    val email = text("email")
    val code = char("code", 6)
    val createdAt = datetime("created_at")
    override val id = email.entityId()
    override val primaryKey = PrimaryKey(email)
}

class VerifyCode(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, VerifyCode>(VerifyCodes)

    var email by VerifyCodes.email
    var code by VerifyCodes.code
    var createdAt by VerifyCodes.createdAt
}

suspend fun RoutingCall.respondEmailVerifyCode(block: EmailVerifyCode.() -> Unit) {
    if (!smtpReady) {
        respond(Auth.Email.Response.Failure)
        return
    }

    val config = EmailVerifyCode().apply(block).verifyDsl()

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

    document.create

    val email = EmailBuilder.startingBlank().apply {
        from("Fhraise", smtpUsername!!)
        to(config.email!!)
        withSubject("Fhraise 邮件地址验证")
        withHTMLText(buildString {
            appendText("<!DOCTYPE html>")
            appendHTML().emailVerifyCode(config.code!!)
            appendLine()
        })
    }.buildEmail()

    MailerBuilder.withTransportStrategy(TransportStrategy.SMTPS)
        .withSMTPServer(smtpServer!!, smtpPort!!, smtpUsername!!, smtpPassword!!).buildMailer().sendMail(email, true)

    respond(Auth.Email.Response.Success)
}

class EmailVerifyCode internal constructor() {
    var email: String? = null
    var code: String? = null

    internal fun verifyDsl(): EmailVerifyCode {
        requireNotNull(email) { "Email is required" }
        requireNotNull(code) { "Code is required" }
        return this
    }
}

fun TagConsumer<StringBuilder>.emailVerifyCode(code: String) = html {
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

fun Application.cleanupVerifyCodes() {
    launch {
        exposedLogger.trace("Cleaning up expired verify codes, ttl: $verifyCodeTtl")
        AppDatabase.current.dbQuery {
            VerifyCode.all().forEach {
                exposedLogger.trace("Checking verify code {} created at {}", it.id, it.createdAt)
                if (it.createdAt.toInstant(TimeZone.currentSystemDefault()) + verifyCodeTtl.milliseconds < Clock.System.now()) {
                    exposedLogger.trace("Verify code {} is expired, deleting", it.id)
                    it.delete()
                } else {
                    exposedLogger.trace("Verify code {} is still valid", it.id)
                }
            }
        }
        exposedLogger.trace("Verify code cleanup done")
    }
}
