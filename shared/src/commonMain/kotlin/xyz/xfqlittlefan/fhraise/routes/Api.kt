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
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair

@Resource("/api")
class Api {
    @Resource("auth")
    class Auth(val parent: Api = Api()) {
        @Resource("{credentialType}")
        class Type(
            val parent: Auth = Auth(), val credentialType: CredentialType
        ) {
            @Resource("request")
            class Request(val parent: Type, val type: VerificationType) {
                @Serializable
                data class RequestBody(val credential: String, val dry: Boolean = false)

                @Serializable
                sealed class ResponseBody {
                    @Serializable
                    data class Success(val token: String)

                    @Serializable
                    data object InvalidCredential

                    @Serializable
                    data object Failure
                }

                @Serializable
                enum class VerificationType { VerificationCode, Password }
            }

            @Resource("verify")
            class Verify(val parent: Type, val token: String) {
                @Serializable
                data class RequestBody(val credential: String, val verification: String)

                @Serializable
                enum class ResponseBody { Success, Failure }
            }

            @Serializable
            enum class CredentialType { Username, PhoneNumber, Email }
        }

        companion object {
            const val CALLBACK = "/api/auth/callback"
        }

        object Query {
            const val REQUEST_ID = "rid"
            const val CALLBACK_PORT = "prt"
        }
    }

    @Resource("oauth")
    class OAuth {
        object Socket {
            const val PATH = "/api/oauth/socket"

            @Serializable
            data class ClientMessage(val provider: Provider, val port: UShort, val sendDeepLink: Boolean = false)

            @Serializable
            enum class ServerMessage(val next: Any? = null) {
                Ready(ReadyMessage), Received, Result(ResultMessage);

                @Serializable
                data class ReadyMessage(val url: String) {
                    constructor(builder: URLBuilder.() -> Unit) : this(URLBuilder().apply(builder).buildString())
                }

                @Serializable
                enum class ResultMessage(val next: Any? = null) {
                    Success(TokenPairMessage), Failure;

                    @Serializable
                    data class TokenPairMessage(val tokenPair: JwtTokenPair)
                }
            }
        }

        @Serializable
        enum class Provider(
            val api: String,
            val callback: String,
            val clientId: String,
            val authorizeUrl: String,
            val authorizeUrlInterceptor: URLBuilder.() -> Unit,
            val accessTokenUrl: String,
            val defaultScopes: List<String>
        ) {
            Google(
                "/api/auth/oauth/sign-in/gg",
                "/api/auth/oauth/callback/gg",
                "64440822162-pd97va9v49vvj07vvhdd02au6li129s1.apps.googleusercontent.com",
                "https://accounts.google.com/o/oauth2/v2/auth",
                {},
                "https://www.googleapis.com/oauth2/v4/token",
                listOf(
                    "https://www.googleapis.com/auth/userinfo.email",
                    "https://www.googleapis.com/auth/userinfo.profile",
                    "openid"
                )
            ),
            Microsoft(
                "/api/auth/oauth/sign-in/ms",
                "/api/auth/oauth/callback/ms",
                "af9f38ce-17ce-481b-8629-36452244007b",
                "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize",
                {
                    parameters["response_type"] = "code id_token"
                    parameters["response_mode"] = "form_post"
                },
                "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                listOf("https://graph.microsoft.com/.default", "openid")
            );
        }
    }
}
