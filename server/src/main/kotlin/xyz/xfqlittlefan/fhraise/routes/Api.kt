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

package xyz.xfqlittlefan.fhraise.routes

import com.sanctionco.jmail.JMail
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import xyz.xfqlittlefan.fhraise.AppDatabase
import xyz.xfqlittlefan.fhraise.models.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.models.queryVerificationCode
import xyz.xfqlittlefan.fhraise.models.respondEmailVerificationCode
import xyz.xfqlittlefan.fhraise.models.verifyCode

fun Route.apiAuthPhoneNumberRequest(database: AppDatabase) = post<Api.Auth.PhoneNumber.Request> {
    val req = call.receive<Api.Auth.PhoneNumber.Request.RequestBody>()
    if (!req.phoneNumber.matches(phoneNumberRegex)) {
        call.respond(Api.Auth.PhoneNumber.Request.ResponseBody.InvalidPhoneNumber)
        return@post
    }

    database.queryVerificationCode(this) { phoneNumberOwner(req.phoneNumber) }

    if (req.dry) {
        call.respond(Api.Auth.PhoneNumber.Request.ResponseBody.Success)
        return@post
    }

    // 无短信服务
    call.respond(Api.Auth.PhoneNumber.Request.ResponseBody.Failure)
}

fun Route.apiAuthPhoneNumberVerify(database: AppDatabase) = post<Api.Auth.PhoneNumber.Verify> {
    val req = call.receive<Api.Auth.PhoneNumber.Verify.RequestBody>()

    if (database.verifyCode(req.code) { phoneNumberOwner(req.phoneNumber) }) {
        call.respond(Api.Auth.PhoneNumber.Verify.ResponseBody.Success)
    } else {
        call.respond(Api.Auth.PhoneNumber.Verify.ResponseBody.Failure)
    }
}

fun Route.apiAuthEmailRequest(database: AppDatabase) = post<Api.Auth.Email.Request> {
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

fun Route.apiAuthEmailVerify(database: AppDatabase) = post<Api.Auth.Email.Verify> {
    val req = call.receive<Api.Auth.Email.Verify.RequestBody>()

    if (database.verifyCode(req.code) { emailOwner(req.email) }) {
        call.respond(Api.Auth.Email.Verify.ResponseBody.Success)
    } else {
        call.respond(Api.Auth.Email.Verify.ResponseBody.Failure)
    }
}
