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
import xyz.xfqlittlefan.fhraise.api.appAuthTimeout
import xyz.xfqlittlefan.fhraise.appDatabase
import xyz.xfqlittlefan.fhraise.appSecret

val smtpServer = appSecret.propertyOrNull("auth.email.smtp.server")?.getString()
val smtpPort = smtpServer?.let { appSecret.propertyOrNull("auth.email.smtp.port")?.getString()?.toIntOrNull() }
val smtpUsername = smtpPort?.let { appSecret.propertyOrNull("auth.email.smtp.username")?.getString() }
val smtpPassword = smtpUsername?.let { appSecret.propertyOrNull("auth.email.smtp.password")?.getString() }
val smtpReady = smtpPassword != null

object VerificationCodes : IdTable<Int>() {
    val tokenHash = integer("token_hash")
    val code = char("code", 6)
    val createdAt = timestamp("created_at")
    override val id = tokenHash.entityId()
    override val primaryKey = PrimaryKey(tokenHash)
}

class VerificationCode(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerificationCode>(VerificationCodes)

    var tokenHash by VerificationCodes.tokenHash
        internal set
    var code by VerificationCodes.code
        internal set
    var createdAt by VerificationCodes.createdAt
        internal set
}

suspend fun AppDatabase.queryOrGenerateVerificationCode(scope: CoroutineScope, tokenHash: Int) = dbQuery {
    VerificationCode.find { VerificationCodes.tokenHash eq tokenHash }.firstOrNull()
} ?: dbQuery {
    VerificationCode.new {
        this.tokenHash = tokenHash
        code = (0 until 6).map { (0..9).random() }.joinToString("")
        createdAt = Clock.System.now()
    }
}.also {
    scope.launch {
        delay(appAuthTimeout)
        dbQuery { it.delete() }
    }
}

suspend fun AppDatabase.verifyCode(tokenHash: Int, code: String) = dbQuery {
    VerificationCode.find { VerificationCodes.tokenHash eq tokenHash }.firstOrNull()?.let {
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
        log.trace("Cleaning up expired verification codes, ttl: $appAuthTimeout")
        appDatabase.dbQuery {
            VerificationCode.find { VerificationCodes.createdAt less (Clock.System.now() - appAuthTimeout) }.forEach {
                log.trace("Deleting expired verification code for token {}", it.tokenHash)
                it.delete()
            }
        }
        log.trace("Finished cleaning up expired verification codes")
    }
}
