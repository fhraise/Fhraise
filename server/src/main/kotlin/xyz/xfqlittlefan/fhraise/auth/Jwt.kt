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
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import xyz.xfqlittlefan.fhraise.applicationSecret
import xyz.xfqlittlefan.fhraise.models.User
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

val jwtSecret = applicationSecret.propertyOrNull("auth.jwt.secret")?.getString() ?: "secret"
val jwtIssuer = applicationSecret.propertyOrNull("auth.jwt.issuer")?.getString() ?: "fhraise"
val jwtAudience = applicationSecret.propertyOrNull("auth.jwt.audience")?.getString() ?: "fhraise-user"
val jwtRealm = applicationSecret.propertyOrNull("auth.jwt.realm")?.getString() ?: "fhraise"

fun User.generateTokenPair() = JwtTokenPair(
    accessToken = JWT.create().run {
        withIssuer(jwtIssuer)
        withAudience(jwtAudience)
        withClaim("id", id.toString())
        withClaim("username", username)
        withClaim("email", email)
        withExpiresAt((Clock.System.now() + 15.minutes).toJavaInstant())
        sign(Algorithm.HMAC256(jwtSecret))
    },
    refreshToken = JWT.create().run {
        withIssuer(jwtIssuer)
        withAudience(jwtAudience)
        withClaim("id", id.toString())
        withExpiresAt((Clock.System.now() + 30.days).toJavaInstant())
        sign(Algorithm.HMAC256(jwtSecret))
    },
)
