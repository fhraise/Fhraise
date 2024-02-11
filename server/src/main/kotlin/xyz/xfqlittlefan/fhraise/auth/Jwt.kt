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
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import xyz.xfqlittlefan.fhraise.appSecret
import xyz.xfqlittlefan.fhraise.models.User
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private val jwtSecret = appSecret.propertyOrNull("auth.jwt.secret")?.getString() ?: "secret"
val jwtIssuer = appSecret.propertyOrNull("auth.jwt.issuer")?.getString() ?: "fhraise"
val jwtAudience = appSecret.propertyOrNull("auth.jwt.audience")?.getString() ?: "fhraise-user"
val jwtRealm = appSecret.propertyOrNull("auth.jwt.realm")?.getString() ?: "fhraise"

/**
 * 创建一个包含默认签发者和受众的 JWT token。
 *
 * @param builder 配置这个 JWT token 的构造器。
 * @return 生成的 JWT token。
 *
 * @throws com.auth0.jwt.exceptions.JWTCreationException 如果构造 JWT token 时发生错误。
 *
 * @see JWTCreator.Builder
 * @see JWTCreator.Builder.sign
 */
fun jwt(builder: JWTCreator.Builder.() -> Unit): String = JWT.create().apply {
    withIssuer(jwtIssuer)
    withAudience(jwtAudience)
    builder()
}.sign(Algorithm.HMAC256(jwtSecret))

fun User.generateTokenPair() = JwtTokenPair(
    accessToken = jwt {
        withIssuedAtNow()
        withExpiresIn(15.minutes)
        withClaim("id", id.toString())
        withClaim("username", username)
        withClaim("email", email)
    },
    refreshToken = jwt {
        withIssuedAtNow()
        withExpiresIn(30.days)
        withClaim("id", id.toString())
    },
)

fun JWTCreator.Builder.withIssuedAtNow(): JWTCreator.Builder = withIssuedAt(Clock.System.now().toJavaInstant())

fun JWTCreator.Builder.withExpiresAt(instant: Instant): JWTCreator.Builder = withExpiresAt(instant.toJavaInstant())

fun JWTCreator.Builder.withExpiresIn(duration: Duration): JWTCreator.Builder =
    withExpiresAt(Clock.System.now() + duration)
