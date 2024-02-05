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

package xyz.xfqlittlefan.fhraise.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import xyz.xfqlittlefan.fhraise.applicationSecret
import xyz.xfqlittlefan.fhraise.models.User
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

internal val jwtSecret = applicationSecret.propertyOrNull("auth.jwt.secret")?.getString() ?: "secret"
internal val jwtIssuer = applicationSecret.propertyOrNull("auth.jwt.issuer")?.getString() ?: "fhraise"
internal val jwtAudience = applicationSecret.propertyOrNull("auth.jwt.audience")?.getString() ?: "fhraise-user"
internal val jwtRealm = applicationSecret.propertyOrNull("auth.jwt.realm")?.getString() ?: "fhraise"

fun AuthenticationConfig.appJwt() {
    jwt("app") {
        realm = jwtRealm
        verifier(JWT.require(Algorithm.HMAC256(jwtSecret)).withIssuer(jwtIssuer).withAudience(jwtAudience).build())
        validate { credential ->
            if (credential.payload.getClaim("id").asString() != "") {
                JWTPrincipal(credential.payload)
            } else {
                null
            }
        }
    }
}

interface JwtTokenPair {
    val accessToken: String
    val refreshToken: String
}

suspend fun RoutingContext.generateTokenPair(user: User) = object : JwtTokenPair {
    override val accessToken = JWT.create().run {
        withIssuer(jwtIssuer)
        withAudience(jwtAudience)
        withClaim("id", user.id.toString())
        withClaim("username", user.username)
        withClaim("email", user.email)
        withExpiresAt((Clock.System.now() + 15.minutes).toJavaInstant())
        sign(Algorithm.HMAC256(jwtSecret))
    }
    override val refreshToken = JWT.create().run {
        withIssuer(jwtIssuer)
        withAudience(jwtAudience)
        withClaim("id", user.id.toString())
        withExpiresAt((Clock.System.now() + 30.days).toJavaInstant())
        sign(Algorithm.HMAC256(jwtSecret))
    }
}
