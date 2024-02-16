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
                    data class Success(val token: String, val otpNeeded: Boolean = false) : ResponseBody()

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
                data class RequestBody(val verification: Verification) {
                    @Serializable
                    data class Verification(val value: String, val otp: String? = null)
                }

                @Serializable
                sealed class ResponseBody {
                    @Serializable
                    data class Success(val tokenPair: JwtTokenPair) : ResponseBody()

                    @Serializable
                    data object Failure : ResponseBody()
                }
            }

            @Serializable
            enum class CredentialType { Username, PhoneNumber, Email }
        }
    }

    @Resource("oauth")
    class OAuth(val parent: Api = Api()) {
        @Resource("request")
        data class Request(val parent: OAuth = OAuth(), val provider: Provider? = null)

        @Serializable
        enum class Provider(val brokerName: String) {
            Google("google"), GitHub("github"), Microsoft("microsoft")
        }

        companion object {
            const val PATH = "/api/oauth"
        }

        @Resource("callback")
        data class Callback(val parent: OAuth = OAuth()) {
            @Serializable
            data class RequestBody(val tokenPair: JwtTokenPair)
        }

        object Query {
            const val PROVIDER = "p"
        }
    }
}
