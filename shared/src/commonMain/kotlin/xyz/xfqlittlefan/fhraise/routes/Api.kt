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

import io.ktor.http.*
import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Resource("/api")
class Api {
    @Resource("auth")
    class Auth(val parent: Api = Api()) {
        @Resource("phoneNumber")
        class PhoneNumber(val parent: Auth = Auth()) {
            @Resource("request")
            class Request(val parent: PhoneNumber = PhoneNumber()) {
                @Serializable
                data class RequestBody(val phoneNumber: String, val dry: Boolean = false)

                @Serializable
                enum class ResponseBody {
                    Success, InvalidPhoneNumber, Failure
                }
            }

            @Resource("verify")
            class Verify(val parent: PhoneNumber = PhoneNumber()) {
                @Serializable
                data class RequestBody(val phoneNumber: String, val code: String)

                @Serializable
                enum class ResponseBody {
                    Success, Failure
                }
            }
        }

        @Resource("email")
        class Email(val parent: Auth = Auth()) {
            @Resource("request")
            class Request(val parent: Email = Email()) {
                @Serializable
                data class RequestBody(val email: String, val dry: Boolean = false)

                @Serializable
                enum class ResponseBody {
                    Success, InvalidEmailAddress, Failure
                }
            }

            @Resource("verify")
            class Verify(val parent: Email = Email()) {
                @Serializable
                data class RequestBody(val email: String, val code: String)

                @Serializable
                enum class ResponseBody {
                    Success, Failure
                }
            }
        }

        @Resource("oauth")
        class OAuth {
            object Socket {
                const val PATH = "/api/auth/oauth/socket"

                object Query {
                    const val REQUEST_ID = "rid"
                    const val CALLBACK_PORT = "prt"
                }

                @Serializable
                data class ClientMessage(val port: UShort, val sendDeepLink: Boolean = false)

                @Serializable
                enum class ServerMessage(val next: Any? = null) {
                    Ready(ReadyMessage), Received, Result(ResultMessage);

                    @Serializable
                    data class ReadyMessage(val url: String) {
                        constructor(builder: URLBuilder.() -> Unit) : this(URLBuilder().apply(builder).buildString())
                    }

                    @Serializable
                    enum class ResultMessage(val next: Any? = null) {
                        Success(UserIdMessage), Failure;

                        @Serializable
                        data class UserIdMessage(val id: String)
                    }
                }
            }

            @Serializable
            enum class Provider(val api: String, val callback: String) {
                Microsoft("/api/auth/oauth/sign-in/ms", "/api/auth/oauth/callback/ms")
            }
        }
    }
}
