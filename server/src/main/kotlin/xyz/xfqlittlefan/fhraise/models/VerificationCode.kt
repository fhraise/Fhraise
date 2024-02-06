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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.html.*
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import xyz.xfqlittlefan.fhraise.AppDatabase
import xyz.xfqlittlefan.fhraise.appDatabase
import xyz.xfqlittlefan.fhraise.applicationConfig
import xyz.xfqlittlefan.fhraise.applicationSecret
import xyz.xfqlittlefan.fhraise.routes.Api
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
    val createdAt = timestamp("created_at")
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

private fun Api.Auth.Type.getOwner(credential: String) = "${type.name}:$credential"

suspend fun AppDatabase.queryOrGenerateVerificationCode(
    scope: CoroutineScope, authType: Api.Auth.Type, credential: String
) = authType.getOwner(credential).let {
    dbQuery {
        VerificationCode.find { VerificationCodes.owner eq it }.firstOrNull()
    } ?: run {
        val newCode = dbQuery {
            VerificationCode.new {
                owner = it
                code = (0 until 6).map { (0..9).random() }.joinToString("")
                createdAt = Clock.System.now()
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

suspend fun AppDatabase.verifyCode(code: String, authType: Api.Auth.Type, credential: String) = dbQuery {
    VerificationCode.find { VerificationCodes.owner eq authType.getOwner(credential) }.firstOrNull()?.let {
        if (it.code == code) {
            it.delete()
            true
        } else {
            false
        }
    } ?: false
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
        appDatabase.dbQuery {
            VerificationCode.find { VerificationCodes.createdAt less (Clock.System.now() - verificationCodeTtl.milliseconds) }
                .forEach {
                    log.trace("Deleting expired verification code for owner {}", it.owner)
                    it.delete()
                }
        }
        log.trace("Finished cleaning up expired verification codes")
    }
}
