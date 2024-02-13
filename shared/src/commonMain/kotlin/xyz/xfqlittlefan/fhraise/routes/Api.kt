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

import io.ktor.resources.*
import kotlinx.serialization.Serializable
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair

@Resource("/api")
class Api {
    @Resource("auth")
    class Auth(val parent: Api = Api()) {
        @Resource("{credentialType}")
        class Type(val parent: Auth = Auth(), val credentialType: CredentialType) {
            @Resource("request")
            class Request(val parent: Type, val type: VerificationType) {
                constructor(credentialType: CredentialType, verificationType: VerificationType) : this(
                    Type(credentialType = credentialType), verificationType
                )

                @Serializable
                data class RequestBody(val credential: String, val dry: Boolean = false)

                @Serializable
                sealed class ResponseBody {
                    @Serializable
                    data class Success(val token: String) : ResponseBody()

                    @Serializable
                    data object InvalidCredential : ResponseBody()

                    @Serializable
                    data object Failure : ResponseBody()
                }

                @Serializable
                enum class VerificationType { VerificationCode, Password }
            }

            @Resource("verify")
            class Verify(val parent: Type, val token: String) {
                constructor(credentialType: CredentialType, token: String) : this(
                    Type(credentialType = credentialType), token
                )

                @Serializable
                data class RequestBody(val verification: String)

                @Serializable
                sealed class ResponseBody {
                    @Serializable
                    data class Success(val tokenPair: JwtTokenPair) : ResponseBody()

                    @Serializable
                    data object Failure : ResponseBody()
                }
            }

            @Serializable
            enum class CredentialType { Username, Email, PhoneNumber }
        }
    }

    @Resource("oauth")
    class OAuth(val parent: Api = Api()) {
        @Resource("request")
        class Request(
            val parent: OAuth = OAuth(), val provider: Provider, val callbackPort: Int, val sendDeepLink: Boolean
        ) {
            @Serializable
            data class ResponseBody(val requestId: String, val signInUrl: String)
        }

        @Resource("message")
        class Message(val parent: OAuth = OAuth()) {
            @Serializable
            data class RequestBody(val authSessionId: String)
        }

        @Serializable
        enum class Provider(val brokerName: String, val domain: String) {
            Google(
                "google", "google.com"
            ),
            GitHub(
                "github", "github.com"
            ),
            Microsoft(
                "microsoft", "microsoftonline.com"
            ),
        }

        companion object {
            const val PATH = "/api/oauth"
        }

        object Endpoint {
            const val PATH = "/api/oauth/endpoint"

            object Query {
                const val BROKER_NAME = "b"
            }
        }

        object Callback {
            const val PATH = "/api/oauth/callback"

            @Serializable
            data class RequestBody(val requestId: String, val tokenPair: JwtTokenPair)
        }

        object Query {
            const val PROVIDER = "p"
            const val REQUEST_ID = "rid"
            const val CALLBACK_PORT = "prt"
            const val SEND_DEEP_LINK = "deep"
        }
    }
}
